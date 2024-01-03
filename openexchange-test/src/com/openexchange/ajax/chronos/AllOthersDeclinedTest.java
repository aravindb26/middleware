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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventData.FlagsEnum;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.EventsResponse;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import com.openexchange.time.TimeTools;

/**
 * {@link AllOthersDeclinedTest}
 *
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class AllOthersDeclinedTest extends AbstractSecondUserChronosTest {

    private TestUser testUser3;
    private UserApi userApi1;
    private UserApi userApi3;
    private ChronosApi chronosApi2;
    private ChronosApi chronosApi3;
    private String defaultFolderId3;

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder()
            .withUserPerContext(3)
        .build();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        userApi1 = defaultUserApi;
        chronosApi2 = userApi2.getChronosApi();
        testUser3 = testContext.acquireUser();
        userApi3 = new UserApi(testUser3.getApiClient(), testUser3);
        chronosApi3 = userApi3.getChronosApi();
        defaultFolderId3 = getDefaultFolder(testUser3.getApiClient());
    }

    private static EventData assertAllOthersDeclinedTrue(ChronosApi chronosApi, String folderId, String objectId, String recurrenceId) throws Exception {
        return assertAllOthersDeclinedFlag(chronosApi, folderId, objectId, recurrenceId, true);
    }

    private static EventData assertAllOthersDeclinedFalse(ChronosApi chronosApi, String folderId, String objectId, String recurrenceId) throws Exception {
        return assertAllOthersDeclinedFlag(chronosApi, folderId, objectId, recurrenceId, false);
    }

    private static EventData assertAllOthersDeclinedFlag(ChronosApi chronosApi, String folderId, String objectId, String recurrenceId, boolean expectAllOthersDeclinedSet) throws Exception {
        /*
         * check in 'all' response
         */
        EventsResponse eventsResponse = chronosApi.getAllEventsBuilder() // @formatter:off
            .withFolder(folderId)
            .withRangeStart(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, -1).getTime()).getValue())
            .withRangeEnd(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, 1).getTime()).getValue())
            .withFields("lastModified,color,createdBy,endDate,flags,folder,id,location,recurrenceId,rrule,seriesId,startDate,summary,timestamp,transp,attendeePrivileges")
            .withExpand(Boolean.valueOf(null != recurrenceId))
        .execute(); // @formatter:on
        EventData eventData = lookupByIdOrSeriesId(eventsResponse.getData(), objectId, recurrenceId);
        assertNotNull(eventData);
        assertTrue(null != eventData.getFlags() && expectAllOthersDeclinedSet == eventData.getFlags().contains(FlagsEnum.ALL_OTHERS_DECLINED));
        /*
         * check in 'get' response
         */
        EventResponse eventResponse = chronosApi.getEventBuilder().withFolder(folderId).withId(objectId).withRecurrenceId(recurrenceId).execute();
        eventData = eventResponse.getData();
        assertNotNull(eventData);
        assertTrue(null != eventData.getFlags() && expectAllOthersDeclinedSet == eventData.getFlags().contains(FlagsEnum.ALL_OTHERS_DECLINED));
        return eventData;
    }

    private static ChronosCalendarResultResponse updatePartStat(ChronosApi chronosApi, String folderId, String eventId, String recurrenceId, int userId, String partStat) throws Exception {
        return chronosApi.updateAttendeeBuilder() // @formatter:off
            .withAttendeeAndAlarm(new AttendeeAndAlarm().attendee(new Attendee().entity(I(userId)).partStat(partStat).cuType(CuTypeEnum.INDIVIDUAL)))
            .withFolder(folderId)
            .withRecurrenceId(recurrenceId)
            .withId(eventId)
            .withTimestamp(L(2116800000000L))
        .execute(); // @formatter:on
    }

    @Test
    public void testSingleEvent() throws Exception {
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser3));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        testAllOthersDeclinedFlag(createdEvent.getId(), null);
    }

    @Test
    public void testSeries() throws Exception {
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser3));
        eventData.setRrule("FREQ=DAILY;COUNT=8");
        EventData createdEvent = eventManager.createEvent(eventData, true);
        testAllOthersDeclinedFlag(createdEvent.getId(), null);
    }

    @Test
    public void testSeriesOccurrence() throws Exception {
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser3));
        eventData.setRrule("FREQ=DAILY;COUNT=8");
        EventData createdEvent = eventManager.createEvent(eventData, true);
        EventsResponse eventsResponse = chronosApi.getAllEventsBuilder() // @formatter:off
            .withFolder(defaultFolderId)
            .withRangeStart(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, -1).getTime()).getValue())
            .withRangeEnd(DateTimeUtil.getZuluDateTime(CalendarUtils.add(new Date(), Calendar.MONTH, 1).getTime()).getValue())
            .withFields("lastModified,color,createdBy,endDate,flags,folder,id,location,recurrenceId,rrule,seriesId,startDate,summary,timestamp,transp,attendeePrivileges")
            .withExpand(Boolean.TRUE)
        .execute(); // @formatter:on
        EventData eventOccurrence = null;
        for (EventData loadedEventData : eventsResponse.getData()) {
            if (Objects.equals(createdEvent.getSeriesId(), loadedEventData.getSeriesId()) && null != loadedEventData.getRecurrenceId()) {
                eventOccurrence = loadedEventData;
                break;
            }
        }
        assertNotNull(eventOccurrence);
        testAllOthersDeclinedFlag(eventOccurrence.getId(), eventOccurrence.getRecurrenceId());
    }

    @Test
    public void testWithoutOthers() throws Exception {
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * check event 'all others declined' flag is always false
         */
        assertAllOthersDeclinedFalse(chronosApi, defaultFolderId, createdEvent.getId(), null);
        updatePartStat(chronosApi, defaultFolderId, createdEvent.getId(), null, userApi1.getUser().getUserId(), "DECLINED");
        assertAllOthersDeclinedFalse(chronosApi, defaultFolderId, createdEvent.getId(), null);
    }

    @Test
    public void testMWB2309() throws Exception {
        Integer resourceId = testContext.acquireResource();
        EventData eventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getResourceAttendee(resourceId));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * check event 'all others declined' flag is always false
         */
        assertAllOthersDeclinedFalse(chronosApi, defaultFolderId, createdEvent.getId(), null);
        updatePartStat(chronosApi, defaultFolderId, createdEvent.getId(), null, userApi1.getUser().getUserId(), "DECLINED");
        assertAllOthersDeclinedFalse(chronosApi, defaultFolderId, createdEvent.getId(), null);
    }

    private void testAllOthersDeclinedFlag(String eventId, String recurrenceId) throws Exception {
        /*
         * check event 'all others declined' flag is false from perspective of each attendee
         */
        assertAllOthersDeclinedFalse(chronosApi, defaultFolderId, eventId, recurrenceId);
        assertAllOthersDeclinedFalse(chronosApi2, defaultFolderId2, eventId, recurrenceId);
        assertAllOthersDeclinedFalse(chronosApi3, defaultFolderId3, eventId, recurrenceId);
        /*
         * as user 2, 'decline' the event
         */
        updatePartStat(chronosApi2, defaultFolderId2, eventId, recurrenceId, userApi2.getUser().getUserId(), "DECLINED");
        /*
         * check event 'all others declined' flag is false from perspective of each attendee
         */
        assertAllOthersDeclinedFalse(chronosApi, defaultFolderId, eventId, recurrenceId);
        assertAllOthersDeclinedFalse(chronosApi2, defaultFolderId2, eventId, recurrenceId);
        assertAllOthersDeclinedFalse(chronosApi3, defaultFolderId3, eventId, recurrenceId);
        /*
         * as user 3, 'decline' the event, too
         */
        updatePartStat(chronosApi3, defaultFolderId3, eventId, recurrenceId, userApi3.getUser().getUserId(), "DECLINED");
        /*
         * check event 'all others declined' flag is true from perspective of user 1, and false from the perspective of the others
         */
        assertAllOthersDeclinedTrue(chronosApi, defaultFolderId, eventId, recurrenceId);
        assertAllOthersDeclinedFalse(chronosApi2, defaultFolderId2, eventId, recurrenceId);
        assertAllOthersDeclinedFalse(chronosApi3, defaultFolderId3, eventId, recurrenceId);
        /*
         * as user 1, 'decline' the event, too
         */
        updatePartStat(chronosApi, defaultFolderId, eventId, recurrenceId, userApi1.getUser().getUserId(), "DECLINED");
        /*
         * check event 'all others declined' flag is true from perspective of each attendee
         */
        assertAllOthersDeclinedTrue(chronosApi, defaultFolderId, eventId, recurrenceId);
        assertAllOthersDeclinedTrue(chronosApi2, defaultFolderId2, eventId, recurrenceId);
        assertAllOthersDeclinedTrue(chronosApi3, defaultFolderId3, eventId, recurrenceId);
        /*
         * as user 2, 'accept' the event again
         */
        updatePartStat(chronosApi2, defaultFolderId2, eventId, recurrenceId, userApi2.getUser().getUserId(), "ACCEPTED");
        /*
         * check event 'all others declined' flag is true from perspective of user 2, and false from the perspective of the others
         */
        assertAllOthersDeclinedFalse(chronosApi, defaultFolderId, eventId, recurrenceId);
        assertAllOthersDeclinedTrue(chronosApi2, defaultFolderId2, eventId, recurrenceId);
        assertAllOthersDeclinedFalse(chronosApi3, defaultFolderId3, eventId, recurrenceId);
    }

    private static EventData prepareEventData(String folderId, CalendarUser organizer, Attendee... attendees) {
        TimeZone timeZone = TimeZone.getTimeZone("Indian/Mahe");
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

    private static EventData lookupByIdOrSeriesId(List<EventData> eventDataList, String id, String recurrenceId) {
        if (null != eventDataList) {
            for (EventData eventData : eventDataList) {
                if (Objects.equals(id, eventData.getId()) && Objects.equals(recurrenceId, eventData.getRecurrenceId())) {
                    return eventData;
                }
                if (null != eventData.getSeriesId() && null != recurrenceId) {
                    if (Objects.equals(id, eventData.getSeriesId()) && Objects.equals(recurrenceId, eventData.getRecurrenceId())) {
                        return eventData;
                    }
                }
            }
        }
        return null;
    }

}
