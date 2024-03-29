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

package com.openexchange.ajax.drive.test;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.drive.action.DeleteLinkRequest;
import com.openexchange.ajax.drive.action.GetLinkRequest;
import com.openexchange.ajax.drive.action.GetLinkResponse;
import com.openexchange.ajax.drive.action.UpdateLinkRequest;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.framework.UserValues;
import com.openexchange.ajax.infostore.actions.GetInfostoreRequest;
import com.openexchange.ajax.infostore.actions.InfostoreTestManager;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.drive.DriveShareTarget;
import com.openexchange.drive.impl.DriveConstants;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.modules.Module;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.TestInit;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link UpdateLinkTest}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.8.0
 */
public class UpdateLinkTest extends AbstractDriveShareTest {

    @SuppressWarnings("hiding")
    private InfostoreTestManager itm;
    private FolderObject rootFolder;
    private FolderObject folder;
    private DefaultFile file;
    private FolderObject folder2;

    /**
     * Initializes a new {@link UpdateLinkTest}.
     *
     * @param name
     */
    public UpdateLinkTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        itm = new InfostoreTestManager(getClient());

        UserValues values = getClient().getValues();
        rootFolder = insertPrivateFolder(EnumAPI.OX_NEW, Module.INFOSTORE.getFolderConstant(), values.getPrivateInfostoreFolder());
        folder = insertPrivateFolder(EnumAPI.OX_NEW, Module.INFOSTORE.getFolderConstant(), rootFolder.getObjectID());
        folder2 = insertPrivateFolder(EnumAPI.OX_NEW, Module.INFOSTORE.getFolderConstant(), rootFolder.getObjectID());

        long now = System.currentTimeMillis();
        file = new DefaultFile();
        file.setFolderId(String.valueOf(folder2.getObjectID()));
        file.setTitle("GetLinkTest_" + now);
        file.setFileName(file.getTitle());
        file.setDescription(file.getTitle());
        file.setFileMD5Sum(getChecksum(new File(TestInit.getTestProperty("ajaxPropertiesFile"))));
        itm.newAction(file, new File(TestInit.getTestProperty("ajaxPropertiesFile")));
    }

    @Test
    public void testUpdateFileLink() throws Exception {
        // Create Link
        DriveShareTarget target = new DriveShareTarget();
        target.setDrivePath("/" + folder2.getFolderName());
        target.setName(file.getFileName());
        target.setChecksum(file.getFileMD5Sum());
        GetLinkRequest getLinkRequest = new GetLinkRequest(I(rootFolder.getObjectID()), target);
        GetLinkResponse getLinkResponse = getClient().execute(getLinkRequest);
        String url = getLinkResponse.getUrl();

        // Check Link
        GuestClient guestClient = resolveShare(url, null, null);
        OCLGuestPermission expectedPermission = createAnonymousGuestPermission();
        expectedPermission.setEntity(guestClient.getValues().getUserId());
        guestClient.checkShareAccessible(expectedPermission);

        // Update Link
        Map<String, Object> newMeta = Collections.<String, Object> singletonMap("test", Boolean.TRUE);
        String newPassword = UUIDs.getUnformattedString(UUID.randomUUID());
        Date newExpiry = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        UpdateLinkRequest updateLinkRequest = new UpdateLinkRequest(I(rootFolder.getObjectID()), target, L(newExpiry.getTime()), newPassword, newMeta, true);
        getClient().execute(updateLinkRequest);

        // Check updated Link
        GuestClient newClient = resolveShare(url, null, newPassword);
        newClient.checkFileAccessible(file.getId(), expectedPermission);
        int guestID = guestClient.getValues().getUserId();

        ExtendedPermissionEntity guestEntity = discoverGuestEntity(file.getId(), guestID);
        assertNotNull(guestEntity);
        assertEquals(newExpiry, guestEntity.getExpiry());

        // Delete Link
        getClient().execute(new DeleteLinkRequest(I(rootFolder.getObjectID()), target));
        guestEntity = discoverGuestEntity(file.getId(), guestID);
        assertNull(guestEntity, "Share was not deleted");
        List<FileStorageObjectPermission> objectPermissions = getClient().execute(new GetInfostoreRequest(file.getId())).getDocumentMetadata().getObjectPermissions();
        assertTrue(objectPermissions.isEmpty(), "Permission was not deleted");
    }

    @Test
    public void testUpdateFolderLink() throws Exception {
        // Create Link
        DriveShareTarget target = new DriveShareTarget();
        target.setDrivePath("/" + folder.getFolderName());
        target.setChecksum(DriveConstants.EMPTY_MD5);
        GetLinkRequest getLinkRequest = new GetLinkRequest(I(rootFolder.getObjectID()), target);
        GetLinkResponse getLinkResponse = getClient().execute(getLinkRequest);
        String url = getLinkResponse.getUrl();

        // Check Link
        GuestClient guestClient = resolveShare(url, null, null);
        OCLGuestPermission expectedPermission = createAnonymousGuestPermission();
        expectedPermission.setEntity(guestClient.getValues().getUserId());
        guestClient.checkShareAccessible(expectedPermission);

        // Update Link
        Map<String, Object> newMeta = Collections.<String, Object> singletonMap("test", Boolean.TRUE);
        String newPassword = UUIDs.getUnformattedString(UUID.randomUUID());
        Date newExpiry = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        UpdateLinkRequest updateLinkRequest = new UpdateLinkRequest(I(rootFolder.getObjectID()), target, L(newExpiry.getTime()), newPassword, newMeta, true);
        getClient().execute(updateLinkRequest);

        // Check updated Link
        GuestClient newClient = resolveShare(url, null, newPassword);
        newClient.checkFolderAccessible(Integer.toString(folder.getObjectID()), expectedPermission);
        String folder = guestClient.getFolder();
        String item = guestClient.getItem();
        int guestID = guestClient.getValues().getUserId();

        ExtendedPermissionEntity guestEntity;
        if (target.isFolder()) {
            guestEntity = discoverGuestEntity(EnumAPI.OX_NEW, FolderObject.INFOSTORE, Integer.parseInt(folder), guestID);
        } else {
            guestEntity = discoverGuestEntity(item, guestID);
        }
        assertNotNull(guestEntity);
        assertEquals(newExpiry, guestEntity.getExpiry());

        // Delete Link
        getClient().execute(new DeleteLinkRequest(I(rootFolder.getObjectID()), target));
        if (target.isFolder()) {
            guestEntity = discoverGuestEntity(EnumAPI.OX_NEW, FolderObject.INFOSTORE, Integer.parseInt(folder), guestID);
        } else {
            guestEntity = discoverGuestEntity(item, guestID);
        }
        assertNull(guestEntity, "Share was not deleted");
        List<FileStorageObjectPermission> objectPermissions = getClient().execute(new GetInfostoreRequest(file.getId())).getDocumentMetadata().getObjectPermissions();
        assertTrue(objectPermissions.isEmpty(), "Permission was not deleted");
    }

}
