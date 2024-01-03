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

package com.openexchange.chronos.scheduling.analyzers;

import static com.openexchange.chronos.common.CalendarUtils.contains;
import static com.openexchange.chronos.common.CalendarUtils.getEventsByUID;
import static com.openexchange.chronos.common.CalendarUtils.getOccurrence;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedPropertyValue;
import static com.openexchange.chronos.scheduling.analyzers.Utils.patchEvent;
import static com.openexchange.chronos.scheduling.common.Utils.optSentByResource;
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.b;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.DelegatingEvent;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.common.ChronosITipData;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.tools.functions.ErrorAwareBiFunction;

/**
 * {@link ObjectResourceProvider}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class ObjectResourceProvider {

    /** The default event fields to load when retrieving the currently stored calendar object resources from the storage */
    private static final EventField[] DEFAULT_FIELDS = { //@formatter:off
        EventField.ID, EventField.SERIES_ID, EventField.FOLDER_ID, EventField.UID, EventField.RECURRENCE_ID, EventField.RECURRENCE_RULE,
        EventField.RECURRENCE_DATES, EventField.DELETE_EXCEPTION_DATES, EventField.CHANGE_EXCEPTION_DATES, EventField.START_DATE, 
        EventField.SEQUENCE, EventField.DTSTAMP, EventField.ORGANIZER, EventField.ATTENDEES, EventField.SUMMARY
    }; //@formatter:on

    private final static Logger LOG = LoggerFactory.getLogger(ObjectResourceProvider.class);

    private final CalendarSession session;
    private final String uid;
    private final int calendarUserId;
    private final EventField[] fieldsToLoad;
    private final IncomingSchedulingMessage incomingMessage;

    private Optional<CalendarObjectResource> storedResource;
    private Optional<CalendarObjectResource> tombstoneResource;
    private Optional<CalendarObjectResource> storedRelatedResource;
    private Boolean usesOrganizerCopy;

    /**
     * Initializes a new {@link ObjectResourceProvider}.
     * <p/>
     * The currently stored calendar object resources are loaded with the {@link #DEFAULT_FIELDS} from the storage.
     * 
     * @param session The calendar session
     * @param incomingMessage The incoming scheduling message
     */
    public ObjectResourceProvider(CalendarSession session, IncomingSchedulingMessage incomingMessage) {
        this(session, incomingMessage, DEFAULT_FIELDS);
    }

    /**
     * Initializes a new {@link ObjectResourceProvider}.
     * 
     * @param session The calendar session
     * @param incomingMessage The incoming scheduling message
     * @param fieldsToLoad The event fields to load when retrieving the currently stored calendar object resources from the storage, or <code>null</code> to load all fields
     */
    public ObjectResourceProvider(CalendarSession session, IncomingSchedulingMessage incomingMessage, EventField[] fieldsToLoad) {
        super();
        this.session = session;
        this.incomingMessage = incomingMessage;
        this.uid = incomingMessage.getResource().getUid();
        int sentByResource = optSentByResource(session, incomingMessage);
        this.calendarUserId = 0 < sentByResource ? sentByResource : incomingMessage.getTargetUser();
        this.fieldsToLoad = fieldsToLoad;
    }

    /**
     * Optionally gets the {@link ChronosITipData} if present in the incoming scheduling message.
     * 
     * @return The optional ITip data, or empty if not set
     */
    public Optional<ChronosITipData> optItipData() {
        return getAdditional(ChronosITipData.PROPERTY_NAME, ChronosITipData.class);
    }

    /**
     * Gets a value indicating whether the incoming scheduling message originates from an <i>internal</i> notification mail of the current
     * session's context, or from a regular, <i>external</i> iTIP message or another context.
     * 
     * @return <code>true</code> if the message originates from an internal notification mail, <code>false</code>, otherwise
     */
    public boolean isInternalSchedulingResource() {
        return com.openexchange.chronos.scheduling.common.Utils.isInternalSchedulingResource(session, incomingMessage);
    }

    /**
     * Gets a value indicating whether the supplied scheduling message originates from an <i>internal</i> notification mail of the given
     * context, and if this message is targeting the (already stored) organizer copy.
     * <p/>
     * This is usually the case for most scheduling messages sent from/to calendar users within the same context, however, there are certain
     * exceptions which are checked in this method, which lead to an additional, detached attendee copy of the scheduling object resource:
     * <ul>
     * <li>The target (calendar) user of the message has no or had no calendar access</li>
     * <li>An uninvited user added a forwarded invitation to his calendar</li>
     * </ul>
     * 
     * @return <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>, if the
     *         scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     */
    public boolean usesOrganizerCopy() {
        if (null == usesOrganizerCopy) {
            ErrorAwareBiFunction<String, Organizer, CalendarObjectResource> storedResourceFunction = (uid, organizer) -> {
                /*
                 * use known stored resource if organizer matches this analysis' perspective
                 */
                if (organizer.getEntity() == calendarUserId && uid.equals(uid)) {
                    return getStoredResource();
                }
                /*
                 * fall back to regular retrieval, otherwise
                 */
                List<Event> events = getStoredEvents(session, EventField.UID, uid, organizer.getEntity(), false, new EventField[] { EventField.ATTENDEES, EventField.ORGANIZER });
                return null == events || events.isEmpty() ? null : new DefaultCalendarObjectResource(events);
            };
            usesOrganizerCopy = B(com.openexchange.chronos.scheduling.common.Utils.usesOrganizerCopy(session, incomingMessage, storedResourceFunction));
        }
        return b(usesOrganizerCopy);
    }

    /**
     * Get additional information.
     * 
     * @param key The key for the value
     * @param clazz The class the value has
     * @return An Optional holding the value casted to the given class
     * @param <T> The class of the returned object
     */
    public <T> Optional<T> getAdditional(String key, Class<T> clazz) {
        return incomingMessage.getAdditional(key, clazz);
    }

    /**
     * Gets the calendar object resource from the incoming scheduling message.
     * 
     * @return The incoming calendar object resource
     */
    public CalendarObjectResource getIncomingResource() {
        return incomingMessage.getResource();
    }

    /**
     * Gets the individual events from the incoming calendar object resource.
     * <p/>
     * For each of the returned events, the generic patch routine from {@link Utils#patchEvent} is applied implicitly.
     * 
     * @return The the individual events from the incoming calendar object resource
     */
    public List<Event> getIncomingEvents() {
        List<Event> events = getIncomingResource().getEvents();
        if (null == events || events.isEmpty()) {
            return events;
        }
        CalendarObjectResource resourceForPatching = optResourceForPatching();
        return events.stream().map((e) -> patchEvent(session, e, resourceForPatching, calendarUserId, usesOrganizerCopy())).collect(Collectors.toList());
    }

    /**
     * Resolves an UID to all stored events belonging to the corresponding calendar object resource. The lookup is performed case-
     * sensitive, within the scope of a specific calendar user. I.e., the unique identifier is resolved to events residing in the user's
     * <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The events will be <i>userized</i> to reflect the view of the calendar user on the events.
     * 
     * @return The <i>userized</i> events as calendar object resource, or <code>null</code> if no events were found
     * @throws OXException If loading of the events fails
     */
    public CalendarObjectResource getStoredResource() throws OXException {
        if (null == storedResource) {
            List<Event> events = getStoredEvents(EventField.UID, uid, false);
            storedResource = events.isEmpty() ? Optional.empty() : Optional.of(new DefaultCalendarObjectResource(events));
        }
        return storedResource.orElse(null);
    }

    /**
     * Looks up all stored events that are decorated with the same (non-empty) {@link EventField#RELATED_TO} value as the incoming resource.
     * <p/>
     * This may be used to find the accompanying event series after a split, via <code>RELTYPE=X-CALENDARSERVER-RECURRENCE-SET</code>.
     * <p/>The lookup is performed case-sensitive, within the scope of a specific calendar user. I.e., the related-to value matched against
     * events residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The events will be <i>userized</i> to reflect the view of the calendar user on the events.
     * 
     * @return The <i>userized</i> events as calendar object resource, or <code>null</code> if no events were found
     * @throws OXException If loading of the events fails
     */
    public CalendarObjectResource getStoredRelatedResource() throws OXException {
        if (null == storedRelatedResource) {
            Event firstEvent = incomingMessage.getResource().getSeriesMaster();
            if (null != firstEvent) {
                /*
                 * try and lookup original series via X-OX-SPLIT-FROM if available
                 */
                String relatedUid = optExtendedPropertyValue(firstEvent.getExtendedProperties(), "X-OX-SPLIT-FROM", String.class);
                if (Strings.isNotEmpty(relatedUid)) {
                    Map<String, List<Event>> relatedEventsByUID = getEventsByUID(getStoredEvents(EventField.UID, relatedUid, false), false);
                    if (false == relatedEventsByUID.isEmpty()) {
                        storedRelatedResource = Optional.of(new DefaultCalendarObjectResource(relatedEventsByUID.values().iterator().next()));
                    }
                }
                /*
                 * also try X-CALENDARSERVER-RECURRENCE-SET if set in RELATED-TO
                 */
                if (null == storedRelatedResource && null != firstEvent.getRelatedTo() && 
                    "X-CALENDARSERVER-RECURRENCE-SET".equals(firstEvent.getRelatedTo().getRelType())) {
                    Map<String, List<Event>> relatedEventsByUID = getEventsByUID(getStoredEvents(EventField.RELATED_TO, firstEvent.getRelatedTo(), false), false);
                    storedRelatedResource = Optional.of(new DefaultCalendarObjectResource(relatedEventsByUID.values().iterator().next()));
                }
            }
            if (null == storedRelatedResource) {
                /*
                 * unknown, otherwise
                 */
                storedRelatedResource = Optional.empty();
            }
        }
        return storedRelatedResource.orElse(null);
    }

    /**
     * Resolves an UID to all events belonging to the corresponding calendar object resource, as found in the <i>tombstone</i> storage.
     * The lookup is performed case-sensitive, within the scope of a specific calendar user. I.e., the unique identifier is resolved to
     * events that were previously residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The events will be <i>userized</i> to reflect the view of the calendar user on the events.
     * 
     * @return The <i>userized</i> event tombstones as calendar object resource, or <code>null</code> if no events were found
     * @throws OXException If loading of the events fails
     */
    public CalendarObjectResource getTombstoneResource() throws OXException {
        if (null == tombstoneResource) {
            List<Event> events = getStoredEvents(EventField.UID, uid, true);
            tombstoneResource = events.isEmpty() ? Optional.empty() : Optional.of(new DefaultCalendarObjectResource(events));
        }
        return tombstoneResource.orElse(null);
    }

    /**
     * Optionally gets the <i>matching</i> event or occurrence from the currently stored calendar object resource for a specific event
     * from an incoming scheduling message.
     * 
     * @param incomingEvent The incoming event to get the corresponding event for
     * @return The corresponding event (or virtual event occurrence), or <code>null</code> if none could be derived
     * @throws OXException If loading of the events fails
     */
    public Event optMatchingEvent(Event incomingEvent) throws OXException {
        return optMatchingEvent(session, incomingEvent, getStoredResource());
    }

    /**
     * Optionally gets the <i>matching</i> tombstone event or occurrence from the currently stored calendar object resource for a specific
     * event from an incoming scheduling message.
     * <p/>
     * In case no explicit tombstone record is stored, and the original resource's series master event contains the targeted occurrence as
     * delete exception date, a virtual tombstone for this occurrence is returned instead.
     * 
     * @param incomingEvent The incoming event to get the corresponding tombstone event for
     * @return The corresponding tombstone event (or virtual event occurrence), or <code>null</code> if none could be derived
     * @throws OXException If loading of the events fails
     */
    public Event optMatchingTombstone(Event incomingEvent) throws OXException {
        Event matchingTombstone = optMatchingEvent(session, incomingEvent, getTombstoneResource());
        if (null != matchingTombstone) {
            return matchingTombstone;
        }
        /*
         * also probe delete exception date of stored series master event if applicable
         */
        if (null != incomingEvent.getRecurrenceId()) {
            CalendarObjectResource storedResource = getStoredResource();
            if (null != storedResource && null != storedResource.getSeriesMaster() && 
                contains(storedResource.getSeriesMaster().getDeleteExceptionDates(), incomingEvent.getRecurrenceId())) {
                DelegatingEvent plainMasterEvent = new DelegatingEvent(storedResource.getSeriesMaster()) {
                    
                    @Override
                    public SortedSet<RecurrenceId> getDeleteExceptionDates() {
                        return null;
                    }
                };
                return optEventOccurrence(session, plainMasterEvent, incomingEvent.getRecurrenceId());
            }
        }
        return null;
    }

    /**
     * Optionally gets the <i>matching</i> event or occurrence from the given calendar object resource for a specific event from an
     * incoming scheduling message.
     * 
     * @param incomingEvent The incoming event to get the corresponding event for
     * @param resource The calendar object resource to get the matching event from, or <code>null</code> if there is none
     * @return The corresponding event (or virtual event occurrence), or <code>null</code> if none could be derived
     */
    private static Event optMatchingEvent(CalendarSession session, Event incomingEvent, CalendarObjectResource resource) {
        if (null == resource) {
            return null;
        }
        if (null != incomingEvent.getRecurrenceId()) {
            /*
             * match existing change exception or event occurrence
             */
            Event originalChangeException = resource.getChangeException(incomingEvent.getRecurrenceId());
            if (null != originalChangeException) {
                return originalChangeException;
            }
            if (null != resource.getSeriesMaster()) {
                return optEventOccurrence(session, resource.getSeriesMaster(), incomingEvent.getRecurrenceId());
            }
            return null;
        }
        if (null != resource.getFirstEvent() && null == resource.getFirstEvent().getRecurrenceId()) {
            /*
             * match series master or non-recurring
             */
            return resource.getFirstEvent();
        }
        return null;
    }

    private static Event optEventOccurrence(CalendarSession session, Event seriesMaster, RecurrenceId recurrenceId) {
        if (null == seriesMaster) {
            return null;
        }
        try {
            return getOccurrence(session.getRecurrenceService(), seriesMaster, recurrenceId);
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error preparing event occurrence for {} of {}", recurrenceId, seriesMaster, e);
            return null;
        }
    }

    private List<Event> getStoredEvents(EventField field, Object value, boolean tombstones) throws OXException {
        return getStoredEvents(session, field, value, calendarUserId, tombstones, fieldsToLoad);
    }

    /**
     * Looks up all events where a field matches a certain value within the scope of a specific calendar user, i.e. the lookup is done
     * across all events residing in the user's <i>personal</i>, as well as <i>public</i> calendar folders.
     * <p/>
     * The found events will be <i>userized</i> to reflect the view of the calendar user on the events.
     *
     * @param session The calendar session
     * @param field The event field to perform the lookup for
     * @param value The value to match
     * @param calendarUserId The identifier of the calendar user the value should be matched for
     * @param tombstones <code>true</code> to lookup event tombstones, <code>false</code> to lookup regular events
     * @param fields The fields to load, or <code>null</code> to load all event data
     * @return The resolved events, or an empty list if none were found
     */
    private static List<Event> getStoredEvents(CalendarSession session, EventField field, Object value, int calendarUserId, boolean tombstones, EventField[] fields) throws OXException {
        EventField[] oldParameterFields = session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class);
        try {
            session.set(CalendarParameters.PARAMETER_FIELDS, fields);
            return session.getCalendarService().getUtilities().lookupByField(session, field, value, calendarUserId, tombstones);
        } finally {
            session.set(CalendarParameters.PARAMETER_FIELDS, oldParameterFields);
        }
    }

    private CalendarObjectResource optResourceForPatching() {
        CalendarObjectResource resourceForPatching = null;
        try {
            resourceForPatching = getStoredResource();
        } catch (OXException e) {
            LOG.debug("Unable to get stored resource for patching", e);
        }
        if (null == resourceForPatching) {
            try {
                resourceForPatching = getTombstoneResource();
            } catch (OXException e) {
                LOG.debug("Unable to get tombstone resource for patching", e);
            }
        }
        return resourceForPatching;
    }

}
