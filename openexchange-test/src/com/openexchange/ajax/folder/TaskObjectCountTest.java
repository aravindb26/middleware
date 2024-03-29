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
import java.io.IOException;
import java.util.Random;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.MultipleRequest;
import com.openexchange.ajax.task.actions.InsertRequest;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.test.FolderTestManager;

/**
 * Verifies if the object count on the folder works successfully for task folders.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class TaskObjectCountTest extends AbstractObjectCountTest {

    private static final Random rand = new Random(System.currentTimeMillis());

    @Test
    public void testCountInPrivateFolder() throws Exception {
        FolderTestManager ftm = new FolderTestManager(client1);
        try {
            int folderId = createPrivateFolder(client1, ftm, FolderObject.TASK).getObjectID();
            assertEquals(0, getCountTotal(client1, folderId), "Wrong object count");

            int numTasks = rand.nextInt(20) + 1;
            createTasks(client1, folderId, numTasks);
            assertEquals(numTasks, getCountTotal(client1, folderId), "Wrong object count");
        } finally {
            ftm.cleanUp();
        }
    }

    @Test
    public void testCountInPublicFolder() throws Exception {
        FolderTestManager ftm = new FolderTestManager(client1);
        try {
            FolderObject created = createPublicFolder(client1, FolderObject.TASK, client2.getValues().getUserId(), ftm);
            int folderId = created.getObjectID();
            assertEquals(0, getCountTotal(client1, folderId), "Wrong object count");

            int numTasks1 = rand.nextInt(20) + 1;
            int numTasks2 = rand.nextInt(20) + 1;
            createTasks(client1, folderId, numTasks1);
            createTasks(client2, folderId, numTasks2);
            assertEquals(numTasks2, getCountTotal(client2, folderId), "Wrong object count for public folder reader.");
            assertEquals(numTasks1 + numTasks2, getCountTotal(client1, folderId), "Wrong object count for public folder creator.");
        } finally {
            ftm.cleanUp();
        }
    }

    @Test
    public void testCountInSharedFolder() throws Exception {
        FolderTestManager ftm = new FolderTestManager(client1);
        try {
            FolderObject created = createSharedFolder(client1, FolderObject.TASK, client2.getValues().getUserId(), ftm);
            int folderId = created.getObjectID();
            assertEquals(0, getCountTotal(client1, folderId), "Wrong object count");

            int numTasks1 = rand.nextInt(20) + 1;
            int numTasks2 = rand.nextInt(20) + 1;
            createTasks(client1, folderId, numTasks1, true);
            createTasks(client2, folderId, numTasks2);
            assertEquals(numTasks2, getCountTotal(client2, folderId), "Wrong object count for shared folder reader.");
            assertEquals(numTasks1 + numTasks2, getCountTotal(client1, folderId), "Wrong object count for shared folder owner.");
        } finally {
            ftm.cleanUp();
        }
    }

    private static void createTasks(AJAXClient client, int folderId, int count) throws OXException, IOException, JSONException {
        createTasks(client, folderId, count, false);
    }

    private static void createTasks(AJAXClient client, int folderId, int count, boolean privat) throws OXException, IOException, JSONException {
        final InsertRequest[] inserts = new InsertRequest[count];
        for (int i = 0; i < inserts.length; i++) {
            final Task task = new Task();
            task.setTitle("Task for folder count test " + (i + 1));
            task.setParentFolderID(folderId);
            task.setPrivateFlag(privat);
            inserts[i] = new InsertRequest(task, client.getValues().getTimeZone());
        }
        client.execute(MultipleRequest.create(inserts));
    }
}
