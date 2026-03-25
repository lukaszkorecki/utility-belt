(ns utility-belt.component.system-test
  (:require [clojure.test :refer [deftest testing is]]
            [utility-belt.component :refer [map->component]]
            [utility-belt.component.system :refer [setup-for-production
                                                   setup-for-dev
                                                   setup-for-test]]
            [utility-belt.lifecycle :as lifecycle]))

(def fn-symbol
  'utility-belt.component.system-test/make-system)

(def counter
  (atom 0))

(defn make-system []
  {:static :value
   :thing (map->component {:start (fn [this]
                                    (swap! counter inc)
                                    (assoc this :started true :stopped false))
                           :stop (fn [this]
                                   (swap! counter dec)
                                   (assoc this :stopped true :started false))})})

(defn- assert-prod-behavior [component-map-fn]
  (let [store (atom nil)]
    (setup-for-production {:store store
                           :component-map-fn component-map-fn})
    (is (some? @store))
    (is (= :value (-> @store :static)))
    (is (= 1 @counter))
    (is (contains? @lifecycle/registerd-hooks :shutdown-system))
    (lifecycle/remove-shutdown-hooks!)))

(deftest setup-for-production-test
  (testing "passing qualified symbol for component map fn"
    (assert-prod-behavior fn-symbol))
  (testing "passing plain function for component map fn"
    (assert-prod-behavior fn-symbol)))

(defn- assert-dev-behavior [component-map-fn]
  (let [control (setup-for-dev {:component-map-fn component-map-fn
                                :reloadable? false})
        {:keys [start-system stop-system get-system]} control]
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
      (is (= 1 @counter))
      (is (= :another-thing (-> (get-system) :another-static)))
      (stop-system)
      (is (zero? @counter)))))

(deftest setup-for-dev-test
  (testing "passing qualified symbol for component map fn"
    (assert-dev-behavior fn-symbol))
  (testing "passing plain function for component map fn"
    (assert-dev-behavior fn-symbol)))

(deftest setup-for-test-test
  (testing "provides utility for unit tests"
    (let [{:keys [use-test-system get-system]} (setup-for-test {:component-map-fn fn-symbol})]
      (testing "within the hook, system is started and can be used"
        (use-test-system (fn []
                           (is (= :value (-> (get-system) :static)))
                           (is (= true (-> (get-system) :thing :started)))
                           (is (= 1 @counter)))))
      (testing "nothing is running, again"
        (is (zero? @counter))))))
