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

package com.openexchange.pns.json;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction.Type;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.pns.DefaultToken;
import com.openexchange.pns.Meta;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushNotifications;
import com.openexchange.pns.PushSubscriptionDescription;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.PushSubscriptionRestrictions;
import com.openexchange.pns.PushSubscriptionResult;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link SubscribeAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
@RestrictedAction(type = RestrictedAction.Type.READ, hasCustomRestrictedAccessCheck = true)
public class SubscribeAction extends AbstractPushJsonAction {

    /**
     * Initializes a new {@link SubscribeAction}.
     */
    public SubscribeAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
        JSONObject jRequestBody = (JSONObject) requestData.requireData();

        PushSubscriptionRegistry subscriptionRegistry = services.getOptionalService(PushSubscriptionRegistry.class);
        if (null == subscriptionRegistry) {
            throw ServiceExceptionCode.absentService(PushSubscriptionRegistry.class);
        }

        String client = jRequestBody.optString("client", null);
        if (null == client) {
            client = session.getClient();
        }
        if (Strings.isEmpty(client)) {
            throw AjaxExceptionCodes.MISSING_FIELD.create("client");
        }

        String token = jRequestBody.optString("token", null);
        String transportId = requireStringField("transport", jRequestBody);
        JSONArray jTopics = requireArrayField("topics", jRequestBody);
        Meta optionalMeta = optionalMetaFrom("meta", jRequestBody);
        long expirationTimeMillis = jRequestBody.optLong("expires", 0);

        List<String> topics = new ArrayList<>(jTopics.length());
        for (Object topicObj : jTopics) {
            String topic = topicObj.toString();
            try {
                PushNotifications.validateTopicName(topic);
            } catch (IllegalArgumentException e) {
                throw PushExceptionCodes.INVALID_TOPIC.create(e, topic);
            }
            topics.add(topic);
        }

        PushSubscriptionDescription desc = new PushSubscriptionDescription()
            .setClient(client)
            .setTopics(topics)
            .setContextId(session.getContextId())
            .setToken(DefaultToken.builder().withValue(token).withMeta(optionalMeta).build())
            .setTransportId(transportId)
            .setUserId(session.getUserId())
            .setExpires(expirationTimeMillis == 0 ? null : new Date(expirationTimeMillis));

        boolean retry;
        do {
            retry = doSubscribe(desc, subscriptionRegistry);
        } while (retry);

        return new AJAXRequestResult(new JSONObject(2).put("success", true), "json");
    }

    private static boolean doSubscribe(PushSubscriptionDescription desc, PushSubscriptionRegistry subscriptionRegistry) throws OXException {
        PushSubscriptionRestrictions restrictions = new PushSubscriptionRestrictions();
        PushSubscriptionResult result = subscriptionRegistry.registerSubscription(desc, restrictions);
        switch (result.getStatus()) {
            case CONFLICT:
                {
                    // Unsubscribe conflicting subscription
                    PushSubscriptionDescription subscriptionToDrop = new PushSubscriptionDescription()
                        .setClient(desc.getClient())
                        .setContextId(result.getTokenUsingContextId())
                        .setToken(desc.getToken())
                        .setTransportId(desc.getTransportId())
                        .setUserId(result.getTokenUsingUserId());
                    LogProperties.put(LogProperties.Name.PNS_NO_RECONNECT, "true");
                    try {
                        subscriptionRegistry.unregisterSubscription(subscriptionToDrop, restrictions);
                    } finally {
                        LogProperties.remove(LogProperties.Name.PNS_NO_RECONNECT);
                    }
                    return true;
                }
            case FAIL:
                throw result.getError();
            case OK:
                /* fall-through */
            default:
                break;
        }

        // Success
        return false;
    }

    @Override
    public String getAction() {
        return "subscribe";
    }

    // ---------------------------- access check methods ------------------------------

    @RestrictedAccessCheck
    public boolean accessAllowed(AJAXRequestData request, @SuppressWarnings("unused") ServerSession session, Scope scope) throws OXException {
        JSONObject jRequestBody = (JSONObject) request.requireData();
        JSONArray jTopics = requireArrayField("topics", jRequestBody);
        if (jTopics.isEmpty()) {
            return false;
        }
        return jTopics.asList()
                      .stream()
                      .map(s -> s.toString())
                      .allMatch(topic -> isValidOAuthTopic(topic, scope));
    }

    /**
     * Checks if the given topic can be accessed with the given scope
     *
     * @param topic The topic
     * @param scope The users scope
     * @return <code>true</code> if the topic can be accessed, <code>false</code> otherwise
     */
    private boolean isValidOAuthTopic(String topic, Scope scope) {
        if (Strings.isEmpty(topic)) {
            return false;
        }
        if (topic.startsWith("ox:mail:")) {
            return scope.has(Type.READ.getScope(Module.MAIL.getName()));
        }
        if (topic.startsWith("ox:calendar:")) {
            return scope.has(Type.READ.getScope(Module.CALENDAR.getName()));
        }
        return false;
    }

}
