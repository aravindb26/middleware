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

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import com.openexchange.config.lean.Property;
import com.openexchange.redis.OperationMode;

/**
 * {@link RedisProperty}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum RedisProperty implements Property {

    /**
     * The switch to enable/disable Redis connector.
     */
    ENABLED("enabled", Boolean.FALSE),
    /**
     * The switch to enable/disable remote sites awareness.
     */
    SITES_ENABLED("sites.enabled", Boolean.FALSE),
    /**
     * The comma-separated list of identifiers for remote sites for which a configuration is available.
     */
    SITES("sites", ""),
    /**
     * Defines the operation mode; Redis cluster, Redis Sentinel or Redis stand-alone.
     */
    MODE("mode", OperationMode.STAND_ALONE.getIdentifier()),
    /**
     * Defines the listing of Redis hosts of the form:
     * <p>
     * <pre>
     *   [host1] + (":" + [port1])? + "," + [host2] + (":" + [port2])? + "," + [host3] + (":" + [port3])?
     * </pre>
     */
    HOSTS("hosts", "localhost:" + RedisHost.PORT_DEFAULT),
    /**
     * Defines the name of the sentinel master
     */
    SENTINEL_MASTER_ID("sentinel.masterId", "mymaster"),
    /**
     * The user name to connect to Redis end-point. Empty string means no user name required.
     */
    USER_NAME("username", ""),
    /**
     * The password to connect to Redis end-point. Empty string means no password required.
     */
    PASSWORD("password", ""),
    /**
     * Sets whether to use SSL to connect to Redis end-point.
     */
    SSL("ssl", Boolean.FALSE),
    /**
     * Sets whether to use STARTTLS to connect to Redis end-point.
     */
    STARTTLS("starttls", Boolean.FALSE),
    /**
     * Sets whether to verify peers when using SSL.
     */
    VERIFY_PEER("verifyPeer", Boolean.FALSE),
    /**
     * Specifies the Redis database number. Databases are only available for Redis stand-alone and Redis Master/Slave.
     * <p>
     * A negative number means no database.
     */
    DATABASE("database", I(-1)),
    /**
     * Enables the delayed double invalidation. In a nutshell it tries
     * to mitigate cache inconsistencies that might occur due to race conditioning,
     * by invalidating the cache after a storage update and performing a delayed
     * invalidation after the first invalidation at some point in the future (dictated
     * by <code>impendingInvalidationDelay</code>).
     *
     * Default: <code>false</code>
     */
    IMPENDING_INVALIDATION("impendingInvalidation", B(false)),
    /**
     * The amount of time (in milliseconds) to wait before performing the impending invalidation.
     *
     * Default: 2000
     */
    IMPENDING_INVALIDATION_DELAY("impendingInvalidationDelay", I(2000)),
    /**
     * Specifies if changes to personal folders (personal in terms of non-global e.g. folders kept in database) are supposed to be propagated
     * to remote nodes. This option is only useful for installations that do offer collaboration features or do not support session stickyness.
     * For instance users are able to share mail folders or might be load-balanced to other nodes while active in a single session.
     */
    REMOTE_INVALIDATION_FOR_PERSONAL_FOLDERS("remoteInvalidationForPersonalFolders", Boolean.FALSE),
    /**
     * Defines whether to enable lettuce's built-in command latency metrics.
     */
    LETTUCE_COMMAND_LATENCY_METRICS("lettuceCommandLatencyMetrics", B(true)),
    /**
     * Defines the timeout for commands in milliseconds.
     */
    COMMAND_TIMEOUT_MILLIS("commandTimeoutMillis", L(5000)),
    /**
     * Defines the connect timeout in milliseconds.
     */
    CONNECT_TIMEOUT_MILLIS("connectTimeoutMillis", L(5000)),

    // -------------------------------------------- Pool configuration ---------------------------------------------------------------------

    /**
     * Specifies the capability on the number of objects that can be allocated by the connection pool (checked out to clients, or idle
     * awaiting checkout) at a given time.
     */
    POOL_MAX_TOTAL("connection.pool.maxTotal", I(-1)),
    /**
     * Specifies Sets the capability on the number of "idle" instances in the connection pool.
     * <p>
     * If maxIdle is set too low on heavily loaded systems it is possible you will see objects being destroyed and almost immediately new
     * objects being created. This is a result of the active threads momentarily returning objects faster than they are requesting them,
     * causing the number of idle objects to rise above maxIdle.
     */
    POOL_MAX_IDLE("connection.pool.maxIdle", I(100)),
    /**
     * Specifies the target for the minimum number of idle objects to maintain in the pool.
     */
    POOL_MIN_IDLE("connection.pool.minIdle", I(0)),
    /**
     * Specifies the maximum duration a borrowing caller should be blocked before throwing an exception when the pool is exhausted.
     */
    POOL_MAX_WAIT_SECONDS("connection.pool.maxWaitSeconds", I(10)),
    /**
     * Specifies the minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     */
    POOL_MIN_IDLE_SECONDS("connection.pool.minIdleSeconds", I(60)),
    /**
     * Specifies the duration to sleep between runs of the cleaner dropping connections marked for eviction.
     */
    POOL_CLEANER_RUN_SECONDS("connection.pool.cleanerRunSeconds", I(10)),

    // -------------------------------------------- Circuit breaker configuration ----------------------------------------------------------

    /**
     * The flag to enable/disable the Redis circuit breaker
     */
    CIRCUIT_BREAKER_ENABLED("breaker.enabled", Boolean.FALSE),
    /**
     * The failure threshold; which is the number of successive failures that must occur in order to open the circuit.
     */
    CIRCUIT_BREAKER_FAILURE_THRESHOLD("breaker.failureThreshold", I(5)),
    /**
     * The number of executions to measure the failures against.<br>
     * Default is always the same as <code>com.openexchange.imap.breaker.failureThreshold</code>
     */
    CIRCUIT_BREAKER_FAILURE_EXECUTIONS("breaker.failureExecutions", I(0)),
    /**
     * The success threshold; which is the number of successive successful executions that must occur when in a half-open state in order to
     * close the circuit.
     */
    CIRCUIT_BREAKER_SUCCESS_THRESHOLD("breaker.successThreshold", I(2)),
    /**
     * The number of executions to measure the successes against.<br>
     * The default is the same value as for <code>com.openexchange.imap.breaker.successThreshold</code>
     */
    CIRCUIT_BREAKER_SUCCESS_EXECUTIONS("breaker.successExecutions", I(0)),
    /**
     * The delay in milliseconds; the number of milliseconds to wait in open state before transitioning to half-open.
     */
    CIRCUIT_BREAKER_DELAY_MILLIS("breaker.delayMillis", L(60000)),
    ;

    private final Object defaultValue;
    private final String fqn;
    private final String appendix;

    /**
     * Initializes a new {@link RedisProperty}.
     *
     * @param appendix The appendix for full-qualified name
     * @param defaultValue The default value
     */
    private RedisProperty(String appendix, Object defaultValue) {
        this.appendix = appendix;
        this.fqn = "com.openexchange.redis." + appendix;
        this.defaultValue = defaultValue;
    }

    /**
     * Gets the appendix; e.g. <code>"hosts"</code>
     *
     * @return The appendix
     */
    public String getAppendix() {
        return appendix;
    }

    /**
     * Returns the fully qualified name for the property
     *
     * @return the fully qualified name for the property
     */
    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    /**
     * Returns the default value of this property
     *
     * @return the default value of this property
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
