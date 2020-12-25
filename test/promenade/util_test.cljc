;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.util-test
  (:require
    #?(:cljs [cljs.test    :refer-macros [deftest is testing]]
        :clj [clojure.test :refer        [deftest is testing]])
    #?(:cljs [promenade.core :as prom :include-macros true]
        :clj [promenade.core :as prom])
    #?(:cljs [promenade.util :as prut :include-macros true :refer [StacklessExceptionInfo]]
        :clj [promenade.util :as prut]))
  #?(:clj (:import
            [clojure.lang IExceptionInfo]
            [promenade.jvm StacklessExceptionInfo])))


(deftest test-se-info
  (let [se-1 (prut/se-info "foo")
        se-2 (prut/se-info "foo" {:foo 10})]
    (testing
      "instance check"
      (is (prut/se-info? se-1))
      (is (prut/se-info? se-2)))
    (testing
      "underlying cause"
      (is (nil? (#?(:cljs .-cause
                     :clj .getCause) se-1)))
      (is (nil? (#?(:cljs .-cause
                     :clj .getCause) se-2))))
    (testing
      "exception message"
      (is (= "foo"     (#?(:cljs .-message
                            :clj .getMessage) se-1)))
      (is (= "foo"     (#?(:cljs .-message
                            :clj .getMessage) se-2))))
    (testing
      "exception data"
      (is (= {}        (ex-data se-1)))
      (is (= {:foo 10} (ex-data se-2))))
    (testing
      "stack trace"
      (is (empty? (#?(:cljs .-stack
                       :clj .getStackTrace) se-1)))
      (is (empty? (#?(:cljs .-stack
                       :clj .getStackTrace) se-2))))
    (testing
      "inheritance"
      (is (instance? #?(:cljs js/Error
                         :clj Throwable) se-1))
      (is (instance? #?(:cljs ExceptionInfo
                         :clj IExceptionInfo) se-2)))
    (testing
      "throw and catch"
      (is (thrown? StacklessExceptionInfo
            (throw (prut/se-info "hello"))))
      (is (= :foo
            (try
              (throw (prut/se-info "test"))
              (catch StacklessExceptionInfo e
                :foo)))))))
