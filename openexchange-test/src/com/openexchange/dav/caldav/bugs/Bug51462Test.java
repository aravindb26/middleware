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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.UserAgents;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.java.util.TimeZones;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug51462Test}
 *
 * Thunderbird Lightning only: Full day Appointment can not be set to regular Apppointment
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug51462Test extends CalDAVTest {

    @Override
    protected String getDefaultUserAgent() {
        return UserAgents.LIGHTNING_4_7_7;
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testRemoveAllDay(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create all-day event at client
         */
        String uid = randomUID();

        Calendar calendar = Calendar.getInstance(TimeZones.UTC);
        calendar.setTime(TimeTools.D("tomorrow at midnight", TimeZones.UTC));
        Date start = calendar.getTime();
        calendar.add(Calendar.DATE, 1);
        Date end = calendar.getTime();
        String iCal =
            "BEGIN:VCALENDAR\r\n" +
            "PRODID:-//Mozilla.org/NONSGML Mozilla Calendar V1.1//EN\r\n" +
            "VERSION:2.0\r\n" +
            "BEGIN:VEVENT\r\n" +
            "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
            "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "UID:" + uid + "\r\n" +
            "SUMMARY:Bug51462Test\r\n" +
            "DTSTART;VALUE=DATE:" + formatAsDate(start) + "\r\n" +
            "DTEND;VALUE=DATE:" + formatAsDate(end) + "\r\n" +
            "DTEND;VALUE=DATE:20170208\r\n" +
            "TRANSP:TRANSPARENT\r\n" +
            "CLASS:PUBLIC\r\n" +
            "END:VEVENT\r\n" +
            "END:VCALENDAR\r\n"
        ;
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertTrue(appointment.getFullTime(), "not fulltime");
        /*
         * verify event on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals("DATE", iCalResource.getVEvent().getProperty("DTSTART").getAttribute("VALUE"), "DTSTART wrong");
        assertEquals("DATE", iCalResource.getVEvent().getProperty("DTEND").getAttribute("VALUE"), "DTEND wrong");
        /*
         * update event on client, set start- and endtime
         */
        Date newStart = TimeTools.D("tomorrow at 16:00", TimeZone.getTimeZone("Europe/Berlin"));
        Date newEnd = TimeTools.D("tomorrow at 17:00", TimeZone.getTimeZone("Europe/Berlin"));
        iCalResource.getVEvent().setProperty("DTSTART", format(newStart, "Europe/Berlin"), Collections.singletonMap("TZID", "Europe/Berlin"));
        iCalResource.getVEvent().setProperty("DTEND", format(newEnd, "Europe/Berlin"), Collections.singletonMap("TZID", "Europe/Berlin"));
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify event on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNull(iCalResource.getVEvent().getProperty("DTSTART").getAttribute("VALUE"), "DTSTART wrong");
        assertNull(iCalResource.getVEvent().getProperty("DTEND").getAttribute("VALUE"), "DTEND wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertFalse(appointment.getFullTime(), "still fulltime");
    }

}
