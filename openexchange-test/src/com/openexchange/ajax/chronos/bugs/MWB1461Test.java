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

package com.openexchange.ajax.chronos.bugs;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.b;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.UserApi;
import com.openexchange.ajax.chronos.factory.AttendeeFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.test.common.test.TestClassConfig;
import com.openexchange.test.common.test.pool.ProvisioningUtils;
import com.openexchange.test.common.test.pool.TestUser;
import com.openexchange.testing.httpclient.models.Attendee;
import com.openexchange.testing.httpclient.models.ChronosConflictDataRaw;
import com.openexchange.testing.httpclient.models.EventData;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link MWB1461Test} - Conflict tests for many events/users
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.0.0
 */
public class MWB1461Test extends AbstractChronosTest {

    private static final int USERS = ProvisioningUtils.USER_NAMES_POOL.length;
    private static final int EVENTS_PER_USER = 201;
    private static final int MAX_CONFLICTS = 100;
    private static final int MAX_EVENTS = 1000;

    /*
     * ============================== Test configuration ==============================
     */

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        setUpConfiguration();
    }

    @Override
    protected String getScope() {
        return "context";
    }

    @Override
    protected Map<String, String> getNeededConfigurations() {
        HashMap<String, String> map = new HashMap<>(4, 0.9f);
        //        map.put("com.openexchange.calendar.maxEventResults", I(MAX_EVENTS).toString());// --> server property, can't overwrite on server level
        map.put("com.openexchange.calendar.maxOccurrencesForConflicts", "100");
        map.put("com.openexchange.calendar.maxSeriesUntilForConflicts", "5");
        map.put("com.openexchange.calendar.maxConflicts", I(MAX_CONFLICTS).toString());
        return map;
    }

    @Override
    public TestClassConfig getTestConfig() {
        return TestClassConfig.builder().withUserPerContext(USERS).build();
    }

    /*
     * ============================== Tests ==============================
     */

    @Test
    public void testConflicts_notTooManyConflicts() throws Exception {
        /*
         * Prepare events
         */
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        start.setTimeInMillis(System.currentTimeMillis());
        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1));

        List<EventData> eventsToCreate = new ArrayList<>(EVENTS_PER_USER);
        for (int j = 0; j < EVENTS_PER_USER; j++) {
            eventsToCreate.add(EventFactory.createSingleEvent(testUser.getUserId(), UUID.randomUUID().toString(), DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end)));
            start.add(Calendar.DAY_OF_MONTH, 1);
            end.add(Calendar.DAY_OF_MONTH, 1);
        }
        /*
         * Create events
         */
        int events = 0;
        for (TestUser user : testContext.getUsers()) {
            UserApi defaultUserApiTargetUser = new UserApi(user.getApiClient(), user);
            String defaultFolderIdTargetUser = getDefaultFolder(user.getApiClient());
            EventManager manager = new EventManager(defaultUserApiTargetUser, defaultFolderIdTargetUser);
            List<Attendee> attendees = Collections.singletonList(AttendeeFactory.createIndividual(I(user.getUserId())));
            for (EventData event : eventsToCreate) {
                event.setAttendees(attendees);
                manager.createEvent(event, true); // Avoid conflict checks for more performance
                events++;
            }
        }
        /*
         * Check that we created over 1000 events, which is the default value for `com.openexchange.calendar.maxEventResults`
         */
        assertThat(I(events), is(greaterThan(I(MAX_EVENTS))));
        assertThat(I(events), is(I(EVENTS_PER_USER * ProvisioningUtils.USER_NAMES_POOL.length)));

        /*
         * Prepare series event with all users, starting from today until the last event for each user,
         * efficiently conflicting with all created events.
         */
        start = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        start.setTimeInMillis(System.currentTimeMillis());
        end = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        end.setTimeInMillis(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(10));

        EventData allEvent = EventFactory.createSeriesEvent(testUser.getUserId(), "MWB-1461", DateTimeUtil.getDateTime(start), DateTimeUtil.getDateTime(end), EVENTS_PER_USER);
        for (TestUser user : testContext.getUsers()) {
            allEvent.addAttendeesItem(AttendeeFactory.createIndividual(I(user.getUserId())));
        }
        /*
         * Check conflicts
         * - No "Too many events are queried." exception because we reached the MAX_EVENTS size for possible conflicts
         * - Not more than the maximum configured conflicts size
         * - Each event is unique, so one conflicting attendee per conflict
         */
        List<ChronosConflictDataRaw> conflicts = eventManager.getConflictsOnCreate(allEvent);
        assertThat("Not all conflicts found", I(conflicts.size()), is(I(MAX_CONFLICTS)));
        for (ChronosConflictDataRaw conflict : conflicts) {
            checkConflict(conflict, 1);
        }
    }

    /*
     * ============================== HELPERS ==============================
     */

    private void checkConflict(ChronosConflictDataRaw conflict, int size) throws AssertionError {
        assertNotNull(conflict);
        assertNotNull(conflict.getHardConflict());
        assertFalse(b(conflict.getHardConflict()));
        assertNotNull(conflict.getConflictingAttendees());
        assertTrue(conflict.getConflictingAttendees().size() == size, "Not all attendee conflicts found:" + conflict);
    }

}
