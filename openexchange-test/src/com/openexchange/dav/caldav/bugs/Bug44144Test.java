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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.openexchange.ajax.importexport.actions.ICalImportRequest;
import com.openexchange.ajax.importexport.actions.ICalImportResponse;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.importexport.ImportResult;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug44144Test}
 *
 * Appointments which have been imported via iCal import aren't synced using CalDAV
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug44144Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSyncImportedAppointment(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * prepare iCal file to import
         */
        String uid = randomUID();
        Date start = TimeTools.D("tomorrow at 6am");
        Date end = TimeTools.D("tomorrow at 8am");
        Date lastModified = TimeTools.D("Last month");
        Date created = TimeTools.D("Last month");
        String iCal = "BEGIN:VCALENDAR\r\n" + "CALSCALE:GREGORIAN\r\n" + "PRODID:-//Apple Inc.//iOS 9.1//EN\r\n" + "VERSION:2.0\r\n" + "BEGIN:VTIMEZONE\r\n" + "TZID:Europe/Berlin\r\n" + "BEGIN:DAYLIGHT\r\n" + "DTSTART:19810329T020000\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n" + "TZNAME:MESZ\r\n" + "TZOFFSETFROM:+0100\r\n" + "TZOFFSETTO:+0200\r\n" + "END:DAYLIGHT\r\n" + "BEGIN:STANDARD\r\n" + "DTSTART:19961027T030000\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n" + "TZNAME:MEZ\r\n" + "TZOFFSETFROM:+0200\r\n" + "TZOFFSETTO:+0100\r\n" + "END:STANDARD\r\n" + "END:VTIMEZONE\r\n" + "BEGIN:VEVENT\r\n" + "CREATED:" + formatAsUTC(created) + "\r\n" + "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" + "DTSTAMP:" + formatAsUTC(lastModified) + "\r\n" + "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" + "LAST-MODIFIED:" + formatAsUTC(lastModified) + "\r\n" + "SUMMARY:testimport\r\n" + "UID:" + uid + "\r\n" + "END:VEVENT\r\n" + "END:VCALENDAR\r\n";
        /*
         * import iCal appointment on server
         */
        int parentFolderID = Integer.parseInt(getDefaultFolderID());
        ICalImportRequest importRequest = new ICalImportRequest(parentFolderID, Streams.newByteArrayInputStream(iCal.getBytes(Charsets.UTF_8)));
        ICalImportResponse importResponse = getClient().execute(importRequest);
        ImportResult[] importResult = importResponse.getImports();
        assertNotNull(importResult, "No import result");
        assertEquals(1, importResult.length, "Unexpected number of import results");
        int objectID = Integer.parseInt(importResult[0].getObjectId());
        /*
         * verify appointment on server
         */
        Appointment appointment = getManager().get(parentFolderID, objectID);
        assertNotNull(appointment, "Appointment not found");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSyncImportedAppointmentFromClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * prepare iCal file to import
         */
        String uid = randomUID();
        Date start = TimeTools.D("tomorrow at 6am");
        Date end = TimeTools.D("tomorrow at 8am");
        Date lastModified = TimeTools.D("Last month");
        Date created = TimeTools.D("Last month");
        String iCal = "BEGIN:VCALENDAR\r\n" + "CALSCALE:GREGORIAN\r\n" + "PRODID:-//Apple Inc.//iOS 9.1//EN\r\n" + "VERSION:2.0\r\n" + "BEGIN:VTIMEZONE\r\n" + "TZID:Europe/Berlin\r\n" + "BEGIN:DAYLIGHT\r\n" + "DTSTART:19810329T020000\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=-1SU\r\n" + "TZNAME:MESZ\r\n" + "TZOFFSETFROM:+0100\r\n" + "TZOFFSETTO:+0200\r\n" + "END:DAYLIGHT\r\n" + "BEGIN:STANDARD\r\n" + "DTSTART:19961027T030000\r\n" + "RRULE:FREQ=YEARLY;BYMONTH=10;BYDAY=-1SU\r\n" + "TZNAME:MEZ\r\n" + "TZOFFSETFROM:+0200\r\n" + "TZOFFSETTO:+0100\r\n" + "END:STANDARD\r\n" + "END:VTIMEZONE\r\n" + "BEGIN:VEVENT\r\n" + "CREATED:" + formatAsUTC(created) + "\r\n" + "DTEND;TZID=Europe/Berlin:" + format(end, "Europe/Berlin") + "\r\n" + "DTSTAMP:" + formatAsUTC(lastModified) + "\r\n" + "DTSTART;TZID=Europe/Berlin:" + format(start, "Europe/Berlin") + "\r\n" + "LAST-MODIFIED:" + formatAsUTC(lastModified) + "\r\n" + "SUMMARY:testimport\r\n" + "UID:" + uid + "\r\n" + "END:VEVENT\r\n" + "END:VCALENDAR\r\n";
        /*
         * import iCal file from client and sync
         */
        assertEquals(StatusCodes.SC_CREATED, putICal(uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        rememberForCleanUp(appointment);
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
    }

}
