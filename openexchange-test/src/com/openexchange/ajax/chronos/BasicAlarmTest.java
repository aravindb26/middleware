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

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.factory.AlarmFactory;
import com.openexchange.ajax.chronos.factory.EventFactory;
import org.junit.jupiter.api.Assertions;
import com.openexchange.testing.httpclient.models.Alarm;
import com.openexchange.testing.httpclient.models.AlarmExtendedProperties;
import com.openexchange.testing.httpclient.models.EventData;

/**
 *
 * {@link BasicAlarmTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.0
 */
public class BasicAlarmTest extends AbstractAlarmTest {


    /**
     * Initializes a new {@link BasicAlarmTest}.
     */
    public BasicAlarmTest() {
        super();
    }

    /**
     * Tests the creation an event with a single alarm
     */
    @Test
    public void testCreateSingleAlarm() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleEventWithSingleAlarm(defaultUserApi.getCalUser().intValue(), "testCreateSingleAlarm", AlarmFactory.createDisplayAlarm("-PT15M"), folderId), true);
        getAndAssertAlarms(expectedEventData, 1, folderId);
    }

    /**
     * Tests the creation of an event without an alarm and the later addition of one
     */
    @Test
    public void testAddSingleAlarm() throws Exception {
        // Create an event without an alarm
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser().intValue(), "testAddSingleAlarm", folderId), true);
        EventData actualEventData = getAndAssertAlarms(expectedEventData, 0, folderId);

        // Create the alarm as a delta for the event
        EventData updateData = new EventData();
        updateData.setAlarms(Collections.singletonList(AlarmFactory.createDisplayAlarm("-PT30M")));
        updateData.setId(actualEventData.getId());
        updateData.setLastModified(actualEventData.getLastModified());
        updateData.setFolder(folderId);

        // Update the event and add the alarm
        expectedEventData = eventManager.updateEvent(updateData);

        // Assert that the alarm was successfully added
        getAndAssertAlarms(expectedEventData, 1, folderId);
    }

    /**
     * Tests the change of the alarm time
     */
    @Test
    public void testChangeAlarmTime() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleEventWithSingleAlarm(defaultUserApi.getCalUser().intValue(), "testChangeAlarmTime", AlarmFactory.createDisplayAlarm("-PT15M"), folderId), true);
        EventData actualEventData = getAndAssertAlarms(expectedEventData, 1, folderId);

        EventData updateData = new EventData();
        updateData.setAlarms(Collections.singletonList(AlarmFactory.createDisplayAlarm("-PT30M")));
        updateData.setId(actualEventData.getId());
        updateData.setLastModified(actualEventData.getLastModified());
        updateData.setFolder(folderId);

        expectedEventData = eventManager.updateEvent(updateData);

        actualEventData = getAndAssertAlarms(expectedEventData, 1, folderId);
        assertEquals("-PT30M", actualEventData.getAlarms().get(0).getTrigger().getDuration());
    }

    /**
     * Tests the creation of different alarm types (display, mail, audio)
     *
     * @throws Exception
     */
    @Test
    public void testDifferentAlarmTypes() throws Exception {
        EventData expectedEventData = eventManager.createEvent(EventFactory.createSingleTwoHourEvent(defaultUserApi.getCalUser().intValue(), "testDifferentAlarmTypes", folderId), true);
        EventData actualEventData = getAndAssertAlarms(expectedEventData, 0, folderId);

        // Test display alarm
        {
            Alarm alarm = AlarmFactory.createDisplayAlarm("-PT30M");

            EventData updateData = new EventData();
            updateData.setAlarms(Collections.singletonList(alarm));
            updateData.setId(actualEventData.getId());
            updateData.setLastModified(actualEventData.getLastModified());
            updateData.setFolder(folderId);

            expectedEventData = eventManager.updateEvent(updateData);

            actualEventData = getAndAssertAlarms(expectedEventData, 1, folderId);

            Alarm changedAlarm = actualEventData.getAlarms().get(0);
            alarm.setUid(changedAlarm.getUid());
            alarm.setId(changedAlarm.getId());
            assertEquals(alarm, changedAlarm, "The created alarm does not match the expected one.");
        }

        // Test mail alarm
        {
            Alarm alarm = AlarmFactory.createMailAlarm("-PT30M", "This is the mail message!", "This is the mail subject");

            EventData updateData = new EventData();
            updateData.setAlarms(Collections.singletonList(alarm));
            updateData.setId(actualEventData.getId());
            updateData.setLastModified(actualEventData.getLastModified());
            updateData.setFolder(folderId);

            expectedEventData = eventManager.updateEvent(updateData);

            actualEventData = getAndAssertAlarms(expectedEventData, 1, folderId);

            Alarm changedAlarm = actualEventData.getAlarms().get(0);

            Assertions.assertNotNull(changedAlarm.getAttendees());
            Assertions.assertEquals(1, changedAlarm.getAttendees().size());
            Assertions.assertEquals(testUser.getLogin(), changedAlarm.getAttendees().get(0).getEmail());
            alarm.setUid(changedAlarm.getUid());
            alarm.setId(changedAlarm.getId());
            alarm.setAttendees(changedAlarm.getAttendees());
            //HashMap<String, String> map = new HashMap<>();
            //map.put("value", "SERVER");
            AlarmExtendedProperties extendedProperty = new AlarmExtendedProperties();
            extendedProperty.setValue("SERVER");
            alarm.putExtendedPropertiesItem("ALARM-AGENT", extendedProperty);
            assertEquals(alarm, changedAlarm, "The created alarm does not match the expected one.");
        }

        // Test AUDIO alarm
        {
            Alarm alarm = AlarmFactory.createAudioAlarm("-PT30M", "ftp://some.fake.ftp.server/file.mp3");
            EventData updateData = new EventData();
            updateData.setAlarms(Collections.singletonList(alarm));
            updateData.setId(actualEventData.getId());
            updateData.setLastModified(actualEventData.getLastModified());
            updateData.setFolder(folderId);

            expectedEventData = eventManager.updateEvent(updateData);

            actualEventData = getAndAssertAlarms(expectedEventData, 1, folderId);

            Alarm changedAlarm = actualEventData.getAlarms().get(0);
            alarm.setUid(changedAlarm.getUid());
            alarm.setId(changedAlarm.getId());
            assertEquals(alarm, changedAlarm, "The created alarm does not match the expected one.");
        }
    }
}
