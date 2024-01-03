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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.TimeZone;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.framework.ListIDs;
import com.openexchange.ajax.task.actions.GetRequest;
import com.openexchange.ajax.task.actions.GetResponse;
import com.openexchange.ajax.task.actions.InsertRequest;
import com.openexchange.ajax.task.actions.InsertResponse;
import com.openexchange.ajax.task.actions.ListRequest;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.test.common.groupware.tasks.Create;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests percentage completed set to 0.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class Bug14002Test extends AbstractTaskTest {

    private AJAXClient client;

    private int folderId;

    private TimeZone tz;

    private Task task;

    public Bug14002Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
        folderId = client.getValues().getPrivateTaskFolder();
        tz = client.getValues().getTimeZone();
        task = Create.createWithDefaults(folderId, "Bug 14002 test task");
        task.setPercentComplete(0);
        final InsertResponse insertR = client.execute(new InsertRequest(task, tz));
        insertR.fillTask(task);
    }

    @Test
    public void testPercentageComplete() throws OXException, IOException, JSONException {
        GetRequest request = new GetRequest(task.getParentFolderID(), task.getObjectID());
        GetResponse response = client.execute(request);
        Task toTest = response.getTask(tz);
        assertTrue(toTest.containsPercentComplete(), "Percentage complete is missing.");
        assertEquals(task.getPercentComplete(), toTest.getPercentComplete(), "Percentage complete has wrong value.");
        ListIDs ids = ListIDs.l(new int[] { task.getParentFolderID(), task.getObjectID() });
        ListRequest request2 = new ListRequest(ids, new int[] { Task.PERCENT_COMPLETED });
        CommonListResponse response2 = client.execute(request2);
        Object percentage = response2.getValue(0, Task.PERCENT_COMPLETED);
        assertNotNull(percentage, "Percentage complete is missing.");
        assertEquals(task.getPercentComplete(), ((Integer) percentage).intValue(), "Percentage complete has wrong value.");
    }
}
