# Introduction to promenade

Promenade helps to deal with program design oddities. It provides few first-class facilities to express the intent of
computation at a higher level. You can use Promenade in a new project or refactor an existing one to use it.


## Require namespaces

In Clojure:

```clojure
(require '[promenade.core :as prom])
```

In ClojureScript:

```clojure
(require '[promenade.core :as prom :include-macros true])
;;
;; or, its expanded form below
;;
(require        '[promenade.core :as prom])
(require-macros '[promenade.core :as prom])
```


## Dealing with success and failure with _Either_

### Expressing success and failure

Failure may be expressed as `(prom/fail failure)`, for example an order item that can not be fulfilled may be
expressed as follows:

```clojure
(prom/fail {:part-num  "HP-4196"
            :order-qty 2
            :stock-qty 1})
```

Any regular value that is not _Failure_ (predicate `prom/failure?`) is considered success.


### Handling success and failure outcomes

Consider an E-Commerce order placement scenario, where we go from a placed order through its fulfilment.

```clojure
(prom/either->> order-id                  ; begin with ID of the placed order
  fetch-order-details                     ; fetch order details (may succeed or fail)
  check-inventory                         ; check inventory levels of the items in order (may succeed or fail)
  [(cancel-order order-id) process-order] ; on failure cancel the order, else process the order
  [stock-replenish-init]                  ; if low stock led to cancelled order then initiate stock replenishment
  fulfil-order)                           ; if there were no failure then initiate order fulfilment
```

Here `either->>` is a thread-last (like `clojure.core/->>`) variant of acting on the result of the previous step.
A non-vector expression is treated as a success-handler, which is invoked if the previous step was a success. The
`order-id` we begin with is a success that becomes the last argument in the `(fetch-order-details order-id)`
expression, whose success is fed into `check-inventory`. If the outcome was a failure instead, then it bypasses
all subsequent success handlers until a failure handler is encountered.

A failure-handler may be specified in a vector form: `[failure-handler]` or `[failure-handler success-handler]`.
In above snippet, `(cancel-order order-id failure)` is invoked only when `fetch-order-details` or `check-inventory`
returns a failure. Once the `(cancel-order order-id failure)` step returns failure, `stock-replenish-init` is
called with that failure argument to take corrective action and return a failure again. If `check-inventory`
was successful then `process-order` is called, followed by `fulfil-order` on success.

A failure-handler may or may not recover from a _failure_, hence they may return either _failure_ or _success_.
However, a failure-handler is only invoked if the prior result is a _failure_. Specifically, in the above example,
`cancel-order` would deliberately keep the status as _failure_ so that the control can flow to the next step
`stock-replenish-init`.

#### The either-bind variants

The success and failure results are basically dealt with using the `prom/bind-either` function:

```clojure
(defn bind-either
  [success-or-failure success-handler]
  [success-or-failure failure-handler success-handler])
```

You may chain together several either-bind operations using macros `either->`, `either->>` and `either-as->`.


## Dealing with presence or absence of a value with _Maybe_

### Expressing a value or its absence thereof

Some times you may want to represent the absence of a value, expressed as `prom/nothing`. This is a special
value that may participate in other bind chains in Promenade. Any regular value that is not _Nothing_
(predicate `prom/nothing?`) is considered presence of a value.


### Handling presence or absence of a value

Let us take the contrived use case of fetching some data from a database that is fronted by a cache.

```clojure
(prom/maybe-> data-id  ; begin with data-id
  fetch-from-cache     ; attemp to fetch the data from cache, which may return a value or prom/nothing
  [fetch-from-db]      ; if not found in cache then fetch from DB, which may return a value or prom/nothing
  decompress)          ; if data was fetched then decompress it before returning to the caller
```

Here `maybe->` is a thread-first (like `clojure.core/->`) variant of acting on the result of previous step. A
non-vector expression is treated as a value-handler, which is invoked if the previous step returned a value.
The `data-id` we begin with is a value that becomes the first argument in the `(fetch-from-cache data-id)`
expression, whose result value is fed into `decompress`. If `fetch-from-cache` returns `prom/nothing` then it
attempts to fetch the data from database, which may return the value or `prom/nothing`.

#### The maybe-bind variants

The value or absence are basically dealt with using the `prom/bind-maybe` function:

```clojure
(defn bind-maybe
  [value-or-nothing value-handler]
  [value-or-nothing nothing-handler value-handler])
```

You may chain together several maybe-bind operations using macros `maybe->`, `maybe->>` and `maybe-as->`.


## Dealing with value or thrown exception with _Trial_

### Expressing a value or thrown exception

We regularly deal with pre-written and third-party code that may throw exceptions. We can capture exceptions
and return as _Thrown_ using the `prom/!` macro, e.g. `(prom/! (third-party-code))`, or construct one using
`prom/thrown` function. Any other regular value is considered not a _Thrown_ (predicate `prom/thrown?`).


### Handling value or thrown exception

Usually the handling or value vs thrown is similar to handling success vs failure (see above). The only change
is to capture exceptions and return a thrown result instead of returning a failure.

#### The trial-bind variants

The value or thrown are basically dealt with using the `prom/bind-trial` function:

```clojure
(defn bind-trial
  [value-or-thrown value-handler]
  [value-or-thrown thrown-handler value-handler])
```

You may chain together several thrown-bind operations using macros `trial->`, `trial->>` and `trial-as->`.


## Working with various result types

The various result types and bind variants we discussed above may be used together to perform composite operations.
For example, the code snippet below enhances upon the use-case we saw in success/failure handling.

```clojure
(-> order-id
  fetch-order-details-from-cache
  (prom/maybe->   [(fetch-order-details-from-db)])
  (prom/either->  check-inventory)
  (prom/either->> [(cancel-order order-id) process-order])
  (prom/either->  [stock-replenish-init])
  (prom/either->  fulfil-order)
  (prom/maybe->   [not-found-error])
  (prom/trial->   [generate-error]))
```


## Low level control during sequence operations

Often we may need to branch our decisions based on whether items in a sequence are failure/nothing/thrown/context or
ordinary values. The `branch` function is helpful in such cases. Consider the snippet below where we avoid processing
items that are not ordinary values:

```clojure
(def process-valid-item (prom/branch prom/free? process-item))

(map process-valid-item found-items)  ; process each item that is context-free (not a context)
```

Another use-case may be where we have to abort processing a sequence based on occurence of even a single error. The
following snippet shows such a use-case:

```clojure
(def context->reduced (prom/branch prom/context? reduced))

(reduce (fn [a x] (context->reduced (prom/either->> x
                                      process-valid-item
                                      (conj a))))
  [] found-items)
```


## Granular flexibility with matchers

The pipeline-threading macros we saw in the sections above are great for readability and linear use-cases. However,
at times the use-case is not linear and we need to match and refer intermediate results. For example, what if you
need to access the previous step's result and also one from three steps earlier? What if you also need to know if
an error was handled and recovered from in one of the previous steps? In such cases we can get to a lower level by
using one of the following match-bind macros.

### `mdo`

The `mdo` is similar to `clojure.core/do`, except that it returns the first encountered context value if any. An empty
body of code yields `nil`.

### `mlet`

The `mlet` macro is a lot like `clojure.core/let`, with the difference that it always binds to a matching result.
Whenever a non-matching result is found, `mlet` immediately returns it without proceeding any further. The following
snippet demonstrates the implicit matcher, which only proceeds on successful result - it aborts if at any point
there's a non-success result. An empty body of code yields `nil`.

```clojure
(prom/mlet [order (find-order-details order-id)    ; `order` binds to value if returned, `nothing` aborts mlet
            stock (check-inventory (:items order)) ; may fail, `stock` binds to success, failure aborts mlet
            f-ord (process-order order stock)]
  (fulfil-order f-ord))
```

You may also specify an exlicit matcher (e.g. `prom/mnothing` - notice the `m` prefix):

```clojure
(prom/mlet [cached (prom/mnothing (find-cached-order order-id) :absent) ; success aborts, not-found continues
            order  (find-order-details-from-db order-id)]               ; failure aborts, success continues
  (update-cache order-id order)                                         ; don't care call fails or succeeds
  order)
```

### `if-mlet` and `when-mlet`

We saw that `mlet` aborts on the first mismatch, but we often need to specify what to do on encountering a mismatch.
This is achieved using `if-mlet`, which is illustrated using the snippet below:

```clojure
(prom/if-mlet [order (find-order-details order-id)    ; `order` binds to value if returned, `nothing` -> else
               stock (check-inventory (:items order)) ; may fail, `stock` binds to success, failure -> else
               f-ord (process-order order stock)]
  (fulfil-order f-ord)
  (prom/fail {:module :order-processing
              :order-id order-id}))
```

Here we return a failure in the _else_ part of `if-mlet`. Now, if we see a similar snippet using `when-mlet` it
returns `nil` on non-match:

```clojure
(prom/when-mlet [order (find-order-details order-id)    ; `order` binds to value if returned, `nothing` aborts
                 stock (check-inventory (:items order)) ; may fail, `stock` binds to success, failure aborts
                 f-ord (process-order order stock)]
  (println "Fulfilling order:" order-id)
  (fulfil-order f-ord))
```

In `when-mlet`, an empty body of code yields `nil`.

### `cond-mlet`

Some times you may need to match several combinations of results, which may be done using `cond-mlet`. In the snippet
below we post and schedule a job and then try to determine the composite status:

```clojure
(let [job (post-job job-details)
      sch (schedule-job job)]
  (prom/cond-mlet
    [j job
     s sch]                 {:status :sucess
                             :job-id (:job-id sch)}
    [j job
     s (prom/mfailure sch)] {:status :partial-success}
    :else                   (prom/fail :failure)))
```
