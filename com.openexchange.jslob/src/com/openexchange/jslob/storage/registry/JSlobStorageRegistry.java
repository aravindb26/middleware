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

package com.openexchange.jslob.storage.registry;

import java.util.Collection;
import com.openexchange.exception.OXException;
import com.openexchange.jslob.storage.JSlobStorage;
import com.openexchange.osgi.annotation.SingletonService;

/**
 * {@link JSlobStorageRegistry} - The registry for JSlob storages.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@SingletonService
public interface JSlobStorageRegistry {

    /**
     * Gets the JSlob storage associated with given service identifier.
     *
     * @param storageId The storage identifier
     * @return The JSlob storage associated with given service identifier or <code>null</code>
     * @throws OXException If returning the storage fails
     */
    JSlobStorage getJSlobStorage(String storageId) throws OXException;

    /**
     * Gets a collection containing all registered JSlob storage
     *
     * @return A collection containing all registered JSlob storage
     * @throws OXException If returning the collection fails
     */
    Collection<JSlobStorage> getJSlobStorages() throws OXException;

    /**
     * Puts given JSlob storage into this registry.
     *
     * @param jslobStorage The JSlob storage to put
     * @return <code>true</code> on success; otherwise <code>false</code> if another storage is already bound to the same identifier
     */
    boolean putJSlobStorage(JSlobStorage jslobStorage);

    /**
     * Removes the JSlob storage associated with given service identifier.
     *
     * @param storageId The storage identifier
     * @throws OXException If removing the storage fails
     */
    void removeJSlobStorage(String storageId) throws OXException;

}
