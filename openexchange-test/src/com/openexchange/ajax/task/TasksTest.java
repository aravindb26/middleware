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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.Abstrac2UserAJAXSession;
import com.openexchange.ajax.participant.ParticipantTools;
import com.openexchange.ajax.task.actions.DeleteRequest;
import com.openexchange.ajax.task.actions.GetRequest;
import com.openexchange.ajax.task.actions.GetResponse;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.ExternalUserParticipant;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.test.TaskTestManager;

/**
 * This class tests the AJAX interface of the tasks.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class TasksTest extends Abstrac2UserAJAXSession {

    /**
     * Logger.
     */
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(TasksTest.class);

    /**
     * Tests inserting a delegated task.
     *
     * @throws Throwable if an error occurs.
     */
    @Test
    public void testInsertDelegatedPrivateTask() throws Throwable {
        final Task task = new Task();
        task.setTitle("Private delegated task");
        task.setPrivateFlag(false);
        task.setCreationDate(new Date());
        Date lastModified = new Date();
        task.setLastModified(lastModified);
        task.setStartDate(new Date(1133964000000L));
        task.setEndDate(new Date(1133967600000L));
        task.setAfterComplete(new Date(1133971200000L));
        task.setNote("Description");
        task.setStatus(Task.NOT_STARTED); //FIXME!
        task.setPriority(I(Task.NORMAL));
        task.setCategories("Categories");
        task.setTargetDuration(L(1440));
        task.setActualDuration(L(1440));
        task.setTargetCosts(new BigDecimal("1.0"));
        task.setActualCosts(new BigDecimal("1.0"));
        task.setCurrency("\u20ac");
        task.setTripMeter("trip meter");
        task.setBillingInformation("billing information");
        task.setCompanies("companies");

        final int folderId = getClient().getValues().getPrivateTaskFolder();

        final List<Participant> participants = ParticipantTools.getParticipants(getClient(), 2, true, getClient().getValues().getUserId());
        final ExternalUserParticipant external = new ExternalUserParticipant("external@external.no");
        external.setDisplayName("External, External");
        participants.add(external);
        task.setParticipants(participants);
        task.setParentFolderID(folderId);

        final int taskId = ttm.insertTaskOnServer(task).getObjectID();
        LOG.trace("Created delegated task: {}", I(taskId));

        final Task reload = ttm.getTaskFromServer(folderId, taskId);
        for (final Participant p1 : reload.getParticipants()) {
            boolean found = false;
            for (final Participant p2 : participants) {
                if (p1.getIdentifier() == p2.getIdentifier()) {
                    found = true;
                }
            }
            if (!found) {
                fail("Storing participant in delegated task failed.");
            }
        }
        lastModified = reload.getLastModified();
        deleteTask(folderId, taskId, lastModified);
    }

    private void deleteTask(int folderId, int taskId, Date lastModified) throws OXException, IOException, JSONException {
        final DeleteRequest request = new DeleteRequest(folderId, taskId, lastModified);
        getClient().execute(request);
    }

    @Test
    public void testUpdateDelegatedTask() throws Throwable {
        // aquire two additional users for this test
        this.testContext.acquireUser();
        this.testContext.acquireUser();

        final List<Participant> participants = ParticipantTools.getParticipants(getClient(), 4, true, getClient().getValues().getUserId());
        final List<Participant> firstParticipants = new ArrayList<Participant>();
        firstParticipants.addAll(participants.subList(0, 2));
        final List<Participant> secondParticipants = new ArrayList<Participant>();
        secondParticipants.addAll(participants.subList(2, 4));
        secondParticipants.add(participants.get(0));

        final Task task = new Task();
        task.setTitle("Private delegated task");
        final int folderId = getClient().getValues().getPrivateTaskFolder();
        task.setParentFolderID(folderId);
        task.setParticipants(firstParticipants);

        LOG.trace("Creating delegated task with participants: {}", firstParticipants);
        final int taskId = ttm.insertTaskOnServer(task).getObjectID();
        LOG.trace("Created delegated task: {}", I(taskId));
        GetRequest getRequest = new GetRequest(folderId, taskId);
        GetResponse getResponse = getClient().execute(getRequest);

        Date lastModified = getResponse.getTimestamp();
        Task reload = getResponse.getTask(TimeZone.getDefault());
        assertEquals(firstParticipants.size(), reload.getParticipants().length, "Number of participants differ");
        for (final Participant p1 : firstParticipants) {
            boolean found = false;
            for (final Participant p2 : reload.getParticipants()) {
                if (p1.getIdentifier() == p2.getIdentifier()) {
                    found = true;
                }
            }
            if (!found) {
                fail("Delegated task misses participant: " + p1.getIdentifier());
            }
        }

        reload.setTitle("Updated delegated task");
        reload.setParticipants(secondParticipants);
        ttm.updateTaskOnServer(reload);
        LOG.trace("Updating delegated task with participants: {}", secondParticipants);
        reload = ttm.getTaskFromServer(folderId, taskId);
        lastModified = reload.getLastModified();
        assertEquals(secondParticipants.size(), reload.getParticipants().length, "Number of participants differ");
        for (final Participant p1 : secondParticipants) {
            boolean found = false;
            for (final Participant p2 : reload.getParticipants()) {
                if (p1.getIdentifier() == p2.getIdentifier()) {
                    found = true;
                }
            }
            if (!found) {
                fail("Delegated task misses participant: " + p1.getIdentifier());
            }
        }

        deleteTask(folderId, taskId, lastModified);
    }

    @Test
    public void testUpdate() throws Throwable {
        final String title = "Title";
        final String updatedTitle = "Complete other title.";
        final Task task = new Task();
        task.setTitle(title);

        final int folderId = getClient().getValues().getPrivateTaskFolder();

        task.setParentFolderID(folderId);
        final int taskId = ttm.insertTaskOnServer(task).getObjectID();

        Task response = ttm.getTaskFromServer(folderId, taskId);
        Date lastModified = response.getLastModified();

        task.setObjectID(taskId);
        task.setTitle(updatedTitle);
        task.setLastModified(lastModified);
        Task updated = ttm.updateTaskOnServer(task);

        response = ttm.getTaskFromServer(folderId, taskId);
        assertEquals(updatedTitle, updated.getTitle(), "Title of task is not updated.");
        lastModified = response.getLastModified();

        deleteTask(folderId, taskId, lastModified);
    }

    /**
     * Tests a full list of tasks in a folder with ordering.
     *
     * @throws Throwable if an error occurs.
     */
    @Test
    public void testAllWithOrder() throws Throwable {
        final int folderId = getClient().getValues().getPrivateTaskFolder();
        final Task task = new Task();
        task.setParentFolderID(folderId);
        final int[][] tasks = new int[10][2];
        for (int i = 0; i < tasks.length; i++) {
            task.setTitle("Task " + (i + 1));
            tasks[i][1] = ttm.insertTaskOnServer(task).getObjectID();
            tasks[i][0] = folderId;
        }
        final int[] columns = new int[] { Task.TITLE, Task.OBJECT_ID, Task.LAST_MODIFIED, Task.FOLDER_ID };
        Task[] loaded = ttm.getAllTasksOnServer(folderId, columns);

        // TODO parse JSON array
        final Date lastModified = loaded[0].getLastModified();
        for (final int[] folderAndTask : tasks) {
            deleteTask(folderAndTask[0], folderAndTask[1], lastModified);
        }
    }

    @Test
    public void testConfirmation() throws Throwable {
        TaskTestManager ttm2 = new TaskTestManager(client2);
        final int folderId = getClient().getValues().getPrivateTaskFolder();

        final Task task = new Task();
        task.setTitle("Task to test confirmation");

        final int folderId2 = client2.getValues().getPrivateTaskFolder();
        final int userId2 = client2.getValues().getUserId();

        final List<UserParticipant> participants = new ArrayList<UserParticipant>();
        final UserParticipant participant = new UserParticipant(userId2);
        participants.add(participant);
        task.setParticipants(participants);
        task.setParentFolderID(folderId);

        final int taskId = ttm.insertTaskOnServer(task).getObjectID();
        LOG.trace("Created delegated task for confirmation: {}", I(taskId));

        Task taskForUser = ttm2.getTaskFromServer(folderId2, taskId);
        taskForUser.setConfirm(Task.ACCEPT);
        taskForUser.setConfirmMessage("Testconfirmation.");
        ttm2.confirm(taskForUser);

        final Task reload = ttm.getTaskFromServer(folderId, taskId);
        final UserParticipant[] users = reload.getUsers();
        boolean confirmed = false;
        for (final UserParticipant user : users) {
            final int confirm = user.getConfirm();
            final int userId = user.getIdentifier();
            if (userId2 == userId && Task.ACCEPT == confirm) {
                confirmed = true;
            }
        }
        assertTrue(confirmed, "Can't find confirmation.");

        ttm2.cleanUp();
    }

    /**
     * Creates a task with a reminder and checks if the reminder is stored
     * correctly.
     *
     * @throws Throwable if an error occurs.
     */
    @Test
    public void testReminder() throws Throwable {
        final int folderId = getClient().getValues().getPrivateTaskFolder();

        final Task task = new Task();
        task.setTitle("Task to test reminder");
        task.setParentFolderID(folderId);
        final long remindTime = System.currentTimeMillis() / 1000 * 1000;
        final Date remind = new Date(remindTime);
        task.setAlarm(remind);

        final int taskId = ttm.insertTaskOnServer(task).getObjectID();

        final Task reload = ttm.getTaskFromServer(folderId, taskId);
        assertEquals(remind, reload.getAlarm(), "Missing reminder.");
        ttm.deleteTaskOnServer(reload);
    }

    /**
     * @param response response object of an update or some other request that
     *            returns a timestamp.
     * @return the timestamp of the response.
     */
    public static Date extractTimestamp(final Response response) {
        final Date retval = response.getTimestamp();
        assertNotNull(retval, "Timestamp is missing.");
        return retval;
    }
}
