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
import static com.openexchange.java.Autoboxing.i;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MWB1098Test}
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Schuerholz</a>
 * @since v8.0.0
 */
public class MWB1098Test extends AbstractAPIClientSession {

    private static final Integer ADMIN_PERMISSION = I(272662788);
    private static final Integer OWNER_PERMISSION = I(403710016);
    private static final int PERMISSIONS_FOLDERADMIN = 268435456;
    private FolderManager folderManagerA;
    private FolderManager folderManagerB;
    private String testFolder;

    public MWB1098Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderManagerA = new FolderManager(new FoldersApi(getApiClient()), "0");
        folderManagerB = new FolderManager(new FoldersApi(testUser2.getApiClient()), "0");

        testFolder = folderManagerA.createFolder(folderManagerA.findInfostoreRoot(), "MWB1098TestFolder", FolderManager.INFOSTORE);

    }

    /**
     * 
     * Tests whether the system permission for a user are preserved after the user has got
     * admin rights to a folder, has created a subfolder and then gets the permissions revoked.
     *
     * @throws ApiException
     */
    @Test
    public void testSystemPermissionAfterAccessRevokeForCreatorOfSubfolder() throws ApiException {
        // User A shares parent folder with user B
        List<FolderPermission> permissionsParent = new ArrayList<>();
        FolderPermission userAPerm = new FolderPermission();
        userAPerm.setEntity(I(testUser.getUserId()));
        userAPerm.setBits(OWNER_PERMISSION);
        userAPerm.setIdentifier(I(testUser.getUserId()).toString());
        userAPerm.setGroup(Boolean.FALSE);
        permissionsParent.add(userAPerm);
        FolderPermission userBPerm = new FolderPermission();
        userBPerm.setEntity(I(testUser2.getUserId()));
        userBPerm.setBits(ADMIN_PERMISSION);
        userBPerm.setIdentifier(I(testUser2.getUserId()).toString());
        userBPerm.setGroup(Boolean.FALSE);
        permissionsParent.add(userBPerm);
        String folderIdParent = folderManagerA.createFolder(testFolder, "Parent", permissionsParent);

        // User B creates subfolder
        String folderIdSubfolder = folderManagerB.createFolder(folderIdParent, "Subfolder", FolderManager.INFOSTORE);

        // User A revokes access for user B for parent folder
        FolderData folderData = new FolderData();
        folderData.setId(folderIdParent);
        List<FolderPermission> updatedPermissionsParent = new ArrayList<>();
        updatedPermissionsParent.add(userAPerm);
        folderData.setPermissions(updatedPermissionsParent);
        folderManagerA.setLastTimestamp(folderManagerB.getLastTimestamp());
        folderManagerA.updateFolder(folderIdParent, folderData, null);

        // Checks permissions for user B in subfolder
        FolderData folder = folderManagerB.getFolder(folderIdSubfolder);
        List<FolderPermission> permissions = folder.getPermissions();
        assertNotNull(permissions);
        assertEquals(2, permissions.size());
        boolean permissionForUserBFound = false;
        for (FolderPermission p : permissions) {
            if (i(p.getEntity()) == testUser2.getUserId() && i(p.getBits()) >= PERMISSIONS_FOLDERADMIN) {
                permissionForUserBFound = true;
            }
        }
        assertTrue(permissionForUserBFound, "User B has no permission to see the subfolder.");

        // Ensures, that it is possible to request folder data of parent folder via user B (has system permission)
        try {
            folder = folderManagerB.getFolder(folderIdParent);
        } catch (AssertionError e) {
            fail("User B has no system permission for the parent folder: " + e.getMessage());
        }
        permissions = folder.getPermissions();
        assertNotNull(permissions);
    }

}
