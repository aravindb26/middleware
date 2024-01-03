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

package com.openexchange.ajax.importexport;

import static com.openexchange.java.Autoboxing.I;
import java.io.File;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.exception.OXException;
import com.openexchange.test.common.configuration.AJAXConfig;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ContactsResponse;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.modules.ContactsApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.ImportApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link JPCSVImportTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.1
 */
public class JPCSVImportTest extends AbstractConfigAwareAPIClientSession {

    private ImportApi   importApi;
    private ContactsApi contactsApi;
    private String folderId;
    private Set<String> folderToDelete;
    private FoldersApi  foldersApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.importApi = new ImportApi(getApiClient());
        foldersApi = new FoldersApi(getApiClient());
        folderId = createAndRememberNewFolder(foldersApi, getDefaultFolder(), getApiClient().getUserId().intValue());
        contactsApi = new ContactsApi(getApiClient());
    }

    /**
     * Creates a new folder and remembers it.
     *
     * @param api The {@link FoldersApi}
     * @param session The user's session
     * @param parent The parent folder
     * @param entity The user id
     * @return The result of the operation
     * @throws ApiException if an API error is occurred
     */
    protected String createAndRememberNewFolder(FoldersApi api, String parent, int entity) throws ApiException {
        FolderPermission perm = new FolderPermission();
        perm.setEntity(I(entity));
        perm.setGroup(Boolean.FALSE);
        perm.setBits(I(403710016));
        List<FolderPermission> permissions = new ArrayList<>();
        permissions.add(perm);

        NewFolderBodyFolder folderData = new NewFolderBodyFolder();
        folderData.setModule("contacts");
        folderData.setSubscribed(Boolean.TRUE);
        folderData.setTitle(this.getClass().getSimpleName() + new UID().toString());
        folderData.setPermissions(permissions);

        NewFolderBody body = new NewFolderBody();
        body.setFolder(folderData);

        FolderUpdateResponse createFolder = api.createFolder(parent, body, "0", null, null, null);
        checkResponse(createFolder.getError(), createFolder.getErrorDesc(), createFolder.getData());

        String result = createFolder.getData();
        rememberFolder(result);

        return result;
    }

    /**
     * Keeps track of the specified folder
     *
     * @param folder The folder
     */
    protected void rememberFolder(String folder) {
        if (folderToDelete == null) {
            folderToDelete = new HashSet<>();
        }
        folderToDelete.add(folder);
    }

    /**
     * Retrieves the default contact folder of the user with the specified session
     *
     * @param session The session of the user
     * @return The default contact folder of the user
     * @throws Exception if the default contact folder cannot be found
     */
    @SuppressWarnings("unchecked")
    private String getDefaultFolder() throws Exception {
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

    private static final String COLUMNS = "501,502,506,507,508,510,511";

    @Test
    public void testJPImport() throws ApiException {
        File file = new File(AJAXConfig.getProperty(AJAXConfig.Property.TEST_DIR), "jpcontact.csv");
        importApi.importCSV(folderId, file, Boolean.FALSE, null);
        ContactsResponse allContacts = contactsApi.getAllContacts(folderId, COLUMNS, null, null, null);
        Object data = checkResponse(allContacts.getError(), allContacts.getErrorDesc(), allContacts.getData());
        Assertions.assertTrue(data instanceof ArrayList<?>, "Wrong response type. Expected ArrayList but received: " + data.getClass().getSimpleName());
        ArrayList<?> contacts = (ArrayList<?>) data;
        Assertions.assertFalse(contacts.isEmpty(), "No contacts found.");
        Assertions.assertEquals(1, contacts.size());
        Object con = contacts.get(0);
        Assertions.assertTrue(con instanceof ArrayList<?>, "Wrong contact data type. Expected ArrayList but was: " + con.getClass().getSimpleName());
        ArrayList<?> conData = (ArrayList<?>) con;
        Assertions.assertEquals(7, conData.size());
        Assertions.assertEquals("Max", conData.get(0).toString(), "Wrong contact data:");
        Assertions.assertEquals("Mustermann", conData.get(1).toString(), "Wrong contact data:");
        Assertions.assertEquals("Hauptweg", conData.get(2).toString(), "Wrong contact data:");
        Assertions.assertEquals("12345", conData.get(3).toString(), "Wrong contact data:");
        Assertions.assertEquals("Olpe", conData.get(4).toString(), "Wrong contact data:");
        Assertions.assertEquals("Deutschland", conData.get(5).toString(), "Wrong contact data:");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(1990, 0, 1);
        Assertions.assertEquals(String.valueOf(cal.getTimeInMillis()), String.valueOf(((Number) conData.get(6)).longValue()), "Wrong contact data:");
    }
}
