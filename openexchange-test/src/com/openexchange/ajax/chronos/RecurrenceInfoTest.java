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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.dmfs.rfc5545.DateTime;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosRecurrenceInfoResponse;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.RecurrenceInfo;
import com.openexchange.time.TimeTools;

/**
 * {@link RecurrenceInfoTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class RecurrenceInfoTest extends AbstractSecondUserChronosTest {
    
    @Test
    public void testNonOverridden() throws Exception {
        /*
         * generate event series in user's personal folder & create it
         */
        EventData eventData = prepareEventSeriesData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getUserAttendee(testUser2));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * get & verify recurrence information for 3rd occurrence of series
         */
        String recurrenceId = getRecurrenceIdOnDay(createdEvent.getStartDate(), 3);
        RecurrenceInfo recurrenceInfo = getRecurrenceInfo(defaultUserApi, defaultFolderId, createdEvent.getId(), recurrenceId);
        assertEquals(Boolean.FALSE, recurrenceInfo.getRescheduled());
        assertEquals(Boolean.FALSE, recurrenceInfo.getOverridden());
        assertNotNull(recurrenceInfo.getMasterEvent());
        assertNotNull(recurrenceInfo.getRecurrenceEvent());
        assertEquals(recurrenceId, recurrenceInfo.getRecurrenceEvent().getRecurrenceId());
        assertEquals(recurrenceInfo.getMasterEvent().getId(), recurrenceInfo.getRecurrenceEvent().getId());
    }

    @Test
    public void testOverridden() throws Exception {
        /*
         * generate event series in user's personal folder & create it
         */
        EventData eventData = prepareEventSeriesData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getUserAttendee(testUser2));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * create overridden but not rescheduled change exception by changing participation status on 3rd occurrence
         */
        eventManager2.setLastTimeStamp(eventManager.getLastTimeStamp());
        String recurrenceId = getRecurrenceIdOnDay(createdEvent.getStartDate(), 3);
        AttendeeAndAlarm attendeeData = new AttendeeAndAlarm().attendee(
            new Attendee().cuType(CuTypeEnum.INDIVIDUAL).entity(I(testUser2.getUserId())).partStat("DECLINED"));
        eventManager2.updateAttendee(createdEvent.getId(), recurrenceId, defaultFolderId2, attendeeData, false);
        /*
         * get & verify recurrence information for 3rd occurrence of series
         */
        RecurrenceInfo recurrenceInfo = getRecurrenceInfo(defaultUserApi, defaultFolderId, createdEvent.getId(), recurrenceId);
        assertEquals(Boolean.FALSE, recurrenceInfo.getRescheduled());
        assertEquals(Boolean.TRUE, recurrenceInfo.getOverridden());
        assertNotNull(recurrenceInfo.getMasterEvent());
        assertNotNull(recurrenceInfo.getRecurrenceEvent());
        assertEquals(recurrenceId, recurrenceInfo.getRecurrenceEvent().getRecurrenceId());
        assertNotEquals(recurrenceInfo.getMasterEvent().getId(), recurrenceInfo.getRecurrenceEvent().getId());
    }
    
    @Test
    public void testRescheduled() throws Exception {
        /*
         * generate event series in user's personal folder & create it
         */
        EventData eventData = prepareEventSeriesData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser), getUserAttendee(testUser2));
        EventData createdEvent = eventManager.createEvent(eventData, true);
        /*
         * create rescheduled change exception by changing location on 3rd occurrence
         */
        String recurrenceId = getRecurrenceIdOnDay(createdEvent.getStartDate(), 3);
        EventData eventUpdate = new EventData().id(createdEvent.getId()).folder(createdEvent.getFolder()).recurrenceId(recurrenceId)
            .location(UUIDs.getUnformattedStringFromRandom());
        eventManager.updateOccurenceEvent(eventUpdate, recurrenceId, true);
        /*
         * get & verify recurrence information for 3rd occurrence of series
         */
        RecurrenceInfo recurrenceInfo = getRecurrenceInfo(defaultUserApi, defaultFolderId, createdEvent.getId(), recurrenceId);
        assertEquals(Boolean.TRUE, recurrenceInfo.getRescheduled());
        assertEquals(Boolean.TRUE, recurrenceInfo.getOverridden());
        assertNotNull(recurrenceInfo.getMasterEvent());
        assertNotNull(recurrenceInfo.getRecurrenceEvent());
        assertEquals(recurrenceId, recurrenceInfo.getRecurrenceEvent().getRecurrenceId());
        assertNotEquals(recurrenceInfo.getMasterEvent().getId(), recurrenceInfo.getRecurrenceEvent().getId());
    }
    
    private static RecurrenceInfo getRecurrenceInfo(UserApi userApi, String folderId, String eventId, String recurrenceId) throws ApiException {
        ChronosRecurrenceInfoResponse recurrenceInfoResponse = userApi.getChronosApi().getRecurrenceBuilder().withFolder(folderId).withId(eventId).withRecurrenceId(recurrenceId).execute();
        assertNull(recurrenceInfoResponse.getErrorDesc(), recurrenceInfoResponse.getError());
        assertNotNull(recurrenceInfoResponse.getData());
        return recurrenceInfoResponse.getData();
    }

    private static String getRecurrenceIdOnDay(DateTimeData startDate, int day) throws Exception {
        Date date = DateTimeUtil.parseDateTime(startDate);
        TimeZone timeZone = TimeZone.getTimeZone(null != startDate.getTzid() ? startDate.getTzid() : "UTC");
        Date recurrenceDate = CalendarUtils.add(date, Calendar.DATE, day, timeZone);
        return CalendarUtils.encode(new DateTime(timeZone, recurrenceDate.getTime()));
    }

    private static EventData prepareEventSeriesData(String folderId, CalendarUser organizer, Attendee... attendees) {
        TimeZone timeZone = TimeZone.getTimeZone("Asia/Almaty");
        EventData eventData = new EventData();
        eventData.setFolder(folderId);
        eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        eventData.setLocation(UUIDs.getUnformattedStringFromRandom());
        eventData.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), TimeTools.D("Next friday at 10 am", timeZone).getTime()));
        eventData.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), TimeTools.D("Next friday at 11 am", timeZone).getTime()));
        eventData.setOrganizer(organizer);
        eventData.setRrule("FREQ=DAILY;COUNT=10");
        if (null != attendees) {
            eventData.setAttendees(java.util.Arrays.asList(attendees));
        }
        return eventData;
    }

}
