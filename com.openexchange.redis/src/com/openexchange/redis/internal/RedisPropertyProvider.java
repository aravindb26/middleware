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
 * {@link RedisPropertyProvider} - Provides access to Redis properties.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface RedisPropertyProvider {

    /**
     * Gets the mode property.
     *
     * @return The mode property
     */
    Property getMode();

    /**
     * Gets the hosts property.
     *
     * @return The hosts property
     */
    Property getHosts();

    /**
     * Gets the sentinel master identifier property.
     *
     * @return The sentinel master identifier property
     */
    Property getSentinelMasterId();

    /**
     * Gets the user name property.
     *
     * @return The user name property
     */
    Property getUserName();

    /**
     * Gets the password property.
     *
     * @return The password property
     */
    Property getPassword();

    /**
     * Gets the SSL property.
     *
     * @return The SSL property
     */
    Property getSsl();

    /**
     * Gets the STARTTLS property.
     *
     * @return The STARTTLS property
     */
    Property getStartTls();

    /**
     * Gets the verify peer property.
     *
     * @return The verify peer property
     */
    Property getVerifyPeer();

    /**
     * Gets the database property.
     *
     * @return The database property
     */
    Property getDatabase();

    /**
     * Gets the latency metrics property.
     *
     * @return The latency metrics property
     */
    Property getLatencyMetrics();

    /**
     * Gets the command timeout property.
     *
     * @return The command timeout property
     */
    Property getCommandTimeoutMillis();

    /**
     * Gets the connect timeout property.
     *
     * @return The connect timeout property
     */
    Property getConnectTimeoutMillis();

    /**
     * Gets the connection pool max. total property.
     *
     * @return The property
     */
    Property getConnectionPoolMaxTotal();

    /**
     * Gets the connection pool max. idle property.
     *
     * @return The property
     */
    Property getConnectionPoolMaxIdle();

    /**
     * Gets the connection pool min. idle property.
     *
     * @return The property
     */
    Property getConnectionPoolMinIdle();

    /**
     * Gets the connection pool max. wait seconds property.
     *
     * @return The property
     */
    Property getConnectionPoolMaxWaitSeconds();

    /**
     * Gets the connection pool min. idle seconds property.
     *
     * @return The property
     */
    Property getConnectionPoolMinIdleSeconds();

    /**
     * Gets the connection pool cleaner run seconds property.
     *
     * @return The property
     */
    Property getConnectionPoolCleanerRunSeconds();

    /**
     * Gets the circuit breaker enabled property.
     *
     * @return The property
     */
    Property getCircuitBreakerEnabled();

    /**
     * Gets the circuit breaker the failure threshold property.
     *
     * @return The property
     */
    Property getCircuitBreakerFailureThreshold();

    /**
     * Gets the circuit breaker number of executions to measure the failures property.
     *
     * @return The property
     */
    Property getCircuitBreakerFailureExecutions();

    /**
     * Gets the circuit breaker the success threshold property.
     *
     * @return The property
     */
    Property getCircuitBreakerSuccessThreshold();

    /**
     * Gets the circuit breaker number of executions to measure the successes property.
     *
     * @return The property
     */
    Property getCircuitBreakerSuccessExecutions();

    /**
     * Gets the circuit breaker delay in milliseconds property.
     *
     * @return The property
     */
    Property getCircuitBreakerDelayMillis();
}
