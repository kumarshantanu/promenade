;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.internal
  (:require
    [promenade.type :as t])
  #?(:clj (:import
            [clojure.lang IDeref IRecord])))


(defn expected
  "Throw illegal input exception citing `expectation` and what was `found` did not match. Optionally accept a predicate
  fn to test `found` before throwing the exception."
  ([expectation found]
    (throw (ex-info
             (str "Expected " expectation ", but found (" (pr-str (type found)) ") " (pr-str found))
             {:found found})))
  ([pred expectation found]
    (when-not (pred found)
      (expected expectation found))))


(defn derefable? [x]
  #?(:cljs (satisfies? IDeref x)
      :clj (instance? IDeref x)))


(defn invoke
  [f & args]
  (apply f args))


(defn bind-expr
  "Given a bind fn (fn [mv right-f] [mv left-f right-f]), left and right expander fns of the arity (fn [form]) and a
  short bind-form, rewrite it as a bind expression."
  [bind-f left-f right-f form]
  (if (vector? form)
    (case (count form)
      2 `(~bind-f ~@(map invoke [left-f right-f] form))
      1 `(~bind-f ~@(map invoke [left-f] form) identity)
      (throw (ex-info (str "Expected vector form to have one or two elements, but found " form) {})))
    `(~bind-f ~(right-f form))))


(defn expand-nothing
  [form]
  (if (list? form)
    (with-meta `(^:once fn* [] ~form) (meta form))
    `(^:once fn* [] (~form))))


(defn expand->
  [form]
  (if (list? form)
    (with-meta `(^:once fn* [x#] (~(first form) x# ~@(rest form))) (meta form))
    form))


(defn expand->>
  [form]
  (if (list? form)
    (with-meta `(^:once fn* [x#] (~(first form) ~@(rest form) x#)) (meta form))
    form))


(defn expand-as->
  [name form]
  (if (list? form)
    (with-meta `(^:once fn* [~name] ~form) (meta form))
    `(^:once fn* [~name] ~form)))


;; ----- context implementation -----


(defrecord Failure [failure]
  t/IContext
  IDeref (#?(:clj deref :cljs -deref) [_] failure)
  t/IFailure)


(defrecord Nothing []
  t/IContext
  t/INothing)


(defrecord Thrown  [thrown]
  t/IContext
  IDeref (#?(:clj deref :cljs -deref) [_] thrown)
  t/IThrown)


#?(:clj (prefer-method print-method IRecord IDeref))


;; ----- context matching support -----


(defrecord Match [match? value])


(defn match-instance? [x] (instance? Match x))


(defmacro if-then
  "Evaulate `then` expression in lexical scope and return [true then] when all bindings match, else return [false nil]."
  [bindings then]
  (if (empty? bindings)
    [true then]
    (let [[lhs rhs & more] bindings
          restof-expansion (if (seq more)
                             [`(if-then [~@more] ~then)]
                             [[true then]])]
      (with-meta `(let [rhs# ~rhs
                        mi?# (match-instance? rhs#)]
                    (if (and mi?# (not (:match? rhs#)))
                      [false nil]
                      (let [~lhs (if mi?# (:value rhs#) rhs#)]
                        ~@restof-expansion)))
        (or (meta rhs) (meta lhs))))))