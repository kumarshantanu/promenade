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
        :clj [promenade.core :as prom]))
  #?(:clj (:import
            [promenade.util StacklessExceptionInfo])))


#?(:cljs
    (defn ^{:jsdoc ["@constructor"]}
          StacklessExceptionInfo [message data]
      (this-as this
        (set! (.-message this) message)
        (set! (.-data this) data)
        this)))


#?(:cljs (set! (.. StacklessExceptionInfo -prototype) (.. ExceptionInfo -prototype)))
#?(:cljs (set! (.. StacklessExceptionInfo -prototype -__proto__) js/Error.prototype))


(defn se-info
  "Like `clojure.core/ex-info`, but with the following differences:

  | `promenade.util/se-info`                     | `clojure.core/ex-info`, `cljs.core/ex-info` |
  |----------------------------------------------|---------------------------------------------|
  | no stack trace or cause support              | full support for stack trace and cause      |
  | much faster and lightweight                  | same cost model as regular exceptions       |
  | meant for flow control, not debugging        | meant for debugging and flow control        |
  | uses `promenade.util.StacklessExceptionInfo` | uses `clojure.lang.ExceptionInfo`           |

  You may use `clojure.core/ex-data` or `cljs.core/ex-data` on this exception.
  See: [[se-info?]], [[!se-info]]"
  ([msg] (StacklessExceptionInfo. msg {}))
  ([msg data] (StacklessExceptionInfo. msg data)))


(defn se-info?
  "Return `true` if argument is an instance of `StacklessExceptionInfo`, `false` otherwise.
  See: [[se-info]]"
  [x]
  (instance? StacklessExceptionInfo x))
