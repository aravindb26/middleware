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
import static com.openexchange.chronos.common.CalendarUtils.getFolderView;
import static com.openexchange.chronos.common.CalendarUtils.isClassifiedFor;
import static com.openexchange.chronos.common.CalendarUtils.isFirstOccurrence;
import static com.openexchange.chronos.common.CalendarUtils.isInRange;
import static com.openexchange.chronos.common.CalendarUtils.isLastOccurrence;
import static com.openexchange.chronos.common.CalendarUtils.isPublicClassification;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesException;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.sortEvents;
import static com.openexchange.chronos.impl.Utils.NON_CLASSIFIED_FIELDS;
import static com.openexchange.chronos.impl.Utils.applyExceptionDates;
import static com.openexchange.chronos.impl.Utils.getFolder;
import static com.openexchange.chronos.impl.Utils.getFrom;
import static com.openexchange.chronos.impl.Utils.getLocale;
import static com.openexchange.chronos.impl.Utils.getTimeZone;
import static com.openexchange.chronos.impl.Utils.getUntil;
import static com.openexchange.chronos.impl.Utils.isBookingDelegate;
import static com.openexchange.chronos.impl.Utils.isParticipating;
import static com.openexchange.chronos.impl.Utils.isResolveOccurrences;
import static com.openexchange.chronos.impl.Utils.isResourceCalendarFolder;
import static com.openexchange.chronos.impl.Utils.isVisible;
import static com.openexchange.chronos.impl.Utils.setPrivateSummary;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.EventFlag;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.common.SelfProtectionFactory.SelfProtection;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.Check;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.impl.groupware.StorageUpdater;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.RecurrenceData;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link EventPostProcessor}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class EventPostProcessor {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EventPostProcessor.class);

    private final CalendarSession session;
    private final CalendarStorage storage;
    private final EventField[] requestedFields;
    private final Map<String, RecurrenceData> knownRecurrenceData;
    private final SelfProtection selfProtection;

    private Set<String> eventIdsWithAttachment;
    private Set<String> eventIdsWithConference;
    private Set<String> alarmTriggersPerEventId;
    private Map<String, Integer> attendeeCountsPerEventId;
    private Map<String, Attendee> userAttendeePerEventId;
    private Set<String> eventIdsWhereAllOthersDeclined;

    private long maximumTimestamp;
    private List<Event> events;
    private CalendarFolderChooser folderChooser;

    /**
     * Initializes a new {@link EventPostProcessor}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     * @param selfProtection A reference to the self protection utility
     */
    public EventPostProcessor(CalendarSession session, CalendarStorage storage, SelfProtection selfProtection) {
        super();
        this.session = session;
        this.storage = storage;
        this.selfProtection = selfProtection;
        this.requestedFields = session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class);
        this.knownRecurrenceData = new HashMap<String, RecurrenceData>();
        reset();
    }

    /**
     * Sets a map holding additional hints to assign the {@link EventFlag#ATTACHMENTS} when processing the events.
     *
     * @param eventIdsWithAttachment A set holding the identifiers of those events where at least one attachment stored
     * @return A self reference
     */
    EventPostProcessor setAttachmentsFlagInfo(Set<String> eventIdsWithAttachment) {
        this.eventIdsWithAttachment = eventIdsWithAttachment;
        return this;
    }

    /**
     * Sets additional hints to assign the {@link EventFlag#CONFERENCES} when processing the events.
     *
     * @param eventIdsWithAttachment A set holding the identifiers of those events where at least one conference stored
     * @return A self reference
     */
    EventPostProcessor setConferencesFlagInfo(Set<String> eventIdsWithConference) {
        this.eventIdsWithConference = eventIdsWithConference;
        return this;
    }

    /**
     * Sets additional hints to assign the {@link EventFlag#ALARMS} when processing the events.
     *
     * @param alarmTriggersPerEventId A set holding the identifiers of those events where at least one alarm trigger is stored for the user
     * @return A self reference
     */
    EventPostProcessor setAlarmsFlagInfo(Set<String> alarmTriggersPerEventId) {
        this.alarmTriggersPerEventId = alarmTriggersPerEventId;
        return this;
    }

    /**
     * Sets a map holding additional hints to assign the {@link EventFlag#SCHEDULED} when processing the events.
     *
     * @param attendeeCountsPerEventId The number of attendees, mapped to the identifiers of the corresponding events
     * @return A self reference
     */
    EventPostProcessor setScheduledFlagInfo(Map<String, Integer> attendeeCountsPerEventId) {
        this.attendeeCountsPerEventId = attendeeCountsPerEventId;
        return this;
    }

    /**
     * Sets a map holding essential information about the calendar user attendee when processing the events.
     *
     * @param userAttendeePerEventId The calendar user attendees, mapped to the identifiers of the corresponding events
     * @return A self reference
     */
    EventPostProcessor setUserAttendeeInfo(Map<String, Attendee> userAttendeePerEventId) {
        this.userAttendeePerEventId = userAttendeePerEventId;
        return this;
    }

    /**
     * Sets additional hints to assign the {@link EventFlag#ALL_OTHERS_DECLINED} when processing the events.
     *
     * @param eventIdsWhereAllOthersDeclined A set holding the identifiers of those events where all other attendees have declined
     * @return A self reference
     */
    EventPostProcessor setAllOthersDeclinedFlagInfo(Set<String> eventIdsWhereAllOthersDeclined) {
        this.eventIdsWhereAllOthersDeclined = eventIdsWhereAllOthersDeclined;
        return this;
    }

    /**
     * Post-processes a list of events prior returning it to the client. This includes
     * <ul>
     * <li>excluding or anonymizing events that are classified for the current user</li>
     * <li>excluding events that are not within the requested range</li>
     * <li>applying the folder identifier from the passed folder</li>
     * <li>generate and apply event flags</li>
     * <li>resolving occurrences of the series master event as per {@link Utils#isResolveOccurrences(com.openexchange.chronos.service.CalendarParameters)}</li>
     * <li>apply <i>userized</i> versions of change- and delete-exception dates in the series master event based on the calendar user's actual attendance</li>
     * <li>sorting the resulting event list based on the requested sort options</li>
     * </ul>
     *
     * @param events The events to post-process
     * @param inFolder The parent folder representing the view on the events
     * @return A self reference
     */
    public EventPostProcessor process(Collection<Event> events, CalendarFolder inFolder) throws OXException {
        return process(events, inFolder, false);
    }

    /**
     * Post-processes a list of events prior returning it to the client. This includes
     * <ul>
     * <li>excluding or anonymizing events that are classified for the current user</li>
     * <li>excluding events that are not within the requested range</li>
     * <li>applying the folder identifier from the passed folder</li>
     * <li>generate and apply event flags</li>
     * <li>optionally customize the event if applicable</li>
     * <li>resolving occurrences of the series master event as per {@link Utils#isResolveOccurrences(com.openexchange.chronos.service.CalendarParameters)}</li>
     * <li>apply <i>userized</i> versions of change- and delete-exception dates in the series master event based on the calendar user's actual attendance</li>
     * <li>sorting the resulting event list based on the requested sort options</li>
     * </ul>
     *
     * @param events The events to post-process
     * @param inFolder The parent folder representing the view on the events
     * @param skipAnonymized <code>true</code> to skip <i>anonymized</i> events, <code>false</code>, otherwise
     * @return A self reference
     */
    public EventPostProcessor process(Collection<Event> events, CalendarFolder inFolder, boolean skipAnonymized) throws OXException {
        for (Event event : events) {
            doProcess(injectUserAttendeeData(event), inFolder, skipAnonymized);
            checkResultSizeNotExceeded();
        }
        return this;
    }

    /**
     * Post-processes a list of event tombstones prior returning it to the client. This includes
     * <ul>
     * <li>excluding or anonymizing events that are classified for the current user</li>
     * <li>excluding events that are not within the requested range</li>
     * <li>applying the folder identifier from the passed folder</li>
     * <li>sorting the resulting event list based on the requested sort options</li>
     * </ul>
     *
     * @param events The events to post-process
     * @param inFolder The parent folder representing the view on the events
     * @return A self reference
     */
    public EventPostProcessor processTombstones(Collection<Event> events, CalendarFolder inFolder) throws OXException {
        for (Event event : events) {
            doProcessTombstone(injectUserAttendeeData(event), inFolder.getId());
            checkResultSizeNotExceeded();
        }
        return this;
    }

    /**
     * Post-processes an event prior returning it to the client. This includes
     * <ul>
     * <li>excluding or anonymizing events that are classified for the current user</li>
     * <li>excluding events that are not within the requested range</li>
     * <li>applying the folder identifier from the passed folder</li>
     * <li>generate and apply event flags</li>
     * <li>resolving occurrences of the series master event as per {@link Utils#isResolveOccurrences(com.openexchange.chronos.service.CalendarParameters)}</li>
     * <li>apply <i>userized</i> versions of change- and delete-exception dates in the series master event based on the calendar user's actual attendance</li>
     * <li>sorting the resulting event list based on the requested sort options</li>
     * </ul>
     *
     * @param event The event to post-process
     * @param inFolder The parent folder representing the view on the event
     * @return A self reference
     */
    public EventPostProcessor process(Event event, CalendarFolder inFolder) throws OXException {
        doProcess(injectUserAttendeeData(event), inFolder);
        checkResultSizeNotExceeded();
        return this;
    }

    /**
     * Post-processes a list of events prior returning it to the client. This includes
     * <ul>
     * <li>excluding or anonymizing events that are classified for the current user</li>
     * <li>excluding events that are not within the requested range</li>
     * <li>selecting the appropriate parent folder identifier for the specific user</li>
     * <li>generate and apply event flags</li>
     * <li>resolving occurrences of the series master event as per {@link Utils#isResolveOccurrences(com.openexchange.chronos.service.CalendarParameters)}</li>
     * <li>apply <i>userized</i> versions of change- and delete-exception dates in the series master event based on the user's actual attendance</li>
     * <li>sorting the resulting event list based on the requested sort options</li>
     * </ul>
     *
     * @param events The events to post-process
     * @param forUser The identifier of the user to apply the parent folder identifier for
     * @return A self reference
     */
    public EventPostProcessor process(Collection<Event> events, int forUser) throws OXException {
        List<Entry<Event, OXException>> warnings = null;
        for (Event event : events) {
            String folderId = getFolderView(injectUserAttendeeData(event), forUser);
            CalendarFolder folder;
            try {
                folder = getFolder(session, folderId, false, true);
            } catch (OXException e) {
                if (CalendarExceptionCodes.FOLDER_NOT_FOUND.equals(e)) {
                    if (null == warnings) {
                        warnings = new ArrayList<Map.Entry<Event,OXException>>();
                    }
                    warnings.add(new AbstractMap.SimpleEntry<Event, OXException>(event, e));
                    continue;
                }
                throw e;
            }
            doProcess(event, folder);
            checkResultSizeNotExceeded();
        }
        resolveWarnings(warnings);
        return this;
    }

    /**
     * Post-processes a list of event tombstones prior returning it to the client. This includes
     * <ul>
     * <li>excluding or anonymizing events that are classified for the current user</li>
     * <li>excluding events that are not within the requested range</li>
     * <li>selecting the appropriate parent folder identifier for the specific user</li>
     * <li>sorting the resulting event list based on the requested sort options</li>
     * </ul>
     *
     * @param events The events to post-process
     * @param forUser The identifier of the user to apply the parent folder identifier for
     * @return A self reference
     */
    public EventPostProcessor processTombstones(Collection<Event> events, int forUser) throws OXException {
        for (Event event : events) {
            event = injectUserAttendeeData(event);
            String folderId;
            try {
                folderId = getFolderView(injectUserAttendeeData(event), forUser);
            } catch (OXException e) {
                /*
                 * orphaned folder information in tombstone event, add warning but continue
                 */
                session.addWarning(e);
                continue;
            }
            doProcessTombstone(event, folderId);
            checkResultSizeNotExceeded();
        }
        return this;
    }

    /**
     * Gets a list of all previously processed events, sorted based on the requested sort options, within an events result structure.
     *
     * @return The events result
     */
    public EventsResult getEventsResult() throws OXException {
        return new DefaultEventsResult(getEvents(), getMaximumTimestamp());
    }

    /**
     * Gets a list of all previously processed events, sorted based on the requested sort options.
     *
     * @return The sorted list of processed events, or an empty list there are none
     */
    public List<Event> getEvents() throws OXException {
        return getEvents(false);
    }

    /**
     * Gets a list of all previously processed events, sorted based on the requested sort options.
     *
     * @param reset <code>true</code> to <i>reset</i> this post processor afterwards, <code>false</code>, otherwise
     * @return The sorted list of processed events, or an empty list there are none
     * @see #reset()
     */
    public List<Event> getEvents(boolean reset) throws OXException {
        List<Event> result = sortEvents(events, new SearchOptions(session).getSortOrders(), Utils.getTimeZone(session));
        if (reset) {
            reset();
        }
        return result;
    }

    /**
     * Gets the first event of the previously processed events.
     *
     * @return The first event, or <code>null</code> if there is none
     */
    public Event getFirstEvent() throws OXException {
        if (1 == events.size()) {
            return events.get(0);
        }
        List<Event> events = getEvents();
        return events.isEmpty() ? null : events.get(0);
    }

    /**
     * Resets the internal list of resulting events and the maximum timestamp.
     */
    public void reset() {
        events = new ArrayList<Event>();
        maximumTimestamp = 0L;
    }

    /**
     * Gets the maximum timestamp of the processed events.
     *
     * @return The maximum timestamp, or <code>0</code> if none were processed
     */
    public long getMaximumTimestamp() {
        return maximumTimestamp;
    }

    private boolean doProcess(Event event, CalendarFolder folder) throws OXException {
        return doProcess(event, folder, false);
    }

    private boolean doProcess(Event event, CalendarFolder folder, boolean skipAnonymized) throws OXException {
        if (Classification.PRIVATE.equals(event.getClassification()) && 
            (false == PrivateType.getInstance().equals(folder.getType()) || folder.getCalendarUserId() != session.getUserId())) {
            /*
             * excluded if classified as private for the session user
             */
            return false;
        }
        Attendee attendee = find(event.getAttendees(), folder.getCalendarUserId());
        if (null != attendee) {
            if (attendee.isHidden()) {
                /*
                 * excluded if marked as hidden for the calendar user
                 */
                //TODO: public folder?
                return false;
            }
            /*
             * overwrite transparency if user specific value is set
             */
            if (null != attendee.getTransp()) {
                event.setTransp(attendee.getTransp());
            }
        }
        if (isSeriesMaster(event)) {
            knownRecurrenceData.put(event.getSeriesId(), new DefaultRecurrenceData(event));
        }
        if (null == requestedFields || Arrays.contains(requestedFields, EventField.ATTENDEES)) {
            /*
             * inject known data from other attendee copies of the same event
             */
            new ResolvePerformer(session, storage).injectKnownAttendeeData(event, folder);
        }
        /*
         * anonymize event data as needed ("private" appointments, slots in resource calendars)
         */
        if (anonymizeIfNeeded(event, folder) && skipAnonymized) {
            return false;
        }
        /*
         * assign folder view & derive event flags
         */
        event.setFolderId(folder.getId());
        if (null == requestedFields || Arrays.contains(requestedFields, EventField.FLAGS)) {
            event.setFlags(getFlags(event, folder));
        }
        if (isSeriesMaster(event)) {
            if (isResolveOccurrences(session)) {
                /*
                 * add resolved occurrences; no need to apply individual exception dates here, as a removed attendee can only occur in exceptions
                 */
                try {
                    if (events.addAll(resolveOccurrences(event))) {
                        maximumTimestamp = Math.max(maximumTimestamp, event.getTimestamp());
                        return true;
                    }
                } catch (OXException e) {
                    LOG.warn("Unexpected error resolving occurrences for {}", event, e);
                }
                return false;
            }
            if (getFrom(session) != null && getUntil(session) != null) {
                try {
                    if (false == session.getRecurrenceService().iterateEventOccurrences(event, getFrom(session), getUntil(session)).hasNext()) {
                        /*
                         * exclude series master event if there are no occurrences in requested range
                         */
                        return false;
                    }
                } catch (OXException e) {
                    LOG.warn("Unexpected error iterating recurrence data for {}", event, e);
                }
            }
            /*
             * apply 'userized' exception dates to series master as requested
             */
            maximumTimestamp = Math.max(maximumTimestamp, event.getTimestamp());
            if (null == requestedFields || Arrays.contains(requestedFields, EventField.CHANGE_EXCEPTION_DATES) ||
                Arrays.contains(requestedFields, EventField.DELETE_EXCEPTION_DATES)) {
                try {
                    return events.add(applyExceptionDates(storage, folder, event));
                } catch (OXException e) {
                    LOG.warn("Unexpected error applying userized exception dates for {} in {}", I(folder.getCalendarUserId()), event, e);
                }
            }
            return events.add(event);
        }
        if (null != event.getStartDate() && false == isInRange(event, getFrom(session), getUntil(session), getTimeZone(session))) {
            /*
             * excluded if not in requested range
             */
            return false;
        }
        maximumTimestamp = Math.max(maximumTimestamp, event.getTimestamp());
        return events.add(event);
    }

    private boolean doProcessTombstone(Event event, String folderId) throws OXException {
        if (Classification.PRIVATE.equals(event.getClassification()) && isClassifiedFor(event, session.getUserId())) {
            /*
             * excluded if classified as private for the session user
             */
            return false;
        }
        event.setFolderId(folderId);
        event = Utils.anonymizeIfNeeded(session, event);
        if (isSeriesMaster(event) && false == session.getRecurrenceService().iterateEventOccurrences(event, getFrom(session), getUntil(session)).hasNext()) {
            /*
             * exclude series master event if there are no occurrences in requested range
             */
            return false;
        } else if (null != event.getStartDate() && false == isInRange(event, getFrom(session), getUntil(session), getTimeZone(session))) {
            /*
             * excluded if not in requested range
             */
            return false;
        }
        maximumTimestamp = Math.max(maximumTimestamp, event.getTimestamp());
        return events.add(event);
    }

    protected EnumSet<EventFlag> getFlags(Event event, CalendarFolder folder) {
        /*
         * get default flags for event data & derive recurrence position info
         */
        EnumSet<EventFlag> flags = CalendarUtils.getFlags(event, folder.getCalendarUserId(), session.getUserId(), PublicType.getInstance().equals(folder.getType()));
        if (isSeriesException(event)) {
            RecurrenceData recurrenceData = optRecurrenceData(event);
            if (null != recurrenceData) {
                try {
                    if (isLastOccurrence(event.getRecurrenceId(), recurrenceData, session.getRecurrenceService())) {
                        flags.add(EventFlag.LAST_OCCURRENCE);
                    }
                    if (isFirstOccurrence(event.getRecurrenceId(), recurrenceData, session.getRecurrenceService())) {
                        flags.add(EventFlag.FIRST_OCCURRENCE);
                    }
                } catch (OXException e) {
                    LOG.warn("Unexpected error determining position in recurrence set for {} with {}", event, recurrenceData, e);
                }
            }
        }
        /*
         * inject additional flags based on available data
         */
        if (null != eventIdsWithAttachment && eventIdsWithAttachment.contains(event.getId())) {
            flags.add(EventFlag.ATTACHMENTS);
        }
        if (null != eventIdsWithConference && eventIdsWithConference.contains(event.getId())) {
            flags.add(EventFlag.CONFERENCES);
        }
        if (null != alarmTriggersPerEventId && alarmTriggersPerEventId.contains(event.getId())) {
            flags.add(EventFlag.ALARMS);
        }
        if (null != attendeeCountsPerEventId) {
            Integer attendeeCount = attendeeCountsPerEventId.get(event.getId());
            if (null != attendeeCount && 1 < i(attendeeCount)) {
                flags.add(EventFlag.SCHEDULED);
            }
        }
        if (flags.contains(EventFlag.SCHEDULED) && 
            null != eventIdsWhereAllOthersDeclined && eventIdsWhereAllOthersDeclined.contains(event.getId())) {
            flags.add(EventFlag.ALL_OTHERS_DECLINED);
        }
        return flags;
    }

    private RecurrenceData optRecurrenceData(Event event) {
        String seriesId = event.getSeriesId();
        if (null == seriesId) {
            return null;
        }
        if ((event.getRecurrenceId() instanceof RecurrenceData)) {
            return ((RecurrenceData) event.getRecurrenceId());
        }
        RecurrenceData recurrenceData = knownRecurrenceData.get(seriesId);
        if (null == recurrenceData) {
            EventField[] fields = new EventField[] { EventField.RECURRENCE_RULE, EventField.START_DATE, EventField.RECURRENCE_DATES, EventField.CHANGE_EXCEPTION_DATES, EventField.DELETE_EXCEPTION_DATES };
            Event seriesMaster = null;
            try {
                seriesMaster = storage.getEventStorage().loadEvent(seriesId, fields);
            } catch (OXException e) {
                LOG.warn("Unexpected error loading series master for {}", event, e);
            }
            if (null != seriesMaster) {
                recurrenceData = new DefaultRecurrenceData(seriesMaster);
                knownRecurrenceData.put(seriesId, recurrenceData);
            }
        }
        return recurrenceData;
    }

    private void checkResultSizeNotExceeded() throws OXException {
        if (null != selfProtection) {
            Check.resultSizeNotExceeded(selfProtection, events, requestedFields);
        }
    }

    private List<Event> resolveOccurrences(Event master) throws OXException {
        Date from = getFrom(session);
        Date until = getUntil(session);
        TimeZone timeZone = getTimeZone(session);
        Iterator<Event> itrerator = session.getRecurrenceService().iterateEventOccurrences(master, from, until);
        List<Event> list = new ArrayList<Event>();
        while (itrerator.hasNext()) {
            Event event = itrerator.next();
            if (isInRange(event, from, until, timeZone)) {
                list.add(event);
            }
        }
        return list;
    }

    /**
     * Injects essential information about the calendar user attendee prior processing the event, in case it is available.
     *
     * @param event The event to enrich with essential information about the calendar user attendee
     * @return The event, enriched with data about the calendar user attendee if available
     */
    private Event injectUserAttendeeData(Event event) {
        if (null != userAttendeePerEventId) {
            /*
             * inject data for attendee of underlying calendar user
             */
            Attendee attendee = userAttendeePerEventId.get(event.getId());
            if (null != attendee) {
                event.setAttendees(Collections.singletonList(attendee));
            }
        }
        return event;
    }

    /**
     * Tries to recover from warnings that occurred when processing certain events.
     *
     * @param warnings The warnings to resolve
     */
    private void resolveWarnings(List<Entry<Event, OXException>> warnings) {
        if (null == warnings || warnings.isEmpty()) {
            return;
        }
        Set<CalendarFolder> staleFolders = null;
        for (Entry<Event, OXException> entry : warnings) {
            OXException e = entry.getValue();
            Event event = entry.getKey();
            if (CalendarExceptionCodes.FOLDER_NOT_FOUND.equals(e) && null != e.getLogArgs() && 0 < e.getLogArgs().length) {
                /*
                 * stale folder, remember placeholder folder for further processing
                 */
                if (null == staleFolders) {
                    staleFolders = new HashSet<CalendarFolder>();
                }
                staleFolders.add(getPlaceholderFolder(event, String.valueOf(e.getLogArgs()[0])));
            }
        }
        if (null != staleFolders) {
            /*
             * purge event data with references to stale calendar folders
             */
            for (CalendarFolder staleFolder : staleFolders) {
                try {
                    int deleted = StorageUpdater.removeEventsInFolder(staleFolder);
                    LOG.info("Purged data for {} events with references to stale calendar folder {}.", I(deleted), staleFolder.getId());
                } catch (OXException e) {
                    LOG.error("Error removing events with stale references to folder {}", staleFolder, e);
                    session.addWarning(e);
                }
            }
        }
    }

    private CalendarFolderChooser getFolderChooser() {
        if (null == folderChooser) {
            folderChooser = new CalendarFolderChooser(session, storage);
        }
        return folderChooser;
    }

    /**
     * Re-loads the attendees that may be of relevance for the visibility of event details in certain folder views.
     * 
     * @param event The event to reload the attendees for
     * @param calendarUserId The identifier of the calendar user the events are processed for
     * @return The (possibly reloaded) attendees of relevance for further post-processing
     */
    private List<Attendee> reloadRelevantAttendees(Event event, int calendarUserId) throws OXException {
        List<Attendee> attendees = event.getAttendees();
        if (null == attendees || 1 == attendees.size() && matches(attendees.get(0), calendarUserId)) {
            List<Attendee> reloadedAttendees;
            if (null != event.getFolderId()) {
                /*
                 * not set or incomplete attendee list in public folder, try and add current session user if attending
                 */
                reloadedAttendees = storage.getAttendeeStorage().loadAttendees(new String[] { event.getId() }, new int[] { session.getUserId() }).get(event.getId());
            } else {
                /*
                 * not set or incomplete attendee list in event with individual folder views, reload attendees that may affect event visibility
                 */
                List<String> folderIds = getFolderChooser().getVisibleFolderIds(PrivateType.getInstance(), SharedType.getInstance());
                reloadedAttendees = storage.getAttendeeStorage().loadAttendees(event.getId(), folderIds, new AttendeeField[] { AttendeeField.ENTITY, AttendeeField.FOLDER_ID });
            }
            if (null != reloadedAttendees) {
                List<Attendee> relevantAttendees = new ArrayList<Attendee>();
                if (null != attendees) {
                    relevantAttendees.addAll(attendees);
                }
                relevantAttendees.addAll(reloadedAttendees);
                return relevantAttendees;
            }
        }
        return attendees;
    }

    /**
     * <i>Anonymizes</i> an event in case details should not get exposed to the current session user under the perspective of a specific
     * folder the event is being processed in.
     * <p/>
     * On the one hand, anonymization occurs if the event is not marked as {@link Classification#PUBLIC}, and the session's user does not
     * participate in the event. On the other hand, anonymization occurs if the event is rendered in a virtual resource folder, and no
     * other physical folder view on the event for the session user is available which would yield event details for him.
     * <p/>
     * After anonymization, the only the fields listed in {@link #NON_CLASSIFIED_FIELDS} will be retained in the event.
     *
     * @param event The event to anonymize
     * @param folder The folder view the event is post-processed for
     * @return <code>true</code> if anonymization took place, <code>false</code>, otherwise
     */
    private boolean anonymizeIfNeeded(Event event, CalendarFolder folder) {
        /*
         * quick check for usual non-classified event in regular folder with sufficient permissions
         */
        if (isPublicClassification(event) && isVisible(folder, event) && false == isResourceCalendarFolder(folder)) {
            return false;
        }
        /*
         * check if user participates, based on available data
         */
        if (isParticipating(session.getUserId(), event)) {
            return false;
        }
        try {
            /*
             * no anonymization of events in resource folders for their booking delegates
             */
            if (isResourceCalendarFolder(folder) && isBookingDelegate(session, folder.getCalendarUserId())) {
                return false;
            }
            /*
             * if unknown whether user participates, check if relevant attendee list was actually completely loaded before, then re-check
             */
            List<Attendee> attendees = reloadRelevantAttendees(event, folder.getCalendarUserId());
            if (contains(attendees, session.getUserId())) {
                return false;
            }
            /*
             * since user does not participate in event, ensure to always anonymize if event is marked as 'private'
             */
            if (isClassifiedFor(event, session.getUserId())) {
                EventMapper.getInstance().retain(event, new HashSet<EventField>(java.util.Arrays.asList(NON_CLASSIFIED_FIELDS)));
                setPrivateSummary(getLocale(session.getSession()), event);
                return true;
            }
            /*
             * for non-classified events, finally check if visible in any folder view
             */
            CalendarFolder chosenFolder = null != event.getFolderId() ? getFolderChooser().lookupFolder(event.getFolderId()) : 
                getFolderChooser().chooseAttendeeFolder(event, attendees);
            if (null != chosenFolder && isVisible(chosenFolder, event)) {
                return false; // event is visible in one of the folder views
            }
        } catch (OXException e) {
            LOG.warn("Unexpected error checking if details of {} are visible for user in {}, continuing with anonymization.", event, folder, e);
        }
        /*
         * anonymize event data (w/o special "Private" summary), otherwise
         */
        EventMapper.getInstance().retain(event, new HashSet<EventField>(java.util.Arrays.asList(NON_CLASSIFIED_FIELDS)));
        return true;
    }

    private CalendarFolder getPlaceholderFolder(Event event, String folderId) {
        try {
            FolderObject folder;
            if (folderId.equals(event.getFolderId())) {
                folder = new FolderObject("", Integer.parseInt(folderId), FolderObject.CALENDAR, FolderObject.PUBLIC, session.getUserId());
            } else {
                Attendee attendee = getAttendeeByFolder(event.getAttendees(), folderId);
                if (null == attendee) {
                    throw CalendarExceptionCodes.UNEXPECTED_ERROR.create("Can't find attendee with folder " + folderId);
                }
                folder = new FolderObject("", Integer.parseInt(folderId), FolderObject.CALENDAR, FolderObject.PRIVATE, attendee.getEntity());
            }
            OCLPermission permission = new OCLPermission(session.getUserId(), 0);
            permission.setFolderAdmin(true);
            permission.setGroupPermission(false);
            permission.setAllPermission(OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION);
            return new CalendarFolder(session.getSession(), folder, permission);
        } catch (Exception e) {
            LOG.warn("Unexpected error preparing placeholder folder", e);
            return null;
        }
    }

    private static Attendee getAttendeeByFolder(List<Attendee> attendees, String folderId) {
        if (null != attendees && 0 < attendees.size()) {
            for (Attendee attendee : attendees) {
                if (folderId.equals(attendee.getFolderId())) {
                    return attendee;
                }
            }
        }
        return null;
    }

}
