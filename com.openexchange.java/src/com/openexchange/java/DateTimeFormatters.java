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


package com.openexchange.java;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * {@link DateTimeFormatters} - Utility class for <code>java.time.format.DateTimeFormatter</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DateTimeFormatters {

    /**
     * Initializes a new {@link DateTimeFormatters}.
     */
    private DateTimeFormatters() {
        super();
    }

    /**
     * Get the number of milliseconds from the epoch of 1970-01-01T00:00:00Z for specified date string using given formatter.
     *
     * @param dateString The date string; e.g. <code>"Tue, 3 Jun 2008 11:05:30 GMT"</code>
     * @param formatter The formatter to use; e.g. <code>DateTimeFormatter.RFC_1123_DATE_TIME</code>
     * @return The number of milliseconds from the epoch of 1970-01-01T00:00:00Z
     * @throws DateTimeException If unable to parse the requested result or unable to convert to an {@code Instant}
     */
    public static long toEpochMillis(String dateString, DateTimeFormatter formatter) {
        if (Strings.isEmpty(dateString)) {
            throw new IllegalArgumentException("Date string must not be null or empty");
        }
        if (formatter == null) {
            throw new IllegalArgumentException("Formatter must not be null");
        }
        return Instant.from(formatter.parse(dateString)).toEpochMilli();
    }

}
