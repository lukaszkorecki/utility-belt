(ns utility-belt.resources-test
  (:require [clojure.test :refer [deftest is testing]]
            [utility-belt.resources :as files]))

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

(deftest load-plain-text-test
  (testing "loads text file correctly"
    (let [data (files/load-plain-text "test/utility_belt/fixtures/some.txt")]
      (is (= "Alice Smith is a 28 year old whose favorite color is blue and her account is active.\n"
             data)))))


