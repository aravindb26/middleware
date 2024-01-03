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
package com.openexchange.apn.common.impl;

import java.util.Arrays;
import com.openexchange.java.Strings;

/**
 * The authentication types supported by APNs
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public enum AuthType {
    /**
     * Connect to APNs using provider certificates
     */
    CERTIFICATE("certificate"),
    /**
     * Connect to APNs using provider authentication JSON Web Token (JWT)
     */
    JWT("jwt"),
    /**
     * The fallback type in case the type is unknown
     */
    UNKNOWN("unknown")
    ;

    private final String id;

    /**
     * Initializes a new {@link AuthType}.
     *
     * @param id The id of the auth type
     */
    private AuthType(String id) {
        this.id = id;
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
     * Gets the authentication type for specified identifier.
     *
     * @param id The identifier
     * @return The authentication type or {@link #UNKNOWN}
     */
    public static AuthType authTypeFor(String id) {
        if (Strings.isEmpty(id)) {
            return UNKNOWN;
        }
        // @formatter:off
        return Arrays.asList(AuthType.values()).stream()
                                               .filter(type -> type.id.equalsIgnoreCase(id))
                                               .findAny()
                                               .orElse(UNKNOWN);
        // @formatter:on
    }
}
