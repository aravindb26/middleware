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

package com.openexchange.ajax.chronos.freebusy.visibility;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.framework.SessionAwareClient;
import com.openexchange.java.util.UUIDs;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.TestContextConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponseData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.modules.JSlobApi;

/**
 *
 * {@link FreeBusyVisibilityCrossContextTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class FreeBusyVisibilityCrossContextTest extends AbstractFreeBusyVisibilityTest {

    private TestUser testUserC2;
    private UserApi userApiC2;
    private String defaultFolderIdC2;
    private EventManager eventManagerC2;

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).withUserPerContext(3).withContextConfig(getContextConfig()).build();
    }

    private TestContextConfig getContextConfig() {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("com.openexchange.calendar.enableCrossContextFreeBusy", Boolean.TRUE.toString());
        configuration.put("com.openexchange.calendar.enableCrossContextConflicts", Boolean.TRUE.toString());
        return TestContextConfig.builder().withConfig(configuration).build();
    }

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        testUserC2 = testContextList.get(1).getRandomUser();
        SessionAwareClient apiClientC2 = testUserC2.getApiClient();
        userApiC2 = new UserApi(apiClientC2, testUserC2);
        defaultFolderIdC2 = getDefaultFolder(userApiC2.getFoldersApi());
        eventManagerC2 = new EventManager(userApiC2, defaultFolderId2);
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityInPersonalFolder(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility & create an event for tomorrow w/o further attendees
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUserC2.getApiClient()), freeBusyVisibility);
        EventData eventData = prepareEventData(defaultFolderIdC2, getCalendarUser(testUserC2), getUserAttendee(testUserC2));
        EventData createdEvent = eventManagerC2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting it to not show up if visibility is configured to 'none' or 'internal-only'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getExternalAttendee(testUserC2)).get(0);
        if ("none".equals(freeBusyVisibility) || "internal-only".equals(freeBusyVisibility)) {
            assertTrue(null == freeBusyData.getFreeBusyTime() || freeBusyData.getFreeBusyTime().isEmpty());
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertMatchingFreeBusyTime(freeBusyData, eventData);
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, create an event with user B for this timeslot, expecting a conflict being raised if visibility not configured to 'none' or 'internal-only'
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getExternalAttendee(testUserC2), getUserAttendee(testUser));
        if ("none".equals(freeBusyVisibility) || "internal-only".equals(freeBusyVisibility)) {
            assertNotNull(eventManager.createEvent(conflictingEventData, false));
        } else {
            assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
        }
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityInPublicFolder(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility, create a new public folder, and create an event for tomorrow w/o further attendees
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUserC2.getApiClient()), freeBusyVisibility);
        CalendarFolderManager folderManagerC2 = new CalendarFolderManager(userApiC2, userApiC2.getFoldersApi());
        String publicFolderId = folderManagerC2.createPublicCalendarFolder(UUIDs.getUnformattedStringFromRandom());
        EventData eventData = prepareEventData(publicFolderId, getCalendarUser(testUserC2), getUserAttendee(testUserC2));
        EventData createdEvent = eventManagerC2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting it to not show up if visibility is configured to 'none' or 'internal-only'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getExternalAttendee(testUserC2)).get(0);
        if ("none".equals(freeBusyVisibility) || "internal-only".equals(freeBusyVisibility)) {
            assertTrue(null == freeBusyData.getFreeBusyTime() || freeBusyData.getFreeBusyTime().isEmpty());
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertMatchingFreeBusyTime(freeBusyData, eventData);
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, create an event with user B for this timeslot, expecting a conflict being raised if visibility not configured to 'none' or 'internal-only'
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getExternalAttendee(testUserC2), getUserAttendee(testUser));
        if ("none".equals(freeBusyVisibility) || "internal-only".equals(freeBusyVisibility)) {
            assertNotNull(eventManager.createEvent(conflictingEventData, false));
        } else {
            assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
        }
    }

}
