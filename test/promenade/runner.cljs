(ns promenade.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [promenade.core-test]))


(enable-console-print!)

(try
  (doo-tests
    'promenade.core-test)
  (catch js/Error e
    (.log js/Console (.-stack e))))
