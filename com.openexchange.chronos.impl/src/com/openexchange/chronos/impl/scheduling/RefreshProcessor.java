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

package com.openexchange.chronos.impl.scheduling;

import java.util.Collections;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.common.mapping.DefaultEventUpdate;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.InternalCalendarResult;
import com.openexchange.chronos.impl.performer.AbstractUpdatePerformer;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link ReplyProcessor} - Handles incoming <code>REFRESH</code> message by sending a <code>REQUEST</code>
 * to the attendee requesting the refresh
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 * @see <a href="https://tools.ietf.org/html/rfc5546#section-3.2.6">RFC5546 Section 3.2.6</a>
 */
public class RefreshProcessor extends AbstractUpdatePerformer {

    /**
     * Initializes a new {@link RefreshProcessor}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param calendarFolder The calendar folder representing the current view on the events
     * @throws OXException In case of error
     */
    public RefreshProcessor(CalendarSession session, CalendarStorage storage, CalendarFolder calendarFolder) throws OXException {
        super(storage, session, calendarFolder);
    }

    /**
     * Processes the incoming REFRESH message
     *
     * @param message The message
     * @return The calendar result (empty) containing the scheduling message
     * @throws OXException In case of error
     */
    public InternalCalendarResult process(IncomingSchedulingMessage message) throws OXException {
        /*
         * Check if we can send the REQUEST
         */
        Event originalEvent = loadEventData(message.getResource().getUid());
        if (null == originalEvent) {
            throw CalendarExceptionCodes.EVENT_NOT_FOUND.create(message.getResource().getUid());
        }
        /*
         * Prepare data
         */
        CalendarObjectResource resource;
        if (CalendarUtils.isSeriesMaster(originalEvent)) {
            resource = new DefaultCalendarObjectResource(originalEvent, loadExceptionData(originalEvent, loadChangeExceptionDates(originalEvent.getId())));
        } else {
            resource = new DefaultCalendarObjectResource(originalEvent);
        }
        /*
         * Find attendee and prepare scheduling message
         */
        Attendee requestingAttendee = getRequestingAttendee(message.getSchedulingObject().getOriginator(), resource);
        if (null == requestingAttendee) {
            throw CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(message.getSchedulingObject().getOriginator(), originalEvent.getId());
        }
        schedulingHelper.trackUpdate(resource, originalEvent, new DefaultEventUpdate(null, null), Collections.singletonList(requestingAttendee));
        return resultTracker.getResult();
    }

    /**
     * Get the attendee who send the REFRESH request
     *
     * @param calendarUser The calendar user to search for
     * @param resource The resource the attendee wants to get the refresh for
     * @return The attendee or <code>null</code> if not found
     */
    private static Attendee getRequestingAttendee(CalendarUser calendarUser, CalendarObjectResource resource) {
        if (null == calendarUser || null == resource) {
            return null;
        }
        for (Event original : resource.getEvents()) {
            Attendee requestingAttendee = CalendarUtils.find(original.getAttendees(), calendarUser);
            if (null != requestingAttendee) {
                return requestingAttendee;
            }
        }
        return null;
    }
}
