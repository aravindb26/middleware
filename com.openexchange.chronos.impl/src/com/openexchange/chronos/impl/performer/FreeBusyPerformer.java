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

import static com.openexchange.chronos.common.CalendarUtils.asExternalResult;
import static com.openexchange.chronos.common.CalendarUtils.getResourceCalendarId;
import static com.openexchange.chronos.common.CalendarUtils.isInternalUser;
import static com.openexchange.chronos.common.CalendarUtils.looksLikeSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.optFind;
import static com.openexchange.chronos.common.FreeBusyUtils.mergeFreeBusy;
import static com.openexchange.chronos.impl.Utils.anonymizeIfNeeded;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.RESTRICTED_FREEBUSY_FIELDS;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.adjustToBoundaries;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.getConfiguredFreeBusyVisibilities;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.getFreeBusyResults;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.getFreeBusyTime;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.getRestrictedVisibilityWarning;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.includeForFreeBusy;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.isVisible;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.mergeFreeBusy;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.resolveAttendees;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.separateAttendees;
import static com.openexchange.chronos.service.CalendarParameters.PARAMETER_MASK_UID;
import static com.openexchange.java.Autoboxing.i;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.FreeBusyTime;
import com.openexchange.chronos.FreeBusyVisibility;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.common.EventOccurrence;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.impl.OccurrenceId;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.chronos.impl.session.CalendarConfigImpl;
import com.openexchange.chronos.service.CalendarConfig;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.FreeBusyResult;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.ThreadPools;

/**
 * {@link FreeBusyPerformer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public class FreeBusyPerformer extends AbstractFreeBusyPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(FreeBusyPerformer.class);

    /**
     * Initializes a new {@link FreeBusyPerformer}.
     *
     * @param storage The underlying calendar storage
     * @param session The calendar session
     */
    public FreeBusyPerformer(CalendarSession session, CalendarStorage storage) {
        super(session, storage);
    }

    /**
     * Performs the free/busy operation.
     *
     * @param attendees The attendees to get the free/busy data for
     * @param from The start of the requested time range
     * @param until The end of the requested time range
     * @param merge <code>true</code> to merge the resulting free/busy-times, <code>false</code>, otherwise
     * @return The free/busy times for each of the requested attendees, wrapped within a free/busy result structure
     */
    public Map<Attendee, FreeBusyResult> perform(List<Attendee> attendees, Date from, Date until, boolean merge) throws OXException {
        if (null == attendees || attendees.isEmpty()) {
            return Collections.emptyMap();
        }
        /*
         * resolve passed attendees prior lookup & distinguish between internal, and external user attendees as cross-context candidates
         */
        Map<Attendee, Attendee> resolvedAttendees = resolveAttendees(session.getEntityResolver(), attendees);
        List<Attendee> internalAttendees = new ArrayList<Attendee>(resolvedAttendees.size());
        Map<String, Attendee> externalUserAttendeesByEmail = new HashMap<String, Attendee>();
        separateAttendees(resolvedAttendees.keySet(), internalAttendees, externalUserAttendeesByEmail);
        /*
         * check and initiate cross-context lookup if applicable
         */
        Future<Map<Attendee, EventsResult>> crossContextLookupFuture = null;
        if (0 < externalUserAttendeesByEmail.size() && session.getConfig().isCrossContextFreeBusy()) {
            String maskUid = session.get(PARAMETER_MASK_UID, String.class, null);
            crossContextLookupFuture = ThreadPools.submitElseExecute(new CrossContextFreeBusyTask(Services.getServiceLookup(), externalUserAttendeesByEmail, from, until, maskUid));
        }
        /*
         * get intersecting events per resolved internal attendee and insert into overall result
         */
        Map<Attendee, EventsResult> eventsPerAttendee = new LinkedHashMap<Attendee, EventsResult>(resolvedAttendees.size());
        eventsPerAttendee.putAll(getOverlappingEvents(internalAttendees, from, until));
        /*
         * if available, take over results from users in other contexts, too
         */
        if (null != crossContextLookupFuture) {
            try {
                eventsPerAttendee.putAll(crossContextLookupFuture.get());
            } catch (InterruptedException | ExecutionException e) {
                session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create(e.getMessage(), e));
            }
        }
        /*
         * derive (merged) free/busy times for found events, mapped back to the client-requested attendees
         */
        Map<OccurrenceId, Event> knownEvents = new HashMap<OccurrenceId, Event>();
        Map<Attendee, List<FreeBusyTime>> freeBusyPerAttendee = new HashMap<Attendee, List<FreeBusyTime>>(eventsPerAttendee.size());
        Map<Attendee, OXException> warningsPerAttendee = new HashMap<Attendee, OXException>(eventsPerAttendee.size());
        for (Entry<Attendee, EventsResult> entry : eventsPerAttendee.entrySet()) {
            Attendee attendee = resolvedAttendees.get(entry.getKey());
            if (null == attendee) {
                session.addWarning(CalendarExceptionCodes.UNEXPECTED_ERROR.create("Skipping free/busy times from unexpected attendee " + entry.getKey()));
                continue;
            }
            if (null != entry.getValue().getError()) {
                warningsPerAttendee.put(attendee, entry.getValue().getError());
            }
            List<Event> eventInPeriods = entry.getValue().getEvents();
            if (null == eventInPeriods || eventInPeriods.isEmpty()) {
                freeBusyPerAttendee.put(attendee, Collections.emptyList());
                continue;
            }
            /*
             * create free/busy times for attendee, using anonymized or enriched event data as applicable
             */
            TimeZone timeZone = getTimeZone(attendee);
            List<FreeBusyTime> freeBusyTimes = new ArrayList<FreeBusyTime>(eventInPeriods.size());
            for (Event event : eventInPeriods) {
                if (looksLikeSeriesMaster(event)) {
                    /*
                     * expand & add all (non overridden) instances of event series in period, expanded by the actual event duration
                     */
                    long duration = event.getEndDate().getTimestamp() - event.getStartDate().getTimestamp();
                    Date iterateFrom = new Date(from.getTime() - duration);
                    Iterator<RecurrenceId> iterator = session.getRecurrenceService().iterateRecurrenceIds(new DefaultRecurrenceData(event), iterateFrom, until);
                    while (iterator.hasNext()) {
                        calculateFreeBusyTime(new EventOccurrence(event, iterator.next()), attendee, from, until, knownEvents, timeZone, freeBusyTimes);
                    }
                } else {
                    calculateFreeBusyTime(event, attendee, from, until, knownEvents, timeZone, freeBusyTimes);
                }
            }
            freeBusyPerAttendee.put(attendee, merge && 1 < freeBusyTimes.size() ? mergeFreeBusy(freeBusyTimes) : freeBusyTimes);
        }
        return getFreeBusyResults(attendees, freeBusyPerAttendee, warningsPerAttendee);
    }

    private void calculateFreeBusyTime(Event event, Attendee attendee, Date from, Date until, Map<OccurrenceId, Event> knownEvents, TimeZone timeZone, List<FreeBusyTime> freeBusyTimes) {
        FreeBusyTime freeBusyTime = getFreeBusyTime(event, attendee, timeZone, this::getResultingEvent);
        if (null != freeBusyTime && null != adjustToBoundaries(freeBusyTime, from, until)) {
            freeBusyTimes.add(rememberInternalOrReplaceExternalEvent(freeBusyTime, knownEvents));
        }
    }

    /**
     * Remembers an <i>internal</i> event (with set object identifiers) from a free/busy timeslot in the supplied map, or re-uses such a
     * previously remembered event for the free/busy timeslot of an <i>external</i> context. Association is done based on the uid /
     * recurrence id pair from {@link OccurrenceId}.
     * 
     * @param freeBusyTime The free/busy time to remember or replace the event in
     * @param knownEvents The previously remembered events, mapped by their {@link OccurrenceId}.
     * @return The (possibly adjusted) free/busy time
     */
    private static FreeBusyTime rememberInternalOrReplaceExternalEvent(FreeBusyTime freeBusyTime, Map<OccurrenceId, Event> knownEvents) {
        Event event = freeBusyTime.getEvent();
        if (null != event) {
            if (null != event.getId()) {
                /*
                 * remember internal event
                 */
                knownEvents.putIfAbsent(new OccurrenceId(event), event);
            } else {
                /*
                 * replace external event
                 */
                Event knownEvent = knownEvents.get(new OccurrenceId(event));
                if (null != knownEvent) {
                    freeBusyTime.setEvent(knownEvent);
                }
            }
        }
        return freeBusyTime;
    }

    /**
     * Adjusts an overlapping event to be used in free/busy results by either preserving the {@link #FREEBUSY_FIELDS} or the
     * {@link #RESTRICTED_FREEBUSY_FIELDS} in the returned event copy, based on the session user's access permission for the event.
     * Optionally, the most appropriate parent folder id representing the session user's view on the event is injected, too.
     * 
     * @param event The event to adjust
     * @param attendee The attendee to adjust the event for
     * @return The resulting event, or <code>null</code> if the event cannot be adjusted
     */
    private Event getResultingEvent(Event event, Attendee attendee) {
        String folderId;
        try {
            folderId = getFolderChooser().chooseFolderID(event);
        } catch (OXException e) {
            LOG.warn("Unexpected error choosing folder id for event {}", event, e);
            folderId = null;
        }
        try {
            Attendee optResourceAttendee = optResourceAttendeeWithDelegatePrivilege(event);
            if (null == folderId) {
                if (null == optResourceAttendee) {
                    Event resultingEvent = EventMapper.getInstance().copy(event, new Event(), RESTRICTED_FREEBUSY_FIELDS);
                    optFind(event.getAttendees(), attendee).map(Attendee::getTransp).ifPresent(resultingEvent::setTransp);
                    return resultingEvent;
                }
                folderId = getResourceCalendarId(optResourceAttendee.getEntity());
            }
            Event resultingEvent = EventMapper.getInstance().copy(event, new Event(), FreeBusyPerformerUtil.FREEBUSY_FIELDS);
            resultingEvent.setFolderId(folderId);
            optFind(event.getAttendees(), attendee).map(Attendee::getTransp).ifPresent(resultingEvent::setTransp);
            return null != optResourceAttendee ? resultingEvent : anonymizeIfNeeded(session, resultingEvent);
        } catch (OXException e) {
            LOG.warn("Unexpected error adjusting event data {}", event, e);
        }
        return null;
    }

    /**
     * Gets a list of overlapping events in a certain range for each requested attendee.
     *
     * @param attendees The attendees to query free/busy information for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @return The overlapping events, mapped to each attendee
     */
    private Map<Attendee, EventsResult> getOverlappingEvents(Collection<Attendee> attendees, Date from, Date until) throws OXException {
        /*
         * determine configured free/busy visibility for each queried attendee
         */
        Map<Attendee, FreeBusyVisibility> freeBusyVisibilityPerAttendee = getConfiguredFreeBusyVisibilities(session.getConfig(), attendees);
        /*
         * get overlapping events and derive results, including a hint if free/busy visibility was restricted
         */
        Map<Attendee, List<Event>> overlappingEvents = new OverlappingEventsLoader(storage).load(attendees, from, until, (e, a) -> considerForFreeBusy(e, a), session.getUserId());
        Map<Attendee, EventsResult> eventsPerAttendee = new LinkedHashMap<Attendee, EventsResult>(overlappingEvents.size());
        for (Entry<Attendee, List<Event>> eventsForAttendee : overlappingEvents.entrySet()) {
            Attendee attendee = eventsForAttendee.getKey();
            OXException error = null;
            if (FreeBusyVisibility.NONE.equals(freeBusyVisibilityPerAttendee.get(attendee)) && session.getUserId() != attendee.getEntity()) {
                error = getRestrictedVisibilityWarning(attendee);
            }
            eventsPerAttendee.put(attendee, new DefaultEventsResult(eventsForAttendee.getValue(), -1L, error));
        }
        return eventsPerAttendee;
    }
    
    /**
     * Performs the merged free/busy operation.
     *
     * @param attendees The attendees to query free/busy information for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @return The free/busy result
     */
    public Map<Attendee, List<FreeBusyTime>> performMerged(List<Attendee> attendees, Date from, Date until) throws OXException {
        Map<Attendee, EventsResult> eventsPerAttendee = getOverlappingEvents(attendees, from, until);
        Map<Attendee, List<FreeBusyTime>> freeBusyDataPerAttendee = new HashMap<Attendee, List<FreeBusyTime>>(eventsPerAttendee.size());
        for (Entry<Attendee, EventsResult> entry : eventsPerAttendee.entrySet()) {
            freeBusyDataPerAttendee.put(entry.getKey(), mergeFreeBusy(entry.getValue().getEvents(), entry.getKey(), from, until, getTimeZone(entry.getKey()), this::getResultingEvent));
        }
        return freeBusyDataPerAttendee;
    }

    /**
     * Calculates the free/busy time ranges from the user defined availability and the free/busy operation
     *
     * @param attendees The attendees to calculate the free/busy information for
     * @param from The start time of the interval
     * @param until The end time of the interval
     * @return A {@link Map} with a {@link FreeBusyResult} per {@link Attendee}
     */
    public Map<Attendee, FreeBusyResult> performCalculateFreeBusyTime(List<Attendee> attendees, Date from, Date until) throws OXException {
        // Get the free busy data for the attendees
        Map<Attendee, List<FreeBusyTime>> freeBusyPerAttendee = performMerged(attendees, from, until);
        Map<Attendee, FreeBusyResult> results = new HashMap<>();
        for (Map.Entry<Attendee, List<FreeBusyTime>> attendeeEntry : freeBusyPerAttendee.entrySet()) {
            FreeBusyResult result = new FreeBusyResult();
            result.setFreeBusyTimes(attendeeEntry.getValue());
            results.put(attendeeEntry.getKey(), result);
        }
        return results;
    }

    private static final class CrossContextFreeBusyTask extends AbstractCrossContextLookupTask<Map<Attendee, EventsResult>> {

        private final String maskUid;

        CrossContextFreeBusyTask(ServiceLookup services, Map<String, Attendee> attendeesByMailAdress, Date from, Date until, String maskUid) {
            super(services, attendeesByMailAdress, from, until);
            this.maskUid = maskUid;
        }

        @Override
        public Map<Attendee, EventsResult> call() throws Exception {
            /*
             * resolve attendee's mail addresses to user attendees per context
             */
            Map<Integer, Map<Attendee, Attendee>> attendeesPerContext = resolveToContexts();
            if (null == attendeesPerContext || attendeesPerContext.isEmpty()) {
                return Collections.emptyMap();
            }
            /*
             * get overlapping events for resolved attendees in each context & re-map to incoming attendees
             */
            Map<Attendee, EventsResult> eventsPerAttendee = new HashMap<Attendee, EventsResult>(attendeesByMailAdress.size());
            for (Entry<Integer, Map<Attendee, Attendee>> entry : attendeesPerContext.entrySet()) {
                int contextId = i(entry.getKey());
                Map<Attendee, Attendee> resolvedAttendees = entry.getValue();
                Map<Attendee, EventsResult> resultsInContext = lookupOverlappingEventsPerAttendee(contextId, resolvedAttendees.keySet());
                for (Entry<Attendee, EventsResult> resultForAttendee : resultsInContext.entrySet()) {
                    eventsPerAttendee.put(resolvedAttendees.get(resultForAttendee.getKey()), asExternalResult(resultForAttendee.getValue()));
                }
            }
            return eventsPerAttendee;
        }

        /**
         * Looks up all overlapping events for one or attendees within a certain context.
         * 
         * @param contextId The context identifier
         * @param attendees The attendees to look up the overlapping events for
         * @return The overlapping events
         */
        private Map<Attendee, EventsResult> lookupOverlappingEventsPerAttendee(int contextId, Collection<Attendee> attendees) throws OXException {
            CalendarConfig calendarConfig = new CalendarConfigImpl(contextId, services);
            if (false == calendarConfig.isCrossContextFreeBusy() || null == attendees || attendees.isEmpty()) {
                return Collections.emptyMap();
            }
            Map<Attendee, EventsResult> resultsPerAttendee = new HashMap<Attendee, EventsResult>(attendees.size());
            /*
             * determine configured free/busy visibility for each queried attendee in the context, prepare result with warning if restricted
             */
            List<Attendee> consideredAttendees = new ArrayList<Attendee>(attendees.size());
            for (Attendee attendee : attendees) {
                /*
                 * exclude users with restricted ('context-internal' or 'none') free/busy visibility
                 */
                if (isInternalUser(attendee) && FreeBusyVisibility.ALL.equals(calendarConfig.getFreeBusyVisibility(attendee.getEntity()))) {
                    consideredAttendees.add(attendee);
                } else {
                    resultsPerAttendee.put(attendee, new DefaultEventsResult(getRestrictedVisibilityWarning(attendee)));
                }
            }
            /*
             * get overlapping events and derive results for considered attendees
             */
            Map<Attendee, List<Event>> overlappingEvents = getOverlappingEventsPerAttendee(
                contextId, consideredAttendees, (e, a) -> isVisible(e, maskUid) && includeForFreeBusy(e, a.getEntity()));
            for (Entry<Attendee, List<Event>> eventsForAttendee : overlappingEvents.entrySet()) {
                resultsPerAttendee.put(eventsForAttendee.getKey(), new DefaultEventsResult(eventsForAttendee.getValue()));
            }
            return resultsPerAttendee;
        }

    }

}
