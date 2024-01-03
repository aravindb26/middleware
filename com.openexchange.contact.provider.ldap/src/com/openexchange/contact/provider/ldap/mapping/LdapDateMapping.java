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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import com.unboundid.ldap.sdk.Attribute;

/**
 * {@link LdapDateMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.6
 */
public abstract class LdapDateMapping extends LdapMapping<Date> {

    private static final ThreadLocal<DateFormat> DATE_FORMAT_OUT = new ThreadLocal<DateFormat>() {

        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss'.0Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat;
        }
    };

    /**
     * Initializes a new {@link LdapDateMapping}.
     *
     * @param ldapAttributeNames The name of the LDAP attribute(s)
     * @param flags Additional flags for the mapping
     */
    protected LdapDateMapping(String[] ldapAttributeNames, String... flags) {
        super(ldapAttributeNames, flags);
    }

    @Override
    protected Date get(Attribute attribute) {
        return attribute.getValueAsDate();
    }

    @Override
    protected String encode(Date value) {
        return null != value ? DATE_FORMAT_OUT.get().format(value) : null;
    }

}