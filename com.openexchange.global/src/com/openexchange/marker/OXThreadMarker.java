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

import java.util.Collection;

/**
 * {@link OXThreadMarker} - The interface to mark such <code>Thread</code>s that are spawned by Open-Xchange Server's thread pool.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface OXThreadMarker {

    /**
     * Checks whether this thread is processing an HTTP request
     *
     * @return <code>true</code> if processing an HTTP request; otherwise <code>false</code>
     */
    boolean isHttpRequestProcessing();

    /**
     * Sets whether this thread is processing an HTTP request
     *
     * @param httpProcessing <code>true</code> if processing an HTTP request; otherwise <code>false</code>
     */
    void setHttpRequestProcessing(boolean httpProcessing);

    /**
     * Gets the thread-local value associated with given key.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @return The value or <code>null</code>
     */
    <V> V getThreadLocalValue(String key);

    /**
     * Puts given key-value-association into thread-local map provided. Any existing value will be replaced.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @param value The value to put
     * @return The previous value associated with the specified key, or <code>null</code> if there was no mapping for the key
     */
    <V> V putThreadLocalValue(String key, V value);

    /**
     * Removes the thread-local value associated with given key.
     *
     * @param <V> The type of the value
     * @param key The key referencing the value
     * @return The removed value or <code>null</code>
     */
    <V> V removeThreadLocalValue(String key);

    /**
     * Removes the thread-local values associated with given keys.
     *
     * @param <V> The type of the value
     * @param keys The keys referencing the values
     */
    void removeThreadLocalValues(Collection<String> keys);

    /**
     * Removes the thread-local values whose key starts with given prefix.
     *
     * @param prefix The prefix
     */
    void removeThreadLocalValuesByPrefix(String prefix);
}
