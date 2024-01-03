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

import static com.openexchange.java.Autoboxing.I;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.InternalCalendarResult;
import com.openexchange.chronos.impl.performer.AbstractUpdatePerformer;
import com.openexchange.chronos.impl.performer.PutPerformer;
import com.openexchange.chronos.impl.performer.ResolvePerformer;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link CounterProcessor} - Handles incoming <code>COUNTER</code> message by either accepting the proposed changes and sending updated
 * <code>REQUEST</code> messages to the attendees, or by declining the changes and sending a <code>DECLINECOUNTER</code> message to the
 * countering attendee.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 * @see <a href="https://tools.ietf.org/html/rfc5546#section-3.2.7">RFC5546 Section 3.2.7</a>
 */
public class CounterProcessor extends AbstractUpdatePerformer {

    /**
     * Initializes a new {@link CounterProcessor}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     * @param calendarFolder The calendar folder representing the current view on the events
     * @throws OXException In case of error
     */
    public CounterProcessor(CalendarSession session, CalendarStorage storage, CalendarFolder calendarFolder) throws OXException {
        super(storage, session, calendarFolder);
    }

    /**
     * Processes an incoming <code>COUNTER</code> message by either accepting the proposed changes or by declining the changes.
     *
     * @param message The counter message
     * @param decline <code>true</code> sending a <code>DECLINECOUNTER</code> message to the countering attendee,
     *            <code>false</code> to accept the proposed changes and sending updated <code>REQUEST</code> messages to the attendees
     * @return The calendar result
     * @throws OXException In case the processing fails
     */
    public InternalCalendarResult process(IncomingSchedulingMessage message, boolean decline) throws OXException {
        if (decline) {
            /*
             * load original scheduling object resource
             */
            List<Event> storedEvents = new ResolvePerformer(session, storage).resolveEventsByUID(message.getResource().getUid(), calendarUserId);
            if (null == storedEvents || storedEvents.isEmpty()) {
                throw CalendarExceptionCodes.EVENT_NOT_FOUND.create(I(-1));
            }
            CalendarObjectResource originalResource = new DefaultCalendarObjectResource(storedEvents);
            /*
             * lookup countering attendee
             */
            CalendarUser counteringAttendee = CalendarUtils.find(originalResource, message.getSchedulingObject().getOriginator());
            if (null == counteringAttendee) {
                throw CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(I(-1), originalResource.getFirstEvent().getId());
            }
            /*
             * prepare decline counter message to this attendee & return empty result
             */
            schedulingHelper.trackDeclineCounter(originalResource, Collections.singletonList(counteringAttendee));
            return resultTracker.getResult();
        }
        /*
         * apply incoming counter proposal (only considering date-time related changes)
         */
        return new PutPerformer(this).perform(message.getResource(), false, getIgnoredFields());
    }

    private EventField[] getIgnoredFields() {
        EventField[] counterFields = session.get(CalendarParameters.PARAMETER_COUNTER_FIELDS, EventField[].class);
        if (null == counterFields) {
            return counterFields;
        }
        EnumSet<EventField> eventFields = EnumSet.allOf(EventField.class);
        eventFields.removeAll(Arrays.asList(counterFields));
        return eventFields.toArray(new EventField[eventFields.size()]);
    }

}
