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

package com.openexchange.crypto;

import com.openexchange.java.Strings;

/**
 * Class for standardizing Crypto Protocols
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v2.10.7
 */
public class CryptoType {

    /**
     * Type of cryptography protocol being used
     * Acceptable types are AES, DES, 3DES, RSA, ECC
     * as well as PGP, SMIME
     *
     * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
     * @since v2.10.7
     */
    public enum PROTOCOL {

        PGP("PGP"),
        SMIME("SMIME"),
        AES("AES"),
        DES("DES"),
        TRIPLE_DES("3DES"),
        RSA("RSA"),
        ECC("ECC"),
        OTHER("OTHER"),
        NONE("NONE");

        private String value;

        private PROTOCOL(String value) {
            this.value = value;
        }

        /**
         * Return string representation
         *
         * @return
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * Find the CryptoType represented by the type_string
     *
     * @param type A string representation of the type
     * @return NONE if type_string is null. Type if found, or TYPE.OTHER if not found
     */
    public static PROTOCOL getTypeFromString(String type) {
        if (Strings.isEmpty(type)) {  // Default not specified is to return PGP
            return PROTOCOL.PGP;
        }
        for (PROTOCOL t : PROTOCOL.values()) {
            if (t.getValue().equalsIgnoreCase(type))
                return t;
        }
        return PROTOCOL.PGP;
    }

}
