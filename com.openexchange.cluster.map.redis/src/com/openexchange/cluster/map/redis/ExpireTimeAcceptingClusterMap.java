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

package com.openexchange.cluster.map.redis;

import java.util.Set;
import com.openexchange.cluster.map.ClusterMap;
import com.openexchange.exception.OXException;


/**
 * {@link ExpireTimeAcceptingClusterMap} - The Redis cluster map accepting a default expire time that is applied to newly added
 * associations for which no dedicated expire time is given.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 * @param <V> The type for map values
 */
public class ExpireTimeAcceptingClusterMap<V> implements ClusterMap<V> {

    private final ClusterMap<V> clusterMap;
    private final long expireTimeMillis;

    /**
     * Initializes a new {@link ExpireTimeAcceptingClusterMap}.
     *
     * @param clusterMap The backing cluster map
     * @param expireTimeMillis The specified expire time, in milliseconds, or less than/equal to <code>0</code> (zero)
     */
    public ExpireTimeAcceptingClusterMap(ClusterMap<V> clusterMap, long expireTimeMillis) {
        super();
        this.clusterMap = clusterMap;
        this.expireTimeMillis = expireTimeMillis;
    }

    @Override
    public boolean containsKey(String key) throws OXException {
        return clusterMap.containsKey(key);
    }

    @Override
    public V get(String key) throws OXException {
        return clusterMap.get(key);
    }

    @Override
    public V put(String key, V value) throws OXException {
        return expireTimeMillis > 0 ? clusterMap.put(key, value, expireTimeMillis) : clusterMap.put(key, value);
    }

    @Override
    public V remove(String key) throws OXException {
        return clusterMap.remove(key);
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue) throws OXException {
        return expireTimeMillis > 0 ? clusterMap.replace(key, oldValue, newValue, expireTimeMillis) : clusterMap.replace(key, oldValue, newValue);
    }

    @Override
    public V putIfAbsent(String key, V value) throws OXException {
        return expireTimeMillis > 0 ? clusterMap.putIfAbsent(key, value, expireTimeMillis) : clusterMap.putIfAbsent(key, value);
    }

    @Override
    public Set<String> keySet() throws OXException {
        return clusterMap.keySet();
    }

    @Override
    public V put(String key, V value, long expireTimeMillis) throws OXException {
        return clusterMap.put(key, value, expireTimeMillis);
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue, long expireTimeMillis) throws OXException {
        return clusterMap.replace(key, oldValue, newValue, expireTimeMillis);
    }

    @Override
    public V putIfAbsent(String key, V value, long expireTimeMillis) throws OXException {
        return clusterMap.putIfAbsent(key, value, expireTimeMillis);
    }
}
