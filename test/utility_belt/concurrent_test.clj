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

(deftest scheduler-task-modes-test
  (testing "fixed rate"
    (with-open [pool (concurrent/make-scheduler-pool {:name "test" :thread-count 1})]

      (let [start-time (System/currentTimeMillis)
            state (atom [])
            task (fn []
                   (Thread/sleep 200)
                   (swap! state conj (- (System/currentTimeMillis) ^long start-time)))

            round-down (fn [ms]
                         (let [round-to 100]
                           (* (quot ms round-to) round-to)))]

        (testing "scheduling a task with fixed-rate will cause the the task fire regardless of previous execution"
          (concurrent/schedule-task pool {:handler task
                                          ;; :mode ::concurrent/fixed-rate - DEFAULT
                                          :period-ms 100})

          (is (empty? @state))

          (Thread/sleep 300)

          (is (= [200] (mapv round-down @state)))

          (Thread/sleep 350)

          (testing "after another 300ms, the task ran again, and the state is updated without waiting on previous tasks"
            (is (= [200 400 600] (mapv round-down @state))))))))

  (testing "fixed delay"
    (with-open [pool (concurrent/make-scheduler-pool {:name "test" :thread-count 1})]

      (let [start-time (System/currentTimeMillis)
            state (atom [])
            task (fn []
                   (Thread/sleep 200)
                   (swap! state conj (- (System/currentTimeMillis) ^long start-time)))

            round-down (fn [ms]
                         (let [round-to 100]
                           (* (quot ms round-to) round-to)))]

        (testing "scheduling a task with fixed-delay will cause the the task fire only after previous finished"
          (concurrent/schedule-task pool {:handler task
                                          :mode ::concurrent/fixed-delay
                                          :period-ms 100})

          (is (empty? @state))

          (Thread/sleep 300)

          (testing "even though we scheduled it to run every 100ms, it only ran once because task blocks for 200ms"
            (is (= [200] (mapv round-down @state))))

          (Thread/sleep 300)

          (testing "after another 300ms, the task ran again, and the state is updated"
            (is (= [200 500] (mapv round-down @state)))))))))
