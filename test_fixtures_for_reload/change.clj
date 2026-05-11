#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns utility-belt.fixtures.to-reload)

(defn get-version [] "v2")

(defn make-system []
  {:version (get-version)})
