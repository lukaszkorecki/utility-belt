(ns utility-belt.type
  "A collection of type-checking predicates.")

(defn atom?
  "Check if a given thing is an atom."
  [thing] (instance? clojure.lang.Atom thing))
