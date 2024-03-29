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

import static com.openexchange.test.common.groupware.calendar.TimeTools.D;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonAllResponse;
import com.openexchange.ajax.task.actions.AllRequest;
import com.openexchange.ajax.task.actions.InsertRequest;
import com.openexchange.groupware.search.Order;
import com.openexchange.groupware.tasks.Task;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link Bug37927Test}
 *
 * Task all request does not support sorting by column 317 and 316
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class Bug37927Test extends AbstractAJAXSession {

    private AJAXClient client;
    private TimeZone timeZone;
    private List<Task> tasksToDelete;

    /**
     * Initializes a new {@link Bug37927Test}.
     *
     * @param name The test name
     */
    public Bug37927Test() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        client = getClient();
        timeZone = client.getValues().getTimeZone();
        tasksToDelete = new ArrayList<Task>();
    }

    @Test
    public void testGetAllTasks() throws Exception {
        /*
         * create tasks
         */
        Task task1 = new Task();
        task1.setStartDate(D("27.03.2015 11:11", timeZone));
        task1.setEndDate(D("28.03.2015 13:00", timeZone));
        task1.setFullTime(false);
        task1.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task1.setTitle("testCreateWithNewClient");
        client.execute(new InsertRequest(task1, timeZone, false, true, false)).fillTask(task1);
        tasksToDelete.add(task1);
        Task task2 = new Task();
        task2.setStartDate(D("27.03.2015 12:12", timeZone));
        task2.setEndDate(D("28.03.2015 14:00", timeZone));
        task2.setFullTime(false);
        task2.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task2.setTitle("testCreateWithNewClient");
        client.execute(new InsertRequest(task2, timeZone, false, true, false)).fillTask(task2);
        tasksToDelete.add(task2);
        /*
         * get tasks via all, sorted by start_time
         */
        int[] columns = new int[] { Task.OBJECT_ID, Task.START_TIME, Task.END_TIME };
        CommonAllResponse allResponse = client.execute(new AllRequest(client.getValues().getPrivateTaskFolder(), columns, Task.START_TIME, Order.ASCENDING));
        assertFalse(allResponse.hasError(), allResponse.getErrorMessage());
        /*
         * get tasks via all, sorted by end_time
         */
        allResponse = client.execute(new AllRequest(client.getValues().getPrivateTaskFolder(), columns, Task.END_TIME, Order.DESCENDING));
        assertFalse(allResponse.hasError(), allResponse.getErrorMessage());
    }

}
