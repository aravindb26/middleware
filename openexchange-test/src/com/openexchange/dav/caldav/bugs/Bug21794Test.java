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

import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link NewTest} - 500 an CalDAVWriteEntityQueueableOperation.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug21794Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testDeclineImportedAppointment(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment
         */
        String uid = "WEBEX-MEETING CENTER-6.0292133-" + new Date().getTime();
        String resourceName = uid.replace(".", "");
        Date start = TimeTools.D("next monday at 15:30");
        Date end = TimeTools.D("next monday at 16:30");
        String iCal = "BEGIN:VCALENDAR" + "\r\n" + "VERSION:2.0" + "\r\n" + "PRODID:-//Apple Inc.//iCal 5.0.2//EN" + "\r\n" + "CALSCALE:GREGORIAN" + "\r\n" + "BEGIN:VTIMEZONE" + "\r\n" + "TZID:Europe/Amsterdam" + "\r\n" + "BEGIN:DAYLIGHT" + "\r\n" + "TZOFFSETFROM:+0100" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU" + "\r\n" + "DTSTART:19810329T020000" + "\r\n" + "TZNAME:CEST" + "\r\n" + "TZOFFSETTO:+0200" + "\r\n" + "END:DAYLIGHT" + "\r\n" + "BEGIN:STANDARD" + "\r\n" + "TZOFFSETFROM:+0200" + "\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU" + "\r\n" + "DTSTART:19961027T030000" + "\r\n" + "TZNAME:CET" + "\r\n" + "TZOFFSETTO:+0100" + "\r\n" + "END:STANDARD" + "\r\n" + "END:VTIMEZONE" + "\r\n" + "BEGIN:VEVENT" + "\r\n" + "DTEND;TZID=Europe/Amsterdam:" + format(end, "Europe/Amsterdam") + "\r\n" + "TRANSP:OPAQUE" + "\r\n" + "ORGANIZER;CN=\"webex\":MAILTO:messenger@example.com" + "\r\n" + "UID:" + uid + "\r\n" + "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" + "LOCATION:https://open-xchange.webex.com/open-xchange-en" + "\r\n" + "DESCRIPTION:stripped" + "\r\n" + "SEQUENCE:2" + "\r\n" + "SUMMARY:Agorum Integration" + "\r\n" + "DTSTART;TZID=Europe/Amsterdam:" + format(start, "Europe/Amsterdam") + "\r\n" + "CREATED:" + formatAsUTC(TimeTools.D("yesterday noon")) + "\r\n" + "ATTENDEE;CN=\"Open-Xchange Presenter\";CUTYPE=INDIVIDUAL;ROLE=REQ-PARTICIP" + "\r\n" + " ANT:MAILTO:webmeeting1@example.com" + "\r\n" + "END:VEVENT" + "\r\n" + "END:VCALENDAR";

        assertEquals(StatusCodes.SC_CREATED, super.putICal(resourceName, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        super.rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(resourceName);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        /*
         * decline appointment on client
         */
        List<Property> attendees = iCalResource.getVEvent().getProperties("ATTENDEE");
        for (Property property : attendees) {
            if (property.getValue().contains(super.getAJAXClient().getValues().getDefaultAddress())) {
                for (Entry<String, String> attribute : property.getAttributes().entrySet()) {
                    if (attribute.getKey().equals("PARTSTAT")) {
                        attribute.setValue("DECLINED");
                        break;
                    }
                }
                break;
            }
        }
        assertEquals(StatusCodes.SC_CREATED, super.putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        UserParticipant[] users = appointment.getUsers();
        assertNotNull(users, "appointment has no users");
        UserParticipant declinedPartipant = null;
        for (UserParticipant user : users) {
            if (getAJAXClient().getValues().getUserId() == user.getIdentifier()) {
                declinedPartipant = user;
                break;
            }
        }
        assertNotNull(declinedPartipant, "declining participant not found");
        assertEquals(Appointment.DECLINE, declinedPartipant.getConfirm(), "confirmation status wrong");
        /*
         * verify appointment on client
         */
        iCalResource = super.get(resourceName);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        Property attendee = null;
        attendees = iCalResource.getVEvent().getProperties("ATTENDEE");
        for (Property property : attendees) {
            if (property.getValue().contains(super.getAJAXClient().getValues().getDefaultAddress())) {
                attendee = property;
                break;
            }
        }
        assertNotNull(attendee, "declining attendee not found");
        assertEquals("DECLINED", attendee.getAttribute("PARTSTAT"), "partstat status wrong");
    }

}
