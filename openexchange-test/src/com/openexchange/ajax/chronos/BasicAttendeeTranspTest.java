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

import static com.openexchange.ajax.chronos.factory.AttendeeFactory.createIndividuals;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.assertBusyTime;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.assertSingleBusyTime;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.checkSingleConflict;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.createAttendees;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.getFromDate;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.getUntilDate;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.FBType.BUSY;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.FBType.FREE;
import static com.openexchange.ajax.chronos.itip.ITipUtil.setAutoProcessing;
import static com.openexchange.ajax.chronos.util.ChronosUtils.find;
import static com.openexchange.java.Autoboxing.l;
import static com.openexchange.testing.httpclient.models.EventData.TranspEnum.OPAQUE;
import static com.openexchange.testing.httpclient.models.EventData.TranspEnum.TRANSPARENT;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.ChronosApiException;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestContext;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.AttendeeAndAlarm;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosConflictDataRaw;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponse;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventData.TranspEnum;
import com.openexchange.testing.httpclient.models.FreeBusyBody;
import com.openexchange.testing.httpclient.modules.JSlobApi;

/**
 * {@link BasicAttendeeTranspTest} - Tests to check the per-attendee transparency on events. This includes
 * <li>conflict checks</li>
 * <li>free/busy checks</li>
 * <li>cross-context setup</li>
 * <li>the per-attendee transparency on events</li>
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class BasicAttendeeTranspTest extends AbstractChronosTest {

    private EventManager eventManagerTargetUser;

    private String defaultFolderIdTargetUser;

    private TestUser freeBusyTarget;

    private TestContext targetContext;
    /*
     * ============================== Test configuration ==============================
     */

    @Override
    protected String getScope() {
        return "context";
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        HashMap<String, String> map = new HashMap<>(4, 0.9f);
        map.put("com.openexchange.calendar.enableCrossContextConflicts", Boolean.TRUE.toString());
        map.put("com.openexchange.calendar.enableCrossContextFreeBusy", Boolean.TRUE.toString());
        return map;
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withContexts(2).build();
    }

    /*
     * ============================== Tests ==============================
     */

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testChangeAttendeeTransp(boolean crossContext) throws Exception {
        EventData targetUserEvent = prepare(crossContext, OPAQUE);
        /*
         * Set all transparency values for the second user
         */
        for (TranspEnum t : TranspEnum.values()) {
            updateTransp(targetUserEvent, t);
        }
        updateTransp(targetUserEvent, null);

    }

    @Test
    public void testResettedAttendeeTransp() throws Exception {
        EventData targetUserEvent = prepare(false, OPAQUE);
        /*
         * Set transparency to opaque, update event and check that
         * attendee transparency has been resetted
         */
        updateTransp(targetUserEvent, OPAQUE);
        EventData delta = EventFactory.deltaOf(targetUserEvent);
        delta.setTransp(TRANSPARENT);
        targetUserEvent = eventManagerTargetUser.updateEvent(delta);
        assertThat(TRANSPARENT, is(targetUserEvent.getTransp()));
    }

    @Test
    public void testTryChangeAttendeeTranspByOrganizer() throws Exception {
        prepare(false, null);
        EventData event = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), BasicAttendeeTranspTest.class.getSimpleName() + "tesChangeAttendeeTranspByOrganizer");
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        EventData createdEvent = eventManager.createEvent(event, false);
        Attendee attendee = find(createdEvent.getAttendees(), freeBusyTarget.getUserId());
        assertThat(attendee, is(not(nullValue())));
        eventManagerTargetUser.setLastTimeStamp(eventManager.getLastTimeStamp());
        updateTransp(createdEvent, OPAQUE);
        eventManager.setLastTimeStamp(eventManagerTargetUser.getLastTimeStamp());
        /*
         * Update as organizer and check that the value wasn't change
         */
        try {
            AttendeeAndAlarm aaa = new AttendeeAndAlarm();
            aaa.attendee(attendee);
            aaa.setTransp(com.openexchange.testing.httpclient.models.AttendeeAndAlarm.TranspEnum.TRANSPARENT);
            eventManager.updateAttendee(createdEvent.getId(), null, defaultFolderId, aaa, true); // Must fail
            fail("No exception");
        } catch (ChronosApiException e) {
            assertThat(e.getMessage(), containsString("insufficient permission"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testChangeEventTransp(boolean crossContext) throws Exception {
        prepare(crossContext, null);
        EventData event = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), BasicAttendeeTranspTest.class.getSimpleName() + "testChangeEventTransp");
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        EventData createdEvent = eventManager.createEvent(event, false);
        /*
         * Change transparency for target attendee
         */
        EventData targetUserEvent = getSingleEvent(createdEvent.getUid());
        updateTransp(targetUserEvent, OPAQUE);
        /*
         * Update transparency as organizer
         */
        createdEvent = eventManager.getEvent(defaultFolderId, createdEvent.getId());
        event = EventFactory.deltaOf(createdEvent);
        event.setTransp(TRANSPARENT);
        createdEvent = eventManager.updateEvent(event);
        assertThat(createdEvent.getTransp(), is(TRANSPARENT));

        /*
         * Check transparency, only internal event should be updated
         */
        for (int i = 0; i < 10; i++) {
            try {
                targetUserEvent = eventManagerTargetUser.getEvent(defaultFolderIdTargetUser, targetUserEvent.getId());
                assertThat(targetUserEvent.getTransp(), is(TRANSPARENT));
            } catch (AssertionError e) {
                if (false == crossContext || i >= 10) {
                    throw e;
                }
                // Wait for change to be propagated through iMIP
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(i));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testChangeTransOnInstance(boolean crossContext) throws Exception {
        prepare(crossContext, null);
        EventData event = EventFactory.createSeriesEvent(freeBusyTarget.getUserId(), "BasicAttendeeTranpsTest" + UUID.randomUUID().toString(), 10, defaultFolderIdTargetUser);
        EventData targetUserEvent = eventManagerTargetUser.createEvent(event);
        assertThat(targetUserEvent.getTransp(), is(OPAQUE));
        assertThat(targetUserEvent.getChangeExceptionDates(), anyOf(nullValue(), empty()));
        assertTrue(targetUserEvent.getSeriesId().equals(targetUserEvent.getId()));
        /*
         * Set transparent value on instance that needs to be created
         */
        List<EventData> occurrences = getAllEvents(targetUserEvent.getUid());
        assertTrue(occurrences.size() > 1);
        EventData secondOccurence = occurrences.get(2);
        secondOccurence = updateTransp(secondOccurence, TRANSPARENT);
        assertFalse(secondOccurence.getId().equals(targetUserEvent.getId()));
        assertTrue(secondOccurence.getSeriesId().equals(targetUserEvent.getId()));
        /*
         * Re-check master event
         */
        targetUserEvent = eventManagerTargetUser.getEvent(defaultFolderIdTargetUser, targetUserEvent.getId());
        assertThat(targetUserEvent.getTransp(), is(OPAQUE));
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testConflictCheck(boolean crossContext) throws Exception {
        EventData targetUserEvent = prepare(crossContext, OPAQUE);
        /*
         * Try create an event with the target attendee, by default the event should conflict
         */
        EventData event = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), BasicAttendeeTranspTest.class.getSimpleName() + "secondEvent");
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        List<ChronosConflictDataRaw> conflicts = eventManager.getConflictsOnCreate(event);
        checkSingleConflict(conflicts, targetUserEvent, freeBusyTarget);
        /*
         * Set to transparent and check that no conflict is found
         */
        updateTransp(targetUserEvent, TRANSPARENT);
        eventManager.createEvent(event, false);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testConflictCheckOnTransparentEvent(boolean crossContext) throws Exception {
        EventData targetUserEvent = prepare(crossContext, TRANSPARENT);
        /*
         * Set to block and check that conflict is found
         */
        updateTransp(targetUserEvent, OPAQUE);
        /*
         * Try create an event with the target attendee, the event should conflict
         */
        EventData event = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), BasicAttendeeTranspTest.class.getSimpleName() + "Conflict on transparent event");
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        List<ChronosConflictDataRaw> conflicts = eventManager.getConflictsOnCreate(event);
        checkSingleConflict(conflicts, targetUserEvent, freeBusyTarget);
        /*
         * Change transparency to free and create
         */
        updateTransp(targetUserEvent, TRANSPARENT);
        eventManager.createEvent(event, false);
    }

    @ParameterizedTest
    @EnumSource(TranspEnum.class)
    public void testCrossContextConflictOnTransparentEvent(TranspEnum eventTransp) throws Exception {
        prepare(true, TRANSPARENT);
        /*
         * Prepare third user
         */
        TestUser thirdUser = targetContext.acquireUser();
        EventManager eventManagerThridUser;
        {
            UserApi api = new UserApi(thirdUser.getApiClient(), thirdUser);
            String folder = getDefaultFolder(thirdUser.getApiClient());
            eventManagerThridUser = new EventManager(api, folder);
        }
        /*
         * Create second event with another context intern user of the target user
         */
        EventData event = EventFactory.createSingleTwoHourEvent(freeBusyTarget.getUserId(), "CrossContext");
        event.setAttendees(createIndividuals(freeBusyTarget, thirdUser));
        event.setTransp(eventTransp);
        EventData targetUserEvent = eventManagerTargetUser.createEvent(event);
        /*
         * Try create event in other context with both cross-context users as attendees
         */
        EventData thirdUserEvent = eventManagerThridUser.getEvent(null, targetUserEvent.getId());
        updateTransp(eventManagerThridUser, thirdUser, thirdUserEvent, OPAQUE.equals(eventTransp) ? TRANSPARENT : OPAQUE);
        EventData eventToConflict = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), BasicAttendeeTranspTest.class.getSimpleName() + "Conflict on event for cross context");
        eventToConflict.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget, thirdUser));
        List<ChronosConflictDataRaw> conflicts = eventManager.getConflictsOnCreate(eventToConflict);
        if (OPAQUE.equals(eventTransp)) {
            // Event blocks, check that target user without attendee-transp is conflicting
            checkSingleConflict(conflicts, targetUserEvent, freeBusyTarget);
        } else {
            // Event is free, check that third user with specific attendee-transp is conflicting
            checkSingleConflict(conflicts, thirdUserEvent, thirdUser);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testNoConflictCheckOnOpaqueEvent(boolean crossContext) throws Exception {
        prepare(crossContext, null);
        EventData event = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), "BasicAttendeeTranpsTest" + UUID.randomUUID().toString(), defaultFolderId);
        event.setTransp(OPAQUE);
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        EventData createdEvent = eventManager.createEvent(event);
        /*
         * Set each attendees transparency to free
         */
        updateTransp(eventManager, testUser, createdEvent, TRANSPARENT);
        EventData targetUserEvent = getSingleEvent(createdEvent.getUid());
        updateTransp(targetUserEvent, TRANSPARENT);
        /*
         * Check that no conflict is shown
         */
        EventData eventNotToConflict = EventFactory.createSingleTwoHourEvent(freeBusyTarget.getUserId(), BasicAttendeeTranspTest.class.getSimpleName() + "No conflict on opaque event");
        eventNotToConflict.setAttendees(createAttendees(freeBusyTarget.getContextId(), testUser, freeBusyTarget));
        eventManagerTargetUser.createEvent(eventNotToConflict, false);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testFreeBusy(boolean crossContext) throws Exception {
        EventData targetUserEvent = prepare(crossContext, OPAQUE);
        /*
         * Check free/busy view, by default the event should block
         */
        String from = getFromDate(targetUserEvent);
        String until = getUntilDate(targetUserEvent);
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), freeBusyTarget));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        assertSingleBusyTime(freeBusyResponse, targetUserEvent, freeBusyTarget, BUSY);
        /*
         * Change transparency and check for "free" timeslot in free/busy
         */
        updateTransp(targetUserEvent, TRANSPARENT);
        freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        assertSingleBusyTime(freeBusyResponse, targetUserEvent, freeBusyTarget, FREE);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testFreeBusyOnTransparentEvent(boolean crossContext) throws Exception {
        EventData targetUserEvent = prepare(crossContext, TRANSPARENT);
        updateTransp(targetUserEvent, OPAQUE);
        /*
         * Check free/busy view, the event should block with the attendee transparency set
         */
        String from = getFromDate(targetUserEvent);
        String until = getUntilDate(targetUserEvent);
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), freeBusyTarget));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        assertSingleBusyTime(freeBusyResponse, targetUserEvent, freeBusyTarget, BUSY);
        /*
         * Remove transparency and check for no entry in free/busy
         */
        updateTransp(targetUserEvent, null);
        freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        assertSingleBusyTime(freeBusyResponse, targetUserEvent, freeBusyTarget, FREE);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testFreeBusyOnOpaqueEvent(boolean crossContext) throws Exception {
        EventData targetUserEvent = prepare(crossContext, OPAQUE);
        updateTransp(targetUserEvent, TRANSPARENT);
        /*
         * Check for entry in free/busy with transparent block
         */
        String from = getFromDate(targetUserEvent);
        String until = getUntilDate(targetUserEvent);
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), freeBusyTarget));
        assertSingleBusyTime(chronosApi.freebusy(from, until, freeBusyBody, null, null), targetUserEvent, freeBusyTarget, FREE);
        /*
         * remove per-attendee transparency, the event should block now
         */
        updateTransp(targetUserEvent, null);
        assertSingleBusyTime(chronosApi.freebusy(from, until, freeBusyBody, null, null), targetUserEvent, freeBusyTarget, BUSY);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testFreeBusyWithDifferentTransp(boolean crossContext) throws Exception {
        prepare(crossContext, null);
        EventData event = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), "BasicAttendeeTranpsTest" + UUID.randomUUID().toString(), defaultFolderId);
        event.setTransp(OPAQUE);
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        EventData createdEvent = eventManager.createEvent(event);
        EventData targetUserEvent = getSingleEvent(createdEvent.getUid());
        assertThat(targetUserEvent.getTransp(), is(OPAQUE));
        /*
         * Try different permutations of attendee transparency
         */
        assertTransparencyAndBusyTimes(OPAQUE, OPAQUE, createdEvent, targetUserEvent);

        updateTransp(eventManager, testUser, createdEvent, TRANSPARENT);
        assertTransparencyAndBusyTimes(TRANSPARENT, OPAQUE, createdEvent, targetUserEvent);

        EventData delta = EventFactory.deltaOf(createdEvent);
        delta.setTransp(TRANSPARENT);
        createdEvent = eventManager.updateEvent(delta);
        assertThat(createdEvent.getTransp(), is(TRANSPARENT));
        assertTransparencyAndBusyTimes(TRANSPARENT, TRANSPARENT, createdEvent, targetUserEvent);

        updateTransp(eventManager, testUser, createdEvent, OPAQUE);
        assertTransparencyAndBusyTimes(OPAQUE, TRANSPARENT, createdEvent, targetUserEvent);
        /*
         * Change as attendee
         */
        targetUserEvent = updateTransp(targetUserEvent, OPAQUE);
        assertTransparencyAndBusyTimes(OPAQUE, OPAQUE, createdEvent, targetUserEvent);

        updateTransp(eventManager, testUser, delta, null); // Use delta for the correct transparency
        assertTransparencyAndBusyTimes(TRANSPARENT, OPAQUE, createdEvent, targetUserEvent);

        delta = EventFactory.deltaOf(targetUserEvent);
        delta.setTransp(TRANSPARENT);
        targetUserEvent = updateTransp(delta, null); // Use delta for the correct transparency
        assertTransparencyAndBusyTimes(TRANSPARENT, TRANSPARENT, createdEvent, targetUserEvent);
    }

    /*
     * ============================== HELPERS ==============================
     */

    /**
     * Prepares for the test execution
     *
     * @param crossContext If the target user is from another context
     * @param trans The transparency of the event to create. If <code>null</code> no event will be created
     * @return The created event by the target user
     * @throws Exception
     */
    private EventData prepare(boolean crossContext, TranspEnum trans) throws Exception {
        setUpConfiguration();
        if (crossContext) {
            targetContext = testContextList.get(1);
            freeBusyTarget = targetContext.acquireUser();
            changeConfigWithOwnClient(freeBusyTarget);
            setAutoProcessing(new JSlobApi(testUser.getApiClient()), "always");
            setAutoProcessing(new JSlobApi(freeBusyTarget.getApiClient()), "always");
        } else {
            freeBusyTarget = testUser2;
            targetContext = testContext;
        }
        /*
         * Prepare event manager for target user
         */
        UserApi defaultUserApiTargetUser = new UserApi(freeBusyTarget.getApiClient(), freeBusyTarget);
        defaultFolderIdTargetUser = getDefaultFolder(freeBusyTarget.getApiClient());
        eventManagerTargetUser = new EventManager(defaultUserApiTargetUser, defaultFolderIdTargetUser);
        /*
         * Create event with the user
         */
        if (null != trans) {
            EventData event = EventFactory.createSingleTwoHourEvent(freeBusyTarget.getUserId(), "BasicAttendeeTranpsTest" + UUID.randomUUID().toString(), defaultFolderIdTargetUser);
            event.setTransp(trans);
            EventData targetUserEvent = eventManagerTargetUser.createEvent(event);
            assertThat(targetUserEvent.getTransp(), is(trans));
            return targetUserEvent;
        }
        return null;
    }

    /**
     * Updates the transparency for the {@link #freeBusyTarget} in the given event
     *
     * @param event The event to update
     * @param transp The transparency to set
     * @throws ChronosApiException In case of error
     * @throws ApiException In case of error
     * @return The updated event
     */
    private EventData updateTransp(EventData event, TranspEnum transp) throws ChronosApiException, ApiException {
        return updateTransp(eventManagerTargetUser, freeBusyTarget, event, transp);
    }

    private static EventData updateTransp(EventManager manager, TestUser target, EventData event, TranspEnum transp) throws ChronosApiException, ApiException {
        /*
         * Update transparency
         */
        Attendee attendee = find(event.getAttendees(), target.getUserId());
        AttendeeAndAlarm aaa = new AttendeeAndAlarm();
        aaa.attendee(attendee);
        aaa.setTransp(null == transp ? null : transp == TRANSPARENT ? com.openexchange.testing.httpclient.models.AttendeeAndAlarm.TranspEnum.TRANSPARENT : com.openexchange.testing.httpclient.models.AttendeeAndAlarm.TranspEnum.OPAQUE);
        CalendarResult result = manager.updateAttendee(event.getId(), event.getRecurrenceId(), null, aaa, false);
        /*
         * Get updated event
         */
        EventData updatedEvent;
        if (null != event.getRecurrenceId()) {
            Optional<EventData> any = result.getCreated().stream().filter(e -> event.getRecurrenceId().equals(e.getRecurrenceId())).findAny();
            if (any.isEmpty()) {
                any = result.getUpdated().stream().filter(e -> event.getRecurrenceId().equals(e.getRecurrenceId())).findAny();
            }
            updatedEvent = manager.getEvent(null, any.orElseThrow().getId());
        } else {
            updatedEvent = manager.getEvent(null, event.getId());
        }
        /*
         * Check that transparency was set, check flags, too
         */
        attendee = find(updatedEvent.getAttendees(), target.getUserId());
        assertThat(attendee, is(not(nullValue())));
        if (null == transp) {
            assertThat(updatedEvent.getTransp(), is(event.getTransp()));
        } else {
            assertThat(updatedEvent.getTransp(), is(transp));
        }
        if (TRANSPARENT.equals(transp)) {
            assertThat(updatedEvent.getFlags(), is(not(nullValue())));
            assertTrue(updatedEvent.getFlags().stream().filter(f -> "TRANSPARENT".equalsIgnoreCase(f.getValue())).findAny().isPresent());
        }
        return updatedEvent;
    }

    /**
     * Assert that the transparency and free/busy times are set correctly for the attendees in their event perspective
     *
     * @param user The transparency of the organizer
     * @param target The transparency of the attendee
     * @param userEvent The event view of the organizer
     * @param targetEvent The event view of the attendee
     * @throws Exception In case of error
     */
    private void assertTransparencyAndBusyTimes(TranspEnum user, TranspEnum target, EventData userEvent, EventData targetEvent) throws Exception {
        /*
         * Get the target user event, wait for changes to be applied on the server, i.e. wait for iMIP mail to be applied in the target users calendar
         */
        EventData loadedEvent = eventManagerTargetUser.getEvent(defaultFolderIdTargetUser, targetEvent.getId());
        int i = 0;
        while (l(loadedEvent.getTimestamp()) < l(userEvent.getTimestamp()) && i++ < 10) {
            loadedEvent = eventManagerTargetUser.getEvent(defaultFolderIdTargetUser, targetEvent.getId());
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(i));
        }
        /*
         * Check that transparencies are set correctly
         */
        assertThat(loadedEvent.getTransp(), is(target));
        userEvent = eventManager.getEvent(defaultFolderId, userEvent.getId());
        assertThat(userEvent.getTransp(), is(user));
        /*
         * Check free/busy times for the attendees, too
         */
        String from = getFromDate(userEvent);
        String until = getUntilDate(userEvent);
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        assertBusyTime(chronosApi.freebusy(from, until, freeBusyBody, null, null), userEvent, Map.of(testUser, OPAQUE.equals(user) ? BUSY : FREE, freeBusyTarget, OPAQUE.equals(target) ? BUSY : FREE));
    }

    /**
     * Get a single event with the given UID.
     * Fails if more than one event is found
     *
     * @param uid The UID to search for
     * @return The single event
     * @throws ApiException In case of error
     */
    private EventData getSingleEvent(String uid) throws ApiException {
        List<EventData> allEvents = getAllEvents(uid);
        assertTrue(1 == allEvents.size());
        return allEvents.get(0);
    }

    /**
     * Get all events by UID in the next week
     * <p>
     * Waits with back-off if no events can be found, to
     * e.g. wait for iMIP mails to be processed
     *
     * @param uid The UID of the event(s) to find
     * @return The events
     * @throws ApiException In case no event can be found
     */
    private List<EventData> getAllEvents(String uid) throws ApiException {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Berlin");
        Date from = CalendarUtils.truncateTime(new Date(), timeZone);
        Date until = CalendarUtils.add(from, Calendar.DATE, 7, timeZone);
        List<EventData> allEvents = null;
        for (int i = 0; i < 10; i++) {
            try {
                allEvents = eventManagerTargetUser.getAllEvents(defaultFolderIdTargetUser, from, until, true);
                if (null != allEvents && false == allEvents.isEmpty()) {
                    return getEventsByUid(allEvents, uid);
                }
                LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(i));
            } catch (Exception e) {
                Assertions.fail(e.getMessage());
            }
        }
        throw new ApiException("Unable to find any event in the next week");
    }

}
