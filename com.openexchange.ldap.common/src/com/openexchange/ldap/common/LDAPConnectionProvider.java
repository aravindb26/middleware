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
import com.openexchange.session.Session;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPInterface;

/**
 * {@link LDAPConnectionProvider} - Provides read-only and read-write connection to the LDAP end-point.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public interface LDAPConnectionProvider {

    /**
     * Gets an LDAP read connection.
     *
     * @param session The session
     * @return The {@link LDAPInterface}
     * @throws OXException If a bind operation is necessary, but the bind operation failed
     */
    LDAPConnection getConnection(Session session) throws OXException;

    /**
     * Gets an LDAP write connection.
     *
     * @param session The session
     * @return The {@link LDAPInterface}
     * @throws OXException If a bind operation is necessary, but the bind operation failed
     */
    LDAPConnection getWriteConnection(Session session) throws OXException;

    /**
     * Returns the read connection to this provider.
     *
     * @param connection The read connection to return
     */
    void back(LDAPConnection connection);

    /**
     * Returns the write connection to this provider.
     *
     * @param connection The write connection to return
     */
    void backWriteConnection(LDAPConnection connection);

    /**
     * Whether an individual bind is used.
     *
     * @return <code>true</code> if it is an individual bind, <code>false</code> otherwise
     */
    boolean isIndividualBind();

    /**
     * Gets the base distinguished name (DN).
     *
     * @return The base DN
     */
    String getBaseDN();

}
