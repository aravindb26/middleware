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

import com.openexchange.java.Strings;

/**
 * {@link TimeOfTheDay} - The time of the day for the 24-hour clock with second granularity; e.g. <code>16:04:15</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public class TimeOfTheDay implements Comparable<TimeOfTheDay> {

    /**
     * Pares given time of the day; e.g. <code>16:04:15</code>.
     *
     * @param timeOfTheDay The time of the day to parse
     * @return The parsed instance
     */
    public static TimeOfTheDay parseFrom(String timeOfTheDay) {
        if (Strings.isEmpty(timeOfTheDay)) {
            throw new IllegalArgumentException("Time of the day must no be null or empty");
        }

        String[] tokens = Strings.splitBy(timeOfTheDay, ':', true);
        if (tokens.length > 3) {
            throw new IllegalArgumentException("Illegal time of the day: " + timeOfTheDay);
        }

        try {
            int hour = Integer.parseInt(tokens[0]);
            if (hour < 0 || hour > 24) {
                throw new IllegalArgumentException("Illegal time of the day: " + timeOfTheDay);
            }

            int minute = 0;
            int second = 0;
            if (tokens.length > 1) {
                minute = Integer.parseInt(tokens[1]);
                if (minute < 0 || minute > 59) {
                    throw new IllegalArgumentException("Illegal time of the day: " + timeOfTheDay);
                }

                if (tokens.length > 2) {
                    second = Integer.parseInt(tokens[2]);
                    if (minute < 0 || minute > 59) {
                        throw new IllegalArgumentException("Illegal time of the day: " + timeOfTheDay);
                    }
                }
            }

            return new TimeOfTheDay(hour, minute, second);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Illegal time of the day: " + timeOfTheDay, e);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------------------

    private final int hour;
    private final int minute;
    private final int second;
    private int hash;

    /**
     * Initializes a new {@link TimeOfTheDay}.
     *
     * @param atHour The hour of day of the 24-hour clock
     * @param atMinute The minute
     * @param atSecond The second
     */
    public TimeOfTheDay(int atHour, int atMinute, int atSecond) {
        super();
        if (atHour < 0 || atHour > 24) {
            throw new IllegalArgumentException("Illegal time of the day: hour=" + atHour + ", minute=" + atMinute + ", second=" + atSecond);
        }
        if (atMinute < 0 || atMinute > 59) {
            throw new IllegalArgumentException("Illegal time of the day: hour=" + atHour + ", minute=" + atMinute + ", second=" + atSecond);
        }
        if (atSecond < 0 || atSecond > 59) {
            throw new IllegalArgumentException("Illegal time of the day: hour=" + atHour + ", minute=" + atMinute + ", second=" + atSecond);
        }
        if (atHour == 24 && (atMinute != 0 || atSecond != 0)) {
            throw new IllegalArgumentException("Illegal time of the day: hour=" + atHour + ", minute=" + atMinute + ", second=" + atSecond);
        }
        this.hour = atHour;
        this.minute = atMinute;
        this.second = atSecond;
        hash = 0;
    }

    /**
     * Gets the hour of the day for the 24-hour clock.
     *
     * @return The hour of the day
     */
    public int getHour() {
        return hour;
    }

    /**
     * Gets the minute within the hour.
     *
     * @return The minute
     */
    public int getMinute() {
        return minute;
    }

    /**
     * Gets the second within the minute.
     *
     * @return The second
     */
    public int getSecond() {
        return second;
    }

    /**
     * Checks if this time of the day is later than given time time of the day.
     *
     * @param o The other time of the day to compare to
     * @return <code>true</code> if later; otherwise <code>false</code>
     */
    public boolean isLaterThan(TimeOfTheDay o) {
        return compareTo(o) > 0;
    }

    /**
     * Checks if this time of the day is earlier than given time time of the day.
     *
     * @param o The other time of the day to compare to
     * @return <code>true</code> if earlier; otherwise <code>false</code>
     */
    public boolean isEarlierThan(TimeOfTheDay o) {
        return compareTo(o) < 0;
    }

    @Override
    public int compareTo(TimeOfTheDay o) {
        int oHour = o.hour;
        if (hour < oHour) {
            return -1;
        } else if (hour > oHour) {
            return 1;
        }

        int oMinute = o.minute;
        if (minute < oMinute) {
            return -1;
        } else if (minute > oMinute) {
            return 1;
        }

        int oSecond = o.second;
        return (second < oSecond) ? -1 : (second == oSecond ? 0 : 1);
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            int prime = 31;
            result = 1;
            result = prime * result + hour;
            result = prime * result + minute;
            result = prime * result + second;
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TimeOfTheDay)) {
            return false;
        }
        TimeOfTheDay other = (TimeOfTheDay) obj;
        if (hour != other.hour) {
            return false;
        }
        if (minute != other.minute) {
            return false;
        }
        if (second != other.second) { // NOSONARLINT
            return false;
        }
        return true;
    }

    /**
     * Checks if this time-of-the-day's end is same beginning of other's day.
     * <p>
     * For instance: Saturday end <code>24:0:0</code> is equal to Sunday start <code>0:0:0</code>
     *
     * @param other The other to check
     * @return <code>true</code> if this time-of-the-day's end is same beginning of other's day; otherwise <code>false</code>
     */
    public boolean isEndBeginningOfOtherDay(TimeOfTheDay other) {
        if (other == null) {
            return false;
        }
        if (hour != 24) {
            return false;
        }
        if (minute != 0) {
            return false;
        }
        if (second != 0) {
            return false;
        }

        if (other.hour != 0) {
            return false;
        }
        if (other.minute != 0) {
            return false;
        }
        if (other.second != 0) { // NOSONARLINT
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(hour).append(':').append(minute).append(':').append(second).toString();
    }

}
