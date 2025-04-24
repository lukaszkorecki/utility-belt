(ns utility-belt.concurrent-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [utility-belt.concurrent :as concurrent]))

(deftest parallel-tasks-test
  (testing "tasks run in parallel"
    (let [task (fn task' []
                 (Thread/.getName (Thread/currentThread)))
          results (concurrent/run-tasks-in-parallel {:task-group-name "test"
                                                     :thread-count 3
                                                     :tasks [task
                                                             task
                                                             task
                                                             task
                                                             task
                                                             task]})]
      (testing "We run N tasks across M threads"
        (is (= 6 (count results)))
        (is (= #{"test-0" "test-1" "test-2"}
               (into #{} results)))))))
