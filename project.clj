(defproject eigenbahn/grafana-simple-json-datasource-client "1.0.0"
  :description "Clojure Grafana Simple JSON Datasource client."
  :url "https://github.com/Eigenbahn/grafana-simple-json-datasource-client"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[clj-http "3.9.1"]
                 [metosin/jsonista "0.2.2"]
                 [tick "0.4.26-alpha"]]
  :profiles {:dev {:dependencies [[clojure.java-time "0.3.2"]
                                  [org.clojure/clojure "1.10.0"]]}})
