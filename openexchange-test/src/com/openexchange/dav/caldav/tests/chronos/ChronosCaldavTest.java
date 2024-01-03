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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.java.util.Pair;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.Alarm;
import com.openexchange.testing.httpclient.models.EventData;
import com.openexchange.testing.httpclient.models.EventId;
import com.openexchange.testing.httpclient.models.EventsResponse;

/**
 * {@link ChronosCaldavTest}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
public class ChronosCaldavTest extends AbstractChronosCaldavTest {

    /**
     * Retrieves the event by UID
     *
     * @param uid The UID
     * @return The event
     * @throws Exception
     * @throws ApiException
     */
    protected EventData getEvent(String uid, boolean remember) throws ApiException, Exception {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        instance.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = instance.getTime();
        instance.add(Calendar.DAY_OF_MONTH, 14);
        Date twoWeeks = instance.getTime();
        String rangeStart = DateTimeUtil.formatZuluDate(yesterday);
        String rangeEnd = DateTimeUtil.formatZuluDate(twoWeeks);
        EventsResponse allEventResponse = defaultUserApi.getChronosApi().getAllEvents(rangeStart, rangeEnd, getDefaultFolder(), null, null, null, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        checkResponse(allEventResponse.getError(), allEventResponse.getErrorDesc(), allEventResponse.getData());

        for (EventData event : allEventResponse.getData()) {
            if (uid.equals(event.getUid())) {
                if (remember) {
                    EventId eventId = new EventId();
                    eventId.setFolder(event.getFolder());
                    eventId.setId(event.getId());
                    eventId.setRecurrenceId(event.getRecurrenceId());
                    rememberEventId(eventId);
                }
                return event;
//                return defaultUserApi.getChronosApi().getEvent(defaultUserApi.getSession(), event.getId(), event.getFolder(), event.getRecurrenceId(), null).getData();
            }
        }
        return null;

    }

    /**
     * Retrieves exceptions by series id
     *
     * @param seriesId The series id
     * @return The exceptions
     * @throws ApiException
     * @throws Exception
     */
    protected List<EventData> getExceptions(String seriesId) throws ApiException, Exception {
        Calendar instance = Calendar.getInstance();
        instance.setTime(new Date());
        instance.add(Calendar.DAY_OF_MONTH, -1);
        Date yesterday = instance.getTime();
        instance.add(Calendar.DAY_OF_MONTH, 14);
        Date twoWeeks = instance.getTime();
        String rangeStart = DateTimeUtil.formatZuluDate(yesterday);
        String rangeEnd = DateTimeUtil.formatZuluDate(twoWeeks);
        EventsResponse allEventResponse = defaultUserApi.getChronosApi().getAllEvents(rangeStart, rangeEnd, getDefaultFolder(), null, null, null, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE);
        checkResponse(allEventResponse.getError(), allEventResponse.getErrorDesc(), allEventResponse.getData());

        List<EventData> result = new ArrayList<>();
        for (EventData event : allEventResponse.getData()) {
            if (seriesId.equals(event.getSeriesId()) && !seriesId.equals(event.getId())) {
                result.add(event);
//                result.add(defaultUserApi.getChronosApi().getEvent(defaultUserApi.getSession(), event.getId(), event.getFolder(), event.getRecurrenceId(), null).getData());
            }
        }
        return result;

    }

    /**
     * Verifies that the event exists
     *
     * Assumes that the event contains exactly 1 alarm
     *
     * @param uid The uid of the event
     * @param remember Whether the event should be remembered for deletion
     * @param duration The expected duration of the alarm
     * @return The event
     * @throws ApiException
     * @throws Exception
     */
    protected EventData verifyEvent(String uid, boolean remember, String triggerValue) throws ApiException, Exception {
        EventData event = getEvent(uid, remember);
        assertNotNull(event, "event not found on server");
        assertTrue(event.getAlarms() != null && !event.getAlarms().isEmpty(), "no alarm found");
        assertEquals(1, event.getAlarms().size(), "no alarm found");
        checkAlarms(event.getAlarms(), getPair(event.getAlarms().get(0).getUid(), triggerValue));
        return event;
    }

    protected Pair<String, String> getPair(String uid, String value) {
        return new Pair<>(uid, value);
    }

    /**
     * Verifies that the event exists
     *
     * @param uid The uid of the event
     * @param remember Whether the event should be remembered for deletion
     * @param alarms The number of expected alarms
     * @return The event
     * @throws ApiException
     * @throws Exception
     */
    protected EventData verifyEvent(String uid, boolean remember, int alarms) throws ApiException, Exception {
        EventData event = getEvent(uid, remember);
        assertNotNull(event, "event not found on server");
        if (alarms > 0) {
            assertTrue(event.getAlarms() != null && !event.getAlarms().isEmpty(), "no alarm found");
            assertEquals(alarms, event.getAlarms().size(), "no alarm found");
        } else {
            assertTrue(event.getAlarms() == null || event.getAlarms().isEmpty(), "Alarm still found");
        }
        return event;
    }

    /**
     * Verifies that exactly one exception exists, which contains the given amount of alarms and that the first alarm has the given duration
     *
     * @param seriesId The series id of the event
     * @param alarms The number of expected alarms
     * @param firstAlarmDuration The duration of the first alarm
     * @return The exceptions
     * @throws ApiException
     * @throws Exception
     */
    @SafeVarargs
    final protected EventData verifyEventException(String seriesId, int alarms, Pair<String, String>... triggerValues) throws ApiException, Exception {
        List<EventData> exceptions = getExceptions(seriesId);
        assertFalse(exceptions.isEmpty(), "No change exceptions found on server");
        assertEquals(1, exceptions.size(), "Unexpected number of change exceptions");
        EventData changeException = exceptions.get(0);
        if (alarms > 0) {
            assertTrue(changeException.getAlarms() != null && !changeException.getAlarms().isEmpty(), "no alarm found");
            assertEquals(alarms, changeException.getAlarms().size(), "Wrong size of alarms found");
            checkAlarms(changeException.getAlarms(), triggerValues);
        } else {
            assertTrue(changeException.getAlarms() == null || changeException.getAlarms().isEmpty(), "Alarm still found");
        }
        return changeException;
    }

    /**
     * Verifies that no event exceptions exist
     *
     * @param seriesId The series id of the event
     * @throws ApiException
     * @throws Exception
     */
    protected void verifyNoEventExceptions(String seriesId) throws ApiException, Exception {
        List<EventData> exceptions = getExceptions(seriesId);
        assertTrue(exceptions.isEmpty(), "Change exceptions found on server");
    }

    /**
     * Checks whether the alarm contain the given value as either duration or dateTime
     *
     * @param alarms The alarm to check
     * @param alarmTriggerValue A pair of alarm uid and value (either duration or date time)
     */
    @SafeVarargs
    final protected void checkAlarms(List<Alarm> alarms, Pair<String, String>... alarmTriggerValue) {

        for (Alarm alarm : alarms) {
            String uid = alarm.getUid();
            for (Pair<String, String> alarmUid : alarmTriggerValue) {
                if (uid.equals(alarmUid.getFirst())) {
                    String expected = alarmUid.getSecond();
                    assertTrue(expected.equals(alarm.getTrigger().getDuration()) || expected.equals(alarm.getTrigger().getDateTime()), "Wrong trigger.");
                }
            }
        }

    }

}
