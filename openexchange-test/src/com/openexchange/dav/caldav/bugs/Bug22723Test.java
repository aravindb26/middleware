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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import com.openexchange.dav.StatusCodes;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.reports.SyncCollectionResponse;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link Bug22723Test} - appointments deleted in webgui do not disappear in Lightning
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug22723Test extends CalDAVTest {

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSynchronizeDeletion(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();
        /*
         * create a new folder on the server
         */
        String subFolderName = "testfolder_" + randomUID();
        FolderObject subFolder = super.createFolder(subFolderName);
        String subFolderID = Integer.toString(subFolder.getObjectID());
        /*
         * fetch initial sync token for later synchronization
         */
        SyncToken syncToken = new SyncToken(super.fetchSyncToken(subFolderID));
        /*
         * create appointment on client
         */
        String uid = randomUID();
        String summary = "hello";
        String location = "here";
        Date start = TimeTools.D("tomorrow at 6am");
        Date end = TimeTools.D("tomorrow at 8am");
        String iCal = generateICal(start, end, uid, summary, location);
        assertEquals(StatusCodes.SC_CREATED, super.putICal(subFolderID, uid, iCal), "response code wrong");
        /*
         * verify appointment on server
         */
        Appointment appointment = super.getAppointment(subFolderID, uid);
        super.rememberForCleanUp(appointment);
        assertAppointmentEquals(appointment, start, end, uid, summary, location);
        /*
         * delete appointment on server
         */
        super.delete(appointment);
        /*
         * verify deletion on server
         */
        assertNull(super.getAppointment(subFolderID, uid), "Appointment not deleted on server");
        /*
         * verify deletion on client
         */
        SyncCollectionResponse syncCollectionResponse = super.syncCollection(syncToken, subFolderID);
        assertTrue(0 < syncCollectionResponse.getHrefsStatusNotFound().size(), "no resource deletions reported on sync collection");
        boolean found = false;
        for (String href : syncCollectionResponse.getHrefsStatusNotFound()) {
            if (null != href && href.contains(uid)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "appointment not reported as deleted");
    }

}
