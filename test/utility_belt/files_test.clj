(ns utility-belt.files-test
  (:require [clojure.test :refer [deftest is testing]]
            [utility-belt.files :as files]))

(deftest load-edn-test
  (testing "loads and parses EDN file correctly"
    (let [data (files/load-edn "test/utility_belt/fixtures/some.edn")]
      (is (= {:id "550e8400-e29b-41d4-a716-446655440000"
              :name "Alice Smith"
              :age 28
              :is_active true
              :favorite_color "blue"}
             data)))))

(deftest load-json-test
  (testing "loads and parses JSON file with string keys"
    (let [data (files/load-json "test/utility_belt/fixtures/some.json")]
      (is (= {"id" "550e8400-e29b-41d4-a716-446655440000"
              "name" "Alice Smith"
              "age" 28
              "is_active" true
              "favorite_color" "blue"}
             data))))

  (testing "loads and parses JSON file with keyword keys"
    (let [data (files/load-json "test/utility_belt/fixtures/some.json" true)]
      (is (= {:id "550e8400-e29b-41d4-a716-446655440000"
              :name "Alice Smith"
              :age 28
              :is_active true
              :favorite_color "blue"}
             data)))))

(deftest load-csv-test
  (testing "loads and parses CSV file correctly"
    (let [data (files/load-csv "test/utility_belt/fixtures/some.csv")]
      (is (= [["id" "name" "age" "is_active" "favorite_color"]
              ["550e8400-e29b-41d4-a716-446655440000" "Alice Smith" "28" "true" "blue"]
              ["f47ac10b-58cc-4372-a567-0e02b2c3d479" "Bob Johnson" "35" "false" "green"]]
             data)))))

(deftest load-txt-test
  (testing "loads text file correctly"
    (let [data (files/load-txt "test/utility_belt/fixtures/some.txt")]
      (is (= "Alice Smith is a 28 year old whose favorite color is blue and her account is active.\n"
             data)))))


