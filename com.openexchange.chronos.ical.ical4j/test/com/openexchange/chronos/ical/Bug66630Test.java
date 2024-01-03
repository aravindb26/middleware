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
 * {@link Bug66630Test}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug66630Test extends ICalTest {

    @Test
    public void testImportMalformedDtStamp() throws Exception {
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" +
            "VERSION:2.0" + "\r\n" +
            "PRODID:https://mustermann" + "\r\n" +
            "CALSCALE:GREGORIAN" + "\r\n" +
            "METHOD:PUBLISH" + "\r\n" +
            "BEGIN:VTIMEZONE" + "\r\n" +
            "TZID:Europe/Berlin" + "\r\n" +
            "BEGIN:DAYLIGHT" + "\r\n" +
            "TZOFFSETFROM:+0100" + "\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" +
            "DTSTART:19810329T020000" + "\r\n" +
            "TZNAME:GMT+2" + "\r\n" +
            "TZOFFSETTO:+0200" + "\r\n" +
            "END:DAYLIGHT" + "\r\n" +
            "BEGIN:STANDARD" + "\r\n" +
            "TZOFFSETFROM:+0200" + "\r\n" +
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" +
            "DTSTART:19961027T030000" + "\r\n" +
            "TZNAME:GMT+1" + "\r\n" +
            "TZOFFSETTO:+0100" + "\r\n" +
            "END:STANDARD" + "\r\n" +
            "END:VTIMEZONE" + "\r\n" +
            "BEGIN:VEVENT" + "\r\n" +
            "UID:14529846_2972_belbo_de" + "\r\n" +
            "ORGANIZER;CN=\"Testet - Musterstraße\":MAILTO:mustermann@mailbox.org" + "\r\n" +
            "LOCATION:Musterstr. 7, 20255, Hamburg" + "\r\n" +
            "SUMMARY:Termin Tester" + "\r\n" +
            "DESCRIPTION:Termin bei Tester" + "\r\n" +
            "CLASS:PRIVATE" + "\r\n" +
            "DTSTART;TZID=Europe/Berlin:20190925T150000" + "\r\n" +
            "DTEND;TZID=Europe/Berlin:20190925T153000" + "\r\n" +
            "DTSTAMP;TZID=Europe/Berlin:20190814T193100" + "\r\n" +
            "END:VEVENT" + "\r\n" +
            "END:VCALENDAR"             ; // @formatter:on
        Event event = importEvent(iCal);
        assertTrue(event.containsDtStamp() && 0L < event.getDtStamp());
    }

}
