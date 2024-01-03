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

package com.openexchange.redis.internal.cluster;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.java.CloseOnExceptionCallable;
import com.openexchange.redis.OperationMode;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.redis.internal.AbstractRedisConnector;
import com.openexchange.redis.internal.RedisConfiguration;
import com.openexchange.redis.internal.RedisPingOperation;
import com.openexchange.redis.internal.codecs.ByteArrayRedisCodec;
import com.openexchange.redis.internal.codecs.JSONCodec;
import com.openexchange.server.ServiceLookup;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.micrometer.core.instrument.Metrics;
import net.jodah.failsafe.CircuitBreaker;

/**
 * {@link RedisClusterConnector} - Connector for a Redis Cluster.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class RedisClusterConnector extends AbstractRedisConnector<RedisClusterClient, StatefulRedisClusterConnection<String, InputStream>> {

    /**
     * Creates a new cluster connector instance.
     *
     * @param configuration The configuration to use
     * @param services The service look-up providing required services
     * @return The new cluster connector instance
     * @throws OXException If initialization fails
     */
    public static RedisClusterConnector newInstance(RedisConfiguration configuration, ServiceLookup services) throws OXException {
        // Get Redis URI
        List<RedisURI> redisURIs = createRedisURIsFromConfig(configuration, optVersion(services), OperationMode.CLUSTER);

        // Builder for client resources
        ClientResources.Builder resourcesBuilder = ClientResources.builder();

        // Enable metrics
        boolean enableCommandLatencyMetrics = configuration.getLatencyMetrics();
        if (enableCommandLatencyMetrics) {
            MicrometerOptions options = MicrometerOptions.create();
            resourcesBuilder.commandLatencyRecorder(new MicrometerCommandLatencyRecorder(Metrics.globalRegistry, options));
        }

        return CloseOnExceptionCallable.execute(new CloseOnExceptionCallable<RedisClusterConnector>() {

            @Override
            protected RedisClusterConnector doCall() throws Exception {
                // Build client
                RedisClusterClient redisClient = addAndReturnCloseable(RedisClusterClient.create(resourcesBuilder.build(), redisURIs));

                // Client options
                RedisURI firstRedisURI = redisURIs.get(0);
                ClusterClientOptions.Builder clientOptions = ClusterClientOptions.builder();
                long commandTimeoutMillis = configuration.getCommandTimeoutMillis();
                long connectTimeoutMillis = configuration.getConnectTimeoutMillis();
                initClientOptions(clientOptions, commandTimeoutMillis, connectTimeoutMillis, firstRedisURI, services);
                {
                    ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                        .enablePeriodicRefresh(Duration.ofMinutes(10))
                        .enableAllAdaptiveRefreshTriggers()
                        .build();
                    clientOptions.topologyRefreshOptions(topologyRefreshOptions);
                }
                redisClient.setOptions(clientOptions.build());

                // Circuit breaker
                Optional<CircuitBreaker> optCircuitBreaker = initCircuitBreaker(firstRedisURI, configuration);

                GenericObjectPool<StatefulRedisClusterConnection<String, InputStream>> pool = addAndReturnCloseable(ConnectionPoolSupport.createGenericObjectPool(() -> newConnection(redisClient, commandTimeoutMillis), getPoolConfig(configuration)));
                RedisClusterConnector connector = new RedisClusterConnector(firstRedisURI, redisClient, pool, optCircuitBreaker.orElse(null), services);
                checkReachability(RedisPingOperation.getInstance(), firstRedisURI, connector);
                return connector;
            }
        }, ERROR_CONVERTER);
    }

    private static StatefulRedisClusterConnection<String, InputStream> newConnection(RedisClusterClient redisClient, long timeoutMillis) {
        StatefulRedisClusterConnection<String, InputStream> connection = redisClient.connect(ByteArrayRedisCodec.getInstance());
        connection.setTimeout(Duration.ofMillis(timeoutMillis < 0 ? 0 : timeoutMillis));
        connection.setReadFrom(ReadFrom.REPLICA_PREFERRED);
        return connection;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link RedisClusterConnector}.
     *
     * @param redisURI The Redis URI for which the connector has been created
     * @param redisClient The Redis client
     * @param pool The connection pool
     * @param optCircuitBreaker The circuit breaker or <code>null</code>
     * @param services The service look-up
     * @throws OXException If initialization fails
     */
    private RedisClusterConnector(RedisURI redisURI, RedisClusterClient redisClient, GenericObjectPool<StatefulRedisClusterConnection<String, InputStream>> pool, CircuitBreaker optCircuitBreaker, ServiceLookup services) {
        super(redisURI, redisClient, pool, optCircuitBreaker, services);
    }

    @Override
    protected RedisCommandsProvider getCommandsProviderFor(StatefulRedisClusterConnection<String, InputStream> connection) {
        return new RedisClusterCommandsProvider(connection);
    }

    @Override
    public StatefulRedisPubSubConnection<String, JSONObject> newPubSubConnection() {
        return redisClient.connectPubSub(JSONCodec.getInstance());
    }

    @Override
    public OperationMode getMode() {
        return OperationMode.CLUSTER;
    }

}
