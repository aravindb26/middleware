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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.tests.chronos.ChronosCaldavTest;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ChronosUpdatesResponse;
import com.openexchange.testing.httpclient.models.EventData;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug22451Test} - Wrong until date after exporting and importing recurring appointment
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug22451Test extends ChronosCaldavTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUntilDate(String authMethod) throws Exception {
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
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(TimeTools.D("3 days after midnight", TimeZone.getTimeZone("UTC")));
        Date until = calendar.getTime();
        Long clientLastModified = null;
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        List<EventData> eventDatas = new ArrayList<EventData>();
        calendar = Calendar.getInstance(timeZone);
        calendar.setTime(TimeTools.D("Tomorrow at midnight", timeZone));
        for (int i = 0; i < 24; i++) {
            EventData eventData = new EventData();
            eventData.setUid(randomUID());
            eventData.setSummary("Series " + i);
            eventData.setStartDate(DateTimeUtil.getDateTime(calendar));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            eventData.setEndDate(DateTimeUtil.getDateTime(calendar));
            eventData.setRrule("FREQ=DAILY;UNTIL=" + formatAsUTC(until));
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
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
            assertEquals(eventData.getRrule(), iCalResource.getVEvent().getPropertyValue("RRULE"), "RRULE wrong");
        }
        /*
         * update appointments on client
         */
        for (ICalResource iCalResource : calendarData) {
            iCalResource.getVEvent().setSummary(iCalResource.getVEvent().getSummary() + "_edit");
            assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        }
        /*
         * verify appointments on server
         */
        ChronosUpdatesResponse updatesResponse = chronosApi.getUpdatesBuilder()
            .withExpand(Boolean.FALSE)
            .withFolder(defaultFolderId)
            .withTimestamp(clientLastModified)
            .withRangeStart(formatAsUTC(TimeTools.D("beginning of last month")))
            .withRangeEnd(formatAsUTC(TimeTools.D("end of next month")))
            .withFields("id,seriesId,uid,summary,startDate,endDate,recurrenceId,rrule")
        .execute();
        assertNull(updatesResponse.getError());
        assertTrue(null != updatesResponse.getData() && null != updatesResponse.getData().getNewAndModified());
        for (EventData eventData : eventDatas) {
            EventData updatedEvent = null;
            for (EventData updatedEventData : updatesResponse.getData().getNewAndModified()) {
                if (updatedEventData.getId().equals(eventData.getId())) {
                    updatedEvent = updatedEventData;
                }
            }
            assertNotNull(updatedEvent, "Event not found");
            assertEquals(eventData.getSummary() + "_edit", updatedEvent.getSummary(), "Summary wrong");
            assertEquals(eventData.getRrule(), updatedEvent.getRrule(), "RRule wrong");
        }
        /*
         * verify appointment series on client
         */
        eTags = super.syncCollection(syncToken, collectionName).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        calendarData = super.calendarMultiget(collectionName, eTags.keySet());
        for (EventData eventData : eventDatas) {
            ICalResource iCalResource = assertContains(eventData.getUid(), calendarData);
            assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
            assertEquals(eventData.getSummary() + "_edit", iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
            assertEquals(eventData.getStartDate().getValue(), iCalResource.getVEvent().getPropertyValue("DTSTART"), "DTSTART wrong");
            assertEquals(eventData.getStartDate().getTzid(), iCalResource.getVEvent().getProperty("DTSTART").getAttribute("TZID"), "DTSTART wrong");
            assertEquals(eventData.getEndDate().getValue(), iCalResource.getVEvent().getPropertyValue("DTEND"), "DTEND wrong");
            assertEquals(eventData.getEndDate().getTzid(), iCalResource.getVEvent().getProperty("DTEND").getAttribute("TZID"), "DTEND wrong");
            assertEquals(eventData.getRrule(), iCalResource.getVEvent().getPropertyValue("RRULE"), "RRULE wrong");
        }
    }

}
