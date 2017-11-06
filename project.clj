(defproject promenade "0.1.0-SNAPSHOT"
  :description "Take outcome variation in stride in Clojure"
  :url "https://github.com/kumarshantanu/promenade"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies []
  :global-vars {*warn-on-reflection* true
                *assert* true
                *unchecked-math* :warn-on-boxed}
  :profiles {:provided {:dependencies [[org.clojure/clojure "1.7.0"]]
                        :global-vars {*unchecked-math* :warn-on-boxed}}
             :c17 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :c18 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :c19 {:dependencies [[org.clojure/clojure "1.9.0-beta4"]]}
             :dln {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :deploy-repositories [["releases" {:url "https://clojars.org" :creds :gpg}]])
