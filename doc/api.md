
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
      (string? base64) (.decode decoder ^String base64 ))))

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






# `utility-belt.component`
> <sup>`src/utility_belt/component.clj`</sup>












<details>
  <summary>Functions, macros & vars</summary>

- [deps](#utility-belt.component/deps)

- [using+](#utility-belt.component/using+)

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
  <summary><sub>Source: <code>src/utility_belt/component.clj:3</code></p></summary>

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



## <a name="utility-belt.component/using+">`utility-belt.component/using+`</a> <sup>var</sup>
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
  <summary><sub>Source: <code>src/utility_belt/component.clj:36</code></p></summary>

```clojure

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

(def ^{:private true :doc "Default hooks list" } hooks (atom {::shutdown-agents shutdown-agents}))

```

</details>



## <a name="utility-belt.lifecycle/add-shutdown-hook">`utility-belt.lifecycle/add-shutdown-hook`</a> <sup>function</sup>
> 





> 
Register a function to run when the application *gracefully* shuts down.
Useful for stopping the Component system or other resources that have a life cycle.


<details>
  <summary><sub>Source: <code>src/utility_belt/lifecycle.clj:8</code></p></summary>

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
  <summary><sub>Source: <code>src/utility_belt/lifecycle.clj:15</code></p></summary>

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
  <summary><sub>Source: <code>src/utility_belt/lifecycle.clj:25</code></p></summary>

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






# `utility-belt.sanitize`
> <sup>`src/utility_belt/sanitize.clj`</sup>












<details>
  <summary>Functions, macros & vars</summary>

- [remove-spaces](#utility-belt.sanitize/remove-spaces)

- [email](#utility-belt.sanitize/email)

</details>

<hr />




## <a name="utility-belt.sanitize/remove-spaces">`utility-belt.sanitize/remove-spaces`</a> <sup>function</sup>
> 





> 
Remove all spaces from a string


<details>
  <summary><sub>Source: <code>src/utility_belt/sanitize.clj:6</code></p></summary>

```clojure

(defn remove-spaces
  "Remove all spaces from a string"
  [st]
  {:pre [(string? st)]}
  (clojure.string/replace st #"\s" ""))

```

</details>



## <a name="utility-belt.sanitize/email">`utility-belt.sanitize/email`</a> <sup>function</sup>
> 





> 
Clean email string and return allowed email using validation/pattern


<details>
  <summary><sub>Source: <code>src/utility_belt/sanitize.clj:12</code></p></summary>

```clojure

(defn email
  "Clean email string and return allowed email using validation/pattern"
  [email]
  {:pre [(string? email)]}
  (when-let [email (-> email
                       (clojure.string/lower-case)
                       (remove-spaces))]
    (re-find utility-belt.validation/email-pattern email)))

```

</details>






# `utility-belt.validation`
> <sup>`src/utility_belt/validation.clj`</sup>












<details>
  <summary>Functions, macros & vars</summary>

- [email-pattern-string](#utility-belt.validation/email-pattern-string)

- [email-pattern](#utility-belt.validation/email-pattern)

- [valid-email?](#utility-belt.validation/valid-email?)

</details>

<hr />




## <a name="utility-belt.validation/email-pattern-string">`utility-belt.validation/email-pattern-string`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/validation.clj:5</code></p></summary>

```clojure

(def email-pattern-string
  #"[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")

```

</details>



## <a name="utility-belt.validation/email-pattern">`utility-belt.validation/email-pattern`</a> <sup>var</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/validation.clj:8</code></p></summary>

```clojure

(def email-pattern (re-pattern email-pattern-string))

```

</details>



## <a name="utility-belt.validation/valid-email?">`utility-belt.validation/valid-email?`</a> <sup>function</sup>
> 





> 
> *no doc*


<details>
  <summary><sub>Source: <code>src/utility_belt/validation.clj:11</code></p></summary>

```clojure

(defn valid-email?
  [email]
  (re-matches email-pattern email))

```

</details>





Generated 25/03/2024 10:03
