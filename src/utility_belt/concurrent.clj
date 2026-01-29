(ns utility-belt.concurrent
  "A set of helpers for working with Java's concurrency constructs"
  (:import [java.lang Thread$Builder$OfVirtual]
           [java.util.concurrent
            Executors
            ExecutorService
            TimeUnit
            ScheduledThreadPoolExecutor
            ThreadFactory
            ThreadPoolExecutor$AbortPolicy
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
  [{:keys [name thread-count] :or {thread-count 2}}]
  (ScheduledThreadPoolExecutor. ^long thread-count
                                ^ThreadFactory (thread-factory {:name name :daemon? true})
                                ;; TODO: add logging variant of this:
                                (ThreadPoolExecutor$AbortPolicy.)))

(defn scheduler-pool?
  "Check if a given thing is a scheduler pool."
  [thing]
  (instance? ScheduledThreadPoolExecutor thing))

(defn shutdown-scheduler-pool
  "Shutdown a scheduler pool, waiting for tasks to complete."
  [^ScheduledThreadPoolExecutor pool]
  (try
    (.shutdown pool)
    (.awaitTermination pool 10 java.util.concurrent.TimeUnit/SECONDS)
    (catch InterruptedException _
      (.shutdownNow pool)
      (.interrupt (Thread/currentThread)))))

(def ^:private modes #{::fixed-rate ::fixed-delay})

(defn schedule-task
  "Schedule a recurring task running (very roughly) every `period-ms` milliseconds.
  and executing the `handler` function."
  [^ScheduledThreadPoolExecutor pool {:keys [handler period-ms delay-ms mode]
                                      :or {delay-ms 0
                                           mode ::fixed-rate}}]
  {:pre [(fn? handler)
         (nat-int? period-ms)
         (not (neg? delay-ms))
         (modes mode)]}

  (cond
    (= mode ::fixed-rate) (ScheduledThreadPoolExecutor/.scheduleAtFixedRate pool
                                                                            ^Runnable handler
                                                                            ^long delay-ms
                                                                            ^long period-ms
                                                                            TimeUnit/MILLISECONDS)

    (= mode ::fixed-delay) (ScheduledThreadPoolExecutor/.scheduleWithFixedDelay pool
                                                                                ^Runnable handler
                                                                                ^long delay-ms
                                                                                ^long period-ms
                                                                                TimeUnit/MILLISECONDS)

    :else (throw (ex-info "Invalid mode for scheduling task"
                          {:mode mode :valid-modes modes}))))
