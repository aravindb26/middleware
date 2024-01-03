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

package com.openexchange.ajax.infostore.thirdparty;

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jcodec.common.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.groupware.modules.Module;
import com.openexchange.test.Host;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.FileAccountCreationResponse;
import com.openexchange.testing.httpclient.models.FileAccountData;
import com.openexchange.testing.httpclient.models.FileAccountsResponse;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.FoldersCleanUpResponse;
import com.openexchange.testing.httpclient.models.InfoItemData;
import com.openexchange.testing.httpclient.models.InfoItemListElement;
import com.openexchange.testing.httpclient.models.InfoItemResponse;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemsMovedResponse;
import com.openexchange.testing.httpclient.models.InfoItemsResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.FilestorageApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;

/**
 * {@link AbstractFileStorageAccountTest}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.10.5
 */
public abstract class AbstractFileStorageAccountTest extends AbstractConfigAwareAPIClientSession {

    private String privateInfostoreFolder;
    private FolderManager folderManager;

    protected FilestorageApi filestorageApi;
    protected InfostoreApi infostoreApi;
    protected FoldersApi foldersApi;
    protected ConfigApi configApi;

    public static class TestFile {

        String folderId;
        String id;
        String name;

        public TestFile(String folderId, String id, String name) {
            this.folderId = folderId;
            this.name = name;
            this.id = id;
        }
    }

    protected final byte[] testContent = "This is a test content".getBytes();

    protected  static Stream<String> data() {
        return Stream.of("default");
    }

    protected void prepareTest(String data, TestInfo testInfo) throws Exception {
    }

    /**
     * Returns the ID of the account's root folder to test
     *
     * @return The ID of the root folder
     * @throws Exception
     */
    abstract protected String getRootFolderId() throws Exception;

    /**
     * Returns the account data used for testing
     *
     * @return The account data
     * @throws Exception
     */
    abstract protected FileAccountData getAccountData() throws Exception;

    abstract protected TestFile createTestFile() throws Exception;

    /**
     * Returns some wrong account configuration
     *
     * @return Some incorrect account configuration data
     */
    abstract protected Object getWrongFileStorageConfiguration();

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        filestorageApi = new FilestorageApi(getApiClient(Host.SINGLENODE));
        infostoreApi = new InfostoreApi(getApiClient(Host.SINGLENODE));
        foldersApi = new FoldersApi(getApiClient(Host.SINGLENODE));
        configApi = new ConfigApi(getApiClient(Host.SINGLENODE));
        folderManager = new FolderManager(foldersApi, "0");
    }

    /**
     * Creates a new file storage account
     *
     * @param serviceId The ID of the service to create an account for
     * @param displayName The display name of the new account
     * @param configuration The configuration to use
     * @return The new created account
     * @throws ApiException
     */
    protected FileAccountData createAccount(String serviceId, String displayName, Object configuration) throws ApiException {

        FileAccountData testFileAccount = new FileAccountData();
        testFileAccount.setFilestorageService(serviceId);
        testFileAccount.setDisplayName(displayName);
        testFileAccount.setConfiguration(configuration);
        FileAccountCreationResponse response = filestorageApi.createFileAccount(testFileAccount);
        String newAccountId = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        testFileAccount.setId(newAccountId);
        return testFileAccount;
    }

    /**
     * Returns a random file name
     *
     * @return The random file name
     */
    protected String getRandomFileName() {
        Random random = new Random();
        return String.format("Test-File-%s", I(random.nextInt()));
    }

    /**
     * Returns a random folder name
     *
     * @return The random folder name
     */
    protected String getRandomFolderName() {
        Random random = new Random();
        //including a "+" sign in the folder regards to test MWB-705
        return String.format("Test+Folder-%s", I(random.nextInt()));
    }

    /**
     * Gets a remote folder
     *
     * @param id The ID of the folder to get
     * @return The {@link FolderData} of the folder with the given id
     * @throws ApiException
     */
    protected FolderData getFolder(String id) throws ApiException {
        return getFolder(getApiClient(Host.SINGLENODE), foldersApi, id);
    }

    /**
     * Gets a remote folder
     *
     * @param client The {@link SessionAwareClient}
     * @param foldersApi the foldersApi to use
     * @param id The ID of the folder to get
     * @return The {@link FolderData} of the folder with the given id
     * @throws ApiException
     */
    protected FolderData getFolder(SessionAwareClient client, FoldersApi foldersApi, String id) throws ApiException {
        FolderResponse response = foldersApi.getFolder(id, null, null, null, null);
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }

    /**
     * Gets a remote Info-Item
     *
     * @param id The ID of the item to fetch
     * @param folder The ID of the item's folder
     * @return The Info-Item for the given id and folder
     * @throws ApiException
     */
    protected InfoItemData getInfoItem(String id, String folder) throws ApiException {
        InfoItemResponse response = infostoreApi.getInfoItem(id, folder);
        InfoItemData itemData = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        //assertThat(id, is(itemData.getId()));
        assertThat(folder, is(itemData.getFolderId()));
        return itemData;
    }

    /*
     * Downloads a document
     */
    protected File getDocument(String folder, String id) throws ApiException {
        return infostoreApi.getInfoItemDocument(folder, id, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Gets all InfoItem IDs of a specified folder
     *
     * @param folder The folder to get the item IDs for
     * @return A list of Item IDs
     * @throws ApiException
     */
    protected List<String> getAllInfoItems(String folder) throws ApiException {
        InfoItemsResponse allResponse = infostoreApi.getAllInfoItems(folder, "1", null, null, null, null, null, null);
        List<List<String>> ret = (List<List<String>>) checkResponse(allResponse.getError(), allResponse.getErrorDesc(), allResponse.getData());
        List<String> itemIds = new ArrayList<>();
        for (List<String> itemData : ret) {
            for (String data : itemData) {
                itemIds.add(data);
            }
        }
        return itemIds;
    }

    /**
     * Helper method to upload a new item
     *
     * @param folderId The ID of the folder to upload an item to
     * @param fileName The name of the item to upload
     * @param content The content to upload
     * @param mimeType The mime-type of the content
     * @return The ID of the new item
     * @throws ApiException
     */
    protected String uploadInfoItem(String folderId, String fileName, byte[] content, String mimeType) throws ApiException {
        return uploadInfoItem(folderId, null, fileName, mimeType, null, content, null, null, null);
    }

    /**
     * Helper method to upload a new item
     *
     * @param folderId The ID of the folder to upload an item to
     * @param id The ID of the item
     * @param fileName The name of the item
     * @param mimeType The mime-type of the content
     * @param versionComment A version comment
     * @param bytes The content
     * @param offset The offset
     * @param filesize The size of the content
     * @param timestamp The time stamp
     * @return The ID of the new item
     * @throws ApiException
     */
    protected String uploadInfoItem(String folderId, String id, String fileName, String mimeType, String versionComment, byte[] bytes, Long offset, Long filesize, Long timestamp) throws ApiException {
        InfoItemUpdateResponse uploadInfoItem = infostoreApi.uploadInfoItem(folderId, fileName, bytes, timestamp, id, fileName, mimeType, null, null, null, null, versionComment, null, null, filesize == null ? Long.valueOf(bytes.length) : filesize, Boolean.FALSE, Boolean.FALSE, offset, null);
        Assertions.assertNull(uploadInfoItem.getErrorDesc(), uploadInfoItem.getError());
        Assertions.assertNotNull(uploadInfoItem.getData());
        return uploadInfoItem.getData();
    }

    /**
     * Helper method to delete an item
     *
     * Uses {@link Long#MAX_VALUE} for the timestamp
     *
     * @param hardDelete Whether or not to perform a hardDelete
     * @param toDelete The items to delete
     * @throws ApiException
     */
    protected void deleteInfoItems(boolean hardDelete, InfoItemListElement... toDelete) throws ApiException {
        deleteInfoItems(hardDelete, L(Long.MAX_VALUE), toDelete);
    }

    /**
     * Helper method to delete an item
     *
     * @param hardDelete Whether or not to perform a hardDelete
     * @param timestamp The timestamp to use
     * @param toDelete The items to delete
     * @throws ApiException
     */
    protected void deleteInfoItems(boolean hardDelete, Long timestamp, InfoItemListElement... toDelete) throws ApiException {
        InfoItemsResponse deleteInfoItems = infostoreApi.deleteInfoItems(timestamp, Arrays.asList(toDelete), B(hardDelete), null);
        Assertions.assertNull(deleteInfoItems.getError());
        Assertions.assertNotNull(deleteInfoItems.getData());
        Object data = deleteInfoItems.getData();
        Assertions.assertTrue(data instanceof ArrayList<?>);
        ArrayList<?> arrayData = (ArrayList<?>) data;
        assertThat(I(0), is(I(arrayData.size())));
    }

    /**
     * Creates a folder
     *
     * @param parentFolder The parent folder of the new folder
     * @param title The title of the new folder
     * @return The data for the new folder
     * @throws ApiException
     */
    protected FolderData createFolder(String parentFolder, String title) throws ApiException {
        return createFolder(getApiClient(Host.SINGLENODE), this.foldersApi, parentFolder, title, null);
    }

    /**
     * Creates a folder
     *
     * @param client The {@link SessionAwareClient}
     * @param foldersApi the FoldersApi to use
     * @param parentFolder The parent folder of the new folder
     * @param title The title of the new folder
     * @return The data for the new folder
     * @throws ApiException
     */
    protected FolderData createFolder(SessionAwareClient client, FoldersApi foldersApi, String parentFolder, String title) throws ApiException {
        return createFolder(client, foldersApi, parentFolder, title, null);
    }

    /**
     * Creates a folder
     *
     * @param client The {@link SessionAwareClient}
     * @param foldersApi The FoldersApi to use
     * @param parentFolder The parent folder of the new folder
     * @param title The title of the new folder
     * @param permissions The permissions to set
     * @return The data for the new folder
     * @throws ApiException
     */
    protected FolderData createFolder(SessionAwareClient client, FoldersApi foldersApi, String parentFolder, String title, List<FolderPermission> permissions) throws ApiException {
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule(Module.INFOSTORE.getName());
        folder.setTitle(title);
        folder.setId(title);
        folder.setPermissions(permissions);
        body.setFolder(folder);
        FolderUpdateResponse response = foldersApi.createFolder(parentFolder, body, null, null, null, null);

        String folderId = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        return getFolder(client, foldersApi, folderId);
    }

    /**
     * Deletes a folder
     *
     * @param folderId The folder to delete
     * @param hardDelete true to perform a hard-delete, false to put the folder into the trash-bin
     * @return A list of folder IDs which could <b>NOT</b> be removed due conflicts or errors
     * @throws ApiException
     */
    protected List<String> deleteFolder(String folderId, boolean hardDelete) throws ApiException {
        return deleteFolder(getApiClient(Host.SINGLENODE), foldersApi, folderId, hardDelete);
    }

    /**
     * Deletes a folder
     *
     * @param client The {@link SessionAwareClient}
     * @param foldersApi
     * @param folderId The folder to delete
     * @param hardDelete true to perform a hard-delete, false to put the folder into the trash-bin
     * @return A list of folder IDs which could <b>NOT</b> be removed due conflicts or errors
     * @throws ApiException
     */
    protected List<String> deleteFolder(SessionAwareClient client, FoldersApi foldersApi, String folderId, boolean hardDelete) throws ApiException {
        final boolean failOnError = true;
        final boolean extendedResponse = false;
        FoldersCleanUpResponse response = foldersApi.deleteFolders(Collections.singletonList(folderId), null, null, null, B(hardDelete), B(failOnError), B(extendedResponse), null, null);
        return checkResponse(response.getError(), response.getErrorDesc(), response.getData());
    }


    /**
     * Helper method to gain the root folder ID of the original infostore
     *
     * @param client The client to use
     * @return the folder ID of the infostore
     * @throws ApiException
     */
    protected String getPrivateInfostoreFolderID(SessionAwareClient client) throws ApiException {
        if (null == privateInfostoreFolder) {
            assertNotNull(client);
            ConfigApi configApi = new ConfigApi(client);
            ConfigResponse configNodeResponse = configApi.getConfigNode(Tree.PrivateInfostoreFolder.getPath());
            Object data = checkResponse(configNodeResponse.getError(), configNodeResponse.getErrorDesc(), configNodeResponse.getData());
            if (data != null && !data.toString().equalsIgnoreCase("null")) {
                privateInfostoreFolder = String.valueOf(data);
            } else {
                Assertions.fail("It seems that the user doesn't support drive.");
            }

        }
        return privateInfostoreFolder;
    }

    /**
     * Returns the name of the user's private infostore folder
     *
     * @param client The client to use
     * @return The name of the user's private infostore folder ("anton anton" for example).
     * @throws ApiException
     */
    protected String getPrivateInfostoreFolderName(SessionAwareClient client) throws ApiException {
        FolderData rootFolder = getFolder(getPrivateInfostoreFolderID(client));
        String name = rootFolder.getTitle();
        return name;
    }

    /**
     * Test if the account could be registered
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testGetAllFileAccounts(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);
        final boolean connectionCheck = true;
        FileAccountsResponse response = filestorageApi.getAllFileAccounts(null, B(connectionCheck));
        List<FileAccountData> allAccounts = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        assertThat(allAccounts, is(not(empty())));
        FileAccountData account = getAccountData();
        assertThat("account must not be null", account, is(notNullValue()));
        List<FileAccountData> accountData = allAccounts.stream().filter(a -> a.getId().equals(account.getId())).collect(Collectors.toList());
        assertThat(accountData, is(not(nullValue())));
        FileAccountData fileAccount = accountData.get(0);
        assertThat(fileAccount.getHasError(), is(nullValue()));
        assertThat(fileAccount.getError(), is(emptyOrNullString()));
    }

    /**
     * Test that account registration with incorrect configuration fails
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testRegisterIncorrectAccountNotPossible(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);
        FileAccountData account = getAccountData();
        assertThat("account must not be null", account, is(notNullValue()));
        final String incorrectDisplayName = "IncorrectAccount";

        FileAccountData incorrectFileAccount = new FileAccountData();
        incorrectFileAccount.setFilestorageService(account.getFilestorageService());
        incorrectFileAccount.setDisplayName(incorrectDisplayName);
        //Wrong configuration
        incorrectFileAccount.setConfiguration(getWrongFileStorageConfiguration());
        FileAccountCreationResponse response = filestorageApi.createFileAccount(incorrectFileAccount);
        assertThat(response.getError(), not(emptyOrNullString()));

        final boolean connectionCheck = true;
        FileAccountsResponse allResponse = filestorageApi.getAllFileAccounts(null, B(connectionCheck));
        List<FileAccountData> allAccounts = checkResponse(allResponse.getError(), allResponse.getErrorDesc(), allResponse.getData());
        List<FileAccountData> shouldBeEmpty = allAccounts.stream().filter(a -> a.getDisplayName().equals(incorrectDisplayName)).collect(Collectors.toList());
        assertThat(shouldBeEmpty, is(empty()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testListFilesInFolder(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);
        TestFile newFile = createTestFile();
        assertThat("testfile must not be null", newFile, is(notNullValue()));
        //TODO/FIXME? Currently the ID returned when creating a new file is not mangled; so we fetch it again to match it against the other items in the directory later
        InfoItemData file = getInfoItem(newFile.id, newFile.folderId);
        FolderData folder = getFolder(file.getFolderId());
        assertThat(folder.getId(), is(file.getFolderId()));
        List<String> allInfoItems = getAllInfoItems(folder.getId());

        boolean found = false;
        for (String infoItem : allInfoItems) {
            InfoItemData f = getInfoItem(infoItem, folder.getId());
            if (f.getId().equals(file.getId())) {
                found = true;
                break;
            }
        }

        assertThat(B(found), is(Boolean.TRUE));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testCreateDeleteFile(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);
        //Create a new file
        String fileName = getRandomFileName();
        InfoItemListElement newItem = new InfoItemListElement();
        String newFileId = uploadInfoItem(getRootFolderId(), fileName, testContent, "application/text");
        newItem.setFolder(getRootFolderId());
        newItem.setId(newFileId);

        //Check if the file is there
        getInfoItem(newItem.getId(), newItem.getFolder());

        //delete the file again
        deleteInfoItems(true, newItem);

        //Gone?
        InfoItemResponse getResponse2 = infostoreApi.getInfoItem(newItem.getId(), newItem.getFolder());
        assertThat(getResponse2.getData(), is(nullValue()));

        //There was a bug which causes a duplicated, null byte, file. Check that this file is not being created anymore
        String mustNotExist = fileName + " (1)";
        InfoItemsResponse allResponse = infostoreApi.getAllInfoItems(newItem.getFolder(), "700" /* title */, null, null, null, null, null, null);
        List<List<String>> ret = (List<List<String>>) checkResponse(allResponse.getError(), allResponse.getErrorDesc(), allResponse.getData());
        for (List<String> itemData : ret) {
            for (String data : itemData) {
                assertThat(data, is(not(mustNotExist)));
            }
        }
    }

    /**
     * Tests to copy a file to another folder
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCopyFile(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);
        //Create a new file in the root folder
        String newFileId = uploadInfoItem(getRootFolderId(), getRandomFileName(), testContent, "application/text");
        InfoItemListElement newItem = new InfoItemListElement();
        newItem.setFolder(getRootFolderId());
        newItem.setId(newFileId);

        //Create a destination folder
        FolderData folder = createFolder(getRootFolderId(), getRandomFolderName());

        //Copy the file
        InfoItemData fileToCopy = getInfoItem(newItem.getId(), newItem.getFolder());
        fileToCopy.setFolderId(folder.getId());
        InfoItemUpdateResponse response = infostoreApi.copyInfoItem(fileToCopy.getId(), fileToCopy, null);
        String copiedId = checkResponse(response.getError(), response.getErrorDesc(), response.getData());

        //Get The file and check if the content is okay
        InfoItemData copiedInfoItem = getInfoItem(copiedId, folder.getId());
        //Check if the file is present get the content
        File copiedContent = infostoreApi.getInfoItemDocument(folder.getId(), copiedInfoItem.getId(), null, null, null, null, null, null, null, null, null, null, null, null, null);
        try {
            assertThat(copiedContent, is(not(nullValue())));
            byte[] data = IOUtils.readFileToByteArray(copiedContent);
            assertThat(data, is(testContent));
        } finally {
            //cleanup
            Files.delete(copiedContent.toPath());
        }

        final boolean hardDelete = true;
        deleteInfoItems(hardDelete, newItem);
        deleteFolder(folder.getId(), hardDelete);
    }

    /**
     * Test to move a file to another folder
     *
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMoveFile(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);

        String originalFolderId = getRootFolderId();
        //Create a new file in the root folder
        String newFileId = uploadInfoItem(originalFolderId, getRandomFileName(), testContent, "application/text");
        InfoItemListElement newItem = new InfoItemListElement();
        newItem.setFolder(originalFolderId);
        newItem.setId(newFileId);

        //Create a destination folder
        String destinationFolderId = createFolder(getRootFolderId(), getRandomFolderName()).getId();
        FolderData folder = getFolder(destinationFolderId);
        assertThat(folder.getId(), is(destinationFolderId));

        //InfoItemsMovedResponse response = infostoreApi.moveFile(L(0), destinationFolderId, newFileId, null);
        InfoItemListElement itemToMove = new InfoItemListElement();
        itemToMove.setFolder(getRootFolderId());
        itemToMove.setId(newFileId);
        InfoItemsMovedResponse response = infostoreApi.moveInfoItems(destinationFolderId, Collections.singletonList(itemToMove), null, null);
        List<InfoItemListElement> notMoved = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        assertThat(notMoved, is(empty()));

        //Check that the file is not in the source directory anymore
        List<String> allInfoItems = getAllInfoItems(originalFolderId);
        for(String id : allInfoItems) {
           assertThat(id, is(not(newFileId)));
        }

        //Get the moved file and check it's content
        InfoItemsResponse allResponse = infostoreApi.getAllInfoItems(destinationFolderId, "1", null, null, null, null, null, null);
        List<List<String>> ret = (List<List<String>>) checkResponse(allResponse.getError(), allResponse.getErrorDesc(), allResponse.getData());
        assertThat(ret, is(not(empty())));
        assertThat(I(ret.size()), is(I(1)));
        String movedId = ret.get(0).get(0);
        File movedContent = infostoreApi.getInfoItemDocument(destinationFolderId, movedId, null, null, null, null, null, null, null, null, null, null, null, null, null);
        try {
            assertThat(movedContent, is(not(nullValue())));
            byte[] data = IOUtils.readFileToByteArray(movedContent);
            assertThat(data, is(testContent));
        } finally {
            //cleanup
            Files.delete(movedContent.toPath());
        }

        final boolean hardDelete = true;
        deleteFolder(destinationFolderId, hardDelete);
    }

    /**
     * Tests to create and delete a folder
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testCreateDeleteFolder(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);

        //Create
        FolderData folder = createFolder(getRootFolderId(), getRandomFolderName());

        //Delete the folder
        final boolean hardDelete = true;
        List<String> notDeletedIds = deleteFolder(folder.getId(), hardDelete);
        assertThat(notDeletedIds, not(contains("")));

        //Folder gone?
        FolderResponse response = foldersApi.getFolder(folder.getId(), null, null, null, hardDelete);
        assertThat(response.getError(), not(emptyOrNullString()));
        assertThat(response.getErrorDesc(), not(emptyOrNullString()));
        assertThat(response.getCode(), anyOf(is("FILE_STORAGE-0007"), is("FLD-0008"))); /* FOLDER_NOT_FOUND */
    }

    /**
     * Tests to move a folder into another folder
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("data")
    public void testMoveFolder(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);

        // Create a folder
        FolderData folder = createFolder(getRootFolderId(), getRandomFolderName());

        // Create a 2nd folder
        FolderData newFolder2 = createFolder(getRootFolderId(), getRandomFolderName());

        // Move the 2nd folder into the first one
        String movedFolderId = folderManager.moveFolder(newFolder2.getId(), folder.getId(), TRUE);

        // Folder present?
        FolderResponse checkResponse = foldersApi.getFolder(movedFolderId, null, null, null, null);
        FolderData checkedFolderData = checkResponse(checkResponse.getError(), checkResponse.getErrorDesc(), checkResponse.getData());
        assertThat(checkedFolderData.getId(), is(movedFolderId));
        // Check that the folder has the new parent folder set
        assertThat(checkedFolderData.getFolderId(), is(folder.getId()));

        final boolean hardDelete = true;
        deleteFolder(folder.getId(), hardDelete);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testDownloadDocument(String fileStorageService, TestInfo testInfo) throws Exception {
        prepareTest(fileStorageService, testInfo);
        TestFile testFile = createTestFile();
        assertThat("testfile must not be null", testFile, is(notNullValue()));
        File document = getDocument(testFile.folderId, testFile.id);
        byte[] content = Files.readAllBytes(Paths.get(document.getPath()));
        assertThat(content, is(testContent));
    }
}