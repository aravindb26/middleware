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

import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_CHECK_CONFLICTS;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_EXPAND;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_FIELDS;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_PUSH_TOKEN;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RANGE_END;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_RANGE_START;
import static com.openexchange.chronos.json.action.ChronosJsonParameters.PARAM_SCHEDULING;
import static com.openexchange.tools.arrays.Collections.unmodifiableSet;
import java.util.Date;
import java.util.Set;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.json.converter.CalendarResultConverter;
import com.openexchange.chronos.provider.composition.IDBasedCalendarAccess;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link NewAction}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
@RestrictedAction(module = ChronosAction.MODULE, type = RestrictedAction.Type.WRITE)
public class NewAction extends ChronosAction {

    private static final Set<String> OPTIONAL_PARAMETERS = unmodifiableSet(PARAM_SCHEDULING, PARAM_CHECK_CONFLICTS, PARAM_RANGE_START, PARAM_RANGE_END, PARAM_EXPAND, PARAM_FIELDS, PARAM_PUSH_TOKEN);

    /**
     * Initializes a new {@link NewAction}.
     *
     * @param services A service lookup reference
     */
    protected NewAction(ServiceLookup services) {
        super(services);
    }

    @Override
    protected Set<String> getOptionalParameters() {
        return OPTIONAL_PARAMETERS;
    }

    @Override
    protected AJAXRequestResult perform(IDBasedCalendarAccess calendarAccess, AJAXRequestData requestData) throws OXException {
        String folderId = requestData.requireParameter(AJAXServlet.PARAMETER_FOLDERID);
        Event event = parseEvent(requestData);
        if (false == calendarAccess.contains(CalendarParameters.PARAMETER_TRACK_ATTENDEE_USAGE)) {
            calendarAccess.set(CalendarParameters.PARAMETER_TRACK_ATTENDEE_USAGE, Boolean.TRUE);
        }
        try {
            CalendarResult calendarResult = calendarAccess.createEvent(folderId, event);
            return new AJAXRequestResult(calendarResult, new Date(calendarResult.getTimestamp()), CalendarResultConverter.INPUT_FORMAT);
        } catch (OXException e) {
            return handleConflictException(e);
        }
    }
}
