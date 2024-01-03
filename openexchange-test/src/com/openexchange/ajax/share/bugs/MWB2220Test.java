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

package com.openexchange.ajax.share.bugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.share.ShareAPITest;
import com.openexchange.groupware.modules.Module;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.CalendarAccountProbeConfig;
import com.openexchange.testing.httpclient.models.CalendarAccountProbeData;
import com.openexchange.testing.httpclient.models.CalendarAccountProbeExtendedProperties;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FolderCalendarConfig;
import com.openexchange.testing.httpclient.models.FolderUpdateResponse;
import com.openexchange.testing.httpclient.models.FoldersVisibilityData;
import com.openexchange.testing.httpclient.models.FoldersVisibilityResponse;
import com.openexchange.testing.httpclient.models.NewFolderBody;
import com.openexchange.testing.httpclient.models.NewFolderBodyFolder;
import com.openexchange.testing.httpclient.models.PasswordChangeBody;
import com.openexchange.testing.httpclient.models.ShareLinkResponse;
import com.openexchange.testing.httpclient.models.ShareLinkUpdateBody;
import com.openexchange.testing.httpclient.models.ShareTargetData;
import com.openexchange.testing.httpclient.modules.FoldersApi;
import com.openexchange.testing.httpclient.modules.PasswordchangeApi;
import com.openexchange.testing.httpclient.modules.ShareManagementApi;

/**
 * {@link MWB2220Test}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class MWB2220Test extends ShareAPITest {

    private static final String EVENT_SUMMARY = "MWB-2220";

    private static final long START_IN_MILLIS = System.currentTimeMillis();

    private static final String SHARED_ICAL_PASSWORD = "test_12";

    private ShareManagementApi shareApi;
    private EventManager eventManager;
    private EventManager eventManager2;
    private UserApi userApi;
    private UserApi userApi2;
    private String defaultFolder;

    private final Calendar start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));;
    private final Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        changeConfigWithOwnClient(testUser2, ImmutableMap.of(//
            "com.openexchange.passwordchange.db.enabled", Boolean.TRUE.toString()));

        shareApi = new ShareManagementApi(getApiClient());
        userApi = new UserApi(testUser.getApiClient(), testUser);
        userApi2 = new UserApi(testUser2.getApiClient(), testUser2);

        defaultFolder = getDefaultFolder(userApi.getFoldersApi());
        eventManager = new EventManager(userApi, defaultFolder);
        eventManager2 = new EventManager(userApi2, defaultFolder);

        start.setTimeInMillis(START_IN_MILLIS);
        end.setTimeInMillis(START_IN_MILLIS + 100000);
        EventData event = EventFactory.createSingleEvent(testUser.getUserId(), EVENT_SUMMARY, DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end));
        eventManager.createEvent(event);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void testICalSubscriptionWithPasswordChange(boolean withPassword) throws Exception {
        String url = getCalendarShare(withPassword);

        probe(url, withPassword);
        FolderUpdateResponse addFolder = addFolder(url, withPassword);
        final EventData eventData = retrieveEventsFromFolder(addFolder.getData());

        PasswordChangeBody body2 = updatePassword();

        // Configure TestUser with new password
        testUser2 = new TestUser(testUser2.getUser(), testUser2.getContext(), body2.getNewPassword(), testUser2.getUserId(), testUser2.getContextId(), testUser2.getCreatedBy());
        userApi2 = new UserApi(testUser2.getApiClient(), testUser2);
        eventManager2 = new EventManager(userApi2, defaultFolder);

        // retrieve event from ical subscription after password change
        Thread.sleep(1000); // wait to let the CryptoService does his work
        List<EventData> allEventsNew = eventManager2.getAllEvents(start.getTime(), end.getTime(), true, addFolder.getData());
        assertEquals(1, allEventsNew.size());
        EventData eventDataNew = allEventsNew.get(0);
        assertEquals(eventData, eventDataNew);
    }

    private String getCalendarShare(boolean withPassword) throws ApiException {
        ShareTargetData shareTargetData = new ShareTargetData();
        shareTargetData.setModule(Module.CALENDAR.getName());
        shareTargetData.setFolder(defaultFolder);
        ShareLinkResponse getLinkResponse = shareApi.getShareLink(shareTargetData);
        ShareLinkUpdateBody update = new ShareLinkUpdateBody();
        if (withPassword) {
            update.setPassword(SHARED_ICAL_PASSWORD);
            update.setFolder(defaultFolder);
            update.setModule(Module.CALENDAR.getName());
            shareApi.updateShareLink(getLinkResponse.getTimestamp(), update);
        }
        return getLinkResponse.getData().getUrl();
    }

    private FolderUpdateResponse addFolder(String url, boolean withPassword) throws ApiException {
        FolderCalendarConfig config = new FolderCalendarConfig();
        if (withPassword) {
            config.setPassword(SHARED_ICAL_PASSWORD);
        }
        config.setUri(url);
        NewFolderBodyFolder folder = new NewFolderBodyFolder();
        folder.setComOpenexchangeCalendarConfig(config);
        folder.setComOpenexchangeCalendarProvider("ical");
        folder.setModule("event");
        folder.setSubscribed(Boolean.TRUE);
        folder.setTitle("the shared calendar from anton");

        NewFolderBody body = new NewFolderBody();
        body.setFolder(folder);
        return userApi2.getFoldersApi().createFolder(CalendarFolderManager.DEFAULT_FOLDER_ID, body, null, null, null, Boolean.TRUE);
    }

    private void probe(String url, boolean withPassword) throws Exception {
        CalendarAccountProbeData data = new CalendarAccountProbeData();
        data.setComOpenexchangeCalendarProvider(CalendarFolderManager.ICAL_ACCOUNT_PROVIDER_ID);

        CalendarAccountProbeConfig config = new CalendarAccountProbeConfig();
        config.setUri(url);
        if (withPassword) {
            config.setPassword(SHARED_ICAL_PASSWORD);
        }
        data.setComOpenexchangeCalendarConfig(config);
        data.setComOpenexchangeCalendarExtendedProperties(new CalendarAccountProbeExtendedProperties());

        userApi2.getChronosApi().probe(data);
    }

    private PasswordChangeBody updatePassword() throws ApiException {
        PasswordchangeApi passwordchangeApi2 = new PasswordchangeApi(userApi2.getClient());
        PasswordChangeBody body2 = new PasswordChangeBody();
        body2.setNewPassword(testUser2.getPassword() + "12");
        body2.setOldPassword(testUser2.getPassword());
        passwordchangeApi2.updatePassword(body2);
        return body2;
    }

    private EventData retrieveEventsFromFolder(final String folderId) throws ApiException {
        start.setTimeInMillis(START_IN_MILLIS - DateUtils.MILLIS_PER_DAY);
        end.setTimeInMillis(START_IN_MILLIS + DateUtils.MILLIS_PER_DAY);
        final List<EventData> allEvents = eventManager2.getAllEvents(start.getTime(), end.getTime(), true, folderId);
        assertEquals(1, allEvents.size());
        final EventData eventData = allEvents.get(0);
        assertEquals(EVENT_SUMMARY, eventData.getSummary());
        return eventData;
    }

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

    @SuppressWarnings({ "unchecked" })
    protected ArrayList<ArrayList<?>> getPrivateFolderList(FoldersApi foldersApi, String module, String columns, String tree) throws Exception {
        FoldersVisibilityResponse resp = foldersApi.getVisibleFolders(module, columns, tree, null, Boolean.TRUE);
        FoldersVisibilityData visibilityData = checkResponse(resp.getError(), resp.getErrorDesc(), resp.getCategories(), resp.getData());
        Object privateFolders = visibilityData.getPrivate();
        return (ArrayList<ArrayList<?>>) privateFolders;
    }
}
