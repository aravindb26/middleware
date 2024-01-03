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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link ExDateTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class ExDateTest extends Abstract2UserCalDAVTest {

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
    public void testDeleteChangeExceptionAsAttendee(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create appointment series on server as user b & invite user a
         */
        String uid = randomUID();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("last month in the morning", TimeZone.getTimeZone("Europe/Berlin")));
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setTitle("ExDateTest");
        appointment.setIgnoreConflicts(true);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.addParticipant(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment.setParentFolderID(manager2.getPrivateFolder());
        manager2.insert(appointment);
        Date clientLastModified = manager2.getLastModification();
        /*
         * create change exception on server as user b
         */
        Appointment exception = new Appointment();
        exception.setTitle("ExDateTest_edit");
        exception.setObjectID(appointment.getObjectID());
        exception.setRecurrencePosition(2);
        exception.setLastModified(clientLastModified);
        exception.setParentFolderID(appointment.getParentFolderID());
        appointment.addParticipant(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        manager2.update(exception);
        clientLastModified = getManager().getLastModification();
        /*
         * delete the change exception as user a
         */
        exception = getManager().get(getManager().getPrivateFolder(), exception.getObjectID());
        getManager().delete(exception);
        /*
         * verify appointment series on client as user a
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(1, iCalResource.getVEvents().size(), "Change exception found");
        assertNotNull(iCalResource.getVEvent().getExDates(), "No EXDATEs in iCal found");
        assertEquals(1, iCalResource.getVEvent().getExDates().size(), "Unexpected number of EXDATEs found");
        /*
         * change confirmation status of series on client as user a to trigger an update
         */
        Property attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "ATTENDEE not found");
        attendee.getAttributes().put("PARTSTAT", "TENTATIVE");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
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
        assertNotNull(partipant, "accepting participant not found");
        assertEquals(Appointment.TENTATIVE, partipant.getConfirm(), "confirmation status wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(1, iCalResource.getVEvents().size(), "Change exception found");
        assertNotNull(iCalResource.getVEvent().getExDates(), "No EXDATEs in iCal found");
        assertEquals(1, iCalResource.getVEvent().getExDates().size(), "Unexpected number of EXDATEs found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "ATTENDEE not found");
        assertEquals("TENTATIVE", attendee.getAttribute("PARTSTAT"), "PARTSTAT wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDeleteOccurrenceAsAttendee(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create appointment series on server as user b & invite user a
         */
        String uid = randomUID();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("last month in the morning", TimeZone.getTimeZone("Europe/Berlin")));
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setTitle("ExDateTest");
        appointment.setIgnoreConflicts(true);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        appointment.setStartDate(calendar.getTime());
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        appointment.setEndDate(calendar.getTime());
        appointment.addParticipant(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        appointment.setParentFolderID(manager2.getPrivateFolder());
        manager2.insert(appointment);
        Date clientLastModified = manager2.getLastModification();
        /*
         * delete occurrence as user a
         */
        Appointment exception = new Appointment();
        exception.setObjectID(appointment.getObjectID());
        exception.setRecurrencePosition(2);
        exception.setLastModified(clientLastModified);
        exception.setParentFolderID(getManager().getPrivateFolder());
        getManager().delete(exception);
        clientLastModified = getManager().getLastModification();
        /*
         * verify appointment series on client as user a
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(1, iCalResource.getVEvents().size(), "Change exception found");
        assertNotNull(iCalResource.getVEvent().getExDates(), "No EXDATEs in iCal found");
        assertEquals(1, iCalResource.getVEvent().getExDates().size(), "Unexpected number of EXDATEs found");
        /*
         * change confirmation status of series on client as user a to trigger an update
         */
        Property attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "ATTENDEE not found");
        attendee.getAttributes().put("PARTSTAT", "TENTATIVE");
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
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
        assertNotNull(partipant, "accepting participant not found");
        assertEquals(Appointment.TENTATIVE, partipant.getConfirm(), "confirmation status wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(1, iCalResource.getVEvents().size(), "Change exception found");
        assertNotNull(iCalResource.getVEvent().getExDates(), "No EXDATEs in iCal found");
        assertEquals(1, iCalResource.getVEvent().getExDates().size(), "Unexpected number of EXDATEs found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "ATTENDEE not found");
        assertEquals("TENTATIVE", attendee.getAttribute("PARTSTAT"), "PARTSTAT wrong");
    }

}
