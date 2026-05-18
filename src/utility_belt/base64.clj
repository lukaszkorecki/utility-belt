(ns utility-belt.base64
  "Encoding and decoding to and from base64"
  (:import
   [java.nio.charset Charset StandardCharsets]
   [java.util Base64 Base64$Decoder Base64$Encoder]))

(set! *warn-on-reflection* true)
(def default-charset (.toString StandardCharsets/UTF_8))

(defn ->bytes
  "Ensure data is bytes"
  ^bytes
  ([data]
   (->bytes data default-charset))
  ([data ^String encoding]
   {:pre [(or (bytes? data) (string? data))
          (Charset/isSupported encoding)]}
   (cond
     (bytes? data) data
     (string? data) (.getBytes ^String data encoding))))

(defn bytes->str
  ^String
  ([^bytes d]
   (bytes->str d default-charset))
  ([^bytes d ^String charset]
   (String. d charset)))

;; DECODING

(def ^Base64$Decoder decoder (Base64/getDecoder))
(def ^Base64$Decoder decoder-url-safe (Base64/getUrlDecoder))

(defn ^:private opts->decoder* ^Base64$Encoder [url-safe?]
  (if url-safe?
    decoder-url-safe
    decoder))

(def ^:private opts->decoder (memoize opts->decoder*))

(defn decode
  "Decode from bytes or string to bytes"
  ^bytes [base64 & {:keys [url-safe?]
                    :or {url-safe? false}}]
  {:pre [(or (bytes? base64) (string? base64))]}
  (byte-array
   ;; needs to cond on type to stop reflection
   (cond
     (bytes? base64) (Base64$Decoder/.decode (opts->decoder url-safe?) ^bytes base64)
     (string? base64) (Base64$Decoder/.decode (opts->decoder url-safe?) ^String base64))))

(defn decode->str
  "Decode from bytes or string to string"
  (^String [base64]
   (decode->str base64 {:encoding default-charset}))
  (^String [base64 {:keys [url-safe? encoding]}]
   {:pre [(Charset/isSupported encoding)]}
   (bytes->str (decode base64 {:url-safe? url-safe?}) encoding)))

;; ENCODING

(def ^Base64$Encoder encoder (Base64/getEncoder))
(def ^Base64$Encoder encoder-without-padding (Base64$Encoder/.withoutPadding (Base64/getEncoder)))
(def ^Base64$Encoder encoder-url-safe (Base64/getUrlEncoder))
(def ^Base64$Encoder encoder-without-padding-url-safe (Base64$Encoder/.withoutPadding (Base64/getUrlEncoder)))

(defn ^:private opts->encoder* ^Base64$Encoder [padding? url-safe?]
  (cond
    (and padding? url-safe?) encoder-url-safe
    (and padding? (not url-safe?)) encoder
    (and (not padding?) url-safe?) encoder-without-padding-url-safe
    (and (not padding?) (not url-safe?)) encoder-without-padding))

(def ^:private opts->encoder (memoize opts->encoder*))

(defn encode
  "Encode string or bytes to bytes
   Accepts an optional map of:
   - :encoding - charset name to use, defaults to UTF-8
   - :padding?  - encode with padding (default) or not
   - :url-safe? - encode url safe or not (default)"
  ^bytes [data & {:keys [encoding
                         url-safe?
                         padding?]
                  :or {encoding default-charset
                       url-safe? false
                       padding? true}}]
  (let [^bytes to-encode (->bytes data encoding)
        encoder (opts->encoder padding? url-safe?)]
    (Base64$Encoder/.encode encoder to-encode)))

(defn encode->str
  "Encode string or bytes to string"
  (^String [data]
   (encode->str data {}))
  (^String [data {:keys [encoding]
                  :or {encoding default-charset}
                  :as opts}]
   (bytes->str (encode data opts) ^String encoding)))
