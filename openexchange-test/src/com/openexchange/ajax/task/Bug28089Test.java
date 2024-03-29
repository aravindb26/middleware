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

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.folder.actions.EnumAPI;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.CommonDeleteResponse;
import com.openexchange.ajax.framework.CommonInsertResponse;
import com.openexchange.ajax.task.actions.InsertRequest;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.test.common.groupware.tasks.Create;
import org.junit.jupiter.api.TestInfo;

/**
 * Verifies that the problem described in bug 28089 does not appear again. The issue is a SQL problem due to different connections used to
 * delete reminder.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public final class Bug28089Test extends AbstractTaskTest {

    private AJAXClient client1;
    private TimeZone tz;
    private Task task;
    private FolderObject folder;

    public Bug28089Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client1 = getClient();
        tz = getTimeZone();
        // Create public folder
        folder = com.openexchange.ajax.folder.Create.setupPublicFolder("Folder for test for bug 28089", FolderObject.TASK, client1.getValues().getUserId());
        folder.setParentFolderID(FolderObject.SYSTEM_PUBLIC_FOLDER_ID);
        CommonInsertResponse response = client1.execute(new com.openexchange.ajax.folder.actions.InsertRequest(EnumAPI.OX_OLD, folder));
        folder.setObjectID(response.getId());
        folder.setLastModified(response.getTimestamp());

        task = Create.createWithDefaults(folder.getObjectID(), "Task to test for bug 28089");
        task.setAlarm(new Date());
        client1.execute(new InsertRequest(task, tz)).fillTask(task);
    }

    @Test
    public void testForBug() throws OXException, IOException, JSONException {
        CommonDeleteResponse response = client1.execute(new com.openexchange.ajax.folder.actions.DeleteRequest(EnumAPI.OX_OLD, folder));
        List<OXException> warnings = response.getResponse().getWarnings();
        String message;
        try {
            message = warnings.get(0).getMessage();
        } catch (@SuppressWarnings("unused") IndexOutOfBoundsException e) {
            message = "Response contains warnings.";
        }
        Assertions.assertTrue(warnings.isEmpty(), message);
    }
}
