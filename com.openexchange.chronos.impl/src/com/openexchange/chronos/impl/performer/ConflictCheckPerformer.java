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

import static com.openexchange.chronos.common.CalendarUtils.add;
import static com.openexchange.chronos.common.CalendarUtils.asExternalEvents;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.isAttendee;
import static com.openexchange.chronos.common.CalendarUtils.isFloating;
import static com.openexchange.chronos.common.CalendarUtils.isGroupScheduled;
import static com.openexchange.chronos.common.CalendarUtils.isInRange;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isInternalUser;
import static com.openexchange.chronos.common.CalendarUtils.isOpaqueTransparency;
import static com.openexchange.chronos.common.CalendarUtils.isOrganizer;
import static com.openexchange.chronos.common.CalendarUtils.isPublicClassification;
import static com.openexchange.chronos.common.CalendarUtils.isSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.looksLikeSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.optEMailAddress;
import static com.openexchange.chronos.common.CalendarUtils.optFind;
import static com.openexchange.chronos.common.CalendarUtils.truncateTime;
import static com.openexchange.chronos.impl.Utils.isCheckConflicts;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.resolveAttendees;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.FreeBusyVisibility;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.common.EventOccurrence;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.CalendarFolder;
import com.openexchange.chronos.impl.EventConflictImpl;
import com.openexchange.chronos.impl.OccurrenceId;
import com.openexchange.chronos.impl.Utils;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.chronos.impl.session.CalendarConfigImpl;
import com.openexchange.chronos.service.CalendarConfig;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventConflict;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.Permission;
import com.openexchange.java.Reference;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.ThreadPools;

/**
 * {@link ConflictCheckPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class ConflictCheckPerformer extends AbstractFreeBusyPerformer {

    private final int maxConflictsPerRecurrence;
    private final int maxConflicts;
    private final int maxAttendeesPerConflict;
    private final int maxOccurrencesForConflicts;
    private final int maxSeriesUntilForConflicts;

    private final Date today;

    private Map<String, Permission> folderPermissions;

    /**
     * Initializes a new {@link ConflictCheckPerformer}.
     *
     * @param session The calendar session
     * @param storage The calendar storage
     * @throws OXException If timezone can't be determined
     */
    public ConflictCheckPerformer(CalendarSession session, CalendarStorage storage) throws OXException {
        super(session, storage);
        this.today = truncateTime(new Date(), Utils.getTimeZone(session));
        maxConflicts = session.getConfig().getMaxConflicts();
        maxAttendeesPerConflict = session.getConfig().getMaxAttendeesPerConflict();
        maxConflictsPerRecurrence = session.getConfig().getMaxConflictsPerRecurrence();
        maxOccurrencesForConflicts = session.getConfig().getMaxOccurrencesForConflicts();
        maxSeriesUntilForConflicts = session.getConfig().getMaxSeriesUntilForConflicts();
    }

    /**
     * Performs the conflict check.
     *
     * @param event The event being inserted/updated
     * @param attendees The event's list of attendees, or <code>null</code> in case of a not group-scheduled event
     * @return The conflicts, or an empty list if there are none
     * @throws OXException In case conflicts can't be calculated
     */
    public List<EventConflict> perform(Event event, List<Attendee> attendees) throws OXException {
        /*
         * check which attendees need to be checked
         */
        List<Attendee> attendeesToCheck = getAttendeesToCheck(event, attendees);
        if (attendeesToCheck.isEmpty()) {
            return Collections.emptyList();
        }
        /*
         * get conflicts for series or regular event
         */
        List<EventConflict> conflicts;
        if (isSeriesMaster(event) || null == event.getId() && null != event.getRecurrenceRule()) {
            conflicts = getSeriesConflicts(event, attendeesToCheck);
        } else {
            conflicts = getEventConflicts(event, attendeesToCheck);
        }
        return sortAndTrim(conflicts);
    }

    /**
     * Checks for conflicts for a single, non recurring event (or a single exception event of a series).
     *
     * @param event The event being inserted/updated
     * @param attendeesToCheck The internal attendees to check
     * @param attendees The event's list of attendees, or <code>null</code> in case of a not group-scheduled event
     * @return The conflicts, or an empty list if there are none
     */
    private List<EventConflict> getEventConflicts(Event event, List<Attendee> attendeesToCheck) throws OXException {
        /*
         * derive checked period (+/- one day to cover floating events in different timezone)
         */
        TimeZone eventTimeZone = isFloating(event) || null == event.getStartDate().getTimeZone() ? Utils.getTimeZone(session) : event.getStartDate().getTimeZone();
        Date from = add(new Date(event.getStartDate().getTimestamp()), Calendar.DATE, -1, eventTimeZone);
        Date until = add(new Date(event.getEndDate().getTimestamp()), Calendar.DATE, 1, eventTimeZone);
        if (today.after(until)) {
            return Collections.emptyList();
        }
        return calculateConflicts(event, attendeesToCheck, from, until, (e) -> isInRange(e, event, eventTimeZone));
    }

    /**
     * Checks for conflicts for a recurring event, considering every occurrence of the series.
     *
     * @param masterEvent The series master event being inserted/updated
     * @param attendeesToCheck The attendees to check
     * @return The conflicts, or an empty list if there are none
     */
    private List<EventConflict> getSeriesConflicts(Event masterEvent, List<Attendee> attendeesToCheck) throws OXException {
        /*
         * resolve checked occurrences for event series & derive checked period
         */
        Iterator<RecurrenceId> recurrenceIterator = session.getRecurrenceService().iterateRecurrenceIds(new DefaultRecurrenceData(masterEvent), today, null);
        if (false == recurrenceIterator.hasNext()) {
            return Collections.emptyList();
        }
        List<RecurrenceId> eventRecurrenceIds = new ArrayList<RecurrenceId>();
        eventRecurrenceIds.add(recurrenceIterator.next());
        Date from = new Date(eventRecurrenceIds.get(0).getValue().getTimestamp());
        long maxUntil = 0 < maxSeriesUntilForConflicts ? add(from, Calendar.YEAR, maxSeriesUntilForConflicts).getTime() : 0L;
        while (recurrenceIterator.hasNext()) {
            RecurrenceId recurrenceId = recurrenceIterator.next();
            eventRecurrenceIds.add(recurrenceId);
            if (0 < maxOccurrencesForConflicts && maxOccurrencesForConflicts <= eventRecurrenceIds.size() || 0 < maxUntil && maxUntil <= recurrenceId.getValue().getTimestamp()) {
                break; // limit of checked occurrences exceeded
            }
        }
        if (0 == eventRecurrenceIds.size()) {
            return Collections.emptyList();
        }
        long masterEventDuration = masterEvent.getEndDate().getTimestamp() - masterEvent.getStartDate().getTimestamp();
        Date until = new Date(eventRecurrenceIds.get(eventRecurrenceIds.size() - 1).getValue().getTimestamp() + masterEventDuration);
        /*
         * adjust checked period (+/- one day to cover floating events in different timezone)
         */
        TimeZone eventTimeZone = isFloating(masterEvent) || null == masterEvent.getStartDate().getTimeZone() ? Utils.getTimeZone(session) : masterEvent.getStartDate().getTimeZone();
        from = add(from, Calendar.DATE, -1, eventTimeZone);
        until = add(until, Calendar.DATE, 1, eventTimeZone);
        if (today.after(until)) {
            return Collections.emptyList();
        }
        return calculateConflicts(masterEvent, attendeesToCheck, from, until, (e) -> isInRangeOfSeries(eventRecurrenceIds, e, masterEventDuration));
    }

    /**
     * Checks for conflicts for a (recurring) event, considering only events that passes the given check
     *
     * @param event The series master event being inserted/updated
     * @param attendeesToCheck The internal attendees to check
     * @param from Lower boundary for the search
     * @param until Upper boundary for the search
     * @param isConflictingEvent A predicate defining which events can be considered conflicting
     * @return The conflicts, or an empty list if there are none
     */
    private List<EventConflict> calculateConflicts(Event event, List<Attendee> attendeesToCheck, Date from, Date until, Predicate<Event> isConflictingEvent) throws OXException {
        /*
         * Initiate cross-context lookup if applicable
         */
        Future<List<Event>> crossContextLookupFuture = null;
        Map<String, Attendee> attendeesByMailAdress = attendeesToCheck.stream().filter(a -> CalendarUtils.isExternalUser(a)).collect(Collectors.toMap(a -> optEMailAddress(a.getUri()), a -> a));
        if (false == attendeesByMailAdress.isEmpty()) {
            crossContextLookupFuture = ThreadPools.submitElseExecute(new CrossContextConflictCheckTask(Services.getServiceLookup(), attendeesByMailAdress, from, until));
        }
        /*
         * search for potentially conflicting events in period
         */
        List<Event> eventsInPeriod = new OverlappingEventsLoader(storage).loadEvents(attendeesToCheck, from, until, true);
        /*
         * if available, gather results from users in other contexts, too
         */
        if (null != crossContextLookupFuture) {
            try {
                eventsInPeriod.addAll(crossContextLookupFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create(e.getMessage(), e));
            }
        }
        /*
         * check against each event in period
         */
        Map<OccurrenceId, EventConflict> conflictsByOccurrenceId = new HashMap<OccurrenceId, EventConflict>();
        for (Event eventInPeriod : eventsInPeriod) {
            if (isSameEventGroup(event, eventInPeriod)) {
                continue;
            }
            /*
             * determine intersecting attendees
             */
            Reference<Boolean> hardConflict = new Reference<Boolean>(Boolean.FALSE);
            List<Attendee> conflictingAttendees = getConflictingAttendees(attendeesToCheck, eventInPeriod, hardConflict);
            if (isNullOrEmpty(conflictingAttendees)) {
                continue;
            }
            if (Boolean.FALSE.equals(hardConflict.getValue()) && false == considerForFreeBusy(eventInPeriod)) {
                continue; // exclude 'soft' conflicts for events classified as 'private' (but keep 'confidential' ones)
            }

            if (looksLikeSeriesMaster(eventInPeriod)) {
                /*
                 * expand & check all (non overridden) instances of event series in period
                 */
                long duration = eventInPeriod.getEndDate().getTimestamp() - eventInPeriod.getStartDate().getTimestamp();
                Date iterateFrom = new Date(from.getTime() - duration);
                Iterator<RecurrenceId> iterator = session.getRecurrenceService().iterateRecurrenceIds(new DefaultRecurrenceData(eventInPeriod), iterateFrom, until);
                for (int conflictCount = 0, occurrenceCount = 0; conflictCount < maxConflictsPerRecurrence && iterator.hasNext(); occurrenceCount++) {
                    if (0 < maxOccurrencesForConflicts && maxOccurrencesForConflicts <= occurrenceCount) {
                        break; // limit of checked occurrences exceeded
                    }
                    EventOccurrence occurrenceInPeriod = new EventOccurrence(eventInPeriod, iterator.next());
                    if (isConflictingEvent.test(occurrenceInPeriod)) {
                        trackConflict(conflictsByOccurrenceId, occurrenceInPeriod, hardConflict, conflictingAttendees);
                        conflictCount++;
                    }
                }
            } else {
                /*
                 * check against single event in period
                 */
                if (isConflictingEvent.test(eventInPeriod)) {
                    trackConflict(conflictsByOccurrenceId, eventInPeriod, hardConflict, conflictingAttendees);
                }
            }
        }
        return new ArrayList<>(conflictsByOccurrenceId.values());
    }

    /**
     * Tracks a conflicting event within an overall conflict result, mapped by occurrence id.
     * 
     * @param conflictsByOccurrenceId The overall collection of so far tracked event conflicts
     * @param event The conflicting event
     * @param conflictingAttendees The conflicting attendees to apply
     * @param hardConflict {@link Boolean#TRUE} to mark as <i>hard</i> conflict, {@link Boolean#FALSE} or <code>null</code>, otherwise
     */
    private void trackConflict(Map<OccurrenceId, EventConflict> conflictsByOccurrenceId, Event event, Reference<Boolean> hardConflict, List<Attendee> conflictingAttendees) throws OXException {
        /*
         * Check against existing event conflict and add attendee if needed
         */
        OccurrenceId occurrenceId = new OccurrenceId(event);
        EventConflict knownEvent = conflictsByOccurrenceId.get(occurrenceId);
        if (null != knownEvent) {
            if (maxAttendeesPerConflict > knownEvent.getConflictingAttendees().size()) {
                knownEvent.getConflictingAttendees().addAll(conflictingAttendees);
            }

        } else {
            /*
             * Add new event conflict
             */
            EventConflict eventConflict = getEventConflict(event, conflictingAttendees, hardConflict.getValue());
            conflictsByOccurrenceId.put(occurrenceId, eventConflict);
        }
    }

    /**
     * Gets a value indicating whether two events can be considered the same
     * by comparing their UIDs
     * 
     * @param eventToCheck The event to perform the conflict check for
     * @param eventInPeriod The event from the DB that is in the same period as the other event
     * @return <code>true</code> if the events can be considered to be the same (group), <code>false</code> otherwise
     */
    private boolean isSameEventGroup(Event eventToCheck, Event eventInPeriod) {
        return null != eventToCheck.getUid() && eventToCheck.getUid().equals(eventInPeriod.getUid());
    }

    /**
     * Gets a value indicating whether the given event matches is in range of a series represented by its recurrences
     *
     * @param eventRecurrenceIds The recurrences of the series
     * @param e The event to check
     * @param masterEventDuration The duration of the master event
     * @return <code>true</code> if the event is in range, <code>false</code> otherwise
     */
    private boolean isInRangeOfSeries(List<RecurrenceId> eventRecurrenceIds, Event e, long masterEventDuration) {
        for (RecurrenceId eventRecurrenceId : eventRecurrenceIds) {
            if (eventRecurrenceId.getValue().getTimestamp() >= e.getEndDate().getTimestamp()) {
                /*
                 * further occurrences are also "after" the checked event
                 */
                return false;
            } else if (eventRecurrenceId.getValue().getTimestamp() + masterEventDuration > e.getStartDate().getTimestamp()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates an event conflict for a single event.
     *
     * @param event The conflicting event
     * @param conflictingAttendees The conflicting attendees to apply
     * @param hardConflict {@link Boolean#TRUE} to mark as <i>hard</i> conflict, {@link Boolean#FALSE} or <code>null</code>, otherwise
     * @return The event conflict
     */
    private EventConflict getEventConflict(Event event, List<Attendee> conflictingAttendees, Boolean hardConflict) throws OXException {
        Event eventData = EventMapper.getInstance().copy(event, null, EventField.START_DATE, EventField.END_DATE, EventField.ID, EventField.SERIES_ID, EventField.RECURRENCE_ID, EventField.TRANSP, EventField.CREATED_BY);
        if (detailsVisible(event)) {
            eventData = EventMapper.getInstance().copy(event, eventData, EventField.SUMMARY, EventField.LOCATION);
            eventData.setFolderId(getFolderChooser().chooseFolderID(event));
        }
        return new EventConflictImpl(eventData, conflictingAttendees, null != hardConflict ? hardConflict.booleanValue() : false);
    }

    /**
     * Gets those attendees of a conflicting event that are actually part of the current conflict check, and do not have a participation
     * status of {@link ParticipationStatus#DECLINED} and free/busy visibility is not restricted for the current session user.
     *
     * @param attendeesToCheck The attendees where conflicts are checked for
     * @param conflictingEvent The conflicting event
     * @param hardConflict A reference that gets set to {@link Boolean#TRUE} if the conflicting attendees will indicate a <i>hard</i> conflict
     * @return The conflicting attendees, i.e. those checked attendees that also attend the conflicting event
     * @see AbstractFreeBusyPerformer#considerForFreeBusy(Event, Attendee)
     */
    private List<Attendee> getConflictingAttendees(Collection<Attendee> attendeesToCheck, Event conflictingEvent, Reference<Boolean> hardConflict) {
        List<Attendee> allAttendees = conflictingEvent.getAttendees();
        if (isNullOrEmpty(allAttendees)) {
            return Collections.emptyList();
        }
        List<Attendee> conflictingAttendees = new ArrayList<Attendee>();
        for (Attendee checkedAttendee : attendeesToCheck) {
            Attendee matchingAttendee = find(allAttendees, checkedAttendee);
            if (false == isOpaqueTransparency(conflictingEvent, matchingAttendee)) {
                continue;
            }
            if (isHardConflict(checkedAttendee)) {
                if (null != matchingAttendee && false == ParticipationStatus.DECLINED.equals(matchingAttendee.getPartStat())) {
                    hardConflict.setValue(Boolean.TRUE);
                    conflictingAttendees.add(0, matchingAttendee);
                }
            } else if (maxAttendeesPerConflict > conflictingAttendees.size()) {
                if (considerForFreeBusy(conflictingEvent, checkedAttendee)) {
                    conflictingAttendees.add(null == matchingAttendee ? checkedAttendee : matchingAttendee);
                }
            }
        }
        return 0 < conflictingAttendees.size() ? conflictingAttendees : Collections.emptyList();
    }

    /**
     * Gets a value indicating whether detailed event data is available for the current user based on the user's access rights.
     *
     * @param conflictingEvent The conflicting event to decide whether details are visible or not
     * @return <code>true</code> if details are available, <code>false</code>, otherwise
     */
    private boolean detailsVisible(Event conflictingEvent) throws OXException {
        int userID = session.getUserId();
        /*
         * details available if user is creator or attendee
         */
        if (matches(conflictingEvent.getCalendarUser(), userID) || matches(conflictingEvent.getCreatedBy(), userID) || isAttendee(conflictingEvent, userID) || isOrganizer(conflictingEvent, userID)) {
            return true;
        }
        /*
         * details visible if there's a resource attendee where the current user acts as booking delegate for
         */
        if (null != optResourceAttendeeWithDelegatePrivilege(conflictingEvent)) {
            return true;
        }
        /*
         * no details for non-public events
         */
        if (false == isPublicClassification(conflictingEvent)) {
            return false;
        }
        /*
         * details available based on folder permissions
         */
        if (null != conflictingEvent.getFolderId()) {
            Permission permission = getFolderPermissions().get(conflictingEvent.getFolderId());
            return null != permission && Permission.READ_ALL_OBJECTS <= permission.getReadPermission();
        } else if (isGroupScheduled(conflictingEvent)) {
            for (Attendee attendee : conflictingEvent.getAttendees()) {
                if (CalendarUserType.INDIVIDUAL.equals(attendee.getCuType()) && 0 < attendee.getEntity()) {
                    Permission permission = getFolderPermissions().get(attendee.getFolderId());
                    if (null != permission && Permission.READ_ALL_OBJECTS <= permission.getReadPermission()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Map<String, Permission> getFolderPermissions() throws OXException {
        if (null == folderPermissions) {
            List<CalendarFolder> folders = getFolderChooser().getVisibleFolders();
            folderPermissions = new HashMap<String, Permission>(folders.size());
            for (CalendarFolder folder : folders) {
                folderPermissions.put(folder.getId(), folder.getOwnPermission());
            }
        }
        return folderPermissions;
    }

    /**
     * Determines which attendees should be included in the conflict check during inserting/updating a certain event.
     * <ul>
     * <li>events that are marked {@link Transp#TRANSPARENT} for the attendee are excluded</li>
     * <li><i>hard</i>-conflicting attendees are always checked, while other internal attendees are included based on
     * {@link CalendarParameters#PARAMETER_CHECK_CONFLICTS}.</li>
     * <li>cross context attendees are included based on {@link CalendarConfig#isCrossContextConflictCheck()}</li>
     * </ul>
     *
     * @param event The event being inserted/updated
     * @param attendees The event's list of attendees, or <code>null</code> in case of a not group-scheduled event
     * @param internalAttendees The list of internal attendees to fill
     * @param externalUserAttendeesByEmail The map of external attendees to fill
     */
    private List<Attendee> getAttendeesToCheck(Event event, List<Attendee> attendees) throws OXException {
        boolean includeUserAttendees = isCheckConflicts(session);
        if (isNullOrEmpty(attendees)) {
            /*
             * assume simple, not group-scheduled event
             */
            if (false == isOpaqueTransparency(event)) {
                return Collections.emptyList();
            }
            if (includeUserAttendees && null != event.getCalendarUser()) {
                return Collections.singletonList(session.getEntityResolver().prepareUserAttendee(event.getCalendarUser().getEntity()));
            }
            return Collections.emptyList();
        }
        List<Attendee> attendeesToCheck = new ArrayList<>(attendees.size());
        boolean isCrossContext = session.getConfig().isCrossContextConflictCheck();
        for (Attendee attendee : resolveAttendees(session.getEntityResolver(), getConflictingAttendees(event, attendees)).keySet()) {
            if (isInternal(attendee)) {
                if (includeUserAttendees || CalendarUserType.RESOURCE.equals(attendee.getCuType()) || CalendarUserType.ROOM.equals(attendee.getCuType())) {
                    attendeesToCheck.add(attendee);
                }
            } else if (isCrossContext && includeUserAttendees && CalendarUtils.isExternalUser(attendee)) {
                /*
                 * Use external user for cross-context lookup
                 */
                if (null != optEMailAddress(attendee.getUri())) {
                    attendeesToCheck.add(attendee);
                }
            }
        }
        return attendeesToCheck;
    }

    /**
     * Get a list of attendees that for which the given event conflicts.
     * <p>
     * An event conflicts if it can be considered <code>opaque</code> for an attendee
     *
     * @param event The event that potentially conflicts
     * @param attendees The attendees to check
     * @return A list of attendees the event conflicts for
     * @throws OXException In case of error
     */
    private List<Attendee> getConflictingAttendees(Event event, List<Attendee> attendees) throws OXException {
        if (null == event.getId() && null == event.getSeriesId()) {
            /*
             * New event, check by transmitted data
             */
            return attendees.stream().filter(a -> isOpaqueTransparency(event, a)).toList();
        }
        List<Attendee> attendeesToCheck = new ArrayList<>(attendees.size());
        List<Attendee> storedAttendees = null;
        for (Attendee a : attendees) {
            /*
             * Check if value has explicitly been set
             */
            if (null != a.getTransp() && isOpaqueTransparency(event, a)) {
                attendeesToCheck.add(a);
            } else {
                /*
                 * Check if the transparency value is set in stored attendee, fallback to event transparency
                 */
                storedAttendees = null != storedAttendees ? storedAttendees : storage.getAttendeeStorage().loadAttendees(null != event.getId() ? event.getId() : event.getSeriesId());
                Attendee matchingAttendee = CalendarUtils.optFind(storedAttendees, a).orElse(a);
                if (isOpaqueTransparency(event, matchingAttendee)) {
                    attendeesToCheck.add(matchingAttendee);
                } // Ignore otherwise
            }
        }
        return attendeesToCheck;
    }

    private List<EventConflict> sortAndTrim(List<EventConflict> conflicts) {
        if (null != conflicts && 1 < conflicts.size()) {
            Collections.sort(conflicts, HARD_CONFLICTS_FIRST_COMPARATOR);
            if (maxConflicts < conflicts.size()) {
                return conflicts.subList(0, maxConflicts);
            }
        }
        return conflicts;
    }

    /**
     * Gets a value indicating whether a conflicting attendee would indicate a <i>hard</i> conflict or not.
     *
     * @param conflictingAttendee The attendee to check
     * @return <code>true</code> if the conflicting attendee would indicate a <i>hard</i> conflict, <code>false</code>, otherwise
     */
    private static boolean isHardConflict(Attendee conflictingAttendee) {
        return CalendarUserType.RESOURCE.equals(conflictingAttendee.getCuType()) || CalendarUserType.ROOM.equals(conflictingAttendee.getCuType());
    }

    /**
     * A comparator for event conflicts that orders <i>hard</i> conflicts first, otherwise compares the conflicting event's start dates.
     */
    private static final Comparator<EventConflict> HARD_CONFLICTS_FIRST_COMPARATOR = new Comparator<EventConflict>() {

        @Override
        public int compare(EventConflict conflict1, EventConflict conflict2) {
            if (conflict1.isHardConflict() && false == conflict2.isHardConflict()) {
                return -1;
            }
            if (false == conflict1.isHardConflict() && conflict2.isHardConflict()) {
                return 1;
            }
            return Long.compare(conflict1.getConflictingEvent().getStartDate().getTimestamp(), conflict2.getConflictingEvent().getStartDate().getTimestamp());
        }
    };

    private static final class CrossContextConflictCheckTask extends AbstractCrossContextLookupTask<List<Event>> {

        CrossContextConflictCheckTask(ServiceLookup services, Map<String, Attendee> attendeesByMailAdress, Date from, Date until) {
            super(services, attendeesByMailAdress, from, until);
        }

        @Override
        public List<Event> call() throws Exception {
            /*
             * resolve attendee's mail addresses to user attendees per context
             */
            Map<Integer, Map<Attendee, Attendee>> attendeesPerContext = resolveToContexts();
            if (null == attendeesPerContext || attendeesPerContext.isEmpty()) {
                return Collections.emptyList();
            }
            /*
             * get overlapping events for resolved attendees in each context & convert into 'external' representation
             */
            List<Event> result = new ArrayList<>();
            for (Entry<Integer, Map<Attendee, Attendee>> entry : attendeesPerContext.entrySet()) {
                int contextId = i(entry.getKey());
                Map<Attendee, Attendee> resolvedAttendees = entry.getValue();
                result.addAll(asExternalEvents(lookupOverlappingEvents(contextId, resolvedAttendees.keySet())));
            }
            return result;
        }

        /**
         * Looks up all overlapping events for one or attendees within a certain context.
         * 
         * @param contextId The context identifier
         * @param attendees The attendees to look up the overlapping events for
         * @return The overlapping events
         */
        private List<Event> lookupOverlappingEvents(int contextId, Collection<Attendee> attendees) throws OXException {
            CalendarConfig calendarConfig = new CalendarConfigImpl(contextId, services);
            if (false == calendarConfig.isCrossContextFreeBusy() || null == attendees || attendees.isEmpty()) {
                return Collections.emptyList();
            }
            /*
             * determine configured free/busy visibility for each queried attendee in the context
             */
            List<Attendee> consideredAttendees = new ArrayList<Attendee>(attendees.size());
            for (Attendee attendee : attendees) {
                /*
                 * exclude users with restricted ('context-internal' or 'none') free/busy visibility
                 */
                if (isInternalUser(attendee) && FreeBusyVisibility.ALL.equals(calendarConfig.getFreeBusyVisibility(attendee.getEntity()))) {
                    consideredAttendees.add(attendee);
                }
            }
            if (consideredAttendees.isEmpty()) {
                return Collections.emptyList();
            }
            /*
             * get overlapping events for considered attendees
             */
            List<Event> overlappingEvents = getOverlappingEvents(contextId, consideredAttendees);
            overlappingEvents = overlappingEvents.stream().filter(e -> hasAnyOpaqueTransparency(e, consideredAttendees)).toList();
            return overlappingEvents;
        }

        /**
         * Gets a value indicating whether the given event can be considered <code>opaque</code> for at least one attendee
         *
         * @param e The event to check
         * @param attendees The attendees that might attend the event
         * @return <code>true</code> if at least for one attendee the event is {@link Transp#OPAQUE}
         *         <code>false</code> if for all attendees the event is {@link Transp#TRANSPARENT}
         */
        private boolean hasAnyOpaqueTransparency(Event e, List<Attendee> attendees) {
            return attendees.stream().filter(a -> isOpaqueTransparency(e, optFind(e.getAttendees(), a.getEntity()))).findAny().isPresent();
        }

    }

}
