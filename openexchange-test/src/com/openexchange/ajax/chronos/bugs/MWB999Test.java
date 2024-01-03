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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractICalCalendarProviderTest;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link MWB999Test}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class MWB999Test extends AbstractICalCalendarProviderTest {

    @Test
    public void testSameEndDate() throws ApiException {
        /*
         * mock iCalendar feed
         */
        String mockediCalFeed = // @formatter:off
            "BEGIN:VCALENDAR\n" +
            "PRODID:-//NOLIS GmbH//NOLIS Erinnerung//DE\n" +
            "VERSION:2.0\n" +
            "METHOD:PUBLISH\n" +
            "X-WR-CALNAME:Abfuhrkalender\n" +
            "X-WR-TIMEZONE:Europe/Berlin\n" +
            "X-VCALENDER-VERSION:3.5\n" +
            "BEGIN:VEVENT\n" +
            "DTSTART;VALUE=DATE:20210108\n" +
            "SUMMARY:Altpapier\n" +
            "PRIORITY:0\n" +
            "DTEND;VALUE=DATE:20210108\n" +
            "UID:c171876c93e4eb8c746fcf0288621b7f\n" +
            "X-ID:900010778\n" +
            "DTSTAMP:20210108T010000Z\n" +
            "CLASS:PUBLIC\n" +
            "TRANSP:TRANSPARENT\n" +
            "STATUS:CONFIRMED\n" +
            "X-NOLISEXPORT:1\n" +
            "X-EXTRAKATEGORIE-ID:\n" +
            "X-EXTRAKATEGORIE:\n" +
            "BEGIN:VALARM\n" +
            "ACTION:DISPLAY\n" +
            "DESCRIPTION:Erinnerung Altpapier\n" +
            "TRIGGER:-PT720M\n" +
            "END:VALARM\n" +
            "END:VEVENT\n" +
            "END:VCALENDAR\n"
        ; // @formatter:off
        String path = "/files/" + UUID.randomUUID().toString() + ".ics";
        String hostname = AJAXConfig.getProperty(AJAXConfig.Property.MOCK_HOSTNAME);
        String port = AJAXConfig.getProperty(AJAXConfig.Property.MOCK_PORT);
        String mockedFeedUri = AJAXConfig.getProperty(AJAXConfig.Property.PROTOCOL) + "://" + hostname + ":" + port + path;
        mock(path, mockediCalFeed, HttpStatus.SC_OK);

        /*
         * subscribe to mocked feed & get imported event data
         */
        String folderId = createDefaultAccount(mockedFeedUri);
        Date rangeFrom = new Date(dateToMillis("20210101T000000Z"));
        Date rangeUntil = new Date(dateToMillis("20210201T000000Z"));
        List<EventData> events = eventManager.getAllEvents(rangeFrom, rangeUntil, true, folderId);
        /*
         * verify event data
         */
        EventData importedEvent = null;
        for (EventData event : events) {
            if ("c171876c93e4eb8c746fcf0288621b7f".equals(event.getUid())) {
                importedEvent = event;
                break;
            }
        }
        assertNotNull(importedEvent, "event not imported");
        assertEquals(new DateTimeData().value("20210108"), importedEvent.getStartDate(), "start date wrong");
        assertTrue(null == importedEvent.getEndDate() || new DateTimeData().value("20210109").equals(importedEvent.getEndDate()), "end date wrong");
    }

}
