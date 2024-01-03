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

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Collections;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.Strings;
import com.openexchange.test.FolderTestManager;
import org.junit.jupiter.params.ParameterizedTest;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.DriveExtendedActionsResponse;
import com.openexchange.testing.httpclient.models.DriveSettingsResponse;
import com.openexchange.testing.httpclient.models.DriveSyncFilesBody;
import com.openexchange.testing.httpclient.models.DriveSyncFolderResponse;
import com.openexchange.testing.httpclient.models.DriveSyncFoldersBody;
import com.openexchange.testing.httpclient.models.UserData;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.DriveApi;
import com.openexchange.testing.httpclient.modules.JSlobApi;
import com.openexchange.testing.httpclient.modules.UserApi;

/**
 * {@link LocalizedPathToRootTest}
 *
 * Tests for MW1559: Localized variant of folders on "path to root" for OX Drive
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Schuerholz</a>
 * @since v7.10.6
 */
public class LocalizedPathToRootTest extends AbstractAPIClientSession {

    public static Stream<Locale> data() {
        return Stream.of(
                null,
                Locale.ENGLISH,
                Locale.GERMAN
        );
    }

    private DriveApi driveApi;
    private FolderTestManager folderTestManager;
    private int realRootFolderId;
    private String realRootPath;
    private int userId;


    public void prepareTest(Locale locale, TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        driveApi = new DriveApi(getApiClient());
        folderTestManager = new FolderTestManager(getClient());
        realRootFolderId = getClient().getValues().getPrivateInfostoreFolder();
        userId = getClient().getValues().getUserId();
        realRootPath = getRealRootPath(locale);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testGetlocalizedPathToRoot_settingsRequest(Locale locale, TestInfo testInfo) throws Exception {
        prepareTest(locale, testInfo);
        String folderName = "localizedPathToRootTest_settingsRequest_" + UUID.randomUUID().toString();
        String folderId = createFolderForTest(folderName);
        DriveSettingsResponse response = driveApi.getSettings(folderId, I(8), locale != null ? locale.getLanguage() : null);
        assertNotNull(response);
        String localizedPathToRoot = response.getData().getLocalizedPathToRoot();
        assertTrue(Strings.isNotEmpty(localizedPathToRoot));
        assertTrue(localizedPathToRoot.startsWith(realRootPath));
        assertEquals(folderName, localizedPathToRoot.substring(realRootPath.length() + 1));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testGetlocalizedPathToRoot_syncFilesRequest(Locale locale, TestInfo testInfo) throws Exception {
        prepareTest(locale, testInfo);
        changeUserSettingLanguage(locale);

        String folderName = "localizedPathToRootTest_syncFilesRequest_" + UUID.randomUUID().toString();
        String folderId = createFolderForTest(folderName);
        DriveSyncFilesBody body = new DriveSyncFilesBody();
        DriveExtendedActionsResponse response = driveApi.syncFiles(folderId, "/", body, I(8), null, null, null, null, null, null);
        assertNotNull(response);
        assertNotNull(response.getData());
        checkResponse(response, folderName);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testGetlocalizedPathToRoot_syncFolderRequest(Locale locale, TestInfo testInfo) throws Exception {
        prepareTest(locale, testInfo);
        changeUserSettingLanguage(locale);

        String folderName = "localizedPathToRootTest_syncFolderRequest_" + UUID.randomUUID().toString();
        String folderId = createFolderForTest(folderName);
        DriveSyncFoldersBody body = new DriveSyncFoldersBody();
        DriveSyncFolderResponse response = driveApi.syncFolders(folderId, body, I(8), null, null, null, null);
        assertNotNull(response);
        checkResponse(response, folderName);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testGetlocalizedPathToRoot_syncFoldersRequest(Locale locale, TestInfo testInfo) throws Exception {
        prepareTest(locale, testInfo);
        changeUserSettingLanguage(locale);

        String folderName = "localizedPathToRootTest_syncFoldersRequest_" + UUID.randomUUID().toString();
        String folderId = createFolderForTest(folderName);
        DriveSyncFoldersBody body = new DriveSyncFoldersBody();
        DriveSyncFolderResponse response = driveApi.syncFolders(folderId, body, I(8), null, null, null, null);
        assertNotNull(response);
        checkResponse(response, folderName);
    }

    private void changeUserSettingLanguage(Locale locale) throws ApiException {
        // Changes the locale from test user to the locale value
        // Setting the language via user-api seems to fail here;
        // We can use JSLOB here because "language" is a "shared" setting (com.openexchange.groupware.settings.tree.Language)
        JSlobApi jslobApi = new JSlobApi(getApiClient());
        CommonResponse response = jslobApi.setJSlob(Collections.singletonMap("language", locale), "io.ox/core", null);
        checkResponse(response.getError(), response.getErrorDesc());

        UserApi userApi = new UserApi(getApiClient());
        UserResponse userResponse = userApi.getUser(getApiClient().getUserId().toString());
        UserData updatedUserData = checkResponse(userResponse.getError(), userResponse.getErrorDesc(), userResponse.getData());
        assertEquals(locale != null ? locale.toString() : "en_US", updatedUserData.getLocale(), "Precondition failed:");
    }

    private void checkResponse(DriveExtendedActionsResponse response, String expectedFolderName) {
        assertNotNull(response.getData());
        String localizedPathToRoot = response.getData().getLocalizedPathToRoot();
        assertTrue(Strings.isNotEmpty(localizedPathToRoot));
        assertTrue(localizedPathToRoot.startsWith(realRootPath));
        String subPath = stripSlash(localizedPathToRoot.substring(realRootPath.length()));
        assertEquals(expectedFolderName, subPath);
    }

    private void checkResponse(DriveSyncFolderResponse response, String expectedFolderName) {
        assertNotNull(response.getData());
        String localizedPathToRoot = response.getData().getLocalizedPathToRoot();
        assertTrue(Strings.isNotEmpty(localizedPathToRoot));
        assertTrue(localizedPathToRoot.startsWith(realRootPath));
        String subPath = stripSlash(localizedPathToRoot.substring(realRootPath.length()));
        assertEquals(expectedFolderName, subPath);
    }

    private String createFolderForTest(String folderName) {
        FolderObject generated = folderTestManager.generatePublicFolder(folderName, FolderObject.INFOSTORE, realRootFolderId, userId);
        generated = folderTestManager.insertFolderOnServer(generated);
        assertNotNull(generated);
        return String.valueOf(generated.getObjectID());
    }

    private String getRealRootPath(Locale locale) throws Exception {
        DriveSettingsResponse resp = driveApi.getSettings(String.valueOf(realRootFolderId), I(8), locale != null ? locale.getLanguage() : null);
        assertNotNull(resp);
        assertNotNull(resp.getData());
        return resp.getData().getLocalizedPathToRoot();
    }

    private String stripSlash(String path) {
        if (Strings.isNotEmpty(path)) {
            if (path.startsWith("/")) {
                return path.substring(1);
            }
        }
        return path;
    }

}
