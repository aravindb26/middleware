/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.marker;

import java.io.Closeable;
import java.util.Collection;
import com.openexchange.startup.impl.ThreadLocalCloseableControl;

/**
 * {@link OXThreadMarkers} - Utility class for {@link OXThreadMarker}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class OXThreadMarkers {

    /**
     * Initializes a new {@link OXThreadMarkers}.
     */
    private OXThreadMarkers() {
        super();
    }

    /**
     * Gets the thread-local value associated with given key.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @return The value or <code>null</code>
     */
    public static <V> V getThreadLocalValue(String key) {
        return getThreadLocalValue(key, Thread.currentThread());
    }

    /**
     * Gets the thread-local value associated with given key.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @param t The thread to acquire from
     * @return The value or <code>null</code>
     */
    public static <V> V getThreadLocalValue(String key, Thread t) {
        return (t instanceof OXThreadMarker) ? ((OXThreadMarker) t).getThreadLocalValue(key) : null;
    }

    /**
     * Puts given key-value-association into thread-local map provided that there is not yet such a key present.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @param value The value to put
     * @return The previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key
     */
    public static <V> V putThreadLocalValue(String key, V value) {
        return putThreadLocalValue(key, value, Thread.currentThread());
    }

    /**
     * Puts given key-value-association into thread-local map provided that there is not yet such a key present.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @param value The value to put
     * @param t The thread to put to
     * @return The previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key
     */
    public static <V> V putThreadLocalValue(String key, V value, Thread t) {
        return (t instanceof OXThreadMarker) ? ((OXThreadMarker) t).putThreadLocalValue(key, value) : null;
    }

    /**
     * Removes the thread-local value associated with given key.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @return The removed value or <code>null</code>
     */
    public static <V> V removeThreadLocalValue(String key) {
        return removeThreadLocalValue(key, Thread.currentThread());
    }

    /**
     * Removes the thread-local value associated with given key.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @param t The thread to remove from
     * @return The removed value or <code>null</code>
     */
    public static <V> V removeThreadLocalValue(String key, Thread t) {
        return (t instanceof OXThreadMarker) ? ((OXThreadMarker) t).removeThreadLocalValue(key) : null;
    }

    /**
     * Removes the thread-local values associated with given keys.
     *
     * @param keys The keys referencing the values
     */
    public static void removeThreadLocalValues(Collection<String> keys) {
        removeThreadLocalValues(keys, Thread.currentThread());
    }

    /**
     * Removes the thread-local values associated with given keys.
     *
     * @param keys The keys referencing the values
     * @param t The thread to remove from
     */
    public static void removeThreadLocalValues(Collection<String> keys, Thread t) {
        if (t instanceof OXThreadMarker) {
            ((OXThreadMarker) t).removeThreadLocalValues(keys);
        }
    }

    /**
     * Removes the thread-local values whose key starts with given prefix.
     *
     * @param prefix The prefix
     */
    public static void removeThreadLocalValuesByPrefix(String prefix) {
        removeThreadLocalValuesByPrefix(prefix, Thread.currentThread());
    }

    /**
     * Removes the thread-local values associated with given keys.
     *
     * @param prefix The prefix
     * @param t The thread to remove from
     */
    public static void removeThreadLocalValuesByPrefix(String prefix, Thread t) {
        if (t instanceof OXThreadMarker) {
            ((OXThreadMarker) t).removeThreadLocalValuesByPrefix(prefix);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if current thread is processing an HTTP request.
     *
     * @return <code>true</code> if is processing an HTTP request; otherwise <code>false</code>
     */
    public static boolean isHttpRequestProcessing() {
        return isHttpRequestProcessing(Thread.currentThread());
    }

    /**
     * Checks if specified thread is processing an HTTP request.
     *
     * @param t The thread to check
     * @return <code>true</code> if is processing an HTTP request; otherwise <code>false</code>
     */
    public static boolean isHttpRequestProcessing(Thread t) {
        return ((t instanceof OXThreadMarker) && ((OXThreadMarker) t).isHttpRequestProcessing());
    }

    /**
     * Remembers specified {@code Closeable} instance.
     *
     * @param closeable The {@code Closeable} instance
     * @return <code>true</code> if successfully added; otherwise <code>false</code>
     */
    public static boolean rememberCloseable(Closeable closeable) {
        if (null == closeable) {
            return false;
        }

        Thread t = Thread.currentThread();
        if (t instanceof OXThreadMarker) {
            try {
                return ThreadLocalCloseableControl.getInstance().addCloseable(closeable);
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

    /**
     * Remembers specified {@code Closeable} instance if current thread is processing an HTTP request.
     *
     * @param closeable The {@code Closeable} instance
     * @return <code>true</code> if successfully added; otherwise <code>false</code>
     */
    public static boolean rememberCloseableIfHttpRequestProcessing(Closeable closeable) {
        if (null == closeable) {
            return false;
        }

        Thread t = Thread.currentThread();
        if ((t instanceof OXThreadMarker) && ((OXThreadMarker) t).isHttpRequestProcessing()) {
            try {
                return ThreadLocalCloseableControl.getInstance().addCloseable(closeable);
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

    /**
     * Un-Remembers specified {@code Closeable} instance.
     *
     * @param closeable The {@code Closeable} instance
     * @return <code>true</code> if successfully removed; otherwise <code>false</code>
     */
    public static boolean unrememberCloseable(Closeable closeable) {
        if (null == closeable) {
            return false;
        }

        Thread t = Thread.currentThread();
        if (t instanceof OXThreadMarker) {
            try {
                return ThreadLocalCloseableControl.getInstance().removeCloseable(closeable);
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

}
