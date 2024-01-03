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

package com.openexchange.ajax.folder.api_client;

import static com.openexchange.java.Autoboxing.I;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.exception.OXException;
import com.openexchange.testing.httpclient.models.FolderBody;
import com.openexchange.testing.httpclient.models.FolderData;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderResponse;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.modules.FoldersApi;

/**
 * {@link SubscribeTest} contains tests which tests to subscribe and unsubscribe task/contact/calendar folders
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.4
 */
public class SubscribeTest extends AbstractConfigAwareAPIClientSession {

    private static final String TREE = "0";

    private FoldersApi foldersApi;
    private String defaultFolder;
    private HashSet<String> toDelete;

    public static Stream<String> data() {
        return Stream.of(
                "contacts",
                "event",
                "tasks"
        );
    }

    public void prepareTest(String module, TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        foldersApi = new FoldersApi(getApiClient());
        defaultFolder = getDefaultFolder(foldersApi, module);
        toDelete = new HashSet<>();
    }

    /**
     * Retrieves the default calendar folder of the user with the specified session
     *
     * @param foldersApi The {@link FoldersApi}
     * @return The default calendar folder of the user
     * @throws Exception if the default calendar folder cannot be found
     */
    protected String getDefaultFolder(FoldersApi foldersApi, String module) throws Exception {
        ArrayList<ArrayList<?>> privateList = getPrivateFolderList(foldersApi, module, "1,308", TREE);
        if (privateList.size() == 1) {
            return (String) privateList.get(0).get(0);
        }
        for (ArrayList<?> folder : privateList) {
            if (folder.get(1) != null && ((Boolean) folder.get(1)).booleanValue()) {
                return (String) folder.get(0);
            }
        }
        throw new Exception("Unable to find default folder!");
    }

    /**
     * @param foldersApi The {@link FoldersApi} to use
     * @param module The folder module
     * @param columns The columns identifier
     * @param tree The folder tree identifier
     * @return List of available folders
     * @throws Exception if the api call fails
     */
    @SuppressWarnings({ "unchecked" })
    protected ArrayList<ArrayList<?>> getPrivateFolderList(FoldersApi foldersApi, String module, String columns, String tree) throws Exception {
        FoldersVisibilityResponse visibleFolders = foldersApi.getVisibleFolders(module, columns, tree, null, Boolean.TRUE);
        if (visibleFolders.getError() != null) {
            throw new OXException(new Exception(visibleFolders.getErrorDesc()));
        }
        Object privateFolders = visibleFolders.getData().getPrivate();
        return (ArrayList<ArrayList<?>>) privateFolders;
    }


    @ParameterizedTest
    @MethodSource("data")
    public void checkDefaultFolder(String module, TestInfo testInfo) throws Exception {
        prepareTest(module, testInfo);
        FolderResponse response = foldersApi.getFolder(defaultFolder, TREE, module, null, null);
        FolderData root = checkResponse(response);
        Long timestamp = response.getTimestamp();
        assertTrue(root.getStandardFolder().booleanValue());

        FolderBody body = new FolderBody();
        FolderData update = new FolderData();
        update.setId(defaultFolder);
        update.setSubscribed(FALSE);
        body.setFolder(update);
        FolderUpdateResponse resp = foldersApi.updateFolder(defaultFolder, body, FALSE, timestamp, TREE, module, FALSE, null, null, Boolean.TRUE);
        assertNotNull(resp.getError());
        assertEquals("FLD-1044", resp.getCode());
    }


    @ParameterizedTest
    @MethodSource("data")
    public void checkRoundtrip(String module, TestInfo testInfo) throws Exception {
        prepareTest(module, testInfo);
        // 1. Create new folder
        NewFolderBody body = new NewFolderBody();
        NewFolderBodyFolder data = new NewFolderBodyFolder();
        String title = this.getClass().getSimpleName()+"_"+System.currentTimeMillis();
        data.setTitle(title);
        data.setSubscribed(TRUE);
        data.setModule(module);

        FolderPermission perm = new FolderPermission();
        perm.entity(I(getUserId()));
        perm.setGroup(Boolean.FALSE);
        perm.setBits(I(403710016));
        List<FolderPermission> perms = Collections.singletonList(perm);
        data.setPermissions(perms);

        body.setFolder(data);
        FolderUpdateResponse resp = foldersApi.createFolder(defaultFolder, body, TREE, module, null, null);
        assertNull(resp.getError());
        assertNotNull(resp.getData());
        String newFolder = rememberFolder(resp.getData());

        // 2. Check is subscribed
        FolderResponse response = foldersApi.getFolder(newFolder, TREE, module, null, null);
        FolderData fData = checkResponse(response);
        Long timestamp = response.getTimestamp();
        assertTrue(fData.getSubscribed().booleanValue(), "Folder should be subscribed");

        // 3. Change subscription to false
        FolderBody updateBody = new FolderBody();
        FolderData updateData = new FolderData();
        updateData.setId(newFolder);
        updateData.setSubscribed(FALSE);
        updateData.setPermissions(perms);
        updateBody.setFolder(updateData);
        FolderUpdateResponse updateResponse = foldersApi.updateFolder(newFolder, updateBody, FALSE, timestamp, TREE, module, FALSE, null, null, Boolean.TRUE);
        assertNull(updateResponse.getError());
        assertNotNull(updateResponse.getData());

        // 4. Check subscribed status again
        response = foldersApi.getFolder(newFolder, TREE, module, null, null);
        fData = checkResponse(response);
        timestamp = response.getTimestamp();
        assertFalse(fData.getSubscribed().booleanValue(), "Folder shouldn't be subscribed anymore");

        // 5. Change subscription back to true
        updateBody = new FolderBody();
        updateData = new FolderData();
        updateData.setId(newFolder);
        updateData.setSubscribed(TRUE);
        updateData.setPermissions(perms);
        updateBody.setFolder(updateData);
        updateResponse = foldersApi.updateFolder(newFolder, updateBody, FALSE, timestamp, TREE, module, FALSE, null, null, Boolean.TRUE);
        assertNull(updateResponse.getError());
        assertNotNull(updateResponse.getData());

        // 6. Check subscribed status again
        fData = checkResponse(foldersApi.getFolder(newFolder, TREE, module, null, null));
        assertTrue(fData.getSubscribed().booleanValue(), "Folder should be subscribed again");
    }

    private String rememberFolder(String id) {
        toDelete.add(id);
        return id;
    }

    private FolderData checkResponse(FolderResponse resp) {
        assertNull(resp.getError());
        assertNotNull(resp.getData());
        return resp.getData();
    }
}
