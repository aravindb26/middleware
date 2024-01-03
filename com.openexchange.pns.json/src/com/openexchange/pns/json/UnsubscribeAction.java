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

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction.Type;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.Strings;
import com.openexchange.pns.DefaultToken;
import com.openexchange.pns.PushSubscriptionDescription;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.PushSubscriptionRestrictions;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link UnsubscribeAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
@RestrictedAction(type = RestrictedAction.Type.WRITE, hasCustomRestrictedAccessCheck = true)
public class UnsubscribeAction extends AbstractPushJsonAction {

    /**
     * Initializes a new {@link UnsubscribeAction}.
     */
    public UnsubscribeAction(ServiceLookup services) {
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

        PushSubscriptionDescription desc = new PushSubscriptionDescription()
            .setClient(client)
            .setContextId(session.getContextId())
            .setToken(DefaultToken.tokenFor(token))
            .setTransportId(transportId)
            .setUserId(session.getUserId());

        subscriptionRegistry.unregisterSubscription(desc, new PushSubscriptionRestrictions());

        return new AJAXRequestResult(new JSONObject(2).put("success", true), "json");
    }

    @Override
    public String getAction() {
        return "unsubscribe";
    }

    // ---------------------------- access check methods ------------------------------

    @SuppressWarnings("unused")
    @RestrictedAccessCheck
    public boolean accessAllowed(AJAXRequestData request, ServerSession session, Scope scope) throws OXException {
        // Allow unsubscribe if at least one of the relevant scopes is present since topic matching is not possible
        return scope.has(Type.READ.getScope(Module.MAIL.getName())) ||
               scope.has(Type.READ.getScope(Module.CALENDAR.getName()));
    }

}
