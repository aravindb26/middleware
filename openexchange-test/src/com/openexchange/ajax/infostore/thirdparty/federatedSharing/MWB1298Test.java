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

package com.openexchange.ajax.infostore.thirdparty.federatedSharing;

import static com.openexchange.ajax.folder.manager.FolderManager.INFOSTORE;
import static com.openexchange.groupware.container.FolderObject.SYSTEM_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.container.FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.container.FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID;
import static com.openexchange.java.Autoboxing.B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.configuration.AJAXConfig.Property;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.ShareLinkAnalyzeResponseData.StateEnum;
import com.openexchange.testing.httpclient.modules.ShareManagementApi;

/**
 * {@link MWB1298Test}
 *
 * arrow symbol disappears after unfolding in File Share Guest UI
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1298Test extends AbstractShareManagementTest {

    private TestContext testContext2;
    private ShareManagementApi smApiC2;
    private FolderManager folderManagerC2;
    private SessionAwareClient apiClientC2;
    private TestUser testUserC2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        testContext2 = testContextList.get(1);
        testUserC2 = testContext2.acquireUser();
        apiClientC2 = testUserC2.getApiClient();
        smApiC2 = new ShareManagementApi(apiClientC2);
        folderManagerC2 = new FolderManager(new FolderApi(apiClientC2, testUserC2), "1");
        sharedFolderName = this.getClass().getSimpleName() + UUID.randomUUID().toString();
        smApi = new ShareManagementApi(getApiClient());
        folderManager = new FolderManager(new FolderApi(getApiClient(), testUser), "1");
        infostoreRoot = folderManager.findInfostoreRoot();
    }

    @Override
    public TestClassConfig getTestConfig() {
        HashMap<String, String> contextConfig = new HashMap<>();
        contextConfig.put("com.openexchange.capability.filestorage_xox", Boolean.TRUE.toString());
        contextConfig.put("com.openexchange.capability.filestorage_xctx", Boolean.FALSE.toString());
        contextConfig.put("com.openexchange.api.client.blacklistedHosts", "");
        contextConfig.put("com.openexchange.share.guestHostname", AJAXConfig.getProperty(Property.SINGLENODE_HOSTNAME));
        return TestClassConfig.builder()
            .withContexts(2)
            .withContextConfig(TestContextConfig.builder().withConfig(contextConfig).build()).build();
    }
    
    @Test
    public void testPublicFiles() throws Exception {
        /*
         * check public files folder of user in context 2, expecting to be visible by default, but w/o subfolders
         */
        checkPublicFilesVisible(folderManagerC2, true);
        checkPublicFilesHasSubfolders(folderManagerC2, false);
        /*
         * share a public folder from context 1 to user in context 2
         */
        String folderId = createPublicFolder();
        FolderData folder = folderManager.getFolder(folderId);
        ArrayList<FolderPermission> updatedPermissions = new ArrayList<FolderPermission>(folder.getPermissions());
        updatedPermissions.add(prepareGuest(testUserC2));
        folderId = setFolderPermission(folderId, updatedPermissions);
        /*
         * receive & analyze share invitation as user in context 2, then add the share
         */
        String shareLink = FederatedSharingUtil.receiveShareLink(apiClientC2, testUser.getLogin(), folder.getTitle());
        analyze(smApiC2, shareLink, StateEnum.ADDABLE);
        addOXShareAccount(smApiC2, shareLink, null);
        /*
         * check public files folder is visible and indicates subfolders
         */
        checkPublicFilesVisible(folderManagerC2, true);
        checkPublicFilesHasSubfolders(folderManagerC2, true);
        /*
         * unsubscribe from share again
         */
        unsubscribe(smApiC2, shareLink);
        /*
         * re-check public files folder to no longer indicate subfolders
         */
        checkPublicFilesVisible(folderManagerC2, true);
        checkPublicFilesHasSubfolders(folderManagerC2, false);
    }

    private String createPublicFolder() throws ApiException {
        return folderManager.createFolder(String.valueOf(SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID), UUIDs.getUnformattedStringFromRandom(), INFOSTORE);
    }

    @Test
    public void testSharedFiles() throws Exception {
        /*
         * check shared files folder of user in context 2, expecting to be invisible as of now
         */
        checkSharedFilesVisible(folderManagerC2, false);
        /*
         * share a private folder from context 1 to user in context 2
         */
        String folderId = createFolder();
        FolderData folder = folderManager.getFolder(folderId);
        ArrayList<FolderPermission> updatedPermissions = new ArrayList<FolderPermission>(folder.getPermissions());
        updatedPermissions.add(prepareGuest(testUserC2));
        folderId = setFolderPermission(folderId, updatedPermissions);
        /*
         * receive & analyze share invitation as user in context 2, then add the share
         */
        String shareLink = receiveShareLink(apiClientC2, testUser.getLogin());
        analyze(smApiC2, shareLink, StateEnum.ADDABLE);
        addOXShareAccount(smApiC2, shareLink, null);
        /*
         * check shared files folder has appeared and indicates subfolders
         */
        checkSharedFilesVisible(folderManagerC2, true);
        checkSharedFilesHasSubfolders(folderManagerC2, true);
        /*
         * unsubscribe from share again
         */
        unsubscribe(smApiC2, shareLink);
        /*
         * re-check shared files folder is no longer indicated
         */
        checkSharedFilesVisible(folderManagerC2, false);
    }

    private static void checkSharedFilesHasSubfolders(FolderManager folderManager, boolean expectSubfolders) throws Exception {
        checkHasSubfolders(folderManager, String.valueOf(SYSTEM_USER_INFOSTORE_FOLDER_ID), expectSubfolders);
    }

    private static void checkPublicFilesHasSubfolders(FolderManager folderManager, boolean expectSubfolders) throws Exception {
        checkHasSubfolders(folderManager, String.valueOf(SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID), expectSubfolders);
    }

    private static void checkHasSubfolders(FolderManager folderManager, String folderId, boolean expectSubfolders) throws Exception {
        FolderData folderData = folderManager.getFolder(folderId);
        assertEquals(B(expectSubfolders), folderData.getSubfolders());
        ArrayList<ArrayList<Object>> subfolders = folderManager.listFolders(folderData.getId(), "1,20,300,301,302", Boolean.FALSE, Boolean.TRUE);
        if (expectSubfolders) {
            assertTrue(null != subfolders && 0 < subfolders.size());
        } else {
            assertTrue(null == subfolders || 0 == subfolders.size());
        }
    }

    private static void checkSharedFilesVisible(FolderManager folderManager, boolean expectVisible) throws Exception {
        checkVisibleBelowRoot(folderManager, String.valueOf(SYSTEM_USER_INFOSTORE_FOLDER_ID), expectVisible);
    }

    private static void checkPublicFilesVisible(FolderManager folderManager, boolean expectVisible) throws Exception {
        checkVisibleBelowRoot(folderManager, String.valueOf(SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID), expectVisible);
    }

    private static void checkVisibleBelowRoot(FolderManager folderManager, String folderId, boolean expectVisible) throws Exception {
        FolderData infostoreRootFolder = folderManager.getFolder(String.valueOf(SYSTEM_INFOSTORE_FOLDER_ID));
        ArrayList<ArrayList<Object>> subfolders = folderManager.listFolders(infostoreRootFolder.getId(), "1,20,300,301,302", Boolean.FALSE, Boolean.TRUE);
        boolean found = false;
        for (ArrayList<Object> subfolder : subfolders) {
            if (folderId.equals(String.valueOf(subfolder.get(0)))) {
                found = true;
                break;
            }
        }
        assertEquals(B(expectVisible), B(found));
    }

}
