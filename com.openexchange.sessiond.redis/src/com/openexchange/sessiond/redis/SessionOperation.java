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

package com.openexchange.sessiond.redis;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import com.openexchange.java.Strings;

/**
 * {@link SessionOperation} - A session operation being distributed via a session event.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum SessionOperation {

    /**
     * If one or more sessions are invalidated.
     */
    INVALIDATE("invalidate"),
    ;

    private final String id;

    private static final Map<String, SessionOperation> VALUES = Arrays.stream(SessionOperation.values()).collect(Collectors.toMap(SessionOperation::getId, e -> e));

    /**
     * Initializes a new {@link SessionOperation}.
     *
     * @param id The operation id
     */
    private SessionOperation(String id) {
        this.id = id;
    }

    /**
     * Gets the identifier.
     *
     * @return The identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the session operation for given identifier.
     *
     * @param id The identifier
     * @return The session operation or <code>null</code>
     */
    public static SessionOperation operationFor(String id) {
        if (id == null) {
            return null;
        }
        return VALUES.get(Strings.asciiLowerCase(id));
    }

}
