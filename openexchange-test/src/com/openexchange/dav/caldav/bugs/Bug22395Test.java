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

package com.openexchange.dav.caldav.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.dmfs.rfc5545.recur.RecurrenceRuleIterator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.dav.caldav.tests.chronos.ChronosCaldavTest;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ChronosUpdatesResponse;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link Bug22395Test} - Change exceptions created in iCal client appear one day off
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug22395Test extends ChronosCaldavTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDateOfChangeExceptions(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        String collectionName = encodeFolderID(getDefaultFolder());
        SyncToken syncToken = new SyncToken(fetchSyncToken(collectionName));
        /*
         * create appointment series on server
         */
        Long clientLastModified = null;
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        List<EventData> eventDatas = new ArrayList<EventData>();
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(TimeTools.D("Tomorrow at midnight", timeZone));
        for (int i = 0; i < 24; i++) {
            EventData eventData = new EventData();
            eventData.setUid(randomUID());
            eventData.setSummary("Series " + i);
            eventData.setStartDate(DateTimeUtil.getDateTime(calendar));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            eventData.setEndDate(DateTimeUtil.getDateTime(calendar));
            eventData.setRrule("FREQ=DAILY");
            ChronosCalendarResultResponse response = chronosApi.createEventBuilder()
                .withExpand(Boolean.FALSE).withFolder(defaultFolderId).withEventData(eventData).withCheckConflicts(Boolean.FALSE).execute();
            assertNull(response.getError());
            assertTrue(null != response.getData() && null != response.getData().getCreated() && 1 == response.getData().getCreated().size());
            eventDatas.add(response.getData().getCreated().get(0));
            clientLastModified = response.getTimestamp();
        }
        /*
         * verify appointment series on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken, collectionName).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(collectionName, eTags.keySet());
        for (EventData eventData : eventDatas) {
            ICalResource iCalResource = assertContains(eventData.getUid(), calendarData);
            assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
            assertEquals(eventData.getSummary(), iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
            assertEquals(eventData.getStartDate().getValue(), iCalResource.getVEvent().getPropertyValue("DTSTART"), "DTSTART wrong");
            assertEquals(eventData.getStartDate().getTzid(), iCalResource.getVEvent().getProperty("DTSTART").getAttribute("TZID"), "DTSTART wrong");
            assertEquals(eventData.getEndDate().getValue(), iCalResource.getVEvent().getPropertyValue("DTEND"), "DTEND wrong");
            assertEquals(eventData.getEndDate().getTzid(), iCalResource.getVEvent().getProperty("DTEND").getAttribute("TZID"), "DTEND wrong");
        }
        /*
         * create exceptions on client (3rd occurrence in the series)
         */
        int occurrence = 3;
        for (ICalResource iCalResource : calendarData) {
            Component exception = new Component(ICalResource.VEVENT);
            exception.setProperty("CREATED", formatAsUTC(new Date()));
            exception.setProperty("UID", iCalResource.getVEvent().getUID());
            Date exceptionStart = iCalResource.getVEvent().getDTStart();
            RecurrenceRuleIterator iterator = CalendarUtils.initRecurrenceRule(iCalResource.getVEvent().getPropertyValue("RRULE")).iterator(exceptionStart.getTime(), timeZone);
            for (int i = 0; i < occurrence; i++) {
                exceptionStart = new Date(iterator.nextMillis());
            }
            Property dtStart = iCalResource.getVEvent().getProperty("DTSTART");
            exception.setProperty("DTSTART", null == dtStart.getAttribute("TZID") ? formatAsUTC(exceptionStart) :
                format(exceptionStart, dtStart.getAttribute("TZID")), dtStart.getAttributes());
            Date exceptionEnd = CalendarUtils.add(exceptionStart, Calendar.HOUR_OF_DAY, 1, timeZone);
            Property dtEnd = iCalResource.getVEvent().getProperty("DTEND");
            exception.setProperty("DTEND", null == dtEnd.getAttribute("TZID") ? formatAsUTC(exceptionEnd) :
                format(exceptionEnd, dtEnd.getAttribute("TZID")), dtEnd.getAttributes());
            exception.setProperty("TRANSP", "OPAQUE");
            exception.setProperty("SUMMARY", iCalResource.getVEvent().getPropertyValue("SUMMARY") + "_edit");
            exception.setProperty("DTSTAMP", formatAsUTC(new Date()));
            exception.setProperty("SEQUENCE", "3");
            exception.setProperty("RECURRENCE-ID", null == dtStart.getAttribute("TZID") ? formatAsUTC(exceptionStart) :
                format(exceptionStart, dtStart.getAttribute("TZID")), dtStart.getAttributes());
            iCalResource.addComponent(exception);
            assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        }
        /*
         * verify exceptions on server
         */
        ChronosUpdatesResponse updatesResponse = chronosApi.getUpdatesBuilder()
            .withExpand(Boolean.FALSE)
            .withFolder(defaultFolderId)
            .withTimestamp(clientLastModified)
            .withRangeStart(formatAsUTC(TimeTools.D("beginning of last month")))
            .withRangeEnd(formatAsUTC(TimeTools.D("end of next month")))
            .withFields("id,seriesId,uid,summary,startDate,endDate,recurrenceId")
        .execute();
        assertNull(updatesResponse.getError());
        assertTrue(null != updatesResponse.getData() && null != updatesResponse.getData().getNewAndModified());
        for (EventData eventData : eventDatas) {
            Date expectedStart = new Date(org.dmfs.rfc5545.DateTime.parse(timeZone, eventData.getStartDate().getValue()).getTimestamp());
            RecurrenceRuleIterator iterator = CalendarUtils.initRecurrenceRule(eventData.getRrule()).iterator(expectedStart.getTime(), timeZone);
            for (int i = 0; i < occurrence; i++) {
                expectedStart = new Date(iterator.nextMillis());
            }
            Date expectedEnd = CalendarUtils.add(expectedStart, Calendar.HOUR_OF_DAY, 1, timeZone);
            EventData exception = null;
            for (EventData updatedEventData : updatesResponse.getData().getNewAndModified()) {
                if (false == updatedEventData.getId().equals(updatedEventData.getSeriesId()) && eventData.getUid().equals(updatedEventData.getUid())) {
                    exception = updatedEventData;
                }
            }
            assertNotNull(exception,"Exception not found");
            assertEquals(eventData.getSummary() + "_edit", exception.getSummary(), "Title wrong");
            assertEquals(expectedStart.getTime(), DateTimeUtil.parseDateTime(exception.getStartDate()).getTime(), "Start date wrong");
            assertEquals(expectedEnd.getTime(), DateTimeUtil.parseDateTime(exception.getEndDate()).getTime(), "End date wrong");
        }
        /*
         * verify appointment series on client
         */
        eTags = super.syncCollection(syncToken, collectionName).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        calendarData = super.calendarMultiget(collectionName, eTags.keySet());
        for (EventData eventData : eventDatas) {

            Date expectedStart = new Date(org.dmfs.rfc5545.DateTime.parse(timeZone, eventData.getStartDate().getValue()).getTimestamp());
            RecurrenceRuleIterator iterator = CalendarUtils.initRecurrenceRule(eventData.getRrule()).iterator(expectedStart.getTime(), timeZone);
            for (int i = 0; i < occurrence; i++) {
                expectedStart = new Date(iterator.nextMillis());
            }
            Date expectedEnd = CalendarUtils.add(expectedStart, Calendar.HOUR_OF_DAY, 1, timeZone);

            ICalResource iCalResource = assertContains(eventData.getUid(), calendarData);
            assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
            assertEquals(2, iCalResource.getVEvents().size(), "No exception found in iCal");
            for (Component vEvent : iCalResource.getVEvents()) {
                Date recurrenceID = vEvent.getRecurrenceID();
                if (null != recurrenceID) {
                    // exception
                    assertEquals(eventData.getSummary() + "_edit", vEvent.getSummary(), "SUMMARY wrong");
                    assertEquals(DateTimeUtil.getDateTime(timeZone.getID(), expectedStart.getTime()).getValue(), vEvent.getPropertyValue("DTSTART"), "DTSTART wrong");
                    assertEquals(timeZone.getID(), vEvent.getProperty("DTSTART").getAttribute("TZID"), "DTSTART wrong");
                    assertEquals(DateTimeUtil.getDateTime(timeZone.getID(), expectedEnd.getTime()).getValue(), vEvent.getPropertyValue("DTEND"), "DTEND wrong");
                    assertEquals(timeZone.getID(), vEvent.getProperty("DTEND").getAttribute("TZID"), "DTEND wrong");
                    assertEquals(DateTimeUtil.getDateTime(timeZone.getID(), expectedStart.getTime()).getValue(), vEvent.getPropertyValue("RECURRENCE-ID"), "RECURRENCE-ID wrong");
                    assertEquals(timeZone.getID(), vEvent.getProperty("RECURRENCE-ID").getAttribute("TZID"), "RECURRENCE-ID wrong");
                } else {
                    // master
                    assertEquals(eventData.getSummary(), vEvent.getSummary(), "SUMMARY wrong");
                    assertEquals(eventData.getStartDate().getValue(), vEvent.getPropertyValue("DTSTART"), "DTSTART wrong");
                    assertEquals(eventData.getStartDate().getTzid(), vEvent.getProperty("DTSTART").getAttribute("TZID"), "DTSTART wrong");
                    assertEquals(eventData.getEndDate().getValue(), vEvent.getPropertyValue("DTEND"), "DTEND wrong");
                    assertEquals(eventData.getEndDate().getTzid(), vEvent.getProperty("DTEND").getAttribute("TZID"), "DTEND wrong");
                }
            }
        }
    }

}
