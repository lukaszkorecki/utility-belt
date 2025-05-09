(ns utility-belt.component.system-test
  (:require [clojure.test :refer [deftest testing is]]
            [utility-belt.component :as util.component]
            [utility-belt.component.system :as util.system]))

(def counter
  (atom 0))

(defn make-system []
  {:static :value
   :thing (util.component/map->component {:start (fn [this]
                                                   (swap! counter inc)
                                                   (assoc this :started true :stopped false))

                                          :stop (fn [this]
                                                  (swap! counter dec)
                                                  (assoc this :stopped true :started false))})})

(deftest setup-for-dev-test
  (let [{:keys [start-system stop-system get-system]} (util.system/setup-for-dev {:component-map-fn 'utility-belt.component.system-test/make-system
                                                                                  :ns-to-attach-to 'utility-belt.component.system-test
                                                                                  :reloadable? false})]

    (testing "system can be started via provided fns"
      (testing "system starts only once"
        (start-system)
        (start-system)
        (is (= 1 @counter)))

      (testing "we can get the 'components'"
        (is (= :value (-> (get-system) :static))))

      (testing "start/stop/get functions are setup in attached namespace"
        (is (= true (-> @@(find-var 'utility-belt.component.system-test.dev-sys/system) :thing :started)))
        (is (fn? @(find-var 'utility-belt.component.system-test.dev-sys/start)))
        (is (fn? @(find-var 'utility-belt.component.system-test.dev-sys/stop)))
        (is (instance? (class (atom {})) @(find-var 'utility-belt.component.system-test.dev-sys/system)))))

    (testing "stopping"
      (stop-system)

      (is (nil? (-> (get-system) :static)))

      (is (nil? (-> @@(find-var 'utility-belt.component.system-test.dev-sys/system) :thing :started)))
      (testing "stop handler was called on the component"
        (is (zero? @counter))))

    (testing "starting again, but with extra components"
      (start-system {:another-static :another-thing})
      (is (= :another-thing (-> (get-system) :another-static)))
      (stop-system))))

(deftest setup-for-test-test
  (testing "provides utility for unit tests"
    (let [{:keys [use-test-system get-system]} (util.system/setup-for-test {:component-map-fn 'utility-belt.component.system-test/make-system
                                                                            :ns-to-attach-to 'utility-belt.component.system-test})]

      (testing "nothing is running"
        (is (nil? @@(find-var 'utility-belt.component.system-test.dev-sys/system))))

      (testing "within the hook, system is started and can be used"
        (use-test-system (fn []
                           (is (= :value (-> (get-system) :static)))
                           (is (= true (-> (get-system) :thing :started)))
                           (is (= 1 @counter)))))

      (testing "nothing is running, again"
        (is (zero? @counter))
        (is (nil? @@(find-var 'utility-belt.component.system-test.dev-sys/system)))))))
