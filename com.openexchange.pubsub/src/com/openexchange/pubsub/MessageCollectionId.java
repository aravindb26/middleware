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

import java.util.Objects;
import java.util.UUID;

/**
 * {@link MessageCollectionId} - The identifier for a message collection.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class MessageCollectionId {

    private final UUID uuid;
    private final ChannelKey channelKey;

    /**
     * Initializes a new {@link MessageCollectionId}.
     * @param uuid The UUID of the message collection
     * @param channelKey The key of the channel to which the message collection belong
     */
    public MessageCollectionId(UUID uuid, ChannelKey channelKey) {
        super();
        this.uuid = uuid;
        this.channelKey = channelKey;
    }

    /**
     * Gets the UUID of the message collection.
     *
     * @return The message collection UUID
     */
    public UUID getId() {
        return uuid;
    }

    /**
     * Gets the key of the channel to which the message collection belongs.
     *
     * @return The channel key
     */
    public ChannelKey getChannelKey() {
        return channelKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelKey, uuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MessageCollectionId other = (MessageCollectionId) obj;
        return Objects.equals(uuid, other.uuid) && Objects.equals(channelKey, other.channelKey);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(64);
        builder.append('[');
        if (uuid != null) {
            builder.append("uuid=").append(uuid);
        }
        if (channelKey != null) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append("channelKey=").append(channelKey);
        }
        builder.append(']');
        return builder.toString();
    }

}
