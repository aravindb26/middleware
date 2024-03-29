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

package com.openexchange.mail;

import com.openexchange.java.Strings;

public enum OrderDirection {

    /**
     * Ascending order
     */
    ASC(1),
    /**
     * Descending order
     */
    DESC(2);

    private final int order;

    private OrderDirection(int order) {
        this.order = order;
    }

    /**
     * @return The order direction's <code>int</code> value
     */
    public int getOrder() {
        return order;
    }

    /**
     * Get the corresponding order direction
     *
     * @param order The order <code>int</code> value
     * @return The corresponding order direction or <code>null</code>
     */
    public static final OrderDirection getOrderDirection(int order) {
        for (OrderDirection direction : OrderDirection.values()) {
            if (direction.order == order) {
                return direction;
            }
        }
        return null;
    }

    /**
     * Get the corresponding order direction
     *
     * @param order The order; either (ignore-case) <code>"asc"</code> or <code>"desc"</code>
     * @return The corresponding order direction or <code>null</code>
     */
    public static final OrderDirection getOrderDirection(String order) {
        if (Strings.isEmpty(order)) {
            return null;
        }
        String ordr = Strings.asciiLowerCase(order.trim());
        if ("asc".equalsIgnoreCase(ordr)) {
            return OrderDirection.ASC;
        }
        if ("desc".equalsIgnoreCase(ordr)) {
            return OrderDirection.DESC;
        }
        return null;
    }
}
