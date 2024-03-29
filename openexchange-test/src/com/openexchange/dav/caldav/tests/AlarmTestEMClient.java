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
import java.util.List;
import java.util.TimeZone;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link AlarmTestEMClient}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
public class AlarmTestEMClient extends CalDAVTest {
    
    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.EM_CLIENT_6_0;
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
        Date start = TimeTools.D("next friday at 08:00");
        Date end = TimeTools.D("next friday at 09:00");
        Date initialAcknowledged = TimeTools.D("next friday at 07:44");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:20151116T121948Z\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:test\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
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
        iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:20151116T121948Z\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:test\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Open-XChange\r\n" +
            "X-MOZ-LASTACK:99991231T235859Z\r\n" +
            "ACKNOWLEDGED:99991231T235859Z\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
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
        Component vAlarm = iCalResource.getVEvent().getVAlarm();
        assertTrue(null == vAlarm || "99991231T235859Z".equals(vAlarm.getPropertyValue("ACKNOWLEDGED")), "Unacknowledged ALARM in iCal found");
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
        Date start = TimeTools.D("next tuesday at 10:00");
        Date end = TimeTools.D("next tuesday at 11:00");
        Date initialAcknowledged = TimeTools.D("next tuesday at 09:44");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:20151116T121948Z\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:snooze\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
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
        Date nextTrigger = TimeTools.D("next tuesday at 09:51:24");
        Date nextAcknowledged = TimeTools.D("next tuesday at 09:50:24");
        iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:20151116T121948Z\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:snooze\r\n" +
            "CLASS:PUBLIC\r\n" +
            "X-MOZ-SNOOZE:" + formatAsUTC(nextTrigger) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Open-XChange\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "TRIGGER;VALUE=DATE-TIME:" + formatAsUTC(nextTrigger) + "\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
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
        List<Component> vAlarms = iCalResource.getVEvent().getVAlarms();
        assertEquals(2, vAlarms.size(), "Unexpected number of VALARMs found");
        for (Component vAlarm : vAlarms) {
            if (null != vAlarm.getProperty("RELATED-TO")) {
                assertEquals(formatAsUTC(nextTrigger), vAlarm.getPropertyValue("TRIGGER"), "ALARM wrong");
            } else {
                assertEquals("-PT15M", vAlarm.getPropertyValue("TRIGGER"), "ALARM wrong");
            }
        }
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
        Date start = TimeTools.D("next tuesday at 10:00");
        Date end = TimeTools.D("next tuesday at 11:00");
        Date initialAcknowledged = TimeTools.D("next tuesday at 09:44");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:20151116T121948Z\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:snooze\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
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
        Date nextAcknowledged = TimeTools.D("next tuesday at 09:29:00");
        iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:20151116T121948Z\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:snooze\r\n" +
            "CLASS:PUBLIC\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;RELATED=START:-PT30M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Open-XChange\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(30, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT30M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
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
        Date start = TimeTools.D("next friday at 10:00");
        Date end = TimeTools.D("next friday at 10:15");
        Date initialAcknowledged = TimeTools.D("next friday at 09:44");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "CLASS:PUBLIC\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
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
        calendar.add(Calendar.DATE, 1);
        Date nextAcknowledged = calendar.getTime();
        iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "CLASS:PUBLIC\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Open-XChange\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
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
        assertEquals(formatAsUTC(nextAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        assertEquals(formatAsUTC(nextAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
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
        Date initialAcknowledged = TimeTools.D("next friday at 09:44");
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "CLASS:PUBLIC\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
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
        Date nextTrigger = TimeTools.D("next friday at 09:51:24");
        Date nextAcknowledged = TimeTools.D("next friday at 09:50:24");
        iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "X-MOZ-SNOOZE:" + formatAsUTC(nextTrigger) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "CLASS:PUBLIC\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Open-XChange\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;VALUE=DATE-TIME:" + formatAsUTC(nextTrigger) + "\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(nextAcknowledged) + "\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
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
        List<Component> vAlarms = iCalResource.getVEvent().getVAlarms();
        assertEquals(2, vAlarms.size(), "Unexpected number of VALARMs found");
        // order of VALARM components seems to be important for the client ...
        assertEquals("-PT15M", vAlarms.get(0).getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(nextAcknowledged), vAlarms.get(0).getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        assertEquals(formatAsUTC(nextAcknowledged), vAlarms.get(0).getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertNotNull(vAlarms.get(1).getProperty("RELATED-TO"), "No RELATED-TO found");
        assertEquals(formatAsUTC(nextTrigger), vAlarms.get(1).getPropertyValue("TRIGGER"), "ALARM wrong");
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
        Date exceptionAcknowledged = calendar.getTime();
        calendar.add(Calendar.DATE, 1);
        Date seriesAcknowledged = calendar.getTime();
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:daily\r\n" +
            "CLASS:PUBLIC\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:dailyEDIT\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(exceptionAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(exceptionAcknowledged) + "\r\n" +
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
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
        assertEquals("dailyEDIT", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvents().get(1).getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvents().get(1).getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * acknowledge exception reminder in client
         */
        Component exceptionAlarm = iCalResource.getVEvents().get(1).getVAlarm();
        exceptionAlarm.setProperty("X-MOZ-LASTACK", "99991231T235859Z");
        exceptionAlarm.setProperty("ACKNOWLEDGED", "99991231T235859Z");
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
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
        assertEquals("dailyEDIT", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        Component vAlarm = iCalResource.getVEvents().get(1).getVAlarm();
        assertTrue(null == vAlarm || "99991231T235859Z".equals(vAlarm.getPropertyValue("ACKNOWLEDGED")), "Unacknowledged ALARM in iCal found");
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
        Date exceptionAcknowledged = calendar.getTime();
        calendar.add(Calendar.DATE, 1);
        Date seriesAcknowledged = calendar.getTime();
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//eM Client/6.0.23421.0\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:W. Europe Standard Time\r\n" +
            "X-EM-DISPLAYNAME:(UTC+01:00) Amsterdam\\, Berlin\\, Bern\\, Rom\\, Stockholm\\, W\r\n" +
            " ien\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Zeit\r\n" +
            "DTSTART:00010101T030000\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=10\r\n" +
            "END:STANDARD\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZNAME:Mitteleurop\u00e4ische Sommerzeit\r\n" +
            "DTSTART:00010101T020000\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYDAY=-1SU;BYMONTH=3\r\n" +
            "END:DAYLIGHT\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:daily\r\n" +
            "CLASS:PUBLIC\r\n" +
            "RRULE:FREQ=DAILY\r\n" +
            "BEGIN:VALARM\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SUMMARY:dailyEDIT\r\n" +
            "DTSTART;TZID=\"W. Europe Standard Time\":" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=\"W. Europe Standard Time\":" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(exceptionAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(exceptionAcknowledged) + "\r\n" +
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
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
        assertEquals("dailyEDIT", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvents().get(1).getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvents().get(1).getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * snooze exception reminder in client
         */
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -10);
        calendar.add(Calendar.SECOND, 24);
        Date nextTrigger = calendar.getTime();
        calendar.add(Calendar.MINUTE, -1);
        Date nextAcknowledged = calendar.getTime();
        Component exceptionEvent = iCalResource.getVEvents().get(1);
        exceptionEvent.setProperty("X-MOZ-SNOOZE", formatAsUTC(nextTrigger));
        Component exceptionAlarm = exceptionEvent.getVAlarms().get(0);
        exceptionAlarm.setProperty("X-MOZ-LASTACK", formatAsUTC(nextAcknowledged));
        exceptionAlarm.setProperty("ACKNOWLEDGED", formatAsUTC(nextAcknowledged));
        Component snoozeAlarm = new Component("VALARM");
        snoozeAlarm.setProperty("ACTION", "DISPLAY");
        snoozeAlarm.setProperty("DESCRIPTION", "Alarm");
        snoozeAlarm.setProperty("TRIGGER", formatAsUTC(nextTrigger), Collections.singletonMap("VALUE", "DATE-TIME"));
        snoozeAlarm.setProperty("X-MOZ-LASTACK", formatAsUTC(nextAcknowledged));
        snoozeAlarm.setProperty("ACKNOWLEDGED", formatAsUTC(nextAcknowledged));
        exceptionEvent.getComponents().add(snoozeAlarm);
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
        assertTrue(changeException.containsAlarm(), "no reminder found");
        assertEquals(15, changeException.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment & exception on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
        assertEquals("dailyEDIT", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvents().get(1).getVAlarm(), "No ALARM in iCal found");
        List<Component> vAlarms = iCalResource.getVEvents().get(1).getVAlarms();
        assertEquals(2, vAlarms.size(), "Unexpected number of VALARMs found");
        for (Component vAlarm : vAlarms) {
            if (null != vAlarm.getProperty("RELATED-TO")) {
                assertEquals(formatAsUTC(nextTrigger), vAlarm.getPropertyValue("TRIGGER"), "ALARM wrong");
            } else {
                assertEquals("-PT15M", vAlarm.getPropertyValue("TRIGGER"), "ALARM wrong");
            }
        }
    }

}
