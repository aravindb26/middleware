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

import static com.openexchange.chronos.common.AlarmUtils.filterRelativeTriggers;
import static com.openexchange.chronos.common.CalendarUtils.addExtendedProperty;
import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getAlarmIDs;
import static com.openexchange.chronos.common.CalendarUtils.getExceptionDates;
import static com.openexchange.chronos.common.CalendarUtils.getFolderView;
import static com.openexchange.chronos.common.CalendarUtils.getRecurrenceIds;
import static com.openexchange.chronos.common.CalendarUtils.hasAttendeePrivileges;
import static com.openexchange.chronos.common.CalendarUtils.hasExternalOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.initRecurrenceRule;
import static com.openexchange.chronos.common.CalendarUtils.isGroupScheduled;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isLastNonHiddenUserAttendee;
import static com.openexchange.chronos.common.CalendarUtils.isOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.isResourceOrRoom;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesEvent;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.splitExceptionDates;
import static com.openexchange.chronos.impl.Check.classificationAllowsUpdate;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.getCalendarUser;
import static com.openexchange.chronos.impl.Utils.injectRecurrenceData;
import static com.openexchange.chronos.impl.Utils.isBookingDelegate;
import static com.openexchange.chronos.impl.Utils.isHidden;
import static com.openexchange.folderstorage.Permission.DELETE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.DELETE_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.WRITE_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import org.dmfs.rfc5545.DateTime;
import org.dmfs.rfc5545.Duration;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import com.openexchange.chronos.Alarm;
import com.openexchange.chronos.AlarmField;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Conference;
import com.openexchange.chronos.ConferenceField;
import com.openexchange.chronos.DefaultAttendeePrivileges;
import com.openexchange.chronos.DelegatingEvent;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ExtendedProperties;
import com.openexchange.chronos.ExtendedProperty;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.RecurrenceRange;
import com.openexchange.chronos.TimeTransparency;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.UnmodifiableEvent;
import com.openexchange.chronos.common.AlarmPreparator;
import com.openexchange.chronos.common.AlarmUtils;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.common.mapping.AlarmMapper;
import com.openexchange.chronos.common.mapping.AttendeeEventUpdate;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.common.mapping.ConferenceMapper;
import com.openexchange.chronos.common.mapping.DefaultEventUpdate;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Consistency;
import com.openexchange.chronos.impl.InterceptorRegistry;
import com.openexchange.chronos.impl.JSONPrintableEvent;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventUpdate;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.groupware.tools.mappings.common.CollectionUpdate;
import com.openexchange.groupware.tools.mappings.common.ItemUpdate;
import com.openexchange.java.Strings;
import com.openexchange.mime.MimeTypeMap;

/**
 * {@link AbstractUpdatePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public abstract class AbstractUpdatePerformer extends AbstractQueryPerformer {

    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractUpdatePerformer.class);

    /** <i>Meta</i>-fields of events that are always skipped when applying updated event data */
    protected static final EventField[] SKIPPED_FIELDS = { EventField.CREATED, EventField.CREATED_BY, EventField.LAST_MODIFIED, EventField.TIMESTAMP, EventField.MODIFIED_BY, EventField.FLAGS
    };

    protected final CalendarUser calendarUser;
    protected final int calendarUserId;
    protected final CalendarFolder folder;
    protected final Date timestamp;
    protected final ResultTracker resultTracker;
    protected final SchedulingHelper schedulingHelper;
    protected final InterceptorRegistry interceptorRegistry;

    /**
     * Initializes a new {@link AbstractUpdatePerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param folder The calendar folder representing the current view on the events
     */
    protected AbstractUpdatePerformer(CalendarStorage storage, CalendarSession session, CalendarFolder folder) throws OXException {
        super(session, storage);
        this.folder = folder;
        this.calendarUser = getCalendarUser(session, folder);
        this.calendarUserId = calendarUser.getEntity();
        this.timestamp = new Date();
        this.resultTracker = new ResultTracker(storage, session, folder, timestamp.getTime(), getSelfProtection());
        this.schedulingHelper = new SchedulingHelper(Services.getServiceLookup(), session, folder, resultTracker);
        this.interceptorRegistry = new InterceptorRegistry(session, folder);
    }

    /**
     * Initializes a new {@link AbstractUpdatePerformer}, taking over the settings from another update performer.
     *
     * @param updatePerformer The update performer to take over the settings from
     */
    protected AbstractUpdatePerformer(AbstractUpdatePerformer updatePerformer) {
        super(updatePerformer.session, updatePerformer.storage);
        this.folder = updatePerformer.folder;
        this.calendarUser = updatePerformer.calendarUser;
        this.calendarUserId = updatePerformer.calendarUserId;
        this.timestamp = updatePerformer.timestamp;
        this.resultTracker = updatePerformer.resultTracker;
        this.schedulingHelper = updatePerformer.schedulingHelper;
        this.interceptorRegistry = updatePerformer.interceptorRegistry;
    }

    /**
     * Prepares a new change exception for a recurring event series. A new object identifier is acquired from the storage implicitly.
     *
     * @param originalMasterEvent The original master event
     * @param recurrenceId The recurrence identifier
     * @return The prepared exception event
     */
    protected Event prepareException(Event originalMasterEvent, RecurrenceId recurrenceId) throws OXException {
        return prepareException(originalMasterEvent, recurrenceId, storage.getEventStorage().nextId());
    }

    /**
     * Prepares a new change exception for a recurring event series.
     *
     * @param originalMasterEvent The original master event
     * @param recurrenceId The recurrence identifier
     * @param objectId The object identifier to take over for the prepared exception
     * @return The prepared exception event
     */
    protected Event prepareException(Event originalMasterEvent, RecurrenceId recurrenceId, String objectId) throws OXException {
        return prepareException(originalMasterEvent, recurrenceId, objectId, timestamp);
    }

    /**
     * Prepares a list of conferences prior inserting it into the storage by assigning a new internal identifier, as well as checking the
     * conference's URI for validity.
     * <p/>
     * As new internal identifiers are generated, this may both be used for client-supplied data, as well as when taking over data from
     * the series master event for overridden instances.
     *
     * @param conferencesData The new conferences to prepare prior insert
     * @return The prepared conferences
     * @throws OXException {@link CalendarExceptionCodes#INVALID_DATA}
     */
    protected List<Conference> prepareConferences(List<Conference> conferencesData) throws OXException {
        if (null == conferencesData || conferencesData.isEmpty()) {
            return conferencesData;
        }
        List<Conference> conferences = new ArrayList<Conference>(conferencesData.size());
        for (Conference conferenceData : conferencesData) {
            Conference conference = ConferenceMapper.getInstance().copy(conferenceData, null, (ConferenceField[]) null);
            conference.setId(storage.getConferenceStorage().nextId());
            conference.setUri(Check.requireValidURI(conferenceData.getUri(), String.valueOf(EventField.CONFERENCES)));
            conferences.add(conference);
        }
        return conferences;
    }

    /**
     * <i>Touches</i> an event in the storage by setting it's last modification timestamp and modified-by property to the current
     * timestamp and calendar user.
     *
     * @param event The loaded event to <i>touch</i>
     */
    protected void touch(Event event) throws OXException {
        Event eventUpdate = new Event();
        eventUpdate.setId(event.getId());
        Consistency.setModified(session, timestamp, eventUpdate, session.getUserId(), hasExternalOrganizer(event));
        storage.getEventStorage().updateEvent(eventUpdate);
    }

    /**
     * Loads the recurrence identifiers of all stored change exception events for a specific event series from the storage.
     *
     * @param seriesId The identifier of the series to load the exception dates for
     * @return The recurrence identifiers of the change exception events, or an empty set if there are none
     */
    protected SortedSet<RecurrenceId> loadChangeExceptionDates(String seriesId) throws OXException {
        return getRecurrenceIds(storage.getEventStorage().loadExceptions(seriesId, new EventField[] { EventField.RECURRENCE_ID }));
    }

    /**
     * Adds a specific recurrence identifier to the series master's change exception array and updates the series master event in the
     * storage.
     * <p/>
     * <b>Note:</b> In case the recurrence id is already used as <code>EXDATE</code> (i.e. contained in the master's delete exception dates), this
     * delete exception date gets removed implicitly, so that the occurrence is effectively re-instantiated.
     *
     * @param originalMasterEvent The original series master event
     * @param recurrenceID The recurrence identifier of the occurrence to add
     * @param incrementSequence <code>true</code> to increment sequence number of the master event
     */
    protected void addChangeExceptionDate(Event originalMasterEvent, RecurrenceId recurrenceID, boolean incrementSequence) throws OXException {
        SortedSet<RecurrenceId> changeExceptionDates = new TreeSet<RecurrenceId>();
        if (null != originalMasterEvent.getChangeExceptionDates()) {
            changeExceptionDates.addAll(originalMasterEvent.getChangeExceptionDates());
        }
        if (false == changeExceptionDates.add(recurrenceID)) {
            LOG.warn("Change exception date for {} already present.", recurrenceID);
        }
        Event eventUpdate = new Event();
        eventUpdate.setId(originalMasterEvent.getId());
        eventUpdate.setChangeExceptionDates(changeExceptionDates);
        if (incrementSequence) {
            eventUpdate.setSequence(originalMasterEvent.getSequence() + 1);
        }
        Consistency.setModified(session, timestamp, eventUpdate, session.getUserId(), hasExternalOrganizer(originalMasterEvent));
        if (contains(originalMasterEvent.getDeleteExceptionDates(), recurrenceID)) {
            SortedSet<RecurrenceId> deleteExceptionDates = new TreeSet<RecurrenceId>(originalMasterEvent.getDeleteExceptionDates());
            if (deleteExceptionDates.removeIf(d -> d.matches(recurrenceID))) {
                LOG.warn("Re-instantiating delete exception date {}.", recurrenceID);
                eventUpdate.setDeleteExceptionDates(deleteExceptionDates);
            }
        }
        /*
         * trigger calendar interceptors & update event in storage
         */
        interceptorRegistry.triggerInterceptorsOnBeforeUpdate(originalMasterEvent, eventUpdate);
        storage.getEventStorage().updateEvent(eventUpdate);
    }

    /**
     * Deletes a single event from the storage. This can be used for any kind of event, i.e. a single, non-recurring event, an existing
     * exception of an event series, or an event series. For the latter one, any existing event exceptions are deleted as well.
     * <p/>
     * The event's attendees are loaded on demand if not yet present in the passed <code>originalEvent</code> {@code originalEvent}.
     * <p/>
     * The deletion includes:
     * <ul>
     * <li>triggering registered calendar service interceptors</li>
     * <li>insertion of a <i>tombstone</i> record for the original event</li>
     * <li>insertion of <i>tombstone</i> records for the original event's attendees</li>
     * <li>deletion of any alarms associated with the event</li>
     * <li>deletion of any attachments associated with the event</li>
     * <li>deletion of the event</li>
     * <li>deletion of the event's attendees</li>
     * <li>tracking the deletion within the result tracker instance</li>
     * </ul>
     *
     * @param originalEvent The original event to delete
     * @return The deleted event(s), possibly more than one in case overridden instances were deleted along with the series master
     */
    protected List<Event> delete(Event originalEvent) throws OXException {
        /*
         * recursively delete any existing event exceptions
         */
        List<Event> deletedEvents = new ArrayList<Event>();
        if (isSeriesMaster(originalEvent)) {
            for (Event changeException : loadExceptionData(originalEvent)) {
                deletedEvents.addAll(delete(changeException));
            }
        }
        /*
         * trigger calendar interceptors
         */
        interceptorRegistry.triggerInterceptorsOnBeforeDelete(originalEvent);
        /*
         * delete event data from storage
         */
        resultTracker.rememberOriginalEvent(originalEvent);
        String id = originalEvent.getId();
        Event tombstone = storage.getUtilities().getTombstone(originalEvent, timestamp, calendarUser);
        tombstone.setAttendees(storage.getUtilities().getTombstones(originalEvent.getAttendees()));
        storage.getEventStorage().insertEventTombstone(tombstone);
        storage.getAttendeeStorage().insertAttendeeTombstones(id, tombstone.getAttendees());
        storage.getAttachmentStorage().deleteAttachments(session.getSession(), folder.getId(), id, originalEvent.getAttachments());
        storage.getConferenceStorage().deleteConferences(id);
        storage.getAlarmTriggerStorage().deleteTriggers(id);
        storage.getAlarmStorage().deleteAlarms(id);
        storage.getAttendeeStorage().deleteAttendees(id);
        storage.getEventStorage().deleteEvent(id);
        /*
         * track deletion in result & return deleted events
         */
        resultTracker.trackDeletion(originalEvent);
        deletedEvents.add(originalEvent);
        return deletedEvents;
    }

    /**
     * Virtually deletes a specific internal user attendee from a single event from the storage by setting the <i>hidden</i>-flag and
     * participation status accordingly.
     * <p/>
     * This can be used for any kind of event, i.e. a single, non-recurring event, an existing exception of an event series, or an event
     * series. For the latter one, the attendee is deleted from any existing event exceptions as well.
     * <p/>
     * The deletion includes:
     * <ul>
     * <li>insertion of a <i>tombstone</i> record for the original event</li>
     * <li>insertion of <i>tombstone</i> records for the original attendee</li>
     * <li>deletion of any alarms of the attendee associated with the event</li>
     * <li>marking the attendee as <i>hidden</i> in the event</li>
     * <li>update of the last-modification timestamp of the original event</li>
     * </ul>
     *
     * @param originalEvent The original event to delete
     * @param originalAttendee The original attendee to delete
     * @return The performed event updates as {@link AttendeeEventUpdate}; possibly more than one in case the attendee was removed from
     *         additional overridden instances along with the series master
     */
    protected List<EventUpdate> delete(Event originalEvent, Attendee originalAttendee) throws OXException {
        /*
         * recursively delete any existing event exceptions for this attendee
         */
        List<EventUpdate> attendeeEventUpdates = new ArrayList<EventUpdate>();
        int userId = originalAttendee.getEntity();
        if (isSeriesMaster(originalEvent)) {
            attendeeEventUpdates.addAll(deleteExceptions(originalEvent, originalEvent.getChangeExceptionDates(), userId));
        }
        /*
         * mark event as deleted for this attendee & insert appropriate tombstone record
         */
        String id = originalEvent.getId();
        Event tombstone = storage.getUtilities().getTombstone(originalEvent, timestamp, calendarUser);
        tombstone.setAttendees(Collections.singletonList(storage.getUtilities().getTombstone(originalAttendee)));
        storage.getEventStorage().insertEventTombstone(tombstone);
        storage.getAttendeeStorage().insertAttendeeTombstones(id, originalEvent.getAttendees());
        Attendee updatedAttendee = AttendeeMapper.getInstance().copy(originalAttendee, null, AttendeeField.ENTITY, AttendeeField.MEMBER, AttendeeField.CU_TYPE, AttendeeField.URI);
        updatedAttendee.setHidden(true);
        updatedAttendee.setPartStat(ParticipationStatus.DECLINED);
        updatedAttendee.setTimestamp(timestamp.getTime());
        updatedAttendee.setSequence(originalEvent.getSequence());
        updatedAttendee.setTransp(TimeTransparency.TRANSPARENT);
        updatedAttendee.setComment(session.get(CalendarParameters.PARAMETER_COMMENT, String.class));
        storage.getAttendeeStorage().updateAttendee(id, updatedAttendee);
        storage.getAlarmStorage().deleteAlarms(id, userId);
        /*
         * 'touch' event & track calendar results
         */
        touch(originalEvent);
        Event updatedEvent = loadEventData(id);
        resultTracker.trackUpdate(originalEvent, updatedEvent);
        attendeeEventUpdates.add(new AttendeeEventUpdate(originalEvent, originalAttendee, updatedAttendee));

        // Update alarm trigger
        Map<Integer, List<Alarm>> alarms = storage.getAlarmStorage().loadAlarms(updatedEvent);
        storage.getAlarmTriggerStorage().deleteTriggers(updatedEvent.getId());
        storage.getAlarmTriggerStorage().insertTriggers(updatedEvent, alarms);
        return attendeeEventUpdates;
    }

    /**
     * Virtually deletes a specific internal user attendee from a change exception events from the storage by setting the <i>hidden</i>-
     * flag and participation status accordingly.
     * <p/>
     * For each change exception, the data is removed by invoking {@link #delete(Event, Attendee)} for the exception, in case the
     * user is found the exception's attendee list.
     *
     * @param seriesMaster The series master event to delete the exceptions from
     * @param exceptionDates The recurrence identifiers of the change exceptions to delete
     * @param userID The identifier of the user attendee to delete
     * @return The performed event updates as {@link AttendeeEventUpdate}; possibly more than one in case the attendee was removed from
     *         additional overridden instances along with the series master
     */
    protected List<EventUpdate> deleteExceptions(Event seriesMaster, Collection<RecurrenceId> exceptionDates, int userID) throws OXException {
        List<EventUpdate> attendeeEventUpdates = new ArrayList<EventUpdate>();
        for (Event originalExceptionEvent : loadExceptionData(seriesMaster, exceptionDates)) {
            Attendee originalUserAttendee = find(originalExceptionEvent.getAttendees(), userID);
            if (null != originalUserAttendee) {
                attendeeEventUpdates.addAll(delete(originalExceptionEvent, originalUserAttendee));
            }
        }
        return attendeeEventUpdates;
    }

    /**
     * Virtually deletes a specific internal user attendee from a specific occurrence of a series event that does not yet exist as change
     * exception by setting the <i>hidden</i>-flag and participation status accordingly.
     *
     * @param originalSeriesMaster The original series master event
     * @param recurrenceId The recurrence identifier of the occurrence to remove the attendee for
     * @param originalAttendee The original attendee to delete from the recurrence
     * @return A list containing the performed update of the change exception as {@link AttendeeEventUpdate}
     * @return The newly created change exception event
     */
    protected List<EventUpdate> deleteFromRecurrence(Event originalSeriesMaster, RecurrenceId recurrenceId, Attendee originalAttendee) throws OXException {
        /*
         * prepare & insert a plain exception first, based on the original data from the master, marking the attendee as deleted
         */
        Map<Integer, List<Alarm>> seriesMasterAlarms = storage.getAlarmStorage().loadAlarms(originalSeriesMaster);
        Event newExceptionEvent = prepareException(originalSeriesMaster, recurrenceId);
        Map<Integer, List<Alarm>> newExceptionAlarms = prepareExceptionAlarms(seriesMasterAlarms);
        Check.quotaNotExceeded(storage, session);
        storage.getEventStorage().insertEvent(newExceptionEvent);
        List<Attendee> attendees = new ArrayList<Attendee>(originalSeriesMaster.getAttendees());
        attendees.remove(originalAttendee);
        Attendee updatedAttendee = AttendeeMapper.getInstance().copy(originalAttendee, null, (AttendeeField[]) null);
        updatedAttendee.setHidden(true);
        updatedAttendee.setPartStat(ParticipationStatus.DECLINED);
        updatedAttendee.setTimestamp(timestamp.getTime());
        updatedAttendee.setSequence(originalSeriesMaster.getSequence());
        updatedAttendee.setTransp(TimeTransparency.TRANSPARENT);
        attendees.add(updatedAttendee);
        storage.getAttendeeStorage().insertAttendees(newExceptionEvent.getId(), attendees);
        storage.getAttachmentStorage().insertAttachments(session.getSession(), folder.getId(), newExceptionEvent.getId(), originalSeriesMaster.getAttachments());
        storage.getConferenceStorage().insertConferences(newExceptionEvent.getId(), prepareConferences(originalSeriesMaster.getConferences()));
        for (Entry<Integer, List<Alarm>> entry : newExceptionAlarms.entrySet()) {
            if (originalAttendee.getEntity() != i(entry.getKey())) {
                insertAlarms(newExceptionEvent, i(entry.getKey()), entry.getValue(), true);
            }
        }
        /*
         * add change exception date to series master & track results
         */
        Event updatedExceptionEvent = loadEventData(newExceptionEvent.getId());
        resultTracker.trackCreation(updatedExceptionEvent, originalSeriesMaster);
        resultTracker.rememberOriginalEvent(originalSeriesMaster);
        addChangeExceptionDate(originalSeriesMaster, recurrenceId, false);
        Event updatedMasterEvent = loadEventData(originalSeriesMaster.getId());
        resultTracker.trackUpdate(originalSeriesMaster, updatedMasterEvent);
        AttendeeEventUpdate attendeeEventUpdate = new AttendeeEventUpdate(newExceptionEvent, originalAttendee, updatedAttendee);
        /*
         * reset alarm triggers for series master event and new change exception
         */
        storage.getAlarmTriggerStorage().deleteTriggers(updatedMasterEvent.getId());
        storage.getAlarmTriggerStorage().insertTriggers(updatedMasterEvent, seriesMasterAlarms);
        storage.getAlarmTriggerStorage().deleteTriggers(updatedExceptionEvent.getId());
        storage.getAlarmTriggerStorage().insertTriggers(updatedExceptionEvent, storage.getAlarmStorage().loadAlarms(updatedExceptionEvent));
        return Collections.singletonList(attendeeEventUpdate);
    }

    /**
     * Prepares a map of copied alarms per user based on the original alarms associated with the series master event, prior inserting them
     * for a newly created overridden instance.
     * <p/>
     * Only alarms with <i>relative</i> triggers are considered, and certain alarm properties are not copied.
     *
     * @param masterAlarmsPerUser The original map of alarms per user id of the series master event
     * @return The copy of alarms per user id to use for a new change exception event, or an empty map if there are none
     */
    protected static Map<Integer, List<Alarm>> prepareExceptionAlarms(Map<Integer, List<Alarm>> masterAlarmsPerUser) throws OXException {
        if (null == masterAlarmsPerUser) {
            return Collections.emptyMap();
        }
        Map<Integer, List<Alarm>> copiedAlarmsPerUser = new HashMap<Integer, List<Alarm>>(masterAlarmsPerUser.size());
        for (Entry<Integer, List<Alarm>> entry : masterAlarmsPerUser.entrySet()) {
            List<Alarm> masterAlarms = filterRelativeTriggers(entry.getValue());
            if (null == masterAlarms || masterAlarms.isEmpty()) {
                continue;
            }
            AlarmField[] copiedFields = com.openexchange.tools.arrays.Arrays.remove(AlarmField.values(), AlarmField.ID, AlarmField.UID, AlarmField.RELATED_TO, AlarmField.ACKNOWLEDGED);
            List<Alarm> copiedAlarms = new ArrayList<Alarm>(masterAlarms.size());
            for (Alarm alarm : masterAlarms) {
                copiedAlarms.add(AlarmMapper.getInstance().copy(alarm, null, copiedFields));
            }
            copiedAlarmsPerUser.put(entry.getKey(), copiedAlarms);
        }
        return copiedAlarmsPerUser;
    }

    /**
     * Inserts alarm data for an event of a specific user, optionally assigning new alarm UIDs in case the alarms are copied over from
     * another event. A new unique alarm identifier is always assigned, and the event is passed from the calendar user's folder view to the
     * storage implicitly (based on {@link Utils#getFolderView}.
     *
     * @param event The event the alarms are associated with
     * @param userId The identifier of the user the alarms should be inserted for
     * @param alarms The alarms to insert
     * @param forceNewUids <code>true</code> if new UIDs should be assigned even if already set in the supplied alarms, <code>false</code>, otherwise
     * @return The inserted alarms
     */
    protected List<Alarm> insertAlarms(Event event, int userId, List<Alarm> alarms, boolean forceNewUids) throws OXException {
        if (null == alarms || 0 == alarms.size()) {
            return Collections.emptyList();
        }
        return insertAlarms(event, Collections.singletonMap(I(userId), alarms), forceNewUids).get(I(userId));
    }

    /**
     * Inserts alarm data for an event of a specific user, optionally assigning new alarm UIDs in case the alarms are copied over from
     * another event. A new unique alarm identifier is always assigned, and the event is passed from the calendar user's folder view to the
     * storage implicitly (based on {@link Utils#getFolderView}.
     *
     * @param event The event the alarms are associated with
     * @param alarmsByUserId The alarms to insert, mapped to the corresponding user identifier
     * @param alarms The alarms to insert
     * @param forceNewUids <code>true</code> if new UIDs should be assigned even if already set in the supplied alarms, <code>false</code>, otherwise
     * @return The inserted alarms, mapped to the corresponding user identifier
     */
    protected Map<Integer, List<Alarm>> insertAlarms(Event event, Map<Integer, List<Alarm>> alarmsByUserId, boolean forceNewUids) throws OXException {
        if (null == alarmsByUserId || 0 == alarmsByUserId.size()) {
            return Collections.emptyMap();
        }
        Map<Integer, List<Alarm>> newAlarmsByUserId = new HashMap<Integer, List<Alarm>>(alarmsByUserId.size());
        for (Entry<Integer, List<Alarm>> entry : alarmsByUserId.entrySet()) {
            List<Alarm> newAlarms = new ArrayList<Alarm>(entry.getValue().size());
            AlarmPreparator.getInstance().prepareEMailAlarms(session, entry.getValue());
            for (Alarm alarm : entry.getValue()) {
                Alarm newAlarm = AlarmMapper.getInstance().copy(alarm, null, (AlarmField[]) null);
                newAlarm.setId(storage.getAlarmStorage().nextId());
                newAlarm.setTimestamp(timestamp.getTime());
                if (forceNewUids || false == newAlarm.containsUid() || Strings.isEmpty(newAlarm.getUid())) {
                    newAlarm.setUid(UUID.randomUUID().toString());
                }

                newAlarms.add(newAlarm);
            }
            String folderView = getFolderView(event, i(entry.getKey()));
            if (false == folderView.equals(event.getFolderId())) {
                event = new DelegatingEvent(event) {

                    @Override
                    public String getFolderId() {
                        return folderView;
                    }

                    @Override
                    public boolean containsFolderId() {
                        return true;
                    }
                };
            }
            storage.getAlarmStorage().insertAlarms(event, i(entry.getKey()), newAlarms);
            newAlarmsByUserId.put(entry.getKey(), newAlarms);
        }
        return newAlarmsByUserId;
    }

    /**
     * Updates a calendar user's alarms for a specific event.
     *
     * @param event The event to update the alarms in
     * @param userId The identifier of the calendar user whose alarms are updated
     * @param originalAlarms The original alarms, or <code>null</code> if there are none
     * @param updatedAlarms The updated alarms
     * @return <code>true</code> if there were any updates, <code>false</code>, otherwise
     */
    protected boolean updateAlarms(Event event, int userId, List<Alarm> originalAlarms, List<Alarm> updatedAlarms) throws OXException {
        AlarmPreparator.getInstance().prepareEMailAlarms(session, updatedAlarms);
        CollectionUpdate<Alarm, AlarmField> alarmUpdates = AlarmUtils.getAlarmUpdates(originalAlarms, updatedAlarms);
        if (alarmUpdates.isEmpty()) {
            return false;
        }
        requireWritePermissions(event, Collections.singletonList(session.getEntityResolver().prepareUserAttendee(userId)));
        int size = updatedAlarms == null ? 0 : updatedAlarms.size();
        List<Integer> toDelete = new ArrayList<>(size);
        List<Integer> toAdd = new ArrayList<>(size);
        /*
         * delete removed alarms
         */
        List<Alarm> removedItems = alarmUpdates.getRemovedItems();
        if (0 < removedItems.size()) {
            int[] alarmIDs = getAlarmIDs(removedItems);
            storage.getAlarmStorage().deleteAlarms(event.getId(), userId, alarmIDs);
            for (int i : alarmIDs) {
                toDelete.add(I(i));
            }
        }
        /*
         * save updated alarms
         */
        List<? extends ItemUpdate<Alarm, AlarmField>> updatedItems = alarmUpdates.getUpdatedItems();
        if (0 < updatedItems.size()) {
            List<Alarm> alarms = new ArrayList<Alarm>(updatedItems.size());
            for (ItemUpdate<Alarm, AlarmField> itemUpdate : updatedItems) {
                Alarm alarm = AlarmMapper.getInstance().copy(itemUpdate.getOriginal(), null, (AlarmField[]) null);
                AlarmMapper.getInstance().copy(itemUpdate.getUpdate(), alarm, AlarmField.values());
                alarm.setId(itemUpdate.getOriginal().getId());
                alarm.setUid(itemUpdate.getOriginal().getUid());
                alarm.setTimestamp(timestamp.getTime());
                alarms.add(Check.alarmIsValid(alarm, itemUpdate.getUpdatedFields().toArray(new AlarmField[itemUpdate.getUpdatedFields().size()])));
                Integer alarmId = I(alarm.getId());
                toDelete.add(alarmId);
                toAdd.add(alarmId);
            }
            final String folderView = getFolderView(event, userId);
            if (false == folderView.equals(event.getFolderId())) {
                Event userizedEvent = new DelegatingEvent(event) {

                    @Override
                    public String getFolderId() {
                        return folderView;
                    }

                    @Override
                    public boolean containsFolderId() {
                        return true;
                    }
                };
                storage.getAlarmStorage().updateAlarms(userizedEvent, userId, alarms);
            } else {
                storage.getAlarmStorage().updateAlarms(event, userId, alarms);
            }
        }
        /*
         * insert new alarms
         */
        List<Alarm> insertAlarms = insertAlarms(event, userId, alarmUpdates.getAddedItems(), false);
        for (Alarm alarm : insertAlarms) {
            toAdd.add(I(alarm.getId()));
        }
        List<Alarm> loadAlarms = storage.getAlarmStorage().loadAlarms(event, userId);
        storage.getAlarmTriggerStorage().deleteTriggersById(toDelete);
        if (null != loadAlarms && 0 < loadAlarms.size()) {
            loadAlarms = new ArrayList<Alarm>(loadAlarms);
            Iterator<Alarm> iterator = loadAlarms.iterator();
            while (iterator.hasNext()) {
                if (!toAdd.contains(I(iterator.next().getId()))) {
                    iterator.remove();
                }
            }
        }
        // only insert the filtered alarms of the current user
        storage.getAlarmTriggerStorage().insertTriggers(event, Collections.singletonMap(I(userId), loadAlarms));

        return true;
    }

    /**
     * Optionally loads all non user-specific data for a specific event, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the event is performed and no alarm data is fetched for a specific attendee, i.e. only the plain/vanilla
     * event data is loaded from the storage.
     *
     * @param id The identifier of the event to load
     * @return The event data, or <code>null</code> if not found
     */
    protected Event optEventData(String id) throws OXException {
        Event event = storage.getEventStorage().loadEvent(id, null);
        if (null == event) {
            return null;
        }
        return new UnmodifiableEvent(storage.getUtilities().loadAdditionalEventData(-1, event, null));
    }

    /**
     * Loads all non user-specific data for a specific event, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the event is performed and no alarm data is fetched for a specific attendee, i.e. only the plain/vanilla
     * event data is loaded from the storage.
     *
     * @param id The identifier of the event to load
     * @return The event data
     * @throws OXException {@link CalendarExceptionCodes#EVENT_NOT_FOUND}
     */
    protected Event loadEventData(String id) throws OXException {
        Event event = optEventData(id);
        if (null == event) {
            throw CalendarExceptionCodes.EVENT_NOT_FOUND.create(id);
        }
        return event;
    }

    /**
     * Loads all non user-specific data for a all exceptions of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception events is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesId The identifier of the series to load the exceptions for
     * @return The event exception data, or an empty list if there are none
     */
    protected List<Event> loadExceptionData(String seriesId) throws OXException {
        List<Event> exceptions = storage.getEventStorage().loadExceptions(seriesId, null);
        exceptions = storage.getUtilities().loadAdditionalEventData(-1, exceptions, null);
        List<Event> unmodifiableExceptions = new ArrayList<Event>(exceptions.size());
        for (Event exception : exceptions) {
            unmodifiableExceptions.add(new UnmodifiableEvent(exception));
        }
        return unmodifiableExceptions;
    }

    /**
     * Loads all non user-specific data for a all exceptions of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception events is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesMaster The series master event to load the exceptions from
     * @return The event exception data, or an empty list if there are none
     */
    protected List<Event> loadExceptionData(Event seriesMaster) throws OXException {
        if (false == isSeriesMaster(seriesMaster) || isNullOrEmpty(seriesMaster.getChangeExceptionDates())) {
            return Collections.emptyList();
        }
        List<Event> exceptions = storage.getEventStorage().loadExceptions(seriesMaster.getSeriesId(), null);
        exceptions = storage.getUtilities().loadAdditionalEventData(-1, exceptions, null);
        exceptions = injectRecurrenceData(exceptions, new DefaultRecurrenceData(seriesMaster.getRecurrenceRule(), seriesMaster.getStartDate()));
        List<Event> unmodifiableExceptions = new ArrayList<Event>(exceptions.size());
        for (Event exception : exceptions) {
            unmodifiableExceptions.add(new UnmodifiableEvent(exception));
        }
        return unmodifiableExceptions;
    }

    /**
     * Loads all non user-specific data for a collection of exceptions of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception events is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesMaster The series master event to load the exceptions from
     * @param recurrenceIds The recurrence identifiers of the exceptions to load
     * @return The event exception data
     * @throws OXException {@link CalendarExceptionCodes#EVENT_RECURRENCE_NOT_FOUND}
     */
    protected List<Event> loadExceptionData(Event seriesMaster, Collection<RecurrenceId> recurrenceIds) throws OXException {
        List<Event> changeExceptions = new ArrayList<Event>();
        if (null != recurrenceIds && 0 < recurrenceIds.size()) {
            for (RecurrenceId recurrenceId : recurrenceIds) {
                Event exception = storage.getEventStorage().loadException(seriesMaster.getSeriesId(), recurrenceId, null);
                if (null == exception) {
                    throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(seriesMaster.getSeriesId(), String.valueOf(recurrenceId));
                }
                changeExceptions.add(exception);
            }
        }
        changeExceptions = storage.getUtilities().loadAdditionalEventData(-1, changeExceptions, null);
        changeExceptions = injectRecurrenceData(changeExceptions, new DefaultRecurrenceData(seriesMaster.getRecurrenceRule(), seriesMaster.getStartDate()));
        return changeExceptions;
    }

    /**
     * Loads all non user-specific data for a specific exception of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception event is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesMaster The series master event to load the exception from
     * @param recurrenceId The recurrence identifier of the exception to load
     * @return The event exception data
     * @throws OXException {@link CalendarExceptionCodes#EVENT_RECURRENCE_NOT_FOUND}
     */
    protected Event loadExceptionData(Event seriesMaster, RecurrenceId recurrenceId) throws OXException {
        Event changeException = optExceptionData(seriesMaster, recurrenceId);
        if (null == changeException) {
            throw CalendarExceptionCodes.EVENT_RECURRENCE_NOT_FOUND.create(seriesMaster.getSeriesId(), recurrenceId);
        }
        return changeException;
    }

    /**
     * Optionally loads all non user-specific data for a specific exception of an event series, including attendees and attachments.
     * <p/>
     * No <i>userization</i> of the exception event is performed and no alarm data is fetched for a specific attendee, i.e. only the
     * plain/vanilla event data is loaded from the storage.
     *
     * @param seriesMaster The series master event to load the exception from
     * @param recurrenceId The recurrence identifier of the exception to load
     * @return The event exception data, or <code>null</code> if not found
     */
    private Event optExceptionData(Event seriesMaster, RecurrenceId recurrenceId) throws OXException {
        Event changeException = storage.getEventStorage().loadException(seriesMaster.getSeriesId(), recurrenceId, null);
        if (null != changeException) {
            changeException = storage.getUtilities().loadAdditionalEventData(-1, changeException, null);
            changeException = injectRecurrenceData(changeException, new DefaultRecurrenceData(seriesMaster.getRecurrenceRule(), seriesMaster.getStartDate()));
        }
        return changeException;
    }

    /**
     * Checks that the current session's user is able to delete a specific event, by either requiring delete access for <i>own</i> or
     * <i>all</i> objects, based on the user being the creator of the event or not.
     * <p/>
     * Additionally, the event's classification is checked.
     *
     * @param originalEvent The event to check the user's delete permissions for
     * @throws OXException {@link CalendarExceptionCodes#NO_DELETE_PERMISSION}, {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     * @see Check#requireCalendarPermission
     * @see Check#classificationAllowsUpdate
     */
    protected void requireDeletePermissions(Event originalEvent) throws OXException {
        requireDeletePermissions(originalEvent, false);
    }

    /**
     * Checks that the current session's user is able to delete a specific event, by either requiring delete access for <i>own</i> or
     * <i>all</i> objects, based on the user being the creator of the event or not.
     * <p/>
     * Additionally, the event's classification is checked.
     *
     * @param originalEvent The event to check the user's delete permissions for
     * @param assumeExternalOrganizerUpdate <code>true</code> if an external organizer update can be assumed, <code>false</code>, otherwise
     * @throws OXException {@link CalendarExceptionCodes#NO_DELETE_PERMISSION}, {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     * @see Check#requireCalendarPermission
     * @see Check#classificationAllowsUpdate
     */
    protected void requireDeletePermissions(Event originalEvent, boolean assumeExternalOrganizerUpdate) throws OXException {
        if (matches(originalEvent.getCreatedBy(), session.getUserId())) {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_OWN_OBJECTS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_ALL_OBJECTS);
        }
        classificationAllowsUpdate(folder, originalEvent);
        /*
         * require organizer role in case there are further internal attendees
         */
        if (false == isLastNonHiddenUserAttendee(originalEvent.getAttendees(), calendarUserId)) {
            requireOrganizerSchedulingResource(originalEvent, assumeExternalOrganizerUpdate);
        }
    }

    /**
     * Checks that data of a specific attendee of an event can be deleted by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The event to check the user's delete permissions for
     * @param deletedAttendee The attendee who is deleted
     * @throws OXException {@link CalendarExceptionCodes#NO_DELETE_PERMISSION}, {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     * @see Check#requireCalendarPermission
     * @see Check#classificationAllowsUpdate
     */
    protected void requireDeletePermissions(Event originalEvent, Attendee deletedAttendee) throws OXException {
        requireDeletePermissions(originalEvent, deletedAttendee, false);
    }

    /**
     * Checks that data of a specific attendee of an event can be deleted by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The event to check the user's delete permissions for
     * @param deletedAttendee The attendee who is deleted
     * @param assumeExternalOrganizerUpdate <code>true</code> if an external organizer update can be assumed, <code>false</code>, otherwise
     * @throws OXException {@link CalendarExceptionCodes#NO_DELETE_PERMISSION}, {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     * @see Check#requireCalendarPermission
     * @see Check#classificationAllowsUpdate
     */
    protected void requireDeletePermissions(Event originalEvent, Attendee deletedAttendee, boolean assumeExternalOrganizerUpdate) throws OXException {
        if (false == matches(deletedAttendee, calendarUserId)) {
            /*
             * always require permissions for whole event in case an attendee different from the calendar user is updated
             */
            requireDeletePermissions(originalEvent, assumeExternalOrganizerUpdate);
        }
        if (session.getUserId() != calendarUserId) {
            /*
             * user acts on behalf of other calendar user, allow attendee deletion based on permissions in underlying folder
             */
            if (matches(originalEvent.getCreatedBy(), session.getUserId())) {
                requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_OWN_OBJECTS);
            } else {
                requireCalendarPermission(folder, READ_FOLDER, NO_PERMISSIONS, NO_PERMISSIONS, DELETE_ALL_OBJECTS);
            }
            classificationAllowsUpdate(folder, originalEvent);
        }
    }

    /**
     * Checks that the current session's user is able to act as the organizer in case the event represents a group scheduled event.
     *
     * @param originalEvent The event to check the calendar user's role in
     * @param assumeExternalOrganizerUpdate <code>true</code> if an external organizer update can be assumed, <code>false</code>, otherwise
     * @throws OXException In case the calendar user is <b>not</b> the organizer and further checks on e.g. attendee privileges as per {@link Event#getAttendeePrivileges()} fail
     */
    void requireOrganizerSchedulingResource(Event originalEvent, boolean assumeExternalOrganizerUpdate) throws OXException {
        if (PublicType.getInstance().equals(folder.getType())) {
            /*
             * event located in public folder, assume change as or on behalf of organizer
             */
            return;
        }
        if (false == isGroupScheduled(originalEvent)) {
            /*
             * non group-schedule events, nothing to check
             */
            return;
        }
        if (false == isOrganizer(originalEvent, calendarUserId) && false == assumeExternalOrganizerUpdate) {
            /*
             * calendar user is not organizer of group scheduled event, throw error if configured
             */
            OXException e = CalendarExceptionCodes.NOT_ORGANIZER.create(folder.getId(), originalEvent.getId(), originalEvent.getOrganizer().getUri(), originalEvent.getOrganizer().getCn());
            if (session.getConfig().isRestrictAllowedAttendeeChanges()) {
                throw e;
            }
            session.addWarning(e);
        }
    }

    /**
     * Checks that a specific event can be updated by the current session's user under the perspective of the current folder, by either
     * requiring write access for <i>own</i> or <i>all</i> objects, based on the user being the creator of the event or not.
     * <p/>
     * Additionally, the event's classification is checked.
     *
     * @param originalEvent The original event being updated
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent) throws OXException {
        requireWritePermissions(originalEvent, false);
    }

    /**
     * Checks that a specific event can be updated by the current session's user under the perspective of the current folder, by either
     * requiring write access for <i>own</i> or <i>all</i> objects, based on the user being the creator of the event or not.
     * <p/>
     * Additionally, the event's classification is checked.
     *
     * @param originalEvent The original event being updated
     * @param assumeExternalOrganizerUpdate <code>true</code> if an external organizer update can be assumed, <code>false</code>, otherwise
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent, boolean assumeExternalOrganizerUpdate) throws OXException {
        if (matches(originalEvent.getCreatedBy(), session.getUserId())) {
            requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, WRITE_OWN_OBJECTS, NO_PERMISSIONS);
        } else {
            requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, WRITE_ALL_OBJECTS, NO_PERMISSIONS);
        }
        Check.classificationAllowsUpdate(folder, originalEvent);
        /*
         * allow update for internally organized events if 'modify' attendee privileges are set
         */
        if (hasAttendeePrivileges(originalEvent, DefaultAttendeePrivileges.MODIFY) && false == hasExternalOrganizer(originalEvent)) {
            return;
        }
        /*
         * require organizer, otherwise
         */
        requireOrganizerSchedulingResource(originalEvent, assumeExternalOrganizerUpdate);
    }

    /**
     * Checks that data of a specific attendee of an event can be updated by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The original event being updated
     * @param updatedAttendee The attendee whose data is updated
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent, CalendarUser updatedAttendee) throws OXException {
        requireWritePermissions(originalEvent, updatedAttendee, false);
    }

    /**
     * Checks that data of a specific attendee of an event can be updated by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The original event being updated
     * @param updatedAttendee The attendee whose data is updated
     * @param assumeExternalOrganizerUpdate <code>true</code> if an external organizer update can be assumed, <code>false</code>, otherwise
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent, CalendarUser updatedAttendee, boolean assumeExternalOrganizerUpdate) throws OXException {
        if (false == matches(updatedAttendee, calendarUserId)) {
            /*
             * always require permissions for whole event in case an attendee different from the calendar user is updated
             */
            requireWritePermissions(originalEvent, assumeExternalOrganizerUpdate);
        }
        if (session.getUserId() != calendarUserId) {
            /*
             * user acts on behalf of other calendar user, allow attendee update based on permissions in underlying folder
             */
            if (matches(originalEvent.getCreatedBy(), session.getUserId())) {
                requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, WRITE_OWN_OBJECTS, NO_PERMISSIONS);
            } else {
                requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, WRITE_ALL_OBJECTS, NO_PERMISSIONS);
            }
            Check.classificationAllowsUpdate(folder, originalEvent);
        }
    }

    /**
     * Checks that data of a specific attendee of an event can be updated by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The original event being updated
     * @param attendeeUpdate The attendee update
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent, ItemUpdate<Attendee, AttendeeField> attendeeUpdate) throws OXException {
        requireWritePermissions(originalEvent, attendeeUpdate, false);
    }

    /**
     * Checks that data of a specific attendee of an event can be updated by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The original event being updated
     * @param attendeeUpdate The attendee update
     * @param assumeExternalOrganizerUpdate <code>true</code> if an external organizer update can be assumed, <code>false</code>, otherwise
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent, ItemUpdate<Attendee, AttendeeField> attendeeUpdate, boolean assumeExternalOrganizerUpdate) throws OXException {
        /*
         * check general write permissions for attendee
         */
        Attendee originalAttendee = attendeeUpdate.getOriginal();
        requireWritePermissions(originalEvent, originalAttendee, assumeExternalOrganizerUpdate);
        /*
         * if configured, deny setting the participation status to a value other than NEEDS-ACTION for other attendees: RFC 6638, section 3.2.1
         * (with the exception of booking delegates setting the status for resources managed by them) 
         */
        if (false == session.getConfig().isAllowOrganizerPartStatChanges() && false == matches(originalAttendee, calendarUserId) && false == assumeExternalOrganizerUpdate && 
            attendeeUpdate.getUpdatedFields().contains(AttendeeField.PARTSTAT) && false == ParticipationStatus.NEEDS_ACTION.matches(attendeeUpdate.getUpdate().getPartStat()) &&
            false == (isInternal(originalAttendee) && isResourceOrRoom(originalAttendee) && isBookingDelegate(session, originalAttendee.getEntity()))) {
            throw CalendarExceptionCodes.FORBIDDEN_ATTENDEE_CHANGE.create(originalEvent.getId(), originalAttendee, AttendeeField.PARTSTAT);
        }
    }

    /**
     * Checks that data of one or more attendees of an event can be updated by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The original event being updated
     * @param updatedAttendees The attendees whose data is updated
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent, List<Attendee> updatedAttendees) throws OXException {
        requireWritePermissions(originalEvent, updatedAttendees, false);
    }

    /**
     * Checks that data of one or more attendees of an event can be updated by the current session's user under the perspective of the
     * current folder.
     *
     * @param originalEvent The original event being updated
     * @param updatedAttendees The attendees whose data is updated
     * @param assumeExternalOrganizerUpdate <code>true</code> if an external organizer update can be assumed, <code>false</code>, otherwise
     * @throws OXException {@link CalendarExceptionCodes#UNSUPPORTED_FOLDER}, {@link CalendarExceptionCodes#NO_READ_PERMISSION},
     *             {@link CalendarExceptionCodes#NO_WRITE_PERMISSION}, {@link CalendarExceptionCodes#NOT_ORGANIZER},
     *             {@link CalendarExceptionCodes#RESTRICTED_BY_CLASSIFICATION}
     */
    protected void requireWritePermissions(Event originalEvent, List<Attendee> updatedAttendees, boolean assumeExternalOrganizerUpdate) throws OXException {
        for (Attendee updatedAttendee : updatedAttendees) {
            requireWritePermissions(originalEvent, updatedAttendee, assumeExternalOrganizerUpdate);
        }
    }

    /**
     * Gets a value indicating whether a delete operation performed in the current folder from the calendar user's perspective would lead
     * to a <i>real</i> deletion of the event from the storage, or if only the calendar user is removed from the attendee list, hence
     * rather an update is performed.
     * <p/>
     * A deletion leads to a complete removal if
     * <ul>
     * <li>the event is located in a <i>public folder</i></li>
     * <li>or the event is not <i>group-scheduled</i></li>
     * <li>or the calendar user is the organizer of the event</li>
     * <li>or the calendar user is the last <i>non-hidden</i> internal user attendee in an <i>internally organized</i> event</li>
     * <li>or the calendar user is the last <i>non-hidden</i> internal user attendee in an <i>externally organized</i> event, that is not part of an event series</li>
     * </ul>
     * <p/>
     * Note: The last constraint (removals of externally organized event series) usually needs additional considerations.
     * <p/>
     * Note: Even if attendees are allowed to modify the event, deletion is out of scope.
     *
     * @param originalEvent The original event to check
     * @return <code>true</code> if a deletion would lead to a removal of the event, <code>false</code>, otherwise
     */
    protected boolean deleteRemovesEvent(Event originalEvent) {
        return Utils.deleteRemovesEvent(folder, originalEvent);
    }

    /**
     * Gets a value indicating whether a delete operation performed in the current folder from the calendar user's perspective would lead
     * to a <i>real</i> deletion of the event from the storage, or if only the calendar user is removed from the attendee list, hence
     * rather an update is performed.
     * <p/>
     * A deletion leads to a complete removal if
     * <ul>
     * <li>the event is located in a <i>public folder</i></li>
     * <li>or the event is not <i>group-scheduled</i></li>
     * <li>or the calendar user is the organizer of the event</li>
     * <li>or the calendar user is the last <i>non-hidden</i> internal user attendee in an <i>internally organized</i> event</li>
     * <li>or the calendar user is the last <i>non-hidden</i> internal user attendee in an <i>externally organized</i> event, that is not part of an event series</li>
     * <li>or the calendar user is the last <i>non-hidden</i> internal user attendee in an <i>externally organized</i> series event, and
     * the user has also been deleted (or hidden in) all other occurrences of the event series</li>
     * </ul>
     * <p/>
     * Note: Even if attendees are allowed to modify the event, deletion is out of scope.
     *
     * @param originalEvent The original event to check
     * @param recurrenceId The recurrence identifier of the occurrence in question, or <code>null</code> if the event itself is being deleted
     * @return <code>true</code> if a deletion would lead to a removal of the event, <code>false</code>, otherwise
     */
    protected boolean deleteRemovesEvent(Event originalEvent, RecurrenceId recurrenceId) throws OXException {
        if (Utils.deleteRemovesEvent(folder, originalEvent)) {
            return true;
        }
        if (hasExternalOrganizer(originalEvent) && isSeriesEvent(originalEvent)) {
            /*
             * deletion of externally organized series event; only remove event data if series master is targeted,
             * or attendee is deleted/hidden in all other occurrences, too
             */
            if (isSeriesMaster(originalEvent)) {
                return null == recurrenceId;
            }
            if (isSeriesException(originalEvent)) {
                Event seriesMaster = optEventData(originalEvent.getSeriesId());
                if (null != seriesMaster && false == isHidden(seriesMaster, calendarUserId)) {
                    return false;
                }
                for (Event changeException : loadExceptionData(originalEvent.getSeriesId())) {
                    if (false == changeException.getId().equals(originalEvent.getId()) && false == isHidden(changeException, calendarUserId)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether an event series has further occurrences besides the occurrences identified by the supplied
     * recurrence identifiers or not.
     *
     * @param seriesMaster The series master event
     * @param recurrenceIds The recurrence identifiers to skip, or <code>null</code> to just check the series itself
     * @return <code>true</code> if the event's recurrence set yields at least one further occurrence, <code>false</code>, otherwise
     */
    protected boolean hasFurtherOccurrences(Event seriesMaster, SortedSet<RecurrenceId> recurrenceIds) throws OXException {
        RecurrenceData recurrenceData = new DefaultRecurrenceData(seriesMaster.getRecurrenceRule(), seriesMaster.getStartDate(), getExceptionDates(recurrenceIds), getExceptionDates(seriesMaster.getRecurrenceDates()));
        try {
            return session.getRecurrenceService().iterateRecurrenceIds(recurrenceData).hasNext();
        } catch (OXException e) {
            if ("CAL-4061".equals(e.getErrorCode())) {
                // "Invalid recurrence id [id ..., rule ...]", so outside recurrence set
                return false;
            }
            throw e;
        }
    }

    /**
     * If <code>TRACE</code> logging is enabled, the original and the updated event
     * will be logged as JSON
     *
     * @param update The event update to log
     */
    protected void logPerform(EventUpdate update) {
        LOG.trace("Original: >\n{}\nUpdated: >\n{}", new JSONPrintableEvent(session, update.getOriginal()), new JSONPrintableEvent(session, update.getUpdate()));
    }

    /**
     * Deletes an existing change exception. Besides the removal of the change exception data via {@link #delete(Event)}, this also
     * includes adjusting the master event's change- and delete exception date arrays.
     *
     * @param originalSeriesMaster The original series master event, or <code>null</code> if not available
     * @param originalExceptionEvent The original exception event
     * @return A list holding the deleted exception event
     */
    protected List<Event> deleteException(Event originalSeriesMaster, Event originalExceptionEvent) throws OXException {
        /*
         * delete the exception
         */
        RecurrenceId recurrenceId = originalExceptionEvent.getRecurrenceId();
        List<Event> deletedEvents = delete(originalExceptionEvent);
        if (null != originalSeriesMaster) {
            /*
             * update the series master accordingly
             */
            addDeleteExceptionDate(originalSeriesMaster, recurrenceId);
        }
        return deletedEvents;
    }

    /**
     * Adds a specific recurrence identifier to the series master's delete exception array, i.e. creates a new delete exception. A
     * previously existing entry for the recurrence identifier in the master's change exception date array is removed implicitly. In case
     * there are no occurrences remaining at all after the deletion, the whole series event is deleted.
     * <p/>
     * Registered calendar service interceptors are triggered for the update of the series master event, too.
     *
     * @param originalMasterEvent The original series master event
     * @param recurrenceId The recurrence identifier of the occurrence to add
     * @return The updated master event, or <code>null</code> if it's gone after the last occurrence was deleted
     */
    protected Event addDeleteExceptionDate(Event originalMasterEvent, RecurrenceId recurrenceId) throws OXException {
        /*
         * build new set of delete exception dates
         */
        SortedSet<RecurrenceId> deleteExceptionDates = new TreeSet<RecurrenceId>();
        if (null != originalMasterEvent.getDeleteExceptionDates()) {
            deleteExceptionDates.addAll(originalMasterEvent.getDeleteExceptionDates());
        }
        if (false == deleteExceptionDates.add(recurrenceId)) {
            /*
             * delete exception data already exists, ignore
             */
            LOG.warn("Delete exeception data for {} already exists, ignoring.", recurrenceId);
        }
        /*
         * check if there are any further occurrences left
         */
        if (false == hasFurtherOccurrences(originalMasterEvent, deleteExceptionDates)) {
            /*
             * delete series master
             */
            delete(originalMasterEvent);
            return null;
        }
        /*
         * re-build exception date lists based on existing series master to guarantee consistency
         */
        SortedSet<RecurrenceId> changeExceptionDates = loadChangeExceptionDates(originalMasterEvent.getSeriesId());
        for (RecurrenceId changeExceptionDate : changeExceptionDates) {
            RecurrenceId matchingChangeExceptionDate = find(deleteExceptionDates, changeExceptionDate);
            if (null != matchingChangeExceptionDate && deleteExceptionDates.remove(matchingChangeExceptionDate)) {
                LOG.warn("Skipping {} in delete exception date collection due to existing change exception event.", matchingChangeExceptionDate);
            }
        }
        /*
         * update series master accordingly
         */
        resultTracker.rememberOriginalEvent(originalMasterEvent);
        Event eventUpdate = new Event();
        eventUpdate.setId(originalMasterEvent.getId());
        eventUpdate.setDeleteExceptionDates(deleteExceptionDates);
        if (false == changeExceptionDates.equals(originalMasterEvent.getChangeExceptionDates())) {
            eventUpdate.setChangeExceptionDates(changeExceptionDates);
        }
        if (null != originalMasterEvent.getOrganizer() && isInternal(originalMasterEvent.getOrganizer(), CalendarUserType.INDIVIDUAL)) {
            eventUpdate.setSequence(originalMasterEvent.getSequence() + 1);
        }
        Consistency.setModified(session, timestamp, eventUpdate, calendarUserId, hasExternalOrganizer(originalMasterEvent));
        Consistency.normalizeRecurrenceIDs(originalMasterEvent.getStartDate(), eventUpdate);
        /*
         * trigger calendar interceptors & update event in storage
         */
        interceptorRegistry.triggerInterceptorsOnBeforeUpdate(originalMasterEvent, eventUpdate);
        storage.getEventStorage().updateEvent(eventUpdate);
        Event updatedMasterEvent = loadEventData(originalMasterEvent.getId());

        /*
         * update alarms
         */
        updateAlarmTrigger(originalMasterEvent, updatedMasterEvent);

        /*
         * track update of master in result
         */
        resultTracker.trackUpdate(originalMasterEvent, updatedMasterEvent);
        return updatedMasterEvent;
    }

    /**
     * Deletes all future occurrences by updating the recurrence rule. Efficiently shortens the series.
     *
     * @param originalMasterEvent The original series master event
     * @param recurrenceId The recurrence identifier of the occurrence to add, containing the {@link RecurrenceRange}
     * @param trackOrphanedAttendees If scheduling messages to orphaned attendees should be sent or not.
     * @return The updated series master
     * @throws OXException
     */
    protected Event deleteFutureRecurrences(Event originalMasterEvent, RecurrenceId recurrenceId, boolean trackOrphanedAttendees) throws OXException {
        /*
         * delete "this and future" recurrences; adjust recurrence rule to have a fixed UNTIL one second or day prior the targeted occurrence
         */
        Check.recurrenceRangeMatches(recurrenceId, RecurrenceRange.THISANDFUTURE);
        Event eventUpdate = EventMapper.getInstance().copy(originalMasterEvent, null, EventField.ID, EventField.SERIES_ID, EventField.START_DATE, EventField.END_DATE);
        RecurrenceRule rule = initRecurrenceRule(originalMasterEvent.getRecurrenceRule());
        DateTime until = recurrenceId.getValue().addDuration(recurrenceId.getValue().isAllDay() ? new Duration(-1, 1, 0) : new Duration(-1, 0, 1));
        rule.setUntil(until);
        eventUpdate.setRecurrenceRule(rule.toString());
        /*
         * remove any change- and delete exceptions after the occurrence & remember one-off attendees not attending the series master event
         */
        Entry<SortedSet<RecurrenceId>, SortedSet<RecurrenceId>> splittedDeleteExceptionDates = splitExceptionDates(originalMasterEvent.getDeleteExceptionDates(), until);
        if (false == splittedDeleteExceptionDates.getValue().isEmpty()) {
            eventUpdate.setDeleteExceptionDates(splittedDeleteExceptionDates.getKey());
        }
        List<Event> deletedChangeExceptions = new ArrayList<Event>();
        List<Attendee> orphanedAttendees = new ArrayList<Attendee>();
        Entry<SortedSet<RecurrenceId>, SortedSet<RecurrenceId>> splittedChangeExceptionDates = splitExceptionDates(originalMasterEvent.getChangeExceptionDates(), until);
        if (false == splittedChangeExceptionDates.getValue().isEmpty()) {
            for (Event changeException : loadExceptionData(originalMasterEvent, splittedChangeExceptionDates.getValue())) {
                deletedChangeExceptions.addAll(delete(changeException));
                for (Attendee attendee : changeException.getAttendees()) {
                    if (false == contains(originalMasterEvent.getAttendees(), attendee) && false == contains(orphanedAttendees, attendee)) {
                        orphanedAttendees.add(attendee);
                    }
                }
            }
            eventUpdate.setChangeExceptionDates(splittedChangeExceptionDates.getKey());
        }
        /*
         * trigger calendar interceptors, update series master in storage & track results as updated request for adjusted event series,
         * or as cancel message for orphaned attendees
         */
        eventUpdate.setSequence(originalMasterEvent.getSequence() + 1);
        Consistency.setModified(session, timestamp, eventUpdate, session.getUserId(), hasExternalOrganizer(originalMasterEvent));
        interceptorRegistry.triggerInterceptorsOnBeforeUpdate(originalMasterEvent, eventUpdate);
        storage.getEventStorage().updateEvent(eventUpdate);
        Event updatedEvent = loadEventData(originalMasterEvent.getId());
        updateAlarmTrigger(originalMasterEvent, updatedEvent);
        resultTracker.trackUpdate(originalMasterEvent, updatedEvent);

        /*
         * send CANCEL mails for orphaned attendees
         */
        if (trackOrphanedAttendees && 0 < orphanedAttendees.size()) {
            schedulingHelper.trackDeletion(new DefaultCalendarObjectResource(deletedChangeExceptions), null, orphanedAttendees);
        }

        return updatedEvent;
    }

    private void updateAlarmTrigger(Event originalMasterEvent, Event updatedMasterEvent) throws OXException {
        Map<Integer, List<Alarm>> alarms = storage.getAlarmStorage().loadAlarms(updatedMasterEvent);
        storage.getAlarmTriggerStorage().deleteTriggers(originalMasterEvent.getId());
        storage.getAlarmTriggerStorage().insertTriggers(updatedMasterEvent, alarms);
    }

    /**
     * Prepares attachment prior insert in the storage
     * <p>
     * Will automatically detected MIME type if not set by the client
     *
     * @param attachments The attachments to prepare
     * @return The prepared attachments
     */
    protected List<Attachment> prepareAttachments(List<Attachment> attachments) {
        for (Attachment attachment : attachments) {
            if (Strings.isEmpty(attachment.getFormatType())) {
                MimeTypeMap mimeTypeMap = Services.optService(MimeTypeMap.class);
                String type;
                if (Strings.isEmpty(attachment.getFilename()) || null == mimeTypeMap) {
                    LOG.debug("Unable to set correct format-/ MIME-type for attachment {}. Using default.", attachment.getFilename());
                    type = com.openexchange.mail.mime.MimeTypes.MIME_APPL_OCTET;
                } else {
                    type = mimeTypeMap.getContentType(attachment.getFilename());
                }
                LOG.trace("Auto detected mime type {} for attachment {}.", type, attachment.getFilename());
                attachment.setFormatType(type);
            }
        }
        return attachments;
    }

    /**
     * Prepares an event update considering the difference of the original recurrence rule only.
     * 
     * @param originalRecurrenceRule The recurrence rule of the original series master event
     * @param originalSplitFrom The original <code>X-OX-SPLIT-FROM</code> extended property
     * @param detachedSeriesMaster The detached series master event
     * @return The event update
     */
    protected static EventUpdate getDetachedSeriesUpdate(String originalRecurrenceRule, ExtendedProperty originalSplitFrom, Event detachedSeriesMaster) {
        Event previousSeriesMaster = new DelegatingEvent(detachedSeriesMaster) {

            @Override
            public String getRecurrenceRule() {
                return originalRecurrenceRule;
            }

            @Override
            public boolean containsRecurrenceRule() {
                return true;
            }

            @Override
            public boolean containsExtendedProperties() {
                return true;
            }

            @Override
            public ExtendedProperties getExtendedProperties() {
                ExtendedProperties extendedProperties = super.getExtendedProperties();
                if (null == originalSplitFrom) {
                    return extendedProperties;
                }
                extendedProperties = null == extendedProperties ? new ExtendedProperties() : new ExtendedProperties(extendedProperties);
                return addExtendedProperty(extendedProperties, originalSplitFrom, true);
            }
        };
        return new DefaultEventUpdate(previousSeriesMaster, detachedSeriesMaster);
    }

    /**
     * Gets a value indicating whether conflict checks should take place due to an attendee update or not, i.e. when one of the original
     * attendee's participation status implied a {@link Transp#TRANSPARENT}, while his updated does not.
     *
     * @param updatedAttendees The attendee updates
     * @return <code>true</code> if conflict checks should take place, <code>false</code>, otherwise
     */
    protected static boolean needsConflictCheck(List<? extends ItemUpdate<Attendee, AttendeeField>> updatedAttendees) {
        if (null != updatedAttendees && 0 < updatedAttendees.size()) {
            for (ItemUpdate<Attendee, AttendeeField> attendeeUpdate : updatedAttendees) {
                if (needsConflictCheck(attendeeUpdate.getOriginal(), attendeeUpdate.getUpdate())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets a value indicating whether conflict checks should take place due to an attendee update or not, i.e. when the original
     * attendee's participation status implied a {@link Transp#TRANSPARENT}, while the updated does not.
     *
     * @param originalAttendee The original attendee
     * @param updatedAttendee The updated attendee
     * @return <code>true</code> if conflict checks should take place, <code>false</code>, otherwise
     */
    protected static boolean needsConflictCheck(Attendee originalAttendee, Attendee updatedAttendee) {
        boolean wasTransparent = ParticipationStatus.DECLINED.matches(originalAttendee.getPartStat()) || originalAttendee.isHidden();
        boolean isTransparent = ParticipationStatus.DECLINED.matches(updatedAttendee.getPartStat()) || updatedAttendee.isHidden();
        return wasTransparent && false == isTransparent;
    }

}
