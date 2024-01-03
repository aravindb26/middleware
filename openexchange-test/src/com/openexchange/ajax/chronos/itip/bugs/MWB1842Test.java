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
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventResponse;
import com.openexchange.testing.httpclient.models.MailData;
import com.openexchange.testing.httpclient.models.UserData;

/**
 * {@link MWB1842Test}
 * 
 * MIME header encoded values in properties of iCal file
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1842Test extends AbstractITipTest {

    @Test
    public void testImportMimeGarbage() throws Exception {
        /*
         * prepare iTIP REQUEST with MIME-encoded strings
         */
        UserData organizerUserData = userResponseC2.getData();
        UserData attendeeUserData = userResponseC1.getData();
        String uid = randomUID();
        String summary = randomUID();
        Date start = com.openexchange.time.TimeTools.D("next friday evening", TimeZone.getTimeZone("Europe/Berlin"));
        Date end = CalendarUtils.add(start, Calendar.HOUR, 1);
        String iTip = // @formatter:off
            "BEGIN:VCALENDAR\r\n" + 
            "METHOD:REQUEST\r\n" + 
            "PRODID:Microsoft Exchange Server 2010\r\n" + 
            "VERSION:2.0\r\n" + 
            "BEGIN:VTIMEZONE\r\n" + 
            "TZID:W. Europe Standard Time\r\n" + 
            "BEGIN:STANDARD\r\n" + 
            "DTSTART:16010101T030000\r\n" + 
            "TZOFFSETFROM:+0200\r\n" + 
            "TZOFFSETTO:+0100\r\n" + 
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=10\r\n" + 
            "END:STANDARD\r\n" + 
            "BEGIN:DAYLIGHT\r\n" + 
            "DTSTART:16010101T020000\r\n" + 
            "TZOFFSETFROM:+0100\r\n" + 
            "TZOFFSETTO:+0200\r\n" + 
            "RRULE:FREQ=YEARLY;INTERVAL=1;BYDAY=-1SU;BYMONTH=3\r\n" + 
            "END:DAYLIGHT\r\n" + 
            "END:VTIMEZONE\r\n" + 
            "BEGIN:VEVENT\r\n" + 
            "ORGANIZER;CN=\"" + organizerUserData.getDisplayName() +"\":MAILTO:" + organizerUserData.getEmail1() + "\r\n" + 
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=\"Sturm, Chr\r\n" + 
            " istian (extern)\":MAILTO:christian.sturm@example.org\r\n" + 
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=\"=?utf-8?B?\r\n" + 
            " R8O2dHogQmVyZ3dvaGw=?=\":MAILTO:goetz.bergwohl@example.org\r\n" + 
            "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE;CN=\"" + attendeeUserData.getDisplayName() + 
            "\":MAILTO:" + attendeeUserData.getEmail1() + "\r\n" +
            "DESCRIPTION;LANGUAGE=de-DE:Hallo zusammen\\,\\n\\nanbei der Termin\\n\\n\r\n" + 
            "RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=TU;WKST=MO\r\n" + 
            "UID:" + uid + "\r\n" +
            "SUMMARY;LANGUAGE=de-DE:=?utf-8?B?UmVnZWxtw6TDn2lnZXIgQXVzdGF1c2NoIC0gWFkgLS\r\n" + 
            " BQ?=  =?utf-8?B?cnVzdGFs?=\r\n" + 
            "DTSTART;TZID=W. Europe Standard Time:" + format(start, "Europe/Berlin") + "\r\n" +
            "DTEND;TZID=W. Europe Standard Time:"+ format(end, "Europe/Berlin")  + "\r\n" +
            "CLASS:PUBLIC\r\n" + 
            "PRIORITY:5\r\n" + 
            "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" +
            "TRANSP:OPAQUE\r\n" + 
            "STATUS:CONFIRMED\r\n" + 
            "SEQUENCE:0\r\n" + 
            "LOCATION;LANGUAGE=de-DE:https://abcdefg.px.example.com/#/room/!eWYQLbWYbwV\r\n" + 
            " sPniqIp:matrix.px.example.com\r\n" + 
            "X-MICROSOFT-CDO-APPT-SEQUENCE:0\r\n" + 
            "X-MICROSOFT-CDO-OWNERAPPTID:424180326\r\n" + 
            "X-MICROSOFT-CDO-BUSYSTATUS:TENTATIVE\r\n" + 
            "X-MICROSOFT-CDO-INTENDEDSTATUS:BUSY\r\n" + 
            "X-MICROSOFT-CDO-ALLDAYEVENT:FALSE\r\n" + 
            "X-MICROSOFT-CDO-IMPORTANCE:1\r\n" + 
            "X-MICROSOFT-CDO-INSTTYPE:1\r\n" + 
            "X-MICROSOFT-DONOTFORWARDMEETING:FALSE\r\n" + 
            "X-MICROSOFT-DISALLOW-COUNTER:FALSE\r\n" + 
            "BEGIN:VALARM\r\n" + 
            "DESCRIPTION:REMINDER\r\n" + 
            "TRIGGER;RELATED=START:-PT15M\r\n" + 
            "ACTION:DISPLAY\r\n" + 
            "END:VALARM\r\n" + 
            "END:VEVENT\r\n" + 
            "END:VCALENDAR\r\n"
        ; // @formatter:on
        /*
         * wrap in iMIP message & send it from user b to user a
         */
        sendImip(apiClientC2, generateImip(organizerUserData.getEmail1(), attendeeUserData.getEmail1(), randomUID(), summary, new Date(), SchedulingMethod.REQUEST, iTip));
        /*
         * receive & analyze iMIP request as user a
         */
        MailData iMipRequestData = receiveIMip(apiClient, organizerUserData.getEmail1(), summary, 0, uid, null, SchedulingMethod.REQUEST);
        AnalysisChangeNewEvent newEvent = assertSingleChange(analyze(apiClient, iMipRequestData)).getNewEvent();
        assertNotNull(newEvent);
        assertEquals(uid, newEvent.getUid());
        assertAttendeePartStat(newEvent.getAttendees(), attendeeUserData.getEmail1(), PartStat.NEEDS_ACTION);
        assertEquals("Regelmäßiger Austausch - XY - Prustal", newEvent.getSummary());
        for (Attendee attendee : newEvent.getAttendees()) {
            if (attendee.getUri().contains("bergwohl")) {
                assertEquals("Götz Bergwohl", attendee.getCn());
            }
        }
        /*
         * reply with "accepted"
         */
        EventData eventData = assertSingleEvent(accept(constructBody(iMipRequestData), null));
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), attendeeUserData.getEmail1(), PartStat.ACCEPTED);
        /*
         * check event in calendar
         */
        EventResponse eventResponse = chronosApi.getEvent(eventData.getId(), eventData.getFolder(), eventData.getRecurrenceId(), null, null);
        assertNull(eventResponse.getError(), eventResponse.getError());
        eventData = eventResponse.getData();
        assertEquals(uid, eventData.getUid());
        assertAttendeePartStat(eventData.getAttendees(), attendeeUserData.getEmail1(), PartStat.ACCEPTED);
        assertEquals("Regelmäßiger Austausch - XY - Prustal", eventData.getSummary());
        for (Attendee attendee : eventData.getAttendees()) {
            if (attendee.getUri().contains("bergwohl")) {
                assertEquals("Götz Bergwohl", attendee.getCn());
            }
        }
    }

}
