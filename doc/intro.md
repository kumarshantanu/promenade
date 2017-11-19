# Introduction to promenade

Promenade helps to deal with program design oddities. It provides few first-class facilities to express the intent of
computation at a higher level. You can use Promenade in a new project or refactor an existing one to use it.


## Require namespaces

```clojure
(require '[promenade.core :as prom])
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

Any regular value that is not failure is considered success.


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
value that may participate in other bind chains in Promenade. Any regular value that is not nothing is considered
presence of a value.


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
and return as thrown using the `prom/!` macro, e.g. `(prom/! (third-party-code))`. Any other regular value is
considered not a thrown.


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


## Composing over various result types

The various result types and bind variants we discussed above may be composed together to perform composite operations.
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
