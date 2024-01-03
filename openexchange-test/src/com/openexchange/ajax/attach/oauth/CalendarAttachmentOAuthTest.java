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

import java.util.ArrayList;
import java.util.List;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.tools.id.IDMangler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link CalendarAttachmentOAuthTest}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class CalendarAttachmentOAuthTest extends AbstractAttachmentOAuthTest {

    private String eventId;
    private int folderId;
    private String fullFolderId;
    private List<Integer> attachmentIds;
    private UserApi defaultUserApi;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        defaultUserApi = new UserApi(getApiClient(), testUser);
        fullFolderId = getDefaultFolder(defaultUserApi.getFoldersApi());
        List<String> unmagled = IDMangler.unmangle(fullFolderId);
        folderId = Integer.parseInt(unmagled.get(unmagled.size() - 1));
        eventId = createTestEvent();
        attachmentIds = attach();
    }

    ///////////////////////////// HELPERS ////////////////////////

    private String createTestEvent() throws Exception {
        EventManager eventManager = new EventManager(defaultUserApi, fullFolderId);
        EventData event = eventManager.createEvent(createTestEventData());
        return event.getId();
    }

    /**
     * Retrieves the default calendar folder of the user with the specified session
     *
     * @param foldersApi The {@link FoldersApi}
     * @return The default calendar folder of the user
     * @throws Exception if the default calendar folder cannot be found
     */
    private String getDefaultFolder(FoldersApi foldersApi) throws Exception {
        ArrayList<ArrayList<?>> privateList = getPrivateFolderList(foldersApi, "event", "1,308", "0");
        if (privateList.size() == 1) {
            return (String) privateList.get(0).get(0);
        }
        for (ArrayList<?> folder : privateList) {
            if (folder.get(1) != null && ((Boolean) folder.get(1)).booleanValue()) {
                return (String) folder.get(0);
            }
        }
        throw new Exception("Unable to find default calendar folder!");
    }

    /**
     * @param api The {@link FoldersApi} to use
     * @param module The folder module
     * @param columns The columns identifier
     * @param tree The folder tree identifier
     * @return List of available folders
     * @throws Exception if the api call fails
     */
    @SuppressWarnings({ "unchecked" })
    private ArrayList<ArrayList<?>> getPrivateFolderList(FoldersApi foldersApi, String module, String columns, String tree) throws Exception {
        FoldersVisibilityResponse resp = foldersApi.getVisibleFolders(module, columns, tree, null, Boolean.TRUE);
        FoldersVisibilityData visibilityData = checkResponse(resp.getError(), resp.getErrorDesc(), resp.getCategories(), resp.getData());
        Object privateFolders = visibilityData.getPrivate();
        return (ArrayList<ArrayList<?>>) privateFolders;
    }

    /**
     * Creates the test event data
     *
     * @return The event data
     */
    private EventData createTestEventData() {
        EventData eventData = new EventData();
        eventData.setDescription("TestEvent");
        DateTimeData start = new DateTimeData();
        start.setTzid("Europe/Berlin");
        start.setValue("20380720T113700");
        eventData.setStartDate(start);
        DateTimeData end = new DateTimeData();
        end.setTzid("Europe/Berlin");
        end.setValue("20380720T133700");
        eventData.setEndDate(end);
        return eventData;
    }

    @Override
    int getFolderId() {
        return folderId;
    }

    @Override
    int getModuleId() {
        return 1;
    }

    @Override
    int getObjectId() {
        return Integer.parseInt(eventId);
    }

    @Override
    List<Integer> getAttachmentIds() {
        return attachmentIds;
    }
}
