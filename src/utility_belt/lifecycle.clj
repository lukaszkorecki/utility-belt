(ns utility-belt.lifecycle
  "Tools for managing application lifecycle"
  (:require
   [clojure.tools.logging :as log]))

(set! *warn-on-reflection* true)

(defonce runtime (Runtime/getRuntime))

(defonce registerd-hooks (atom {}))

(defn add-shutdown-hook
  "Register a shutdown hook with the JVM. The hook identified by a keyword will  be executed when the JVM shuts down."
  [name hook-fn]
  {:pre [(keyword? name)
         (fn? hook-fn)]}
  (log/debugf "registered hook '%s'" name)
  (let [hook* (Thread. ^Runnable
                       (fn hook' []
                         (try
                           (log/debugf "executing shutdown hook '%s'" name)
                           (hook-fn)
                           (catch Throwable err
                             (log/errorf err "failed to execute shutdown hook '%s'" name)))))]
    (Runtime/.addShutdownHook runtime hook*)
    (swap! registerd-hooks assoc name hook*)))

(add-shutdown-hook ::shutdown-agents shutdown-agents)

(defn register-shutdown-hooks!
  "Install the shutdown handler, which will run any registered shutdown hooks.
  If you don't care about the order of execution, pass a map with hooks.
  Otherwise, pass a vector of tuples with the name and the function to run - hooks are going to run in reverse order"
  [hooks]
  {:pre [(seq hooks)
         (every? (fn [[name hook-fn]]
                   (and (keyword? name)
                        (fn? hook-fn)))
                 hooks)]}
  (mapv (fn [[name hook-fn]]
          (add-shutdown-hook name hook-fn))
        hooks))

(defn remove-shutdown-hooks!
  "Clear all registered shutdown hooks"
  []
  (doseq [[name hook*] @registerd-hooks]
    (log/debugf "removing shutdown hook '%s'" name)
    (try
      (Runtime/.removeShutdownHook runtime hook*)
      (catch Exception e
        (log/errorf e "failed to remove shutdown hook '%s'" name))))

  (reset! registerd-hooks {}))
