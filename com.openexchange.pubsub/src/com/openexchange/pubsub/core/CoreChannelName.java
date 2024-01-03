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

package com.openexchange.pubsub.core;

import com.openexchange.pubsub.ChannelName;

/**
 * {@link CoreChannelName} - An enumeration for known core channel names.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum CoreChannelName implements ChannelName {

    /**
     * The name for cache event channel.
     */
    CACHE_EVENTS("cacheEvents"),
    /**
     * The name for cache listener channel.
     */
    CACHE_LISTENER("cacheListener"),
    /**
     * The name for OX Drive event channel.
     */
    DRIVE_EVENTS("driveEvents"),
    /**
     * The name for Open-Xchange event bridge to distribute local events to remote nodes.
     */
    REMOTE_EVENTS("remoteEvents"),
    /**
     * The name for Open-Xchange events.
     */
    OPEN_XCHANGE_EVENTS("oxEvents"),
    /**
     * The name for channel for delivering Web Socket messages.
     */
    WEBSOCKET_MESSAGES("wsMessages"),
    /**
     * The name for session event channel.
     */
    SESSION_EVENTS("sessionEvents"),
    ;

    private final String name;

    /**
     * Initializes a new {@link CoreChannelName}.
     *
     * @param name The name
     */
    private CoreChannelName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
