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

package com.openexchange.ajax.share.tests;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.Autoboxing.l;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.share.ShareAPITest;
import com.openexchange.groupware.modules.Module;
import com.openexchange.share.recipient.RecipientType;
import org.junit.jupiter.params.ParameterizedTest;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.DriveDirectoryMetadata;
import com.openexchange.testing.httpclient.models.DriveFileMetadata;
import com.openexchange.testing.httpclient.models.DriveFileResponse;
import com.openexchange.testing.httpclient.models.DriveFileUpdateBody;
import com.openexchange.testing.httpclient.models.DriveFolderExtendedPermission;
import com.openexchange.testing.httpclient.models.DriveFolderPermission;
import com.openexchange.testing.httpclient.models.DriveFolderResponse;
import com.openexchange.testing.httpclient.models.DriveFolderUpdateBody;
import com.openexchange.testing.httpclient.models.DriveObjectExtendedPermission;
import com.openexchange.testing.httpclient.models.DriveObjectPermission;
import com.openexchange.testing.httpclient.models.DriveShareLinkData;
import com.openexchange.testing.httpclient.models.DriveShareLinkResponse;
import com.openexchange.testing.httpclient.models.DriveShareLinkUpdateBody;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemBody;
import com.openexchange.testing.httpclient.models.InfoItemData;
import com.openexchange.testing.httpclient.models.InfoItemPermission;
import com.openexchange.testing.httpclient.models.InfoItemResponse;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.models.ShareLinkData;
import com.openexchange.testing.httpclient.models.ShareLinkResponse;
import com.openexchange.testing.httpclient.models.ShareLinkUpdateBody;
import com.openexchange.testing.httpclient.models.ShareTargetData;
import com.openexchange.testing.httpclient.models.UserData;
import com.openexchange.testing.httpclient.models.UserResponse;
import com.openexchange.testing.httpclient.modules.DriveApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;
import com.openexchange.testing.httpclient.modules.ShareManagementApi;
import com.openexchange.testing.httpclient.modules.UserApi;
import com.openexchange.tools.io.IOTools;
import jonelo.jacksum.algorithm.MD;

/**
 * Contains test cases for MW-1637.
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Schuerholz</a>
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class LinkPermissionUpdateTest extends ShareAPITest {
    private static final String EMPTY_FOLDER_CHECKSUM = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String INFOSTORE_MODULE = Module.INFOSTORE.getName();
    private static final Long NEW_EXPIRY_DATE = L(1893452400000l);
    private static final String NEW_PASSWORD = "newPassword";
    private static final Long OLD_EXPIRY_DATE = L(1893452000000l);
    private static final String OLD_PASSWORD = "oldPassword";

    //@formatter:off
    public static Stream<Arguments> data() {
        return Stream.of(
                Arguments.of(NEW_PASSWORD, NEW_EXPIRY_DATE, Boolean.TRUE, Boolean.FALSE),
                Arguments.of(NEW_PASSWORD, null, Boolean.TRUE, Boolean.FALSE),
                Arguments.of(NEW_PASSWORD, NEW_EXPIRY_DATE, Boolean.FALSE, Boolean.FALSE),
                Arguments.of(NEW_PASSWORD, null, Boolean.FALSE, Boolean.FALSE),
                Arguments.of(null, NEW_EXPIRY_DATE, Boolean.TRUE, Boolean.FALSE),
                Arguments.of(null, null, Boolean.TRUE, Boolean.FALSE),
                Arguments.of(null, NEW_EXPIRY_DATE, Boolean.FALSE, Boolean.FALSE),
                Arguments.of(null, null, Boolean.FALSE, Boolean.FALSE),
                Arguments.of(NEW_PASSWORD, NEW_EXPIRY_DATE, Boolean.TRUE, Boolean.TRUE),
                Arguments.of(NEW_PASSWORD, null, Boolean.TRUE, Boolean.TRUE),
                Arguments.of(NEW_PASSWORD, NEW_EXPIRY_DATE, Boolean.FALSE, Boolean.TRUE),
                Arguments.of(NEW_PASSWORD, null, Boolean.FALSE, Boolean.TRUE),
                Arguments.of(null, NEW_EXPIRY_DATE, Boolean.TRUE, Boolean.TRUE),
                Arguments.of(null, null, Boolean.TRUE, Boolean.TRUE),
                Arguments.of(null, NEW_EXPIRY_DATE, Boolean.FALSE, Boolean.TRUE),
                Arguments.of(null, null, Boolean.FALSE, Boolean.TRUE)
        );
    }
    //@formatter:on

    private ShareLinkResponse createdShareLinkResponse;
    private File file;
    private String fileId;
    private String folder;
    private String testFolder;

    private DriveApi driveApi;
    private FolderManager folderManager;
    private FoldersApi foldersApi;
    private InfostoreApi infostoreApi;
    private ShareManagementApi shareApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        shareApi = new ShareManagementApi(getApiClient());
        infostoreApi = new InfostoreApi(getApiClient());
        foldersApi = new FoldersApi(getApiClient());
        driveApi = new DriveApi(getApiClient());

        folderManager = new FolderManager(foldersApi, "0");
        testFolder = folderManager.createFolder(folderManager.findInfostoreRoot(), testInfo.getDisplayName(), Module.INFOSTORE.getName());
    }

    /**
     *
     * Tests if the drive API updateFile request updates the password and expiry date of a link permission for a file.
     *
     * @throws ApiException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_DriveUpdateFile(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException, IOException, NoSuchAlgorithmException {
        ShareLinkData shareLinkData = setUpFile(alreadySet);
        Long expiryDateUTC = this.toUTCDate(expiryDate);
        DriveFileMetadata response = driveUpdateFile(password, expiryDateUTC);
        checkExtendedPermissionsInResponse(response, password, expiryDateUTC);
        checkUpdatedLinkFile(fileId, shareLinkData, password, expiryDate);
    }

    /**
     *
     * Tests if the drive API updateFolder request updates the password, expiry date and includeSubfolders flag of a link permission for a folder.
     *
     * @throws ApiException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_DriveUpdateFolder(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException {
        ShareLinkData shareLinkData = setUpFolder(alreadySet);
        Long expiryDateUTC = this.toUTCDate(expiryDate);
        DriveDirectoryMetadata response = driveUpdateFolder(folder, password, expiryDateUTC, includeSubfolders);
        checkExtendedPermissionsInResponse(response, password, expiryDateUTC, includeSubfolders);
        checkUpdateLinkFolder(shareLinkData, password, expiryDate, includeSubfolders);
    }

    /**
     *
     * Tests if the drive API updateLink request updates the password and expiry date of a link permission for a file.
     *
     * @throws ApiException
     * @throws IOException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_DriveUpdateLink_File(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException, IOException {
        ShareLinkData shareLinkData = setUpFile(alreadySet);
        Long expiryDateUTC = this.toUTCDate(expiryDate);
        DriveShareLinkData response = driveUpdateLinkForFile(password, expiryDateUTC);
        checkUpdateLinkResponse(response, password, expiryDateUTC, null);
        checkUpdatedLinkFile(fileId, shareLinkData, password, expiryDate);
    }

    /**
     *
     * Tests if the drive API updateLink request updates the password, expiry date and includeSubfolders flag of a link permission for a folder.
     *
     * @throws ApiException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_DriveUpdateLink_Folder(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException {
        ShareLinkData shareLinkData = setUpFolder(alreadySet);
        Long expiryDateUTC = this.toUTCDate(expiryDate);
        DriveShareLinkData response = driveUpdateLinkForFolder(password, expiryDateUTC, includeSubfolders);
        checkUpdateLinkResponse(response, password, expiryDateUTC, includeSubfolders);
        checkUpdateLinkFolder(shareLinkData, password, expiryDate, includeSubfolders);
    }

    /**
     *
     * Tests if the folders API update request updates the password, expiry date and includeSubfolders flag of a link permission for a folder.
     *
     * @throws ApiException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_FoldersAPI(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException {
        ShareLinkData shareLinkData = setUpFolder(alreadySet);
        foldersUpdate(folder, createdShareLinkResponse.getTimestamp(), password, expiryDate, includeSubfolders);
        checkUpdateLinkFolder(shareLinkData, password, expiryDate, includeSubfolders);
    }

    /**
     *
     * Tests if the infostore API update request updates the password and expiry date of a link permission for a file.
     *
     * @throws ApiException
     * @throws IOException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_InfostoreAPI(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException, IOException {
        ShareLinkData shareLinkData = setUpFile(alreadySet);
        infostoreUpdate(fileId, createdShareLinkResponse.getTimestamp(), password, expiryDate);
        checkUpdatedLinkFile(fileId, shareLinkData, password, expiryDate);
    }

    /**
     *
     * Tests if the share management API updateShareLink request updates the password and expiry date of a link permission for a file.
     *
     * @throws ApiException
     * @throws IOException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_ShareAPI_File(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException, IOException {
        ShareLinkData shareLinkData = setUpFile(alreadySet);
        shareUpdateFile(fileId, createdShareLinkResponse, password, expiryDate);
        checkUpdatedLinkFile(fileId, shareLinkData, password, expiryDate);
    }

    /**
     *
     * Tests if the share management API updateShareLink request updates the password, expiry date and includeSubfolders flag of a link permission for a folder.
     *
     * @throws ApiException
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testUpdateLink_ShareAPI_Folder(String password, Long expiryDate, Boolean includeSubfolders, Boolean alreadySet) throws ApiException {
        ShareLinkData shareLinkData = setUpFolder(alreadySet);
        shareUpdateFolder(folder, createdShareLinkResponse, password, expiryDate, includeSubfolders);
        checkUpdateLinkFolder(shareLinkData, password, expiryDate, includeSubfolders);
    }

    private List<DriveFolderPermission> adjustPermissions(List<FolderPermission> permissions, String password, Long expiryDate, Boolean includeSubfolders) {
        List<DriveFolderPermission> drivePermissions = new ArrayList<>();
        for (FolderPermission p : permissions) {
            DriveFolderPermission dp = new DriveFolderPermission();
            dp.setBits(p.getBits());
            dp.setContactFolder(p.getContactFolder());
            dp.setContactId(p.getContactId());
            dp.setDisplayName(p.getDisplayName());
            dp.setEmailAddress(p.getEmailAddress());
            dp.setEntity(p.getEntity());
            dp.setGroup(p.getGroup());
            dp.setIdentifier(p.getIdentifier());
            dp.setRights(p.getRights());
            if (!dp.getEntity().equals(I(testUser.getUserId()))) {
                dp.setType(RecipientType.ANONYMOUS.toString());
                dp.setPassword(password);
                dp.setExpiryDate(expiryDate);
                dp.setIncludeSubfolders(includeSubfolders);
            } else {
                dp.setType(p.getType());
                dp.setPassword(p.getPassword());
                dp.setExpiryDate(p.getExpiryDate());
                dp.setIncludeSubfolders(p.getIncludeSubfolders());
            }
            drivePermissions.add(dp);
        }
        return drivePermissions;
    }

    private void changeSharelinkSettingsInPermissions(List<FolderPermission> permissions, String password, Long expiryDate, Boolean includeSubfolders) {
        for (FolderPermission p : permissions) {
            if (!p.getEntity().equals(I(testUser.getUserId()))) {
                p.setType(RecipientType.ANONYMOUS.toString());
                p.setPassword(password);
                p.setExpiryDate(expiryDate);
                p.setIncludeSubfolders(includeSubfolders);
            }
        }
    }

    private void checkExtendedPermissionsInResponse(DriveDirectoryMetadata response, String password, Long expiryDate, Boolean includeSubfolders) {
        assertNotNull(response);
        List<DriveFolderExtendedPermission> extendedObjectPermissions = response.getExtendedPermissions();
        assertNotNull(extendedObjectPermissions);
        for (DriveFolderExtendedPermission p : extendedObjectPermissions) {
            if (i(p.getEntity()) != testUser.getUserId()) {
                if (password != null) {
                    assertEquals(password, p.getPassword());
                }
                if (expiryDate != null) {
                    assertEquals(expiryDate, p.getExpiryDate());
                }
                if (includeSubfolders != null) {
                    assertEquals(includeSubfolders, p.getIncludeSubfolders());
                }
            }
        }
    }

    private void checkExtendedPermissionsInResponse(DriveFileMetadata response, String password, Long expiryDate) {
        assertNotNull(response);
        List<DriveObjectExtendedPermission> extendedObjectPermissions = response.getExtendedObjectPermissions();
        assertNotNull(extendedObjectPermissions);
        for (DriveObjectExtendedPermission p : extendedObjectPermissions) {
            if (i(p.getEntity()) != testUser.getUserId()) {
                if (password != null) {
                    assertEquals(password, p.getPassword());
                }
                if (expiryDate != null) {
                    assertEquals(expiryDate, p.getExpiryDate());
                }
            }
        }
    }

    private void checkUpdatedLinkData(ShareLinkData originalShareLinkData, ShareLinkData updatedShareLinkData, String updatedPassword, Long updatedExpiryDate, Boolean updatedIncludeSubfolders) {
        assertEquals(originalShareLinkData.getEntity(), updatedShareLinkData.getEntity());
        assertEquals(Boolean.FALSE, updatedShareLinkData.getIsNew());
        assertEquals(originalShareLinkData.getUrl(), updatedShareLinkData.getUrl());
        if (updatedExpiryDate == null) {
            assertEquals(originalShareLinkData.getExpiryDate(), updatedShareLinkData.getExpiryDate());
        } else {
            assertEquals(updatedExpiryDate, updatedShareLinkData.getExpiryDate());
        }
        if (updatedPassword == null) {
            assertEquals(originalShareLinkData.getPassword(), updatedShareLinkData.getPassword());
        } else {
            assertEquals(updatedPassword, updatedShareLinkData.getPassword());
        }
        if (updatedIncludeSubfolders != null) {
            assertEquals(updatedIncludeSubfolders, updatedShareLinkData.getIncludeSubfolders());
        }
    }

    private void checkUpdatedLinkFile(String fileId, ShareLinkData shareLinkData, String password, Long expiryDate) throws ApiException {
        ShareLinkResponse getShareLinkResponse = createOrGetLinkForFile(fileId, testFolder);
        ShareLinkData updatedShareLinkData = checkResponse(getShareLinkResponse.getError(), getShareLinkResponse.getErrorDesc(), getShareLinkResponse.getData());
        checkUpdatedLinkData(shareLinkData, updatedShareLinkData, password, expiryDate, null);
    }

    private void checkUpdateLinkFolder(ShareLinkData shareLinkData, String password, Long expiryDate, Boolean includeSubfolders) throws ApiException {
        ShareLinkResponse getShareLinkResponse = createOrGetLinkForFolder(folder);
        ShareLinkData updatedShareLinkData = checkResponse(getShareLinkResponse.getError(), getShareLinkResponse.getErrorDesc(), getShareLinkResponse.getData());
        checkUpdatedLinkData(shareLinkData, updatedShareLinkData, password, expiryDate, includeSubfolders);
    }

    private void checkUpdateLinkResponse(DriveShareLinkData response, String password, Long expiryDate, Boolean includeSubfolders) {
        assertNotNull(response);
        if (password != null) {
            assertEquals(password, response.getPassword());
        }
        if (expiryDate != null) {
            assertEquals(expiryDate, response.getExpiryDate());
        }
        if (includeSubfolders != null) {
            assertEquals(includeSubfolders, response.getIncludeSubfolders());
        } else {
            assertEquals(Boolean.FALSE, response.getIncludeSubfolders());
        }
    }

    private ShareLinkResponse createOrGetLinkForFile(String fileId, String folderId) throws ApiException {
        ShareTargetData shareTargetData = new ShareTargetData();
        shareTargetData.setItem(fileId);
        shareTargetData.setFolder(folderId);
        shareTargetData.setModule(INFOSTORE_MODULE);
        ShareLinkResponse shareLinkResponse = shareApi.getShareLink(shareTargetData);
        return shareLinkResponse;
    }

    private ShareLinkResponse createOrGetLinkForFolder(String folder) throws ApiException {
        ShareTargetData shareTargetData = new ShareTargetData();
        shareTargetData.setFolder(folder);
        shareTargetData.setModule(INFOSTORE_MODULE);
        ShareLinkResponse shareLinkResponse = shareApi.getShareLink(shareTargetData);
        return shareLinkResponse;
    }

    private String createTestFile(String parentFolderId) throws IOException, ApiException {
        file = File.createTempFile("LinkPermissionUpdateTestFile", ".txt");
        String name = file.getName();
        byte[] bytesData = IOTools.getBytes(new FileInputStream(file));
        InfoItemUpdateResponse uploadInfoItem = infostoreApi.uploadInfoItem(parentFolderId, name, bytesData, null, null, name, "text/plain", null, null, null, null, null, null, null, null, Boolean.FALSE, Boolean.FALSE, null, null);
        Assertions.assertNull(uploadInfoItem.getErrorDesc(), uploadInfoItem.getError());
        Assertions.assertNotNull(uploadInfoItem.getData());
        return uploadInfoItem.getData();
    }

    private DriveFileMetadata driveUpdateFile(String password, Long expiryDate) throws ApiException, IOException, NoSuchAlgorithmException {
        DriveFileResponse driveFileResponse = driveApi.getFile(folderManager.findInfostoreRoot(), "/" + folderManager.getFolderName(testFolder), file.getName(), getFileChecksum());
        DriveFileMetadata driveFileMeta = checkResponse(driveFileResponse.getError(), driveFileResponse.getErrorDesc(), driveFileResponse.getData());
        List<DriveObjectPermission> objectPermissions = driveFileMeta.getObjectPermissions();
        for (DriveObjectPermission p : objectPermissions) {
            if (!p.getEntity().equals(I(testUser.getUserId()))) {
                p.setType(RecipientType.ANONYMOUS.toString());
                p.setPassword(password);
                p.setExpiryDate(expiryDate);
            }
        }

        DriveFileUpdateBody driveFileUpdateBody = new DriveFileUpdateBody();
        DriveFileMetadata driveFileMetadata = new DriveFileMetadata();
        driveFileMetadata.setObjectPermissions(objectPermissions);
        driveFileUpdateBody.setFile(driveFileMetadata);
        DriveFileResponse updateShareLinkResponse = driveApi.updateFile(folderManager.findInfostoreRoot(), "/" + folderManager.getFolderName(testFolder), file.getName(), getFileChecksum(), driveFileUpdateBody);
        return checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc(), updateShareLinkResponse.getData());
    }

    private DriveDirectoryMetadata driveUpdateFolder(String folder, String password, Long expiryDate, Boolean includeSubfolders) throws ApiException {
        FolderData requestedFolderData = folderManager.getFolder(folder);
        List<FolderPermission> permissions = requestedFolderData.getPermissions();
        assertEquals(2, permissions.size());
        List<DriveFolderPermission> drivePermissions = adjustPermissions(permissions, password, expiryDate, includeSubfolders);

        DriveFolderUpdateBody driveFolderUpdateBody = new DriveFolderUpdateBody();
        DriveDirectoryMetadata driveDirectoryMetadata = new DriveDirectoryMetadata();
        driveDirectoryMetadata.setPermissions(drivePermissions);
        driveFolderUpdateBody.setFolder(driveDirectoryMetadata);
        DriveFolderResponse updateShareLinkResponse = driveApi.updateFolder(folderManager.findInfostoreRoot(), "/" + folderManager.getFolderName(testFolder) + "/" + folderManager.getFolderName(folder), EMPTY_FOLDER_CHECKSUM, driveFolderUpdateBody);
        return checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc(), updateShareLinkResponse.getData());

    }

    private DriveShareLinkData driveUpdateLinkForFile(String password, Long expiryDate) throws ApiException {
        DriveShareLinkUpdateBody driveShareLinkUpdateBody = new DriveShareLinkUpdateBody();
        driveShareLinkUpdateBody.setPassword(password);
        driveShareLinkUpdateBody.setExpiryDate(expiryDate);
        driveShareLinkUpdateBody.setName(file.getName());
        driveShareLinkUpdateBody.setPath("/" + folderManager.getFolder(testFolder).getTitle());
        driveShareLinkUpdateBody.setChecksum(EMPTY_FOLDER_CHECKSUM);
        DriveShareLinkResponse updateShareLinkResponse = driveApi.updateShareLink(folderManager.findInfostoreRoot(), driveShareLinkUpdateBody);
        return checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc(), updateShareLinkResponse.getData());
    }

    private DriveShareLinkData driveUpdateLinkForFolder(String password, Long expiryDate, Boolean includeSubfolders) throws ApiException {
        DriveShareLinkUpdateBody driveShareLinkUpdateBody = new DriveShareLinkUpdateBody();
        driveShareLinkUpdateBody.setExpiryDate(expiryDate);
        driveShareLinkUpdateBody.setPassword(password);
        driveShareLinkUpdateBody.setIncludeSubfolders(includeSubfolders);
        driveShareLinkUpdateBody.setPath("/" + folderManager.getFolder(testFolder).getTitle() + "/testFolder");
        driveShareLinkUpdateBody.setChecksum(EMPTY_FOLDER_CHECKSUM);
        DriveShareLinkResponse updateShareLinkResponse = driveApi.updateShareLink(folderManager.findInfostoreRoot(), driveShareLinkUpdateBody);
        return checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc(), updateShareLinkResponse.getData());
    }

    private void foldersUpdate(String folder, Long timestamp, String password, Long expiryDate, Boolean includeSubfolders) throws ApiException {
        FolderData requestedFolderData = folderManager.getFolder(folder);
        List<FolderPermission> permissions = requestedFolderData.getPermissions();
        assertEquals(2, permissions.size());
        changeSharelinkSettingsInPermissions(permissions, password, expiryDate, includeSubfolders);

        FolderBody folderBody = new FolderBody();
        FolderData folderData = new FolderData();
        folderData.setPermissions(permissions);
        folderBody.setFolder(folderData);
        FolderUpdateResponse updateShareLinkResponse = foldersApi.updateFolder(folder, folderBody, null, timestamp, null, null, null, null, null, null);
        checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc());
    }

    private String getFileChecksum() throws IOException, NoSuchAlgorithmException {
        InputStream document = new FileInputStream(file);

        byte[] buffer = new byte[2048];
        MD md5 = new MD("MD5");
        int read;
        do {
            read = document.read(buffer);
            if (0 < read) {
                md5.update(buffer, 0, read);
            }
        } while (-1 != read);

        document.close();

        return md5.getFormattedValue();
    }

    private void infostoreUpdate(String fileId, Long timestamp, String password, Long expiryDate) throws ApiException {
        InfoItemResponse infoItemResponse = infostoreApi.getInfoItem(fileId, testFolder);
        InfoItemData infoItem = checkResponse(infoItemResponse.getError(), infoItemResponse.getErrorDesc(), infoItemResponse.getData());
        List<InfoItemPermission> objectPermissions = infoItem.getObjectPermissions();
        for (InfoItemPermission p : objectPermissions) {
            if (!p.getEntity().equals(I(testUser.getUserId()))) {
                p.setType(RecipientType.ANONYMOUS.toString());
                p.setPassword(password);
                p.setExpiryDate(expiryDate);
            }
        }

        InfoItemBody infoItemBody = new InfoItemBody();
        InfoItemData infoItemData = new InfoItemData();
        infoItemData.setObjectPermissions(objectPermissions);
        infoItemBody.setFile(infoItemData);
        InfoItemUpdateResponse updateShareLinkResponse = infostoreApi.updateInfoItem(fileId, timestamp, infoItemBody, null, B(false));
        checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc());
    }

    private ShareLinkData setUpFile(Boolean alreadySet) throws IOException, ApiException {
        fileId = createTestFile(testFolder);
        createdShareLinkResponse = createOrGetLinkForFile(fileId, testFolder);
        ShareLinkData shareLinkData = checkResponse(createdShareLinkResponse.getError(), createdShareLinkResponse.getErrorDesc(), createdShareLinkResponse.getData());

        if (alreadySet.booleanValue()) {
            Long timestamp = shareUpdateFile(fileId, createdShareLinkResponse, OLD_PASSWORD, OLD_EXPIRY_DATE);
            createdShareLinkResponse.setTimestamp(timestamp);
            shareLinkData.setPassword(OLD_PASSWORD);
            shareLinkData.setExpiryDate(OLD_EXPIRY_DATE);
        }

        return shareLinkData;
    }

    private ShareLinkData setUpFolder(Boolean alreadySet) throws ApiException {
        folder = folderManager.createFolder(testFolder, "testFolder", INFOSTORE_MODULE);
        createdShareLinkResponse = createOrGetLinkForFolder(folder);
        ShareLinkData shareLinkData = checkResponse(createdShareLinkResponse.getError(), createdShareLinkResponse.getErrorDesc(), createdShareLinkResponse.getData());

        if (alreadySet.booleanValue()) {
            Long timestamp = shareUpdateFolder(folder, createdShareLinkResponse, OLD_PASSWORD, OLD_EXPIRY_DATE, Boolean.FALSE);
            createdShareLinkResponse.setTimestamp(timestamp);
            shareLinkData.setPassword(OLD_PASSWORD);
            shareLinkData.setExpiryDate(OLD_EXPIRY_DATE);
            shareLinkData.setIncludeSubfolders(Boolean.FALSE);
        }

        return shareLinkData;
    }

    private Long shareUpdateFile(String fileId, ShareLinkResponse createdShareLinkResponse, String password, Long expiryDate) throws ApiException {
        ShareLinkUpdateBody shareLinkUpdateBody = new ShareLinkUpdateBody();
        shareLinkUpdateBody.setItem(fileId);
        shareLinkUpdateBody.setFolder(testFolder);
        shareLinkUpdateBody.setPassword(password);
        shareLinkUpdateBody.setExpiryDate(expiryDate);
        shareLinkUpdateBody.setModule(INFOSTORE_MODULE);
        CommonResponse updateShareLinkResponse = shareApi.updateShareLink(createdShareLinkResponse.getTimestamp(), shareLinkUpdateBody);
        checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc());
        return updateShareLinkResponse.getTimestamp();
    }

    private Long shareUpdateFolder(String folder, ShareLinkResponse createdShareLinkResponse, String password, Long expiryDate, Boolean includeSubfolders) throws ApiException {
        ShareLinkUpdateBody shareLinkUpdateBody = new ShareLinkUpdateBody();
        shareLinkUpdateBody.setFolder(folder);
        shareLinkUpdateBody.setPassword(password);
        shareLinkUpdateBody.setExpiryDate(expiryDate);
        shareLinkUpdateBody.setIncludeSubfolders(includeSubfolders);
        shareLinkUpdateBody.setModule(INFOSTORE_MODULE);
        CommonResponse updateShareLinkResponse = shareApi.updateShareLink(createdShareLinkResponse.getTimestamp(), shareLinkUpdateBody);
        checkResponse(updateShareLinkResponse.getError(), updateShareLinkResponse.getErrorDesc());
        return updateShareLinkResponse.getTimestamp();

    }

    private Long toUTCDate(Long date) throws ApiException {
        if (date == null) {
            return null;
        }
        UserApi userApi = new UserApi(getApiClient());
        UserResponse userResponse = userApi.getUser(I(testUser.getUserId()).toString());
        UserData userData = checkResponse(userResponse.getError(), userResponse.getErrorDesc(), userResponse.getData());
        String timeZoneID = userData.getTimezone();
        long expiryDate = l(date);
        TimeZone timeZone = TimeZone.getTimeZone(timeZoneID);
        if (null != timeZone) {
            expiryDate -= timeZone.getOffset(expiryDate);
        }
        return L(expiryDate);
    }

}
