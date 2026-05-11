(ns utility-belt.hashing
  (:require [utility-belt.base64 :refer [->bytes]])
 (:import
   [java.security MessageDigest]))



(defn sha256 ^bytes [thing]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (MessageDigest/.update digest ^bytes (->bytes thing))
    (MessageDigest/.digest digest)))

(defn md5 ^bytes [thing]
  (let [digest (MessageDigest/getInstance "MD5")]
    (MessageDigest/.update digest ^bytes (->bytes thing))
    (MessageDigest/.digest digest)))
