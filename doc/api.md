
# API Documentation



# `utility-belt.base64`
> <sup>`src/utility_belt/base64.clj`</sup>








> Encoding and decoding to and from base64



<details>
  <summary>Functions, macros & vars</summary>

- [decoder](#utility-belt.base64/decoder)

- [encoder](#utility-belt.base64/encoder)

- [default-charset](#utility-belt.base64/default-charset)

- [decode](#utility-belt.base64/decode)

- [decode-&gt;str](#utility-belt.base64/decode-&gt;str)

- [encode](#utility-belt.base64/encode)

- [encode-&gt;str](#utility-belt.base64/encode-&gt;str)

</details>

<hr />




## <a name="utility-belt.base64/decoder">`utility-belt.base64/decoder`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:7</code></p></summary>

```clojure

(def ^Base64$Decoder decoder (Base64/getDecoder))

```

</details>



## <a name="utility-belt.base64/encoder">`utility-belt.base64/encoder`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:8</code></p></summary>

```clojure

(def ^Base64$Encoder encoder (Base64/getEncoder))

```

</details>



## <a name="utility-belt.base64/default-charset">`utility-belt.base64/default-charset`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:10</code></p></summary>

```clojure

(def default-charset (.toString StandardCharsets/UTF_8))

```

</details>



## <a name="utility-belt.base64/decode">`utility-belt.base64/decode`</a> <sup>function</sup>
> 





> 
Decode from bytes or string to bytes


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:12</code></p></summary>

```clojure

(defn decode
  "Decode from bytes or string to bytes"
  ^bytes [base64]
  {:pre [(or (bytes? base64) (string? base64))]}
  (byte-array
    ;; needs to cond on type to stop reflection
   (cond
     (bytes? base64) (.decode decoder ^bytes base64)
     (string? base64) (.decode decoder ^String base64))))

```

</details>



## <a name="utility-belt.base64/decode-&gt;str">`utility-belt.base64/decode->str`</a> <sup>function</sup>
> 





> 
Decode from bytes or string to string


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:22</code></p></summary>

```clojure

(defn decode->str
  "Decode from bytes or string to string"
  (^String [base64]
   (decode->str base64 default-charset))
  (^String [base64 ^String encoding]
   {:pre [(Charset/isSupported encoding)]}
   (String. (decode base64) encoding)))

```

</details>



## <a name="utility-belt.base64/to-bytes">`utility-belt.base64/to-bytes`</a> <sup>function</sup>
> 

**Private**





> 
Ensure data is bytes


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:30</code></p></summary>

```clojure

(defn- to-bytes
  "Ensure data is bytes"
  ^bytes [data ^String encoding]
  {:pre [(or (bytes? data) (string? data))
         (Charset/isSupported encoding)]}
  (cond
    (bytes? data) data
    (string? data) (.getBytes ^String data encoding)))

```

</details>



## <a name="utility-belt.base64/encode">`utility-belt.base64/encode`</a> <sup>function</sup>
> 





> 
Encode string or bytes to bytes


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:39</code></p></summary>

```clojure

(defn encode
  "Encode string or bytes to bytes"
  (^bytes [data]
   (encode data default-charset))
  (^bytes [data ^String encoding]
   (let [^bytes to-encode (to-bytes data encoding)]
     (.encode encoder to-encode))))

```

</details>



## <a name="utility-belt.base64/encode-&gt;str">`utility-belt.base64/encode->str`</a> <sup>function</sup>
> 





> 
Encode string or bytes to string


<details>
  <summary><sub>Source: <code>src/utility_belt/base64.clj:47</code></p></summary>

```clojure

(defn encode->str
  "Encode string or bytes to string"
  (^String [data]
   (encode->str data default-charset))
  (^String [data ^String encoding]
   (-> data (encode encoding) (String. encoding))))

```

</details>






# `utility-belt.compile`
> <sup>`src/utility_belt/compile.clj`</sup>








> Utilities for conditional code evaluation/loading



<details>
  <summary>Functions, macros & vars</summary>

- [compile-if](#utility-belt.compile/compile-if)

- [compile-when](#utility-belt.compile/compile-when)

</details>

<hr />




## <a name="utility-belt.compile/compile-if">`utility-belt.compile/compile-if`</a> <sup>macro</sup>
> 





> 
Evaluate `exp` and if it returns logical true and doesn't error, expand to
`then`.  Else expand to `else`.

```clojure
(compile-if (Class/forName "java.util.concurrent.ForkJoinTask")
(do-cool-stuff-with-fork-join)
(fall-back-to-executor-services))
```



<details>
  <summary><sub>Source: <code>src/utility_belt/compile.clj:4</code></p></summary>

```clojure

(defmacro compile-if
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to
  `then`.  Else expand to `else`.

  ```clojure
  (compile-if (Class/forName \"java.util.concurrent.ForkJoinTask\")
    (do-cool-stuff-with-fork-join)
    (fall-back-to-executor-services))
  ```
  "
  [exp then else]
  (if (try (eval exp)
           (catch Throwable _ false))
    `(do ~then)
    `(do ~else)))

```

</details>



## <a name="utility-belt.compile/compile-when">`utility-belt.compile/compile-when`</a> <sup>macro</sup>
> 





> 
Evaluate `exp` and if it returns logical true and doesn't error, expand to `then`
Otherwise evaluates to `nil`

```clojure
(compile-when (require '[java-time.api :as jt])
(do-cool-stuff-with-java-time))
```



<details>
  <summary><sub>Source: <code>src/utility_belt/compile.clj:20</code></p></summary>

```clojure

(defmacro compile-when
  "Evaluate `exp` and if it returns logical true and doesn't error, expand to `then`
  Otherwise evaluates to `nil`

  ```clojure
  (compile-when (require '[java-time.api :as jt])
    (do-cool-stuff-with-java-time))
  ```
  "
  [exp & then]
  `(compile-if ~exp (do ~@then) nil))

```

</details>






# `utility-belt.component`
> <sup>`src/utility_belt/component.clj`</sup>












<details>
  <summary>Functions, macros & vars</summary>

- [deps](#utility-belt.component/deps)

- [using+](#utility-belt.component/using+)

- [map-&gt;system](#utility-belt.component/map-&gt;system)

- [map-&gt;component](#utility-belt.component/map-&gt;component)

</details>

<hr />




## <a name="utility-belt.component/deps">`utility-belt.component/deps`</a> <sup>function</sup>
> 





> 
Converts a mixed list of dependencies into a
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
the same but has one or two exceptions which require aliasing.


<details>
  <summary><sub>Source: <code>src/utility_belt/component.clj:6</code></p></summary>

```clojure

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

```

</details>



## <a name="utility-belt.component/using+">`utility-belt.component/using+`</a> <sup>function</sup>
> 





> 
Like component/using but accepts a mixed list of component dependencies.
See `+utility-belt.component/deps+`:

```clojure
(using+ (some-component/create) [ :a :b {:c :d}])
```

> [!WARNING]
> requires Component library to be pressent in the classpath


<details>
  <summary><sub>Source: <code>src/utility_belt/component.clj:39</code></p></summary>

```clojure

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

```

</details>



## <a name="utility-belt.component/map-&gt;system">`utility-belt.component/map->system`</a> <sup>function</sup>
> 





> 
Convinence function to convert a map into a SystemMap record


<details>
  <summary><sub>Source: <code>src/utility_belt/component.clj:52</code></p></summary>

```clojure

(defn map->system
  "Convinence function to convert a map into a SystemMap record"
  [sys-map]
  (component/map->SystemMap sys-map))

```

</details>



## <a name="utility-belt.component/map-&gt;component">`utility-belt.component/map->component`</a> <sup>function</sup>
> 





> 
Given an inital (a map) and single arg-start/stop functions accepting the
map representing the component, returns a component.
Simplifies extending-via-metadata pattern to ensure right naming of things.

> [!NOTE]
> this is only suitable for simpler components, with minimal state that don't need
>  to implement any other protocols or interfaces


<details>
  <summary><sub>Source: <code>src/utility_belt/component.clj:57</code></p></summary>

```clojure

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

```

</details>






# `utility-belt.component.nrepl`
> <sup>`src/utility_belt/component/nrepl.clj`</sup>












<details>
  <summary>Functions, macros & vars</summary>

- [create](#utility-belt.component.nrepl/create)

</details>

<hr />




## <a name="utility-belt.component.nrepl/create">`utility-belt.component.nrepl/create`</a> <sup>function</sup>
> 





> 
Creates an nREPL server component, by default it binds to loopback address


<details>
  <summary><sub>Source: <code>src/utility_belt/component/nrepl.clj:5</code></p></summary>

```clojure

(defn create
  "Creates an nREPL server component, by default it binds to loopback address"
  [{:keys [host port]
    :or {host "0.0.0.0"}
    :as config}]
  {:pre [(pos? port)]}
  (component/map->component {:init config
                             :start (fn [this]
                                      (if (:server this)
                                        this
                                        (assoc this :server (nrepl.server/start-server :port port :bind host))))

                             :stop (fn [this]
                                     (if-let [server (:server this)]
                                       (do
                                         (nrepl.server/stop-server server)
                                         (assoc this :server nil))
                                       this))}))

```

</details>






# `utility-belt.component.system`
> <sup>`src/utility_belt/component/system.clj`</sup>












<details>
  <summary>Functions, macros & vars</summary>

- [init-for-prod](#utility-belt.component.system/init-for-prod)

- [tools-ns-available?](#utility-belt.component.system/tools-ns-available?)

- [tools-ns-available?](#utility-belt.component.system/tools-ns-available?)

- [init-for-dev](#utility-belt.component.system/init-for-dev)

- [system](#utility-belt.component.system/system)

- [start](#utility-belt.component.system/start)

- [restart](#utility-belt.component.system/restart)

- [stop](#utility-belt.component.system/stop)

- [system](#utility-belt.component.system/system)

- [start](#utility-belt.component.system/start)

- [stop](#utility-belt.component.system/stop)

- [with-test-system](#utility-belt.component.system/with-test-system)

</details>

<hr />




## <a name="utility-belt.component.system/init-for-prod">`utility-belt.component.system/init-for-prod`</a> <sup>function</sup>
> 





> 
Simplifies wiring up the system as the app entry point, with a graceful shutdown.
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



<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:9</code></p></summary>

```clojure

(defn init-for-prod
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
  {:pre [(type/atom? store)
         (fn? system-fn)]}
  (reset! store (component/start-system (util.component/map->system (system-fn))))
  (lifecycle/add-shutdown-hook :shutdown-system (fn stop! []
                                                  (swap! store
                                                         #(when % (component/stop-system %)))))

  store)

```

</details>



## <a name="utility-belt.component.system/tools-ns-available?">`utility-belt.component.system/tools-ns-available?`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:48</code></p></summary>

```clojure

(def tools-ns-available? true)

```

</details>



## <a name="utility-belt.component.system/tools-ns-available?">`utility-belt.component.system/tools-ns-available?`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:49</code></p></summary>

```clojure

(def tools-ns-available? false)

```

</details>



## <a name="utility-belt.component.system/init-for-dev">`utility-belt.component.system/init-for-dev`</a> <sup>function</sup>
> 





> 
Sets up a dev-system namespace which will provide start, stop and getter function as well
as hold on to the started system.
This makes it easier to work with component as part of a dev setup (with easy reloading)
as well as tests, where systems can be controlled in a programatic way without
affecting how normal 'production' system is started/stopped/restarted.

Returns a map with keys :start-system, :stop-system, :get-system, :restart-system, you can
destructure it and assing to vars in your namespace.

The reason why this exists is to make sure that code reloaded via `tools.namespace.repl` is
truly reloadable, by ensuring all satate is nuked before reloading.

Args:
- ns-to-attach-to: namespace to attach the dev-system to, by default it attaches to the current namespace (`*ns*`)
- system-map-fn: a symbol pointing to a function that returns a **map of components** NOT an instance of `component/SystemMap`
- reloadable?: boolean, if true, will enable reloading of the system map function and the system itself, default is false



<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:51</code></p></summary>

```clojure

(defn init-for-dev
  "Sets up a dev-system namespace which will provide start, stop and getter function as well
  as hold on to the started system.
  This makes it easier to work with component as part of a dev setup (with easy reloading)
  as well as tests, where systems can be controlled in a programatic way without
  affecting how normal 'production' system is started/stopped/restarted.

  Returns a map with keys :start-system, :stop-system, :get-system, :restart-system, you can
  destructure it and assing to vars in your namespace.

  The reason why this exists is to make sure that code reloaded via `tools.namespace.repl` is
  truly reloadable, by ensuring all satate is nuked before reloading.

  Args:
  - ns-to-attach-to: namespace to attach the dev-system to, by default it attaches to the current namespace (`*ns*`)
  - system-map-fn: a symbol pointing to a function that returns a **map of components** NOT an instance of `component/SystemMap`
  - reloadable?: boolean, if true, will enable reloading of the system map function and the system itself, default is false
  "
  [{:keys [ns-to-attach-to
           system-map-fn
           reloadable?]}]
  {:pre [(qualified-symbol? system-map-fn)
         (or (nil? ns-to-attach-to)
             (symbol? ns-to-attach-to))]}
  (when reloadable?
    (assert tools-ns-available? "clojure.tools.namespace.repl is not available, cannot enable code reloading!")
    #_{:clj-kondo/ignore [:unresolved-namespace]}
    (clojure.tools.namespace.repl/disable-reload! *ns*)
    (when ns-to-attach-to
      #_{:clj-kondo/ignore [:unresolved-namespace]}
      (clojure.tools.namespace.repl/disable-reload! (find-ns ns-to-attach-to))))

  (let [;; by default, attaches itself to the current namespace
        sys-ns (str (if ns-to-attach-to
                      ns-to-attach-to
                      (str *ns*))
                    ".dev-sys")
        sys-ns-sym (symbol sys-ns)
        ;; get the ns symbol, for reloading
        system-fn-ns-sym (-> system-map-fn
                             resolve
                             meta
                             :ns
                             clojure.lang.Namespace/.getName
                             symbol)]
    (when-not system-fn-ns-sym
      (throw (ex-info "not a valid symbol for system map fn"
                      {:system-map-fn system-map-fn})))

    {:start-system (fn start-dev-sys' [& additional-component-map]
                     (println "Setting up " sys-ns-sym)
                     ;; first check if we have previous state hanging
                     ;; if so, nuke it
                     (when (resolve (symbol sys-ns "system"))
                       (remove-ns sys-ns-sym))

                     ;; now (re)create the  namespace and its vars
                     (create-ns sys-ns-sym)
                     (let [;; we will hold the running system in an atom
                           sys-atom' (intern sys-ns-sym 'system (atom nil))

                           ;; start function reloads the system 'factory' namespace and starts the system
                           ;; started system is stored in the atom
                           start' (intern sys-ns-sym 'start (fn sys-start' []
                                                              ;; reload the namespace which provides component map
                                                              (require system-fn-ns-sym :reload)
                                                              ;; now start the system and store it for later
                                                              (swap! (var-get sys-atom')
                                                                     (fn [sys]
                                                                       (if sys
                                                                         (do
                                                                           (println "System already running")
                                                                           sys)
                                                                         (do
                                                                           (println "Starting system")
                                                                           (when (and tools-ns-available? reloadable?)
                                                                             #_{:clj-kondo/ignore [:unresolved-namespace]}
                                                                             (clojure.tools.namespace.repl/refresh))
                                                                           (-> ((var-get (resolve system-map-fn)))
                                                                               (merge (first additional-component-map))
                                                                               component/map->SystemMap
                                                                               component/start)))))))
                           ;; define simple stop-fn, it will not be used here
                           ;; but called by a separate stop fn wrapper
                           _stop' (intern sys-ns-sym 'stop (fn sys-stop' []
                                                             (swap! (var-get sys-atom')
                                                                    (fn [sys]
                                                                      (if sys
                                                                        (do
                                                                          (println "Stopping system")
                                                                          (component/stop sys)
                                                                          nil)
                                                                        (println "System not running"))))))]

                       (start')))

     :stop-system (fn stop-dev-system' []
                    (if-let [stop' (ns-resolve sys-ns-sym 'stop)]
                      (do
                        (println "Stopping system")
                        (stop'))
                      (println "error: system not started")))

     :get-system (fn get-dev-system' []
                   @(var-get (resolve (symbol sys-ns "system"))))}))

```

</details>



## <a name="utility-belt.component.system/system">`utility-belt.component.system/system`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:164</code></p></summary>

```clojure

(def system get-system)

```

</details>



## <a name="utility-belt.component.system/start">`utility-belt.component.system/start`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:165</code></p></summary>

```clojure

(def start start-system)

```

</details>



## <a name="utility-belt.component.system/restart">`utility-belt.component.system/restart`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:166</code></p></summary>

```clojure

(def restart (fn [] (stop-system) (start-system)))

```

</details>



## <a name="utility-belt.component.system/stop">`utility-belt.component.system/stop`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:167</code></p></summary>

```clojure

(def stop stop-system)

```

</details>



## <a name="utility-belt.component.system/system">`utility-belt.component.system/system`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:173</code></p></summary>

```clojure

(def system get-system)

```

</details>



## <a name="utility-belt.component.system/start">`utility-belt.component.system/start`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:174</code></p></summary>

```clojure

(def start start-system)

```

</details>



## <a name="utility-belt.component.system/stop">`utility-belt.component.system/stop`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:175</code></p></summary>

```clojure

(def stop stop-system)

```

</details>



## <a name="utility-belt.component.system/with-test-system">`utility-belt.component.system/with-test-system`</a> <sup>function</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/component/system.clj:177</code></p></summary>

```clojure

(defn with-test-system [test-fn]
      (try
        (start)
        (test-fn)
        (finally
          (stop))))

```

</details>






# `utility-belt.lifecycle`
> <sup>`src/utility_belt/lifecycle.clj`</sup>








> Tools for managing application lifecycle



<details>
  <summary>Functions, macros & vars</summary>

- [add-shutdown-hook](#utility-belt.lifecycle/add-shutdown-hook)

- [run-registered-hooks](#utility-belt.lifecycle/run-registered-hooks)

- [register-shutdown-hooks!](#utility-belt.lifecycle/register-shutdown-hooks!)

</details>

<hr />




## <a name="utility-belt.lifecycle/hooks">`utility-belt.lifecycle/hooks`</a> <sup>var</sup>
> 

**Private**





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/lifecycle.clj:6</code></p></summary>

```clojure

(def ^{:private true :doc "Default hooks list"}
  hooks
  (atom {::shutdown-agents shutdown-agents}))

```

</details>



## <a name="utility-belt.lifecycle/add-shutdown-hook">`utility-belt.lifecycle/add-shutdown-hook`</a> <sup>function</sup>
> 





> 
Register a function to run when the application *gracefully* shuts down.
Useful for stopping the Component system or other resources that have a life cycle.


<details>
  <summary><sub>Source: <code>src/utility_belt/lifecycle.clj:10</code></p></summary>

```clojure

(defn add-shutdown-hook
  "Register a function to run when the application *gracefully* shuts down.
  Useful for stopping the Component system or other resources that have a life cycle."
  [name hook-fn]
  (log/infof "registered hook '%s'" name)
  (swap! hooks assoc name hook-fn))

```

</details>



## <a name="utility-belt.lifecycle/run-registered-hooks">`utility-belt.lifecycle/run-registered-hooks`</a> <sup>function</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/lifecycle.clj:17</code></p></summary>

```clojure

(defn run-registered-hooks
  []
  (mapv (fn [[name hook-fn]]
          (try
            (log/infof "running shutdown hook '%s'" name)
            (hook-fn)
            (catch Exception err
              (log/errorf err "shutdown hook '%s' failed" name))))
        @hooks))

```

</details>



## <a name="utility-belt.lifecycle/register-shutdown-hooks!">`utility-belt.lifecycle/register-shutdown-hooks!`</a> <sup>function</sup>
> 





> 
Install the shutdown handler, which will run any registered shutdown hooks.


<details>
  <summary><sub>Source: <code>src/utility_belt/lifecycle.clj:27</code></p></summary>

```clojure

(defn register-shutdown-hooks!
  "Install the shutdown handler, which will run any registered shutdown hooks."
  [hooks]
  {:pre [(seq hooks)
         (every? (fn [[name hook-fn]]
                   (and (keyword? name)
                        (fn? hook-fn)))
                 hooks)]}
  (mapv (fn [[name hook-fn]]
          (add-shutdown-hook name hook-fn))
        hooks)
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable run-registered-hooks)))

```

</details>






# `utility-belt.type`
> <sup>`src/utility_belt/type.clj`</sup>












<details>
  <summary>Functions, macros & vars</summary>

- [atom?](#utility-belt.type/atom?)

</details>

<hr />




## <a name="utility-belt.type/atom?">`utility-belt.type/atom?`</a> <sup>function</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/type.clj:3</code></p></summary>

```clojure

(defn atom? [thing] (instance? clojure.lang.Atom thing))

```

</details>





Generated 07/10/2024 15:51
