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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import com.google.common.io.BaseEncoding;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.CalDAVTest;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.java.Charsets;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link MWB47Test}
 *
 * Task Sync via CalDAV not working properly between Apple products and OX
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.4
 */
public class MWB47Test extends CalDAVTest {

    @Override
    protected String encodeFolderID(String folderID) {
        return BaseEncoding.base64Url().omitPadding().encode(folderID.getBytes(Charsets.US_ASCII));
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSyncCreation(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String folderID = String.valueOf(getClient().getValues().getPrivateTaskFolder());
        /*
         * fetch sync token prior creation
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken(folderID));
        /*
         * create task on server
         */
        String uid = randomUID();
        String summary = "MWB47Test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Task task = generateTask(start, end, uid, summary);
        task = create(folderID, task);
        /*
         * check that the sync token has changed
         */
        assertNotEquals(syncToken.getToken(), fetchSyncToken(folderID), "Sync-Token not changed");
        /*
         * check that the task is included in sync report
         */
        Map<String, String> eTags = syncCollection(syncToken, folderID).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<String> hrefs = new ArrayList<String>();
        for (String href : eTags.keySet()) {
            hrefs.add(getBaseUri() + href);
        }
        List<ICalResource> calendarData = calendarMultiget(folderID, hrefs);
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVTodo(), "No VTODO in iCal found");
        assertEquals(summary, iCalResource.getVTodo().getSummary(), "SUMMARY wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSyncUpdate(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String folderID = String.valueOf(getClient().getValues().getPrivateTaskFolder());
        /*
         * create task on server
         */
        String uid = randomUID();
        String summary = "MWB47Test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Task task = generateTask(start, end, uid, summary);
        task = create(folderID, task);
        /*
         * fetch sync token prior update
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken(folderID));
        /*
         * update task on server
         */
        String updatedSummary = task.getTitle() + "_updated";
        task.setTitle(updatedSummary);
        task = update(task);
        /*
         * check that the sync token has changed
         */
        assertNotEquals(syncToken.getToken(), fetchSyncToken(folderID), "Sync-Token not changed");
        /*
         * check that the task is included in sync report
         */
        Map<String, String> eTags = syncCollection(syncToken, folderID).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<String> hrefs = new ArrayList<String>();
        for (String href : eTags.keySet()) {
            hrefs.add(getBaseUri() + href);
        }
        List<ICalResource> calendarData = calendarMultiget(folderID, hrefs);
        ICalResource iCalResource = assertContains(uid, calendarData);
        assertNotNull(iCalResource.getVTodo(), "No VTODO in iCal found");
        assertEquals(updatedSummary, iCalResource.getVTodo().getSummary(), "SUMMARY wrong");
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void testSyncDeletion(String authMethod) throws Exception {
        /*
         * prepare OAuthClient if authMethod is OAuth
         */
        if(authMethod.equals("OAuth")) prepareOAuthClient();

        String folderID = String.valueOf(getClient().getValues().getPrivateTaskFolder());
        /*
         * create task on server
         */
        String uid = randomUID();
        String summary = "MWB47Test";
        Date start = TimeTools.D("next friday at 11:30");
        Date end = TimeTools.D("next friday at 12:45");
        Task task = generateTask(start, end, uid, summary);
        task = create(folderID, task);
        /*
         * fetch sync token prior delete
         */
        SyncToken syncToken = new SyncToken(fetchSyncToken(folderID));
        /*
         * delete task on server
         */
        delete(task);
        /*
         * check that the sync token has changed
         */
        assertNotEquals(syncToken.getToken(), fetchSyncToken(folderID), "Sync-Token not changed");
        /*
         * check that the task is included in sync report
         */
        List<String> hrefsStatusNotFound = syncCollection(syncToken, folderID).getHrefsStatusNotFound();
        assertTrue(0 < hrefsStatusNotFound.size(), "no resource deletions reported on sync collection");
        boolean found = false;
        for (String href : hrefsStatusNotFound) {
            if (null != href && href.contains(uid)) {
                found = true;
                break;
            }
        }
        assertTrue(found, "task not reported as deleted");
    }

}
