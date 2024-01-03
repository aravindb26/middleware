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

package com.openexchange.ajax.chronos.itip;

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.parseICalAttachment;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.MailApi;

/**
 * {@link SentByTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class SentByTest extends AbstractITipTest {

    private TimeZone timeZone;

    private SessionAwareClient apiClientUserA;
    private EventManager eventManagerUserA;
    private Integer idUserA;
    private String emailUserA;

    private SessionAwareClient apiClientUserB;
    private Integer idUserB;
    private String emailUserB;
    private String calendarFolderIdUserB;

    private SessionAwareClient apiClientUserE;
    private String emailUserE;

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).build();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        /*
         * initializations
         */
        timeZone = TimeZone.getTimeZone("Europe/Berlin");
        apiClientUserA = getApiClient();
        eventManagerUserA = eventManager;
        idUserA = apiClientUserA.getUserId();
        UserResponse userResponseUserA = new com.openexchange.testing.httpclient.modules.UserApi(apiClientUserA).getUser(idUserA.toString());
        emailUserA = userResponseUserA.getData().getEmail1();
        apiClientUserB = testUser2.getApiClient();
        calendarFolderIdUserB = getDefaultFolder(apiClientUserB);
        idUserB = apiClientUserB.getUserId();
        UserResponse userResponseUserB = new com.openexchange.testing.httpclient.modules.UserApi(apiClientUserB).getUser(idUserB.toString());
        emailUserB = userResponseUserB.getData().getEmail1();
        apiClientUserE = testUserC2.getApiClient();
        UserResponse userResponseUserE = new com.openexchange.testing.httpclient.modules.UserApi(apiClientUserE).getUser(apiClientUserE.getUserId().toString());
        emailUserE = userResponseUserE.getData().getEmail1();
        /*
         * in context 1, share user B's calendar to user A with "author" permissions
         */
        UserApi userApiUserB = new UserApi(apiClientUserB, testUser2);
        CalendarFolderManager folderManagerUserB = new CalendarFolderManager(userApiUserB, userApiUserB.getFoldersApi());
        FolderData folderData = new FolderData();
        folderData.setId(getDefaultFolder(apiClientUserB));
        FolderPermission adminPermission = new FolderPermission();
        adminPermission.setBits(I(403710016));
        adminPermission.setEntity(apiClientUserB.getUserId());
        adminPermission.setGroup(Boolean.FALSE);
        FolderPermission secretaryPermission = new FolderPermission();
        secretaryPermission.setBits(I(4227332));
        secretaryPermission.setEntity(apiClientUserA.getUserId());
        secretaryPermission.setGroup(Boolean.FALSE);
        folderData.setPermissions(Arrays.asList(adminPermission, secretaryPermission));
        folderManagerUserB.updateFolder(folderData);
    }

    private EventData createInitialEvent() throws Exception {
        /*
         * in context 1 as user A, create a new appointment in user B's calendar & invite user E from context 2
         */
        String uid = UUID.randomUUID().toString();
        String summary = UUID.randomUUID().toString();
        Date start = com.openexchange.time.TimeTools.D("next monday afternoon", timeZone);
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
        EventData eventData = new EventData();
        eventData.setFolder(calendarFolderIdUserB);
        eventData.setUid(uid);
        eventData.setSummary(summary);
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), start.getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), end.getTime()));
        eventData.setAttendees(Arrays.asList(AttendeeFactory.createIndividual(idUserB), AttendeeFactory.createIndividual(emailUserE)));
        EventData createdEvent = eventManagerUserA.createEvent(eventData, true);
        /*
         * as user A, verify user B was inserted as organizer & attendee, w/o sent-by being set
         */
        assertNotNull(createdEvent.getOrganizer());
        assertEquals(idUserB, createdEvent.getOrganizer().getEntity());
        assertEquals(emailUserB, CalendarUtils.extractEMailAddress(createdEvent.getOrganizer().getUri()));
        assertNull(createdEvent.getOrganizer().getSentBy());
        assertNotNull(createdEvent.getAttendees());
        assertEquals(2, createdEvent.getAttendees().size());
        Attendee attendeeUserB = null;
        Attendee attendeeUserE = null;
        for (Attendee attendee : createdEvent.getAttendees()) {
            if (idUserB.equals(attendee.getEntity())) {
                attendeeUserB = attendee;
            } else if (emailUserE.equals(CalendarUtils.extractEMailAddress(attendee.getUri()))) {
                attendeeUserE = attendee;
            }
        }
        assertNotNull(attendeeUserE);
        assertNotNull(attendeeUserB);
        /*
         * as user B, receive & verify the notification mail, expecting "From" and "Sender" to be set appropriately
         */
        MailData notificationMail = ITipUtil.receiveNotification(apiClientUserB, emailUserB, summary);
        assertNotNull(notificationMail);
        assertEquals(emailUserB, extractFromAddress(notificationMail));
        assertEquals(emailUserA, extractSenderAddress(notificationMail));
        deleteMail(new MailApi(apiClientUserB), notificationMail);
        /*
         * as external user E, receive & verify the iMIP mail, expecting "From" and "Sender" to be set appropriately
         */
        MailData schedulingMail = ITipUtil.receiveIMip(apiClientUserE, emailUserB, summary, -1, uid, null, SchedulingMethod.REQUEST);
        assertNotNull(schedulingMail);
        assertEquals(emailUserB, extractFromAddress(schedulingMail));
        assertEquals(emailUserA, extractSenderAddress(schedulingMail));
        /*
         * parse & check SENT-BY in iCal attachment
         */
        ImportedCalendar iTipRequest = parseICalAttachment(apiClientUserE, schedulingMail);
        assertTrue(null != iTipRequest.getEvents() && 1 == iTipRequest.getEvents().size());
        Event event = iTipRequest.getEvents().get(0);
        assertNotNull(event.getOrganizer());
        assertEquals(emailUserB, CalendarUtils.extractEMailAddress(event.getOrganizer().getUri()));
        assertNotNull(event.getOrganizer().getSentBy());
        assertEquals(emailUserA, CalendarUtils.extractEMailAddress(event.getOrganizer().getSentBy().getUri()));
        /*
         * import to calendar & check organizer in event data
         */
        EventData importedEvent = assertSingleEvent(applyCreate(apiClientUserE, constructBody(schedulingMail)), event.getUid());
        assertEquals(emailUserB, CalendarUtils.extractEMailAddress(importedEvent.getOrganizer().getUri()));
        assertNull(importedEvent.getOrganizer().getSentBy());
        /*
         * cleanup & return initially created event for further tests
         */
        deleteMail(new MailApi(apiClientUserE), schedulingMail);
        return createdEvent;
    }

    @Test
    public void testReplyAsSecretary() throws Exception {
        /*
         * create initial appointment (as user A, create a new appointment in user B's calendar with external user E)
         */
        EventData eventData = createInitialEvent();
        /*
         * as user A, set user B's participation status to tentative
         */
        Attendee attendee = AttendeeFactory.createIndividual(idUserB);
        attendee.setPartStat("TENTATIVE");
        AttendeeAndAlarm attendeeAndAlarm = new AttendeeAndAlarm();
        attendeeAndAlarm.setAttendee(attendee);
        eventManager.updateAttendee(eventData.getId(), null, calendarFolderIdUserB, attendeeAndAlarm, false);
        /*
         * as user B, receive & verify the notification mail, expecting "From" to be the user's mail address
         */
        MailData notification = ITipUtil.receiveNotification(apiClientUserB, emailUserB, eventData.getSummary());
        assertNotNull(notification);
        assertEquals(emailUserB, extractFromAddress(notification));
        assertEquals(emailUserA, extractSenderAddress(notification));
    }

    @Test
    public void testUpdateAsSecretary() throws Exception {
        /*
         * create initial appointment (as user A, create a new appointment in user B's calendar with external user E)
         */
        EventData eventData = createInitialEvent();
        /*
         * as user A, re-schedule the appointment in user B's calendar
         */
        EventData eventUpdate = new EventData();
        eventUpdate.setId(eventData.getId());
        eventUpdate.setFolder(eventData.getFolder());
        eventUpdate.setStartDate(DateTimeUtil.incrementDateTimeData(eventData.getStartDate(), TimeUnit.MINUTES.toMillis(30L)));
        eventUpdate.setEndDate(DateTimeUtil.incrementDateTimeData(eventData.getEndDate(), TimeUnit.MINUTES.toMillis(30L)));
        eventUpdate.setAttendees(null); // explicitly set to null to not sent an empty array during update
        EventData updatedEvent = eventManagerUserA.updateEvent(eventUpdate, false, false);
        /*
         * as user A, verify user B is still the organizer
         */
        assertNotNull(updatedEvent.getOrganizer());
        assertEquals(idUserB, updatedEvent.getOrganizer().getEntity());
        assertEquals(emailUserB, CalendarUtils.extractEMailAddress(updatedEvent.getOrganizer().getUri()));
        /*
         * as user B, receive & verify the notification mail, expecting "From" to be the user's mail address
         */
        MailData notification = ITipUtil.receiveNotification(apiClientUserB, emailUserB, eventData.getSummary());
        assertNotNull(notification);
        assertEquals(emailUserB, extractFromAddress(notification));
        assertEquals(emailUserA, extractSenderAddress(notification));
        /*
         * as external user E, receive & verify the iMIP mail, expecting "From" and "Sender" to be set appropriately
         */
        MailData schedulingMail = ITipUtil.receiveIMip(apiClientUserE, emailUserB, updatedEvent.getSummary(), i(updatedEvent.getSequence()), updatedEvent.getUid(), null, SchedulingMethod.REQUEST);
        assertNotNull(schedulingMail);
        assertEquals(emailUserB, extractFromAddress(schedulingMail));
        assertEquals(emailUserA, extractSenderAddress(schedulingMail));
        /*
         * parse & check SENT-BY in iCal attachment
         */
        ImportedCalendar iTipRequest = parseICalAttachment(apiClientUserE, schedulingMail);
        assertTrue(null != iTipRequest.getEvents() && 1 == iTipRequest.getEvents().size());
        Event event = iTipRequest.getEvents().get(0);
        assertNotNull(event.getOrganizer());
        assertEquals(emailUserB, CalendarUtils.extractEMailAddress(event.getOrganizer().getUri()));
        assertNotNull(event.getOrganizer().getSentBy());
        assertEquals(emailUserA, CalendarUtils.extractEMailAddress(event.getOrganizer().getSentBy().getUri()));
        /*
         * import to calendar & check organizer in event data
         */
        EventData importedEvent = assertSingleEvent(applyCreate(apiClientUserE, constructBody(schedulingMail)), event.getUid());
        assertEquals(emailUserB, CalendarUtils.extractEMailAddress(importedEvent.getOrganizer().getUri()));
        assertNull(importedEvent.getOrganizer().getSentBy());
    }

    @Test
    public void testDeleteAsSecretary() throws Exception {
        /*
         * create initial appointment (as user A, create a new appointment in user B's calendar with external user E)
         */
        EventData eventData = createInitialEvent();
        /*
         * as user A, delete the appointment from user B's calendar
         */
        EventId eventId = new EventId();
        eventId.setId(eventData.getId());
        eventId.setFolder(eventData.getFolder());
        eventManagerUserA.deleteEvent(eventId);
        /*
         * as user B, receive & verify the notification mail, expecting "From" to be the user's mail address
         */
        MailData notification = ITipUtil.receiveNotification(apiClientUserB, emailUserB, eventData.getSummary());
        assertNotNull(notification);
        assertEquals(emailUserB, extractFromAddress(notification));
        assertEquals(emailUserA, extractSenderAddress(notification));
        /*
         * as external user E, receive & verify the iMIP mail, expecting "From" and "Sender" to be set appropriately
         */
        MailData schedulingMail = ITipUtil.receiveIMip(apiClientUserE, emailUserB, eventData.getSummary(), -1, eventData.getUid(), null, SchedulingMethod.CANCEL);
        assertNotNull(schedulingMail);
        assertEquals(emailUserB, extractFromAddress(schedulingMail));
        assertEquals(emailUserA, extractSenderAddress(schedulingMail));
        /*
         * parse & check SENT-BY in iCal attachment
         */
        ImportedCalendar iTipRequest = parseICalAttachment(apiClientUserE, schedulingMail);
        assertTrue(null != iTipRequest.getEvents() && 1 == iTipRequest.getEvents().size());
        Event event = iTipRequest.getEvents().get(0);
        assertNotNull(event.getOrganizer());
        assertEquals(emailUserB, CalendarUtils.extractEMailAddress(event.getOrganizer().getUri()));
        assertNotNull(event.getOrganizer().getSentBy());
        assertEquals(emailUserA, CalendarUtils.extractEMailAddress(event.getOrganizer().getSentBy().getUri()));
    }

    private static String extractFromAddress(MailData mailData) {
        List<List<String>> addresses = mailData.getFrom();
        if (null != addresses) {
            for (List<String> address : addresses) {
                if (2 == address.size()) {
                    return address.get(1);
                }
            }
        }
        return null;
    }

    private static String extractSenderAddress(MailData mailData) {
        List<List<String>> addresses = mailData.getSender();
        if (null != addresses) {
            for (List<String> address : addresses) {
                if (2 == address.size()) {
                    return address.get(1);
                }
            }
        }
        return null;
    }

}
