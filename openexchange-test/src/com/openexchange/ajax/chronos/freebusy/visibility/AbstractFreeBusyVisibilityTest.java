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

package com.openexchange.ajax.chronos.freebusy.visibility;

import static com.openexchange.ajax.chronos.util.DateTimeUtil.formatZuluDate;
import static com.openexchange.ajax.chronos.util.DateTimeUtil.getDateTime;
import static com.openexchange.ajax.chronos.util.DateTimeUtil.parseDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.chronos.AbstractSecondUserChronosTest;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.ChronosConflictDataRaw;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponse;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponseData;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FreeBusyBody;
import com.openexchange.testing.httpclient.models.FreeBusyTime;
import com.openexchange.testing.httpclient.models.JSlobData;
import com.openexchange.testing.httpclient.models.JSlobsResponse;
import com.openexchange.testing.httpclient.models.Warning;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import com.openexchange.time.TimeTools;

/**
 * {@link AbstractFreeBusyVisibilityTest} - base class for free/busy visibility tests
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public abstract class AbstractFreeBusyVisibilityTest extends AbstractSecondUserChronosTest {

    /** The JSlob key of the free/busy visibility setting */
    protected static final String FREE_BUSY_VISIBILITY = "freeBusyVisibility";

    /**
     * Sets a certain value for the "freeBusyVisibility" JSlob entry, verifying that it was oerderly set afterwards.
     * 
     * @param jslobApi The underlying JSlob API to use
     * @param value The value to set
     */
    public static void setAndCheckFreeBusyVisibility(JSlobApi jslobApi, String value) throws ApiException {
        CommonResponse response = jslobApi.setJSlob(Collections.singletonMap("chronos", Collections.singletonMap(FREE_BUSY_VISIBILITY, value)), "io.ox/calendar", null);

        assertNotNull(response, "Response missing!");
        assertNull(response.getError());
        JSlobsResponse jSlobsResponse = jslobApi.getJSlobList(Collections.singletonList("io.ox/calendar"), null);
        assertNotNull(jSlobsResponse, "Response missing!");
        assertNull(jSlobsResponse.getError());
        List<JSlobData> data = jSlobsResponse.getData();
        assertNotNull(data);

        for (JSlobData jSlobData : data) {
            Assertions.assertTrue(jSlobData.getTree().toString().contains(FREE_BUSY_VISIBILITY + "=" + value));
        }
    }

    /**
     * The tested values for the user's configured free/busy visibility.
     * 
     * @return The test data
     */
    public static Stream<String> testedFreeBusyVisibilityValues() {
        return Stream.of("none", "internal-only", "all");
    }

    /**
     * Asserts that the supplied conflict data contains an item representing certain event data.
     * 
     * @param conflicts The conflicts data as taken from an API response
     * @param eventData The event data to expect a matching conflict slot for
     * @return The matching conflict, raising a test failure otherwise
     */
    protected static ChronosConflictDataRaw assertMatchingConflict(List<ChronosConflictDataRaw> conflicts, EventData eventData) throws Exception {
        assertNotNull(conflicts);
        ChronosConflictDataRaw matchingConflictData = null;
        for (ChronosConflictDataRaw conflictData : conflicts) {
            if (null != conflictData.getEvent() && 
                parseDateTime(conflictData.getEvent().getStartDate()).getTime() == parseDateTime(eventData.getStartDate()).getTime() &&
                parseDateTime(conflictData.getEvent().getEndDate()).getTime() == parseDateTime(eventData.getEndDate()).getTime())  {
                matchingConflictData = conflictData;
                break;
            }
        }
        assertNotNull(matchingConflictData);
        return matchingConflictData;
    }

    /**
     * Asserts that the supplied free/busy data contains a slot representing certain event data.
     * 
     * @param freeBusyData The free/busy data as taken from an API response
     * @param eventData The event data to expect a matching free/busy slot for
     * @return The matching free/busy time, raising a test failure otherwise
     */
    protected static FreeBusyTime assertMatchingFreeBusyTime(ChronosFreeBusyResponseData freeBusyData, EventData eventData) throws Exception {
        assertNotNull(freeBusyData.getFreeBusyTime());
        FreeBusyTime matchingFreeBusyTime = null;
        for (FreeBusyTime freeBusyTime : freeBusyData.getFreeBusyTime()) {
            if (freeBusyTime.getStartTime().longValue() == parseDateTime(eventData.getStartDate()).getTime() &&
                freeBusyTime.getEndTime().longValue() == parseDateTime(eventData.getEndDate()).getTime())  {
                matchingFreeBusyTime = freeBusyTime;
                break;
            }
        }
        assertNotNull(matchingFreeBusyTime);
        return matchingFreeBusyTime;
    }

    /**
     * Asserts that a warning with a specific error code is contained in the supplied free/busy data.
     * 
     * @param freeBusyData The free/busy data as taken from an API response
     * @param expectedErrorCode The expected error code
     * @return The matching warning, raising a test failure otherwise
     */
    protected static Warning assertWarning(ChronosFreeBusyResponseData freeBusyData, String expectedErrorCode) {
        assertNotNull(freeBusyData.getWarnings());
        for (Warning warning : freeBusyData.getWarnings()) {
            if (expectedErrorCode.equals(warning.getCode())) {
                return warning;
            }
        }
        org.junit.jupiter.api.Assertions.fail();
        return null;
    }

    /**
     * Performs a free/busy query in a timeframe of a specific event for one or more attendees.
     * 
     * @param apiClient The underlying API client to use
     * @param eventData The event data to derive the queried interval from
     * @param attendees The attendees to query
     * @return The free/busy response data
     */
    protected static List<ChronosFreeBusyResponseData> queryFreeBusy(ApiClient apiClient, EventData eventData, Attendee... attendees) throws Exception {
        return queryFreeBusy(apiClient, parseDateTime(eventData.getStartDate()), parseDateTime(eventData.getEndDate()), attendees);
    }

    /**
     * Performs a free/busy query in a certain timeframe for one or more attendees.
     * 
     * @param apiClient The underlying API client to use
     * @param from The start time of the queried interval
     * @param until The until time of the queried interval
     * @param attendees The attendees to query
     * @return The free/busy response data
     */
    protected static List<ChronosFreeBusyResponseData> queryFreeBusy(ApiClient apiClient, Date from, Date until, Attendee... attendees) throws Exception {
        ChronosFreeBusyResponse freeBusyResponse = new ChronosApi(apiClient).freebusyBuilder()
            .withFrom(formatZuluDate(from))
            .withUntil(formatZuluDate(until))
            .withFreeBusyBody(new FreeBusyBody().attendees(Arrays.asList(attendees)))
        .execute();
        assertNull(freeBusyResponse.getError());
        List<ChronosFreeBusyResponseData> data = freeBusyResponse.getData();
        assertNotNull(data);
        assertEquals(attendees.length, data.size() );
        return data;
    }
    
    /**
     * Prepares a single event for testing, taking over the passed properties.
     * 
     * @param folderId The folder identifier to apply
     * @param organizer The organizer to apply
     * @param attendees The attendees to apply
     * @return The prepared event data
     */
    protected static EventData prepareEventData(String folderId, CalendarUser organizer, Attendee... attendees) {
        TimeZone timeZone = TimeZone.getTimeZone("America/Fortaleza");
        EventData eventData = new EventData();
        eventData.setFolder(folderId);
        eventData.setSummary(UUIDs.getUnformattedStringFromRandom());
        eventData.setLocation(UUIDs.getUnformattedStringFromRandom());
        eventData.setStartDate(getDateTime(timeZone.getID(), TimeTools.D("Tomorrow at 10 am", timeZone).getTime()));
        eventData.setEndDate(getDateTime(timeZone.getID(), TimeTools.D("Tomorrow at 11 am", timeZone).getTime()));
        eventData.setOrganizer(organizer);
        if (null != attendees) {
            eventData.setAttendees(java.util.Arrays.asList(attendees));
        }
        return eventData;
    }

}
