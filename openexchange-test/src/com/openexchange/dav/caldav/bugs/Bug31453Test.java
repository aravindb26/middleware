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

package com.openexchange.dav.caldav.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug31453Test}
 *
 * Calendar reminders do not show up in iCal when creating an event in a public calendar
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug31453Test extends CalDAVTest {

    private FolderObject publicFolder = null;
    private String publicFolderID = null;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        publicFolder = createPublicFolder();
        publicFolderID = String.valueOf(publicFolder.getObjectID());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateReminderInClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next sunday at 14:15");
        Date end = TimeTools.D("next sunday at 15:30");
        String iCal = "BEGIN:VCALENDAR" + "\r\n" + "VERSION:2.0" + "\r\n" + "METHOD:REQUEST" + "\r\n" + "PRODID:-//Apple Inc.//Mac OS X 10.8.2//EN" + "\r\n" + "CALSCALE:GREGORIAN" + "\r\n" + "BEGIN:VTIMEZONE" + "\r\n" + "TZID:Europe/Berlin" + "\r\n" + "BEGIN:DAYLIGHT" + "\r\n" + "TZOFFSETFROM:+0100" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" + "DTSTART:19810329T020000" + "\r\n" + "TZNAME:CEST" + "\r\n" + "TZOFFSETTO:+0200" + "\r\n" + "END:DAYLIGHT" + "\r\n" + "BEGIN:STANDARD" + "\r\n" + "TZOFFSETFROM:+0200" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" + "DTSTART:19961027T030000" + "\r\n" + "TZNAME:CET" + "\r\n" + "TZOFFSETTO:+0100" + "\r\n" + "END:STANDARD" + "\r\n" + "END:VTIMEZONE" + "\r\n" + "BEGIN:VEVENT" + "\r\n" + "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" + "TRANSP:OPAQUE" + "\r\n" + "UID:" + uid + "\r\n" + "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" + "LOCATION:loc" + "\r\n" + "DESCRIPTION:stripped" + "\r\n" + "SEQUENCE:2" + "\r\n" + "SUMMARY:test" + "\r\n" + "BEGIN:VALARM" + "\r\n" + "X-WR-ALARMUID:" + uid + "\r\n" + "UID:" + uid + "\r\n" + "TRIGGER:-PT15M" + "\r\n" + "DESCRIPTION:Ereignisbenachrichtigung" + "\r\n" + "ACTION:DISPLAY" + "\r\n" + "END:VALARM" + "\r\n" + "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" + "CREATED:" + formatAsUTC(TimeTools.D("yesterday noon")) + "\r\n" + "END:VEVENT" + "\r\n" + "END:VCALENDAR";
        assertEquals(StatusCodes.SC_CREATED, super.putICal(publicFolderID, uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(publicFolderID, uid);
        assertNotNull(appointment, "appointment not found on server");
        super.rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minuteswrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = super.get(publicFolderID, uid, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemoveReminderInClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next sunday at 14:15");
        Date end = TimeTools.D("next sunday at 15:30");
        String iCal = "BEGIN:VCALENDAR" + "\r\n" + "VERSION:2.0" + "\r\n" + "METHOD:REQUEST" + "\r\n" + "PRODID:-//Apple Inc.//Mac OS X 10.8.2//EN" + "\r\n" + "CALSCALE:GREGORIAN" + "\r\n" + "BEGIN:VTIMEZONE" + "\r\n" + "TZID:Europe/Berlin" + "\r\n" + "BEGIN:DAYLIGHT" + "\r\n" + "TZOFFSETFROM:+0100" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" + "DTSTART:19810329T020000" + "\r\n" + "TZNAME:CEST" + "\r\n" + "TZOFFSETTO:+0200" + "\r\n" + "END:DAYLIGHT" + "\r\n" + "BEGIN:STANDARD" + "\r\n" + "TZOFFSETFROM:+0200" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" + "DTSTART:19961027T030000" + "\r\n" + "TZNAME:CET" + "\r\n" + "TZOFFSETTO:+0100" + "\r\n" + "END:STANDARD" + "\r\n" + "END:VTIMEZONE" + "\r\n" + "BEGIN:VEVENT" + "\r\n" + "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" + "TRANSP:OPAQUE" + "\r\n" + "UID:" + uid + "\r\n" + "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" + "LOCATION:loc" + "\r\n" + "DESCRIPTION:stripped" + "\r\n" + "SEQUENCE:2" + "\r\n" + "SUMMARY:test" + "\r\n" + "BEGIN:VALARM" + "\r\n" + "X-WR-ALARMUID:" + uid + "\r\n" + "UID:" + uid + "\r\n" + "TRIGGER:-PT15M" + "\r\n" + "DESCRIPTION:Ereignisbenachrichtigung" + "\r\n" + "ACTION:DISPLAY" + "\r\n" + "END:VALARM" + "\r\n" + "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" + "CREATED:" + formatAsUTC(TimeTools.D("yesterday noon")) + "\r\n" + "END:VEVENT" + "\r\n" + "END:VCALENDAR";
        assertEquals(StatusCodes.SC_CREATED, super.putICal(publicFolderID, uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(publicFolderID, uid);
        assertNotNull(appointment, "appointment not found on server");
        super.rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = super.get(publicFolderID, uid, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * remove reminder on client
         */
        Component vAlarm = iCalResource.getVEvent().getVAlarm();
        iCalResource.getVEvent().getComponents().remove(vAlarm);
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(publicFolderID, uid);
        assertNotNull(appointment, "appointment not found on server");
        assertFalse(appointment.containsAlarm(), "reminder found");
        assertEquals(0, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = super.get(publicFolderID, uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertDummyAlarm(iCalResource.getVEvent());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testEditReminderInClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next sunday at 14:15");
        Date end = TimeTools.D("next sunday at 15:30");
        String iCal = "BEGIN:VCALENDAR" + "\r\n" + "VERSION:2.0" + "\r\n" + "METHOD:REQUEST" + "\r\n" + "PRODID:-//Apple Inc.//Mac OS X 10.8.2//EN" + "\r\n" + "CALSCALE:GREGORIAN" + "\r\n" + "BEGIN:VTIMEZONE" + "\r\n" + "TZID:Europe/Berlin" + "\r\n" + "BEGIN:DAYLIGHT" + "\r\n" + "TZOFFSETFROM:+0100" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" + "DTSTART:19810329T020000" + "\r\n" + "TZNAME:CEST" + "\r\n" + "TZOFFSETTO:+0200" + "\r\n" + "END:DAYLIGHT" + "\r\n" + "BEGIN:STANDARD" + "\r\n" + "TZOFFSETFROM:+0200" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" + "DTSTART:19961027T030000" + "\r\n" + "TZNAME:CET" + "\r\n" + "TZOFFSETTO:+0100" + "\r\n" + "END:STANDARD" + "\r\n" + "END:VTIMEZONE" + "\r\n" + "BEGIN:VEVENT" + "\r\n" + "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" + "TRANSP:OPAQUE" + "\r\n" + "UID:" + uid + "\r\n" + "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" + "LOCATION:loc" + "\r\n" + "DESCRIPTION:stripped" + "\r\n" + "SEQUENCE:2" + "\r\n" + "SUMMARY:test" + "\r\n" + "BEGIN:VALARM" + "\r\n" + "X-WR-ALARMUID:" + uid + "\r\n" + "UID:" + uid + "\r\n" + "TRIGGER:-PT15M" + "\r\n" + "DESCRIPTION:Ereignisbenachrichtigung" + "\r\n" + "ACTION:DISPLAY" + "\r\n" + "END:VALARM" + "\r\n" + "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" + "CREATED:" + formatAsUTC(TimeTools.D("yesterday noon")) + "\r\n" + "END:VEVENT" + "\r\n" + "END:VCALENDAR";
        assertEquals(StatusCodes.SC_CREATED, super.putICal(publicFolderID, uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(publicFolderID, uid);
        assertNotNull(appointment, "appointment not found on server");
        super.rememberForCleanUp(appointment);
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(15, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = super.get(publicFolderID, uid, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT15M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * edit reminder on client
         */
        iCalResource.getVEvent().getVAlarm().setProperty("TRIGGER", "-PT20M");
        iCalResource.getVEvent().getVAlarm().removeProperties("ACKNOWLEDGED");
        iCalResource.getVEvent().getVAlarm().removeProperties("X-MOZ-LASTACK");
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(publicFolderID, uid);
        assertNotNull(appointment, "appointment not found on server");
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(20, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = super.get(publicFolderID, uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT20M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateReminderAtServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next friday at 08:50");
        Date end = TimeTools.D("next friday at 09:20");
        Appointment appointment = generateAppointment(start, end, uid, "test", "test");
        appointment.setAlarm(30);
        appointment = create(publicFolderID, appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = super.get(String.valueOf(publicFolder.getObjectID()), uid, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT30M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemoveReminderAtServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next friday at 08:50");
        Date end = TimeTools.D("next friday at 09:20");
        Appointment appointment = generateAppointment(start, end, uid, "test", "test");
        appointment.setAlarm(30);
        appointment = create(publicFolderID, appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = super.get(String.valueOf(publicFolder.getObjectID()), uid, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT30M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * remove reminder at server
         */
        appointment.setAlarm(-1);
        super.update(appointment);
        /*
         * verify appointment on client
         */
        iCalResource = super.get(publicFolderID, uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertDummyAlarm(iCalResource.getVEvent());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testEditReminderAtServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("next friday at 08:50");
        Date end = TimeTools.D("next friday at 09:20");
        Appointment appointment = generateAppointment(start, end, uid, "test", "test");
        appointment.setAlarm(30);
        appointment = create(publicFolderID, appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = super.get(String.valueOf(publicFolder.getObjectID()), uid, null);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT30M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * edit reminder at server
         */
        appointment.setAlarm(20);
        super.update(appointment);
        /*
         * verify appointment on client
         */
        iCalResource = super.get(publicFolderID, uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT20M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
    }

}
