(ns utility-belt.component.scheduler
  "Simple wrapper around `ScheduledThreadPoolExecutor` nothing fancy"
  (:require
   [clojure.tools.logging :as log]
   [utility-belt.concurrent :as concurrent]
   [utility-belt.component :as component.util]))

(defn create-pool
  "Creates a scheduler pool for scheduled tasks to register with"
  [{:keys [name] :as opts}]
  {:pre [(not-empty name)]}
  (component.util/map->component
    {:init opts
     :start (fn [this]
              (if (:executor this)
                this
                (do
                  (log/infof "Creating scheduler pool %s" name)
                  (assoc this :executor (concurrent/make-scheduler-pool opts)))))

     :stop (fn [this]
             (if (:executor this)
               (do
                 (log/warnf "stopping %s scheduler pool" name)
                 (concurrent/stop-scheduler-pool (:executor this))
                 (assoc this :executor nil))
               this))}))

(defn create-task
  "Creates a scheduled task component, NOTE: it requires a :scheduler dependency to be present"
  [{:keys [name period-ms delay-ms handler]
    :or {delay-ms 0}
    :as opts}]
  {:pre [(not-empty name)
         (nat-int? period-ms)
         (fn? handler)]}
  (component.util/map->component
    {:init opts
     :start (fn [this]
              (if (:task this)
                this
                (do
                  (log/infof "Creating scheduled task %s" name)
                  (assert (concurrent/scheduler-pool? (:executor (:scheduler this)))
                          "Scheduled task requires a :scheduler dependency ")
                  (assoc this :task (concurrent/schedule-task (-> this :scheduler :executor)
                                                              {:handler (fn scheduled' []
                                                                          (try
                                                                            (handler (dissoc this :scheduler :task))
                                                                            (catch Throwable err
                                                                              (log/errorf err "recurring task '%s' failed" name))))
                                                               :period-ms period-ms
                                                               :delay-ms delay-ms})))))

     :stop (fn [this]
             (if (:task this)
               (do
                 (log/warnf "stopping task %s" (:name this))
                 ;; no need to do anything special, the task will be stopped when the pool is stopped
                 (assoc this :task nil))
               this))}))
