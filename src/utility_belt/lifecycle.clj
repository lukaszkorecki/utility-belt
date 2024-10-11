(ns utility-belt.lifecycle
  "Tools for managing application lifecycle")

;; FIXME: this should use a logging backend (c.tools.logging), instead of println!
(set! *warn-on-reflection* true)

(def ^{:private true :doc "Shutdown hook store"}
  hooks
  (atom [[::shutdown-agents shutdown-agents]]))

(defn add-shutdown-hook
  "Register a function to run when the application *gracefully* shuts down.
  Useful for stopping the Component system or other resources that have a life cycle."
  [name hook-fn]
  (println (format "registered hook '%s'" name))
  (swap! hooks conj [name hook-fn]))

(defn run-registered-hooks
  []
  (mapv (fn [[name hook-fn]]
          (try
            (println (format "running shutdown hook '%s'" name))
            (hook-fn)
            (catch Exception err
              (println (format "shutdown hook '%s' failed, %s" name err)))))
        @hooks))

(defn register-shutdown-hooks!
  "Install the shutdown handler, which will run any registered shutdown hooks."
  [hooks]
  {:pre [(seq hooks)
         (every? (fn [[name hook-fn]]
                   (and (keyword? name)
                        (fn? hook-fn)))
                 hooks)]}
  (mapv (fn [[name hook-fn]]
          (add-shutdown-hook name hook-fn))
        hooks)
  (Runtime/.addShutdownHook (Runtime/getRuntime)
                            (Thread. ^Runnable run-registered-hooks)))
