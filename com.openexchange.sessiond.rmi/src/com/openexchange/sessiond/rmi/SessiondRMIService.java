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

package com.openexchange.sessiond.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Set;
import com.openexchange.auth.Credentials;

/**
 * {@link SessiondRMIService} - The RMI service for session operations.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public interface SessiondRMIService extends Remote {

    public static final String RMI_NAME = "SessiondRMIService";

    /**
     * Clears the session with the specified identifier
     *
     * @param sessionId The session identifier
     * @param auth Credentials for authenticating against server.
     * @return <code>true</code> if the sessions was successfully removed; <code>false</code> otherwise
     * @throws RemoteException if the operation fails or any other error is occurred
     */
    boolean clearUserSession(String sessionId, Credentials auth) throws RemoteException;

    /**
     * Clears the session with the specified identifier
     *
     * @param sessionId The session identifier
     * @param global <code>true</code> if the sessions should be cleared globally
     * @param auth Credentials for authenticating against server.
     * @return <code>true</code> if the sessions was successfully removed; <code>false</code> otherwise
     * @throws RemoteException if the operation fails or any other error is occurred
     */
    boolean clearUserSession(String sessionId, boolean global, Credentials auth) throws RemoteException;

    /**
     * Clears all sessions belonging to the user identified by given user ID in specified context
     *
     * @param userId The user ID
     * @param contextId The context ID
     * @param auth Credentials for authenticating against server.
     * @return The number of removed sessions belonging to the user or <code>-1</code> if an error occurred
     * @throws RemoteException If the operation fails or any other error is occurred
     */
    int clearUserSessions(int userId, int contextId, Credentials auth) throws RemoteException;

    /**
     * Clears all sessions from cluster belonging to the user identified by given user ID in specified context
     *
     * @param userId The user ID
     * @param contextId The context ID
     * @param auth Credentials for authenticating against server.
     * @throws RemoteException If the operation fails or any other error is occurred
     */
    void clearUserSessionsGlobally(int userId, int contextId, Credentials auth) throws RemoteException;

    /**
     * Clears all sessions belonging to specified context
     *
     * @param contextId The context identifier
     * @param auth Credentials for authenticating against server.
     * @throws RemoteException If the operation fails or any other error is occurred
     */
    void clearContextSessions(int contextId, Credentials auth) throws RemoteException;

    /**
     * Clears all sessions belonging to given context.
     *
     * @param contextId The context identifier to remove sessions for
     * @param auth Credentials for authenticating against server.
     * @throws RemoteException If the operation fails or any other error is occurred
     */
    void clearContextSessionsGlobally(int contextId, Credentials auth) throws RemoteException;

    /**
     * Clears all sessions belonging to given contexts.
     *
     * @param contextId The context identifiers to remove sessions for
     * @param auth Credentials for authenticating against server.
     * @throws RemoteException If the operation fails or any other error is occurred
     */
    void clearContextSessionsGlobally(Set<Integer> contextIds, Credentials auth) throws RemoteException;

    /**
     * Clear all sessions in central session storage. This does not affect the local short term session container.
     *
     * @param auth Credentials for authenticating against server.
     * @throws RemoteException If the operation fails or any other error is occurred
     */
    void clearSessionStorage(Credentials auth) throws RemoteException;
}
