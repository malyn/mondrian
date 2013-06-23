(defproject mondrian "0.1.1-SNAPSHOT"
  :description "Framework for creating browser-based interactive artwork in ClojureScript"
  :url "https://github.com/malyn/mondrian"
  :license {:name "BSD"
            :url "http://www.opensource.org/licenses/BSD-3-Clause"
            :distribution :repo}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.webjars/jquery "1.9.1"]
                 [org.webjars/jquery-ui "1.10.2-1"]
                 [org.webjars/jquery-ui-themes "1.10.0"]
                 [prismatic/dommy "0.1.1"]
                 [rm-hull/monet "0.1.7"]]

  :min-lein-version "2.1.2"
  :plugins [[lein-cljsbuild "0.3.2"]]
  :hooks [leiningen.cljsbuild]

  :source-paths ["src/clj"]

  :cljsbuild {:builds {:main {:source-paths ["src/cljs"]
                              :jar true}}})
