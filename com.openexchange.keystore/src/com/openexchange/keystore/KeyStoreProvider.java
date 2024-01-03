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

import java.security.KeyStore;
import java.util.Optional;
import com.openexchange.exception.OXException;

/**
 * {@link KeyStoreProvider}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public interface KeyStoreProvider {

    /**
     * Gets the keystore provider id
     *
     * @return The keystore provider id
     */
    String getId();

    /**
     * Get the key store if available
     *
     * @return The optional {@link KeyStore}
     */
    Optional<KeyStore> optKeyStore();

    /**
     * Gets the password of the keystore if available
     *
     * @return The optional password
     */
    Optional<String> optPassword();

    /**
     * Reloads the underlying keystore
     *
     * @return <code>true</code> if the keystore has changed, <code>false</code> otherwise
     * @throws OXException If reloading fails
     */
     boolean reload() throws OXException;

}
