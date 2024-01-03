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

import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_FIELDS;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_LEFT_HAND_LIMIT;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_ORDER;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_ORDER_BY;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RANGE_END;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RANGE_START;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RIGHT_HAND_LIMIT;
import static com.openexchange.tools.arrays.Collections.unmodifiableSet;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.json.converter.EventResultConverter;
import com.openexchange.chronos.json.converter.EventsPerAttendeeResultConverter;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link NeedsActionAction}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
@RestrictedAction(module = ChronosAction.MODULE, type = RestrictedAction.Type.READ)
public class NeedsActionAction extends ChronosAction {

    private static final Set<String> REQUIRED_PARAMETERS = unmodifiableSet(PARAM_RANGE_START, PARAM_RANGE_END);

    private static final Set<String> OPTIONAL_PARAMETERS = unmodifiableSet(PARAM_ORDER_BY, PARAM_ORDER, PARAM_FIELDS, PARAM_LEFT_HAND_LIMIT, PARAM_RIGHT_HAND_LIMIT);

    /** The parameter indicating whether an extended response, including the events needing action for delegate attendees, is requested */
    private static final String PARAM_INCLUDE_DELEGATES = "includeDelegates";

    /**
     * Initializes a new {@link NeedsActionAction}.
     *
     * @param services A service lookup reference
     */
    protected NeedsActionAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected Set<String> getRequiredParameters() {
        return REQUIRED_PARAMETERS;
    }

    @Override
    protected Set<String> getOptionalParameters() {
        return OPTIONAL_PARAMETERS;
    }

    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException {
        String includeDelegates = requestData.getParameter(PARAM_INCLUDE_DELEGATES);
        Map<Attendee, EventsResult> eventsPerAttendee = calendarAccess.getEventsNeedingAction(Boolean.parseBoolean(includeDelegates));
        if (Strings.isEmpty(includeDelegates)) {
            /*
             * return 'legacy' flat result if parameter not specified
             */
            Set<Entry<Attendee, EventsResult>> entrySet = eventsPerAttendee.entrySet();
            if (1 == entrySet.size()) {
                Entry<Attendee, EventsResult> entry = entrySet.iterator().next();
                if (requestData.getSession().getUserId() == entry.getKey().getEntity()) {
                    if (null != entry.getValue().getError()) {
                        throw entry.getValue().getError();
                    }
                    return new AJAXRequestResult(entry.getValue().getEvents(), new Date(entry.getValue().getTimestamp()), EventResultConverter.INPUT_FORMAT);
                }
            }
            throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Unexpected 'needs-action' result");
        }
        return new AJAXRequestResult(eventsPerAttendee, EventsPerAttendeeResultConverter.INPUT_FORMAT);
    }

}
