(ns utility-belt.component
  (:require
   [com.stuartsierra.component :as component]))

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

  If your component needs to implement addional protocol(s), which have `:extend-via-metadata true` set
  your component definition should include all the keys required by those protocol as fully qualified symbols or keywords:

  ```clojure
  (ns app.sth)
  (defprotocol Bananas
    (do-thing [this])
    (do-more [this]))


  (ns app.sth.component)

  (defn create [conf]
     (util.component/map->component {:init conf ;; initial state of the component
                                     :start (fn [this] (assoc this :started true))
                                     :stop  (fn [this] (assoc this :stopped true))
                                     ;; protocol methods, symbols work too!
                                     :app.sth/Bananas/do-thing (fn [this] :did-thing)
                                     :app.sth/Bananas/do-more  (fn [this] :did-more)}))
  ```
  "
  [{:keys [init
           start
           stop]
    :or {init {}
         start identity
         stop identity}
    :as component-def}]
  (with-meta init
    (merge (-> (or component-def {})
               (dissoc :init :start :stop)
               (update-keys symbol))
           {'com.stuartsierra.component/start start
            'com.stuartsierra.component/stop stop})))
