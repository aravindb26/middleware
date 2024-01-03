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

package com.openexchange.auth;

import java.rmi.RemoteException;
import com.openexchange.exception.OXException;

/**
<<<<<<< HEAD
 * {@link AbstractAuthenticatorRMIService} - For RMI services that need authentication.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
=======
 * {@link AbstractAuthenticatorRMIService} - Super class for RMI services that need administrative authentication.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
>>>>>>> Security: MWB-1996;CVE-2023-26455;CVSS:5.6
 */
public abstract class AbstractAuthenticatorRMIService {

    /**
     * Initializes a new {@link AbstractAuthenticatorRMIService}.
     */
    protected AbstractAuthenticatorRMIService() {
        super();
    }

    /**
     * Performs master administrator authentication using given login and password against specified <code>Authenticator</code> instance.
     *
     * @param authenticator The authenticator to use
     * @param login The login
     * @param password The password
     * @throws RemoteException If authentication fails
     */
    protected static void authenticate(Authenticator authenticator, String login, String password) throws RemoteException {
        authenticate(authenticator, new Credentials(login, password));
    }

    /**
     * Performs master administrator authentication using given credentials against specified <code>Authenticator</code> instance.
     *
     * @param authenticator The authenticator to use
     * @param creds The credentials to validate
     * @throws RemoteException If authentication fails
     */
    protected static void authenticate(Authenticator authenticator, Credentials creds) throws RemoteException {
        if (authenticator == null) {
            throw new RemoteException("Authenticator service not available.");
        }
        try {
            authenticator.doAuthentication(creds);
        } catch (OXException e) {
            throw new RemoteException("Master admin authentication failed", e);
        }
    }

    /**
     * Performs context administrator authentication using given credentials against specified <code>Authenticator</code> instance.
     *
     * @param authenticator The authenticator to use
     * @param creds The credentials to validate
     * @param contextId The identifier of the context
     * @throws RemoteException If authentication fails
     */
    protected static void authenticate(Authenticator authenticator, Credentials creds, int contextId) throws RemoteException {
        if (authenticator == null) {
            throw new RemoteException("Authenticator service not available.");
        }
        try {
            authenticator.doAuthentication(creds, contextId);
        } catch (OXException e) {
            throw new RemoteException("Context admin authentication failed", e);
        }
    }

    /**
     * Performs optional context, falls-back to master administrator authentication using given credentials against specified <code>Authenticator</code> instance.
     *
     * @param authenticator The authenticator to use
     * @param creds The credentials to validate
     * @param contextId The optional identifier of the context for context administrator authentication or equal to/less than <code>0</code> (zero) to perform master administrator authentication
     * @throws RemoteException If authentication fails
     */
    protected void authenticateCascaded(Authenticator authenticator, Credentials creds, int contextId) throws RemoteException {
        if (contextId > 0) {
            try {
                authenticator.doAuthentication(creds, contextId);
                return;
            } catch (OXException e) {
                throw new RemoteException("Context admin authentication failed", e);
            }
        }

        try {
            authenticator.doAuthentication(creds);
        } catch (OXException e) {
            throw new RemoteException("Master admin authentication failed", e);
        }
    }

}
