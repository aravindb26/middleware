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

import static com.openexchange.ajax.infostore.thirdparty.federatedSharing.FederatedSharingUtil.cleanInbox;
import static com.openexchange.ajax.infostore.thirdparty.federatedSharing.FederatedSharingUtil.prepareGuest;
import static com.openexchange.ajax.infostore.thirdparty.federatedSharing.FederatedSharingUtil.receiveShareLink;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import com.google.gson.annotations.SerializedName;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.ajax.infostore.thirdparty.AbstractFileStorageAccountTest;
import com.openexchange.ajax.infostore.thirdparty.federatedSharing.FederatedSharingUtil.PermissionLevel;
import com.openexchange.test.Host;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.configuration.AJAXConfig.Property;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FileAccountData;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.ShareManagementApi;

/**
 * {@link FederatedSharingFileStorageAccountTests}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.5
 */
@Execution(SAME_THREAD)
public class FederatedSharingFileStorageAccountTests extends AbstractFileStorageAccountTest {

    private static final String XOX8 = "xox8";
    private static final String XCTX8 = "xctx8";

    protected static final String FILE_STORAGE_SERVICE_DISPLAY_NAME = "Federated Sharing test storage";

    protected ShareManagementApi shareApi;

    private FileAccountData account;
    private String sharedFolderId;
    private FoldersApi sharingFoldersApi;
    private String fileStorageServiceId;

    /**
     * {@link FederatedSharingFileAccountConfiguration}
     *
     * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
     * @since v7.10.5
     */
    public static class FederatedSharingFileAccountConfiguration {

        @SerializedName(value = "url")
        private final String shareLink;
        private final String password;

        /**
         * Initializes a new {@link FederatedSharingFileAccountConfiguration}.
         *
         * @param shareLink The share link to use
         * @param password The, optional, password
         */
        public FederatedSharingFileAccountConfiguration(String shareLink, String password) {
            super();
            this.shareLink = shareLink;
            this.password = password;
        }

        /**
         * Gets the shareLink
         *
         * @return The shareLink
         */
        public String getShareLink() {
            return shareLink;
        }

        /**
         * Gets the optional password
         *
         * @return The optional password
         */
        public String getPassword() {
            return password;
        }
    }

    /**
     * The file storages to test
     *
     * @return The storages
     */
    public static Stream<String> data() {
        //@formatter:off
        return Stream.of(
            XCTX8,
            XOX8
        );
        //@formatter:on
    }

    protected String toXOXId(FileAccountData account, String folderId) {
        return String.format("%s://%s/%s", fileStorageServiceId, account.getId(), folderId);
    }

    protected String toXOXId(FileAccountData account, String folderId, String fileId) {
        return String.format("%s://%s/%s/%s", fileStorageServiceId, account.getId(), folderId, fileId.replace("/", "=2F"));
    }

    @Override
    protected void prepareTest(String fileStorageServiceId, TestInfo testInfo) throws Exception {
        this.fileStorageServiceId = fileStorageServiceId;
        setUp(testInfo);
    }

    @Override
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.shareApi = new ShareManagementApi(getApiClient(Host.SINGLENODE));
        // Clear inbox for the user who will receive the share
        cleanInbox(getApiClient(Host.SINGLENODE));

        //Acquire a user who shares a folder
        TestUser context2user = testContextList.get(1).acquireUser();
        SessionAwareClient sharingClient = context2user.getApiClient(Host.SINGLENODE);
        sharingFoldersApi = new FoldersApi(sharingClient);
        FolderManager folderManager = new FolderManager(sharingFoldersApi, "0");

        //The sharing user create a folder which is shared to the actual user
        FolderData sharedFolder = createFolder(sharingClient, sharingFoldersApi, getPrivateInfostoreFolderID(sharingClient), getRandomFolderName());
        folderManager.getFolder(sharedFolder.getFolderId()); // Update timestamp of foldermanager
        sharedFolderId = sharedFolder.getId();

        //Share it
        shareFolder(sharedFolder, testUser, folderManager);

        //Get the share link
        String shareLink = receiveShareLink(getApiClient(Host.SINGLENODE), context2user.getLogin(), sharedFolder.getTitle());
        assertNotNull(shareLink);

        //Register an XOX account which integrates the share
        FederatedSharingFileAccountConfiguration configuration = new FederatedSharingFileAccountConfiguration(shareLink, null);
        account = createAccount(fileStorageServiceId, FILE_STORAGE_SERVICE_DISPLAY_NAME, configuration);
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).withContextConfig(getContextConfig()).build();
    }

    /**
     * Gets the {@link TestContextConfig}
     *
     * @return the {@link TestContextConfig}
     */
    private TestContextConfig getContextConfig() {
        HashMap<String, String> configuration = new HashMap<>();
        if(XOX8.equals(fileStorageServiceId)) {
            configuration.put("com.openexchange.capability.filestorage_xox", Boolean.TRUE.toString());
            configuration.put("com.openexchange.capability.filestorage_xctx", Boolean.FALSE.toString());
        } else if (XCTX8.equals(fileStorageServiceId)) {
            configuration.put("com.openexchange.capability.filestorage_xox", Boolean.FALSE.toString());
            configuration.put("com.openexchange.capability.filestorage_xctx", Boolean.TRUE.toString());
        }
        configuration.put("com.openexchange.api.client.blacklistedHosts", "");
        configuration.put("com.openexchange.share.guestHostname", AJAXConfig.getProperty(Property.SINGLENODE_HOSTNAME));
        return TestContextConfig.builder().withConfig(configuration).build();
    }

    /**
     * Shares a folder to another user
     *
     * @param folderToShare The folder to share
     * @param to The user to share the folder with
     * @param folderManager The folder manager to update the folder with
     * @throws ApiException
     */
    private void shareFolder(FolderData folderToShare, TestUser to, FolderManager folderManager) throws ApiException {
        List<FolderPermission> permissions = new ArrayList<FolderPermission>(folderToShare.getPermissions().size() + 1);

        //Take over existing permissions
        permissions.addAll(folderToShare.getPermissions());

        //And ..create new guest permission
        permissions.add(prepareGuest(to, PermissionLevel.AUTHOR));

        //Set permission
        FolderData data = new FolderData();
        data.setId(folderToShare.getId());
        data.setPermissions(permissions);

        //update with new guest permission and send mail notification
        folderManager.updateFolder(sharedFolderId, data, null);
    }

    @Override
    protected String getRootFolderId() throws Exception {
        return toXOXId(account, sharedFolderId);
    }

    @Override
    protected FileAccountData getAccountData() throws Exception {
        return account;
    }

    @Override
    protected TestFile createTestFile() throws Exception {
        String fileName = getRandomFileName();
        String newFileId = uploadInfoItem(toXOXId(account, sharedFolderId), fileName, testContent, "application/text");
        return new TestFile(toXOXId(account, sharedFolderId), newFileId, fileName);
    }

    @Override
    protected Object getWrongFileStorageConfiguration() {
        return null;
    }

}
