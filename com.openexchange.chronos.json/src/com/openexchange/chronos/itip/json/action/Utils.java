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

package com.openexchange.chronos.itip.json.action;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.json.converter.mapper.EventMapper;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * 
 * {@link Utils}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public class Utils {

    private Utils() {}

    /**
     * Converts the given list of events to a {@link AJAXRequestResult}
     *
     * @param session The session
     * @param tz The timezone to consider for the client
     * @param events The list of events to convert
     * @return A {@link AJAXRequestResult}
     * @throws OXException In case serialization fails
     * @throws JSONException In case serialization fails
     */
    public static AJAXRequestResult convertToResult(Session session, TimeZone tz, List<Event> events) throws OXException, JSONException {
        List<Event> sorted = CalendarUtils.sortSeriesMasterFirst(events);
        if (sorted != null) {
            JSONArray array = new JSONArray(sorted.size());
            for (Event event : sorted) {
                JSONObject object = EventMapper.getInstance().serialize(event, EventMapper.getInstance().getAssignedFields(event), tz, session);
                array.put(object);
            }
            return new AJAXRequestResult(array, new Date(), "json");
        }

        JSONObject object = new JSONObject();
        object.put("msg", "Done");
        return new AJAXRequestResult(object, new Date(), "json");
    }

    /** The parameter name for the timezone format to use */
    private static final String TIMEZONE = "timezone";

    /**
     * Get the timezone for the user
     *
     * @param request The request optional containing the timezone
     * @param session The users session to get the timezone from
     * @return The timezone to use
     * @throws OXException In case server session can't be created
     */
    public static TimeZone getTimeZone(AJAXRequestData request, Session session) throws OXException {
        TimeZone tz = TimeZone.getTimeZone(ServerSessionAdapter.valueOf(session).getUser().getTimeZone());
        String timezoneParameter = request.getParameter(TIMEZONE);
        return timezoneParameter == null ? tz : TimeZone.getTimeZone(timezoneParameter);
    }

}
