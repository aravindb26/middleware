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

package com.openexchange.redis;

import java.time.Duration;
import java.util.Optional;
import com.openexchange.exception.OXException;

/**
 * {@link RedisOperation} - Represents an operation performed against Redis end-point that executes one or more commands.
 * <p>
 * Moreover, an operation may specify whether circuit break shall be omitted and the concrete command timeout.
 *
 * @param <R> The return type
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@FunctionalInterface
public interface RedisOperation<R> {

    /**
     * Executes the operation and returns the outcome.
     * <p>
     * The connection is either an instance of <code>io.lettuce.core.cluster.api.StatefulRedisClusterConnection</code>
     * (if {@link #isCluster()} signals <code>false</code>) or an instance of <code>io.lettuce.core.api.StatefulRedisConnection<K, V></code>
     * (if {@link #isCluster()} signals <code>true</code>)
     *
     * @param commandsProvider Provides access to different command sets to communicate with Redis end-point
     * @return The outcome of the operation
     * @throws OXException If an error is occurred during the execution
     */
    R execute(RedisCommandsProvider commandsProvider) throws OXException;

    /**
     * Whether circuit breaker (if any) shall be omitted for this operation.
     *
     * @return <code>true</code> to omit; otherwise <code>false</code>
     */
    default boolean omitCircuitBreaker() {
        return false;
    }

    /**
     * Gets the individual command timeout to apply for this operation.
     * <p>
     * If empty is returned the command timeout as per configuration is used;<br>
     * see <code>"com.openexchange.redis.commandTimeoutMillis"</code> (<code>com.openexchange.redis.internal.RedisProperty.COMMAND_TIMEOUT_MILLIS</code>).
     *
     * @return The individual command timeout or empty to use default command timeout as per configuration
     */
    default Optional<Duration> getCommandTimeout() {
        return Optional.empty();
    }

}
