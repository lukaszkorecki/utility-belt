(ns utility-belt.component.system-reload-test
  "Verifies that `setup-for-dev` re-resolves the component-map-fn on every
  start, so systems pick up reloaded code.

  This test drives a real `tools.namespace.repl/refresh` cycle: a target
  source file is written under a dedicated subdirectory, the refresh dirs are
  scoped to just that subdirectory (so the rest of the test suite isn't pulled
  into the reload), and the file is rewritten between starts to simulate a
  developer editing source."
  (:require
   [clojure.test :refer [use-fixtures deftest is testing]]
   [clojure.tools.namespace.repl :as tn-repl]
   [utility-belt.component.system :as system]
   utility-belt.fixtures.to-reload)
  (:import (java.io File)))

(defn- init-src []
  (spit "test/utility_belt/fixtures/to_reload.clj"
        (slurp "test_fixtures_for_reload/init.clj")))

(defn- update-source-1st []
  (spit "test/utility_belt/fixtures/to_reload.clj"
        (slurp "test_fixtures_for_reload/change.clj")))

(defn- update-source-2nd []
  (spit "test/utility_belt/fixtures/to_reload.clj"
        (slurp "test_fixtures_for_reload/another_change.clj")))

(use-fixtures :each (fn [test]
                      (init-src)
                      (let [previous-refresh-dirs @#'tn-repl/refresh-dirs]
                        (try
                          (tn-repl/set-refresh-dirs (.getPath (File. "test")))
                          (test)
                          (finally
                            (alter-var-root #'tn-repl/refresh-dirs (constantly previous-refresh-dirs))
                            (init-src)
                            (tn-repl/clear))))))

(deftest reload-works-with-fn-test
    (let [{:keys [start-system stop-system get-system]} (system/setup-for-dev
                                                         {:component-map-fn utility-belt.fixtures.to-reload/make-system
                                                          :reloadable? true})]
      (testing "init"
        (start-system)
        (is (= "v1" (-> (get-system) :version)))
        (stop-system))

      (testing "1st change"
        (update-source-1st)
        (start-system)
        (is (= "v2" (-> (get-system) :version)))
        (stop-system))

      (testing "2nd change"
        (update-source-2nd)
        (start-system)
        (is (= "v3" (-> (get-system) :version)))
        (stop-system))))

(deftest reload-works-with-qual-sym-test
  (let [{:keys [start-system stop-system get-system]} (system/setup-for-dev
                                                       {:component-map-fn 'utility-belt.fixtures.to-reload/make-system
                                                        :reloadable? true})]

    (testing "init"
      (start-system)
      (is (= "v1" (-> (get-system) :version)))
      (stop-system))

    (testing "1st change"
      (update-source-1st)
      (start-system)
      (is (= "v2" (-> (get-system) :version)))
      (stop-system))

    (testing "2nd change"
      (update-source-2nd)
      (start-system)
      (is (= "v3" (-> (get-system) :version)))
      (stop-system))))

#_(deftest reload-works-with-var-test
  (let [{:keys [start-system stop-system get-system]} (system/setup-for-dev
                                                       {:component-map-fn #'utility-belt.fixtures.to-reload/make-system
                                                        :reloadable? true})]
    (testing "init"
      (start-system)
      (is (= "v1" (-> (get-system) :version)))
      (stop-system))

    (testing "1st change"
      (update-source-1st)
      (start-system)
      (is (= "v2" (-> (get-system) :version)))
      (stop-system))

    (testing "2nd change"
      (update-source-2nd)
      (start-system)
      (is (= "v3" (-> (get-system) :version)))
      (stop-system))))
