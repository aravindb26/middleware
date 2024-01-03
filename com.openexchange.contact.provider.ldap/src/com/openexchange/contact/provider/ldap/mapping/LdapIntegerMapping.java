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

package com.openexchange.contact.provider.ldap.mapping;

import com.unboundid.ldap.sdk.Attribute;

/**
 * {@link LdapIntegerMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.6
 */
public abstract class LdapIntegerMapping extends LdapMapping<Integer> {

    /**
     * Initializes a new {@link LdapIntegerMapping}.
     *
     * @param ldapAttributeNames The name of the LDAP attribute(s)
     * @param flags Additional flags for the mapping
     */
    protected LdapIntegerMapping(String[] ldapAttributeName, String... flags) {
        super(ldapAttributeName, flags);
    }

    @Override
    protected Integer get(Attribute attribute) {
        return attribute.getValueAsInteger();
    }

    @Override
    protected String encode(Integer value) {
        return null == value ? null : String.valueOf(value);
    }

}