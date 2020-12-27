;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.internal
  "Internal implementation details of Promenade - likely to break across versions."
  (:require
    #?(:clj [clojure.pprint :as pp])
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


(defn holder? [x]
  (satisfies? t/IHolder x))


;; ----- context implementation -----


(defrecord Failure [failure]
  t/IContext
  IDeref (#?(:clj deref :cljs -deref) [_] failure)
  t/IHolder (-obtain [_] failure)
  t/IFailure)


(defrecord Nothing []
  t/IContext
  t/INothing)


(defrecord Thrown  [thrown]
  t/IContext
  IDeref (#?(:clj deref :cljs -deref) [_] thrown)
  t/IHolder (-obtain [_] thrown)
  t/IThrown)


;; make Failure, Nothing and Thrown printable at the REPL
#?(:clj (defmethod print-method Failure [x writer] ((get-method print-method IRecord) x writer)))
#?(:clj (defmethod print-method Nothing [x writer] ((get-method print-method IRecord) x writer)))
#?(:clj (defmethod print-method Thrown  [x writer] ((get-method print-method IRecord) x writer)))

;; make Failure, Nothing and Thrown pretty-printable at the REPL
#?(:clj (defmethod pp/simple-dispatch Failure [x] ((get-method pp/simple-dispatch IRecord) x)))
#?(:clj (defmethod pp/simple-dispatch Nothing [x] ((get-method pp/simple-dispatch IRecord) x)))
#?(:clj (defmethod pp/simple-dispatch Thrown  [x] ((get-method pp/simple-dispatch IRecord) x)))


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
                    (if (or (and mi?# (not (:match? rhs#)))
                          (satisfies? t/IContext rhs#))
                      [false nil]
                      (let [~lhs (if mi?# (:value rhs#) rhs#)]
                        ~@restof-expansion)))
        (or (meta rhs) (meta lhs))))))


(defn throw-unsupported
  [msg]
  (throw #?(:cljs (js/Error. msg)
             :clj (UnsupportedOperationException. ^String msg))))


(defn reduce-form-fn
  [make-handler form bind]
  (if (vector? form)
    (case (count form)
      1 (let [[alt-handler] form]
          `[~bind ~(make-handler alt-handler) identity])
      2 (let [[alt-handler val-handler] form]
          `[~bind ~(make-handler alt-handler) ~(make-handler val-handler)])
      3 (let [[bind alt-handler val-handler] form]
          `[~bind ~(make-handler alt-handler) ~(make-handler val-handler)])
      (expected "vector containing either 1, 2 or 3 forms" form))
    `[~bind ~(make-handler form)]))


(defn gen-reduce
  [make-handler bind expr forms]
  `(reduce (fn [accumulator# forms#]
             (if (= 2 (count forms#))
               (let [[bind# val-handler#] forms#]
                 (bind# accumulator# val-handler#))
               (let [[bind# alt-handler# val-handler#] forms#]
                 (bind# accumulator# alt-handler# val-handler#))))
     ~expr [~@(map #(reduce-form-fn make-handler % bind) forms)]))
