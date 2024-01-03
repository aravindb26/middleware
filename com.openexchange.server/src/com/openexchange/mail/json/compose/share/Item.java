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

package com.openexchange.mail.json.compose.share;

/**
 * Represents an item associated with a share reference.
 */
public class Item {

    private final String id;
    private final String name;

    /**
     * Initializes a new {@link Item}.
     *
     * @param id The item identifier
     * @param name The name
     */
    public Item(String id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    /**
     * Gets the identifier
     *
     * @return The identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(32);
        builder.append('[');
        if (id != null) {
            builder.append("id=").append(id).append(", ");
        }
        if (name != null) {
            builder.append("name=").append(name);
        }
        builder.append(']');
        return builder.toString();
    }
}