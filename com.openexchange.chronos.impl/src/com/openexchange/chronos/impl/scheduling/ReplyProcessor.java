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

import static com.openexchange.chronos.common.CalendarUtils.collectAttendees;
import static com.openexchange.chronos.common.CalendarUtils.getSimpleAttendeeUpdates;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.InternalCalendarResult;
import com.openexchange.chronos.impl.InternalEventUpdate;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.impl.performer.AbstractUpdatePerformer;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.SchedulingSource;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.common.AbstractSimpleCollectionUpdate;
import com.openexchange.groupware.tools.mappings.common.DefaultItemUpdate;
import com.openexchange.groupware.tools.mappings.common.ItemUpdate;

/**
 * {@link ReplyProcessor} - Handles incoming <code>REPLY</code> message by external calendar users and tries to apply
 * the changes transmitted in the message.
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 * @see <a href="https://tools.ietf.org/html/rfc5546#section-3.2.3">RFC5546 Section 3.2.3</a>
 */
public class ReplyProcessor extends AbstractUpdatePerformer {

    private static final AttendeeField[] UPDATE_FIELDS = { AttendeeField.COMMENT, AttendeeField.EXTENDED_PARAMETERS, AttendeeField.PARTSTAT, AttendeeField.SENT_BY };

    private static final AttendeeField[] ADDITIONAL_FIELDS = { AttendeeField.CN, AttendeeField.EMAIL, AttendeeField.URI, AttendeeField.CU_TYPE };

    private final SchedulingSource source;

    /**
     * Initializes a new {@link AbstractUpdatePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     * @param source The source from which the scheduling has been triggered
     * @throws OXException If initialization fails
     */
    public ReplyProcessor(CalendarStorage storage, CalendarSession session, CalendarFolder folder, SchedulingSource source) throws OXException {
        super(storage, session, folder);
        this.source = source;

    }

    /**
     * Performs an update on an event by either
     * <li> taking over the attendees new participant status</li>
     * or
     * <li>adding an unknown calendar user to the event. This will trigger new messages to inform all other attendees.</li>
     *
     * @param message The {@link IncomingSchedulingMessage}
     * @return An {@link InternalCalendarResult} containing the changes that has been performed
     * @throws OXException In case data is invalid, outdated or permissions are missing
     */
    public InternalCalendarResult process(IncomingSchedulingMessage message) throws OXException {
        Event firstEvent = message.getResource().getFirstEvent();
        CalendarUser originator = message.getSchedulingObject().getOriginator();
        Attendee replyingAttendee = getReplyingAttendee(firstEvent, originator);
        EventID eventID = Utils.resolveEventId(session, storage, firstEvent.getUid(), firstEvent.getRecurrenceId(), message.getTargetUser());
        Event originalEvent = loadEventData(eventID.getObjectID());

        Check.eventIsVisible(folder, originalEvent);
        Check.eventIsInFolder(originalEvent, folder);

        /*
         * Lookup if the originator is known, or we must handle a 'party-crasher'
         */
        if (null == CalendarUtils.find(originalEvent.getAttendees(), replyingAttendee)) {
            /*
             * Do not support auto adding of party crashers, allow only as user triggered
             */
            if (false == SchedulingSource.API.equals(source)) {
                LOG.info("Found a \"party-crasher\". Stop auto-processing.");
                return resultTracker.getResult();
            }

            LOG.debug("Adding \"party-crasher\" to event {}", originalEvent.getId());
            addPartyCrasher(message, originalEvent, replyingAttendee);
        } else {
            /*
             * Update the attendee status, update only relevant fields
             */
            updateAttendee(message, originator, replyingAttendee, originalEvent);
        }
        return resultTracker.getResult();
    }

    private void updateAttendee(IncomingSchedulingMessage message, CalendarUser originator, Attendee replyingAttendee, Event originalEvent) throws OXException {
        if (CalendarUtils.isSeriesEvent(originalEvent)) {
            if (CalendarUtils.isSeriesMaster(originalEvent) && null != message.getResource().getSeriesMaster()) {
                /*
                 * Update series master, afterwards exceptions, too
                 */
                Optional<DefaultItemUpdate<Attendee, AttendeeField>> attendeeUpdate = prepareAttendeeUpdate(originalEvent, message.getResource().getSeriesMaster(), replyingAttendee);
                if (attendeeUpdate.isPresent()) {
                    Event updatedMasterEvent = updateAttendee(originalEvent, attendeeUpdate.get());
                    resultTracker.trackUpdate(originalEvent, updatedMasterEvent);
                }
                List<Event> originalChangeExceptions = loadExceptionData(originalEvent);
                for (Event originalChangeException : originalChangeExceptions) {
                    /*
                     * Lookup update for specific exception
                     */
                    Event updatedEventData = message.getResource().getChangeException(originalChangeException.getRecurrenceId());
                    if (null != updatedEventData) {
                        /*
                         * Apply data transmitted for this exception
                         */
                        attendeeUpdate = prepareAttendeeUpdate(originalChangeException, updatedEventData, getReplyingAttendee(updatedEventData, originator));
                        if (attendeeUpdate.isPresent()) {
                            Event updatedEventException = updateAttendee(originalChangeException, attendeeUpdate.get());
                            resultTracker.trackUpdate(originalChangeException, updatedEventException);
                        }
                    }
                }
                /*
                 * Create new change exceptions for unknown exception data
                 */
                RecurrenceService recurrenceService = session.getRecurrenceService();
                List<RecurrenceId> recurrenceIds = originalChangeExceptions.stream().filter(e -> null != e.getRecurrenceId()).map(e -> e.getRecurrenceId()).collect(Collectors.toList());
                for (Event event : message.getResource().getChangeExceptions()) {
                    if (null == CalendarUtils.find(recurrenceIds, event.getRecurrenceId())) {
                        Check.recurrenceIdExists(recurrenceService, originalEvent, event.getRecurrenceId());
                        createNewChangeException(originalEvent, event.getRecurrenceId(), prepareAttendee(event, replyingAttendee));
                    }
                }
            } else {
                /*
                 * Update specific occurrence
                 */
                Event originalSeriesMaster = loadEventData(originalEvent.getSeriesId());
                RecurrenceService recurrenceService = session.getRecurrenceService();
                List<RecurrenceId> exceptionData = loadExceptionData(originalSeriesMaster).stream().filter(e -> null != e.getRecurrenceId()).map(e -> e.getRecurrenceId()).collect(Collectors.toList());
                for (Event updatedEventData : message.getResource().getEvents()) {
                    RecurrenceId recurrenceId = updatedEventData.getRecurrenceId();
                    if (null == recurrenceId) {
                        /*
                         * Probably received an reply to an event that is a series meanwhile (which triggers a rescheduling and a new reply message). Abort.
                         */
                        throw CalendarExceptionCodes.INVALID_DATA.create(EventField.RECURRENCE_ID, "Unable to find the correct event to update.");
                    }
                    if (null == CalendarUtils.find(exceptionData, recurrenceId)) {
                        /*
                         * Create new change exceptions for unknown exception data
                         */
                        Check.recurrenceIdExists(recurrenceService, originalSeriesMaster, recurrenceId);
                        createNewChangeException(originalSeriesMaster, recurrenceId, prepareAttendee(updatedEventData, replyingAttendee));
                    } else {
                        /*
                         * Update known occurrence
                         */
                        Event originalChangeException = loadExceptionData(originalSeriesMaster, recurrenceId);
                        Optional<DefaultItemUpdate<Attendee, AttendeeField>> attendeeUpdate = prepareAttendeeUpdate(originalChangeException, updatedEventData, getReplyingAttendee(updatedEventData, originator));
                        if (attendeeUpdate.isPresent()) {
                            Event updatedEvent = updateAttendee(originalChangeException, attendeeUpdate.get());
                            resultTracker.trackUpdate(updatedEventData, updatedEvent);
                        }
                    }
                }
            }

        } else {
            /*
             * Update event
             */
            Optional<DefaultItemUpdate<Attendee, AttendeeField>> attendeeUpdate = prepareAttendeeUpdate(originalEvent, message.getResource().getFirstEvent(), replyingAttendee);
            if (attendeeUpdate.isPresent()) {
                Event updatedEvent = updateAttendee(originalEvent, attendeeUpdate.get());
                resultTracker.trackUpdate(originalEvent, updatedEvent);
            }
        }
    }

    /**
     * Prepares an attendee update by applying new data for an existing attendee by updating fields {@link #UPDATE_FIELDS}
     *
     * @param message The incoming message
     * @param originalEvent The original event to get the attendee data from
     * @param updatedEventData The updated event data as transmitted by the client
     * @param replyingAttendee The attendee replying to copy the new data from
     * @return An {@link Optional} containing the attendee update with new data, or {@link Optional#empty()} if there is nothing to update
     * @throws OXException
     */
    private Optional<DefaultItemUpdate<Attendee, AttendeeField>> prepareAttendeeUpdate(Event originalEvent, Event updatedEventData, Attendee replyingAttendee) throws OXException {
        Attendee originalAttendee = CalendarUtils.find(originalEvent.getAttendees(), replyingAttendee);
        if (null == originalAttendee) {
            LOG.trace("Can't find attendee {} to update in event {}.", replyingAttendee, originalEvent);
            return Optional.empty();
        }
        /*
         * Prepare copy, set timestamp and sequence based on incoming event data
         */
        Attendee update = prepareAttendee(updatedEventData, replyingAttendee);
        update = AttendeeMapper.getInstance().copy(replyingAttendee, update, UPDATE_FIELDS);
        update = AttendeeMapper.getInstance().copy(originalAttendee, update, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
        if (update.getTimestamp() < 0) {
            update.setTimestamp(replyingAttendee.getTimestamp());
        }
        if (update.getSequence() < 0) {
            update.setSequence(replyingAttendee.getSequence());
        }
        Check.requireUpToDateTimestamp(originalAttendee, update);

        DefaultItemUpdate<Attendee, AttendeeField> attendeeUpdate = new DefaultItemUpdate<Attendee, AttendeeField>(AttendeeMapper.getInstance(), originalAttendee, update);
        if (attendeeUpdate.isEmpty()) {
            LOG.trace("No data to update for event {}.", originalEvent);
            return Optional.empty();
        }
        return Optional.of(attendeeUpdate);
    }

    /**
     * Prepares an attendee by applying data from the given event to the attendee
     *
     * @param updatedEventData The updated event data as transmitted by the client
     * @param replyingAttendee The attendee replying to copy the new data from
     * @return The updated attendee
     * @throws OXException In case copy fails
     */
    private Attendee prepareAttendee(Event updatedEventData, Attendee replyingAttendee) throws OXException {
        if (null == updatedEventData) {
            return replyingAttendee;
        }
        /*
         * Prepare copy, set timestamp and sequence based on incoming event data
         */
        Attendee update = AttendeeMapper.getInstance().copy(replyingAttendee, null, (AttendeeField[]) null);
        update.setTimestamp(updatedEventData.getDtStamp());
        update.setSequence(updatedEventData.getSequence());
        return update;
    }

    /**
     * Updates an attendee in the given event
     *
     * @param originalEvent The existing event to update
     * @param attendeeUpdate The attendee to update
     * @return The updated event
     * @throws OXException If updating fails
     */
    private Event updateAttendee(Event originalEvent, DefaultItemUpdate<Attendee, AttendeeField> attendeeUpdate) throws OXException {
        requireWritePermissions(originalEvent, attendeeUpdate.getUpdate(), false);
        storage.getAttendeeStorage().updateAttendees(originalEvent.getId(), Collections.singletonList(attendeeUpdate.getUpdate()));
        touch(originalEvent);
        Event updatedEvent = loadEventData(originalEvent.getId());
        resultTracker.trackUpdate(originalEvent, updatedEvent);
        return updatedEvent;
    }

    /**
     * Creates a new change exception for the given series and adds the given attendee to the attendee list
     *
     * @param message The incoming message
     * @param originalSeriesMaster The series master
     * @param recurrenceId The {@link RecurrenceId} to create the exception on
     * @param attendee The attendee to add
     * @return The created change exception
     * @throws OXException
     */
    private Event createNewChangeException(Event originalSeriesMaster, RecurrenceId recurrenceId, Attendee attendee) throws OXException {
        /*
         * Add new change exceptions
         */
        Map<Integer, List<Alarm>> seriesMasterAlarms = storage.getAlarmStorage().loadAlarms(originalSeriesMaster);
        Event newExceptionEvent = prepareException(originalSeriesMaster, recurrenceId);
        Map<Integer, List<Alarm>> newExceptionAlarms = prepareExceptionAlarms(seriesMasterAlarms);
        Check.quotaNotExceeded(storage, session);
        storage.getEventStorage().insertEvent(newExceptionEvent);
        storage.getAttendeeStorage().insertAttendees(newExceptionEvent.getId(), originalSeriesMaster.getAttendees());
        storage.getAttachmentStorage().insertAttachments(session.getSession(), folder.getId(), newExceptionEvent.getId(), originalSeriesMaster.getAttachments());
        storage.getConferenceStorage().insertConferences(newExceptionEvent.getId(), prepareConferences(originalSeriesMaster.getConferences()));
        insertAlarms(newExceptionEvent, newExceptionAlarms, true);
        newExceptionEvent = loadEventData(newExceptionEvent.getId());
        resultTracker.trackCreation(newExceptionEvent, originalSeriesMaster);
        /*
         * perform the attendee update & track results
         */
        resultTracker.rememberOriginalEvent(newExceptionEvent);
        Optional<DefaultItemUpdate<Attendee, AttendeeField>> attendeeUpdate = prepareAttendeeUpdate(newExceptionEvent, null, attendee);
        if (attendeeUpdate.isPresent()) {
            storage.getAttendeeStorage().updateAttendee(newExceptionEvent.getId(), attendeeUpdate.get().getUpdate());
        }
        Event updatedExceptionEvent = loadEventData(newExceptionEvent.getId());
        resultTracker.trackUpdate(newExceptionEvent, updatedExceptionEvent);
        /*
         * add change exception date to series master & track results
         */
        resultTracker.rememberOriginalEvent(originalSeriesMaster);
        addChangeExceptionDate(originalSeriesMaster, recurrenceId, false);
        Event updatedMasterEvent = loadEventData(originalSeriesMaster.getId());
        resultTracker.trackUpdate(originalSeriesMaster, updatedMasterEvent);
        /*
         * reset alarm triggers for series master event and new change exception
         */
        storage.getAlarmTriggerStorage().deleteTriggers(updatedMasterEvent.getId());
        storage.getAlarmTriggerStorage().insertTriggers(updatedMasterEvent, seriesMasterAlarms);
        storage.getAlarmTriggerStorage().deleteTriggers(updatedExceptionEvent.getId());
        storage.getAlarmTriggerStorage().insertTriggers(updatedExceptionEvent, storage.getAlarmStorage().loadAlarms(updatedExceptionEvent));
        return updatedExceptionEvent;
    }

    /**
     * Adds an additional attendee to the events in the incoming message
     *
     * @param message The message to handle
     * @param originalEvent The first existing event received from the incoming message
     * @param replyingAttendee The attendee that replied to the organizer
     * @throws OXException If updating fails
     */
    private void addPartyCrasher(IncomingSchedulingMessage message, Event originalEvent, Attendee replyingAttendee) throws OXException {
        if (CalendarUtils.isSeriesMaster(originalEvent) && null == message.getResource().getFirstEvent().getRecurrenceId()) {
            /*
             * Update whole series, ignore transmitted exceptions
             */
            addPartyCrasher(message.getResource().getFirstEvent(), replyingAttendee, message.getTargetUser());
        } else {
            /*
             * Update specific event(s)
             */
            for (Event event : message.getResource().getEvents()) {
                addPartyCrasher(event, replyingAttendee, message.getTargetUser());
            }
        }
    }

    /**
     * Adds an additional attendee to the existing event
     *
     * @param event The event to add the attendee to
     * @param attendee The attendee to add
     * @param targetUser The target user to perform the update for
     * @return The updated event
     * @throws OXException If updating fails
     */
    private Event addPartyCrasher(Event event, Attendee attendee, int targetUser) throws OXException {
        Event original = loadEventData(Utils.resolveEventId(session, storage, event.getUid(), event.getRecurrenceId(), targetUser).getObjectID());
        Event originalSeriesMasterEvent = CalendarUtils.isSeriesEvent(original) ? loadEventData(original.getSeriesId()) : null;
        requireWritePermissions(original, false);

        /*
         * Prepare update
         */
        Attendee partyCrasher = preparePartyCrasher(original, attendee);
        Event eventData = prepareEvent(original, partyCrasher);
        List<Event> originalChangeExceptions = loadExceptionData(original);
        InternalEventUpdate eventUpdate = new InternalEventUpdate(session, folder, original, originalChangeExceptions, originalSeriesMasterEvent, eventData, timestamp, false, SKIPPED_FIELDS);

        /*
         * Perform update on event and possible exceptions
         */
        Event updatedEvent = insertAttendee(eventUpdate, partyCrasher);
        resultTracker.trackUpdate(original, updatedEvent);
        List<Event> updatedChangeExceptions = new ArrayList<Event>();
        for (ItemUpdate<Event, EventField> updatedException : eventUpdate.getExceptionUpdates().getUpdatedItems()) {
            Event originalChangeException = updatedException.getOriginal();
            InternalEventUpdate exceptionUpdate = new InternalEventUpdate(session, folder, originalChangeException, null, originalSeriesMasterEvent, prepareEvent(originalChangeException, partyCrasher), timestamp, false, SKIPPED_FIELDS);
            Event updatedChangeException = insertAttendee(exceptionUpdate, partyCrasher);
            resultTracker.trackUpdate(original, updatedChangeException);
            updatedChangeExceptions.add(updatedChangeException);
        }

        /*
         * Generate scheduling messages about newly added attendee
         */
        //@formatter:off
        DefaultCalendarObjectResource updatedResource = new DefaultCalendarObjectResource(updatedEvent, updatedChangeExceptions);
        AbstractSimpleCollectionUpdate<Attendee> collectedAttendeeUpdates = getSimpleAttendeeUpdates(
            collectAttendees(eventUpdate.getOriginalResource(), null, (CalendarUserType[]) null),
            collectAttendees(updatedResource, null, (CalendarUserType[]) null));
        schedulingHelper.trackUpdate(updatedResource, CalendarUtils.isSeriesEvent(original) ? loadEventData(original.getSeriesId() ): null, eventUpdate, collectedAttendeeUpdates.getRetainedItems());
        //@formatter:on
        logPerform(eventUpdate);
        return updatedEvent;
    }

    /**
     * Copies the given event and adds the given attendee to the attendees list
     *
     * @param original The original event
     * @param attendee The attendee to add
     * @return An event containing the attendee
     * @throws OXException In case max attendee size is reached
     */
    private Event prepareEvent(Event original, Attendee attendee) throws OXException {
        Event eventData = EventMapper.getInstance().copy(original, null, false, (EventField[]) null);
        eventData.getAttendees().add(attendee);
        getSelfProtection().checkEvent(eventData);
        return eventData;
    }

    /**
     * Generates a party crasher attendee
     * 
     * @param original The original event to add the party crasher to
     * @param partCrasher The party crasher
     * @return Copy of the party crasher with only relevant fields
     * @throws OXException In case mail of the party crasher is invalid
     */
    private Attendee preparePartyCrasher(Event original, Attendee attendee) throws OXException {
        Check.requireValidEMail(attendee);
        Attendee partyCrasher = prepareAttendee(original, attendee);
        AttendeeMapper.getInstance().copy(attendee, partyCrasher, UPDATE_FIELDS);
        AttendeeMapper.getInstance().copy(attendee, partyCrasher, ADDITIONAL_FIELDS);
        return partyCrasher;
    }

    /**
     * Inserts the given attendee to the event
     *
     * @param eventUpdate The event to update, containing the attendee already
     * @param attendee The attendee to add
     * @return The updated event
     * @throws OXException In case updated fails
     */
    private Event insertAttendee(InternalEventUpdate eventUpdate, Attendee attendee) throws OXException {
        List<Attendee> addedAttendees = Collections.singletonList(attendee);
        Check.uidIsUnique(session, storage, eventUpdate.getOriginal(), addedAttendees);
        storage.getEventStorage().updateEvent(eventUpdate.getDelta());
        storage.getAttendeeStorage().insertAttendees(eventUpdate.getOriginal().getId(), addedAttendees);
        return loadEventData(eventUpdate.getOriginal().getId());
    }

    /**
     * Get the attendee that replied
     *
     * @param updatedEventData The event containing updated data
     * @param originator The originator of the message
     * @return The attendee that replied
     * @throws OXException If the attendee can not be found
     * @see <a href="https://tools.ietf.org/html/rfc5546#section-3.2.3">RFC5546 Section 3.2.3</a>
     * @see <a href="https://tools.ietf.org/html/rfc6047#section-3">RFC6047 Section 3</a>
     */
    private Attendee getReplyingAttendee(Event updatedEventData, CalendarUser originator) throws OXException {
        Attendee replyingAttendee = CalendarUtils.find(updatedEventData.getAttendees(), originator);
        if (null == replyingAttendee) {
            LOG.debug("Didn't find originator {} in the attendee list.", originator);
            /*
             * iTIP only allows one attendee in a reply, search for it
             */
            if (SchedulingSource.API.equals(source)) {
                /*
                 * Scheduled or rather allowed via API, use attendee transmitted in the iCal, regardless security considerations
                 */
                if (1 != updatedEventData.getAttendees().size()) {
                    throw CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(I(originator.getEntity()), updatedEventData.getId());
                }
            } else {
                /*
                 * Look up if originator is set in SENT-BY
                 */
                LOG.debug("Didn't find attendee. Searching in SENT-BY field.");
                if (1 != updatedEventData.getAttendees().size() || false == updatedEventData.getAttendees().get(0).containsSentBy()) {
                    throw CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(I(originator.getEntity()), updatedEventData.getId());
                }
                CalendarUser sentBy = updatedEventData.getAttendees().get(0).getSentBy();
                if (false == CalendarUtils.matches(originator, sentBy)) {
                    throw CalendarExceptionCodes.ATTENDEE_NOT_FOUND.create(I(originator.getEntity()), updatedEventData.getId());
                }
            }
            replyingAttendee = updatedEventData.getAttendees().get(0);
        }
        replyingAttendee = AttendeeMapper.getInstance().copy(replyingAttendee, null, (AttendeeField[]) null);
        replyingAttendee.setCuType(CalendarUserType.INDIVIDUAL);
        return replyingAttendee;
    }

}
