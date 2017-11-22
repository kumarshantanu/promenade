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
  (:require
    [promenade.internal :as i]
    [promenade.type     :as t]))


;; ----- helpers for making or uncovering context -----


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
         (satisfies? t/IFailure x) x
         (satisfies? t/IContext x) (throw (ex-info "Cannot derive failure from other context" {:context x}))
         :otherwise                (i/->Failure x)))
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
         (satisfies? t/INothing x) x
         (satisfies? t/IContext x) (throw (ex-info "Cannot turn other context into nothing" {:context x}))
         :otherwise                nothing))
  ([] nothing))


(defmacro !
  "Evaluate given form and return it; on exception return the exception as thrown result."
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


;;~~~~~~~~~~~~~~~~
;; Unwrap context

(defn deref-context
  "Deref argument if it is a context, return as it is otherwise."
  ([x] (if (satisfies? t/IContext x)
         (if (i/derefable? x)
           (deref x)
           (throw (ex-info "Context does not support deref" {:context x})))
         x))
  ([x default] (if (satisfies? t/IContext x)
                 (if (i/derefable? x)
                   (deref x)
                   default)
                 x)))


;; ----- bind -----


(defn bind-either
  "Given a context mval (success or failure) bind it with a function of respective type, i.e. success-f or failure-f.
  See:
    either->
    either->>
    either-as->
    bind-maybe
    bind-thrown"
  ([mval success-f] (if (satisfies? t/IContext mval)
                      mval
                      (success-f mval)))
  ([mval failure-f success-f] (cond
                                (satisfies? t/IFailure mval) (failure-f (deref mval))
                                (satisfies? t/IContext mval) mval
                                :otherwise                   (success-f mval))))


(defn bind-maybe
  "Given a context mval (just or nothing) bind it with a function of respective type, i.e. just-f or nothing-f.
  See:
    maybe->
    maybe->>
    maybe-as->
    bind-either
    bind-thrown"
  ([mval just-f] (if (satisfies? t/IContext mval)
                   mval
                   (just-f mval)))
  ([mval nothing-f just-f] (cond
                             (satisfies? t/INothing mval) (nothing-f)
                             (satisfies? t/IContext mval) mval
                             :otherwise                   (just-f mval))))


(defn bind-trial
  "Given a context mval (value or exception) bind it with a function of respective type, i.e. result-f or thrown-f.
  See:
    trial->
    trial->>
    trial-as->
    bind-either
    bind-maybe"
  ([mval result-f] (if (satisfies? t/IContext mval)
                     mval
                     (result-f mval)))
  ([mval thrown-f result-f] (cond
                              (satisfies? t/IThrown mval)  (thrown-f (deref mval))
                              (satisfies? t/IContext mval) mval
                              :otherwise                   (result-f mval))))


;; ----- pipeline macros -----


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
  "Thread-last expansion using bind. A vector form of one element [x] is applied only to 'failure' leaving
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
  "Thread-anywhere expansion using bind. A vector form of one element [x] is applied only to 'failure' leaving
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
  "Thread-last expansion using bind. A vector form of one element [x] is applied only to 'nothing' leaving
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
  "Thread-anywhere expansion using bind. A vector form of one element [x] is applied only to 'nothing' leaving
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
  "Thread-first expansion using bind-either. A vector form of one element [x] is applied only to 'thrown' leaving
  'result' intact.
  Example usage                           Expanded as
  -------------                         | -----------
  (trial-> (place-order)                  | (-> (place-order)
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


;; ----- support for matching binding forms ----


(defn mfailure
  "Match argument as Failure, returning a match-result.
  See:
    mnothing
    mthrown
    mlet
    if-mlet
    when-mlet
    cond-mlet"
  [x]
  (if (satisfies? t/IFailure x)
    (i/->Match true (deref x))
    (i/->Match false x)))


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
  (if (satisfies? t/INothing x)
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
  [x]
  (if (satisfies? t/IThrown x)
    (i/->Match true (deref x))
    (i/->Match false x)))


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
    `(do ~@body)
    (let [[lhs rhs & more] bindings
          restof-expansion (if (seq more)
                             [`(mlet [~@more] ~@body)]
                             body)]
      (with-meta `(let [rhs# ~rhs
                        mi?# (i/match-instance? rhs#)]
                    (if (and mi?# (not (:match? rhs#)))
                      (:value rhs#)
                      (let [~lhs (if mi?# (:value rhs#) rhs#)]
                        ~@restof-expansion)))
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
  scope. If a non-matching context is encountered, return a promenade.type.INothing instance.
  See:
    mfailure
    mnothing
    mthrown
    mlet
    if-mlet
    cond-mlet"
  [bindings & body]
  `(if-mlet ~bindings
     (do ~@body)
     nothing))


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