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

package com.openexchange.chronos.json.action;

import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RECURRENCE_ID;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RECURRENCE_RANGE;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXActionService;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceId;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.json.fields.ChronosJsonFields;
import com.openexchange.chronos.service.EventID;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;

/**
 * {@link AbstractChronosAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public abstract class AbstractChronosAction implements AJAXActionService {

    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractChronosAction.class);
    protected final ServiceLookup services;

    /**
     * Initializes a new {@link AbstractChronosAction}.
     */
    public AbstractChronosAction(ServiceLookup services) {
        this.services = services;
    }

    /**
     * Gets the request's data as JSON object.
     *
     * @return the JSON object
     * @throws OXException if the 'data' field is missing or is of invalid type
     */
    protected static JSONObject getJSONData(AJAXRequestData request) throws OXException {
        Object data = request.getData();
        if (null == data) {
            throw OXJSONExceptionCodes.MISSING_FIELD.create("data");
        }
        if (false == (data instanceof JSONObject)) {
            throw OXJSONExceptionCodes.INVALID_VALUE.create("data", data.toString());
        }
        return JSONObject.class.cast(data);
    }

    /**
     * Gets a list of required parameter names that will be evaluated. If missing in the request, an appropriate exception is thrown. By
     * default, an empty list is returned.
     *
     * @return The list of required parameters
     */
    protected Set<String> getRequiredParameters() {
        return Collections.emptySet();
    }

    /**
     * Gets a list of parameter names that will be evaluated if set, but are not required to fulfill the request. By default, an empty
     * list is returned.
     *
     * @return The list of optional parameters
     */
    protected Set<String> getOptionalParameters() {
        return Collections.emptySet();
    }

    protected <S extends Object> S requireService(Class<? extends S> clazz) throws OXException {
        return com.openexchange.osgi.Tools.requireService(clazz, services);
    }

    /**
     * Constructs a full event identifier from the supplied folder-, object- and recurrence-id values.
     *
     * @param folderId The folder identifier
     * @param objectId The object identifier
     * @param optRecurrenceId The recurrence identifier value, or <code>null</code> if not set
     * @param optRecurrenceRange The recurrence range value, or <code>null</code> if not set
     * @return The full event identifier
     */
    protected EventID getEventID(String folderId, String objectId, String optRecurrenceId, String optRecurrenceRange) throws OXException {
        if (null == optRecurrenceId) {
            return new EventID(folderId, objectId);
        }
        try {
            return new EventID(folderId, objectId, new DefaultRecurrenceId(CalendarUtils.decode(optRecurrenceId), optRecurrenceRange));
        } catch (IllegalArgumentException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(e, PARAM_RECURRENCE_ID, optRecurrenceId);
        }
    }

    /**
     * Parses a full event identifier based on the request parameters {@link AJAXServlet#PARAMETER_ID},
     * {@link AJAXServlet#PARAMETER_FOLDERID} and {@link AbstractChronosAction#PARAM_RECURRENCE_ID}.
     *
     * @param requestData The request data to parse the event identifier from
     * @return The parsed full event identifier
     */
    protected EventID parseIdParameter(AJAXRequestData requestData) throws OXException {
        String objectId = requestData.nonEmptyParameter(AJAXServlet.PARAMETER_ID);
        String folderId = requestData.nonEmptyParameter(AJAXServlet.PARAMETER_FOLDERID);
        String optRecurrenceId = requestData.getParameter(PARAM_RECURRENCE_ID);
        String optRecurrenceRange = requestData.getParameter(PARAM_RECURRENCE_RANGE);
        return getEventID(folderId, objectId, optRecurrenceId, optRecurrenceRange);
    }

    /**
     * Parses a full event identifier based on the request parameters {@link AJAXServlet#PARAMETER_ID},
     * {@link AJAXServlet#PARAMETER_FOLDERID} and {@link AbstractChronosAction#PARAM_RECURRENCE_ID}.
     *
     * @param jsonObject The JSON object to parse the event identifier from
     * @return The parsed full event identifier
     */
    protected EventID parseIdParameter(JSONObject jsonObject) throws OXException {
        String objectId;
        try {
            objectId = jsonObject.getString(AJAXServlet.PARAMETER_ID);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(e, AJAXServlet.PARAMETER_ID);
        }
        String folderId;
        try {
            folderId = jsonObject.getString(AJAXServlet.PARAMETER_FOLDERID);
        } catch (JSONException e) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(e, AJAXServlet.PARAMETER_FOLDERID);
        }
        String optRecurrenceId = jsonObject.optString(PARAM_RECURRENCE_ID, null);
        String optRecurrenceRange = jsonObject.optString(PARAM_RECURRENCE_RANGE, null);
        return getEventID(folderId, objectId, optRecurrenceId, optRecurrenceRange);
    }

    /**
     * Parses an array of full event identifiers, i.e. a JSON array with json object elements having the properties
     * {@link AJAXServlet#PARAMETER_ID}, {@link AJAXServlet#PARAMETER_FOLDERID} and optionally
     * {@link AbstractChronosAction#PARAM_RECURRENCE_ID}.
     *
     * @param jsonArray The json array off full event identifiers to parse
     * @return The parsed full event identifiers
     */
    protected List<EventID> parseEventIDs(JSONArray jsonArray) throws OXException {
        if (null == jsonArray) {
            return null;
        }
        List<EventID> eventIDs = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                eventIDs.add(parseIdParameter(jsonArray.getJSONObject(i)));
            } catch (JSONException e) {
                throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, e.getMessage());
            }
        }
        return eventIDs;
    }

    /**
     * Parses the attachment identifier from given request data
     *
     * @param requestData The request data
     * @return The parsed attachment identifier
     * @throws OXException If attachment identifier cannot be parsed
     */
    protected int parseAttachmentId(AJAXRequestData requestData) throws OXException {
        String managedId = requestData.requireParameter(ChronosJsonFields.Attachment.MANAGED_ID);
        try {
            return Integer.parseInt(managedId);
        } catch (NumberFormatException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER.create(e, ChronosJsonFields.Attachment.MANAGED_ID);
        }
    }

    protected long parseClientTimestamp(AJAXRequestData requestData) throws OXException {
        String parameter = requestData.checkParameter(AJAXServlet.PARAMETER_TIMESTAMP);
        try {
            return Long.parseLong(parameter.trim());
        } catch (NumberFormatException e) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create(AJAXServlet.PARAMETER_TIMESTAMP, parameter);
        }
    }

    protected boolean isConflict(OXException e) {
        return Category.CATEGORY_CONFLICT.equals(e.getCategory()) && (CalendarExceptionCodes.HARD_EVENT_CONFLICTS.equals(e) || CalendarExceptionCodes.EVENT_CONFLICTS.equals(e));
    }

}

