(ns utility-belt.component.scheduler-test
  (:require
   [clojure.test :refer [deftest testing is use-fixtures]]
   [com.stuartsierra.component :as component]
   [utility-belt.component :as c]
   [utility-belt.component.scheduler :as comp.scheduler]))

(def sys
  (c/map->system
   {:scheduler (comp.scheduler/create-pool {:name "Test"})
    :store (atom [])
    :task-1 (component/using
             (comp.scheduler/create-task {:name "task 1"
                                          :period-ms 100
                                          :handler (fn [{:keys [store]}]
                                                     (swap! store conj :task-1))})
             [:scheduler :store])

    :task-2 (component/using
             (comp.scheduler/create-task {:name "task 2"
                                          :period-ms 200
                                          :handler (fn [{:keys [store]}]
                                                     (swap! store conj :task-2))})
             [:scheduler :store])}))

(def system (atom nil))

(use-fixtures :once (fn [test]
                      (try
                        (reset! system (component/start sys))
                        (test)
                        (finally
                          (swap! system #(when %
                                           (component/stop %)))))))

(deftest scheduler-test
  (Thread/sleep 500)
  (testing "tasks 'tick' for 500s"
    (is (= [:task-1
            :task-1
            :task-1
            :task-1
            :task-1
            :task-2
            :task-2
            :task-2]
           (->> @system :store deref (take 8) sort)))))
