(ns utility-belt.component.jetty
  "Provides a component for running a Jetty server with a Ring handler. Requires `ring/ring-jetty-adapter` dependency."
  (:require [ring.adapter.jetty :as jetty]
            [utility-belt.component :as component])
  (:import
   [org.eclipse.jetty.server Server]))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn create
  "Creates a component that runs a Jetty server with the provided Ring handler.
   The handler should be a function that takes a request map and returns a response map.
  Dependencies provided to the Jetty component will be available in the request map under the `:component` key.

  To configure underlying Jetty server, pass a `:config` map with options like `:port` and `:host`
  See doc string for `ring.adapter.jetty/run-jetty`
  Note that `:join?` is set to `false` by default, and cannot be overridden."
  [{:keys [config handler]
    :or {config {:port 3000 :host "0.0.0.0"}}}]
  {:pre [(fn? handler)
         (pos? (-> config :port))]}
  (component/map->component
   {:init {:config (merge config {:join? false})}
    :start (fn [this]
             (if (:jetty this)
               this
               (let [deps (dissoc this :config)
                     wrapped-handler (fn with-deps' [request]
                                       (handler (assoc request :component deps)))]
                 (assoc this :jetty (jetty/run-jetty wrapped-handler
                                                     (:config this))))))
    :stop (fn [this]
            (if-let [server (:jetty this)]
              (do
                (Server/.stop server)
                (assoc this :jetty nil))
              this))}))
