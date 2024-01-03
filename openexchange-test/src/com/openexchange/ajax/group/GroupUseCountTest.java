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

package com.openexchange.ajax.group;

import static com.openexchange.java.Autoboxing.I;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.GroupData;
import com.openexchange.testing.httpclient.models.GroupSearchBody;
import com.openexchange.testing.httpclient.models.GroupUpdateResponse;
import com.openexchange.testing.httpclient.models.GroupsResponse;
import com.openexchange.testing.httpclient.models.TaskData;
import com.openexchange.testing.httpclient.models.TaskListElement;
import com.openexchange.testing.httpclient.models.TaskParticipant;
import com.openexchange.testing.httpclient.models.TaskParticipant.TypeEnum;
import com.openexchange.testing.httpclient.models.TaskUpdateResponse;
import com.openexchange.testing.httpclient.modules.GroupsApi;
import com.openexchange.testing.httpclient.modules.TasksApi;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link GroupUseCountTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.2
 */
public class GroupUseCountTest extends AbstractChronosTest {
    
    final int MAX_RETRY = 60;

    private GroupsApi groupsApi;
    private Integer[] groupIds;
    private TasksApi taskApi;
    private List<TaskListElement> tasksToDelete;
    private String randPrefix;

    /**
     * Initializes a new {@link GroupUseCountTest}.
     */
    public GroupUseCountTest() {
        super();
    }


    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        randPrefix = UUID.randomUUID().toString();
        groupsApi = new GroupsApi(getApiClient());
        GroupData groupData = new GroupData();
        String name1 = randPrefix + "_1";
        groupData.setDisplayName(name1);
        groupData.setName(name1);
        GroupUpdateResponse response = groupsApi.createGroup(groupData);
        Assertions.assertNull(response.getError(), response.getErrorDesc());
        groupIds = new Integer[2];
        groupIds[0] = response.getData().getId();
        groupData = new GroupData();
        String name2 = randPrefix + "_2";
        groupData.setDisplayName(name2);
        groupData.setName(name2);
        List<Integer> members = new ArrayList<>(2);
        members.add(I(3));
        members.add(I(4));
        groupData.setMembers(members);
        response = groupsApi.createGroup(groupData);
        Assertions.assertNull(response.getError(), response.getErrorDesc());
        groupIds[1] = response.getData().getId();

        tasksToDelete = new ArrayList<TaskListElement>();
        taskApi = new TasksApi(getApiClient());
    }

    private synchronized void rememberTask(String folderId, String id) {
        TaskListElement element = new TaskListElement();
        element.setId(id);
        element.setFolder(folderId);
        this.tasksToDelete.add(element);
    }

    @Test
    public void testUseCount() throws ApiException, InterruptedException {
        GroupSearchBody body = new GroupSearchBody();
        body.setPattern(randPrefix);
        GroupsResponse response = groupsApi.searchGroups(body);
        Assertions.assertNull(response.getError(), response.getErrorDesc());
        List<GroupData> groups = response.getData();
        Assertions.assertEquals(2, groups.size());
        int x = 0;
        // Check that groups are returned in the same order
        for (GroupData group : groups) {
            Assertions.assertEquals(groupIds[x++], group.getId());
        }

        // use group 2
        EventData eventData = EventFactory.createSingleTwoHourEvent(getCalendaruser(), "testUseCount", folderId);
        Attendee att = new Attendee();
        att.setEntity(groupIds[1]);
        att.setCuType(CuTypeEnum.GROUP);
        eventData.addAttendeesItem(att);
        eventManager.createEvent(eventData, true);

        // Check order again
        body = new GroupSearchBody();
        body.setPattern(randPrefix);
        boolean correctOrder = false;
        int retryCounter = 0;
        while (retryCounter < MAX_RETRY) {
            response = groupsApi.searchGroups(body);
            Assertions.assertNull(response.getError(), response.getErrorDesc());
            groups = response.getData();
            Assertions.assertEquals(2, groups.size());
            try {
                x = 1;
                // Check that groups are returned in the inverse order now
                for (GroupData group : groups) {
                    Assertions.assertEquals(groupIds[x--], group.getId());
                }
                correctOrder = true;
                return;
            } catch (@SuppressWarnings("unused") AssertionError error) {
                // Order is not correct yet. Maybe the use count just take some more time.
                retryCounter++;
                Thread.sleep(1000);
            }
        }
        if (!correctOrder) {
            fail("Unexpected order of groups.");
        }
    }

    @Test
    public void testUseCountWithTask() throws Exception {
        GroupSearchBody body = new GroupSearchBody();
        body.setPattern(randPrefix);
        GroupsResponse response = groupsApi.searchGroups(body);
        Assertions.assertNull(response.getError(), response.getErrorDesc());
        List<GroupData> groups = response.getData();
        Assertions.assertEquals(2, groups.size());
        int x = 0;
        // Check that groups are returned in the same order
        for (GroupData group : groups) {
            Assertions.assertEquals(groupIds[x++], group.getId());
        }

        // use group 2
        TaskData task = new TaskData();
        task.setTitle("testUseCountWithTask");
        task.setFolderId(getTaskFolderId());
        TaskParticipant participant = new TaskParticipant();
        participant.setId(groupIds[1]);
        participant.setType(TypeEnum.NUMBER_2); // 2 == user group
        task.addParticipantsItem(participant);
        TaskUpdateResponse createTask = taskApi.createTask(task);
        Assertions.assertNull(createTask.getError(), createTask.getErrorDesc());
        rememberTask(task.getFolderId(), createTask.getData().getId());

        // Check order again
        body = new GroupSearchBody();
        body.setPattern(randPrefix);
        boolean correctOrder = false;
        int retryCounter = 0;
        while (retryCounter < MAX_RETRY) {
            response = groupsApi.searchGroups(body);
            Assertions.assertNull(response.getError(), response.getErrorDesc());
            groups = response.getData();
            Assertions.assertEquals(2, groups.size());
            try {
                x = 1;
                // Check that groups are returned in the inverse order now
                for (GroupData group : groups) {
                    Assertions.assertEquals(groupIds[x--], group.getId());
                }
                correctOrder = true;
                return;
            } catch (@SuppressWarnings("unused") AssertionError error) {
                // Order is not correct yet. Maybe the use count just take some more time.
                retryCounter++;
                Thread.sleep(1000);
            }
        }
        if (!correctOrder) {
            fail("Unexpected order of groups.");
        }
    }

    /**
     * @return
     * @throws Exception
     */
    private String getTaskFolderId() throws Exception {
        ArrayList<ArrayList<?>> privateList = getPrivateFolderList(foldersApi, "tasks", "1,308", "0");
        if (privateList.size() == 1) {
            return (String) privateList.get(0).get(0);
        }
        for (ArrayList<?> folder : privateList) {
            if (((Boolean) folder.get(1)).booleanValue()) {
                return (String) folder.get(0);
            }
        }
        throw new Exception("Unable to find default tasks folder!");
    }

}
