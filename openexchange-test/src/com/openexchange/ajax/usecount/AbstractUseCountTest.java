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

package com.openexchange.ajax.usecount;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.exception.OXException;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactResponse;
import com.openexchange.testing.httpclient.models.ContactUpdateResponse;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.modules.ContactsApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.UsecountApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;


/**
 * {@link AbstractUseCountTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class AbstractUseCountTest extends AbstractAPIClientSession {

    protected UsecountApi usecountApi;
    protected ContactsApi contactsApi;
    protected String contactFolderId;
    private Long lastTimestamp;

    private final List<String> createdContacts = new ArrayList<>();

    /**
     * Initializes a new {@link AbstractUseCountTest}.
     */
    public AbstractUseCountTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        usecountApi = new UsecountApi(getApiClient());
        contactsApi = new ContactsApi(getApiClient());
        contactFolderId = getDefaultFolder(new FoldersApi(getApiClient()));
    }

    @AfterEach
    public void tearDown() {
        contactsApi = null;
        usecountApi = null;
    }

    protected ContactData createContactObject(final String displayname) {
        final ContactData contactObj = new ContactData();
        contactObj.setLastName("Meier");
        contactObj.setFirstName("Herbert");
        contactObj.setDisplayName(displayname);
        contactObj.setStreetBusiness("Franz-Meier Weg 17");
        contactObj.setCityBusiness("Test Stadt");
        contactObj.setStateBusiness("NRW");
        contactObj.setCountryBusiness("Deutschland");
        contactObj.setTelephoneBusiness1("+49112233445566");
        contactObj.setCompany("Internal Test AG");
        contactObj.setEmail1("hebert.meier@open-xchange.com");
        contactObj.setFolderId(contactFolderId);
        contactObj.setMarkAsDistributionlist(Boolean.FALSE);
        contactObj.setDistributionList(null);
        return contactObj;
    }

    /**
     * Creates and remembers the given contact and returns its id
     *
     * @param contactObj The {@link ContactData}
     * @return The contact id
     * @throws Exception
     */
    protected String createContact(final ContactData contactObj) throws Exception {
        ContactUpdateResponse response = contactsApi.createContact(contactObj);
        assertNull(response.getError(), response.getErrorDesc());
        assertNotNull(response.getData());
        lastTimestamp = response.getTimestamp();
        return rememberContact(response.getData().getId());
    }

    private String rememberContact(String id) {
        createdContacts.add(id);
        return id;
    }

    protected ContactData loadContact(final String objectId, final String folder) throws Exception {
        ContactResponse response = contactsApi.getContact(objectId, folder);
        assertNull(response.getError(), response.getErrorDesc());
        assertNotNull(response.getData());
        lastTimestamp = response.getTimestamp();
        return response.getData();
    }

    /**
     * Retrieves the default contact folder of the user with the specified session
     *
     * @param foldersApi The {@link FoldersApi}
     * @return The default contact folder of the user
     * @throws Exception if the default contact folder cannot be found
     */
    @SuppressWarnings("unchecked")
    private String getDefaultFolder(FoldersApi foldersApi) throws Exception {
        FoldersVisibilityResponse visibleFolders = foldersApi.getVisibleFolders("contacts", "1,308", "0", null, Boolean.TRUE);
        if (visibleFolders.getError() != null) {
            throw new OXException(new Exception(visibleFolders.getErrorDesc()));
        }

        Object privateFolders = visibleFolders.getData().getPrivate();
        ArrayList<ArrayList<?>> privateList = (ArrayList<ArrayList<?>>) privateFolders;
        if (privateList.size() == 1) {
            return (String) privateList.get(0).get(0);
        }
        for (ArrayList<?> folder : privateList) {
            if (((Boolean) folder.get(1)).booleanValue()) {
                return (String) folder.get(0);
            }
        }
        throw new Exception("Unable to find default contact folder!");
    }

}
