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

package com.openexchange.sessiond.redis;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONServices;
import com.openexchange.pubsub.ChannelMessageCodec;

/**
 * {@link SessionEventChannelMessageCodec} - The channel message codec for cache events.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class SessionEventChannelMessageCodec implements ChannelMessageCodec<SessionEvent> {

    private static final SessionEventChannelMessageCodec INSTANCE = new SessionEventChannelMessageCodec();

    private static final String OPERATION = "operation";
    private static final String SESSION_IDS = "sessionIds";

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static SessionEventChannelMessageCodec getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link SessionEventChannelMessageCodec}.
     */
    private SessionEventChannelMessageCodec() {
        super();
    }

    @Override
    public String serialize(SessionEvent message) throws Exception {
        // Put operation to JSON representation
        JSONObject jMessage = new JSONObject(2);
        jMessage.put(OPERATION, message.getOperation().getId());

        // Put session identifiers to JSON representation
        List<String> sessionIds = message.getSessionIds();
        JSONArray jKeys = new JSONArray(sessionIds.size());
        for (String sessionId : sessionIds) {
            jKeys.put(sessionId);
        }
        jMessage.put(SESSION_IDS, jKeys);
        return jMessage.toString();
    }

    @Override
    public SessionEvent deserialize(String data) throws Exception {
        JSONObject jMessage = JSONServices.parseObject(data);

        // Get operation from JSON representation
        SessionOperation operation = SessionOperation.operationFor(jMessage.getString(OPERATION));

        // Get session identifiers from JSON representation
        JSONArray jSessionIds = jMessage.getJSONArray(SESSION_IDS);
        int length = jSessionIds.length();
        List<String> sessionIds = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            sessionIds.add(jSessionIds.getString(i));
        }
        return new SessionEvent(operation, sessionIds);
    }

}
