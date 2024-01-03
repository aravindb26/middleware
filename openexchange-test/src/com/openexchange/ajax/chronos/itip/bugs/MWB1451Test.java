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

package com.openexchange.ajax.chronos.itip.bugs;

import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertAttendeePartStat;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleChange;
import static com.openexchange.ajax.chronos.itip.ITipAssertion.assertSingleEvent;
import static com.openexchange.ajax.chronos.itip.ITipUtil.constructBody;
import static com.openexchange.ajax.chronos.itip.ITipUtil.receiveIMip;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.PartStat;
import com.openexchange.ajax.chronos.itip.AbstractITipTest;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.scheduling.SchedulingMethod;
import com.openexchange.testing.httpclient.models.AnalysisChangeNewEvent;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.MailData;

/**
 * 
 * {@link MWB1451Test}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class MWB1451Test extends AbstractITipTest {

    @Test
    public void testInvitationFromIOS() throws Exception {
        /*
         * prepare initial iTIP REQUEST from iCloud or rather iOS
         * Note: Mock with internal mail address for the organizer instead of an imip.me
         * address, do verify that the reply arrives
         */
        String organizerEmail = testUserC2.getLogin();
        String attendeeEmail = testUser.getLogin();
        String uid = randomUID();
        String summary = randomUID();
        Date start = com.openexchange.time.TimeTools.D("next friday evening", TimeZone.getTimeZone("US/Pacific"));
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
        String iTip = // @formatter:off 
          "BEGIN:VCALENDAR" + "\r\n" + 
          "PRODID:-//caldav.icloud.com//CALDAVJ 2112B541//EN" + "\r\n" + 
          "METHOD:REQUEST" + "\r\n" + 
          "VERSION:2.0" + "\r\n" + 
          "BEGIN:VEVENT" + "\r\n" + 
          "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
          "LOCATION:" + "\r\n" + 
          "TZID:US/Pacific" + "\r\n" + 
          "SEQUENCE:0" + "\r\n" + 
          "SUMMARY:" + summary + "\r\n" + 
          "UID:" + uid + "\r\n" +
          "CREATED:" + formatAsUTC(new Date()) + "\r\n" +
          "DTSTART;TZID=US/Pacific:" + format(start, "US/Pacific") + "\r\n" +
          "DTEND;TZID=US/Pacific:"+ format(end, "US/Pacific")  + "\r\n" +
          "ATTENDEE;CN=Example;CUTYPE=INDIVIDUAL;PARTSTAT=ACCEPTED;ROLE=CHAIR;EMAIL=" + "\r\n" + 
          " " + organizerEmail + ":/aMTAwMzI1MzIxOTgxMDAzMjUohR7y9UmcSdNZ2Wc-e6HlrC3VI-e" + "\r\n" + 
          " diy0BKHD12ROZ/principal/" + "\r\n" + 
          "ORGANIZER;CN=Example;EMAIL=" + organizerEmail + ":mailto:" + organizerEmail + "\r\n" + 
          "ATTENDEE;CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIPANT;EMAIL=" + attendeeEmail + "\r\n" + 
          " ;SCHEDULE-STATUS=1.1;PARTSTAT=NEEDS-ACTION:mailto:" + attendeeEmail + "\r\n" + 
          "END:VEVENT" + "\r\n" + 
          "BEGIN:VTIMEZONE" + "\r\n" + 
          "TZID:US/Pacific" + "\r\n" + 
          "X-LIC-LOCATION:US/Pacific" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:18831118T120702" + "\r\n" + 
          "RDATE:18831118T120702" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-075258" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19180331T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=19190330T100000Z;BYMONTH=3;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PDT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:19181027T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=19191026T090000Z;BYMONTH=10;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-0700" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19420209T020000" + "\r\n" + 
          "RDATE:19420209T020000" + "\r\n" + 
          "TZNAME:PWT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19450814T160000" + "\r\n" + 
          "RDATE:19450814T160000" + "\r\n" + 
          "TZNAME:PPT" + "\r\n" + 
          "TZOFFSETFROM:-0700" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:19450930T020000" + "\r\n" + 
          "RDATE:19450930T020000" + "\r\n" + 
          "RDATE:19490101T020000" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-0700" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:19460101T000000" + "\r\n" + 
          "RDATE:19460101T000000" + "\r\n" + 
          "RDATE:19670101T000000" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19480314T020100" + "\r\n" + 
          "RDATE:19480314T020100" + "\r\n" + 
          "RDATE:19740106T020000" + "\r\n" + 
          "RDATE:19750223T020000" + "\r\n" + 
          "TZNAME:PDT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19500430T010000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=19660424T090000Z;BYMONTH=4;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PDT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:19500924T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=19610924T090000Z;BYMONTH=9;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-0700" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:19621028T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=19661030T090000Z;BYMONTH=10;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-0700" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19670430T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=19730429T100000Z;BYMONTH=4;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PDT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:19671029T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=20061029T090000Z;BYMONTH=10;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-0700" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19760425T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=19860427T100000Z;BYMONTH=4;BYDAY=-1SU" + "\r\n" + 
          "TZNAME:PDT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:19870405T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;UNTIL=20060402T100000Z;BYMONTH=4;BYDAY=1SU" + "\r\n" + 
          "TZNAME:PDT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:DAYLIGHT" + "\r\n" + 
          "DTSTART:20070311T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU" + "\r\n" + 
          "TZNAME:PDT" + "\r\n" + 
          "TZOFFSETFROM:-0800" + "\r\n" + 
          "TZOFFSETTO:-0700" + "\r\n" + 
          "END:DAYLIGHT" + "\r\n" + 
          "BEGIN:STANDARD" + "\r\n" + 
          "DTSTART:20071104T020000" + "\r\n" + 
          "RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU" + "\r\n" + 
          "TZNAME:PST" + "\r\n" + 
          "TZOFFSETFROM:-0700" + "\r\n" + 
          "TZOFFSETTO:-0800" + "\r\n" + 
          "END:STANDARD" + "\r\n" + 
          "END:VTIMEZONE" + "\r\n" + 
          "END:VCALENDAR" + "\r\n" 
        ; // @formatter:on
        /*
         * wrap in iMIP message & send it from user b to user a
         */
        sendImip(apiClientC2, generateImip(organizerEmail, attendeeEmail, randomUID(), summary, new Date(), SchedulingMethod.REQUEST, iTip));
        /*
         * receive & analyze iMIP request as user a
         */
        MailData iMipRequestData = receiveIMip(apiClient, organizerEmail, summary, 0, uid, null, SchedulingMethod.REQUEST);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), attendeeEmail, PartStat.NEEDS_ACTION);
        /*
         * reply with "accepted"
         */
        EventData eventData = assertSingleEvent(accept(constructBody(iMipRequestData), null));
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), attendeeEmail, PartStat.ACCEPTED);
        /*
         * check event in calendar
         */
        EventResponse eventResponse = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), eventData.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        eventData = eventResponse.getData();
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), attendeeEmail, PartStat.ACCEPTED);
        /*
         * receive reply as organizer
         * Note: Event was mocked, so analyzing doesn't make sense
         */
        receiveIMip(apiClientC2, attendeeEmail, summary, 0, SchedulingMethod.REPLY);
    }

}
