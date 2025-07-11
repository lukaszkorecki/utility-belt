(ns utility-belt.component.nrepl
  "Provides `nrepl` component - requires nrepl to be provided as a dependency"
  (:require [utility-belt.component :as component]
            nrepl.server))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn create
  "Creates an nREPL server component, by default it binds to loopback address"
  [{:keys [host port]
    :or {host "0.0.0.0"}
    :as config}]
  {:pre [(pos? port)]}
  (component/map->component {:init config
                             :start (fn [this]
                                      (if (:server this)
                                        this
                                        (assoc this :server (nrepl.server/start-server :port port :bind host))))

                             :stop (fn [this]
                                     (if-let [server (:server this)]
                                       (do
                                         (nrepl.server/stop-server server)
                                         (assoc this :server nil))
                                       this))}))
