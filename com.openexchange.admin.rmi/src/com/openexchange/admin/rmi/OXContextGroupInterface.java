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

package com.openexchange.admin.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.auth.Credentials;

/**
 * {@link OXContextGroupInterface}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public interface OXContextGroupInterface extends Remote {

    /**
     * RMI name to be used in the naming lookup.
     */
    public static final String RMI_NAME = "OXContextGroup";

    /**
     * Deletes all data from the globaldb that is associated to the specified context group.
     *
     * @param contextGroupId The context group identifier
     * @param auth Credentials for authenticating against server.
     * @throws RemoteException
     */
    void deleteContextGroup(String contextGroupId, Credentials auth) throws RemoteException, StorageException;
}
