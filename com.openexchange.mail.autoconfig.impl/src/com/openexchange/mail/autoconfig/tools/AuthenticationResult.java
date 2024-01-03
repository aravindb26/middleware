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

package com.openexchange.mail.autoconfig.tools;

import com.openexchange.exception.OXException;

/**
 * {@link AuthenticationResult} - Provides information about attempted authentication.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public abstract class AuthenticationResult {

    /** The result type for an authentication attempt */
    public static enum Type {
        /** The authentication attempt succeeded. */
        SUCCEEDED,
        /** The authentication attempt failed due to wrong credentials/authentication data. */
        FAILED_AUTHENTICATION,
        /** The authentication attempt failed due to any other error/failure than wrong credentials/authentication data. */
        ERROR;
    }

    /**
     * Creates a result signaling that the authentication attempt succeeded.
     *
     * @param user The user login
     * @param pwd The password
     * @param host The host name or textual representation of its IP address
     * @param port The port
     * @param connectMode The connect mode
     * @return The created result
     */
    public static SuccessAuthenticationResult success(String user, String pwd, String host, int port, ConnectMode connectMode) {
        return new SuccessAuthenticationResult(user, pwd, host, port, connectMode);
    }

    /**
     * Creates a result signaling that the authentication attempt failed due to any other error/failure than wrong credentials/authentication data.
     *
     * @return The created result
     */
    public static ErrorAuthenticationResult error() {
        return ErrorAuthenticationResult.getInstance();
    }

    /**
     * Creates a result signaling that the authentication attempt failed due wrong credentials/authentication data.
     *
     * @param authFailedException The exception representing the failed authentication due to wrong credentials/authentication data
     * @return The created result
     */
    public static FailedAuthenticationResult failedAuthentication(OXException authFailedException) {
        return new FailedAuthenticationResult(authFailedException);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /** The result type */
    protected final Type type;

    /**
     * Initializes a new {@link AuthenticationResult}.
     *
     * @param type The result type
     */
    protected AuthenticationResult(Type type) {
        super();
        this.type = type;
    }

    /**
     * Gets this result's type.
     *
     * @return The type
     */
    public Type getType() {
        return type;
    }

    /**
     * Checks if this authentication result represents a successful authentication attempt.
     *
     * @return <code>true</code> if successful; otherwise <code>false</code>
     */
    public boolean isSuccess() {
        return Type.SUCCEEDED == type;
    }

    /**
     * Checks if this authentication result represents a failed authentication attempt due to wrong credentials/authentication data.
     *
     * @return <code>true</code> if failed due to wrong credentials/authentication data; otherwise <code>false</code>
     */
    public boolean isFailedAuthentication() {
        return Type.FAILED_AUTHENTICATION == type;
    }

    /**
     * Checks if this authentication result represents a failed authentication attempt due an error.
     *
     * @return <code>true</code> if failed due to an error; otherwise <code>false</code>
     */
    public boolean isError() {
        return Type.ERROR == type;
    }

    /**
     * Checks if this authentication result represents a successful or failed authentication attempt due to wrong credentials/authentication data.
     * <p>
     * Either this result signals success or a failed authentication. In both cases no further authentication attempts are reasonable.
     *
     * @return <code>true</code> if successful or failed due to wrong credentials/authentication data; otherwise <code>false</code>
     */
    public boolean isSuccessOrFailedAuthentication() {
        return Type.SUCCEEDED == type || Type.FAILED_AUTHENTICATION == type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (type != null) {
            builder.append("type=").append(type);
        }
        builder.append(']');
        return builder.toString();
    }

}
