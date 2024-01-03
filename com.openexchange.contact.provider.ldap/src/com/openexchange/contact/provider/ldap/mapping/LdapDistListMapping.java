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

import com.openexchange.contact.provider.ldap.Utils;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.ContactExceptionCodes;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.unboundid.ldap.sdk.Attribute;

/**
 * {@link LdapDistListMapping}
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public abstract class LdapDistListMapping extends LdapMapping<DistributionListEntryObject[]> {

    /**
     * Initializes a new {@link LdapDistListMapping}.
     *
     * @param ldapAttributeNames The name of the LDAP attribute(s)
     * @param flags Additional flags for the mapping
     */
    protected LdapDistListMapping(String[] ldapAttributeNames, String... flags) {
        super(ldapAttributeNames, flags);
    }

    @Override
    protected DistributionListEntryObject[] get(Attribute attribute) {
        String[] values = attribute.getValues();
        if (null == values) {
            return null;
        }
        DistributionListEntryObject[] members = new DistributionListEntryObject[values.length];
        for (int i = 0; i < values.length; i++) {
            /*
             * set the display name to the member attribute value temporary; will be
             * resolved at a later stage by the storage.
             */
            DistributionListEntryObject member = new DistributionListEntryObject();
            member.setDisplayname(values[i]);
            members[i] = member;
        }
        return members;
    }

    @Override
    public String encodeForFilter(String attributeName, Object value) throws OXException {
        try {
            return Utils.escapeForFilter((String) value);
        } catch (ClassCastException e) {
            throw ContactExceptionCodes.UNEXPECTED_ERROR.create(e, "Error encoding value \"" + value + "\" for '" + this + "'");
        }
    }

    @Override
    protected String encode(DistributionListEntryObject[] value) {
        throw new UnsupportedOperationException();
    }

}