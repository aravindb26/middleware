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

package com.openexchange.chronos.scheduling.common;

import static com.openexchange.java.Autoboxing.I;
import java.util.Base64;
import java.util.Objects;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.changes.ChangeAction;
import com.openexchange.java.Charsets;
import com.openexchange.java.Enums;

/**
 * {@link ChronosITipData}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ChronosITipData {

    /**
     * The name of the property when used within a <code>VCALENDAR</code> component, or the header name when injected into scheduling
     * mails, or as key of the additional property within a parsed {@link IncomingSchedulingMessage}.
     */
    public static final String PROPERTY_NAME = "X-OX-ITIP";

    private final String serverUid;
    private final int contextId;
    private final ChangeAction action;
    private final int sentByResource;

    /**
     * Initializes a new {@link ChronosITipData}.
     *
     * @param serverUid The unique server identifier, or <code>null</code> if not applicable
     * @param contextId The context identifier
     * @param action The change action
     * @param sentByResource The internal identifier of the resource the message is sent by, or <code>-1</code> if not applicable
     */
    public ChronosITipData(String serverUid, int contextId, ChangeAction action, int sentByResource) {
        super();
        this.serverUid = serverUid;
        this.contextId = contextId;
        this.action = action;
        this.sentByResource = sentByResource;
    }

    /**
     * Gets the unique server identifier.
     *
     * @return The unique server identifier, or <code>null</code> if not applicable
     */
    public String getServerUid() {
        return serverUid;
    }

    /**
     * Gets the context identifier.
     *
     * @return The context identifier
     */
    public int getContextId() {
        return contextId;
    }

    /**
     * Gets the change action
     *
     * @return The change action
     */
    public ChangeAction getAction() {
        return action;
    }

    /**
     * Gets the internal identifier of the resource the message is sent by.
     *
     * @return The internal identifier of the resource the message is sent by, or <code>-1</code> if not applicable
     */
    public int getSentByResource() {
        return sentByResource;
    }

    /**
     * Gets a value indicating whether this iTIP data matches the given server- and context identifier, i.e. whether it originates from a
     * context on a certain deployment.
     * <p/>
     * This check usually needs to be done to decide whether a scheduling object resource can be treated as <i>internal</i> resource or
     * not.
     *
     * @param serverUid The unique server id to check against
     * @param contextId The context id to check against
     * @return <code>true</code> if unique server and context id are matching, <code>false</code>, otherwise
     */
    public boolean matches(String serverUid, int contextId) {
        return this.contextId == contextId && Objects.equals(this.serverUid, serverUid);
    }

    @Override
    public String toString() {
        return "ChronosITipData [serverUid=" + serverUid + ", contextId=" + contextId + ", action=" + action + ", sentByResource=" + sentByResource + "]";
    }

    public static ChronosITipData decode(String value) {
        if (null == value || 2 > value.length() || false == value.startsWith("1:")) {
            throw new IllegalArgumentException(value);
        }
        try {
            return deserialize(new JSONObject(Base64.getDecoder().decode(value.substring(2))));
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static String encode(ChronosITipData value) {
        JSONObject jsonObject = serialize(value);
        String encoded = Base64.getEncoder().withoutPadding().encodeToString(jsonObject.toString(true).getBytes(Charsets.US_ASCII));
        return "1:" + encoded;
    }

    private static ChronosITipData deserialize(JSONObject jsonObject) throws JSONException {
        String serverUid = jsonObject.optString("serverUid", null);
        int contextId = jsonObject.getInt("contextId");
        ChangeAction action = Enums.parse(ChangeAction.class, jsonObject.getString("action"));
        int sentByResource = jsonObject.optInt("sentByResource", -1);
        return new ChronosITipData(serverUid, contextId, action, sentByResource);
    }

    private static JSONObject serialize(ChronosITipData value) {
        return new JSONObject() // @formatter:off
            .putSafe("serverUid", value.serverUid)
            .putSafe("contextId", I(value.contextId))
            .putSafe("action", value.action.name())
            .putSafe("sentByResource", I(value.sentByResource))
        ; // @formatter:on
    }

}
