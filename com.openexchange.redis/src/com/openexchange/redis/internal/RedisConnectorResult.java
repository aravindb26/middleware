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

package com.openexchange.redis.internal;

import java.util.function.Supplier;
import com.openexchange.health.MWHealthCheck;
import com.openexchange.redis.RedisConnectorProvider;

/**
 * {@link RedisConnectorResult} - The Redis connector result.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisConnectorResult {

    private final AbstractRedisConnector<?, ?> redisConnector;
    private final Supplier<RedisConnectorProvider> connectorProviderSupplier;
    private final Supplier<MWHealthCheck> healthCheckSupplier;

    /**
     * Initializes a new {@link RedisConnectorResult}.
     *
     * @param redisConnector The Redis connector
     * @param connectorServiceSupplier The supplier for the appropriate connector service
     * @param healthCheckSupplier The supplier for health check
     */
    public RedisConnectorResult(AbstractRedisConnector<?, ?> redisConnector, Supplier<RedisConnectorProvider> connectorProviderSupplier, Supplier<MWHealthCheck> healthCheckSupplier) {
        super();
        this.redisConnector = redisConnector;
        this.connectorProviderSupplier = connectorProviderSupplier;
        this.healthCheckSupplier = healthCheckSupplier;
    }

    /**
     * Gets the connector.
     *
     * @return The connector
     */
    public AbstractRedisConnector<?, ?> getRedisConnector() {
        return redisConnector;
    }

    /**
     * Creates the connector provider for Redis connector.
     *
     * @return The connector provider
     */
    public RedisConnectorProvider createConnectorProvider() {
        return connectorProviderSupplier.get();
    }

    /**
     * Creates the health check for Redis connector.
     *
     * @return The health check
     */
    public MWHealthCheck createHealthCheck() {
        return healthCheckSupplier.get();
    }

}
