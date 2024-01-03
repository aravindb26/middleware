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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FoldersCleanUpResponse;
import com.openexchange.testing.httpclient.models.FoldersResponse;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MWB1863Test}
 * 
 * Restoring Folders in "Public Files" not working
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1863Test extends AbstractAPIClientSession {

    private static final String PUBLIC_FILES_ID = String.valueOf(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID);

    private FoldersApi foldersApi;
    private FolderManager folderManager;
    private String trashFolderId;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        foldersApi = new FoldersApi(getApiClient());
        folderManager = new FolderManager(foldersApi, null);
        trashFolderId = String.valueOf(getClient().getValues().getInfostoreTrashFolder());
    }

    @Test
    public void testRestoreBelowPublic() throws Exception {
        /*
         * create a subfolder below "public files" & soft delete it afterwards
         */
        String folderId = folderManager.createFolder(PUBLIC_FILES_ID, UUIDs.getUnformattedStringFromRandom(), "infostore");
        FolderData createdFolder = folderManager.getFolder(folderId);
        deleteFolder(foldersApi, folderId, folderManager.getLastTimestamp(), Boolean.FALSE);
        /*
         * check that the folder appears in user's trash folder 
         */
        FolderData folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNotNull(folderInTrash);
        /*
         * restore the folder to its original location
         */
        foldersApi.restoreFoldersBuilder().withRequestBody(Collections.singletonList(String.valueOf(folderInTrash.getId()))).execute();
        /*
         * verify the folder exists at the expected location
         */
        FolderData restoredFolder = lookupFolder(foldersApi, PUBLIC_FILES_ID, createdFolder.getTitle());
        assertNotNull(restoredFolder);
        /*
         * verify the folder does no longer appear in user's trash folder
         */
        folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNull(folderInTrash);
    }

    @Test
    public void testRestoreSubfolderBelowPublic() throws Exception {
        /*
         * create a subfolder in a subfolder below "public files" & soft delete it afterwards
         */
        String parentFolderId = folderManager.createFolder(PUBLIC_FILES_ID, UUIDs.getUnformattedStringFromRandom(), "infostore");
        String folderId = folderManager.createFolder(parentFolderId, UUIDs.getUnformattedStringFromRandom(), "infostore");
        FolderData createdFolder = folderManager.getFolder(folderId);
        deleteFolder(foldersApi, folderId, folderManager.getLastTimestamp(), Boolean.FALSE);
        /*
         * check that the folder appears in user's trash folder
         */
        FolderData folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNotNull(folderInTrash);
        /*
         * restore the folder to its original location
         */
        foldersApi.restoreFoldersBuilder().withRequestBody(Collections.singletonList(String.valueOf(folderInTrash.getId()))).execute();
        /*
         * verify the folder exists at the expected location
         */
        FolderData restoredFolder = lookupFolder(foldersApi, parentFolderId, createdFolder.getTitle());
        assertNotNull(restoredFolder);
        /*
         * verify the folder does no longer appear in user's trash folder
         */
        folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNull(folderInTrash);
    }

    @Test
    public void testRestoreSubfolderBelowPublicAfterParentDeleted() throws Exception {
        /*
         * create a subfolder in a subfolder below "public files" & soft delete it afterwards
         */
        String parentFolderId = folderManager.createFolder(PUBLIC_FILES_ID, UUIDs.getUnformattedStringFromRandom(), "infostore");
        String folderId = folderManager.createFolder(parentFolderId, UUIDs.getUnformattedStringFromRandom(), "infostore");
        FolderData createdFolder = folderManager.getFolder(folderId);
        deleteFolder(foldersApi, folderId, folderManager.getLastTimestamp(), Boolean.FALSE);
        /*
         * check that the folder appears in user's trash folder
         */
        FolderData folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNotNull(folderInTrash);
        /*
         * soft delete the parent folder as well
         */
        FolderData createdParentFolder = folderManager.getFolder(parentFolderId);
        deleteFolder(foldersApi, createdParentFolder.getId(), folderManager.getLastTimestamp(), Boolean.FALSE);
        /*
         * check that this folder appears in user's trash folder as well
         */
        FolderData parentFolderInTrash = lookupFolder(foldersApi, trashFolderId, createdParentFolder.getTitle());
        assertNotNull(parentFolderInTrash);
        /*
         * restore the folder to its original location
         */
        foldersApi.restoreFoldersBuilder().withRequestBody(Collections.singletonList(String.valueOf(folderInTrash.getId()))).execute();
        /*
         * verify the folder exists at the expected location
         */
        FolderData restoredParentFolder = lookupFolder(foldersApi, PUBLIC_FILES_ID, createdParentFolder.getTitle());
        assertNotNull(restoredParentFolder);
        FolderData restoredFolder = lookupFolder(foldersApi, restoredParentFolder.getId(), createdFolder.getTitle());
        assertNotNull(restoredFolder);
        /*
         * verify the folder does no longer appear in user's trash folder
         */
        folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNull(folderInTrash);
    }

    @Test
    public void testRestoreSubfolderBelowPublicAfterParentHardDeleted() throws Exception {
        /*
         * create a subfolder in a subfolder below "public files" & soft delete it afterwards
         */
        String parentFolderId = folderManager.createFolder(PUBLIC_FILES_ID, UUIDs.getUnformattedStringFromRandom(), "infostore");
        String folderId = folderManager.createFolder(parentFolderId, UUIDs.getUnformattedStringFromRandom(), "infostore");
        FolderData createdFolder = folderManager.getFolder(folderId);
        deleteFolder(foldersApi, folderId, folderManager.getLastTimestamp(), Boolean.FALSE);
        /*
         * check that the folder appears in user's trash folder
         */
        FolderData folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNotNull(folderInTrash);
        /*
         * hard delete the parent folder
         */
        FolderData createdParentFolder = folderManager.getFolder(parentFolderId);
        deleteFolder(foldersApi, createdParentFolder.getId(), folderManager.getLastTimestamp(), Boolean.TRUE);
        /*
         * check that this folder does not appear in user's trash folder
         */
        FolderData parentFolderInTrash = lookupFolder(foldersApi, trashFolderId, createdParentFolder.getTitle());
        assertNull(parentFolderInTrash);
        /*
         * restore the folder to its original location
         */
        foldersApi.restoreFoldersBuilder().withRequestBody(Collections.singletonList(String.valueOf(folderInTrash.getId()))).execute();
        /*
         * verify the folder exists at the expected location
         */
        FolderData restoredParentFolder = lookupFolder(foldersApi, PUBLIC_FILES_ID, createdParentFolder.getTitle());
        assertNotNull(restoredParentFolder);
        FolderData restoredFolder = lookupFolder(foldersApi, restoredParentFolder.getId(), createdFolder.getTitle());
        assertNotNull(restoredFolder);
        /*
         * verify the folder does no longer appear in user's trash folder
         */
        folderInTrash = lookupFolder(foldersApi, trashFolderId, createdFolder.getTitle());
        assertNull(folderInTrash);
    }

    private static void deleteFolder(FoldersApi foldersApi, String folderId, Long clientTimestamp, Boolean hardDelete) throws Exception {
        FoldersCleanUpResponse cleanUpResponse = foldersApi.deleteFoldersBuilder() // @formatter:off
            .withHardDelete(hardDelete)
            .withRequestBody(Collections.singletonList(folderId))
            .withTimestamp(clientTimestamp)
        .execute(); // @formatter:on
        assertNull(cleanUpResponse.getErrorDesc(), cleanUpResponse.getError());
        assertTrue(null == cleanUpResponse.getData() || 0 == cleanUpResponse.getData().size());
    }

    private static FolderData lookupFolder(FoldersApi foldersApi, String parentFolderId, String name) throws Exception {
        FoldersResponse subfoldersResponse = foldersApi.getSubFoldersBuilder() // @formatter:off
            .withColumns("1,300")
            .withParent(parentFolderId)
            .withAltNames(Boolean.TRUE)
        .execute(); // @formatter:on
        assertNull(subfoldersResponse.getErrorDesc(), subfoldersResponse.getError());
        String matchingFolderId = null;
        for (ArrayList<Object> folderProperties : (ArrayList<ArrayList<Object>>) subfoldersResponse.getData()) {
            assertEquals(2, folderProperties.size());
            if (Objects.equals(name, folderProperties.get(1))) {
                matchingFolderId = String.valueOf(folderProperties.get(0));
                break;
            }
        }
        if (null == matchingFolderId) {
            return null;
        }
        FolderResponse getResponse = foldersApi.getFolderBuilder().withId(matchingFolderId).withAltNames(Boolean.TRUE).execute();
        assertNull(getResponse.getErrorDesc(), getResponse.getError());
        return getResponse.getData();
    }

}
