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

package com.openexchange.deputy.json.action;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.deputy.DeputyPermission;
import com.openexchange.deputy.DeputyService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link NewDeputyAction} - Performs the "get" action of the deputy permission module.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class NewDeputyAction extends AbstractDeputyAction {

    /**
     * Initializes a new {@link NewDeputyAction}.
     *
     * @param services The service look-up
     */
    public NewDeputyAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult doPerform(DeputyService deputyService, AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
        JSONValue jDeputyPermission = requireJSONBody(requestData);
        if (jDeputyPermission.isArray()) {
            throw AjaxExceptionCodes.INVALID_REQUEST_BODY.create(JSONObject.class, jDeputyPermission.getClass());
        }

        DeputyPermission deputyPermission = parseDeputyPermission(jDeputyPermission.toObject());
        String deputyId = deputyService.grantDeputyPermission(deputyPermission, session);

        return new AJAXRequestResult(deputyService.getDeputyPermission(deputyId, session), "deputypermission");
    }

}