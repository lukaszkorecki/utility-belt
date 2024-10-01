(ns utility-belt.base64-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [utility-belt.base64 :as base64]))

(def default-charset "UTF-8")
(def ^String test-str "a test string encoded as base64")
(def ^bytes test-str-bytes (.getBytes test-str default-charset))
(def ^String test-str-64 "YSB0ZXN0IHN0cmluZyBlbmNvZGVkIGFzIGJhc2U2NA==")
(def ^bytes test-str-64-bytes (.getBytes test-str-64 default-charset))

(defn same-bytes?
  "= doesn't work with bytes but (= seq seq) compares 1-1"
  [^bytes a ^bytes b]
  (= (seq a) (seq b)))

(deftest same-bytes-test
  (testing "not same"
    (is (not (same-bytes? (.getBytes "a" "UTF-8") (.getBytes "b" "UTF-8")))))
  (testing "same"
    (is (same-bytes? (.getBytes "a" "UTF-8") (.getBytes "a" "UTF-8")))))

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
