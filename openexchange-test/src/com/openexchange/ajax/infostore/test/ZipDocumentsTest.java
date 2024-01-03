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

package com.openexchange.ajax.infostore.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.AbstractObjectCountTest;
import com.openexchange.ajax.infostore.actions.InfostoreTestManager;
import com.openexchange.ajax.infostore.actions.ZipDocumentsRequest;
import com.openexchange.ajax.infostore.actions.ZipDocumentsRequest.IdVersionPair;
import com.openexchange.ajax.infostore.actions.ZipDocumentsResponse;
import com.openexchange.file.storage.DefaultFile;
import com.openexchange.file.storage.File;
import com.openexchange.folderstorage.Folder;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.test.FolderTestManager;
import com.openexchange.test.common.test.TestInit;

/**
 * {@link ZipDocumentsTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.2.2
 */
public final class ZipDocumentsTest extends AbstractObjectCountTest {

    /**
     * Initializes a new {@link ZipDocumentsTest}.
     *
     * @param name
     */
    public ZipDocumentsTest() {
        super();
    }

    @Test
    public void testZipDocumentsInInfostoreFolder() throws Exception {
        FolderTestManager folderTestManager = new FolderTestManager(client1);
        InfostoreTestManager infostoreTestManager = new InfostoreTestManager(client1);

        try {
            FolderObject created = createPrivateFolder(client1, folderTestManager, FolderObject.INFOSTORE);
            Folder folder = getFolder(client1, created.getObjectID(), DEFAULT_COLUMNS);
            assertEquals(0, getCountTotal(client1, created.getObjectID()), "Wrong object count");

            final String id1;
            {
                File expected = new DefaultFile();
                expected.setCreated(new Date());
                expected.setFolderId(folder.getID());
                expected.setTitle("InfostoreCreateDeleteTest File1");
                expected.setLastModified(new Date());
                java.io.File file = new java.io.File(TestInit.getTestProperty("ajaxPropertiesFile"));

                infostoreTestManager.newAction(expected, file);
                assertFalse(infostoreTestManager.getLastResponse().hasError(), "Creating an entry should work");
                id1 = expected.getId();

                File actual = infostoreTestManager.getAction(expected.getId());
                assertEquals(expected.getTitle(), actual.getTitle(), "Name should be the same");
            }

            final String id2;
            {
                File expected = new DefaultFile();
                expected.setCreated(new Date());
                expected.setFolderId(folder.getID());
                expected.setTitle("InfostoreCreateDeleteTest File2");
                expected.setLastModified(new Date());
                java.io.File file = new java.io.File(TestInit.getTestProperty("ajaxPropertiesFile"));

                infostoreTestManager.newAction(expected, file);
                assertFalse(infostoreTestManager.getLastResponse().hasError(), "Creating an entry should work");
                id2 = expected.getId();

                File actual = infostoreTestManager.getAction(expected.getId());
                assertEquals(expected.getTitle(), actual.getTitle(), "Name should be the same");
            }

            final List<IdVersionPair> pairs = new LinkedList<IdVersionPair>();
            pairs.add(new IdVersionPair(id1, null));
            pairs.add(new IdVersionPair(id2, null));
            ZipDocumentsRequest request = new ZipDocumentsRequest(pairs, folder.getID());
            ZipDocumentsResponse resp = getClient().execute(request);
            assertNull(resp.getException());
        } finally {
            infostoreTestManager.cleanUp();
            folderTestManager.cleanUp();
        }
    }

}
