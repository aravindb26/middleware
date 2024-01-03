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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.share.Abstract2UserShareTest;
import com.openexchange.file.storage.DefaultFileStorageObjectPermission;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageObjectPermission;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.testing.httpclient.models.InfoItemListElement;
import com.openexchange.testing.httpclient.models.InfoItemsResponse;
import com.openexchange.testing.httpclient.modules.InfostoreApi;

/**
 * {@link MWB1857} - Incomplete response when requesting /infostore?action=list
 *
 * If a user has access to a file via folder permissions (shared folder) and,
 * in addition, directly via object permission (shared file),
 * and that user lists the file twice, once via it's regular folder and secondly
 * via folder 10 the returned response was incomplete.
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v8.0.0
 */
public class MWB1857 extends Abstract2UserShareTest {

    private InfostoreApi infostoreApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        this.infostoreApi = new InfostoreApi(apiClient2);
    }

    @Test
    public void testListResharedFile() throws Exception {

        //As user1: Create a folder and share it to user2
        final int userId = client2.getValues().getUserId();
        final OCLPermission permission = new OCLPermission(userId, 0);
        permission.setAllPermission(OCLPermission.READ_ALL_OBJECTS, OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
        FolderObject folder = insertSharedFolder(EnumAPI.OX_NEW, FolderObject.INFOSTORE, getClient().getValues().getPrivateInfostoreFolder(), permission);

        //As user1: Create file in this new folder and share it to user2, too
        DefaultFileStorageObjectPermission objectPermission = new DefaultFileStorageObjectPermission(userId, false, FileStorageObjectPermission.READ);
        File file = insertSharedFile(folder.getObjectID(), objectPermission);

        //As user2: List the file twice,
        //once from it's original folder and
        //secondly from the shared files folder (10)
        InfoItemsResponse response = infostoreApi.getInfoItemListBuilder()
            .withColumns("1")
            .withInfoItemListElement(Arrays.asList(

                new InfoItemListElement()
                    .folder(file.getFolderId())
                    .id(file.getUniqueId()),

                new InfoItemListElement()
                    .folder("10")
                    .id(file.getUniqueId())
             ))
            .execute();

        @SuppressWarnings("unchecked")
        List<List<String>> data = (List<List<String>>) checkResponse(response.getError(), response.getErrorDesc(), response.getData());
        assertEquals(2, data.size(), "The response should contain two entries");
        assertTrue(data.stream().anyMatch( item -> item.get(0).equals(file.getId())), "The requested file should be in the response");
        assertTrue(data.stream().anyMatch( item -> item.get(0).equals("10/"+file.getUniqueId())),"The requested file should be in the response");
    }
}