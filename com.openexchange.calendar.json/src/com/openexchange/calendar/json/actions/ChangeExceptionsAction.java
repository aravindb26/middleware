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

package com.openexchange.calendar.json.actions;

import java.util.List;
import org.json.JSONException;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.calendar.json.AppointmentAJAXRequest;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
/**
 * {@link ChangeExceptionsAction}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
@RestrictedAction(module = AppointmentAction.MODULE, type = RestrictedAction.Type.READ)
public class ChangeExceptionsAction extends AppointmentAction {

    /**
     * Initializes a new {@link ChangeExceptionsAction}.
     *
     * @param services A service lookup reference
     */
    public ChangeExceptionsAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(CalendarSession session, AppointmentAJAXRequest request) throws OXException, JSONException {
        String folderId = request.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
        String objectId = request.checkParameter(AJAXServlet.PARAMETER_ID);
        List<Event> events = session.getCalendarService().getChangeExceptions(session, folderId, objectId);
        return getAppointmentResultWithTimestamp(getEventConverter(session), events);
    }

}
