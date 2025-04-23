(ns utility-belt.concurrent
  (:import [java.util.concurrent
            Executors
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
  (let [exec ^ThreadPoolExecutor (Executors/newFixedThreadPool ^long thread-count
                                                               ^ThreadFactory (thread-factory {:name task-group-name :daemon? false}))
        futures (mapv #(.submit exec ^Callable %) tasks)
        results (mapv (fn [fut] (deref fut max-wait-time-ms :task-timeout)) futures)]
    (.shutdown exec)
    (.awaitTermination exec ^long (* 2 max-wait-time-ms) TimeUnit/MILLISECONDS)
    results))

(defn make-scheduler-pool [{:keys [name thread-count] :or {thread-count 2}}]
  (ScheduledThreadPoolExecutor. ^long thread-count
                                ^ThreadFactory (thread-factory {:name name :daemon? true})
                                ;; TODO: add logging variant of this:
                                (ThreadPoolExecutor$AbortPolicy.)))

(defn scheduler-pool? [thing]
  (instance? ScheduledThreadPoolExecutor thing))

(defn stop-scheduler-pool [^ScheduledThreadPoolExecutor pool]
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
