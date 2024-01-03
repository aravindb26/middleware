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

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.OCLGuestPermission;
import com.openexchange.ajax.share.GuestClient;
import com.openexchange.ajax.share.ShareTest;
import com.openexchange.ajax.share.actions.ExtendedPermissionEntity;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.DefaultFileStorageObjectPermission;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageGuestObjectPermission;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.share.recipient.AnonymousRecipient;
import org.junit.jupiter.api.TestInfo;

/**
 * Anonymous guests are created for creating links to folders/items. At most one link must exist
 * per folder or item. This implies that once created anonymous guests must not be re-used and added
 * as permission entities to files or items others than their initial one. I.e. there must be one-to-one
 * relationships between folder/item, anonymous guests and folder/object permissions.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class AnonymousGuestTest extends ShareTest {

    private FolderObject folder;

    private File file;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getClient().getValues().getPrivateInfostoreFolder());
        remember(folder);
        file = insertFile(folder.getObjectID());
        remember(file);
    }

    @Test
    public void testAddAnonymousGuestToFolder() throws Exception {
        OCLGuestPermission guestPermission = createAnonymousGuestPermission();
        FolderObject updated = addPermissions(folder, guestPermission);
        OCLPermission matchingPermission = findAndCheckPermission(updated);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(EnumAPI.OX_NEW, FolderObject.INFOSTORE, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(guest, guestPermission.getRecipient(), guestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);
    }

    @Test
    public void testAddAnonymousGuestWithInvalidPermissionToFolder() throws Exception {
        /*
         * Permission bits are ignored for anonymous guests, therefore the update should succeed
         * and the created permission will be read-only
         */
        OCLGuestPermission guestPermission = createInvalidAnonymousGuestPermission();

        FolderObject updated = addPermissions(folder, guestPermission);
        OCLPermission matchingPermission = findAndCheckPermission(updated);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(EnumAPI.OX_NEW, FolderObject.INFOSTORE, folder.getObjectID(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(guest, guestPermission.getRecipient(), guestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);
    }

    @Test
    public void testAddTwoAnonymousGuestsToFolder() throws Exception {
        /*
         * Only one anonymous permission is allowed, so the update must fail.
         */
        OCLGuestPermission guestPermission1 = createAnonymousGuestPermission();
        OCLGuestPermission guestPermission2 = createAnonymousGuestPermission();

        boolean thrown = false;
        try {
            addPermissions(folder, guestPermission1, guestPermission2);
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testAddTwoAnonymousGuestsSubsequentlyToFolder() throws Exception {
        /*
         * Only one anonymous permission is allowed, so the second update must fail.
         */
        OCLGuestPermission guestPermission1 = createAnonymousGuestPermission();
        FolderObject updated = addPermissions(folder, guestPermission1);
        OCLGuestPermission guestPermission2 = createAnonymousGuestPermission();
        boolean thrown = false;
        try {
            addPermissions(updated, guestPermission2);
        } catch (AssertionError e) {
            Assertions.assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testAddExistingAnonymousGuestToNewFolder() throws Exception {
        OCLGuestPermission guestPermission = createAnonymousGuestPermission();
        FolderObject updated = addPermissions(folder, guestPermission);
        OCLPermission entityPermission = findAndCheckPermission(updated);

        FolderObject newFolder = insertPrivateFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getClient().getValues().getPrivateInfostoreFolder());
        remember(newFolder);

        boolean thrown = false;
        try {
            addPermissions(newFolder, entityPermission);
        } catch (AssertionError e) {
            Assertions.assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testUpdateAnonymousGuestPermissionToWritableOnFolder() throws Exception {
        OCLGuestPermission guestPermission = createAnonymousGuestPermission();
        FolderObject updated = addPermissions(folder, guestPermission);
        OCLPermission entityPermission = findAndCheckPermission(updated);
        /*
         * Change to writable
         */
        entityPermission.setAllPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
        boolean thrown = false;
        try {
            updateFolder(EnumAPI.OX_NEW, updated);
        } catch (AssertionError e) {
            Assertions.assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testAddAnonymousGuestToFile() throws Exception {
        OCLGuestPermission oclGuestPermission = createAnonymousGuestPermission();
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(oclGuestPermission);

        File updated = addPermissions(file, guestPermission);
        FileStorageObjectPermission matchingPermission = findAndCheckPermission(updated);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(file.getId(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(guest, guestPermission.getRecipient(), oclGuestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);
    }

    @Test
    public void testAddAnonymousGuestWithInvalidPermissionToFile() throws Exception {
        /*
         * Permission bits are ignored for anonymous guests, therefore the update should succeed
         * and the created permission will be read-only
         */
        OCLGuestPermission oclGuestPermission = createInvalidAnonymousGuestPermission();
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(oclGuestPermission);

        File updated = addPermissions(file, guestPermission);
        FileStorageObjectPermission matchingPermission = findAndCheckPermission(updated);
        /*
         * discover & check guest
         */
        ExtendedPermissionEntity guest = discoverGuestEntity(file.getId(), matchingPermission.getEntity());
        checkGuestPermission(guestPermission, guest);
        /*
         * check access to share
         */
        GuestClient guestClient = resolveShare(guest, guestPermission.getRecipient(), oclGuestPermission.getApiClient());
        guestClient.checkShareModuleAvailable();
        guestClient.checkShareAccessible(guestPermission);
    }

    @Test
    public void testAddTwoAnonymousGuestsToFile() throws Exception {
        /*
         * Only one anonymous permission is allowed, so the update must fail.
         */
        FileStorageGuestObjectPermission guestPermission1 = asObjectPermission(createAnonymousGuestPermission());
        FileStorageGuestObjectPermission guestPermission2 = asObjectPermission(createAnonymousGuestPermission());

        boolean thrown = false;
        try {
            addPermissions(file, guestPermission1, guestPermission2);
        } catch (AssertionError e) {
            assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testAddTwoAnonymousGuestsSubsequentlyToFile() throws Exception {
        /*
         * Only one anonymous permission is allowed, so the second update must fail.
         */
        FileStorageGuestObjectPermission guestPermission1 = asObjectPermission(createAnonymousGuestPermission());
        File updated = addPermissions(file, guestPermission1);
        FileStorageGuestObjectPermission guestPermission2 = asObjectPermission(createAnonymousGuestPermission());
        boolean thrown = false;
        try {
            addPermissions(updated, guestPermission2);
        } catch (AssertionError e) {
            Assertions.assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testAddExistingAnonymousGuestToNewFile() throws Exception {
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(createAnonymousGuestPermission());
        File updated = addPermissions(file, guestPermission);
        FileStorageObjectPermission entityPermission = findAndCheckPermission(updated);

        File newFile = insertFile(getClient().getValues().getPrivateInfostoreFolder());
        remember(newFile);

        boolean thrown = false;
        try {
            addPermissions(newFile, entityPermission);
        } catch (AssertionError e) {
            Assertions.assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    @Test
    public void testUpdateAnonymousGuestPermissionToWritableOnFile() throws Exception {
        FileStorageGuestObjectPermission guestPermission = asObjectPermission(createAnonymousGuestPermission());
        File updated = addPermissions(file, guestPermission);
        FileStorageObjectPermission entityPermission = findAndCheckPermission(updated);
        /*
         * Change to writable
         */
        List<FileStorageObjectPermission> newPermissions = new ArrayList<>(2);
        newPermissions.addAll(updated.getObjectPermissions());
        newPermissions.remove(entityPermission);
        newPermissions.add(new DefaultFileStorageObjectPermission(entityPermission.getEntity(), false, FileStorageObjectPermission.DELETE));

        DefaultFile toUpdate = new DefaultFile();
        toUpdate.setId(file.getId());
        toUpdate.setFolderId(file.getFolderId());
        toUpdate.setLastModified(file.getLastModified());
        toUpdate.setObjectPermissions(newPermissions);

        boolean thrown = false;
        try {
            updateFile(toUpdate, new Field[] { Field.OBJECT_PERMISSIONS });
        } catch (AssertionError e) {
            Assertions.assertTrue(e.getMessage().contains("PERMISSION_DENIED"));
            thrown = true;
        }
        assertTrue(thrown);
    }

    protected static OCLGuestPermission createInvalidAnonymousGuestPermission(String password) {
        OCLGuestPermission guestPermission = createAnonymousPermission(password);
        guestPermission.setAllPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER, OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    protected static OCLGuestPermission createInvalidAnonymousGuestPermission() {
        return createAnonymousGuestPermission(null);
    }

    protected static OCLGuestPermission createInvalidAnonymousPermission(String password) {
        AnonymousRecipient recipient = new AnonymousRecipient();
        recipient.setPassword(password);
        OCLGuestPermission guestPermission = new OCLGuestPermission(recipient);
        AnonymousRecipient anonymousRecipient = new AnonymousRecipient();
        anonymousRecipient.setPassword(password);
        guestPermission.setRecipient(anonymousRecipient);
        guestPermission.setGroupPermission(false);
        guestPermission.setFolderAdmin(false);
        guestPermission.getRecipient().setBits(guestPermission.getPermissionBits());
        return guestPermission;
    }

    private FolderObject addPermissions(FolderObject folder, OCLPermission... permissions) throws Exception {
        FolderObject toUpdate = new FolderObject(folder.getObjectID());
        toUpdate.setLastModified(folder.getLastModified());
        toUpdate.setFolderName(folder.getFolderName());
        toUpdate.setPermissions(folder.getPermissions());
        for (OCLPermission p : permissions) {
            toUpdate.addPermission(p);
        }
        return updateFolder(EnumAPI.OX_NEW, toUpdate);
    }

    private File addPermissions(File file, FileStorageObjectPermission... permissions) throws Exception {
        DefaultFile metadata = new DefaultFile();
        metadata.setId(file.getId());
        metadata.setFolderId(file.getFolderId());
        metadata.setLastModified(file.getLastModified());
        List<FileStorageObjectPermission> newPermissions = new ArrayList<>(2);
        List<FileStorageObjectPermission> oldPermissions = file.getObjectPermissions();
        if (oldPermissions != null) {
            newPermissions.addAll(oldPermissions);
        }

        for (FileStorageObjectPermission p : permissions) {
            newPermissions.add(p);
        }
        metadata.setObjectPermissions(newPermissions);
        return updateFile(metadata, new Field[] { Field.OBJECT_PERMISSIONS });
    }

    private OCLPermission findAndCheckPermission(FolderObject folder) throws Exception {
        List<OCLPermission> permissions = folder.getPermissions();
        Assertions.assertEquals(2, permissions.size(), "Wrong number of permissions");

        OCLPermission matchingPermission = null;
        for (OCLPermission p : permissions) {
            if (p.getEntity() != getClient().getValues().getUserId()) {
                Assertions.assertTrue(p.isFolderVisible());
                Assertions.assertTrue(p.canReadOwnObjects());
                Assertions.assertTrue(p.canReadAllObjects());
                Assertions.assertFalse(p.isFolderAdmin());
                Assertions.assertFalse(p.isSystem());
                Assertions.assertFalse(p.isGroupPermission());
                Assertions.assertFalse(p.canWriteOwnObjects());
                Assertions.assertFalse(p.canWriteAllObjects());
                Assertions.assertFalse(p.canDeleteOwnObjects());
                Assertions.assertFalse(p.canDeleteAllObjects());
                matchingPermission = p;
            }
        }
        Assertions.assertNotNull(matchingPermission, "Missing expected guest permission");
        return matchingPermission;
    }

    private FileStorageObjectPermission findAndCheckPermission(File file) throws Exception {
        List<FileStorageObjectPermission> permissions = file.getObjectPermissions();
        Assertions.assertNotNull(permissions, "Missing object permissions");
        Assertions.assertEquals(1, permissions.size(), "Wrong number of permissions");

        FileStorageObjectPermission matchingPermission = null;
        for (FileStorageObjectPermission p : permissions) {
            if (p.getEntity() != getClient().getValues().getUserId()) {
                Assertions.assertTrue(p.canRead());
                Assertions.assertFalse(p.canWrite());
                Assertions.assertFalse(p.canDelete());
                matchingPermission = p;
            }
        }
        Assertions.assertNotNull(matchingPermission, "Missing expected guest permission");
        return matchingPermission;
    }

}
