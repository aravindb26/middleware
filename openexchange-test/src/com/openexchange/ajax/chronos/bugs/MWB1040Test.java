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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import com.google.api.client.util.Objects;
import com.openexchange.ajax.chronos.AbstractChronosTest;
import com.openexchange.ajax.chronos.util.DateTimeUtil;
import com.openexchange.chronos.common.CalendarUtils;
import com.openexchange.test.common.groupware.calendar.TimeTools;
import com.openexchange.testing.httpclient.models.ChronosCalendarResultResponse;
import com.openexchange.testing.httpclient.models.ChronosConflictDataRaw;
import com.openexchange.testing.httpclient.models.EventData;

/**
 * {@link MWB1040Test}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class MWB1040Test extends AbstractChronosTest {

    private TimeZone timeZone;

    @Override
    @BeforeEach
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        timeZone = TimeZone.getTimeZone("Europe/Berlin");
    }

    @Test
    public void testBeginWithinLastOccurrence() throws Exception {
        /*
         * create first event series
         */
        EventData eventSeries = insertFirstEventSeries();
        /*
         * prepare second event series, starting within the period of the first one's last occurrence
         */
        EventData eventSeries2 = new EventData();
        eventSeries2.setSummary("MWB1040Test2");
        Date startDate2 = CalendarUtils.add(CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getStartDate()), Calendar.WEEK_OF_YEAR, 1, timeZone), Calendar.HOUR, 2, timeZone);
        Date endDate2 = CalendarUtils.add(CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getEndDate()), Calendar.WEEK_OF_YEAR, 1, timeZone), Calendar.HOUR, 2, timeZone);
        eventSeries2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), startDate2.getTime()));
        eventSeries2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), endDate2.getTime()));
        /*
         * try and create second series, expecting a conflict with the first series
         */
        checkCreateConflictingEvent(eventSeries2, eventSeries.getId(), true);
    }

    @Test
    public void testSeriesBeginWithinLastOccurrence() throws Exception {
        /*
         * create first event series
         */
        EventData eventSeries = insertFirstEventSeries();
        /*
         * prepare second event series, starting within the period of the first one's last occurrence
         */
        EventData eventSeries2 = new EventData();
        eventSeries2.setSummary("MWB1040Test2");
        Date startDate2 = CalendarUtils.add(CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getStartDate()), Calendar.WEEK_OF_YEAR, 1, timeZone), Calendar.HOUR, 2, timeZone);
        Date endDate2 = CalendarUtils.add(CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getEndDate()), Calendar.WEEK_OF_YEAR, 1, timeZone), Calendar.HOUR, 2, timeZone);
        eventSeries2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), startDate2.getTime()));
        eventSeries2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), endDate2.getTime()));
        eventSeries2.setRrule("FREQ=WEEKLY;COUNT=2");
        /*
         * try and create second series, expecting a conflict with the first series
         */
        checkCreateConflictingEvent(eventSeries2, eventSeries.getId(), true);
    }

    @Test
    public void testBeginAfterLastOccurrence() throws Exception {
        /*
         * create first event series
         */
        EventData eventSeries = insertFirstEventSeries();
        /*
         * prepare second event series, starting within the period of the first one's last occurrence
         */
        EventData eventSeries2 = new EventData();
        eventSeries2.setSummary("MWB1040Test2");
        Date startDate2 = CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getEndDate()), Calendar.WEEK_OF_YEAR, 1, timeZone);
        Date endDate2 = CalendarUtils.add(startDate2, Calendar.HOUR, 2, timeZone);
        eventSeries2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), startDate2.getTime()));
        eventSeries2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), endDate2.getTime()));
        /*
         * try and create second series, expecting no conflict with the first series
         */
        checkCreateConflictingEvent(eventSeries2, eventSeries.getId(), false);
    }

    @Test
    public void testSeriesBeginAfterLastOccurrence() throws Exception {
        /*
         * create first event series
         */
        EventData eventSeries = insertFirstEventSeries();
        /*
         * prepare second event series, starting within the period of the first one's last occurrence
         */
        EventData eventSeries2 = new EventData();
        eventSeries2.setSummary("MWB1040Test2");
        Date startDate2 = CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getEndDate()), Calendar.WEEK_OF_YEAR, 1, timeZone);
        Date endDate2 = CalendarUtils.add(startDate2, Calendar.HOUR, 2, timeZone);
        eventSeries2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), startDate2.getTime()));
        eventSeries2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), endDate2.getTime()));
        eventSeries2.setRrule("FREQ=WEEKLY;COUNT=2");
        /*
         * try and create second series, expecting no conflict with the first series
         */
        checkCreateConflictingEvent(eventSeries2, eventSeries.getId(), false);
    }

    @Test
    public void testBeginBeforeLastOccurrence() throws Exception {
        /*
         * create first event series
         */
        EventData eventSeries = insertFirstEventSeries();
        /*
         * prepare second event series, starting within the period of the first one's last occurrence
         */
        EventData eventSeries2 = new EventData();
        eventSeries2.setSummary("MWB1040Test2");
        Date startDate2 = CalendarUtils.add(CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getStartDate()), Calendar.WEEK_OF_YEAR, 1, timeZone), Calendar.HOUR, -2, timeZone);
        Date endDate2 = CalendarUtils.add(startDate2, Calendar.HOUR, 2, timeZone);
        eventSeries2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), startDate2.getTime()));
        eventSeries2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), endDate2.getTime()));
        /*
         * try and create second series, expecting no conflict with the first series
         */
        checkCreateConflictingEvent(eventSeries2, eventSeries.getId(), false);
    }

    @Test
    public void testSeriesBeginBeforeLastOccurrence() throws Exception {
        /*
         * create first event series
         */
        EventData eventSeries = insertFirstEventSeries();
        /*
         * prepare second event series, starting within the period of the first one's last occurrence
         */
        EventData eventSeries2 = new EventData();
        eventSeries2.setSummary("MWB1040Test2");
        Date startDate2 = CalendarUtils.add(CalendarUtils.add(DateTimeUtil.parseDateTime(eventSeries.getStartDate()), Calendar.WEEK_OF_YEAR, 1, timeZone), Calendar.HOUR, -2, timeZone);
        Date endDate2 = CalendarUtils.add(startDate2, Calendar.HOUR, 2);
        eventSeries2.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), startDate2.getTime()));
        eventSeries2.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), endDate2.getTime()));
        eventSeries2.setRrule("FREQ=WEEKLY;COUNT=2");
        /*
         * try and create second series, expecting no conflict with the first series
         */
        checkCreateConflictingEvent(eventSeries2, eventSeries.getId(), false);
    }

    private void checkCreateConflictingEvent(EventData eventToCreate, String conflictingEventId, boolean expectConflict) throws Exception {
        ChronosCalendarResultResponse createResponse = chronosApi.createEvent(defaultFolderId, eventToCreate, Boolean.TRUE, null, null, null, null, null, null, null);
        List<EventData> createdEvents = createResponse.getData().getCreated();
        List<ChronosConflictDataRaw> conflicts = createResponse.getData().getConflicts();
        ChronosConflictDataRaw conflict = lookupConflict(conflicts, conflictingEventId);
        if (expectConflict) {
            assertNotNull(conflict, "Conflict with first series not found");
            assertTrue(null == createdEvents || createdEvents.isEmpty(), "Conflicting event should not have been created ");
        } else {
            assertNull(conflict, "Unexpected conflict with first series");
            assertTrue(null != createdEvents && 0 < createdEvents.size(), "Event should have been created ");
        }
    }

    private EventData insertFirstEventSeries() throws Exception {
        EventData eventSeries1 = new EventData();
        eventSeries1.setSummary("MWB1040Test1");
        Date startDate1 = TimeTools.D("next monday at 2 pm", timeZone);
        Date endDate1 = TimeTools.D("next monday at 6 pm", timeZone);
        eventSeries1.setStartDate(DateTimeUtil.getDateTime(timeZone.getID(), startDate1.getTime()));
        eventSeries1.setEndDate(DateTimeUtil.getDateTime(timeZone.getID(), endDate1.getTime()));
        eventSeries1.setRrule("FREQ=WEEKLY;COUNT=2");
        eventSeries1.setFolder(defaultFolderId);
        EventData createdEvent1 = eventManager.createEvent(eventSeries1, true);
        return eventManager.getEvent(defaultFolderId, createdEvent1.getId());
    }

    private static ChronosConflictDataRaw lookupConflict(List<ChronosConflictDataRaw> conflicts, String conflictingEventId) {
        if (null != conflicts) {
            for (ChronosConflictDataRaw conflict : conflicts) {
                if (null != conflict.getEvent() && Objects.equal(conflictingEventId, conflict.getEvent().getId())) {
                    return conflict;
                }
            }
        }
        return null;
    }

}
