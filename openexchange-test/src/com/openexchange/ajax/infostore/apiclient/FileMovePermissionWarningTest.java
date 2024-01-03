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

package com.openexchange.ajax.infostore.apiclient;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.ajax.parser.ResponseParser;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.invoker.JSON;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.InfoItemMovedResponse;
import com.openexchange.testing.httpclient.models.InfoItemPermission;
import com.openexchange.testing.httpclient.models.InfoItemsMovedResponse;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.tools.io.IOTools;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 *
 * {@link FileMovePermissionWarningTest}
 *
 * Tests the warning behavior of {@link com.openexchange.file.storage.json.actions.files.MoveAction}.
 * The action can handle single files, files as pairs and folders.
 * The folder move is only tested with the MoveFolderPermissionMode INHERIT to test moving a folder via the files move action.
 * The generation of warnings of the other mode is tested in {@link com.openexchange.ajax.folder.MergeWarningPermissionOnMoveTest}.
 *
 * Tests with (expected = AssertionError.class) test if a MOVE_TO_SHARED_WARNING occurs when a unshared file/folder is moved into a folder with permissions.
 * Warnings for this case are disabled, so the test must fail.
 *
 * @author <a href="mailto:anna.ottersbach@open-xchange.com">Anna Ottersbach</a>
 * @since v7.10.5
 */
public class FileMovePermissionWarningTest extends InfostoreApiClientTest {

    public enum MoveItem {
        FILE_PAIR,
        SINGLE_FILE;
    }

    private static final Integer BITS_ADMIN = Integer.valueOf(403710016);
    private static final Integer BITS_REVIEWER = Integer.valueOf(33025);
    private static final String SHARED_FOLDER_ID = "15";
    private SessionAwareClient apiClient3;
    private File file;
    private FolderManager folderManager;
    private FolderManager folderManager2;
    private Integer userId1;
    private Integer userId2;
    private Integer userId3;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        userId1 = I(getUserId());
        userId2 = I(testUser2.getUserId());
        apiClient3 = testContext.acquireUser().getApiClient();
        userId3 = apiClient3.getUserId();

        folderManager = new FolderManager(new FoldersApi(getApiClient()), "1");
        folderManager2 = new FolderManager(new FoldersApi(testUser2.getApiClient()), "1");

        file = File.createTempFile("FileMovePermissionWarningTest", ".txt");
    }

    /**
     *
     * Tests whether the warning MOVE_TO_ANOTHER_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved
     * from a private folder shared with user 2 to a private folder shared with user 3.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromPrivateSharedToSharedFolder(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createPrivateFolder(userId2);
        String destinationFolderId = createPrivateFolder(userId3);
        OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_ANOTHER_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
    }

    /**
     *
     * Tests whether the warning MOVE_TO_NOT_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved
     * from a private folder shared with user 2 to a private unshared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromPrivateSharedToUnsharedFolder(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createPrivateFolder(userId2);
        String destinationFolderId = createPrivateFolder(null);
        OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_NOT_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
    }

    /**
     *
     * Tests whether the warning MOVE_TO_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved
     * from a private unshared folder to a public folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromPrivateToPublicFolder(MoveItem itemToMove) {
        Assertions.assertThrows(AssertionError.class, () -> {
            String sourceFolderId = createPrivateFolder(null);
            String destinationFolderId = createPublicFolder();
            OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PUBLIC));
            checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
        });
    }

    /**
     *
     * Tests whether the warning MOVE_TO_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved
     * from a private unshared folder to a shared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromPrivateToSharedFolder(MoveItem itemToMove) {
        Assertions.assertThrows(AssertionError.class, () -> {
            String sourceFolderId = createPrivateFolder(null);
            String destinationFolderId = createSharedFolder();
            OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.SHARED));
            checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
        });
    }

    /**
     *
     * Tests whether the warning MOVE_TO_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved
     * from a private unshared folder to a private folder shared with user 2.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromPrivateUnsharedToSharedFolder(MoveItem itemToMove) {
        Assertions.assertThrows(AssertionError.class, () -> {
            String sourceFolderId = createPrivateFolder(null);
            String destinationFolderId = createPrivateFolder(userId2);
            OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));
            checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
        });
    }

    /**
     *
     * Tests whether the warning MOVE_TO_NOT_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved from a public folder to a private unshared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromPublicToPrivateUnsharedFolder(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createPublicFolder();
        String destinationFolderId = createPrivateFolder(null);
        OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_NOT_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.PUBLIC, FolderType.PRIVATE));
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
    }

    /**
     *
     * Tests whether the warning MOVE_TO_ANOTHER_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved from a public folder to a shared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromPublicToSharedFolder(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createPublicFolder();
        String destinationFolderId = createSharedFolder();
        OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_ANOTHER_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.PUBLIC, FolderType.SHARED));
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
    }

    /**
     *
     * Tests whether the warning MOVE_TO_NOT_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved from a shared folder to a private unshared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromSharedToPrivateUnsharedFolder(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createSharedFolder();
        String destinationFolderId = createPrivateFolder(null);
        OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_NOT_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.SHARED, FolderType.PRIVATE));
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
    }

    /**
     *
     * Tests whether the warning MOVE_TO_ANOTHER_SHARED_WARNING
     * is returned, when a file (as pair), a single file or a folder (with permission mode inherit) is moved from a shared folder to a public folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveFromSharedToPublicFolder(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createSharedFolder();
        String destinationFolderId = createPublicFolder();
        OXException expectedFileWarning = FileStorageExceptionCodes.MOVE_TO_ANOTHER_SHARED_WARNING.create(getFileWarningArguments(sourceFolderId, destinationFolderId, FolderType.SHARED, FolderType.PUBLIC));
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId, expectedFileWarning);
    }

    /**
     *
     * Tests whether the warnings
     * {@link com.openexchange.file.storage.FileStorageExceptionCodes#MOVE_SHARED_FILE_WARNING}
     * and {@link com.openexchange.file.storage.FileStorageExceptionCodes#MOVE_TO_SHARED_WARNING}
     * is returned, when a file shared with user 2 is moved from a private unshared to another private shared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveSharedFileFromPrivateUnsharedToPrivateSharedFolder(MoveItem itemToMove) {
        Assertions.assertThrows(AssertionError.class, () -> {
            String sourceFolderId = createPrivateFolder(null);
            String destinationFolderId = createPrivateFolder(userId3);
            String newFileId = uploadInfoItemToFolder(null, file, sourceFolderId, "text/plain", null, null, null, null, null);
            shareFileToUser(newFileId, userId2);
            OXException expectedWarning1 = FileStorageExceptionCodes.MOVE_SHARED_FILE_WARNING.create(getFileWarningArguments(newFileId, sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));
            OXException expectedWarning2 = FileStorageExceptionCodes.MOVE_TO_SHARED_WARNING.create(getFileWarningArguments(newFileId, sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));
            switch (itemToMove) {
                case FILE_PAIR:
                    InfoItemsMovedResponse response = moveInfoItem(newFileId, sourceFolderId, destinationFolderId, false);
                    checkResponseForWarning(response, expectedWarning1);
                    checkResponseForWarning(response, expectedWarning2);
                    assertTrue(fileExistsInFolder(newFileId, sourceFolderId));
                    checkMoveInfoItemRequestWithIgnoreWarnings(newFileId, sourceFolderId, destinationFolderId);
                    assertFalse(fileExistsInFolder(newFileId, destinationFolderId));
                    break;
                case SINGLE_FILE:
                    InfoItemMovedResponse infoItemMovedResponse = moveFile(newFileId, sourceFolderId, destinationFolderId, false);
                    checkResponseForWarning(infoItemMovedResponse, expectedWarning1);
                    checkResponseForWarning(infoItemMovedResponse, expectedWarning2);
                    assertTrue(fileExistsInFolder(newFileId, sourceFolderId));
                    checkMoveFileRequestWithIgnoreWarnings(newFileId, sourceFolderId, destinationFolderId);
                    assertFalse(fileExistsInFolder(newFileId, sourceFolderId));
                    break;
                default:
                    break;
            }
        });
    }

    /**
     *
     * Tests whether the warning
     * {@link com.openexchange.file.storage.FileStorageExceptionCodes#MOVE_SHARED_FILE_WARNING}
     * is returned, when files shared with user 2 is moved from a private unshared to another private unshared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveMultiSharedFilesFromPrivateUnsharedToUnsharedFolder() throws Exception {
        String sourceFolderId = createPrivateFolder(null);
        String destinationFolderId = createPrivateFolder(null);
        String newFileId1 = uploadInfoItemToFolder(null, file, sourceFolderId, "text/plain", null, null, null, null, null);
        shareFileToUser(newFileId1, userId2);
        String newFileId2 = uploadInfoItemToFolder(null, file, sourceFolderId, "text/plain", null, null, null, null, null);
        shareFileToUser(newFileId2, userId2);
        OXException expectedWarning1 = FileStorageExceptionCodes.MOVE_SHARED_FILE_WARNING.create(getFileWarningArguments(newFileId1, sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));
        OXException expectedWarning2 = FileStorageExceptionCodes.MOVE_SHARED_FILE_WARNING.create(getFileWarningArguments(newFileId2, sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));

        InfoItemsMovedResponse response = moveInfoItems(Arrays.asList(newFileId1, newFileId2), sourceFolderId, destinationFolderId, false);
        checkResponseForWarning(response, expectedWarning2);
        checkResponseForWarning(response, expectedWarning1);
        assertTrue(fileExistsInFolder(newFileId1, sourceFolderId));
        assertTrue(fileExistsInFolder(newFileId2, sourceFolderId));
        InfoItemsMovedResponse responseIgnored = moveInfoItems(Arrays.asList(newFileId1, newFileId2), sourceFolderId, destinationFolderId, true);
        assertNotNull(responseIgnored, "Response expected");
        assertEquals(Category.CATEGORY_WARNING.toString(), responseIgnored.getCategories(), "Warning expected, but no error.");
        assertFalse(fileExistsInFolder(newFileId1, destinationFolderId));
        assertFalse(fileExistsInFolder(newFileId2, destinationFolderId));

    }

    /**
     *
     * Tests whether the warning
     * {@link com.openexchange.file.storage.FileStorageExceptionCodes#MOVE_SHARED_FILE_WARNING}
     * is returned, when a file shared with user 2 is moved from a private unshared to another private unshared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveSharedFileFromPrivateUnsharedToUnsharedFolder(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createPrivateFolder(null);
        String destinationFolderId = createPrivateFolder(null);
        String newFileId = uploadInfoItemToFolder(null, file, sourceFolderId, "text/plain", null, null, null, null, null);
        shareFileToUser(newFileId, userId2);
        OXException expectedWarning = FileStorageExceptionCodes.MOVE_SHARED_FILE_WARNING.create(getFileWarningArguments(newFileId, sourceFolderId, destinationFolderId, FolderType.PRIVATE, FolderType.PRIVATE));
        switch (itemToMove) {
            case FILE_PAIR:
                InfoItemsMovedResponse response = moveInfoItem(newFileId, sourceFolderId, destinationFolderId, false);
                checkResponseForWarning(response, expectedWarning);
                assertTrue(fileExistsInFolder(newFileId, sourceFolderId));
                checkMoveInfoItemRequestWithIgnoreWarnings(newFileId, sourceFolderId, destinationFolderId);
                assertFalse(fileExistsInFolder(newFileId, destinationFolderId));
                break;
            case SINGLE_FILE:
                InfoItemMovedResponse infoItemMovedResponse = moveFile(newFileId, sourceFolderId, destinationFolderId, false);
                checkResponseForWarning(infoItemMovedResponse, expectedWarning);
                assertTrue(fileExistsInFolder(newFileId, sourceFolderId));
                checkMoveFileRequestWithIgnoreWarnings(newFileId, sourceFolderId, destinationFolderId);
                assertFalse(fileExistsInFolder(newFileId, sourceFolderId));
                break;
            default:
                break;
        }
    }

    /**
     *
     * Tests whether no warning is returned,
     * when a file (as pair), a single file or a folder (with permission mode inherit) is moved
     * from a private folder shared with user 2 to a private folder shared with user 2.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveToPrivateSharedSamePermissions(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createPrivateFolder(userId2);
        String destinationFolderId = createPrivateFolder(userId2);
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId);
    }

    /**
     *
     * Tests whether no warning is returned,
     * when a file (as pair), a single file or a folder (with permission mode inherit) is moved
     * from a private unshared folder to a private unshared folder.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @EnumSource(MoveItem.class)
    public void testMoveToPrivateUnsharedSamePermission(MoveItem itemToMove) throws Exception {
        String sourceFolderId = createPrivateFolder(null);
        String destinationFolderId = createPrivateFolder(null);
        checkInfoItemMove(itemToMove, sourceFolderId, destinationFolderId);
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        Map<String, String> configs = new HashMap<>();
        configs.put("com.openexchange.folderstorage.permissions.moveToPublic", "inherit");
        configs.put("com.openexchange.folderstorage.permissions.moveToShared", "inherit");
        configs.put("com.openexchange.folderstorage.permissions.moveToPrivate", "inherit");
        return configs;
    }

    @Override
    protected String getScope() {
        return "user";
    }

    private void addFileIdToException(OXException expectedFileWarning, String fileID) {
        Object[] displayArgs = expectedFileWarning.getDisplayArgs();
        displayArgs[3] = fileID;
        expectedFileWarning.setDisplayMessage(expectedFileWarning.getDisplayMessage(null), displayArgs);
    }

    private void checkInfoItemMove(MoveItem itemToMove, String sourceFolderId, String destinationFolderId) throws ApiException, FileNotFoundException, IOException {
        String newItemToMove;
        switch (itemToMove) {
            case FILE_PAIR:
                newItemToMove = createFile(sourceFolderId);
                InfoItemsMovedResponse response = moveInfoItem(newItemToMove, sourceFolderId, destinationFolderId, false);
                assertNotNull(response, "Response expected");
                assertNull(response.getError(), "No errors and warnings expected.");
                assertFalse(fileExistsInFolder(newItemToMove, destinationFolderId));
                break;
            case SINGLE_FILE:
                newItemToMove = createFile(sourceFolderId);
                InfoItemMovedResponse infoItemMovedResponse = moveFile(newItemToMove, sourceFolderId, destinationFolderId, false);
                assertNotNull(infoItemMovedResponse, "Response expected");
                assertNull(infoItemMovedResponse.getError(), "No errors and warnings expected.");
                assertFalse(fileExistsInFolder(newItemToMove, sourceFolderId));
                break;
            default:
                break;
        }
    }

    private void checkInfoItemMove(MoveItem itemToMove, String sourceFolderId, String destinationFolderId, OXException expectedFileWarning) throws ApiException, FileNotFoundException, IOException, JSONException {
        String newItemToMove;
        switch (itemToMove) {
            case FILE_PAIR:
                newItemToMove = createFile(sourceFolderId);
                addFileIdToException(expectedFileWarning, newItemToMove);
                InfoItemsMovedResponse response = moveInfoItem(newItemToMove, sourceFolderId, destinationFolderId, false);
                checkResponseForWarning(response, expectedFileWarning);
                assertTrue(fileExistsInFolder(newItemToMove, sourceFolderId));
                checkMoveInfoItemRequestWithIgnoreWarnings(newItemToMove, sourceFolderId, destinationFolderId);
                assertFalse(fileExistsInFolder(newItemToMove, sourceFolderId));
                break;
            case SINGLE_FILE:
                newItemToMove = createFile(sourceFolderId);
                addFileIdToException(expectedFileWarning, newItemToMove);
                InfoItemMovedResponse infoItemMovedResponse = moveFile(newItemToMove, sourceFolderId, destinationFolderId, false);
                checkResponseForWarning(infoItemMovedResponse, expectedFileWarning);
                assertTrue(fileExistsInFolder(newItemToMove, sourceFolderId));
                checkMoveFileRequestWithIgnoreWarnings(newItemToMove, sourceFolderId, destinationFolderId);
                assertFalse(fileExistsInFolder(newItemToMove, sourceFolderId));
                break;
            default:
                break;
        }
    }

    private void checkMoveFileRequestWithIgnoreWarnings(String fileId, String sourceFolderId, String destinationFolderId) throws ApiException {
        InfoItemMovedResponse ignoreWarningsResponse = moveFile(fileId, sourceFolderId, destinationFolderId, true);
        assertNotNull(ignoreWarningsResponse, "Response expected");
        String[] splittedID = fileId.split("/");
        String newObjectID = destinationFolderId + "/" + splittedID[1];
        assertEquals(newObjectID, ignoreWarningsResponse.getData(), "Expected the new object id.");
        assertEquals(Category.CATEGORY_WARNING.toString(), ignoreWarningsResponse.getCategories(), "Warning expected, but no error.");
    }

    private void checkMoveInfoItemRequestWithIgnoreWarnings(String fileId, String sourceFolderId, String destinationFolderId) throws ApiException {
        InfoItemsMovedResponse ignoreWarningsResponse = moveInfoItem(fileId, sourceFolderId, destinationFolderId, true);
        assertNotNull(ignoreWarningsResponse, "Response expected");
        assertTrue(ignoreWarningsResponse.getData().isEmpty(), "Empty list of conflicting items expected.");
        assertEquals(Category.CATEGORY_WARNING.toString(), ignoreWarningsResponse.getCategories(), "Warning expected, but no error.");
    }

    private void checkParsedResponseForWarning(Response parsedResponse, OXException expectedWarning) {
        List<OXException> warnings = parsedResponse.getWarnings();
        boolean result = false;
        for (OXException warning : warnings) {
            if (warning.getErrorCode().contentEquals(expectedWarning.getErrorCode()) && // equality in error code
                warning.getPlainLogMessage().equals(expectedWarning.getPlainLogMessage())) { // equality in plain log message
                for (int i = 0; i < warning.getDisplayArgs().length; i++) { // equality in display args
                    if (warning.getDisplayArgs()[i].equals(expectedWarning.getDisplayArgs()[i])) {
                        result = true;
                    } else {
                        result = false;
                        continue;
                    }
                }
            }
        }
        assertTrue(result, "Excepected: \"" + expectedWarning.getMessage() + "\"\n, but warnings only contains : \"" + warnings.toString() + "\"");
    }

    private void checkResponseForWarning(InfoItemMovedResponse response, OXException expectedWarning) throws JSONException {
        assertNotNull(response, "Response expected");
        assertNotNull(response.getError(), "Warning expected");
        assertNull(response.getData(), "Expected null for new infoitem id.");
        Response parsedResponse = ResponseParser.parse(new JSON().serialize(response));
        assertTrue(parsedResponse.hasWarnings(), response.getErrorDesc());
        checkParsedResponseForWarning(parsedResponse, expectedWarning);
    }

    private void checkResponseForWarning(InfoItemsMovedResponse response, OXException expectedWarning) throws JSONException {
        assertNotNull(response, "Response expected");
        assertNotNull(response.getError(), "Warning expected");
        assertNotNull(response.getData());
        assertFalse(response.getData().isEmpty(), "Expected an array with the conflicting infoitems");
        Response parsedResponse = ResponseParser.parse(new JSON().serialize(response));
        assertTrue(parsedResponse.hasWarnings(), response.getErrorDesc());
        checkParsedResponseForWarning(parsedResponse, expectedWarning);
    }

    private String createFile(String parentFolderId) throws ApiException, FileNotFoundException, IOException {
        return uploadInfoItemToFolder(null, file, parentFolderId, "text/plain", null, IOTools.getBytes(new FileInputStream(file)), null, null, null);
    }

    private FolderPermission createPermissionFor(Integer entity, Integer bits, Boolean isGroup) {
        FolderPermission p = new FolderPermission();
        p.setEntity(entity);
        p.setGroup(isGroup);
        p.setBits(bits);
        return p;
    }

    private String createPrivateFolder(Integer sharedToUser) throws ApiException {
        return createPrivateFolder(folderId, sharedToUser);
    }

    private String createPrivateFolder(String parentFolderId, Integer sharedToUser) throws ApiException {
        List<FolderPermission> perm = new ArrayList<>();
        FolderPermission p1 = createPermissionFor(userId1, BITS_ADMIN, Boolean.FALSE);
        perm.add(p1);
        if (sharedToUser != null && sharedToUser.intValue() > 0) {
            FolderPermission p = createPermissionFor(sharedToUser, BITS_REVIEWER, Boolean.FALSE);
            perm.add(p);
        }
        return folderManager.createFolder(parentFolderId, "FileMovePermissionWarningTest" + UUID.randomUUID().toString(), perm);
    }

    private String createPublicFolder() throws ApiException {
        List<FolderPermission> perm = new ArrayList<>();
        FolderPermission p1 = createPermissionFor(userId1, BITS_ADMIN, Boolean.FALSE);
        perm.add(p1);
        FolderPermission p = createPermissionFor(I(0), BITS_REVIEWER, Boolean.TRUE);
        perm.add(p);
        String id = folderManager.createFolder(SHARED_FOLDER_ID, "FileMovePermissionWarningTest" + UUID.randomUUID().toString(), perm);
        return id;
    }

    private String createSharedFolder() throws ApiException {
        List<FolderPermission> perm = new ArrayList<>();
        FolderPermission p1 = createPermissionFor(userId2, BITS_ADMIN, Boolean.FALSE);
        perm.add(p1);
        FolderPermission p2 = createPermissionFor(userId1, BITS_ADMIN, Boolean.FALSE);
        perm.add(p2);
        String id = folderManager2.createFolder(getPrivateInfostoreFolder(testUser2.getApiClient()), "FolderPermissionTest_" + UUID.randomUUID().toString(), perm);
        return id;
    }

    private Object[] getFileWarningArguments(String sourceFolderId, String destinationFolderId, FolderType sourceType, FolderType destinationType) throws ApiException {
        return getFileWarningArguments(null, sourceFolderId, destinationFolderId, sourceType, destinationType);
    }

    private Object[] getFileWarningArguments(String fileId, String sourceFolderId, String destinationFolderId, FolderType sourceType, FolderType destinationType) throws ApiException {
        // @formatter:off
        String sourceFolderPath = sourceType.getRootPath() +
                                 (sourceType.equals(FolderType.SHARED) ? folderManager2.getFolderName(getPrivateInfostoreFolder(testUser2.getApiClient())) + "/" : "") +
                                 (sourceType.equals(FolderType.PRIVATE) ? folderTitle + "/" : "") +
                                 folderManager.getFolderName(sourceFolderId);
        String destinationFolderPath = destinationType.getRootPath() +
                                      (destinationType.equals(FolderType.SHARED) ? folderManager2.getFolderName(getPrivateInfostoreFolder(testUser2.getApiClient())) + "/" : "") +
                                      (destinationType.equals(FolderType.PRIVATE) ? folderTitle + "/" : "") +
                                      folderManager.getFolderName(destinationFolderId);
        // @formatter:on
        Object[] args = { file.getName(), sourceFolderPath, destinationFolderPath, fileId, destinationFolderId };
        return args;
    }

    private void shareFileToUser(String fileId, Integer userId) throws ApiException {
        InfoItemPermission permission = new InfoItemPermission();
        permission.entity(userId);
        permission.setGroup(Boolean.FALSE);
        permission.setBits(InfoItemPermission.BitsEnum.NUMBER_1);
        List<InfoItemPermission> permissions = Collections.singletonList(permission);
        updatePermissions(fileId, permissions);
    }

    public enum FolderType {

        PRIVATE("/Drive/My files/"),
        SHARED("/Drive/Shared files/"),
        PUBLIC("/Drive/Public files/");

        private final String rootPath;

        FolderType(String rootPath) {
            this.rootPath = rootPath;
        }

        /**
         * Gets the rootPath
         *
         * @return The rootPath
         */
        public String getRootPath() {
            return rootPath;
        }
    }


}
