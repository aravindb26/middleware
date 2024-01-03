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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.CalendarAssertUtil;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.ParticipationStatus;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosNeedsActionResponse;
import com.openexchange.testing.httpclient.models.ChronosNeedsActionResponseData;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link NeedsActionActionTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.4
 */
public class NeedsActionActionTest extends AbstractExtendedChronosTest {

    protected CalendarUser organizerCU;

    protected Attendee organizerAttendee;
    protected Attendee actingAttendee1;
    protected Attendee actingAttendee2;
    protected Attendee actingAttendee3;
    protected EventData event;
    private String summary;

    private TestUser testUser3;
    private TestUser testUser4;


    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);

        testUser3 = testContext.acquireUser();
        testUser4 = testContext.acquireUser();

        summary = this.getClass().getSimpleName() + UUID.randomUUID();
        event = EventFactory.createSeriesEvent(testUser.getUserId(), summary, 10, folderId);
        // The internal attendees
        organizerAttendee = createAttendee(I(testUser.getUserId()));
        actingAttendee1 = createAttendee(I(testUser2.getUserId()));
        actingAttendee2 = createAttendee(I(testUser3.getUserId()));
        actingAttendee3 = createAttendee(I(testUser4.getUserId()));

        LinkedList<Attendee> attendees = new LinkedList<>();
        attendees.add(organizerAttendee);
        attendees.add(actingAttendee1);
        attendees.add(actingAttendee2);
        attendees.add(actingAttendee3);
        event.setAttendees(attendees);

        // The original organizer
        organizerCU = AttendeeFactory.createOrganizerFrom(organizerAttendee);
        event.setOrganizer(organizerCU);
        event.setCalendarUser(organizerCU);

        EventData expectedEventData = eventManager.createEvent(event, true);
        event = eventManager.getEvent(folderId, expectedEventData.getId());
        CalendarAssertUtil.assertEventsEqual(expectedEventData, event);
    }

    @Test
    public void testCreateSeriesWithoutExceptions_returnOneNeedsAction() throws Exception {
        Calendar start = getStart();
        SessionAwareClient client3 = testUser3.getApiClient();
        UserApi userApi3 = new UserApi(client3, testUser3);

        Calendar end = getEnd();

        List<EventData> eventsNeedingAction = getEventsNeedingAction(userApi3, start, end);
        Assertions.assertEquals(1, filter(eventsNeedingAction).size());
    }

    @Test
    public void testCreateSeriesWithoutExceptionsAndOneSingleEventsFromDifferentUser_returnTwoNeedsAction() throws Exception {
        Calendar start = getStart();
        createSingleEvent();

        SessionAwareClient client3 = testUser3.getApiClient();
        UserApi userApi3 = new UserApi(client3, testUser3);

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(end.getTimeInMillis() + TimeUnit.HOURS.toMillis(2));

        List<EventData> eventsNeedingAction = getEventsNeedingAction(userApi3, start, end);
        Assertions.assertEquals(2, filter(eventsNeedingAction).size());
    }

    @Test
    public void testCreateSeriesWithChangedSummary_returnTwoNeedsAction() throws Exception {
        Calendar start = getStart();
        EventData secondOccurrence = getSecondOccurrence(eventManager, event);
        secondOccurrence.setSummary(event.getSummary() + "The summary changed and that should result in a dedicated action");
        eventManager.updateOccurenceEvent(secondOccurrence, secondOccurrence.getRecurrenceId(), true);

        SessionAwareClient client3 = testUser3.getApiClient();
        UserApi userApi3 = new UserApi(client3, testUser3);

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(end.getTimeInMillis() + TimeUnit.DAYS.toMillis(14));

        List<EventData> eventsNeedingAction = getEventsNeedingAction(userApi3, start, end);
        Assertions.assertEquals(2, filter(eventsNeedingAction).size());
    }

    @Test
    public void testCreateSeriesWithOneDeclineOccurrences_returnOneNeedsActionForSeriesOnly() throws Exception {
        Calendar start = getStart();
        AttendeeAndAlarm data = new AttendeeAndAlarm();
        organizerAttendee.setPartStat(ParticipationStatus.DECLINED.toString());
        organizerAttendee.setMember(null);
        data.setAttendee(organizerAttendee);

        EventData secondOccurrence = getSecondOccurrence(eventManager, event);
        eventManager.updateAttendee(secondOccurrence.getId(), secondOccurrence.getRecurrenceId(), folderId, data, false);

        SessionAwareClient client3 = testUser3.getApiClient();
        UserApi userApi3 = new UserApi(client3, testUser3);

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(end.getTimeInMillis() + TimeUnit.DAYS.toMillis(14));

        List<EventData> eventsNeedingAction = getEventsNeedingAction(userApi3, start, end);
        Assertions.assertEquals(1, filter(eventsNeedingAction).size());
    }

    @Test
    public void testCreateSeriesWithChangedSummaryAndOneSingleEvent_returnThreeNeedsAction() throws Exception {
        Calendar start = getStart();
        EventData secondOccurrence = getSecondOccurrence(eventManager, event);
        secondOccurrence.setSummary(event.getSummary() + "The summary changed and that should result in a dedicated action");
        eventManager.updateOccurenceEvent(secondOccurrence, secondOccurrence.getRecurrenceId(), true);

        createSingleEvent();

        SessionAwareClient client3 = testUser3.getApiClient();
        UserApi userApi3 = new UserApi(client3, testUser3);

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(end.getTimeInMillis() + TimeUnit.DAYS.toMillis(14));

        List<EventData> eventsNeedingAction = getEventsNeedingAction(userApi3, start, end);
        Assertions.assertEquals(3, filter(eventsNeedingAction).size());
    }

    private void createSingleEvent() throws ApiException {
        EventData singleEvent = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), summary);
        LinkedList<Attendee> attendees = new LinkedList<>();
        attendees.add(organizerAttendee);
        attendees.add(actingAttendee1);
        attendees.add(actingAttendee2);
        attendees.add(actingAttendee3);
        singleEvent.setAttendees(attendees);
        // The original organizer
        CalendarUser organizerCU = AttendeeFactory.createOrganizerFrom(organizerAttendee);
        singleEvent.setOrganizer(organizerCU);
        singleEvent.setCalendarUser(organizerCU);
        eventManager.createEvent(singleEvent, true);
    }

    private static EventData getSecondOccurrence(EventManager manager, EventData event) throws ApiException {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        Date from = CalendarUtils.truncateTime(new Date(), timeZone);
        Date until = CalendarUtils.add(from, Calendar.DATE, 7, timeZone);
        List<EventData> occurrences = manager.getAllEvents(event.getFolder(), from, until, true);
        occurrences = occurrences.stream().filter(x -> x.getId().equals(event.getId())).collect(Collectors.toList());

        return occurrences.get(2);
    }

    private Calendar getEnd() {
        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(end.getTimeInMillis() + TimeUnit.HOURS.toMillis(2));
        return end;
    }

    private Calendar getStart() {
        Calendar start = Calendar.getInstance();
        start.setTimeInMillis(start.getTimeInMillis() - TimeUnit.HOURS.toMillis(1));
        return start;
    }

    private List<EventData> filter(List<EventData> eventsNeedingAction) {
        return EventManager.filterEventBySummary(eventsNeedingAction, summary);
    }

    private static List<EventData> getEventsNeedingAction(UserApi userApi, Calendar start, Calendar end) throws ApiException {
        // @formatter:off
        ChronosNeedsActionResponse needsActionResponse = userApi.getChronosApi()
                                                                .getEventsNeedingActionBuilder()
                                                                .withRangeStart(DateTimeUtil.getDateTime(start).getValue())
                                                                .withRangeEnd(DateTimeUtil.getDateTime(end).getValue())
                                                                .withIncludeDelegates(Boolean.FALSE)
                                                                .execute();
        // @formatter:on
        return getEventsForEntity(needsActionResponse, userApi.getUser().getUserId());
    }

    private static List<EventData> getEventsForEntity(ChronosNeedsActionResponse needsActionResponse, int entity) {
        assertNotNull(needsActionResponse);
        List<ChronosNeedsActionResponseData> responseDataList = needsActionResponse.getData();
        assertNotNull(responseDataList);
        ChronosNeedsActionResponseData matchingResponseData = null;
        for (ChronosNeedsActionResponseData responseData : responseDataList) {
            if (null != responseData.getAttendee() && null != responseData.getAttendee().getEntity() && entity == responseData.getAttendee().getEntity().intValue()) {
                matchingResponseData = responseData;
                break;
            }
        }
        assertNotNull(matchingResponseData);
        return matchingResponseData.getEvents();
    }

}
