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

import static com.openexchange.chronos.common.CalendarUtils.extractEMailAddress;
import static com.openexchange.chronos.common.CalendarUtils.filterByField;
import static com.openexchange.chronos.common.CalendarUtils.filterByUid;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getEventID;
import static com.openexchange.chronos.common.CalendarUtils.getEventsByUID;
import static com.openexchange.chronos.common.CalendarUtils.getFields;
import static com.openexchange.chronos.common.CalendarUtils.getObjectIDs;
import static com.openexchange.chronos.common.CalendarUtils.getResourceCalendarId;
import static com.openexchange.chronos.common.CalendarUtils.isExternalUser;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isResourceOrRoom;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.sortSeriesMasterFirst;
import static com.openexchange.chronos.common.SearchUtils.getSearchTerm;
import static com.openexchange.chronos.impl.Check.requireCalendarPermission;
import static com.openexchange.chronos.impl.Utils.getCalendarUserId;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.getFolderIdTerm;
import static com.openexchange.chronos.impl.Utils.isSameMailDomain;
import static com.openexchange.folderstorage.Permission.NO_PERMISSIONS;
import static com.openexchange.folderstorage.Permission.READ_ALL_OBJECTS;
import static com.openexchange.folderstorage.Permission.READ_FOLDER;
import static com.openexchange.folderstorage.Permission.READ_OWN_OBJECTS;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.arrays.Arrays.contains;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.DelegatingEvent;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.service.CalendarConfig;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventID;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.java.Strings;
import com.openexchange.search.CompositeSearchTerm;
import com.openexchange.search.CompositeSearchTerm.CompositeOperation;
import com.openexchange.search.SearchTerm;
import com.openexchange.search.SingleSearchTerm.SingleOperation;
import com.openexchange.search.internal.operands.ColumnFieldOperand;

/**
 * {@link ResolvePerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class ResolvePerformer extends AbstractQueryPerformer {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ResolvePerformer.class);

    /**
     * Initializes a new {@link ResolvePerformer}.
     *
     * @param session The calendar session
     * @param storage The underlying calendar storage
     */
    public ResolvePerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    /**
     * Performs the resolve by id operation.
     *
     * @param eventId The identifier of the event to resolve
     * @param sequence The expected sequence number to match, or <code>null</code> to resolve independently of the event's sequence number
     * @return The resolved event, or <code>null</code> if not found
     */
    public Event resolveById(String eventId, Integer sequence) throws OXException {
        return resolveById(eventId, sequence, session.getUserId());
    }

    /**
     * Performs the resolve by id operation.
     *
     * @param eventId The identifier of the event to resolve
     * @param sequence The expected sequence number to match, or <code>null</code> to resolve independently of the event's sequence number
     * @param calendarUserId The identifier of the calendar user the unique identifier should be resolved for
     * @return The resolved event, or <code>null</code> if not found
     * @throws OXException In case permissions are missing
     */
    public Event resolveById(String eventId, Integer sequence, int calendarUserId) throws OXException {
        /*
         * load event data, check permissions & apply folder identifier
         */
        Event event = storage.getEventStorage().loadEvent(eventId, null);
        if (null == event || null != sequence && i(sequence) != event.getSequence()) {
            return null;
        }
        event = storage.getUtilities().loadAdditionalEventData(calendarUserId, event, null);
        String folderId = CalendarUtils.getFolderView(event, calendarUserId);
        CalendarFolder folder = getFolder(session, folderId, false);
        if (false == hasReadPermission(event)) {
            if (false == matches(event.getCreatedBy(), session.getUserId())) {
                requireCalendarPermission(folder, READ_FOLDER, READ_ALL_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
            } else {
                requireCalendarPermission(folder, READ_FOLDER, READ_OWN_OBJECTS, NO_PERMISSIONS, NO_PERMISSIONS);
            }
        }
        return postProcessor().process(event, folder).getFirstEvent();
    }

    /**
     * Performs the resolve by uid operation.
     *
     * @param uid The unique identifier to resolve
     * @return The identifier of the resolved event, or <code>null</code> if not found
     */
    public String resolveByUid(String uid) throws OXException {
        /*
         * construct search term
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid))
            .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                .addSearchTerm(getSearchTerm(EventField.SERIES_ID, SingleOperation.ISNULL))
                .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, new ColumnFieldOperand<EventField>(EventField.SERIES_ID)))
            )
        ;
        /*
         * search for an event matching the UID & verify equality via String#equals
         */
        List<Event> events = filterByUid(storage.getEventStorage().searchEvents(searchTerm, null, new EventField[] { EventField.ID, EventField.UID }), uid);
        if (1 < events.size()) {
            String conflictingIds = events.stream().map(Event::getId).collect(Collectors.joining(", "));
            Exception cause = new IllegalStateException("UID \"" + uid + "\" resolves to multiple events [" + conflictingIds + ']');
            throw CalendarExceptionCodes.UID_CONFLICT.create(cause, uid, "", conflictingIds);
        }
        return events.isEmpty() ? null : events.get(0).getId();
    }

    /**
     * Resolves an unique identifier within the scope of a specific calendar user, i.e. the unique identifier is resolved to events
     * residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     *
     * @param uid The unique identifier to resolve
     * @param calendarUserId The identifier of the calendar user the unique identifier should be resolved for
     * @return The identifier of the resolved event, or <code>null</code> if not found
     */
    public EventID resolveByUid(String uid, int calendarUserId) throws OXException {
        return resolveByUid(uid, null, calendarUserId);
    }
    
    /**
     * Looks up all events where a field matches a certain value within the scope of a specific calendar user, i.e. the lookup is done
     * across all events residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The found events will be <i>userized</i> to reflect the view of the calendar user on the events.
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param field The event field to perform the lookup for
     * @param value The value to match
     * @param calendarUserId The identifier of the calendar user the value should be matched for
     * @param tombstones <code>true</code> to lookup event tombstones, <code>false</code> to lookup regular events
     * @return The resolved events, or an empty list if none were found
     */
    public List<Event> lookupByField(EventField field, Object value, int calendarUserId, boolean tombstones) throws OXException {
        List<Event> events = lookupByField(field, value, calendarUserId, getFields(session, EventField.ATTENDEES, EventField.ORGANIZER), tombstones);
        return (tombstones ? postProcessor().processTombstones(events, calendarUserId) : postProcessor().process(events, calendarUserId)).getEvents();
    }

    public List<Event> resolveEventsByUID(String uid, int calendarUserId) throws OXException {
        return lookupByField(EventField.UID, uid, calendarUserId, false);
    }

    public List<Event> resolveDeletedEventsByUID(String uid, int calendarUserId) throws OXException {
        return lookupByField(EventField.UID, uid, calendarUserId, true);
    }

    /**
     * Looks up all events matching a UID within the scope of specific calendar users, i.e. the lookup is done across all events residing
     * in each user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The found events will be <i>userized</i> to reflect the view of the associated calendar user on the events. Event details are loaded
     * based on {@link CalendarParameters#PARAMETER_FIELDS}.
     *
     * @param uid The UID to match
     * @param calendarUserIds The identifiers of the calendar users the value should be matched for
     * @param fields The fields to retrieve from the storage, or <code>null</code> to load all event data
     * @return The resolved events, or an empty map if none were found
     */
    public Map<Integer, List<Event>> resolveEventsByUID(String uid, int[] calendarUserIds, EventField[] fields) throws OXException {
        return resolveByField(EventField.UID, uid, calendarUserIds, false, fields);
    }

    /**
     * Looks up all events where a field matches a certain value within the scope of specific calendar users, i.e. the lookup is done
     * across all events residing in each user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The found events will be <i>userized</i> and post-processed to reflect the individual view of the associated calendar user on his
     * events. Event details are loaded based on {@link CalendarParameters#PARAMETER_FIELDS}.
     *
     * @param field The event field to perform the lookup for
     * @param value The value to match
     * @param calendarUserIds The identifiers of the calendar users the value should be matched for
     * @param tombstones <code>true</code> to lookup event tombstones, <code>false</code> to lookup regular events
     * @param fields The fields to retrieve from the storage, or <code>null</code> to load all event data
     * @return The resolved events, or an empty map if none were found
     */
    private Map<Integer, List<Event>> resolveByField(EventField field, Object value, int[] calendarUserIds, boolean tombstones, EventField[] fields) throws OXException {
        Map<Integer, List<Event>> eventsPerCalendarUser = lookupByField(field, value, calendarUserIds, fields, tombstones);
        Map<Integer, List<Event>> processedEventsPerCalendarUser = new HashMap<Integer, List<Event>>(eventsPerCalendarUser.size());
        for (Entry<Integer, List<Event>> entry : eventsPerCalendarUser.entrySet()) {
            processedEventsPerCalendarUser.put(entry.getKey(), postProcess(entry.getValue(), i(entry.getKey()), tombstones));
        }
        return processedEventsPerCalendarUser;
    }

    public String resolveFolderIdByUID(String uid, int calendarUserId, boolean fallbackToDefault) throws OXException {
        EventField[] oldParameterFields = session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class);
        List<Event> resolvedEvents;
        try {
            session.set(CalendarParameters.PARAMETER_FIELDS, new EventField[] { EventField.FOLDER_ID });
            resolvedEvents = resolveEventsByUID(uid, calendarUserId);
        } finally {
            session.set(CalendarParameters.PARAMETER_FIELDS, oldParameterFields);
        }
        if (resolvedEvents.isEmpty()) {
            return fallbackToDefault ? session.getConfig().getDefaultFolderId(calendarUserId) : null;
        }
        return resolvedEvents.get(0).getFolderId();
    }

    /**
     * Looks up all events matching the given unique identifier within the scope of a specific calendar user, i.e. the unique identifier
     * is resolved to events residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The plain event data is received, i.e. no per-user data is loaded, and no post-processing takes place. However, the returned event
     * collections per calendar user will only contain those events that are actually resolved from their point of view.
     *
     * @param uid The unique identifier to resolve
     * @param calendarUserId The identifier of the calendar user the unique identifier should be resolved for
     * @param fields The event fields to load for the resolved events, or <code>null</code> to load all properties
     * @return The resolved events, or an empty list if none were found
     */
    public List<Event> lookupByUid(String uid, int calendarUserId, EventField... fields) throws OXException {
        return lookupByField(EventField.UID, uid, calendarUserId, fields, false);
    }
    
    /**
     * Looks up all events where a field matches a certain value within the scope of a specific calendar user, i.e. the lookup is done
     * across all events residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The plain event data is received, i.e. no per-user data is loaded, and no post-processing takes place. However, the returned event
     * collections per calendar user will only contain those events that are actually resolved from their point of view.
     *
     * @param field The event field to perform the lookup for
     * @param value The value to match
     * @param calendarUserId The identifier of the calendar user the value should be matched for
     * @param fields The event fields to load for the resolved events, or <code>null</code> to load all properties
     * @param tombstones <code>true</code> to lookup event tombstones, <code>false</code> to lookup regular events
     * @return The resolved events, or an empty list if none were found
     */
    public List<Event> lookupByField(EventField field, Object value, int calendarUserId, EventField[] fields, boolean tombstones) throws OXException {
        List<Event> events = lookupByField(field, value, new int[] { calendarUserId }, fields, tombstones).get(I(calendarUserId));
        return null == events ? Collections.emptyList() : events;
    }

    /**
     * Looks up all events where a field matches a certain value within the scope of specific calendar user(s), i.e. the lookup is done
     * across all events residing in each user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The plain event data is received, i.e. no per-user data is loaded, and no post-processing takes place. However, the returned event
     * collections per calendar user will only contain those events that are actually resolved from their point of view.
     *
     * @param field The event field to perform the lookup for
     * @param value The value to match
     * @param calendarUserIds The identifiers of the calendar users the value should be matched for
     * @param fields The event fields to load for the resolved events, or <code>null</code> to load all properties
     * @param tombstones <code>true</code> to lookup event tombstones, <code>false</code> to lookup regular events
     * @return The resolved events associated with each calendar user id, an empty map if none were found
     */
    public Map<Integer, List<Event>> lookupByField(EventField field, Object value, int[] calendarUserIds, EventField[] fields, boolean tombstones) throws OXException {
        if (null == calendarUserIds || 0 == calendarUserIds.length) {
            return Collections.emptyMap();
        }
        /*
         * lookup matching events in storage, as well as (necessary) calendar user attendee data
         */        
        EventField[] fieldsToQuery = getFields(fields, field, EventField.ID, EventField.SERIES_ID, EventField.RECURRENCE_ID, EventField.FOLDER_ID, EventField.UID, EventField.CALENDAR_USER);
        List<Event> matchingEvents = loadMatchingEvents(storage, field, value, fieldsToQuery, tombstones);
        if (null == matchingEvents || matchingEvents.isEmpty()) {
            return Collections.emptyMap();
        }
        if (false == contains(fieldsToQuery, EventField.ATTENDEES)) {
            Map<String, List<Attendee>> attendeesPerEvent = loadAttendeesPerEvent(storage, getObjectIDs(matchingEvents), calendarUserIds, tombstones);
            for (Event event : matchingEvents) {
                event.setAttendees(attendeesPerEvent.get(event.getId()));
            }
        }
        /*
         * resolve found events from each calendar user's perspective
         */
        return resolveEventsPerCalendarUser(matchingEvents, calendarUserIds);
    }

    /**
     * Associates a list of events with multiple calendar users based on each individual user's scope.
     * 
     * @param events The events to resolve per calendar user
     * @param calendarUserIds The calendar users to consider
     * @return The resolved events per calendar user
     */
    private Map<Integer, List<Event>> resolveEventsPerCalendarUser(List<Event> events, int[] calendarUserIds) throws OXException {
        Map<Integer, List<Event>> eventsPerCalendarUser = new HashMap<Integer, List<Event>>(calendarUserIds.length);
        for (Event event : events) {
            int copyCount = 0;
            for (int calendarUserId : calendarUserIds) {
                if (isResolvedForCalendarUser(event, calendarUserId)) {
                    Event toAdd = 0 == copyCount ? event : EventMapper.getInstance().copy(event, new Event(), (EventField[]) null);
                    com.openexchange.tools.arrays.Collections.put(eventsPerCalendarUser, I(calendarUserId), toAdd);
                    copyCount++;
                }
            }
        }
        return eventsPerCalendarUser;
    }

    /**
     * Gets a value indicating whether a specific event is considered as 'resolved' within the scope of a specific calendar user, i.e. it
     * is located in the user's <i>personal</i>, or in a <i>public</i> calendar folder.
     * 
     * @param event The event to check
     * @param calendarUserId The calendar user id to check
     * @return <code>true</code> if the event is considered as 'resolved' within the scope of the calendar user, <code>false</code>, otherwise
     */
    private boolean isResolvedForCalendarUser(Event event, int calendarUserId) throws OXException {
        if (null != event.getFolderId()) {
            /*
             * common folder identifier is assigned, consider 'resolved' in public or calendar user's private folder
             */
            CalendarFolder folder = getFolder(session, event.getFolderId(), false);
            return PublicType.getInstance().equals(folder.getType()) || folder.getCreatedBy() == calendarUserId;
        } 
        /*
         * group scheduled event with no common folder identifier assigned, consider 'resolved' if calendar user attends
         */
        Attendee attendee = find(event.getAttendees(), calendarUserId);
        if (null == attendee) {
            return false;
        }
        CalendarFolder folder = null;
        if (Strings.isNotEmpty(attendee.getFolderId())) {
            folder = getFolder(session, attendee.getFolderId(), false);
        } else if (CalendarUserType.RESOURCE.matches(attendee.getCuType())) {
            String resourceCalendarId = getResourceCalendarId(attendee.getEntity());
            folder = getFolder(session, resourceCalendarId, false, true);
        }
        return null != folder && false == PublicType.getInstance().equals(folder.getType()) && folder.getCreatedBy() == calendarUserId;
    }

    /**
     * Post-processes a list of events under the perspective of a certain calendar user.
     * 
     * @param events The events to post-process
     * @param forUser The identifier of the user to apply the parent folder identifier for
     * @param tombstones <code>true</code> if the events were loaded from the <i>tombstone</i> storage, false, otherwise
     * @return The post-processed events
     */
    private List<Event> postProcess(List<Event> events, int forUser, boolean tombstones) throws OXException {
        return (tombstones ? postProcessor().processTombstones(events, forUser) : postProcessor().process(events, forUser)).getEvents();
    }

    private static List<Event> loadMatchingEvents(CalendarStorage storage, EventField field, Object value, EventField[] fields, boolean tombstones) throws OXException {
        if (null == value || (value instanceof String) && Strings.isEmpty((String) value)) {
            return Collections.emptyList();
        }
        /*
         * construct search term & lookup matching events in storage
         */
        SearchTerm<?> searchTerm = getSearchTerm(field, SingleOperation.EQUALS, value);
        if (tombstones) {
            List<Event> matchingEvents = filterByField(storage.getEventStorage().searchEventTombstones(searchTerm, null, fields), field, value);
            return storage.getUtilities().loadAdditionalEventTombstoneData(matchingEvents, fields);
        }
        List<Event> matchingEvents = filterByField(storage.getEventStorage().searchEvents(searchTerm, null, fields), field, value);
        return storage.getUtilities().loadAdditionalEventData(-1, matchingEvents, fields);
    }
    
    private static Map<String, List<Attendee>> loadAttendeesPerEvent(CalendarStorage storage, String[] eventIds, int[] calendarUserIds, boolean tombstones) throws OXException {
        if (null == eventIds || 0 == eventIds.length || null == calendarUserIds || 0 == calendarUserIds.length) {
            return Collections.emptyMap();
        }
        if (tombstones) {
            return storage.getAttendeeStorage().loadAttendeeTombstones(eventIds, calendarUserIds);
        }
        return storage.getAttendeeStorage().loadAttendees(eventIds, calendarUserIds);
    }

    /**
     * Resolves an unique identifier and optional recurrence identifier pair within the scope of a specific calendar user, i.e. the unique
     * identifier is resolved to events residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     *
     * @param uid The unique identifier to resolve
     * @param recurrenceId The recurrence identifier to match, or <code>null</code> to resolve to non-recurring or series master events only
     * @param calendarUserId The identifier of the calendar user the unique identifier should be resolved for
     * @return The identifier of the resolved event, or <code>null</code> if not found
     */
    public EventID resolveByUid(String uid, RecurrenceId recurrenceId, int calendarUserId) throws OXException {
        if (Strings.isEmpty(uid)) {
            return null;
        }
        /*
         * construct search term & lookup matching events in storage
         */
        CompositeSearchTerm searchTerm;
        if (null == recurrenceId) {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid))
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(getSearchTerm(EventField.SERIES_ID, SingleOperation.ISNULL))
                    .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.EQUALS, new ColumnFieldOperand<EventField>(EventField.SERIES_ID)))
                );
        } else {
            searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
                .addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid))
                .addSearchTerm(new CompositeSearchTerm(CompositeOperation.OR)
                    .addSearchTerm(new CompositeSearchTerm(CompositeOperation.NOT).addSearchTerm(getSearchTerm(EventField.SERIES_ID, SingleOperation.ISNULL)))
                    .addSearchTerm(getSearchTerm(EventField.ID, SingleOperation.NOT_EQUALS, new ColumnFieldOperand<EventField>(EventField.SERIES_ID)))
                );
        }
        EventField[] fields = { EventField.ID, EventField.SERIES_ID, EventField.RECURRENCE_ID, EventField.FOLDER_ID, EventField.UID, EventField.CALENDAR_USER };
        List<Event> events = filterByUid(storage.getEventStorage().searchEvents(searchTerm, null, fields), uid);
        /*
         * load calendar user's attendee data for found events and resolve to first matching event for this folder's calendar user
         */
        Attendee calendarUserAttendee = new Attendee();
        calendarUserAttendee.setEntity(calendarUserId);
        Map<String, Attendee> attendeePerEvent = storage.getAttendeeStorage().loadAttendee(getObjectIDs(events), calendarUserAttendee, (AttendeeField[]) null);
        for (Event event : events) {
            /*
             * match recurrence if specified
             */
            if (null != recurrenceId && false == recurrenceId.matches(event.getRecurrenceId()) || null == recurrenceId && isSeriesException(event)) {
                continue;
            }
            if (null != event.getFolderId()) {
                /*
                 * common folder identifier is assigned, consider 'resolved' in non-shared folder
                 */
                CalendarFolder folder = getFolder(session, event.getFolderId(), false);
                if (false == SharedType.getInstance().equals(folder.getType())) {
                    return getEventID(event);
                }
            } else {
                /*
                 * group scheduled event with no common folder identifier assigned, consider 'resolved' if calendar user attends
                 */
                Attendee attendee = attendeePerEvent.get(event.getId());
                if (null != attendee) {
                    if (Strings.isNotEmpty(attendee.getFolderId())) {
                        return new EventID(attendee.getFolderId(), event.getId(), event.getRecurrenceId());
                    } else if (isResourceOrRoom(attendee)) {
                        return new EventID(getResourceCalendarId(attendee.getEntity()), event.getId(), event.getRecurrenceId());
                    }
                }
            }
        }
        /*
         * not found, otherwise
         */
        return null;
    }

    private List<Event> resolveByField(EventField field, CalendarFolder folder, String resourceName, EventField[] fields) throws OXException {
        /*
         * construct search term to lookup matching events in folder
         */
        CompositeSearchTerm searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
            .addSearchTerm(getFolderIdTerm(session, folder))
            .addSearchTerm(getSearchTerm(field, SingleOperation.EQUALS, resourceName))
        ;
        /*
         * return matching events
         */
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, null, fields);
        events = storage.getUtilities().loadAdditionalEventData(folder.getCalendarUserId(), events, fields);
        return sortSeriesMasterFirst(events);
    }

    /**
     * Resolves a specific event (and any overridden instances or <i>change exceptions</i>) by its externally used resource name, which
     * typically matches the event's UID or filename property. The lookup is performed within a specific folder in a case-sensitive way.
     * If an event series with overridden instances is matched, the series master event will be the first event in the returned list.
     * <p/>
     * It is also possible that only overridden instances of an event series are returned, which may be the case for <i>detached</i>
     * instances where the user has no access to the corresponding series master event.
     * <p/>
     * The following calendar parameters are evaluated:
     * <ul>
     * <li>{@link CalendarParameters#PARAMETER_FIELDS}</li>
     * </ul>
     *
     * @param folderId The identifier of the folder to resolve the resource name in
     * @param resourceName The resource name to resolve
     * @return The resolved event(s), or <code>null</code> if no matching event was found
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-4.1">RFC 4791, section 4.1</a>
     */
    public EventsResult resolve(String folderId, String resourceName) throws OXException {
        /*
         * resolve by UID or filename
         */
        CalendarFolder folder = getFolder(session, folderId);
        EventField[] fields = getFields(session, EventField.ORGANIZER, EventField.ATTENDEES);
        List<Event> events = resolveByField(EventField.UID, folder, resourceName, fields);
        if (null == events || events.isEmpty()) {
            events = resolveByField(EventField.FILENAME, folder, resourceName, fields);
            if (null == events || events.isEmpty()) {
                return null;
            }
        }
        return getResult(folder, events);
    }

    /**
     * Resolves multiple events (and any overridden instances or <i>change exceptions</i>) by their externally used resource name, which
     * typically matches the event's UID or filename property. The lookup is performed within a specific folder in a case-sensitive way.
     * If an event series with overridden instances is matched, the series master event will be the first event in the returned list of
     * the corresponding events result.
     * <p/>
     * It is also possible that only overridden instances of an event series are returned, which may be the case for <i>detached</i>
     * instances where the user has no access to the corresponding series master event.
     *
     * @param folderId The identifier of the folder to resolve the resource names in
     * @param resourceNames The resource names to resolve
     * @return The resolved event(s), mapped to their corresponding resource name
     * @see <a href="https://tools.ietf.org/html/rfc4791#section-4.1">RFC 4791, section 4.1</a>
     */
    public Map<String, EventsResult> resolve(String folderId, List<String> resourceNames) throws OXException {
        if (null == resourceNames || resourceNames.isEmpty()) {
            return Collections.emptyMap();
        }
        CalendarFolder folder = getFolder(session, folderId);
        /*
         * construct search term to find events and overridden instances in folder by uid
         */
        SearchTerm<?> searchTerm;
        if (1 == resourceNames.size()) {
            searchTerm = getSearchTerm(EventField.UID, SingleOperation.EQUALS, resourceNames.get(0));
        } else {
            CompositeSearchTerm orTerm = new CompositeSearchTerm(CompositeOperation.OR);
            for (String uid : resourceNames) {
                orTerm.addSearchTerm(getSearchTerm(EventField.UID, SingleOperation.EQUALS, uid));
            }
            searchTerm = orTerm;
        }
        //        searchTerm = new CompositeSearchTerm(CompositeOperation.AND)
        //            .addSearchTerm(getFolderIdTerm(session, folder))
        //            .addSearchTerm(searchTerm)
        //        ;
        /*
         * perform search & map resulting events by UID
         */
        EventField[] fields = getFields(session, EventField.ORGANIZER, EventField.ATTENDEES);
        List<Event> events = storage.getEventStorage().searchEvents(searchTerm, null, fields);
        events = storage.getUtilities().loadAdditionalEventData(getCalendarUserId(folder), events, fields);
        Map<String, List<Event>> eventsByUID = getEventsByUID(events, false);
        /*
         * generate appropriate result for each requested resource name
         */
        Map<String, EventsResult> resultsPerResourceName = new HashMap<String, EventsResult>(resourceNames.size());
        for (String resourceName : resourceNames) {
            EventsResult result = getResult(folder, eventsByUID.get(resourceName));
            if (null == result) {
                /*
                 * try and resolve resource name manually as fallback; mark as not found, otherwise
                 */
                result = resolve(folderId, resourceName);
                if (null == result) {
                    result = new DefaultEventsResult(CalendarExceptionCodes.EVENT_NOT_FOUND_IN_FOLDER.create(folderId, resourceName));
                }
            }
            resultsPerResourceName.put(resourceName, result);
        }
        return resultsPerResourceName;
    }

    private EventsResult getResult(CalendarFolder folder, List<Event> events) {
        events = sortSeriesMasterFirst(events);
        if (null == events || events.isEmpty()) {
            return null;
        }
        for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
            Event event = iterator.next();
            if (false == Utils.isInFolder(event, folder) || false == Utils.isVisible(folder, event)) {
                iterator.remove();
            }
        }
        if (events.isEmpty()) {
            return null;
        }
        try {
            return postProcessor().process(events, folder).getEventsResult();
        } catch (OXException e) {
            return new DefaultEventsResult(e);
        }
    }

    /**
     * Looks up and injects attendee from copies of the same group-scheduled event located in calendar folders of other internal users.
     * <p/>
     * Matching is attempted for other attendees in externally organized events, based on the event's UID, recurrence-identifier and
     * same sequence number.
     *
     * @param event The event to inject data for known internal attendees in
     * @param folder The calendar folder representing the current view on the event
     * @return <code>true</code> if the event was enriched successfully, <code>false</code>, otherwise
     * @see CalendarConfig#isLookupPeerAttendeesForSameMailDomainOnly()
     */
    public boolean injectKnownAttendeeData(Event event, CalendarFolder folder) {
        if (null == event.getAttendees() || null == event.getOrganizer() || PublicType.getInstance().equals(folder.getType()) ||
            isInternal(event.getOrganizer(), CalendarUserType.INDIVIDUAL)) {
            return false;
        }
        Attendee calendarUserAttendee = find(event.getAttendees(), folder.getCalendarUserId());
        if (null == calendarUserAttendee) {
            return false;
        }
        boolean modified = false;
        for (ListIterator<Attendee> iterator = event.getAttendees().listIterator(); iterator.hasNext();) {
            Attendee attendee = iterator.next();
            /*
             * try and resolve external user attendee to internal user entity
             */
            if (false == isExternalUser(attendee) || matches(event.getOrganizer(), attendee) || matches(calendarUserAttendee, attendee)) {
                continue; // already internal user, external organizer, or the user himself
            }
            if (session.getConfig().isLookupPeerAttendeesForSameMailDomainOnly() &&
                false == isSameMailDomain(extractEMailAddress(attendee.getUri()), extractEMailAddress(calendarUserAttendee.getUri()))) {
                continue;
            }
            try {
                Attendee resolvedAttendee = session.getEntityResolver().prepare(
                    AttendeeMapper.getInstance().copy(attendee, null, (AttendeeField[]) null), CalendarUserType.INDIVIDUAL);
                if (isInternal(resolvedAttendee)) {
                    /*
                     * take over attendee data from corresponding calendar user's event copy if available
                     */
                    Attendee matchingAttendeeCopy = resolveFromAttendeeCopy(event, resolvedAttendee.getEntity());
                    if (null != matchingAttendeeCopy) {
                        LOG.debug("Injecting known attendee data {} from calendar {} for {} in {}", matchingAttendeeCopy, matchingAttendeeCopy.getFolderId(), attendee, event);
                        iterator.set(AttendeeMapper.getInstance().copy(matchingAttendeeCopy, resolvedAttendee, AttendeeField.PARTSTAT, AttendeeField.COMMENT));
                    }
                }
            } catch (OXException e) {
                LOG.warn("Error injecting known participation status for {} in {}", attendee, event, e);
            }
        }
        return modified;
    }

    /**
     * Looks up attendee data from copies of the same group-scheduled event located in calendar folders of other internal users.
     * <p/>
     * Matching is attempted for other attendees in externally organized events, based on the event's UID, recurrence-identifier and
     * same sequence number.
     *
     * @param event The event to inject data for known internal attendees in
     * @param folder The calendar folder representing the current view on the event
     * @return A new list of the event's attendees, with possibly injected data from other event copies, or the original attendee list if not applicable
     * @see CalendarConfig#isLookupPeerAttendeesForSameMailDomainOnly()
     */
    public List<Attendee> resolveFromAttendeeCopies(Event event, CalendarFolder folder) {
        if (null == event.getAttendees()) {
            return null;
        }
        List<Attendee> attendees = new ArrayList<Attendee>(event.getAttendees());
        Event delegate = new DelegatingEvent(event) {

            @Override
            public List<Attendee> getAttendees() {
                return attendees;
            }
        };
        return injectKnownAttendeeData(delegate, folder) ? attendees : event.getAttendees();
    }

    /**
     * Looks up and loads attendee data from a possible copy of a group-scheduled event located in this attendee's personal calendar
     * folder, for the given event data.
     * <p/>
     * Matching is performed based on the event's UID, recurrence-identifier and same sequence number.
     *
     * @param event The event to resolve the attendee data for
     * @param calendarUserId The identifier of the user to resolve the event for
     * @return The attendee data, or <code>null</code> if no matching event copy was found for the calendar user
     * @see #resolveByUid(String, RecurrenceId, int)
     * @see CalendarConfig#isLookupPeerAttendeesForSameMailDomainOnly()
     */
    public Attendee resolveFromAttendeeCopy(Event event, int calendarUserId) throws OXException {
        /*
         * resolve by event uid / recurrence id
         */
        EventID eventId = resolveByUid(event.getUid(), event.getRecurrenceId(), calendarUserId);
        if (null == eventId) {
            return null; // no event copy found for this attendee
        }
        /*
         * cross-check identifying properties of attendee's copy of the group-scheduled event
         */
        String objectId = eventId.getObjectID();
        Event attendeeCopy = storage.getEventStorage().loadEvent(objectId, new EventField[] { EventField.UID, EventField.RECURRENCE_ID, EventField.SEQUENCE, EventField.ORGANIZER });
        if (null == attendeeCopy || false == event.getUid().equals(attendeeCopy.getUid()) || false == matches(event.getRecurrenceId(), attendeeCopy.getRecurrenceId()) ||
            event.getSequence() != attendeeCopy.getSequence() || false == matches(event.getOrganizer(), attendeeCopy.getOrganizer())) {
            return null; // different revision of event
        }
        /*
         * load data for this attendee's event copy
         */
        Attendee userAttendee = session.getEntityResolver().prepareUserAttendee(calendarUserId);
        return storage.getAttendeeStorage().loadAttendee(new String[] { objectId }, userAttendee, (AttendeeField[]) null).get(objectId);
    }

}
