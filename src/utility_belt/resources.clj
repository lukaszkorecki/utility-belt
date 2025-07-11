(ns utility-belt.resources
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cheshire.core :as json]))

(defn load-edn
  "Given a resource name, reads the EDN file and returns a Clojure data structure."
  [resource-name]
  (-> (io/resource resource-name)
      (slurp)
      (edn/read-string)))

(defn load-json
  "Given a resource name, reads the JSON file and returns a Clojure data structure.
   Accepts an optional argument `keyword?` to convert keys to keywords."
  [resource-name & [keyword?]]
  (-> (io/resource resource-name)
      (slurp)
      (json/parse-string (or keyword? false))))

(defn load-plain-text
  "Given a resource name, reads the text file and returns a string."
  [resource-name]
  (-> (io/resource resource-name)
      (slurp)))
