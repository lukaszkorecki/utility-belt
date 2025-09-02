# 🦇 utility-belt

> Although seemingly unremarkable in appearance, the utility belt is one of Batman's most important tools in fighting crime.


## About

[Utility belt](https://github.com/nomnom-insights/nomnom.utility-belt) started at [EnjoyHQ](https://enjoyhq.com) as a way of sharing code that didn't belong anywhere else but was useful to have and use across services and libraries.

This fork carries on that tradition, but further reduces the scope so that it provides nearly zero-dependency code useful for building applications.

Thank you to [Peerspace](https://peerspace.com) for their continued support in maintaining this library.


## Installation

`utility-belt` is released to Clojars.


- [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.lukaszkorecki/utility-belt.svg)](https://clojars.org/org.clojars.lukaszkorecki/utility-belt)
- [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.lukaszkorecki/utility-belt.svg?include_prereleases)](https://clojars.org/org.clojars.lukaszkorecki/utility-belt)


## Documentation



### General utilities


- `utility-belt.base64` - Encoding and decoding to and from base64
- `utility-belt.compile` - Utilities for conditional code evaluation/loading
- `utility-belt.type` - A collection of type-checking predicates.
- `utility-belt.resources` - readers for `.txt`, `.json`, and `.edn` resources:
- `utility-belt.concurrent` - A set of helpers for working with Java's concurrency constructs
- `utility-belt.lifecycle` - Tools for managing application lifecycle

### Application utilities


#### `utility-belt.lifecycle`

Provides easy management of JVM process shutdown hooks.
Typical usage:

``` clojure
(ns my.app
  (:require [utility-belt.lifecycle :as lifecycle]
            [my.app.entrypoint :as entrypoint]))


(defn -main [& args]
  (let [system (entrypoint/production)]
    (lifecycle/register-shutdown-hooks!
     {:stop-system (fn [] (entrypoint/stop-system system))
      :bye (fn [] (log/info "Bye!"))})

    system))


;; you can also register shutdown hooks from anywhere in your code
;; although it's not recommended to do so

(utility-belt.lifecycle/add-shutdown-hook :bye (fn [] (log/info "Bye!")))


```


### Component utilities


- `utility-belt.component` - utilities for making it easier to create components, and systems
- `utility-belt.component.jetty` - Provides a component for running a Jetty server with a Ring handler. Requires `ring/ring-jetty-adapter` dependency.
- `utility-belt.component.nrepl` - A component for running an nREPL server.
- `utility-belt.component.scheduler` - A component for scheduling tasks to run at a fixed rate.
- `utility-belt.component.system` - Functions for managing the lifecycle of a component-based system.

#### `ut.c.component`

`map->component` function can be used to create components out of maps, rather than records, this is useful when creating a record is an overkill or you want to easily provide a mock implementation of a component:

```clojure
(ns app.publisher)
(defprotocol Publisher
  :extend-via-metadata true
  (send! [this message]))


(ns app.system)

(def sys-map
  {:publisher (map->component {:init {:queue-name "foobar" }
                               :start (fn [this] (assoc this :conn (pub/create-connection)))
                               :stop (fn [this] (update this :conn pub/connection-close))
                               :app.publisher/send! (fn [this message] (pub/send! (:conn this) message))})})

```

this is a very simple example, your start and stop functions have to obey the same rules as record-based components etc.

#### `ut.c.system`

##### production


`utility-belt.component.system/setup-for-production` reduces boilerplate for creating a system of components for production environments. It registers a shutdown hook to stop the system when the JVM is shutting down.

Note: `:component-map-fn` **must** return a map of Components, not an instance of `SysteMap`!

```clojure
(ns app.core
  (:require [utility-belt.component.system :as system]
            [app.system]))


;; so that you can access it later via nREPL, optional
(def sys (atom nil))


(def -main [& args]
  (system/setup-for-production {:store sys
                                ;; NOTE: it has to return a map, not a SystemMap!
                                ;; it can also be a fully qualified symbol
                                :component-map-fn app.system/production}))


;; now when the app starts, started system will be in `app.core/sys` atom
;; stopping the JVM process will automatically stop the system

```

##### dev/test

`utility-belt.component.system/setup-for-dev` reduces boilerplate for creating a system of components for dev and test environments.
Additionally it allows for easy system reloading in the REPL, with additional state clean up so that all code used by components is guaranteed to be reloaded.
- `utility-belt.component.system/setup-for-test` - similar to the above, also includes a `use-test-system` hook for `clojure.test/use-fixtures`


Just like in `setup-for-production`, `:component-map-fn` has to be a qualified symbol, and has to return a map of components.

Dev workflow setup:

```clojure
(ns app.repl ;; here or in user.clj
  (:require [utility-belt.component.system :as system]
            [app.system]))


(let [sys-utils (system/setup-for-dev {;; NOTE: has to be a fully qualified symbol, the fn HAS TO return a map
                                       ;;       of components, not a SystemMap
                                       :component-map-fn 'app.system/development
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


For testing env (unit tests, end-to-end system tests etc), the setup is similar:

```clojure
(ns app.test-utils
  (:require [app.system]
            [utility-belt.component.system :as sys]))


(let [sys-utils (system/setup-for-test {;; NOTE: has to be a fully-qualified symbol. fn has to return
                                        ;;       a map, not a SystemMap!
                                        :component-map-fn 'app.system/test})
      {:keys [use-test-system get-system]} sys-utils]
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
                        ;; You can inject additional components or replace existing ones by passing
                        ;; an optional component map
                        {:extra-component :hello
                         :something-else (component.util/map->component {:name :something-else})})))

```
