(ns utility-belt.concurrent
  "A set of helpers for working with Java's concurrency constructs"
  (:import
   ;; see more here why this is used: https://javadoc.io/doc/io.timeandspace/cron-scheduler/latest/io/timeandspace/cronscheduler/CronScheduler.html
   [io.timeandspace.cronscheduler CronScheduler CronSchedulerBuilder CronTask OneShotTasksShutdownPolicy]
   [java.lang Thread$Builder$OfVirtual]
   [java.util.concurrent
    Executors
    ExecutorService
    TimeUnit
    ThreadFactory
    TimeUnit]
   [java.util.concurrent.atomic AtomicLong]))

(set! *warn-on-reflection* true)

#_{:clojure-lsp/ignore [:clojure-lsp/unused-public-var]}
(def num-cpus
  "Number of CPUs available to the JVM, used to determine the number of threads in a thread pool."
  (max 1 (.. Runtime getRuntime availableProcessors)))

(defn- thread-factory
  "Create a new thread factory for use with thread pools"
  [{:keys [name daemon?]}]
  (let [thread-id (AtomicLong. 0)]
    (reify ThreadFactory
      (^Thread newThread [_ ^Runnable r]
        (doto (Thread. r)
          (.setDaemon daemon?)
          (.setName (str name "-" (AtomicLong/.getAndIncrement thread-id))))))))

(defn- virtual-thread-factory
  "Create a new thread factory for virtual threads"
  ^ThreadFactory
  [{:keys [name]}]
  (-> (Thread/ofVirtual)
      ^Thread$Builder$OfVirtual (.name ^String name)
      (.factory)))

(defn make-task-pool
  "Make a new ThreadPoolExecutor that will execute tasks in parallel.

  Options:
  - `:thread-count` - number of threads in the pool (ignored when using virtual threads)
  - `:pool-name` - name prefix for threads
  - `:virtual-threads?` - if true, use virtual threads instead of a fixed thread pool"
  ^ExecutorService
  [{:keys [thread-count pool-name virtual-threads?]}]
  (if virtual-threads?
    (Executors/newThreadPerTaskExecutor
     (virtual-thread-factory {:name (str pool-name "-")}))
    (Executors/newFixedThreadPool ^long thread-count
                                  ^ThreadFactory (thread-factory {:name pool-name :daemon? false}))))

(defn shutdown-task-pool
  "Shutdown a task pool, waiting for tasks to complete."
  ([^ExecutorService pool]
   (shutdown-task-pool pool {:max-wait-time-ms 1000}))
  ([^ExecutorService pool {:keys [max-wait-time-ms]}]
   {:pre [(instance? ExecutorService pool)
          (pos? max-wait-time-ms)]}
   (try
     (.shutdown pool)
     (.awaitTermination pool max-wait-time-ms TimeUnit/MILLISECONDS)
     (catch InterruptedException _
       (.shutdownNow pool)
       (.interrupt (Thread/currentThread))))))

(defn run-tasks
  "Run a collection of tasks in a thread pool, waiting for them to complete."
  [^ExecutorService exec-pool {:keys [tasks max-wait-time-ms]}]
  (->> tasks
       (mapv (fn submit' [^Callable task] (.submit exec-pool task)))
       (mapv (fn get' [fut] (deref fut max-wait-time-ms :task-timeout)))))

(defn run-tasks-in-parallel
  "`pmap` replacement, but allows you to schedule tasks (functions) in a controlled threadpool.
  Note if that one tasks throws an exception - all other tasks will be cancelled and the exception will be rethrown.

  Options:
  - `:task-group-name` - name prefix for threads (default: \"tasks\")
  - `:tasks` - collection of functions to execute
  - `:thread-count` - number of threads in the pool (default: 2, ignored when using virtual threads)
  - `:max-wait-time-ms` - maximum time to wait for tasks to complete (default: 1000)
  - `:virtual-threads?` - if true, use virtual threads instead of a fixed thread pool (default: false)
  "
  [{:keys [task-group-name
           tasks
           thread-count
           max-wait-time-ms
           virtual-threads?]
    :or {task-group-name "tasks"
         max-wait-time-ms 1000
         thread-count 2
         virtual-threads? false}}]
  {:pre [(every? fn? tasks)
         (or virtual-threads? (pos? thread-count))
         (> max-wait-time-ms 50)]}
  (let [exec (make-task-pool {:thread-count thread-count
                              :pool-name task-group-name
                              :virtual-threads? virtual-threads?})
        results (run-tasks exec {:tasks tasks
                                 :max-wait-time-ms max-wait-time-ms})]
    (shutdown-task-pool exec {:max-wait-time-ms max-wait-time-ms})
    results))

;; Scheduler

(defn make-scheduler-pool
  "Create a new scheduler pool for running recurring tasks."
  [{:keys [name]}]
  ;; resync wall clock every 5 minutes - which is recommended for server side use as per javadoc
  (let [scheduler-builder ^CronSchedulerBuilder (CronScheduler/newBuilder (java.time.Duration/ofMinutes 5))
        scheduler (-> scheduler-builder
                      (.setThreadFactory (thread-factory {:name (str name "-scheduler")
                                                          :daemon? true}))
                      (.build))]
    (CronScheduler/.prestartThread scheduler)
    scheduler))

(defn scheduler-pool?
  "Check if a given thing is a scheduler pool."
  [thing]
  (instance? CronScheduler thing))

(def shutdown-policy (OneShotTasksShutdownPolicy/valueOf "DISCARD_DELAYED"))

(defn shutdown-scheduler-pool
  "Shutdown a scheduler pool, waiting for tasks to complete."
  [^CronScheduler pool]
  (try
    (.shutdown pool ^OneShotTasksShutdownPolicy shutdown-policy)
    (when-not (.awaitTermination pool 10 TimeUnit/SECONDS)
      (.shutdownNow pool))
    ;; handle the case where the thread is interrupted  - we can't block for too long
    (catch InterruptedException _
      (.shutdownNow pool)
      (.interrupt (Thread/currentThread)))))

(defn- fn->cron-task
  "Turns a Clojure function into a CronTask.
  While also protecting against clock drift by checking the scheduled run time
  See here: https://javadoc.io/doc/io.timeandspace/cron-scheduler/latest/io/timeandspace/cronscheduler/CronScheduler.html
  "
  [a-fn]
  (reify CronTask
    (run [_this _scheduled-run-time-ms]
      (a-fn))))

(defn schedule-task
  "Schedule a recurring task running (very roughly) every `period-ms` milliseconds.
  and executing the `handler` function.
  Underlying scheduler class will also attempt to compensate for clock drift, unlike Java's ScheduledThreadPoolExecutor, so your tasks will run more reliably at the expected intervals."
  [^CronScheduler pool {:keys [handler period-ms delay-ms mode]
                        :or {delay-ms 0}}]
  {:pre [(fn? handler)
         (nat-int? period-ms)
         (not (neg? delay-ms))]}

  ;; NOTE: this is to catch code which uses different scheduler mmodes - because we're now using a different
  ;;       scheduler implementation, we only support fixed-rate scheduling,
  ;;       and fixed-delay scheduling is not supported, so we want to throw if we see that mode being used.
  (when mode
    (throw (ex-info "Only fixed-rate scheduling is supported" {:mode mode})))
  (CronScheduler/.scheduleAtFixedRate pool
                                      ^long delay-ms
                                      ^long period-ms
                                      TimeUnit/MILLISECONDS
                                      ^CronTask (fn->cron-task handler)))
