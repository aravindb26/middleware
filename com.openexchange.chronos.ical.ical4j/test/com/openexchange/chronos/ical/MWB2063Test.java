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

package com.openexchange.chronos.ical;

import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.openexchange.chronos.Event;

/**
 * {@link MWB2063Test}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MWB2063Test extends ICalTest {

    @Test
    public void testImportMalformedDtStamp() throws Exception {
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "METHOD:PUBLISH" + "\r\n" +
            "PRODID:ALLRIS" + "\r\n" +
            "CONTACT:allris@example.com" + "\r\n" +
            "X-WR-CALNAME:Sitzungstermine Sitzungsdienst" + "\r\n" +
            "X-WR-CALDESC:Internetkalender abonniert aus ALLRIS" + "\r\n" +
            "X-WR-TIMEZONE:Europe/Berlin" + "\r\n" +
            "X-PUBLISHED-TTL:PT60M" + "\r\n" +
            "BEGIN:VTIMEZONE" + "\r\n" +
            "TZID:Europe/Berlin" + "\r\n" +
            "X-LIC-LOCATION:Europe/Berlin" + "\r\n" +
            "BEGIN:DAYLIGHT" + "\r\n" +
            "TZOFFSETFROM:+0100" + "\r\n" +
            "TZOFFSETTO:+0200" + "\r\n" +
            "TZNAME:CEST" + "\r\n" +
            "DTSTART:19700329T020000" + "\r\n" +
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=3" + "\r\n" +
            "END:DAYLIGHT" + "\r\n" +
            "BEGIN:STANDARD" + "\r\n" +
            "TZOFFSETFROM:+0200" + "\r\n" +
            "TZOFFSETTO:+0100" + "\r\n" +
            "TZNAME:CET" + "\r\n" +
            "DTSTART:19701025T030000" + "\r\n" +
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=10" + "\r\n" +
            "END:STANDARD" + "\r\n" +
            "END:VTIMEZONE" + "\r\n" +
            "CALSCALE:GREGORIAN" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "UID:ALLRIS-Sitzung-1004064" + "\r\n" +
            "PRIORITY:2" + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" +
            "X-APPLE-CALENDAR-COLOR:#000000" + "\r\n" +
            "X-OUTLOOK-COLOR:#000000" + "\r\n" +
            "X-FUNAMBOL-COLOR:#000000" + "\r\n" +
            "X-LOTUS-CHARSET:UTF-8" + "\r\n" +
            "DTSTAMP;TZID=Europe/Berlin:20230223T163947" + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:20231211T160000" + "\r\n" +
            "DTEND;TZID=Europe/Berlin:20231212T000000" + "\r\n" +
            "X-MICROSOFT-CDO-ALLDAYEVENT:FALSE" + "\r\n" +
            "SUMMARY;LANGUAGE=de:Rat der Stadt Example" + "\r\n" +
            "LOCATION;LANGUAGE=de:Ratssaal" + "\r\n" +
            "DESCRIPTION;LANGUAGE=de:Exportiert am 23.02.2023\n\nSitzungskalender in ALLRIS net:\nhttps://www.example.com/public/si020?DD=11&MM=12&YY=2023" + "\r\n" +
            "URL:https://www.example.com/public/si020?DD=11&MM=12&YY=2023" + "\r\n" +
            "" + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR" + "\r\n"
            ; // @formatter:on
        Event event = importEvent(iCal);
        assertTrue(event.containsDtStamp() && 0L < event.getDtStamp());
    }

}
