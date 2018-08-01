;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.core-test
  (:require
    #?(:cljs [cljs.test    :refer-macros [deftest is testing]]
        :clj [clojure.test :refer        [deftest is testing]])
    #?(:cljs [promenade.core :as prom :include-macros true]
        :clj [promenade.core :as prom])))


(defn throwable [msg]
  #?(:clj (Exception. msg) :cljs (js/Error. msg)))


(deftest test-context-util
  (testing "Positive tests"
    (is ((every-pred prom/failure? prom/context?) (prom/fail :fail)))
    (is ((every-pred prom/failure? prom/context?) prom/failure))
    (is ((every-pred prom/nothing? prom/context?) prom/nothing))
    (is ((every-pred prom/nothing? prom/context?) (prom/void :foo)))
    (is ((every-pred prom/thrown?  prom/context?) (prom/thrown (throwable "test"))))
    (is ((every-pred prom/thrown?  prom/context?) (prom/! (throw (throwable "test")))))
    (is ((every-pred prom/thrown?  prom/context?) ((prom/!wrap (fn [] (throw (throwable "test")))))))
    (is (prom/free? :foo)))
  (testing "Negative tests"
    (is (not (prom/failure? prom/nothing)))
    (is (not (prom/failure? :foo)))
    (is (not (prom/nothing? (prom/fail :fail))))
    (is (not (prom/nothing? :foo)))
    (is (not (prom/thrown?  (prom/void :foo))))
    (is (not (prom/thrown?  :foo)))
    (is (not (prom/context? :foo)))
    (is (not (prom/free? (prom/fail :fail))))))


(deftest test-branch
  (is (= 101 ((prom/branch even? inc) 100)))
  (is (= 101 ((prom/branch even? inc) 101)))
  (is (= 101 ((prom/branch dec even? inc) 100)))
  (is (= 100 ((prom/branch dec even? inc) 101))))


(deftest test-bind-either
  (is (= :foo (prom/bind-either :foo identity)))
  (is (= 1000 (-> :foo
                (prom/bind-either prom/fail)
                (prom/bind-either {:foo 1000} vector))) "failure channel")
  (is (= [20] (-> :foo
                (prom/bind-either {:foo 20})
                (prom/bind-either {:foo 1000} vector))) "success channel"))


(deftest test-either->
  (is (= 4
        (prom/either-> :foo
          {:foo 1
           :bar 2}
          [(* 0) (+ 2)]
          inc)))
  (is (= 60
        (prom/either-> :foo
          prom/fail
          [{:foo 10
            :bar 20} vector]
          (+ 50))))
  (is (= :foo
        (prom/either-> :foo
          prom/fail
          [identity])) "1-element vector applies to the left ('failure' in this case)")
  (is (= :foo
        (prom/either-> :foo
          identity
          [identity])) "1-element vector applies to the left ('failure' in this case)"))


(deftest test-either->>
  (is (= 2
        (prom/either->> :foo
          {:foo 1
           :bar 2}
          [(* 0) (vector 2)]
          first)))
  (is (= 1
        (prom/either->> :foo
          {:foo 1
           :bar 2}
          prom/fail
          [(* 0) (vector 2)]
          inc)))
  (is (= [:foo]
        (prom/either->> :foo
          prom/fail
          [vector])) "1-element vector applies to the left ('failure' in this case)")
  (is (= :foo
        (prom/either->> :foo
          identity
          [vector])) "1-element vector applies to the left ('failure' in this case)"))


(deftest test-either-as->
  (is (= 1
        (prom/either-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(* 0 $) (vector $ 2)]
          (first $))))
  (is (= 30
        (prom/either-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(* 0 $) (vector $ 2)]
          30)))
  (is (= [:foo]
        (prom/either-as-> :foo $
          (prom/fail [$])
          [$])) "1-element vector applies to the left ('failure' in this case)")
  (is (= [:foo]
        (prom/either-as-> :foo $
          (vector $)
          [$])) "1-element vector applies to the left ('failure' in this case)"))


(deftest test-bind-maybe
  (is (= :foo (prom/bind-maybe :foo identity)))
  (is (= 1000 (-> :foo
                (prom/bind-maybe (constantly prom/nothing))
                (prom/bind-maybe #(do 1000) vector))) "failure channel")
  (is (= [20] (-> :foo
                (prom/bind-maybe {:foo 20})
                (prom/bind-maybe #(do 1000) vector))) "success channel"))


(deftest test-maybe->
  (is (= 4
        (prom/maybe-> :foo
          {:foo 1
           :bar 2}
          [(* 0) (+ 2)]
          inc)))
  (is (= 60
        (prom/maybe-> :foo
          prom/void
          [(do 10) vector]
          (+ 50))))
  (is (= :bar
        (prom/maybe-> :foo
          prom/void
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)")
  (is (= :foo
        (prom/maybe-> :foo
          identity
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)"))


(deftest test-maybe->>
  (is (= 2
        (prom/maybe->> :foo
          {:foo 1
           :bar 2}
          [(* 0) (vector 2)]
          first)))
  (is (= 1
        (prom/maybe->> :foo
          {:foo 1
           :bar 2}
          prom/void
          [(* 0) (vector 2)]
          inc)))
  (is (= 4
        (prom/maybe->> :foo
          prom/void
          [(do 4)])) "1-element vector applies to the left ('nothing' in this case)")
  (is (= :foo
        (prom/maybe->> :foo
          identity
          [vector])) "1-element vector applies to the left ('nothing' in this case)"))


(deftest test-maybe-as->
  (is (= 1
        (prom/maybe-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(do [2 1]) (vector $ 2)]
          (first $))))
  (is (= 30
        (prom/maybe-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(do [2 1]) (vector $ 2)]
          30)))
  (is (= :bar
        (prom/maybe-as-> :foo $
          (prom/void [$])
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)")
  (is (= [:foo]
        (prom/maybe-as-> :foo $
          (vector $)
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)"))


(defn throwx [msg]
  (prom/! (throw (throwable msg))))


(defn exception? [x]
  (instance? #?(:clj Exception :cljs js/Error) x))


(deftest test-bind-trial
  (is (= :foo (prom/bind-trial :foo identity)))
  (is (= true (-> :foo
                (prom/bind-trial (fn [_] (throwx "test")))
                (prom/bind-trial exception? vector))) "failure channel")
  (is (= [20] (-> :foo
                (prom/bind-trial {:foo 20})
                (prom/bind-trial {:foo 1000} vector))) "success channel"))


(deftest test-trial->
  (is (= 4
        (prom/trial-> :foo
          {:foo 1
           :bar 2}
          [exception? (+ 2)]
          inc)))
  (is (= false
        (prom/trial-> "foo"
          throwx
          [exception? vector]
          not)))
  (is (= true
        (prom/trial-> :foo
          throwx
          [exception?])) "1-element vector applies to the left ('exception' in this case)")
  (is (= :foo
        (prom/trial-> :foo
          identity
          [exception?])) "1-element vector applies to the left ('exception' in this case)"))


(deftest test-trial->>
  (is (= 2
        (prom/trial->> :foo
          {:foo 1
           :bar 2}
          [(instance? #?(:clj Exception :cljs js/Error)) (vector 2)]
          first)))
  (is (= false
        (prom/trial->> :foo
          {:foo 1
           :bar 2}
          throwx
          [(instance? #?(:clj Exception :cljs js/Error)) (vector 2)]
          not)))
  (is (= true
        (prom/trial->> :foo
          throwx
          [(instance? #?(:clj Exception :cljs js/Error))]))
    "1-element vector applies to the left ('exception' in this case)")
  (is (= :foo
        (prom/trial->> :foo
          identity
          [(instance? #?(:clj Exception :cljs js/Error))]))
    "1-element vector applies to the left ('exception' in this case)"))


(defn get-ex-msg [e]
  #?(:cljs (.-message e)
      :clj (.getMessage ^Exception e) ))


(deftest test-trial-as->
  (is (= 1
        (prom/trial-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(do [2 1]) (vector $ 2)]
          (first $))))
  (is (= "bar-foo"
        (prom/trial-as-> :foo $
          ({:foo 1
            :bar 2} $)
          (throwx "foo")
          [(get-ex-msg $) (vector $ 2)]
          (str "bar-" $))))
  (is (= "foo"
        (prom/trial-as-> :foo $
          (throwx (name $))
          [(get-ex-msg $)]))
    "1-element vector applies to the left ('exception' in this case)")
  (is (= [:foo]
        (prom/trial-as-> :foo $
          (vector $)
          [(get-ex-msg $)]))
    "1-element vector applies to the left ('exception' in this case)"))


(deftest test-mdo
  (testing "Match success"
    (is (nil? (prom/mdo nil)))
    (is (= 10 (prom/mdo 10)))
    (is (= 20 (prom/mdo 10 20))))
  (testing "Match implicit failure"
    (is (= nil (prom/mdo)))
    (is (= (prom/fail :foo) (prom/mdo (prom/fail :foo) (prom/fail :bar) 20)) "context bails out early")
    (is (= 20 (prom/mdo (prom/mfailure (prom/fail :foo)) 20)) "matching context makes no difference")))


(deftest test-mlet
  (testing "Match success"
    (is (= 10 (prom/mlet [] 10)))
    (is (= 11 (prom/mlet [a 10] (inc a))))
    (is (nil? (prom/mlet [a nil] a)))
    (is (= 30 (prom/mlet [a 10
                          b 20] (+ a b))))
    (is (= [:foo :bar] (prom/mlet [a (prom/mfailure (prom/fail :foo))] [a :bar])))
    (is (= [:foo :bar] (prom/mlet [a (prom/mfailure (prom/fail :foo))
                                   b (prom/mnothing prom/nothing :bar)] [a b]))))
  (testing "Body"
    (is (= nil (prom/mlet [])))
    (is (= 20 (prom/mlet [] 10 20)))
    (is (= (prom/fail 10) (prom/mlet [] (prom/fail 10) 20))))
  (testing "Match implicit failure"
    (is (= (prom/fail 10) (prom/mlet [a (prom/fail 10)] :foo)))
    (is (= prom/nothing (prom/mlet [a prom/nothing] :foo)))
    (is (= (prom/thrown 10) (prom/mlet [a (prom/thrown 10)] :foo))))
  (testing "Match explicit failure"
    (is (= 10 (prom/mlet [a (prom/mfailure 10)] (inc a))))
    (is (= 20 (prom/mlet [a 10
                          b (prom/mfailure 20)] (+ a b))))
    (is (= 30 (prom/mlet [a (prom/mfailure (prom/fail :foo))
                          b 20
                          c (prom/mnothing 30 :bar)] [a b c])))))


(deftest test-if-mlet
  (testing "Match success"
    (is (= 10 (prom/if-mlet [] 10)))
    (is (= 11 (prom/if-mlet [a 10] (inc a) 10)))
    (is (= 30 (prom/if-mlet [a 10
                             b 20] (+ a b) 10)))
    (is (= 30 (prom/if-mlet [a 10
                             b (prom/mfailure (prom/fail 20))] (+ a b))))
    (is (= 60 (prom/if-mlet [a 10
                             b 20
                             c 30] (+ a b c))))
    (is (= 60 (prom/if-mlet [a (prom/mfailure (prom/fail 10))
                             b 20
                             c (prom/mnothing prom/nothing 30)] (+ a b c) 50))))
  (testing "Match implicit failure"
    (is (= prom/nothing (prom/if-mlet [a (prom/fail 10)] :foo)))
    (is (= :bar         (prom/if-mlet [a (prom/fail 10)] :foo :bar)))
    (is (= prom/nothing (prom/if-mlet [a prom/nothing] :foo)))
    (is (= :bar         (prom/if-mlet [a prom/nothing] :foo :bar)))
    (is (= prom/nothing (prom/if-mlet [a (prom/thrown 10)] :foo)))
    (is (= :bar         (prom/if-mlet [a (prom/thrown 10)] :foo :bar))))
  (testing "Match failure, with else specified"
    (is (= 10 (prom/if-mlet [a (prom/mfailure 20)] a 10)))
    (is (= 10 (prom/if-mlet [a 10
                             b (prom/mfailure 20)] (+ a b) 10)))
    (is (= 10 (prom/if-mlet [a (prom/mfailure (prom/fail 10))
                             b 20
                             c (prom/mfailure 20)] (+ a b c) 10))))
  (testing "Match failure, with else unspecified"
    (is (= prom/nothing (prom/if-mlet [a (prom/mfailure 20)] a)))
    (is (= prom/nothing (prom/if-mlet [a 10
                                       b (prom/mfailure 20)] (+ a b))))
    (is (= prom/nothing (prom/if-mlet [a (prom/mfailure (prom/fail 10))
                                       b 20
                                       c (prom/mfailure 20)] (+ a b c))))))


(deftest test-when-mlet
  (testing "Match success"
    (is (= 20 (prom/when-mlet [] 10 20)))
    (is (= 11 (prom/when-mlet [a 10] (inc a))))
    (is (= 10 (prom/when-mlet [a 10
                               b 20] (+ a b) 10)))
    (is (= 30 (prom/when-mlet [a 10
                               b (prom/mfailure (prom/fail 20))] (+ a b))))
    (is (= 60 (prom/when-mlet [a 10
                               b 20
                               c 30] (+ a b c))))
    (is (= 50 (prom/when-mlet [a (prom/mfailure (prom/fail 10))
                               b 20
                               c (prom/mnothing prom/nothing 30)] (+ a b c) 50))))
  (testing "Failure in body"
    (is (= nil (prom/when-mlet [])))
    (is (= (prom/fail 10) (prom/when-mlet [a 10] (prom/fail a) a))))
  (testing "Match implicit failure"
    (is (= nil (prom/when-mlet [a (prom/fail 10)] :foo)))
    (is (= nil (prom/when-mlet [a prom/nothing] :foo)))
    (is (= nil (prom/when-mlet [a (prom/thrown 10)] :foo))))
  (testing "Match failure"
    (is (= nil (prom/when-mlet [a (prom/mfailure 10)] a)))
    (is (= nil (prom/when-mlet [a 10
                                b (prom/mfailure 20)] (+ a b))))
    (is (= nil (prom/when-mlet [a 10
                                b 20
                                c (prom/mfailure 30)] (+ a b c))))))


(deftest test-cond-mlet
  (testing "Match success"
    (is (= 10 (prom/cond-mlet
                :foo 10)))
    (is (= 11 (prom/cond-mlet
                [a 10] (inc a))))
    (is (= 30 (prom/cond-mlet
                [a 10
                 b 20] (+ a b))))
    (is (= 20 (prom/cond-mlet
                [a 10
                 b (prom/mfailure 20)] (+ a b)
                [a 40
                 b 20] (- a b))))
    (is (= 50 (prom/cond-mlet
                [a 10
                 b (prom/mfailure 20)] (+ a b)
                [a 40
                 b (prom/mthrown 20)] (- a b)
                :else 50))))
  (testing "Match implicit failure"
    (is (= prom/nothing (prom/cond-mlet [a (prom/fail 10)] :foo)))
    (is (= :bar         (prom/cond-mlet [a (prom/fail 10)] :foo :else :bar)))
    (is (= prom/nothing (prom/cond-mlet [a prom/nothing] :foo)))
    (is (= :bar         (prom/cond-mlet [a prom/nothing] :foo :else :bar)))
    (is (= prom/nothing (prom/cond-mlet [a (prom/thrown 10)] :foo)))
    (is (= :bar         (prom/cond-mlet [a (prom/thrown 10)] :foo :else :bar))))
  (testing "Match failure"
    (is (= prom/nothing (prom/cond-mlet)))
    (is (= prom/nothing (prom/cond-mlet
                          false 10)))
    (is (= prom/nothing (prom/cond-mlet
                          [a (prom/mfailure 10)] (inc a))))
    (is (= prom/nothing (prom/cond-mlet
                          [a (prom/mfailure 10)] (inc a)
                          [a (prom/mthrown 20)]  (inc a))))))


(deftest test-refn
  (let [f (prom/refn [a x] (if (zero? x) prom/failure (* a x)))]
    (-> f
      (reduce 1 [1 2 3 4 5 6 7 8 9])
      prom/free?
      (is "happy case"))
    (-> f
      (reduce 1 [1 2 3 0 5 6 7 8 9])
      prom/failure?
      (is "failure case")))
  (let [f (prom/refn prom/nothing? [a x] (if (zero? x) prom/nothing (* a x)))]
    (-> f
      (reduce 1 [1 2 3 4 5 6 7 8 9])
      prom/free?
      (is "happy case"))
    (-> f
      (reduce 1 [1 2 3 0 5 6 7 8 9])
      prom/nothing?
      (is "failure case"))))


(deftest test-rewrap
  (let [f (prom/rewrap (fn [a x] (if (zero? x) prom/failure (* a x))))]
    (-> f
      (reduce 1 [1 2 3 4 5 6 7 8 9])
      prom/free?
      (is "happy case"))
    (-> f
      (reduce 1 [1 2 3 0 5 6 7 8 9])
      prom/failure?
      (is "failure case")))
  (let [f (prom/rewrap prom/nothing? (fn [a x] (if (zero? x) prom/nothing (* a x))))]
    (-> f
      (reduce 1 [1 2 3 4 5 6 7 8 9])
      prom/free?
      (is "happy case"))
    (-> f
      (reduce 1 [1 2 3 0 5 6 7 8 9])
      prom/nothing?
      (is "failure case"))))
