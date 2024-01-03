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

package com.openexchange.ajax.folder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;

/**
 * {@link GetVirtualTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class GetVirtualTest extends AbstractAJAXSession {


    @Test
    public void testGetVirtual() throws Throwable {
        final GetRequest getRequest = new GetRequest(EnumAPI.OX_OLD, FolderObject.VIRTUAL_LIST_CALENDAR_FOLDER_ID, new int[] { FolderObject.OBJECT_ID, FolderObject.FOLDER_NAME, FolderObject.OWN_RIGHTS, FolderObject.PERMISSIONS_BITS });
        final GetResponse getResponse = getClient().execute(getRequest);
        assertFalse(getResponse.hasError(), "GET request failed.");
        final FolderObject folder = getResponse.getFolder();
        assertEquals(FolderObject.VIRTUAL_LIST_CALENDAR_FOLDER_ID, folder.getObjectID(), "Unexpected object ID: ");

        final OCLPermission[] perms = folder.getNonSystemPermissionsAsArray();
        assertNotNull(perms, "Missing permissions");
        assertEquals(1, perms.length, "Unexpected number of permissions: ");

        final OCLPermission p = perms[0];
        assertNotNull(p, "Missing permission");
        assertEquals(OCLPermission.ALL_GROUPS_AND_USERS, p.getEntity(), "Unexpected entity: ");
        assertEquals(OCLPermission.READ_FOLDER, p.getFolderPermission(), "Unexpected folder permission: ");
        assertEquals(OCLPermission.NO_PERMISSIONS, p.getReadPermission(), "Unexpected read permission: ");
        assertEquals(OCLPermission.NO_PERMISSIONS, p.getWritePermission(), "Unexpected write permission: ");
        assertEquals(OCLPermission.NO_PERMISSIONS, p.getDeletePermission(), "Unexpected delete permission: ");
        assertFalse(p.isFolderAdmin(), "Unexpected folder admin flag: ");
        assertTrue(p.isGroupPermission(), "Unexpected group flag: ");
    }

}
