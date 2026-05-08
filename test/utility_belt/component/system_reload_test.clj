(ns utility-belt.component.system-reload-test
  "Verifies that `setup-for-dev` re-resolves the component-map-fn on every
  start, so systems pick up reloaded code.

  This test drives a real `tools.namespace.repl/refresh` cycle: a target
  source file is written under a dedicated subdirectory, the refresh dirs are
  scoped to just that subdirectory (so the rest of the test suite isn't pulled
  into the reload), and the file is rewritten between starts to simulate a
  developer editing source."
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [clojure.tools.namespace.repl :as tn-repl]
   [utility-belt.component.system :as system])
  (:import (java.io File)))

(def target-ns-sym 'utility-belt.component.reload-target.system)
(def target-fn-sym 'utility-belt.component.reload-target.system/make-system)
(def ^File target-dir (io/file "test/utility_belt/component/reload_target"))
(def ^File target-file (io/file target-dir "system.clj"))

(defn- source-for [version]
  (str "(ns " target-ns-sym "\n"
       "  (:require [utility-belt.component :as component]))\n"
       "(defn make-system []\n"
       "  {:version " version "\n"
       "   :thing (component/map->component\n"
       "            {:start (fn [this] (assoc this :started-version " version "))\n"
       "             :stop  (fn [this] this)})})\n"))

(defn- write-source!
  "Writes the target file and bumps mtime forward by at least 2s to defeat
  filesystems with 1s mtime resolution, so tools.namespace will reliably
  detect the change."
  [version]
  (.mkdirs target-dir)
  (let [prev (if (.exists target-file) (.lastModified target-file) 0)]
    (spit target-file (source-for version))
    (.setLastModified target-file (max (System/currentTimeMillis)
                                       (+ prev 2000)))))

(defn- cleanup! []
  (when (find-ns target-ns-sym) (remove-ns target-ns-sym))
  (when (.exists target-file) (.delete target-file))
  (when (.exists target-dir) (.delete target-dir))
  (tn-repl/clear))

(deftest reload-picks-up-updated-component-map-fn
  (testing "after editing source + refresh, started system reflects the new definition"
    (cleanup!)
    (write-source! :v1)
    (let [previous-refresh-dirs @#'tn-repl/refresh-dirs]
      (try
        (tn-repl/set-refresh-dirs (.getPath target-dir))
        (let [{:keys [start-system stop-system get-system]}
              (system/setup-for-dev
               {:component-map-fn target-fn-sym
                :reloadable? true})]
          (start-system)
          (is (= :v1 (-> (get-system) :version))
              "first start uses the initial source")
          (is (= :v1 (-> (get-system) :thing :started-version)))
          (stop-system)

          (write-source! :v2)
          (start-system)
          (is (= :v2 (-> (get-system) :version))
              "second start, after refresh, picks up the edited source")
          (is (= :v2 (-> (get-system) :thing :started-version)))
          (stop-system))
        (finally
          (alter-var-root #'tn-repl/refresh-dirs (constantly previous-refresh-dirs))
          (cleanup!))))))
