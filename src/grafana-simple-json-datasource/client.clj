(ns grafana-simple-json-datasource.client
  "Client library to the [Grafana Simple JSON Datasource](https://grafana.com/grafana/plugins/grafana-simple-json-datasource) API format."
  (:require [clj-http.client :as http-client]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [jsonista.core :as json]
            [tick.alpha.api :as t])
  (:use [slingshot.slingshot :only [try+]]))


(declare parse-map-output
         http-request format-inst-for-query)



;; DYNAMIC VARS

(def ^:dynamic content-level
  "Level of content returned for API response calls.
  Valid values:
  - `::http-client`: raw response from `clj-http.client`, good for debugging
  - `::body`: HTTP body parsing into a clojure data structure
  - `::data`: \"data\" part of the prometheus response
  - `::best`: only the most sensible data for each endpoint (default)"
  ::best)


(def ^:dynamic convert-result
  "If true, parse convert the results into more clojuresque data structures.
  Time series are converted into maps, their timestamps converted into inst and scalar values parsed.

  Default value is true.

  Gets ignored if [[content-level]] is `::http-client`"
  true)



;; QUERY: PING

(defn ping
  [conn]
  (binding [content-level ::http-client]
    (http-request http-client/get conn "/")
    nil))



;; QUERY: INDICATORS LIST

(defn search
  [conn & {:keys [target]}]
  (http-request http-client/post conn "/search"
                {:target (str target)}
                parse-map-output))



;; QUERY: METRICS

(defn metrics-body-parse-one [query-res]
  (let [
        ;; legends (get body "legends")
        timestamps (get query-res "times")
        series-list (get query-res "data")]
    (map
     (fn [series]
       (let [label (get series "label")
             unit (get series "unit")
             values (get series "data")
             ctx {:unit unit
                  :label label}]
         {ctx
          (into {}
                (map #(vector %1 %2) timestamps values))}))
     series-list)))

(defn metrics-body-parse [query-res-list]
  (mapcat metrics-body-parse-one query-res-list))

(defn query
  [conn targets from to
   & {:keys [interval limit]}]
  (let [targets (mapv #(assoc {:type "timeserie"} :target %) targets)
        from (format-inst-for-query from)
        to (format-inst-for-query to)]
    (http-request http-client/post conn "/query"
                  {:targets (str/join "," targets)
                   :range {:from from
                           :to   to}}
                  metrics-body-parse)))



;; QUERY: ANNOTATIONS (EVENTS)

(defn annotations
  [conn target from to]
  (http-request http-client/post conn "/annotations"
                {:annotation {:name target
                              :enable true
                              :query (str "#" target)}
                 :range {:from from
                         :to   to}}))



;; QUERY: META-DATA

(defn tag-keys
  [conn]
  (http-request http-client/post conn "/tag-keys"
                {}))

(defn tag-values
  [conn target]
  (http-request http-client/post conn "/tag-values"
                {:key target}))



;; UTILS: TIME

(defn format-inst
  "Convert INST into string according to FORMAT-STR.

  Unless specified, INST is assumed to be in UTC.
  An alternative TZ-OFFSET can be applied optionally."
  [inst format-str & [tz-offset]]
  (let [tz-offset (or tz-offset 0)
        t (-> inst
              t/instant
              (t/offset-by tz-offset))
        f (t/formatter format-str)]
    (t/format f t)))

(defn format-inst-for-query
  "Convert INST into a ISO 8601 string, as expected by the Grafana Simple JSON Datasource HTTP API."
  [inst]
  (format-inst inst :iso-zoned-date-time))



;; UTILS: REUSABLE PARSING

(defn parse-map-output
  [output]
  (into {} (map #(vector (get % "value") (get % "text")) output)))



;; UTILS: HTTP REQUESTS WRAPPERS

(defn http-request [http-method conn endpoint & [body parse-fn]]
(let [url (str (:url conn) endpoint)
      body-str (when body
                 (json/write-value-as-string body))
      client-params {:accept :json
                     :content-type (when body :json)
                     :body body-str}
      client-params (into {} (remove nil? client-params))
      raw-resp (http-method url client-params)]
  (if (= content-level ::http-client)
    raw-resp
    (let [body (-> raw-resp
                   :body
                   json/read-value)]
      (case content-level
        ::body
        body

        ::best
        (if parse-fn
          (parse-fn body)
          body)

        (throw (ex-info "Unexpected `content-level`" {:ex-type ::unexpected-content-level,
                                                      :input content-level})))))))
