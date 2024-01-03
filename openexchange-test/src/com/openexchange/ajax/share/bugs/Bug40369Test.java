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

package com.openexchange.ajax.share.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.ajax.share.actions.GetLinkRequest;
import com.openexchange.ajax.share.actions.GetLinkResponse;
import com.openexchange.ajax.share.actions.ShareLink;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.share.ShareTarget;

/**
 * {@link Bug40369Test}
 *
 * NPE when calling "getLink" for shares
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug40369Test extends ShareTest {

    private static final int NUM_THREADS = 20;

    @Test
    public void testCreateFolderLinkConcurrentlyRandomly() throws Exception {
        testCreateFolderLinkConcurrently(randomFolderAPI(), randomModule());
    }

    public void noTestCreateFolderLinkConcurrentlyExtensively() throws Exception {
        for (EnumAPI api : TESTED_FOLDER_APIS) {
            for (int module : TESTED_MODULES) {
                testCreateFolderLinkConcurrently(api, module);
            }
        }
    }

    private void testCreateFolderLinkConcurrently(EnumAPI api, int module) throws Exception {
        testCreateFolderLinkConcurrently(api, module, getDefaultFolder(module));
    }

    private void testCreateFolderLinkConcurrently(EnumAPI api, int module, int parent) throws Exception {
        /*
         * create folder
         */
        FolderObject folder = insertPrivateFolder(api, module, parent);
        /*
         * get a link for the file concurrently
         */
        ShareTarget target = new ShareTarget(module, String.valueOf(folder.getObjectID()));
        GetLinkResponse[] responses = getLinkConcurrently(target, NUM_THREADS);
        /*
         * check that there's exactly one anonymous guest entity in folder afterwards
         */
        folder = getFolder(api, folder.getObjectID());
        assertNotNull(folder.getPermissions());
        assertEquals(2, folder.getPermissions().size());
        OCLPermission matchingPermission = null;
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created file found");
        ExtendedPermissionEntity guest = discoverGuestEntity(api, module, folder.getObjectID(), matchingPermission.getEntity());
        assertNotNull(guest);
        /*
         * check that all requests went through and yield a share link (allow link to be different temporarily, though)
         */
        for (GetLinkResponse response : responses) {
            if (response.hasError()) {
                fail(response.getErrorMessage());
            }
            assertNotNull(response.getShareLink());
        }
        /*
         * then check that the share url stays the same from now on
         */
        GetLinkRequest request = new GetLinkRequest(target, getTimeZone());
        GetLinkResponse resp = getClient().execute(request);
        ShareLink shareLink = resp.getShareLink();
        resp = getClient().execute(request);
        assertEquals(shareLink.getShareURL(), resp.getShareLink().getShareURL());
    }

    private GetLinkResponse[] getLinkConcurrently(ShareTarget target, int numThreads) throws Exception {
        final GetLinkRequest request = new GetLinkRequest(target, getTimeZone());
        request.setFailOnError(false);
        Thread[] threads = new Thread[numThreads];
        final GetLinkResponse[] responses = new GetLinkResponse[threads.length];
        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            AJAXClient client = getClient();
            threads[i] = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        responses[index] = client.execute(request);
                    } catch (Exception e) {
                        fail(e.getMessage());
                    }
                }
            });
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        return responses;
    }
}
