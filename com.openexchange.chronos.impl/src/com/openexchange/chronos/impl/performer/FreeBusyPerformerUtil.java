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
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getObjectIDs;
import static com.openexchange.chronos.common.CalendarUtils.isExternalUser;
import static com.openexchange.chronos.common.CalendarUtils.isGroupScheduled;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isPublicClassification;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.optEMailAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.BiFunction;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.EventStatus;
import com.openexchange.chronos.FbType;
import com.openexchange.chronos.FreeBusyTime;
import com.openexchange.chronos.FreeBusyVisibility;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.FreeBusyUtils;
import com.openexchange.chronos.compat.ShownAsTransparency;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.service.CalendarConfig;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.FreeBusyResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link FreeBusyPerformerUtil}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.4
 */
public class FreeBusyPerformerUtil {

    //@formatter:off
    /** The event fields returned in free/busy queries by default */
    static final EventField[] FREEBUSY_FIELDS = {
        EventField.CREATED_BY, EventField.ID, EventField.SERIES_ID, EventField.FOLDER_ID, EventField.COLOR, EventField.CLASSIFICATION,
        EventField.SUMMARY, EventField.START_DATE, EventField.END_DATE, EventField.CATEGORIES, EventField.TRANSP, EventField.LOCATION,
        EventField.RECURRENCE_ID, EventField.RECURRENCE_RULE, EventField.STATUS, EventField.UID
    };

    /** The restricted event fields returned in free/busy queries if the user has no access to the event */
    static final EventField[] RESTRICTED_FREEBUSY_FIELDS = { EventField.CREATED_BY, EventField.ID, EventField.SERIES_ID,
        EventField.CLASSIFICATION, EventField.START_DATE, EventField.END_DATE, EventField.TRANSP, EventField.RECURRENCE_ID,
        EventField.RECURRENCE_RULE, EventField.STATUS, EventField.UID
    };
    //@formatter:on

    /**
     * Gets a resulting userized event occurrence for the free/busy result based on the supplied data of the master event. Only a subset
     * of properties is copied over, and a folder identifier is applied optionally, depending on the user's access permissions for the
     * actual event data.
     *
     * @param occurence The occurence
     * @param masterEvent The master event data
     * @param recurrenceId The recurrence identifier of the occurrence
     * @return The resulting event occurrence representing the free/busy slot
     */
    static Event getResultingOccurrence(Event occurence, Event masterEvent, RecurrenceId recurrenceId) {
        occurence.setRecurrenceRule(null);
        occurence.removeSeriesId();
        occurence.removeClassification();
        occurence.setRecurrenceId(recurrenceId);
        occurence.setStartDate(CalendarUtils.calculateStart(masterEvent, recurrenceId));
        occurence.setEndDate(CalendarUtils.calculateEnd(masterEvent, recurrenceId));
        return occurence;
    }

    /**
     * Resolves the supplied list of attendees using the supplied entity resolver, and associates them in a map to the passed ones.
     *
     * @param entityResolver The entity resolver to use
     * @param requestedAttendees The attendees as requested from the client
     * @return The resolved attendees in a map as keys, associated with their supplied variants as values
     */
    static Map<Attendee, Attendee> resolveAttendees(EntityResolver entityResolver, List<Attendee> requestedAttendees) throws OXException {
        if (null == requestedAttendees || requestedAttendees.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Attendee, Attendee> resolvedAttendees = new HashMap<Attendee, Attendee>(requestedAttendees.size());
        for (Attendee requestedAttendee : requestedAttendees) {
            resolvedAttendees.put(entityResolver.prepare(requestedAttendee, true), requestedAttendee);
        }
        return resolvedAttendees;
    }

    /**
     * Distinguishes between <i>internal</i> and <i>external</i> user attendees and sorts them into the passed collections.
     * <p/>
     * External attendees without valid email address in their URI property are skipped implicitly.
     * 
     * @param attendees The attendees to separate
     * @param internalAttendees The list to put <i>internal</i> attendees into
     * @param externalUserAttendeesByEmail A to put <i>external</i> user attendees into, mapped by their email address
     */
    static void separateAttendees(Collection<Attendee> attendees, List<Attendee> internalAttendees, Map<String, Attendee> externalUserAttendeesByEmail) {
        if (null != attendees) {
            for (Attendee attendee : attendees) {
                if (isInternal(attendee)) {
                    internalAttendees.add(attendee);
                } else if (isExternalUser(attendee)) {
                    String email = optEMailAddress(attendee.getUri());
                    if (null != email) {
                        externalUserAttendeesByEmail.put(email, attendee);
                    }
                }
            }
        }
    }

    /**
     * Normalizes the contained free/busy intervals. This means
     * <ul>
     * <li>the intervals are sorted chronologically, i.e. the earliest interval is first</li>
     * <li>all intervals beyond or above the 'from' and 'until' range are removed, intervals overlapping the boundaries are shortened to
     * fit</li>
     * <li>overlapping intervals are merged so that only the most conflicting ones of overlapping time ranges are used</li>
     * </ul>
     *
     * @param events The events to get the free/busy-times from
     * @param attendee The attendee to generate the free/busy-time for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @param eventAdjuster A function to optionally adjust the free/busy time's event reference, or <code>null</code> if not applicable
     */
    static List<FreeBusyTime> mergeFreeBusy(List<Event> events, Attendee attendee, Date from, Date until, TimeZone timeZone, BiFunction<Event, Attendee, Event> eventAdjuster) {
        if (null == events || 0 == events.size()) {
            return Collections.emptyList(); // nothing to do
        }
        /*
         * get free/busy times, normalize to requested period & perform the merge
         */
        List<FreeBusyTime> freeBusyTimes = adjustToBoundaries(getFreeBusyTimes(events, attendee, timeZone, eventAdjuster), from, until);
        if (2 > freeBusyTimes.size()) {
            return freeBusyTimes; // nothing more to do
        }
        return FreeBusyUtils.mergeFreeBusy(freeBusyTimes);
    }

    /**
     * Gets the {@link FbType} for the given event
     * getFbType
     *
     * @param event The event
     * @return The {@link FbType}
     */
    private static FbType getFbType(Event event) {
        Transp transp = event.getTransp();
        if (null == transp) {
            return FbType.BUSY;
        }
        if ((transp instanceof ShownAsTransparency)) {
            switch ((ShownAsTransparency) transp) {
                case ABSENT:
                    return FbType.BUSY_UNAVAILABLE;
                case FREE:
                    return FbType.FREE;
                case TEMPORARY:
                    return FbType.BUSY_TENTATIVE;
                default:
                    return FbType.BUSY;
            }
        }

        if (Transp.TRANSPARENT.equals(transp.getValue())) {
            return FbType.FREE;
        }
        if (event.getStatus() == null) {
            return FbType.BUSY;
        }
        if (EventStatus.TENTATIVE.equals(event.getStatus())) {
            return FbType.BUSY_TENTATIVE;
        }
        if (EventStatus.CANCELLED.equals(event.getStatus())) {
            return FbType.FREE;
        }
        return FbType.BUSY;
    }

    /**
     * Normalizes a list of free/busy times to the boundaries of a given period, i.e. removes free/busy times outside range and adjusts
     * the start-/end-times of periods overlapping the start- or enddate of the period.
     *
     * @param freeBusyTimes The free/busy times to normalize
     * @param from The lower inclusive limit of the range
     * @param until The upper exclusive limit of the range
     * @return The normalized free/busy times
     */
    static List<FreeBusyTime> adjustToBoundaries(List<FreeBusyTime> freeBusyTimes, Date from, Date until) {
        for (Iterator<FreeBusyTime> iterator = freeBusyTimes.iterator(); iterator.hasNext();) {
            if (null == adjustToBoundaries(iterator.next(), from, until)) {
                iterator.remove(); // outside range
            }
        }
        return freeBusyTimes;
    }

    /**
     * Normalizes a free/busy time to the boundaries of a given period, adjusts the start-/end-times of the timeslot if it overlaps the
     * start- or enddate of the period. If the free/busy time is outside of the range, <code>null</code> is returned.
     *
     * @param freeBusyTime The free/busy time to normalize
     * @param from The lower inclusive limit of the range
     * @param until The upper exclusive limit of the range
     * @return The normalized free/busy time, or <code>null</code> if it was outside the requested range
     */
    static FreeBusyTime adjustToBoundaries(FreeBusyTime freeBusyTime, Date from, Date until) {
        if (freeBusyTime.getEndTime().after(from) && freeBusyTime.getStartTime().before(until)) {
            if (freeBusyTime.getStartTime().before(from)) {
                freeBusyTime.setStartTime(from);
            }
            if (freeBusyTime.getEndTime().after(until)) {
                freeBusyTime.setEndTime(until);
            }
        } else {
            return null; // outside range
        }
        return freeBusyTime;
    }

    /**
     * Gets a list of free/busy times for the supplied events.
     *
     * @param events The events to get the free/busy times for
     * @param attendee The attendee to generate the free/busy-time for
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @param eventAdjuster A function to optionally adjust the free/busy time's event reference, or <code>null</code> if not applicable
     * @return The free/busy times
     */
    static List<FreeBusyTime> getFreeBusyTimes(List<Event> events, Attendee attendee, TimeZone timeZone, BiFunction<Event, Attendee, Event> eventAdjuster) {
        List<FreeBusyTime> freeBusyTimes = new ArrayList<FreeBusyTime>(events.size());
        for (Event event : events) {
            FreeBusyTime freeBusyTime = getFreeBusyTime(event, attendee, timeZone, eventAdjuster);
            if (null != freeBusyTime) {
                freeBusyTimes.add(freeBusyTime);
            }
        }
        return freeBusyTimes;
    }

    /**
     * Gets a free/busy time for a specific event.
     *
     * @param events The event to get the free/busy time for
     * @param attendee The attendee to generate the free/busy-time for
     * @param timeZone The timezone to consider if the event has <i>floating</i> dates
     * @param eventAdjuster A function to optionally adjust the free/busy time's event reference, or <code>null</code> if not applicable
     * @return The free/busy time
     */
    static FreeBusyTime getFreeBusyTime(Event event, Attendee attendee, TimeZone timeZone, BiFunction<Event, Attendee, Event> eventAdjuster) {
        Event freeBusyEvent = null != eventAdjuster ? eventAdjuster.apply(event, attendee) : event;
        long start = freeBusyEvent.getStartDate().getTimestamp();
        long end = freeBusyEvent.getEndDate().getTimestamp();
        if (CalendarUtils.isFloating(freeBusyEvent)) {
            start = CalendarUtils.getDateInTimeZone(freeBusyEvent.getStartDate(), timeZone);
            end = CalendarUtils.getDateInTimeZone(freeBusyEvent.getEndDate(), timeZone);
        }
        return new FreeBusyTime(getFbType(freeBusyEvent), new Date(start), new Date(end), freeBusyEvent);
    }

    /**
     * Generates the free/busy results associated with each requested attendee based on the given free/busy times.
     * 
     * @param attendees The attendees as requested by the client
     * @param freeBusyPerAttendee The determined free/busy times for each attendee
     * @param warningsPerAttendee The warnings to include for each attendee
     * @return The free/busy results per requested attendee
     */
    static Map<Attendee, FreeBusyResult> getFreeBusyResults(List<Attendee> attendees, Map<Attendee, List<FreeBusyTime>> freeBusyPerAttendee, Map<Attendee, OXException> warningsPerAttendee) {
        if (null == attendees || attendees.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Attendee, FreeBusyResult> results = new LinkedHashMap<Attendee, FreeBusyResult>(attendees.size());
        for (Attendee attendee : attendees) {
            List<FreeBusyTime> freeBusyTimes = freeBusyPerAttendee.get(attendee);
            List<OXException> warnings;
            OXException warning = warningsPerAttendee.get(attendee);
            if (null != warning) {
                warnings = Collections.singletonList(warning);
            } else {
                warnings = null != freeBusyTimes ? null : Collections.singletonList(CalendarExceptionCodes.FREEBUSY_NOT_AVAILABLE.create(extractEMailAddress(attendee.getUri())));
            }
            results.put(attendee, new FreeBusyResult(freeBusyTimes, warnings));
        }
        return results;
    }

    /**
     * Reads the attendee data from storage
     *
     * @param events The events to load
     * @param internal whether to only consider internal attendees or not
     * @param storage The {@link CalendarStorage}
     * @return The {@link Event}s containing the attendee data
     * @throws OXException
     */
    protected static List<Event> readAttendeeData(List<Event> events, Boolean internal, CalendarStorage storage) throws OXException {
        if (null != events && 0 < events.size()) {
            Map<String, List<Attendee>> attendeesById = storage.getAttendeeStorage().loadAttendees(getObjectIDs(events), internal);
            for (Event event : events) {
                event.setAttendees(attendeesById.get(event.getId()));
            }
        }
        return events;
    }

    /**
     * Gets a value indicating whether a certain event is visible or <i>opaque to</i> free/busy results in the view of the current
     * session's user or not.
     * 
     * @param event The event to check
     * @param maskUid The UID for events to hide
     * @return <code>true</code> if the event is visible, <code>false</code> otherwise
     */
    public static boolean isVisible(Event event, String maskUid) {
        if (null != maskUid && maskUid.equals(event.getUid())) {
            return false;
        }
        return isPublicClassification(event) || Classification.CONFIDENTIAL.equals(event.getClassification());
    }

    /**
     * Gets a value indicating whether the given event shall be included in a free/busy calculation
     * or not
     *
     * @param event The event to check
     * @param entity The identifier of the entity to check for
     * @return <code>true</code> if the event can be included, <code>false</code> otherwise
     */
    public static boolean includeForFreeBusy(Event event, int entity) {
        if (isGroupScheduled(event)) {
            /*
             * exclude if attendee doesn't attend
             */
            Attendee eventAttendee = find(event.getAttendees(), entity);
            if (null == eventAttendee || eventAttendee.isHidden() || ParticipationStatus.DECLINED.equals(eventAttendee.getPartStat())) {
                return false;
            }
        } else {
            /*
             * exclude if attendee doesn't match event owner
             */
            if (false == matches(event.getCalendarUser(), entity)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets a value indicating whether the given event shall be included in a free/busy calculation or not, based on the effective
     * attendance in the event.
     *
     * @param event The event to check
     * @param attendee The attendee to check for
     * @return <code>true</code> if the event can be included, <code>false</code> otherwise
     */
    public static boolean includeForFreeBusy(Event event, Attendee attendee) {
        if (isGroupScheduled(event)) {
            /*
             * exclude if attendee doesn't attend
             */
            Attendee eventAttendee = find(event.getAttendees(), attendee);
            if (null == eventAttendee || eventAttendee.isHidden() || ParticipationStatus.DECLINED.equals(eventAttendee.getPartStat())) {
                return false;
            }
        } else {
            /*
             * exclude if attendee doesn't match event owner
             */
            if (false == matches(event.getCalendarUser(), attendee)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Looks up the configured free/busy visibility from the given internal user attendees.
     * 
     * @param config The underlying calendar config for the context the user attendees are located in
     * @param attendees The attendees to get the configured free/busy visibility for
     * @return The configured free/busy visibilities for the internal user attendees
     * @throws OXException
     */
    static Map<Attendee, FreeBusyVisibility> getConfiguredFreeBusyVisibilities(CalendarConfig config, Collection<Attendee> attendees) throws OXException {
        if (null == attendees || attendees.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Attendee, FreeBusyVisibility> freeBusyVisibilityPerAttendee = new HashMap<Attendee, FreeBusyVisibility>(attendees.size());
        for (Attendee attendee : attendees) {
            if (CalendarUtils.isInternalUser(attendee)) {
                freeBusyVisibilityPerAttendee.put(attendee, config.getFreeBusyVisibility(attendee.getEntity()));
            }
        }
        return freeBusyVisibilityPerAttendee;
    }

    /**
     * Prepares a {@link CalendarExceptionCodes#FREEBUSY_NOT_AVAILABLE_PER_CONFIGURATION} warning for a specific attendee, ready to be
     * used in free/busy results.
     * 
     * @param calendarUser The calendar user to generate the warning for
     * @return The warning
     */
    static OXException getRestrictedVisibilityWarning(CalendarUser calendarUser) {
        return CalendarExceptionCodes.FREEBUSY_NOT_AVAILABLE_PER_CONFIGURATION.create(extractEMailAddress(calendarUser.getUri()));
    }

}
