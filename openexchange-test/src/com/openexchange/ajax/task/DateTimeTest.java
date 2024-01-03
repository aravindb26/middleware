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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.fields.CalendarFields;
import com.openexchange.ajax.fields.TaskFields;
import com.openexchange.ajax.framework.AJAXClient;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.framework.CommonListResponse;
import com.openexchange.ajax.task.actions.GetRequest;
import com.openexchange.ajax.task.actions.GetResponse;
import com.openexchange.ajax.task.actions.InsertRequest;
import com.openexchange.ajax.task.actions.ListRequest;
import com.openexchange.ajax.task.actions.UpdateRequest;
import com.openexchange.ajax.task.actions.UpdateResponse;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.tasks.Task;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link DateTimeTest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public final class DateTimeTest extends AbstractAJAXSession {

    private AJAXClient client;
    private TimeZone timeZone;
    private List<Task> tasksToDelete;

    /**
     * Initializes a new {@link DateTimeTest}.
     *
     * @param name The test name
     */
    public DateTimeTest() {
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
    public void testCreateWithLegacyClient() throws Exception {
        /*
         * create task as a legacy client would do
         */
        Task task = new Task();
        task.setStartDate(D("09.10.2014 00:00", TimeZone.getTimeZone("UTC")));
        task.setEndDate(D("13.10.2014 00:00", TimeZone.getTimeZone("UTC")));
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testCreateWithLegacyClient");
        client.execute(new InsertRequest(task, timeZone)).fillTask(task);
        tasksToDelete.add(task);
        /*
         * verify created task, interpreted as legacy client
         */
        GetResponse getResponse = client.execute(new GetRequest(task));
        Task legacyTask = getResponse.getTask(timeZone, true);
        assertNotNull(legacyTask, "No task created");
        assertEquals(task.getStartDate(), legacyTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), legacyTask.getEndDate(), "End date wrong");
        /*
         * verify created task, interpreted as new client
         */
        Task newTask = getResponse.getTask(timeZone, false);
        assertNotNull(newTask, "No task created");
        assertTrue(newTask.getFullTime(), "Fulltime wrong");
        assertEquals(task.getStartDate(), newTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), newTask.getEndDate(), "End date wrong");
        /*
         * verify raw data
         */
        JSONObject data = (JSONObject) getResponse.getData();
        assertTrue(data.hasAndNotNull(CalendarFields.FULL_TIME), "No fulltime");
        assertTrue(data.getBoolean(CalendarFields.FULL_TIME), "Fulltime wrong");
        assertTrue(data.hasAndNotNull(TaskFields.START_TIME), "No start time");
        assertEquals(task.getStartDate().getTime(), data.getLong(TaskFields.START_TIME), "Start time wrong");
        assertTrue(data.hasAndNotNull(TaskFields.END_TIME), "No end time");
        assertEquals(task.getEndDate().getTime(), data.getLong(TaskFields.END_TIME), "End time wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.START_DATE), "No start date");
        assertEquals(task.getStartDate().getTime(), data.getLong(CalendarFields.START_DATE), "Start date wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.END_DATE), "No end date");
        assertEquals(task.getEndDate().getTime(), data.getLong(CalendarFields.END_DATE), "End date wrong");
    }

    @Test
    public void testCreateWithNewClient() throws Exception {
        /*
         * create task as a new client would do
         */
        Task task = new Task();
        task.setStartDate(D("11.11.2014 11:11", timeZone));
        task.setEndDate(D("12.11.2014 04:00", timeZone));
        task.setFullTime(false);
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testCreateWithNewClient");
        client.execute(new InsertRequest(task, timeZone, false, true, false)).fillTask(task);
        tasksToDelete.add(task);
        /*
         * verify created task, interpreted as legacy client
         */
        GetResponse getResponse = client.execute(new GetRequest(task));
        Task legacyTask = getResponse.getTask(timeZone, true);
        assertNotNull(legacyTask, "No task created");
        assertEquals(task.getStartDate(), legacyTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), legacyTask.getEndDate(), "End date wrong");
        /*
         * verify created task, interpreted as new client
         */
        Task newTask = getResponse.getTask(timeZone, false);
        assertNotNull(newTask, "No task created");
        assertFalse(newTask.getFullTime(), "Fulltime wrong");
        assertEquals(task.getStartDate(), newTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), newTask.getEndDate(), "End date wrong");
        /*
         * verify raw data
         */
        JSONObject data = (JSONObject) getResponse.getData();
        assertTrue(data.hasAndNotNull(CalendarFields.FULL_TIME), "No fulltime");
        assertFalse(data.getBoolean(CalendarFields.FULL_TIME), "Fulltime wrong");
        assertTrue(data.hasAndNotNull(TaskFields.START_TIME), "No start time");
        assertEquals(task.getStartDate().getTime() + timeZone.getOffset(task.getStartDate().getTime()), data.getLong(TaskFields.START_TIME), "Start time wrong");
        assertTrue(data.hasAndNotNull(TaskFields.END_TIME), "No end time");
        assertEquals(task.getEndDate().getTime() + timeZone.getOffset(task.getEndDate().getTime()), data.getLong(TaskFields.END_TIME), "End time wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.START_DATE), "No start date");
        assertEquals(task.getStartDate().getTime(), data.getLong(CalendarFields.START_DATE), "Start date wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.END_DATE), "No end date");
        assertEquals(task.getEndDate().getTime(), data.getLong(CalendarFields.END_DATE), "End date wrong");
    }

    @Test
    public void testCreateFulltimeWithNewClient() throws Exception {
        /*
         * create fulltime task as a new client would do
         */
        Task task = new Task();
        task.setStartDate(D("20.12.2014 00:00", TimeZone.getTimeZone("UTC")));
        task.setEndDate(D("23.12.2014 00:00", TimeZone.getTimeZone("UTC")));
        task.setFullTime(true);
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testCreateFulltimeWithNewClient");
        client.execute(new InsertRequest(task, timeZone, false, true, false)).fillTask(task);
        tasksToDelete.add(task);
        /*
         * verify created task, interpreted as legacy client
         */
        GetResponse getResponse = client.execute(new GetRequest(task));
        Task legacyTask = getResponse.getTask(timeZone, true);
        assertNotNull(legacyTask, "No task created");
        assertEquals(task.getStartDate(), legacyTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), legacyTask.getEndDate(), "End date wrong");
        /*
         * verify created task, interpreted as new client
         */
        Task newTask = getResponse.getTask(timeZone, false);
        assertNotNull(newTask, "No task created");
        assertTrue(newTask.getFullTime(), "Fulltime wrong");
        assertEquals(task.getStartDate(), newTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), newTask.getEndDate(), "End date wrong");
        /*
         * verify raw data
         */
        JSONObject data = (JSONObject) getResponse.getData();
        assertTrue(data.hasAndNotNull(CalendarFields.FULL_TIME), "No fulltime");
        assertTrue(data.getBoolean(CalendarFields.FULL_TIME), "Fulltime wrong");
        assertTrue(data.hasAndNotNull(TaskFields.START_TIME), "No start time");
        assertEquals(task.getStartDate().getTime(), data.getLong(TaskFields.START_TIME), "Start time wrong");
        assertTrue(data.hasAndNotNull(TaskFields.END_TIME), "No end time");
        assertEquals(task.getEndDate().getTime(), data.getLong(TaskFields.END_TIME), "End time wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.START_DATE), "No start date");
        assertEquals(task.getStartDate().getTime(), data.getLong(CalendarFields.START_DATE), "Start date wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.END_DATE), "No end date");
        assertEquals(task.getEndDate().getTime(), data.getLong(CalendarFields.END_DATE), "End date wrong");
    }

    @Test
    public void testCreateWithBrokenLegacyClient() throws Exception {
        /*
         * create task as a "broken" legacy client would do, i.e. insert time values not being a multiple of 8.64e7
         */
        Task task = new Task();
        task.setStartDate(D("22.10.2014 07:00", TimeZone.getTimeZone("UTC")));
        task.setEndDate(D("25.10.2014 14:00", TimeZone.getTimeZone("UTC")));
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testCreateWithBrokenLegacyClient");
        client.execute(new InsertRequest(task, timeZone)).fillTask(task);
        tasksToDelete.add(task);
        /*
         * verify created task, interpreted as legacy client
         */
        GetResponse getResponse = client.execute(new GetRequest(task));
        Task legacyTask = getResponse.getTask(timeZone, true);
        assertNotNull(legacyTask, "No task created");
        assertEquals(task.getStartDate(), legacyTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), legacyTask.getEndDate(), "End date wrong");
        /*
         * verify created task, interpreted as new client
         */
        Task newTask = getResponse.getTask(timeZone, false);
        assertNotNull(newTask, "No task created");
        assertTrue(newTask.getFullTime(), "Fulltime wrong");
        assertEquals(D("22.10.2014 00:00", TimeZone.getTimeZone("UTC")), newTask.getStartDate(), "Start date wrong");
        assertEquals(D("25.10.2014 00:00", TimeZone.getTimeZone("UTC")), newTask.getEndDate(), "End date wrong");
        /*
         * verify raw data
         */
        JSONObject data = (JSONObject) getResponse.getData();
        assertTrue(data.hasAndNotNull(CalendarFields.FULL_TIME), "No fulltime");
        assertTrue(data.getBoolean(CalendarFields.FULL_TIME), "Fulltime wrong");
        assertTrue(data.hasAndNotNull(TaskFields.START_TIME), "No start time");
        assertEquals(D("22.10.2014 00:00", TimeZone.getTimeZone("UTC")).getTime(), data.getLong(TaskFields.START_TIME), "Start time wrong");
        assertTrue(data.hasAndNotNull(TaskFields.END_TIME), "No end time");
        assertEquals(D("25.10.2014 00:00", TimeZone.getTimeZone("UTC")).getTime(), data.getLong(TaskFields.END_TIME), "End time wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.START_DATE), "No start date");
        assertEquals(task.getStartDate().getTime(), data.getLong(CalendarFields.START_DATE), "Start date wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.END_DATE), "No end date");
        assertEquals(task.getEndDate().getTime(), data.getLong(CalendarFields.END_DATE), "End date wrong");
    }

    @Test
    public void testUpdateWithLegacyClient() throws Exception {
        /*
         * create task as a new client would do
         */
        Task task = new Task();
        task.setStartDate(D("05.11.2014 20:00", timeZone));
        task.setEndDate(D("05.11.2014 21:00", timeZone));
        task.setFullTime(false);
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testUpdateWithLegacyClient");
        client.execute(new InsertRequest(task, timeZone, false, true, false)).fillTask(task);
        tasksToDelete.add(task);
        /*
         * update task as legacy client, changing time-related properties
         */
        Task toUpdate = new Task();
        toUpdate.setObjectID(task.getObjectID());
        toUpdate.setParentFolderID(task.getParentFolderID());
        toUpdate.setLastModified(task.getLastModified());
        toUpdate.setStartDate(D("05.11.2014 00:00", TimeZone.getTimeZone("UTC")));
        toUpdate.setEndDate(D("06.11.2014 00:00", TimeZone.getTimeZone("UTC")));
        UpdateResponse updateResponse = client.execute(new UpdateRequest(toUpdate, timeZone));
        assertFalse(updateResponse.hasError(), "Errors in response");
        /*
         * verify updated task, interpreted as legacy client
         */
        GetResponse getResponse = client.execute(new GetRequest(task));
        Task legacyTask = getResponse.getTask(timeZone, true);
        assertNotNull(legacyTask, "No task created");
        assertEquals(toUpdate.getStartDate(), legacyTask.getStartDate(), "Start date wrong");
        assertEquals(toUpdate.getEndDate(), legacyTask.getEndDate(), "End date wrong");
        /*
         * verify created task, interpreted as new client
         */
        Task newTask = getResponse.getTask(timeZone, false);
        assertNotNull(newTask, "No task created");
        assertTrue(newTask.getFullTime(), "Fulltime wrong");
        assertEquals(toUpdate.getStartDate(), newTask.getStartDate(), "Start date wrong");
        assertEquals(toUpdate.getEndDate(), newTask.getEndDate(), "End date wrong");
        /*
         * verify raw data
         */
        JSONObject data = (JSONObject) getResponse.getData();
        assertTrue(data.hasAndNotNull(CalendarFields.FULL_TIME), "No fulltime");
        assertTrue(data.getBoolean(CalendarFields.FULL_TIME), "Fulltime wrong");
        assertTrue(data.hasAndNotNull(TaskFields.START_TIME), "No start time");
        assertEquals(toUpdate.getStartDate().getTime(), data.getLong(TaskFields.START_TIME), "Start time wrong");
        assertTrue(data.hasAndNotNull(TaskFields.END_TIME), "No end time");
        assertEquals(toUpdate.getEndDate().getTime(), data.getLong(TaskFields.END_TIME), "End time wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.START_DATE), "No start date");
        assertEquals(toUpdate.getStartDate().getTime(), data.getLong(CalendarFields.START_DATE), "Start date wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.END_DATE), "No end date");
        assertEquals(toUpdate.getEndDate().getTime(), data.getLong(CalendarFields.END_DATE), "End date wrong");
    }

    @Test
    public void testUpdateWithNewClient() throws Exception {
        /*
         * create task as a legacy client would do
         */
        Task task = new Task();
        task.setStartDate(D("23.10.2014 00:00", TimeZone.getTimeZone("UTC")));
        task.setEndDate(D("23.10.2014 00:00", TimeZone.getTimeZone("UTC")));
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testUpdateWithNewClient");
        client.execute(new InsertRequest(task, timeZone)).fillTask(task);
        tasksToDelete.add(task);
        /*
         * update task as new client, changing time-related properties
         */
        Task toUpdate = new Task();
        toUpdate.setObjectID(task.getObjectID());
        toUpdate.setParentFolderID(task.getParentFolderID());
        toUpdate.setLastModified(task.getLastModified());
        toUpdate.setStartDate(D("23.10.2014 08:00", timeZone));
        toUpdate.setEndDate(D("24.10.2014 18:00", timeZone));
        toUpdate.setFullTime(false);
        UpdateResponse updateResponse = client.execute(new UpdateRequest(toUpdate, timeZone, true, false));
        assertFalse(updateResponse.hasError(), "Errors in response");
        /*
         * verify updated task, interpreted as legacy client
         */
        GetResponse getResponse = client.execute(new GetRequest(task));
        Task legacyTask = getResponse.getTask(timeZone, true);
        assertNotNull(legacyTask, "No task created");
        assertEquals(toUpdate.getStartDate(), legacyTask.getStartDate(), "Start date wrong");
        assertEquals(toUpdate.getEndDate(), legacyTask.getEndDate(), "End date wrong");
        /*
         * verify created task, interpreted as new client
         */
        Task newTask = getResponse.getTask(timeZone, false);
        assertNotNull(newTask, "No task created");
        assertFalse(newTask.getFullTime(), "Fulltime wrong");
        assertEquals(toUpdate.getStartDate(), newTask.getStartDate(), "Start date wrong");
        assertEquals(toUpdate.getEndDate(), newTask.getEndDate(), "End date wrong");
        /*
         * verify raw data
         */
        JSONObject data = (JSONObject) getResponse.getData();
        assertTrue(data.hasAndNotNull(CalendarFields.FULL_TIME), "No fulltime");
        assertFalse(data.getBoolean(CalendarFields.FULL_TIME), "Fulltime wrong");
        assertTrue(data.hasAndNotNull(TaskFields.START_TIME), "No start time");
        assertEquals(toUpdate.getStartDate().getTime() + timeZone.getOffset(toUpdate.getStartDate().getTime()), data.getLong(TaskFields.START_TIME), "Start time wrong");
        assertTrue(data.hasAndNotNull(TaskFields.END_TIME), "No end time");
        assertEquals(toUpdate.getEndDate().getTime() + timeZone.getOffset(toUpdate.getEndDate().getTime()), data.getLong(TaskFields.END_TIME), "End time wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.START_DATE), "No start date");
        assertEquals(toUpdate.getStartDate().getTime(), data.getLong(CalendarFields.START_DATE), "Start date wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.END_DATE), "No end date");
        assertEquals(toUpdate.getEndDate().getTime(), data.getLong(CalendarFields.END_DATE), "End date wrong");
    }

    @Test
    public void testUpdateOtherAttribute() throws Exception {
        /*
         * create task as a new client would do
         */
        Task task = new Task();
        task.setStartDate(D("28.10.2014 08:00", timeZone));
        task.setEndDate(D("01.11.2014 16:00", timeZone));
        task.setFullTime(false);
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testUpdateOtherAttribute");
        client.execute(new InsertRequest(task, timeZone, false, true, false)).fillTask(task);
        tasksToDelete.add(task);
        /*
         * update task, not changing time-related properties
         */
        Task toUpdate = new Task();
        toUpdate.setObjectID(task.getObjectID());
        toUpdate.setParentFolderID(task.getParentFolderID());
        toUpdate.setLastModified(task.getLastModified());
        toUpdate.setNote("added notes");
        UpdateResponse updateResponse = client.execute(new UpdateRequest(toUpdate, timeZone));
        assertFalse(updateResponse.hasError(), "Errors in response");
        /*
         * verify updated task, interpreted as legacy client
         */
        GetResponse getResponse = client.execute(new GetRequest(task));
        Task legacyTask = getResponse.getTask(timeZone, true);
        assertNotNull(legacyTask, "No task created");
        assertEquals(task.getStartDate(), legacyTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), legacyTask.getEndDate(), "End date wrong");
        assertEquals(toUpdate.getNote(), legacyTask.getNote(), "Note wrong");
        /*
         * verify created task, interpreted as new client
         */
        Task newTask = getResponse.getTask(timeZone, false);
        assertNotNull(newTask, "No task created");
        assertFalse(newTask.getFullTime(), "Fulltime wrong");
        assertEquals(task.getStartDate(), newTask.getStartDate(), "Start date wrong");
        assertEquals(task.getEndDate(), newTask.getEndDate(), "End date wrong");
        assertEquals(toUpdate.getNote(), newTask.getNote(), "Note wrong");
        /*
         * verify raw data
         */
        JSONObject data = (JSONObject) getResponse.getData();
        assertTrue(data.hasAndNotNull(CalendarFields.FULL_TIME), "No fulltime");
        assertFalse(data.getBoolean(CalendarFields.FULL_TIME), "Fulltime wrong");
        assertTrue(data.hasAndNotNull(TaskFields.START_TIME), "No start time");
        assertEquals(task.getStartDate().getTime() + timeZone.getOffset(task.getStartDate().getTime()), data.getLong(TaskFields.START_TIME), "Start time wrong");
        assertTrue(data.hasAndNotNull(TaskFields.END_TIME), "No end time");
        assertEquals(task.getEndDate().getTime() + timeZone.getOffset(task.getEndDate().getTime()), data.getLong(TaskFields.END_TIME), "End time wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.START_DATE), "No start date");
        assertEquals(task.getStartDate().getTime(), data.getLong(CalendarFields.START_DATE), "Start date wrong");
        assertTrue(data.hasAndNotNull(CalendarFields.END_DATE), "No end date");
        assertEquals(task.getEndDate().getTime(), data.getLong(CalendarFields.END_DATE), "End date wrong");
    }

    @Test
    public void testColumns() throws Exception {
        /*
         * create task as a new client would do
         */
        Task task = new Task();
        task.setStartDate(D("09.12.2014 19:15", timeZone));
        task.setEndDate(D("03.01.2015 14:00", timeZone));
        task.setFullTime(false);
        task.setParentFolderID(client.getValues().getPrivateTaskFolder());
        task.setTitle("testUpdateOtherAttribute");
        client.execute(new InsertRequest(task, timeZone, false, true, false)).fillTask(task);
        tasksToDelete.add(task);
        int[][] folderAndTaskIDs = new int[][] { { task.getParentFolderID(), task.getObjectID() } };
        /*
         * retrieve & check full_time
         */
        CommonListResponse listResponse = client.execute(new ListRequest(folderAndTaskIDs, new int[] { CalendarObject.FULL_TIME }));
        Object value = listResponse.getValue(0, CalendarObject.FULL_TIME);
        assertTrue(null != value && (value instanceof Boolean), "Wrong type for fulltime");
        assertFalse(((Boolean) value).booleanValue(), "Fulltime wrong");
        /*
         * retrieve & check start_date
         */
        listResponse = client.execute(new ListRequest(folderAndTaskIDs, new int[] { CalendarObject.START_DATE }));
        value = listResponse.getValue(0, CalendarObject.START_DATE);
        assertTrue(null != value && (value instanceof Long), "Wrong type for start date");
        assertEquals(task.getStartDate().getTime(), ((Long) value).longValue(), "Start date wrong");
        /*
         * retrieve & check end_date
         */
        listResponse = client.execute(new ListRequest(folderAndTaskIDs, new int[] { CalendarObject.END_DATE }));
        value = listResponse.getValue(0, CalendarObject.END_DATE);
        assertTrue(null != value && (value instanceof Long), "Wrong type for end date");
        assertEquals(task.getEndDate().getTime(), ((Long) value).longValue(), "End date wrong");
        /*
         * retrieve & check start_time
         */
        listResponse = client.execute(new ListRequest(folderAndTaskIDs, new int[] { Task.START_TIME }));
        value = listResponse.getValue(0, Task.START_TIME);
        assertTrue(null != value && (value instanceof Long), "Wrong type for start time");
        assertEquals(task.getStartDate().getTime() + timeZone.getOffset(task.getStartDate().getTime()), ((Long) value).longValue(), "Start time time");
        /*
         * retrieve & check end_date
         */
        listResponse = client.execute(new ListRequest(folderAndTaskIDs, new int[] { Task.END_TIME }));
        value = listResponse.getValue(0, Task.END_TIME);
        assertTrue(null != value && (value instanceof Long), "Wrong type for end time");
        assertEquals(task.getEndDate().getTime() + timeZone.getOffset(task.getEndDate().getTime()), ((Long) value).longValue(), "End time wrong");
    }

}
