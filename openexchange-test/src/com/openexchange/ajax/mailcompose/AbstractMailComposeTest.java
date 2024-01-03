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

package com.openexchange.ajax.mailcompose;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.java.Strings;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.test.common.test.TestInit;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.MailComposeAttachmentPostResponse;
import com.openexchange.testing.httpclient.models.MailComposeAttachmentResponse;
import com.openexchange.testing.httpclient.models.MailComposeGetResponse;
import com.openexchange.testing.httpclient.models.MailComposeResponse;
import com.openexchange.testing.httpclient.models.MailComposeResponseMessageModel;
import com.openexchange.testing.httpclient.models.MailComposeSendResponse;
import com.openexchange.testing.httpclient.models.MailDestinationData;
import com.openexchange.testing.httpclient.models.MailImportResponse;
import com.openexchange.testing.httpclient.models.MailListElement;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.modules.ContactsApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.MailApi;
import com.openexchange.testing.httpclient.modules.MailComposeApi;

/**
 * {@link AbstractMailComposeTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.2
 */
public abstract class AbstractMailComposeTest extends AbstractAPIClientSession {

    protected MailComposeApi api;
    protected final String DEFAULT_COLUMNS = "from,sender,to,cc,bcc,subject";
    protected final String ALL_COLUMNS = "from,sender,to,cc,bcc,subject,content,contentType,attachments,sharedAttachmentsInfo,meta,requestReadReceipt,priority,security,contentEncrypted";
    protected final List<String> compositionSpaceIds = new ArrayList<>();

    protected MailApi mailApi;
    protected File attachment;
    protected File attachment2;
    protected String testMailDir;
    protected final List<MailDestinationData> IMPORTED_EMAILS = new ArrayList<>();
    protected FoldersApi foldersApi;

    private String testFolderId = null;

    private static final String FOLDER = "default0/INBOX";

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.api = new MailComposeApi(getApiClient());

        mailApi = new MailApi(getApiClient());
        foldersApi = new FoldersApi(getApiClient());
        testMailDir = AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR);

        attachment = new File(TestInit.getTestProperty("ajaxPropertiesFile"));
        attachment2 = new File(TestInit.getTestProperty("provisioningFile"));

        MailComposeGetResponse allSpaces = api.getMailCompose((String) null);
        for (MailComposeResponseMessageModel model : allSpaces.getData()) {
            api.deleteMailComposeById(model.getId(), null, null);
        }
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        for (MailDestinationData mailDestinationData : IMPORTED_EMAILS) {
            MailListElement element = new MailListElement();
            element.setFolder(mailDestinationData.getFolderId());
            element.setId(mailDestinationData.getId());
            mailApi.deleteMails(Collections.singletonList(element), null, Boolean.TRUE, Boolean.FALSE);
        }
        super.tearDown();
    }

    protected MailDestinationData importTestMailWithAttachment() throws ApiException {
        return importTestMail("mailcompose_mail-with-pdf-attachment.eml");
    }

    /**
     *
     * importTestMail
     *
     * @param fileName The file name of the mail to upload
     * @return
     * @throws ApiException
     */
    protected MailDestinationData importTestMail(String fileName) throws ApiException {
        if (testFolderId == null) {
            NewFolderBody body = new NewFolderBody();
            NewFolderBodyFolder folder = new NewFolderBodyFolder();
            folder.setTitle(this.getClass().getSimpleName() + "_" + new UID().toString());
            folder.setModule("mail");
            folder.setPermissions(null);
            body.setFolder(folder);
            FolderUpdateResponse createFolder = foldersApi.createFolder(FOLDER, body, "0", null, null, null);
            assertNull(createFolder.getError(), createFolder.getErrorDesc());
            assertNotNull(createFolder.getData());
            testFolderId = createFolder.getData();
        }

        File emlFile = new File(testMailDir, fileName);
        MailImportResponse response = mailApi.importMail(testFolderId, emlFile, null, Boolean.TRUE);
        List<MailDestinationData> data = response.getData();
        MailDestinationData mailWithAttachment = data.get(0);
        IMPORTED_EMAILS.add(mailWithAttachment);
        return mailWithAttachment;
    }

    protected MailComposeResponseMessageModel createNewCompositionSpace() throws Exception {
        MailComposeResponse response = api.postMailCompose(null, null, null, null, null);
        assertTrue(Strings.isEmpty(response.getError()), response.getErrorDesc());
        MailComposeResponseMessageModel data = response.getData();
        compositionSpaceIds.add(data.getId());
        return data;
    }

    protected String getMailAddress() throws Exception {
        ContactsApi contactsApi = new ContactsApi(getApiClient());
        ContactData data = contactsApi.getContactByUser(I(testUser.getUserId())).getData();
        assertNotNull(data, "No contact data for user.");
        String mailAddress = data.getEmail1();
        assertFalse(Strings.isEmpty(mailAddress), "No mail address for user.");
        return mailAddress;
    }

    protected String getOtherMailAddress() throws Exception {
        ContactsApi contactsApi = new ContactsApi(getApiClient());
        ContactData data = contactsApi.getContactByUser(I(testUser2.getUserId())).getData();
        assertNotNull(data, "No contact data for other user.");
        String mailAddress = data.getEmail1();
        assertFalse(Strings.isEmpty(mailAddress), "No mail address for other user.");
        return mailAddress;
    }

    protected List<String> getSender() throws Exception {
        return Arrays.asList(new String[] { testUser.getUser(), getMailAddress() });
    }

    protected List<List<String>> getRecipient() throws Exception {
        return Collections.singletonList(Arrays.asList(new String[] { testUser2.getUser(), getOtherMailAddress() }));
    }

    protected void check(MailComposeSendResponse response) {
        check(response.getErrorDesc(), response.getError(), response);
    }

    protected void check(MailComposeResponse response) {
        check(response.getErrorDesc(), response.getError(), response);
    }

    protected void check(MailComposeGetResponse response) {
        check(response.getErrorDesc(), response.getError(), response);
    }

    protected void check(MailComposeAttachmentResponse response) {
        check(response.getErrorDesc(), response.getError(), response);
    }

    protected void check(MailComposeAttachmentPostResponse response) {
        check(response.getErrorDesc(), response.getError(), response);
    }

    //    protected void check(InlineResponse2002 response) {
    //        assertTrue(b(response.getSuccess()));
    //    }

    protected void check(String errorDesc, String error, Object notNull) {
        assertNotNull(notNull);
        assertNull(error, errorDesc);
    }

}
