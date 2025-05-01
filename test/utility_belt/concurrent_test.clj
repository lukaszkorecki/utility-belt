(ns utility-belt.concurrent-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [clojure.tools.logging :as log]
   [utility-belt.compile :as compile]
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

(compile/compile-if concurrent/virtual-threads-available?
                    (defn get-executor []
                      (log/info "using virtual threads")
                      (java.util.concurrent.Executors/newVirtualThreadPerTaskExecutor))

                    (defn get-executor []
                      (log/info "using fixed thread pool")
                      (java.util.concurrent.Executors/newFixedThreadPool 3)))

(deftest custom-executor-test
  (let [counter (atom 0)
        task (fn task' []
               (swap! counter inc))
        exec (get-executor)
        results (concurrent/run-tasks exec
                                      {:tasks [task
                                               task
                                               task
                                               task
                                               task
                                               task]
                                       :max-wait-time-ms 500})]

    (concurrent/shutdown-task-pool exec)

    (is (= [1 2 3 4 5 6]
           (sort results)))))
