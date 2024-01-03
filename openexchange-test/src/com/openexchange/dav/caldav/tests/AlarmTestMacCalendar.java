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
import com.openexchange.dav.caldav.ical.SimpleICal;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link AlarmTestMacCalendar}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
public class AlarmTestMacCalendar extends CalDAVTest {

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
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:ack\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Ereignisbenachrichtigung\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        Date initialAcknowledged = TimeTools.D("next sunday at 15:44");
        Date acknowledgedDate = TimeTools.D("next sunday at 15:47:32");
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:ack\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "ACTION:DISPLAY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(acknowledgedDate) + "\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        assertAcknowledgedOrDummyAlarm(iCalResource.getVEvent(), formatAsUTC(acknowledgedDate));
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
        String relatedUID = randomUID();
        Date start = TimeTools.D("next sunday at 16:00");
        Date end = TimeTools.D("next sunday at 17:00");
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:ack\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + relatedUID + "\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Ereignisbenachrichtigung\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        Date initialAcknowledged = TimeTools.D("next sunday at 15:44");
        Date acknowledgedDate = TimeTools.D("next sunday at 15:47:32");
        Date nextTrigger = TimeTools.D("next sunday at 15:52:32");
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:ack\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + relatedUID + "\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "ACTION:DISPLAY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(acknowledgedDate) + "\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:" + formatAsUTC(nextTrigger) + "\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "RELATED-TO:" + relatedUID + "\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        Date start = TimeTools.D("next sunday at 16:00");
        Date end = TimeTools.D("next sunday at 17:00");
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:ack\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Ereignisbenachrichtigung\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        Date initialAcknowledged = TimeTools.D("next sunday at 15:44");
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:ack\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER:-PT20M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
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
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:7B669A77-E205-4B03-A1AF-40FB146C4A3F\r\n" +
            "UID:7B669A77-E205-4B03-A1AF-40FB146C4A3F\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Ereignisbenachrichtigung\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "UID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "UID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(acknowledgedDate) + "\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "UID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        assertEquals(formatAsUTC(acknowledgedDate), iCalResource.getVEvent().getVAlarm().getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
    }

    // tests the handling without workaround for bug #43376
    public void noTestSnoozeRecurringReminder(String authMethod) throws Exception {
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
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:7B669A77-E205-4B03-A1AF-40FB146C4A3F\r\n" +
            "UID:7B669A77-E205-4B03-A1AF-40FB146C4A3F\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Ereignisbenachrichtigung\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "UID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(start);
        calendar.add(Calendar.DATE, 2);
        Date exceptionStart = calendar.getTime();
        calendar.setTime(end);
        calendar.add(Calendar.DATE, 2);
        Date exceptionEnd = calendar.getTime();
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -14);
        calendar.add(Calendar.SECOND, 17);
        Date acknowledgedDate = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date nextTrigger = calendar.getTime();
        String relatedUID = randomUID();
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "UID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "TRIGGER;RELATED=START:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(initialAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(acknowledgedDate) + "\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "UID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + relatedUID + "\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "ACTION:DISPLAY\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(acknowledgedDate) + "\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:" + formatAsUTC(nextTrigger) + "\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "RELATED-TO:" + relatedUID + "\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:" + randomUID() + "\r\n" +
            "UID:" + randomUID() + "\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
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
        Appointment changeException = getChangeExceptions(appointment).get(0);
        rememberForCleanUp(changeException);
        assertTrue(changeException.containsAlarm(), "no reminder found");
        assertEquals(15, changeException.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment & exception on client
         */
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.DATE, 1);
        calendar.add(Calendar.MINUTE, -16);
        Date seriesAcknowledged = calendar.getTime();
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
        assertEquals(formatAsUTC(seriesAcknowledged), iCalResource.getVEvent().getVAlarm().getPropertyValue("X-MOZ-LASTACK"), "X-MOZ-LASTACK wrong");
        assertEquals(2, iCalResource.getVEvents().size(), "Not all VEVENTs in iCal found");
        assertEquals(uid, iCalResource.getVEvents().get(1).getUID(), "UID wrong");
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
        Date start = TimeTools.D("next saturday at 15:30");
        Date end = TimeTools.D("next saturday at 17:15");
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:RecurringReminder\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:7B669A77-E205-4B03-A1AF-40FB146C4A3F\r\n" +
            "UID:7B669A77-E205-4B03-A1AF-40FB146C4A3F\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Ereignisbenachrichtigung\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "UID:56C5C265-7442-44E6-8F9C-17C71DCF932A\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        calendar.setTime(start);
        calendar.add(Calendar.DATE, 2);
        Date exceptionStart = calendar.getTime();
        calendar.setTime(end);
        calendar.add(Calendar.DATE, 2);
        Date exceptionEnd = calendar.getTime();
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -14);
        calendar.add(Calendar.SECOND, 17);
        Date acknowledgedDate = calendar.getTime();
        calendar.add(Calendar.MINUTE, 5);
        Date nextTrigger = calendar.getTime();
        calendar.add(Calendar.MINUTE, -1);
        String relatedUID = randomUID();
        Component masterEvent = iCalResource.getVEvent();
        Component exceptionEvent = SimpleICal.parse(masterEvent.toString(), "VEVENT");
        iCalResource.addComponent(exceptionEvent);
        exceptionEvent.removeProperties("RRULE");
        exceptionEvent.setDTStart(exceptionStart, "Europe/Berlin");
        exceptionEvent.setDTEnd(exceptionEnd, "Europe/Berlin");
        exceptionEvent.setProperty("RECURRENCE-ID", formatAsUTC(exceptionStart));
        for (Component exceptionAlarm : exceptionEvent.getVAlarms()) {
            exceptionAlarm.setProperty("X-WR-ALARMUID", randomUID());
            if ("TRUE".equals(exceptionAlarm.getPropertyValue("X-APPLE-DEFAULT-ALARM"))) {
                exceptionAlarm.setProperty("UID", randomUID());
            } else {
                exceptionAlarm.setProperty("UID", relatedUID);
                exceptionAlarm.setProperty("ACKNOWLEDGED", formatAsUTC(acknowledgedDate));
            }
        }
        Component snoozeAlarm = new Component("VALARM");
        snoozeAlarm.setProperty("UID", randomUID());
        snoozeAlarm.setProperty("X-WR-ALARMUID", randomUID());
        snoozeAlarm.setProperty("ACTION", "DISPLAY");
        snoozeAlarm.setProperty("DESCRIPTION", "Alarm");
        snoozeAlarm.setProperty("TRIGGER", formatAsUTC(nextTrigger), Collections.singletonMap("VALUE", "DATE-TIME"));
        snoozeAlarm.setProperty("RELATED-TO", relatedUID);
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
        assertTrue(null == appointment.getChangeException() || 0 == appointment.getChangeException().length, "change exceptions found on server");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(1, iCalResource.getVEvents().size(), "More than one VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        List<Component> vAlarms = iCalResource.getVEvent().getVAlarms();
        assertEquals(2, vAlarms.size(), "Unexpected number of VALARMs found");
        assertEquals("-PT15M", vAlarms.get(0).getPropertyValue("TRIGGER"), "ALARM wrong");
        assertEquals(formatAsUTC(acknowledgedDate), vAlarms.get(0).getPropertyValue("ACKNOWLEDGED"), "ACKNOWLEDGED wrong");
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
        calendar.add(Calendar.DATE, 1);
        Date seriesAcknowledged = calendar.getTime();
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:Neues Ereignis\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:47B1F982-7DAE-4029-9FBD-88899D1577FB\r\n" +
            "UID:47B1F982-7DAE-4029-9FBD-88899D1577FB\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:30C8AEDF-B12D-4DAE-8079-59A1FE218CA0\r\n" +
            "UID:30C8AEDF-B12D-4DAE-8079-59A1FE218CA0\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "SUMMARY:EXception\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:3\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:3159E1B2-5778-41AB-B1C1-67B5514E7A9E\r\n" +
            "UID:3159E1B2-5778-41AB-B1C1-67B5514E7A9E\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:4EA89B39-B283-439F-824F-194AD29DC41F\r\n" +
            "UID:4EA89B39-B283-439F-824F-194AD29DC41F\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        assertEquals("EXception", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertNotNull(iCalResource.getVEvents().get(1).getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvents().get(1).getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * acknowledge exception reminder in client
         */
        calendar.setTime(exceptionStart);
        calendar.add(Calendar.MINUTE, -14);
        calendar.add(Calendar.SECOND, 52);
        Date exceptionAcknowledged = calendar.getTime();
        iCalResource.getVEvents().get(1).getVAlarm().setProperty("ACKNOWLEDGED", formatAsUTC(exceptionAcknowledged));
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
        assertEquals("EXception", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
        assertAcknowledgedOrDummyAlarm(iCalResource.getVEvents().get(1), formatAsUTC(exceptionAcknowledged));
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
        calendar.add(Calendar.DATE, 1);
        Date seriesAcknowledged = calendar.getTime();
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:2\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:Neues Ereignis\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:47B1F982-7DAE-4029-9FBD-88899D1577FB\r\n" +
            "UID:47B1F982-7DAE-4029-9FBD-88899D1577FB\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:30C8AEDF-B12D-4DAE-8079-59A1FE218CA0\r\n" +
            "UID:30C8AEDF-B12D-4DAE-8079-59A1FE218CA0\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "SUMMARY:EXception\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:3\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:3159E1B2-5778-41AB-B1C1-67B5514E7A9E\r\n" +
            "UID:3159E1B2-5778-41AB-B1C1-67B5514E7A9E\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "ACTION:DISPLAY\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:4EA89B39-B283-439F-824F-194AD29DC41F\r\n" +
            "UID:4EA89B39-B283-439F-824F-194AD29DC41F\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
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
        assertEquals("EXception", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
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
        iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.8.5//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
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
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "RRULE:FREQ=DAILY;INTERVAL=1\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:3\r\n" +
            "CLASS:PUBLIC\r\n" +
            "SUMMARY:Neues Ereignis\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:47B1F982-7DAE-4029-9FBD-88899D1577FB\r\n" +
            "UID:47B1F982-7DAE-4029-9FBD-88899D1577FB\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:30C8AEDF-B12D-4DAE-8079-59A1FE218CA0\r\n" +
            "UID:30C8AEDF-B12D-4DAE-8079-59A1FE218CA0\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "BEGIN:VEVENT\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(exceptionStart, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(exceptionEnd, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:20151123T072538Z\r\n" +
            "SEQUENCE:3\r\n" +
            "CLASS:PUBLIC\r\n" +
            "RECURRENCE-ID:" + formatAsUTC(exceptionStart) + "\r\n" +
            "SUMMARY:EXception\r\n" +
            "X-MICROSOFT-CDO-BUSYSTATUS:BUSY\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:C5CD4D18-0448-43AB-8402-C401F0A38C8C\r\n" +
            "UID:C5CD4D18-0448-43AB-8402-C401F0A38C8C\r\n" +
            "TRIGGER;VALUE=DATE-TIME:" + formatAsUTC(nextTrigger) + "\r\n" +
            "ACTION:DISPLAY\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "RELATED-TO:4BEE3916-2A02-463F-AA31-B9C90084F092\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:4BEE3916-2A02-463F-AA31-B9C90084F092\r\n" +
            "UID:4BEE3916-2A02-463F-AA31-B9C90084F092\r\n" +
            "TRIGGER:-PT15M\r\n" +
            "ACTION:DISPLAY\r\n" +
            "X-MOZ-LASTACK:" + formatAsUTC(seriesAcknowledged) + "\r\n" +
            "ACKNOWLEDGED:" + formatAsUTC(exceptionAcknowledged) + "\r\n" +
            "DESCRIPTION:Alarm\r\n" +
            "END:VALARM\r\n" +
            "BEGIN:VALARM\r\n" +
            "X-WR-ALARMUID:4EA89B39-B283-439F-824F-194AD29DC41F\r\n" +
            "UID:4EA89B39-B283-439F-824F-194AD29DC41F\r\n" +
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z\r\n" +
            "X-APPLE-DEFAULT-ALARM:TRUE\r\n" +
            "ACTION:NONE\r\n" +
            "END:VALARM\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCal, iCalResource.getETag()), "response code wrong");
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
        assertEquals("EXception", iCalResource.getVEvents().get(1).getSummary(), "SUMMARY wrong");
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
