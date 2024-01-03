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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.jackrabbit.webdav.client.methods.MoveMethod;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Property;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.webdav.protocol.WebdavPath;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MoveTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.2
 */
public class MoveTest extends Abstract2UserCalDAVTest {

    private CalendarTestManager manager2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        manager2 = new CalendarTestManager(client2);
        manager2.setFailOnError(true);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testMoveAsAttendeeOnClient(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create subfolder & fetch sync token for later synchronization
         */
        FolderObject subfolder = createFolder(randomUID());
        SyncToken syncToken = new SyncToken(fetchSyncToken());
        /*
         * create appointment as user 2 on server
         */
        String uid = randomUID();
        String summary = "serie";
        String location = "test";
        Date start = TimeTools.D("next sunday at 11:30");
        Date end = TimeTools.D("next sunday at 12:45");
        Appointment appointment = generateAppointment(start, end, uid, summary, location);
        List<Participant> participants = new ArrayList<Participant>();
        participants.add(new UserParticipant(getClient().getValues().getUserId()));
        participants.add(new UserParticipant(manager2.getClient().getValues().getUserId()));
        appointment.setParticipants(participants);
        appointment.setParentFolderID(manager2.getPrivateFolder());
        appointment.setIgnoreConflicts(true);
        manager2.insert(appointment);
        /*
         * verify appointment on client
         */
        Map<String, String> eTags = syncCollection(syncToken).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        Property attendee = iCalResource.getVEvent().getAttendee(getClient().getValues().getDefaultAddress());
        assertNotNull(attendee, "Attendee not found in iCal");
        /*
         * move appointment to subfolder on client
         */
        MoveMethod move = null;
        String targetHref = "/caldav/" + encodeFolderID(String.valueOf(subfolder.getObjectID())) + '/' + new WebdavPath(iCalResource.getHref()).name();
        try {
            move = new MoveMethod(getBaseUri() + iCalResource.getHref(), getBaseUri() + targetHref, false);
            Assertions.assertEquals(StatusCodes.SC_CREATED, webDAVClient.executeMethod(move), "response code wrong");
        } finally {
            release(move);
        }
        /*
         * verify appointment was moved properly
         */
        assertNull(getAppointment(getDefaultFolderID(), appointment.getUid()));
        assertNotNull(getAppointment(String.valueOf(subfolder.getObjectID()), appointment.getUid()));
    }

}
