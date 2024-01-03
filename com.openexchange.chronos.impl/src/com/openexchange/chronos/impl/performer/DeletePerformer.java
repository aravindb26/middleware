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

package com.openexchange.chronos.impl.performer;

import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getUpdatedResource;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.sortSeriesMasterFirst;
import static com.openexchange.chronos.impl.Check.requireUpToDateTimestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.common.EventOccurrence;
import com.openexchange.chronos.common.mapping.AttendeeEventUpdate;
import com.openexchange.chronos.common.mapping.DefaultEventUpdate;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.InternalCalendarResult;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link DeletePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class DeletePerformer extends AbstractUpdatePerformer {

    /**
     * Initializes a new {@link DeletePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    public DeletePerformer(CalendarStorage storage, CalendarSession session, CalendarFolder folder) throws OXException {
        super(storage, session, folder);
    }

    /**
     * Initializes a new {@link DeletePerformer}, taking over the settings from another update performer.
     *
     * @param updatePerformer The update performer to take over the settings from
     */
    protected DeletePerformer(AbstractUpdatePerformer updatePerformer) {
        super(updatePerformer);
    }

    /**
     * Performs the deletion of an event.
     *
     * @param objectId The identifier of the event to delete
     * @param recurrenceId The recurrence identifier of the occurrence to delete, or <code>null</code> if no specific occurrence is targeted
     * @param clientTimestamp The client timestamp to catch concurrent modifications
     * @return The result
     */
    public InternalCalendarResult perform(String objectId, RecurrenceId recurrenceId, long clientTimestamp) throws OXException {
        /*
         * load plain original event data
         */
        Event originalEvent = loadEventData(objectId);
        /*
         * check current session user's permissions
         */
        Check.eventIsInFolder(originalEvent, folder);
        requireUpToDateTimestamp(originalEvent, clientTimestamp);
        if (null == recurrenceId) {
            deleteEvent(originalEvent);
        } else {
            deleteRecurrence(originalEvent, recurrenceId);
        }
        return resultTracker.getResult();
    }

    /**
     * Deletes a single event.
     * <p/>
     * Besides the deletion through {@link AbstractUpdatePerformer#deleteException} or {@link AbstractUpdatePerformer#delete}, associated
     * scheduling messages are tracked in the performer's {@link ResultTracker} instance implicitly.
     *
     * @param originalEvent The original event to delete
     */
    private void deleteEvent(Event originalEvent) throws OXException {
        Event originalSeriesMaster = isSeriesException(originalEvent) ? optEventData(originalEvent.getSeriesId()) : null;
        if (deleteRemovesEvent(originalEvent, originalEvent.getRecurrenceId())) {
            /*
             * deletion of not group-scheduled event / by organizer / last user attendee
             */
            List<Event> deletedEvents = deleteEvent(originalEvent, originalSeriesMaster);
            schedulingHelper.trackDeletion(getCalendarObjectResourceSafe(deletedEvents), originalSeriesMaster, null);
        } else {
            /*
             * deletion as one of the attendees
             */
            Attendee userAttendee = find(originalEvent.getAttendees(), calendarUserId);
            List<EventUpdate> attendeeEventUpdates = deleteEvent(originalEvent, originalSeriesMaster, userAttendee);
            schedulingHelper.trackReply(userAttendee, getUpdatedCalendarObjectResourceSafe(attendeeEventUpdates), originalSeriesMaster, attendeeEventUpdates);
        }
    }
    
    /**
     * Deletes a single event or change exception after checking necessary permissions. The plain results are tracked internally, however, no
     * scheduling messages are prepared implicitly.
     * 
     * @param originalEvent The original event to delete
     * @param originalSeriesMaster The original series master event, or <code>null</code> if not applicable
     * @return The deleted events
     */
    protected List<Event> deleteEvent(Event originalEvent, Event originalSeriesMaster) throws OXException {
        requireDeletePermissions(originalEvent);
        if (isSeriesException(originalEvent)) {
            return deleteException(originalSeriesMaster, originalEvent);
        }
        return delete(originalEvent);
    }
    
    /**
     * Deletes an attendee from a single event or change exception after checking necessary permissions. The plain results are tracked
     * internally, however, no scheduling messages are prepared implicitly.
     * 
     * @param originalEvent The original event to delete the attendee from
     * @param originalSeriesMaster The original series master event, or <code>null</code> if not applicable
     * @param userAttendee The user attendee to delete
     * @return The performed attendee event updates
     */
    protected List<EventUpdate> deleteEvent(Event originalEvent, Event originalSeriesMaster, Attendee userAttendee) throws OXException {
        if (null == userAttendee) {
            throw CalendarExceptionCodes.NO_DELETE_PERMISSION.create(folder.getId());
        }
        requireDeletePermissions(originalEvent, userAttendee);
        if (isSeriesException(originalEvent)) {
            return deleteException(originalSeriesMaster, originalEvent, userAttendee);
        }
        return delete(originalEvent, userAttendee);
    }
    
    /**
     * Deletes a specific recurrence of a recurring event.
     *
     * @param originalEvent The original exception event, or the targeted series master event
     * @param recurrenceId The recurrence identifier of the occurrence to delete
     */
    private void deleteRecurrence(Event originalEvent, RecurrenceId recurrenceId) throws OXException {
        if (deleteRemovesEvent(originalEvent, recurrenceId)) {
            /*
             * deletion of not group-scheduled event / by organizer / last user attendee
             */
            requireDeletePermissions(originalEvent);
            if (isSeriesMaster(originalEvent)) {
                recurrenceId = Check.recurrenceIdExists(session.getRecurrenceService(), originalEvent, recurrenceId);
                if (null != recurrenceId.getRange()) {
                    /*
                     * delete "this and future" recurrences
                     */
                    Event updatedEvent = deleteFutureRecurrences(originalEvent, recurrenceId, true);
                    CalendarObjectResource updatedResource = getCalendarObjectResourceSafe(updatedEvent, loadExceptionData(updatedEvent, updatedEvent.getChangeExceptionDates()));
                    schedulingHelper.trackUpdate(updatedResource, new DefaultEventUpdate(originalEvent, updatedEvent));
                } else if (contains(originalEvent.getChangeExceptionDates(), recurrenceId)) {
                    /*
                     * deletion of existing change exception
                     */
                    // deleteException(loadExceptionData(originalEvent.getId(), recurrenceID));
                    // TODO: not supported in old stack (attempt fails with APP-0011), so throwing exception as expected by test for now
                    // com.openexchange.ajax.appointment.recurrence.TestsForCreatingChangeExceptions.testShouldFailIfTryingToCreateADeleteExceptionOnTopOfAChangeException())
                    throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create(
                        new Exception("Deletion of existing change exception not supported"), recurrenceId, new DefaultRecurrenceData(originalEvent));
                } else {
                    /*
                     * create new delete exception in master & prepare cancel or reply scheduling message representing the delete operation
                     */
                    Event virtualException = new EventOccurrence(originalEvent, recurrenceId);
                    addDeleteExceptionDate(originalEvent, recurrenceId);
                    schedulingHelper.trackDeletion(new DefaultCalendarObjectResource(virtualException), originalEvent, null);
                }
            } else if (isSeriesException(originalEvent)) {
                /*
                 * delete existing change exception & prepare cancel or reply scheduling message representing the delete operation
                 */
                Event originalSeriesMaster = optEventData(originalEvent.getSeriesId());
                List<Event> deletedEvents = deleteException(originalSeriesMaster, originalEvent);
                schedulingHelper.trackDeletion(new DefaultCalendarObjectResource(deletedEvents), originalSeriesMaster, null);
            } else {
                /*
                 * unsupported, otherwise
                 */
                throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(originalEvent.getId(), String.valueOf(recurrenceId));
            }
        } else {
            /*
             * deletion as one of the attendees
             */
            Attendee userAttendee = find(originalEvent.getAttendees(), calendarUserId);
            if (null == userAttendee) {
                throw CalendarExceptionCodes.NO_DELETE_PERMISSION.create(folder.getId());
            }
            requireDeletePermissions(originalEvent, userAttendee);
            if (isSeriesMaster(originalEvent)) {
                recurrenceId = Check.recurrenceIdExists(session.getRecurrenceService(), originalEvent, recurrenceId);
                if (contains(originalEvent.getChangeExceptionDates(), recurrenceId)) {
                    /*
                     * deletion of existing change exception
                     */
                    // deleteException(loadExceptionData(originalEvent.getId(), recurrenceID), userAttendee);
                    // TODO: not supported in old stack (attempt fails with APP-0011), so throwing exception as expected by test for now
                    // com.openexchange.ajax.appointment.recurrence.TestsForCreatingChangeExceptions.testShouldFailIfTryingToCreateADeleteExceptionOnTopOfAChangeException())
                    throw CalendarExceptionCodes.INVALID_RECURRENCE_ID.create(
                        new Exception("Deletion of existing change exception not supported"), recurrenceId, originalEvent.getRecurrenceRule());
                } else if (null != recurrenceId.getRange()) {
                    throw CalendarExceptionCodes.FORBIDDEN_CHANGE.create(originalEvent.getId(), originalEvent.getRecurrenceRule());
                } else {
                    /*
                     * creation of new delete exception
                     */
                    List<EventUpdate> attendeeEventUpdates = deleteFromRecurrence(originalEvent, recurrenceId, userAttendee);
                    schedulingHelper.trackReply(userAttendee, getUpdatedResource(attendeeEventUpdates), originalEvent, attendeeEventUpdates);
                }
            } else if (isSeriesException(originalEvent)) {
                /*
                 * deletion of existing change exception
                 */
                Event originalSeriesMaster = optEventData(originalEvent.getSeriesId());
                List<EventUpdate> attendeeEventUpdates = deleteException(originalSeriesMaster, originalEvent, userAttendee);
                schedulingHelper.trackReply(userAttendee, getUpdatedResource(attendeeEventUpdates), originalSeriesMaster, attendeeEventUpdates);
            } else {
                /*
                 * unsupported, otherwise
                 */
                throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(originalEvent.getId(), String.valueOf(recurrenceId));
            }
        }
    }

    /**
     * Deletes a specific internal user attendee from an existing change exception. Besides the removal of the attendee via
     * {@link #delete(Event, Attendee)}, this also includes 'touching' the master event's last-modification timestamp.
     *
     * @param originalSeriesMaster The original series master event, or <code>null</code> if not available
     * @param originalExceptionEvent The original exception event
     * @param originalAttendee The original attendee to delete
     * @return A list containing the performed event update as {@link AttendeeEventUpdate}
     */
    private List<EventUpdate> deleteException(Event originalSeriesMaster, Event originalExceptionEvent, Attendee originalAttendee) throws OXException {
        /*
         * delete the attendee in the exception
         */
        List<EventUpdate> attendeeEventUpdates = delete(originalExceptionEvent, originalAttendee);
        /*
         * 'touch' the series master accordingly & track result
         */
        if (null != originalSeriesMaster) {
            touch(originalSeriesMaster);
            resultTracker.trackUpdate(originalSeriesMaster, loadEventData(originalSeriesMaster.getId()));
        }
        return attendeeEventUpdates;
    }

    /**
     * Constructs a calendar object resource in a <i>safe</i> way, omitting any change exception events that have a different organizer
     * or UID value than the series master event implicitly.
     * <p/>
     * Appropriate warnings are generated for malformed change exception data.
     * 
     * @param seriesMaster The series master event
     * @param changeExceptions The change exceptions
     * @return The calendar object resource
     */
    private CalendarObjectResource getCalendarObjectResourceSafe(Event seriesMaster, List<Event> changeExceptions) {
        try {
            return new DefaultCalendarObjectResource(seriesMaster, changeExceptions);
        } catch (IllegalArgumentException e) {
            LOG.warn("Error creating calendar object resource, trying to omit malformed exception data.", e);
            String masterUid = seriesMaster.getUid();
            Organizer masterOrganizer = seriesMaster.getOrganizer();
            List<Event> consideredExceptions = new ArrayList<Event>();
            if (null != changeExceptions) {
                for (Event changeException : changeExceptions) {
                    if (false == matches(masterOrganizer, changeException.getOrganizer())) {
                        session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create(
                            "Omitting change exception {} with organizer different from {} in object resource", changeException, masterOrganizer));
                    } else if (false == Objects.equals(masterUid, changeException.getUid())) {
                        session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create(
                            "Omitting change exception {} with UID different from {} in object resource", changeException, masterUid));
                    } else {
                        consideredExceptions.add(changeException);
                    }
                }
            }
            return new DefaultCalendarObjectResource(seriesMaster, consideredExceptions);
        }
    }

    /**
     * Constructs a calendar object resource consisting of the <i>updated</i> events from the supplied event update collection in a
     * <i>safe</i> way, omitting any instances that have a different organizer or UID value than the most significant event in the list
     * implicitly.
     * <p/>
     * Appropriate warnings are generated for malformed change exception data.
     * 
     * @param eventUpdates The event updates to construct the calendar object resource from
     * @return The calendar object resource
     */
    private CalendarObjectResource getUpdatedCalendarObjectResourceSafe(List<? extends EventUpdate> eventUpdates) {
        List<Event> events = new ArrayList<Event>();
        for (EventUpdate eventUpdate : eventUpdates) {
            events.add(eventUpdate.getUpdate());
        }
        return getCalendarObjectResourceSafe(events);
    }

    /**
     * Constructs a calendar object resource in a <i>safe</i> way, omitting any instances that have a different organizer
     * or UID value than the most significant event in the list implicitly.
     * <p/>
     * Appropriate warnings are generated for malformed change exception data.
     * 
     * @param events The events to construct the calendar object resoource from
     * @return The calendar object resource
     */
    private CalendarObjectResource getCalendarObjectResourceSafe(List<Event> events) {
        try {
            return new DefaultCalendarObjectResource(events);
        } catch (IllegalArgumentException e) {
            List<Event> sortedEvents = sortSeriesMasterFirst(events);
            if (0 < sortedEvents.size()) {
                LOG.warn("Error creating calendar object resource, trying to omit malformed event data.", e);
                String firstUid = sortedEvents.get(0).getUid();
                Organizer firstOrganizer = sortedEvents.get(0).getOrganizer();
                List<Event> consideredEvents = new ArrayList<Event>();
                consideredEvents.add(sortedEvents.get(0));
                if (1 < sortedEvents.size()) {
                    for (int i = 1; i < sortedEvents.size(); i++) {
                        Event event = sortedEvents.get(i);
                        if (false == matches(firstOrganizer, event.getOrganizer())) {
                            session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create(
                                "Omitting event {} with organizer different from {} in object resource", event, firstOrganizer));
                        } else if (false == Objects.equals(firstUid, event.getUid())) {
                            session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create(
                                "Omitting event {} with UID different from {} in object resource", event, firstUid));
                        } else {
                            consideredEvents.add(event);
                        }
                    }
                }
                return new DefaultCalendarObjectResource(consideredEvents);
            }
            throw e;
            
        }
    }

}
