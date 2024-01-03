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

import static com.openexchange.ajax.chronos.util.DateTimeUtil.formatZuluDate;
import static com.openexchange.ajax.chronos.util.DateTimeUtil.incrementDateTimeData;
import static com.openexchange.ajax.framework.ClientCommons.checkResponse;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.b;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.java.Strings;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.Attendee.CuTypeEnum;
import com.openexchange.testing.httpclient.models.ChronosConflictDataRaw;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponse;
import com.openexchange.testing.httpclient.models.ChronosFreeBusyResponseData;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.FreeBusyBody;
import com.openexchange.testing.httpclient.models.FreeBusyTime;
import com.openexchange.testing.httpclient.modules.ChronosApi;

/**
 * {@link FreeBusyUtils}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.3
 */
public class FreeBusyUtils {

    /**
     * {@link FBType}
     *
     * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
     */
    public enum FBType {

        /** Timeslot is blocked */
        BUSY,
        /** Timeslot is free */
        FREE;

        public boolean matches(String toMatch) {
            return name().equalsIgnoreCase(toMatch);
        }
    }

    private final static String DEFAULT_TIMEZONE = "UTC";

    /**
     * Get the free busy data for the desired users
     *
     * @param chronosApi The API to use
     * @param contextId The context identifier of the acting user
     * @param freeBusyTargets The users to get the data for
     * @return The free busy response per attendee
     * @throws Exception in case of error
     */
    public static List<ChronosFreeBusyResponseData> getData(ChronosApi chronosApi, int contextId, TestUser... freeBusyTargets) throws Exception {
        String from = getFromDate();
        String until = getUntilDate();
        FreeBusyBody freeBusyBody = new FreeBusyBody().attendees(createAttendees(contextId, freeBusyTargets));
        ChronosFreeBusyResponse freeBusyResponse = chronosApi.freebusy(from, until, freeBusyBody, null, Boolean.TRUE);
        assertNull(freeBusyResponse.getError(), freeBusyResponse.getError());
        return checkResponse(freeBusyResponse.getError(), freeBusyResponse.getError(), freeBusyResponse.getData());
    }

    /**
     * Creates attendees for a free/busy call
     *
     * @param contextId The context identifier of the acting user
     * @param freeBusyTargets The users to conver to to attendees
     * @return A list of attendees
     */
    public static List<Attendee> createAttendees(int contextId, TestUser... freeBusyTargets) {
        ArrayList<Attendee> result = new ArrayList<>(freeBusyTargets.length);
        for (TestUser user : freeBusyTargets) {
            Attendee attendee = new Attendee().email(user.getLogin()).uri("mailto:" + user.getLogin()).cuType(CuTypeEnum.INDIVIDUAL);
            /*
             * Check if the additional user is an internal user or not
             */
            if (user.getContextId() == contextId) {
                attendee.entity(I(user.getUserId()));
            } else {
                attendee.entity(null);
            }
            result.add(attendee);
        }
        return result;
    }

    /**
     * Get the from date for a free/busy call based on the start date of an event
     *
     * @param createdEvent The event
     * @return The from data as string
     * @throws Exception In case of error
     */
    public static String getFromDate(EventData createdEvent) throws Exception {
        return formatZuluDate(incrementDateTimeData(createdEvent.getStartDate(), -1 * TimeUnit.DAYS.toMillis(1)));
    }

    /**
     * Get the until date for a free/busy call based on the end date of an event
     *
     * @param createdEvent The event
     * @return The until data as string
     * @throws Exception In case of error
     */
    public static String getUntilDate(EventData createdEvent) throws Exception {
        return formatZuluDate(incrementDateTimeData(createdEvent.getEndDate(), TimeUnit.DAYS.toMillis(1)));
    }

    /**
     * Get a from date for a free/busy call based on relative "yesterday"
     *
     * @param tzid The timzone identifier for the formatted from
     * @return The from data as string
     */
    public static String getFromDate(String tzid) {
        return formatZuluDate(TimeTools.D("yesterday at 09:00", TimeZone.getTimeZone(Strings.isEmpty(tzid) ? DEFAULT_TIMEZONE : tzid)));
    }

    /**
     * Get a until date for a free/busy call based on relative "tomorrow"
     *
     * @param tzid The timzone identifier for the formatted from
     * @return The from data as string
     */
    public static String getUntilDate(String tzid) {
        return formatZuluDate(TimeTools.D("tomorrow at 17:00", TimeZone.getTimeZone(Strings.isEmpty(tzid) ? DEFAULT_TIMEZONE : tzid)));
    }

    /**
     * Get a from date for a free/busy call based on relative "yesterday" in UTC
     *
     * @return The from data as string
     */
    public static String getFromDate() {
        return getFromDate(DEFAULT_TIMEZONE);
    }

    /**
     * Get a until date for a free/busy call based on relative "tomorrow" in UTC
     *
     * @return The until data as string
     */
    public static String getUntilDate() {
        return getUntilDate(DEFAULT_TIMEZONE);
    }

    /**
     * Checks the data contains only a single conflict
     *
     * @param conflicts The conflicts
     * @param event The event that should conflict
     * @param target The target user
     * @throws AssertionError In case of mismatch
     */
    public static void checkSingleConflict(List<ChronosConflictDataRaw> conflicts, EventData event, TestUser target) throws AssertionError {
        assertTrue(conflicts.size() == 1, "Not all attendee conflicts found:" + conflicts);
        ChronosConflictDataRaw conflict = conflicts.get(0);
        checkConflict(conflict, target);
        assertThat(conflict.getEvent().getStartDate(), is(event.getStartDate()));
    }

    /**
     * Check that the data contains conflicts for the given user.
     * <p>
     * Hard conflict is not expected
     *
     * @param conflict The conflict to check
     * @param target The target user
     * @throws AssertionError
     */
    public static void checkConflict(ChronosConflictDataRaw conflict, TestUser target) throws AssertionError {
        assertNotNull(conflict);
        assertNotNull(conflict.getHardConflict());
        Assertions.assertFalse(b(conflict.getHardConflict()));
        assertNotNull(conflict.getConflictingAttendees());
        assertTrue(conflict.getConflictingAttendees().size() == 1, "Not all attendee conflicts found:" + conflict);
        Optional<Attendee> conflictingAttendee = conflict.getConflictingAttendees().stream().filter(a -> target.getLogin().equals(a.getEmail())).findAny();
        assertTrue(conflictingAttendee.isPresent());
    }

    /**
     * Asserts that a single busy time was transmitted
     *
     * @param freeBusyResponse The free/busy response
     * @param event The event that should be matched
     * @param user The user the free/busy time slot should be generated for
     * @param type The {@link FBType}
     * @throws Exception
     */
    public static void assertSingleBusyTime(ChronosFreeBusyResponse freeBusyResponse, EventData event, TestUser user, FBType type) throws Exception {
        assertBusyTime(freeBusyResponse, event, Map.of(user, type));
    }

    /**
     * Asserts that a single busy time was transmitted
     *
     * @param freeBusyResponse The free/busy response
     * @param event The event that should be matched
     * @param types The users and their {@link FBType}
     * @throws Exception
     */
    public static void assertBusyTime(ChronosFreeBusyResponse freeBusyResponse, EventData event, Map<TestUser, FBType> types) throws Exception {
        List<ChronosFreeBusyResponseData> data = checkResponse(freeBusyResponse.getError(), freeBusyResponse.getErrorDesc(), freeBusyResponse.getData());
        assertNotNull(data);
        assertTrue(data.size() == types.size());
        Map<TestUser, Boolean> found = new HashMap<>(types.size());
        for (ChronosFreeBusyResponseData freebusy : data) {
            assertNotNull(freebusy.getAttendee());
            Attendee busyAttendee = freebusy.getAttendee();
            List<FreeBusyTime> busyTime = freebusy.getFreeBusyTime();
            assertNotNull(busyTime);
            assertTrue(busyTime.size() == 1, "Not all busy times found:" + busyTime);
            FreeBusyTime freeBusyTime = busyTime.get(0);
            assertTrue(null != busyAttendee.getEmail());
            for (Entry<TestUser, FBType> entry : types.entrySet()) {
                if (busyAttendee.getEmail().equals(entry.getKey().getLogin())) {
                    assertThat(freeBusyTime.getStartTime(), is(L(DateTimeUtil.parseDateTime(event.getStartDate()).getTime())));
                    assertTrue(entry.getValue().matches(freeBusyTime.getFbType()));
                    found.put(entry.getKey(), Boolean.valueOf(true));
                }
            }
        }
        for (TestUser testUser : types.keySet()) {
            Boolean b = found.get(testUser);
            assertTrue(null != b && Boolean.TRUE.equals(b));
        }
    }

}
