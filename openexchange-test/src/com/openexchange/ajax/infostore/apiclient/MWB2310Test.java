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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.InfoItemData;
import com.openexchange.testing.httpclient.models.InfoItemUpdateResponse;

/**
 * {@link MWB2310Test} - Uploading a file in chunks caused a quota error although the quota was not reached.
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class MWB2310Test extends InfostoreApiClientTest {

    private static final int MB = 1000000; // 1 MB
    private static final long QUOTA = 10; // 10 MB

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContextConfig(TestContextConfig.builder().withMaxQuota(Long.valueOf(QUOTA))
            // at least one (dummy) configuration is required in order to create a custom context configuration
            .withConfig(Map.of("custom-context", "true")).build()).build();
    }

    /**
     * Helper method to upload a test file of the given size in chunks of up to 1MB
     *
     * @param fileSize The size of the test file to create
     * @param <code>true</code> to indicate the final size, <code>false</code>, otherwise
     * @return The ID of the file created and uploaded in chunks of 1 MB
     * @throws ApiException
     */
    private String uploadFileInChunks(int fileSize, boolean indicateFilesize) throws ApiException {
        int chunkSize = fileSize < MB ? fileSize : MB;
        int remaining = fileSize;
        File file = new File("file.dat");
        Long fileSizeParameter = indicateFilesize ? Long.valueOf(fileSize) : null;

        //upload the initial bunch of data to a new file
        byte[] initialData = new byte[chunkSize];
        String id = uploadInfoItem(null, file, "application/octet-stream", null, initialData, Long.valueOf(0), fileSizeParameter, "file.dat");
        remaining = remaining - chunkSize;
        int offset = chunkSize;
        //if more data is available, add chunk-wise
        while (remaining > 0) {
            //just upload the remaining bytes if they are less than 1MB, or upload a 1MB chunk
            byte[] chunk = remaining < chunkSize ? new byte[remaining] : new byte[chunkSize];
            id = uploadInfoItem(id, file, "application/octet-stream", null, chunk, Long.valueOf(offset), fileSizeParameter, "file.dat");
            remaining = remaining - chunkSize;
            offset = offset + chunkSize;
        }
        return id;
    }

    /**
     * Overridden to be able to omit optional filesize parameter.
     */
    @Override
    protected String uploadInfoItem(String id, File file, String mimeType, String versionComment, byte[] bytes, Long offset, Long filesize, String filename) throws ApiException {
        String name = filename == null ? file.getName() : filename;
        InfoItemUpdateResponse uploadInfoItem = infostoreApi.uploadInfoItem(folderId, name, bytes, timestamp, id, name, mimeType, null, null, null, null, versionComment, null, null, filesize, Boolean.FALSE, Boolean.FALSE, offset, null);
        Assertions.assertNull(uploadInfoItem.getErrorDesc(), uploadInfoItem.getError());
        Assertions.assertNotNull(uploadInfoItem.getData());
        timestamp = uploadInfoItem.getTimestamp();
        return uploadInfoItem.getData();
    }

    /**
     * Test that chunk uploading a file, which fits into the quota, will cause a quota reached error
     *
     * @param indicateFilesize <code>true</code> if the filesize is indicated by the client, <code>false</code>, otherwise
     */
    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testUploadFileInChunksWhichFitsIntoQuota(boolean indicateFilesize) throws Exception {
        int fileSize = MB * 10;
        var fileId = uploadFileInChunks(fileSize, indicateFilesize);
        InfoItemData item = getItem(fileId);
        assertEquals(item.getFileSize(), fileSize);
    }

    /**
     * Test that chunk uploading a file, which does not fit into the quota, will still cause a quota reached error
     * 
     * @param indicateFilesize <code>true</code> if the filesize is indicated by the client, <code>false</code>, otherwise
     */
    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testUploadingTooMuchDataInChunksWillResultInQuotaReachedMessage(boolean indicateFilesize) {
        AssertionFailedError exception = assertThrows(AssertionFailedError.class, () -> {
            int fileSize = MB * 11;
            var fileId = uploadFileInChunks(fileSize, indicateFilesize);
            InfoItemData item = getItem(fileId);
            assertEquals(item.getFileSize(), fileSize);
        });
        assertTrue("A quota reached error should have been returned", exception.getMessage().startsWith(("The allowed quota is reached")));
    }
}
