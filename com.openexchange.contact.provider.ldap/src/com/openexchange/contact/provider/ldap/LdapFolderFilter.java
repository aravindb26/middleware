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

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;

/**
 * {@link LdapFolderFilter}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class LdapFolderFilter {

    private final String name;
    private final Filter contactFilter;
    private final SearchScope contactSearchScope;

    /**
     * Initializes a new {@link LdapFolderFilter}.
     * 
     * @param name The folder's display name
     * @param The underlying LDAP contact filter
     * @param contactSearchScope The scope to use when performing the LDAP search
     */
    public LdapFolderFilter(String name, Filter contactFilter, SearchScope contactSearchScope) {
        super();
        this.name = name;
        this.contactFilter = contactFilter;
        this.contactSearchScope = contactSearchScope;
    }

    /**
     * Gets the folder's display name.
     * 
     * @return The folder's display name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the underlying LDAP contact filter
     * 
     * @return The underlying LDAP contact filter
     */
    public Filter getContactFilter() {
        return contactFilter;
    }

    /**
     * Gets the scope to use when performing the LDAP search
     * 
     * @return The scope to use when performing the LDAP search
     */
    public SearchScope getContactSearchScope() {
        return contactSearchScope;
    }

    @Override
    public String toString() {
        return "LdapFolderFilter [name=" + name + ", contactFilter=" + contactFilter + ", contactSearchScope=" + contactSearchScope + "]";
    }

}
