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
                                                                                  :reloadable? false})]

    (testing "system can be started via provided fns"
      (testing "system starts only once"
        (start-system)
        (start-system)
        (is (= 1 @counter)))

      (testing "we can get the 'components'"
        (is (= :value (-> (get-system) :static)))))

    (testing "stopping"
      (stop-system)

      (is (nil? (-> (get-system) :static)))

      (testing "stop handler was called on the component"
        (is (zero? @counter))))

    (testing "starting again, but with extra components"
      (start-system {:another-static :another-thing})
      (is (= :another-thing (-> (get-system) :another-static)))
      (stop-system))))

(deftest setup-for-test-test
  (testing "provides utility for unit tests"
    (let [{:keys [use-test-system get-system]} (util.system/setup-for-test {:component-map-fn 'utility-belt.component.system-test/make-system})]

      (testing "within the hook, system is started and can be used"
        (use-test-system (fn []
                           (is (= :value (-> (get-system) :static)))
                           (is (= true (-> (get-system) :thing :started)))
                           (is (= 1 @counter)))))

      (testing "nothing is running, again"
        (is (zero? @counter))))))
