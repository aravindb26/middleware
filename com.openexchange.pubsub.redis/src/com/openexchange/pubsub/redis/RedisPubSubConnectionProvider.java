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

package com.openexchange.pubsub.redis;

import org.json.JSONObject;
import com.openexchange.exception.OXException;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

/**
 * {@link RedisPubSubConnectionProvider} - Provides the pub/sub connection to Redis server.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public interface RedisPubSubConnectionProvider {

    /**
     * Opens a new pub/sub connection to a Redis server.
     *
     * @return The newly established pub/sub connection to the Redis server
     * @throws OXException If pub/sub connection cannot be established
     */
    StatefulRedisPubSubConnection<String, JSONObject> newPubSubConnection() throws OXException;

}
