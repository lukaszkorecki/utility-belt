(ns utility-belt.component.jetty-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as component]
   [utility-belt.compile]
   [utility-belt.component :as component.utils]
   [utility-belt.component.jetty :as jetty]))

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
    (testing "virtual thread pool is not used by default"
      (is (nil? (-> system :web-server :jetty bean :threadPool bean :virtualThreadsExecutor))))

    (is (= "echo: /test/foo" (slurp (format "http://localhost:%s/test/foo" port))))
    (component/stop system)))

(deftest it-can-use-virtual-threads-test
  (let [port (+ 3000 (rand-int 1000))
        system (-> {:echo (fn [s] (str "echo: " s))
                    :web-server (component/using
                                 (jetty/create {:config {:port port
                                                         :virtual-threads? true}
                                                :handler (fn [{:keys [uri component]}]
                                                           (let [echo-fn (:echo component)]
                                                             {:status 200
                                                              :body (echo-fn uri)}))})
                                 [:echo])}
                   (component.utils/map->system)
                   (component/start))]

    (is (instance? org.eclipse.jetty.server.Server (-> system :web-server :jetty)))
    (testing "virtual thread pool is used"
      (is (-> system :web-server :jetty bean :threadPool bean :virtualThreadsExecutor)))

    (is (= "echo: /test/foo" (slurp (format "http://localhost:%s/test/foo" port))))
    (component/stop system)))
