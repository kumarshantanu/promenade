;   Copyright (c) Shantanu Kumar. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file LICENSE at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.


(ns promenade.type)


(defprotocol IContext "Marker protocol for all context types")


(defprotocol IFailure "Marker protocol for failure - to be implemented with IDeref")
(defprotocol INothing "Marker protocol for nothing")
(defprotocol IThrown  "Marker protocol for thrown - to be implemented with IDeref")
