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

package com.openexchange.chronos;

import com.openexchange.java.Strings;

/**
 * {@link TimeTransparency}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 * @see <a href="https://tools.ietf.org/html/rfc5545#section-3.8.2.7">RFC 5545, section 3.8.2.7</a>
 */
public enum TimeTransparency implements Transp {

    /**
     * Blocks or opaque on busy time searches.
     */
    OPAQUE(Transp.OPAQUE),

    /**
     * Transparent on busy time searches.
     */
    TRANSPARENT(Transp.TRANSPARENT),

    ;

    private final String value;

    /**
     * Initializes a new {@link TimeTransparency}.
     *
     * @param value The transparency value
     */
    private TimeTransparency(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    /**
     * Parses the specified value to a {@link TimeTransparency} enum item
     * 
     * @param value The value to parse
     * @return The {@link TimeTransparency} that corresponds to that value or <code>null</code>
     */
    public static TimeTransparency parse(String value) {
        if (Strings.isEmpty(value)) {
            return null;
        }
        for (TimeTransparency transparency : values()) {
            if (transparency.getValue().equalsIgnoreCase(value)) {
                return transparency;
            }
        }
        return null;
    }

}
