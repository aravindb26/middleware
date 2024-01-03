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

/**
 * {@link ChannelKeyService} - The service to create and obtain channel keys.
 * <p>
 * Each key must have an {@link ChannelApplicationName} and channel name.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public interface ChannelKeyService {

    /**
     * Gets the delimiter character.
     *
     * @return The delimiter character
     */
    char getDelimiter();

    /**
     * Creates a new channel key for the specified application and channel name parts.
     *
     * @param application The application name
     * @param channel The channel name
     * @return The new {@link ChannelKey}
     */
    ChannelKey newKey(ChannelApplicationName application, ChannelName channel);
}
