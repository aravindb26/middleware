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

package com.openexchange.ajax.appointment.recurrence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.factory.EventFactory;
import com.openexchange.ajax.chronos.manager.ChronosApiException;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.testing.httpclient.invoker.ApiException;
import com.openexchange.testing.httpclient.models.EventData;

public class Bug12212Test extends AbstractChronosTest {

    final String bugname = "Test for bug 12212";

    public Bug12212Test() {
        super();
    }

    public EventData shiftEventDateOneHour(EventData event, Calendar calendar) throws ParseException, ApiException, ChronosApiException {
        calendar.setTime(DateTimeUtil.parseDateTime(event.getStartDate()));
        calendar.add(Calendar.HOUR, 1);
        event.setStartDate(DateTimeUtil.getDateTime(calendar));

        calendar.setTime(DateTimeUtil.parseDateTime(event.getEndDate()));
        calendar.add(Calendar.HOUR, 1);
        event.setEndDate(DateTimeUtil.getDateTime(calendar));

        return eventManager.updateEvent(event);
    }

    @Test
    public void testMovingExceptionTwiceShouldNeitherCrashNorDuplicate() throws ApiException, ChronosApiException, ParseException {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));

        // create appointment
        final EventData toCreate = EventFactory.createSeriesEvent(defaultUserApi.getCalUser().intValue(), bugname, 5, defaultFolderId);
        EventData eventSeries = eventManager.createEvent(toCreate);

        // get one occurrence
        calendar.setTime(DateTimeUtil.parseDateTime(eventSeries.getStartDate()));
        calendar.add(Calendar.DATE, -5);
        Date from = calendar.getTime();

        calendar.setTime(DateTimeUtil.parseDateTime(eventSeries.getEndDate()));
        calendar.add(Calendar.DATE, 5);
        Date to = calendar.getTime();

        List<EventData> allEvents = eventManager.getAllEvents(defaultFolderId, from, to, true);

        allEvents = getEventsByUid(allEvents, eventSeries.getUid());
        assertEquals(5, allEvents.size(), "Expected 5 occurrences");
        EventData occurrence = allEvents.get(3);

        // make an exception out of the occurrence by moving an event one hour
        EventData exception = shiftEventDateOneHour(occurrence, calendar);

        // move it again
        exception = shiftEventDateOneHour(exception, calendar);

        //assert no duplicate exists
        List<EventData> allAppointmentsWithinTimeframe = eventManager.getAllEvents(DateTimeUtil.parseDateTime(exception.getStartDate()), DateTimeUtil.parseDateTime(exception.getEndDate()));
        int countOfPotentialDuplicates = 0;
        for (EventData event : allAppointmentsWithinTimeframe) {
            if (event.getSummary().startsWith(bugname)) {
                countOfPotentialDuplicates++;
            }
        }
        assertEquals(Integer.valueOf(1), Integer.valueOf(countOfPotentialDuplicates), "Should be only one occurrence of this appointment");
    }

}