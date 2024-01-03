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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.infostore.actions.GetInfostoreRequest;
import com.openexchange.ajax.infostore.actions.GetInfostoreResponse;
import com.openexchange.ajax.share.Abstract2UserShareTest;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.ajax.share.actions.FileShare;
import com.openexchange.ajax.share.actions.FolderShare;
import com.openexchange.ajax.user.actions.GetRequest;
import com.openexchange.file.storage.DefaultFileStorageObjectPermission;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageGuestObjectPermission;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.util.TimeZones;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link Bug40627Test}
 *
 * Sharing exposes internal and other guests mail addresses to guests
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug40627Test extends Abstract2UserShareTest {

    @Test
    public void testCheckExtendedFolderPermissionAsAnonymousGuest() throws Exception {
        testCheckExtendedFolderPermissions(createAnonymousGuestPermission());
    }

    @Test
    public void testCheckExtendedFolderPermissionAsInvitedGuest() throws Exception {
        testCheckExtendedFolderPermissions(createNamedGuestPermission());
    }

    @Test
    public void testCheckExtendedObjectPermissionAsAnonymousGuest() throws Exception {
        testCheckExtendedObjectPermissions(createAnonymousGuestPermission());
    }

    @Test
    public void testCheckExtendedObjectPermissionAsInvitedGuest() throws Exception {
        testCheckExtendedObjectPermissions(createNamedGuestPermission());
    }

    private void testCheckExtendedFolderPermissions(OCLGuestPermission guestPermission) throws Exception {
        /*
         * create shared folder
         */
        int module = randomModule();
        List<OCLPermission> permissions = new ArrayList<OCLPermission>();
        permissions.add(guestPermission);
        OCLPermission groupPermission = new OCLPermission(GroupStorage.GROUP_ZERO_IDENTIFIER, 0);
        groupPermission.setAllPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
        groupPermission.setGroupPermission(true);
        permissions.add(groupPermission);
        int userId2 = testUser2.getUserId();
        OCLPermission userPermission = new OCLPermission(userId2, 0);
        userPermission.setAllPermission(OCLPermission.READ_ALL_OBJECTS, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
        permissions.add(userPermission);
        FolderObject folder = insertSharedFolder(EnumAPI.OX_NEW, module, getDefaultFolder(module), randomUID(), permissions.toArray(new OCLPermission[permissions.size()]));
        /*
         * check permissions
         */
        OCLPermission matchingPermission = null;
        for (OCLPermission permission : folder.getPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId() && false == permission.isGroupPermission() && permission.getEntity() != userId2) {
                matchingPermission = permission;
                break;
            }
        }
        assertNotNull(matchingPermission, "No matching permission in created folder found");
        checkPermissions(guestPermission, matchingPermission);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(EnumAPI.OX_NEW, module, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(discoverShare(guest, guestPermission), guestPermission.getRecipient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);
        /*
         * check extended permissions as guest user
         */
        GetResponse getResponse = guestClient.execute(new com.openexchange.ajax.folder.actions.GetRequest(EnumAPI.OX_NEW, folder.getObjectID()));
        assertFalse(getResponse.hasError(), getResponse.getErrorMessage());
        FolderShare folderShare = FolderShare.parse((JSONObject) getResponse.getData(), guestClient.getValues().getTimeZone());
        /*
         * expect to see other permission entities based on recipient type
         */
        int[] visibleUserIDs;
        if (RecipientType.ANONYMOUS.equals(guestPermission.getRecipient().getType())) {
            visibleUserIDs = new int[] { getClient().getValues().getUserId() };
        } else {
            visibleUserIDs = new int[] { getClient().getValues().getUserId(), userId2 };
        }
        checkExtendedPermissions(getClient(), folderShare.getExtendedPermissions(), guest.getEntity(), visibleUserIDs);
    }

    /**
     * Discovers the share url for the given guest and checks that one exists
     *
     * @param guest The guest
     * @return The share url
     * @throws Exception
     */
    private String discoverShare(ExtendedPermissionEntity guest, OCLGuestPermission guestPermission) throws Exception {
        String shareURL = discoverShareURL(guestPermission.getApiClient(), guest);
        assertNotNull(shareURL, "Share mail not found");
        return shareURL;
    }

    private void testCheckExtendedObjectPermissions(OCLGuestPermission oclGuestPermission) throws Exception {
        /*
         * create folder and a shared file inside
         */

        FileStorageGuestObjectPermission guestPermission = asObjectPermission(oclGuestPermission);
        List<FileStorageObjectPermission> permissions = new ArrayList<FileStorageObjectPermission>();
        permissions.add(guestPermission);
        permissions.add(new DefaultFileStorageObjectPermission(GroupStorage.GROUP_ZERO_IDENTIFIER, true, FileStorageObjectPermission.READ));
        int userId2 = testUser2.getUserId();
        permissions.add(new DefaultFileStorageObjectPermission(userId2, false, FileStorageObjectPermission.WRITE));
        byte[] contents = new byte[64 + random.nextInt(256)];
        random.nextBytes(contents);
        String filename = randomUID();
        FolderObject folder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getDefaultFolder(FolderObject.INFOSTORE));
        File file = insertSharedFile(folder.getObjectID(), filename, permissions, contents);
        /*
         * check permissions
         */
        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission permission : file.getObjectPermissions()) {
            if (permission.getEntity() != getClient().getValues().getUserId() && false == permission.isGroup() && permission.getEntity() != userId2) {
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
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(discoverShare(guest, oclGuestPermission), guestPermission.getRecipient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission, contents);
        /*
         * check extended permissions as guest user
         */
        GetInfostoreRequest getInfostoreRequest = new GetInfostoreRequest(guestClient.getItem());
        getInfostoreRequest.setFailOnError(true);
        GetInfostoreResponse getInfostoreResponse = guestClient.execute(getInfostoreRequest);
        assertFalse(getInfostoreResponse.hasError(), getInfostoreResponse.getErrorMessage());
        FileShare fileShare = FileShare.parse((JSONObject) getInfostoreResponse.getData(), guestClient.getValues().getTimeZone());
        /*
         * expect to see other permission entities based on recipient type
         */
        int[] visibleUserIDs;
        if (RecipientType.ANONYMOUS.equals(guestPermission.getRecipient().getType())) {
            visibleUserIDs = new int[] { getClient().getValues().getUserId() };
        } else {
            visibleUserIDs = new int[] { getClient().getValues().getUserId(), userId2 };
        }
        checkExtendedPermissions(getClient(), fileShare.getExtendedPermissions(), guest.getEntity(), visibleUserIDs);
    }

    private static void checkExtendedPermissions(AJAXClient sharingClient, List<ExtendedPermissionEntity> actual, int guestID, int[] visibleUserIDs) throws Exception {
        assertNotNull(actual);
        for (ExtendedPermissionEntity permissionEntity : actual) {
            switch (permissionEntity.getType()) {
                case GROUP:
                    assertEquals("Group " + permissionEntity.getEntity(), permissionEntity.getDisplayName());
                    break;
                case GUEST:
                    assertEquals(permissionEntity.getEntity(), guestID);
                    break;
                case USER:
                    if (Arrays.contains(visibleUserIDs, permissionEntity.getEntity())) {
                        GetRequest getRequest = new com.openexchange.ajax.user.actions.GetRequest(permissionEntity.getEntity(), TimeZones.UTC);
                        com.openexchange.ajax.user.actions.GetResponse getResponse = sharingClient.execute(getRequest);
                        com.openexchange.user.User expectedUser = getResponse.getUser();
                        assertEquals(expectedUser.getDisplayName(), permissionEntity.getDisplayName());
                        assertNotNull(permissionEntity.getContact());
                        assertEquals(expectedUser.getSurname(), permissionEntity.getContact().getSurName());
                        assertEquals(expectedUser.getGivenName(), permissionEntity.getContact().getGivenName());
                        assertFalse(permissionEntity.getContact().containsEmail1()); // Shouldn't contain the email1 field, because a guest doesn't has the webmail permission
                    } else {
                        assertEquals("User " + permissionEntity.getEntity(), permissionEntity.getDisplayName());
                        assertNotNull(permissionEntity.getContact());
                        assertEquals("User", permissionEntity.getContact().getSurName());
                        assertEquals(String.valueOf(permissionEntity.getEntity()), permissionEntity.getContact().getGivenName());
                    }
                    break;
                default:
                    break;
            }
        }
    }

}
