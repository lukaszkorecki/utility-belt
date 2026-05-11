(ns utility-belt.base64-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [utility-belt.base64 :as base64]
   [utility-belt.hashing :as hashing]))

(def default-charset "UTF-8")
(def ^String test-str "a test string encoded as base64")
(def ^bytes test-str-bytes (String/.getBytes test-str ^String default-charset))
(def ^String test-str-64 "YSB0ZXN0IHN0cmluZyBlbmNvZGVkIGFzIGJhc2U2NA==")
(def ^bytes test-str-64-bytes (String/.getBytes test-str-64 ^String default-charset))

(deftest factory-arrow-fns-test
  (testing "decoders"
    (is (#'base64/opts->decoder* true))
    (is (#'base64/opts->decoder* false)))
  (testing "encoders"
    (is (#'base64/opts->encoder* true true))
    (is (#'base64/opts->encoder* true false))
    (is (#'base64/opts->encoder* false true))
    (is (#'base64/opts->encoder* false false))
    (is (#'base64/opts->encoder* nil nil))))

(defn same-bytes?
  "= doesn't work with bytes but (= seq seq) compares 1-1"
  [^bytes a ^bytes b]
  (= (seq a) (seq b)))

(deftest same-bytes-test
  (testing "not same"
    (is (not (same-bytes? (String/.getBytes "abcdef1234" "UTF-8")
                          (String/.getBytes "badef1234" "UTF-8")))))
  (testing "same"
    (is (same-bytes? (String/.getBytes "abcdef1234" "UTF-8") (String/.getBytes "abcdef1234" "UTF-8")))))

(deftest encode-decode-test
  (testing "decodes bytes"
    (is (same-bytes? test-str-bytes (base64/decode test-str-64-bytes))))
  (testing "decodes strings"
    (is (same-bytes? test-str-bytes (base64/decode test-str-64))))
  (testing "decodes bytes/strings to string"
    (is (= test-str (base64/decode->str test-str-64-bytes)))
    (is (= test-str (base64/decode->str test-str-64))))
  (testing "encodes bytes"
    (is (same-bytes? test-str-64-bytes (base64/encode test-str-bytes))))
  (testing "encodes strings"
    (is (same-bytes? test-str-64-bytes (base64/encode test-str))))
  (testing "encodes bytes/strings to string"
    (is (= test-str-64 (base64/encode->str test-str)))
    (is (= test-str-64 (base64/encode->str test-str-bytes)))))



(deftest encoding-with-opts-test
  (let [some-bytes (hashing/sha256 "have some 🧀")]
    (testing "defaults"
      (is (= "bvRAHOoEG8Zk25KX24xeqpaWKJ5zoDLg7zP+UX65aqg="
             (base64/encode->str some-bytes))))

    (testing "no padding"
      (is (= "bvRAHOoEG8Zk25KX24xeqpaWKJ5zoDLg7zP+UX65aqg"
             (base64/encode->str some-bytes {:padding? false}))))

    (testing "url safe"
      (is (= "bvRAHOoEG8Zk25KX24xeqpaWKJ5zoDLg7zP-UX65aqg="
             (base64/encode->str some-bytes {:url-safe? true}))))

    (testing "url safe, no padding"
      (is (= "bvRAHOoEG8Zk25KX24xeqpaWKJ5zoDLg7zP-UX65aqg"
             (base64/encode->str some-bytes {:padding? false :url-safe? true}))))))
