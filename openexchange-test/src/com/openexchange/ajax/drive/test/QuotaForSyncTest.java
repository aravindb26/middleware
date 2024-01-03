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

package com.openexchange.ajax.drive.test;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.file.storage.Quota;
import com.openexchange.java.Strings;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.DriveAction;
import com.openexchange.testing.httpclient.models.DriveExtendedAction;
import com.openexchange.testing.httpclient.models.DriveExtendedActionsResponse;
import com.openexchange.testing.httpclient.models.DriveQuota;
import com.openexchange.testing.httpclient.models.DriveQuotaResponse;
import com.openexchange.testing.httpclient.models.DriveSubfoldersResponse;
import com.openexchange.testing.httpclient.models.DriveSyncFilesBody;
import com.openexchange.testing.httpclient.models.DriveSyncFolderResponse;
import com.openexchange.testing.httpclient.models.DriveSyncFoldersBody;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.DriveApi;
import org.junit.jupiter.api.TestInfo;

/**
 *
 * {@link QuotaForSyncTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.1
 */
public class QuotaForSyncTest extends AbstractAPIClientSession {

    private DriveApi driveApi;
    private String infostoreFolder;


    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo)throws Exception {
        super.setUp(testInfo);
        driveApi = new DriveApi(getApiClient());
        infostoreFolder = getPrivateInfostoreFolder();
    }

    private String getPrivateInfostoreFolder() throws Exception {
        ConfigApi configApi = new ConfigApi(getApiClient());
        ConfigResponse configNode = configApi.getConfigNode(Tree.PrivateInfostoreFolder.getPath());
        return (configNode.getData()).toString();
    }

    @Test
    public void testSyncFolders_quotaNotSent() throws ApiException {
        DriveSyncFoldersBody body = new DriveSyncFoldersBody();
        DriveSyncFolderResponse syncFolders = driveApi.syncFolders(infostoreFolder, body, I(8), "8", null, null, null);

        assertNull(syncFolders.getError(), syncFolders.getErrorDesc());
        for (DriveAction action : syncFolders.getData().getActions()) {
            assertTrue(action.getAction().equals("sync"));
        }
        assertTrue(syncFolders.getData().getActions().size() > 2);
    }

    @Test
    public void testSyncFolders_quotaFalse() throws ApiException {
        DriveSyncFoldersBody body = new DriveSyncFoldersBody();
        DriveSyncFolderResponse syncFolders = driveApi.syncFolders(infostoreFolder, body, I(8), "8", null, Boolean.FALSE, null);

        assertNull(syncFolders.getError(), syncFolders.getErrorDesc());
        for (DriveAction action : syncFolders.getData().getActions()) {
            assertTrue(action.getAction().equals("sync"));
        }
        assertTrue(syncFolders.getData().getActions().size() > 2);
    }

    @Test
    public void testSyncFolders_quotaRequested() throws ApiException {
        DriveSyncFoldersBody body = new DriveSyncFoldersBody();
        DriveSyncFolderResponse syncFolders = driveApi.syncFolders(infostoreFolder, body, I(8), "8", null, Boolean.TRUE, null);
        assertNull(syncFolders.getError());
        //        assertNotNull(syncFolders.getData().getQuota());
    }

    @Test
    public void testSyncFolders_quotaRequestedButWrongVersion_noQuotaReturned() throws ApiException {
        DriveSyncFoldersBody body = new DriveSyncFoldersBody();
        DriveSyncFolderResponse syncFolders = driveApi.syncFolders(infostoreFolder, body, null, null, null, Boolean.TRUE, null);
        assertNull(syncFolders.getError());
        //        assertNull(syncFolders.getData().getQuota());
    }

    @Test
    public void testSyncFiles_quotaSent() throws ApiException {
        DriveSyncFilesBody body = new DriveSyncFilesBody();
        DriveSubfoldersResponse response = driveApi.getSynchronizableFolders(infostoreFolder);
        assertNull(response.getError(), response.getErrorDesc());
        String path = response.getData().get(0).getPath();
        DriveExtendedActionsResponse syncFiles = driveApi.syncFiles(infostoreFolder, path, body, I(8), null, null, Boolean.TRUE, null, null, "false");

        assertNull(syncFiles.getError());
        assertFalse(syncFiles.getData().getQuota().isEmpty());
        assertTrue(syncFiles.getData().getActions().isEmpty());
    }

    @Test
    public void testSyncFiles_quotaNotSent() throws ApiException {
        DriveSyncFilesBody body = new DriveSyncFilesBody();
        DriveSubfoldersResponse response = driveApi.getSynchronizableFolders(infostoreFolder);
        String path = response.getData().get(0).getPath();
        DriveExtendedActionsResponse syncFiles = driveApi.syncFiles(infostoreFolder, path, body, I(8), null, null, null, null, null, "false");
        assertNull(syncFiles.getError());
        List<DriveExtendedAction> actions = syncFiles.getData().getActions();
        assertTrue(actions.isEmpty(), "Actions were not empty: " + actions);
    }

    @Test
    public void testSyncFiles_quotaFalse() throws ApiException {
        DriveSyncFilesBody body = new DriveSyncFilesBody();
        DriveSubfoldersResponse synchronizableFolders = driveApi.getSynchronizableFolders(infostoreFolder);
        String path = synchronizableFolders.getData().get(0).getPath();
        DriveExtendedActionsResponse syncFiles = driveApi.syncFiles(infostoreFolder, path, body, I(8), null, null, Boolean.FALSE, null, null, "false");

        assertNull(syncFiles.getError());
        assertTrue(syncFiles.getData().getActions().isEmpty());
    }

    @Test
    public void testSyncFiles_quotaRequested() throws ApiException {
        DriveSyncFilesBody body = new DriveSyncFilesBody();
        DriveSubfoldersResponse synchronizableFolders = driveApi.getSynchronizableFolders(infostoreFolder);
        String path = synchronizableFolders.getData().get(0).getPath();
        DriveExtendedActionsResponse syncFiles = driveApi.syncFiles(infostoreFolder, path, body, I(8), null, null, Boolean.TRUE, null, null, null);
        assertNull(syncFiles.getError());
        //        assertNotNull(syncFiles.getData().getQuota());
    }

    @Test
    public void testSyncFiles_quotaRequestedButWrongVersion_noQuotaReturned() throws ApiException {
        DriveSyncFilesBody body = new DriveSyncFilesBody();
        DriveExtendedActionsResponse syncFiles = driveApi.syncFiles(infostoreFolder, "/", body, null, null, null, Boolean.TRUE, null, null, null);
        assertNull(syncFiles.getError());
        //      assertNull(syncFiles.getData().getQuota());
    }

    @Test
    public void testQuota() throws ApiException {
        DriveQuotaResponse quotaResponse = driveApi.getQuota(infostoreFolder);
        assertNull(quotaResponse.getError(), quotaResponse.getErrorDesc());
        List<DriveQuota> quota = quotaResponse.getData().getQuota();
        assertEquals(2, quota.size());
        boolean foundStorage = false;
        boolean foundFile = false;
        for (DriveQuota driveQuota : quota) {
            if (driveQuota.getType().equalsIgnoreCase(Quota.Type.FILE.toString())) {
                foundFile = true;
            } else if (driveQuota.getType().equalsIgnoreCase(Quota.Type.STORAGE.toString())) {
                foundStorage = true;
            }
        }
        assertTrue(foundStorage);
        assertTrue(foundFile);

        String manageLink = quotaResponse.getData().getManageLink();
        assertTrue(Strings.isNotEmpty(manageLink));
    }
}
