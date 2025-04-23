(ns utility-belt.concurrent
  (:import [java.util.concurrent
            Executors
            Executor
            ThreadPoolExecutor
            TimeUnit
            ScheduledThreadPoolExecutor
            ThreadFactory
            ThreadPoolExecutor$AbortPolicy
            TimeUnit]
           [java.util.concurrent.atomic AtomicLong]))

(set! *warn-on-reflection* true)

(defn- thread-factory [{:keys [name daemon?]}]
  (let [thread-id (AtomicLong. 0)]
    (reify ThreadFactory
      (^Thread newThread [_ ^Runnable r]
       (doto (Thread. r)
         (.setDaemon daemon?)
         (.setName (str name "-" (AtomicLong/.getAndIncrement thread-id))))))))

(defn make-task-pool
  "Make a new ThreadPoolExecutor."
  ^ThreadPoolExecutor
  [{:keys [thread-count pool-name]}]
  (Executors/newFixedThreadPool ^long thread-count
                                ^ThreadFactory (thread-factory {:name pool-name :daemon? false})))

(defn shutdown-task-pool
  ([^ThreadPoolExecutor pool]
   (shutdown-task-pool pool {:max-wait-time-ms 1000}))
  ([^ThreadPoolExecutor pool {:keys [max-wait-time-ms]}]
   {:pre [(instance? ThreadPoolExecutor pool)
          (pos? max-wait-time-ms)]}
   (try
     (.shutdown pool)
     (.awaitTermination pool max-wait-time-ms TimeUnit/MILLISECONDS)
     (catch InterruptedException _
       (.shutdownNow pool)
       (.interrupt (Thread/currentThread))))))

(defn run-tasks [^ThreadPoolExecutor exec-pool {:keys [tasks max-wait-time-ms]}]
  (->> tasks
       (mapv (fn submit' [^Callable task] (.submit exec-pool task)))
       (mapv (fn get' [fut] (deref fut max-wait-time-ms :task-timeout)))))

(defn run-tasks-in-parallel
  "`pmap` replacement, but allows you to schedule tasks (functions) in a controlled threadpool.
  Note if that one tasks throws an exception - all other tasks will be cancelled and the exception will be rethrown.
  "
  [{:keys [task-group-name
           tasks
           thread-count
           max-wait-time-ms]
    :or {task-group-name "tasks"
         max-wait-time-ms 1000
         thread-count 2}}]
  {:pre [(every? fn? tasks)
         (pos? thread-count)
         (> max-wait-time-ms 50)]}
  (let [exec (make-task-pool {:thread-count thread-count
                              :pool-name task-group-name})
        results (run-tasks exec {:tasks tasks
                                 :max-wait-time-ms max-wait-time-ms})]
    (shutdown-task-pool exec {:max-wait-time-ms max-wait-time-ms})
    results))

;; Scheduler

(defn make-scheduler-pool [{:keys [name thread-count] :or {thread-count 2}}]
  (ScheduledThreadPoolExecutor. ^long thread-count
                                ^ThreadFactory (thread-factory {:name name :daemon? true})
                                ;; TODO: add logging variant of this:
                                (ThreadPoolExecutor$AbortPolicy.)))

(defn scheduler-pool? [thing]
  (instance? ScheduledThreadPoolExecutor thing))

(defn shutdown-scheduler-pool [^ScheduledThreadPoolExecutor pool]
  (try
    (.shutdown pool)
    (.awaitTermination pool 10 java.util.concurrent.TimeUnit/SECONDS)
    (catch InterruptedException _
      (.shutdownNow pool)
      (.interrupt (Thread/currentThread)))))

(defn schedule-task
  "Schedule a recurring task running (very roughly) every `period-ms` milliseconds.
  and executing the `handler` function."
  [^ScheduledThreadPoolExecutor pool {:keys [handler period-ms delay-ms]}]
  {:pre [(fn? handler)
         (nat-int? period-ms)
         (not (neg? delay-ms))]}
  (ScheduledThreadPoolExecutor/.scheduleAtFixedRate pool
                                                    ^Runnable handler
                                                    ^long delay-ms
                                                    ^long period-ms
                                                    TimeUnit/MILLISECONDS))
