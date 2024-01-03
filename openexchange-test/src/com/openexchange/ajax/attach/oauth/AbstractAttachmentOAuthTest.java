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

package com.openexchange.ajax.attach.oauth;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.common.collect.ImmutableList;
import com.openexchange.ajax.appPassword.AbstractAppPasswordTest;
import com.openexchange.exception.Category;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.attach.impl.AttachmentImpl;
import com.openexchange.test.AttachmentTestManager;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AppPasswordApplication;
import com.openexchange.testing.httpclient.models.AppPasswordRegistrationResponseData;
import com.openexchange.testing.httpclient.models.AttachmentResponse;
import com.openexchange.testing.httpclient.models.AttachmentUpdatesResponse;
import com.openexchange.testing.httpclient.models.AttachmentsResponse;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.InfoItemsResponse;
import com.openexchange.testing.httpclient.models.MailCountResponse;
import com.openexchange.testing.httpclient.models.SnippetData;
import com.openexchange.testing.httpclient.models.SnippetUpdateResponse;
import com.openexchange.testing.httpclient.modules.AttachmentsApi;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.SnippetApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractAttachmentOAuthTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
abstract class AbstractAttachmentOAuthTest extends AbstractAppPasswordTest {

    private static final List<String> ATTACHMENTS = ImmutableList.of("attachment.base64", "ox.html.txt", "ox_logo_sml.jpg");

    static final String APP_NAME = "attachments";
    static final String PERMISSION_DENIED = Category.CATEGORY_PERMISSION_DENIED.toString();

    TestUser newTestUser;
    AppPasswordRegistrationResponseData attachmentsLoginData;

    //////////////////////////////////// SETUP //////////////////////////////////

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        List<AppPasswordApplication> apps = getApps();
        assertThat(I(apps.size()), greaterThan(I(1)));
        for (AppPasswordApplication app : apps) {
            if (!app.getName().equals(APP_NAME)) {
                continue;
            }
            attachmentsLoginData = addPassword(app.getName());
            break;
        }
    }

    /////////////////////////////////// TESTS //////////////////////////////////

    @Test
    public void testDocumentAction() throws Exception {
        int attachmentIndex = (int) (Math.random() * getAttachmentIds().size() - 1);
        documentAction(i(getAttachmentIds().get(attachmentIndex)));
    }

    @Test
    public void testGetAction() throws Exception {
        int attachmentIndex = (int) (Math.random() * getAttachmentIds().size() - 1);
        getAction(i(getAttachmentIds().get(attachmentIndex)));
    }

    @Test
    public void testAttachAction() throws Exception {
        attachAction();
    }

    @Test
    public void testDetachAction() throws Exception {
        int attachmentIndex = (int) (Math.random() * getAttachmentIds().size() - 1);
        detachAction(attachmentIndex);
    }

    @Test
    public void testUpdatesAction() throws Exception {
        attach();
        updatesAction();
    }

    @Test
    public void testAllAction() throws Exception {
        allAction();
    }

    @Test
    public void testListAction() throws Exception {
        listAction(getAttachmentIds());
    }

    @Test
    public void testZipDocumentsAction() throws Exception {
        zipDocumentsAction(getAttachmentIds());
    }

    /**
     * Tests that other oauth-protected modules are not
     * accessible if their scopes are not granted.
     */
    @Test
    public void testNotGrantedScopes() throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        tryReadMail(apiClient, false);
        tryWriteSnippet(apiClient, false);
    }

    /////////////////////////////////// HELPERS /////////////////////////////

    /**
     * Gets an attachment's document/filedata.
     *
     * @param attachmentId the attachment's id
     */
    void documentAction(int attachmentId) throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);
        try {
            File file = attApi.getAttachmentDocument(I(getObjectId()), I(getFolderId()), I(getModuleId()), Integer.toString(attachmentId), "application/octet-stream", B(false));
            assertNotNull(file);
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Gets the specified attachment
     *
     * @param attachmentId The attachment to get
     */
    void getAction(int attachmentId) throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);
        try {
            AttachmentResponse attachment = attApi.getAttachment(Integer.toString(attachmentId), I(getObjectId()), I(getFolderId()), I(getModuleId()));
            assertNotNull(attachment);
            assertNull(attachment.getError());
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Attaches a file
     */
    void attachAction() throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);
        File file = new File(AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR), "ox.jpg");

        JSONObject json = new JSONObject();
        json.put("folder", getFolderId());
        json.put("attached", getObjectId());
        json.put("module", getModuleId());

        try {
            String attachment = attApi.createAttachment(json.toString(), file);
            assertNotNull(attachment);
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Detaches the specified attachment
     *
     * @param attachmentId The attachment id
     */
    void detachAction(int attachmentId) throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);

        try {
            CommonResponse response = attApi.deleteAttachments(I(getObjectId()), I(getFolderId()), I(getModuleId()), ImmutableList.of(Integer.toString(attachmentId)));
            assertNull(response.getError());
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Gets the new and deleted attachments.
     */
    void updatesAction() throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);
        try {
            AttachmentUpdatesResponse updates = attApi.getAttachmentUpdates(I(getObjectId()), I(getFolderId()), "1", I(getModuleId()), L(0L), null, null, null);
            assertNotNull(updates);
            assertNull(updates.getError());
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Returns all attachments
     */
    void allAction() throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);
        try {
            AttachmentsResponse allAttachments = attApi.getAllAttachments(I(getObjectId()), I(getFolderId()), I(getModuleId()), "1", null, null);
            assertNotNull(allAttachments);
            assertNull(allAttachments.getError());
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Lists the specified attachments
     *
     * @param attachmentIds The attachment ids
     */
    void listAction(List<Integer> attachmentIds) throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);
        try {
            InfoItemsResponse list = attApi.getAttachmentList(I(getObjectId()), I(getFolderId()), I(getModuleId()), "1", attachmentIds);
            assertNotNull(list);
            assertNull(list.getError());
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Zips the specified attachments
     *
     * @param attachmentIds
     */
    void zipDocumentsAction(List<Integer> attachmentIds) throws Exception {
        ApiClient apiClient = loginWithAppPassword(attachmentsLoginData);
        AttachmentsApi attApi = new AttachmentsApi(apiClient);
        try {
            File r = attApi.getZippedAttachmentDocuments(I(getObjectId()), I(getFolderId()), I(getModuleId()), attachmentIds);
            assertNotNull(r);
        } catch (Exception e) {
            fail("No access");
        }
    }

    /**
     * Attaches multiple pre-defined files to the object under test
     *
     * @return The attachment ids
     */
    List<Integer> attach() throws Exception {
        AttachmentMetadata attachment = new AttachmentImpl();
        attachment.setFolderId(getFolderId());
        attachment.setAttachedId(getObjectId());
        attachment.setModuleId(getModuleId());
        List<Integer> attachmentIds = new LinkedList<>();
        for (String filename : ATTACHMENTS) {
            File testFile = new File(AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR), filename);
            AttachmentTestManager atm = new AttachmentTestManager(getClient());
            attachmentIds.add(I(atm.attach(attachment, testFile.getName(), FileUtils.openInputStream(testFile), "application/octet-stream")));
        }
        return attachmentIds;
    }

    /**
     * Performs a login with an app specific password and returns the logged in client
     *
     * @param loginData The login data
     * @return The logged in client
     * @throws ApiException
     */
    ApiClient loginWithAppPassword(AppPasswordRegistrationResponseData loginData) throws ApiException {
        testUser.performLogout();

        String login = loginData.getLogin();
        String username = login.contains("@") ? login.substring(0, login.indexOf("@")) : login;
        String domain = login.contains("@") ? login.substring(login.indexOf("@") + 1) : testUser.getContext();
        newTestUser = new TestUser(username, domain, loginData.getPassword(), testContext.getUsedBy());
        return newTestUser.generateApiClient("testclient");
    }

    /**
     * Tries to read mail with the specified client
     *
     * @param client The client
     * @param hasAccess Whether the client shall have access
     */
    private void tryReadMail(ApiClient client, boolean hasAccess) throws ApiException {
        MailApi api = new MailApi(client);
        ApiResponse response = execute(hasAccess, () -> {
            MailCountResponse r = api.getMailCount("default0/INBOX");
            return new ApiResponse(r.getError(), r.getCategories());
        });
        assertResponse(response, hasAccess);
    }

    /**
     * Tries to write a snippet with the specified client
     *
     * @param client the client
     * @param hasAccess whether the client shall have access
     */
    private void tryWriteSnippet(ApiClient client, boolean hasAccess) throws ApiException {
        SnippetApi snippetApi = new SnippetApi(client);
        SnippetData snippetData = new SnippetData();
        // @formatter:off
        snippetData.type("signature")
                   .module("io.ox./mail")
                   .displayname("showcase")
                   .content("<p>test</p>");
        // @formatter:on
        ApiResponse response = execute(hasAccess, () -> {
            SnippetUpdateResponse r = snippetApi.createSnippet(snippetData);
            return new ApiResponse(r.getError(), r.getCategories());
        });
        assertResponse(response, hasAccess);
    }

    //////////////////////////////////////INTERNAL STUFF ///////////////////////////////////////

    /**
     * Returns the attachment ids
     *
     * @return
     */
    abstract List<Integer> getAttachmentIds();

    /**
     * Retrieves the folder id in which tests take place
     *
     * @return the folder id
     */
    abstract int getFolderId();

    /**
     * Returns the module id for which the tests are supposed to be run
     *
     * @return the module id
     */
    abstract int getModuleId();

    /**
     * Returns the object id which has or should have the attachments
     *
     * @return the object id
     */
    abstract int getObjectId();

    /**
     * Asserts if the specified response contains a permission denied error
     *
     * @param response The response
     * @param hasAccess <code>false</code> the response implies that a resource shall not be accessed
     */
    void assertResponse(ApiResponse response, boolean hasAccess) {
        if (response == null) {
            return;
        }
        if (hasAccess) {
            assertNotNull(response);
            assertNull(response.getError());
            return;
        }
        assertNotNull(response.getError());
        assertEquals(PERMISSION_DENIED, response.getCategories());
    }

    /**
     * Executes the specified function
     *
     * @param hasAccess Whether the function shall have access
     * @param supplier The function to execute
     * @return The API response of the function, or <code>null</code> if any error is occurred
     * @throws ApiException if an API error is occurred
     */
    ApiResponse execute(boolean hasAccess, Executor<ApiResponse> supplier) throws ApiException {
        try {
            return supplier.execute();
        } catch (ApiException e) {
            if (hasAccess) {
                throw e;
            }
        }
        return null;
    }

    /**
     * {@link Executor} - Simple functional interface for
     * executing API calls
     *
     * @param <T> - The API response
     */
    static interface Executor<T> {

        T execute() throws ApiException;
    }

    /**
     * {@link ApiResponse} - Simple response wrapper
     */
    static class ApiResponse {

        String error;
        String categories;

        /**
         * Initialises a new {@link ApiResponse}.
         *
         * @param error the error
         * @param categories The error categories
         */
        ApiResponse(String error, String categories) {
            this.error = error;
            this.categories = categories;
        }

        String getError() {
            return error;
        }

        String getCategories() {
            return categories;
        }
    }
}
