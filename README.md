# grafana-simple-json-datasource-client

Client library wrapper around [Grafana Simple JSON Datasource](https://grafana.com/grafana/plugins/grafana-simple-json-datasource) HTTP API format.

It provides the extraction of only relevant data from the responses and, for endpoints returning time series, automated response conversion to a more Clojure-friendly format.


## Installation

Add the following dependency to your `project.clj` file:

    [eigenbahn/grafana-simple-json-datasource-client "1.0.0"]


## General Usage

#### Connection

All methods take a connection `conn` as a first argument. It's just a simple map in the form:

```clojure
{:url "<grafana_json_datasource_url>"}
```

So to connect to a localhost instance:

```clojure
(def simple-json-conn {:url "http://localhost"})
```


#### Time instants

Arguments corresponding to a time instant (and by extension the boundaries of a time range) are expected to be Clojure instant objects.


#### Optional arguments

All optional arguments are keyword arguments.


#### Error / Exception handling

Errors translate to HTTP error codes, throwing exceptions.

As we are relying on [clj-http.client](https://github.com/dakrone/clj-http), those exceptions are [Slingshot](https://github.com/scgilardi/slingshot) Stones. Refer to [this section](https://github.com/dakrone/clj-http#exceptions) to see how to handle them.


#### Result extraction & conversion

By default, only the most sensible data in the API response is returned by each function.

This can be tweaked by adjusting the value of dynamic var `content-level`:

- `::http-client`: raw value from `clj-http.client`, good for debugging
- `::body`: HTTP body parsing into a clojure data structure
- `::best`: only the most sensible data for each endpoint (default)

Likewise, we by default convert time series into more clojure-friendly data structures (maps) with timestamps converted to `inst` and used as keys and values parsed when scalar instead of just strings. This can be disabled by setting `convert-result` to false.


## Example usage

#### Ensure the datasource is alive

```clojure
(ping {:url "http://localhost"})
;; -> nil
```

Throws a Slingshot Stone (exception) on error.

NB: The accuracy of the reported HTTP status depends on the quality of the datasource implementations.

Some will perform numerous healthchecks, others will blindly respond `200 OK`.


#### List all available indicators / metrics

```clojure
(search {:url "http://localhost"})
;; -> {"<unique_id_1>" "nb-of-babies"
;;     ;; [...]
;;     "<unique_id_n>" "pct-of-cows"}
```

#### List indicators matching word

```clojure
(search {:url "http://localhost"} :target "cow")
;; -> {"<unique_id_m>" "nb-of-cow-species"
;;     "<unique_id_n>" "pct-of-cows"}
```

NB: Most datasource implementations will match any indicator containing `:target`, others might implement more regexp-like syntaxes.


#### Query indicators values

```clojure
(require '[tick.alpha.api :as t])

(let [from (t/- (t/now) (t/new-duration 10 :hours))
      to   (t/now)]
  (query {:url "http://localhost"} ["<unique_id_n>"] from to))
;; -> '({
;;       ;; context for 1rst series
;;       {:unit "percent", :label "cows-with-horns"}
;;       ;; values for 1rst series
;;       {#inst "2020-09-14T12:47:02.000000000-00:00" 0.00457012,
;;        #inst "2020-09-14T12:17:10.000000000-00:00" 0.00456644,
;;        ...
;;        #inst "2020-09-14T14:13:36.000000000-00:00" 0.004636848}
;;       })
```
