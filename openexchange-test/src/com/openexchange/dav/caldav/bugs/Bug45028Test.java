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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.test.common.test.PermissionTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug45028Test}
 *
 * Client repeatedly tries to update appointment in public folder
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.3
 */
public class Bug45028Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager manager2;
    private FolderObject publicFolder;
    private String publicFolderId;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        manager2 = new CalendarTestManager(client2);
        manager2.setFailOnError(true);
        FolderObject folder = new FolderObject();
        folder.setModule(FolderObject.CALENDAR);
        folder.setParentFolderID(FolderObject.SYSTEM_PUBLIC_FOLDER_ID);
        folder.setPermissions(PermissionTools.P(Integer.valueOf(manager2.getClient().getValues().getUserId()), PermissionTools.ADMIN, Integer.valueOf(getClient().getValues().getUserId()), "vr"));
        folder.setFolderName(randomUID());
        ftm.setClient(client2);
        publicFolder = ftm.insertFolderOnServer(folder);
        publicFolderId = String.valueOf(publicFolder.getObjectID());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSetAlarmInReadOnlyFolder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken(publicFolderId));
        /*
         * create appointment with users A and B on server as user B in public folder
         */
        String uid = randomUID();
        Appointment appointment = new Appointment();
        appointment.setUid(uid);
        appointment.setTitle("Bug45028Test");
        appointment.setIgnoreConflicts(true);
        appointment.setStartDate(TimeTools.D("next monday at 15:30"));
        appointment.setEndDate(TimeTools.D("next monday at 16:30"));
        appointment.setParentFolderID(publicFolder.getObjectID());
        appointment.addParticipant(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.addParticipant(new UserParticipant(getClient().getValues().getUserId()));
        manager2.insert(appointment);
        /*
         * synchronize the public calendar as user A
         */
        Map<String, String> eTags = syncCollection(syncToken, publicFolderId).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        /*
         * add reminder in user a's client
         */
        iCalResource.getVEvent().getComponents().clear();
        String iCal = "BEGIN:VALARM\r\n" + "ACTION:DISPLAY\r\n" + "DESCRIPTION:Alarm\r\n" + "TRIGGER:-PT25M\r\n" + "UID:F7FCDC9A-BA2A-4548-BC5A-815008F0FC6E\r\n" + "X-WR-ALARMUID:F7FCDC9A-BA2A-4548-BC5A-815008F0FC6E\r\n" + "END:VALARM\r\n";
        Component vAlarm = SimpleICal.parse(iCal, "VALARM");
        iCalResource.getVEvent().getComponents().add(vAlarm);
        assertEquals(StatusCodes.SC_CREATED, putICalUpdate(iCalResource), "response code wrong");
        /*
         * verify appointment on server
         */
        appointment = getAppointment(publicFolderId, uid);
        assertNotNull(appointment, "appointment not found on server");
        assertTrue(appointment.containsAlarm(), "no reminder found");
        assertEquals(25, appointment.getAlarm(), "reminder minutes wrong");
        /*
         * verify appointment on client
         */
        iCalResource = get(publicFolderId, uid);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        assertEquals(uid, iCalResource.getVEvent().getUID(), "UID wrong");
        assertNotNull(iCalResource.getVEvent().getVAlarm(), "No ALARM in iCal found");
        assertEquals("-PT25M", iCalResource.getVEvent().getVAlarm().getPropertyValue("TRIGGER"), "ALARM wrong");
    }

}
