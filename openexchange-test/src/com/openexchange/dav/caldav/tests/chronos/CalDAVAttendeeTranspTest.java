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

package com.openexchange.dav.caldav.tests.chronos;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.testing.httpclient.models.EventData.TranspEnum.OPAQUE;
import static com.openexchange.testing.httpclient.models.EventData.TranspEnum.TRANSPARENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.ChronosUtils;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.dav.SyncToken;
import com.openexchange.dav.caldav.ICalResource;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm.TranspEnum;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link CalDAVAttendeeTranspTest}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class CalDAVAttendeeTranspTest extends ChronosCaldavTest {

    protected UserApi userApi2;
    protected EventManager eventManager2;
    protected String defaultFolderId2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        SessionAwareClient apiClient2 = testUser2.getApiClient();
        userApi2 = new UserApi(apiClient2, testUser2);
        defaultFolderId2 = getDefaultFolder(userApi2.getFoldersApi());
        eventManager2 = new EventManager(userApi2, defaultFolderId2);
    }

    @ParameterizedTest
    @MethodSource("availableAuthMethods")
    public void tesAttendeeTranspViaCalDAV(String authMethod) throws Exception {
        /*
         * Prepare OAuthClient if authMethod is OAuth
         */
        if (authMethod.equals("OAuth")) {
            prepareOAuthClient();
        }
        /*
         * Fetch sync token for later synchronization
         */
        String collectionName = encodeFolderID(getDefaultFolder());
        SyncToken syncToken = new SyncToken(fetchSyncToken(collectionName));

        /*
         * Create event with a second user
         */
        EventData event = EventFactory.createSingleTwoHourEvent(testUser2.getUserId(), "tesAttendeeTranspViaDAV", defaultFolderId2);
        event.setTransp(OPAQUE);
        event.attendees(AttendeeFactory.createIndividuals(testUser, testUser2));
        EventData createEvent = eventManager2.createEvent(event);
        /**
         * Check initial value for the second user
         */
        EventData firstUserEvent = eventManager.getEvent(defaultFolderId, createEvent.getId());
        Attendee attendee = ChronosUtils.find(firstUserEvent.getAttendees(), testUser.getUserId());
        assertThat(attendee, is(not(nullValue())));
        assertThat(firstUserEvent.getTransp(), is(OPAQUE));
        /*
         * Set transparency for the second user
         */
        AttendeeAndAlarm aaa = new AttendeeAndAlarm();
        aaa.attendee(attendee);
        aaa.setTransp(TranspEnum.TRANSPARENT);
        eventManager.updateAttendee(firstUserEvent.getId(), null, defaultFolderId, aaa, false);
        firstUserEvent = eventManager.getEvent(defaultFolderId, createEvent.getId());
        attendee = ChronosUtils.find(firstUserEvent.getAttendees(), testUser.getUserId());
        assertThat(attendee, is(not(nullValue())));
        assertThat(firstUserEvent.getTransp(), is(TRANSPARENT));
        /*
         * Verify appointment on client
         */
        Map<String, String> eTags = super.syncCollection(syncToken, collectionName).getETagsStatusOK();
        assertTrue(0 < eTags.size(), "no resource changes reported on sync collection");
        List<ICalResource> calendarData = super.calendarMultiget(collectionName, eTags.keySet());
        assertThat(I(calendarData.size()), is(I(1)));
        /*
         * Verify appointment on client, transparency of the event should be adjusted
         */
        ICalResource iCalResource = calendarData.get(0);
        assertThat(iCalResource.getVEvent(), is(not(nullValue())));
        assertThat(iCalResource.getVEvent().getUID(), is(createEvent.getUid()));
        assertThat(iCalResource.getVEvent().getTransp(), not(equalToIgnoringCase(createEvent.getTransp().toString())));
        assertThat(iCalResource.getVEvent().getTransp(), equalToIgnoringCase(TRANSPARENT.toString()));

    }
}
