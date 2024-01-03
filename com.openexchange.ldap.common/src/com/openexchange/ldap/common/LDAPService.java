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

package com.openexchange.ldap.common;

import com.openexchange.exception.OXException;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * The {@link LDAPService} serves LDAP connection providers.
 * <p>
 * Instead of a connection a {@link LDAPConnectionProvider} is returned. This holder can be stored locally by the caller of this interface
 * but be aware that this {@link LDAPConnectionProvider} is not refreshed in case the connection is reloaded.
 * <p>
 * A {@link LDAPConnectionProvider} must be used in the following way:
 *
 * <pre>
 * LDAPConnectionProvider connectionHolder = ldapService.getConnection("myConnectionId");
 * LDAPInterface connection = connectionHolder.getConnection(session);
 * try {
 *    // do something with the connection
 * } finally {
 *    connectionHolder.back(connection);
 * }
 * </pre>
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
@SingletonService
public interface LDAPService {

    /**
     * Gets the LDAP connection for the given identifier referencing to a certain LDAP server.
     *
     * @param id The identifier
     * @return The {@link LDAPConnectionProvider}
     * @throws OXException If no connection for the given identifier exists
     */
    public LDAPConnectionProvider getConnection(String id) throws OXException;

}
