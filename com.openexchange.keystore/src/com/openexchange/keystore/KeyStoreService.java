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

import com.openexchange.exception.OXException;
import java.security.KeyStore;
import java.util.Optional;

/**
 * {@link KeyStoreService} is a singleton service which provides keystores
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public interface KeyStoreService {

    /**
     * Gets the content of a secret with the given id as a byte array
     *
     * If the id of the secret is null or empty then {@link Optional#empty()} is returned
     *
     * @param id The id of the secret
     * @return The optional secret content as a byte array
     */
    public Optional<byte[]> optSecret(String id);

    /**
     * Gets the keystore with the given id as a {@link KeyStore}
     *
     * If the id of the keystore is null then {@link Optional#empty()} is returned
     *
     * @param keystoreId The id of the keystore
     * @param optType type of the keystore. E.g. PKCS12
     * @param optPassword The optional keystore password
     * @return The optional {@link KeyStore} if an keystore with the given id exists
     * @throws OXException in case an error occured while converting the keystore byte stream to a keystore
     */
    public Optional<KeyStore> optKeyStore(String keystoreId, Optional<String> optType, Optional<String> optPassword) throws OXException;

}
