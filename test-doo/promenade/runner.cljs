;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.runner
  (:require
    [doo.runner :refer-macros [doo-tests]]
    [promenade.core-test]
    [promenade.util-test]))


(enable-console-print!)

(try
  (doo-tests
    'promenade.core-test
    'promenade.util-test)
  (catch js/Error e
    (.log js/Console (.-stack e))))
