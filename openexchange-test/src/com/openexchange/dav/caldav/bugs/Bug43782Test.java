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
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug43782Test}
 *
 * Alarm lost after changing other appointment property via CalDAV
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug43782Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateSummary(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testUpdateSummary(true);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateSummaryRemovingAcknowledged(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testUpdateSummary(false);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateStartAndEnd(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testUpdateStartAndEnd(true);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUpdateStartAndEndRemovingAcknowledged(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        testUpdateStartAndEnd(false);
    }

    private void testUpdateSummary(boolean preserveAcknowledged) throws Exception {
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create appointment on server
         */
        String uid = randomUID();
        String summary = "test";
        String location = "ort";
        Date start = TimeTools.D("next tuesday at 12:30");
        Date end = TimeTools.D("next tuesday at 13:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setAlarm(15);
        appointment.setAlarmFlag(true);
        rememberForCleanUp(create(appointment));
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        List<Component> vAlarms = iCalResource.getVEvent().getVAlarms();
        assertEquals(1, vAlarms.size(), "Unexpected number of VALARMs found");
        assertEquals("-PT15M", vAlarms.get(0).getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * update appointment on client
         */
        summary += "_edit";
        iCalResource.getVEvent().setSummary(summary);
        if (false == preserveAcknowledged) {
            iCalResource.getVEvent().getVAlarm().removeProperties("ACKNOWLEDGED");
        }
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertEquals(summary, appointment.getTitle(), "Title wrong");
        assertEquals(start, appointment.getStartDate(), "Start wrong");
        assertEquals(end, appointment.getEndDate(), "End wrong");
        assertTrue(appointment.containsAlarm(), "No reminder found");
        assertEquals(15, appointment.getAlarm(), "Reminder wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        vAlarms = iCalResource.getVEvent().getVAlarms();
        assertEquals(1, vAlarms.size(), "Unexpected number of VALARMs found");
        assertEquals("-PT15M", vAlarms.get(0).getPropertyValue("TRIGGER"), "ALARM wrong");
    }

    private void testUpdateStartAndEnd(boolean preserveAcknowledged) throws Exception {
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create appointment on server
         */
        String uid = randomUID();
        String summary = "test";
        String location = "ort";
        Date start = TimeTools.D("next tuesday at 12:30");
        Date end = TimeTools.D("next tuesday at 13:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        appointment.setAlarm(15);
        appointment.setAlarmFlag(true);
        rememberForCleanUp(create(appointment));
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        List<Component> vAlarms = iCalResource.getVEvent().getVAlarms();
        assertEquals(1, vAlarms.size(), "Unexpected number of VALARMs found");
        assertEquals("-PT15M", vAlarms.get(0).getPropertyValue("TRIGGER"), "ALARM wrong");
        /*
         * update appointment on client
         */
        start = TimeTools.D("next tuesday at 11:30");
        Property dtStart = iCalResource.getVEvent().getProperty("DTSTART");
        iCalResource.getVEvent().setProperty("DTSTART", null == dtStart.getAttribute("TZID") ? formatAsUTC(start) : format(start, dtStart.getAttribute("TZID")), dtStart.getAttributes());
        end = TimeTools.D("next tuesday at 12:45");
        Property dtEnd = iCalResource.getVEvent().getProperty("DTEND");
        iCalResource.getVEvent().setProperty("DTEND", null == dtEnd.getAttribute("TZID") ? formatAsUTC(end) : format(end, dtEnd.getAttribute("TZID")), dtEnd.getAttributes());
        if (false == preserveAcknowledged) {
            iCalResource.getVEvent().getVAlarm().removeProperties("ACKNOWLEDGED");
        }
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(uid);
        assertNotNull(appointment, "appointment not found on server");
        assertEquals(summary, appointment.getTitle(), "Title wrong");
        assertEquals(start, appointment.getStartDate(), "Start wrong");
        assertEquals(end, appointment.getEndDate(), "End wrong");
        assertTrue(appointment.containsAlarm(), "No reminder found");
        assertEquals(15, appointment.getAlarm(), "Reminder wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertEquals(summary, iCalResource.getVEvent().getSummary(), "SUMMARY wrong");
        assertEquals(location, iCalResource.getVEvent().getLocation(), "LOCATION wrong");
        vAlarms = iCalResource.getVEvent().getVAlarms();
        assertEquals(1, vAlarms.size(), "Unexpected number of VALARMs found");
        assertEquals("-PT15M", vAlarms.get(0).getPropertyValue("TRIGGER"), "ALARM wrong");
    }

}
