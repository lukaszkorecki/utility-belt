(ns utility-belt.lifecycle-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [utility-belt.lifecycle :as lc]))

(def test-file (io/file "/tmp/ut-test-file"))

(deftest shutdown-outer
  (testing "should crate a file on exit"
    (io/delete-file test-file :silently true)
    (lc/register-shutdown-hooks! {:passing-test #(spit test-file "hi")})
    ;; so that linter stops complaining - actual test happens
    ;; outside of the test run and we assert the contents of the test file
    ;; in a 2nd step, which runs in CI
    (is true)))
