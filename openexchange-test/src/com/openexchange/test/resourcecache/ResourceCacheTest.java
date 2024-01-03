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

package com.openexchange.test.resourcecache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXResponse;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.resourcecache.actions.AbstractResourceCacheRequest;
import com.openexchange.test.resourcecache.actions.ConfigurationRequest;
import com.openexchange.test.resourcecache.actions.ConfigurationResponse;
import com.openexchange.test.resourcecache.actions.DeleteRequest;
import com.openexchange.test.resourcecache.actions.DownloadRequest;
import com.openexchange.test.resourcecache.actions.DownloadResponse;
import com.openexchange.test.resourcecache.actions.UploadRequest;
import com.openexchange.test.resourcecache.actions.UploadResponse;
import com.openexchange.test.resourcecache.actions.UsedRequest;

/**
 * {@link ResourceCacheTest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class ResourceCacheTest extends AbstractAJAXSession {

    private static final String FS = "FS";

    private static final String DB = "DB";

    String current = FS;

    public ResourceCacheTest() {
        super();
    }

    @AfterEach
    public void tearDown() throws Exception {
        clearCache();
    }

    private void clearCache() throws OXException, IOException, JSONException {
        DeleteRequest deleteRequest = new DeleteRequest();
        executeTyped(deleteRequest, current);
    }

    @Test
    public void testLifecycleFS() throws Exception {
        current = FS;
        lifecycle();
    }

    private void lifecycle() throws Exception {

        //Preperations if the cache holds old elements
        clearCache();
        long used = executeTyped(new UsedRequest(), current).getUsed();
        assertTrue(used == 0, "Cache is not empty");

        byte file[] = prepareFile(1024);
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.addFile("someimage.jpg", "image/jpeg", new ByteArrayInputStream(file));
        UploadResponse uploadResponse = executeTyped(uploadRequest, current);
        List<String> ids = uploadResponse.getIds();
        assertEquals(1, ids.size(), "wrong number of ids");

        DownloadRequest downloadRequest = new DownloadRequest(ids.get(0));
        DownloadResponse downloadResponse = executeTyped(downloadRequest, current);
        byte[] reloaded = downloadResponse.getBytes();
        assertTrue(Arrays.equals(file, reloaded), "download was not equals upload");

        DeleteRequest deleteRequest = new DeleteRequest(ids.get(0));
        executeTyped(deleteRequest, current);
        downloadResponse = executeTyped(downloadRequest, current);
        assertNull(downloadResponse.getBytes(), "resource was not deleted");
    }

    /*
     * Requires
     * com.openexchange.preview.cache.quotaPerDocument > 0
     * com.openexchange.preview.cache.quota = n * com.openexchange.preview.cache.quotaPerDocument
     */
    @Test
    public void testQuotaAndInvalidationFS() throws Exception {
        current = FS;
        quotaAndInvalidation();
    }

    @Test
    public void testQuotaAndInvalidationDB() throws Exception {
        current = DB;
        quotaAndInvalidation();
    }

    private void quotaAndInvalidation() throws Exception {
        clearCache();
        int[] qts = loadQuotas();
        int quota = qts[0];
        int perDocument = qts[1];
        int n = quota / perDocument;
        if (quota <= 0 || perDocument <= 0 || n < 1) {
            fail("test system is misconfigured. Set correct quotas in preview.properties!");
        }

        long used = executeTyped(new UsedRequest(), current).getUsed();
        assertTrue(used == 0, "Cache is not empty");

        // Fill up the whole cache
        byte[] file = prepareFile(perDocument);
        UploadRequest uploadRequest = new UploadRequest();
        for (int i = 0; i < n; i++) {
            uploadRequest.addFile("someimage_" + i + ".jpg", "image/jpeg", new ByteArrayInputStream(file));
        }
        UploadResponse uploadResponse = executeTyped(uploadRequest, current);
        List<String> ids = uploadResponse.getIds();
        assertEquals(n, ids.size(), "wrong number of ids");

        used = executeTyped(new UsedRequest(), current).getUsed();
        assertTrue(used <= quota, "Quota exceeded: used: " + used + " quota: " + quota);

        for (String id : ids) {
            DownloadRequest downloadRequest = new DownloadRequest(id);
            DownloadResponse downloadResponse = executeTyped(downloadRequest, current);
            byte[] reloaded = downloadResponse.getBytes();
            assertNotNull(reloaded, "resource not found");
            assertTrue(Arrays.equals(file, reloaded), "download was not equals upload");
        }

        uploadRequest = new UploadRequest(true);
        uploadRequest.addFile("someimage_" + n + ".jpg", "image/jpeg", new ByteArrayInputStream(file));
        uploadResponse = executeTyped(uploadRequest, current);
        assertEquals(1, uploadResponse.getIds().size(), "newest resource was not added");
        DownloadRequest downloadRequest = new DownloadRequest(uploadResponse.getIds().get(0));
        DownloadResponse downloadResponse = executeTyped(downloadRequest, current);
        byte[] reloaded = downloadResponse.getBytes();
        assertNotNull(reloaded, "resource not found");
        assertTrue(Arrays.equals(file, reloaded), "download was not equals upload");

        int missing = 0;
        for (String id : ids) {
            downloadRequest = new DownloadRequest(id);
            downloadResponse = executeTyped(downloadRequest, current);
            reloaded = downloadResponse.getBytes();
            if (reloaded == null) {
                missing++;
            }
        }

        assertEquals(1, missing, "Exactly one old resource should have been deleted");
    }

    @Test
    public void testResourceExceedsQuota() throws Exception {
        current = FS;
        resourceExceedsQuota();
    }

    private void resourceExceedsQuota() throws Exception {

        //Preperations if the cache holds old elements
        clearCache();
        long used = executeTyped(new UsedRequest(), current).getUsed();
        assertTrue(used == 0, "Cache is not empty");

        int[] qts = loadQuotas();
        int perDocument = qts[1];
        byte[] file = prepareFile(perDocument + 1);
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.addFile("someimage.jpg", "image/jpeg", new ByteArrayInputStream(file));
        UploadResponse uploadResponse = executeTyped(uploadRequest, current);
        assertEquals(0, uploadResponse.getIds().size(), "resource should not have been cached");
    }

    @Test
    public void testUpdateFS() throws Exception {
        current = FS;

        //Preperations if the cache holds old elements
        clearCache();
        long used = executeTyped(new UsedRequest(), current).getUsed();
        assertTrue(used == 0, "Cache is not empty");

        String id = UUIDs.getUnformattedString(UUID.randomUUID());
        byte file[] = prepareFile(1024);
        UploadRequest uploadRequest = new UploadRequest();
        uploadRequest.setResourceId(id);
        uploadRequest.addFile("someimage.jpg", "image/jpeg", new ByteArrayInputStream(file));
        UploadResponse uploadResponse = executeTyped(uploadRequest, current);
        List<String> ids = uploadResponse.getIds();
        assertEquals(1, ids.size(), "wrong number of ids");

        DownloadRequest downloadRequest = new DownloadRequest(ids.get(0));
        DownloadResponse downloadResponse = executeTyped(downloadRequest, current);
        byte[] reloaded = downloadResponse.getBytes();
        assertTrue(Arrays.equals(file, reloaded), "download was not equals upload");

        // Now update the file and check it again
        file = prepareFile(1024);
        uploadRequest = new UploadRequest();
        uploadRequest.addFile("someimage.jpg", "image/jpeg", new ByteArrayInputStream(file));
        uploadRequest.setResourceId(id);
        uploadResponse = executeTyped(uploadRequest, current);
        List<String> newIds = uploadResponse.getIds();
        assertEquals(1, newIds.size(), "wrong number of ids");
        assertEquals(ids.get(0), newIds.get(0), "id has changed");

        downloadRequest = new DownloadRequest(ids.get(0));
        downloadResponse = executeTyped(downloadRequest, current);
        reloaded = downloadResponse.getBytes();
        assertTrue(Arrays.equals(file, reloaded), "download was not equals upload");

        DeleteRequest deleteRequest = new DeleteRequest(ids.get(0));
        executeTyped(deleteRequest, current);
        downloadResponse = executeTyped(downloadRequest, current);
        assertNull(downloadResponse.getBytes(), "resource was not deleted");
    }

    private int[] loadQuotas() throws Exception {
        ConfigurationRequest configurationRequest = new ConfigurationRequest();
        ConfigurationResponse configurationResponse = executeTyped(configurationRequest, current);
        JSONObject config = configurationResponse.getConfigObject();
        int quota = config.getInt("com.openexchange.preview.cache.quota");
        int perDocument = config.getInt("com.openexchange.preview.cache.quotaPerDocument");
        return new int[] { quota, perDocument };
    }

    <T extends AbstractAJAXResponse> T executeTyped(AbstractResourceCacheRequest<T> request, String cacheType) throws OXException, IOException, JSONException {
        request.setCacheType(cacheType);
        return getClient().execute(request);
    }

    byte[] prepareFile(int length) {
        Random r = new Random();
        byte file[] = new byte[length];
        r.nextBytes(file);
        return file;
    }

}
