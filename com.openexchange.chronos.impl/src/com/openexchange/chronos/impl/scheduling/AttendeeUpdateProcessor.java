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

import static com.openexchange.chronos.common.CalendarUtils.getEventIDs;
import static com.openexchange.chronos.common.CalendarUtils.isResourceCalendarFolderId;
import static com.openexchange.chronos.impl.scheduling.SchedulingUtils.usesOrganizerCopy;
import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.InternalCalendarResult;
import com.openexchange.chronos.impl.performer.AbstractUpdatePerformer;
import com.openexchange.chronos.impl.performer.ResolvePerformer;
import com.openexchange.chronos.impl.performer.UpdateAttendeePerformer;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.service.CalendarResult;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.CreateResult;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.UpdateResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link AttendeeUpdateProcessor} - Updates the status for an participant on events that
 * were created or updated before.
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class AttendeeUpdateProcessor extends AbstractUpdatePerformer {

    protected final SchedulingMethod method;

    /**
     * Initializes a new {@link UpdateAttendeePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    public AttendeeUpdateProcessor(CalendarStorage storage, CalendarSession session, CalendarFolder folder, SchedulingMethod method) throws OXException {
        super(storage, session, folder);
        this.method = method;
    }

    /**
     * Initializes a new {@link AttendeeUpdateProcessor}.
     * 
     * @param perfomer The existing performer
     * @param method The method being processed
     */
    public AttendeeUpdateProcessor(AbstractUpdatePerformer perfomer, SchedulingMethod method) {
        super(perfomer);
        this.method = method;
    }

    /**
     * Updates the attendee status
     * 
     * @param message The incoming message
     * @param attendee The attendee to gain information for the update from
     * @return The calendar result
     * @throws OXException In case update isn't successful
     */
    public InternalCalendarResult process(IncomingSchedulingMessage message, Attendee attendee) throws OXException {
        if (null == attendee) {
            return resultTracker.getResult();
        }
        return process(message, attendee.getPartStat(), attendee.getComment());
    }

    /**
     * Updates the attendee status
     *
     * @param message The incoming message
     * @param partStat The status to set
     * @param comment optional comment for the attendee to set
     * @return The calendar result
     * @throws OXException In case update isn't successful
     */
    public InternalCalendarResult process(IncomingSchedulingMessage message, ParticipationStatus partStat, String comment) throws OXException {
        /*
         * perform immediate attendee updates when operating on the organizer copy of known calendar object resources from internal notification mails
         */
        if (usesOrganizerCopy(session, storage, message)) {
            List<Event> events = message.getResource().getEvents();
            if (isNullOrEmpty(events)) {
                return resultTracker.getResult();
            }
            Attendee update = prepareAttendeeUpdate(partStat, comment);
            List<EventID> eventIDs = resolveEventIDs(message.getResource());
            return new UpdateAttendeePerformer(this).perform(eventIDs, update, null);
        }
        /*
         * Check if applicable
         */
        InternalCalendarResult result = resultTracker.getResult();
        if (false == SchedulingMethod.REQUEST.equals(method) && false == SchedulingMethod.ADD.equals(method)) {
            return result;
        }
        if (ParticipationStatus.NEEDS_ACTION.matches(partStat)) {
            return result;
        }
        CalendarResult userizedResult = result.getUserizedResult();
        if (isNullOrEmpty(userizedResult.getCreations()) && isNullOrEmpty(userizedResult.getUpdates())) {
            return result;
        }
        /*
         * Update
         */
        return update(message, partStat, comment, result);
    }

    /**
     * Resolves the events in the given calendar object resource to their internal identifiers, by looking up their <code>UID</code>- and
     * <code>RECURRENCE-ID</code> values in the storage.
     * 
     * @param resource The calendar object resource to resolve
     * @return The resolved identifiers of the events or occurrences
     * @throws OXException {@link CalendarExceptionCodes#EVENT_NOT_FOUND} or {@link CalendarExceptionCodes#EVENT_RECURRENCE_NOT_FOUND}
     */
    private List<EventID> resolveEventIDs(CalendarObjectResource resource) throws OXException {
        List<Event> events = resource.getEvents();
        if (isNullOrEmpty(events)) {
            return Collections.emptyList();
        }
        ResolvePerformer resolvePerformer = new ResolvePerformer(session, storage);
        List<EventID> eventIDs = new ArrayList<EventID>(events.size());
        for (Event event : events) {
            EventID eventID = resolvePerformer.resolveByUid(event.getUid(), event.getRecurrenceId(), calendarUserId);
            if (null == eventID) {
                throw null == event.getRecurrenceId() ? CalendarExceptionCodes.EVENT_NOT_FOUND.create(event.getUid()) :
                    CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(event.getUid(), event.getRecurrenceId());
            }
            eventIDs.add(eventID);
        }
        return eventIDs;
    }

    /*
     * ============================== HELPERS ==============================
     */

    private InternalCalendarResult update(IncomingSchedulingMessage message, ParticipationStatus partStat, String comment, InternalCalendarResult result) throws OXException {
        CalendarResult userizedResult = result.getUserizedResult();
        List<Event> events = new LinkedList<>();
        Attendee update = prepareAttendeeUpdate(partStat, comment);
        /*
         * Gather relevant events
         */
        List<UpdateResult> updates = userizedResult.getUpdates();
        if (false == isNullOrEmpty(updates)) {
            events.addAll(updates.stream().map(u -> u.getUpdate()).collect(Collectors.toList()));
        }
        List<CreateResult> creations = userizedResult.getCreations();
        if (false == isNullOrEmpty(creations)) {
            events.addAll(creations.stream().map(c -> c.getCreatedEvent()).collect(Collectors.toList()));
        }
        if (isNullOrEmpty(events)) {
            return result;
        }
        /*
         * Filter for relevant events and update status
         */
        events = filterMatching(message.getResource(), CalendarUtils.sortSeriesMasterFirst(events));
        return new UpdateAttendeePerformer(this).perform(getEventIDs(events), update, null);
    }

    private List<Event> filterMatching(CalendarObjectResource resource, List<Event> events) {
        for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
            Event event = iterator.next();
            if (CalendarUtils.isSeriesException(event)) {
                if (null == resource.getChangeException(event.getRecurrenceId())) {
                    iterator.remove();
                }
            } else if (CalendarUtils.isSeriesMaster(event)) {
                if (null == resource.getSeriesMaster()) {
                    iterator.remove();
                }
            }
        }
        return events;
    }

    /**
     * Prepares an attendee update for the current calendar user
     *
     * @param partStat The participant status to set
     * @param comment The optional comment to set
     * @return The attendee for the update
     */
    private Attendee prepareAttendeeUpdate(ParticipationStatus partStat, String comment) {
        Attendee update = new Attendee();
        update.setCuType(isResourceCalendarFolderId(folder.getId()) ? CalendarUserType.RESOURCE : CalendarUserType.INDIVIDUAL);
        update.setEntity(calendarUserId);
        update.setPartStat(partStat);
        update.setComment(comment);
        //        update.setTimestamp(timestamp.getTime());
        return update;
    }

}
