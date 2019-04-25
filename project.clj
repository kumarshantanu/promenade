(defproject promenade "0.7.2"
  :description "Take program design oddities in stride with Clojure/ClojureScript"
  :url "https://github.com/kumarshantanu/promenade"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :global-vars {*warn-on-reflection* true
                *assert* true
                *unchecked-math* :warn-on-boxed}
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :cljs {:plugins   [[lein-cljsbuild "1.1.7"]
                                [lein-doo "0.1.10"]]
                    :doo       {:build "test"}
                    :cljsbuild {:builds {:test {:source-paths ["src" "test" "test-doo"]
                                                :compiler {:main          promenade.runner
                                                           :output-dir    "target/out"
                                                           :output-to     "target/test/core.js"
                                                           :target        :nodejs
                                                           :optimizations :none
                                                           :source-map    true
                                                           :pretty-print  true}}}}
                    :prep-tasks [["cljsbuild" "once"]]
                    :hooks      [leiningen.cljsbuild]}
             :ctn {:dependencies [[org.clojure/tools.nrepl "0.2.13"]]}
             :c07 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c08 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c09 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :c10 {:dependencies [[org.clojure/clojure "1.10.0"]]}
             :s09 {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.9.946"]]}
             :s10 {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.10.339"]]}
             :dln {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"clj-test"  ["with-profile" "c07:c08:c09:c10" "test"]
            "cljs-test" ["with-profile" "cljs,s09:cljs,s10" "doo" "node" "once"]}
  :deploy-repositories [["releases" {:url "https://clojars.org" :creds :gpg}]])
