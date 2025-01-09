(ns utility-belt.files
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [cheshire.core :as json]
            [clojure.data.csv :as csv]))


(defn load-edn
  "Given a resource name or file path, reads the EDN file and returns a Clojure data structure."
  [resource-name-or-path]
  (let [resource (or (io/resource resource-name-or-path)
                     resource-name-or-path)]
    (with-open [reader (java.io.PushbackReader. (io/reader resource))]
      (edn/read reader))))

(defn load-csv
  "Given a resource name or file path, reads the CSV file and returns a sequence of rows."
  [resource-name-or-path]
  (let [resource (or (io/resource resource-name-or-path)
                     resource-name-or-path)]
    (with-open [reader (io/reader resource)]
      (doall (csv/read-csv reader)))))

(defn load-json
  "Given a resource name or file path, reads the JSON file and returns a Clojure data structure.
   Accepts an optional argument `keyword?` to convert keys to keywords."
  [resource-name-or-path & [keyword?]]
  (let [resource (or (io/resource resource-name-or-path)
                     resource-name-or-path)]
    (with-open [reader (io/reader resource)]
      (json/parse-stream reader (or keyword? false)))))

(defn load-txt
  "Given a resource name or file path, reads the text file and returns a string."
  [resource-name-or-path]
  (let [resource (or (io/resource resource-name-or-path)
                     resource-name-or-path)]
    (with-open [reader (io/reader resource)]
      (slurp reader))))
