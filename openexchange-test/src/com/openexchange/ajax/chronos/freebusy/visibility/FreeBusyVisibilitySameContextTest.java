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

import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.createAttendees;
import static com.openexchange.ajax.chronos.util.DateTimeUtil.formatZuluDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.CalendarFolderManager;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.java.Autoboxing;
import com.openexchange.java.util.UUIDs;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponse;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponseData;
import com.openexchange.testing.httpclient.models.CommonResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FreeBusyBody;
import com.openexchange.testing.httpclient.modules.JSlobApi;

/**
 *
 * {@link FreeBusyVisibilitySameContextTest}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 */
public class FreeBusyVisibilitySameContextTest extends AbstractFreeBusyVisibilityTest {

    private EventManager eventManagerTargetUser;

    private String defaultFolderIdTargetUser;

    private Calendar start;
    private Calendar end;

    private JSlobApi jSlobApiTargetUser;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        /*
         * Prepare events
         */
        start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        start.setTimeInMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
        end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1) + TimeUnit.HOURS.toMillis(1));
        /*
         * Prepare event manager for user 2
         */
        UserApi defaultUserApiTargetUser = new UserApi(testUser2.getApiClient(), testUser2);
        defaultFolderIdTargetUser = getDefaultFolder(testUser2.getApiClient());
        eventManagerTargetUser = new EventManager(defaultUserApiTargetUser, defaultFolderIdTargetUser);

        jSlobApiTargetUser = new JSlobApi(testUser2.getApiClient());
    }

    @Test
    public void testFreeBusy_attendeeInSameContextButDisabledFreeBusyVisibility() throws Exception {
        /*
         * set free/busy visibility to 'none' for user 2
         */
        CommonResponse response = jSlobApiTargetUser.setJSlob(Collections.singletonMap("chronos", Collections.singletonMap(FREE_BUSY_VISIBILITY, "none")), "io.ox/calendar", null);
        assertNull(response.getError());
        /*
         * create event as user 2
         */
        EventData singleEvent = EventFactory.createSingleEvent(testUser2.getUserId(), "my event for tomorrow", DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end));
        eventManagerTargetUser.createEvent(singleEvent, true); // Avoid conflict checks for more performance
        /*
         * lookup free/busy data as user 1
         */
        String from = formatZuluDate(start.getTime());
        String until = formatZuluDate(end.getTime());
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), testUser, testUser2));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        /*
         * verify that no data for user 2 is in response
         */
        assertNull(freeBusyResponse.getError());
        List<ChronosFreeBusyResponseData> data = freeBusyResponse.getData();
        assertNotNull(data);
        assertTrue(data.size() == 2);
        boolean userFound = false;
        for (ChronosFreeBusyResponseData attendeeData : data) {
            if (attendeeData.getAttendee().getEmail().equalsIgnoreCase(testUser2.getLogin())) { //user 1 from other context
                userFound = true;
                assertEquals(1, attendeeData.getWarnings().size());
                assertEquals(0, attendeeData.getFreeBusyTime().size());
                break;
            }
        }
        assertTrue(userFound, "Attendee information not in response!");
    }

    @Test
    public void testFreeBusy_disabledFreeBusyVisibilityButOrganizerIsAttendee() throws Exception {
        /*
         * set free/busy visibility to 'none' for user 2
         */
        CommonResponse response = jSlobApiTargetUser.setJSlob(Collections.singletonMap("chronos", Collections.singletonMap(FREE_BUSY_VISIBILITY, "none")), "io.ox/calendar", null);
        assertNull(response.getError());
        /*
         * create event as user 2, and invite user 1
         */
        EventData singleEvent = EventFactory.createSingleEvent(testUser2.getUserId(), "my event with Anton tomorrow", DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end));
        singleEvent.addAttendeesItem(AttendeeFactory.createIndividual(Autoboxing.I(testUser.getUserId())));
        eventManagerTargetUser.createEvent(singleEvent, true); // Avoid conflict checks for more performance
        /*
         * lookup free/busy data as user 1
         */
        String from = formatZuluDate(start.getTime());
        String until = formatZuluDate(end.getTime());
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), testUser, testUser2));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        /*
         * verify that data for user 2 is in response
         */
        assertNull(freeBusyResponse.getError());
        List<ChronosFreeBusyResponseData> data = freeBusyResponse.getData();
        assertNotNull(data);
        assertTrue(data.size() == 2);
        boolean userFound = false;
        for (ChronosFreeBusyResponseData attendeeData : data) {
            if (attendeeData.getAttendee().getEmail().equalsIgnoreCase(testUser2.getLogin())) {
                userFound = true;
                assertEquals(1, attendeeData.getWarnings().size());
                assertEquals(1, attendeeData.getFreeBusyTime().size());
                break;
            }
        }
        assertTrue(userFound, "Attendee information not in response!");
    }

    @Test
    public void testFreeBusy_organizerSetFreeBusyVisibilityToNone_noWarning() throws Exception {
        /*
         * set free/busy visibility to 'none' for user 1
         */
        JSlobApi jSlobApiOrganizer = new JSlobApi(testUser.getApiClient());
        CommonResponse response = jSlobApiOrganizer.setJSlob(Collections.singletonMap("chronos", Collections.singletonMap(FREE_BUSY_VISIBILITY, "none")), "io.ox/calendar", null);
        assertNull(response.getError());
        /*
         * create event as user 2, and invite user 1
         */
        EventData singleEvent = EventFactory.createSingleEvent(testUser2.getUserId(), "my event with Anton tomorrow", DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end));
        singleEvent.addAttendeesItem(AttendeeFactory.createIndividual(Autoboxing.I(testUser.getUserId())));
        eventManagerTargetUser.createEvent(singleEvent, true); // Avoid conflict checks for more performance
        /*
         * lookup free/busy data as user 1
         */
        String from = formatZuluDate(start.getTime());
        String until = formatZuluDate(end.getTime());
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), testUser, testUser2));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        /*
         * verify that data for both users are in response, as well as no warnings
         */
        assertNull(freeBusyResponse.getError());
        List<ChronosFreeBusyResponseData> data = freeBusyResponse.getData();
        assertNotNull(data);
        assertTrue(data.size() == 2);
        int noOfUsers = 2;
        for (ChronosFreeBusyResponseData attendeeData : data) {
            if (attendeeData.getAttendee().getEmail().equalsIgnoreCase(testUser.getLogin())) {
                noOfUsers--;
                assertNull(attendeeData.getWarnings());
                assertEquals(1, attendeeData.getFreeBusyTime().size());
                continue;
            }
            if (attendeeData.getAttendee().getEmail().equalsIgnoreCase(testUser2.getLogin())) {
                noOfUsers--;
                assertNull(attendeeData.getWarnings());
                assertEquals(1, attendeeData.getFreeBusyTime().size());
                continue;
            }
        }
        assertTrue(noOfUsers == 0, "Not all users found!");
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityWhenNotParticipating(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility & create an event for tomorrow w/o further attendees
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUser2.getApiClient()), freeBusyVisibility);
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting it to not show up if visibility is configured to 'none'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getUserAttendee(testUser2)).get(0);
        if ("none".equals(freeBusyVisibility)) {
            assertTrue(null == freeBusyData.getFreeBusyTime() || freeBusyData.getFreeBusyTime().isEmpty());
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertMatchingFreeBusyTime(freeBusyData, eventData);
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, create an event with user B for this timeslot, expecting a conflict being raised if visibility not configured to 'none'
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser));
        if ("none".equalsIgnoreCase(freeBusyVisibility)) {
            assertNotNull(eventManager.createEvent(conflictingEventData, false));
        } else {
            assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
        }
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityInSharedFolder(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility, share the personal folder to user A, and create an event for tomorrow w/o further attendees
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUser2.getApiClient()), freeBusyVisibility);
        CalendarFolderManager folderManager2 = new CalendarFolderManager(userApi2, userApi2.getFoldersApi());
        folderManager2.shareFolder(folderManager2.getFolder(defaultFolderId2), testUser);
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting the event always being visible, yet with an additional warning if configured to 'none'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getUserAttendee(testUser2)).get(0);
        assertMatchingFreeBusyTime(freeBusyData, eventData);
        if ("none".equals(freeBusyVisibility)) {
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, try and create an event with user B for this timeslot, always expecting a conflict being raised
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser));
        assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityWhenParticipating(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility and create an event for tomorrow w/ other attendee
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUser2.getApiClient()), freeBusyVisibility);
        EventData eventData = prepareEventData(defaultFolderId2, getCalendarUser(testUser2), getUserAttendee(testUser2), getUserAttendee(testUser));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting the event always being visible, yet with an additional warning if configured to 'none'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getUserAttendee(testUser2)).get(0);
        assertMatchingFreeBusyTime(freeBusyData, eventData);
        if ("none".equals(freeBusyVisibility)) {
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, try and create an event with user B for this timeslot, always expecting a conflict being raised
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser));
        assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityInPublicFolderWithoutAccess(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility, create a new (non shared) public folder, and create an event for tomorrow w/o further attendees
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUser2.getApiClient()), freeBusyVisibility);
        CalendarFolderManager folderManager2 = new CalendarFolderManager(userApi2, userApi2.getFoldersApi());
        String publicFolderId = folderManager2.createPublicCalendarFolder(UUIDs.getUnformattedStringFromRandom());
        EventData eventData = prepareEventData(publicFolderId, getCalendarUser(testUser2), getUserAttendee(testUser2));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting it to not show up if visibility is configured to 'none'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getUserAttendee(testUser2)).get(0);
        if ("none".equals(freeBusyVisibility)) {
            assertTrue(null == freeBusyData.getFreeBusyTime() || freeBusyData.getFreeBusyTime().isEmpty());
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertMatchingFreeBusyTime(freeBusyData, eventData);
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, create an event with user B for this timeslot, expecting a conflict being raised if visibility not configured to 'none'
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser));
        if ("none".equalsIgnoreCase(freeBusyVisibility)) {
            assertNotNull(eventManager.createEvent(conflictingEventData, false));
        } else {
            assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
        }
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityInPublicFolderWithAccess(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility, create a new public folder and grant access to user A, and create an event for tomorrow w/o further attendees
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUser2.getApiClient()), freeBusyVisibility);
        CalendarFolderManager folderManager2 = new CalendarFolderManager(userApi2, userApi2.getFoldersApi());
        String publicFolderId = folderManager2.createPublicCalendarFolder(UUIDs.getUnformattedStringFromRandom());
        folderManager2.shareFolder(folderManager2.getFolder(publicFolderId), testUser);
        EventData eventData = prepareEventData(publicFolderId, getCalendarUser(testUser2), getUserAttendee(testUser2));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting the event always being visible, yet with an additional warning if configured to 'none'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getUserAttendee(testUser2)).get(0);
        assertMatchingFreeBusyTime(freeBusyData, eventData);
        if ("none".equals(freeBusyVisibility)) {
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, try and create an event with user B for this timeslot, always expecting a conflict being raised
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser));
        assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
    }

    @ParameterizedTest
    @MethodSource("testedFreeBusyVisibilityValues")
    public void testFreeBusyVisibilityInPublicWhenParticipating(String freeBusyVisibility) throws Exception {
        /*
         * as user B, configure free/busy visibility, create a new (non shared) public folder, and create an event for tomorrow w/ other attendees
         */
        setAndCheckFreeBusyVisibility(new JSlobApi(testUser2.getApiClient()), freeBusyVisibility);
        CalendarFolderManager folderManager2 = new CalendarFolderManager(userApi2, userApi2.getFoldersApi());
        String publicFolderId = folderManager2.createPublicCalendarFolder(UUIDs.getUnformattedStringFromRandom());
        EventData eventData = prepareEventData(publicFolderId, getCalendarUser(testUser2), getUserAttendee(testUser2), getUserAttendee(testUser));
        EventData createdEvent = eventManager2.createEvent(eventData, true);
        /*
         * as user A, check free/busy of user B for this timeslot, expecting the event always being visible, yet with an additional warning if configured to 'none'
         */
        ChronosFreeBusyResponseData freeBusyData = queryFreeBusy(testUser.getApiClient(), createdEvent, getUserAttendee(testUser2)).get(0);
        assertMatchingFreeBusyTime(freeBusyData, eventData);
        if ("none".equals(freeBusyVisibility)) {
            assertWarning(freeBusyData, "CAL-40312");
        } else {
            assertTrue(null == freeBusyData.getWarnings() || freeBusyData.getWarnings().isEmpty());
        }
        /*
         * as user A, try and create an event with user B for this timeslot, always expecting a conflict being raised
         */
        EventData conflictingEventData = prepareEventData(defaultFolderId, getCalendarUser(testUser), getUserAttendee(testUser2), getUserAttendee(testUser));
        assertMatchingConflict(eventManager.getConflictsOnCreate(conflictingEventData), conflictingEventData);
    }

}
