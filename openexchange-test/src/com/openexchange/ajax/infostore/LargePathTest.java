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

package com.openexchange.ajax.infostore;

import static org.junit.Assert.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.B;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.manager.FolderFactory;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.infostore.apiclient.InfostoreApiClientTest;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemListElement;
import com.openexchange.testing.httpclient.models.InfoItemMovedResponse;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;
import com.openexchange.testing.httpclient.models.InfoItemsMovedResponse;
import com.openexchange.testing.httpclient.models.InfoItemsResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link LargePathTest}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v8.0.0
 */
public class LargePathTest extends InfostoreApiClientTest {

    private static final int MAX_FOLDER_NAME_LENGTH = 767;
    private static final String TRASH_FOLDER_ID = "35";

    private FoldersApi foldersApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.foldersApi = new FoldersApi(getApiClient());
    }

    private InfoItemListElement uploadFile(InfostoreApi api, String folderId, String fileName, byte[] content) throws ApiException {
        //@formatter:off
        InfoItemUpdateResponse uploadResponse = api.uploadInfoItemBuilder()
                                                    .withFolderId(folderId)
                                                    .withFilename(fileName)
                                                    .withBody(content)
                                                    .execute();
        //@formatter:on
        assertThat(uploadResponse, is(notNullValue()));
        checkResponse(uploadResponse.getError(), uploadResponse.getErrorDesc());

        InfoItemListElement fileElement = new InfoItemListElement();
        fileElement.setFolder(folderId);
        fileElement.setId(uploadResponse.getData());
        return fileElement;
    }


    private InfoItemsResponse deleteFile(InfoItemListElement file, long timestamp, boolean hardDelete) throws ApiException {
        return infostoreApi.deleteInfoItemsBuilder().withInfoItemListElement(Collections.singletonList(file)).withTimestamp(L(timestamp)).withHardDelete(B(hardDelete)).execute();
    }

    private InfoItemMovedResponse moveFile(InfoItemListElement file, String newFolderId, long timestamp) throws ApiException {
        //GET: Move a single file
        return infostoreApi.moveFileBuilder().withId(file.getId()).withFolder(newFolderId).withTimestamp(L(timestamp)).execute();
    }

    private InfoItemsMovedResponse moveFiles(List<InfoItemListElement> items, String newFolderId) throws ApiException {
        //PUT: Move a set of files
        return infostoreApi.moveInfoItemsBuilder().withFolder(newFolderId).withInfoItemListElement(items).execute();
    }

    private String creatLongFolderName(String basename, char character, int size) {
        StringBuilder s = new StringBuilder(size).append(basename);
        while (s.length() < size) {
            s.append(character);
        }
        return s.toString();
    }

    private String createFolderWithLongName(String parentId) throws ApiException {
        String folderName = creatLongFolderName(UUID.randomUUID().toString(), 'a', MAX_FOLDER_NAME_LENGTH);
        NewFolderBody newFolderBody = new NewFolderBody();
        newFolderBody.setFolder(FolderFactory.getSimpleFolder(folderName, FolderManager.INFOSTORE));
        FolderUpdateResponse response = foldersApi.createFolderBuilder().withFolderId(parentId).withTree("1").withNewFolderBody(newFolderBody).execute();
        String folderId = checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        return folderId;
    }

    private void deleteFileWithLargePath(boolean hardDelete) throws Exception {
        String newFolderId = createFolderWithLongName(getPrivateInfostoreFolder());
        InfoItemListElement newFile = uploadFile(infostoreApi, newFolderId, "a test file.txt", "hello world".getBytes(StandardCharsets.UTF_8));
        InfoItemsResponse deleteFile = deleteFile(newFile, Long.MAX_VALUE, hardDelete);
        assertNull("Deleting a file from a folder with a large path should work", deleteFile.getError());
    }

    /**
     * Tests to move a single file into another folder with a large path
     *
     * @throws Exception
     */
    @Test
    public void testMoveSingleItemWithinLargerPath() throws Exception {
        String sourceFolderWithLargePath = createFolderWithLongName(getPrivateInfostoreFolder());
        String destinationFolderWithLargePath = createFolderWithLongName(getPrivateInfostoreFolder());
        InfoItemListElement newFile = uploadFile(infostoreApi, sourceFolderWithLargePath, "a test file.txt", "hello world".getBytes(StandardCharsets.UTF_8));
        //GET: Move a single file
        InfoItemMovedResponse moveFile = moveFile(newFile, destinationFolderWithLargePath, Long.MAX_VALUE);
        assertNull("Moving a file from a folder with a large path should work", moveFile.getError());
    }

    /**
     * Tests to move multiple files into another folder with a large path
     *
     * @throws Exception
     */
    @Test
    public void testMoveItemsWithinLargerPath() throws Exception {
        String sourceFolderWithLargePath = createFolderWithLongName(getPrivateInfostoreFolder());
        String destinationFolderWithLargePath = createFolderWithLongName(getPrivateInfostoreFolder());
        InfoItemListElement newFile = uploadFile(infostoreApi, sourceFolderWithLargePath, "a test file.txt", "hello world".getBytes(StandardCharsets.UTF_8));
        InfoItemListElement newFile2 = uploadFile(infostoreApi, sourceFolderWithLargePath, "a second test file.txt", "hello world".getBytes(StandardCharsets.UTF_8));
        //PUT: Move multiple files
        InfoItemsMovedResponse response = moveFiles(Arrays.asList(newFile, newFile2), destinationFolderWithLargePath);
        assertNull("Moving a file from a folder with a large path should work", response.getError());
    }

    /**
     * Tests to delete a file from from a folder with a large path
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFileWithLargePath() throws Exception {
        final boolean hardDelete = false;
        deleteFileWithLargePath(hardDelete);
    }

    /**
     * Tests to hard-delete a file from a folder with a large path
     *
     * @throws Exception
     */
    @Test
    public void testHardDeleteFileWithLargePath() throws Exception {
        final boolean hardDelete = true;
        deleteFileWithLargePath(hardDelete);
    }

    /**
     * Tests to move a file from a folder with a large path into the trash
     *
     * @throws Exception
     */
    @Test
    public void testMoveSingleFileFromLargePathToTrash() throws Exception {
        String newFolderId = createFolderWithLongName(getPrivateInfostoreFolder());
        InfoItemListElement newFile = uploadFile(infostoreApi, newFolderId, "a test file.txt", "hello world".getBytes(StandardCharsets.UTF_8));
        //GET: Move a single file
        InfoItemMovedResponse moveFile = moveFile(newFile, TRASH_FOLDER_ID, Long.MAX_VALUE);
        assertNull("Moving a file from a folder with a large path should work", moveFile.getError());
    }

    /**
     * Tests to move multiple files from a folder with a large path into the trash
     *
     * @throws Exception
     */
    @Test
    public void testMoveFileFromLargePathToTrash() throws Exception {
        String sourceFolderWithLargePath = createFolderWithLongName(getPrivateInfostoreFolder());
        InfoItemListElement newFile = uploadFile(infostoreApi, sourceFolderWithLargePath, "a test file.txt", "hello world".getBytes(StandardCharsets.UTF_8));
        InfoItemListElement newFile2 = uploadFile(infostoreApi, sourceFolderWithLargePath, "a second test file.txt", "hello world".getBytes(StandardCharsets.UTF_8));
        //POST: Move multiple files
        InfoItemsMovedResponse response = moveFiles(Arrays.asList(newFile, newFile2), TRASH_FOLDER_ID);
        assertNull("Moving a file from a folder with a large path should work", response.getError());
    }

    /**
     * Tests to delete a folder with a large folder path/origin into trash
     *
     * @throws Exception
     */
    @Test
    public void testDeleteFolderWithLargePath() throws Exception {
        String folder1 = createFolderWithLongName(getPrivateInfostoreFolder());
        String folder2 = createFolderWithLongName(folder1);
        final Boolean hardDelete = Boolean.FALSE;
        deleteFolder(folder2, hardDelete);
    }

    /**
     * Tests to hard-delete a folder with a large folder path/origin
     *
     * @throws Exception
     */
    @Test
    public void testHardDeleteFolderWithLargePath() throws Exception {
        String folder1 = createFolderWithLongName(getPrivateInfostoreFolder());
        String folder2 = createFolderWithLongName(folder1);
        final Boolean hardDelete = Boolean.TRUE;
        deleteFolder(folder2, hardDelete);
    }
}
