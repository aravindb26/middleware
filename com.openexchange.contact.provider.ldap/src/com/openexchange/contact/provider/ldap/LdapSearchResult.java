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

package com.openexchange.contact.provider.ldap;

import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.unboundid.ldap.sdk.SearchResultEntry;

/**
 * {@link LdapSearchResult}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class LdapSearchResult {

    private final LDAPConnectionProvider connectionProvider;
    private final SearchResultEntry searchEntry;

    /**
     * Initializes a new {@link LdapSearchResult}.
     * 
     * @param connectionProvider The underlying connection provider for the the LDAP server
     * @param searchEntry The wrapped search result entry
     */
    public LdapSearchResult(LDAPConnectionProvider connectionProvider, SearchResultEntry searchEntry) {
        super();
        this.connectionProvider = connectionProvider;
        this.searchEntry = searchEntry;
    }

    /**
     * Gets the underlying connection provider for the the LDAP server.
     * 
     * @return The connection provider
     */
    public LDAPConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    /**
     * Gets the wrapped search result entry
     * 
     * @return The search entry
     */
    public SearchResultEntry getSearchEntry() {
        return searchEntry;
    }

    @Override
    public String toString() {
        return "LdapSearchResult [searchEntry=" + searchEntry + "]";
    }

}
