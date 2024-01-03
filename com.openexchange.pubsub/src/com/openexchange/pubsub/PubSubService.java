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

package com.openexchange.pubsub;

import java.util.Optional;
import java.util.UUID;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * {@link PubSubService} - The pub-sub service.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
@SingletonService
public interface PubSubService extends ChannelKeyService {

    /**
     * Gets the unique identifier of this service instance in cluster.
     *
     * @return The unique service instance identifier
     */
    UUID getInstanceId();

    /**
     * Gets the channel for given key.
     *
     * @param <M> The message type
     * @param key The channel key
     * @param codec The codec to use for de-/serialization of the messages
     * @return The channel
     */
    <M> Channel<M> getChannel(ChannelKey key, ChannelMessageCodec<M> codec);

    /**
     * Gets the optional channel for given key.
     *
     * @param <M> The message type
     * @param key The channel key
     * @return The channel or empty
     */
    <M> Optional<Channel<M>> optChannel(ChannelKey key);

}
