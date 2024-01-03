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
package com.openexchange.keystore;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;
import com.openexchange.exception.OXException;

/**
 * {@link KeyStoreUtil} is a utility class which simplifies {@link KeyStore} creation
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class KeyStoreUtil {

    /**
     * Prevents initialization of {@link KeyStoreUtil}.
     */
    private KeyStoreUtil() {}

    /**
     * Converts the given {@link InputStream} into a {@link KeyStore}.
     *
     * @param stream The keystore stream
     * @param optType The optional type of the keystore. E.g. PKCS12
     * @param optPassword The optional keystore password
     * @return The {@link KeyStore}
     * @throws OXException in case an error occured while converting the keystore byte stream to a keystore
     */
    public static KeyStore toKeyStore(InputStream stream, Optional<String> optType, Optional<String> password) throws OXException {
        try {
            KeyStore keyStore = KeyStore.getInstance(optType.orElseGet(() -> KeyStore.getDefaultType())); // e.g. "PKCS12"
            keyStore.load(stream, password.map(pw -> pw.toCharArray()).orElse(null));
            return keyStore;
        } catch (IOException | KeyStoreException | NoSuchAlgorithmException | CertificateException e) {
            throw OXException.general("Unable to convert stream to keystore", e);
        }
    }

}
