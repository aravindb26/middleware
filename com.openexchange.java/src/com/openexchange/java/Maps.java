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

import java.util.Map;

/**
 * 
 * {@link Maps} - Utilities for maps
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public final class Maps {

    /**
     * Initializes a new {@link Maps}.
     *
     */
    private Maps() {
        super();
    }

    /**
     * Gets a value indicating whether the supplied map reference is <code>null</code> or does not contain any element.
     *
     * @param map The map to check
     * @return <code>true</code> if the map is <code>null</code> or empty, <code>false</code>, otherwise
     */
    public static boolean isNullOrEmpty(Map<?, ?> map) {
        return null == map || map.isEmpty();
    }

    /**
     * Gets a value indicating whether the supplied map reference does contain at least one element or not
     *
     * @param map The map to check
     * @return <code>true</code> if the map is <b>not</b> <code>null</code> and <b>not</b> empty, <code>false</code> if <code>null</code> or empty
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return null != map && false == map.isEmpty();
    }

}
