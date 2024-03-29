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

import java.util.Date;
import java.util.TimeZone;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MailtoEncodingTest} - Tests appointment creation via the CalDAV interface
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MailtoEncodingTest extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        String uid = randomUID();
        Date start = TimeTools.D("tomorrow at 16:00", TimeZone.getTimeZone("Europe/Berlin"));
        Date end = TimeTools.D("tomorrow at 17:00", TimeZone.getTimeZone("Europe/Berlin"));
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR\r\n" +
            "VERSION:2.0\r\n" +
            "PRODID:-//Apple Inc.//Mac OS X 10.12.3//EN\r\n" +
            "CALSCALE:GREGORIAN\r\n" +
            "BEGIN:VTIMEZONE\r\n" +
            "TZID:Europe/Berlin\r\n" +
            "BEGIN:DAYLIGHT\r\n" +
            "TZOFFSETFROM:+0100\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n" +
            "DTSTART:19810329T020000\r\n" +
            "TZNAME:GMT+2\r\n" +
            "TZOFFSETTO:+0200\r\n" +
            "END:DAYLIGHT\r\n" +
            "BEGIN:STANDARD\r\n" +
            "TZOFFSETFROM:+0200\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n" +
            "DTSTART:19961027T030000\r\n" +
            "TZNAME:GMT+1\r\n" +
            "TZOFFSETTO:+0100\r\n" +
            "END:STANDARD\r\n" +
            "END:VTIMEZONE\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" +
            "TRANSP:OPAQUE\r\n" +
            "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC\r\n" +
            "SUMMARY:test idn\r\n" +
            "ORGANIZER:mailto:" + getAJAXClient().getValues().getDefaultAddress() + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "SEQUENCE:0\r\n" +
            "ATTENDEE;CN=\"Horst M\u00fcller\";CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIP" + "\r\n" +
            " ANT:mailto:horst@m%C3%BCller.example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION:mailto:" + getAJAXClient().getValues().getDefaultAddress() + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR"
        ;   // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify event on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
        assertNotNull(iCalResource.getVEvent().getAttendee("horst@m%C3%BCller.example.com"), "ATTENDEE not found");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertEquals(2, appointment.getParticipants().length, "unexpected number of participants");
        Participant externalParticipant = null;
        for (Participant participant : appointment.getParticipants()) {
            if (0 >= participant.getIdentifier()) {
                externalParticipant = participant;
                break;
            }
        }
        assertNotNull(externalParticipant, "No external participant found");
        assertEquals("horst@m\u00fcller.example.com", externalParticipant.getEmailAddress(), "E-Mail address wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on server
         */
        String uid = randomUID();
        String summary = "idn test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, "test");
        appointment.addParticipant(new ExternalUserParticipant("horst@m\u00fcller.example.com"));
        rememberForCleanUp(create(appointment));
        /*
         * verify event on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "DTSTART wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "DTEND wrong");
        assertNotNull(iCalResource.getVEvent().getAttendee("horst@m%C3%BCller.example.com"), "ATTENDEE not found");
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertEquals(2, appointment.getParticipants().length, "unexpected number of participants");
        Participant externalParticipant = null;
        for (Participant participant : appointment.getParticipants()) {
            if (0 >= participant.getIdentifier()) {
                externalParticipant = participant;
                break;
            }
        }
        assertNotNull(externalParticipant, "No external participant found");
        assertEquals("horst@m\u00fcller.example.com", externalParticipant.getEmailAddress(), "E-Mail address wrong");
    }

}
