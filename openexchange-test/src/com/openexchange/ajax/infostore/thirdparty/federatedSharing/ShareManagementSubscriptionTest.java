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

import static com.openexchange.ajax.infostore.thirdparty.federatedSharing.FederatedSharingUtil.clearAccountError;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.slf4j.LoggerFactory;
import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.ajax.framework.config.util.TestConfigurationChangeUtil;
import com.openexchange.ajax.framework.config.util.TestConfigurationChangeUtil.Scopes;
import com.openexchange.ajax.passwordchange.actions.PasswordChangeUpdateRequest;
import com.openexchange.ajax.passwordchange.actions.PasswordChangeUpdateResponse;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.test.Host;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.configuration.AJAXConfig.Property;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.FileAccountData;
import com.openexchange.testing.httpclient.models.FileAccountResponse;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.models.ShareLinkAnalyzeResponse;
import com.openexchange.testing.httpclient.models.ShareLinkAnalyzeResponseData.StateEnum;
import com.openexchange.testing.httpclient.models.ShareLinkData;
import com.openexchange.testing.httpclient.models.SubscribeShareBody;
import com.openexchange.testing.httpclient.models.SubscribeShareResponseData;
import com.openexchange.testing.httpclient.modules.FilestorageApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;
import com.openexchange.testing.httpclient.modules.ShareManagementApi;
import com.openexchange.tools.id.IDMangler;

/**
 * {@link ShareManagementSubscriptionTest} - Test for the <code>analyze</code> action of the share management module.
 * <p>
 * User 1 from context A will share the folder
 * User 2 from context B will analyze the share
 * <p>
 * For local testing set
 * <code>com.openexchange.capability.xctx=true</code>
 * <code>com.openexchange.capability.xox=false</code>
 * <code>com.openexchange.api.client.blacklistedHosts=""</code>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.5
 */
@Execution(SAME_THREAD)
public class ShareManagementSubscriptionTest extends AbstractShareManagementTest {

    /* Context 2 */
    private TestContext testContext2;
    private ShareManagementApi smApiC2;
    private SubscribeShareResponseData accountData;
    private SessionAwareClient apiClientC2;
    private TestUser testUserC2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        testContext2 = testContextList.get(1);
        testUserC2 = testContext2.acquireUser();
        apiClientC2 = testUserC2.getApiClient(Host.SINGLENODE);
        smApiC2 = new ShareManagementApi(apiClientC2);
        sharedFolderName = this.getClass().getSimpleName() + UUID.randomUUID().toString();
        smApi = new ShareManagementApi(getApiClient(Host.SINGLENODE));
        folderManager = new FolderManager(new FolderApi(getApiClient(Host.SINGLENODE), testUser), "1");
        infostoreRoot = folderManager.findInfostoreRoot();
        TestConfigurationChangeUtil.changeConfigWithOwnClient(AJAXConfig.getProperty(AJAXConfig.Property.SINGLENODE_HOSTNAME), testUser, Map.of("com.openexchange.passwordchange.db.enabled", Boolean.TRUE.toString()), Scopes.CONTEXT, null);
        TestConfigurationChangeUtil.changeConfigWithOwnClient(AJAXConfig.getProperty(AJAXConfig.Property.SINGLENODE_HOSTNAME), testUserC2, Map.of("com.openexchange.passwordchange.db.enabled", Boolean.TRUE.toString()), Scopes.CONTEXT, null);
    }

    @Override
    public TestClassConfig getTestConfig() {
        // @formatter:off
        return TestClassConfig.builder()
                              .withContexts(2)
                              .withContextConfig(TestContextConfig.builder()
                                                                  .withConfig(getContextConfig())
                                                                  .build())
                              .build();
        // @formatter:on
    }

    public Map<String, String> getContextConfig() {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("com.openexchange.capability.filestorage_xox", Boolean.TRUE.toString());
        configuration.put("com.openexchange.capability.filestorage_xctx", Boolean.FALSE.toString());
        configuration.put("com.openexchange.api.client.blacklistedHosts", "");
        configuration.put("com.openexchange.share.guestHostname", AJAXConfig.getProperty(Property.SINGLENODE_HOSTNAME));
        return configuration;
    }

    @Test
    public void testMissingLink() throws Exception {
        ShareLinkAnalyzeResponse analyzeShareLink = smApiC2.analyzeShareLink(getBody(""));
        assertNull(analyzeShareLink.getData());
        assertNotNull(analyzeShareLink.getErrorDesc(), analyzeShareLink.getError());
        assertTrue(analyzeShareLink.getErrorDesc().equals("Missing the following request parameter: link"));
    }

    @Test
    public void testSomeLink() throws Exception {
        analyze("https://example.org/no/share/link", StateEnum.UNSUPPORTED);
    }

    @Test
    public void testBrokenLink() throws Exception {
        analyze("https://example.org/ajax/share/aaaf78820506e0b2faf7883506ce41388f98fa02a4e314c9/1/8/MTk3Njk0", StateEnum.UNSUPPORTED);
    }

    @Test
    public void testAnonymousLink() throws Exception {
        String folderId = createFolder();
        ShareLinkData shareLink = getOrCreateShareLink(folderManager, smApi, folderId);

        analyze(shareLink, StateEnum.UNSUPPORTED);
    }

    @Test
    public void testSingleFile() throws Exception {
        String folderId = createFolder();

        String item = createFile(folderId, "file" + sharedFolderName);
        ShareLinkData shareLink = getOrCreateShareLink(folderManager, smApi, folderId, item);
        analyze(shareLink, StateEnum.UNSUPPORTED);
    }

    @Test
    public void testAnonymousLinkWithPassword() throws Exception {
        String folderId = createFolder();
        ShareLinkData shareLink = getOrCreateShareLink(folderManager, smApi, folderId);

        analyze(shareLink, StateEnum.UNSUPPORTED);

        updateLinkWithPassword(folderManager, smApi, folderId);
        analyze(smApiC2, getOrCreateShareLink(folderManager, smApi, folderId), StateEnum.UNSUPPORTED);
    }

    @Test
    public void testDeletedAnonymousLink() throws Exception {
        String folderId = createFolder();
        ShareLinkData shareLink = getOrCreateShareLink(folderManager, smApi, folderId);

        analyze(shareLink, StateEnum.UNSUPPORTED);

        deleteShareLink(folderManager, smApi, folderId);

        /*
         * Wait so the anonymous guest user is deleted. Otherwise the guest
         * is resolved "just in time" and a "forbidden" for an anonymous share link
         * is returned.
         */
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        analyze(shareLink, StateEnum.UNRESOLVABLE);
    }

    /**
     * Test most status for a guest user. This is done in one test because some states
     * can only be tested by doing the same ground work.
     *
     * @throws Exception In case of failure
     */
    @Test
    public void testGuest() throws Exception {
        String folderId = createFolder();

        /*
         * Add a guest to the folder
         */
        FolderData folder = folderManager.getFolder(folderId);
        ArrayList<FolderPermission> originalPermissions = new ArrayList<>(folder.getPermissions());
        ArrayList<FolderPermission> updatedPermissions = new ArrayList<>(folder.getPermissions());
        updatedPermissions.add(prepareGuest(testUserC2));

        folderId = setFolderPermission(folderId, updatedPermissions);

        /*
         * Receive mail as guest and extract share link
         */
        String shareLink = receiveShareLink(apiClientC2, testUser.getLogin());
        analyze(shareLink, StateEnum.ADDABLE);

        /*
         * Add share and verify analyze changed
         */
        accountData = addOXShareAccount(smApiC2, shareLink, null);

        /*
         * Remove guest from folder permission
         */
        folderId = setFolderPermission(folderId, originalPermissions);
        analyze(shareLink, StateEnum.REMOVED);

        /*
         * Re-add guest, account should still exist
         */
        folderId = setFolderPermission(folderId, updatedPermissions);
        analyze(shareLink, StateEnum.SUBSCRIBED);

        /*
         * Unsubscribe from share and check response, last share gone, so the account should have been removed
         */
        unsubscribe(shareLink);
        analyze(shareLink, StateEnum.ADDABLE);
        accountData = null;
    }

    /**
     * Test status for a changed guest user password. This is done in one test because some states
     * can only be tested by doing the same ground work.
     *
     * @throws Exception In case of failure
     */
    @Test
    public void testGuest_PWDChange() throws Exception {
        String folderId = createFolder();

        /*
         * Add a guest to the folder
         */
        FolderData folder = folderManager.getFolder(folderId);
        ArrayList<FolderPermission> originalPermissions = new ArrayList<>(folder.getPermissions());
        ArrayList<FolderPermission> updatedPermissions = new ArrayList<>(folder.getPermissions());
        updatedPermissions.add(prepareGuest(testUserC2));

        folderId = setFolderPermission(folderId, updatedPermissions);

        /*
         * Receive mail as guest and extract share link
         */
        String shareLink = receiveShareLink(apiClientC2, testUser.getLogin());
        analyze(shareLink, StateEnum.ADDABLE);

        accountData = addOXShareAccount(smApiC2, shareLink, null);
        FilestorageApi filestorageApiC2 = new FilestorageApi(apiClientC2);
        FileAccountResponse fileAccountResponse = filestorageApiC2.getFileAccount(IDMangler.unmangle(accountData.getFolder()).get(0), accountData.getAccount());
        FileAccountData fileAccountData = checkResponse(fileAccountResponse.getError(), fileAccountResponse.getErrorDesc(), fileAccountResponse.getData());

        /*
         * Change password of guest and verify response.
         */
        GuestClient guestClient = new GuestClient(testContext.getUsedBy(), shareLink, null, null, true);
        String password = "secret";
        PasswordChangeUpdateResponse response = guestClient.execute(new PasswordChangeUpdateRequest(password, null, true));
        MatcherAssert.assertThat(response.getErrorMessage(), nullValue());
        MatcherAssert.assertThat(response.getException(), nullValue());
        Object data = response.getData();
        MatcherAssert.assertThat(data, notNullValue());

        analyze(shareLink, StateEnum.CREDENTIALS_REFRESH);
        clearAccountError(new FilestorageApi(apiClientC2), fileAccountData);

        /*
         * Update password in local instance and check response
         */
        smApiC2.resubscribeShare(getExtendedBody(shareLink, password, null));
        analyze(shareLink, StateEnum.SUBSCRIBED);

        /*
         * Delete guest from share and verify analyze changed
         */
        folderId = setFolderPermission(folderId, originalPermissions);
        analyze(shareLink, StateEnum.REMOVED);

        /*
         * Re-add guest, account should still exist and session of the guest still be valid
         */
        folderId = setFolderPermission(folderId, updatedPermissions);
        analyze(shareLink, StateEnum.SUBSCRIBED);

        /*
         * Unsubscribe from share and check response, last share gone, so the account should have been removed
         */
        unsubscribe(shareLink);
        analyze(shareLink, StateEnum.ADDABLE_WITH_PASSWORD);

        accountData = null;
    }

    private void analyze(ShareLinkData shareLink, StateEnum e) throws ApiException {
        analyze(smApiC2, shareLink, e);
    }

    private void analyze(String shareLink, StateEnum e) throws ApiException {
        analyze(smApiC2, shareLink, e);
    }

    /**
     *
     * Creates a file with the given file name, owner and shared user.
     *
     * @param folderId The ID of the folder to put the file in
     * @param fileName The name of the file.
     * @return An entry with the object id of the file.
     * @throws ApiException
     */
    private String createFile(String folderId, String fileName) throws ApiException {
        InfoItemUpdateResponse uploadResponse = new InfostoreApi(getApiClient(Host.SINGLENODE)).uploadInfoItem(folderId, fileName, new byte[] { 34, 45, 35, 23 }, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertNotNull(uploadResponse);
        assertNull(uploadResponse.getError(), uploadResponse.getErrorDesc());
        return uploadResponse.getData();
    }

    /**
     * Unsubscribes a share
     *
     * @param shareLink The share to unsubscribe
     * @throws ApiException In case unsubscribe fails
     */
    private void unsubscribe(String shareLink) throws ApiException {
        SubscribeShareBody body = new SubscribeShareBody();
        body.setLink(shareLink);
        CommonResponse response = smApiC2.unsubscribeShare(body);
        checkResponse(response);
    }

    /**
     * Deletes the account.
     * <p>
     * Note: The account is deleted not unsubscribed!
     *
     * @param client The client to use
     * @param accountId The account ID
     * @throws Exception
     */
    protected void deleteOXShareAccount() {
        try {
            FilestorageApi filestorageApi = new FilestorageApi(apiClientC2);
            filestorageApi.deleteFileAccount(accountData.getModule(), accountData.getAccount());
        } catch (ApiException e) {
            LoggerFactory.getLogger(ShareManagementSubscriptionTest.class).info("Unable to remove account", e);
        }
    }
}
