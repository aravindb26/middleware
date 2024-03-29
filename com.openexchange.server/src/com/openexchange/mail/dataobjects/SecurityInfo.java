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

package com.openexchange.mail.dataobjects;

import com.openexchange.crypto.CryptoType;

/**
 * Class to handle the security Information for an email
 * Will keep track if email has signature or is Encrypted
 * This for emails before decryption or verification
 * After Crypto Actions, see SecurityResult class
 * {@link SecurityInfo}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.4
 */
public class SecurityInfo {

    private final boolean signed;
    private final boolean encrypted;
    private final CryptoType.PROTOCOL type;

    /**
     * Initializes a new {@link SecurityInfo}.
     *
     * @param encrypted Whether E-Mail is encrypted
     * @param signed Whether E-Mail is signed
     */
    public SecurityInfo(boolean encrypted, boolean signed, CryptoType.PROTOCOL type) {
        this.signed = signed;
        this.encrypted = encrypted;
        this.type = type;
    }

    /**
     * Checks if E-Mail is encrypted.
     *
     * @return <code>true</code> if encrypted; otherwise <code>false</code>
     */
    public boolean isEncrypted () {
        return encrypted;
    }

    /**
     * Checks if E-Mail is signed.
     *
     * @return <code>true</code> if signed; otherwise <code>false</code>
     */
    public boolean isSigned () {
        return signed;
    }

    /**
     * Returns type of crypto used
     * getType
     *
     * @return
     */
    public CryptoType.PROTOCOL getType() {
        return type;
    }

}
