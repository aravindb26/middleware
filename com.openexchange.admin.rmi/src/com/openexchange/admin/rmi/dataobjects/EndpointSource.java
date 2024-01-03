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

package com.openexchange.admin.rmi.dataobjects;

import com.openexchange.java.Strings;

/**
 * {@link EndpointSource} - The end-point source on secondary mail account creatoion.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public enum EndpointSource {

    /**
     * No end-point source to fall-back to. Mail/transport end-point needs to be specified using appropriate arguments.
     */
    NONE("none"),
    /**
     * Mail/transport end-point is assumed to be equal to the primary one.
     */
    PRIMARY("primary"),
    /**
     * Mail/transport end-point is assumed to be localhost using defaults for protocol, port, etc.
     */
    LOCALHOST("localhost"),
    ;

    private final String id;

    private EndpointSource(String id) {
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
     * Gets the end-point source for given identifier.
     *
     * @param endpointSource The identifier
     * @return The end-point source
     */
    public static EndpointSource endpointSourceFor(String endpointSource) {
        if (Strings.isEmpty(endpointSource)) {
            return NONE;
        }

        String lookUp = Strings.asciiLowerCase(endpointSource.trim());
        for (EndpointSource es : EndpointSource.values()) {
            if (es.getId().equals(lookUp)) {
                return es;
            }
        }
        return NONE;
    }
}
