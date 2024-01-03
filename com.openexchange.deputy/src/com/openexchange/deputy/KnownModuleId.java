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

package com.openexchange.deputy;

import com.openexchange.java.Strings;

/**
 * {@link KnownModuleId} - An enumeration for known module identifiers.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public enum KnownModuleId {

    /**
     * The identifier for mail module.
     */
    MAIL("mail"),
    /**
     * The identifier for calendar module.
     */
    CALENDAR("calendar"),
    /**
     * The identifier for contacts module.
     */
    CONTACTS("contacts"),
    /**
     * The identifier for tasks module.
     */
    TASKS("tasks"),
    /**
     * The identifier for drive module.
     */
    DRIVE("drive"),
    ;

    private final String id;

    private KnownModuleId(String id) {
        this.id = id;
    }

    /**
     * Gets the module identifier.
     *
     * @return The module identifier
     */
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    /**
     * Gets the known module identifier for given identifier.
     *
     * @param id The identifier to look-up
     * @return The known module identifier or <code>null</code>
     */
    public static KnownModuleId getKnownModuleIdFor(String id) {
        if (Strings.isEmpty(id)) {
            return null;
        }

        String lcid = Strings.asciiLowerCase(id.trim());
        for (KnownModuleId knownModuleId : KnownModuleId.values()) {
            if (knownModuleId.id.equals(lcid)) {
                return knownModuleId;
            }
        }
        return null;
    }

}
