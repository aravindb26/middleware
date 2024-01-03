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

package com.openexchange.ajax.chronos.bugs;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.itip.ITipUtil;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.UpdateEventBody;

/**
 *
 * {@link MWB1311Test}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class MWB1311Test extends AbstractChronosTest {

    private String defaultFolderId2;
    private EventManager eventManager2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        SessionAwareClient client = testUser2.getApiClient();
        UserApi defaultUserApi2 = new UserApi(client, testUser2);
        defaultFolderId2 = getDefaultFolder(client);
        eventManager2 = new EventManager(defaultUserApi2, defaultFolderId2);
    }

    @Test
    public void testResetPartStatSeries() throws Exception {
        /*
         * Create daily series
         */
        String subject = "MWB-1311.testResetPartStatSeries";
        EventData seriesEvent = EventFactory.createSeriesEvent(getCalendaruser(), subject, 10, defaultFolderId);
        Attendee attendee = AttendeeFactory.createAttendee(I(testUser2.getUserId()), CuTypeEnum.INDIVIDUAL);
        attendee.cn(testUser2.getUser());
        attendee.email(testUser2.getLogin());
        attendee.setUri("mailto:" + testUser2.getLogin());
        seriesEvent.setAttendees(Collections.singletonList(attendee));

        EventData createEvent = eventManager.createEvent(seriesEvent);
        assertThat(createEvent.getRrule(), is(seriesEvent.getRrule()));

        /*
         * Accept series as attendee
         */
        String eventId = createEvent.getId();
        EventData reloadedEvent = eventManager2.getEvent(defaultFolderId2, eventId);
        AttendeeAndAlarm attendeeAndAlarm = new AttendeeAndAlarm();
        attendee.setPartStat("ACCEPTED");
        attendeeAndAlarm.setAttendee(attendee);
        eventManager2.updateAttendee(reloadedEvent.getId(), attendeeAndAlarm, false);
        /*
         * Delete specific occurrence
         */
        EventData secondOccurrence = getSecondOccurrence(eventManager2, createEvent);
        eventManager2.deleteEvent(secondOccurrence, defaultFolderId2);
        secondOccurrence = getException(eventManager, eventId);
        assertThat(secondOccurrence.getId(), is(not(eventId)));
        Attendee updatedAttendee = secondOccurrence.getAttendees().stream().filter(a -> i(a.getEntity()) == testUser2.getUserId()).findAny().orElseThrow(() -> new AssertionError("Attendee not found"));
        assertThat(updatedAttendee.getPartStat(), is("DECLINED"));
        createEvent = eventManager.getEvent(defaultFolderId, eventId);

        /*
         * Reset status of attendee as organizer for the whole series
         */
        EventData deltaEvent = new EventData();
        deltaEvent.setId(eventId);
        deltaEvent.setFolder(createEvent.getFolder());
        deltaEvent.setChangeExceptionDates(createEvent.getChangeExceptionDates());
        List<Attendee> attendees = new ArrayList<>(createEvent.getAttendees().size());
        for (Attendee a : createEvent.getAttendees()) {
            if (i(a.getEntity()) == testUser2.getUserId()) {
                a.setPartStat("NEEDS-ACTION");
            }
            attendees.add(a);
        }
        deltaEvent.setAttendees(attendees);
        UpdateEventBody body = new UpdateEventBody();
        body.setEvent(deltaEvent);
        ChronosCalendarResultResponse updateResponse = defaultUserApi.getChronosApi()
            .updateEvent(defaultFolderId, eventId, L(eventManager.getLastTimeStamp()), body, null, null, Boolean.TRUE, null, Boolean.FALSE, null, null, null, null, Boolean.FALSE, null);
        CalendarResult calendarResult = checkResponse(updateResponse.getErrorDesc(), updateResponse.getError(), updateResponse.getCategories(), updateResponse.getData());
        assertThat("Found unexpected conflicts", calendarResult.getConflicts(), anyOf(empty(), nullValue()));
        List<EventData> updates = calendarResult.getUpdated();
        assertEquals(1, updates.size());
        EventData e = updates.get(0);
        if (e.getId().equals(e.getSeriesId()) && null == e.getRecurrenceId()) {
            updatedAttendee = e.getAttendees().stream().filter(a -> i(a.getEntity()) == testUser2.getUserId()).findAny().orElseThrow(() -> new AssertionError("Attendee not found"));
            assertThat(updatedAttendee.getPartStat(), is("NEEDS-ACTION"));
        }
        /*
         * Receive notification as attendee
         */
        ITipUtil.receiveNotification(testUser2.getApiClient(), testUser.getLogin(), subject);
    }

    @Test
    public void testResetPartStatException() throws Exception {
        /*
         * Create daily series
         */
        String subject = "MWB-1311.testResetPartStatException";
        EventData seriesEvent = EventFactory.createSeriesEvent(getCalendaruser(), subject, 10, defaultFolderId);
        Attendee attendee = AttendeeFactory.createAttendee(I(testUser2.getUserId()), CuTypeEnum.INDIVIDUAL);
        attendee.cn(testUser2.getUser());
        attendee.email(testUser2.getLogin());
        attendee.setUri("mailto:" + testUser2.getLogin());
        seriesEvent.setAttendees(Collections.singletonList(attendee));

        EventData createEvent = eventManager.createEvent(seriesEvent);
        assertThat(createEvent.getRrule(), is(seriesEvent.getRrule()));

        /*
         * Accept series as attendee
         */
        String eventId = createEvent.getId();
        EventData reloadedEvent = eventManager2.getEvent(defaultFolderId2, eventId);
        AttendeeAndAlarm attendeeAndAlarm = new AttendeeAndAlarm();
        attendee.setPartStat("ACCEPTED");
        attendeeAndAlarm.setAttendee(attendee);
        eventManager2.updateAttendee(reloadedEvent.getId(), attendeeAndAlarm, false);

        /*
         * Delete specific occurrence
         */
        EventData secondOccurrence = getSecondOccurrence(eventManager2, createEvent);
        eventManager2.deleteEvent(secondOccurrence, defaultFolderId2);
        secondOccurrence = getException(eventManager, eventId);
        assertThat(secondOccurrence.getId(), is(not(eventId)));
        Attendee updatedAttendee = secondOccurrence.getAttendees().stream().filter(a -> i(a.getEntity()) == testUser2.getUserId()).findAny().orElseThrow(() -> new AssertionError("Attendee not found"));
        assertThat(updatedAttendee.getPartStat(), is("DECLINED"));
        createEvent = eventManager.getEvent(defaultFolderId, eventId);

        /*
         * Reset status of attendee as organizer for the exception only
         */
        EventData deltaEvent = new EventData();
        deltaEvent.setId(secondOccurrence.getId());
        deltaEvent.setFolder(secondOccurrence.getFolder());
        List<Attendee> attendees = new ArrayList<>(createEvent.getAttendees().size());
        for (Attendee a : createEvent.getAttendees()) {
            if (i(a.getEntity()) == testUser2.getUserId()) {
                a.setPartStat("NEEDS-ACTION");
            }
            attendees.add(a);
        }
        deltaEvent.setAttendees(attendees);
        EventData updateEvent = eventManager.updateEvent(deltaEvent);
        updatedAttendee = updateEvent.getAttendees().stream().filter(a -> i(a.getEntity()) == testUser2.getUserId()).findAny().orElseThrow(() -> new AssertionError("Attendee not found"));
        /*
         * The actual bug:
         * Check that participant status was reset for exception, too
         */
        assertThat(updatedAttendee.getPartStat(), is("NEEDS-ACTION"));
        /*
         * Receive notification as attendee
         */
        ITipUtil.receiveNotification(testUser2.getApiClient(), testUser.getLogin(), subject);
    }

    private static EventData getSecondOccurrence(EventManager manager, EventData event) throws ApiException {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        Date from = CalendarUtils.truncateTime(new Date(), timeZone);
        Date until = CalendarUtils.add(from, Calendar.DATE, 7, timeZone);
        List<EventData> occurrences = manager.getAllEvents(null, from, until, true);
        occurrences = occurrences.stream().filter(x -> x.getId().equals(event.getId())).collect(Collectors.toList());

        return occurrences.get(2);
    }

    private static EventData getException(EventManager manager, String eventId) throws ApiException {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        Date from = CalendarUtils.truncateTime(new Date(), timeZone);
        Date until = CalendarUtils.add(from, Calendar.DATE, 7, timeZone);
        List<EventData> occurrences = manager.getAllEvents(null, from, until, true);
        occurrences = occurrences.stream().filter(x -> !x.getId().equals(eventId) && x.getSeriesId().equals(eventId)).collect(Collectors.toList());
        assertThat(I(occurrences.size()), is(I(1)));
        return occurrences.get(0);
    }

}
