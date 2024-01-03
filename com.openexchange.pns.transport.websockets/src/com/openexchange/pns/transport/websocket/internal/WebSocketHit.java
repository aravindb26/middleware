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

package com.openexchange.pns.transport.websocket.internal;

import java.util.Collections;
import java.util.List;
import com.openexchange.pns.Hit;
import com.openexchange.pns.PushMatch;

/**
 * {@link WebSocketHit}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
final class WebSocketHit implements Hit {

    private final String client;
    private final PushMatch pushMatch;

    /**
     * Initializes a new {@link WebSocketHit}.
     *
     * @param client The Web Socket client
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param topic The topic of interest
     */
    WebSocketHit(String client, int userId, int contextId, String topic) {
        super();
        this.client = client;
        this.pushMatch = new PushMatchImpl(userId, contextId, client, WebSocketPushNotificationTransport.ID, WebSocketPushNotificationTransport.createTokenFor(client, userId, contextId), topic);
    }

    @Override
    public String getTransportId() {
        return WebSocketPushNotificationTransport.ID;
    }

    @Override
    public List<PushMatch> getMatches() {
        return Collections.singletonList(pushMatch);
    }

    @Override
    public String getClient() {
        return client;
    }
}