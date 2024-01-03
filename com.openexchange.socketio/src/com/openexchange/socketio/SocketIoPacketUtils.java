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


package com.openexchange.socketio;

import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * {@link SocketIoPacketUtils} - Utility class for Socket.IO packets.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public final class SocketIoPacketUtils {

    /**
     * Initializes a new {@link SocketIoPacketUtils}.
     */
    private SocketIoPacketUtils() {
        super();
    }

    /**
     * Checks validity of given JSON array packet data
     *
     * @param array The JSON array packet data
     * @return <code>true</code> if valid; otherwise <code>false</code> if invalid
     */
    public static boolean isPacketDataValid(JSONArray array) {
        try {
            for (int idx = 0; idx < array.length(); idx++) {
                final Object item = array.get(idx);

                if (!isPacketDataValidType(item)) {
                    return false;
                }
                if (item == null) {
                    array.put(idx, JSONObject.NULL);
                }
                if ((item instanceof JSONArray) && !isPacketDataValid((JSONArray) item)) {
                    return false;
                }
                if ((item instanceof JSONObject) && !isPacketDataValid((JSONObject) item)) {
                    return false;
                }
            }

            return true;
        } catch (JSONException ignore) {
            // Nothing
        }
        return false;
    }

    /**
     * Checks validity of given JSON object packet data
     *
     * @param object The JSON object packet data
     * @return <code>true</code> if valid; otherwise <code>false</code> if invalid
     */
    public static boolean isPacketDataValid(JSONObject object) {
        try {
            final Iterator<?> keys = object.keys();
            while (keys.hasNext()) {
                final Object keyObj = keys.next();
                if (!(keyObj instanceof String)) {
                    return false;
                }

                final String key = (String) keyObj;
                final Object item = object.get(key);

                if (!isPacketDataValidType(item)) {
                    return false;
                }
                if (item == null) {
                    object.put(key, JSONObject.NULL);
                }
                if ((item instanceof JSONArray) && !isPacketDataValid((JSONArray) item)) {
                    return false;
                }
                if ((item instanceof JSONObject) && !isPacketDataValid((JSONObject) item)) {
                    return false;
                }
            }

            return true;
        } catch (JSONException ignore) {
            // Nothing
        }
        return false;
    }

    private static boolean isPacketDataValidType(Object object) {
        return ((object == null) ||
                (object == JSONObject.NULL) ||
                (object instanceof JSONObject) ||
                (object instanceof JSONArray) ||
                (object instanceof CharSequence) ||
                (object instanceof Number) ||
                (object instanceof Boolean) ||
                (object instanceof byte[]));
    }

}
