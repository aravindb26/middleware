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

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * {@link RedisConnectorService} - The Redis connector service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@SingletonService
public interface RedisConnectorService {

    /**
     * Gets the connector provider for the (local) Redis storage.
     *
     * @return The connector provider for the Redis storage
     * @throws OXException If the connector provider for the Redis storage cannot be returned
     */
    RedisConnectorProvider getConnectorProvider() throws OXException;

    /**
     * Gets the connector providers for the remote Redis storages.
     * <p>
     * Exemplary usage:
     * <pre>
     *  // Delete by key in all associated remote Redis storages
     *  String myKey = ...;
     *  List<RedisConnectorProvider> remoteConnectorProviders = connectorService.getRemoteConnectorProviders();
     *  for (RedisConnectorProvider connectorProvider : remoteConnectorProviders) {
     *     connectorProvider.getConnector().executeVoidOperation(connection -> {
     *        RedisCommandsProvider commandsProvider = RedisCommands.getRedisCommandsProvider(connection);
     *        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
     *        // Delete by key
     *        keyCommands.del(myKey)
     *     }
     *  }
     * </pre>
     *
     * @return The remote connector providers
     * @throws OXException If remote connector providers cannot be returned
     */
    List<RedisConnectorProvider> getRemoteConnectorProviders() throws OXException;

}
