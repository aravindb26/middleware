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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.Date;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.WebDAVClient;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Participant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug60193Test}
 *
 * Calendar: Accepted appointments continue to show up as to be accepted
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.1
 */
public class Bug60193Test extends Abstract2UserCalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRSVPAfterAcceptOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * as user b, create appointment via caldav & set rsvp flag for user a
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 11:30");
        Date end = TimeTools.D("next monday at 12:15");
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
            "SUMMARY:Test\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "ORGANIZER:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        WebDAVClient webDAVClient2 = new WebDAVClient(testUser2, getDefaultUserAgent(), null);
        assertEquals(StatusCodes.SC_CREATED, putICal(webDAVClient2, encodeFolderID(String.valueOf(client2.getValues().getPrivateAppointmentFolder())), uid, iCal, Collections.emptyMap()), "response code wrong");
        /*
         * as user a, check appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("TRUE", userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
        /*
         * as user a, accept appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        Participant participant = null;
        for (Participant p : appointment.getParticipants()) {
            if (getClient().getValues().getUserId() == p.getIdentifier()) {
                participant = p;
                break;
            }
        }
        assertNotNull(participant, "participant not found on server");
        getManager().confirm(appointment, Appointment.ACCEPT, "ok");
        /*
         * as user a, check appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("ACCEPTED", userAttendee.getAttribute("PARTSTAT"), "PARTSTAT wrong in user attendee");
        assertNotEquals("TRUE", userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRSVPAfterAcceptOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * as user b, create appointment via caldav & set rsvp flag for user a
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 11:30");
        Date end = TimeTools.D("next monday at 12:15");
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
            "SUMMARY:Test\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "ORGANIZER:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        WebDAVClient webDAVClient2 = new WebDAVClient(testUser2, getDefaultUserAgent(), null);
        assertEquals(StatusCodes.SC_CREATED, putICal(webDAVClient2, encodeFolderID(String.valueOf(client2.getValues().getPrivateAppointmentFolder())), uid, iCal, Collections.emptyMap()), "response code wrong");
        /*
         * as user a, check appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("TRUE", userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
        /*
         * as user a, accept appointment on client
         */
        userAttendee.getAttributes().put("PARTSTAT", "ACCEPTED");
        userAttendee.getAttributes().remove("RSVP");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * as user a, check appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("ACCEPTED", userAttendee.getAttribute("PARTSTAT"), "PARTSTAT wrong in user attendee");
        assertNull(userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRSVPAfterAcceptOnClientWithoutRemovingRSVP(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * as user b, create appointment via caldav & set rsvp flag for user a
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 11:30");
        Date end = TimeTools.D("next monday at 12:15");
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
            "SUMMARY:Test\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "ORGANIZER:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        WebDAVClient webDAVClient2 = new WebDAVClient(testUser2, getDefaultUserAgent(), null);
        assertEquals(StatusCodes.SC_CREATED, putICal(webDAVClient2, encodeFolderID(String.valueOf(client2.getValues().getPrivateAppointmentFolder())), uid, iCal, Collections.emptyMap()), "response code wrong");
        /*
         * as user a, check appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("TRUE", userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
        /*
         * as user a, accept appointment on client
         */
        userAttendee.getAttributes().put("PARTSTAT", "ACCEPTED");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * as user a, check appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("ACCEPTED", userAttendee.getAttribute("PARTSTAT"), "PARTSTAT wrong in user attendee");
        assertNotEquals("TRUE", userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRSVPAfterAcceptingAcceptedOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * as user b, "import" appointment via caldav & set rsvp flag for user a (already accepted)
         */
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 11:30");
        Date end = TimeTools.D("next monday at 12:15");
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
            "SUMMARY:Test\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "ORGANIZER:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + client2.getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED;RSVP=TRUE:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        WebDAVClient webDAVClient2 = new WebDAVClient(testUser2, getDefaultUserAgent(), null);
        assertEquals(StatusCodes.SC_CREATED, putICal(webDAVClient2, encodeFolderID(String.valueOf(client2.getValues().getPrivateAppointmentFolder())), uid, iCal, Collections.emptyMap()), "response code wrong");
        /*
         * as user a, check appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("TRUE", userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
        assertEquals("ACCEPTED", userAttendee.getAttribute("PARTSTAT"), "PARTSTAT wrong in user attendee");
        /*
         * as user a, accept appointment on client
         */
        userAttendee.getAttributes().put("PARTSTAT", "ACCEPTED");
        userAttendee.getAttributes().remove("RSVP");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * as user a, check appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        userAttendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(userAttendee, "attendee not found in ical");
        assertEquals("ACCEPTED", userAttendee.getAttribute("PARTSTAT"), "PARTSTAT wrong in user attendee");
        assertNull(userAttendee.getAttribute("RSVP"), "RSVP wrong in user attendee");
    }

}
