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

package com.openexchange.chronos.provider;

/**
 * 
 * {@link CalendarAccountAttribute}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.0
 */
public enum CalendarAccountAttribute {
    ID_LITERAL("id"),
    PROVIDER_LITERAL("provider"),
    LOGIN_LITERAL("login"),
    PASSWORD_LITERAL("password"),
    TOKEN_LITERAL("token"),
    ;

    private final String attrName;

    private CalendarAccountAttribute(String name) {
        attrName = name;
    }

    /**
     * Gets the attribute name.
     *
     * @return The name
     */
    public String getName() {
        return attrName;
    }

}
