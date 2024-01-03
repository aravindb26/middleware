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

import static com.openexchange.ajax.folder.manager.FolderPermissionsBits.ADMIN;
import static com.openexchange.java.Autoboxing.I;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.TestInfo;

/**
 * 
 * {@link MWB1418Test}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.0.0
 */
public class MWB1418Test extends AbstractAPIClientSession {

    private FolderManager folderManagerA;
    private FolderManager folderManagerB;
    private String testFolder;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderManagerA = new FolderManager(new FoldersApi(getApiClient()), "0");
        folderManagerB = new FolderManager(new FoldersApi(testUser2.getApiClient()), "0");

        testFolder = folderManagerA.createFolder(folderManagerA.findInfostoreRoot(), "MWB1418TestTestFolder", FolderManager.INFOSTORE);
        folderManagerA.createFolder(testFolder, "SubfolderOne", FolderManager.INFOSTORE);
        folderManagerA.createFolder(testFolder, "SubfolderTwo", FolderManager.INFOSTORE);
        folderManagerA.createFolder(testFolder, "SubfolderThree", FolderManager.INFOSTORE);
    }

    @Test
    public void testAuthorCreatesSubfolderWithoutOwner() throws ApiException {
        Integer userId2 = I(testUser2.getUserId());
        /*
         * User A shares parent folder with user B
         */
        FolderData parentFolder = folderManagerA.getFolder(testFolder);
        FolderData folder = prepareFolder(parentFolder, testUser2);
        String folderIdParent = folderManagerA.updateFolder(testFolder, folder, "Now you are author for my folder");
        /*
         * User B creates subfolder
         */
        String withoutOwnerFolderId = folderManagerB.createFolder(folderIdParent, "FolderWithoutYou", singletonList(ADMIN.getPermission(userId2)));
        FolderData withoutOwnerFolder = folderManagerB.getFolder(withoutOwnerFolderId);
        /*
         * Checks permissions for user B in subfolder
         */
        List<FolderPermission> permissions = withoutOwnerFolder.getPermissions();
        assertNotNull(permissions);
        assertEquals(1, permissions.size());
        FolderPermission p = permissions.get(0);
        assertThat("Wrong user", p.getEntity(), is(userId2));
        assertThat("Wrong user", p.getBits(), greaterThanOrEqualTo(userId2));
        /*
         * Share folder with antoher user, should fail
         */
        TestUser testUser3 = testContext.acquireUser();
        parentFolder = folderManagerA.getFolder(testFolder);
        folder = prepareFolder(parentFolder, testUser3);
        String error = folderManagerA.updateFolderExpectingError(testFolder, folder, "A new author for my folder", Boolean.TRUE, null, Boolean.FALSE);
        assertThat("Wrong error message", error, is("The folder was not updated. Please review the warnings for details."));

        /*
         * Share by ignoring warnings
         */
        folderIdParent = folderManagerA.updateFolder(testFolder, folder, "Ignore those subfolders, add my new author", Boolean.TRUE, null, Boolean.TRUE);
        parentFolder = folderManagerA.getFolder(testFolder);
        List<FolderPermission> parentPermissions = parentFolder.getPermissions();
        assertNotNull(parentPermissions);
        assertEquals(3, parentPermissions.size());
    }

    private FolderData prepareFolder(FolderData parentFolder, TestUser user) {
        List<FolderPermission> permissionsParent = new ArrayList<>(parentFolder.getPermissions());
        permissionsParent.add(ADMIN.getPermission(user.getUserId()));
        FolderData folder = new FolderData();
        folder.setId(parentFolder.getId());
        folder.setAccountId(parentFolder.getAccountId());
        folder.setFolderId(parentFolder.getFolderId());
        folder.setPermissions(permissionsParent);
        return folder;
    }

}
