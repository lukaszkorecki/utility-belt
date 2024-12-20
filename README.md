# utility-belt

> Although seemingly unremarkable in appearance, the utility belt is one of Batman's most important tools in fighting crime.


## About

Utility belt started at [EnjoyHQ](https://github.com/nomnom-insights/nomnom.utility-belt) as a way of sharing code that didn't belong anywhere else but was useful to have and use across services and libraries.

This fork carries on that tradition, but further reduces the scope so that it provides only zero-dependency code useful for building applications.

## Documentation



### General utilities


- `utility-belt.base64` - wraps JDK base64 encoding and decoding with string and byte array methods
- `utility-belt.type` - type checking utilities such as missing `atom?` function
- `utility-belt.compile` - macros for conditional compilation


### Application utilities


- `utility-belt.lifecycle` - easy management of shutdown hooks


### Component utilities


- `utility-belt.component` - utilities for making it easier to create components, and systems
- `utility-belt.component.nrepl` - pre-built component for the nREPL server

- `utility-belt.component.system` - utilities for managing systems of components for dev, test and production (see below)


#### production setup


`utility-belt.component.system/init-for-prod` reduces boilerplate for creating a system of components for production environments. It registers a shutdown hook to stop the system when the JVM is shutting down.

```clojure
(ns app.core
  (:require [utility-belt.component.system :as system]
            [app.system]))


;; so that you can access it later via nREPL, optional
(def sys (atom nil))


(def -main [& args]
  (system/setup-for-prod {:store sys
                          :component-map-fn app.system/production}))


;; now when the app starts, started system will be in `app.core/sys` atom
;; stopping the JVM process will automatically stop the system
```

#### dev/test component systems

`utility-belt.component.system/setup-for-dev` reduces boilerplate for creating a system of components for dev and test environments.
Additionally it allows for easy system reloading in the REPL, with additional state clean up so that all code used by components is guaranteed to be reloaded.
- `utility-belt.component.system/setup-for-test` - similar to the above, also includes a `use-test-system` hook for `clojure.test/use-fixtures`


Dev workflow setup:

```clojure
(ns app.repl ;; here or in user.clj
  (:require [utility-belt.component.system :as system]
            [app.system]))




(let [sys-utils (system/setup-for-dev {:component-map-fn 'app.system/development
                                      :reloadable? true})
      {:keys [start-system stop-system get-system] } sys-utils]

  (def start start-system)
  (def stop stop-system)
  (def system get-system))

;; now in the repl you can run:
;; started system will be stored in atom: app.repl.dev-sys/system
(start)
;; you can get the system by running:
(get-system)

;; make some changes in the code and reload the system:
(stop)
(start)
;; code is reloaded and system is started again
```


In unit testing, the setup is similar:

```clojure
(ns app.test-utils
  (:require [app.system]
            [utility-belt.component.system :as sys]))


(let [sys-utils (system/setup-for-test {:component-map-fn 'app.system/test})
      {:keys [use-test-system get-system] } sys-utils]
  (def system get-system)
  (def with-test-system use-test-system))



;; now in tests:
(ns app.something-test
  (:require [app.test-utils :as test-utils]
            [clojure.test :refer [use-fixtures deftest])))


(use-fixtures :each test-utils/with-test-system)

(deftest something-test
  (is (= :bananas (some/handler {:component (test-utils/system)
                                 :body {:fruit :bananas}}))))

;; you can also attach extra component map:


(use-fixtures :each (fn [test]
                      (test-utils/with-test-system
                        test
                        {:extra-component :hello
                         :something-else (component.util/map->component {:name :something-else})})))

```
