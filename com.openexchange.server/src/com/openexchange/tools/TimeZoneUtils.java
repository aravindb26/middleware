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

package com.openexchange.tools;

import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link TimeZoneUtils} - Utility class for time zone.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class TimeZoneUtils {

    /**
     * Initializes a new {@link TimeZoneUtils}.
     */
    private TimeZoneUtils() {
        super();
    }

    private static final ConcurrentMap<String, TimeZone> ZONE_CACHE = new ConcurrentHashMap<String, TimeZone>();

    /**
     * Gets the <code>TimeZone</code> for the given identifier.
     *
     * @param timeZoneId The identifier for a <code>TimeZone</code>, either an abbreviation such as "PST", a full name such as "America/Los_Angeles", or a
     *            custom identifier such as "GMT-8:00".
     * @return The specified <code>TimeZone</code>, or the GMT zone if the given identifier cannot be understood.
     */
    public static TimeZone getTimeZone(final String timeZoneId) {
        if (null == timeZoneId) {
            return ZONE_CACHE.get("GMT");
        }
        TimeZone tz = ZONE_CACHE.get(timeZoneId);
        if (tz == null) {
            final TimeZone tmp =  TimeZone.getTimeZone(timeZoneId);
            tz = ZONE_CACHE.putIfAbsent(timeZoneId, tmp);
            if (null == tz) {
                tz = tmp;
            }
        }
        return tz;
    }

    /**
     * Adds the time zone offset to given date milliseconds.
     *
     * @param date The date milliseconds
     * @param timeZone The time zone identifier
     * @return The date milliseconds with time zone offset added
     */
    public static long addTimeZoneOffset(final long date, final String timeZone) {
        return addTimeZoneOffset(date, getTimeZone(timeZone));
    }

    /**
     * Adds the time zone offset to given date milliseconds.
     *
     * @param date The date milliseconds
     * @param timeZone The time zone
     * @return The date milliseconds with time zone offset added
     */
    public static long addTimeZoneOffset(final long date, final TimeZone timeZone) {
        return (date + timeZone.getOffset(date));
    }

}
