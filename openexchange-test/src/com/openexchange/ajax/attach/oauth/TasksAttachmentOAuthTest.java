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

package com.openexchange.ajax.attach.oauth;

import static com.openexchange.java.Autoboxing.I;
import java.util.List;
import com.openexchange.testing.httpclient.models.TaskData;
import com.openexchange.testing.httpclient.models.TaskUpdateResponse;
import com.openexchange.testing.httpclient.modules.TasksApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link TasksAttachmentOAuthTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class TasksAttachmentOAuthTest extends AbstractAttachmentOAuthTest {

    private String taskId;
    private int folderId;
    private List<Integer> attachmentIds;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        folderId = getClient().getValues().getPrivateTaskFolder();
        taskId = createTestTask();
        attachmentIds = attach();
    }

    ///////////////////////////// HELPERS ////////////////////////

    /**
     * Creates a test task
     * 
     * @return The id of the test task
     */
    private String createTestTask() throws Exception {
        TasksApi api = new TasksApi(getApiClient());
        TaskUpdateResponse task = api.createTask(createTestTaskData(folderId));
        return task.getData().getId();
    }

    /**
     * Creates the test task data
     * 
     * @param folder the folder
     * @return The test task data
     */
    private TaskData createTestTaskData(int folder) {
        TaskData taskData = new TaskData();
        taskData.setFolderId(I(folder).toString());
        taskData.setTitle("TestTask");
        return taskData;
    }

    @Override
    int getFolderId() {
        return folderId;
    }

    @Override
    int getModuleId() {
        return 4;
    }

    @Override
    int getObjectId() {
        return Integer.parseInt(taskId);
    }

    @Override
    List<Integer> getAttachmentIds() {
        return attachmentIds;
    }
}
