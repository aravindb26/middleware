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


package com.openexchange.schedule;

import java.time.DayOfWeek;
import java.util.Calendar;
import java.util.Collections;
import java.util.Map;

/**
 * {@link RangesOfTheWeek} - A parsed schedule. Mainly a mapping for day-of-the-week to associated time ranges on that day.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public final class RangesOfTheWeek {

    /** The constant for empty schedule */
    public static final RangesOfTheWeek EMPTY_SCHEDULE = new RangesOfTheWeek(Collections.emptyMap());

    private final Map<DayOfWeek, DayOfWeekTimeRanges> map;

    /**
     * Initializes a new {@link RangesOfTheWeek}.
     *
     * @param rangesOfTheWeek The time ranges of the week
     */
    RangesOfTheWeek(Map<DayOfWeek, DayOfWeekTimeRanges> rangesOfTheWeek) {
        super();
        this.map = rangesOfTheWeek;
    }

    /**
     * Gets the time range for specified {@link Calendar#DAY_OF_WEEK} value.
     *
     * @param dayOfTheWeek The value for the day of the week
     * @return The time range or <code>null</code>
     * @throws IllegalArgumentException If given value for the day of the week is invalid
     *
     * @see Calendar#SUNDAY
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     */
    public DayOfWeekTimeRanges getDayOfWeekTimeRangesFor(int dayOfTheWeek) {
        return map.get(dayOfWeekFor(dayOfTheWeek));
    }

    /**
     * Gets the time range for specified day of the week.
     *
     * @param dayOfWeek The day of the week
     * @return The time range or <code>null</code>
     */
    public DayOfWeekTimeRanges getDayOfWeekTimeRangesFor(DayOfWeek dayOfWeek) {
        return dayOfWeek == null ? null : map.get(dayOfWeek);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (DayOfWeekTimeRanges dayOfWeekTimeRanges : map.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(dayOfWeekTimeRanges);
        }
        return sb.toString();
    }

    /**
     * Gets the appropriate {@link DayOfWeek} instance for given {@link Calendar#DAY_OF_WEEK} value.
     *
     * @param dayOfTheWeek The value for the day of the week
     * @return The appropriate {@link DayOfWeek} instance
     * @throws IllegalArgumentException If given value for the day of the week is invalid
     *
     * @see Calendar#SUNDAY
     * @see Calendar#MONDAY
     * @see Calendar#TUESDAY
     * @see Calendar#WEDNESDAY
     * @see Calendar#THURSDAY
     * @see Calendar#FRIDAY
     * @see Calendar#SATURDAY
     */
    private static DayOfWeek dayOfWeekFor(int dayOfTheWeek) {
        switch (dayOfTheWeek) {
            case Calendar.SUNDAY:
                return DayOfWeek.SUNDAY;
            case Calendar.MONDAY:
                return DayOfWeek.MONDAY;
            case Calendar.TUESDAY:
                return DayOfWeek.TUESDAY;
            case Calendar.WEDNESDAY:
                return DayOfWeek.WEDNESDAY;
            case Calendar.THURSDAY:
                return DayOfWeek.THURSDAY;
            case Calendar.FRIDAY:
                return DayOfWeek.FRIDAY;
            case Calendar.SATURDAY:
                return DayOfWeek.SATURDAY;
            default:
                throw new IllegalArgumentException("Not a valid value for java.util.Calendar.DAY_OF_WEEK: " + dayOfTheWeek);
        }
    }

}
