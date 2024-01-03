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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug23610Test}
 *
 * "Shown as" status changed when confirming/declining appointment in Apple iCal client as participant
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug23610Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testConfirmAppointment(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        for (int shownAs : new int[] { Appointment.FREE, Appointment.TEMPORARY, Appointment.RESERVED, Appointment.ABSENT }) {
            for (int confirmation : new int[] { Appointment.ACCEPT, Appointment.DECLINE, Appointment.TENTATIVE }) {
                this.confirmAppointment(shownAs, confirmation);
            }
        }
    }

    private void confirmAppointment(int appointmentShownAs, int confirmationStatus) throws Exception {
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment on server
         */
        String uid = randomUID();
        String summary = "Bug23610Test-" + appointmentShownAs + "-" + confirmationStatus;
        String location = "test";
        Date start = TimeTools.D("next saturday at 10:00");
        Date end = TimeTools.D("next saturday at 11:00");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setOrganizer("otto@example.com");
        appointment.addParticipant(new UserParticipant(super.getAJAXClient().getValues().getUserId()));
        ExternalUserParticipant participant = new ExternalUserParticipant("otto@example.com");
        participant.setConfirm(Appointment.ACCEPT);
        appointment.addParticipant(participant);
        appointment.setShownAs(appointmentShownAs);
        super.rememberForCleanUp(super.create(appointment));
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        if (null != iCalResource.getVEvent().getTransp()) {
            assertEquals(Appointment.FREE == appointmentShownAs ? "TRANSPARENT" : "OPAQUE", iCalResource.getVEvent().getTransp(), "TRANSP wrong");
        }
        /*
         * confirm appointment on client
         */
        String partstat = Appointment.TENTATIVE == confirmationStatus ? "TENTATIVE" : Appointment.DECLINE == confirmationStatus ? "DECLINED" : "ACCEPTED";
        List<Property> attendees = iCalResource.getVEvent().getProperties("ATTENDEE");
        for (Property property : attendees) {
            if (property.getValue().contains(super.getAJAXClient().getValues().getDefaultAddress())) {
                for (Entry<String, String> attribute : property.getAttributes().entrySet()) {
                    if (attribute.getKey().equals("PARTSTAT") && false == partstat.equals(attribute.getValue())) {
                        attribute.setValue(partstat);
                        iCalResource.getVEvent().setTransp(Appointment.DECLINE == confirmationStatus ? "TRANSPARENT" : "OPAQUE");
                    }
                }
                break;
            }
        }
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        UserParticipant[] users = appointment.getUsers();
        assertNotNull(users, "appointment has no users");
        UserParticipant partipant = null;
        for (UserParticipant user : users) {
            if (getAJAXClient().getValues().getUserId() == user.getIdentifier()) {
                partipant = user;
                break;
            }
        }
        assertNotNull(partipant, "confirming participant not found");
        assertEquals(confirmationStatus, partipant.getConfirm(), "confirmation status wrong");
        assertEquals(appointmentShownAs, appointment.getShownAs(), "shown as wrong");
        /*
         * verify appointment on client
         */
        iCalResource = super.get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        Property attendee = null;
        attendees = iCalResource.getVEvent().getProperties("ATTENDEE");
        for (Property property : attendees) {
            if (property.getValue().contains(super.getAJAXClient().getValues().getDefaultAddress())) {
                attendee = property;
                break;
            }
        }
        assertNotNull(attendee, "confirming attendee not found");
        assertEquals(partstat, attendee.getAttribute("PARTSTAT"), "partstat status wrong");
        if (null != iCalResource.getVEvent().getTransp()) {
            assertEquals(Appointment.FREE == appointmentShownAs ? "TRANSPARENT" : "OPAQUE", iCalResource.getVEvent().getTransp(), "TRANSP wrong");
        }
    }

}
