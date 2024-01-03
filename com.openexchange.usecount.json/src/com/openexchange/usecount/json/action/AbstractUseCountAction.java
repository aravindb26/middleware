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

package com.openexchange.usecount.json.action;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.contactcollector.ContactCollectorService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.objectusecount.ObjectUseCountService;
import com.openexchange.principalusecount.PrincipalUseCountService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.usecount.json.Type;

/**
 * {@link AbstractUseCountAction} - Abstract use-count action.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public abstract class AbstractUseCountAction implements AJAXActionService {

    /**
     * The service look-up
     */
    protected final ServiceLookup services;

    /**
     * Initializes a new {@link AbstractUseCountAction}.
     *
     * @param services The service look-up
     */
    protected AbstractUseCountAction(ServiceLookup services) {
        super();
        this.services = services;
    }

    /**
     * Gets the principal use-count service.
     *
     * @return The principal use-count service
     * @throws OXException If principal use-count service cannot be returned
     */
    protected PrincipalUseCountService getPrincipalUseCountService() throws OXException {
        return services.getServiceSafe(PrincipalUseCountService.class);
    }

    /**
     * Gets the object use-count service.
     *
     * @return The object use-count service
     * @throws OXException If object use-count service cannot be returned
     */
    protected ObjectUseCountService getObjectUseCountService() throws OXException {
        return services.getServiceSafe(ObjectUseCountService.class);
    }

    /**
     * Gets the contact-collector service.
     *
     * @return The contact-collector service
     * @throws OXException If contact-collector service cannot be returned
     */
    protected ContactCollectorService getContactCollectorService() throws OXException {
        return services.getServiceSafe(ContactCollectorService.class);
    }

    /**
     * Requires a JSON content in given request's data.
     *
     * @param requestData The request data to read from
     * @return The JSON content
     * @throws OXException If JSON content cannot be returned
     */
    protected JSONValue requireJSONBody(AJAXRequestData requestData) throws OXException {
        Object data = requestData.getData();
        if (null == data) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
        }
        JSONValue jBody = requestData.getData(JSONValue.class);
        if (null == jBody) {
            throw AjaxExceptionCodes.INVALID_REQUEST_BODY.create(JSONValue.class, data.getClass());
        }
        return (JSONValue) data;
    }

    /**
     * Gets the <code>"id"</code> field from given JSON object.
     *
     * @param jBody The JSON object to grab from
     * @return The identifier
     * @throws OXException If identifier cannot be returned
     */
    protected int getIdFrom(JSONObject jBody) throws OXException {
        String id = jBody.optString("id", null);
        if (id == null) {
            throw AjaxExceptionCodes.MISSING_FIELD.create("id");
        }
        try {
            return Strings.parseInt(id);
        } catch (NumberFormatException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER.create(e, "id");
        }
    }

    /**
     * Gets specified integer field from given JSON object.
     *
     * @param field The name of the integer field
     * @param jBody The JSON object to grab from
     * @param noEntryValue The <code>int</code> value that represents <code>null</code>
     * @return The <code>int</code> value
     * @throws OXException If <code>int</code> value cannot be returned
     */
    protected int getIntFrom(String field, JSONObject jBody, int noEntryValue) throws OXException {
        int id = jBody.optInt(field, noEntryValue);
        if (id <= noEntryValue) {
            if (jBody.has(field)) {
                throw AjaxExceptionCodes.INVALID_PARAMETER.create(field);
            }
            throw AjaxExceptionCodes.MISSING_FIELD.create(field);
        }
        return id;
    }

    /**
     * Gets the <code>"type"</code> field from given JSON object.
     *
     * @param jBody The JSON object to grab from
     * @return The parsed type
     * @throws OXException If type cannot be returned
     */
    protected Type getTypeFrom(JSONObject jBody) throws OXException {
        String sType = jBody.optString("type", null);
        if (Strings.isEmpty(sType)) {
            throw AjaxExceptionCodes.MISSING_FIELD.create("type");
        }
        Type type = Type.typeFor(sType);
        if (type == null) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create("type", type);
        }
        return type;
    }

    @Override
    public AJAXRequestResult perform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            return doPerform(requestData, session);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Performs given use-count request.
     *
     * @param requestData The request data
     * @param session The session providing user information
     * @return The AJAX result
     * @throws OXException If performing request fails
     */
    protected abstract AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException;

}
