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

package com.openexchange.time;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.utils.Span;

/**
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 *
 * Copied from the calendar test classes. Need this in Integrated Tests as well.
 */
public final class TimeTools {

    /**
     * Prevent instantiation.
     */
    private TimeTools() {
        super();
    }

    /**
     * @deprecated use {@link #getHour(int, TimeZone)}
     */
    @Deprecated
    public static long getHour(final int diff) {
        return (System.currentTimeMillis() / 3600000 + diff) * 3600000;
    }

    public static long getHour(final int diff, final TimeZone tz) {
        final Calendar calendar = new GregorianCalendar(tz);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.HOUR_OF_DAY, diff);
        return calendar.getTimeInMillis();
    }

    /**
     * Creates a new calendar and sets it to the last current full hour.
     *
     * @param tz TimeZone.
     * @return a calendar set to last full hour.
     */
    public static Calendar createCalendar(final TimeZone tz) {
        final Calendar calendar = new GregorianCalendar(tz);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    private static final String[] patterns = { "dd/MM/yyyy HH:mm", "dd.MM.yyyy HH:mm" };

    public static Calendar createCalendar(TimeZone tz, int year, int month, int day, int hour) {
        Calendar calendar = new GregorianCalendar(tz);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static Date D(final String value, TimeZone timeZone) {
        for (String fallbackPattern : patterns) {
            try {
                final SimpleDateFormat sdf = new SimpleDateFormat(fallbackPattern);
                if (null != timeZone) {
                    sdf.setTimeZone(timeZone);
                }
                return sdf.parse(value);
            } catch (ParseException e) {
                // let Chronic have a try then
            }
        }

        Date date = null;
        final Span span = Chronic.parse(value);
        if (null == span) {
            return null;
        }

        date = span.getBeginCalendar().getTime();

        if (null != timeZone) {
            date = applyTimeZone(timeZone, date);
        }

        return date;

    }

    public static Date D(final String date) {
        return D(date, TimeZone.getTimeZone("UTC"));
    }


    public static Date applyTimeZone(final TimeZone timeZone, final Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat();
        final String dateString = sdf.format(date);
        sdf.setTimeZone(timeZone);
        try {
            return sdf.parse(dateString);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Date removeMilliseconds(final Date roundme) {
        long timestamp = roundme.getTime();
        timestamp /= 1000;
        timestamp *= 1000;
        return new Date(timestamp);
    }
}
