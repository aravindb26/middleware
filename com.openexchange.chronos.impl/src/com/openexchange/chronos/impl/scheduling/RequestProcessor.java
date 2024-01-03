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

import static com.openexchange.chronos.common.CalendarUtils.asExternal;
import static com.openexchange.chronos.common.CalendarUtils.asExternalOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.impl.scheduling.SchedulingUtils.usesOrganizerCopy;
import static com.openexchange.chronos.impl.scheduling.SchedulingUtils.validateOrganizer;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.IncomingCalendarObjectResource;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.InternalCalendarResult;
import com.openexchange.chronos.impl.performer.AbstractUpdatePerformer;
import com.openexchange.chronos.impl.performer.PutPerformer;
import com.openexchange.chronos.impl.performer.ResolvePerformer;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * {@link RequestProcessor} - Processes incoming REQUEST method
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class RequestProcessor extends AbstractUpdatePerformer {

    private SchedulingSource source;

    /**
     * Initializes a new {@link RequestProcessor}.
     * 
     * @param session The calendar session
     * @param storage The {@link ServiceLookup}
     * @param folder The calendar folder
     * @param source The source of the scheduling
     * @throws OXException In case of error
     */
    public RequestProcessor(CalendarSession session, CalendarStorage storage, CalendarFolder folder, SchedulingSource source) throws OXException {
        super(storage, session, folder);
        this.source = source;
    }

    /**
     * Puts a new or updated calendar object resource, i.e. an event and/or its change exceptions, to the calendar and afterwards
     * updates the attendee status as needed.
     * 
     * @param message The incoming message to get the calendar object resource to store from
     * @return The result
     * @throws OXException In case of error
     */
    public InternalCalendarResult process(IncomingSchedulingMessage message) throws OXException {
        if (usesOrganizerCopy(session, storage, message)) {
            /*
             * no-op if incoming scheduling message originates from notification mail targeting the organizer copy
             */
            return resultTracker.getResult();
        }
        CalendarObjectResource resource = message.getResource();
        /*
         * Check if user is a party-crasher within resolved attendees, put data as-is to storage if not
         */
        for (Event event : resource.getEvents()) {
            List<Attendee> attendees = AttendeeMapper.getInstance().copy(event.getAttendees(), (AttendeeField[]) null);
            attendees = session.getEntityResolver().prepare(attendees, new int[] { calendarUserId });
            if (contains(attendees, calendarUserId)) {
                check(message, resource);
                CalendarObjectResource preparedResource = prepareResource(resource, null);
                return new PutPerformer(this).perform(preparedResource, shallReplace(preparedResource));
            }
        }
        if (false == SchedulingSource.API.equals(source)) {
            /*
             * Avoid adding party-crasher automatically
             */
            throw CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(I(calendarUserId), resource.getUid());
        }
        /*
         * Current calendar user is party-crasher, prepare event by inserting the user with needs action
         */
        Attendee userAttendee = session.getEntityResolver().prepareUserAttendee(calendarUserId);
        userAttendee.setPartStat(ParticipationStatus.NEEDS_ACTION);
        CalendarObjectResource preparedResource = prepareResource(resource, userAttendee);
        return new PutPerformer(this).perform(preparedResource, shallReplace(preparedResource));
    }

    private CalendarObjectResource prepareResource(CalendarObjectResource incomingResource, Attendee attendeeToAdd) throws OXException {
        List<Event> events = incomingResource.getEvents();
        if (null == events || events.isEmpty()) {
            return incomingResource;
        }
        List<Event> preparedEvents = new ArrayList<Event>(events.size());
        for (Event event : events) {
            preparedEvents.add(prepareEvent(event, attendeeToAdd));
        }
        return new IncomingCalendarObjectResource(preparedEvents);
    }

    private Event prepareEvent(Event incomingEvent, Attendee attendeeToAdd) throws OXException {
        boolean changed = false;
        /*
         * ensure organizer does not refer to an internal entity
         */
        Organizer organizer = incomingEvent.getOrganizer();
        if (null != organizer && isInternal(organizer, CalendarUserType.INDIVIDUAL) && false == matches(organizer, calendarUser)) {
            session.addWarning(CalendarExceptionCodes.INVALID_DATA.create(
                "Transforming internal " + organizer + " to external entity in incoming " + incomingEvent));
            organizer = asExternalOrganizer(organizer);
            changed = true;
        }
        /*
         * check that only the calendar user is 'resolved' as internal entity, add passed attendee if applicable
         */
        List<Attendee> attendees = incomingEvent.getAttendees();
        if (null == attendees && null != attendeeToAdd) {
            attendees = new ArrayList<Attendee>();
        }
        if (null != attendees) {
            boolean changedAttendees = false;
            List<Attendee> preparedAttendees = new ArrayList<Attendee>();
            if (null != attendeeToAdd) {
                preparedAttendees.add(attendeeToAdd);
                changedAttendees = true;
            }
            for (Attendee attendee : attendees) {
                if (isInternal(attendee) && false == matches(attendee, calendarUser)) {
                    session.addWarning(CalendarExceptionCodes.INVALID_DATA.create(
                        "Transforming internal " + attendee + " to external entity in incoming " + incomingEvent));
                    preparedAttendees.add(asExternal(attendee, (AttendeeField[]) null));
                    changedAttendees = true;
                } else {
                    preparedAttendees.add(attendee);
                }
            }
            if (changedAttendees) {
                attendees = preparedAttendees;
                changed = true;
            }
        }
        if (false == changed) {
            return incomingEvent;
        }
        /*
         * initialize event copy with prepared data
         */
        Event preparedEvent = EventMapper.getInstance().copy(incomingEvent, null, (EventField[]) null);
        preparedEvent.setOrganizer(organizer);
        preparedEvent.setAttendees(attendees);
        return preparedEvent;
    }

    /*
     * ============================== HELPERS ==============================
     */

    /**
     * Performs checks towards the resource
     *
     * @param message The incoming message
     * @param resource The resource to check
     * @throws OXException In case a check failed
     */
    private void check(IncomingSchedulingMessage message, CalendarObjectResource resource) throws OXException {
        if (SchedulingSource.API.equals(source)) {
            return;
        }
        /*
         * Check if originator is allowed to perform the change
         */
        ResolvePerformer resolvePerformer = new ResolvePerformer(session, storage);
        EventID eventID = resolvePerformer.resolveByUid(resource.getUid(), calendarUserId);
        if (null != eventID) {
            Event originalEvent = loadEventData(eventID.getObjectID());
            validateOrganizer(folder, originalEvent, resource.getFirstEvent(), message.getSchedulingObject().getOriginator());
        }
    }

    /**
     * Gets a value indicating whether non-transmitted events shall be replaced or not
     *
     * @param resource The transmitted resource
     * @return <code>true</code> if events shall be replaced, <code>false</code> if not
     */
    private boolean shallReplace(CalendarObjectResource resource) {
        return null != resource.getSeriesMaster();
    }
}
