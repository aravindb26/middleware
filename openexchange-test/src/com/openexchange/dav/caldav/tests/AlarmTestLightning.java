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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link AlarmTestLightning}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
public class AlarmTestLightning extends CalDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.LIGHTNING_4_0_3_1;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAcknowledgeReminder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next sunday at 16:00");
        Date end = TimeTools.D("next sunday at 17:00");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "TZNAME:CEST\r\n" +
            "DTSTART:19700329T020000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "TZNAME:CET\r\n" +
            "DTSTART:19701025T030000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:test\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Mozilla Standardbeschreibung\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * acknowledge reminder in client
         */
        Date acknowledgedDate = TimeTools.D("next sunday at 15:47:32");
        iCalResource.getVEvent().setProperty("X-MOZ-LASTACK", formatAsUTC(acknowledgedDate));
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertFalse(appointment.containsAlarm(), "reminder still found");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(formatAsUTC(acknowledgedDate), iCalResource.getVEvent().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSnoozeReminder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next sunday at 16:00");
        Date end = TimeTools.D("next sunday at 17:00");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "TZNAME:CEST\r\n" +
            "DTSTART:19700329T020000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "TZNAME:CET\r\n" +
            "DTSTART:19701025T030000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:test\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Mozilla Standardbeschreibung\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * snooze reminder in client
         */
        Date acknowledgedDate = TimeTools.D("next sunday at 15:47:32");
        Date nextTrigger = TimeTools.D("next sunday at 15:52:32");
        iCalResource.getVEvent().setProperty("X-MOZ-LASTACK", formatAsUTC(acknowledgedDate));
        iCalResource.getVEvent().setProperty("X-MOZ-SNOOZE-TIME", formatAsUTC(nextTrigger));
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(acknowledgedDate), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(formatAsUTC(nextTrigger), iCalResource.getVEvent().getPropertyValue("X-MOZ-SNOOZE-TIME"), "X-MOZ-SNOOZE-TIME wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testEditReminder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next sunday at 16:00");
        Date end = TimeTools.D("next sunday at 17:00");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "TZNAME:CEST\r\n" +
            "DTSTART:19700329T020000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "TZNAME:CET\r\n" +
            "DTSTART:19701025T030000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:test\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Mozilla Standardbeschreibung\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * edit reminder in client
         */
        iCalResource.getVEvent().getVAlarm().setProperty("TRIGGER", "-PT20M", Collections.singletonMap("VALUE", "DURATION"));
        iCalResource.getVEvent().getVAlarm().setProperty("DESCRIPTION", "Mozilla Standardbeschreibung");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(20, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT20M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAcknowledgeRecurringReminder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next saturday at 15:30");
        Date end = TimeTools.D("next saturday at 17:15");
        Date initialAcknowledged = TimeTools.D("next saturday at 15:14");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "TZNAME:CEST\r\n" +
            "DTSTART:19700329T020000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "TZNAME:CET\r\n" +
            "DTSTART:19701025T030000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:recurring\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Mozilla Standardbeschreibung\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * acknowledge reminder in client
         */
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(initialAcknowledged);
        calendar.add(Calendar.MINUTE, 3);
        calendar.add(Calendar.SECOND, 17);
        Date acknowledgedDate = calendar.getTime();
        iCalResource.getVEvent().setProperty("X-MOZ-LASTACK", formatAsUTC(acknowledgedDate));
        iCalResource.getVEvent().getVAlarm().setProperty("X-LIC-ERROR", "Parse error in property name: ACKNOWLEDGED", 
            Collections.singletonMap("X-LIC-ERRORTYPE", "PROPERTY-PARSE-ERROR")); 
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(acknowledgedDate), iCalResource.getVEvent().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSnoozeRecurringReminder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next friday at 10:00");
        Date end = TimeTools.D("next friday at 10:15");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "TZNAME:CEST\r\n" +
            "DTSTART:19700329T020000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "TZNAME:CET\r\n" +
            "DTSTART:19701025T030000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:recurring\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Mozilla Standardbeschreibung\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * snooze reminder in client
         */
        Date acknowledgedDate = TimeTools.D("next friday at 09:46:24");
        Date nextTrigger = TimeTools.D("next friday at 09:51:24");
        iCalResource.getVEvent().setProperty("X-MOZ-LASTACK", formatAsUTC(acknowledgedDate));
        iCalResource.getVEvent().setProperty("X-MOZ-SNOOZE-TIME-" + start.getTime() + "000", formatAsUTC(nextTrigger));
        iCalResource.getVEvent().getVAlarm().setProperty("X-LIC-ERROR", "Parse error in property name: ACKNOWLEDGED", 
            Collections.singletonMap("X-LIC-ERRORTYPE", "PROPERTY-PARSE-ERROR")); 
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(acknowledgedDate), iCalResource.getVEvent().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(formatAsUTC(nextTrigger), iCalResource.getVEvent().getPropertyValue("X-MOZ-SNOOZE-TIME-" + start.getTime() + "000"), "X-MOZ-SNOOZE-TIME wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAcknowledgeExceptionReminder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next friday at 10:00");
        Date end = TimeTools.D("next friday at 11:00");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(start);
        calendar.add(Calendar.DATE, 2);
        Date exceptionStart = calendar.getTime();
        calendar.add(Calendar.HOUR, 1);
        Date exceptionEnd = calendar.getTime();
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -16);
        Date seriesAcknowledged = calendar.getTime();
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "TZNAME:CEST\r\n" +
            "DTSTART:19700329T020000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "TZNAME:CET\r\n" +
            "DTSTART:19701025T030000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:sdfs\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "SEQUENCE:0\r\n" +
            "X-MOZ-GENERATION:1\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:edit\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "CLASS:PUBLIC\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "SEQUENCE:0\r\n" +
            "X-MOZ-GENERATION:1\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment & exception on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        assertNotNull(appointment.getChangeException(), "No change exceptions found on server");
        assertEquals(1, appointment.getChangeException().length, "Unexpected number of change exceptions");
        Appointment changeException = getChangeExceptions(appointment).get(0);
        rememberForCleanUp(changeException);
        assertEquals(15, changeException.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment & exception on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
        assertEquals("edit", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvents().get(1).getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvents().get(1).getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * acknowledge exception reminder in client
         */
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -14);
        calendar.add(Calendar.SECOND, 52);
        Date exceptionAcknowledged = calendar.getTime();
        iCalResource.getVEvents().get(1).setProperty("X-MOZ-LASTACK", formatAsUTC(exceptionAcknowledged));
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment & exception on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        assertNotNull(appointment.getChangeException(), "No change exceptions found on server");
        assertEquals(1, appointment.getChangeException().length, "Unexpected number of change exceptions");
        changeException = getChangeExceptions(appointment).get(0);
        rememberForCleanUp(changeException);
        assertFalse(changeException.containsAlarm(), "reminder still found");
        /*
         * verify appointment & exception on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
        assertEquals("edit", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvents().get(1).getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvents().get(1).getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(exceptionAcknowledged), iCalResource.getVEvents().get(1).getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSnoozeExceptionReminder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next friday at 10:00");
        Date end = TimeTools.D("next friday at 11:00");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(start);
        calendar.add(Calendar.DATE, 2);
        Date exceptionStart = calendar.getTime();
        calendar.add(Calendar.HOUR, 1);
        Date exceptionEnd = calendar.getTime();
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -16);
        Date seriesAcknowledged = calendar.getTime();
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "TZNAME:CEST\r\n" +
            "DTSTART:19700329T020000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "TZNAME:CET\r\n" +
            "DTSTART:19701025T030000\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:sdfs\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "SEQUENCE:0\r\n" +
            "X-MOZ-GENERATION:1\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:edit\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "CLASS:PUBLIC\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "SEQUENCE:0\r\n" +
            "X-MOZ-GENERATION:1\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "TRIGGER;VALUE=DURATION:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment & exception on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        assertNotNull(appointment.getChangeException(), "No change exceptions found on server");
        assertEquals(1, appointment.getChangeException().length, "Unexpected number of change exceptions");
        Appointment changeException = getChangeExceptions(appointment).get(0);
        rememberForCleanUp(changeException);
        assertEquals(15, changeException.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment & exception on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
        assertEquals("edit", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvents().get(1).getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvents().get(1).getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * snooze exception reminder in client
         */
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -14);
        calendar.add(Calendar.SECOND, 52);
        Date exceptionAcknowledged = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date nextTrigger = calendar.getTime();
        iCalResource.getVEvent().setProperty("X-MOZ-LASTACK", formatAsUTC(exceptionAcknowledged));
        iCalResource.getVEvent().setProperty("X-MOZ-SNOOZE-TIME-" + exceptionStart.getTime() + "000", formatAsUTC(nextTrigger));
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");

        // TODO: The snooze information is hidden within the series master. This is currently not considered.

    }

}
