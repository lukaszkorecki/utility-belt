(ns utility-belt.component.nrepl
  (:require [utility-belt.component :as component]
            nrepl.server))

(defn create [{:keys [host port]
               :or {host "0.0.0.0"}
               :as config}]

  (component/map->component {:init-val config
                             :start #(assoc % :server (nrepl.server/start-server :port port
                                                                                 :host host))

                             :stop (fn [this]
                                     (if-let [server (:server this)]
                                       (do
                                         (nrepl.server/stop-server server)
                                         (assoc this :server nil))
                                       this))}))
