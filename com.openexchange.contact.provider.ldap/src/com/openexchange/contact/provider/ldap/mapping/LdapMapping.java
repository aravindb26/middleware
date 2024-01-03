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

import java.util.Arrays;
import com.openexchange.contact.provider.ldap.LdapContactsExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.tools.mappings.DefaultMapping;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;

/**
 * {@link LdapMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @param <T>
 * @since 7.10.6
 */
public abstract class LdapMapping<T> extends DefaultMapping<T, Contact> implements Cloneable {

    protected final String[] attributeNames;
    protected final String[] flags;

    /**
     * Initializes a new {@link LdapMapping}.
     *
     * @param attributeNames The name of the possible LDAP attribute(s)
     * @param flags Additional flags for the mapping
     */
    protected LdapMapping(String[] attributeNames, String... flags) {
        super();
        this.attributeNames = attributeNames;
        this.flags = flags;
    }

    protected boolean isBinary() {
        return null != flags && com.openexchange.tools.arrays.Arrays.contains(flags, "binary");
    }

    /**
     * Gets the mapping's value from a the mapped LDAP attribute.
     *
     * @param attribute The LDAP attribute to get the mapped value for
     * @return The value, or <code>null</code> if not set in the attribute
     */
    protected abstract T get(Attribute attribute);

    /**
     * Encodes a value for its usage in LDAP filters.
     *
     * @param value The value to encode
     * @return The encoded value
     */
    protected abstract String encode(T value);

    /**
     * Gets the name of the mapped LDAP attribute(s).
     *
     * @return The attribute names
     */
    public String[] getAttributeNames() {
        return attributeNames;
    }

    /**
     * Gets the mapping's value from an LDAP entry.
     *
     * @param entry The LDAP entry to get the mapped value for
     * @return The value, or <code>null</code> if not set in the entry
     */
    public T get(Entry entry) {
        for (String attributeName : attributeNames) {
            Attribute attribute = entry.getAttribute(attributeName);
            if (null != attribute) {
                return get(attribute);
            }
        }
        return null;
    }

    /**
     * Sets the value of the mapped property in an object.
     *
     * @param entry The LDAP entry to read out the value from
     * @param contact The object to set the value in
     */
    public void set(Entry entry, Contact contact) throws OXException {
        set(contact, get(entry));
    }

    /**
     * Prepares the supplied value to be used in search filters against one of this mapping's LDAP-attributes.
     *
     * @param attributeName The actual attribute name
     * @param value The value
     * @return The encoded value
     */
    public String encodeForFilter(String attributeName, Object value) throws OXException {
        try {
            @SuppressWarnings("unchecked") T t = (T) value;
            return encode(t);
        } catch (Exception e) {
            throw LdapContactsExceptionCodes.UNEXPECTED_ERROR.create(e, "Error encoding value \"" + value + "\" for '" + this + "'");
        }
    }

    @Override
    public String toString() {
        return "LdapMapping [attributeNames=" + Arrays.toString(attributeNames) + "]";
    }

}