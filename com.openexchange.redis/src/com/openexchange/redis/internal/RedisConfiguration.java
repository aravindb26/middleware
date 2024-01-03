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

import java.util.Collections;
import java.util.Map;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.java.Strings;

/**
 * {@link RedisConfiguration} - Provides access to Redis properties.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisConfiguration {

    private final RedisPropertyProvider propertyProvider;
    private final LeanConfigurationService configService;
    private final Map<String, String> optionals;

    /**
     * Initializes a new {@link RedisConfiguration}.
     *
     * @param propertyProvider
     * @param configService
     */
    public RedisConfiguration(RedisPropertyProvider propertyProvider, LeanConfigurationService configService) {
        this(null, propertyProvider, configService);
    }

    /**
     * Initializes a new {@link RedisConfiguration}.
     *
     * @param siteId
     * @param instance
     * @param configService2
     */
    public RedisConfiguration(String siteId, RedisPropertyProvider propertyProvider, LeanConfigurationService configService) {
        super();
        optionals = Strings.isEmpty(siteId) ? Collections.emptyMap() : Collections.singletonMap(RemoteRedisProperty.QUALIFIER_SITE_ID, siteId);
        this.propertyProvider = propertyProvider;
        this.configService = configService;
    }

    /**
     * Gets the mode.
     *
     * @return The mode
     */
    public String getMode() {
        return configService.getProperty(propertyProvider.getMode(), optionals);
    }

    /**
     * Gets the hosts.
     *
     * @return The hosts
     */
    public String getHosts() {
        return configService.getProperty(propertyProvider.getHosts(), optionals);
    }

    /**
     * Gets the sentinel master identifier.
     *
     * @return The sentinel master identifier
     */
    public String getSentinelMasterId() {
        return configService.getProperty(propertyProvider.getSentinelMasterId(), optionals);
    }

    /**
     * Gets the user name.
     *
     * @return The user name
     */
    public String getUserName() {
        return configService.getProperty(propertyProvider.getUserName(), optionals);
    }

    /**
     * Gets the password property.
     *
     * @return The password property
     */
    public String getPassword() {
        return configService.getProperty(propertyProvider.getPassword(), optionals);
    }

    /**
     * Gets the SSL flag.
     *
     * @return The SSL flag
     */
    public boolean getSsl() {
        return configService.getBooleanProperty(propertyProvider.getSsl(), optionals);
    }

    /**
     * Gets the STARTTLS flag.
     *
     * @return The STARTTLS flag
     */
    public boolean getStartTls() {
        return configService.getBooleanProperty(propertyProvider.getStartTls(), optionals);
    }

    /**
     * Gets the verify peer flag.
     *
     * @return The verify peer flag
     */
    public boolean getVerifyPeer() {
        return configService.getBooleanProperty(propertyProvider.getVerifyPeer(), optionals);
    }

    /**
     * Gets the database.
     *
     * @return The database
     */
    public int getDatabase() {
        return configService.getIntProperty(propertyProvider.getDatabase(), optionals);
    }

    /**
     * Gets the latency metrics flag.
     *
     * @return The latency metrics flag
     */
    public boolean getLatencyMetrics() {
        return configService.getBooleanProperty(propertyProvider.getLatencyMetrics(), optionals);
    }

    /**
     * Gets the command timeout.
     *
     * @return The command timeout
     */
    public long getCommandTimeoutMillis() {
        return configService.getLongProperty(propertyProvider.getCommandTimeoutMillis(), optionals);
    }

    /**
     * Gets the connect timeout.
     *
     * @return The connect timeout
     */
    public long getConnectTimeoutMillis() {
        return configService.getLongProperty(propertyProvider.getConnectTimeoutMillis(), optionals);
    }

    // ------------------------------------------------------------------------------------------

    /**
     * Gets the capability on the number of objects that can be allocated by the connection pool (checked out to clients, or idle
     * awaiting checkout) at a given time.
     *
     * @return The max. number of pooled connections
     */
    public int getConnectionPoolMaxTotal() {
        return configService.getIntProperty(propertyProvider.getConnectionPoolMaxTotal(), optionals);
    }

    /**
     * Gets the capability on the number of "idle" instances in the connection pool.
     *
     * @return The max. number of idle connections
     */
    public int getConnectionPoolMaxIdle() {
        return configService.getIntProperty(propertyProvider.getConnectionPoolMaxIdle(), optionals);
    }

    /**
     * Gets the target for the minimum number of idle objects to maintain in the pool.
     *
     * @return The min. number of idle connections
     */
    public int getConnectionPoolMinIdle() {
        return configService.getIntProperty(propertyProvider.getConnectionPoolMinIdle(), optionals);
    }

    /**
     * Gets the maximum duration a borrowing caller should be blocked before throwing an exception when the pool is exhausted.
     *
     * @return The max. wait seconds
     */
    public int getConnectionPoolMaxWaitSeconds() {
        return configService.getIntProperty(propertyProvider.getConnectionPoolMaxWaitSeconds(), optionals);
    }

    /**
     * Gets the minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
     *
     * @return The idle seconds
     */
    public int getConnectionPoolMinIdleSeconds() {
        return configService.getIntProperty(propertyProvider.getConnectionPoolMinIdleSeconds(), optionals);
    }

    public int getConnectionPoolCleanerRunSeconds() {
        return configService.getIntProperty(propertyProvider.getConnectionPoolCleanerRunSeconds(), optionals);
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Gets the flag to enable/disable the Redis circuit breaker.
     *
     * @return The flag
     */
    public boolean getCircuitBreakerEnabled() {
        return configService.getBooleanProperty(propertyProvider.getCircuitBreakerEnabled(), optionals);
    }

    /**
     * Gets the failure threshold; which is the number of successive failures that must occur in order to open the circuit.
     *
     * @return The threshold
     */
    public int getCircuitBreakerFailureThreshold() {
        return configService.getIntProperty(propertyProvider.getCircuitBreakerFailureThreshold(), optionals);
    }

    /**
     * Gets the number of executions to measure the failures against
     *
     * @return The number
     */
    public int getCircuitBreakerFailureExecutions() {
        return configService.getIntProperty(propertyProvider.getCircuitBreakerFailureExecutions(), optionals);
    }

    /**
     * Gets the success threshold; which is the number of successive successful executions that must occur when in a half-open state in order to
     * close the circuit.
     *
     * @return The threshold
     */
    public int getCircuitBreakerSuccessThreshold() {
        return configService.getIntProperty(propertyProvider.getCircuitBreakerSuccessThreshold(), optionals);
    }

    /**
     * Gets the number of executions to measure the successes against.
     *
     * @return The number
     */
    public int getCircuitBreakerSuccessExecutions() {
        return configService.getIntProperty(propertyProvider.getCircuitBreakerSuccessExecutions(), optionals);
    }

    /**
     * Gets the delay in milliseconds; the number of milliseconds to wait in open state before transitioning to half-open.
     *
     * @return The delay in milliseconds
     */
    public long getCircuitBreakerDelayMillis() {
        return configService.getLongProperty(propertyProvider.getCircuitBreakerDelayMillis(), optionals);
    }
}
