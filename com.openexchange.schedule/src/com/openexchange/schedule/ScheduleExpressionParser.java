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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.openexchange.java.Strings;

/**
 * {@link ScheduleExpressionParser} - A parser for a schedule expression; e.g.
 *
 * <pre>"Mon 0:12-6:45; Tue-Thu 0-7:15; Fri 0-6,22:30-24; Sat,Sun 0-8"</pre>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public final class ScheduleExpressionParser {

    /**
     * Initializes a new {@link ScheduleExpressionParser}.
     */
    private ScheduleExpressionParser() {
        super();
    }

    /**
     * Parses the given schedule expression; e.g. <code>"Mon 0:12-6:45; Tue-Thu 0-7:15; Fri 0-6,22:30-24; Sat,Sun 0-8"</code>.
     *
     * @param scheduleExpression The schedule expression to parse
     * @return A map of day of week to corresponding time range
     * @throws IllegalArgumentException If schedule information is invalid
     */
    public static RangesOfTheWeek parse(String scheduleExpression) {
        if (Strings.isEmpty(scheduleExpression)) {
            return RangesOfTheWeek.EMPTY_SCHEDULE;
        }

        // The map carrying parsed results
        Map<DayOfWeek, DayOfWeekTimeRanges> rangesOfTheWeek = new EnumMap<>(DayOfWeek.class);

        // Mon 0:12-6:45; Tue-Thu 0-7:15; Fri 0-6,22:30-24; Sat,Sun 0-8
        for (String part : Strings.splitBy(scheduleExpression, ';', true)) {
            parsePart(part, rangesOfTheWeek);
        }

        return new RangesOfTheWeek(Maps.immutableEnumMap(rangesOfTheWeek));
    }

    /**
     * Parses the given configuration part; e.g. <code>"Tue-Thu,Fri 0-6,22:30-24"</code>
     *
     * @param part The configuration part to parse
     * @param rangesOfTheWeek The map carrying parsed results
     * @throws IllegalArgumentException If schedule information is invalid
     */
    private static void parsePart(String part, Map<DayOfWeek, DayOfWeekTimeRanges> rangesOfTheWeek) {
        if (Strings.isEmpty(part)) {
            return;
        }

        // Tue-Thu,Fri 0-6,22:30-24
        // Tue,Thu 0-6,22:30-24
        String daysOfWeek;
        String hoursOfDay;

        int length = part.length();
        int pos = -1;
        for (int i = 0; pos < 0 && i < length; i++) {
            if (Strings.isDigit(part.charAt(i))) {
                pos = i;
            }
        }

        if (pos < 0) {
            daysOfWeek = part;
            hoursOfDay = "0-24";
        } else {
            daysOfWeek = part.substring(0, pos).trim();
            hoursOfDay = part.substring(pos);
        }

        List<DayOfWeek> applicableDaysOfWeek = parseAndSortDaysOfWeek(daysOfWeek, part);
        List<TimeRange> applicableTimeRanges = parseAndSortTimeRanges(hoursOfDay, part);
        for (DayOfWeek dayOfWeek : applicableDaysOfWeek) {
            DayOfWeekTimeRanges dayOfWeekTimeRanges = new DayOfWeekTimeRanges(dayOfWeek, applicableTimeRanges);
            rangesOfTheWeek.put(dayOfWeek, dayOfWeekTimeRanges);
        }
    }

    /**
     * Parses the applicable days of week from given days of week portion of the configuration part.
     *
     * @param daysOfWeek The days of week to parse
     * @param part The configuration part from which given days of week was extracted; e.g. <code>"Tue-Thu,Fri 0-6,22:30-24"</code>
     * @return The applicable days of week
     */
    private static List<DayOfWeek> parseAndSortDaysOfWeek(String daysOfWeek, String part) {
        List<DayOfWeek> applicableDaysOfWeek = new ArrayList<>(7);

        int pos;
        for (String token : Strings.splitBy(daysOfWeek, ',', true)) {
            pos = token.indexOf('-');
            if (pos > 0) {
                // A range
                DayOfWeek day1 = getDayOfWeekFor(token.substring(0, pos));
                DayOfWeek day2 = getDayOfWeekFor(token.substring(pos+1));
                if (day2.getValue() == day1.getValue()) {
                    throw new IllegalArgumentException("Illegal days of week range in part: " + part);
                }
                DayOfWeek dayOfWeek = day1;
                // Add all days in-between
                while (dayOfWeek != day2) {
                    checkIfAlreadyContained(dayOfWeek, applicableDaysOfWeek, part);
                    applicableDaysOfWeek.add(dayOfWeek);
                    dayOfWeek = dayOfWeek.plus(1);
                }
                checkIfAlreadyContained(dayOfWeek, applicableDaysOfWeek, part);
                applicableDaysOfWeek.add(dayOfWeek);
            } else {
                DayOfWeek dayOfWeek = getDayOfWeekFor(token);
                checkIfAlreadyContained(dayOfWeek, applicableDaysOfWeek, part);
                applicableDaysOfWeek.add(dayOfWeek);
            }
        }


        // Sort them
        Collections.sort(applicableDaysOfWeek, new Comparator<DayOfWeek>() {

            @Override
            public int compare(DayOfWeek o1, DayOfWeek o2) {
                return Integer.compare(o1.getValue(), o2.getValue());
            }
        });
        return applicableDaysOfWeek;
    }

    /**
     * Checks if given day of week is already contained in specified collection.
     *
     * @param dayOfWeek The day of week to check for
     * @param applicableDaysOfWeek The collection to check against
     * @param part The configuration part that is processed; e.g. <code>"Tue-Thu,Fri 0-6,22:30-24"</code>
     */
    private static void checkIfAlreadyContained(DayOfWeek dayOfWeek, List<DayOfWeek> applicableDaysOfWeek, String part) {
        if (!applicableDaysOfWeek.isEmpty() && applicableDaysOfWeek.contains(dayOfWeek)) {
            throw new IllegalArgumentException("Duplicate days of week in part: " + part);
        }
    }

    /**
     * Parses the applicable time ranges from given time range portion of the configuration part.
     *
     * @param hoursOfDay The hours of the day to parse
     * @param part The configuration part from which given days of week was extracted; e.g. <code>"Tue-Thu,Fri 0-6,22:30-24"</code>
     * @return The applicable time ranges
     */
    private static List<TimeRange> parseAndSortTimeRanges(String hoursOfDay, String part) {
        List<TimeRange> applicableTimeRanges = new ArrayList<>(2);

        for (String token : Strings.splitBy(hoursOfDay, ',', true)) {
            TimeRange timeRange = TimeRange.parseFrom(token);
            if (!applicableTimeRanges.isEmpty()) {
                for (TimeRange anotherRange : applicableTimeRanges) {
                    if (anotherRange.overlapsWith(timeRange)) {
                        throw new IllegalArgumentException("Overlapping time ranges in part: " + part);
                    }
                }
            }
            applicableTimeRanges.add(timeRange);
        }

        Collections.sort(applicableTimeRanges);
        applicableTimeRanges = TimeRange.foldTimeRanges(applicableTimeRanges);
        return applicableTimeRanges;
    }

    private static final Set<String> DAY_OF_WEEK_MONDAY = ImmutableSet.of("mo", "mon", "monday"); // NOSONARLINT
    private static final Set<String> DAY_OF_WEEK_TUESDAY = ImmutableSet.of("tu", "tue", "tuesday"); // NOSONARLINT
    private static final Set<String> DAY_OF_WEEK_WEDNESDAY = ImmutableSet.of("we", "wed", "wednesday"); // NOSONARLINT
    private static final Set<String> DAY_OF_WEEK_THURSDAY = ImmutableSet.of("th", "thu", "thursday"); // NOSONARLINT
    private static final Set<String> DAY_OF_WEEK_FRIDAY = ImmutableSet.of("fr", "fri", "friday"); // NOSONARLINT
    private static final Set<String> DAY_OF_WEEK_SATURDAY = ImmutableSet.of("sa", "sat", "saturday"); // NOSONARLINT
    private static final Set<String> DAY_OF_WEEK_SUNDAY = ImmutableSet.of("su", "sun", "sunday"); // NOSONARLINT

    /**
     * Parses specified day of week to associated calendar constant.
     *
     * @param day The day of week to parse
     * @return The calendar constant
     * @throws IllegalArgumentException If given day of week cannot be parsed to a calendar constant
     * @see DayOfWeek#SUNDAY
     * @see DayOfWeek#MONDAY
     * @see DayOfWeek#TUESDAY
     * @see DayOfWeek#WEDNESDAY
     * @see DayOfWeek#THURSDAY
     * @see DayOfWeek#FRIDAY
     * @see DayOfWeek#SATURDAY
     */
    private static DayOfWeek getDayOfWeekFor(String day) {
        if (day == null) {
            throw new IllegalArgumentException("The day to parse must not be null");
        }
        String toCheck = Strings.asciiLowerCase(day.trim());
        if (DAY_OF_WEEK_MONDAY.contains(toCheck)) {
            return DayOfWeek.MONDAY;
        }
        if (DAY_OF_WEEK_TUESDAY.contains(toCheck)) {
            return DayOfWeek.TUESDAY;
        }
        if (DAY_OF_WEEK_WEDNESDAY.contains(toCheck)) {
            return DayOfWeek.WEDNESDAY;
        }
        if (DAY_OF_WEEK_THURSDAY.contains(toCheck)) {
            return DayOfWeek.THURSDAY;
        }
        if (DAY_OF_WEEK_FRIDAY.contains(toCheck)) {
            return DayOfWeek.FRIDAY;
        }
        if (DAY_OF_WEEK_SATURDAY.contains(toCheck)) {
            return DayOfWeek.SATURDAY;
        }
        if (DAY_OF_WEEK_SUNDAY.contains(toCheck)) {
            return DayOfWeek.SUNDAY;
        }
        throw new IllegalArgumentException("Cannot be parsed to a day of week: " + day);
    }

}
