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

package com.openexchange.ajax.chronos;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.i;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.AbstractConfigAwareAPIClientSession;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.ajax.infostore.manager.InfostoreManager;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.chronos.provider.CalendarProviders;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.test.common.asset.AssetManager;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiClient;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.CalendarUser;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.FolderPermission;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.FoldersVisibilityData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.models.UpdateEventBody;
import com.openexchange.testing.httpclient.models.UserData;
import com.openexchange.testing.httpclient.modules.ChronosApi;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.InfostoreApi;

/**
 * {@link AbstractChronosTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public class AbstractChronosTest extends AbstractConfigAwareAPIClientSession {

    /** The {@value #THIS_AND_FUTURE} recurrence ID */
    public static final String THIS_AND_FUTURE = "THISANDFUTURE";

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractChronosTest.class);

    private Set<EventId> eventIds;
    private Set<String> folderToDelete;
    private long lastTimeStamp;

    private static final String CONTACT_MODULE = "contacts";
    private static final String EVENT_MODULE = "event";
    private static final String CALENDAR_MODULE = "calendar";

    protected UserApi defaultUserApi;
    protected ChronosApi chronosApi;
    protected FoldersApi foldersApi;
    protected String defaultFolderId;

    protected EventManager eventManager;
    protected AssetManager assetManager;
    protected CalendarFolderManager folderManager;
    protected InfostoreManager infostoreManager;

    protected String folderId;

    /**
     * Initializes a new {@link AbstractChronosTest}.
     */
    public AbstractChronosTest() {
        super();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        LOG.info("Setup for test ...");
        SessionAwareClient client = getApiClient();
        defaultUserApi = new UserApi(client, testUser);
        chronosApi = defaultUserApi.getChronosApi();
        foldersApi = defaultUserApi.getFoldersApi();
        defaultFolderId = getDefaultFolder();
        assetManager = new AssetManager();
        eventManager = new EventManager(defaultUserApi, defaultFolderId);
        folderManager = new CalendarFolderManager(defaultUserApi, foldersApi);
        infostoreManager = new InfostoreManager(new InfostoreApi(client));
        folderId = createAndRememberNewFolder(defaultUserApi, getDefaultFolder(), defaultUserApi.getCalUser().intValue());
    }

    /**
     * Keeps track of the specified {@link EventId}
     *
     * @param eventId The {@link EventId}
     */
    protected void rememberEventId(EventId eventId) {
        if (eventIds == null) {
            eventIds = new HashSet<>();
        }
        eventIds.add(eventId);
    }

    /**
     * Keeps track of the specified folder
     *
     * @param folder The folder
     */
    protected void rememberFolder(String folder) {
        if (folderToDelete == null) {
            folderToDelete = new HashSet<>();
        }
        folderToDelete.add(folder);
    }

    /**
     * Creates a new folder and remembers it.
     *
     * @param api The {@link UserApi}
     * @param parent The parent folder
     * @param entity The user id
     * @return The result of the operation
     * @throws ApiException if an API error is occurred
     */
    protected String createAndRememberNewFolder(UserApi api, String parent, int entity) throws ApiException {
        FolderPermission perm = new FolderPermission();
        perm.setEntity(I(entity));
        perm.setGroup(Boolean.FALSE);
        perm.setBits(I(403710016));

        List<FolderPermission> permissions = new ArrayList<>();
        permissions.add(perm);
        return createAndRememberNewFolder(api, parent, permissions);
    }

    /**
     * Creates a new folder and remembers it.
     *
     * @param api The {@link UserApi}
     * @param parent The parent folder
     * @param permissions The permissions to set
     * @return The result of the operation
     * @throws ApiException if an API error is occurred
     */
    protected String createAndRememberNewFolder(UserApi api, String parent, List<FolderPermission> permissions) throws ApiException {

        NewFolderBodyFolder folderData = new NewFolderBodyFolder();
        folderData.setModule(EVENT_MODULE);
        folderData.setSubscribed(Boolean.TRUE);
        folderData.setTitle("chronos_test_" + new UID().toString());
        folderData.setPermissions(permissions);

        NewFolderBody body = new NewFolderBody();
        body.setFolder(folderData);

        FolderUpdateResponse createFolder = api.getFoldersApi().createFolder(parent, body, "0", CALENDAR_MODULE, null, null);
        checkResponse(createFolder.getError(), createFolder.getErrorDesc(), createFolder.getData());

        String result = createFolder.getData();
        rememberFolder(result);

        return result;
    }

    /**
     * Retrieves the default calendar folder of the current user
     *
     * @return The default calendar folder of the current user
     * @throws Exception if the default calendar folder cannot be found
     */
    protected String getDefaultFolder() throws Exception {
        return getDefaultFolder(defaultUserApi.getFoldersApi());
    }

    /**
     * Retrieves the default calendar folder of the user with the specified session
     *
     * @param client The {@link ApiClient}
     * @return The default calendar folder of the user
     * @throws Exception if the default calendar folder cannot be found
     */
    public String getDefaultFolder(ApiClient client) throws Exception {
        return getDefaultFolder(new FoldersApi(client));
    }

    /**
     * Gets the birthday calendar folder
     *
     * @throws Exception if the birthday calendar folder cannot be found
     */
    protected String getBirthdayCalendarFolder() throws Exception {
        return getBirthdayCalendarFolder(foldersApi);
    }

    /**
     * Optionally gets the birthday calendar folder.
     *
     * @param foldersApi The folders API to use
     * @return The identifier of the birthday calendar folder, or <code>null</code> if not found
     */
    protected String optBirthdayCalendarFolder(FoldersApi foldersApi) throws Exception {
        ArrayList<ArrayList<?>> folderArrays = getPrivateFolderList(foldersApi, EVENT_MODULE, "1,300,308,3203", "1");
        for (ArrayList<?> folderArray : folderArrays) {
            if (CalendarProviders.ID_BIRTHDAYS.equals(folderArray.get(3))) {
                return String.valueOf(folderArray.get(0));
            }
        }
        return null;
    }

    /**
     * Gets the birthday calendar folder
     *
     * @param foldersApi The folders API to use
     * @return The identifier of the birthday calendar folder
     * @throws Exception if the birthday calendar folder cannot be found
     */
    protected String getBirthdayCalendarFolder(FoldersApi foldersApi) throws Exception {
        String folderId = optBirthdayCalendarFolder(foldersApi);
        if (null == folderId) {
            throw new Exception("Unable to find birthdays calendar folder");
        }
        return folderId;
    }

    /**
     * Gets the default contact folder
     *
     * @return String The identifier of the default contact folder
     * @throws Exception if the default contact folder cannot be found
     */
    protected String getDefaultContactFolder() throws Exception {
        return getDefaultContactFolder(foldersApi);
    }

    /**
     * Gets the default contact folder
     *
     * @param foldersApi The {@link FoldersApi}
     * @return String The identifier of the default contact folder
     * @throws Exception if the default contact folder cannot be found
     */
    protected String getDefaultContactFolder(FoldersApi foldersApi) throws Exception {
        ArrayList<ArrayList<?>> folderArrays = getPrivateFolderList(foldersApi, CONTACT_MODULE, "1,300,302,308", "1");
        for (ArrayList<?> folderArray : folderArrays) {
            if (PrivateType.getInstance().getType() == ((Double) folderArray.get(2)).intValue() && Boolean.TRUE.equals(folderArray.get(3))) {
                return String.valueOf(folderArray.get(0));
            }
        }
        throw new Exception("Unable to find default contacts folder");
    }

    /**
     * Retrieves the default calendar folder of the user with the specified session
     *
     * @param foldersApi The {@link FoldersApi}
     * @return The default calendar folder of the user
     * @throws Exception if the default calendar folder cannot be found
     */
    protected String getDefaultFolder(FoldersApi foldersApi) throws Exception {
        ArrayList<ArrayList<?>> privateList = getPrivateFolderList(foldersApi, "event", "1,308", "0");
        assertThat("Unable to get any folders", privateList, is(not(nullValue())));
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
     * @param foldersApi The {@link FoldersApi} to use
     * @param module The folder module
     * @param columns The columns identifier
     * @param tree The folder tree identifier
     * @return List of available folders
     * @throws Exception if the api call fails
     */
    @SuppressWarnings({ "unchecked" })
    protected ArrayList<ArrayList<?>> getPrivateFolderList(FoldersApi foldersApi, String module, String columns, String tree) throws Exception {
        FoldersVisibilityResponse resp = foldersApi.getVisibleFolders(module, columns, tree, null, Boolean.TRUE);
        FoldersVisibilityData visibilityData = checkResponse(resp.getError(), resp.getErrorDesc(), resp.getCategories(), resp.getData());
        Object privateFolders = visibilityData.getPrivate();
        return (ArrayList<ArrayList<?>>) privateFolders;
    }

    /**
     * Sets the last timestamp
     *
     * @param timestamp the last timestamp to set
     */
    protected void setLastTimestamp(long timestamp) {
        this.lastTimeStamp = timestamp;
    }

    /**
     * Gets the last timestamp
     *
     * @return the last timestamp
     */
    protected long getLastTimestamp() {
        return lastTimeStamp;
    }

    /**
     * Changes the timezone of the default user to the given value
     *
     * @param tz The new timezone
     * @throws ApiException
     */
    protected void changeTimezone(TimeZone tz) throws ApiException {
        String body = "{timezone: \"" + tz.getID() + "\"}";
        CommonResponse updateJSlob = defaultUserApi.getJslob().updateJSlob("io.ox/core", body, null);
        assertNull(updateJSlob.getError(), updateJSlob.getErrorDesc());
    }

    /**
     * Generates an {@link UpdateEventBody}.
     *
     * @param eventData The {@link EventData} to update
     */
    protected UpdateEventBody getUpdateBody(EventData eventData) {
        UpdateEventBody body = new UpdateEventBody();
        body.setEvent(eventData);
        return body;
    }

    protected static List<EventData> getEventsByUid(List<EventData> events, String uid) {
        List<EventData> matchingEvents = new ArrayList<EventData>();
        if (null != events) {
            for (EventData event : events) {
                if (uid.equals(event.getUid())) {
                    matchingEvents.add(event);
                }
            }
        }
        matchingEvents.sort(new Comparator<EventData>() {

            @Override
            public int compare(EventData event1, EventData event2) {
                String recurrenceId1 = event1.getRecurrenceId();
                String recurrenceId2 = event2.getRecurrenceId();
                if (null == recurrenceId1) {
                    return null == recurrenceId2 ? 0 : -1;
                }
                if (null == recurrenceId2) {
                    return 1;
                }
                long dateTime1 = CalendarUtils.decode(recurrenceId1).getTimestamp();
                long dateTime2 = CalendarUtils.decode(recurrenceId2).getTimestamp();
                if (dateTime1 == dateTime2) {
                    return 0;
                }
                return dateTime1 < dateTime2 ? -1 : 1;
            }
        });
        return matchingEvents;
    }

    /**
     * Returns the id of the calendar user of the default user api
     *
     * @return The id of the calendar user
     */
    protected int getCalendaruser() {
        return i(defaultUserApi.getCalUser());
    }

    protected static Attendee getResourceAttendee(Integer resourceId) {
        return AttendeeFactory.createAttendee(resourceId, CuTypeEnum.RESOURCE);
    }

    protected static Attendee getUserAttendee(TestUser testUser) {
        return AttendeeFactory.createAttendee(I(testUser.getUserId()), CuTypeEnum.INDIVIDUAL);
    }

    protected static Attendee getExternalAttendee(TestUser testUser) {
        return AttendeeFactory.createAsExternal(testUser);
    }

    protected static Attendee getExternalAttendee(UserData userData) {
        return AttendeeFactory.createIndividual(userData.getEmail1());
    }

    protected static CalendarUser getCalendarUser(TestUser testUser) {
        CalendarUser calendarUser = new CalendarUser();
        calendarUser.setEntity(I(testUser.getUserId()));
        return calendarUser;
    }

}
