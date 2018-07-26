;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.core
  "Success/Failure (known as Either) are the dual of each other with respect to an operation result. Similarly, other
  duals include Just/Nothing (known as Maybe) and Result/Exception (known as Trial) on related perspectives. This
  namespace provides unified, standalone and composable mechanism to represent and process such operation outcomes."
  #?(:cljs (:require-macros promenade.core))
  (:require
    [promenade.internal :as i]
    [promenade.type     :as t]))


;; ----- Context utility -----


;;~~~~~~~~~~~~~~~~~~~~
;; Context predicates

(defn failure? [x] "Return true if argument is a Failure, false otherwise." (satisfies? t/IFailure x))
(defn nothing? [x] "Return true if argument is a Nothing, false otherwise." (satisfies? t/INothing x))
(defn thrown?  [x] "Return true if argument is a Thrown, false otherwise."  (satisfies? t/IThrown x))
(defn context? [x] "Return true if argument is a Context, false otherwise." (satisfies? t/IContext x))
(defn free?    [x] "Return true if argument is Context-free (not a Context), false otherwise." (not (context? x)))


;;~~~~~~~~~~~~~~~~~~~
;; Terminal contexts

(def failure (i/->Failure nil))
(def nothing (i/->Nothing))


;;~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
;; Infer nil as terminal context

(def nil->failure (fnil identity failure))
(def nil->nothing (fnil identity nothing))


;;~~~~~~~~~~~~~~~~~~~~~~~
;; Make terminal context

(defn fail
  "Turn given argument into 'failure' unless it is already a context."
  ([x] (cond
         (failure? x) x
         (context? x) (throw (ex-info "Cannot derive failure from other context" {:context x}))
         :otherwise   (i/->Failure x)))
  ([] failure))


(defmacro ex-fail
  "Catch ExceptionInfo and turn its ex-data into 'failure'."
  [expr]
  `(try ~expr
     (catch ExceptionInfo ex#
       (i/->Failure (ex-data ex#)))))


(defn void
  "Turn given argument into 'nothing' unless it is already a context."
  ([x] (cond
         (nothing? x) x
         (context? x) (throw (ex-info "Cannot turn other context into nothing" {:context x}))
         :otherwise   nothing))
  ([] nothing))


(defn thrown
  "Turn given argument into a 'thrown' unless it is already a context."
  [x]
  (cond
    (thrown? x)  x
    (context? x) (throw (ex-info "Cannot derive thrown from other context" {:context x}))
    :otherwise   (i/->Thrown x)))


(defmacro !
  "Evaluate given form and return it; on exception return the exception as thrown context."
  ([x]
    ;; In CLJS `defmacro` is called by ClojureJVM, hence reader conditionals always choose :clj -
    ;; so we discover the environment using a hack (:ns &env), which returns truthy for CLJS.
    ;; Reference: https://groups.google.com/forum/#!topic/clojure/DvIxYnO1QLQ
    ;; Reference: https://dev.clojure.org/jira/browse/CLJ-1750
    `(! ~(if (:ns &env) `js/Error `Exception) ~x))
  ([catch-class x] (let [catch-expr    (fn [clazz]
                                         (i/expected symbol? "exception class name" clazz)
                                         `(catch ~clazz ex# (i/->Thrown ex#)))
                         catch-clauses (cond
                                         (symbol? catch-class) [(catch-expr catch-class)]
                                         (vector? catch-class) (map catch-expr catch-class)
                                         :otherwise            (i/expected "class-name or vector of class-names"
                                                                 catch-class))]
                     `(try ~x
                        ~@catch-clauses))))


(defmacro !wrap
  "Wrap given function such that on exception it returns the exception as a thrown context."
  ([f]
    `(fn [& args#]
       (! (apply ~f args#))))
  ([catch-class f]
    `(fn [& args#]
       (! ~catch-class (apply ~f args#)))))


;;~~~~~~~~~~~~~~~~
;; Unwrap context

(defn deref-context
  "Deref argument if it is a context, return as it is otherwise."
  ([x] (if (context? x)
         (if (i/derefable? x)
           (deref x)
           (throw (ex-info "Context does not support deref" {:context x})))
         x))
  ([x default] (if (context? x)
                 (if (i/derefable? x)
                   (deref x)
                   default)
                 x)))


;; ----- Bind -----


(defn branch
  "Given an optional fallback fn (fn [x]) (default: clojure.core/identity), compose it with branch execution as per
  predicate. You may use this to compose a branching pipeline:
  (-> identity
    (branch pred f)
    (branch pred2 f2))"
  ([pred f]
    (branch identity pred f))
  ([fallback pred f]
    (fn [mval]
      (if (pred mval)
        (f mval)
        (fallback mval)))))


(defn bind-either
  "Given a context mval (success or failure) bind it with a function of respective type, i.e. success-f or failure-f.
  See:
    either->
    either->>
    either-as->
    bind-maybe
    bind-trial"
  ([mval success-f] (if (context? mval)
                      mval
                      (success-f mval)))
  ([mval failure-f success-f] (cond
                                (failure? mval) (failure-f (deref mval))
                                (context? mval) mval
                                :otherwise      (success-f mval))))


(defn bind-maybe
  "Given a context mval (just or nothing) bind it with a function of respective type, i.e. just-f or nothing-f.
  See:
    maybe->
    maybe->>
    maybe-as->
    bind-either
    bind-trial"
  ([mval just-f] (if (context? mval)
                   mval
                   (just-f mval)))
  ([mval nothing-f just-f] (cond
                             (nothing? mval) (nothing-f)
                             (context? mval) mval
                             :otherwise      (just-f mval))))


(defn bind-trial
  "Given a context mval (value or exception) bind it with a function of respective type, i.e. result-f or thrown-f.
  See:
    trial->
    trial->>
    trial-as->
    bind-either
    bind-maybe"
  ([mval result-f] (if (context? mval)
                     mval
                     (result-f mval)))
  ([mval thrown-f result-f] (cond
                              (thrown? mval)  (thrown-f (deref mval))
                              (context? mval) mval
                              :otherwise      (result-f mval))))


;; ----- Pipeline macros -----


(defmacro either->
  "Thread-first expansion using bind-either. A vector form of one element [x] is applied only to 'failure' leaving
  'success' intact.
  Example usage                           Expanded as
  -------------                         | -----------
  (either-> (place-order)               | (-> (place-order)
    (check-inventory :foo)              |   (bind-either (fn [x] (check-inventory x :foo)))
    [(cancel-order :bar) process-order] |   (bind-either (fn [x] (cancel-order x :bar)) process-order)
    fulfil-order)                       |   (bind-either fulfil-order))
  See:
    either->>
    either-as->
    maybe->
    trial->"
  [x & forms]
  `(-> ~x
     ~@(map #(i/bind-expr 'promenade.core/bind-either i/expand-> i/expand-> %) forms)))


(defmacro either->>
  "Thread-last expansion using bind-either. A vector form of one element [x] is applied only to 'failure' leaving
  'success' intact.
  Example usage                           Expanded as
  -------------                         | -----------
  (either->> (place-order)              | (-> (place-order)
    (check-inventory :foo)              |   (bind-either (fn [x] (check-inventory :foo x)))
    [(cancel-order :bar) process-order] |   (bind-either (fn [x] (cancel-order :bar x)) process-order)
    fulfil-order)                       |   (bind-either fulfil-order))
  See:
    either->
    either-as->
    maybe->>
    trial->>"
  [x & forms]
  `(-> ~x
     ~@(map #(i/bind-expr 'promenade.core/bind-either i/expand->> i/expand->> %)
         forms)))


(defmacro either-as->
  "Thread-anywhere expansion using bind-either. A vector form of one element [x] is applied only to 'failure' leaving
  'success' intact.
  Example usage                             Expanded as
  -------------                           | -----------
  (either-as-> (place-order) $            | (-> (place-order)
    (check-inventory $ :foo)              |   (bind-either (fn [$] (check-inventory $ :foo)))
    [(cancel-order :bar $) process-order] |   (bind-either (fn [$] (cancel-order :bar $)) (fn [_] process-order))
    (fulfil-order $))                     |   (bind-either (fn [$] (fulfil-order $))))
  See:
    either->
    either->>
    maybe-as->
    trial-as->"
  [expr name & forms]
  `(-> ~expr
     ~@(map #(i/bind-expr 'promenade.core/bind-either (partial i/expand-as-> name) (partial i/expand-as-> name) %)
         forms)))


(defmacro maybe->
  "Thread-first expansion using bind-maybe. A vector form of one element [x] is applied only to 'nothing' leaving
  'just' intact.
  Example usage                           Expanded as
  -------------                         | -----------
  (maybe-> (find-order)                 | (-> (find-order)
    (check-inventory :foo)              |   (bind-maybe (fn [x] (check-inventory x :foo)))
    [(cancel-order :bar) process-order] |   (bind-maybe (fn [] (cancel-order :bar)) process-order)
    fulfil-order)                       |   (bind-maybe fulfil-order))
  See:
    maybe->>
    maybe-as->
    either->
    trial->"
  [x & forms]
  `(-> ~x
     ~@(map #(i/bind-expr 'promenade.core/bind-maybe i/expand-nothing i/expand-> %)
         forms)))


(defmacro maybe->>
  "Thread-last expansion using bind-maybe. A vector form of one element [x] is applied only to 'nothing' leaving
  'just' intact.
  Example usage                           Expanded as
  -------------                         | -----------
  (maybe->> (find-order)                | (-> (find-order)
    (check-inventory :foo)              |   (bind-maybe (fn [x] (check-inventory :foo x)))
    [(cancel-order :bar) process-order] |   (bind-maybe (fn [] (cancel-order :bar)) process-order)
    fulfil-order)                       |   (bind-maybe fulfil-order))
  See:
    either->
    either-as->
    maybe->>
    trial->>"
  [x & forms]
  `(-> ~x
     ~@(map #(i/bind-expr 'promenade.core/bind-maybe i/expand-nothing i/expand->> %)
         forms)))


(defmacro maybe-as->
  "Thread-anywhere expansion using bind-maybe. A vector form of one element [x] is applied only to 'nothing' leaving
  'just' intact.
  Example usage                             Expanded as
  -------------                         | -----------
  (maybe-as-> (find-order) $            | (-> (find-order)
    (check-inventory $ :foo)            |   (bind-maybe (fn [$] (check-inventory $ :foo)))
    [(cancel-order :bar) process-order] |   (bind-maybe (fn [] (cancel-order :bar)) (fn [_] process-order))
    (fulfil-order $))                   |   (bind-maybe (fn [$] (fulfil-order $))))
  See:
    maybe->
    maybe->>
    either-as->
    trial-as->"
  [expr name & forms]
  `(-> ~expr
     ~@(map #(i/bind-expr 'promenade.core/bind-maybe i/expand-nothing (partial i/expand-as-> name) %)
         forms)))


(defmacro trial->
  "Thread-first expansion using bind-trial. A vector form of one element [x] is applied only to 'thrown' leaving
  'result' intact.
  Example usage                           Expanded as
  -------------                         | -----------
  (trial-> (place-order)                | (-> (place-order)
    (check-inventory :foo)              |   (bind-trial (fn [x] (check-inventory x :foo)))
    [(cancel-order :bar) process-order] |   (bind-trial (fn [x] (cancel-order x :bar)) process-order)
    fulfil-order)                       |   (bind-trial fulfil-order))
  See:
    trial->>
    trial-as->
    either->
    maybe->"
  [x & forms]
  `(-> ~x
     ~@(map #(i/bind-expr 'promenade.core/bind-trial i/expand-> i/expand-> %)
         forms)))


(defmacro trial->>
  "Thread-last expansion using bind-trial. A vector form of one element [x] is applied only to 'thrown' leaving
  'result' intact.
  Example usage                           Expanded as
  -------------                         | -----------
  (either->> (place-order)              | (-> (place-order)
    (check-inventory :foo)              |   (bind-trial (fn [x] (check-inventory :foo x)))
    [(cancel-order :bar) process-order] |   (bind-trial (fn [x] (cancel-order :bar x)) process-order)
    fulfil-order)                       |   (bind-trial fulfil-order))
  See:
    trial->
    trial-as->
    either->>
    maybe->>"
  [x & forms]
  `(-> ~x
     ~@(map #(i/bind-expr 'promenade.core/bind-trial i/expand->> i/expand->> %)
         forms)))


(defmacro trial-as->
  "Thread-anywhere expansion using bind-trial. A vector form of one element [x] is applied only to 'thrown' leaving
  'result' intact.
  Example usage                             Expanded as
  -------------                           | -----------
  (either-as-> (place-order) $            | (-> (place-order)
    (check-inventory $ :foo)              |   (bind-trial (fn [$] (check-inventory $ :foo)))
    [(cancel-order :bar $) process-order] |   (bind-trial (fn [$] (cancel-order :bar $)) (fn [_] process-order))
    (fulfil-order $))                     |   (bind-trial (fn [$] (fulfil-order $))))
  See:
    trial->
    trial->>
    either-as->
    maybe-as->"
  [expr name & forms]
  `(-> ~expr
     ~@(map #(i/bind-expr 'promenade.core/bind-trial (partial i/expand-as-> name) (partial i/expand-as-> name) %)
         forms)))


;; ----- Match binding ----


(defn mfailure
  "Match argument as Failure, returning a match-result.
  See:
    mnothing
    mthrown
    mlet
    if-mlet
    when-mlet
    cond-mlet"
  ([x]
    (if (failure? x)
      (i/->Match true (deref x))
      (i/->Match false x)))
  ([x default]
    (if (failure? x)
      (i/->Match true default)
      (i/->Match false x))))


(defn mnothing
  "Match argument as Nothing, returning a match-result.
  See:
    mfailure
    mthrown
    mlet
    if-mlet
    when-mlet
    cond-mlet"
  [x value]
  (if (nothing? x)
    (i/->Match true value)
    (i/->Match false x)))


(defn mthrown
  "Match argument as Thrown, returning a match-result.
  See:
    mfailure
    mnothing
    mlet
    if-mlet
    when-mlet
    cond-mlet"
  ([x]
    (if (thrown? x)
      (i/->Match true (deref x))
      (i/->Match false x)))
  ([x default]
    (if (thrown? x)
      (i/->Match true default)
      (i/->Match false x))))


(defmacro mdo
  "Evaluate body of code such that any context is returned as soon as it is encountered unexpectedly. However, context
  matches are ignored. Return nil for empty body."
  [& body]
  (if (empty? body)
    `nil
    (let [[expr & more] body]
      (with-meta
        `(let [val# ~expr]
           (if (context? val#)
             val#
             (if ~(empty? more)
               val#
               (mdo ~@more))))
        (meta expr)))))


(defmacro mlet
  "Bind symbols in the binding forms to their respective matching context and evaluate body of code in the lexical
  scope. If a non-matching context is encountered, return it without proceeding any further.
  See:
    mfailure
    mnothing
    mthrown
    if-mlet
    when-mlet
    cond-mlet"
  [bindings & body]
  (i/expected vector? "vector of binding forms" bindings)
  (when (odd? (count bindings))
    (i/expected "even number of binding forms" bindings))
  (if (empty? bindings)
    (with-meta `(mdo ~@body)
      (meta body))
    (let [[lhs rhs & more] bindings
          restof-expansion (with-meta (if (seq more)
                                        `(mlet [~@more] (mdo ~@body))
                                        `(mdo ~@body))
                             (meta body))]
      (with-meta `(let [rhs# ~rhs
                        mi?# (i/match-instance? rhs#)]
                    (if (and mi?# (not (:match? rhs#)))
                      (:value rhs#)
                      (if (context? rhs#)
                        rhs#
                        (let [~lhs (if mi?# (:value rhs#) rhs#)]
                          ~restof-expansion))))
        (or (meta rhs) (meta lhs))))))


(defmacro if-mlet
  "Bind symbols in the binding forms to their respective matching context and evaluate `then` form in the lexical
  scope. If a non-matching context is encountered, evaluate the `else` form independent of the binding context, or
  return a promenade.type.INothing instance when `else` is unspecified.
  See:
    mfailure
    mnothing
    mthrown
    mlet
    when-mlet
    cond-mlet"
  ([bindings then]
    `(if-mlet ~bindings ~then nothing))
  ([bindings then else]
    (i/expected vector? "vector of binding forms" bindings)
    (when (odd? (count bindings))
      (i/expected "even number of binding forms" bindings))
    `(let [[match?# result#] (i/if-then ~bindings ~then)]
       (if match?#
         result#
         ~else))))


(defmacro when-mlet
  "Bind symbols in the binding forms to their respective matching context and evaluate th body of code in the lexical
  scope. If a non-matching context is encountered, return nil.
  See:
    mfailure
    mnothing
    mthrown
    mlet
    if-mlet
    cond-mlet"
  [bindings & body]
  `(if-mlet ~bindings
     (mdo ~@body)
     nil))


(defmacro cond-mlet
  "Given a set of match-bindings/expression pairs, match each binding vector one by one - upon full match evaluate
  respective expression in the lexical scope and return the result without proceeding any further. When a binding
  form is not a vector, evaluate it like an expression and a truthy value triggers evaluating respective expression.
  When no match is found, return a promenade.type.INothing instance."
  [& clauses]
  (when (odd? (count clauses))
    (i/expected "even number of test/expr clauses" clauses))
  (if (empty? clauses)
    `nothing
    (let [[test expr & more] clauses]
      (if (vector? test)
        `(if-mlet ~test
           ~expr
           (cond-mlet ~@more))
        `(if ~test
           ~expr
           (cond-mlet ~@more))))))


;; ----- Support for reducing functions -----


(defmacro refn
  "Given `accumulator` and `each` arguments placeholder and an S-expression to evaluate, return a reducing function
  (fn [accumulator each]) that bails out on encountering a context.
  Example: (reduce (refn [vs x] (if (odd? x) (conj vs (* x 2)) vs)) [] coll)"
  ([argvec expr]
    `(refn context? ~argvec ~expr))
  ([context-pred argvec expr]
    (i/expected vector? "argument vector" argvec)
    (i/expected #(= 2 (count %)) "2-argument vector" argvec)
    (let [[acc each] argvec]
      `(fn
         ([] (i/throw-unsupported "Unsupported arity"))
         ([~acc ~each] (let [result# ~expr]
                         (if (~context-pred result#)
                           (reduced result#)
                           result#)))))))


(defmacro rewrap
  "Given a reducing function (fn [val each]) wrap it such that it bails out on encountering a context.
  Example: (reduce (rewrap f) init coll)"
  ([f]
    `(rewrap context? ~f))
  ([context-pred f]
    `(refn ~context-pred [acc# each#]
       (~f acc# each#))))


(defmacro !rewrap
  "Given a reducing function (fn [val each]) wrap it such that it bails out on encountering a context or exception.
  Usage: (reduce (!rewrap NullPointerException f) init coll)"
  ([f]
    `(refn [acc# each#] (! (~f acc# each#))))
  ([classes f]
    `(!rewrap ~classes context? ~f))
  ([classes context-pred f]
    `(refn ~context-pred [acc# each#] (! ~classes (~f acc# each#)))))
