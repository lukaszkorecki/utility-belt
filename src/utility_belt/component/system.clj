(ns utility-belt.component.system
  (:require
   [clojure.tools.logging :as log]
   [clojure.string :as str]
   [com.stuartsierra.component :as component]
   [utility-belt.compile :as compile]
   [utility-belt.component :as util.component]
   [utility-belt.lifecycle :as lifecycle]
   [utility-belt.type :as type]))

(defn fn-sym->ns-sym
  "Given a fully qualified symbol for a fn, returns its namespace symbol"
  [fn-sym]
  (-> fn-sym
      resolve
      meta
      :ns
      clojure.lang.Namespace/.getName
      symbol))

(defn setup-for-production
  "Simplifies wiring up the system as the app entry point, with a graceful shutdown.
  This is helpful to reduce boilerplate in the main namespace.

  Args:

  - `store` - an atom to store the system in once it's started.
              You can later refer to it by derefing it in your REPL session
  - `service` - name of the service, if not provided, will infer it from current namespace
  - `component-map-fn` - a function that returns the system map, **NOT** an instance of `SystemMap`


  Example:
  ```clojure
  (ns some.api.core
    (:require [some.api.system :as system]
              [utility-belt.component :as component]))

  (def app (atom nil))

  (defn -main [& _args]
    (component/init-app-system {:store app
                                :component-map-fn  system/production)))
  ```
  "
  [{:keys [store service component-map-fn]}]
  {:pre [(type/atom? store)
         (or (fn? component-map-fn)
             (qualified-symbol? component-map-fn))]}
  (let [svc-name (str (or service
                          (first (str/split (str *ns*) #"\."))))
        ;; support both the fn or a fully qual symbol
        component-map-fn (if (fn? component-map-fn)
                           component-map-fn
                           (fn-sym->ns-sym component-map-fn))]
    (reset! store (component/start-system (util.component/map->system (component-map-fn))))
    (lifecycle/add-shutdown-hook :shutdown-system (fn stop! []
                                                    (log/infof "stopping %s" svc-name)
                                                    (swap! store
                                                           #(when % (component/stop-system %))))))

  store)

;; ----- dev mode -----

(compile/compile-if (do
                      (require '[clojure.tools.namespace.repl])
                      true)
                    ;; we can refresh
                    (do
                      (def tools-ns-available? true)
                      (def refresh
                        (requiring-resolve 'clojure.tools.namespace.repl/refresh))
                      (def disable-reload!
                        (requiring-resolve 'clojure.tools.namespace.repl/disable-reload!)))

                    ;; we can't refresh, provide no-ops
                    (do
                      (def tools-ns-available? false)
                      (def refresh identity)
                      (def disable-reload! identity)))

(defn setup-for-dev
  "Sets up a dev-system namespace which will provide start, stop and getter function as well
  as hold on to the started system.
  This makes it easier to work with component as part of a dev setup (with easy reloading)
  as well as tests, where systems can be controlled in a programatic way without
  affecting how normal 'production' system is started/stopped/restarted.

  Returns a map with keys :start-system, :stop-system, :get-system, :restart-system, you can
  destructure it and assing to vars in your namespace.

  The reason why this exists is to make sure that code reloaded via `tools.namespace.repl` is
  truly reloadable, by ensuring all satate is nuked before reloading.

  This is also more robust solution if you want to keep your dev system running while running tests for example.
  There are some caveats but it should work for the most part, as opposed to `component.repl` approach.

  Args:
  - `ns-to-attach-to`: namespace to attach the dev-system to, by default it attaches to the current namespace (`*ns*`)
  - `component-map-fn`: a symbol pointing to a function that returns a **map of components** NOT an instance of `component/SystemMap`
  - `reloadable?`: boolean, if true, will enable reloading of the system map function and the system itself, default is false
                  When true, it requires `clojure.tools.namespace` to be present in the classpath, otherwise it will throw an error.
  - `debug?`: boolean, if true will print start/stop/no-op messages
  "
  [{:keys [ns-to-attach-to
           component-map-fn
           reloadable?
           debug?]}]
  {:pre [(qualified-symbol? component-map-fn)
         (or (nil? ns-to-attach-to)
             (symbol? ns-to-attach-to))]}
  (when reloadable?
    (assert tools-ns-available? "clojure.tools.namespace.repl is not available, cannot enable code reloading!")
    (disable-reload! *ns*)
    (when ns-to-attach-to
      (disable-reload! (find-ns ns-to-attach-to))))
  (let [;; by default, attaches itself to the current namespace
        sys-ns (str (or ns-to-attach-to *ns*) ".dev-sys")
        sys-ns-sym (symbol sys-ns)
        ;; get the ns symbol, for reloading
        component-map-fn-ns-sym (fn-sym->ns-sym component-map-fn)]
    (when-not component-map-fn-ns-sym
      (throw (ex-info "not a valid symbol for system map fn"
                      {:component-map-fn component-map-fn})))
    ;; now return all the functions that can be 'mounted' into system namespace
    {:start-system (fn start-dev-sys'
                     ([]
                      (start-dev-sys' {}))
                     ([additional-component-map]
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
                                                               ;; reload the namespace which provides component map, this will ensure
                                                               ;; that the reloaded code is using latest version
                                                               (require component-map-fn-ns-sym :reload)
                                                               (when reloadable?
                                                                 (refresh))
                                                               ;; now start the system and store it for later
                                                               (swap! (var-get sys-atom')
                                                                      (fn [sys]
                                                                        (if sys
                                                                          (do
                                                                            (when debug?
                                                                              (log/debugf "System already running in %s" sys-ns-sym))
                                                                            sys)
                                                                          (do
                                                                            (when debug?
                                                                              (log/debugf "Starting system in %s" sys-ns-sym))
                                                                            (-> ((var-get (resolve component-map-fn)))
                                                                                (merge additional-component-map)
                                                                                component/map->SystemMap
                                                                                component/start)))))))

                            ;; define simple stop-fn, it will not be used here
                            ;; but called by a separate stop fn wrapper
                            _stop' (intern sys-ns-sym 'stop (fn sys-stop' []
                                                              (swap! (var-get sys-atom')
                                                                     (fn [sys]
                                                                       (if sys
                                                                         (do
                                                                           (when debug?
                                                                             (log/debugf "Stopping system in %s" sys-ns-sym))
                                                                           (component/stop sys)
                                                                           nil)
                                                                         (when debug?
                                                                           (log/debugf "System not running in %s" sys-ns-sym)))))))]

                        (start'))))

     :stop-system (fn stop-dev-system' []
                    (if-let [stop' (ns-resolve sys-ns-sym 'stop)]
                      (stop')
                      (when debug?
                        (log/error "ERROR: system not started, run start it first"))))

     :get-system (fn get-dev-system' []
                   @(var-get (resolve (symbol sys-ns "system"))))}))

(defn setup-for-test
  "Like `setup-for-dev` but also provides a `use-test-system` function that will start the system as well and doesn't enable reloading"
  [{:keys [ns-to-attach-to component-map-fn]}]

  (let [{:keys [start-system stop-system] :as sys-fns} (setup-for-dev {:component-map-fn component-map-fn
                                                                       :ns-to-attach-to ns-to-attach-to
                                                                       :reloadable? false})]

    (assoc sys-fns
           :use-test-system (fn use-test-system'
                              ([test-fn]
                               (use-test-system' test-fn {}))
                              ([test-fn additional-component-map]
                               (try
                                 (start-system additional-component-map)
                                 (test-fn)
                                 (finally
                                   (stop-system))))))))

#_(comment
    (in-ns 'app.repl)

    (let [{:keys [start-system stop-system get-system]} (setup-for-dev {:component-map-fn 'app.system/development
                                                                        :reloadable? true})]

      (def system get-system)
      (def start start-system)
      (def restart (fn [] (stop-system) (start-system)))
      (def stop stop-system))

    (in-ns 'app.tests)
    (let [{:keys [get-system use-test-system]} (setup-for-test {:component-map-fn 'app.system/test})]

      (def system get-system)

      (def with-test-system use-test-system))

    ;; simple use case:
    (clojure.test/use-fixtures :once with-test-system)
    ;; with extra components:
    (clojure.test/use-fixtures :once (fn [test]
                                       (with-test-system test {:extra-component :extra}))))
