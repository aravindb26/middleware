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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.openexchange.ajax.chronos.manager.EventManager;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.ajax.framework.SessionAwareClient;
import org.junit.jupiter.api.Assertions;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.AlarmTrigger;
import com.openexchange.testing.httpclient.models.AlarmTriggerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * {@link AbstractAlarmTriggerTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public abstract class AbstractAlarmTriggerTest extends AbstractAlarmTest {

    protected String folderId2;
    protected UserApi user2;
    protected EventManager eventManager2;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        SessionAwareClient client2 = testUser2.getApiClient();
        user2 = new UserApi(client2, testUser2);
        folderId2 = getDefaultFolder(client2);
        eventManager2 = new EventManager(user2, getDefaultFolder(client2));
    }

    /**
     * Retrieves alarm triggers with a trigger time lower than the given limit and checks if the response contains the correct amount of alarm trigger objects.
     * Its also possible to filter for specific actions.
     *
     * @param until The upper limit
     * @param actions The actions to retrieve
     * @param expected The expected amount of alarm objects
     * @param filter The event id to filter for
     * @return The {@link AlarmTriggerData}
     * @throws ApiException
     */
    List<AlarmTrigger> getAndCheckAlarmTrigger(long until, String actions, int expected, String filter) throws ApiException {
        //AlarmTriggerData triggers = eventManager.getAlarmTrigger(until, actions);
        List<AlarmTrigger> triggers = eventManager.getAlarmTrigger(until, actions);
        //AlarmTriggerData result = triggers;
        List<AlarmTrigger> result = null;
        if (filter != null) {
            result = new ArrayList<>();
            for(AlarmTrigger trigger: triggers) {
                if (trigger.getEventId().equals(filter)) {
                    result.add(trigger);
                }
            }
        }
        assertNotNull(result);
        assertEquals(expected, result.size());
        return result;
    }

    /**
     * Retrieves alarm triggers from the given time until two days and checks if the response contains the correct amount of alarm trigger objects
     *
     * @param until The upper limit of the request
     * @param min The minimum amount of expected alarm trigger objects
     * @return The {@link AlarmTriggerData}
     * @throws ApiException
     */
    List<AlarmTrigger> getAndCheckAlarmTrigger(long until, int min) throws ApiException {
    	List<AlarmTrigger> triggers = eventManager.getAlarmTrigger(until);
        assertTrue(min <= triggers.size(), String.format("Missing some triggers. Expected at least %s triggers but only found %s", I(min), I(triggers.size())));
        return triggers;
    }

    /**
     * Retrieves alarm triggers until two days and checks if the response contains the correct amount of alarm trigger objects
     *
     * @param min The minimum amount of expected alarm trigger objects
     * @return The {@link AlarmTriggerData}
     * @throws ApiException
     */
    List<AlarmTrigger> getAndCheckAlarmTrigger(int min) throws ApiException {
        return getAndCheckAlarmTrigger(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2), min);
    }

    /**
     * Retrieves alarm triggers until two days.
     *
     * @return The {@link AlarmTriggerData}
     * @throws ApiException
     */
    List<AlarmTrigger> getAlarmTriggers() throws ApiException {
        return eventManager.getAlarmTrigger(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));
    }

    /**
     * Checks if the trigger is related to the given event and if the trigger time is correct
     *
     * @param trigger The trigger
     * @param eventId The event id
     * @param expectedTime The expected trigger time
     * @throws ParseException
     */
    protected void checkAlarmTime(AlarmTrigger trigger, String eventId, long expectedTime) throws ParseException {
        assertEquals(eventId, trigger.getEventId(), "Wrong event id.");
        Date parsedTime = DateTimeUtil.parseZuluDateTime(trigger.getTime());
        assertEquals(expectedTime, parsedTime.getTime(), "The alarm trigger time is different than expected.");
    }

    //protected boolean containsAlarm(AlarmTriggerData data, String folder, String alarmId, String eventId) {
    protected boolean containsAlarm(List<AlarmTrigger> data, String folder, String alarmId, String eventId) {
        for (AlarmTrigger trigger : data) {
            if ((folder == null || folder.equals(trigger.getFolder())) &&
                (alarmId == null || alarmId.equals(trigger.getAlarmId())) &&
                (eventId == null || eventId.equals(trigger.getEventId()))) {
                return true;
            }
        }
        return false;
    }

    protected AlarmTrigger findTrigger(String eventId, List<AlarmTrigger> triggers) {
        Optional<AlarmTrigger> result = triggers.stream().filter(t -> t.getEventId().equals(eventId)).findFirst();
        if (result.isPresent() == false) {
            Assertions.fail(String.format("Alarm trigger not found. Search for '%s', but only found the following ids: %s", eventId, triggers.stream().map(t -> t.getEventId()).collect(Collectors.joining(", "))));
            return null;
        }
        return result.get();
    }

    protected AlarmTrigger findTriggerByAlarm(Integer alarmId, List<AlarmTrigger> triggers) {
        for (AlarmTrigger trigger : triggers) {
            if (trigger.getAlarmId().equals(String.valueOf(alarmId))) {
                return trigger;
            }
        }
        Assertions.fail(String.format("Alarm trigger not found. Search for alarm id '%s', but only found the following alarm ids: %s", alarmId, triggers.stream().map(t -> t.getAlarmId()).collect(Collectors.joining(", "))));
        return null;
    }

}
