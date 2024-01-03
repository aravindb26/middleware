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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.folder.manager.FolderApi;
import com.openexchange.ajax.folder.manager.FolderManager;
import com.openexchange.ajax.framework.AbstractAPIClientSession;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug16899Test}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com>Steffen Templin</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class Bug16899Test extends AbstractAPIClientSession {

    private static final String MODULE = "mail";
    private static final String COLUMNS = "1";
    private FolderManager folderManager;

    /**
     * Initializes a new {@link Bug16899Test}.
     */
    public Bug16899Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderManager = new FolderManager(new FolderApi(getApiClient(), testUser), String.valueOf(EnumAPI.OX_NEW.getTreeId()));
    }

    @Test
    public void testBug16899() throws Exception {
        String folderName = "Bug_16899_Test" + new UID().toString();
        String root = folderManager.getDefaultFolder(MODULE);
        String folderId = folderManager.createFolder(root, folderName, MODULE);

        ArrayList<ArrayList<Object>> listFolders = folderManager.listFolders(root, COLUMNS, Boolean.TRUE);
        assertNotNull(listFolders);
        assertTrue(listFolders.stream().filter(folder -> folder.get(0).equals(folderId)).findAny().isPresent(), "Testfolder not found");

        List<String> deleted = folderManager.deleteFolder(Collections.singletonList(folderId));
        assertTrue(deleted.isEmpty());
        folderManager.forgetFolder(folderId);

        listFolders = folderManager.listFolders(root, COLUMNS, Boolean.TRUE);
        assertNotNull(listFolders);
        assertFalse(listFolders.stream().filter(folder -> folder.get(0).equals(folderId)).findAny().isPresent(), "Testfolder still found");
    }

}
