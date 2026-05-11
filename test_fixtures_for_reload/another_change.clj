#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns utility-belt.fixtures.to-reload)

(def version "v3")

(defn make-system []
  {:version version})
