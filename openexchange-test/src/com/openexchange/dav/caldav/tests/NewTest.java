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

package com.openexchange.dav.caldav.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link NewTest} - Tests appointment creation via the CalDAV interface
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class NewTest extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateSimpleOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on client
         */
        String uid = randomUID();
        String summary = "test";
        String location = "testcity";
        Date start = TimeTools.D("tomorrow at 3pm");
        Date end = TimeTools.D("tomorrow at 4pm");
        String iCal = generateICal(start, end, uid, summary, location);
        assertEquals(StatusCodes.SC_CREATED, super.putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(uid);
        super.rememberForCleanUp(appointment);
        assertAppointmentEquals(appointment, start, end, uid, summary, location);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateSimpleOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment on server
         */
        String uid = randomUID();
        String summary = "hallo";
        String location = "achtung";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        super.rememberForCleanUp(super.create(appointment));
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateAllDayOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on client
         */
        String uid = randomUID();
        String summary = "test all day";
        Date start = TimeTools.D("midnight");
        Date end = TimeTools.D("tomorrow at midnight");
        String iCal = "BEGIN:VCALENDAR" + "\r\n" + "VERSION:2.0" + "\r\n" + "PRODID:-//Apple Inc.//iCal 5.0.2//EN" + "\r\n" + "CALSCALE:GREGORIAN" + "\r\n" + "BEGIN:VEVENT" + "\r\n" + "CREATED:" + formatAsUTC(new Date()) + "\r\n" + "UID:" + uid + "\r\n" + "DTEND;VALUE=DATE:" + formatAsDate(end) + "\r\n" + "TRANSP:OPAQUE" + "\r\n" + "CLASS:PUBLIC" + "\r\n" + "SUMMARY:" + summary + "\r\n" + "LAST-MODIFIED:" + formatAsUTC(new Date()) + "\r\n" + "DTSTAMP:" + formatAsUTC(new Date()) + "\r\n" + "DTSTART;VALUE=DATE:" + formatAsDate(start) + "\r\n" + "SEQUENCE:0" + "\r\n" + "END:VEVENT" + "\r\n" + "END:VCALENDAR";
        assertEquals(StatusCodes.SC_CREATED, super.putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(uid);
        assertNotNull(appointment, "appointmnet not found on server");
        super.rememberForCleanUp(appointment);
        assertEquals(summary, appointment.getTitle(), "title wrong");
        assertTrue(appointment.getFullTime(), "full time wrong");
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "START wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "END wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testAllDayOnServer(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken());
        /*
         * create appointment on server
         */
        String uid = randomUID();
        String summary = "all day";
        String location = "testing";
        Date start = TimeTools.D("next monday at midnight");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date end = calendar.getTime();
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setFullTime(true);
        super.rememberForCleanUp(super.create(appointment));
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        assertEquals(start, iCalResource.getVEvent().getDTStart(), "START wrong");
        assertEquals(end, iCalResource.getVEvent().getDTEnd(), "END wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testCreateWithDifferentName(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create appointment on client
         */
        String resourceName = randomUID();
        String uid = randomUID();
        String summary = "test with filename";
        String location = "loco";
        Date start = TimeTools.D("last sunday at 2am");
        Date end = TimeTools.D("last sunday at 7am");
        String iCal = generateICal(start, end, uid, summary, location);
        assertEquals(StatusCodes.SC_CREATED, super.putICal(resourceName, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(uid);
        super.rememberForCleanUp(appointment);
        assertAppointmentEquals(appointment, start, end, uid, summary, location);
        /*
         * verify appointment on client
         */
        ICalResource iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
    }

}
