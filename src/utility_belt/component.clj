(ns utility-belt.component)

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

(def using+
  "Like component/using but accepts a mixed list of component dependencies.
  See `+utility-belt.component/deps+`:

  ```clojure
  (using+ (some-component/create) [ :a :b {:c :d}])
  ```

  > [!WARNING]
  > requires Component library to be pressent in the classpath"

  (if-let [using* (try
                    (requiring-resolve 'com.stuartsierra.component/using)
                    (catch Exception _e
                      false))]
    (fn using+' [component dependencies-list]
      (using* component (deps dependencies-list)))
    (fn using-missing' [_ _]
      (throw (ex-info "'Component' was not found in classpath" {})))))

(defn map->system
  "Convinence function to convert a map into a SystemMap record"
  [sys-map]
  (if-let [f' (requiring-resolve 'com.stuartsierra.component/map->SystemMap)]
    (f' sys-map)
    (throw (ex-info "'Component' was not found in classpath" {}))))

(defn map->component
  "Given an inital (a map) and single arg-start/stop functions accepting the
  map representing the component, returns a component.
  Simplifies extending-via-metadata pattern to ensure right naming of things.

  NOTE: this is only suitable for simpler components, with minimal state that don't need
  to implement any other protocols or interfaces"
  [{:keys [init-val
           start
           stop]
    :or {init-val {}
         start identity
         stop identity}}]
  (with-meta init-val
    {'com.stuartsierra.component/start start
     'com.stuartsierra.component/stop stop}))
