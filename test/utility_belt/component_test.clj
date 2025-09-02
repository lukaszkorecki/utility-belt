(ns utility-belt.component-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [com.stuartsierra.component :as component]
   [utility-belt.component :as component.util]))

(set! *warn-on-reflection* true)

(deftest deps-list
  (is (= {:a :a
          :b :b
          :c :f
          :d :d}
         (component.util/deps [:a :b {:c :f} :d]))))

(defrecord Fake [dep-a dep-b dep-d]
  component/Lifecycle
  (start [this] this)
  (stop [this] this))

(defprotocol ISomething
  :extend-via-metadata true
  (do-something [this])
  (do-more [this]))

(deftest using+-test
  (let [com (component.util/map->system
             {:test (component.util/using+
                     (->Fake nil nil nil)
                     [:a :b {:d-alias :d}])
              ;; fake components
              :a (fn [] :dep-a)
              :b (fn [] :dep-b)
              :d (fn [] :dep-d-as-alias)})
        sys (component/start-system com)]
    (is (= :dep-d-as-alias
           ;; "use" the dependency by calling the function
           ((:d-alias (:test sys)))))))

(deftest map-components-test
  (let [mini-sys (component.util/map->system
                  {:test (component/using (component.util/map->component {:init {:hello :world}
                                                                          :start #(assoc % :started true)
                                                                          :stop #(assoc % :started false
                                                                                        :stopped true)})
                                          [:dep])
                   :dep :HELLO})]

    (testing "initial state"
      (is (= {:hello :world}
             (:test mini-sys))))

    (testing "starting & stopping"
      (let [sys (component/start mini-sys)]
        (is (= {:hello :world
                :started true
                :dep :HELLO}
               (:test sys)))
        (is (= {:hello :world
                :started false
                :stopped true
                :dep :HELLO}
               (:test (component/stop sys))))))))

(deftest map-components-with-extra-protocol-test

  (let [thing (component/start (component.util/map->component
                                {:init {:hello :world}
                                 :start #(assoc % :started true)
                                 :stop #(assoc % :started false
                                               :stopped true)
                                 ;; qual kws work
                                 ::do-something (fn [this] (str "Did something with " (:hello this)))
                                 ;; symbols work too:
                                 'utility-belt.component-test/do-more (fn [this] (str "Did more with " (:hello this)))}))]
    (is (= "Did something with :world"
           (do-something thing)))

    (is (= "Did more with :world"
           (do-more thing)))))
