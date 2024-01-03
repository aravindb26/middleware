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

package com.openexchange.ajax.drive.apiclient.test;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.DriveUploadResponse;
import com.openexchange.testing.httpclient.models.FoldersResponse;
import com.openexchange.testing.httpclient.models.TrashContent;
import com.openexchange.testing.httpclient.models.TrashFolderResponse;
import com.openexchange.testing.httpclient.models.TrashTargetsBody;
import com.openexchange.testing.httpclient.modules.DriveApi;
import jonelo.jacksum.algorithm.MD;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link TrashTests}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class TrashTests extends AbstractAPIClientSession {

    private DriveApi driveApi;
    private ApiClient client;
    private FolderApi folderApi;
    private FolderManager folderManager;
    private String rootId;
    private String infostoreFolder;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getApiClient();
        folderApi = new FolderApi(client, testUser);
        infostoreFolder = folderApi.getInfostoreFolder();
        folderManager = new FolderManager(folderApi, "0");

        FoldersResponse rootFolders = folderApi.getFoldersApi().getRootFolders("1,319", null, "infostore");
        rootId = getFolderId("Infostore", rootFolders);
        FoldersResponse subFolders = folderApi.getFoldersApi().getSubFolders(rootId, "1,319", I(1), "0", "infostore", null, null, Boolean.FALSE);
        String trashId = getFolderId("Trash", subFolders);
        driveApi = new DriveApi(client);
        driveApi.emptyTrash(rootId);
        byte[] body = new byte[4];
        DriveUploadResponse uploadFile = driveApi.uploadFile(trashId, "/", "test.txt", getChecksum(body), body, I(1), null, null, "text/plain", L(0), Long.valueOf(4), null, null, null, null, null);
        assertNull(uploadFile.getError(), uploadFile.getErrorDesc());

        folderManager.createFolder(trashId, "trashedFolder", "infostore");
    }

    private String getFolderId(String name, FoldersResponse folders) {
        assertNull(folders.getError());
        Object data = folders.getData();
        @SuppressWarnings("unchecked") ArrayList<ArrayList<Object>> rootFolderArray = (ArrayList<ArrayList<Object>>) data;
        String Id = null;
        for(ArrayList<Object> obj : rootFolderArray) {
            if (obj.get(1).equals(name)) {
                Id = (String) obj.get(0);
                break;
            }
        }
        assertNotNull(Id);
        return Id;
    }

    @Test
    public void testTrashContent() throws ApiException {
        TrashFolderResponse trashContent = driveApi.getTrashContent(String.valueOf(infostoreFolder));
        assertNull(trashContent.getError());
        assertNotNull(trashContent.getData());
        TrashContent data = trashContent.getData();
        assertNotNull(data.getFiles());
        assertFalse(data.getFiles().isEmpty());
        assertEquals(1, data.getFiles().size());
        assertNotNull(data.getDirectories());
        assertFalse(data.getDirectories().isEmpty());
        assertEquals(1, data.getDirectories().size());
    }

    @Test
    public void testDeleteFromTrash() throws ApiException {
        TrashFolderResponse trashContent = driveApi.getTrashContent(String.valueOf(infostoreFolder));
        TrashContent data = trashContent.getData();

        TrashTargetsBody body = new TrashTargetsBody();
        body.addFilesItem(data.getFiles().get(0).getName());
        TrashFolderResponse removeFromTrash = driveApi.deleteFromTrash(String.valueOf(infostoreFolder), body);
        assertNull(removeFromTrash.getError(), removeFromTrash.getErrorDesc());

        trashContent = driveApi.getTrashContent("/");
        assertNull(trashContent.getError());
        assertNotNull(trashContent.getData());
        data = trashContent.getData();
        assertNull(data.getFiles());
        assertNotNull(data.getDirectories());
        assertFalse(data.getDirectories().isEmpty());
        assertEquals(1, data.getDirectories().size());
    }

    @Test
    public void testRestoreFromTrash() throws ApiException {
        TrashFolderResponse trashContent = driveApi.getTrashContent(String.valueOf(infostoreFolder));
        TrashContent data = trashContent.getData();

        TrashTargetsBody body = new TrashTargetsBody();
        body.addFilesItem(data.getFiles().get(0).getName());
        TrashFolderResponse restoredFromTrash = driveApi.restoreFromTrash(String.valueOf(infostoreFolder), body);
        assertNull(restoredFromTrash.getError(), restoredFromTrash.getErrorDesc());
        assertNotNull(restoredFromTrash.getData());
        TrashContent restoreData = restoredFromTrash.getData();
        assertNull(restoreData.getFiles());

        trashContent = driveApi.getTrashContent(String.valueOf(infostoreFolder));
        assertNull(trashContent.getError());
        assertNotNull(trashContent.getData());
        data = trashContent.getData();
        assertNull(data.getFiles());
        assertNotNull(data.getDirectories());
        assertFalse(data.getDirectories().isEmpty());
        assertEquals(1, data.getDirectories().size());
    }

    protected String getChecksum(byte[] bytes) throws Exception {
        MD md5 = new MD("MD5");
        md5.update(bytes);
        return md5.getFormattedValue();
    }

}
