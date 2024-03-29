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

package com.openexchange.userfeedback.json.actions;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.notify.hostname.HostnameService;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.userfeedback.FeedbackService;
import com.openexchange.userfeedback.json.osgi.Services;

/**
 * {@link StoreAction}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.4
 */
public class StoreAction implements AJAXActionService {

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        Object data = requestData.getData();
        if ((data == null) || (false == (data instanceof JSONObject))) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();

        }
        JSONObject dataObject = (JSONObject) data;

        String type = requestData.getParameter("type");
        if (null == type || Strings.isEmpty(type)) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create("type");
        }

        FeedbackService service = Services.getService(FeedbackService.class);
        if (service == null) {
            throw ServiceExceptionCode.absentService(FeedbackService.class);
        }
        String hostname = getHostname(requestData, session);

        Map<String, String> params = new HashMap<>();
        params.put("type", type);
        params.put("hostname", hostname);

        service.store(session, dataObject, params);

        return new AJAXRequestResult();
    }

    private static String getHostname(AJAXRequestData request, ServerSession session) {
        String hostname = null;
        HostnameService hostnameService = Services.optService(HostnameService.class);

        if (hostnameService != null) {
            hostname = hostnameService.getHostname(session.getUserId(), session.getContextId());
        }

        if (hostname == null) {
            hostname = request.getHostname();
        }

        return hostname;
    }
}
