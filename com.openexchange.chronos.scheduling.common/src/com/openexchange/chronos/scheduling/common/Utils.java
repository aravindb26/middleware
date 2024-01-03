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

package com.openexchange.chronos.scheduling.common;

import static com.openexchange.chronos.common.CalendarUtils.filter;
import static com.openexchange.chronos.common.CalendarUtils.find;
import static com.openexchange.chronos.common.CalendarUtils.getURI;
import static com.openexchange.chronos.common.CalendarUtils.isAttendeeSchedulingResource;
import static com.openexchange.chronos.common.CalendarUtils.isInternal;
import static com.openexchange.chronos.common.CalendarUtils.isResourceOrRoom;
import static com.openexchange.chronos.common.CalendarUtils.matches;
import static com.openexchange.chronos.common.CalendarUtils.optEMailAddress;
import static com.openexchange.java.Autoboxing.I;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;
import javax.mail.internet.AddressException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.CalendarObjectResource;
import com.openexchange.chronos.CalendarUser;
import com.openexchange.chronos.CalendarUserType;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.RecurrenceId;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.DefaultCalendarObjectResource;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.changes.Change;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.service.CalendarSession;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.tools.arrays.Collections;
import com.openexchange.tools.functions.ErrorAwareBiFunction;

/**
 * {@link Utils}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    private Utils() {}

    /**
     * Selects a specific event from a calendar object resource that is referenced by the supplied list of changes.
     *
     * @param resource The calendar object resource to select the event from
     * @param changes The changes for which to get the event for
     * @return The described event, or the resource's first event if no better match could be selected
     */
    public static Event selectDescribedEvent(CalendarObjectResource resource, List<Change> changes) {
        if (Collections.isNotEmpty(changes)) {
            RecurrenceId recurrenceId = changes.get(0).getRecurrenceId();
            if (null != recurrenceId) {
                Event event = resource.getChangeException(recurrenceId);
                if (null != event) {
                    return event;
                }
            }
        }
        return resource.getFirstEvent();
    }

    /**
     * Gets a calendar user's display name, falling back to his e-mail or URI properties as needed.
     *
     * @param calendarUser The calendar user to get the display name from
     * @return The display name
     */
    public static String getDisplayName(CalendarUser calendarUser) {
        if (Strings.isNotEmpty(calendarUser.getCn())) {
            return calendarUser.getCn();
        }
        if (Strings.isNotEmpty(calendarUser.getEMail())) {
            return calendarUser.getEMail();
        }
        return CalendarUtils.extractEMailAddress(calendarUser.getUri());
    }

    /**
     * Gets a value indicating whether a calendar user represents an <i>internal</i> entity, an internal user, group or resource , or not.
     *
     * @param calendarUser The calendar user to check
     * @return <code>true</code> if the calendar user is internal, <code>false</code>, otherwise
     */
    public static boolean isInternalCalendarUser(CalendarUser calendarUser) {
        if (Attendee.class.isAssignableFrom(calendarUser.getClass())) {
            Attendee attendee = (Attendee) calendarUser;
            return CalendarUtils.isInternalUser(attendee);
        }
        return CalendarUtils.isInternal(calendarUser, CalendarUserType.INDIVIDUAL);
    }

    /**
     * Gets a value indicating whether the calendar user is resource or not
     *
     * @param calendarUser The calendar user
     * @param attendees The attendees in the event
     * @return <code>true</code> If the calendar user is an internal resource, <code>false</code> otherwise
     */
    public static boolean isResource(CalendarUser calendarUser, List<Attendee> attendees) {
        if (null == calendarUser || calendarUser.getEntity() <= 0) {
            return false;
        }
        if (calendarUser instanceof Attendee attendee) {
            return CalendarUtils.isResourceOrRoom(attendee);
        }
        return filter(attendees, Boolean.TRUE, CalendarUserType.RESOURCE, CalendarUserType.ROOM).stream()//Filter for internal resources
            .anyMatch(a -> a.getEntity() == calendarUser.getEntity());
    }

    /**
     * Optionally gets the internal entity identifier of the resource represented by the supplied calendar user.
     *
     * @param calendarUser The calendar user
     * @param event The event the calendar user possibly attends in
     * @return The resource identifier if the supplied calendar user denotes an internal resource, or <code>-1</code>, otherwise
     */
    public static int optResourceId(CalendarUser calendarUser, Event event) {
        if (null == calendarUser) {
            return -1;
        }
        if (calendarUser instanceof Attendee attendee && isInternal(attendee) && isResourceOrRoom(attendee)) {
            return attendee.getEntity();
        }
        if (null != event) {
            Attendee matchingAttendee = find(event.getAttendees(), calendarUser);
            if (null != matchingAttendee && isInternal(matchingAttendee) && isResourceOrRoom(matchingAttendee)) {
                return matchingAttendee.getEntity();
            }
        }
        return -1;
    }

    /**
     * Gets a value indicating whether the supplied scheduling message originates from an <i>internal</i> notification mail of the given
     * context, or from a regular, <i>external</i> iTIP message or another context.
     * 
     * @param session The calendar session
     * @param message The incoming scheduling message to check
     * @return <code>true</code> if the message originates from an internal notification mail, <code>false</code>, otherwise
     */
    public static boolean isInternalSchedulingResource(CalendarSession session, IncomingSchedulingMessage message) {
        return isInternalSchedulingResource(session.getServerUid(), session.getContextId(), message);
    }

    /**
     * Gets a value indicating whether the supplied scheduling message originates from an <i>internal</i> notification mail of the given
     * context, or from a regular, <i>external</i> iTIP message or another context.
     * 
     * @param serverUid The unique identifier of the current server
     * @param contextId The identifier of the current context
     * @param message The incoming scheduling message to check
     * @return <code>true</code> if the message originates from an internal notification mail, <code>false</code>, otherwise
     */
    public static boolean isInternalSchedulingResource(String serverUid, int contextId, IncomingSchedulingMessage message) {
        return null != optMatchingITipData(serverUid, contextId, message);
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
     * @param session The calendar session
     * @param message The incoming scheduling message to evaluate
     * @return <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>, if the
     *         scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @see Utils#isInternalSchedulingResource(String, int, IncomingSchedulingMessage)
     */
    public static boolean usesOrganizerCopy(CalendarSession session, IncomingSchedulingMessage message) {
        ErrorAwareBiFunction<String, Organizer, CalendarObjectResource> storedResourceFunction = (uid, organizer) -> {
            EventField[] oldParameterFields = session.get(CalendarParameters.PARAMETER_FIELDS, EventField[].class);
            try {
                session.set(CalendarParameters.PARAMETER_FIELDS, new EventField[] { EventField.ATTENDEES, EventField.ORGANIZER });
                List<Event> events = session.getCalendarService().getUtilities().resolveEventsByUID(session, uid, organizer.getEntity());
                return null != events && 0 < events.size() ? new DefaultCalendarObjectResource(events) : null;
            } finally {
                session.set(CalendarParameters.PARAMETER_FIELDS, oldParameterFields);
            }
        };
        return usesOrganizerCopy(session, message, storedResourceFunction);
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
     * @param session The calendar session
     * @param message The incoming scheduling message to evaluate
     * @param storedResourceFunction A function to retrieve the stored organizer copy of the scheduling object resource, taking the
     *            resource's UID and organizer as input parameters
     * @return <code>true</code> if the scheduling message refers to the (already stored) organizer copy, <code>false</code>, if the
     *         scheduling message refers to an <i>externally</i> organized event, or a detached attendee copy is used for the targeted user
     * @see Utils#isInternalSchedulingResource(String, int, IncomingSchedulingMessage)
     */
    public static boolean usesOrganizerCopy(CalendarSession session, IncomingSchedulingMessage message, ErrorAwareBiFunction<String, Organizer, CalendarObjectResource> storedResourceFunction) {
        /*
         * a shared organizer copy is only applicable for 'internal' scheduling/notification messages
         */
        if (false == isInternalSchedulingResource(session, message)) {
            return false;
        }
        /*
         * only applicable for 'internal' organizers
         */
        Organizer organizer = message.getResource().getOrganizer();
        try {
            organizer = session.getEntityResolver().prepare(new Organizer(organizer), CalendarUserType.INDIVIDUAL);
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error resolving {}, assuming to be 'external'.", organizer, e);
            return false;
        }
        if (false == isInternal(organizer, CalendarUserType.INDIVIDUAL)) {
            return false;
        }
        /*
         * implicitly uses the organizer copy if effective target calendar user is the organizer
         */
        int sentByResource = optSentByResource(session, message);
        int targetUserId = 0 < sentByResource ? sentByResource : message.getTargetUser();
        if (matches(organizer, targetUserId)) {
            return true;
        }
        /*
         * otherwise, organizer copy is used if targeted calendar user is 'resolved' as internal attendee in stored organizer resource
         */
        CalendarObjectResource storedOrganizerResource;
        try {
            storedOrganizerResource = storedResourceFunction.apply(message.getResource().getUid(), organizer);
        } catch (OXException e) {
            session.addWarning(e);
            LOG.warn("Unexpected error looking up organizer event copy for {}, assuming to be 'external'.", message.getSchedulingObject(), e);
            return false;
        }
        return null != storedOrganizerResource && isAttendeeSchedulingResource(storedOrganizerResource, targetUserId);
    }

    /**
     * Optionally gets the identifier of the context-internal resource for which the supplied scheduling message was sent.
     * 
     * @param session The calendar session
     * @param message The incoming scheduling message to extract the optional resource identifier from
     * @return The resource identifier, or <code>-1</code> if not found or applicable
     */
    public static int optSentByResource(CalendarSession session, IncomingSchedulingMessage message) {
        return optSentByResource(session.getServerUid(), session.getContextId(), message);
    }

    /**
     * Optionally gets the identifier of the context-internal resource for which the supplied scheduling message was sent.
     * 
     * @param contextId The identifier of the current context
     * @param message The incoming scheduling message to extract the optional resource identifier from
     * @return The resource identifier, or <code>-1</code> if not found or applicable
     */
    public static int optSentByResource(String serverUid, int contextId, IncomingSchedulingMessage message) {
        ChronosITipData iTipData = optMatchingITipData(serverUid, contextId, message);
        return null != iTipData ? iTipData.getSentByResource() : -1;
    }

    private static ChronosITipData optMatchingITipData(String serverUid, int contextId, IncomingSchedulingMessage message) {
        if (null != message) {
            Optional<ChronosITipData> optITipData = message.getAdditional(ChronosITipData.PROPERTY_NAME, ChronosITipData.class);
            if (optITipData.isPresent()) {
                ChronosITipData iTipData = optITipData.get();
                if (iTipData.matches(serverUid, contextId)) {
                    return iTipData;
                }
            }
        }
        return null;
    }

    /**
     * Tries to generate a {@link QuotedInternetAddress} based on the common name of the
     * given calendar user
     *
     * @param calendarUser The calendar user to generate the address for
     * @return The address as {@link String}
     */
    public static String getQuotedAddress(CalendarUser calendarUser) throws OXException {
        return getQuotedAddress(calendarUser, false);
    }

    /**
     * Tries to generate a {@link QuotedInternetAddress} based on the common name of the given calendar user.
     * <p/>
     * Optionally falls back to the calendar user's email address, which might be an option for internal delivery of notification mails.
     *
     * @param calendarUser The calendar user to generate the address for
     * @param fallbackToEMail <code>true</code> if the calendar user's email property may get used in case no <code>mailto:</code> URI
     *            is set, <code>false</code>, otherwise
     * @return The address as {@link String}
     */
    public static String getQuotedAddress(CalendarUser calendarUser, boolean fallbackToEMail) throws OXException {
        try {
            return getQuotedAddress(calendarUser.getCn(), calendarUser.getUri(), calendarUser.getEntity());
        } catch (OXException e) {
            if (CalendarExceptionCodes.INVALID_CALENDAR_USER.equals(e) && fallbackToEMail && Strings.isNotEmpty(calendarUser.getEMail())) {
                LOG.debug("Could not get quoted address from URI {} for {}, trying email as fallback", calendarUser.getUri(), calendarUser, e);
                try {
                    return getQuotedAddress(calendarUser.getCn(), getURI(calendarUser.getEMail()), calendarUser.getEntity());
                } catch (Exception x) {
                    LOG.debug("Could not get quoted address from email fallback {} for {}", calendarUser.getEMail(), calendarUser, x);
                }
            }
            throw e;
        }
    }

    /**
     * Tries to generate a {@link QuotedInternetAddress} based on the common name of the
     * given calendar user
     *
     * @param displayName The display name to use for the address
     * @param uri The URI to extract the mail address from
     * @param entity The senders entity identifier
     * @return The address as {@link String}
     */
    public static String getQuotedAddress(String displayName, String uri, int entity) throws OXException {
        String email = optEMailAddress(uri);
        if (Strings.isEmpty(email)) {
            throw CalendarExceptionCodes.INVALID_CALENDAR_USER.create(uri, I(entity), "");
        }
        if (displayName != null) {
            try {
                return new QuotedInternetAddress(email, displayName, "UTF-8").toUnicodeString();
            } catch (AddressException | UnsupportedEncodingException e) {
                LOG.warn("Interned address could not be generated. Returning fall-back instead.", e);
                return "\"" + displayName + "\"" + " <" + email + ">";
            }
        }
        // Without personal part
        return email;
    }

}
