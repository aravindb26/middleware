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

package com.openexchange.groupware.tools.mappings.utils;

import com.openexchange.java.Strings;

/**
 * {@link MappingUtils} - Utility methods for database mappings.
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public class MappingUtils {

    /**
     * No instance.
     */
    private MappingUtils() {
        super();
    }

    /**
     * Ensures that a column is quoted with back-ticks (<code>'`'</code>).
     * <p>
     * Methods accepts string in following formats
     * <code>
     * <li>columnName
     * <li>tableName.coloumnName
     * </code>
     *
     * @param examineMe The column to quote with back-ticks
     * @return The quoted column
     */
    public static String quote(String examineMe) {
        if (Strings.isEmpty(examineMe)) {
            return examineMe;
        }

        String toQuote = examineMe.trim();
        StringBuilder builder = new StringBuilder(toQuote.length() + 4);
        if (toQuote.charAt(0) != '`') {
            builder.append('`');
        }

        // Check if we have a table and if it is quoted correctly
        int dot = toQuote.indexOf('.');
        if (dot < 0) {
            builder.append(toQuote);
        } else {
            if (0 == dot) {
                // Dot with column name, don't add a tick before the dot
                builder.replace(dot, dot + 1, "");
            } else {
                builder.append(toQuote.substring(0, dot));
                String potentialQuote = toQuote.substring(dot - 1, dot);
                if (false == "`".equals(potentialQuote)) {
                    builder.append('`');
                }
            }
            builder.append('.');
            String potentialQuote = toQuote.substring(dot + 1, dot + 2);
            if (false == "`".equals(potentialQuote)) {
                builder.append('`');
            }
            builder.append(toQuote.substring(dot + 1, toQuote.length()));
        }

        if (false == toQuote.endsWith("`")) {
            builder.append('`');
        }

        return builder.toString();
    }

}
