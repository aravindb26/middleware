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
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.actions.GetRequest;
import com.openexchange.ajax.folder.actions.GetResponse;
import com.openexchange.ajax.folder.actions.InsertRequest;
import com.openexchange.ajax.folder.actions.InsertResponse;
import com.openexchange.ajax.folder.actions.ListRequest;
import com.openexchange.ajax.folder.actions.ListResponse;
import com.openexchange.ajax.folder.actions.UpdateRequest;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;

/**
 * {@link PermissionsCascadeTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class PermissionsCascadeTest extends AbstractAJAXSession {

    private final List<FolderObject> testFolders = new ArrayList<>();

    private FolderObject rootFolder;

    /**
     * Initializes a new {@link PermissionsCascadeTest}.
     *
     * @param name
     */
    public PermissionsCascadeTest() {
        super();
    }

    /**
     * Test simple permissions cascade.
     *
     * Creates a simple folder tree, assigns permissions and asserts
     */
    @Test
    public void testCascadePermissionsInChildrenFolders() throws Exception {
        // Create a simple folder tree
        rootFolder = createSimpleTree("testCascadePermissionsInChildrenFolders", 5);
        assertCascadePermissions();
    }

    /**
     * Test cascading in sibling folders
     */
    @Test
    public void testCasccadePermissionsInSiblingFolders() throws Exception {
        rootFolder = createRandomTree("testCasccadePermissionsInSiblingFolders", 20);
        assertCascadePermissions();
    }

    /**
     * Assert cascaded permissions in tree, starting by root node
     */
    private void assertCascadePermissions() throws Exception {
        // Fetch that folder
        GetResponse response = getClient().execute(new GetRequest(EnumAPI.OUTLOOK, rootFolder.getObjectID()));
        Date timestamp = response.getTimestamp();

        // User to share the folder with
        AJAXClient client2 = testUser2.getAjaxClient();

        // Apply permissions
        rootFolder.addPermission(Create.ocl(client2.getValues().getUserId(), false, false, OCLPermission.READ_FOLDER, OCLPermission.READ_OWN_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS));
        rootFolder.setLastModified(new Date(timestamp.getTime() + 3600));
        getClient().execute(new UpdateRequest(EnumAPI.OUTLOOK, rootFolder).setCascadePermissions(true));

        // Fetch all folders of the tree and make sure that the permissions are cascaded
        for (int i = 2; i < testFolders.size(); i++) {
            FolderObject fo = testFolders.get(i);
            response = getClient().execute(new GetRequest(EnumAPI.OUTLOOK, fo.getObjectID(), new int[] { 300, 306 }));
            List<OCLPermission> permissions = response.getFolder().getPermissions();
            boolean found = false;
            for (int p = 0; !found && p < permissions.size(); p++) {
                found = permissions.get(p).getEntity() == client2.getValues().getUserId();
            }
            assertTrue(found, "Second user not found in permissions of folder '" + response.getFolder().getFolderName() + "'");
        }
    }

    /**
     * Test roll-back functionality.
     *
     * Create a random tree and find a folder in the tree (say (A)) that has at least one sub-folder.
     * Select that sub-folder (A) and create a new folder (B) under that sub-folder (A) with a second client
     * Remove the permissions from the new folder (B) for client 1 and make client 2 the administrator
     * for that folder.
     *
     * Apply new permissions to folder (A) and assert that the permissions are not cascaded.
     * Then apply the new permissions again and ignore the warnings. Assert that the permissions are cascaded.
     */
    @Test
    public void testCascadePermissionsInTreeRollbackAndThenIgnore() throws Exception {
        rootFolder = createRandomTree("testCascadePermissionsInTreeRollback", 20);

        // Pick one folder that has at least one sub-folder
        int rootNodeIdOfSubTree = -1;
        for (int f = 1; f < testFolders.size(); f++) {
            GetResponse response = getClient().execute(new GetRequest(EnumAPI.OUTLOOK, testFolders.get(f).getObjectID(), new int[] { 304 }));
            if (response.getFolder().hasSubfolders()) {
                rootNodeIdOfSubTree = response.getFolder().getObjectID();
                break;
            }
        }
        // If none found, then use the root folder
        if (rootNodeIdOfSubTree < 0) {
            rootNodeIdOfSubTree = testFolders.get(0).getObjectID();
        }

        // Fetch all of its sub-folders.
        List<FolderObject> tree = new ArrayList<>();
        fetchAllSubfolders(rootNodeIdOfSubTree, tree);

        // Assert that there is a tree
        assertTrue(tree.size() > 0, "No tree");

        // Fetch information about the leaf folder
        FolderObject leaf = tree.get(tree.size() - 1);
        GetResponse response = getClient().execute(new GetRequest(EnumAPI.OUTLOOK, leaf.getObjectID()));
        Date timestamp = response.getTimestamp();
        leaf.setPermissions(response.getFolder().getPermissions());

        // User to share the folder with
        AJAXClient client2 = testUser2.getAjaxClient();

        // Make second user an admin
        leaf.addPermission(Create.ocl(client2.getValues().getUserId(), false, true, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION));
        leaf.setLastModified(timestamp);
        getClient().execute(new UpdateRequest(EnumAPI.OUTLOOK, leaf));

        // Create a folder under leaf
        int lol = createFolder("Leaf", leaf.getObjectID(), client2).getObjectID();

        // Fetch permissions of the newly created folder
        response = client2.execute(new GetRequest(EnumAPI.OUTLOOK, lol, new int[] { 5, 306 }));
        FolderObject leafOfLeaf = response.getFolder();
        timestamp = response.getTimestamp();

        // Apply administrative permissions to the leaf of leaf folder.
        leafOfLeaf.removePermissions();
        leafOfLeaf.addPermission(Create.ocl(client2.getValues().getUserId(), false, true, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION, OCLPermission.ADMIN_PERMISSION));
        leafOfLeaf.setLastModified(timestamp);
        client2.execute(new UpdateRequest(EnumAPI.OUTLOOK, leafOfLeaf));

        // Try to apply permissions to the rootNodeOfSubTree and cascade (should fail and roll-back)
        response = getClient().execute(new GetRequest(EnumAPI.OUTLOOK, rootNodeIdOfSubTree, new int[] { 5, 306 }));
        FolderObject rootNode = response.getFolder();
        timestamp = response.getTimestamp();

        AJAXClient client3 = testContext.acquireUser().getAjaxClient();
        rootNode.addPermission(Create.ocl(client3.getValues().getUserId(), false, false, OCLPermission.READ_FOLDER, OCLPermission.READ_OWN_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS));
        rootNode.setLastModified(timestamp);
        UpdateRequest setCascadePermissions = new UpdateRequest(EnumAPI.OUTLOOK, rootNode, false).setCascadePermissions(true);
        InsertResponse cascadeResponse = getClient().execute(setCascadePermissions);

        int owner = getClient().getValues().getUserId();
        int user2 = client2.getValues().getUserId();
        int user3 = client3.getValues().getUserId();

        // Assert permissions
        assertPermissions(rootNode, new int[] { owner }, new int[] { user2 }, getClient());
        assertPermissions(leaf, new int[] { owner, user2 }, new int[] { user3 }, getClient());
        assertPermissions(leafOfLeaf, new int[] { user2 }, new int[] { owner, user3 }, client2);

        // Ignore warnings and apply permissions to the rootNodeOfSubTree again (should succeed)
        setCascadePermissions.setIgnoreWarnings(true);
        rootNode.setLastModified(cascadeResponse.getTimestamp());
        cascadeResponse = getClient().execute(setCascadePermissions);
        assertFalse(cascadeResponse.hasError(), cascadeResponse.getErrorMessage());

        // Assert permissions
        assertPermissions(rootNode, new int[] { owner, user3 }, new int[] { user2 }, getClient());
        assertPermissions(leaf, new int[] { owner, user3 }, new int[] { user2 }, getClient());
        assertPermissions(leafOfLeaf, new int[] { user2 }, new int[] { owner, user3 }, client2);
    }

    /**
     * Assert permissions
     *
     * @param folderObject The folder objects
     * @param includedUsers The users that should be included in the permission bits
     * @param excludedUsers The users that should be excluded from the permission bits
     * @param client The client
     */
    private void assertPermissions(FolderObject folderObject, int[] includedUsers, int[] excludedUsers, AJAXClient client) throws Exception {
        final GetResponse getResponse = client.execute(new GetRequest(EnumAPI.OUTLOOK, folderObject.getObjectID()));
        final List<OCLPermission> permissions = getResponse.getFolder().getPermissions();

        final int pSize = permissions.size();
        assertEquals(includedUsers.length, pSize, "Unexpected number of permissions for folder '" + folderObject.getObjectID() + "': ");

        assertUserInPermissions(includedUsers, permissions, folderObject.getObjectID(), true);
        assertUserInPermissions(excludedUsers, permissions, folderObject.getObjectID(), false);
    }

    /**
     * Assert users in permissions
     *
     * @param userIds The user identifiers
     * @param permissions The permission bits
     * @param folderId The folder identifier
     * @param isContained true to assert if the users are included; false to assert if users are excluded
     */
    private void assertUserInPermissions(int[] userIds, List<OCLPermission> permissions, int folderId, boolean isContained) {
        boolean found = false;
        for (int userId : userIds) {
            for (int i = 0; !found && i < permissions.size(); i++) {
                found = permissions.get(i).getEntity() == userId;
            }
            assertTrue(isContained == found, "User " + ((!isContained) ? "not" : "") + " found in permissions for folder '" + folderId + "'");
        }
    }

    /**
     * Fetch all sub-folders of the specified folder
     *
     * @param folderId The folder identifier
     * @param tree The tree
     */
    private void fetchAllSubfolders(int folderId, List<FolderObject> tree) throws Exception {
        ListResponse listResponse = getClient().execute(new ListRequest(EnumAPI.OUTLOOK, Integer.toString(folderId), new int[] { 1, 304, 306 }, false));
        Iterator<FolderObject> iterator = listResponse.getFolder();
        while (iterator.hasNext()) {
            FolderObject fo = iterator.next();
            tree.add(fo);
            if (fo.hasSubfolders()) {
                fetchAllSubfolders(fo.getObjectID(), tree);
            }
        }
    }

    /**
     * Create a folder and add it to the delete list for later cleanup (Helper method)
     *
     * @param folderName The folder name
     * @param parent The parent
     * @return The folder identifier
     */
    private FolderObject createFolder(String folderName, int parent) throws Exception {
        return createFolder(folderName, parent, getClient());
    }

    /**
     * Create a folder and add it to the delete list for later cleanup
     *
     * @param folderName The folder name
     * @param parent The parent
     * @param client The client
     * @return The folder identifier
     */
    private FolderObject createFolder(String folderName, int parent, AJAXClient client) throws Exception {
        FolderObject folder = Create.createPrivateFolder(folderName, FolderObject.INFOSTORE, client.getValues().getUserId());
        folder.setParentFolderID(parent);
        InsertResponse response = client.execute(new InsertRequest(EnumAPI.OUTLOOK, folder));
        //String data = (String) response.getData();
        response.fillObject(folder);
        testFolders.add(folder);
        return folder;
    }

    /**
     * Creates a simple folder tree
     *
     * @param rootName The name of the root node
     */
    private FolderObject createSimpleTree(String rootName, int levels) throws Exception {
        FolderObject rootFolder = createFolder(rootName, getClient().getValues().getPrivateInfostoreFolder());
        int parent = rootFolder.getObjectID();
        for (int i = 1; i <= levels; i++) {
            parent = createFolder("Level " + i, parent).getObjectID();
        }
        return rootFolder;
    }

    /**
     * Creates a folder tree with folders placed randomly in the hierarchy.
     *
     * @param rootName The name of the root node
     */
    private FolderObject createRandomTree(String rootName, int folderCount) throws Exception {
        FolderObject rootFolder = createFolder(rootName, getClient().getValues().getPrivateInfostoreFolder());
        int parentId = rootFolder.getObjectID();
        for (int i = 0; i < folderCount; i++) {
            int r = (int) (Math.random() * (testFolders.size() - 1));
            parentId = testFolders.get(r).getObjectID();
            createFolder(UUID.randomUUID().toString(), parentId);
        }
        return rootFolder;
    }
}
