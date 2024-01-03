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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.appPassword.AbstractAppPasswordTest;
import com.openexchange.ajax.chronos.manager.ICalImportExportManager;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.test.common.asset.Asset;
import com.openexchange.test.common.asset.AssetManager;
import com.openexchange.test.common.asset.AssetType;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.invoker.ApiResponse;
import com.openexchange.testing.httpclient.models.AppPasswordApplication;
import com.openexchange.testing.httpclient.models.AppPasswordRegistrationResponseData;
import com.openexchange.testing.httpclient.models.ContactData;
import com.openexchange.testing.httpclient.models.ContactUpdateResponse;
import com.openexchange.testing.httpclient.modules.ContactsApi;
import com.openexchange.testing.httpclient.modules.ExportApi;
import com.openexchange.testing.httpclient.modules.ImportApi;

/**
 * {@link ImportExportOAuthTests}
 *
 * Tests for MW-1679: Make import/export modules OAuth aware
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Schuerholz</a>
 * @since 8.0.0
 */
public class ImportExportOAuthTests extends AbstractAppPasswordTest {

    private static final String EXAMPLE_CSV = "Given name, Sur name \nTest, User";
    private static final String EXAMPLE_VCARD = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Contact\\, Test\r\nPRODID:-//Open-Xchange//7.10.6-Rev4//EN\r\nREV:2022-01-12T08:14:33Z\r\nN:Contact;Test\r\nEND:VCARD\r\n";
    private static final String PERMISSION_DENIED = Category.CATEGORY_PERMISSION_DENIED.toString();
    private ApiClient client;
    private Map<String, AppPasswordRegistrationResponseData> logins;
    private TestUser newTestUser;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        List<AppPasswordApplication> apps = getApps();
        assertThat(I(apps.size()), greaterThan(I(1)));
        logins = new HashMap<>();
        for (AppPasswordApplication app : apps) {
            AppPasswordRegistrationResponseData loginData = addPassword(app.getName());
            logins.put(app.getName(), loginData);
        }

        createTestContact(getClient().getValues().getPrivateContactFolder());

    }

    /**
     *
     * Tests if the restricted actions behave as expected for the scope definition of application Export:
     * <code>[read_export,read_contacts,read_calendar,read_tasks]</code>.
     *
     * @throws Exception
     */
    @Test
    public void testExportApi_AppPassword() throws Exception {
        AppPasswordRegistrationResponseData loginData = logins.get("export");
        client = loginWithAppPassword(loginData);

        tryCSVExport(true);
        tryCSVImport(false);
        tryICalAppointmentExport(true);
        tryICalAppointmentImport(false);
        tryICalTaskExport(true);
        tryICalTaskImport(false);
        tryVCardExport(true);
        tryVCardImport(false);
    }

    /**
     *
     * Tests if the restricted actions behave as expected for the scope definition of application Import:
     * <code>write_import,write_contacts,write_calendar,write_tasks</code>.
     *
     * @throws Exception
     */
    @Test
    public void testImportApi_AppPassword() throws Exception {
        AppPasswordRegistrationResponseData loginData = logins.get("import");
        client = loginWithAppPassword(loginData);

        tryCSVExport(false);
        tryCSVImport(true);
        tryICalAppointmentExport(false);
        tryICalAppointmentImport(true);
        tryICalTaskExport(false);
        tryICalTaskImport(true);
        tryVCardExport(false);
        tryVCardImport(true);
    }

    /**
     *
     * Tests if the restricted actions behave as expected for a normal user
     * of the ApiClient without using application specific passwords.
     *
     * @throws Exception
     */
    @Test
    public void testNormalTestUser() throws Exception {
        client = getApiClient();

        tryCSVExport(true);
        tryCSVImport(true);
        tryICalAppointmentExport(true);
        tryICalAppointmentImport(true);
        tryICalTaskExport(true);
        tryICalTaskImport(true);
        tryVCardExport(true);
        tryVCardImport(true);
    }

    /**
     *
     * Tests, if export of a CSV file is possible via the export API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     * @throws ApiException
     */
    private void tryCSVExport(boolean expectedSuccess) throws OXException, IOException, JSONException, ApiException {
        ExportApi exportApi = new ExportApi(client);
        int folder = getClient().getValues().getPrivateContactFolder();
        ApiResponse<String> resp = null;
        try {
            resp = exportApi.exportAsCSVGetReqWithHttpInfo(I(folder).toString(), "501,502", null);
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }

        }
        checkHTTPResponse("CSV", expectedSuccess, resp);
    }

    /**
     *
     * Tests, if import of a CSV file is possible via the import API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws ApiException
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    private void tryCSVImport(boolean expectedSuccess) throws ApiException, OXException, IOException, JSONException {
        ImportApi importApi = new ImportApi(client);
        int folder = getClient().getValues().getPrivateContactFolder();
        File file = getFileFromString(EXAMPLE_CSV, ".csv");

        ApiResponse<String> resp = null;
        try {
            resp = importApi.importCSVWithHttpInfo(I(folder).toString(), file, null, null);
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }
        }

        checkHTTPResponse("CSV", expectedSuccess, resp);
    }

    /**
     *
     * Tests, if export of a iCal file for an appointment is possible via the export API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws ApiException
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    private void tryICalAppointmentExport(boolean expectedSuccess) throws ApiException, OXException, IOException, JSONException {
        ExportApi exportApi = new ExportApi(client);
        int folder = getClient().getValues().getPrivateAppointmentFolder();
        ApiResponse<String> resp = null;
        try {
            resp = exportApi.exportAsICalGetReqWithHttpInfo(I(folder).toString());
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }

        }
        checkHTTPResponse("iCal", expectedSuccess, resp);
    }

    /**
     *
     * Tests, if import of a iCal file with an appointment is possible via the import API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws Exception
     */
    private void tryICalAppointmentImport(boolean expectedSuccess) throws Exception {
        ImportApi importApi = new ImportApi(client);
        int folder = getClient().getValues().getPrivateAppointmentFolder();
        File icalFile = getICalFile(ICalImportExportManager.SINGLE_IMPORT_ICS.fileName());

        ApiResponse<String> resp = null;
        try {
            resp = importApi.importICalWithHttpInfo(I(folder).toString(), icalFile, null, null, null, null);
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }
        }
        checkHTTPResponse("iCal", expectedSuccess, resp);
    }

    /**
     *
     * Tests, if export of a iCal file with a task is possible via the export API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws ApiException
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    private void tryICalTaskExport(boolean expectedSuccess) throws ApiException, OXException, IOException, JSONException {
        ExportApi exportApi = new ExportApi(client);
        int folder = getClient().getValues().getPrivateTaskFolder();
        ApiResponse<String> resp = null;
        try {
            resp = exportApi.exportAsICalGetReqWithHttpInfo(I(folder).toString());
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }

        }
        checkHTTPResponse("iCal", expectedSuccess, resp);
    }

    /**
     *
     * Tests, if import of a iCal file with a task is possible via the import API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws Exception
     */
    private void tryICalTaskImport(boolean expectedSuccess) throws Exception {
        ImportApi importApi = new ImportApi(client);
        int folder = getClient().getValues().getPrivateTaskFolder();
        File icalFile = getICalFile("single_task.ics");

        ApiResponse<String> resp = null;
        try {
            resp = importApi.importICalWithHttpInfo(I(folder).toString(), icalFile, null, null, null, null);
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }
        }
        checkHTTPResponse("iCal", expectedSuccess, resp);
    }

    /**
     *
     * Tests, if export of a vCard file is possible via the export API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws ApiException
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    private void tryVCardExport(boolean expectedSuccess) throws ApiException, OXException, IOException, JSONException {
        ExportApi exportApi = new ExportApi(client);
        int folder = getClient().getValues().getPrivateContactFolder();

        ApiResponse<String> resp = null;
        try {
            resp = exportApi.exportAsVCardGetReqWithHttpInfo(I(folder).toString(), null);
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }

        }
        checkHTTPResponse("vCard", expectedSuccess, resp);
    }

    /**
     *
     * Tests, if import of a vCard file is possible via the import API.
     *
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @throws ApiException
     * @throws OXException
     * @throws IOException
     * @throws JSONException
     */
    private void tryVCardImport(boolean expectedSuccess) throws ApiException, OXException, IOException, JSONException {
        ImportApi importApi = new ImportApi(client);
        int folder = getClient().getValues().getPrivateContactFolder();
        File file = getFileFromString(EXAMPLE_VCARD, ".vcard");
        ApiResponse<String> resp = null;
        try {
            resp = importApi.importVCardWithHttpInfo(I(folder).toString(), file, null);
        } catch (ApiException e) {
            if (expectedSuccess) {
                throw e;
            }
        }
        checkHTTPResponse("vCard", expectedSuccess, resp);
    }

    /**
     *
     * Checks whether an HTTP response from an import or export action
     * matches the expected success of the action.
     *
     * @param format The format of the imported / exported data.
     * @param expectedSuccess <code>true</code> if the operation should work without an error, <code>false</code> otherwise.
     * @param resp The HTTP response.
     */
    private void checkHTTPResponse(String format, boolean expectedSuccess, ApiResponse<String> resp) {
        if (expectedSuccess) {
            assertNotNull(resp);
            assertEquals(200, resp.getStatusCode());
            assertNotNull(resp.getData());
            String data = resp.getData();
            assertFalse(data.isEmpty(), format + " is emtpy.");
            assertFalse(data.contains("error"), format + " contains error." + data);
            assertFalse(data.contains(PERMISSION_DENIED), format + " contains PERMISSION_DENIED error.");
        } else {
            if (resp != null) {
                String data = resp.getData();
                assertTrue(data == null || data.contains(PERMISSION_DENIED), format + " does not contain PERMISSION_DENIED error: " + data);

            }
        }
    }

    /**
     *
     * Creates a test contact via contact API.
     *
     * @param folder The contact folder.
     * @throws ApiException
     */
    private void createTestContact(int folder) throws ApiException {
        ContactsApi contactApi = new ContactsApi(getApiClient());
        ContactUpdateResponse createContactResp = contactApi.createContact(createTestContactData(folder));
        checkResponse(createContactResp.getError(), createContactResp.getErrorDesc());
    }

    /**
     *
     * Creates contact data with example contact information.
     *
     * @param folder The contact folder.
     * @return The contact data.
     */
    private ContactData createTestContactData(int folder) {
        ContactData contactData = new ContactData();
        contactData.setFolderId(I(folder).toString());
        contactData.setFirstName("Test");
        contactData.setLastName("Contact");
        contactData.setDisplayName("Contact, Test");
        return contactData;
    }

    /**
     *
     * Gets a file from a string containing the file content.
     *
     * @param data The file content.
     * @param suffix The file suffix.
     * @return A file.
     * @throws IOException
     */
    private File getFileFromString(String data, String suffix) throws IOException {
        File file = File.createTempFile(this.getClass().getName(), suffix);
        file.deleteOnExit();
        FileUtils.writeByteArrayToFile(file, data.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     *
     * Gets the iCal file via asset manager.
     *
     * @param fileName The file name.
     * @return The file.
     * @throws Exception
     */
    private File getICalFile(String fileName) throws Exception {
        AssetManager assetManager = new AssetManager();
        Asset asset = assetManager.getAsset(AssetType.ics, fileName);
        return new File(asset.getAbsolutePath());

    }

    /**
     *
     * Performs a login with the application specific password.
     *
     * @param loginData The app password registration response data.
     * @return The api client.
     * @throws ApiException
     */
    private ApiClient loginWithAppPassword(AppPasswordRegistrationResponseData loginData) throws ApiException {
        testUser.performLogout();

        String login = loginData.getLogin();
        String username = login.contains("@") ? login.substring(0, login.indexOf("@")) : login;
        String domain = login.contains("@") ? login.substring(login.indexOf("@") + 1) : testUser.getContext();
        newTestUser = new TestUser(username, domain, loginData.getPassword(), testContext.getUsedBy());
        return newTestUser.generateApiClient("testclient");
    }
}
