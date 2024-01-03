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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.Classification;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.EventsResponse;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.MultipleEventData;
import com.openexchange.testing.httpclient.models.MultipleFolderEventsResponse;
import com.openexchange.time.TimeTools;

/**
 * {@link ResourceCalendarTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class ResourceCalendarTest extends AbstractSecondUserChronosTest {
    
    private Integer resourceId;
    private String resourceFolderId;
    private CalendarFolderManager folderManager2;
    
    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.resourceId = testContext.acquireResource();
        this.resourceFolderId = "cal://0/resource" + resourceId;
        this.folderManager2 = new CalendarFolderManager(userApi2, userApi2.getFoldersApi());
    }

    @Test
    public void testGetOwnEvent() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions with full details
         */
        assertContainsEventWithDetails(getAllEvents(defaultUserApi, defaultFolderId), createdEvent);
        EventData eventInResourceFolder = assertContainsEventWithDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), true));
    }
    
    @Test
    public void testGetOwnPrivateEvent() throws Exception {
        /*
         * generate event with resource in user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getResourceAttendee(resourceId));
        eventData.setPropertyClass(Classification.CONFIDENTIAL.getValue());
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions with full details
         */
        assertContainsEventWithDetails(getAllEvents(defaultUserApi, defaultFolderId), createdEvent);
        EventData eventInResourceFolder = assertContainsEventWithDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), true));
    }

    @Test
    public void testGetForeignEvent() throws Exception {
        /*
         * generate event with resource in other user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' action for resource folder, but w/o details
         */
        EventData eventInResourceFolder = assertContainsEventWithoutDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasNoDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasNoDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertNull(searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), false));
    }

    @Test
    public void testGetForeignEventAsAttendee() throws Exception {
        /*
         * generate event with resource in other user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId), getUserAttendee(testUser));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions with full details
         */
        assertContainsEventWithDetails(getAllEvents(defaultUserApi, defaultFolderId), createdEvent);
        EventData eventInResourceFolder = assertContainsEventWithDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), true));
    }

    @Test
    public void testGetForeignPrivateEvent() throws Exception {
        /*
         * generate event with resource in other user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId));
        eventData.setPropertyClass(Classification.CONFIDENTIAL.getValue());
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' action for resource folder, but w/o details
         */
        EventData eventInResourceFolder = assertContainsEventWithoutDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasNoDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasNoDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertNull(searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), false));
    }

    @Test
    public void testGetForeignPrivateEventAsAttendee() throws Exception {
        /*
         * generate event with resource in other user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId), getUserAttendee(testUser));
        eventData.setPropertyClass(Classification.CONFIDENTIAL.getValue());
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions with full details
         */
        assertContainsEventWithDetails(getAllEvents(defaultUserApi, defaultFolderId), createdEvent);
        EventData eventInResourceFolder = assertContainsEventWithDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), true));
    }

    @Test
    public void testGetForeignEventInSharedWithAccess() throws Exception {
        /*
         * share other user's calendar folder to user
         */
        FolderData folderData = userApi2.getFoldersApi().getFolderBuilder().withId(defaultFolderId2).execute().getData();
        folderManager2.shareFolder(folderData, testUser);
        /*
         * generate event with resource in other user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions with full details
         */
        assertContainsEventWithDetails(getAllEvents(defaultUserApi, defaultFolderId2), createdEvent);
        EventData eventInResourceFolder = assertContainsEventWithDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), true));
    }

    @Test
    public void testGetForeignEventInSharedWithoutAccess() throws Exception {
        /*
         * generate event with resource in other user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' action for resource folder, but w/o details
         */
        EventData eventInResourceFolder = assertContainsEventWithoutDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasNoDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasNoDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertNull(searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), false));
    }

    @Test
    public void testGetForeignPrivateEventInSharedWithAccess() throws Exception {
        /*
         * share other user's calendar folder to user
         */
        FolderData folderData = userApi2.getFoldersApi().getFolderBuilder().withId(defaultFolderId2).execute().getData();
        folderManager2.shareFolder(folderData, testUser);
        /*
         * generate event with resource in other user's personal folder & create it
         */
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId));
        eventData.setPropertyClass(Classification.CONFIDENTIAL.getValue());
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions, but w/o details
         */
        assertContainsEventWithoutDetails(getAllEvents(defaultUserApi, defaultFolderId2), createdEvent);
        EventData eventInResourceFolder = assertContainsEventWithoutDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasNoDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasNoDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertNull(searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), false));
    }

    @Test
    public void testGetForeignEventInPublicWithAccess() throws Exception {
        /*
         * create & share a public calendar folder to user
         */
        String folderId = folderManager2.createPublicCalendarFolder(UUIDs.getUnformattedStringFromRandom());
        FolderData folderData = userApi2.getFoldersApi().getFolderBuilder().withId(folderId).execute().getData();
        folderManager2.shareFolder(folderData, testUser);
        /*
         * generate event with resource in public folder & create it
         */
        EventData eventData = prepareEventData(folderId, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions with full details
         */
        assertContainsEventWithDetails(getAllEvents(defaultUserApi, folderId), createdEvent);
        EventData eventInResourceFolder = assertContainsEventWithDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), true));
    }

    @Test
    public void testGetForeignEventInPublicWithoutAccess() throws Exception {
        /*
         * create a public calendar folder
         */
        String folderId = folderManager2.createPublicCalendarFolder(UUIDs.getUnformattedStringFromRandom());
        /*
         * generate event with resource in public folder & create it
         */
        EventData eventData = prepareEventData(folderId, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions without details
         */
        EventData eventInResourceFolder = assertContainsEventWithoutDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasNoDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasNoDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertNull(searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), false));
    }

    @Test
    public void testGetForeignEventInPublicAsAttendee() throws Exception {
        /*
         * create a public calendar folder
         */
        String folderId = folderManager2.createPublicCalendarFolder(UUIDs.getUnformattedStringFromRandom());
        /*
         * generate event with resource in public folder & create it
         */
        EventData eventData = prepareEventData(folderId, getCalendarUser(testUser2), getUserAttendee(testUser2), getResourceAttendee(resourceId), getUserAttendee(testUser));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * check that created event appears in response to 'all' actions with full details
         */
        EventData eventInResourceFolder = assertContainsEventWithDetails(getAllEvents(defaultUserApi, resourceFolderId), createdEvent);
        /*
         * get, list and search event, from perspective of resource folder & verify the data as well
         */
        assertEventHasDetails(createdEvent, getEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, listEvent(defaultUserApi, getEventId(eventInResourceFolder)));
        assertEventHasDetails(createdEvent, searchEvent(defaultUserApi, getEventId(eventInResourceFolder), createdEvent.getSummary(), true));
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

    private static List<EventData> getAllEvents(UserApi userApi, String folderId) throws Exception {
        // @formatter:off
        return new EventManager(userApi, null).getAllEvents(
            CalendarUtils.add(new Date(), Calendar.MONTH, -1),
            CalendarUtils.add(new Date(), Calendar.MONTH, 1),
            true, 
            folderId,
            null,
            "lastModified,color,createdBy,endDate,flags,folder,id,location,recurrenceId,rrule,seriesId,startDate,summary,timestamp,transp,attendeePrivileges"
        );
       // @formatter:on
    }

    private static EventId getEventId(EventData eventData) {
        EventId eventId = new EventId();
        eventId.setFolder(eventData.getFolder());
        eventId.setId(eventData.getId());
        eventId.setRecurrenceId(eventData.getRecurrenceId());
        return eventId;
    }

    private static EventData getEvent(UserApi userApi, EventId eventId) throws Exception {
        EventResponse eventResponse = userApi.getChronosApi().getEventBuilder() // @formatter:off
            .withFolder(eventId.getFolder())
            .withId(eventId.getId())
            .withRecurrenceId(eventId.getRecurrenceId())
        .execute(); // @formatter:on
        assertNull(eventResponse.getError());
        return eventResponse.getData();
    }

    private static EventData listEvent(UserApi userApi, EventId eventId) throws Exception {
        EventsResponse eventsResponse = userApi.getChronosApi().getEventListBuilder() // @formatter:off
            .withEventId(Collections.singletonList(eventId))
            .withFields("lastModified,color,createdBy,endDate,flags,folder,id,location,recurrenceId,rrule,seriesId,startDate,summary,timestamp,transp,attendeePrivileges")
        .execute(); // @formatter:on
        assertNull(eventsResponse.getError());
        EventData eventData = find(eventsResponse.getData(), eventId.getFolder(), eventId.getId(), eventId.getRecurrenceId());
        assertNotNull(eventData);
        return eventData;
    }

    private static EventData searchEvent(UserApi userApi, EventId eventId, String pattern, boolean expectToFind) throws Exception {
        String folderId = eventId.getFolder();
        String body = "{"
            + "\"filter\":[\"and\",[\"or\",[\"=\",{\"field\":\"summary\"},\"*" + pattern + "*\"],[\"=\",{\"field\":\"location\"},\"*" + pattern + "*\"]]],"
            + "\"folders\":[\"" + folderId + "\"]"
        + "}";
        MultipleFolderEventsResponse searchResponse = userApi.getChronosApi().searchChronosAdvancedBuilder() // @formatter:off
            .withExpand(Boolean.TRUE)
            .withRangeStart(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, -1).getTime()).getValue())
            .withRangeEnd(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, 1).getTime()).getValue())
            .withBody(body)
            .withFields("lastModified,color,createdBy,endDate,flags,folder,id,location,recurrenceId,rrule,seriesId,startDate,summary,timestamp,transp,attendeePrivileges")
        .execute(); // @formatter:on
        List<MultipleEventData> searchResponseData = searchResponse.getData();
        assertTrue(null != searchResponseData && 1 == searchResponseData.size());
        MultipleEventData multipleEventData = searchResponseData.get(0);
        assertTrue(null != multipleEventData && folderId.equals(multipleEventData.getFolder()));
        assertNull(multipleEventData.getError());
        EventData eventData = find(multipleEventData.getEvents(), eventId);
        if (expectToFind) {
            assertNotNull(eventData);
        } else {
            assertNull(eventData);
        }
        return eventData;
    }

    private static EventData find(List<EventData> eventDatas, EventId eventId) {
        return find(eventDatas, eventId.getFolder(), eventId.getId(), eventId.getRecurrenceId());
    }

    private static EventData find(List<EventData> eventDatas, String folderId, String id, String recurrenceId) {
        if (null != eventDatas) {
            for (EventData eventData : eventDatas) {
                if (null == folderId || folderId.equals(eventData.getFolder())) {
                    if (null == id || id.equals(eventData.getId())) {
                        if (null == recurrenceId || recurrenceId.equals(eventData.getRecurrenceId())) {
                            return eventData;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static EventData assertContainsEventWithDetails(List<EventData> actual, EventData expected) {
        return assertEventHasDetails(expected, find(actual, null, expected.getId(), null));
    }

    private static EventData assertEventHasDetails(EventData expected, EventData actual) {
        assertNotNull(actual);
        assertEquals(expected.getSummary(), actual.getSummary());
        assertEquals(expected.getLocation(), actual.getLocation());
        return actual;
    }

    private static EventData assertContainsEventWithoutDetails(List<EventData> actual, EventData expected) {
        return assertEventHasNoDetails(expected, find(actual, null, expected.getId(), null));
    }

    private static EventData assertEventHasNoDetails(EventData expected, EventData actual) {
        assertNotNull(actual);
        assertNotEquals(expected.getSummary(), actual.getSummary()); // expect either "null" or "Private", but not original summary
        assertNull(actual.getLocation());
        return actual;
    }

}
