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

import static com.openexchange.java.Autoboxing.i;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import com.openexchange.chronos.ResourceId;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Participant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug60589Test}
 *
 * Options to accept external meeting invite missing
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.1
 */
public class Bug60589Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateWithInternalResource(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * lookup internal resource for test
         */
        int resId = i(testContext.acquireResource());
        String resourceUri = ResourceId.forResource(getClient().getValues().getContextId(), resId);
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("tomorrow at 15:30");
        Date end = TimeTools.D("tomorrow at 16:00");
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
            "ORGANIZER:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;CUTYPE=RESOURCE;PARTSTAT=NEEDS-ACTION:" + resourceUri + "\r\n" +
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
        assertEquals(2, appointment.getParticipants().length, "wrong number of participants");

        Participant resourceParticipant = null;
        for (Participant participant : appointment.getParticipants()) {
            if (Participant.RESOURCE == participant.getType()) {
                resourceParticipant = participant;
                break;
            }
        }
        assertNotNull(resourceParticipant, "Resource participant not found in appointment");
        assertEquals(resId, resourceParticipant.getIdentifier(), "Internal resource not resolved");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateWithInternalResource(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * lookup internal resource for test
         */
        int resId = i(testContext.acquireResource());
        String resourceUri = ResourceId.forResource(getClient().getValues().getContextId(), resId);
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("tomorrow at 15:30");
        Date end = TimeTools.D("tomorrow at 16:00");
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
            "ORGANIZER:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
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
        assertEquals(1, appointment.getParticipants().length, "wrong number of participants");
        /*
         * update appointment on client & add resource
         */
        ICalResource iCalResource = get(uid);
        iCalResource.getVEvent().getProperties().add(new Property("ATTENDEE;CUTYPE=RESOURCE;PARTSTAT=NEEDS-ACTION:" + resourceUri));
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertEquals(2, appointment.getParticipants().length, "wrong number of participants");
        Participant resourceParticipant = null;
        for (Participant participant : appointment.getParticipants()) {
            if (Participant.RESOURCE == participant.getType()) {
                resourceParticipant = participant;
                break;
            }
        }
        assertNotNull(resourceParticipant, "Resource participant not found in appointment");
        assertEquals(resId, resourceParticipant.getIdentifier(), "Internal resource not resolved");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateWithForeignResource(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("tomorrow at 15:30");
        Date end = TimeTools.D("tomorrow at 16:00");
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
            "ORGANIZER:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;CUTYPE=RESOURCE;PARTSTAT=NEEDS-ACTION;CN=Telefonkonferenzraum-78:urn:uuid:00000000-000a-0003-0000-0000000001c1\r\n" +
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
        assertEquals(2, appointment.getParticipants().length, "wrong number of participants");

        Participant resourceParticipant = null;
        for (Participant participant : appointment.getParticipants()) {
            if (Participant.RESOURCE == participant.getType()) {
                resourceParticipant = participant;
                break;
            }
        }
        assertNotNull(resourceParticipant, "Resource participant not found in appointment");
        assertEquals(0, resourceParticipant.getIdentifier(), "Internal identifier for foreign resource participant");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateWithForeignResource(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = randomUID();
        Date start = TimeTools.D("tomorrow at 15:30");
        Date end = TimeTools.D("tomorrow at 16:00");
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
            "ORGANIZER:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED:mailto:" + getClient().getValues().getDefaultAddress() + "\r\n" +
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
        assertEquals(1, appointment.getParticipants().length, "wrong number of participants");
        /*
         * update appointment on client & add resource
         */
        ICalResource iCalResource = get(uid);
        iCalResource.getVEvent().getProperties().add(new Property("ATTENDEE;CUTYPE=RESOURCE;PARTSTAT=NEEDS-ACTION;CN=Telefonkonferenzraum-78:urn:uuid:00000000-000a-0003-0000-0000000001c1"));
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        assertEquals(2, appointment.getParticipants().length, "wrong number of participants");
        Participant resourceParticipant = null;
        for (Participant participant : appointment.getParticipants()) {
            if (Participant.RESOURCE == participant.getType()) {
                resourceParticipant = participant;
                break;
            }
        }
        assertNotNull(resourceParticipant, "Resource participant not found in appointment");
        assertEquals(0, resourceParticipant.getIdentifier(), "Internal identifier for foreign resource participant");
    }

}
