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

package com.openexchange.ajax.chronos;

import static com.openexchange.ajax.chronos.factory.PartStat.ACCEPTED;
import static com.openexchange.ajax.chronos.factory.PartStat.DECLINED;
import static com.openexchange.ajax.chronos.factory.PartStat.NEEDS_ACTION;
import static com.openexchange.ajax.chronos.factory.PartStat.TENTATIVE;
import static com.openexchange.ajax.chronos.itip.ITipUtil.assertContent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveNotification;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.ChronosApiException;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.ChronosUtils;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.ConfigAwareProvisioningService;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosConflictDataRaw;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.models.MailsCleanUpResponse;
import com.openexchange.testing.httpclient.models.ResourceData;
import com.openexchange.testing.httpclient.models.ResourcePermission;
import com.openexchange.testing.httpclient.models.ResourcePermission.PrivilegeEnum;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.ResourcesApi;
import com.openexchange.time.TimeTools;

/**
 * {@link ManagedResourcesTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class ManagedResourcesTest extends AbstractSecondUserChronosTest {

    private Integer resourceId;
    private String resourceFolderId;
    private TestUser testUser3;
    private String defaultFolderId3;
    private UserApi userApi3;
    private EventManager eventManager3;
    private TestUser testUser4;
    private String defaultFolderId4;
    private EventManager eventManager4;
    private TestUser resource;

    @Override
    public TestClassConfig getTestConfig() {
        UserModuleAccess moduleAccess = new UserModuleAccess();
        moduleAccess.enableAll();
        moduleAccess.setEditResource(Boolean.TRUE);
        return TestClassConfig.builder()
                              .withContextConfig(TestContextConfig.builder().withUserModuleAccess(moduleAccess).build())
                              .withUserPerContext(4)
                              .build();
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        return Map.of("com.openexchange.resource.simplePermissionMode", Boolean.FALSE.toString());
    }

    @Override
    protected String getScope() {
        return "context";
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
        testUser3 = testContext.acquireUser();
        userApi3 = new UserApi(testUser3.getApiClient(), testUser3);
        defaultFolderId3 = getDefaultFolder(testUser3.getApiClient());
        eventManager3 = new EventManager(userApi3, defaultFolderId3);
        testUser4 = testContext.acquireUser();
        UserApi userApi4 = new UserApi(testUser4.getApiClient(), testUser4);
        defaultFolderId4 = getDefaultFolder(testUser4.getApiClient());
        eventManager4 = new EventManager(userApi4, defaultFolderId4);
        resourceId = testContext.acquireResource();
        resourceFolderId = "cal://0/resource" + resourceId;
        setResourcePermissions(userApi2.getClient(),
                               resourceId, //@formatter:off
            new ResourcePermission().entity(I(testUser.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.ASK_TO_BOOK),
            new ResourcePermission().entity(I(testUser2.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(testUser3.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.BOOK_DIRECTLY));//@formatter:on
        resource = ConfigAwareProvisioningService.getService().getResource(testContext.getId(), i(resourceId));
    }

    private static void setResourcePermissions(ApiClient apiClient, Integer resourceId, ResourcePermission... permissions) throws Exception {
        ResourcesApi resourcesApi = new ResourcesApi(apiClient);
        CommonResponse response = resourcesApi.updateResourceBuilder()
                                              .withId(resourceId)
                                              .withTimestamp(L(2116800000000L))
                                              .withResourceData(new ResourceData().permissions(Arrays.asList(permissions)))
                                              .execute();
        checkResponse(response);
    }

    @Test
    public void testInitialPartstat() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * reload created event & check participation status of resource
         */
        EventData reloadedEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        /*
         * reload created event as other user (who is the booking delegate) in resource folder & check again
         */
        reloadedEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        assertNotificationMailForDelegate(reloadedEvent);
    }

    @Test
    public void testInitialPartstatAsDelegate() throws Exception {
        /*
         * generate event with resource in user b's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * reload created event & check participation status of resource
         */
        EventData reloadedEvent = eventManager2.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
        /*
         * reload created event as other user (who is attendee, too) in resource folder & check again
         */
        reloadedEvent = eventManager.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
    }

    @Test
    public void testInitialPartstatBookDirectly() throws Exception {
        /*
         * generate event with resource in user c's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId3, getCalendarUser(testUser3), getUserAttendee(testUser3), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager3.createEvent(eventData, true);
        /*
         * reload created event & check participation status of resource
         */
        EventData reloadedEvent = eventManager3.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
        /*
         * reload created event as other user (who is attendee, too) in resource folder & check again
         */
        reloadedEvent = eventManager.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
        assertNotificationMailForDelegate(reloadedEvent);
    }

    @Test
    public void testInitialPartstatBookDirectlyWithCalendarDelegate() throws Exception {
        /*
         * share user a's personal folder with user c, which can book directly
         */
        FolderData data = folderManager.getFolder(defaultFolderId);
        folderManager.shareFolder(data, testUser3);

        /*
         * generate event with resource in user a's personal folder & create it as user c
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getUserAttendee(testUser4), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager3.createEvent(eventData, true);
        /*
         * reload created event & check participation status of resource
         */
        EventData reloadedEvent = eventManager3.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
        /*
         * reload created event as other user (who is attendee, too) in resource folder & check again
         */
        reloadedEvent = eventManager.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
        MailData mail = assertNotificationMailForDelegate(reloadedEvent);
        assertContent(mail, "On behalf of", "Expected that user C has created event on behalf of user A");
        /*
         * receive notification as user a that user c created an event with the resource on its behalf
         */
        MailData invite = receiveNotification(testUser.getApiClient(), testUser.getLogin(), reloadedEvent.getSummary());
        assertTrue(null != invite.getSender() && invite.getSender().size() == 1 && invite.getSender().get(0).contains(testUser3.getLogin()));
        assertContent(invite, "on your behalf");
    }

    @Test
    public void testNoBookingPrivilege() throws Exception {
        /*
         * generate event with resource in user d's personal folder & try to create it
         */
        EventData eventData = prepareEventData(defaultFolderId4, getCalendarUser(testUser4), getUserAttendee(testUser4), getUserAttendee(testUser), getResourceAttendee(resourceId));
        String code = eventManager4.tryCreateEvent(eventData, true);
        assertEquals("CAL-40311", code);
    }

    @Test
    public void testNoBookingPrivilegeWithCalendarDelegate() throws Exception {
        /*
         * share user c's personal folder with user d
         */
        CalendarFolderManager folderManager3 = new CalendarFolderManager(userApi3, userApi3.getFoldersApi());
        FolderData data = folderManager3.getFolder(defaultFolderId3);
        folderManager3.shareFolder(data, testUser4);
        /*
         * generate event with resource in user c's personal folder & create it as user 4 which has no privileges on the resource
         */
        EventData eventData = prepareEventData(defaultFolderId3, getCalendarUser(testUser3), getUserAttendee(testUser3), getUserAttendee(testUser), getResourceAttendee(resourceId));
        String code = eventManager4.tryCreateEvent(eventData, true);
        assertEquals("CAL-40311", code);
    }

    @Test
    public void testChangePartstat() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * reload created event & check participation status of resource
         */
        EventData reloadedEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        /*
         * reload created event as other user (who is the booking delegate) in resource folder & check again
         */
        reloadedEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        assertNotificationMailForDelegate(reloadedEvent);
        /*
         * as booking delegate, change "PARTSTAT" to different values & check again
         */
        for (PartStat partStat : new PartStat[] { ACCEPTED, TENTATIVE, DECLINED, NEEDS_ACTION }) {
            setPartStat(eventManager2, resourceFolderId, createdEvent.getId(), resourceId, partStat);
            reloadedEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
            assertPartStat(reloadedEvent.getAttendees(), resourceId, partStat);
            reloadedEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
            assertPartStat(reloadedEvent.getAttendees(), resourceId, partStat);
            assertNotificationMailForOrganizer(reloadedEvent, partStat);
        }
        /*
         * as organizer (who is not a booking delegate), also try to change the participation status
         */
        for (PartStat partStat : new PartStat[] { ACCEPTED, TENTATIVE, DECLINED, NEEDS_ACTION }) {
            setPartStat(eventManager, resourceFolderId, createdEvent.getId(), resourceId, partStat, Optional.empty(), "CAL-4031");
        }

        /*
         * as organizer, delete the event. The booking delegate should be informed about this
         */
        eventManager.deleteEvent(reloadedEvent, null);
        assertNotificationMailForDelegate(reloadedEvent);
    }

    @Test
    public void testChangePartstatWithOtherDelegates() throws Exception {
        /*
         * Make two users an delegate
         */
        setResourcePermissions(userApi2.getClient(),
                               resourceId, //@formatter:off
                               new ResourcePermission().entity(I(testUser.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.BOOK_DIRECTLY),
                               new ResourcePermission().entity(I(testUser2.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
                               new ResourcePermission().entity(I(testUser3.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE));//@formatter:on
        /*
         * generate event as user with "book directly" privileges of resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * reload created event & check participation status of resource
         */
        EventData reloadedEvent = eventManager3.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
        reloadedEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED);
        /*
         * as booking delegates, who are both not the organizer, check for notification
         */
        assertNotificationMailForDelegate(testUser2, reloadedEvent.getSummary());
        assertNotificationMailForDelegate(testUser3, reloadedEvent.getSummary());
        /*
         * as other booking delegate, change "PARTSTAT" to different value & check again
         */
        setPartStat(eventManager2, resourceFolderId, createdEvent.getId(), resourceId, DECLINED);
        reloadedEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, DECLINED);
        /*
         * as first delegate, check for notification mail about changed status
         */
        assertNotificationMailForDelegate(testUser3, "for resource " + resource.getUser() + " " + DECLINED.getStatus().toLowerCase() + ": " + reloadedEvent.getSummary());
        /*
         * as organizer, check for notification mail
         */
        assertNotificationMailForOrganizer(reloadedEvent, DECLINED);
        /*
         * as organizer, delete the event. The booking delegates should be informed about this
         */
        createdEvent = eventManager.getEvent(null, createdEvent.getId());
        eventManager.deleteEvent(createdEvent, null);
        assertNotificationMailForDelegate(testUser2, reloadedEvent.getSummary());
        assertNotificationMailForDelegate(testUser3, reloadedEvent.getSummary());
    }

    @Test
    public void testChangePartstatWithComment() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * reload created event & check participation status of resource
         */
        EventData reloadedEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        /*
         * reload created event as other user (who is the booking delegate) in resource folder & check again
         */
        reloadedEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        /*
         * as booking delegate, change "PARTSTAT" to different value including a comment & check again
         */
        Optional<String> comment = Optional.of("okay");
        setPartStat(eventManager2, resourceFolderId, createdEvent.getId(), resourceId, ACCEPTED, comment);
        reloadedEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED, comment);
        reloadedEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, ACCEPTED, comment);
        assertNotificationMailForOrganizer(reloadedEvent, ACCEPTED);
    }

    @Test
    public void testConflict() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * decline booking request as other user (who is the booking delegate) in resource folder
         */
        assertNotificationMailForDelegate(createdEvent);
        createdEvent = eventManager2.getEvent(resourceFolderId, createdEvent.getId());
        setPartStat(eventManager2, resourceFolderId, createdEvent.getId(), resourceId, DECLINED);
        assertNotificationMailForOrganizer(createdEvent, DECLINED);
        /*
         * generate another event at the same time with resource in user's personal folder & create it
         */
        EventData eventData2 = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent2 = eventManager.createEvent(eventData2, true);
        assertNotificationMailForDelegate(createdEvent2);
        /*
         * now try to accept the resource in the initial event again, expecting a conflict
         */
        for (PartStat partStat : new PartStat[] { ACCEPTED, TENTATIVE, NEEDS_ACTION }) {
            CalendarResult calendarResult = setPartStat(eventManager2, resourceFolderId, createdEvent.getId(), resourceId, partStat);
            assertTrue(null == calendarResult.getUpdated() || calendarResult.getUpdated().isEmpty());
            assertConflict(calendarResult.getConflicts(), resourceId);
        }
        /*
         * decline the booking request of the second event as booking delegate
         */
        createdEvent2 = eventManager2.getEvent(resourceFolderId, createdEvent2.getId());
        setPartStat(eventManager2, resourceFolderId, createdEvent2.getId(), resourceId, DECLINED);
        assertNotificationMailForOrganizer(createdEvent2, DECLINED);
        /*
         * now try to accept the resource in the initial event again, expecting no errors
         */
        for (PartStat partStat : new PartStat[] { ACCEPTED, TENTATIVE, NEEDS_ACTION }) {
            setPartStat(eventManager2, resourceFolderId, createdEvent.getId(), resourceId, partStat);
            assertNotificationMailForOrganizer(createdEvent, partStat);
        }
    }

    @Test
    public void testInitialPartstatNewlyAdded() throws Exception {
        /*
         * generate event in user's personal folder (w/o resource yet) & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getUserAttendee(testUser3));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * reload created event, re-schedule it and add a resource attendee
         */
        EventData reloadedEvent = eventManager.getEvent(createdEvent.getFolder(), createdEvent.getId());
        List<Attendee> attendeeUpdates = new ArrayList<>(createdEvent.getAttendees());
        attendeeUpdates.add(getResourceAttendee(resourceId));
        EventData eventUpdate = new EventData().id(createdEvent.getId())
                                               .folder(createdEvent.getFolder())
                                               .startDate(DateTimeUtil.incrementDateTimeData(createdEvent.getStartDate(), TimeUnit.HOURS.toMillis(1L)))
                                               .endDate(DateTimeUtil.incrementDateTimeData(createdEvent.getEndDate(), TimeUnit.HOURS.toMillis(1L)))
                                               .attendees(attendeeUpdates);
        EventData updatedEvent = eventManager.updateEvent(eventUpdate);
        /*
         * reload created event & check participation status of resource
         */
        reloadedEvent = eventManager.getEvent(updatedEvent.getFolder(), updatedEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        /*
         * reload created event as other user (who is the booking delegate) in resource folder & check again
         */
        reloadedEvent = eventManager2.getEvent(resourceFolderId, updatedEvent.getId());
        assertPartStat(reloadedEvent.getAttendees(), resourceId, NEEDS_ACTION);
        assertNotificationMailForDelegate(reloadedEvent);
    }

    private static ChronosConflictDataRaw assertConflict(List<ChronosConflictDataRaw> conflicts, Integer entity) {
        assertNotNull(conflicts);
        for (ChronosConflictDataRaw conflictData : conflicts) {
            Attendee attendee = ChronosUtils.find(conflictData.getConflictingAttendees(), entity);
            if (null != attendee) {
                return conflictData;
            }
        }
        fail("no conflict data found for " + entity);
        return null;
    }

    private static CalendarResult setPartStat(EventManager eventManager, String folderId, String id, Integer entity, PartStat partStat) throws Exception {
        return setPartStat(eventManager, folderId, id, entity, partStat, Optional.empty());
    }

    private static CalendarResult setPartStat(EventManager eventManager, String folderId, String id, Integer entity, PartStat partStat, Optional<String> comment) throws Exception {
        return setPartStat(eventManager, folderId, id, entity, partStat, comment, null);
    }

    private static CalendarResult setPartStat(EventManager eventManager, String folderId, String id, Integer entity, PartStat partStat, Optional<String> comment, String expectedErrorCode) throws Exception {
        AttendeeAndAlarm attendeeAndAlarm = new AttendeeAndAlarm();
        Attendee resourceAttendee = getResourceAttendee(entity);
        resourceAttendee.setPartStat(partStat.getStatus());
        if (comment.isPresent()) {
            resourceAttendee.setComment(comment.get());
        }
        attendeeAndAlarm.setAttendee(resourceAttendee);
        if (null != expectedErrorCode) {
            CalendarResult result = null;
            ChronosApiException expectedException = null;
            try {
                result = eventManager.updateAttendee(id, null, folderId, attendeeAndAlarm, true);
            } catch (ChronosApiException e) {
                expectedException = e;
            }
            assertNotNull(expectedException);
            assertEquals(expectedErrorCode, expectedException.getErrorCode());
            return result;
        }
        return eventManager.updateAttendee(id, null, folderId, attendeeAndAlarm, false);
    }

    private static Attendee assertPartStat(List<Attendee> actualAttendees, Integer entity, PartStat expectedPartStat) {
        return expectedPartStat.assertStatus(ChronosUtils.find(actualAttendees, entity));
    }

    private static Attendee assertPartStat(List<Attendee> actualAttendees, Integer entity, PartStat expectedPartStat, Optional<String> expectedComment) {
        Attendee matchingAttendee = expectedPartStat.assertStatus(ChronosUtils.find(actualAttendees, entity));
        if (expectedComment.isPresent()) {
            assertEquals(expectedComment.get(), matchingAttendee.getComment());
        }
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
            eventData.setAttendees(Arrays.asList(attendees));
        }
        return eventData;
    }

    private MailData assertNotificationMailForOrganizer(EventData reloadedEvent, PartStat partStat) throws Exception, ApiException {
        MailData mail = receiveNotification(testUser.getApiClient(), resource.getLogin(), reloadedEvent.getSummary());
        assertContent(mail, "the booking request for the resource", "Booking delegate didn't act");
        if (null != partStat) {
            if (NEEDS_ACTION.equals(partStat)) {
                assertContent(mail, "deferred", "Participant status isn't described");
            } else {
                assertContent(mail, partStat.getStatus().toLowerCase(), "Participant status isn't described");
            }
        }
        return cleanUpNotification(testUser, mail);
    }

    private MailData assertNotificationMailForDelegate(EventData event) throws Exception {
        return assertNotificationMailForDelegate(testUser2, event.getSummary());
    }

    private MailData assertNotificationMailForDelegate(TestUser user, String subject) throws Exception {
        MailData mail = receiveNotification(user.getApiClient(), resource.getLogin(), subject);
        assertContent(mail, "which is managed by you", "Expected resource manager justification");
        cleanUpNotification(user, mail);
        return mail;
    }

    private MailData cleanUpNotification(TestUser user, MailData mail) throws ApiException {
        MailsCleanUpResponse response = new MailApi(user.getApiClient()).deleteMails(List.of(new MailListElement().id(mail.getId()).folder(mail.getFolderId())), null, Boolean.TRUE, Boolean.FALSE);
        checkResponse(response.getError(), response.getErrorDesc());
        return mail;
    }

}
