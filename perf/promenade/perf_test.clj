;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.perf-test
  (:import
    [clojure.lang ExceptionInfo]
    [promenade.jvm StacklessExceptionInfo])
  (:require
    [clojure.test :refer [deftest is testing use-fixtures]]
    [promenade.core :as prom]
    [promenade.util :as prut]
    [citius.core    :as c]))


(use-fixtures :once
  (c/make-bench-wrapper
    ["se-info" "ex-info" "prom/fail"]
    {:chart-title "se-info/ex-info/prom-fail"
     :chart-filename (format "bench-clj-%s.png" c/clojure-version-str)}))


(deftest test-generation
  (c/compare-perf "generating exception"
    (prut/se-info "foo" {:foo 10})
    (ex-info      "foo" {:foo 10})
    (prom/fail    {:msg "foo"
                   :foo 10})))


(deftest test-throw-catch
  (let [se (prut/se-info "foo" {:foo 10})
        ex (ex-info      "foo" {:foo 10})
        pf (prom/fail    {:msg "foo"
                          :foo 10})
        se-f (fn [] (throw se))
        ex-f (fn [] (throw ex))
        pf-f (fn [] pf)]
    (c/compare-perf "throw/catch exception"
      (try
        (se-f)
        (catch StacklessExceptionInfo x
          x))
      (try
        (ex-f)
        (catch ExceptionInfo x
          x))
      (pf-f))))
