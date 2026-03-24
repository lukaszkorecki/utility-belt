(ns utility-belt.component.system-test
  (:require [clojure.test :refer [deftest testing is]]
            [utility-belt.component :as util.component]
            [utility-belt.component.system :as util.system]
            [utility-belt.lifecycle :as lifecycle]))

(def counter
  (atom 0))

(defn make-system []
  {:static :value
   :thing (-> {:start (fn [this]
                        (swap! counter inc)
                        (assoc this :started true :stopped false))
               :stop (fn [this]
                       (swap! counter dec)
                       (assoc this :stopped true :started false))}
              util.component/map->component)})

(defn- assert-prod-behavior [component-map-fn]
  (let [store (atom nil)]
    (-> {:store store
         :component-map-fn component-map-fn}
        util.system/setup-for-production)
    (is (some? @store))
    (is (= :value (-> @store :static)))
    (is (= 1 @counter))
    (is (contains? @lifecycle/registerd-hooks :shutdown-system))
    (lifecycle/remove-shutdown-hooks!)))

(deftest setup-for-production-test
  (testing "passing qualified symbol for component map fn"
    (assert-prod-behavior 'utility-belt.component.system-test/make-system))
  (testing "passing plain function for component map fn"
    (assert-prod-behavior utility-belt.component.system-test/make-system)))

(defn- assert-dev-behavior [component-map-fn]
  (let [control (-> {:component-map-fn component-map-fn
                     :reloadable? false}
                    util.system/setup-for-dev)
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
    (assert-dev-behavior 'utility-belt.component.system-test/make-system))
  (testing "passing plain function for component map fn"
    (assert-dev-behavior utility-belt.component.system-test/make-system)))

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
