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

import static com.eaio.util.text.HumanTime.exactly;
import static com.openexchange.exception.ExceptionUtils.dropStackTraceFor;
import static com.openexchange.exception.ExceptionUtils.getLastChainedThrowable;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jctools.maps.NonBlockingHashMap;
import org.slf4j.Logger;
import com.google.common.collect.ImmutableList;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.openexchange.redis.OperationMode;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.redis.RedisConnector;
import com.openexchange.redis.RedisConversionException;
import com.openexchange.redis.RedisExceptionCode;
import com.openexchange.redis.RedisOperation;
import com.openexchange.redis.RedisOperationKey;
import com.openexchange.server.ServiceLookup;
import com.openexchange.version.Version;
import com.openexchange.version.VersionService;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisCommandTimeoutException;
import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.RedisException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.RedisURI.Builder;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.SslOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.TimeoutOptions.TimeoutSource;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.protocol.RedisCommand;
import io.netty.handler.ssl.SslContextBuilder;
import net.jodah.failsafe.CircuitBreaker;
import net.jodah.failsafe.CircuitBreakerOpenException;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeException;
import net.jodah.failsafe.function.Predicate;
import net.jodah.failsafe.util.Ratio;

/**
 * {@link AbstractRedisConnector} - The abstract connector for a Redis storage.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 * @param <R> The type of the lettuce Redis client
 * @param <C> The type of the lettuce Redis connection
 */
public abstract class AbstractRedisConnector<R extends AbstractRedisClient, C extends StatefulConnection<String, InputStream>> implements RedisConnector {

    /** The logger constant */
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AbstractRedisConnector.class);

    /** The error converter for <code>OXException</code> */
    protected static final Function<Exception, OXException> ERROR_CONVERTER = e -> e instanceof OXException oxe ? oxe : OXException.general("Callable execution failed", e);

    /**
     * Gets the configuration for the connection pool.
     *
     * @param <C> The type of the Redis connection managed in pool
     * @param configService The config service to use
     * @return The pool config
     */
    public static <C extends StatefulConnection<String, InputStream>> GenericObjectPoolConfig<C> getPoolConfig(RedisConfiguration configuration) {
        GenericObjectPoolConfig<C> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(configuration.getConnectionPoolMaxTotal());
        poolConfig.setMaxIdle(configuration.getConnectionPoolMaxIdle());
        poolConfig.setMinIdle(configuration.getConnectionPoolMinIdle());
        poolConfig.setMaxWait(Duration.ofSeconds(configuration.getConnectionPoolMaxWaitSeconds()));
        poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(configuration.getConnectionPoolMinIdleSeconds()));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(configuration.getConnectionPoolCleanerRunSeconds()));
        poolConfig.setJmxEnabled(true);
        poolConfig.setJmxNameBase("com.openexchange.redis.connection.pool:type=RedisConnectionPool,name=");
        poolConfig.setJmxNamePrefix("pool");
        return poolConfig;
    }

    /**
     * Initializes the options for the Redis client.
     *
     * @param clientOptions The client options builder to decorate
     * @param commandTimeoutMillis The command timeout in milliseconds
     * @param connectTimeoutMillis The connect timeout in milliseconds
     * @param redisURI The Redis URI providing end-point information; e.g. SSL
     * @param services The service look-up to acquire services
     * @throws OXException If initializing client options fails
     */
    public static void initClientOptions(ClientOptions.Builder clientOptions, long commandTimeoutMillis, long connectTimeoutMillis, RedisURI redisURI, ServiceLookup services) throws OXException {
        // Timeout
        {
            Set<String> listings = Set.of("mget", "mset", "msetnx", "scan", "keys");
            TimeoutSource source = new TimeoutSource() {

                @Override
                public long getTimeout(RedisCommand<?, ?, ?> command) {
                    String commandName = Strings.asciiLowerCase(command.getType().name());
                    return listings.contains(commandName) ? (commandTimeoutMillis << 3) : commandTimeoutMillis;
                }
            };
            clientOptions.timeoutOptions(TimeoutOptions.builder().timeoutCommands().timeoutSource(source).build());
        }

        // SSL
        SSLSocketFactoryProvider factoryProvider = services.getServiceSafe(SSLSocketFactoryProvider.class);
        SSLConfigurationService sslConfigService = services.getServiceSafe(SSLConfigurationService.class);

        Consumer<SslContextBuilder> contextBuilderCustomizer = sslContextBuilder -> {
            sslContextBuilder.ciphers(Arrays.asList(sslConfigService.getSupportedCipherSuites()));
            sslContextBuilder.protocols(sslConfigService.getSupportedProtocols());
            sslContextBuilder.startTls(redisURI.isStartTls());
            sslContextBuilder.trustManager(factoryProvider.getTrustManagerFactory());
        };

        SslOptions.Builder sslOptions = SslOptions.builder()
            .cipherSuites(sslConfigService.getSupportedCipherSuites())
            .protocols(sslConfigService.getSupportedProtocols())
            .sslContext(contextBuilderCustomizer);
        clientOptions.sslOptions(sslOptions.build());

        clientOptions.socketOptions(SocketOptions.builder().connectTimeout(Duration.ofMillis(connectTimeoutMillis)).build());
    }

    /**
     * Initializes the circuit breaker for the Redis connector.
     *
     * @param redisURI The Redis URI providing end-point information
     * @param configuration The configuration to use
     * @return The circuit breaker or empty
     * @throws OXException If initializing circuit breaker fails
     */
    public static Optional<CircuitBreaker> initCircuitBreaker(RedisURI redisURI, RedisConfiguration configuration) throws OXException {
        if (!configuration.getCircuitBreakerEnabled()) {
            // Circuit breaker not enabled
            LOGGER.info("Circuit breaker disabled for {}", redisURI);
            return Optional.empty();
        }

        int failures = configuration.getCircuitBreakerFailureThreshold();
        int failureExecutions = failures;
        {
            int num = configuration.getCircuitBreakerFailureExecutions();
            if (num > 0) {
                failureExecutions = num;
            }
        }
        if (failureExecutions < failures) {
            failureExecutions = failures;
        }

        int success = configuration.getCircuitBreakerSuccessThreshold();
        int successExecutions = success;
        {
            int num = configuration.getCircuitBreakerSuccessExecutions();
            if (num > 0) {
                successExecutions = num;
            }
        }
        if (successExecutions < success) {
            successExecutions = success;
        }

        long delayMillis = configuration.getCircuitBreakerDelayMillis();

        Ratio failureThreshold = ratioOf(failures, failureExecutions);
        Ratio successThreshold = ratioOf(success, successExecutions);
        CircuitBreaker circuitBreaker = new CircuitBreaker()
            .withFailureThreshold(failureThreshold.numerator, failureThreshold.denominator)
            .withSuccessThreshold(successThreshold.numerator, successThreshold.denominator)
            .withDelay(delayMillis, TimeUnit.MILLISECONDS)
            .failOn(PREDICATE_REDIS_FAILURE)
            .onOpen(() -> LOGGER.warn("Redis circuit breaker opened for: {}", redisURI))
            .onHalfOpen(() -> LOGGER.info("Redis circuit breaker half-opened for: {}", redisURI))
            .onClose(() -> LOGGER.info("Redis circuit breaker closed for: {}", redisURI));
        return Optional.of(circuitBreaker);
    }

    /** Specify when circuit breaker considers an exception a failure */
    private static final Predicate<? extends Throwable> PREDICATE_REDIS_FAILURE = throwable -> {
        if (throwable instanceof Error) {
            return true;
        }
        if (throwable instanceof OXException) {
            OXException oxe = (OXException) throwable;
            return RedisExceptionCode.CONNECT_FAILURE.equals(oxe) || RedisExceptionCode.CONNECTION_CLOSED.equals(oxe);
        }
        return false;
    };

    /**
     * Creates a new {@link Ratio} object avoiding division by zero. If the
     * denominator is zero then a new {@link Ratio} object will be created
     * with numerator equals to 0 and denominator equals to 1.
     *
     * @param numerator The numerator
     * @param denominator The denominator
     * @return The new {@link Ratio} object
     */
    private static Ratio ratioOf(int numerator, int denominator) {
        return denominator == 0 ? new Ratio(0, 1) : new Ratio(numerator, denominator);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /** The service look-up */
    protected final ServiceLookup services;

    /** The Redis client */
    protected final R redisClient;

    /** The connection pool */
    protected final GenericObjectPool<C> pool;

    /** The Redis URI for which the connector has been created */
    protected final RedisURI redisURI;

    /** The circuit breaker or <code>null</code> */
    protected final CircuitBreaker optCircuitBreaker;

    /** The map used for volatile operation results */
    protected final ConcurrentMap<RedisOperationKey<?>, Future<Object>> operationSynchronizer;

    /**
     * Initializes a new {@link AbstractRedisConnector}.
     *
     * @param redisURI The Redis URI for which the connector has been created
     * @param redisClient The Redis client
     * @param pool The connection pool
     * @param optCircuitBreaker The circuit breaker or <code>null</code>
     * @param services The service look-up
     */
    protected AbstractRedisConnector(RedisURI redisURI, R redisClient, GenericObjectPool<C> pool, CircuitBreaker optCircuitBreaker, ServiceLookup services) {
        super();
        this.operationSynchronizer = new NonBlockingHashMap<>();
        this.redisURI = redisURI;
        this.redisClient = redisClient;
        this.pool = pool;
        this.optCircuitBreaker = optCircuitBreaker;
        this.services = services;
    }

    /**
     * Gets the appropriate commands provider for given connection.
     *
     * @param connection The connection
     * @return The commands provider
     */
    protected abstract RedisCommandsProvider getCommandsProviderFor(C connection);

    /**
     * Gets the service look-up.
     *
     * @return The service look-up
     */
    public ServiceLookup getServices() {
        return services;
    }

    @Override
    public <V> V executeOperation(RedisOperationKey<?> operationKey, RedisOperation<V> operation) throws OXException {
        if (operationKey == null) {
            // No operation key given. Just execute operation.
            return executeOperation0(operation);
        }

        // Ensure that given operation is only executed by one thread
        boolean remove = false;
        try {
            Future<Object> result = operationSynchronizer.get(operationKey);
            if (result == null) {
                FutureTask<Object> ft = new FutureTask<>(new InvokeCallable<V, R, C>(operation, this));
                result = operationSynchronizer.putIfAbsent(operationKey, ft);
                if (result == null) {
                    // This thread acquired slot. Execute operation and mark for removal.
                    remove = true;
                    result = ft;
                    ft.run();
                }
            }

            return (V) result.get();
        } catch (InterruptedException e) {
            // Keep interrupted state
            Thread.currentThread().interrupt();
            throw RedisExceptionCode.UNEXPECTED_ERROR.create(e, "Interrupted while retrieving Redis operation result");
        } catch (ExecutionException e) {
            Throwable failure = e.getCause();
            if (failure instanceof OXException oxe) {
                throw oxe;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            throw RedisExceptionCode.UNEXPECTED_ERROR.create(failure, failure.getMessage());
        } finally {
            if (remove) {
                operationSynchronizer.remove(operationKey);
            }
        }
    }

    private <V> V executeOperation0(RedisOperation<V> operation) throws OXException {
        if (optCircuitBreaker == null || operation.omitCircuitBreaker()) {
            return doExecuteOperation(operation);
        }

        try {
            return Failsafe.with(optCircuitBreaker).get(new ExecuteOperationCallable<V, R, C>(operation, this));
        } catch (CircuitBreakerOpenException e) {
            // Circuit breaker is open
            throw RedisExceptionCode.CONNECT_FAILURE.create(new IOException("Denied connect attempt to Redis end-point since circuit breaker is open.", e), redisURI);
        } catch (FailsafeException e) {
            Throwable failure = e.getCause();
            if (failure instanceof OXException oxe) {
                throw oxe;
            }
            if (failure instanceof Error) {
                throw (Error) failure;
            }
            throw RedisExceptionCode.UNEXPECTED_ERROR.create(failure, failure.getMessage());
        }
    }

    private <V> V doExecuteOperation(RedisOperation<V> operation) throws OXException {
        Duration timeoutToRestore = null;
        C connection = null;
        try {
            connection = pool.borrowObject();
            timeoutToRestore = setTimeoutIfPresent(operation, connection);
            return operation.execute(getCommandsProviderFor(connection));
        } catch (OXException e) {
            throw e;
        } catch (RedisException e) {
            throw handleRedisException(e, redisURI, connection);
        } catch (IllegalArgumentException e) {
            throw RedisExceptionCode.REDIS_COMMAND_INVALID_ARGUMENTS.create(e, e.getMessage());
        } catch (Exception e) {
            RedisException optRedisException = ExceptionUtils.extractFrom(e, RedisException.class);
            if (optRedisException != null) {
                throw handleRedisException(optRedisException, redisURI, connection);
            }
            throw RedisExceptionCode.CONNECTION_POOL_ERROR.create(e, e.getMessage());
        } finally {
            setTimeoutSafe(timeoutToRestore, connection);
            returnToPoolQuietly(connection);
        }
    }

    private static <V, C extends StatefulConnection<String, InputStream>> Duration setTimeoutIfPresent(RedisOperation<V> operation, C connection) {
        Optional<Duration> optCommandTimeout = operation.getCommandTimeout();
        if (optCommandTimeout.isEmpty()) {
            return null;
        }

        Duration timeoutToRestore = connection.getTimeout();
        connection.setTimeout(optCommandTimeout.get());
        return timeoutToRestore;
    }

    private static <C extends StatefulConnection<String, InputStream>> OXException handleRedisException(RedisException e, RedisURI redisURI, C connection) {
        if (e instanceof RedisConnectionException) {
            return RedisExceptionCode.CONNECT_FAILURE.create(e, redisURI);
        }
        if (e instanceof RedisCommandTimeoutException) {
            if (connection != null) {
                Duration timeout = connection.getTimeout();
                if (timeout != null) {
                    long millis = timeout.toMillis();
                    return RedisExceptionCode.REDIS_COMMAND_TIMEOUT_WITH_MILLIS.create(e, format(millis), exactly(millis, true));
                }
            }
            return RedisExceptionCode.REDIS_COMMAND_TIMEOUT.create(e, e.getMessage());
        }
        if (e instanceof RedisCommandExecutionException) {
            return RedisExceptionCode.REDIS_COMMAND_ERROR.create(e, e.getMessage());
        }
        if (e instanceof RedisConversionException) {
            return RedisExceptionCode.CONVERSION_ERROR.create(e);
        }
        // io.lettuce.core.RedisException: Connection closed
        if ("Connection closed".equals(e.getMessage())) {
            return RedisExceptionCode.CONNECTION_CLOSED.create(e, redisURI);
        }
        return RedisExceptionCode.REDIS_ERROR.create(e, e.getMessage());
    }

    /**
     * Shuts down the connector
     */
    public void shutdown() {
        pool.close();
        redisClient.shutdown();
    }

    /**
     * Puts given connection back to pool.
     * <p>
     * If connection cannot be put back into pool, it is closed.
     *
     * @param connection The connection instance to put back or <code>null</code> (as no-op)
     */
    protected void returnToPoolQuietly(C connection) {
        if (connection != null) {
            C con = connection;
            try {
                pool.returnObject(con);
                con = null; // Successfully put into pool. Nullify to avoid closing.
            } catch (Exception e) {
                LOGGER.error("Failed to return Redis connection back to pool", e);
            } catch (Throwable t) { // NOSONARLINT
                LOGGER.error("Critical error while returning Redis connection back to pool", t);
                throw t;
            } finally {
                if (con != null) {
                    con.closeAsync();
                }
            }
        }
    }

    /**
     * Safely sets the given timeout on specified connection.
     *
     * @param <C> The connection type
     * @param timeout The timeout to set
     * @param connection The connection to apply timeout to
     */
    protected static <C extends StatefulConnection<String, InputStream>> void setTimeoutSafe(Duration timeout, C connection) {
        if (timeout != null && connection != null) {
            try {
                connection.setTimeout(timeout);
            } catch (Exception e) {
                LOGGER.error("Failed to set command timeout on Redis connection", e);
            }
        }
    }

    // --------------------------------------------------- Callables -----------------------------------------------------------------------

    /**
     * The callable implementation invoking given connector's <b>executeOperation0()</b> method.
     *
     * @param <V> The type of the return value
     * @param <R> The Redis client type
     * @param <C> The Redis connection type
     */
    private static class InvokeCallable<V, R extends AbstractRedisClient, C extends StatefulConnection<String, InputStream>> implements Callable<Object> {

        private final AbstractRedisConnector<R, C> connector;
        private final RedisOperation<V> operation;

        /**
         * Initializes a new {@link InvokeCallable}.
         *
         * @param operation The operation to execute
         * @param connector The connector to use
         */
        InvokeCallable(RedisOperation<V> operation, AbstractRedisConnector<R, C> connector) {
            super();
            this.operation = operation;
            this.connector = connector;
        }

        @Override
        public Object call() throws Exception {
            return connector.executeOperation0(operation);
        }
    }

    /**
     * The callable implementation invoking given connector's <b>doExecuteOperation()</b> method.
     *
     * @param <V> The type of the return value
     * @param <R> The Redis client type
     * @param <C> The Redis connection type
     */
    private static class ExecuteOperationCallable<V, R extends AbstractRedisClient, C extends StatefulConnection<String, InputStream>> implements Callable<V> {

        private final AbstractRedisConnector<R, C> connector;
        private final RedisOperation<V> operation;

        /**
         * Initializes a new {@link ExecuteOperationCallable}.
         *
         * @param operation The operation to execute
         * @param connector The connector to use
         */
        ExecuteOperationCallable(RedisOperation<V> operation, AbstractRedisConnector<R, C> connector) {
            super();
            this.operation = operation;
            this.connector = connector;
        }

        @Override
        public V call() throws Exception {
            return connector.doExecuteOperation(operation);
        }
    }

    // -------------------------------------------------------- Utilities ------------------------------------------------------------------

    /**
     * Gets the version of the Middleware (if appropriate service is available).
     *
     * @param services The tracked OSGi services
     * @return The version string or empty
     */
    protected static Optional<String> optVersion(ServiceLookup services) {
        VersionService versionService = services.getOptionalService(VersionService.class);
        if (versionService == null) {
            return Optional.empty();
        }

        String versionString = versionService.getVersion().toString();
        return Version.getUnknown().equals(versionString) ? Optional.empty() : Optional.ofNullable(versionString);
    }

    /**
     * Creates the Redis URIs by fetching property values from the specified service.
     *
     * @param configuration The configuration to use
     * @param optionalVersion The optional version of the Middleware to use in announced client name
     * @param mode Specifies the mode of the Redis Server for which the Redis URI is supposed to be created
     * @return The crafted Redis URIs from Redis connector configuration
     */
    protected static List<RedisURI> createRedisURIsFromConfig(RedisConfiguration configuration, Optional<String> optionalVersion, OperationMode mode) {
        // Sentinel mode
        boolean sentinelMode = OperationMode.SENTINEL == mode;

        // Hosts
        RedisHost redisHost1;
        List<RedisHost> otherHosts;
        {
            int defaultPort = sentinelMode ? RedisHost.PORT_DEFAULT_SENTINEL : RedisHost.PORT_DEFAULT;
            List<RedisHost> hosts = RedisHost.parse(configuration.getHosts(), defaultPort);
            int numberOfHosts = hosts.size();
            if (numberOfHosts <= 0) {
                redisHost1 = new RedisHost("localhost", defaultPort);
                otherHosts = Collections.emptyList();
            } else {
                redisHost1 = hosts.get(0);
                otherHosts = numberOfHosts > 1 ? hosts.subList(1, numberOfHosts) : Collections.emptyList();
            }
        }

        // Redis URI for Redis Sentinel
        if (sentinelMode) {
            Builder redisUriBuilder = Builder.sentinel(redisHost1.getHost(), redisHost1.getPort(), configuration.getSentinelMasterId());
            for (RedisHost sentinelNode : otherHosts) {
                redisUriBuilder.withSentinel(sentinelNode.getHost(), sentinelNode.getPort());
            }
            decorateURIBuilder(redisUriBuilder, configuration, optionalVersion);
            return Collections.singletonList(redisUriBuilder.build());
        }

        // Redis URI w/o other hosts
        if (otherHosts.isEmpty()) {
            Builder redisUriBuilder = RedisURI.builder();
            redisUriBuilder.withHost(redisHost1.getHost()).withPort(redisHost1.getPort());
            decorateURIBuilder(redisUriBuilder, configuration, optionalVersion);
            return Collections.singletonList(redisUriBuilder.build());
        }

        // Redis URI w/ other hosts
        ImmutableList.Builder<RedisURI> redisURIS = ImmutableList.builderWithExpectedSize(otherHosts.size() + 1);
        {
            Builder redisUriBuilder = RedisURI.builder();
            redisUriBuilder.withHost(redisHost1.getHost()).withPort(redisHost1.getPort());
            decorateURIBuilder(redisUriBuilder, configuration, optionalVersion);
            redisURIS.add(redisUriBuilder.build());
        }
        for (RedisHost otherHost : otherHosts) {
            Builder redisUriBuilder = RedisURI.builder();
            redisUriBuilder.withHost(otherHost.getHost()).withPort(otherHost.getPort());
            decorateURIBuilder(redisUriBuilder, configuration, optionalVersion);
            redisURIS.add(redisUriBuilder.build());
        }
        return redisURIS.build();
    }

    private static void decorateURIBuilder(Builder redisUriBuilder, RedisConfiguration configuration, Optional<String> optionalVersion) {
        // Other stuff
        redisUriBuilder
            .withSsl(configuration.getSsl())
            .withStartTls(configuration.getStartTls())
            .withVerifyPeer(configuration.getVerifyPeer());

        // Client name
        if (optionalVersion.isPresent()) {
            redisUriBuilder.withClientName("Open-Xchange-Redis-Connector-v" + optionalVersion.get());
        } else {
            redisUriBuilder.withClientName("Open-Xchange-Redis-Connector");
        }

        // Database
        int database = configuration.getDatabase();
        if (database > 0) {
            redisUriBuilder.withDatabase(database);
        }

        // Authentication
        String userName = configuration.getUserName();
        String password = configuration.getPassword();
        if (Strings.isNotEmpty(password) && Strings.isNotEmpty(userName)) {
            redisUriBuilder.withAuthentication(userName, password);
        } else if (Strings.isNotEmpty(password)) {
            redisUriBuilder.withPassword(password.toCharArray());
        }

        // Command timeout
        long timeoutMillis = configuration.getCommandTimeoutMillis();
        redisUriBuilder.withTimeout(Duration.ofMillis(timeoutMillis < 0 ? 0 : timeoutMillis));
    }

    /** The decimal format to use when printing milliseconds */
    private static final NumberFormat MILLIS_FORMAT = newNumberFormat();

    /** The accompanying lock for shared decimal format */
    private static final Lock MILLIS_FORMAT_LOCK = new ReentrantLock();

    /**
     * Creates a new {@code DecimalFormat} instance.
     *
     * @return The format instance
     */
    private static NumberFormat newNumberFormat() {
        NumberFormat f = NumberFormat.getInstance(Locale.US);
        if (f instanceof DecimalFormat df) {
            df.applyPattern("#,##0");
        }
        return f;
    }

    private static String format(long millis) {
        if (MILLIS_FORMAT_LOCK.tryLock()) {
            try {
                return MILLIS_FORMAT.format(millis);
            } finally {
                MILLIS_FORMAT_LOCK.unlock();
            }
        }

        // Use thread-specific DecimalFormat instance
        NumberFormat format = newNumberFormat();
        return format.format(millis);
    }

    /**
     * Checks ( and possibly awaits) reachability of the Redis end-point to which given connector is linked to.
     *
     * @param <R> The Redis client type
     * @param <C> The connection type
     * @param pingOperation The pnig operation to use
     * @param redisURI The Redis URI associated with Redis end-point
     * @param connector The connector to use
     * @throws OXException If connect attempt fails
     */
    public static <R extends AbstractRedisClient, C extends StatefulConnection<String, InputStream>> void checkReachability(RedisOperation<Boolean> pingOperation, RedisURI redisURI, AbstractRedisConnector<R, C> connector) throws OXException {
        int connectCounter = 0;
        while (true) {
            try {
                LOGGER.info("Trying to connect to Redis end-point: {}", redisURI);
                if (!connector.executeOperation(pingOperation).booleanValue()) {
                    throw RedisExceptionCode.PING_FAILURE.create(redisURI);
                }
                LOGGER.info("Successfully connected to Redis end-point: {}", redisURI);
                return;
            } catch (OXException e) {
                if (!RedisExceptionCode.isConnectivityError(e)) {
                    throw e;
                }
                long millis = exponentialBackoffWait(++connectCounter, 1000L);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.warn("Failed to connect to Redis end-point: {}. Waiting {} ({}) for retry attempt...", redisURI, format(millis), exactly(millis, true), e);
                } else {
                    LOGGER.warn("Failed to connect to Redis end-point: {}. Waiting {} ({}) for retry attempt...", redisURI, format(millis), exactly(millis, true), dropStackTraceFor(getLastChainedThrowable(e)));
                }
                LockSupport.parkNanos(TimeUnit.NANOSECONDS.convert(millis, TimeUnit.MILLISECONDS));
            }
        }
    }

    /**
     * Performs a wait according to exponential back-off strategy.
     * <pre>
     * (retry-count * base-millis) + random-millis
     * </pre>
     *
     * @param retryCount The current number of retries
     * @param baseMillis The base milliseconds
     * @return The time to wait in milliseconds
     */
    private static long exponentialBackoffWait(int retryCount, long baseMillis) {
        return (retryCount * baseMillis) + ((long) (Math.random() * baseMillis));
    }

}
