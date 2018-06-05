# Introduction to promenade

Promenade helps to deal with program design oddities. It provides few first-class facilities to express the intent of
computation at a higher level. You can use Promenade in a new project or refactor an existing one to use it.

## Require namespaces

```clojure
(require '[promenade.core :as prom])
```

### A Simple Example

Say you're trying to write a sequence of instructions where errors can occur:

```clojure
(let [v (lookup id)
      tv (when-not (error? v) (transform v))]
  (if tv
    (write-to-db tv)
    (log-error v)))
```

Such logic is relatively linear, and can often be compressed a bit if you
want to nil-pun (return `nil` on errors in functions):

```clojure
(some-> id
  lookup
  transform
  write-to-db)
```

but this has a number of weaknesses: you have to use nil, you lose the
error context (where did things go wrong?), and since the value being
carried through the sequence becomes `nil`, no possible error recovery
can be done in the sequence.

Sequences like these can be expressed in this library with more context-aware threading:

```clojure
(prom/either-> id
  lookup
  [(attempt-alternate-lookup id)]
  transform
  write-to-db
  [(log-error other-arg)])
```

In the example above the `id` is left-threaded through a sequence of operations. It works
*just* like normal threading, except for the presence of the vectors. A vector
is an `[error-handler optional-success-handler]` entry. It defines what to do
when there is a problem in the sequence.  Problem values never thread through the normal
functions, though error handler can recover and continue the sequence.

Thus, in the above example a failure from `lookup` will flow through
`attempt-alternate-lookup`, and if it fails `log-error`. If there is no
error or recovery is possible, then `transform` and `write-to-db` will execute.

## Context

This library is primarily concerned with allowing you to cleanly express a sequence of operations
where the context can indicate that error handling, recovery, or termination
of the sequence should occur.

The base library defines these contexts:

1. `Failure` - Represents an explicit, code-generated error value.
2. `Nothing` - Represents an explicit lack of a result.
3. `Thrown` - Represents a thrown exception.

There are support functions for creating and testing for these:

* `(prom/! form)` - Runs form. If an exception is thrown, returns a `Thrown` context instead. `deref` can be used to access the exception details.
* `(prom/thrown ex)` - A Thrown with an exception (`deref` can be used to get the exception)
* `prom/nothing` - The Nothing context
* `prom/failure` - A generic non-informative Failure
* `(prom/fail v)` - A Failure with some details (`deref` can be used to get the details)
* `(prom/failure? c)` - Returns true only if the given context c is a Failure
* `(prom/nothing? c)` - Returns true only if the given context c is Nothing
* `(prom/thrown? c)` - Returns true only if the given context c is a Thrown
* `(prom/context? c)` - Returns true if c is any kind of context (i.e. Nothing, Thrown, Failure). Returns false for all other value types.

These built-in contexts are based on marker protocols, and are therefore extensible.

### Binding Handlers with Context

In general we want to run a regular value through a sequence of functions that
expect no errors. The happy path. When there is some kind of error we want to be able
to bind some kind of error/recovery handler and either continue or finish.

This library defines a number of bind functions that encompass one step of this logic. They
look like this:

```clojure
(defn bind-CONTEXT-TYPE
  ([mval success-f] (if (context? mval)  ; Is it some kind of problem?
                      mval               ; Don't do anything to it...just pass it through
                      (success-f mval))) ; Do the happy-path operation
  ([mval failure-f success-f] (cond
                                (CONTEXT-TEST mval) (failure-f (deref mval)) ; Is it my kind of problem? If so, use the error handler
                                (context? mval) mval                         ; Is it someone else's problem? If so, pass it on
                                :otherwise      (success-f mval))))          ; Looks ok. Keep on the happy-path
```

where `CONTEXT-TYPE` indicates which kind of context you're interested in handling errors for,
and `CONTEXT-TEST` checks for that kind of context.

Chaining a sequence of these kind of bindings through threading leads to the decision logic
living in the binds and not your primary logic.

## Handling Explicit Failure

The success and failure results are basically dealt with using the `prom/bind-either` function:

```clojure
(defn bind-either
  ([mval success-f] (if (context? mval)
                      mval
                      (success-f mval)))
  ([mval failure-f success-f] (cond
                                (failure? mval) (failure-f (deref mval))
                                (context? mval) mval
                                :otherwise      (success-f mval))))
```

So, you could write a sequence of possibly failing calls with:

```
(-> order-id
  (prom/bind-either (fn [v] (fetch-order-details v)))
  (prom/bind-either (fn [v] (cancel-order v order-id)) (fn [v] (process-order v)))
  ...)
```

where the success functions are only called if the threaded value remains a value (and not any kind of error context).

NOTE: `bind-either` will only *handle* values or `Failure` error contexts. Any other context types simply pass through.

You never write these this way, though. It is much more natural to use the
built-in threading macros. You may chain together several bind-either operations using the macros
`either->`, `either->>` and `either-as->`.

### `either` Threading

The `either->` family of threading macros work very much like Clojure's
standard threading macros. The exception is that error handlers are placed
within vectors. The threading *does* apply to the first item in the vector,
but the second item is a function, and is passed the threaded value as the only argument.

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

A failure-handler may or may not recover from a _failure_, hence they may return either _failure_ (via `prom/failure`) or _success_
(any value that is not an error context).
However, a failure-handler is only invoked if the prior result is a _failure_. Specifically, in the above example,
`cancel-order` would deliberately return a _failure_ so that the control can flow to the next step
`stock-replenish-init`.

## Dealing with presence or absence of a value with _Maybe_

### Expressing a value or its absence thereof

Some times you may want to represent the absence of a value, expressed as `prom/nothing` (a Nothing context). This is a special
value that may participate in other bind chains in Promenade. Any regular value that is not _Nothing_
(predicate `prom/nothing?`) is considered presence of a value.

#### The bind-maybe variants

The value or absence are basically dealt with using the `prom/bind-maybe` function, which follows our
already established pattern:

```clojure
(defn bind-maybe
  ([mval just-f] (if (context? mval)
                   mval
                   (just-f mval)))
  ([mval nothing-f just-f] (cond
                             (nothing? mval) (nothing-f)
                             (context? mval) mval
                             :otherwise      (just-f mval))))
```

It has an "error-handling" 3-arity version that can call a no-arg nothing handler. If the value
is a normal value then it passes it through the `just-f` function.

Just like `bind-either`, this function ignores other kinds of incoming contexts (i.e. that are not `Nothing`).

You may chain together several maybe-bind operations using macros `maybe->`, `maybe->>` and `maybe-as->`.

### `maybe` Threading

`bind-maybe` is rarely used directly, just like `bind-either`. There are threading
macros that make it look just like the success/error handling case.

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

## Exceptions and `bind-trial`

The variant for exceptions is `bind-trial`. It is identical to the other two except that it
uses `thrown?` to detect the cases that should be error-handled.

The one major difference is that since exceptions normally unroll the stack we'll need
a way for functions to turn the thrown exception into a context value.
We can capture exceptions and return as _Thrown_ using the `prom/!` macro, e.g. `(prom/! (third-party-code))`, or construct one using
`(prom/thrown ex)` function.

For example:

```clojure
(defn get-from-db [id]
  (prom/!
    (let [v (jdbc/query ...)]
      v)))
```

will ensure that any thrown exceptions will be converted to a `Thrown` context
value and returned instead.

You may chain together several bind-trial operations using macros `trial->`, `trial->>` and `trial-as->`.

## Composition

Remember that each bind variant will pass an "unknown" context on to the next bind without acting on it. This
means you can freely compose the binds (and threading macros) to express complex chains of processing
that base their operation on the context.

The various result types and bind variants we discussed above may be used together to perform composite operations.

For example, the code snippet below enhances upon the use-case we saw in success/failure handling.

```clojure
(-> order-id
  fetch-order-details-from-cache
  (prom/maybe->   [(fetch-order-details-from-db)]) ; only error-handles Nothing. E.g. a cache miss
  (prom/either->  check-inventory)
  (prom/either->> [(cancel-order order-id) process-order]) ; Either cancel the order due to Failure, or process valid values
  (prom/either->  [stock-replenish-init]) ; Only if the prior step result was a Failure
  (prom/either->  fulfil-order)           ; Only if we still have a valid value
  (prom/maybe->   [not-found-error])      ; Handle the possiblity that Nothing flowed through from the fetch
  (prom/trial->   [generate-error]))      ; Handle any exceptions. Any of the above steps could have returned a Thrown
```

Remember that all of the behind-the-scene bind functions will flow "unknown" context through to the next. Thus it is perfectly
fine for *any* of the steps above to return whatever kind of context they want. Suppose you're written every one of the
functions above by wrapping the body with a `(prom/! ...)`. This means that any unexpected exception would naturally flow down
to the final `trial->` error handler.

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
