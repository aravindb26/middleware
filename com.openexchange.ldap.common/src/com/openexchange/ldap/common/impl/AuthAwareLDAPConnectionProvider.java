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

package com.openexchange.ldap.common.impl;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.BindRequestFactory;
import com.openexchange.ldap.common.LDAPCommonErrorCodes;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.session.Session;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.LDAPReadWriteConnectionPool;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link AuthAwareLDAPConnectionProvider} is a auth aware {@link LDAPConnectionProvider} and performs a bind operation on the connection if necessary
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class AuthAwareLDAPConnectionProvider implements LDAPConnectionProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AuthAwareLDAPConnectionProvider.class);

    private static final String NAMING_CONTEXTS = "namingContexts";
    private static final String DEFAULT_NAMING_CONTEXT = "defaultNamingContext";

    private final LDAPInterface pool;
    private final Optional<BindRequestFactory> bindFactory;
    private final String baseDN;

    /**
     * Initializes a new {@link AuthAwareLDAPConnectionProvider}.
     *
     * @param pool The ldap connection pool
     * @param bindFactory The optional {@link BindRequestFactory}
     * @param baseDN The optional base dn
     * @throws OXException in case of errors during base dn discovery
     */
    public AuthAwareLDAPConnectionProvider(LDAPInterface pool, BindRequestFactory bindFactory, String baseDN) throws OXException {
        super();
        this.pool = pool;
        this.bindFactory = Optional.ofNullable(bindFactory);
        this.baseDN = baseDN == null ? discoverBaseDN() : baseDN;
    }

    /**
     * Discovers the default naming context of the ldap server
     *
     * @return The default naming context
     * @throws OXException in case the naming context couldn't be discovered
     */
    private String discoverBaseDN() throws OXException {
        try {
            SearchResult result = this.pool.search("", SearchScope.BASE, "(objectClass=*)", DEFAULT_NAMING_CONTEXT, NAMING_CONTEXTS);
            List<SearchResultEntry> searchEntries = result.getSearchEntries();
            for (SearchResultEntry entry : searchEntries) {
                Attribute attribute = entry.getAttribute(DEFAULT_NAMING_CONTEXT);
                if (attribute != null && attribute.getValue() != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Discovered defaultNamingContext attribute with value '{}'", attribute.getValue());
                    }
                    return attribute.getValue();
                }
                attribute = entry.getAttribute(NAMING_CONTEXTS);
                if (attribute != null && attribute.getValues().length > 0) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Discovered namingContexts attribute with value '{}'", String.join(",", attribute.getValues()));
                    }
                    return attribute.getValues()[0];
                }
            }
        } catch (LDAPSearchException e) {
            throw LDAPCommonErrorCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
        throw LDAPCommonErrorCodes.INVALID_CONFIG.create();
    }

    @Override
    public LDAPConnection getConnection(Session session) throws OXException {
        return getConnection(session, false);
    }

    @Override
    public LDAPConnection getWriteConnection(Session session) throws OXException {
        return getConnection(session, true);
    }

    /**
     * Gets the connection and performs a bind if necessary
     *
     * @param session The users session
     * @param write Whether a write or read connection is requested
     * @return The properly binded connection
     * @throws OXException in case no connection could be retrieved or in case the bind operation fails
     */
    private LDAPConnection getConnection(Session session, boolean write) throws OXException {
        try {
            LDAPConnection result = getConnection(write);
            if (bindFactory.isPresent()) {
                result.bind(bindFactory.get().createBindRequest(session));
            }
            return result;
        } catch (LDAPException e) {
            throw LDAPCommonErrorCodes.CONNECTION_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets a connection with default bind
     *
     * @param write Whether a write or read connection is requested
     * @return The connection
     * @throws LDAPException in case no connection could be retrieved from the pool
     */
    private LDAPConnection getConnection(boolean write) throws LDAPException {
        if (this.pool instanceof LDAPReadWriteConnectionPool) {
            return write ? ((LDAPReadWriteConnectionPool) pool).getWriteConnection() : ((LDAPReadWriteConnectionPool) pool).getReadConnection();
        }
        return ((LDAPConnectionPool) pool).getConnection();
    }

    @Override
    public void back(LDAPConnection connection) {
        back(connection, false);
    }

    @Override
    public void backWriteConnection(LDAPConnection connection) {
        back(connection, true);
    }

    /**
     * Releases the connection back to the pool
     *
     * @param connection The connection to return
     * @param write Whether it was a write or read connection
     */
    private void back(LDAPConnection connection, boolean write) {
        if (connection == null) {
            return;
        }

        if (bindFactory.isPresent()) {
            if (this.pool instanceof LDAPReadWriteConnectionPool) {
                if (write) {
                    ((LDAPReadWriteConnectionPool) this.pool).getWritePool().releaseAndReAuthenticateConnection(connection);
                    return;
                }
                ((LDAPReadWriteConnectionPool) this.pool).getReadPool().releaseAndReAuthenticateConnection(connection);
                return;
            }
            ((LDAPConnectionPool) this.pool).releaseAndReAuthenticateConnection(connection);
            return;
        }

        if (this.pool instanceof LDAPReadWriteConnectionPool) {
            if (write) {
                ((LDAPReadWriteConnectionPool) this.pool).getWritePool().releaseAndReAuthenticateConnection(connection);
                return;
            }
            ((LDAPReadWriteConnectionPool) this.pool).getReadPool().releaseAndReAuthenticateConnection(connection);
            return;
        }
        ((LDAPConnectionPool) this.pool).releaseAndReAuthenticateConnection(connection);

    }

    @Override
    public boolean isIndividualBind() {
        return bindFactory.isPresent();
    }

    @Override
    public String getBaseDN() {
        return baseDN;
    }

}
