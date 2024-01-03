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
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequestNew;
import com.openexchange.ajax.folder.actions.GetResponseNew;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.folderstorage.Folder;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.Strings;
import com.openexchange.server.impl.OCLPermission;

/**
 * {@link FolderItemCountTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.0
 */
public class FolderItemCountTest extends ShareTest {

    /**
     * Initializes a new {@link FolderItemCountTest}.
     *
     * @param name The test name
     */
    public FolderItemCountTest() {
        super();
    }

    @Test
    public void testFolderItemCount() throws Exception {
        /*
         * create shared folder with some files inside
         */
        OCLGuestPermission guestPermission = randomGuestPermission(FolderObject.INFOSTORE);
        FolderObject folder = insertSharedFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getDefaultFolder(FolderObject.INFOSTORE), guestPermission);
        int count = random.nextInt(10);
        for (int i = 0; i < count; i++) {
            insertFile(folder.getObjectID());
        }
        /*
         * check permissions
         */
        OCLPermission matchingPermission = null;
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created folder found");
        checkPermissions(guestPermission, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(EnumAPI.OX_NEW, FolderObject.INFOSTORE, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        /*
         * check item count in shared folder if supported
         */
        GuestClient guestClient = resolveShare(discoverShareURL(guestPermission.getApiClient(), guest), guestPermission.getRecipient());
        GetRequestNew req = new GetRequestNew(EnumAPI.OX_NEW, guestClient.getFolder(), new int[] { 1, 2, 3, 4, 5, 6, 20, 300, 301, 302, 309, 313 });
        GetResponseNew res = guestClient.execute(req);
        Folder fo = res.getFolder();
        String capabilityName = Strings.asciiLowerCase(com.openexchange.file.storage.FileStorageCapability.COUNT_TOTAL.name());
        Set<String> supportedCapabilities = fo.getSupportedCapabilities();
        Assumptions.assumeTrue(null != supportedCapabilities && supportedCapabilities.contains(capabilityName));
        assertEquals(count, fo.getTotal());
    }

}
