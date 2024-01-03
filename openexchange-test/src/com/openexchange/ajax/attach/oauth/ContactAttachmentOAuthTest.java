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

import java.util.List;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactUpdateResponse;
import com.openexchange.testing.httpclient.modules.ContactsApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link ContactAttachmentOAuthTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class ContactAttachmentOAuthTest extends AbstractAttachmentOAuthTest {

    private String contactId;
    private int folderId;
    private List<Integer> attachmentIds;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderId = getClient().getValues().getPrivateContactFolder();
        contactId = createTestContact();
        attachmentIds = attach();
    }

    ///////////////////////////// HELPERS ////////////////////////

    /**
     * Creates a test contact in the specified folder
     * 
     * @param folder The folder
     * @return the Contact identifier
     * @throws ApiException
     */
    private String createTestContact() throws ApiException {
        ContactsApi contactApi = new ContactsApi(getApiClient());
        ContactUpdateResponse createContactResp = contactApi.createContact(createTestContactData());
        checkResponse(createContactResp.getError(), createContactResp.getErrorDesc());
        return createContactResp.getData().getId();
    }

    /**
     * Creates the test contact data
     * 
     * @return the contact data
     */
    private ContactData createTestContactData() {
        ContactData contactData = new ContactData();
        contactData.setFolderId(Integer.toString(getFolderId()));
        contactData.setFirstName("Foo");
        contactData.setLastName("Bar");
        contactData.setDisplayName("Bar, Foo");
        return contactData;
    }

    @Override
    int getFolderId() {
        return folderId;
    }

    @Override
    int getModuleId() {
        return 7;
    }

    @Override
    int getObjectId() {
        return Integer.parseInt(contactId);
    }

    @Override
    List<Integer> getAttachmentIds() {
        return attachmentIds;
    }
}
