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

package com.openexchange.ajax.appointment;

import static com.openexchange.ajax.chronos.factory.EventFactory.prepareDeltaEvent;
import static com.openexchange.java.Autoboxing.L;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import org.dmfs.rfc5545.DateTime;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.java.util.Pair;
import com.openexchange.testing.httpclient.models.CalendarResult;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.DateTimeData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.UpdateEventBody;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * {@link RecurrenceRuleDeviatationTest} Testclass to check the correct adjustment of events with different recurrence rules while creating or updating them.
 *
 * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
 * @since v8.0.0
 */
public class RecurrenceRuleDeviatationTest extends AbstractChronosTest {

    private EventData createdEvent;

    private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Europe/Berlin");

    /**
     * {@link TestData} is a wrapper for test data
     *
     * @param rrule The recurrence rule
     * @param expectedOriginalDates The original start and end date
     * @param expectedUpdateDates The updated start and end date
     * @author <a href="mailto:marcel.broecher@open-xchange.com">Marcel Broecher</a>
     * @since v8
     */
    final record TestData(String rrule, Pair<DateTime, DateTime> expectedOriginalDates, Pair<DateTime, DateTime> expectedUpdateDates, Pair<DateTime, DateTime> initialDatesCreateWithChange) {

        static final DateTime INITIAL_START_DATE = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 4, 10, 15, 0);
        static final DateTime UPDATE_START_DATE = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 2, 11, 0, 0);
        static final DateTime INITIAL_END_DATE = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 4, 10, 30, 0);
        static final DateTime UPDATE_END_DATE = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 2, 15, 0, 0);
        static final Pair<DateTime, DateTime> INITIAL_EVENT_TIME = new Pair<>(INITIAL_START_DATE, INITIAL_END_DATE);
        static final Pair<DateTime, DateTime> UPDATE_EVENT_TIME = new Pair<>(UPDATE_START_DATE, UPDATE_END_DATE);
    }

    /**
     * Gets test parameters
     *
     * @return The test parameters
     */
    protected static Iterable<TestData> testConfigs() {
        List<TestData> testConfig = new ArrayList<>(4);

        //createSeriesWithChangeExceptionTest() needs a different initial start and end date to work correctly
        DateTime initialStartDateCreateWithChange = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 03, 10, 15, 0);
        DateTime initialEndDateCreateWithChange = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 03, 10, 30, 0);
        Pair<DateTime, DateTime> initialDatesCreateWithChange = new Pair<>(initialStartDateCreateWithChange, initialEndDateCreateWithChange);

        //test config for test 1
        String recurrenceRule1 = "FREQ=WEEKLY;BYWEEKNO=1;BYDAY=TU;UNTIL=20270508T205959Z";
        DateTime expectedOriginalStartDate = new DateTime(TIME_ZONE, 2024, Calendar.JANUARY, 02, 10, 15, 0);
        DateTime expectedOriginalEndDate = new DateTime(TIME_ZONE, 2024, Calendar.JANUARY, 02, 10, 30, 0);
        DateTime expectedUpdateStartDate = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 03, 11, 0, 0);
        DateTime expectedUpdateEndDate = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 03, 15, 0, 0);
        Pair<DateTime, DateTime> expectedOriginalDates = new Pair<>(expectedOriginalStartDate, expectedOriginalEndDate);
        Pair<DateTime, DateTime> expectedUpdateDates = new Pair<>(expectedUpdateStartDate, expectedUpdateEndDate);
        TestData testConfig1 = new TestData(recurrenceRule1, expectedOriginalDates, expectedUpdateDates, initialDatesCreateWithChange);
        testConfig.add(testConfig1);

        //test config for test 2
        String recurrenceRule2 = "FREQ=WEEKLY;BYDAY=TU;UNTIL=20270508T205959Z";
        DateTime expectedOriginalStartDate2 = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 10, 10, 15, 0);
        DateTime expectedOriginalEndDate2 = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 10, 10, 30, 0);
        Pair<DateTime, DateTime> expectedOriginalDates2 = new Pair<>(expectedOriginalStartDate2, expectedOriginalEndDate2);
        TestData testConfig2 = new TestData(recurrenceRule2, expectedOriginalDates2, expectedUpdateDates, initialDatesCreateWithChange);
        testConfig.add(testConfig2);

        //test config for test 3
        String recurrenceRule3 = "FREQ=MONTHLY;BYMONTHDAY=3;UNTIL=20270508T205959Z";
        DateTime expectedOriginalStartDate3 = new DateTime(TIME_ZONE, 2023, Calendar.FEBRUARY, 3, 10, 15, 0);
        DateTime expectedOriginalEndDate3 = new DateTime(TIME_ZONE, 2023, Calendar.FEBRUARY, 3, 10, 30, 0);
        Pair<DateTime, DateTime> expectedOriginalDates3 = new Pair<>(expectedOriginalStartDate3, expectedOriginalEndDate3);
        TestData testConfig3 = new TestData(recurrenceRule3, expectedOriginalDates3, expectedUpdateDates, initialDatesCreateWithChange);
        testConfig.add(testConfig3);

        //test config for test 4
        String recurrenceRule4 = "FREQ=YEARLY;BYYEARDAY=3;UNTIL=20270508T205959Z";
        DateTime expectedOriginalStartDate4 = new DateTime(TIME_ZONE, 2024, Calendar.JANUARY, 3, 10, 15, 0);
        DateTime expectedOriginalEndDate4 = new DateTime(TIME_ZONE, 2024, Calendar.JANUARY, 3, 10, 30, 0);
        Pair<DateTime, DateTime> expectedOriginalDates4 = new Pair<>(expectedOriginalStartDate4, expectedOriginalEndDate4);
        TestData testConfig4 = new TestData(recurrenceRule4, expectedOriginalDates4, expectedUpdateDates, initialDatesCreateWithChange);
        testConfig.add(testConfig4);

        return testConfig;
    }

    /**
     * Test to check the start and end date adjustment of a created or updated event for different recurrence rules.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("testConfigs")
    public void testDifferentRecurrenceRules(TestData testData) throws Exception {
        EventData OriginalEvent = createSeries(testData, false, false);
        EventData UpdateEvent = updateSeries(testData, false, false);

        assertTrue(checkDates(OriginalEvent, testData.expectedOriginalDates()), "The description does not contain the right original date.");
        assertTrue(checkDates(UpdateEvent, testData.expectedUpdateDates()), "The description does not contain the right updated date.");
    }

    /**
     * Test to check the start and end date adjustment for different recurrence rules while updating the series with a deleteExceptionDate
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("testConfigs")
    public void testUpdateSeriesWithDeleteException(TestData testData) throws Exception {
        EventData OriginalEvent = createSeries(testData, false, false);
        EventData UpdateEvent = updateSeries(testData, true, false);

        assertTrue(checkDates(OriginalEvent, testData.expectedOriginalDates()), "The description does not contain the right original date.");
        assertTrue(checkDates(UpdateEvent, testData.expectedUpdateDates()), "The description does not contain the right updated date.");
    }

    /**
     * Test to check the start and end date adjustment for different recurrence rules while updating the series with a changeExceptionDate
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("testConfigs")
    public void testUpdateSeriesWithChangeException(TestData testData) throws Exception {
        EventData OriginalEvent = createSeries(testData, false, false);
        EventData UpdateEvent = updateSeries(testData, false, true);

        assertTrue(checkDates(OriginalEvent, testData.expectedOriginalDates()), "The description does not contain the right original date.");
        assertTrue(checkDates(UpdateEvent, testData.expectedUpdateDates()), "The description does not contain the right updated date.");
    }

    /**
     * Test to check the start and end date adjustment for different recurrence rules while creating the series with a deleteExceptionDate
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("testConfigs")
    public void testCreateSeriesWithDeleteException(TestData testData) throws Exception {
        EventData OriginalEvent = createSeries(testData, true, false);
        EventData UpdateEvent = updateSeries(testData, false, false);

        assertTrue(checkDates(OriginalEvent, testData.expectedOriginalDates()), "The description does not contain the right original date.");
        assertTrue(checkDates(UpdateEvent, testData.expectedUpdateDates()), "The description does not contain the right updated date.");
    }

    /**
     * Test to check the start and end date adjustment for different recurrence rules while creating the series with a changeExceptionDate.
     * checkDates() for the OriginalEvent isn't necessary here, because for this test case the initial start and end date matches the recurrence rule anyway.
     *
     * @throws Exception
     */
    @ParameterizedTest
    @MethodSource("testConfigs")
    public void testCreateSeriesWithChangeException(TestData testData) throws Exception {
        createSeries(testData, false, true);
        EventData UpdateEvent = updateSeries(testData, false, false);

        assertTrue(checkDates(UpdateEvent, testData.expectedUpdateDates()), "The description does not contain the right updated date.");
    }

    // ------------------------- helper methods --------------------------

    /**
     * Check if the start and end date of a given event matches the expected start and end date.
     *
     * @param event The event to be checked
     * @param eventTime the expected start and end date
     * @return <code>true<code> if the dates of the event match the expectations, return <code>false<code> otherwise
     */
    private boolean checkDates(EventData event, Pair<DateTime, DateTime> eventTime) {
        String actualStartDate = event.getStartDate().getValue();
        String actualEndDate = event.getEndDate().getValue();

        String specifiedStartDate = DateTimeUtil.getZuluDateTime(eventTime.getFirst().getTimestamp()).getValue();
        String specifiedEndDate = DateTimeUtil.getZuluDateTime(eventTime.getSecond().getTimestamp()).getValue();

        return (actualStartDate.equals(specifiedStartDate) && actualEndDate.equals(specifiedEndDate));
    }

    /**
     * Creates an event series with the specified start and end date and the given recurrence rule
     * additionally adds a delete or changeExceptionDate to the event if specified
     *
     * @param testData the test data containing the start and end date specification
     * @param deleteException determines if a deleteExceptionDate should be set
     * @param changeException determines if a changeExceptionDate should be set
     * @return the created event series as <code>EventData<code>
     * @throws Exception
     */
    private EventData createSeries(TestData testData, boolean deleteException, boolean changeException) throws Exception {
        List<String> exceptionList = new ArrayList<String>();
        Pair<DateTime, DateTime> eventTime = TestData.INITIAL_EVENT_TIME;
        DateTimeData startDateData = DateTimeUtil.getZuluDateTime(eventTime.getFirst().getTimestamp());
        DateTimeData endDateData = DateTimeUtil.getZuluDateTime(eventTime.getSecond().getTimestamp());

        String summary = this.getClass().getName() + " " + UUID.randomUUID().toString();

        EventData event = new EventData();

        if (deleteException) {
            exceptionList.add(DateTimeUtil.getZuluDateTime(testData.expectedOriginalDates().getFirst().getTimestamp()).getValue());
            event = EventFactory.createSeriesEventWithDeleteExceptions(getUserId(), summary, startDateData, endDateData, testData.rrule(), defaultFolderId, exceptionList);
            createdEvent = eventManager.createEvent(event);
        } else if (changeException) {
            startDateData = DateTimeUtil.getZuluDateTime(testData.initialDatesCreateWithChange.getFirst().getTimestamp());
            endDateData = DateTimeUtil.getZuluDateTime(testData.initialDatesCreateWithChange.getSecond().getTimestamp());
            event = EventFactory.createSeriesEvent(getUserId(), summary, startDateData, endDateData, testData.rrule(), defaultFolderId);
            createdEvent = eventManager.createEvent(event);

            DateTime updateStartTime = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 4, 13, 15, 0);
            DateTime updateEndTime = new DateTime(TIME_ZONE, 2023, Calendar.JANUARY, 4, 13, 30, 0);
            Pair<DateTime, DateTime> update = new Pair<>(updateStartTime, updateEndTime);
            updateFirstOccurrence(testData, update);
        } else {
            event = EventFactory.createSeriesEvent(getUserId(), summary, startDateData, endDateData, testData.rrule(), defaultFolderId);
            createdEvent = eventManager.createEvent(event);
        }

        assertEquals(event.getRrule(), createdEvent.getRrule(), "RRule not equals");
        return createdEvent;
    }

    /**
     * Performs an update regarding start and end date for the test series, additionally adds a delete or changeExceptionDate
     * to the event if specified
     *
     * @param testData the test data containing the start and end date specification
     * @param deleteException determines if a deleteExceptionDate should be set
     * @param changeException determines if a changeExceptionDate should be set
     * @return the updated event series as <code>EventData<code>
     * @throws Exception
     */
    private EventData updateSeries(TestData testData, boolean deleteException, boolean changeException) throws Exception {
        Pair<DateTime, DateTime> eventTime = TestData.UPDATE_EVENT_TIME;
        List<String> exceptionList = new ArrayList<String>();
        /*
         * Update a series as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setDescription("Totally new description for " + this.getClass().getName());

        UpdateEventBody body = getUpdateBody(deltaEvent);

        deltaEvent = prepareDeltaEvent(createdEvent);

        deltaEvent.setStartDate(DateTimeUtil.getZuluDateTime(eventTime.getFirst().getTimestamp()));
        deltaEvent.setEndDate(DateTimeUtil.getZuluDateTime(eventTime.getSecond().getTimestamp()));

        if (deleteException) {
            exceptionList.add(DateTimeUtil.getZuluDateTime(testData.expectedUpdateDates().getFirst().getTimestamp()).getValue());
            deltaEvent.setDeleteExceptionDates(exceptionList);
        }

        if (changeException) {
            exceptionList.add(DateTimeUtil.getZuluDateTime(testData.expectedUpdateDates().getFirst().getTimestamp()).getValue());
            deltaEvent.setChangeExceptionDates(exceptionList);
        }

        body = getUpdateBody(deltaEvent);

        ChronosCalendarResultResponse result = chronosApi.updateEventBuilder()
                                                         .withFolder(defaultFolderId)
                                                         .withId(createdEvent.getId())
                                                         .withTimestamp(Long.valueOf(System.currentTimeMillis()))
                                                         .withUpdateEventBody(body)
                                                         .withRecurrenceRange(THIS_AND_FUTURE)
                                                         .execute();

        /*
         * Check result
         */
        assertNotNull(result);
        CalendarResult calendarResult = checkResponse(result.getError(), result.getErrorDesc(), "WARNING", result.getData());
        assertTrue(calendarResult.getUpdated().size() == 1);
        createdEvent = calendarResult.getUpdated().get(0);

        return createdEvent;
    }

    /**
     * Updates the first occurrence of an event series.
     *
     * @param testData the test data containing the information about the recurrenceId of the first event
     * @param update the start and end date of the updated event
     * @throws Exception
     */
    private void updateFirstOccurrence(TestData testData, Pair<DateTime, DateTime> update) throws Exception {
        /*
         * Update a single occurrence as organizer
         */
        EventData deltaEvent = prepareDeltaEvent(createdEvent);
        deltaEvent.setDescription("Totally new description for " + this.getClass().getName());
        deltaEvent.setStartDate(DateTimeUtil.getZuluDateTime(update.getFirst().getTimestamp()));
        deltaEvent.setEndDate(DateTimeUtil.getZuluDateTime(update.getSecond().getTimestamp()));

        String recurrenceId = DateTimeUtil.getZuluDateTime(testData.initialDatesCreateWithChange.getFirst().getTimestamp()).getValue();

        UpdateEventBody body = getUpdateBody(deltaEvent);
        // @formatter:off
        ChronosCalendarResultResponse result = chronosApi.updateEventBuilder()
                                                         .withFolder(defaultFolderId)
                                                         .withId(createdEvent.getId())
                                                         .withTimestamp(L(eventManager.getLastTimeStamp()))
                                                         .withUpdateEventBody(body)
                                                         .withRecurrenceId(recurrenceId)
                                                         .execute();

        /*
         * Check result
         */
        assertNotNull(result);
        CalendarResult calendarResult = checkResponse(result.getError(), result.getErrorDesc(), "WARNING", result.getData());
        assertTrue(calendarResult.getUpdated().size() == 1);
    }
}