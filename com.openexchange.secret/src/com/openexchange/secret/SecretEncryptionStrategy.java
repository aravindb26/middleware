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

package com.openexchange.secret;

import com.openexchange.exception.OXException;

/**
 * {@link SecretEncryptionStrategy} - This interface defines a strategy to updated a secret's en- and decryption mechanism
 * after it has been unsuccessfully <b>decrypted</b>
 * 
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface SecretEncryptionStrategy<T> {

    /**
     * Updates using <code>recrypted</code>.
     *
     * @param recrypted The re-crypted string
     * @param customizationNote The optional customization note
     * @throws OXException If update fails
     */
    void update(String recrypted, T customizationNote) throws OXException;

}
