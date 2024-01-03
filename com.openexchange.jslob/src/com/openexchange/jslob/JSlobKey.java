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

package com.openexchange.jslob;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.openexchange.java.Strings;

/**
 * {@link JSlobKey} - An enumeration of all available JSlob keys.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public enum JSlobKey {

    /**
     * The JSlob key for core: <code>"io.ox/core"</code>
     */
    CORE("io.ox/core"),
    /**
     * The JSlob key for folder: <code>"io.ox/folder"</code>
     */
    FOLDER("io.ox/folder"),
    /**
     * The JSlob key for mail: <code>"io.ox/mail"</code>
     */
    MAIL("io.ox/mail"),
    /**
     * The JSlob key for calendar: <code>"io.ox/calendar"</code>
     */
    CALENDAR("io.ox/calendar"),
    /**
     * The JSlob key for tasks: <code>"io.ox/tasks"</code>
     */
    TASKS("io.ox/tasks"),
    /**
     * The JSlob key for contacts: <code>"io.ox/contacts"</code>
     */
    CONTACTS("io.ox/contacts"),
    /**
     * The JSlob key for files: <code>"io.ox/files"</code>
     */
    FILES("io.ox/files"),
    ;

    private final String identifier;

    private JSlobKey(String identifier) {
        this.identifier = identifier;
    }

    /**
     * Gets the identifier for this JSlob key.
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final Map<String, JSlobKey> keys;

    static {
        JSlobKey[] values = JSlobKey.values();
        ImmutableMap.Builder<String, JSlobKey> m = ImmutableMap.builderWithExpectedSize(values.length);
        for (JSlobKey value : values) {
            m.put(value.getIdentifier(), value);
        }
        keys = m.build();
    }

    /**
     * Gets the JSlob key for given identifier.
     *
     * @param identifier The identifier to look-up by
     * @return The JSlob key or <code>null</code>
     */
    public static JSlobKey jslobKeyFor(String identifier) {
        return identifier == null ? null : keys.get(Strings.asciiLowerCase(identifier));
    }

}
