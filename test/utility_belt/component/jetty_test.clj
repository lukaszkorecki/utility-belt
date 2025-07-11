(ns utility-belt.component.jetty-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [utility-belt.component.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [utility-belt.component :as component.utils]))

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
    (component/stop system)))
