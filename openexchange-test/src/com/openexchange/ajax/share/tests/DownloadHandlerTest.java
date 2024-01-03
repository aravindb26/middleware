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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageGuestObjectPermission;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.container.FolderObject;
import okhttp3.Credentials;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.util.concurrent.TimeUnit;

/**
 * {@link DownloadHandlerTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DownloadHandlerTest extends ShareTest {

    /**
     * Initializes a new {@link DownloadHandlerTest}.
     *
     */
    public DownloadHandlerTest() {
        super();
    }

    @Test
    public void testDownloadSharedFileRandomly() throws Exception {
        testDownloadSharedFile(randomFolderAPI(), randomGuestPermission());
    }

    public void noTestDownloadSharedFileExtensively() throws Exception {
        for (GuestPermissionType permissionType : GuestPermissionType.values()) {
            OCLGuestPermission guestPermission = createGuestPermission(permissionType);
            testDownloadSharedFile(EnumAPI.OX_NEW, guestPermission);
        }
    }

    private void testDownloadSharedFile(EnumAPI api, OCLGuestPermission guestPermission) throws Exception {
        testDownloadSharedFile(api, getDefaultFolder(FolderObject.INFOSTORE), guestPermission);
    }

    private void testDownloadSharedFile(EnumAPI api, int parent, OCLGuestPermission guestPermission) throws Exception {
        /*
         * create folder and a shared file inside
         */
        FileStorageGuestObjectPermission guestObjectPermission = asObjectPermission(guestPermission);
        byte[] contents = new byte[64 + random.nextInt(256)];
        random.nextBytes(contents);
        String filename = randomUID();
        FolderObject folder = insertPrivateFolder(api, FolderObject.INFOSTORE, parent);
        File file = insertSharedFile(folder.getObjectID(), filename, guestObjectPermission, contents);
        /*
         * check permissions
         */
        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission permission : file.getObjectPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created file found");
        checkPermissions(guestObjectPermission, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(file.getId(), matchingPermission.getEntity());
        checkGuestPermission(guestObjectPermission, guest);
        /*
         * check access to share (via guest client)
         */
        String shareURL = discoverShareURL(guestPermission.getApiClient(), guest);
        GuestClient guestClient = resolveShare(shareURL, guestObjectPermission.getRecipient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestObjectPermission);
        /*
         * prepare basic http client to access file directly
         */
        OkHttpClient client = new OkHttpClient.Builder().connectionPool(new ConnectionPool(10,5, TimeUnit.SECONDS)).build();
        String password = getPassword(guestObjectPermission.getRecipient());
        Builder reqBuilder = new Request.Builder().addHeader("http.protocol.cookie-policy", "compatibility");
        if (null != password) {
            String credential = Credentials.basic(getUsername(guestObjectPermission.getRecipient()), getPassword(guestObjectPermission.getRecipient()));
            reqBuilder.addHeader("Authorization", credential);
        }
        Request req = reqBuilder.url(shareURL).build();
        /*
         * check direct download
         */
        for (String queryParameter : new String[] { "delivery=download", "dl=1", "dl=true" }) {
            Request get = req.newBuilder().get().url(shareURL + '?' + queryParameter).build();
            Response resp = client.newCall(get).execute();
            assertEquals(200, resp.code(), "Wrong HTTP status for GET request againt '" + shareURL + '?' + queryParameter + "'");
            String disposition = resp.header("Content-Disposition");
            assertTrue(null != disposition && null != disposition && disposition.startsWith("attachment"), "Wrong content disposition");
            ResponseBody body = resp.body();
            assertNotNull(body, "No file downloaded");
            byte[] downloadedFile = body.bytes();
            Assertions.assertArrayEquals(contents, downloadedFile, "Different contents downloaded");
        }
    }

}
