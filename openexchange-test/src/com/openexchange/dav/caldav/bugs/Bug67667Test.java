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

import java.util.Calendar;
import java.util.TimeZone;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug67667Test}
 *
 * Appointments tend to lose confirmation status
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.4
 */
public class Bug67667Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager catm2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        catm2 = new CalendarTestManager(client2);
        catm2.setFailOnError(true);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testPreservePartstat(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * as user b, create appointment on server & invite user a
         */
        String uid = randomUID();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("next monday in the morning", TimeZone.getTimeZone("Europe/Berlin")));
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setTitle("Bug67667Test");
        appointment.setIgnoreConflicts(true);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.addParticipant(new UserParticipant(catm2.getClient().getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment.setParentFolderID(catm2.getPrivateFolder());
        appointment = catm2.insert(appointment);
        /*
         * as user a, get & "accept" the appointment using the web client
         */
        appointment = catm.get(catm.getPrivateFolder(), appointment.getObjectID());
        assertNotNull(appointment);
        catm.confirm(appointment, Appointment.ACCEPT, "ja");
        /*
         * as user a, synchronize & get the event resource
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        Property attendeeProperty = iCalResource.getVEvent().getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "No matching attendee in iCal found");
        assertEquals("ACCEPTED", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        /*
         * wait a second before the next update to catch concurrent updates properly (timestamp precision is limited to seconds)
         */
        Thread.sleep(1000L);
        /*
         * as user a, get & "decline" the appointment using the web client
         */
        appointment = catm.get(catm.getPrivateFolder(), appointment.getObjectID());
        assertNotNull(appointment);
        catm.confirm(appointment, Appointment.DECLINE, "doch nicht");
        /*
         * as user a, update the event resource by adding a reminder, w/o updating the client copy
         */
        Component alarmComponent = new Component("VALARM");
        alarmComponent.getProperties().add(new Property("UID:" + randomUID()));
        alarmComponent.getProperties().add(new Property("TRIGGER:-PT5M"));
        alarmComponent.getProperties().add(new Property("DESCRIPTION:Ereignisbenachrichtigung"));
        alarmComponent.getProperties().add(new Property("ACTION:DISPLAY"));
        iCalResource.getVEvent().getComponents().add(alarmComponent);
        assertEquals(HttpServletResponse.SC_PRECONDITION_FAILED, putICalUpdate(getDefaultFolderID(), uid, iCalResource.toString(), null, iCalResource.getScheduleTag()), "response code wrong");
        /*
         * verify that attendees partstat was not changed on server
         */
        appointment = catm.get(appointment);
        for (UserParticipant participant : appointment.getUsers()) {
            if (catm.getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
    }

}
