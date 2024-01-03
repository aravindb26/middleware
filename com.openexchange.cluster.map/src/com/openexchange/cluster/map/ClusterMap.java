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

package com.openexchange.cluster.map;

import java.util.Set;
import com.openexchange.exception.OXException;

/**
 * {@link ClusterMap} - A typed cluster map.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 * @param <V> The type for map values
 */
public interface ClusterMap<V> {

    /**
     * Checks if this map contains a mapping for the specified key.
     *
     * @param key The key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     * @throws OXException If operation fails
     */
    boolean containsKey(String key) throws OXException;

    /**
     * Gets the value to which the specified key is mapped,
     * or {@code null} if this map contains no mapping for the key.
     *
     * @param key The key whose associated value is to be returned
     * @return The value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     * @throws OXException If operation fails
     */
    V get(String key) throws OXException;

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).
     * <p>
     * If the map previously contained a mapping for the key, the old
     * value is replaced by the specified value.
     *
     * @param key The key with which the specified value is to be associated
     * @param value The value to be associated with the specified key
     * @return The previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key},
     *         if the implementation supports {@code null} values.)
     * @throws OXException If operation fails
     */
    default V put(String key, V value) throws OXException {
        return put(key, value, 0);
    }

    /**
     * Associates the specified value with the specified key in this map
     * (optional operation).
     * <p>
     * If the map previously contained a mapping for the key, the old
     * value is replaced by the specified value.
     *
     * @param key The key with which the specified value is to be associated
     * @param value The value to be associated with the specified key
     * @param expireMillis The expire time, in milliseconds, which specifies the time-to-live for added association. A value less than/equal to <code>0</code> (zero) means infinite time-to-live.
     * @return The previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with {@code key},
     *         if the implementation supports {@code null} values.)
     * @throws OXException If operation fails
     */
    V put(String key, V value, long expireMillis) throws OXException;

    /**
     * Removes the mapping for a key from this map if it is present
     * (optional operation).
     *
     * @param key The key whose mapping is to be removed from the map
     * @return The previous value associated with {@code key}, or
     *         {@code null} if there was no mapping for {@code key}.
     * @throws OXException If operation fails
     */
    V remove(String key) throws OXException;

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     * <p>
     * <b>WARNING:</b> The underlying cache might not support an atomic replacing of values. Try to avoid using this method.
     *
     * @param key The key with which the specified value is associated
     * @param oldValue The value expected to be associated with the specified key
     * @param newValue The value to be associated with the specified key
     * @return <code>true</code> if the value has change from <code>oldValue</code> to <code>newValue</code>, <code>false</code> if the value associated with the key didn't change
     * @throws OXException If operation fails
     */
    default boolean replace(String key, V oldValue, V newValue) throws OXException {
        return replace(key, oldValue, newValue, 0);
    }

    /**
     * Replaces the entry for the specified key only if currently
     * mapped to the specified value.
     * <p>
     * <b>WARNING:</b> The underlying cache might not support an atomic replacing of values. Try to avoid using this method.
     *
     * @param key The key with which the specified value is associated
     * @param oldValue The value expected to be associated with the specified key
     * @param newValue The value to be associated with the specified key
     * @param expireMillis The expire time, in milliseconds, which specifies the time-to-live for added association. A value less than/equal to <code>0</code> (zero) means infinite time-to-live.
     * @return <code>true</code> if the value has change from <code>oldValue</code> to <code>newValue</code>, <code>false</code> if the value associated with the key didn't change
     * @throws OXException If operation fails
     */
    boolean replace(String key, V oldValue, V newValue, long expireMillis) throws OXException;

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns
     * {@code null}, else returns the current value.
     *
     * @param key The key with which the specified value is to be associated
     * @param value The value to be associated with the specified key
     * @param expireMillis The expire time, in milliseconds, which specifies the time-to-live for added association. A value less than/equal to <code>0</code> (zero) means infinite time-to-live.
     * @return The previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws OXException If operation fails
     */
    default V putIfAbsent(String key, V value) throws OXException {
        return putIfAbsent(key, value, 0);
    }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns
     * {@code null}, else returns the current value.
     *
     * @param key The key with which the specified value is to be associated
     * @param value The value to be associated with the specified key
     * @param expireMillis The expire time, in milliseconds, which specifies the time-to-live for added association. A value less than/equal to <code>0</code> (zero) means infinite time-to-live.
     * @return The previous value associated with the specified key, or
     *         {@code null} if there was no mapping for the key.
     *         (A {@code null} return can also indicate that the map
     *         previously associated {@code null} with the key,
     *         if the implementation supports null values.)
     * @throws OXException If operation fails
     */
    V putIfAbsent(String key, V value, long expireMillis) throws OXException;

    /**
     * Gets the key set.
     * <p>
     * Any changes to returned set are not reflected to this cluster map
     *
     * @return The key set
     * @throws OXException If operation fails
     */
    Set<String> keySet() throws OXException;

}
