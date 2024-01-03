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

package com.openexchange.chronos.json.converter;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Converter;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.json.converter.mapper.AttendeesMapping;
import com.openexchange.chronos.json.converter.mapper.EventMapper;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link EventsPerAttendeeResultConverter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class EventsPerAttendeeResultConverter extends EventResultConverter {

    @SuppressWarnings("hiding")
    public static final String INPUT_FORMAT = "eventsResultsPerAttendee";

    /**
     * Initializes a new {@link EventsPerAttendeeResultConverter}.
     *
     * @param services A service lookup reference
     */
    public EventsPerAttendeeResultConverter(ServiceLookup services) {
        super(services);
    }

    @Override
    public String getInputFormat() {
        return INPUT_FORMAT;
    }

    @Override
    public String getOutputFormat() {
        return "json";
    }

    @Override
    public Quality getQuality() {
        return Quality.GOOD;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void convert(AJAXRequestData requestData, AJAXRequestResult result, ServerSession session, Converter converter) throws OXException {
        /*
         * check and convert result object
         */
        Object resultObject = result.getResultObject();
        if (null != resultObject) {
            Map<Attendee, EventsResult> eventsResults;
            try {
                eventsResults = (Map<Attendee, EventsResult>) resultObject;
            } catch (ClassCastException e) {
                throw new UnsupportedOperationException(e);
            }
            resultObject = convertEventsResults(eventsResults, getTimeZoneID(requestData, session), session, getFields(requestData), isExtendedEntities(requestData));
        }
        result.setResultObject(resultObject, getOutputFormat());
    }

    /**
     * Serializes a map of events results to their JSON representation.
     * 
     * @param eventsResults The events results to convert, mapped to their associated attendee
     * @param timeZoneID The identifier of the timezone to consider when converting API date/time properties
     * @param session The current user's session
     * @param requestedFields The event fields to consider as requested by the client
     * @param extendedEntities <code>true</code> to lookup and inject additional information for <i>internal</i> calendar user entities,
     *            <code>false</code>, otherwise
     * @return The converted events results, or <code>null</code> if passed map argument has been <code>null</code>
     * @throws OXException If serialization fails for any reason
     */
    private JSONValue convertEventsResults(Map<Attendee, EventsResult> eventsResults, String timeZoneID, ServerSession session, Set<EventField> requestedFields, boolean extendedEntities) throws OXException {
        if (null == eventsResults) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(eventsResults.size());
        for (Entry<Attendee, EventsResult> entry : eventsResults.entrySet()) {
            EventsResult eventsResult = entry.getValue();
            if (null != eventsResult) {
                jsonArray.put(convertEventsResult(entry.getKey(), eventsResult, timeZoneID, session, requestedFields, extendedEntities));
            }
        }
        return jsonArray;
    }

    /**
     * Serializes an events result associated with a certain attendee into its JSON representation.
     * 
     * @param attendee The attendee where the events result is associated with
     * @param eventsResult The events result to convert
     * @param timeZoneID The identifier of the timezone to consider when converting API date/time properties
     * @param session The current user's session
     * @param requestedFields The event fields to consider as requested by the client
     * @param extendedEntities <code>true</code> to lookup and inject additional information for <i>internal</i> calendar user entities,
     *            <code>false</code>, otherwise
     * @return The converted events result, or <code>null</code> if passed events result argument has been <code>null</code>
     * @throws OXException If serialization fails for any reason
     */
    private JSONObject convertEventsResult(Attendee attendee, EventsResult eventsResult, String timeZoneID, ServerSession session, Set<EventField> requestedFields, boolean extendedEntities) throws OXException {
        if (null == eventsResult) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(3);
        try {
            jsonObject.put("attendee", ((AttendeesMapping<?>) EventMapper.getInstance().get(EventField.ATTENDEES)).serialize(attendee, TimeZone.getTimeZone(timeZoneID)));
            if (null != eventsResult.getError()) {
                JSONObject error = new JSONObject();
                ResponseWriter.addException(error, eventsResult.getError(), session.getUser().getLocale());
                jsonObject.put("error", error);
            } else {
                jsonObject.putOpt("events", convertEvents(eventsResult.getEvents(), timeZoneID, session, requestedFields, extendedEntities));
                jsonObject.put("timestamp", eventsResult.getTimestamp());
            }
        } catch (JSONException e) {
            throw OXJSONExceptionCodes.JSON_WRITE_ERROR.create(e);
        }
        return jsonObject;
    }

}
