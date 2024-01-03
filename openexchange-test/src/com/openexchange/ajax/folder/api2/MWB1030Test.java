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

package com.openexchange.ajax.folder.api2;

import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.util.Pair;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FolderUpdatesResponse;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MWB1030Test}
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Schuerholz</a>
 * @since v7.10.6
 */
public class MWB1030Test extends AbstractAPIClientSession {

    private FoldersApi foldersApi;
    private FolderManager folderManager;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        foldersApi = new FoldersApi(getApiClient());
        folderManager = new FolderManager(foldersApi, null);
    }

    /**
     * 
     * Tests that the Folder Updates action does not return duplicates in the response
     * when called after creating a new folder (see MWB-1030).
     *
     * @throws ApiException
     */
    @Test
    public void testForDuplicatesInFoldersUpdatesAfterFolderCreation() throws Exception {
        Long timestamp = getTimestamp();
        String parent = folderManager.findInfostoreRoot();
        String newFolderId = folderManager.createFolder(parent, "TestFolder", Module.INFOSTORE.getName());
        FolderUpdatesResponse folderUpdates = foldersApi.getFolderUpdates(timestamp, "1,300", null, null, null, null);
        // Expects an array with the structure: [[folderId1, folderName1],[folderId2, folderName2],...]
        ArrayList<Object> data = (ArrayList<Object>) checkResponse(folderUpdates.getError(), folderUpdates.getErrorDesc(), folderUpdates.getData());

        // Test for no duplicates in response data
        Pair<ArrayList<String>, ArrayList<String>> pair = getFolderIds(data);
        ArrayList<String> modifiedFolderIds = pair.getFirst();
        assertTrue(modifiedFolderIds.contains(parent), "Parent folder not included in updated folders.");
        assertTrue(modifiedFolderIds.contains(newFolderId), "New created folder not included in updated folders.");
    }

    /**
     * 
     * Tests that the Folder Updates action does not return duplicates in the response
     * when called after a folder is deleted.
     *
     * @throws ApiException
     */
    @Test
    public void testForDuplicatesInFoldersUpdatesAfterFolderRemove() throws Exception {
        String parent = folderManager.findInfostoreRoot();
        String newFolderId = folderManager.createFolder(parent, "TestFolder", Module.INFOSTORE.getName());
        Long timestamp = getTimestamp();
        folderManager.deleteFolder(Collections.singletonList(newFolderId));
        FolderUpdatesResponse folderUpdates = foldersApi.getFolderUpdates(timestamp, "1,300", null, null, null, null);
        // Expects an array with the structure: [[folderId1, folderName1],[folderId2, folderName2],...]
        ArrayList<Object> data = (ArrayList<Object>) checkResponse(folderUpdates.getError(), folderUpdates.getErrorDesc(), folderUpdates.getData());

        // Test for no duplicates in response data
        Pair<ArrayList<String>, ArrayList<String>> pair = getFolderIds(data);
        ArrayList<String> modifiedFolderIds = pair.getFirst();
        ArrayList<String> deletedFolderIds = pair.getSecond();
        assertTrue(deletedFolderIds.contains(newFolderId), "Deleted folder not included in response.");
        assertTrue(modifiedFolderIds.contains(parent), "Parent folder not included in updated folders.");
    }

    private Long getTimestamp() throws Exception {
        return L(new GregorianCalendar(getClient().getValues().getTimeZone()).getTimeInMillis());
    }

    /**
     * 
     * Gets the folder ids of the modified and deleted folders from an updates response.
     *
     * @param updatesResponseData An array list with data from FolderUpdatesResponse.
     * @return A pair with two array lists. The lists contain the folder ids from the modified (1) and deleted (2) folders.
     */
    private Pair<ArrayList<String>, ArrayList<String>> getFolderIds(ArrayList<Object> updatesResponseData) {
        ArrayList<String> modifiedFolderIds = new ArrayList<String>();
        ArrayList<String> deletedFolderIds = new ArrayList<String>();
        for (Object item : updatesResponseData) {
            if (item instanceof ArrayList) {
                @SuppressWarnings("unchecked") ArrayList<String> folder = (ArrayList<String>) item;
                String folderId = folder.get(0);
                if (modifiedFolderIds.contains(folderId)) {
                    fail("There are duplicates in the response array: Folder \"" + folder.get(1) + "\" with folder id " + folderId);
                } else {
                    modifiedFolderIds.add(folderId);
                }
            } else if (item instanceof String) {
                String folderId = (String) item;
                if (deletedFolderIds.contains(folderId)) {
                    fail("There are duplicates in the response array: Folder id " + folderId);
                } else {
                    deletedFolderIds.add(folderId);
                }
            }
        }
        return new Pair<ArrayList<String>, ArrayList<String>>(modifiedFolderIds, deletedFolderIds);
    }
}
