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

package com.openexchange.report.internal;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import javax.management.MBeanException;

/**
 * {@link LoginCounterRMIService}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public interface LoginCounterRMIService extends Remote {

    public static final String RMI_NAME = LoginCounterRMIService.class.getSimpleName();

    /**
     * Gets the time stamp of last login for specified user for given client.
     * <p>
     * The number of milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param client The client identifier
     * @return The time stamp of last login as UTC <code>long</code><br>
     *         (the number of milliseconds since January 1, 1970, 00:00:00 GMT)
     * @throws MBeanException If retrieval fails
     */
    List<Object[]> getLastLoginTimeStamp(int userId, int contextId, String client) throws RemoteException;
}
