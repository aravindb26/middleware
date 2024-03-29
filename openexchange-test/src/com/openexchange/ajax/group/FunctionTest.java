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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.framework.AbstractAJAXSession;
import com.openexchange.ajax.group.actions.AbstractGroupResponse;
import com.openexchange.ajax.group.actions.AllRequest;
import com.openexchange.ajax.group.actions.AllResponse;
import com.openexchange.ajax.group.actions.CreateRequest;
import com.openexchange.ajax.group.actions.CreateResponse;
import com.openexchange.ajax.group.actions.DeleteRequest;
import com.openexchange.ajax.group.actions.GetRequest;
import com.openexchange.ajax.group.actions.GetResponse;
import com.openexchange.ajax.group.actions.ListRequest;
import com.openexchange.ajax.group.actions.SearchRequest;
import com.openexchange.ajax.group.actions.SearchResponse;
import com.openexchange.ajax.group.actions.UpdatesRequest;
import com.openexchange.ajax.group.actions.UpdatesResponse;
import com.openexchange.group.Group;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class FunctionTest extends AbstractAJAXSession {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FunctionTest.class);

    @Test
    public void testSearch() throws Throwable {
        SearchResponse response = getClient().execute(new SearchRequest("*"));
        final Group[] groups = response.getGroups();
        LOG.trace("Found " + groups.length + " groups.");
        assertTrue(groups.length > 0, "Size of group array should be more than 0.");

        JSONArray arr = (JSONArray) response.getResponse().getData();
        assertContainsLastModifiedUTC(arr);
    }

    @Test
    public void testRealSearch() throws Throwable {
        final Group[] groups = getClient().execute(new SearchRequest("*l*")).getGroups();
        LOG.trace("Found " + groups.length + " groups.");
        assertNotNull(groups);
    }

    @Test
    public void testList() throws Throwable {
        Group[] groups = getClient().execute(new SearchRequest("*")).getGroups();
        LOG.trace("Found " + groups.length + " groups.");
        assertTrue(groups.length > 0, "Size of group array should be more than 0.");
        final int[] groupIds = new int[groups.length];
        for (int i = 0; i < groupIds.length; i++) {
            groupIds[i] = groups[i].getIdentifier();
        }
        AbstractGroupResponse listResponse = getClient().execute(new ListRequest(groupIds));
        groups = listResponse.getGroups();
        LOG.trace("Listed " + groups.length + " groups.");
        assertTrue(groups.length > 0, "Size of group array should be more than 0.");
        assertEquals(groupIds.length, groups.length, "Size of requested groups and listed groups should be equal.");

        JSONArray arr = (JSONArray) listResponse.getResponse().getData();
        assertContainsLastModifiedUTC(arr);
    }

    @Test
    public void testAllWithMembers() throws Throwable {
        int groupLengthBySearch = getClient().execute(new SearchRequest("*")).getGroups().length;

        AllRequest allRequest = new AllRequest(Group.ALL_COLUMNS, true);
        AllResponse allResponse = getClient().execute(allRequest);
        JSONArray data = (JSONArray) allResponse.getData();

        int groupLengthByAll = data.length();

        assertEquals(groupLengthBySearch, groupLengthByAll);

        int memberPos = 4;
        int memberCount = 0;
        for (int i = 0; i < data.length(); i++) {
            JSONArray row = data.getJSONArray(i);
            String[] members = row.getString(memberPos).split(",");
            memberCount += members.length;
        }
        assertTrue(memberCount > 0);
    }

    @Test
    public void testAllWithoutMembers() throws Throwable {
        int groupLengthBySearch = getClient().execute(new SearchRequest("*")).getGroups().length;

        AllResponse allResponse = getClient().execute(new AllRequest(Group.ALL_COLUMNS_EXCEPT_MEMBERS, true));
        JSONArray data = (JSONArray) allResponse.getData();

        int groupLengthByAll = data.length();

        assertEquals(groupLengthBySearch, groupLengthByAll);

        int arrLen = Group.ALL_COLUMNS_EXCEPT_MEMBERS.length;
        for (int i = 0; i < data.length(); i++) {
            JSONArray row = data.getJSONArray(i);
            assertEquals(arrLen, row.length());
        }
    }

    @Test
    public void testUpdatesViaComparingWithSearch() throws Exception {
        Group[] groupsViaSearch = getClient().execute(new SearchRequest("*")).getGroups();
        UpdatesResponse response = getClient().execute(new UpdatesRequest(new Date(0), false));
        List<Group> groupsViaUpdates = response.getModified();
        for (Group group : groupsViaSearch) {
            Group matchingGroup = null;
            for (Group updatedGroup : groupsViaUpdates) {
                if (updatedGroup.getIdentifier() == group.getIdentifier()) {
                    matchingGroup = updatedGroup;
                    break;
                }
            }
            assertNotNull(matchingGroup, "Group " + group.getDisplayName() + " not find via updates since day 0");
        }
    }

    @Test
    public void testUpdatesViaCreateAndDelete() throws Exception {
        int staticGroupCount = 2; // "all users" & "guests" are always included in new/modified responses
        Group group = new Group();
        group.setSimpleName("simplename_" + new Date().getTime());
        group.setDisplayName("Group Updates Test" + new Date());

        CreateResponse createResponse = getClient().execute(new CreateRequest(group, true));
        int id = createResponse.getId();
        group.setIdentifier(id);
        group.setLastModified(createResponse.getTimestamp());
        Date lm = new Date(group.getLastModified().getTime() - 1);

        UpdatesResponse updatesResponseAfterCreate = getClient().execute(new UpdatesRequest(lm, true));
        int numberNewAfterCreation = updatesResponseAfterCreate.getNew().size();
        int numberModifiedAfterCreation = updatesResponseAfterCreate.getModified().size();
        int numberDeletedAfterCreation = updatesResponseAfterCreate.getDeleted().size();
        assertEquals(1 + staticGroupCount, numberModifiedAfterCreation, "Amount of modified elements should have increased after creation");
        assertEquals(0 + staticGroupCount, numberDeletedAfterCreation, "Amount of deleted elements should not change after creation");
        assertEquals(numberNewAfterCreation, numberModifiedAfterCreation, "Amount of new elements should equal modfied elements, since we cannot distinguish between the two");

        getClient().execute(new DeleteRequest(group, true));

        UpdatesResponse updatesResponseAfterDeletion = getClient().execute(new UpdatesRequest(lm, true));
        int numberNewAfterDeletion = updatesResponseAfterDeletion.getNew().size();
        int numberModifiedAfterDeletion = updatesResponseAfterDeletion.getModified().size();
        int numberDeletedAfterDeletion = updatesResponseAfterDeletion.getDeleted().size();
        assertEquals(0 + staticGroupCount, numberModifiedAfterDeletion, "Amount of modified elements should have decreased after deletion");
        assertEquals(1 + staticGroupCount, numberDeletedAfterDeletion, "Amount of deleted elements should have increased after deletion");
        assertEquals(numberNewAfterDeletion, numberModifiedAfterDeletion, "Amount of new elements should equal modfied elements, since we cannot distinguish between the two");
    }

    public void assertContainsLastModifiedUTC(JSONArray arr) {
        for (int i = 0, size = arr.length(); i < size; i++) {
            JSONObject entry = arr.optJSONObject(i);
            assertNotNull(entry);
            assertTrue(entry.has("last_modified_utc"));
        }
    }

    @Test
    public void testGet() throws Throwable {
        final Group groups[] = getClient().execute(new SearchRequest("*")).getGroups();
        LOG.trace("Found " + groups.length + " groups.");
        assertTrue(groups.length > 0, "Size of group array should be more than 0.");
        final int pos = new Random(System.currentTimeMillis()).nextInt(groups.length);
        GetResponse response = getClient().execute(new GetRequest(groups[pos].getIdentifier()));
        final Group group = response.getGroup();
        LOG.trace("Loaded group: " + group.toString());
        JSONObject entry = (JSONObject) response.getData();
        assertTrue(entry.has("last_modified_utc"));
    }

}
