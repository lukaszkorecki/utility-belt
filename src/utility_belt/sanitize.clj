(ns utility-belt.sanitize
  (:require
    [clojure.string]
    [utility-belt.validation]))

(defn remove-spaces
  "Remove all spaces from a string"
  [st]
  {:pre [(string? st)]}
  (clojure.string/replace st #"\s" ""))

(defn email
  "Clean email string and return allowed email using validation/pattern"
  [email]
  {:pre [(string? email)]}
  (when-let [email (-> email
                       (clojure.string/lower-case)
                       (remove-spaces))]
    (re-find utility-belt.validation/email-pattern email)))
