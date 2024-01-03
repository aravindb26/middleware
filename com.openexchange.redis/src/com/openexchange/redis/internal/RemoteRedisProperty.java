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

import com.openexchange.config.lean.Property;

/**
 * {@link RemoteRedisProperty}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum RemoteRedisProperty implements Property {

    /**
     * Defines the operation mode; Redis cluster, Redis Sentinel or Redis stand-alone.
     */
    MODE(RedisProperty.MODE),
    /**
     * Defines the listing of Redis hosts of the form:
     * <p>
     * <pre>
     *   [host1] + (":" + [port1])? + "," + [host2] + (":" + [port2])? + "," + [host3] + (":" + [port3])?
     * </pre>
     */
    HOSTS(RedisProperty.HOSTS),
    /**
     * Defines the name of the sentinel master
     */
    SENTINEL_MASTER_ID(RedisProperty.SENTINEL_MASTER_ID),
    /**
     * The user name to connect to Redis end-point. Empty string means no user name required.
     */
    USER_NAME(RedisProperty.USER_NAME),
    /**
     * The password to connect to Redis end-point. Empty string means no password required.
     */
    PASSWORD(RedisProperty.PASSWORD),
    /**
     * Sets whether to use SSL to connect to Redis end-point.
     */
    SSL(RedisProperty.SSL),
    /**
     * Sets whether to use STARTTLS to connect to Redis end-point.
     */
    STARTTLS(RedisProperty.STARTTLS),
    /**
     * Sets whether to verify peers when using SSL.
     */
    VERIFY_PEER(RedisProperty.VERIFY_PEER),
    /**
     * Specifies the Redis database number. Databases are only available for Redis stand-alone and Redis Master/Slave.
     * <p>
     * A negative number means no database.
     */
    DATABASE(RedisProperty.DATABASE),
    /**
     * Defines whether to enable lettuce's built-in command latency metrics.
     */
    LETTUCE_COMMAND_LATENCY_METRICS(RedisProperty.LETTUCE_COMMAND_LATENCY_METRICS),
    /**
     * Defines the timeout for commands in milliseconds.
     */
    COMMAND_TIMEOUT_MILLIS(RedisProperty.COMMAND_TIMEOUT_MILLIS),
    /**
     * Defines the connect timeout in milliseconds.
     */
    CONNECT_TIMEOUT_MILLIS(RedisProperty.CONNECT_TIMEOUT_MILLIS),

    // -------------------------------------------- Pool configuration ---------------------------------------------------------------------

    /**
     * Specifies the capability on the number of objects that can be allocated by the connection pool (checked out to clients, or idle
     * awaiting checkout) at a given time.
     */
    POOL_MAX_TOTAL(RedisProperty.POOL_MAX_TOTAL),
    /**
     * Specifies Sets the capability on the number of "idle" instances in the connection pool.
     * <p>
     * If maxIdle is set too low on heavily loaded systems it is possible you will see objects being destroyed and almost immediately new
     * objects being created. This is a result of the active threads momentarily returning objects faster than they are requesting them,
     * causing the number of idle objects to rise above maxIdle.
     */
    POOL_MAX_IDLE(RedisProperty.POOL_MAX_IDLE),
    /**
     * Specifies the target for the minimum number of idle objects to maintain in the pool.
     */
    POOL_MIN_IDLE(RedisProperty.POOL_MIN_IDLE),
    /**
     * Specifies the maximum duration a borrowing caller should be blocked before throwing an exception when the pool is exhausted.
     */
    POOL_MAX_WAIT_SECONDS(RedisProperty.POOL_MAX_WAIT_SECONDS),
    /**
     * Specifies the minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     */
    POOL_MIN_IDLE_SECONDS(RedisProperty.POOL_MIN_IDLE_SECONDS),
    /**
     * Specifies the duration to sleep between runs of the cleaner dropping connections marked for eviction.
     */
    POOL_CLEANER_RUN_SECONDS(RedisProperty.POOL_CLEANER_RUN_SECONDS),

    // -------------------------------------------- Circuit breaker configuration ----------------------------------------------------------

    /**
     * The flag to enable/disable the Redis circuit breaker
     */
    CIRCUIT_BREAKER_ENABLED(RedisProperty.CIRCUIT_BREAKER_ENABLED),
    /**
     * The failure threshold; which is the number of successive failures that must occur in order to open the circuit.
     */
    CIRCUIT_BREAKER_FAILURE_THRESHOLD(RedisProperty.CIRCUIT_BREAKER_FAILURE_THRESHOLD),
    /**
     * The number of executions to measure the failures against.<br>
     * Default is always the same as <code>com.openexchange.imap.breaker.failureThreshold</code>
     */
    CIRCUIT_BREAKER_FAILURE_EXECUTIONS(RedisProperty.CIRCUIT_BREAKER_FAILURE_EXECUTIONS),
    /**
     * The success threshold; which is the number of successive successful executions that must occur when in a half-open state in order to
     * close the circuit.
     */
    CIRCUIT_BREAKER_SUCCESS_THRESHOLD(RedisProperty.CIRCUIT_BREAKER_SUCCESS_THRESHOLD),
    /**
     * The number of executions to measure the successes against.<br>
     * The default is the same value as for <code>com.openexchange.imap.breaker.successThreshold</code>
     */
    CIRCUIT_BREAKER_SUCCESS_EXECUTIONS(RedisProperty.CIRCUIT_BREAKER_SUCCESS_EXECUTIONS),
    /**
     * The delay in milliseconds; the number of milliseconds to wait in open state before transitioning to half-open.
     */
    CIRCUIT_BREAKER_DELAY_MILLIS(RedisProperty.CIRCUIT_BREAKER_DELAY_MILLIS),
    ;

    public static final String QUALIFIER_SITE_ID = "siteId";

    private final Object defaultValue;
    private final String fqn;

    /**
     * Initializes a new {@link RemoteRedisProperty}.
     *
     * @param appendix The appendix for full-qualified name
     * @param defaultValue The default value
     */
    private RemoteRedisProperty(RedisProperty property) {
        this.fqn = new StringBuilder("com.openexchange.redis.[").append(QUALIFIER_SITE_ID).append("].").append(property.getAppendix()).toString();
        this.defaultValue = property.getDefaultValue();
    }

    /**
     * Gets the fully-qualified name for the property
     *
     * @return The fully-qualified name for the property
     */
    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    /**
     * Gets the default value of this property
     *
     * @return The default value of this property
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
