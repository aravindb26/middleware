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

package com.openexchange.ldap.common.config;

import java.util.Arrays;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;

/**
 * {@link LDAPConnectionPoolType} contains the possible connection pool types supported
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public enum LDAPConnectionPoolType {

    /**
     * A connection pool containing only a single server
     */
    simple,
    /**
     * A connection pool with a server set which is resolved using dns and which selects servers using round robin
     */
    dnsRoundRobin,
    /**
     * A connection pool with a server set which selects servers using round robin
     */
    roundRobin,
    /**
     * A connection pool with a server set which selects servers with the least connections
     */
    fewestConnections,
    /**
     * A connection pool with a server set which selects servers in order. The servers beside the first are therefore only used as failovers
     */
    failover,
    /**
     * A connection pool with a server set containing a different pool for read and write operations
     */
    readWrite;

    /**
     * Gets the {@link LDAPConnectionPoolType} for the given name
     *
     * @param name
     * @return The {@link LDAPConnectionPoolType}
     * @throws OXException if no {@link LDAPConnectionPoolType} exists with the given name
     */
    public static LDAPConnectionPoolType forName(String name) throws OXException {
        return Arrays.asList(values()).stream().filter(type -> type.name().toLowerCase().equals(name.toLowerCase())).findFirst().orElseThrow(() -> LDAPCommonErrorCodes.INVALID_CONFIG.create());
    }
}
