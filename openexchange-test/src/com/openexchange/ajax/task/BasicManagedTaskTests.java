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

package com.openexchange.ajax.task;

import static com.openexchange.test.common.groupware.calendar.TimeTools.removeMilliseconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Date;
import org.junit.jupiter.api.Test;
import com.openexchange.groupware.tasks.Task;

/**
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class BasicManagedTaskTests extends ManagedTaskTest {

    @Test
    public void testCreateAndGet() {
        Task expected = generateTask("Create test");
        ttm.insertTaskOnServer(expected);
        actual = ttm.getTaskFromServer(expected);
        assertEquals(expected.getTitle(), actual.getTitle(), "Should have the same title");
        assertEquals(expected.getParentFolderID(), actual.getParentFolderID(), "Should have the same folder");
        assertEquals(removeMilliseconds(expected.getStartDate()), actual.getStartDate(), "Should have the same start date");
        assertEquals(removeMilliseconds(expected.getEndDate()), actual.getEndDate(), "Should have the same end date");
        assertEquals(expected.getLastModified(), actual.getLastModified(), "Should have the same last modified");
    }

    @Test
    public void testAll() {
        int numberBefore = ttm.getAllTasksOnServer(folderID, new int[] { 1, 4, 5, 20, 209 }).length;
        Task expected = generateTask("Create test");
        ttm.insertTaskOnServer(expected);
        Task[] allTasksOnServer = ttm.getAllTasksOnServer(folderID, new int[] { 1, 4, 5, 20, 209 });
        actual = null;
        for (Task temp : allTasksOnServer) {
            if (expected.getObjectID() == temp.getObjectID()) {
                actual = temp;
            }
        }
        assertEquals(numberBefore + 1, allTasksOnServer.length, "Should find one more element than before");
        assertNotNull(actual, "Should find the newly created element");
        assertEquals(expected.get(1), actual.get(1), "Should have the same field #1 (id)");
        //assertEquals("Should have the same field #4 (creation date)", expected.get(4), actual.get(4));
        //assertEquals("Should have the same field #5 (last modified)", expected.get(5), actual.get(5));
        //assertEquals("Should have the same field #20 (folder)", expected.get(20), actual.get(20));
        assertEquals(expected.get(209), actual.get(209), "Should have the same field #209");
    }

    @Test
    public void testUpdateAndReceiveUpdates() {
        Task expected = generateTask("Create test");
        ttm.insertTaskOnServer(expected);

        Task updated = generateTask("Updates Test");
        updated.setParentFolderID(expected.getParentFolderID());
        updated.setObjectID(expected.getObjectID());
        updated.setLastModified(expected.getLastModified());

        ttm.updateTaskOnServer(updated);

        Date aMillisecondEarlier = new Date(expected.getLastModified().getTime() - 1);
        Task[] updates = ttm.getUpdatedTasksOnServer(folderID, new int[] { 1, 4, 5, 209 }, aMillisecondEarlier);
        assertEquals(1, updates.length, "Should find one update only");

        actual = updates[0];
        assertEquals(expected.get(1), actual.get(1), "Should have the same field #1");
        //assertEquals("Should have the same field #4", expected.get(4), actual.get(4));
        //assertEquals("Should have the same field #5", expected.get(5), actual.get(5));
        //assertEquals("Should have the same field #20", expected.get(20), actual.get(20));
        assertEquals(expected.get(209), actual.get(209), "Should have the same field #209");
    }

    @Test
    public void testCreateAndDelete() {
        Task task = generateTask("Create test");
        ttm.insertTaskOnServer(task);

        ttm.deleteTaskOnServer(task);
        boolean fail = false;
        try {
            ttm.getTaskFromServer(task);
            fail = true;
        } catch (AssertionError e) {

        }
        if (fail) {
            fail("Should fail by not finding task");
        }
    }

}
