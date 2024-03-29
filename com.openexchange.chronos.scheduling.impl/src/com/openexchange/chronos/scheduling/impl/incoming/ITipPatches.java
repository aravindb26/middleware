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

package com.openexchange.chronos.scheduling.impl.incoming;

import static com.openexchange.tools.arrays.Collections.isNullOrEmpty;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attendee;
import com.openexchange.chronos.AttendeeField;
import com.openexchange.chronos.Calendar;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.ExtendedPropertyParameter;
import com.openexchange.chronos.Organizer;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.common.mapping.AttendeeMapper;
import com.openexchange.chronos.common.mapping.EventMapper;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.exception.ProblemSeverity;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tools.mappings.Mapping;
import com.openexchange.java.Strings;
import com.openexchange.tools.arrays.Collections;

/**
 * {@link ITipPatches}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class ITipPatches {

    private static final String GOOGLE_COMMENT = "X-RESPONSE-COMMENT";
    private static final String COMMENT = "COMMENT";
    private static final Logger LOG = LoggerFactory.getLogger(ITipPatches.class);

    /**
     * Applies all known patches for an imported iTIP message.
     *
     * @param importedCalendar The calendar representing the iTIP message
     * @return The (possibly patched) calendar
     */
    public static ImportedCalendar applyAll(ImportedCalendar importedCalendar) {
        if (null == importedCalendar) {
            return importedCalendar;
        }
        List<Event> events = importedCalendar.getEvents();
        if (null == events || events.isEmpty()) {
            return importedCalendar;
        }
        List<OXException> warnings = importedCalendar.getWarnings();
        events = CalendarUtils.sortSeriesMasterFirst(new ArrayList<Event>(events));
        /*
         * apply common patches
         */
        copyCommentToAttendee(events, importedCalendar);
        copySequenceToAttendee(events, importedCalendar.getMethod());
        /*
         * apply microsoft-specific patches if applicable
         */
        if (looksLikeMicrosoft(importedCalendar)) {
            SchedulingMethod method = SchedulingMethod.valueOf(importedCalendar.getMethod());
            events = removeOverriddenInstanceLeftovers(method, events, warnings);
            events = decodeMimeGarbage(events, warnings);
            ensureOrganizer(events);
            ensureAtteendees(events);
        }
        /*
         * initialize & return new calendar based on patched events
         */
        Calendar patchedCalendar = new Calendar(importedCalendar);
        patchedCalendar.setEvents(events);
        return new ImportedCalendar(patchedCalendar, warnings);
    }

    /**
     * Copies the comment for an event to the corresponding attendee in case of
     * an {@link SchedulingMethod#REPLY}
     *
     * @param events The events to copy the comment from
     * @param calendar The imported calendar
     */
    private static void copyCommentToAttendee(List<Event> events, ImportedCalendar calendar) {
        String method = calendar.getMethod();
        boolean looksLikeGoogle = looksLikeGoogle(calendar);
        if (method == null || false == SchedulingMethod.REPLY.name().equals(method.toUpperCase())) {
            return;
        }
        for (Event event : events) {
            /*
             * Check if applicable
             */
            if (null == event.getAttendees() || event.getAttendees().size() != 1) {
                return;
            }
            Attendee replyingAttendee = event.getAttendees().get(0);
            if (null != event.getExtendedProperties() && null != event.getExtendedProperties().get(COMMENT)) {
                /*
                 * Move comment to the corresponding replying attendee and remove from event
                 */
                Object comment = event.getExtendedProperties().get(COMMENT).getValue();
                if (null != comment && Strings.isNotEmpty(comment.toString())) {
                    replyingAttendee.setComment(comment.toString());
                }
                event.getExtendedProperties().removeAll(COMMENT);
            } else if (looksLikeGoogle && Collections.isNotEmpty(replyingAttendee.getExtendedParameters())) {
                /*
                 * Find and move Google specific comment
                 */
                for (ExtendedPropertyParameter parameter : replyingAttendee.getExtendedParameters()) {
                    if (GOOGLE_COMMENT.equals(parameter.getName())) {
                        replyingAttendee.setComment(parameter.getValue());
                        break;
                    }
                }
            }
        }
    }

    /**
     * Copies the sequence for an event to the corresponding attendee in case of
     * an {@link SchedulingMethod#REPLY}
     *
     * @param events The events to copy the comment from
     * @param method The calendar method
     */
    private static void copySequenceToAttendee(List<Event> events, String method) {
        if (method == null || false == SchedulingMethod.REPLY.name().equals(method.toUpperCase())) {
            return;
        }
        for (Event event : events) {
            /*
             * Check if applicable
             */
            if (null == event.getAttendees() // @formatter:off
                || event.getAttendees().size() != 1
                || false == event.containsSequence()) {  // @formatter:on
                return;
            }
            /*
             * Set sequence to replying attendee
             */
            Attendee replyingAttendee = event.getAttendees().get(0);
            replyingAttendee.setSequence(event.getSequence());
        }
    }

    /**
     * Removes leftovers from overridden instances of a recurring event series, that sometimes are sent by Microsoft to attendees that
     * were previously invited to single instances only. These incomplete event neither contain the organizer or attendees, and have an
     * incorrect recurrence id value set to the 00:00 local time.
     * <p/>
     * <b>Example:</b>
     * <pre>
     * BEGIN:VEVENT
     * SUMMARY:
     * DTSTART;TZID=W. Europe Standard Time:20210420T090000
     * DTEND;TZID=W. Europe Standard Time:20210420T093000
     * UID:040000008200E00074C5B7101A82E00800000000EC4CA982E931D701000000000000000
     * 0100000008108FCBB3F72F046A2D505AD100241B8
     * RECURRENCE-ID;TZID=W. Europe Standard Time:20210420T000000
     * CLASS:PUBLIC
     * PRIORITY:5
     * DTSTAMP:20210415T114505Z
     * TRANSP:OPAQUE
     * STATUS:CONFIRMED
     * SEQUENCE:0
     * LOCATION:e
     * X-MICROSOFT-CDO-APPT-SEQUENCE:0
     * X-MICROSOFT-CDO-BUSYSTATUS:BUSY
     * X-MICROSOFT-CDO-INTENDEDSTATUS:BUSY
     * X-MICROSOFT-CDO-ALLDAYEVENT:FALSE
     * X-MICROSOFT-CDO-IMPORTANCE:1
     * X-MICROSOFT-CDO-INSTTYPE:0
     * X-MICROSOFT-DISALLOW-COUNTER:FALSE
     * END:VEVENT
     * </pre>
     *
     * @param method The iTIP method as indicated by the imported calendar
     * @param events The events to patch
     * @param warnings A reference to collect warnings
     * @return The (possibly patched) list of events
     */
    private static List<Event> removeOverriddenInstanceLeftovers(SchedulingMethod method, List<Event> events, List<OXException> warnings) {
        if (SchedulingMethod.REQUEST.equals(method) || SchedulingMethod.CANCEL.equals(method)) {
            for (Iterator<Event> iterator = events.iterator(); iterator.hasNext();) {
                Event event = iterator.next();
                if (null != event.getRecurrenceId()) {
                    if (null == event.getOrganizer()) {
                        addInvalidDataWarning(warnings, event, EventField.ORGANIZER, "Ignoring overridden instance without organizer");
                        iterator.remove();
                    } else if (isNullOrEmpty(event.getAttendees())) {
                        addInvalidDataWarning(warnings, event, EventField.ATTENDEES, "Ignoring overridden instance without attendees");
                        iterator.remove();
                    } else if (Strings.isEmpty(event.getUid())) {
                        addInvalidDataWarning(warnings, event, EventField.UID, "Ignoring overridden instance without uid");
                        iterator.remove();
                    }
                }
            }
        }
        return events;
    }

    /**
     * Performs a prophylactical decoding of potentially MIME-encoded strings in property values of the given events.
     *
     * @param events The events to process
     * @param warnings A reference to collect warnings
     * @return The (possibly patched) list of events
     */
    private static List<Event> decodeMimeGarbage(List<Event> events, List<OXException> warnings) {
        for (Event event : events) {
            decodeMimeGarbage(event, warnings);
        }
        return events;
    }

    /**
     * Performs a prophylactical decoding of potentially MIME-encoded strings in property values of the given event.
     *
     * @param event The event to process
     * @param warnings A reference to collect warnings
     * @return The (possibly patched) event
     */
    private static Event decodeMimeGarbage(Event event, List<OXException> warnings) {
        for (EventField field : EventMapper.getInstance().getAssignedFields(event)) {
            Mapping<? extends Object, Event> mapping = EventMapper.getInstance().opt(field);
            if (null != mapping && decodeMimeGarbage(mapping, event)) {
                addInvalidDataWarning(warnings, event, field, "Decoded MIME garbage from imported data");
            }
        }
        if (false == isNullOrEmpty(event.getAttendees())) {
            for (Attendee attendee : event.getAttendees()) {
                for (AttendeeField field : AttendeeMapper.getInstance().getAssignedFields(attendee)) {
                    Mapping<? extends Object, Attendee> mapping = AttendeeMapper.getInstance().opt(field);
                    if (null != mapping && decodeMimeGarbage(mapping, attendee)) {
                        addInvalidDataWarning(warnings, event, EventField.ATTENDEES, "Decoded MIME garbage from imported data");
                    }
                }
            }
        }
        return event;
    }

    private static <O> boolean decodeMimeGarbage(Mapping<? extends Object, O> mapping, O object) {
        Object value = mapping.get(object);
        if (null == value || false == (value instanceof String)) {
            return false;
        }
        String stringValue = (String) value;
        if (stringValue.length() > 65536) {
            LOG.trace("Skip prophylactic MIME decoding of too long value for mapping {}.");
            return false;
        }
        String decodedValue;
        try {
            decodedValue = javax.mail.internet.MimeUtility.decodeText(stringValue);
        } catch (UnsupportedEncodingException e) {
            LOG.warn("Unexpected error decoding \"{}\", using as-is", stringValue, e);
            return false;
        }
        if (null != decodedValue && false == decodedValue.equals(stringValue)) {
            try {
                @SuppressWarnings("unchecked") Mapping<Object, O> rawMapping = (Mapping<Object, O>) mapping;
                rawMapping.set(object, decodedValue);
                return true;
            } catch (Exception e) {
                LOG.warn("Unexpected error applying patched value \"{}\" for \"{}\", using as-is", decodedValue, stringValue, e);
            }
        }
        return false;
    }

    /**
     * Adds (if at least present once) the organizer instance to all given events
     *
     * @param events The events to add the organizer to
     */
    private static void ensureOrganizer(List<Event> events) {
        /*
         * Check that the organizer value can and must be added to some events
         */
        if (1 == events.size()) {
            return;
        }
        Optional<Event> organizerEvent = events.stream().filter(e -> null != e.getOrganizer()).findFirst();
        if (false == organizerEvent.isPresent()) {
            return;
        }
        /*
         * Add organizer to events that have no organizer
         */
        Organizer organizer = organizerEvent.get().getOrganizer();
        events.stream().filter(e -> null == e.getOrganizer()).forEach((e) -> e.setOrganizer(new Organizer(organizer)));
    }

    /**
     * Adds (if at least present once) attendees(s) to all given events
     *
     * @param events The events to add the attendee(s) to
     */
    private static void ensureAtteendees(List<Event> events) {
        /*
         * Check that the attendee value can and must be added to some events
         */
        if (1 == events.size()) {
            return;
        }
        Optional<Event> attendeeEvent = events.stream().filter(e -> null != e.getAttendees() && false == e.getAttendees().isEmpty()).findFirst();
        if (false == attendeeEvent.isPresent()) {
            return;
        }
        /*
         * Add attendees to events that have no attendees
         */
        List<Attendee> attendees = attendeeEvent.get().getAttendees();
        List<Attendee> copy;
        try {
            copy = AttendeeMapper.getInstance().copy(attendees, (AttendeeField[]) null);
        } catch (OXException e) {
            LOG.warn("Unexpected error copying attendees", e);
            return;
        }
        events.stream().filter(e -> null == e.getAttendees()).forEach((e) -> e.setAttendees(copy));
    }

    /**
     * Gets a value indicating whether the imported calendar looks like it was
     * generated by a Microsoft software or not
     *
     * @param calendar The imported calendar
     * @return <code>true</code> if the calendar looks like it was generated with a Microsoft software,
     *         <code>false</code> otherwise
     */
    private static boolean looksLikeMicrosoft(ImportedCalendar calendar) {
        return looksLike(calendar, "microsoft");
    }

    /**
     * Gets a value indicating whether the imported calendar looks like it was
     * generated by a Google or not
     *
     * @param calendar The imported calendar
     * @return <code>true</code> if the calendar looks like it was generated with Google,
     *         <code>false</code> otherwise
     */
    private static boolean looksLikeGoogle(ImportedCalendar calendar) {
        return looksLike(calendar, "google");
    }

    private static boolean looksLike(ImportedCalendar calendar, String toMatch) {
        String property = calendar.getProdId();
        return Strings.isNotEmpty(property) && Strings.toLowerCase(property).indexOf(toMatch) >= 0;
    }

    private static void addInvalidDataWarning(List<OXException> warnings, Event event, EventField field, String message) {
        String id = event.getUid() + " | " + event.getRecurrenceId();
        OXException warning = CalendarExceptionCodes.IGNORED_INVALID_DATA.create(id, field, ProblemSeverity.NORMAL, message);
        LOG.debug("Patching invalid data in imported calendar for event {}: {}", id, message, warning);
        warnings.add(warning);
    }

}
