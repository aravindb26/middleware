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

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.openexchange.ajax.config.actions.Tree;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.groupware.modules.Module;
import org.junit.jupiter.api.Assertions;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.ConfigResponse;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.modules.ConfigApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractFolderMovePermissionsTest}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.4
 */
public abstract class AbstractFolderMovePermissionsTest extends AbstractConfigAwareAPIClientSession {

    protected final String type;
    protected final String TREE = "0";
    protected final Integer BITS_ADMIN = Integer.valueOf(403710016);
    protected final Integer BITS_AUTHOR = Integer.valueOf(4227332);
    protected final Integer BITS_REVIEWER = Integer.valueOf(33025);
    protected final Integer BITS_VIEWER = Integer.valueOf(257);
    protected final Integer BITS_OWNER = Integer.valueOf(272662788);
    private final List<String> createdFolders;

    protected FoldersApi api;
    protected FoldersApi api2;
    protected Integer userId1;
    protected Integer userId2;
    protected String privateFolderId;
    protected String publicFolderId;
    protected String sharedFolderId;
    private SessionAwareClient apiClient2;

    protected AbstractFolderMovePermissionsTest(String type) {
        super();
        this.type = type;
        createdFolders = new ArrayList<String>();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
        apiClient2 = testUser2.getApiClient();
        userId1 = I(testUser.getUserId());
        userId2 = I(testUser2.getUserId());
        api = new FoldersApi(getApiClient());
        api2 = new FoldersApi(apiClient2);
        switch (type) {
            case "keep":
            case "inherit":
                privateFolderId = createNewFolder(true, BITS_REVIEWER, false, true);
                publicFolderId = createNewFolder(true, BITS_REVIEWER, true, false);
                sharedFolderId = createSharedFolder();
                break;
            case "merge":
                privateFolderId = createNewFolder(false, I(0), true, true);
                publicFolderId = createNewFolder(false, I(0), true, false);
                sharedFolderId = createSharedFolder();
                break;
            default:
                fail("Unexpected type: " + type);
        }
    }

    @Override
    protected String getScope() {
        return "user";
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        Map<String, String> configs = new HashMap<>();
        configs.put("com.openexchange.folderstorage.permissions.moveToPublic", type);
        configs.put("com.openexchange.folderstorage.permissions.moveToShared", type);
        configs.put("com.openexchange.folderstorage.permissions.moveToPrivate", type);
        return configs;
    }

    protected String getPrivateInfostoreFolder(SessionAwareClient apiClient) throws ApiException {
        ConfigApi configApi = new ConfigApi(apiClient);
        ConfigResponse configNode = configApi.getConfigNode(Tree.PrivateInfostoreFolder.getPath());
        Object data = checkResponse(configNode);
        if (data != null && !data.toString().equalsIgnoreCase("null")) {
            return String.valueOf(data);
        }
        Assertions.fail("It seems that the user doesn't support drive.");
        return null;
    }

    protected Object checkResponse(ConfigResponse resp) {
        Assertions.assertNull(resp.getErrorDesc(), resp.getError());
        Assertions.assertNotNull(resp.getData());
        return resp.getData();
    }

    protected String createNewFolder(boolean additionalPermissions, Integer additionalBits, boolean addGroup, boolean privateTree) throws Exception {
        return createNewFolder(additionalPermissions ? userId2 : null, additionalBits, addGroup, privateTree);
    }

    protected String createNewFolder(Integer userIdToShare, Integer additionalBits, boolean addGroup, boolean privateTree) throws Exception {
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule(Module.INFOSTORE.getName());
        folder.setSummary("FolderPermissionTest_" + UUID.randomUUID().toString());
        folder.setTitle(folder.getSummary());
        folder.setSubscribed(Boolean.TRUE);
        List<FolderPermission> perm = new ArrayList<FolderPermission>();
        FolderPermission p1 = createPermissionFor(userId1, BITS_ADMIN, Boolean.FALSE);
        perm.add(p1);
        if (userIdToShare != null && userIdToShare.intValue() > 0) {
            FolderPermission p = createPermissionFor(userIdToShare, additionalBits, Boolean.FALSE);
            perm.add(p);
        }
        if (addGroup) {
            FolderPermission p = createPermissionFor(I(0), BITS_VIEWER, Boolean.TRUE);
            perm.add(p);
        }
        folder.setPermissions(perm);
        body.setFolder(folder);
        FolderUpdateResponse response = api.createFolder(privateTree ? getPrivateInfostoreFolder(getApiClient()) : "15", body, TREE, null, null, null);
        String folderId = response.getData();
        createdFolders.add(folderId);
        return folderId;
    }

    protected String createChildFolder(String parentFolderId) throws ApiException {
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule(Module.INFOSTORE.getName());
        folder.setSummary("FolderPermissionTest_" + UUID.randomUUID().toString());
        folder.setTitle(folder.getSummary());
        folder.setSubscribed(Boolean.TRUE);
        folder.setPermissions(null);
        body.setFolder(folder);
        FolderUpdateResponse response = api.createFolder(parentFolderId, body, TREE, null, null, null);
        String folderId = response.getData();
        createdFolders.add(folderId);
        return folderId;
    }

    protected String createChildFolder(String parentFolderId, Integer userIdToShare) throws ApiException {
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule(Module.INFOSTORE.getName());
        folder.setSummary("FolderPermissionTest_" + UUID.randomUUID().toString());
        folder.setTitle(folder.getSummary());
        folder.setSubscribed(Boolean.TRUE);
        List<FolderPermission> perm = new ArrayList<FolderPermission>();
        FolderPermission p1 = createPermissionFor(userId1, BITS_ADMIN, Boolean.FALSE);
        perm.add(p1);
        if (userIdToShare != null && userIdToShare.intValue() > 0) {
            FolderPermission p = createPermissionFor(userIdToShare, BITS_VIEWER, Boolean.FALSE);
            perm.add(p);
        }
        folder.setPermissions(perm);
        body.setFolder(folder);
        FolderUpdateResponse response = api.createFolder(parentFolderId, body, TREE, null, null, null);
        String folderId = response.getData();
        createdFolders.add(folderId);
        return folderId;
    }

    protected FolderPermission createPermissionFor(Integer entity, Integer bits, Boolean isGroup) {
        FolderPermission p = new FolderPermission();
        p.setEntity(entity);
        p.setGroup(isGroup);
        p.setBits(bits);
        return p;
    }

    private String createSharedFolder() throws Exception {
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setModule(Module.INFOSTORE.getName());
        folder.setSummary("FolderPermissionTest_" + UUID.randomUUID().toString());
        folder.setTitle(folder.getSummary());
        folder.setSubscribed(Boolean.TRUE);
        List<FolderPermission> perm = new ArrayList<FolderPermission>();
        FolderPermission p1 = createPermissionFor(userId2, BITS_ADMIN, Boolean.FALSE);
        perm.add(p1);
        FolderPermission p2 = createPermissionFor(userId1, BITS_AUTHOR, Boolean.FALSE);
        perm.add(p2);
        folder.setPermissions(perm);
        body.setFolder(folder);
        FolderUpdateResponse response = api2.createFolder(getPrivateInfostoreFolder(apiClient2), body, TREE, null, null, null);
        return response.getData();
    }

}
