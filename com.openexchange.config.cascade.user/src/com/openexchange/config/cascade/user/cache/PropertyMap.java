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

package com.openexchange.config.cascade.user.cache;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.cascade.BasicProperty;

/**
 * {@link PropertyMap} - An in-memory property map with LRU eviction policy.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class PropertyMap {

    private final Cache<String, BasicProperty> cache;

    /**
     * Initializes a new {@link PropertyMap}.
     *
     * @param maxLifeMillis the max life milliseconds
     */
    public PropertyMap(int maxLifeMillis) {
        this(maxLifeMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Initializes a new {@link PropertyMap}.
     *
     * @param maxLifeUnits the max life units
     * @param unit the unit
     */
    public PropertyMap(int maxLifeUnits, TimeUnit unit) {
        super();
        cache = CacheBuilder.newBuilder().expireAfterWrite(maxLifeUnits, unit).build();
    }

    /**
     * Put if absent.
     *
     * @param propertyName the property name
     * @param property the property
     * @return The property
     */
    public BasicProperty putIfAbsent(final String propertyName, final BasicProperty property) {
        return this.cache.asMap().putIfAbsent(propertyName, property);
    }

    /**
     * Gets the size.
     *
     * @return The size
     */
    public int size() {
        return (int) cache.size();
    }

    /**
     * Checks if empty flag is set.
     *
     * @return <code>true</code> if empty flag is set; otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return size() <= 0;
    }

    /**
     * Contains.
     *
     * @param propertyName the property name
     * @return <code>true</code> if successful; otherwise <code>false</code>
     */
    public boolean contains(final String propertyName) {
        return cache.getIfPresent(propertyName) != null;
    }

    /**
     * Gets the property.
     *
     * @param propertyName the property name
     * @return The property or <code>null</code> if absent
     */
    public BasicProperty get(final String propertyName) {
        return cache.getIfPresent(propertyName);
    }

    /**
     * Puts specified property.
     *
     * @param propertyName the property name
     * @param property the property
     * @return The previous property or <code>null</code>
     */
    public BasicProperty put(final String propertyName, final BasicProperty property) {
        return this.cache.asMap().put(propertyName, property);
    }

    /**
     * Removes the property.
     *
     * @param propertyName the property name
     * @return The removed property or <code>null</code>
     */
    public BasicProperty remove(final String propertyName) {
        ConcurrentMap<String, BasicProperty> map = this.cache.asMap();
        return map.remove(propertyName);
    }

    /**
     * Clears this map.
     */
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public String toString() {
        return cache.toString();
    }

}
