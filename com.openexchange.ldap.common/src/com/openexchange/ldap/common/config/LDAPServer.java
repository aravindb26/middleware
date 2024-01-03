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

import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.ldap.common.config.ConfigUtils.get;
import static com.openexchange.ldap.common.config.ConfigUtils.opt;
import java.util.Map;
import com.openexchange.exception.OXException;

/**
 *
 * {@link LDAPServer} is a wrapper for ldap host configuration
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LDAPServer {

    private final String host;
    private final int port;

    /**
     * Creates a new {@link LDAPServer} from the given configuration entry
     *
     * @param configEntry The configuration entry
     * @return The new {@link LDAPServer}
     * @throws OXException in case the configuration is invalid
     */
    public static LDAPServer init(Map<String, Object> configEntry) throws OXException {
        // @formatter:off
        return new LDAPServer(get(configEntry, "address"),
                              opt(configEntry, "port", Integer.class));
        // @formatter:on
    }

    /**
     * Initializes a new {@link LDAPServer}.
     *
     * @param host The host of the ldap server
     * @param port The port of the ldap server
     */
    private LDAPServer(String host, Integer port) {
        super();
        this.host = host;
        this.port = port == null ? 10389 : i(port);
    }

    /**
     * Gets the host
     *
     * @return The host
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port
     *
     * @return The port
     */
    public int getPort() {
        return port;
    }

}