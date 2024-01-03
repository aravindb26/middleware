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

package com.openexchange.ajax.drive.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.infostore.apiclient.InfostoreApiClientTest;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.DriveDirectoryMetadata;
import com.openexchange.testing.httpclient.models.DriveSettingsData;
import com.openexchange.testing.httpclient.models.DriveSettingsResponse;
import com.openexchange.testing.httpclient.models.DriveSubfoldersResponse;
import com.openexchange.testing.httpclient.models.UserData;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.DriveApi;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import com.openexchange.testing.httpclient.modules.UserApi;
import org.junit.jupiter.api.TestInfo;

/**
 *
 * {@link LocalizedFolderNameTests}
 *
 * Tests for MW1558: Indicate localizable folder names for OX Drive
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Schuerholz</a>
 * @since v7.10.6
 */
public class LocalizedFolderNameTests extends InfostoreApiClientTest {

    private static final String ROOT = "/";
    private static final String VIDEOS = "/Videos";
    private static final String TEMPLATES = "/Documents/Templates";
    private static final String PICTURES = "/Pictures";
    private static final String DOCUMENTS = "/Documents";
    private static final String MUSIC = "/Music";

    private static final String LOCALE_DE = Locale.GERMANY.toString();
    private static final String LOCALE_EN = Locale.ENGLISH.toString();

    private DriveApi driveApi;

    private final HashMap<String, String> folders_de = new HashMap<String, String>() {

        private static final long serialVersionUID = -5717084296164976230L;
        {
            put(MUSIC, "Musik");
            put(DOCUMENTS, "Dokumente");
            put(PICTURES, "Bilder");
            put(TEMPLATES, "Vorlagen");
            put(VIDEOS, "Videos");
            put(ROOT, "Meine Dateien");
        }
    };

    private final HashMap<String, String> folders_en = new HashMap<String, String>() {

        private static final long serialVersionUID = 4610685633278333855L;
        {
            put(MUSIC, "Music");
            put(DOCUMENTS, "Documents");
            put(PICTURES, "Pictures");
            put(TEMPLATES, "Templates");
            put(VIDEOS, "Videos");
            put(ROOT, "My files");
        }
    };

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        driveApi = new DriveApi(getApiClient());
    }

    /**
     *
     * Tests if each of the expected localized folder names is contained in the settings response.
     * Locale: GERMAN
     *
     * @throws ApiException
     */
    @Test
    public void localizedNames_Settings_Test_de() throws ApiException {
        checkSettingsResponse(LOCALE_DE, folders_de);
    }

    /**
     *
     * Tests if each of the expected localized folder names is contained in the settings response.
     * Locale: ENGLISH
     *
     * @throws ApiException
     */
    @Test
    public void localizedNames_Settings_Test_en() throws ApiException {
        checkSettingsResponse(LOCALE_EN, folders_en);
    }

    /**
     *
     * Tests if only the localizable folders in infostore root have a localized name in the metadata of subfolders response.
     * Locale: GERMAN
     *
     * @throws ApiException
     */
    @Test
    public void localizedNames_Subfolders_Test_de() throws ApiException {
        // Changes the locale from test user to the german locale value
        // Changes the locale from test user to the locale value
        // Setting the language via user-api seems to fail here;
        // We can use JSLOB here because "language" is a "shared" setting (com.openexchange.groupware.settings.tree.Language)
        JSlobApi jslobApi = new JSlobApi(getApiClient());
        CommonResponse response = jslobApi.setJSlob(Collections.singletonMap("language", LOCALE_DE), "io.ox/core", null);
        checkResponse(response.getError(), response.getErrorDesc());

        UserApi userApi = new UserApi(getApiClient());
        UserResponse userResponse = userApi.getUser(getApiClient().getUserId().toString());
        UserData updatedUserData = checkResponse(userResponse.getError(), userResponse.getErrorDesc(), userResponse.getData());
        assertEquals(LOCALE_DE, updatedUserData.getLocale(), "Precondition failed:");

        checkSubfoldersResponse(folders_de);
    }

    /**
     *
     * Tests if only the localizable folders in infostore root have a localized name in the metadata of subfolders response.
     * Locale: ENGLISH
     *
     * @throws ApiException
     */
    @Test
    public void localizedNames_Subfolders_Test_en() throws ApiException {
        checkSubfoldersResponse(folders_en);
    }

    private void checkSettingsResponse(String locale, HashMap<String, String> expectedFolderNames) throws ApiException {
        DriveSettingsResponse settingsResponse = driveApi.getSettings(getPrivateInfostoreFolder(), null, locale);
        DriveSettingsData settingsData = checkResponse(settingsResponse.getError(), settingsResponse.getErrorDesc(), settingsResponse.getData());
        Object localizedFolderNames = settingsData.getLocalizedFolderNames();
        @SuppressWarnings("unchecked") Map<String, String> namesMap = (Map<String, String>) localizedFolderNames;
        for (String folderPath : expectedFolderNames.keySet()) {
            String expLocalName = expectedFolderNames.get(folderPath);
            String localizedName = namesMap.get(folderPath);
            assertEquals(expLocalName, localizedName, namesMap.toString() + " does not contain expected localized folder: " + expLocalName);
        }
    }

    private void checkSubfoldersResponse(HashMap<String, String> folderMap) throws ApiException {
        DriveSubfoldersResponse subfoldersResponse = driveApi.getSynchronizableFolders(getPrivateInfostoreFolder());
        List<DriveDirectoryMetadata> subfolders = checkResponse(subfoldersResponse.getError(), subfoldersResponse.getErrorDesc(), subfoldersResponse.getData());
        for (DriveDirectoryMetadata data : subfolders) {
            String localizedName = data.getLocalizedName();
            String path = data.getPath();
            if (folderMap.containsKey(path)) {
                assertNotNull(localizedName, path + " must have a localized name.");
                assertTrue(folderMap.containsValue(localizedName), "Unexpected localized folder name: " + localizedName);
            }
        }
    }

}
