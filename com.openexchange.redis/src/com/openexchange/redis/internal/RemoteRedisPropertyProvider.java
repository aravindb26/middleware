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
 * {@link RemoteRedisPropertyProvider}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RemoteRedisPropertyProvider implements RedisPropertyProvider {

    private static final RemoteRedisPropertyProvider INSTANCE = new RemoteRedisPropertyProvider();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static RemoteRedisPropertyProvider getInstance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link RemoteRedisPropertyProvider}.
     */
    private RemoteRedisPropertyProvider() {
        super();
    }

    @Override
    public Property getMode() {
        return RemoteRedisProperty.MODE;
    }

    @Override
    public Property getHosts() {
        return RemoteRedisProperty.HOSTS;
    }

    @Override
    public Property getSentinelMasterId() {
        return RemoteRedisProperty.SENTINEL_MASTER_ID;
    }

    @Override
    public Property getUserName() {
        return RemoteRedisProperty.USER_NAME;
    }

    @Override
    public Property getPassword() {
        return RemoteRedisProperty.PASSWORD;
    }

    @Override
    public Property getSsl() {
        return RemoteRedisProperty.SSL;
    }

    @Override
    public Property getStartTls() {
        return RemoteRedisProperty.STARTTLS;
    }

    @Override
    public Property getVerifyPeer() {
        return RemoteRedisProperty.VERIFY_PEER;
    }

    @Override
    public Property getDatabase() {
        return RemoteRedisProperty.DATABASE;
    }

    @Override
    public Property getLatencyMetrics() {
        return RemoteRedisProperty.LETTUCE_COMMAND_LATENCY_METRICS;
    }

    @Override
    public Property getCommandTimeoutMillis() {
        return RemoteRedisProperty.COMMAND_TIMEOUT_MILLIS;
    }

    @Override
    public Property getConnectTimeoutMillis() {
        return RemoteRedisProperty.CONNECT_TIMEOUT_MILLIS;
    }

    @Override
    public Property getConnectionPoolMaxTotal() {
        return RemoteRedisProperty.POOL_MAX_TOTAL;
    }

    @Override
    public Property getConnectionPoolMaxIdle() {
        return RemoteRedisProperty.POOL_MAX_IDLE;
    }

    @Override
    public Property getConnectionPoolMinIdle() {
        return RemoteRedisProperty.POOL_MIN_IDLE;
    }

    @Override
    public Property getConnectionPoolMaxWaitSeconds() {
        return RemoteRedisProperty.POOL_MAX_WAIT_SECONDS;
    }

    @Override
    public Property getConnectionPoolMinIdleSeconds() {
        return RemoteRedisProperty.POOL_MIN_IDLE_SECONDS;
    }

    @Override
    public Property getConnectionPoolCleanerRunSeconds() {
        return RemoteRedisProperty.POOL_CLEANER_RUN_SECONDS;
    }

    @Override
    public Property getCircuitBreakerEnabled() {
        return RemoteRedisProperty.CIRCUIT_BREAKER_ENABLED;
    }

    @Override
    public Property getCircuitBreakerFailureThreshold() {
        return RemoteRedisProperty.CIRCUIT_BREAKER_FAILURE_THRESHOLD;
    }

    @Override
    public Property getCircuitBreakerFailureExecutions() {
        return RemoteRedisProperty.CIRCUIT_BREAKER_FAILURE_EXECUTIONS;
    }

    @Override
    public Property getCircuitBreakerSuccessThreshold() {
        return RemoteRedisProperty.CIRCUIT_BREAKER_SUCCESS_THRESHOLD;
    }

    @Override
    public Property getCircuitBreakerSuccessExecutions() {
        return RemoteRedisProperty.CIRCUIT_BREAKER_SUCCESS_EXECUTIONS;
    }

    @Override
    public Property getCircuitBreakerDelayMillis() {
        return RemoteRedisProperty.CIRCUIT_BREAKER_DELAY_MILLIS;
    }

}
