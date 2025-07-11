(ns utility-belt.base64
  "Encoding and decoding to and from base64"
  (:import
   [java.nio.charset Charset StandardCharsets]
   [java.util Base64 Base64$Decoder Base64$Encoder]))

(def ^Base64$Decoder decoder (Base64/getDecoder))
(def ^Base64$Encoder encoder (Base64/getEncoder))

(def default-charset (.toString StandardCharsets/UTF_8))

(defn decode
  "Decode from bytes or string to bytes"
  ^bytes [base64]
  {:pre [(or (bytes? base64) (string? base64))]}
  (byte-array
    ;; needs to cond on type to stop reflection
   (cond
    (bytes? base64) (.decode decoder ^bytes base64)
    (string? base64) (.decode decoder ^String base64))))

(defn decode->str
  "Decode from bytes or string to string"
  (^String [base64]
   (decode->str base64 default-charset))
  (^String [base64 ^String encoding]
   {:pre [(Charset/isSupported encoding)]}
   (String. (decode base64) encoding)))

(defn- to-bytes
  "Ensure data is bytes"
  ^bytes [data ^String encoding]
  {:pre [(or (bytes? data) (string? data))
         (Charset/isSupported encoding)]}
  (cond
   (bytes? data) data
   (string? data) (.getBytes ^String data encoding)))

(defn encode
  "Encode string or bytes to bytes"
  (^bytes [data]
   (encode data default-charset))
  (^bytes [data ^String encoding]
   (let [^bytes to-encode (to-bytes data encoding)]
     (.encode encoder to-encode))))

(defn encode->str
  "Encode string or bytes to string"
  (^String [data]
   (encode->str data default-charset))
  (^String [data ^String encoding]
   (-> data (encode encoding) (String. encoding))))

