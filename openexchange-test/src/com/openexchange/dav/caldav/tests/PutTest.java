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

package com.openexchange.dav.caldav.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.WebDAVClient;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link PutTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 8.0.0
 */
public class PutTest extends Abstract2UserCalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAddException(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create overridden instance w/o series master event on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 09:00", TimeZone.getTimeZone("Europe/Berlin"));
        Date end = TimeTools.D("next monday at 09:30", TimeZone.getTimeZone("Europe/Berlin"));
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        List<Appointment> appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "added appointment not found on server");
        assertEquals(1, appointments.size(), "unexpected number of added appointments");
        Appointment appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAddSecondException(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create overridden instance w/o series master event on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 09:00", TimeZone.getTimeZone("Europe/Berlin"));
        Date end = TimeTools.D("next monday at 09:30", TimeZone.getTimeZone("Europe/Berlin"));
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        List<Appointment> appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "added appointment not found on server");
        assertEquals(1, appointments.size(), "unexpected number of added appointments");
        Appointment appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
        /*
         * update event & add another overridden instance w/o series master event on client
         */
        Date start2 = CalendarUtils.add(start, Calendar.DATE, 1, TimeZone.getTimeZone("Europe/Berlin"));
        Date end2 = CalendarUtils.add(end, Calendar.DATE, 1, TimeZone.getTimeZone("Europe/Berlin"));
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            generateEventComponent(uid, format(start2, "Europe/Berlin"), null, start2, end2, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
        /*
         * verify appointments on server
         */
        appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "updated appointments not found on server");
        assertEquals(2, appointments.size(), "unexpected number of updated appointments");
        appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        appointment = appointments.get(1);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointments on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvents(), "No VEVENTs in iCal found");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs in iCal found");
        assertNotNull(iCalResource.getVEvent(format(start, "Europe/Berlin")), "No VEVENT for first occurrence in iCal found");
        assertNotNull(iCalResource.getVEvent(format(start2, "Europe/Berlin")), "No VEVENT for second occurrence in iCal found");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemoveSecondException(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create overridden instance w/o series master event on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 09:00", TimeZone.getTimeZone("Europe/Berlin"));
        Date end = TimeTools.D("next monday at 09:30", TimeZone.getTimeZone("Europe/Berlin"));
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        List<Appointment> appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "added appointment not found on server");
        assertEquals(1, appointments.size(), "unexpected number of added appointments");
        Appointment appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
        /*
         * update event & add another overridden instance w/o series master event on client
         */
        Date start2 = CalendarUtils.add(start, Calendar.DATE, 1, TimeZone.getTimeZone("Europe/Berlin"));
        Date end2 = CalendarUtils.add(end, Calendar.DATE, 1, TimeZone.getTimeZone("Europe/Berlin"));
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            generateEventComponent(uid, format(start2, "Europe/Berlin"), null, start2, end2, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
        /*
         * verify appointments on server
         */
        appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "updated appointments not found on server");
        assertEquals(2, appointments.size(), "unexpected number of updated appointments");
        appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        appointment = appointments.get(1);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointments on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvents(), "No VEVENTs in iCal found");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs in iCal found");
        assertNotNull(iCalResource.getVEvent(format(start, "Europe/Berlin")), "No VEVENT for first occurrence in iCal found");
        assertNotNull(iCalResource.getVEvent(format(start2, "Europe/Berlin")), "No VEVENT for second occurrence in iCal found");
        /*
         * update event & remove first overridden instance again on client
         */
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start2, "Europe/Berlin"), null, start2, end2, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
        /*
         * verify appointment on server
         */
        appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "remaining appointment not found on server");
        assertEquals(1, appointments.size(), "unexpected number of appointments");
        appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start2, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start2, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end2, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAddMasterAfterException(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create overridden instance w/o series master event on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 09:00", TimeZone.getTimeZone("Europe/Berlin"));
        Date end = TimeTools.D("next monday at 09:30", TimeZone.getTimeZone("Europe/Berlin"));
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        List<Appointment> appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "added appointment not found on server");
        assertEquals(1, appointments.size(), "unexpected number of added appointments");
        Appointment appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
        /*
         * update event & add another series master event on client
         */
        Date masterStart = CalendarUtils.add(start, Calendar.DATE, -3, TimeZone.getTimeZone("Europe/Berlin"));
        Date masterEnd = CalendarUtils.add(end, Calendar.DATE, -3, TimeZone.getTimeZone("Europe/Berlin"));
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, null, "FREQ=DAILY;COUNT=4", masterStart, masterEnd, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
        /*
         * verify appointments on server
         */
        appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "updated appointments not found on server");
        assertEquals(2, appointments.size(), "unexpected number of updated appointments");
        appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        appointment = appointments.get(1);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointments on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvents(), "No VEVENTs in iCal found");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs in iCal found");
        assertNotNull(iCalResource.getVEvent(format(start, "Europe/Berlin")), "No VEVENT for first occurrence in iCal found");
        assertNotNull(iCalResource.getVEvent(null), "No VEVENT for master event in iCal found");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDontAddConflictingEvent(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on client
         */
        String uid = randomUID();
        String summary = "test";
        String location = "testcity";
        Date start = TimeTools.D("tomorrow at 3pm");
        Date end = TimeTools.D("tomorrow at 4pm");
        String iCal = generateICal(start, end, uid, summary, location);
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        rememberForCleanUp(appointment);
        assertAppointmentEquals(appointment, start, end, uid, summary, location);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        /*
         * try to create appointment with same uid in other private folder
         */
        FolderObject subfolder = createFolder(randomUID());
        assertEquals(StatusCodes.SC_FORBIDDEN, putICal(String.valueOf(subfolder.getObjectID()), uid, iCal), "response code wrong");
        /*
         * try to create appointment with same uid in other public folder
         */
        FolderObject publicFolder = createPublicFolder(randomUID());
        assertEquals(StatusCodes.SC_FORBIDDEN, putICal(String.valueOf(publicFolder.getObjectID()), uid, iCal), "response code wrong");
        /*
         * try to create appointment with same uid, but different filename, in same folder
         */
        String resourceName = randomUID();
        assertEquals(StatusCodes.SC_FORBIDDEN, putICal(resourceName, iCal), "response code wrong");
        /*
         * try to create appointment with same uid, but different filename, in other private folder
         */
        assertEquals(StatusCodes.SC_FORBIDDEN, putICal(String.valueOf(subfolder.getObjectID()), resourceName, iCal), "response code wrong");
        /*
         * try to create appointment with same uid, but different filename, in other public folder
         */
        assertEquals(StatusCodes.SC_FORBIDDEN, putICal(String.valueOf(publicFolder.getObjectID()), resourceName, iCal), "response code wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMultipleAttendees(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create overridden instance w/o series master event on client
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 09:00", TimeZone.getTimeZone("Europe/Berlin"));
        Date end = TimeTools.D("next monday at 09:30", TimeZone.getTimeZone("Europe/Berlin"));
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            getEuropeBerlinTimezoneComponent() +
            generateEventComponent(uid, format(start, "Europe/Berlin"), null, start, end, "Europe/Berlin", "test@example.com", getClient().getValues().getDefaultAddress(), client2.getValues().getDefaultAddress()) +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        List<Appointment> appointments = getAppointments(getDefaultFolderID(), uid);
        assertNotNull(appointments, "added appointment not found on server");
        assertEquals(1, appointments.size(), "unexpected number of added appointments");
        Appointment appointment = appointments.get(0);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
        /*
         * create the same event resource for other user on second client
         */
        WebDAVClient webDAVClient2 = new WebDAVClient(testUser2, getDefaultUserAgent(), null);
        String folderId2 = encodeFolderID(String.valueOf(client2.getValues().getPrivateAppointmentFolder()));
        assertEquals(StatusCodes.SC_CREATED, putICal(webDAVClient2, folderId2, uid, iCal, Collections.emptyMap()), "response code wrong");
        /*
         * verify appointment on second client
         */
        ICalResource iCalResource2 = get(webDAVClient2, folderId2, uid, null, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
        /*
         * delete appointment on first client
         */
        assertEquals(StatusCodes.SC_NO_CONTENT, delete(getDefaultFolderID(), uid, iCalResource.getETag(), null), "response code wrong");
        /*
         * verify appointment on second client
         */
        iCalResource2 = get(webDAVClient2, folderId2, uid, null, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(format(start, "Europe/Berlin"), iCalResource.getVEvent().getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
    }

    private static String generateEventComponent(String uid, String recurrenceId, String rrule, Date start, Date end, String timezoneId, String organizerMail, String... attendeeMails) {
        String iCal = // @formatter:off
            "BEGIN:VEVENT\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "DTEND;TZID=" + timezoneId + ":" + format(end, timezoneId) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:3\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:summary\r\n" +
            (null == recurrenceId ? "" : ("RECURRENCE-ID;TZID=" + timezoneId + ":" + format(start, timezoneId) + "\r\n")) +
            (null == rrule ? "" : ("RRULE:" + rrule + "\r\n")) +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART;TZID=" + timezoneId + ":" + format(start, timezoneId) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "ORGANIZER:mailto:" + organizerMail + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + organizerMail + "\r\n"
        ; // @formatter:on
        for (String attendeeMail : attendeeMails) {
            iCal += "ATTENDEE;PARTSTAT=NEEDS-ACTION:mailto:" + attendeeMail + "\r\n";
        }
        return iCal + "END:VEVENT\r\n";
    }

    private static String getEuropeBerlinTimezoneComponent() {
        return // @formatter:off
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n" +
            "DTSTART:19810329T020000\r\n" +
            "TZNAME:MESZ\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n" +
            "DTSTART:19961027T030000\r\n" +
            "TZNAME:MEZ\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n"
        ; // @formatter:on
    }

}
