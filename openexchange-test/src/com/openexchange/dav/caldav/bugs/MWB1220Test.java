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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB1220Test}
 *
 * 500 internal server error(s) for one dedicated EAS Account
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class MWB1220Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDuplicateUserAttendeeViaCalendarApi(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create externally organized event where the calendar user is listed twice
         */
        String email = getClient().getValues().getDefaultAddress();
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 12:00");
        Date end = TimeTools.D("next monday at 13:00");
        Appointment appointment = generateAppointment(start, end, uid, "test", "test2");
        appointment.setOrganizer("test@example.org");
        appointment.addParticipant(new ExternalUserParticipant("test@example.org"));
        ExternalUserParticipant externalParticipant1 = new ExternalUserParticipant(email);
        externalParticipant1.setDisplayName("B, A");
        ExternalUserParticipant externalParticipant2 = new ExternalUserParticipant(email);
        externalParticipant2.setDisplayName("A B");
        appointment.addParticipant(externalParticipant1);
        appointment.addParticipant(externalParticipant2);
        appointment.setIgnoreConflicts(true);
        appointment.setParentFolderID(getClient().getValues().getPrivateAppointmentFolder());
        appointment.setSequence(0);
        getManager().insert(appointment);
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        rememberForCleanUp(appointment);
        assertEquals(uid, appointment.getUid());
        assertEquals(2, appointment.getParticipants().length);
        /*
         * verify appointment on client via GET
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(2, iCalResource.getVEvent().getProperties("ATTENDEE").size());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDuplicateUserAttendeeViaCalDAV(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create externally organized event where the calendar user is listed twice
         */
        String email = getClient().getValues().getDefaultAddress();
        String uid = randomUID();
        Date start = TimeTools.D("next monday at 12:00");
        Date end = TimeTools.D("next monday at 13:00");
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART:" + formatAsUTC(start) + "\r\n" +
            "DTEND:" + formatAsUTC(end) + "\r\n" +
            "SUMMARY:test\r\n" +
            "ORGANIZER:mailto:test@example.org\r\n" +
            "ATTENDEE;PARTSTAT=ACCEPTED;CUTYPE=INDIVIDUAL:mailto:test@example.org\r\n" +
            "ATTENDEE;CN=\"B, A\";PARTSTAT=NEEDS-ACTION;CUTYPE=INDIVIDUAL:MAILTO:" + email + "\r\n" +
            "ATTENDEE;CN=\"A B\";PARTSTAT=NEEDS-ACTION;CUTYPE=INDIVIDUAL:mailto:" + email + "\r\n" +
            "SUMMARY:test\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR" + "\r\n";
        // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        rememberForCleanUp(appointment);
        assertEquals(uid, appointment.getUid());
        assertEquals(2, appointment.getParticipants().length);
        /*
         * verify appointment on client via GET
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(2, iCalResource.getVEvent().getProperties("ATTENDEE").size());
    }

}
