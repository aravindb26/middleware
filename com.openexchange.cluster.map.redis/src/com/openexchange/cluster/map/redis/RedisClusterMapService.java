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

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.cluster.map.ApplicationName;
import com.openexchange.cluster.map.ClusterMap;
import com.openexchange.cluster.map.ClusterMapService;
import com.openexchange.cluster.map.MapName;
import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.redis.RedisConnector;

/**
 * {@link RedisClusterMapService} - The Redis cluster map service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisClusterMapService implements ClusterMapService {

    private final RedisConnector connector;
    private final Cache<Key, ClusterMap<?>> cachedClusterMaps;

    /**
     * Initializes a new {@link RedisClusterMapService}.
     *
     * @param connector The connector
     * @param enableCachingForClusterMaps Whether to enable caching for cluster maps or to create a new instance whenever requested
     */
    public RedisClusterMapService(RedisConnector connector, boolean enableCachingForClusterMaps) {
        super();
        this.connector = connector;
        cachedClusterMaps = enableCachingForClusterMaps ? CacheBuilder.newBuilder().expireAfterAccess(Duration.ofMinutes(30)).build() : null;
    }

    @Override
    public <V> ClusterMap<V> getMap(ApplicationName appName, MapName mapName, MapCodec<V> codec, long expireTimeMillis) {
        return new ExpireTimeAcceptingClusterMap<>(getMap(appName, mapName, codec), expireTimeMillis);
    }

    @Override
    public <V> ClusterMap<V> getMap(ApplicationName appName, MapName mapName, MapCodec<V> codec) {
        if (cachedClusterMaps == null) {
            return doGetMap(appName, mapName, codec);
        }

        Key key = new Key(appName, mapName, codec);
        ClusterMap<?> clusterMap = cachedClusterMaps.getIfPresent(key);
        if (clusterMap != null) {
            return (ClusterMap<V>) clusterMap;
        }

        try {
            return (ClusterMap<V>) cachedClusterMaps.get(key, () -> doGetMap(appName, mapName, codec));
        } catch (ExecutionException e) {
            throw new IllegalStateException("Cluster map could not be loaded", e.getCause() == null ? e : e.getCause());
        } catch (UncheckedExecutionException e) {
            throw new IllegalStateException("Cluster map could not be loaded", e.getCause() == null ? e : e.getCause());
        }
    }

    /**
     * Gets the cluster map for given arguments.
     *
     * @param appName The application name; e.g. <code>"ox-map"</code>
     * @param mapName The map name; e.g. <code>"distributedFiles"</code>
     * @param codec The codec to use for de-/serialization of the values
     * @param <V> The value type
     * @return The map
     */
    protected <V> ClusterMap<V> doGetMap(ApplicationName appName, MapName mapName, MapCodec<V> codec) {
        return new RedisClusterMap<V>(appName, mapName, codec, connector);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static class Key {

        private final ApplicationName appName;
        private final MapName mapName;
        private final MapCodec<?> codec;
        private final int hash;

        /**
         * Initializes a new {@link Key}.
         *
         * @param appName The application name
         * @param mapName The map name
         * @param codec The codec
         */
        Key(ApplicationName appName, MapName mapName, MapCodec<?> codec) {
            super();
            this.appName = appName;
            this.mapName = mapName;
            this.codec = codec;

            int prime = 31;
            int result = 1;
            result = prime * result + ((appName == null) ? 0 : appName.hashCode());
            result = prime * result + ((mapName == null) ? 0 : mapName.hashCode());
            result = prime * result + ((codec == null) ? 0 : codec.hashCode());
            this.hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            if (appName == null) {
                if (other.appName != null) {
                    return false;
                }
            } else if (!appName.equals(other.appName)) {
                return false;
            }
            if (mapName == null) {
                if (other.mapName != null) {
                    return false;
                }
            } else if (!mapName.equals(other.mapName)) {
                return false;
            }
            if (codec == null) {
                if (other.codec != null) {
                    return false;
                }
            } else if (!codec.equals(other.codec)) {
                return false;
            }
            return true;
        }
    }

}
