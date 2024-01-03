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

package com.openexchange.ajax.chronos.freebusy;

import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.checkConflict;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.checkSingleConflict;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.createAttendees;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.getFromDate;
import static com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.getUntilDate;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.factory.EventFactory.RecurringFrequency;
import com.openexchange.ajax.chronos.factory.RRuleFactory;
import com.openexchange.ajax.chronos.freebusy.FreeBusyUtils.FBType;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.ChronosConflictDataRaw;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponse;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponseData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventData.TranspEnum;
import com.openexchange.testing.httpclient.models.FreeBusyBody;
import com.openexchange.testing.httpclient.models.FreeBusyTime;

/**
 *
 * {@link FreeBusyAndConflictTest} - Free/busy and conflict tests, tests cross-context lookups, too.
 * Ensures equal responses even tough cross context lookups might have been performed
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class FreeBusyAndConflictTest extends AbstractChronosTest {

    /*
     * ============================== Test configuration ==============================
     */

    protected EventManager eventManagerTargetUser;

    private String defaultFolderIdTargetUser;

    private TestUser freeBusyTarget;

    private void prepare(boolean crossContext) throws Exception {
        setUpConfiguration();
        if (crossContext) {
            freeBusyTarget = testContextList.get(1).acquireUser();
            changeConfigWithOwnClient(freeBusyTarget);
        } else {
            freeBusyTarget = testUser2;
        }
        /*
         * Prepare event manager for user 2
         */
        UserApi defaultUserApiTargetUser = new UserApi(freeBusyTarget.getApiClient(), freeBusyTarget);
        defaultFolderIdTargetUser = getDefaultFolder(freeBusyTarget.getApiClient());
        eventManagerTargetUser = new EventManager(defaultUserApiTargetUser, defaultFolderIdTargetUser);
    }

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
    public void testConflict_noEntry(boolean crossContext) throws Exception {
        prepare(crossContext);
        EventData event = EventFactory.createSingleTwoHourEvent(testUser.getUserId(), FreeBusyAndConflictTest.class.getSimpleName() + "testConflict_noEntry");
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        EventData createEvent = eventManager.createEvent(event, false);
        assertTrue(createEvent.getAttendees().size() == 2, "Not all attendees created:" + createEvent.getAttendees());
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testFreeBusy_noEntry(boolean crossContext) throws Exception {
        prepare(crossContext);
        String from = getFromDate();
        String until = getUntilDate();
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        assertNull(freeBusyResponse.getError(), freeBusyResponse.getError());
        List<ChronosFreeBusyResponseData> data = freeBusyResponse.getData();
        assertNotNull(data);
        assertTrue(data.size() == 2);
        for (ChronosFreeBusyResponseData attendeeData : data) {
            assertTrue(null == attendeeData.getFreeBusyTime() || attendeeData.getFreeBusyTime().isEmpty());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testConflict_OneEntry(boolean crossContext) throws Exception {
        prepare(crossContext);
        /*
         * Create event for the attendee
         */
        EventData event = EventFactory.createSingleTwoHourEvent(freeBusyTarget.getUserId(), FreeBusyAndConflictTest.class.getSimpleName() + "testConflict_OneEntry");
        EventData createdEvent = eventManagerTargetUser.createEvent(event);
        /*
         * Create event with that attendee and check conflict
         */
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        List<ChronosConflictDataRaw> conflicts = eventManager.getConflictsOnCreate(event);
        checkSingleConflict(conflicts, createdEvent, freeBusyTarget);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testConflict_OneOverlapping_noConflict(boolean crossContext) throws Exception {
        prepare(crossContext);
        /*
         * Create event for the attendee
         */
        EventData event = EventFactory.createSingleTwoHourEvent(freeBusyTarget.getUserId(), FreeBusyAndConflictTest.class.getSimpleName() + "testConflict_OneEntry");
        eventManagerTargetUser.createEvent(event);
        /*
         * Create the event, which touches the event, but don't actually overlaps
         */
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        event.setStartDate(event.getEndDate());
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(end.getTimeInMillis() + TimeUnit.HOURS.toMillis(4));
        event.setEndDate(DateTimeUtil.getDateTime(end));
        eventManager.createEvent(event);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testFreeBusy_oneEntry(boolean crossContext) throws Exception {
        prepare(crossContext);
        /*
         * Create event for the attendee
         */
        EventData event = EventFactory.createSingleTwoHourEvent(freeBusyTarget.getUserId(), FreeBusyAndConflictTest.class.getSimpleName() + "testFreeBusy_oneEntry");
        EventData createdEvent = eventManagerTargetUser.createEvent(event);
        /*
         * Check free/busy for both attendees
         */
        String from = getFromDate(createdEvent);
        String until = getUntilDate(createdEvent);
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        List<ChronosFreeBusyResponseData> data = checkResponse(freeBusyResponse.getError(), freeBusyResponse.getErrorDesc(), freeBusyResponse.getData());
        assertThat(data, is(not(empty())));
        assertTrue(data.size() == 2);
        for (ChronosFreeBusyResponseData freebusy : data) {
            assertNotNull(freebusy.getAttendee());
            Attendee busyAttendee = freebusy.getAttendee();
            List<FreeBusyTime> busyTime = freebusy.getFreeBusyTime();
            if (null != busyAttendee.getEntity() && testUser.getUserId() == i(busyAttendee.getEntity())) {
                assertThat("Busy time found for acting user", busyTime, is(empty()));
            } else {
                assertNotNull(busyTime);
                assertTrue(busyTime.size() == 1, "Not all busy times found:" + busyTime);
                FreeBusyTime freeBusyTime = busyTime.get(0);
                assertThat(freeBusyTime.getStartTime(), is(L(DateTimeUtil.parseDateTime(createdEvent.getStartDate()).getTime())));
                assertTrue(FBType.BUSY.matches(freeBusyTime.getFbType()));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testSeriesConflict_withSingleEvent(boolean crossContext) throws Exception {
        prepare(crossContext);
        /*
         * Create series event for the attendee
         */
        EventData event = EventFactory.createSeriesEvent(freeBusyTarget.getUserId(), FreeBusyAndConflictTest.class.getSimpleName() + "testSeriesConflict_withSingleEvent", 5, null);
        EventData createdEvent = eventManagerTargetUser.createEvent(event);
        /*
         * Create single event with that attendee and check conflict
         */
        event = new EventData();
        event.setTransp(TranspEnum.OPAQUE);
        event.setStartDate(createdEvent.getStartDate());
        event.setEndDate(createdEvent.getEndDate());
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        List<ChronosConflictDataRaw> conflicts = eventManager.getConflictsOnCreate(event);
        checkSingleConflict(conflicts, event, freeBusyTarget);
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testSeriesFreeBusy_twoEntries(boolean crossContext) throws Exception {
        prepare(crossContext);
        /*
         * Create event for the attendee
         */
        EventData event = EventFactory.createSeriesEvent(freeBusyTarget.getUserId(), FreeBusyAndConflictTest.class.getSimpleName() + "testSeriesFreeBusy_twoEntries", 5, null);
        EventData createSeries = eventManagerTargetUser.createEvent(event);
        /*
         * Check free/busy for both attendees
         */
        String from = getFromDate(createSeries);
        String until = getUntilDate(createSeries);
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, null);
        assertNull(freeBusyResponse.getError(), freeBusyResponse.getError());
        List<ChronosFreeBusyResponseData> data = freeBusyResponse.getData();
        assertThat(data, is(not(empty())));
        assertTrue(data.size() == 2);
        for (ChronosFreeBusyResponseData freebusy : data) {
            assertNotNull(freebusy.getAttendee());
            Attendee busyAttendee = freebusy.getAttendee();
            List<FreeBusyTime> busyTime = freebusy.getFreeBusyTime();
            if (null != busyAttendee.getEntity() && testUser.getUserId() == i(busyAttendee.getEntity())) {
                assertThat("Busy time found for acting user", busyTime, is(empty()));
            } else {
                assertNotNull(busyTime);
                assertTrue(busyTime.size() == 2, "Not all busy times found:" + busyTime); //daily series with 5 occurrences, requested only 3 days, yesterday no event
                for (FreeBusyTime time : busyTime) {
                    assertTrue(FBType.BUSY.matches(time.getFbType()));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans =
    { true, false })
    public void testSeriesConflict_withSeriesEvent(boolean crossContext) throws Exception {
        prepare(crossContext);
        /*
         * Create event for the attendee
         */
        EventData event = EventFactory.createSeriesEvent(freeBusyTarget.getUserId(), FreeBusyAndConflictTest.class.getSimpleName() + "testSeriesConflict_withSeriesEvent", 10, null);
        eventManagerTargetUser.createEvent(event);
        /*
         * Create series event with 5 occurrences to conflict
         */
        int occurencesNewSeries = 5;
        event.setAttendees(createAttendees(testUser.getContextId(), testUser, freeBusyTarget));
        event.setRrule(RRuleFactory.getFrequencyWithOccurenceLimit(RecurringFrequency.DAILY, occurencesNewSeries));
        List<ChronosConflictDataRaw> conflicts = eventManager.getConflictsOnCreate(event);
        assertTrue(conflicts.size() == occurencesNewSeries, "Not all attendee conflicts found:" + conflicts);
        for (ChronosConflictDataRaw conflict : conflicts) {
            checkConflict(conflict, freeBusyTarget);
        }
    }



}
