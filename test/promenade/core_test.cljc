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
    #?(:cljs [promenade.core :as prom :refer-macros [either-> either->> either-as->
                                                     maybe->  maybe->>  maybe-as->
                                                     trial->  trial->>  trial-as-> !]]
        :clj [promenade.core :as prom :refer        [either-> either->> either-as->
                                                     maybe->  maybe->>  maybe-as->
                                                     trial->  trial->>  trial-as-> !]])))


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
        (either-> :foo
          {:foo 1
           :bar 2}
          [(* 0) (+ 2)]
          inc)))
  (is (= 60
        (either-> :foo
          prom/fail
          [{:foo 10
            :bar 20} vector]
          (+ 50))))
  (is (= :foo
        (either-> :foo
          prom/fail
          [identity])) "1-element vector applies to the left ('failure' in this case)")
  (is (= :foo
        (either-> :foo
          identity
          [identity])) "1-element vector applies to the left ('failure' in this case)"))


(deftest test-either->>
  (is (= 2
        (either->> :foo
          {:foo 1
           :bar 2}
          [(* 0) (vector 2)]
          first)))
  (is (= 1
        (either->> :foo
          {:foo 1
           :bar 2}
          prom/fail
          [(* 0) (vector 2)]
          inc)))
  (is (= [:foo]
        (either->> :foo
          prom/fail
          [vector])) "1-element vector applies to the left ('failure' in this case)")
  (is (= :foo
        (either->> :foo
          identity
          [vector])) "1-element vector applies to the left ('failure' in this case)"))


(deftest test-either-as->
  (is (= 1
        (either-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(* 0 $) (vector $ 2)]
          (first $))))
  (is (= 30
        (either-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(* 0 $) (vector $ 2)]
          30)))
  (is (= [:foo]
        (either-as-> :foo $
          (prom/fail [$])
          [$])) "1-element vector applies to the left ('failure' in this case)")
  (is (= [:foo]
        (either-as-> :foo $
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
        (maybe-> :foo
          {:foo 1
           :bar 2}
          [(* 0) (+ 2)]
          inc)))
  (is (= 60
        (maybe-> :foo
          prom/void
          [(do 10) vector]
          (+ 50))))
  (is (= :bar
        (maybe-> :foo
          prom/void
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)")
  (is (= :foo
        (maybe-> :foo
          identity
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)"))


(deftest test-maybe->>
  (is (= 2
        (maybe->> :foo
          {:foo 1
           :bar 2}
          [(* 0) (vector 2)]
          first)))
  (is (= 1
        (maybe->> :foo
          {:foo 1
           :bar 2}
          prom/void
          [(* 0) (vector 2)]
          inc)))
  (is (= 4
        (maybe->> :foo
          prom/void
          [(do 4)])) "1-element vector applies to the left ('nothing' in this case)")
  (is (= :foo
        (maybe->> :foo
          identity
          [vector])) "1-element vector applies to the left ('nothing' in this case)"))


(deftest test-maybe-as->
  (is (= 1
        (maybe-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(do [2 1]) (vector $ 2)]
          (first $))))
  (is (= 30
        (maybe-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(do [2 1]) (vector $ 2)]
          30)))
  (is (= :bar
        (maybe-as-> :foo $
          (prom/void [$])
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)")
  (is (= [:foo]
        (maybe-as-> :foo $
          (vector $)
          [(do :bar)])) "1-element vector applies to the left ('nothing' in this case)"))


(defn throwx [msg]
  (! (throw #?(:clj (Exception. msg) :cljs (js/Error. msg)) )))


(defn exception? [x]
  (instance? #?(:clj Exception :cljs js/Error) x))


(deftest test-trial->
  (is (= 4
        (trial-> :foo
          {:foo 1
           :bar 2}
          [exception? (+ 2)]
          inc)))
  (is (= false
        (trial-> "foo"
          throwx
          [exception? vector]
          not)))
  (is (= true
        (trial-> :foo
          throwx
          [exception?])) "1-element vector applies to the left ('exception' in this case)")
  (is (= :foo
        (trial-> :foo
          identity
          [exception?])) "1-element vector applies to the left ('exception' in this case)"))


(deftest test-trial->>
  (is (= 2
        (trial->> :foo
          {:foo 1
           :bar 2}
          [(instance? #?(:clj Exception :cljs js/Error)) (vector 2)]
          first)))
  (is (= false
        (trial->> :foo
          {:foo 1
           :bar 2}
          throwx
          [(instance? #?(:clj Exception :cljs js/Error)) (vector 2)]
          not)))
  (is (= true
        (trial->> :foo
          throwx
          [(instance? #?(:clj Exception :cljs js/Error))]))
    "1-element vector applies to the left ('exception' in this case)")
  (is (= :foo
        (trial->> :foo
          identity
          [(instance? #?(:clj Exception :cljs js/Error))]))
    "1-element vector applies to the left ('exception' in this case)"))


(defn get-ex-msg [e]
  #?(:cljs (.-message e)
      :clj (.getMessage ^Exception e) ))


(deftest test-trial-as->
  (is (= 1
        (trial-as-> :foo $
          ({:foo 1
            :bar 2} $)
          [(do [2 1]) (vector $ 2)]
          (first $))))
  (is (= "bar-foo"
        (trial-as-> :foo $
          ({:foo 1
            :bar 2} $)
          (throwx "foo")
          [(get-ex-msg $) (vector $ 2)]
          (str "bar-" $))))
  (is (= "foo"
        (trial-as-> :foo $
          (throwx (name $))
          [(get-ex-msg $)]))
    "1-element vector applies to the left ('exception' in this case)")
  (is (= [:foo]
        (trial-as-> :foo $
          (vector $)
          [(get-ex-msg $)]))
    "1-element vector applies to the left ('exception' in this case)"))
