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

import java.util.Optional;
import java.util.function.Supplier;
import com.openexchange.exception.OXException;
import com.openexchange.health.MWHealthCheck;
import com.openexchange.redis.OperationMode;
import com.openexchange.redis.RedisConnectorProvider;
import com.openexchange.redis.internal.cluster.RedisClusterConnector;
import com.openexchange.redis.internal.cluster.RedisClusterHealthCheck;
import com.openexchange.redis.internal.connectors.RedisClusterConnectorProvider;
import com.openexchange.redis.internal.connectors.RedisSentinelConnectorProvider;
import com.openexchange.redis.internal.connectors.RedisStandAloneConnectorProvider;
import com.openexchange.redis.internal.sentinel.RedisSentinelConnector;
import com.openexchange.redis.internal.sentinel.RedisSentinelHealthCheck;
import com.openexchange.redis.internal.standalone.RedisStandAloneConnector;
import com.openexchange.redis.internal.standalone.RedisStandAloneHealthCheck;
import com.openexchange.server.ServiceLookup;

/**
 * {@link RedisConnectorInitializer} - Initializes a Redis connector from given properties.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisConnectorInitializer {

    private final RedisConfiguration configuration;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link RedisConnectorInitializer}.
     *
     * @param configuration The configuration to use
     * @param services The seervice look-up
     */
    public RedisConnectorInitializer(RedisConfiguration configuration, ServiceLookup services) {
        super();
        this.configuration = configuration;
        this.services = services;
    }

    /**
     * Determines the configured operation mode for the Redis connector.
     *
     * @param configService The configuration service to read property from
     * @return The operation mode
     * @throws OXException If specified operation mode is invalid/unknown
     */
    private OperationMode determineOperationMode() throws OXException {
        String sOperationMode = configuration.getMode();
        Optional<OperationMode> optOperationMode = OperationMode.operationModeFor(sOperationMode);
        if (optOperationMode.isEmpty()) {
            throw OXException.general("Invalid Redis operation mode specified: " + sOperationMode);
        }
        return optOperationMode.get();
    }

    /**
     * Initializes the connector.
     *
     * @return The result
     * @throws OXException If initialization fails
     */
    public RedisConnectorResult init() throws OXException {
        AbstractRedisConnector<?, ?> redisConnector = null;
        try {
            Supplier<RedisConnectorProvider> connectorProviderSupplier;
            Supplier<MWHealthCheck> healthCheckSupplier;

            OperationMode mode = determineOperationMode();
            switch (mode) {
                case CLUSTER:
                    {
                        RedisClusterConnector redisClusterConnector = RedisClusterConnector.newInstance(configuration, services);
                        redisConnector = redisClusterConnector;
                        connectorProviderSupplier = () -> new RedisClusterConnectorProvider(redisClusterConnector);
                        healthCheckSupplier = () -> new RedisClusterHealthCheck(redisClusterConnector);
                    }
                    break;
                case SENTINEL:
                    {
                        RedisSentinelConnector redisSentinelConnector = RedisSentinelConnector.newInstance(configuration, services);
                        redisConnector = redisSentinelConnector;
                        connectorProviderSupplier = () -> new RedisSentinelConnectorProvider(redisSentinelConnector);
                        healthCheckSupplier = () -> new RedisSentinelHealthCheck(redisSentinelConnector);
                    }
                    break;
                case STAND_ALONE:
                    {
                        RedisStandAloneConnector redisStandAloneConnector = RedisStandAloneConnector.newInstance(configuration, services);
                        redisConnector = redisStandAloneConnector;
                        connectorProviderSupplier = () -> new RedisStandAloneConnectorProvider(redisStandAloneConnector);
                        healthCheckSupplier = () -> new RedisStandAloneHealthCheck(redisStandAloneConnector);
                    }
                    break;
                default:
                    throw OXException.general("Invalid Redis operation mode specified: " + mode.getIdentifier());
            }

            RedisConnectorResult result = new RedisConnectorResult(redisConnector, connectorProviderSupplier, healthCheckSupplier);
            redisConnector = null;
            return result;
        } finally {
            if (redisConnector != null) {
                redisConnector.shutdown();
            }
        }
    }

}
