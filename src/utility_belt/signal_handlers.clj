(ns utility-belt.signal-handlers
  (:import [sun.misc Signal SignalHandler]))

(deftype ExitZeroHandler []
  SignalHandler
  (handle [_this _signal] (System/exit 0)))

(def exit-zero-handler (ExitZeroHandler.))

(defn exit-zero-on-signal
  [^String signal]
  (Signal/handle (Signal. signal) exit-zero-handler))

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(defn use-exit-code-zero-for-graceful-exit!
  "By default, the JVM handles INT and TERM signals by return status
  codes by triggering graceful shutdown and returning a status code
  based on the signal that initiated it (128 + unix value of the
  signal). This is great, except that the exit code should be 0 if, in
  fact things did exit gracefully."
  []
  (exit-zero-on-signal "TERM")
  (exit-zero-on-signal "INT"))
