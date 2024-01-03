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

import org.json.JSONObject;
import com.openexchange.exception.OXException;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * {@link RedisConnector} - The Redis connector.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface RedisConnector {

    /**
     * Gets the mode this connector is for; e.g. a Redis cluster, Redis Sentinel or Redis stand-alone.
     *
     * @return The mode
     */
    OperationMode getMode();

    /**
     * Opens a new pub/sub connection to the Redis server.
     *
     * @return The newly established pub/sub connection to the Redis server
     */
    StatefulRedisPubSubConnection<String, JSONObject> newPubSubConnection();

    /**
     * Executes the specified Redis operation that returns no result.
     *
     * @param operation The operation to execute
     * @return The result
     * @throws OXException If executing operation fails
     */
    default void executeVoidOperation(RedisVoidOperation operation) throws OXException {
        executeOperation(null, operation);
    }

    /**
     * Executes the specified Redis operation and returns the result.
     *
     * @param <V> The return type
     * @param operation The operation to execute
     * @return The result
     * @throws OXException If executing operation fails
     */
    default <V> V executeOperation(RedisOperation<V> operation) throws OXException {
        return executeOperation(null, operation);
    }

    /**
     * Executes the specified Redis operation and returns the result.
     * <p>
     * Provide an operation key for frequently invoked operations to avoid concurrent executions of the same operation.
     *
     * @param operationKey The optional operation key to avoid concurrent executions of the same operation or <code>null</code>
     * @param operation The operation to execute
     * @param <V> The return type
     * @return The result
     * @throws OXException If executing operation fails
     */
    <V> V executeOperation(RedisOperationKey<?> operationKey, RedisOperation<V> operation) throws OXException;

}
