(ns utility-belt.sanitization-test
  (:require
    [clojure.test :refer [is deftest testing]]
    [utility-belt.sanitize :as sanitization]))

(deftest remove-spaces-test
  (testing "it removes all spaces"
    (is (= "itremovedallspaces"
           (sanitization/remove-spaces "it removed all spaces")))
    (is (= "itremovedallspacescool"
           (sanitization/remove-spaces "  \n   it  \r removed a l l spaces        cool")))
    (is (= "foo@cool.com"
           (sanitization/remove-spaces "foo@\ncool.com"))))
  (testing "it keeps original string if no spaces"
    (is (= "foo@cool.com" (sanitization/remove-spaces "foo@cool.com")))))

(def expected-emails
  [["test@example.com" "test@example.com"]
   ["    test@ e x a m p l e   . com  " "test@example.com"]
   ["@example.com" nil]
   ["ok@example." nil]
   ["foo" nil]
   ["ok.lolling@then.example.com" "ok.lolling@then.example.com"]
   ["ok_lolling-1@then.example" "ok_lolling-1@then.example"]
   ["ok_lolling-1+cool-label_ok@then" nil]
   ["ok_lolling-1+cool-label_ok@then.example.com" "ok_lolling-1+cool-label_ok@then.example.com"]])

(deftest email-test
  (testing "sanitizes email"
    (mapv
      (fn [[to-clean res]]
        (is
          (= res (sanitization/email to-clean))
          (str to-clean " clean should be " res)))
      expected-emails)))
