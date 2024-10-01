(ns utility-belt.component
  (:require
   [com.stuartsierra.component :as component]
   [utility-belt.lifecycle :as lifecycle]))

(defn deps
  "Converts a mixed list of dependencies into a
  dependency map.

  ```clojure
  [:a :b {:c :f} :d] -> { :a :a :b :b :c :f :d :d }
  ```

  More readable example:

  ```clojure
  [ :db :es {:redis :redis-conn} :http-client]
  ```

  would expand to:

  ```clojure
  { :db :db
    :es :es
    :redis :redis-conn
    :http-client :http-client }
  ```

  Useful when given component's dependency list is mostly
  the same but has one or two exceptions which require aliasing."
  [dependencies]
  (->> dependencies
       (map (fn [x]
              (if (map? x)
                (vec (flatten (seq x)))
                [x x])))
       (into {})))

(defn using+
  "Like component/using but accepts a mixed list of component dependencies.
  See `+utility-belt.component/deps+`:

  ```clojure
  (using+ (some-component/create) [ :a :b {:c :d}])
  ```

  > [!WARNING]
  > requires Component library to be pressent in the classpath"
  [component dependencies-list]
  (component/using component (deps dependencies-list)))

(defn map->system
  "Convinence function to convert a map into a SystemMap record"
  [sys-map]
  (component/map->SystemMap sys-map))

(defn map->component
  "Given an inital (a map) and single arg-start/stop functions accepting the
  map representing the component, returns a component.
  Simplifies extending-via-metadata pattern to ensure right naming of things.

  > [!NOTE]
  > this is only suitable for simpler components, with minimal state that don't need
  >  to implement any other protocols or interfaces"
  [{:keys [init
           start
           stop]
    :or {init {}
         start identity
         stop identity}}]
  (with-meta init
    {'com.stuartsierra.component/start start
     'com.stuartsierra.component/stop stop}))

(defn init-app-system
  "Simplifies wiring up the system as the app entry point, with a graceful shutdown.
  This is helpful to reduce boilerplate in the main namespace.

  Args:

  - `store` - an atom to store the system in once it's started.
              You can later refer to it by derefing it in your REPL session
  - `system-fn` - a function that returns the system map, **NOT** an instance of `SystemMap`


  Example:
  ```clojure
  (ns some.api.core
    (:require [some.api.system :as system]
              [utility-belt.component :as component]))

  (def app (atom nil))

  (defn -main [& _args]
    (component/init-app-system {:store app
                                :system-fn  system/production)))
  ```
  "
  [{:keys [store system-fn]}]
  {:pre [(instance? clojure.lang.Atom store)
         (fn? system-fn)]}
  (reset! store (component/start-system (map->system (system-fn))))
  (lifecycle/add-shutdown-hook :shutdown-system (fn stop! []
                                                  (swap! store
                                                         #(when % (component/stop-system %)))))

  store)
