;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.util
  "Utility functions for using Promenade effectively"
  #?(:cljs (:require-macros promenade.util))
  (:require
    #?(:cljs [promenade.core :as prom :include-macros true]
        :clj [promenade.core :as prom])
    [promenade.type :as prot])
  #?(:clj (:import
            [clojure.lang ExceptionInfo IDeref]
            [promenade.util StacklessExceptionInfo])))


#?(:cljs
    (defn ^{:jsdoc ["@constructor"]}
          StacklessExceptionInfo [message data]
      (when (nil? data)
        (throw (js/Error. "Additional data must be non-nil")))
      (this-as this
        (set! (.-message this) message)
        (set! (.-data this) data)
        this)))


#?(:cljs (set! (.. StacklessExceptionInfo -prototype) (.. ExceptionInfo -prototype)))
#?(:cljs (set! (.. StacklessExceptionInfo -prototype -__proto__) js/Error.prototype))


(defn se-info
  "Like `clojure.core/ex-info`, but with the following differences:

  | `promenade.util/se-info`                     | `clojure.core/ex-info`, `cljs.core/ex-info`   |
  |----------------------------------------------|-----------------------------------------------|
  | no stack trace or cause support              | full support for stack trace and cause        |
  | much faster and lightweight                  | same cost model as regular exceptions         |
  | meant for flow control, not debugging        | meant for debugging and flow control          |
  | uses `promenade.util.StacklessExceptionInfo` | uses `(clojure.lang|cljs.core).ExceptionInfo` |

  You may use `clojure.core/ex-data` or `cljs.core/ex-data` on this exception to retrieve the data.

  See: [[se-info?]], [[!se-info]], [[!wrap-se-info]]"
  ([msg] (StacklessExceptionInfo. msg {}))
  ([msg data] (StacklessExceptionInfo. msg data)))


(defn se-info?
  "Return `true` if argument is an instance of `StacklessExceptionInfo`, `false` otherwise.

  See: [[se-info]], [[!se-info]], [[!wrap-se-info]]"
  [x]
  #?(:cljs (and (instance? StacklessExceptionInfo x)
             (nil? (.-stack x)))
      :clj (instance? StacklessExceptionInfo x)))


(defmacro !se-info
  "Evaluate given form and return the result; on `promenade.util.StacklessExceptionInfo` return the exception as a
  thrown context.

  See: [[promenade.core/!]], [[se-info]], [[se-info?]], [[!wrap-se-info]]"
  [expr]
  (let [se-sym (gensym "se-")]
    `(try ~expr
       (catch promenade.util.StacklessExceptionInfo ~se-sym
         ;; In CLJS `defmacro` is called by ClojureJVM, hence reader conditionals always choose :clj -
         ;; so we discover the environment using a hack (:ns &env), which returns truthy for CLJS.
         ;; Reference: https://groups.google.com/forum/#!topic/clojure/DvIxYnO1QLQ
         ;; Reference: https://dev.clojure.org/jira/browse/CLJ-1750
         ~(if (:ns &env)
            `(if (se-info? ~se-sym)
               ~se-sym
               (throw ~se-sym))
            se-sym)))))


(defmacro !wrap-se-info
  "Wrap given function such that on `promenade.util.StacklessExceptionInfo` it returns the exception as a thrown
  context.

  See: [[promenade.core/!wrap]], [[se-info]], [[se-info?]], [[!se-info]]"
  [f]
  ;; NOTE: This could be a function instead of a macro, but the function doesn't work in CLJS
  `(fn [& args#]
     (!se-info (apply ~f args#))))


(defmacro defentity
  "Define entity record and corresponding instance type predicate function. If fields are unspecified, assume single
  field `value`.
  Examples:
  ```
  (defentity ProductCode)  ; create record ProductCode with single field 'value', and predicate fn ProductCode?
  (defentity Approval [by when])  ; create record Approval with fields `by` and `when`, and predicate Approval?
  ```
  See: [[defailure]]"
  ([the-name]
    `(defentity ~the-name [~'value]))
  ([the-name fields & more]
    `(let [klass# (defrecord ~the-name ~fields ~@more)]
       ;; define predicate
       (defn ~(symbol (str the-name "?"))
         ~(str "Return `true` if argument is an instance of " the-name ", `false` otherwise.")
         [x#]
         (instance? klass# x#))
       ;; return what defrecord returned
       klass#)))


(defmacro defailure
  "Define a failure entity and corresponding instance type predicate function."
  [the-name fields & more]
  (let [deref-sym (if (:ns &env) '-deref 'deref)]
    `(defentity ~the-name ~fields
       prot/IContext
       prot/IFailure
       ~(if (:ns &env) 'IDeref 'clojure.lang.IDeref) (~deref-sym [this#] this#)
       prot/IHolder (prot/-obtain [this#] this#)
       ~@more)))
