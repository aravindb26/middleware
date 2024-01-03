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

import static com.openexchange.ajax.chronos.factory.PartStat.ACCEPTED;
import static com.openexchange.ajax.chronos.factory.PartStat.DECLINED;
import static com.openexchange.ajax.chronos.factory.PartStat.NEEDS_ACTION;
import static com.openexchange.ajax.chronos.factory.PartStat.TENTATIVE;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.AttendeePrivilegesTest;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.ChronosUtils;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.ical.ImportedCalendar;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.group.GroupStorage;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.AnalyzeResponse;
import com.openexchange.testing.httpclient.models.Annotations;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.ConversionDataSource;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.models.MailsCleanUpResponse;
import com.openexchange.testing.httpclient.models.ResourceData;
import com.openexchange.testing.httpclient.models.ResourcePermission;
import com.openexchange.testing.httpclient.models.ResourcePermission.PrivilegeEnum;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.ResourcesApi;
import com.openexchange.time.TimeTools;

/**
 * {@link BookingDelegatesTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class BookingDelegatesTest extends AbstractITipAnalyzeTest {

    private Integer resourceId;
    private UserApi userApi2;
    private ResourceData resourceData;

    @Override
    public TestClassConfig getTestConfig() {
        UserModuleAccess moduleAccess = new UserModuleAccess();
        moduleAccess.enableAll();
        moduleAccess.setEditResource(Boolean.TRUE);
        return TestClassConfig.builder().withContextConfig(TestContextConfig.builder().withUserModuleAccess(moduleAccess).build()).withUserPerContext(2).build();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
        userApi2 = new UserApi(testUser2.getApiClient(), testUser2);
        resourceId = testContext.acquireResource();
        setResourcePermissions(userApi2.getClient(), resourceId, //@formatter:off
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK),
            new ResourcePermission().entity(I(testUser2.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE));//@formatter:on
        resourceData = new ResourcesApi(userApi2.getClient()).getResource(resourceId).getData();
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return Collections.singletonMap("com.openexchange.calendar.useIMipForInternalUsers", Boolean.TRUE.toString());
    }

    @Override
    protected String getScope() {
        return "context";
    }

    private static void setResourcePermissions(ApiClient apiClient, Integer resourceId, ResourcePermission... permissions) throws Exception {
        ResourcesApi resourcesApi = new ResourcesApi(apiClient);
        CommonResponse response = resourcesApi.updateResourceBuilder().withId(resourceId).withTimestamp(L(2116800000000L)).withResourceData(new ResourceData().permissions(Arrays.asList(permissions))).execute();
        checkResponse(response);
    }

    @Test
    public void testBookingRequestWorkflowAccepted() throws Exception {
        testBookingRequestWorkflow(ACCEPTED);
    }

    @Test
    public void testBookingRequestWorkflowTentative() throws Exception {
        testBookingRequestWorkflow(TENTATIVE);
    }

    @Test
    public void testBookingRequestWorkflowDeclined() throws Exception {
        testBookingRequestWorkflow(DECLINED);
    }

    @Test
    public void testSubsequentUpdates() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * as booking delegate, check & analyze received notification mail
         */
        MailData notificationForBookingDelegate = assertNotificationForResource(testUser2.getApiClient(), resourceData, createdEvent.getSummary(), NEEDS_ACTION, ITipActionSet.ACTIONS);
        /*
         * re-schedule event with resource
         */
        EventData updatedEvent = rescheduleEvent(eventManager, createdEvent.getFolder(), createdEvent.getId());
        /*
         * as booking delegate, re-analyze previously received notification mail, expecting it to be outdated now
         */
        assertAnalyzeActions(analyzeNotification(testUser2.getApiClient(), notificationForBookingDelegate), ITipActionSet.IGNORE);
        cleanUpNotification(testUser2, notificationForBookingDelegate);
        /*
         * as booking delegate, check & analyze next received notification mail
         */
        notificationForBookingDelegate = assertNotificationForResource(testUser2.getApiClient(), resourceData, createdEvent.getSummary(), NEEDS_ACTION, ITipActionSet.ACTIONS);
        /*
         * delete the event with resource
         */
        deleteEvent(eventManager, updatedEvent.getFolder(), updatedEvent.getId());
        /*
         * as booking delegate, re-analyze previously received notification mail, expecting it to be outdated now
         */
        assertAnalyzeActions(analyzeNotification(testUser2.getApiClient(), notificationForBookingDelegate), ITipActionSet.IGNORE);
        cleanUpNotification(testUser2, notificationForBookingDelegate);
        /*
         * as booking delegate, check & analyze next received notification mail
         */
        assertNotificationForResource(testUser2.getApiClient(), resourceData, createdEvent.getSummary(), null, ITipActionSet.EMPTY);
    }

    @Test
    public void testSubsequentReplies() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        String summary = createdEvent.getSummary();
        /*
         * as booking delegate, check & analyze received notification mail, then reply with ACCEPTED
         */
        MailData notificationForBookingDelegate = assertNotificationForResource(testUser2.getApiClient(), resourceData, summary, NEEDS_ACTION, ITipActionSet.ACTIONS);
        assertSingleEvent(new ChronosApi(testUser2.getApiClient()).accept(constructNotificationBody(notificationForBookingDelegate), "comment", null));
        /*
         * receive & check notification
         */
        MailData notificationForOrganizer = assertNotificationForResource(testUser.getApiClient(), resourceData, summary, ACCEPTED, ITipActionSet.EMPTY);
        /*
         * as booking delegate, now reply with TENTATIVE
         */
        assertSingleEvent(new ChronosApi(testUser2.getApiClient()).tentative(constructNotificationBody(notificationForBookingDelegate), "comment", null));
        /*
         * re-analyze previously received notification mail, expecting it to be outdated now
         */
        assertAnalyzeActions(analyzeNotification(testUser.getApiClient(), notificationForOrganizer), ITipActionSet.IGNORE);
        cleanUpNotification(testUser, notificationForOrganizer);
        /*
         * now check & analyze next received notification mail
         */
        notificationForOrganizer = assertNotificationForResource(testUser.getApiClient(), resourceData, summary, TENTATIVE, ITipActionSet.EMPTY);
        /*
         * re-schedule event with resource
         */
        rescheduleEvent(eventManager, createdEvent.getFolder(), createdEvent.getId());
        /*
         * re-analyze previously received notification mail, expecting it to be outdated now
         */
        assertAnalyzeActions(analyzeNotification(testUser.getApiClient(), notificationForOrganizer), ITipActionSet.IGNORE);
        /*
         * as booking delegate, re-analyze previously received notification mail, expecting it to be outdated now
         */
        assertAnalyzeActions(analyzeNotification(testUser2.getApiClient(), notificationForBookingDelegate), ITipActionSet.IGNORE);
        cleanUpNotification(testUser2, notificationForBookingDelegate);
        /*
         * as booking delegate, check & analyze next received notification mail, reply with DECLINED
         */
        notificationForBookingDelegate = assertNotificationForResource(testUser2.getApiClient(), resourceData, summary, NEEDS_ACTION, ITipActionSet.ACTIONS);
        assertSingleEvent(new ChronosApi(testUser2.getApiClient()).decline(constructNotificationBody(notificationForBookingDelegate), "comment", null));
        /*
         * re-analyze previously received notification mail, expecting it to be still outdated
         */
        assertAnalyzeActions(analyzeNotification(testUser.getApiClient(), notificationForOrganizer), ITipActionSet.IGNORE);
        cleanUpNotification(testUser, notificationForOrganizer);
        /*
         * now check & analyze next received notification mail
         */
        notificationForOrganizer = assertNotificationForResource(testUser.getApiClient(), resourceData, summary, DECLINED, ITipActionSet.EMPTY);
        /*
         * delete the event with resource
         */
        deleteEvent(eventManager, createdEvent.getFolder(), createdEvent.getId());
        /*
         * as booking delegate, re-analyze previously received notification mail, expecting it to be outdated now
         */
        assertAnalyzeActions(analyzeNotification(testUser2.getApiClient(), notificationForBookingDelegate), ITipActionSet.IGNORE);
        cleanUpNotification(testUser2, notificationForBookingDelegate);
        /*
         * as booking delegate, check & analyze next received notification mail
         */
        assertNotificationForResource(testUser2.getApiClient(), resourceData, summary, null, ITipActionSet.EMPTY);
    }

    @Test
    public void testAnnotationForBookingDelegate() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        String summary = createdEvent.getSummary();
        /*
         * as booking delegate, receive notification mail
         */
        MailData notificationForBookingDelegate = receiveNotification(testUser2.getApiClient(), resourceData.getMailaddress(), summary);
        assertNotNull(notificationForBookingDelegate);
        /*
         * check relevant mail headers explicitly
         */
        String calendarUserEmail = userResponseC1.getData().getEmail1();
        String resourceEmail = resourceData.getMailaddress();
        assertTrue(String.valueOf(notificationForBookingDelegate.getSender()).contains(calendarUserEmail));
        assertTrue(String.valueOf(notificationForBookingDelegate.getFrom()).contains(resourceEmail));
        /*
         * check attendee/organizer in iCal REQUEST explicitly
         */
        ImportedCalendar calendar = ITipUtil.parseICalAttachment(testUser2.getApiClient(), notificationForBookingDelegate, SchedulingMethod.REQUEST);
        assertTrue(null != calendar && null != calendar.getEvents() && 1 == calendar.getEvents().size());
        com.openexchange.chronos.Event event = calendar.getEvents().get(0);
        assertEquals(CalendarUtils.getURI(calendarUserEmail), event.getOrganizer().getUri());
        assertNull(event.getOrganizer().getSentBy());
        com.openexchange.chronos.Attendee resourceAttendee = CalendarUtils.find(event.getAttendees(), CalendarUtils.getURI(resourceEmail));
        assertNotNull(resourceAttendee);
        assertEquals(CalendarUtils.getURI(resourceEmail), resourceAttendee.getUri());
        assertNotNull(resourceAttendee.getSentBy());
        assertEquals(CalendarUtils.getURI(calendarUserEmail), resourceAttendee.getSentBy().getUri());
        /*
         * check generated annotation strings
         */
        AnalyzeResponse analyzeResponse = analyzeNotification(testUser2.getApiClient(), notificationForBookingDelegate);
        assertTrue(null != analyzeResponse.getData() && 1 == analyzeResponse.getData().size());
        List<Annotations> annotations = analyzeResponse.getData().get(0).getAnnotations();
        assertTrue(String.valueOf(annotations).contains("added the resource " + resourceData.getDisplayName()));
        assertFalse(String.valueOf(annotations).contains("n behalf of "));
        assertTrue(String.valueOf(annotations).contains("pending your approval"));
    }

    @Test
    public void testAnnotationForBookingDelegateOnBehalf() throws Exception {
        /*
         * prepare another test user in context
         */
        TestUser testUser3 = testContext.acquireUser();
        UserApi userApi3 = new UserApi(testUser3.getApiClient(), testUser3);
        EventManager eventManager3 = new EventManager(userApi3, getDefaultFolder(testUser3.getApiClient()));
        /*
         * generate event in user's personal folder with other attendee w/ modify privileges & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getUserAttendee(testUser3));
        eventData.setAttendeePrivileges(AttendeePrivilegesTest.Privileges.MODIFY.name());
        EventData createdEvent = eventManager.createEvent(eventData, true);
        String summary = createdEvent.getSummary();
        /*
         * as other user, edit this event & add resource attendee to it
         */
        EventData eventUpdate = new EventData();
        eventUpdate.setId(createdEvent.getId());
        eventUpdate.setFolder(getDefaultFolder(testUser3.getApiClient()));
        eventManager3.setLastTimeStamp(eventManager.getLastTimeStamp());
        ArrayList<Attendee> updatedAttendees = new ArrayList<Attendee>(createdEvent.getAttendees());
        updatedAttendees.add(getResourceAttendee(resourceId));
        eventUpdate.setAttendees(updatedAttendees);
        eventManager3.updateEvent(eventUpdate);
        /*
         * as booking delegate, receive notification mail
         */
        MailData notificationForBookingDelegate = receiveNotification(testUser2.getApiClient(), resourceData.getMailaddress(), summary);
        assertNotNull(notificationForBookingDelegate);
        /*
         * check relevant mail headers explicitly
         */
        String onBehalfUserEmail = new com.openexchange.testing.httpclient.modules.UserApi(testUser3.getApiClient())
            .getUser(String.valueOf(testUser3.getUserId())).getData().getEmail1();
        String calendarUserEmail = userResponseC1.getData().getEmail1();
        String resourceEmail = resourceData.getMailaddress();
        assertTrue(String.valueOf(notificationForBookingDelegate.getSender()).contains(onBehalfUserEmail));
        assertTrue(String.valueOf(notificationForBookingDelegate.getFrom()).contains(resourceEmail));
        /*
         * check attendee/organizer in iCal REQUEST explicitly
         */
        ImportedCalendar calendar = ITipUtil.parseICalAttachment(testUser2.getApiClient(), notificationForBookingDelegate, SchedulingMethod.REQUEST);
        assertTrue(null != calendar && null != calendar.getEvents() && 1 == calendar.getEvents().size());
        com.openexchange.chronos.Event event = calendar.getEvents().get(0);
        assertEquals(CalendarUtils.getURI(calendarUserEmail), event.getOrganizer().getUri());
        assertNotNull(event.getOrganizer().getSentBy());
        assertEquals(CalendarUtils.getURI(onBehalfUserEmail), event.getOrganizer().getSentBy().getUri());
        com.openexchange.chronos.Attendee resourceAttendee = CalendarUtils.find(event.getAttendees(), CalendarUtils.getURI(resourceEmail));
        assertNotNull(resourceAttendee);
        assertEquals(CalendarUtils.getURI(resourceEmail), resourceAttendee.getUri());
        assertNotNull(resourceAttendee.getSentBy());
        assertEquals(CalendarUtils.getURI(calendarUserEmail), resourceAttendee.getSentBy().getUri());
        /*
         * check generated annotation strings
         */
        AnalyzeResponse analyzeResponse = analyzeNotification(testUser2.getApiClient(), notificationForBookingDelegate);
        assertTrue(null != analyzeResponse.getData() && 1 == analyzeResponse.getData().size());
        List<Annotations> annotations = analyzeResponse.getData().get(0).getAnnotations();
        assertTrue(String.valueOf(annotations).contains("added the resource " + resourceData.getDisplayName()));
        assertTrue(String.valueOf(annotations).contains("n behalf of "));
        assertTrue(String.valueOf(annotations).contains("pending your approval"));
    }

    private static EventData rescheduleEvent(EventManager eventManager, String folderId, String id) throws Exception {
        EventData event = eventManager.getEvent(folderId, id);
        EventData eventUpdate = new EventData().folder(event.getFolder()).id(event.getId())
            .startDate(DateTimeUtil.getDateTime(event.getStartDate().getTzid(), CalendarUtils.add(DateTimeUtil.parseDateTime(event.getStartDate()), Calendar.DATE, 1).getTime()))
            .endDate(DateTimeUtil.getDateTime(event.getEndDate().getTzid(), CalendarUtils.add(DateTimeUtil.parseDateTime(event.getEndDate()), Calendar.DATE, 1).getTime()))
        ;
        return eventManager.updateEvent(eventUpdate);
    }

    private static void deleteEvent(EventManager eventManager, String folderId, String id) throws Exception {
        EventData event = eventManager.getEvent(folderId, id);
        eventManager.deleteEvent(event, event.getFolder());
    }

    private void testBookingRequestWorkflow(PartStat resourcePartStatToSet) throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * let booking delegate "accept" the booking request via notification mails
         */
        doResourceBookingWorkflow(testUser, testUser2, resourceData, createdEvent, resourcePartStatToSet);
        /*
         * re-schedule event with resource
         */
        EventData updatedEvent = rescheduleEvent(eventManager, createdEvent.getFolder(), createdEvent.getId());
        /*
         * let booking delegate "accept" the booking request again via notification mails
         */
        doResourceBookingWorkflow(testUser, testUser2, resourceData, updatedEvent, resourcePartStatToSet);
        /*
         * delete the event again
         */
        deleteEvent(eventManager, createdEvent.getFolder(), createdEvent.getId());
        assertNotificationForResource(testUser2.getApiClient(), resourceData, updatedEvent.getSummary(), null, ITipActionSet.EMPTY);
    }

    /**
     * Follows the booking workflow for a managed resource in an appointment between organizer and booking delegate:
     * <ul>
     * <li>the booking delegate receives the notification mail representing the resource booking request</li>
     * <li>the booking delegate accepts/declines it by setting the corresponding participation status of the resource</li>
     * <li>the organizer receives a corresponding reply</li>
     * </ul>
     */
    private static void doResourceBookingWorkflow(TestUser organizer, TestUser bookingDelegate, ResourceData resourceData, EventData bookingRequestEventData, PartStat resourcePartStatToSet) throws Exception {
        /*
         * as organizer, get event & check participation status of resource
         */
        EventResponse eventResponse = new ChronosApi(organizer.getApiClient()).getEventBuilder()
            .withId(bookingRequestEventData.getId()).withFolder(bookingRequestEventData.getFolder()).execute();
        EventData reloadedEvent = checkResponse(eventResponse.getError(), eventResponse.getErrorDesc(), eventResponse.getData());
        assertPartStat(reloadedEvent.getAttendees(), resourceData.getId(), NEEDS_ACTION);
        /*
         * as booking delegate, do the same in resource folder
         */
        eventResponse = new ChronosApi(bookingDelegate.getApiClient()).getEventBuilder()
            .withId(bookingRequestEventData.getId()).withFolder("cal://0/resource" + resourceData.getId()).execute();
        reloadedEvent = checkResponse(eventResponse.getError(), eventResponse.getErrorDesc(), eventResponse.getData());
        assertPartStat(reloadedEvent.getAttendees(), resourceData.getId(), NEEDS_ACTION);
        /*
         * as booking delegate, check & analyze received notification mail
         */
        MailData notificationMailData = assertNotificationForResource(bookingDelegate.getApiClient(), resourceData, bookingRequestEventData.getSummary(), NEEDS_ACTION, ITipActionSet.ACTIONS);
        /*
         * as booking delegate, accept/decline on behalf of resource attendee
         */
        EventData updatedEvent;
        switch (resourcePartStatToSet) {
            case ACCEPTED:
                updatedEvent = assertSingleEvent(new ChronosApi(bookingDelegate.getApiClient()).accept(constructNotificationBody(notificationMailData), "comment", null));
                break;
            case DECLINED:
                updatedEvent = assertSingleEvent(new ChronosApi(bookingDelegate.getApiClient()).decline(constructNotificationBody(notificationMailData), "comment", null));
                break;
            case TENTATIVE:
                updatedEvent = assertSingleEvent(new ChronosApi(bookingDelegate.getApiClient()).tentative(constructNotificationBody(notificationMailData), "comment", null));
                break;
            default:
                throw new IllegalArgumentException(resourcePartStatToSet.getStatus());
        }
        assertAttendeePartStat(updatedEvent.getAttendees(), resourceData.getMailaddress(), resourcePartStatToSet);
        cleanUpNotification(bookingDelegate, notificationMailData);
        /*
         * as organizer, reload updated event & check participation status of resource
         */
        eventResponse = new ChronosApi(organizer.getApiClient()).getEventBuilder()
            .withId(bookingRequestEventData.getId()).withFolder(bookingRequestEventData.getFolder()).execute();
        reloadedEvent = checkResponse(eventResponse.getError(), eventResponse.getErrorDesc(), eventResponse.getData());
        assertPartStat(reloadedEvent.getAttendees(), resourceData.getId(), resourcePartStatToSet);
        /*
         * as booking delegate, do the same in resource folder
         */
        eventResponse = new ChronosApi(bookingDelegate.getApiClient()).getEventBuilder()
            .withId(bookingRequestEventData.getId()).withFolder("cal://0/resource" + resourceData.getId()).execute();
        reloadedEvent = checkResponse(eventResponse.getError(), eventResponse.getErrorDesc(), eventResponse.getData());
        assertPartStat(reloadedEvent.getAttendees(), resourceData.getId(), resourcePartStatToSet);
        /*
         * as organizer, receive & check notification
         */
        notificationMailData = assertNotificationForResource(organizer.getApiClient(), resourceData, bookingRequestEventData.getSummary(), resourcePartStatToSet, ITipActionSet.EMPTY);
        cleanUpNotification(organizer, notificationMailData);
    }

    private static MailData assertNotificationForResource(SessionAwareClient apiClient, ResourceData resourceData, String subjectToMatch, PartStat expectedResourcePartStat, ITipActionSet expectedActionSet) throws Exception {
        MailData notificationMailData = receiveNotification(apiClient, resourceData.getMailaddress(), subjectToMatch);
        assertNotNull(notificationMailData);
        AnalyzeResponse analyzeResponse = analyzeNotification(apiClient, notificationMailData);
        if (null != expectedResourcePartStat) {
            AnalysisChangeNewEvent newEvent = assertSingleChange(analyzeResponse).getNewEvent();
            assertNotNull(newEvent);
            assertAttendeePartStat(newEvent.getAttendees(), resourceData.getMailaddress(), expectedResourcePartStat);
        }
        assertTrue(null != analyzeResponse.getData() && 1 == analyzeResponse.getData().size());
        expectedActionSet.getConsumer().accept(analyzeResponse.getData().get(0));
        return notificationMailData;
    }

    private static AnalyzeResponse analyzeNotification(SessionAwareClient apiClient, MailData notificationMailData) throws ApiException {
        return new ChronosApi(apiClient).analyze(constructNotificationBody(notificationMailData), null);
    }

    private static ConversionDataSource constructNotificationBody(MailData notificationMailData) {
        return constructBody(notificationMailData.getId(), null, notificationMailData.getFolderId());
    }

    private static MailData receiveNotification(SessionAwareClient apiClient, String fromToMatch, String subjectToMatch) throws Exception {
        return ITipUtil.receiveIMip(apiClient, fromToMatch, subjectToMatch, -1, null);
    }

    private static Attendee assertPartStat(List<Attendee> actualAttendees, Integer entity, PartStat expectedPartStat) {
        Attendee matchingAttendee = ChronosUtils.find(actualAttendees, entity);
        expectedPartStat.assertStatus(matchingAttendee);
        return matchingAttendee;
    }

    private static EventData prepareEventData(String folderId, CalendarUser organizer, Attendee... attendees) {
        TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
        EventData eventData = new EventData();
        eventData.setFolder(folderId);
        eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        eventData.setLocation(UUIDs.getUnformattedStringFromRandom());
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), TimeTools.D("Next friday at 10 am", timeZone).getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), TimeTools.D("Next friday at 11 am", timeZone).getTime()));
        eventData.setOrganizer(organizer);
        if (null != attendees) {
            eventData.setAttendees(java.util.Arrays.asList(attendees));
        }
        return eventData;
    }

    private static MailData cleanUpNotification(TestUser user, MailData mail) throws ApiException {
        MailsCleanUpResponse response = new MailApi(user.getApiClient()).deleteMails(List.of(new MailListElement().id(mail.getId()).folder(mail.getFolderId())), null, Boolean.TRUE, Boolean.FALSE);
        checkResponse(response.getError(), response.getErrorDesc());
        return mail;
    }

}
