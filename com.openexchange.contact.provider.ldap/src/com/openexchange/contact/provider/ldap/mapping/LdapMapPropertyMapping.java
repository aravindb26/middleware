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

import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;

/**
 * {@link LdapMapPropertyMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class LdapMapPropertyMapping extends LdapStringMapping {

    /** The property name for the login user info string that can be used to resolve the actual internal user id indirectly */
    public static final String PROP_LOGIN_USER_INFO = "loginUserInfo";

    /** The property name for the login user info string that can be used to resolve the actual internal context id indirectly */
    public static final String PROP_LOGIN_CONTEXT_INFO = "loginContextInfo";

    protected final String propertyName;
    
    /**
     * Initializes a new {@link LdapMapPropertyMapping}.
     *
     * @param attributeNames The name of the LDAP attribute(s)
     * @param propertyName The name of the extended contact property to use
     * @param flags Additional flags for the mapping
     */
    protected LdapMapPropertyMapping(String[] attributeNames, String propertyName, String[] flags) {
        super(attributeNames, flags);
        this.propertyName = propertyName;
    }

    @Override
    public boolean isSet(Contact object) {
        return object.containsMap() && null != object.getProperty(propertyName);
    }

    @Override
    public void set(Contact object, String value) throws OXException {
        if (null == value) {
            object.removeProperty(propertyName);
        } else {
            object.setProperty(propertyName, value);
        }
    }

    @Override
    public String get(Contact object) {
        return object.getProperty(propertyName);
    }

    @Override
    public void remove(Contact object) {
        object.removeProperty(propertyName);
    }

}
