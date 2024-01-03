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

import java.util.Optional;
import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.exception.OXException;

/**
 * {@link BasicCoreClusterMapProvider} - A basic class to obtain a cluster map.
 * <p>
 * If possible service-using classes inherit from this class to facilitate access to cluster map.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 * @param <V> The value type
 */
public class BasicCoreClusterMapProvider<V> {

    /**
     * Creates a new builder.
     *
     * @return The builder
     */
    public static <V> Builder<V> builder() {
        return new Builder<V>();
    }

    /**
     * The builder for an instance of <b>BasicCoreClusterMapProvider</b>
     * 
     * @param <V> The value type
     */
    public static class Builder<V> {

        private CoreMap coreMap;
        private MapCodec<V> codec;
        private long expireMillis;
        private ClusterMapServiceSupplier serviceSupplier;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Sets the core map.
         *
         * @param coreMap The core map to set
         * @return This builder for chained invocations
         */
        public Builder<V> withCoreMap(CoreMap coreMap) {
            this.coreMap = coreMap;
            return this;
        }

        /**
         * Sets the codec for serialization/deserialization from/to cluster map.
         *
         * @param codec The codec to set
         * @return This builder for chained invocations
         */
        public Builder<V> withCodec(MapCodec<V> codec) {
            this.codec = codec;
            return this;
        }

        /**
         * Sets the optional default expire time, in milliseconds, that is applied to cluster map.
         *
         * @param expireMillis The expire time to set
         * @return This builder for chained invocations
         */
        public Builder<V> withExpireMillis(long expireMillis) {
            this.expireMillis = expireMillis;
            return this;
        }

        /**
         * Sets the service supplier for the cluster map service reference.
         *
         * @param serviceSupplier The service supplier to set
         * @return This builder for chained invocations
         */
        public Builder<V> withServiceSupplier(ClusterMapServiceSupplier serviceSupplier) {
            this.serviceSupplier = serviceSupplier;
            return this;
        }

        /**
         * Creates the resulting instance of <b>BasicCoreClusterMapProvider</b> from this builder's arguments.
         *
         * @return The instance of <b>BasicCoreClusterMapProvider</b>
         */
        public BasicCoreClusterMapProvider<V> build() {
            return new BasicCoreClusterMapProvider<>(coreMap, codec, expireMillis, serviceSupplier);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    /** The core map used by this provider */
    protected final CoreMap coreMap;

    /** The map codec for serialization/deserialization from/to cluster map */
    protected final MapCodec<V> codec;

    /** The optional default expire time, in milliseconds, that is applied to cluster map */
    protected final long expireMillis;

    /** The optional supplier for the cluster map service reference */
    protected Optional<ClusterMapServiceSupplier> optionalServiceSupplier;

    /**
     * Initializes a new {@link BasicCoreClusterMapProvider} accepting a default expire time.
     * <p>
     * Meaning; methods, that do potentially add a new association into map, which do not accept expire time parameter will apply that
     * default expire time then; e.g.
     * <pre>
     * clusterMap.put(key, object); // Applies default expire time
     * </pre>
     *
     * @param coreMap The core map used by this provider
     * @param codec The map codec for serialization/deserialization from/to cluster map
     * @param expireMillis The specified default expire time, in milliseconds, or less than/equal to <code>0</code> (zero)
     * @param optionalServiceSupplier The supplier for the cluster map service reference or <code>null</code>
     */
    protected BasicCoreClusterMapProvider(CoreMap coreMap, MapCodec<V> codec, long expireMillis, ClusterMapServiceSupplier optionalServiceSupplier) {
        super();
        this.coreMap = coreMap;
        this.codec = codec;
        this.expireMillis = expireMillis;
        this.optionalServiceSupplier = optionalServiceSupplier == null ? Optional.empty() : Optional.of(optionalServiceSupplier);
    }

    /**
     * Gets the cluster map using the service reference provided by optional supplier passed as constructor argument.
     *
     * @return The cluster map
     * @throws OXException If optional supplier has not been specified on construction
     */
    public ClusterMap<V> getMap() throws OXException {
        if (optionalServiceSupplier.isEmpty()) {
            throw OXException.general("No supplier given");
        }
        return getMap(optionalServiceSupplier.get().get());
    }

    /**
     * Gets the cluster map using given service reference.
     *
     * @param clusterMapService The cluster map service reference
     * @return The cluster map
     * @throws OXException If given service reference is <code>null</code>
     */
    public ClusterMap<V> getMap(ClusterMapService clusterMapService) throws OXException {
        if (clusterMapService == null) {
            throw OXException.general("Cluster map service must not be null");
        }
        return expireMillis > 0 ? clusterMapService.getMap(coreMap, codec, expireMillis) : clusterMapService.getMap(coreMap, codec);
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    /**
     * {@link ClusterMapServiceSupplier} - Supplies the cluster map service to use for obtaining a certain cluster map.
     *
     * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
     */
    @FunctionalInterface
    public static interface ClusterMapServiceSupplier {

        /**
         * Gets the cluster map service.
         *
         * @return The cluster map service
         * @throws OXException If cluster map service cannot be provided
         */
        ClusterMapService get() throws OXException;
    }

}
