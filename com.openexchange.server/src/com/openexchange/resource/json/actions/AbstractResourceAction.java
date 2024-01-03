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

package com.openexchange.resource.json.actions;

import static com.openexchange.tools.TimeZoneUtils.getTimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.resource.json.ResourceAJAXRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractResourceAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractResourceAction implements AJAXActionService {

    private static final AJAXRequestResult RESULT_JSON_NULL = new AJAXRequestResult(JSONObject.NULL, "json");

    protected final ServiceLookup services;

    /**
     * Initializes a new {@link AbstractResourceAction}.
     */
    protected AbstractResourceAction(final ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Gets the service of specified type
     *
     * @param clazz The service's class
     * @return The service or <code>null</code> if absent
     */
    protected <S> S getService(final Class<? extends S> clazz) {
        return services.getService(clazz);
    }

    @Override
    public AJAXRequestResult perform(final AJAXRequestData requestData, final ServerSession session) throws OXException {
        try {
            final ResourceAJAXRequest reminderRequest = new ResourceAJAXRequest(requestData, session);
            final String sTimeZone = requestData.getParameter(AJAXServlet.PARAMETER_TIMEZONE);
            if (null != sTimeZone) {
                reminderRequest.setTimeZone(getTimeZone(sTimeZone));
            }
            return perform(reminderRequest);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Performs specified group request.
     *
     * @param req The group request
     * @return The result
     * @throws OXException If an error occurs
     * @throws JSONException If a JSON error occurs
     */
    protected abstract AJAXRequestResult perform(ResourceAJAXRequest req) throws OXException, JSONException;

    /**
     * Gets the result filled with JSON <code>NULL</code>.
     *
     * @return The result with JSON <code>NULL</code>.
     */
    protected static AJAXRequestResult getJSONNullResult() {
        return RESULT_JSON_NULL;
    }
}
