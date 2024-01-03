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

package com.openexchange.gdpr.dataexport.internal;

import static org.junit.Assert.assertTrue;
import java.time.DayOfWeek;
import java.util.List;
import org.junit.Test;
import com.openexchange.gdpr.dataexport.DataExportConfig;
import com.openexchange.schedule.DayOfWeekTimeRanges;
import com.openexchange.schedule.RangesOfTheWeek;
import com.openexchange.schedule.TimeOfTheDay;
import com.openexchange.schedule.TimeRange;

/**
 * {@link DataExportConfigTest}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class DataExportConfigTest {

    /**
     * Initializes a new {@link DataExportConfigTest}.
     */
    public DataExportConfigTest() {
        super();
    }

    @Test
    public void testScheduleParser1() {
        DataExportConfig config = DataExportConfig.builder().parse("Sun-Sat 0-24").build();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            assertTrue(config.getRangesOfTheWeek().getDayOfWeekTimeRangesFor(dayOfWeek) != null);
        }
    }

    @Test
    public void testScheduleParser2() {
        DataExportConfig config = DataExportConfig.builder().parse("Mon-Wed 0-24").build();

        assertTrue(config.getRangesOfTheWeek().getDayOfWeekTimeRangesFor(DayOfWeek.MONDAY) != null);
        assertTrue(config.getRangesOfTheWeek().getDayOfWeekTimeRangesFor(DayOfWeek.TUESDAY) != null);
        assertTrue(config.getRangesOfTheWeek().getDayOfWeekTimeRangesFor(DayOfWeek.WEDNESDAY) != null);
    }

    @Test
    public void testScheduleParser3() {
        DataExportConfig config = DataExportConfig.builder().parse("Mon-Sun 0-6").build();

        RangesOfTheWeek rangesOfTheWeek = config.getRangesOfTheWeek();
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            assertTrue(rangesOfTheWeek.getDayOfWeekTimeRangesFor(dayOfWeek) != null);
        }

        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            DayOfWeekTimeRanges dayOfWeekTimeRanges = rangesOfTheWeek.getDayOfWeekTimeRangesFor(dayOfWeek);

            List<TimeRange> ranges = dayOfWeekTimeRanges.getRanges();
            assertTrue(ranges.size() == 1);

            TimeRange timeRange = ranges.get(0);
            assertTrue(timeRange.getStart().equals(new TimeOfTheDay(0, 0, 0)));
            assertTrue(timeRange.getEnd().equals(new TimeOfTheDay(6, 0, 0)));
        }
    }

    @Test
    public void testScheduleParser4() {
        DataExportConfig config = DataExportConfig.builder().parse("Fri 0-6,22:30-24").build();
        assertTrue(config.getRangesOfTheWeek().getDayOfWeekTimeRangesFor(DayOfWeek.FRIDAY) != null);
    }

    @Test
    public void testScheduleParser5() {
        DataExportConfig config = DataExportConfig.builder().parse("Mo 0-6,6-12,12-15").build();
        DayOfWeekTimeRanges dayOfWeekTimeRanges = config.getRangesOfTheWeek().getDayOfWeekTimeRangesFor(DayOfWeek.MONDAY);
        assertTrue(dayOfWeekTimeRanges != null);
        assertTrue(dayOfWeekTimeRanges.getRanges().size() == 1);
    }

}
