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

package com.openexchange.ajax.folder;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.java.Strings;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.TestInfo;


/**
 * {@link MWB938Test}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.6
 */
public class MWB938Test extends AbstractConfigAwareAPIClientSession {

    private FolderApi folderApiAdmin;
    private String rootFolderId;
    private FolderManager adminFolderManager;
    private TestUser testUser3;

    @Test
    public void testAlterPermissionsAsNonAdminUser() throws Exception {
        FoldersApi foldersApiViewer = new FoldersApi(testUser2.generateApiClient());
        FolderPermission testUserPermission = new FolderPermission();
        testUserPermission.setBits(I(257));
        testUserPermission.setEntity(I(testUser3.getUserId()));
        testUserPermission.setGroup(Boolean.FALSE);
        FolderData data = new FolderData();
        data.addPermissionsItem(testUserPermission);
        FolderBody body = new FolderBody();
        body.setFolder(data);
        FolderUpdateResponse response = foldersApiViewer.updateFolder(rootFolderId, body, Boolean.TRUE, L(System.currentTimeMillis()), "1", "infostore", Boolean.TRUE, null, Boolean.FALSE, Boolean.FALSE);

        // FolderExceptionErrorMessage.FOLDER_UPDATE_ABORTED -> FLD-1038
        assertTrue("FLD-1038".equals(response.getCode()));

        FolderResponse folderResponse = foldersApiViewer.getFolder(rootFolderId, "1", "infostore", null, null);
        assertNotNull(folderResponse.getData());
        FolderData folder = folderResponse.getData();

        // testUser3 was not added, must not be in cache
        assertNotNull(folder.getPermissions());
        assertEquals(2, folder.getPermissions().size());
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderApiAdmin = new FolderApi(testUser.generateApiClient(), testUser);
        adminFolderManager = new FolderManager(folderApiAdmin.getFoldersApi(), "1");
        testUser3 = testContext.acquireUser();
        rootFolderId = adminFolderManager.createFolder(folderApiAdmin.getInfostoreFolder(), "MWB938Test" + UUID.randomUUID().toString(), "infostore");
        String subFolderId = adminFolderManager.createFolder(rootFolderId, "Sub_MWB938Test" + UUID.randomUUID().toString(), "infostore");

        FolderPermission adminPerm = new FolderPermission();
        adminPerm.setBits(I(272662788));
        adminPerm.setEntity(I(testUser2.getUserId()));
        adminPerm.setGroup(Boolean.FALSE);
        FolderData rootFolderData = new FolderData();
        rootFolderData.addPermissionsItem(adminPerm);
        adminFolderManager.updateFolder(rootFolderId, rootFolderData, "");

        FolderPermission viewerPerm = new FolderPermission();
        viewerPerm.setBits(I(257));
        viewerPerm.setEntity(I(testUser2.getUserId()));
        viewerPerm.setGroup(Boolean.FALSE);
        FolderData subFolderData = new FolderData();
        subFolderData.addPermissionsItem(viewerPerm);
        adminFolderManager.updateFolder(subFolderId, subFolderData, "");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (null != adminFolderManager && Strings.isNotEmpty(rootFolderId)) {
            adminFolderManager.deleteFolder(Collections.singletonList(rootFolderId));
        }
    }

}
