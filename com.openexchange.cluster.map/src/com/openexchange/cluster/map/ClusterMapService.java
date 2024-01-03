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

import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * {@link ClusterMapService} - The cluster map service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
@SingletonService
public interface ClusterMapService {

    /**
     * Gets the cluster map for core map.
     *
     * @param coreMap The core map name
     * @param codec The codec to use for de-/serialization of the values
     * @param <V> The value type
     * @return The map
     * @throws IllegalStateException In cases the cluster map can't be loaded
     */
    default <V> ClusterMap<V> getMap(CoreMap coreMap, MapCodec<V> codec) throws IllegalStateException {
        return getMap(coreMap.getApplicationName(), coreMap.getMapName(), codec);
    }

    /**
     * Gets the cluster map for given names.
     *
     * @param appName The application name; e.g. <code>"ox-map"</code>
     * @param mapName The map name; e.g. <code>"distributedFiles"</code>
     * @param codec The codec to use for de-/serialization of the values
     * @param <V> The value type
     * @return The map
     * @throws IllegalStateException In cases the cluster map can't be loaded
     */
    <V> ClusterMap<V> getMap(ApplicationName appName, MapName mapName, MapCodec<V> codec) throws IllegalStateException;

    /**
     * Gets the cluster map for given core map having specified expire time as default.
     * <p>
     * Meaning; methods, that do potentially add a new association into map, which do not accept expire time parameter will apply that
     * default expire time then; e.g.
     * <pre>
     *  clusterMap.put(key, object); // Applies default expire time
     * </pre>
     *
     * @param coreMap The core map name
     * @param codec The codec to use for de-/serialization of the values
     * @param expireMillis The specified expire time, in milliseconds, or less than/equal to <code>0</code> (zero)
     * @param <V> The value type
     * @return The map
     */
    default <V> ClusterMap<V> getMap(CoreMap coreMap, MapCodec<V> codec, long expireMillis) {
        return getMap(coreMap.getApplicationName(), coreMap.getMapName(), codec, expireMillis);
    }

    /**
     * Gets the cluster map for given names having specified expire time as default.
     * <p>
     * Meaning; methods, that do potentially add a new association into map, which do not accept expire time parameter will apply that
     * default expire time then; e.g.
     * <pre>
     *  clusterMap.put(key, object); // Applies default expire time
     * </pre>
     *
     * @param appName The application name; e.g. <code>"ox-map"</code>
     * @param mapName The map name; e.g. <code>"distributedFiles"</code>
     * @param codec The codec to use for de-/serialization of the values
     * @param expireMillis The specified expire time, in milliseconds, or less than/equal to <code>0</code> (zero)
     * @param <V> The value type
     * @return The map
     */
    <V> ClusterMap<V> getMap(ApplicationName appName, MapName mapName, MapCodec<V> codec, long expireMillis);

}
