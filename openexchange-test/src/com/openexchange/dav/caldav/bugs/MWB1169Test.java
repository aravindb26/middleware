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
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.ajax.appointment.action.DeleteRequest;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.Abstract2UserCalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.dav.caldav.ical.SimpleICal.Component;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.java.Strings;
import com.openexchange.test.CalendarTestManager;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.test.common.test.PermissionTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB1169Test}
 *
 * appointment series exceptions are not shown via CalDAV all the time
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class MWB1169Test extends Abstract2UserCalDAVTest {

    private CalendarTestManager catm2;
    private FolderObject publicFolder;
    private String publicFolderId;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        catm2 = new CalendarTestManager(client2);
        catm2.setFailOnError(true);
        FolderObject folder = new FolderObject();
        folder.setModule(FolderObject.CALENDAR);
        folder.setParentFolderID(FolderObject.SYSTEM_PUBLIC_FOLDER_ID);
        folder.setPermissions(PermissionTools.P(
            Integer.valueOf(client2.getValues().getUserId()), PermissionTools.ADMIN, 
            Integer.valueOf(getClient().getValues().getUserId()), PermissionTools.ADMIN));
        folder.setFolderName(randomUID());
        ftm.setClient(client2);
        publicFolder = ftm.insertFolderOnServer(folder);
        publicFolderId = String.valueOf(publicFolder.getObjectID());
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testUserizedExDatesInPublicFolder(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * as user A, fetch sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken(publicFolderId));
        /*
         * as user B, create appointment series in public folder 
         */
        String uid = randomUID();
        Appointment seriesMaster = new Appointment();
        seriesMaster.setUid(uid);
        seriesMaster.setInterval(1);
        seriesMaster.setRecurrenceCount(10);
        seriesMaster.setRecurrenceType(Appointment.DAILY);
        seriesMaster.setTitle("MWB1169Test_series");
        seriesMaster.setIgnoreConflicts(true);
        seriesMaster.setStartDate(TimeTools.D("next monday at 15:30"));
        seriesMaster.setEndDate(TimeTools.D("next monday at 16:30"));
        seriesMaster.setParentFolderID(publicFolder.getObjectID());
        seriesMaster.addParticipant(new UserParticipant(client2.getValues().getUserId()));
        catm2.insert(seriesMaster);
        Date clientLastModified = catm2.getLastModification();
        /*
         * create change exceptions on server
         */
        Appointment exception1 = new Appointment();
        exception1.setTitle("MWB1169Test_exception1");
        exception1.setObjectID(seriesMaster.getObjectID());
        exception1.setRecurrencePosition(3);
        exception1.setLastModified(clientLastModified);
        exception1.setParentFolderID(seriesMaster.getParentFolderID());
        exception1.setIgnoreConflicts(true);
        catm2.update(exception1);
        clientLastModified = catm2.getLastModification();
        Appointment exception2 = new Appointment();
        exception2.setTitle("MWB1169Test_exception2");
        exception2.setObjectID(seriesMaster.getObjectID());
        exception2.setRecurrencePosition(5);
        exception2.setLastModified(clientLastModified);
        exception2.setParentFolderID(seriesMaster.getParentFolderID());
        exception2.setIgnoreConflicts(true);
        catm2.update(exception2);
        clientLastModified = catm2.getLastModification();
        /*
         * create a delete exception on server
         */
        client2.execute(new DeleteRequest(seriesMaster.getObjectID(), seriesMaster.getParentFolderID(), 7, clientLastModified));
        /*
         * as user A, synchronize the public calendar
         */
        Map<String, String> eTags = syncCollection(syncToken, publicFolderId).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = calendarMultiget(eTags.keySet());
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVEvent(), "No VEVENT in iCal found");
        /*
         * check series master event and overridden instances are contained
         */
        assertEquals(3, iCalResource.getVEvents().size(), "unexpected number of VEVENTs");
        assertTrue(iCalResource.toString().contains(seriesMaster.getTitle()), "series master not found");
        assertTrue(iCalResource.toString().contains(exception1.getTitle()), "change exception not found");
        assertTrue(iCalResource.toString().contains(exception2.getTitle()), "change exception not found");
        /*
         * check EXDATE property of series master event
         */
        Component seriesMasterComponent = iCalResource.getVEvent(null);
        String exDateValue = seriesMasterComponent.getPropertyValue("EXDATE");
        String[] exDates = Strings.splitByComma(exDateValue);
        assertTrue(null != exDates && 1 == exDates.length, "unexpected number of EXDATEs in " + exDateValue);
    }

}
