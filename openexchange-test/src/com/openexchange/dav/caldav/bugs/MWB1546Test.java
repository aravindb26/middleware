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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB1546Test}
 *
 * CalDAV issue
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1546Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testOverriddenFirstOccurrence(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create weekly event series with a delete exception date
         */
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Amsterdam");
        String uid = randomUID();
        Date start = TimeTools.D("next tuesday at 15:30", timeZone);
        Date end = TimeTools.D("next tuesday at 16:30", timeZone);
        Date deleteExceptionRecurrenceId1 = CalendarUtils.add(start, Calendar.WEEK_OF_YEAR, 1, timeZone);
        Date deleteExceptionRecurrenceId2 = CalendarUtils.add(start, Calendar.WEEK_OF_YEAR, 3, timeZone);
        Date changeExceptionStart1 = CalendarUtils.add(start, Calendar.DATE, 2, timeZone);
        Date changeExceptionEnd1 = CalendarUtils.add(end, Calendar.DATE, 2, timeZone);
        Date changeExceptionRecurrenceId1 = new Date(start.getTime());
        Date changeExceptionStart2 = CalendarUtils.add(start, Calendar.WEEK_OF_YEAR, 2, timeZone);
        Date changeExceptionEnd2 = CalendarUtils.add(start, Calendar.WEEK_OF_YEAR, 2, timeZone);
        Date changeExceptionRecurrenceId2 = CalendarUtils.add(start, Calendar.WEEK_OF_YEAR, 2, timeZone);
        String iCal = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" + 
            "VERSION:2.0" + "\r\n" + 
            "PRODID:-//Open-Xchange//7.10.6-Rev10//EN" + "\r\n" + 
            "BEGIN:VTIMEZONE" + "\r\n" + 
            "TZID:Europe/Amsterdam" + "\r\n" + 
            "LAST-MODIFIED:20220317T223602Z" + "\r\n" + 
            "TZURL:http://tzurl.org/zoneinfo-outlook/Europe/Amsterdam" + "\r\n" + 
            "X-LIC-LOCATION:Europe/Amsterdam" + "\r\n" + 
            "BEGIN:DAYLIGHT" + "\r\n" + 
            "TZNAME:CEST" + "\r\n" + 
            "TZOFFSETFROM:+0100" + "\r\n" + 
            "TZOFFSETTO:+0200" + "\r\n" + 
            "DTSTART:19700329T020000" + "\r\n" + 
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" + 
            "END:DAYLIGHT" + "\r\n" + 
            "BEGIN:STANDARD" + "\r\n" + 
            "TZNAME:CET" + "\r\n" + 
            "TZOFFSETFROM:+0200" + "\r\n" + 
            "TZOFFSETTO:+0100" + "\r\n" + 
            "DTSTART:19701025T030000" + "\r\n" + 
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" + 
            "END:STANDARD" + "\r\n" + 
            "END:VTIMEZONE" + "\r\n" + 
            "BEGIN:VEVENT" + "\r\n" + 
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" + 
            "CREATED:20220420T100731Z" + "\r\n" + 
            "DESCRIPTION:" + "\r\n" + 
            "DTSTART;TZID=Europe/Amsterdam:" + format(start, "Europe/Amsterdam") + "\r\n" +
            "DTEND;TZID=Europe/Amsterdam:" + format(end, "Europe/Amsterdam") + "\r\n" +
            "EXDATE;TZID=Europe/Amsterdam:" + format(deleteExceptionRecurrenceId1, "Europe/Amsterdam") + "\r\n" +
            "EXDATE;TZID=Europe/Amsterdam:" + format(deleteExceptionRecurrenceId2, "Europe/Amsterdam") + "\r\n" +
            "LAST-MODIFIED:20220420T100731Z" + "\r\n" + 
            "RELATED-TO;RELTYPE=X-CALENDARSERVER-RECURRENCE-SET:e8b82405-b736-4f6d-9369-377102402697" + "\r\n" + 
            "RRULE:FREQ=WEEKLY" + "\r\n" + 
            "SEQUENCE:1" + "\r\n" + 
            "STATUS:CONFIRMED" + "\r\n" + 
            "SUMMARY:MWB1546Test\r\n" +
            "TRANSP:OPAQUE" + "\r\n" + 
            "UID:" + uid + "\r\n" +
            "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC" + "\r\n" + 
            "BEGIN:VALARM" + "\r\n" + 
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z" + "\r\n" + 
            "UID:B6A752DE-1BD8-4783-8388-5013929CA1A5" + "\r\n" + 
            "ACTION:NONE" + "\r\n" + 
            "X-WR-ALARMUID:B6A752DE-1BD8-4783-8388-5013929CA1A5" + "\r\n" + 
            "X-APPLE-LOCAL-DEFAULT-ALARM:TRUE" + "\r\n" + 
            "X-APPLE-DEFAULT-ALARM:TRUE" + "\r\n" + 
            "END:VALARM" + "\r\n" + 
            "END:VEVENT" + "\r\n" + 
            "BEGIN:VEVENT" + "\r\n" + 
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" + 
            "CREATED:20220309T103926Z" + "\r\n" + 
            "DESCRIPTION:" + "\r\n" + 
            "DTSTART;TZID=Europe/Amsterdam:" + format(changeExceptionStart1, "Europe/Amsterdam") + "\r\n" +
            "DTEND;TZID=Europe/Amsterdam:" + format(changeExceptionEnd1, "Europe/Amsterdam") + "\r\n" +
            "LAST-MODIFIED:20220420T100731Z" + "\r\n" + 
            "RECURRENCE-ID;TZID=Europe/Amsterdam:" + format(changeExceptionRecurrenceId1, "Europe/Amsterdam") + "\r\n" +
            "RELATED-TO;RELTYPE=X-CALENDARSERVER-RECURRENCE-SET:e8b82405-b736-4f6d-9369-" + "\r\n" + 
            " 377102402697" + "\r\n" + 
            "SEQUENCE:2" + "\r\n" + 
            "STATUS:CONFIRMED" + "\r\n" + 
            "SUMMARY:MWB1546Test\r\n" +
            "TRANSP:OPAQUE" + "\r\n" + 
            "UID:" + uid + "\r\n" +
            "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC" + "\r\n" + 
            "BEGIN:VALARM" + "\r\n" + 
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z" + "\r\n" + 
            "UID:4B7C09C3-D585-4B82-92EA-1EDE139A8BE9" + "\r\n" + 
            "ACTION:NONE" + "\r\n" + 
            "X-WR-ALARMUID:4B7C09C3-D585-4B82-92EA-1EDE139A8BE9" + "\r\n" + 
            "X-APPLE-LOCAL-DEFAULT-ALARM:TRUE" + "\r\n" + 
            "X-APPLE-DEFAULT-ALARM:TRUE" + "\r\n" + 
            "END:VALARM" + "\r\n" + 
            "END:VEVENT" + "\r\n" + 
            "BEGIN:VEVENT" + "\r\n" + 
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "CLASS:PUBLIC" + "\r\n" + 
            "CREATED:20220309T103926Z" + "\r\n" + 
            "DESCRIPTION:" + "\r\n" + 
            "DTSTART;TZID=Europe/Amsterdam:" + format(changeExceptionStart2, "Europe/Amsterdam") + "\r\n" +
            "DTEND;TZID=Europe/Amsterdam:" + format(changeExceptionEnd2, "Europe/Amsterdam") + "\r\n" +
            "LAST-MODIFIED:20220420T100731Z" + "\r\n" + 
            "RECURRENCE-ID;TZID=Europe/Amsterdam:" + format(changeExceptionRecurrenceId2, "Europe/Amsterdam") + "\r\n" +
            "RELATED-TO;RELTYPE=X-CALENDARSERVER-RECURRENCE-SET:e8b82405-b736-4f6d-9369-" + "\r\n" + 
            " 377102402697" + "\r\n" + 
            "SEQUENCE:2" + "\r\n" + 
            "STATUS:CONFIRMED" + "\r\n" + 
            "SUMMARY:MWB1546Test\r\n" +
            "TRANSP:OPAQUE" + "\r\n" + 
            "UID:" + uid + "\r\n" +
            "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC" + "\r\n" + 
            "BEGIN:VALARM" + "\r\n" + 
            "TRIGGER;VALUE=DATE-TIME:19760401T005545Z" + "\r\n" + 
            "UID:B6BE78AE-AC7E-41C3-A122-BE4603AF0CB3" + "\r\n" + 
            "ACTION:NONE" + "\r\n" + 
            "X-WR-ALARMUID:B6BE78AE-AC7E-41C3-A122-BE4603AF0CB3" + "\r\n" + 
            "X-APPLE-LOCAL-DEFAULT-ALARM:TRUE" + "\r\n" + 
            "X-APPLE-DEFAULT-ALARM:TRUE" + "\r\n" + 
            "END:VALARM" + "\r\n" + 
            "END:VEVENT" + "\r\n" + 
            "END:VCALENDAR"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify series on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify series on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        /*
         * prepare series update
         */
        String iCalUpdate = // @formatter:off
            "BEGIN:VCALENDAR" + "\r\n" + 
            "VERSION:2.0" + "\r\n" + 
            "CALSCALE:GREGORIAN" + "\r\n" + 
            "PRODID:-//Apple Inc.//macOS 12.3.1//EN" + "\r\n" + 
            "BEGIN:VTIMEZONE" + "\r\n" + 
            "RELATED-TO;RELTYPE=X-CALENDARSERVER-RECURRENCE-SET:e8b82405-b736-4f6d-93" + "\r\n" + 
            " 69-377102402697" + "\r\n" + 
            "TZID:Europe/Amsterdam" + "\r\n" + 
            "BEGIN:DAYLIGHT" + "\r\n" + 
            "TZOFFSETFROM:+0100" + "\r\n" + 
            "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" + 
            "DTSTART:19810329T020000" + "\r\n" + 
            "TZNAME:CEST" + "\r\n" + 
            "TZOFFSETTO:+0200" + "\r\n" + 
            "END:DAYLIGHT" + "\r\n" + 
            "BEGIN:STANDARD" + "\r\n" + 
            "TZOFFSETFROM:+0200" + "\r\n" + 
            "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" + 
            "DTSTART:19961027T030000" + "\r\n" + 
            "TZNAME:CET" + "\r\n" + 
            "TZOFFSETTO:+0100" + "\r\n" + 
            "END:STANDARD" + "\r\n" + 
            "END:VTIMEZONE" + "\r\n" + 
            "BEGIN:VEVENT" + "\r\n" + 
            "TRANSP:OPAQUE" + "\r\n" + 
            "DTSTART;TZID=Europe/Amsterdam:" + format(start, "Europe/Amsterdam") + "\r\n" +
            "DTEND;TZID=Europe/Amsterdam:" + format(end, "Europe/Amsterdam") + "\r\n" +
            "EXDATE;TZID=Europe/Amsterdam:" + format(deleteExceptionRecurrenceId1, "Europe/Amsterdam") + "\r\n" +
            "EXDATE;TZID=Europe/Amsterdam:" + format(deleteExceptionRecurrenceId2, "Europe/Amsterdam") + "\r\n" +
            "RRULE:FREQ=WEEKLY;INTERVAL=1" + "\r\n" + 
            "RELATED-TO;RELTYPE=X-CALENDARSERVER-RECURRENCE-SET:e8b82405-b736-4f6d-93" + "\r\n" + 
            " 69-377102402697" + "\r\n" + 
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DESCRIPTION:" + "\r\n" + 
            "STATUS:CONFIRMED" + "\r\n" + 
            "SEQUENCE:1" + "\r\n" + 
            "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC" + "\r\n" + 
            "SUMMARY:MWB1546Test\r\n" +
            "LAST-MODIFIED:20220309T103926Z" + "\r\n" + 
            "CREATED:20220420T100731Z" + "\r\n" + 
            "END:VEVENT" + "\r\n" + 
            "BEGIN:VEVENT" + "\r\n" + 
            "TRANSP:OPAQUE" + "\r\n" + 
            "DTSTART;TZID=Europe/Amsterdam:" + format(changeExceptionStart1, "Europe/Amsterdam") + "\r\n" +
            "DTEND;TZID=Europe/Amsterdam:" + format(changeExceptionEnd1, "Europe/Amsterdam") + "\r\n" +
            "RELATED-TO;RELTYPE=X-CALENDARSERVER-RECURRENCE-SET:e8b82405-b736-4f6d-93" + "\r\n" + 
            " 69-377102402697" + "\r\n" + 
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DESCRIPTION:" + "\r\n" + 
            "STATUS:CONFIRMED" + "\r\n" + 
            "SEQUENCE:2" + "\r\n" + 
            "RECURRENCE-ID;TZID=Europe/Amsterdam:" + format(changeExceptionRecurrenceId1, "Europe/Amsterdam") + "\r\n" +
            "SUMMARY:MWB1546Test\r\n" +
            "LAST-MODIFIED:20220309T103926Z" + "\r\n" + 
            "CREATED:20220309T103926Z" + "\r\n" + 
            "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC" + "\r\n" + 
            "END:VEVENT" + "\r\n" + 
            "BEGIN:VEVENT" + "\r\n" + 
            "TRANSP:OPAQUE" + "\r\n" + 
            "DTSTART;TZID=Europe/Amsterdam:" + format(changeExceptionStart2, "Europe/Amsterdam") + "\r\n" +
            "DTEND;TZID=Europe/Amsterdam:" + format(changeExceptionEnd2, "Europe/Amsterdam") + "\r\n" +
            "RELATED-TO;RELTYPE=X-CALENDARSERVER-RECURRENCE-SET:e8b82405-b736-4f6d-93" + "\r\n" + 
            " 69-377102402697" + "\r\n" + 
            "UID:" + uid + "\r\n" +
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "DESCRIPTION:" + "\r\n" + 
            "STATUS:CONFIRMED" + "\r\n" + 
            "SEQUENCE:2" + "\r\n" + 
            "RECURRENCE-ID;TZID=Europe/Amsterdam:" + format(changeExceptionRecurrenceId2, "Europe/Amsterdam") + "\r\n" +
            "SUMMARY:MWB1546Test\r\n" +
            "LAST-MODIFIED:20220309T103926Z" + "\r\n" + 
            "CREATED:20220309T103926Z" + "\r\n" + 
            "X-APPLE-TRAVEL-ADVISORY-BEHAVIOR:AUTOMATIC" + "\r\n" + 
            "END:VEVENT" + "\r\n" + 
            "END:VCALENDAR"
        ; // @formatter:on
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(uid, iCalUpdate, iCalResource.getETag()), "response code wrong");
        /*
         * check that change exceptions are still there
         */
        ICalResource updatedICalResource = get(uid);
        assertNotNull(updatedICalResource);
        assertEquals(3, updatedICalResource.getVEvents().size());
    }

}
