(ns utility-belt.type)

(defn atom? [thing] (instance? clojure.lang.Atom thing))
