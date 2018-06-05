(defproject promenade "0.5.2"
  :description "Take program design oddities in stride with Clojure/ClojureScript"
  :url "https://github.com/kumarshantanu/promenade"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :global-vars {*warn-on-reflection* true
                *assert* true
                *unchecked-math* :warn-on-boxed}
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.7.0"]]
                        :global-vars {*unchecked-math* :warn-on-boxed}}
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
             :c17 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c18 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c19 {:dependencies [[org.clojure/clojure "1.9.0"]]}
             :s09 {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.9.946"]]}
             :s10 {:dependencies [[org.clojure/clojure "1.9.0"]
                                  [org.clojure/clojurescript "1.10.238"]]}
             :dln {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :aliases {"clj-test"  ["with-profile" "c17:c18:c19" "test"]
            "cljs-test" ["with-profile" "cljs,s09:cljs,s10" "doo" "node" "once"]}
  :deploy-repositories [["releases" {:url "https://clojars.org" :creds :gpg}]])
