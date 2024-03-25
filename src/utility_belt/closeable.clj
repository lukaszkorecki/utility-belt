(ns utility-belt.closeable
  "Makes things closeable so they play nice with `with-open`."
  (:import [java.io Closeable]
           [clojure.lang IFn]))

(defrecord CloseableFn [f close-fn]
  Closeable
  (close [this] (if close-fn
                  (close-fn)
                  this))
  IFn
  (invoke [_this] (f))
  (invoke [_this a] (f a))
  (invoke [_this a b] (f a b))
  (invoke [_this a b c] (f a b c))
  (invoke [_this a b c d] (f a b c d))
  (invoke [_this a b c d e] (f a b c d e)))

(defn closeable-fn [f]
  (->CloseableFn f nil))

(defn make-component
  "Not quite Stuart Sierra's component, but close enough.
  Use it with `with-open`. A
  map has to provide a start and stop function accepting the map
  itself as an argument.
  Returns a closeable function"
  [{:keys [start stop] :as _map}]
    ;; return a closeable map
  (->CloseableFn start stop))
