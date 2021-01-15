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
        :clj [promenade.util :as prut])
    [promenade.type :as prot])
  #?(:clj (:import
            [clojure.lang ExceptionInfo IExceptionInfo]
            [promenade.util StacklessExceptionInfo])))


(deftest test-se-info
  (let [se-1 (prut/se-info "foo")
        se-2 (prut/se-info "foo" {:foo 10})
        ex-1 (ex-info "foo" {})]
    (testing
      "construction failure"
      (is (thrown? #?(:cljs js/Error
                       :clj IllegalArgumentException)
            (prut/se-info "test" nil)) "se-info with nil data"))
    (testing
      "instance check"
      (is (prut/se-info? se-1))
      (is (prut/se-info? se-2))
      (is (not (prut/se-info? ex-1))))
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
                :foo))))
      (is (= :foo
            (try
              (throw (prut/se-info "test"))
              (catch ExceptionInfo _
                :foo))))
      #?(:cljs "This test is broken in CLJS because StacklessExceptionInfo shares the prototype of ExceptionInfo"
          :clj (is (thrown? ExceptionInfo
                     (try
                       (throw (ex-info "test" {}))
                       (catch StacklessExceptionInfo _
                         :foo))))))))


(deftest test-!se-info
  (let [se-1 (prut/se-info "test")
        ex-1 (ex-info "other" {})
        f (fn [] :foo)
        g (fn [] (throw se-1))
        h (fn [] (throw ex-1))]
    (is (= :foo (prut/!se-info (f))) "returned value")
    (is (= se-1 (prut/!se-info (g))) "returned StacklessExceptionInfo")
    (is (thrown? ExceptionInfo (prut/!se-info (h)))  "thrown ExceptionInfo")))


(deftest test-!wrap-se-info
  (let [se-1 (prut/se-info "test")
        ex-1 (ex-info "other" {})
        f (fn [] :foo)
        g (fn [] (throw se-1))
        h (fn [] (throw ex-1))
        f-wrapped (prut/!wrap-se-info f)
        g-wrapped (prut/!wrap-se-info g)
        h-wrapped (prut/!wrap-se-info h)]
    (is (= :foo (f-wrapped)) "returned value")
    (is (= se-1 (g-wrapped)) "returned StacklessExceptionInfo")
    (is (thrown? ExceptionInfo (h-wrapped)) "thrown ExceptionInfo")))


(prut/defentity Amount)
(prut/defentity User [id
                      active?])


(deftest test-defentity
  (let [a1 (->Amount 10)
        u1 (->User "u123" true)]
    (is (Amount? a1))
    (is (= 10 (:value a1)))
    (is (User? u1))
    (is (= "u123" (:id u1)))
    (is (true? (:active? u1)))))


(prut/defailure Undelivered [order-id attempts])


(deftest test-defailure
  (let [f1 (->Undelivered "x123" 3)]
    (is (Undelivered? f1))
    (is (prom/failure? f1))
    (is (= f1 @f1))
    (is (satisfies? prot/IHolder f1))))
