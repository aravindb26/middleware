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

import java.util.ArrayList;
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
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link ConfirmationTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ConfirmationTest extends Abstract2UserCalDAVTest {

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
    public void testConfirmSeriesOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment as user 2 on server
         */
        String uid = randomUID();
        String summary = "serie";
        String location = "test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        List<Participant> participants = new ArrayList<Participant>();
        participants.add(new UserParticipant(getClient().getValues().getUserId()));
        participants.add(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.setParticipants(participants);
        appointment.setParentFolderID(manager2.getPrivateFolder());
        appointment.setIgnoreConflicts(true);
        manager2.insert(appointment);
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in iCal");
        assertTrue(null == attendee.getAttribute("PARTSTAT") || "NEEDS-ACTION".equals(attendee.getAttribute("PARTSTAT")), "PARTSTAT wrong");
        /*
         * accept series on client
         */
        attendee.getAttributes().put("PARTSTAT", "ACCEPTED");
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertNotNull(appointment.getUsers(), "no users found in apointment");
        UserParticipant user = null;
        for (UserParticipant participant : appointment.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                user = participant;
                break;
            }
        }
        assertNotNull(user, "User not found");
        assertEquals(Appointment.ACCEPT, user.getConfirm(), "Confirm status wrong");
        /*
         * verify appointment on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        calendarData = super.calendarMultiget(eTags.keySet());
        iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in iCal");
        assertEquals("ACCEPTED", attendee.getAttribute("PARTSTAT"), "PARTSTAT wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testConfirmSeriesOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment as user 2 on server
         */
        String uid = randomUID();
        String summary = "serie";
        String location = "test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        List<Participant> participants = new ArrayList<Participant>();
        participants.add(new UserParticipant(getClient().getValues().getUserId()));
        participants.add(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.setParticipants(participants);
        appointment.setParentFolderID(manager2.getPrivateFolder());
        appointment.setIgnoreConflicts(true);
        manager2.insert(appointment);
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in iCal");
        assertTrue(null == attendee.getAttribute("PARTSTAT") || "NEEDS-ACTION".equals(attendee.getAttribute("PARTSTAT")), "PARTSTAT wrong");
        /*
         * accept series on server
         */
        appointment = getAppointment(uid);
        getManager().confirm(appointment, Appointment.ACCEPT, "ok");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertNotNull(appointment.getUsers(), "no users found in apointment");
        UserParticipant user = null;
        for (UserParticipant participant : appointment.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                user = participant;
                break;
            }
        }
        assertNotNull(user, "User not found");
        assertEquals(Appointment.ACCEPT, user.getConfirm(), "Confirm status wrong");
        /*
         * verify appointment on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        calendarData = super.calendarMultiget(eTags.keySet());
        iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in iCal");
        assertEquals("ACCEPTED", attendee.getAttribute("PARTSTAT"), "PARTSTAT wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testConfirmOccurrenceOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment as user 2 on server
         */
        String uid = randomUID();
        String summary = "serie";
        String location = "test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setTimezone("Europe/Berlin");
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        List<Participant> participants = new ArrayList<Participant>();
        participants.add(new UserParticipant(getClient().getValues().getUserId()));
        participants.add(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.setParticipants(participants);
        appointment.setParentFolderID(manager2.getPrivateFolder());
        appointment.setIgnoreConflicts(true);
        manager2.insert(appointment);
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in iCal");
        assertTrue(null == attendee.getAttribute("PARTSTAT") || "NEEDS-ACTION".equals(attendee.getAttribute("PARTSTAT")), "PARTSTAT of series wrong");
        /*
         * accept occurrence on client
         */
        Component exception = new Component("VEVENT");
        for (Property property : iCalResource.getVEvent().getProperties()) {
            exception.getProperties().add(new Property(property.toString()));
        }
        attendee = exception.getAttendee(getClient().getValues().getDefaultAddress());
        attendee.getAttributes().put("PARTSTAT", "ACCEPTED");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(appointment.getTimezone()));
        calendar.setTime(appointment.getStartDate());
        calendar.add(Calendar.DAY_OF_YEAR, 5);
        Date exceptionStartDate = calendar.getTime();
        calendar.setTime(appointment.getEndDate());
        calendar.add(Calendar.DAY_OF_YEAR, 5);
        Date exceptionEndDate = calendar.getTime();
        exception.getProperties().add(new Property("RECURRENCE-ID;TZID=" + appointment.getTimezone() + ":" + format(exceptionStartDate, appointment.getTimezone())));
        iCalResource.addComponent(exception);
        exception.setDTStart(exceptionStartDate, exception.getProperty("DTSTART").getAttribute("TZID"));
        exception.setDTEnd(exceptionEndDate, exception.getProperty("DTEND").getAttribute("TZID"));
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertNotNull(appointment.getUsers(), "no users found in apointment");
        UserParticipant user = null;
        for (UserParticipant participant : appointment.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                user = participant;
                break;
            }
        }
        assertNotNull(user, "User not found");
        assertEquals(Appointment.NONE, user.getConfirm(), "Confirm status of series wrong");
        assertNotNull(appointment.getChangeException(), "No change exceptions found");
        assertEquals(1, appointment.getChangeException().length, "Invalid number of change exceptions found");
        List<Appointment> changeExceptions = getManager().getChangeExceptions(appointment.getParentFolderID(), appointment.getObjectID(), Appointment.ALL_COLUMNS);
        assertNotNull(changeExceptions, "no change exceptions found");
        assertEquals(1, changeExceptions.size(), "Invalid number of change exceptions found");
        Appointment changeException = getManager().get(appointment.getParentFolderID(), changeExceptions.get(0).getObjectID());
        assertNotNull(changeException, "change exception not found");
        assertEquals(exceptionStartDate, changeException.getStartDate(), "Invalid start date of change exception");
        assertNotNull(changeException.getUsers(), "no users found in apointment");
        user = null;
        for (UserParticipant participant : changeException.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                user = participant;
                break;
            }
        }
        assertNotNull(user, "User not found");
        assertEquals(Appointment.ACCEPT, user.getConfirm(), "Confirm status of change exception wrong");
        /*
         * verify appointment on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        calendarData = super.calendarMultiget(eTags.keySet());
        assertEquals(2, iCalResource.getVEvents().size(), "no exception found in iCal");
        Component seriesVEvent;
        Component exceptionVEvent;
        if (null == iCalResource.getVEvents().get(0).getProperty("RECURRENCE-ID")) {
            seriesVEvent = iCalResource.getVEvents().get(0);
            exceptionVEvent = iCalResource.getVEvents().get(1);
        } else {
            seriesVEvent = iCalResource.getVEvents().get(1);
            exceptionVEvent = iCalResource.getVEvents().get(0);
        }
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        attendee = seriesVEvent.getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in series iCal");
        assertTrue(null == attendee.getAttribute("PARTSTAT") || "NEEDS-ACTION".equals(attendee.getAttribute("PARTSTAT")), "PARTSTAT of series wrong");
        attendee = exceptionVEvent.getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in exception iCal");
        assertEquals("ACCEPTED", attendee.getAttribute("PARTSTAT"), "PARTSTAT wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testConfirmOccurrenceOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment as user 2 on server
         */
        String uid = randomUID();
        String summary = "serie";
        String location = "test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setTimezone("Europe/Berlin");
        appointment.setRecurrenceType(Appointment.DAILY);
        appointment.setInterval(1);
        List<Participant> participants = new ArrayList<Participant>();
        participants.add(new UserParticipant(getClient().getValues().getUserId()));
        participants.add(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.setParticipants(participants);
        appointment.setParentFolderID(manager2.getPrivateFolder());
        appointment.setIgnoreConflicts(true);
        manager2.insert(appointment);
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in iCal");
        assertTrue(null == attendee.getAttribute("PARTSTAT") || "NEEDS-ACTION".equals(attendee.getAttribute("PARTSTAT")), "PARTSTAT of series wrong");
        /*
         * accept occurrence on server
         */
        appointment = getAppointment(uid);
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(appointment.getTimezone()));
        calendar.setTime(appointment.getStartDate());
        calendar.add(Calendar.DAY_OF_YEAR, 5);
        Date exceptionStartDate = calendar.getTime();
        getManager().confirm(appointment, Appointment.ACCEPT, "ok", 6);
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertNotNull(appointment.getUsers(), "no users found in apointment");
        UserParticipant user = null;
        for (UserParticipant participant : appointment.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                user = participant;
                break;
            }
        }
        assertNotNull(user, "User not found");
        assertEquals(Appointment.NONE, user.getConfirm(), "Confirm status of series wrong");
        assertNotNull(appointment.getChangeException(), "No change exceptions found");
        assertEquals(1, appointment.getChangeException().length, "Invalid number of change exceptions found");
        List<Appointment> changeExceptions = getManager().getChangeExceptions(appointment.getParentFolderID(), appointment.getObjectID(), Appointment.ALL_COLUMNS);
        assertNotNull(changeExceptions, "no change exceptions found");
        assertEquals(1, changeExceptions.size(), "Invalid number of change exceptions found");
        Appointment changeException = getManager().get(appointment.getParentFolderID(), changeExceptions.get(0).getObjectID());
        assertNotNull(changeException, "change exception not found");
        assertEquals(exceptionStartDate, changeException.getStartDate(), "Invalid start date of change exception");
        assertNotNull(changeException.getUsers(), "no users found in apointment");
        user = null;
        for (UserParticipant participant : changeException.getUsers()) {
            if (getClient().getValues().getUserId() == participant.getIdentifier()) {
                user = participant;
                break;
            }
        }
        assertNotNull(user, "User not found");
        assertEquals(Appointment.ACCEPT, user.getConfirm(), "Confirm status of change exception wrong");
        /*
         * verify appointment on client
         */
        eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        calendarData = super.calendarMultiget(eTags.keySet());
        iCalResource = assertContains(uid, calendarData);
        assertEquals(2, iCalResource.getVEvents().size(), "no exception found in iCal");
        Component seriesVEvent;
        Component exceptionVEvent;
        if (null == iCalResource.getVEvents().get(0).getProperty("RECURRENCE-ID")) {
            seriesVEvent = iCalResource.getVEvents().get(0);
            exceptionVEvent = iCalResource.getVEvents().get(1);
        } else {
            seriesVEvent = iCalResource.getVEvents().get(1);
            exceptionVEvent = iCalResource.getVEvents().get(0);
        }
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        attendee = seriesVEvent.getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in series iCal");
        assertTrue(null == attendee.getAttribute("PARTSTAT") || "NEEDS-ACTION".equals(attendee.getAttribute("PARTSTAT")), "PARTSTAT of series wrong");
        attendee = exceptionVEvent.getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in exception iCal");
        assertEquals("ACCEPTED", attendee.getAttribute("PARTSTAT"), "PARTSTAT wrong");
    }

}
