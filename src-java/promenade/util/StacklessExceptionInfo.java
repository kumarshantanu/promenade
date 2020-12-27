/**
 *   Copyright (c) Shantanu Kumar. All rights reserved.
 *   The use and distribution terms for this software are covered by the
 *   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 *   which can be found in the file LICENSE at the root of this distribution.
 *   By using this software in any fashion, you are agreeing to be bound by
 *      the terms of this license.
 *   You must not remove this notice, or any other, from this software.
 **/

package promenade.util;

import clojure.lang.IExceptionInfo;
import clojure.lang.IPersistentMap;

/**
 * Stackless (fast, lightweight) equivalent of {@code clojure.lang.ExceptionInfo}. Meant only for flow control, by
 * representing anticipated error condition. This exception does not have any stack trace or any underlying cause.
 *
 */
@SuppressWarnings("serial")
public class StacklessExceptionInfo extends RuntimeException implements IExceptionInfo {

    private static final String CLASS_NAME_PREFIX = StacklessExceptionInfo.class.getName() + ": ";

    public final IPersistentMap data;

    public StacklessExceptionInfo(String message, IPersistentMap data) {
        super(message, /* cause */ null, /* enableSuppression */ true, /* writeableStackTrace */ false);
        if (data == null) {
            throw new IllegalArgumentException("Additional data must be non-nil");
        }
        this.data = data;
    }

    public IPersistentMap getData() {
        return data;
    }

    @Override
    public String toString() {
        return CLASS_NAME_PREFIX + getMessage() + " " + data.toString();
    }

}
