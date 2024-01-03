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

package com.openexchange.redis.internal.sentinel;

import java.io.InputStream;
import java.time.Duration;
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
import io.lettuce.core.ClientOptions;
import io.lettuce.core.ReadFrom;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.masterreplica.MasterReplica;
import io.lettuce.core.masterreplica.StatefulRedisMasterReplicaConnection;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.support.ConnectionPoolSupport;
import io.micrometer.core.instrument.Metrics;
import net.jodah.failsafe.CircuitBreaker;

/**
 * {@link RedisSentinelConnector} - Connector for a Redis Sentinel.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class RedisSentinelConnector extends AbstractRedisConnector<RedisClient, StatefulRedisMasterReplicaConnection<String, InputStream>> {

    /**
     * Creates a new connector instance.
     *
     * @param configuration The configuration to use
     * @param services The service look-up providing required services
     * @return The new connector instance
     * @throws OXException If initialization fails
     */
    public static RedisSentinelConnector newInstance(RedisConfiguration configuration, ServiceLookup services) throws OXException {
        // Get Redis URI
        RedisURI redisURI = createRedisURIsFromConfig(configuration, optVersion(services), OperationMode.SENTINEL).get(0);

        // Builder for client resources
        ClientResources.Builder resourcesBuilder = ClientResources.builder();

        // Enable metrics
        boolean enableCommandLatencyMetrics = configuration.getLatencyMetrics();
        if (enableCommandLatencyMetrics) {
            MicrometerOptions options = MicrometerOptions.create();
            resourcesBuilder.commandLatencyRecorder(new MicrometerCommandLatencyRecorder(Metrics.globalRegistry, options));
        }

        return CloseOnExceptionCallable.execute(new CloseOnExceptionCallable<RedisSentinelConnector>() {

            @Override
            protected RedisSentinelConnector doCall() throws Exception {
                // Build client
                RedisClient redisClient = addAndReturnCloseable(RedisClient.create(resourcesBuilder.build(), redisURI));

                // Client options
                ClientOptions.Builder clientOptions = ClientOptions.builder();
                long commandTimeoutMillis = configuration.getCommandTimeoutMillis();
                long connectTimeoutMillis = configuration.getConnectTimeoutMillis();
                initClientOptions(clientOptions, commandTimeoutMillis, connectTimeoutMillis, redisURI, services);
                redisClient.setOptions(clientOptions.build());

                // Circuit breaker
                Optional<CircuitBreaker> optCircuitBreaker = initCircuitBreaker(redisURI, configuration);

                GenericObjectPool<StatefulRedisMasterReplicaConnection<String, InputStream>> pool = addAndReturnCloseable(ConnectionPoolSupport.createGenericObjectPool(() -> newConnection(redisClient, commandTimeoutMillis, redisURI), getPoolConfig(configuration)));
                RedisSentinelConnector connector = new RedisSentinelConnector(redisURI, redisClient, pool, optCircuitBreaker.orElse(null), services);
                checkReachability(RedisPingOperation.getInstance(), redisURI, connector);
                return connector;
            }
        }, ERROR_CONVERTER);
    }

    private static StatefulRedisMasterReplicaConnection<String, InputStream> newConnection(RedisClient redisClient, long timeoutMillis, RedisURI redisURI) {
        StatefulRedisMasterReplicaConnection<String, InputStream> connection = MasterReplica.connect(redisClient, ByteArrayRedisCodec.getInstance(), redisURI);
        connection.setTimeout(Duration.ofMillis(timeoutMillis < 0 ? 0 : timeoutMillis));
        // ReadFrom.REPLICA_PREFERRED vs. ReadFrom.MASTER_PREFERRED
        connection.setReadFrom(ReadFrom.MASTER_PREFERRED);
        return connection;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link RedisSentinelConnector}.
     *
     * @param redisURI The Redis URI for which the connector has been created
     * @param redisClient The Redis client
     * @param pool The connection pool
     * @param optCircuitBreaker The circuit breaker or <code>null</code>
     * @param services The service look-up
     * @throws OXException If initialization fails
     */
    private RedisSentinelConnector(RedisURI redisURI, RedisClient redisClient, GenericObjectPool<StatefulRedisMasterReplicaConnection<String, InputStream>> pool, CircuitBreaker optCircuitBreaker, ServiceLookup services) {
        super(redisURI, redisClient, pool, optCircuitBreaker, services);
    }

    @Override
    protected RedisCommandsProvider getCommandsProviderFor(StatefulRedisMasterReplicaConnection<String, InputStream> connection) {
        return new RedisSentinelCommandsProvider(connection);
    }

    @Override
    public StatefulRedisPubSubConnection<String, JSONObject> newPubSubConnection() {
        return redisClient.connectPubSub(JSONCodec.getInstance());
    }

    @Override
    public OperationMode getMode() {
        return OperationMode.SENTINEL;
    }

}
