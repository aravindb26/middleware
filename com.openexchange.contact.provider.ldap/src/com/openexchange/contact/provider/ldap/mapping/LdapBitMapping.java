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

import static com.openexchange.java.Autoboxing.I;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.unboundid.ldap.sdk.Attribute;

/**
 * {@link LdapBitMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public abstract class LdapBitMapping extends LdapMapping<Integer> {

    protected final Map<String, String> attributesAndValues;

    /**
     * Initializes a new {@link LdapBitMapping}.
     *
     * @param attributesAndValues A map associating the attribute names to their values to evaluate the boolean value
     * @param flags Additional flags for the mapping
     */
    protected LdapBitMapping(Map<String, String> attributesAndValues, String... flags) {
        super(attributesAndValues.keySet().toArray(new String[attributesAndValues.size()]), flags);
        this.attributesAndValues = attributesAndValues;
    }

    @Override
    protected Integer get(Attribute attribute) {
        String valueToMatch = attributesAndValues.get(attribute.getBaseName());
        if (null == valueToMatch) {
            throw new IllegalArgumentException(attribute.getBaseName());
        }
        String value = attribute.getValue();
        return ("*".equals(valueToMatch) && null != value || valueToMatch.equals(attribute.getValue())) ? I(1) : I(0);
    }

    @Override
    public String encodeForFilter(String attributeName, Object value) throws OXException {
        if (I(1).equals(value) || "1".equals(value)) {
            String valueToMatch = attributesAndValues.get(attributeName);
            if (null == valueToMatch) {
                throw new IllegalArgumentException(attributeName);
            }
            return valueToMatch;
        }
        throw new IllegalArgumentException("unable to encode other values than 1");
    }

    @Override
    protected String encode(Integer value) {
        throw new UnsupportedOperationException();
    }

}
