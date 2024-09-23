(ns utility-belt.component-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as c]
   [utility-belt.component :as component]))

(set! *warn-on-reflection* true)

(deftest deps-list
  (is (= {:a :a
          :b :b
          :c :f
          :d :d}
         (component/deps [:a :b {:c :f} :d]))))

(defrecord Fake
  [dep-a dep-b dep-d]
  c/Lifecycle
  (start [this] this)
  (stop [this] this))

(deftest using+-test
  (let [com (c/map->SystemMap
             {:test (component/using+
                     (->Fake nil nil nil)
                     [:a :b {:d-alias :d}])
               ;; fake components
              :a (fn [] :dep-a)
              :b (fn [] :dep-b)
              :d (fn [] :dep-d-as-alias)})
        sys (c/start-system com)]
    (is (= :dep-d-as-alias
           ;; "use" the dependency by calling the function
           ((:d-alias (:test sys)))))))

(deftest map-components-test
  (let [mini-sys (component/map->system
                  {:test (component/map->component {:init-val {:hello :world}
                                                    :start #(assoc % :started true)
                                                    :stop #(assoc % :started false
                                                                  :stopped true)})})]

    (testing "initial state"
      (is (= {:hello :world}
             (:test mini-sys))))

    (testing "starting & stopping"
      (let [sys (c/start mini-sys)]
        (is (= {:hello :world
                :started true}
               (:test sys)))
        (is (= {:hello :world
                :started false
                :stopped true}
               (:test (c/stop sys))))))))
