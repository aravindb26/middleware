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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.container.participants.ConfirmableParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug23181Test}
 *
 * Unable to import external appointment update with newly added participant from Thunderbird/Lightning
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug23181Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager manager2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        manager2 = new CalendarTestManager(client2);
        manager2.setFailOnError(true);

    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testImportAppointment(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * Create appointment in user B's calendar on server
         */
        String userA = client1.getValues().getDefaultAddress();
        String userB = client2.getValues().getDefaultAddress();
        String uid = randomUID();
        String summary = "Bug23181Test";
        String location = "tbd";
        Date start = TimeTools.D("tomorrow at 3pm");
        Date end = TimeTools.D("tomorrow at 4pm");
        Appointment appointmentB = generateAppointment(start, end, uid, summary, location);
        appointmentB.setOrganizer("extern1@example.com");
        appointmentB.addParticipant(new ExternalUserParticipant("extern2@example.com"));
        appointmentB.addParticipant(new ExternalUserParticipant("extern3@example.com"));
        appointmentB.addParticipant(new ExternalUserParticipant(userB));
        appointmentB.setIgnoreConflicts(true);
        appointmentB.setParentFolderID(manager2.getClient().getValues().getPrivateAppointmentFolder());
        appointmentB.setSequence(0);
        manager2.insert(appointmentB);
        /*
         * confirm updated appointment as user A in client
         */
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "METHOD:REQUEST" + "\r\n" +
            "PRODID:Microsoft Exchange Server 2007" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "CALSCALE:GREGORIAN" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "ORGANIZER;CN=Extern 1:MAILTO:extern1@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=Extern 2:MAILTO:extern2@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=Extern 3:MAILTO:extern3@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=" + userB + ":MAILTO:" + userB + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;RSVP=TRUE;CN=" + userA + ":MAILTO:" + userA + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTEND:" + formatAsUTC(end) + "\r\n" +
            "TRANSP:OPAQUE" + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" +
            "SUMMARY:" + summary + "\r\n" +
            "LOCATION:" + location + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART:" + formatAsUTC(start) + "\r\n" +
            "SEQUENCE:1" + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, super.putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment as user A on server
         */
        Appointment appointmentA = super.getAppointment(uid);
        super.rememberForCleanUp(appointmentA);
        assertNotNull(appointmentA, "appointment not found on server");
        assertNotNull(appointmentA.getUsers(), "appointment has no users");
        UserParticipant userParticipantA = null;
        ConfirmableParticipant participantB = null;
        for (UserParticipant participant : appointmentA.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                userParticipantA = participant;
            }
        }
        for (ConfirmableParticipant participant : appointmentA.getConfirmations()) {
            if (manager2.getClient().getValues().getDefaultAddress().toLowerCase().equals(participant.getEmailAddress())) {
                participantB = participant;
            }
        }
        assertNotNull(userParticipantA, "added user participant not found");
        assertNotNull(participantB, "previous participant not found");
        assertEquals(Appointment.ACCEPT, userParticipantA.getConfirm(), "confirmation status wrong");
        /*
         * verify appointment as user B on server
         */
        appointmentB = manager2.get(manager2.getClient().getValues().getPrivateAppointmentFolder(), appointmentB.getObjectID());
        assertNotNull(appointmentB, "appointment not found on server");
        assertNotNull(appointmentB.getUsers(), "appointment has no users");
        ConfirmableParticipant participantA = null;
        UserParticipant userParticipantB = null;
        for (UserParticipant participant : appointmentB.getUsers()) {
            if (manager2.getClient().getValues().getUserId() == participant.getIdentifier()) {
                userParticipantB = participant;
            }
        }
        for (ConfirmableParticipant participant : appointmentB.getConfirmations()) {
            if (getClient().getValues().getDefaultAddress().toLowerCase().equals(participant.getEmailAddress())) {
                participantA = participant;
            }
        }
        assertNull(participantA, "added user participant A was found in B's appointment");
        assertNotNull(userParticipantB, "previous participant not found");
        /*
         * verify appointment as user A on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(appointmentB.getLocation(), iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        Property attendeeA = null;
        Property attendeeB = null;
        List<Property> attendees = iCalResource.getVEvent().getProperties("ATTENDEE");
        for (Property property : attendees) {
            if (property.getValue().toLowerCase().contains(super.getAJAXClient().getValues().getDefaultAddress().toLowerCase())) {
                attendeeA = property;
            } else if (property.getValue().toLowerCase().contains(manager2.getClient().getValues().getDefaultAddress().toLowerCase())) {
                attendeeB = property;
            }
        }
        assertNotNull(attendeeA, "added user attendee not found");
        assertNotNull(attendeeB, "previous attendee not found");
        assertEquals("ACCEPTED", attendeeA.getAttribute("PARTSTAT"), "partstat status wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAlsoImportOutSequencedAppointment(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * Create appointment in user B's calendar on server
         */
        String userA = testUser.getLogin();
        String userB = testUser2.getLogin();
        String uid = randomUID();
        String summary = "Bug23181Test2";
        String location = "tbd";
        Date start = TimeTools.D("tomorrow at 1pm");
        Date end = TimeTools.D("tomorrow at 2pm");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setOrganizer("extern1@example.com");
        appointment.addParticipant(new ExternalUserParticipant("extern2@example.com"));
        appointment.addParticipant(new ExternalUserParticipant("extern3@example.com"));
        appointment.addParticipant(new ExternalUserParticipant(userB));
        appointment.setIgnoreConflicts(true);
        appointment.setParentFolderID(manager2.getClient().getValues().getPrivateAppointmentFolder());
        appointment.setSequence(0);
        appointment = manager2.insert(appointment);
        /*
         * update the appointment once to increase the sequence number
         */
        appointment.setLocation("new location");
        manager2.update(appointment);
        /*
         * try to confirm updated appointment as user A in client
         */
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "METHOD:REQUEST" + "\r\n" +
            "PRODID:Microsoft Exchange Server 2007" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "CALSCALE:GREGORIAN" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "ORGANIZER;CN=Extern 1:MAILTO:extern1@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=Extern 2:MAILTO:extern2@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=Extern 3:MAILTO:extern3@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=" + userB + ":MAILTO:" + userB + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;RSVP=TRUE;CN=" + userA + ":MAILTO:" + userA + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTEND:" + formatAsUTC(end) + "\r\n" +
            "TRANSP:OPAQUE" + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" +
            "SUMMARY:" + summary + "\r\n" +
            "LOCATION:abcdefg" + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART:" + formatAsUTC(start) + "\r\n" +
            "SEQUENCE:0" + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, super.putICal(uid, iCal), "response code wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAlsoImportOtherOrganizersAppointment(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * Create appointment in user B's calendar on server
         */
        String userA = testUser.getLogin();
        String userB = testUser2.getLogin();
        String uid = randomUID();
        String summary = "Bug23181Test3";
        String location = "tbd";
        Date start = TimeTools.D("tomorrow at 1pm");
        Date end = TimeTools.D("tomorrow at 2pm");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setOrganizer("extern1@example.com");
        appointment.addParticipant(new ExternalUserParticipant("extern2@example.com"));
        appointment.addParticipant(new ExternalUserParticipant("extern3@example.com"));
        appointment.addParticipant(new ExternalUserParticipant(userB));
        appointment.setIgnoreConflicts(true);
        appointment.setParentFolderID(manager2.getClient().getValues().getPrivateAppointmentFolder());
        appointment.setSequence(0);
        manager2.insert(appointment);
        /*
         * try to confirm updated appointment as user A in client
         */
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "METHOD:REQUEST" + "\r\n" +
            "PRODID:Microsoft Exchange Server 2007" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "CALSCALE:GREGORIAN" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "ORGANIZER;CN=Extern 1:MAILTO:extern4@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=Extern 2:MAILTO:extern2@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=Extern 3:MAILTO:extern3@example.com" + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=" + userB + ":MAILTO:" + userB + "\r\n" +
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=ACCEPTED;RSVP=TRUE;CN=" + userA + ":MAILTO:" + userA + "\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "DTEND:" + formatAsUTC(end) + "\r\n" +
            "TRANSP:OPAQUE" + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" +
            "SUMMARY:" + summary + "\r\n" +
            "LOCATION:abcdefg" + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTART:" + formatAsUTC(start) + "\r\n" +
            "SEQUENCE:1" + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, super.putICal(uid, iCal), "response code wrong");
    }

}
