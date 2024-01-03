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

import static com.openexchange.java.util.UUIDs.UUID_BYTE_LENGTH;
import static org.slf4j.LoggerFactory.getLogger;
import java.nio.ByteBuffer;
import java.util.UUID;
import com.openexchange.contact.provider.ldap.Utils;
import com.openexchange.java.Charsets;
import com.openexchange.java.util.UUIDs;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Filter;

/**
 * {@link LdapStringMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.6
 */
public abstract class LdapStringMapping extends LdapMapping<String> {

    /**
     * Initializes a new {@link LdapStringMapping}.
     *
     * @param ldapAttributeNames The name of the LDAP attribute(s)
     * @param flags Additional flags for the mapping
     */
    protected LdapStringMapping(String[] ldapAttributeNames, String... flags) {
        super(ldapAttributeNames, flags);
    }

    protected boolean isGuid() {
        return null != flags && com.openexchange.tools.arrays.Arrays.contains(flags, "guid");
    }

    @Override
    protected String get(Attribute attribute) {
        if (isGuid()) {
            /*
             * try to decode Microsoft GUID
             */
            byte[] bytes = attribute.getValueByteArray();
            if (null != bytes && UUID_BYTE_LENGTH == bytes.length) {
                return swapGuidByteOrder(bytes).toString();
            }
        }
        if (isBinary()) {
            /*
             * try to decode common UUID
             */
            byte[] bytes = attribute.getValueByteArray();
            if (null != bytes && UUID_BYTE_LENGTH == bytes.length) {
                return UUIDs.toUUID(bytes).toString();
            }
        }
        return attribute.getValue();
    }
    
    @Override
    protected String encode(String value) {
        if (null == value || "*".equals(value)) {
            return value;
        }
        if (isGuid()) {
            /*
             * encode Microsoft GUID
             */
            byte[] bytes = UUIDs.toByteArray(UUID.fromString(value));
            bytes = UUIDs.toByteArray(swapGuidByteOrder(bytes));
            return Filter.encodeValue(bytes);
        }
        if (isBinary()) {
            /*
             * try to encode common UUID, otherwise use plaing UTF-8 byte represenatation
             */
            byte[] bytes;
            try {
                bytes = UUIDs.toByteArray(UUID.fromString(value));
            } catch (IllegalArgumentException e) {
                getLogger(LdapStringMapping.class).debug("Error interpreting string value in binary mapping as UUID, falling back to plaing UTF-8 bytes.", e);
                bytes = value.getBytes(Charsets.UTF_8);
            }
            return Filter.encodeValue(bytes);
        }
        return Utils.escapeForFilter(value);
    }
    
    /**
     * Adjusts the byte order of the most significant bits within the passed 16-byte array representing an UUID or Microsoft GUID, to
     * convert between the little-/big-endian representations.
     * 
     * @param bytes The 16-byte array representing an UUID or Microsoft GUID to swap the byte order in
     * @return An UUID holding the passed bytes with swapped MSB order
     */
    private static UUID swapGuidByteOrder(byte[] bytes) {
        if (null == bytes || UUIDs.UUID_BYTE_LENGTH != bytes.length) {
            throw new IllegalArgumentException();
        }        
        // MSB in little-endian byte orders 
        long msb = ByteBuffer.allocate(8)
            .put(3, bytes[0]).put(2, bytes[1]).put(1, bytes[2]).put(0, bytes[3])
            .put(5, bytes[4]).put(4, bytes[5])
            .put(7, bytes[6]).put(6, bytes[7])
         .getLong();
        // LSB in common byte order
        long lsb = ByteBuffer.wrap(bytes, 8, 8).getLong();
        return new UUID(msb, lsb);
    }

}