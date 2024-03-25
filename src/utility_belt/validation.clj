(ns utility-belt.validation)


;; Kudos to helpful people on the internets, including SO and IRC
(def email-pattern-string
  #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

(def email-pattern (re-pattern email-pattern-string))


(defn valid-email?
  [email]
  (re-matches email-pattern email))
