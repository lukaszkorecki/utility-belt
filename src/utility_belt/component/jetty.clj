(ns utility-belt.component.jetty
  "Provides a component for running a Jetty server with a Ring handler. Requires `ring/ring-jetty-adapter` dependency."
  (:require [ring.adapter.jetty :as jetty]
            [utility-belt.component :as component])
  (:import [java.util.concurrent Executors]
           [org.eclipse.jetty.util.thread QueuedThreadPool]
           [org.eclipse.jetty.server Server]))

(set! *warn-on-reflection* true)

(defn- make-virtual-thread-pool []
  (doto (QueuedThreadPool.)
    ;; as per Jetty docs, do not reserve any threads for internal tasks when using
    ;; virtual threads, since that makes no difference due to virtual thread starting instantly
    ;; see: https://jetty.org/docs/jetty/12.1/programming-guide/arch/threads.html
    (.setReservedThreads 0)
    (.setVirtualThreadsExecutor (Executors/newVirtualThreadPerTaskExecutor))))

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
  {:pre [(or (fn? handler) (var? handler))
         (pos? (-> config :port))]}
  (component/map->component
   {:init {:config (merge config {:join? false})}
    :start (fn [this]
             (if (:jetty this)
               this
               (let [deps (dissoc this :config)
                     wrapped-handler (fn with-deps' [request]
                                       (handler (assoc request :component deps)))
                     jetty-config (cond-> (:config this)
                                    (:virtual-threads? (:config this)) (assoc :thread-pool (make-virtual-thread-pool))
                                    :always (dissoc :virtual-threads?))]
                 (assoc this :jetty (jetty/run-jetty wrapped-handler jetty-config)))))
    :stop (fn [this]
            (if-let [server (:jetty this)]
              (do
                (Server/.stop server)
                (assoc this :jetty nil))
              this))}))
