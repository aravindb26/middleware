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
package com.openexchange.admin.plugins;

import java.util.List;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;

public interface BasicAuthenticatorPluginInterface {

    /**
     * Authenticate the given credentials
     *
     * @param authdata The {@link Credentials}
     * @throws InvalidCredentialsException in case the authentication fails
     */
    public void doAuthentication(final Credentials authdata) throws InvalidCredentialsException;

    /**
     * Checks if the admin identified by the provided credentials is the owner of the given context
     *
     * @param creds The credentials of the admin
     * @param ctx The context to check
     * @return <code>true</code> if the admin is the owner, <code>false</code> otherwise
     * @throws InvalidCredentialsException in case the credentials are invalid
     */
    public boolean isOwnerOfContext(final Credentials creds, final Context ctx) throws InvalidCredentialsException;

    /**
     * Checks if the admin identified by the provided credentials is the master of the given context
     *
     * @param creds The credentials of the admin
     * @param ctx The context to check
     * @return <code>true</code> if the admin is the master, <code>false</code> otherwise
     * @throws InvalidCredentialsException in case the credentials are invalid
     */
    public boolean isMasterOfContext(final Credentials creds, final Context ctx) throws InvalidCredentialsException;

    /**
     * Checks if the admin identified by the provided credentials is the master of all the given contexts
     *
     * @param creds The credentials of the admin
     * @param ctxs The contexts to check
     * @return <code>true</code> if the admin is the master, <code>false</code> otherwise
     * @throws InvalidCredentialsException in case the credentials are invalid
     */
    public boolean isMasterOfContext(final Credentials creds, final List<Context> ctxs) throws InvalidCredentialsException;
}
