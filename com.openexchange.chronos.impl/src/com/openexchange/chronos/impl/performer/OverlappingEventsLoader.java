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

import static com.openexchange.chronos.common.CalendarUtils.filter;
import static com.openexchange.chronos.common.CalendarUtils.getFields;
import static com.openexchange.chronos.common.CalendarUtils.getObjectIDs;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import static com.openexchange.java.Autoboxing.i2I;
import static com.openexchange.tools.arrays.Collections.put;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Transp;
import com.openexchange.chronos.service.SearchOptions;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.exception.OXException;

/**
 * {@link OverlappingEventsLoader}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class OverlappingEventsLoader {

    private final CalendarStorage storage;

    /**
     * Initializes a new {@link OverlappingEventsLoader}.
     *
     * @param storage The underlying calendar storage
     */
    public OverlappingEventsLoader(CalendarStorage storage) {
        super();
        this.storage = storage;
    }

    /**
     * Gets a list of overlapping events in a certain range.
     * <li>The found events are not anonymized or adjusted to represent a certain calendar folder view on the events.</li>
     * <li>{@link Transp#TRANSPARENT} events are skipped implicitly</li>
     *
     * @param attendees The attendees to query free/busy information for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @return The overlapping plain event data
     * @throws OXException In case of error during the load
     */
    public List<Event> loadEvents(Collection<Attendee> attendees, Date from, Date until) throws OXException {
        return loadEvents(attendees, from, until, false);
    }

    /**
     * Gets a list of overlapping events in a certain range.
     * <li>The found events are not anonymized or adjusted to represent a certain calendar folder view on the events.</li>
     *
     * @param attendees The attendees to query free/busy information for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @param includeTransparent If set to <code>false</code> {@link Transp#TRANSPARENT} events are skipped implicitly
     * @return The overlapping plain event data
     * @throws OXException In case of error during the load
     */
    public List<Event> loadEvents(Collection<Attendee> attendees, Date from, Date until, boolean includeTransparent) throws OXException {
        /*
         * prepare & filter internal attendees for lookup
         */
        List<Attendee> internalAttendees = filter(attendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL, CalendarUserType.RESOURCE, CalendarUserType.GROUP);
        if (0 == internalAttendees.size()) {
            return Collections.emptyList();
        }
        /*
         * search (potentially) overlapping events for the attendees
         */
        return getEventsInPeriod(storage, includeTransparent, from, until, internalAttendees);
    }

    /**
     * Gets a list of overlapping events in a certain range for each requested attendee.
     * <p/>
     * The found events are <b>NOT</b> anonymized or adjusted to represent a certain calendar folder view on the events.
     *
     * @param attendees The attendees to query free/busy information for
     * @param from The start date of the period to consider
     * @param until The end date of the period to consider
     * @param includePredicate An optional additional predicate to decide whether an event is included for an attendee or not
     * @param additionalAttendeeEntities The entity identifiers of additional internal attendees to load the data for, e.g. to always
     *            include the current user in case he attends
     * @return The overlapping plain event data, mapped to each attendee
     * @throws OXException In case of error during the load
     */
    public Map<Attendee, List<Event>> load(Collection<Attendee> attendees, Date from, Date until, BiPredicate<Event, Attendee> includePredicate, int... additionalAttendeeEntities) throws OXException {
        /*
         * prepare & filter internal attendees for lookup
         */
        List<Attendee> internalAttendees = filter(attendees, Boolean.TRUE, CalendarUserType.INDIVIDUAL, CalendarUserType.RESOURCE, CalendarUserType.GROUP);
        if (0 == internalAttendees.size()) {
            return Collections.emptyMap();
        }
        Map<Attendee, List<Event>> eventsPerAttendee = new HashMap<Attendee, List<Event>>(attendees.size());
        for (Attendee attendee : attendees) {
            eventsPerAttendee.put(attendee, new ArrayList<Event>());
        }
        /*
         * search (potentially) overlapping events for the attendees
         */
        List<Event> eventsInPeriod = getEventsInPeriod(storage, true, from, until, internalAttendees, additionalAttendeeEntities);
        if (0 == eventsInPeriod.size()) {
            return eventsPerAttendee;
        }
        /*
         * build response per attendee
         */
        for (Event eventInPeriod : eventsInPeriod) {
            for (Attendee attendee : internalAttendees) {
                if (null == includePredicate || includePredicate.test(eventInPeriod, attendee)) {
                    put(eventsPerAttendee, attendee, eventInPeriod);
                }
            }
        }
        return eventsPerAttendee;
    }

    private static List<Event> getEventsInPeriod(CalendarStorage storage, boolean includeTransparent, Date from, Date until, List<Attendee> internalAttendees, int... additionalAttendeeEntities) throws OXException {
        SearchOptions searchOptions = new SearchOptions().setRange(from, until);
        EventField[] fields = getFields(FreeBusyPerformerUtil.FREEBUSY_FIELDS, EventField.ORGANIZER, EventField.DELETE_EXCEPTION_DATES, EventField.CHANGE_EXCEPTION_DATES, EventField.RECURRENCE_ID);
        List<Event> eventsInPeriod = storage.getEventStorage().searchOverlappingEvents(internalAttendees, includeTransparent, searchOptions, fields);
        if (0 < eventsInPeriod.size()) {
            Map<String, List<Attendee>> attendeesById = storage.getAttendeeStorage().loadAttendees(getObjectIDs(eventsInPeriod), getEntities(internalAttendees, additionalAttendeeEntities));
            for (Event event : eventsInPeriod) {
                event.setAttendees(attendeesById.get(event.getId()));
            }
        }
        return eventsInPeriod;
    }

    private static int[] getEntities(List<Attendee> attendees, int... additionalEntities) {
        if (null == attendees) {
            return null == additionalEntities ? new int[0] : additionalEntities;
        }
        Set<Integer> entities = new HashSet<Integer>(attendees.size());
        for (Attendee attendee : attendees) {
            if (isInternal(attendee)) {
                entities.add(I(attendee.getEntity()));
            }
        }
        if (null != additionalEntities && 0 < additionalEntities.length) {
            entities.addAll(Arrays.asList(i2I(additionalEntities)));
        }
        return I2i(entities);
    }

}
