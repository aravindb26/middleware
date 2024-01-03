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
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.UserAgents;
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
 * {@link Bug62008Test} - Calendar: Appointment gets deleted w/o action by organizer / Sync with caldav iOS
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.2
 */
public class Bug62008Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager catm2;

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.IOS_12_0;
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        catm2 = new CalendarTestManager(client2);
        catm2.setFailOnError(true);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRestoreDeclinedExceptionAsOrganizer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment series on server
         */
        String uid = randomUID();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("last week at noon", TimeZone.getTimeZone("Europe/Berlin")));
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setTitle("Bug62008Test");
        appointment.setIgnoreConflicts(true);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setRecurrenceCount(20);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.addParticipant(new UserParticipant(client2.getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment.setParentFolderID(catm.getPrivateFolder());
        catm.insert(appointment);
        Date clientLastModified = catm.getLastModification();
        /*
         * create change exception on server & decline it
         */
        catm.confirm(appointment.getParentFolderID(), appointment.getObjectID(), clientLastModified, Appointment.DECLINE, "keine zeit", 4);
        clientLastModified = catm.getLastModification();
        /*
         * verify participation status in exception as user a
         */
        Appointment exception = catm.get(appointment.getParentFolderID(), appointment.getObjectID(), 4);
        assertNotNull(exception);
        assertNotNull(exception.getUsers());
        for (UserParticipant participant : exception.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * verify participation status in exception as user b
         */
        Appointment exception2 = catm2.get(catm2.getPrivateFolder(), appointment.getObjectID(), 4);
        assertNotNull(exception2);
        assertNotNull(exception2.getUsers());
        for (UserParticipant participant : exception2.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * get & check appointment via caldav as user a
         */
        ICalResource iCalResource = get(appointment.getUid());
        assertNotNull(iCalResource, "Event not found via CalDAV");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs");
        Property attendeeProperty = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found");
        assertEquals("ACCEPTED", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        attendeeProperty = iCalResource.getVEvents().get(1).getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found in exception");
        assertEquals("DECLINED", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong in exception");
        /*
         * (try and) delete exception as user a
         */
        Component exceptionComponent = iCalResource.getVEvents().get(1);
        Property recurrenceIdProperty = exceptionComponent.getProperty("RECURRENCE-ID");
        iCalResource.getVCalendar().getComponents().remove(exceptionComponent);
        iCalResource.getVEvent().setProperty("EXDATE", recurrenceIdProperty.getValue(), recurrenceIdProperty.getAttributes());
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify participation status in exception as user a
         */
        exception = catm.get(appointment.getParentFolderID(), appointment.getObjectID(), 4);
        assertNotNull(exception);
        assertNotNull(exception.getUsers());
        for (UserParticipant participant : exception.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * verify participation status in exception as user b
         */
        exception2 = catm2.get(catm2.getPrivateFolder(), appointment.getObjectID(), 4);
        assertNotNull(exception2);
        assertNotNull(exception2.getUsers());
        for (UserParticipant participant : exception2.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * get & check appointment via caldav as user a
         */
        iCalResource = get(appointment.getUid());
        assertNotNull(iCalResource, "Event not found via CalDAV");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs");
        attendeeProperty = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found");
        assertEquals("ACCEPTED", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        attendeeProperty = iCalResource.getVEvents().get(1).getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found in exception");
        assertEquals("DECLINED", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong in exception");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRestoreDeclinedExceptionAsAttendee(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment series on server as user b
         */
        String uid = randomUID();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("last week at noon", TimeZone.getTimeZone("Europe/Berlin")));
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setTitle("Bug62008Test");
        appointment.setIgnoreConflicts(true);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setRecurrenceCount(20);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.addParticipant(new UserParticipant(client2.getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment.setParentFolderID(catm2.getPrivateFolder());
        catm2.insert(appointment);
        Date clientLastModified = catm2.getLastModification();
        /*
         * create change exception on server & decline it as user a
         */
        catm.confirm(catm.getPrivateFolder(), appointment.getObjectID(), clientLastModified, Appointment.DECLINE, "keine zeit", 4);
        clientLastModified = catm.getLastModification();
        /*
         * verify participation status in exception as user b
         */
        Appointment exception = catm2.get(appointment.getParentFolderID(), appointment.getObjectID(), 4);
        assertNotNull(exception);
        assertNotNull(exception.getUsers());
        for (UserParticipant participant : exception.getUsers()) {
            if (catm.getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * verify participation status in exception as user a
         */
        Appointment exception2 = catm.get(catm.getPrivateFolder(), appointment.getObjectID(), 4);
        assertNotNull(exception2);
        assertNotNull(exception2.getUsers());
        for (UserParticipant participant : exception2.getUsers()) {
            if (catm.getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * get & check appointment via caldav as user a
         */
        ICalResource iCalResource = get(appointment.getUid());
        assertNotNull(iCalResource, "Event not found via CalDAV");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs");
        Property attendeeProperty = iCalResource.getVEvent().getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found");
        assertEquals("NEEDS-ACTION", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        attendeeProperty = iCalResource.getVEvents().get(1).getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found in exception");
        assertEquals("DECLINED", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong in exception");
        /*
         * (try and) delete exception as user a
         */
        Component exceptionComponent = iCalResource.getVEvents().get(1);
        Property recurrenceIdProperty = exceptionComponent.getProperty("RECURRENCE-ID");
        iCalResource.getVCalendar().getComponents().remove(exceptionComponent);
        iCalResource.getVEvent().setProperty("EXDATE", recurrenceIdProperty.getValue(), recurrenceIdProperty.getAttributes());
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify participation status in exception as user b
         */
        exception = catm2.get(appointment.getParentFolderID(), appointment.getObjectID(), 4);
        assertNotNull(exception);
        assertNotNull(exception.getUsers());
        for (UserParticipant participant : exception.getUsers()) {
            if (catm.getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * verify participation status in exception as user a
         */
        exception2 = catm.get(catm.getPrivateFolder(), appointment.getObjectID(), 4);
        assertNotNull(exception2);
        assertNotNull(exception2.getUsers());
        for (UserParticipant participant : exception2.getUsers()) {
            if (catm.getClient().getValues().getUserId() == participant.getIdentifier()) {
                assertEquals(Appointment.DECLINE, participant.getConfirm(), "Wrong participation status");
            }
        }
        /*
         * get & check appointment via caldav as user a
         */
        iCalResource = get(appointment.getUid());
        assertNotNull(iCalResource, "Event not found via CalDAV");
        assertEquals(2, iCalResource.getVEvents().size(), "Unexpected number of VEVENTs");
        attendeeProperty = iCalResource.getVEvent().getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found");
        assertEquals("NEEDS-ACTION", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong");
        attendeeProperty = iCalResource.getVEvents().get(1).getAttendee(catm.getClient().getValues().getDefaultAddress());
        assertNotNull(attendeeProperty, "Attendee not found in exception");
        assertEquals("DECLINED", attendeeProperty.getAttribute("PARTSTAT"), "PARTSTAT wrong in exception");
    }

}
