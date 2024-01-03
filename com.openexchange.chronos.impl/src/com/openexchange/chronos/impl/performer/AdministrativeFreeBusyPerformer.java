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

import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isPublicClassification;
import static com.openexchange.chronos.common.CalendarUtils.looksLikeSeriesMaster;
import static com.openexchange.chronos.common.CalendarUtils.optFind;
import static com.openexchange.chronos.common.FreeBusyUtils.mergeFreeBusy;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.FREEBUSY_FIELDS;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.RESTRICTED_FREEBUSY_FIELDS;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.adjustToBoundaries;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.getFreeBusyResults;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.getFreeBusyTimes;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.getRestrictedVisibilityWarning;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.includeForFreeBusy;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.isVisible;
import static com.openexchange.chronos.impl.performer.FreeBusyPerformerUtil.resolveAttendees;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.FreeBusyTime;
import com.openexchange.chronos.FreeBusyVisibility;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultEventsResult;
import com.openexchange.chronos.common.DefaultRecurrenceData;
import com.openexchange.chronos.common.EventOccurrence;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.impl.osgi.Services;
import com.openexchange.chronos.impl.session.CalendarConfigImpl;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.EntityResolver;
import com.openexchange.chronos.service.EventsResult;
import com.openexchange.chronos.service.FreeBusyResult;
import com.openexchange.chronos.service.RecurrenceService;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link AdministrativeFreeBusyPerformer} - similar to the {@link FreeBusyPerformer} but requires no session
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.4
 */
public class AdministrativeFreeBusyPerformer {

    private static final Logger LOG = LoggerFactory.getLogger(AdministrativeFreeBusyPerformer.class);

    private final CalendarStorage storage;
    private final EntityResolver resolver;
    private final Optional<CalendarParameters> params;

    /**
     * Initializes a new {@link AdministrativeFreeBusyPerformer}.
     *
     * @param storage The underlying calendar storage
     * @param resolver The entity resolver
     * @param params The optional {@link CalendarParameters}
     */
    public AdministrativeFreeBusyPerformer(CalendarStorage storage, EntityResolver resolver, Optional<CalendarParameters> params) {
        super();
        this.storage = storage;
        this.resolver = resolver;
        this.params = params;
    }

    /**
     * Performs the free/busy operation.
     *
     * @param attendees The attendees to get the free/busy data for
     * @param from The start of the requested time range
     * @param until The end of the requested time range
     * @param merge <code>true</code> to merge the resulting free/busy-times, <code>false</code>, otherwise
     * @param includeDetails <code>true</code> to include details for non-classified events, <code>false</code> to always stick to the restricted fields
     * @return The free/busy times for each of the requested attendees, wrapped within a free/busy result structure
     */
    public Map<Attendee, FreeBusyResult> perform(List<Attendee> attendees, Date from, Date until, boolean merge, boolean includeDetails) throws OXException {
        if (null == attendees || attendees.isEmpty()) {
            return Collections.emptyMap();
        }
        /*
         * resolve passed attendees prior lookup & get intersecting events per resolved attendee
         */
        Map<Attendee, Attendee> resolvedAttendees = resolveAttendees(resolver, attendees);
        Map<Attendee, EventsResult> eventsPerAttendee = getOverlappingEvents(resolvedAttendees.keySet(), from, until);
        /*
         * derive (merged) free/busy times for found events, mapped back to the requested attendees
         */
        Map<Attendee, List<FreeBusyTime>> freeBusyPerAttendee = new HashMap<Attendee, List<FreeBusyTime>>(eventsPerAttendee.size());
        Map<Attendee, OXException> warningsPerAttendee = new HashMap<Attendee, OXException>(eventsPerAttendee.size());
        for (Entry<Attendee, EventsResult> entry : eventsPerAttendee.entrySet()) {
            Attendee attendee = resolvedAttendees.get(entry.getKey());
            if (null == attendee) {
                LOG.warn("Skipping free/busy times from unexpected attendee {}", entry.getKey());
                continue;
            }
            if (null != entry.getValue().getError()) {
                warningsPerAttendee.put(attendee, entry.getValue().getError());
            }
            List<Event> events = entry.getValue().getEvents();
            if (null == events || events.isEmpty()) {
                freeBusyPerAttendee.put(attendee, null == events ? null : Collections.emptyList());
                continue;
            }
            /*
             * create free/busy times for attendee, using anonymized event data
             */
            List<Event> eventsInPeriod = new ArrayList<Event>(events.size());
            for (Event event : events) {
                if (looksLikeSeriesMaster(event)) {
                    /*
                     * expand & add all (non overridden) instances of event series in period, expanded by the actual event duration
                     */
                    long duration = event.getEndDate().getTimestamp() - event.getStartDate().getTimestamp();
                    Date iterateFrom = new Date(from.getTime() - duration);
                    Iterator<RecurrenceId> iterator = Services.getServiceLookup().getServiceSafe(RecurrenceService.class).iterateRecurrenceIds(new DefaultRecurrenceData(event), iterateFrom, until);
                    while (iterator.hasNext()) {
                        eventsInPeriod.add(new EventOccurrence(event, iterator.next()));
                    }
                } else {
                    eventsInPeriod.add(event);
                }
            }
            List<FreeBusyTime> freeBusyTimes = adjustToBoundaries(getFreeBusyTimes(eventsInPeriod, attendee, getTimeZone(attendee), (e, a) -> getResultingEvent(e, a, includeDetails)), from, until);
            freeBusyPerAttendee.put(attendee, merge && 1 < freeBusyTimes.size() ? mergeFreeBusy(freeBusyTimes) : freeBusyTimes);
        }
        /*
         * generate & return appropriate results
         */
        return getFreeBusyResults(attendees, freeBusyPerAttendee, warningsPerAttendee);
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
        Map<Attendee, EventsResult> resultsPerAttendee = new HashMap<Attendee, EventsResult>(attendees.size());
        /*
         * determine configured free/busy visibility for each queried attendee in the context, prepare result with warning if restricted
         */
        List<Attendee> consideredAttendees = new ArrayList<Attendee>(attendees.size());
        CalendarConfigImpl calendarConfig = new CalendarConfigImpl(resolver.getContextID(), Services.getServiceLookup());
        for (Attendee attendee : attendees) {
            /*
             * exclude non-internal attendees
             */
            if (false == isInternal(attendee)) {
                resultsPerAttendee.put(attendee, new DefaultEventsResult(getRestrictedVisibilityWarning(attendee)));
                continue;
            }
            /*
             * exclude users with restricted ('context-internal' or 'none') free/busy visibility
             */
            if (CalendarUserType.INDIVIDUAL.equals(attendee.getCuType()) && false == FreeBusyVisibility.ALL.equals(calendarConfig.getFreeBusyVisibility(attendee.getEntity()))) {
                resultsPerAttendee.put(attendee, new DefaultEventsResult(getRestrictedVisibilityWarning(attendee)));
                continue;
            }
            consideredAttendees.add(attendee);
        }
        /*
         * get overlapping events and derive results for considered attendees
         */
        String maskUid = params.isPresent() ? params.get().get(CalendarParameters.PARAMETER_MASK_UID, String.class) : null;
        Map<Attendee, List<Event>> overlappingEvents = new OverlappingEventsLoader(storage).load(
            consideredAttendees, from, until, (e, a) -> isVisible(e, maskUid) && includeForFreeBusy(e, a.getEntity()));
        for (Entry<Attendee, List<Event>> eventsForAttendee : overlappingEvents.entrySet()) {
            resultsPerAttendee.put(eventsForAttendee.getKey(), new DefaultEventsResult(eventsForAttendee.getValue()));
        }
        return resultsPerAttendee;
    }

    /**
     * Gets the timezone to consider for <i>floating</i> dates of a specific attendee.
     * <p/>
     * For <i>internal</i>, individual calendar user attendees, this is the configured timezone of the user; otherwise, the timezone of
     * the {@link CalendarParameters} are used or if not provided the UTC timezone is used
     *
     * @param attendee The attendee to get the timezone to consider for <i>floating</i> dates for
     * @return The timezone
     */
    private TimeZone getTimeZone(Attendee attendee) throws OXException {
        if (isInternal(attendee) && CalendarUserType.INDIVIDUAL.equals(attendee.getCuType())) {
            return resolver.getTimeZone(attendee.getEntity());
        }
        return getTimeZone();
    }

    /**
     * Gets the timezone from the {@link CalendarParameters} or return the UTC timezone
     *
     * @return The timezone
     */
    private TimeZone getTimeZone() {
        TimeZone result = params.isPresent() ? params.get().get(CalendarParameters.PARAMETER_TIMEZONE, TimeZone.class) : null;
        return result == null ? TimeZone.getTimeZone("UTC") : result;
    }

    /**
     * Adjusts an overlapping event to be used in free/busy results by preserving either {@link #RESTRICTED_FREEBUSY_FIELDS}, or
     * {@link #FREEBUSY_FIELDS} in the returned event copy.
     * 
     * @param event The event to adjust
     * @param attendee The attendee to adjust the event for
     * @param includeDetails <code>true</code> to include details for non-classified events, <code>false</code> to always stick to the restricted fields
     * @return The resulting event, or <code>null</code> if the event cannot be adjusted
     */
    private static Event getResultingEvent(Event event, Attendee attendee, boolean includeDetails) {
        EventField[] fields = includeDetails && isPublicClassification(event) ? FREEBUSY_FIELDS : RESTRICTED_FREEBUSY_FIELDS;
        try {
            Event copy = EventMapper.getInstance().copy(event, new Event(), fields);
            optFind(copy.getAttendees(), attendee).map(Attendee::getTransp).ifPresent(copy::setTransp);
            return copy;
        } catch (OXException e) {
            LOG.warn("Unexpected error adjusting event data {}", event, e);
        }
        return null;
    }

}
