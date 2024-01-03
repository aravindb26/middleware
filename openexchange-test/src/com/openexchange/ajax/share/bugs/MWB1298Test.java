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

import static com.openexchange.groupware.container.FolderObject.SYSTEM_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.container.FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.container.FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID;
import static com.openexchange.java.Autoboxing.B;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.folder.actions.ListRequest;
import com.openexchange.ajax.folder.actions.ListResponse;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.folder.actions.UpdateRequest;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.ajax.share.actions.FolderShare;
import com.openexchange.file.storage.DefaultFileStorageObjectPermission;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageGuestObjectPermission;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.modules.FoldersApi;

/**
 * {@link MWB1298Test}
 *
 * arrow symbol disappears after unfolding in File Share Guest UI
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class MWB1298Test extends ShareTest {

    @Test
    public void testPublicFiles() throws Exception {
        /*
         * prepare named guest permission
         */
        OCLGuestPermission guestPermission = createNamedGuestPermission();
        /*
         * create public folder & add permission for guest
         */
        FolderObject folder = insertPublicFolder(getClient(), EnumAPI.OX_NEW, FolderObject.INFOSTORE);
        folder.addPermission(guestPermission);
        folder = updateFolder(EnumAPI.OX_NEW, folder);
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
        ExtendedPermissionEntity guest = null;
        GetResponse getResponse = getClient().execute(new com.openexchange.ajax.folder.actions.GetRequest(EnumAPI.OX_NEW, folder.getObjectID()));
        assertFalse(getResponse.hasError(), getResponse.getErrorMessage());
        FolderShare folderShare = FolderShare.parse((JSONObject) getResponse.getData(), getClient().getValues().getTimeZone());
        for (ExtendedPermissionEntity permissionEntity : folderShare.getExtendedPermissions()) {
            if (permissionEntity.getEntity() == matchingPermission.getEntity()) {
                guest = permissionEntity;
                break;
            }
        }
        assertNotNull(guest);
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(guest, guestPermission.getRecipient(), guestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);
        /*
         * check shared & public files folders
         */
        checkSharedFilesVisible(guestClient, false);
        checkPublicFilesVisible(guestClient, true);
        checkPublicFilesHasSubfolders(guestClient, true);
        /*
         * share an additional single file to guest
         */
        insertSharedFile(getDefaultFolder(FolderObject.INFOSTORE), asObjectPermission(guestPermission));
        /*
         * re-check shared & public files folders
         */
        checkSharedFilesVisible(guestClient, true);
        checkSharedFilesHasSubfolders(guestClient, false);
        checkPublicFilesVisible(guestClient, true);
        checkPublicFilesHasSubfolders(guestClient, true);
        /*
         * revoke folder share
         */
        List<OCLPermission> permissionUpdate = new ArrayList<OCLPermission>();
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != guest.getEntity()) {
                permissionUpdate.add(permission);
            }
        }
        FolderObject folderUpdate = new FolderObject();
        folderUpdate.setObjectID(folder.getObjectID());
        folderUpdate.setLastModified(folder.getLastModified());
        folderUpdate.setPermissions(permissionUpdate);
        UpdateRequest updateRequest = new UpdateRequest(EnumAPI.OX_NEW, folderUpdate);
        InsertResponse updateResponse = getClient().execute(updateRequest);
        assertFalse(updateResponse.hasError(), "Errors in response");
        /*
         * re-check shared & public files folders
         */
        checkSharedFilesVisible(guestClient, true);
        checkSharedFilesHasSubfolders(guestClient, false);
        checkPublicFilesVisible(guestClient, false);
    }

    @Test
    public void testSharedFiles() throws Exception {
        /*
         * prepare named guest permission
         */
        OCLGuestPermission oclGuestPermission = createNamedGuestPermission();
        int defaultFolderId = getDefaultFolder(FolderObject.INFOSTORE);
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(oclGuestPermission);
        /*
         * share a single file to guest user & lookup matching permission
         */
        File file = insertSharedFile(defaultFolderId, guestPermission);
        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission permission : file.getObjectPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created file found");
        checkPermissions(guestPermission, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(file.getId(), matchingPermission.getEntity());
        assertNotNull(guest, "No guest permission entitiy");
        checkPermissions(guestPermission, guest.toObjectPermission());
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(guest, guestPermission.getRecipient(), oclGuestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);
        /*
         * check shared & public files folders
         */
        checkSharedFilesVisible(guestClient, true);
        checkSharedFilesHasSubfolders(guestClient, false);
        checkPublicFilesVisible(guestClient, false);
        /*
         * share an additional folder to the guest user
         */
        FolderObject folder = insertSharedFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, defaultFolderId, oclGuestPermission);
        /*
         * re-check shared & public files folders
         */
        checkSharedFilesVisible(guestClient, true);
        checkSharedFilesHasSubfolders(guestClient, true);
        checkPublicFilesVisible(guestClient, false);
        /*
         * revoke folder share
         */
        List<OCLPermission> permissionUpdate = new ArrayList<OCLPermission>();
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != guest.getEntity()) {
                permissionUpdate.add(permission);
            }
        }
        FolderObject folderUpdate = new FolderObject();
        folderUpdate.setObjectID(folder.getObjectID());
        folderUpdate.setLastModified(folder.getLastModified());
        folderUpdate.setPermissions(permissionUpdate);
        UpdateRequest updateRequest = new UpdateRequest(EnumAPI.OX_NEW, folderUpdate);
        InsertResponse updateResponse = getClient().execute(updateRequest);
        assertFalse(updateResponse.hasError(), "Errors in response");
        /*
         * re-check shared & public files folders
         */
        checkSharedFilesVisible(guestClient, true);
        checkSharedFilesHasSubfolders(guestClient, false);
        checkPublicFilesVisible(guestClient, false);
    }

    @Test
    public void testShareInternal() throws Exception {
        // Create file and share to second user in same context
        int defaultFolderId = getDefaultFolder(FolderObject.INFOSTORE);
        DefaultFileStorageObjectPermission permission = new DefaultFileStorageObjectPermission(testUser2.getUserId(), false, FileStorageObjectPermission.READ);
        File file = insertSharedFile(defaultFolderId, permission);
        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission perm : file.getObjectPermissions()) {
            if (perm.getEntity() != getClient().getValues().getUserId()) {
                matchingPermission = perm;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created file found");
        checkPermissions(permission, matchingPermission);

        // Request shared files as second user
        ApiClient apiClient2 = testUser2.getApiClient();
        FoldersApi foldersApi2 = new FoldersApi(apiClient2);
        FolderResponse response = foldersApi2.getFolder(String.valueOf(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID), null, null, null, Boolean.TRUE);
        FolderData folder = response.getData();
        assertNotNull(folder);
        assertFalse(folder.getSubfolders().booleanValue(), "Folder has subfolder-flag set");
        assertFalse(folder.getSubscrSubflds().booleanValue(), "Folder has subscribed-subfolders-flag set");
    }

    private static void checkSharedFilesHasSubfolders(GuestClient guestClient, boolean expectSubfolders) throws Exception {
        checkHasSubfolders(guestClient, SYSTEM_USER_INFOSTORE_FOLDER_ID, expectSubfolders);
    }

    private static void checkPublicFilesHasSubfolders(GuestClient guestClient, boolean expectSubfolders) throws Exception {
        checkHasSubfolders(guestClient, SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID, expectSubfolders);
    }

    private static void checkHasSubfolders(GuestClient guestClient, int folderId, boolean expectSubfolders) throws Exception {
        GetResponse getResponse = guestClient.execute(new GetRequest(EnumAPI.OX_NEW, folderId, false));
        assertFalse(getResponse.hasError(), "Errors in response");
        FolderObject folder = getResponse.getFolder();
        assertEquals(B(expectSubfolders), B(folder.hasSubfolders()));
        ListRequest listRequest = new ListRequest(EnumAPI.OX_NEW, folderId);
        listRequest.setAltNames(true);
        ListResponse listResponse = guestClient.execute(listRequest);
        assertFalse(listResponse.hasError(), "Errors in response");
        Object[][] listResponseArray = listResponse.getArray();
        assertEquals(B(expectSubfolders), B(null != listResponseArray && 0 < listResponseArray.length));
    }

    private static void checkSharedFilesVisible(GuestClient guestClient, boolean expectVisible) throws Exception {
        checkVisibleBelowRoot(guestClient, SYSTEM_USER_INFOSTORE_FOLDER_ID, expectVisible);
    }

    private static void checkPublicFilesVisible(GuestClient guestClient, boolean expectVisible) throws Exception {
        checkVisibleBelowRoot(guestClient, SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID, expectVisible);
    }

    private static void checkVisibleBelowRoot(GuestClient guestClient, int folderId, boolean expectVisible) throws Exception {
        GetResponse getResponse = guestClient.execute(new GetRequest(EnumAPI.OX_NEW, SYSTEM_INFOSTORE_FOLDER_ID, false));
        assertFalse(getResponse.hasError(), "Errors in response");
        FolderObject infostoreRootFolder = getResponse.getFolder();
        if (expectVisible) {
            assertTrue(infostoreRootFolder.hasSubfolders());
        }
        ListRequest listRequest = new ListRequest(EnumAPI.OX_NEW, SYSTEM_INFOSTORE_FOLDER_ID);
        listRequest.setAltNames(true);
        ListResponse listResponse = guestClient.execute(listRequest);
        assertFalse(listResponse.hasError(), "Errors in response");
        boolean found = false;
        Object[][] listResponseArray = listResponse.getArray();
        if (null != listResponseArray && 0 < listResponseArray.length) {
            for (Object[] object : listResponseArray) {
                if (String.valueOf(folderId).equals(String.valueOf(object[0]))) {
                    found = true;
                    break;
                }
            }
        }
        assertEquals(B(expectVisible), B(found));
    }

}
