(defproject org.clojars.lukaszkorecki/utility-belt "2.5.0"
  :description "Some of the tools you'll ever need to fight crime and write Clojure stuffs"
  :license "MIT"
  :url "https://github.com/lukaszkorecki/utility-belt"
  :deploy-repositories {"clojars" {:sign-releases false
                                   :username :env/clojars_username
                                   :password :env/clojars_password}}

  :dependencies [[org.clojure/clojure "1.12.4"]
                 [com.stuartsierra/component "1.2.0"]
                 [org.clojure/tools.logging "1.3.0"]
                 [cheshire "6.1.0"]]

  :global-vars {*warn-on-reflection* true}
  :profiles {:dev
             {:dependencies [[ch.qos.logback/logback-classic "1.5.22"]
                             [ring/ring-jetty-adapter "1.15.3"]]}})
