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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.folder.manager.FolderPermissionsBits;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.group.GroupStorage;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.test.common.test.pool.UserModuleAccess;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ChronosNeedsActionResponse;
import com.openexchange.testing.httpclient.models.ChronosNeedsActionResponseData;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.ResourceData;
import com.openexchange.testing.httpclient.models.ResourcePermission;
import com.openexchange.testing.httpclient.models.ResourcePermission.PrivilegeEnum;
import com.openexchange.testing.httpclient.modules.ResourcesApi;
import com.openexchange.time.TimeTools;

/**
 * {@link NeedsActionDelegatesTest}
 *
 * Checks the <code>chronos?action=needsAction</code> in different scenarios where the requesting user has delegate
 * permissions for another calendar user, or a managed resource.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class NeedsActionDelegatesTest extends AbstractSecondUserChronosTest {

    private Integer resourceId;
    private String resourceFolderId;
    private TestUser testUser3;
    private String defaultFolderId3;
    private EventManager eventManager3;

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
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        testUser3 = testContext.acquireUser();
        UserApi userApi3 = new UserApi(testUser3.getApiClient(), testUser3);
        defaultFolderId3 = getDefaultFolder(testUser3.getApiClient());
        eventManager3 = new EventManager(userApi3, defaultFolderId3);
        /*
         * share folder from user 2 to user 1 with "author" permissions
         */
        CalendarFolderManager folderManager2 = new CalendarFolderManager(userApi2, userApi2.getFoldersApi());
        folderManager2.shareFolder(folderManager2.getFolder(defaultFolderId2), testUser, FolderPermissionsBits.AUTHOR);
        /*
         * prepare resource user where 1 acts as booking delegate
         */
        resourceId = testContext.acquireResource();
        resourceFolderId = "cal://0/resource" + resourceId;
        setResourcePermissions(userApi2.getClient(), resourceId,
            new ResourcePermission().entity(I(testUser.getUserId())).group(Boolean.FALSE).privilege(PrivilegeEnum.DELEGATE),
            new ResourcePermission().entity(I(GroupStorage.GROUP_ZERO_IDENTIFIER)).group(Boolean.TRUE).privilege(PrivilegeEnum.ASK_TO_BOOK)
        );
    }

    @Test
    public void testOwnAttendee() throws Exception {
        /*
         * as user 3, generate event with user 1 as attendee & create it
         */
        EventData eventData = prepareEventData(defaultFolderId3, getCalendarUser(testUser3), getUserAttendee(testUser), getUserAttendee(testUser3));
        EventData createdEvent = eventManager3.createEvent(eventData, true);
        /*
         * as user 1, check event is listed within 'needs-action' response, if delegates are included
         */
        List<EventData> eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, testUser.getUserId());
        assertContains(eventsNeedingAction, createdEvent.getId(), null);
        /*
         * as user 1, check event is also listed within 'needs-action' response, if delegates are not included
         */
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.FALSE, testUser.getUserId());
        assertContains(eventsNeedingAction, createdEvent.getId(), null);
        /*
         * as user 1, 'accept' the event, then check it is no longer listed within 'needs-action' response
         */
        updatePartStat(defaultUserApi, defaultFolderId, createdEvent.getId(), testUser.getUserId(), CuTypeEnum.INDIVIDUAL, "ACCEPTED");
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, testUser.getUserId());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
    }

    @Test
    public void testOtherAttendee() throws Exception {
        /*
         * as user 3, generate event with user 2 as attendee & create it
         */
        EventData eventData = prepareEventData(defaultFolderId3, getCalendarUser(testUser3), getUserAttendee(testUser2), getUserAttendee(testUser3));
        EventData createdEvent = eventManager3.createEvent(eventData, true);
        /*
         * as user 1, check event is listed within 'needs-action' response, if delegates are included
         */
        List<EventData> eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, testUser2.getUserId());
        assertContains(eventsNeedingAction, createdEvent.getId(), null);
        /*
         * as user 1, check event is not listed within 'needs-action' response, if delegates are not included
         */
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.FALSE, testUser2.getUserId());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
        /*
         * as user 1, 'accept' the event as delegate, then check it is no longer listed within 'needs-action' response
         */
        updatePartStat(defaultUserApi, defaultFolderId2, createdEvent.getId(), testUser2.getUserId(), CuTypeEnum.INDIVIDUAL, "ACCEPTED");
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, testUser2.getUserId());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
    }

    @Test
    public void testResourceAttendee() throws Exception {
        /*
         * as user 3, generate event with resource attendee & create it
         */
        EventData eventData = prepareEventData(defaultFolderId3, getCalendarUser(testUser3), getResourceAttendee(resourceId), getUserAttendee(testUser3));
        EventData createdEvent = eventManager3.createEvent(eventData, true);
        /*
         * as user 1, check event is listed within 'needs-action' response, if delegates are included
         */
        List<EventData> eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, resourceId.intValue());
        assertContains(eventsNeedingAction, createdEvent.getId(), null);
        /*
         * as user 1, check event is not listed within 'needs-action' response, if delegates are not included
         */
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.FALSE, resourceId.intValue());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
        /*
         * as user 1, 'accept' the event as delegate, then check it is no longer listed within 'needs-action' response
         */
        updatePartStat(defaultUserApi, resourceFolderId, createdEvent.getId(), resourceId.intValue(), CuTypeEnum.RESOURCE, "ACCEPTED");
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, resourceId.intValue());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
    }

    @Test
    public void testCollapsedEventsWithResourceAttendee() throws Exception {
        /*
         * as user 3, generate event series with resource attendee & create it
         */
        EventData eventData = prepareEventData(defaultFolderId3, getCalendarUser(testUser3), getResourceAttendee(resourceId), getUserAttendee(testUser2), getUserAttendee(testUser3));
        eventData.setRrule("FREQ=DAILY;COUNT=10");
        EventData createdEvent = eventManager3.createEvent(eventData, true);
        /*
         * as user 2, decline an instance of the event series so that an overridden occurrence get created
         */
        DateTimeData startDateData = createdEvent.getStartDate();
        Date recurrenceStartDate = CalendarUtils.add(DateTimeUtil.parseDateTime(startDateData), Calendar.DATE, 3, TimeZone.getTimeZone(startDateData.getTzid()));
        String recurrenceId = CalendarUtils.encode(new DateTime(TimeZone.getTimeZone(startDateData.getTzid()), recurrenceStartDate.getTime()));
        EventData createdException = updatePartStat(userApi2, defaultFolderId2, createdEvent.getId(), recurrenceId, testUser2.getUserId(), CuTypeEnum.INDIVIDUAL, "DECLINED");
        /*
         * as user 1, check that only series master event is listed within 'needs-action' response, if delegates are included
         */
        List<EventData> eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, resourceId.intValue());
        assertContains(eventsNeedingAction, createdEvent.getId(), null);
        assertNotContains(eventsNeedingAction, createdException.getId(), recurrenceId);
        /*
         * as user 1, check event series is not listed within 'needs-action' response, if delegates are not included
         */
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.FALSE, resourceId.intValue());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
        assertNotContains(eventsNeedingAction, createdException.getId(), recurrenceId);
        /*
         * as user 1, 'accept' the event series as delegate, then check it is no longer listed within 'needs-action' response
         */
        updatePartStat(defaultUserApi, resourceFolderId, createdEvent.getId(), resourceId.intValue(), CuTypeEnum.RESOURCE, "ACCEPTED");
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, resourceId.intValue());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
        assertNotContains(eventsNeedingAction, createdException.getId(), recurrenceId);
    }

    @Test
    public void testUncollapsedEventsWithResourceAttendee() throws Exception {
        /*
         * as user 3, generate event series with resource attendee & create it
         */
        EventData eventData = prepareEventData(defaultFolderId3, getCalendarUser(testUser3), getResourceAttendee(resourceId), getUserAttendee(testUser3));
        eventData.setRrule("FREQ=DAILY;COUNT=10");
        EventData createdEvent = eventManager3.createEvent(eventData, true);
        /*
         * as user 3, update an instance of the event series so that an overridden occurrence get created
         */
        DateTimeData startDateData = createdEvent.getStartDate();
        Date recurrenceStartDate = CalendarUtils.add(DateTimeUtil.parseDateTime(startDateData), Calendar.DATE, 3, TimeZone.getTimeZone(startDateData.getTzid()));
        String recurrenceId = CalendarUtils.encode(new DateTime(TimeZone.getTimeZone(startDateData.getTzid()), recurrenceStartDate.getTime()));
        EventData eventUpdate = new EventData().id(createdEvent.getId()).location("other location").recurrenceId(recurrenceId).lastModified(createdEvent.getLastModified());
        EventData createdException = eventManager3.updateOccurenceEvent(eventUpdate, recurrenceId, true);
        /*
         * as user 1, check that both the series master event as well as the exception are listed within 'needs-action' response, if delegates are included
         */
        List<EventData> eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, resourceId.intValue());
        assertContains(eventsNeedingAction, createdEvent.getId(), null);
        assertContains(eventsNeedingAction, createdException.getId(), recurrenceId);
        /*
         * as user 1, check event series is not listed within 'needs-action' response, if delegates are not included
         */
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.FALSE, resourceId.intValue());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
        assertNotContains(eventsNeedingAction, createdException.getId(), recurrenceId);
        /*
         * as user 1, 'accept' the event series as delegate, then check it is no longer listed within 'needs-action' response (but the exception still is)
         */
        updatePartStat(defaultUserApi, resourceFolderId, createdEvent.getId(), resourceId.intValue(), CuTypeEnum.RESOURCE, "ACCEPTED");
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, resourceId.intValue());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
        assertContains(eventsNeedingAction, createdException.getId(), recurrenceId);
        /*
         * as user 1, 'accept' the exception as delegate, then check it is no longer listed within 'needs-action' response (as well as the series master)
         */
        updatePartStat(defaultUserApi, resourceFolderId, createdException.getId(), recurrenceId, resourceId.intValue(), CuTypeEnum.RESOURCE, "ACCEPTED");
        eventsNeedingAction = getEventsNeedingAction(defaultUserApi, Boolean.TRUE, resourceId.intValue());
        assertNotContains(eventsNeedingAction, createdEvent.getId(), null);
        assertNotContains(eventsNeedingAction, createdException.getId(), recurrenceId);
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

    private static EventData assertContains(List<EventData> eventDataList, String id, String recurrenceId) {
        EventData eventData = lookupById(eventDataList, id, recurrenceId);
        assertNotNull(eventData, "event not found with id " + id + ", recurrence id " + recurrenceId);
        return null;
    }

    private static void assertNotContains(List<EventData> eventDataList, String id, String recurrenceId) {
        EventData eventData = lookupById(eventDataList, id, recurrenceId);
        assertNull(eventData, "event found with id " + id + ", recurrence id " + recurrenceId);
    }

    private static EventData lookupById(List<EventData> eventDataList, String id, String recurrenceId) {
        if (null != eventDataList) {
            for (EventData eventData : eventDataList) {
                if (Objects.equals(id, eventData.getId()) && Objects.equals(recurrenceId, eventData.getRecurrenceId())) {
                    return eventData;
                }
            }
        }
        return null;
    }

    private static void setResourcePermissions(ApiClient apiClient, Integer resourceId, ResourcePermission...permissions) throws Exception {
        ResourcesApi resourcesApi = new ResourcesApi(apiClient);
        CommonResponse response = resourcesApi.updateResourceBuilder()
            .withId(resourceId)
            .withTimestamp(L(2116800000000L))
            .withResourceData(new ResourceData().permissions(Arrays.asList(permissions)))
        .execute();
        assertNull(response.getErrorDesc(), response.getError());
    }

    private static List<EventData> getEventsNeedingAction(UserApi userApi, Boolean includeDelegates, int entity) throws Exception {
        ChronosNeedsActionResponse needsActionResponse = userApi.getChronosApi().getEventsNeedingActionBuilder() // @formatter:off
            .withIncludeDelegates(includeDelegates)
            .withRangeStart(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, -1).getTime()).getValue())
            .withRangeEnd(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, 1).getTime()).getValue())
            .withFields("lastModified,color,createdBy,endDate,flags,folder,id,location,recurrenceId,rrule,seriesId,startDate,summary,timestamp,transp,attendeePrivileges")
        .execute(); // @formatter:on
        List<ChronosNeedsActionResponseData> needsActionResponseList = needsActionResponse.getData();
        assertTrue(null != needsActionResponseList && 0 < needsActionResponseList.size());
        for (ChronosNeedsActionResponseData needsActionResponseData : needsActionResponseList) {
            if (null != needsActionResponseData.getAttendee() && null != needsActionResponseData.getAttendee().getEntity() &&
                entity == needsActionResponseData.getAttendee().getEntity().intValue()) {
                return needsActionResponseData.getEvents();
            }
        }
        return null;
    }

    private static EventData updatePartStat(UserApi userApi, String folderId, String eventId, int entity, CuTypeEnum cuType, String partStat) throws Exception {
        return updatePartStat(userApi, folderId, eventId, null, entity, cuType, partStat);
    }

    private static EventData updatePartStat(UserApi userApi, String folderId, String eventId, String recurrenceId, int entity, CuTypeEnum cuType, String partStat) throws Exception {
        ChronosCalendarResultResponse response = userApi.getChronosApi().updateAttendeeBuilder()
            .withAttendeeAndAlarm(new AttendeeAndAlarm().attendee(new Attendee().entity(I(entity)).partStat(partStat).cuType(cuType)))
            .withFolder(folderId)
            .withRecurrenceId(recurrenceId)
            .withId(eventId)
            .withTimestamp(L(2116800000000L))
        .execute();
        EventData updatedEventData = lookupById(response.getData().getUpdated(), eventId, recurrenceId);
        if (null == updatedEventData && null != recurrenceId && null != response.getData().getCreated()) {
            for (EventData eventData : response.getData().getCreated()) {
                if (Objects.equals(recurrenceId, eventData.getRecurrenceId()) && Objects.equals(eventId, eventData.getSeriesId())) {
                    updatedEventData = eventData;
                    break;
                }
            }
        }
        assertNotNull(updatedEventData, "updated event not found.");
        return updatedEventData;
    }

}
