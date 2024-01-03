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

import static com.openexchange.chronos.common.CalendarUtils.filter;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getFlags;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.optExtendedPropertyValue;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.scheduling.AnalyzedChange;
import com.openexchange.chronos.scheduling.ITipAnalysis;
import com.openexchange.chronos.scheduling.ITipChange;
import com.openexchange.chronos.scheduling.ITipChange.Type;
import com.openexchange.chronos.scheduling.ITipSequence;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.i18n.LocaleTools;
import com.openexchange.java.Strings;
import com.openexchange.tools.arrays.Collections;

/**
 * {@link Utils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class Utils {

    private final static Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * Safely gets the summary of the supplied event, falling back to an empty string if there is none.
     * 
     * @param event The event to get the summary from
     * @return The summary, or an empty string if there is none
     */
    public static String getSummary(Event event) {
        return null != event && Strings.isNotEmpty(event.getSummary()) ? event.getSummary() : "";
    }

    /**
     * Safely gets the summary of the supplied event, falling back to an alternative event and finally an empty string if there is none.
     * 
     * @param event The event to get the summary from
     * @param alternativeEvent The alternative event to get the summary from
     * @return The summary, or an empty string if there is none
     */
    public static String getSummary(Event event, Event alternativeEvent) {
        String summary = getSummary(event);
        return Strings.isNotEmpty(summary) ? summary : getSummary(alternativeEvent);
    }

    /**
     * Safely gets the locale of the calendar session user.
     * 
     * @param session The session to derive the locale for
     * @return The locale
     */
    public static Locale getLocale(CalendarSession session) {
        try {
            return session.getEntityResolver().getLocale(session.getUserId());
        } catch (Exception e) {
            LOG.warn("Unexpected error getting locale for session user, falling back to default locale.", e);
            return LocaleTools.DEFAULT_LOCALE;
        }
    }

    /**
     * Safely gets the timezone of the calendar session user.
     * 
     * @param session The session to derive the timezone for
     * @return The timezone
     */
    public static TimeZone getTimeZone(CalendarSession session) {
        try {
            return session.getEntityResolver().getTimeZone(session.getUserId());
        } catch (Exception e) {
            LOG.warn("Unexpected error getting timezone for session user, falling back to default timezone.", e);
            return TimeZone.getDefault();
        }
    }

    /**
     * Creates the resulting iTIP analysis from a list of individual changes for the targeted calendar object resource.
     * 
     * @param method The scheduling method to take over in the analysis
     * @param uid The unique identifier of the scheduling object resource
     * @param analyzedChanges The analyzed changes
     * @param originalResource The currently stored original calendar object resource, or <code>null</code> if there is none
     * @param storedRelatedResource The currently stored resource that is related to the incoming scheduling object resource via its
     *            {@link EventField#RELATED_TO}, or <code>null</code> if not applicable
     * @return The resulting iTIP analysis
     */
    protected static ITipAnalysis getAnalysis(SchedulingMethod method, String uid, List<AnalyzedChange> analyzedChanges, CalendarObjectResource originalResource, CalendarObjectResource storedRelatedResource) {
        ITipAnalysis analysis = new ITipAnalysis();
        analysis.setMethod(method);
        analysis.setUid(uid);
        analysis.setOriginalResource(originalResource);
        analysis.setStoredRelatedResource(storedRelatedResource);
        analysis.setChanges(analyzedChanges);
        analysis.setMainChange(findMainChange(analyzedChanges));
        return analysis;
    }

    /**
     * Prepares an analysis result representing insufficient permissions to process the incoming scheduling message.
     * 
     * @param method The scheduling method to take over in the analysis
     * @param uid The unique identifier of the scheduling object resource
     * @return The resulting iTIP analysis
     */
    protected static ITipAnalysis getInsufficientPermissionsAnalysis(SchedulingMethod method, String uid) {
        ITipAnalysis analysis = new ITipAnalysis();
        analysis.setMethod(method);
        analysis.setUid(uid);
        return analysis;
    }

    /**
     * Optionally gets a comment included in the supplied event's extended properties that may have been set by the scheduling message's
     * originator.
     * 
     * @param event The event to get the comment from
     * @return The optional comment, or an empty optional if there is none
     */
    public static Optional<String> optComment(Event event) {
        String value = optExtendedPropertyValue(event.getExtendedProperties(), "COMMENT", String.class);
        return Strings.isEmpty(value) ? Optional.empty() : Optional.of(value.trim());
    }

    /**
     * Creates a copy of an event from an incoming scheduling object resource and applies certain patches to it to ease further
     * processing. For <i>externally</i> organizied events, this includes:
     * <ul>
     * <li>adjusting the timezones referenced by the date properties to match a known and supported timezone</li>
     * <li>resolving the current calendar- and session user within the event's attendees and organizer</li>
     * <li>setting the event flags to provide hints for the client</li>
     * </ul>
     * For <i>internal</i> events, this means:
     * <ul>
     * <li>transferring known entity identifiers from the stored resource for matching attendees and organizer</li>
     * <li>resolving all remaining entities within the event's attendees and organizer</li>
     * <li>setting the event flags to provide hints for the client</li>
     * </ul>
     * 
     * @param session The underlying calendar session
     * @param event The event from the incoming scheduling object resource to patch
     * @param storedResource The currently stored calendar object resource the message is targeted at, or <code>null</code> if not applicable
     * @param calendarUserId The effective calendar user id
     * @param usesOrganizerCopy <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>,
     *            if the scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @return The patched event
     */
    static Event patchEvent(CalendarSession session, Event event, CalendarObjectResource storedResource, int calendarUserId, boolean usesOrganizerCopy) {
        Event originalEvent = null != storedResource ? storedResource.getFirstEvent() : null;
        try {
            Event patchedEvent = session.getUtilities().copyEvent(event, (EventField[]) null);
            int[] resolvableEntities;
            if (usesOrganizerCopy) {
                patchedEvent = transferEntities(originalEvent, patchedEvent);
                resolvableEntities = null;
            } else {
                session.getUtilities().adjustTimeZones(session.getSession(), calendarUserId, patchedEvent, originalEvent);
                resolvableEntities = new int[] { calendarUserId, session.getUserId() };
            }
            session.getEntityResolver().prepare(patchedEvent.getAttendees(), resolvableEntities);
            session.getEntityResolver().prepare(patchedEvent.getOrganizer(), CalendarUserType.INDIVIDUAL, resolvableEntities);
            patchedEvent.setFlags(getFlags(patchedEvent, calendarUserId, session.getUserId()));
            return patchedEvent;
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error patching {}, falling back to original representation", event, e);
            return event;
        }
    }
    
    /**
     * Transfers the entity identifiers of the calendar users within an original event to their matching ones in an incoming event of 
     * the same context.
     * 
     * @param originalEvent The original event to use for copying, or <code>null</code> for a no-op
     * @param event The event to set the entities in
     * @return The passed event
     */
    private static Event transferEntities(Event originalEvent, Event event) {
        if (null != originalEvent) {
            if (null != event.getOrganizer() && false == isInternal(event.getOrganizer(), CalendarUserType.INDIVIDUAL) && 
                null != originalEvent.getOrganizer() && isInternal(originalEvent.getOrganizer(), CalendarUserType.INDIVIDUAL) && 
                matches(event.getOrganizer(), originalEvent.getOrganizer())) {
                event.getOrganizer().setEntity(originalEvent.getOrganizer().getEntity());
            }
            if (null != event.getAttendees()) {
                for (Attendee attendee : event.getAttendees()) {
                    Attendee matchingAttendee = find(originalEvent.getAttendees(), attendee);
                    if (null != matchingAttendee && isInternal(matchingAttendee) && false == isInternal(attendee)) {
                        attendee.setEntity(matchingAttendee.getEntity());
                    }
                }
            }
        }
        return event;
    }

    /**
     * Selects the originating attendee from an incoming event, based on the scheduling messages originator.
     * 
     * @param incomingEvent The incoming event to select the attendee in
     * @param originator The originator of the scheduling message
     * @return The attendee, or <code>null</code> if no matching attendee is present in the event
     */
    static Attendee selectAttendee(Event incomingEvent, CalendarUser originator) {
        return selectAttendee(incomingEvent, originator, false);
    }

    /**
     * Selects the originating attendee from an incoming event, based on the scheduling messages originator.
     * 
     * @param incomingEvent The incoming event to select the attendee in
     * @param originator The originator of the scheduling message
     * @param mustMatch <code>true</code> if the attende <i>must</i> match the originator, <code>false</code> to allow falling back to the first/only attendee
     * @return The attendee, or <code>null</code> if no matching attendee is present in the event
     */
    static Attendee selectAttendee(Event incomingEvent, CalendarUser originator, boolean mustMatch) {
        List<Attendee> attendees = incomingEvent.getAttendees();
        if (null != attendees && 0 < attendees.size()) {
            /*
             * select attendee based on originator if applicable
             */
            if (null != originator) {
                if (mustMatch) {
                    return find(attendees, originator);
                }
                if (1 < attendees.size()) {
                    Attendee attendee = find(attendees, originator);
                    if (null != attendee) {
                        return attendee;
                    }
                }
            }
            /*
             * use first / only attendee, otherwise
             */
            return attendees.get(0);
        }
        return null;
    }

    /**
     * Gets an attendee representation for the supplied calendar user.
     * 
     * @param calendarUser The calendar user to get the attendee from
     * @param cuType The calendar user type to apply
     * @return The attendee
     */
    static Attendee asAttendee(CalendarUser calendarUser, CalendarUserType cuType) {
        if (null == calendarUser) {
            return null;
        }
        Attendee attendee = new Attendee();
        attendee.setCuType(cuType);
        attendee.setCn(calendarUser.getCn());
        attendee.setEMail(calendarUser.getEMail());
        attendee.setEntity(calendarUser.getEntity());
        attendee.setUri(calendarUser.getUri());
        attendee.setSentBy(calendarUser.getSentBy());
        return attendee;
    }

    /**
     * Finds the change that best fits the main change. Can be either
     * <li> the only change available for non-series event</li>
     * <li> the change about the master</li>
     * <li> any change about an exception, if no master change is present</li>
     * 
     * @param analysis The analysis
     * @return The single main change, or <code>null</code> if the changes are empty
     */
    private static AnalyzedChange findMainChange(List<AnalyzedChange> analyzedChanges) {
        if (Collections.isNullOrEmpty(analyzedChanges)) {
            return null;
        }
        /*
         * No series or single event instance
         */
        if (analyzedChanges.size() == 1) {
            return analyzedChanges.get(0);
        }
        /*
         * Search for a master event
         */
        for (AnalyzedChange change : analyzedChanges) {
            ITipChange iTipChange = change.getChange();
            Event newEvent = iTipChange.getNewEvent();
            if (Type.UPDATE.equals(iTipChange.getType()) || Type.CREATE.equals(iTipChange.getType())) {
                if (CalendarUtils.looksLikeSeriesMaster(newEvent)) {
                    return change;
                }
            }
        }
        /*
         * Fallback to describe only the first change
         */
        return analyzedChanges.get(0);
    }

    /**
     * Gets a value indicating whether an incoming event is considered to have the ITip revision as the corresponding stored event copy.
     * <p/>
     * Comparison is performed using both the values of {@link EventField#SEQUENCE} and {@link EventField#DTSTAMP} in case the stored
     * event is organized by an <i>external</i> entity, but only considers {@link EventField#SEQUENCE} for <i>internally</i> organized
     * events as their {@link EventField#DTSTAMP} is incremented for non-consequential changes as well.
     * 
     * @param event The event from the incoming scheduling object resource
     * @param storedEvent The corresponding stored event or occurrence
     * @return <code>true</code> if the ITip revision of both events matches, <code>false</code>, otherwise
     */
    static boolean matchesITipRevision(Event event, Event storedEvent) {
        if (isInternal(event.getOrganizer(), CalendarUserType.INDIVIDUAL) && event.getSequence() == storedEvent.getSequence()) {
            return true;
        }
        if (ITipSequence.of(storedEvent).equals(ITipSequence.of(event))) {
            return true;
        }
        return false;
    }

    /**
     * Looks up the attendee from the given event whose <code>SENT-BY</code> property matches the originator.
     * <p/>
     * Within internal scheduling messages to/from booking delegates of managed resources, this is the targeted resource attendee.
     * 
     * @param event The event to lookup the resource attendee in
     * @param originator The originator of the scheduling message
     * @return The resource attendee sent by the originator, or <code>null</code> if none was found
     */
    static Attendee optResourceSentBy(Event event, CalendarUser originator) {
        for (Attendee resourceAttendee : filter(event.getAttendees(), null, CalendarUserType.RESOURCE, CalendarUserType.ROOM)) {
            if (matches(resourceAttendee.getSentBy(), originator)) {
                return resourceAttendee;
            }
        }
        return null;
    }

}
