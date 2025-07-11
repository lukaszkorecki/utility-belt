(ns utility-belt.component.jetty-test

  (:require [utility-belt.compile]
            [clojure.string]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [com.stuartsierra.component :as component]
            [utility-belt.component :as component.utils]))

(def java-major-version (-> (clojure.string/split (System/getProperty "java.version") #"\.") first parse-long))

(utility-belt.compile/compile-when (> java-major-version 11)
  (require '[utility-belt.component.jetty :as jetty])

  (deftest it-does-things-test
    (let [port (+ 3000 (rand-int 1000))
          system (-> {:echo (fn [s] (str "echo: " s))

                      :web-server (component/using
                                   (jetty/create {:config {:port port}
                                                  :handler (fn [{:keys [uri component]}]
                                                             (let [echo-fn (:echo component)]
                                                               {:status 200
                                                                :body (echo-fn uri)}))})
                                   [:echo])}

                     (component.utils/map->system)
                     (component/start))]

      (is (instance? org.eclipse.jetty.server.Server (-> system :web-server :jetty)))

      (is (= "echo: /test/foo" (slurp (format "http://localhost:%s/test/foo" port))))
      (component/stop system))))
