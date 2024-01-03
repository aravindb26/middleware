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

/**
 * {@link SuccessAuthenticationResult} - The authentication result signaling a successful authentication attempt.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class SuccessAuthenticationResult extends AuthenticationResult {

    private final String user;
    private final String pwd;
    private final String host;
    private final int port;
    private final ConnectMode connectMode;

    /**
     * Initializes a new {@link SuccessAuthenticationResult}.
     *
     * @param type The result type
     * @param authFailedException The exception representing the failed authentication due to wrong credentials/authentication data
     * @param user The user login
     * @param pwd The password
     * @param host The host name or textual representation of its IP address
     * @param port The port
     * @param connectMode The connect mode
     */
    SuccessAuthenticationResult(String user, String pwd, String host, int port, ConnectMode connectMode) {
        super(AuthenticationResult.Type.SUCCEEDED);
        this.user = user;
        this.pwd = pwd;
        this.host = host;
        this.port = port;
        this.connectMode = connectMode;
    }

    /**
     * Gets the user login.
     *
     * @return The user login
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets the password.
     *
     * @return The password
     */
    public String getPwd() {
        return pwd;
    }

    /**
     * Gets the host name or textual representation of its IP address.
     *
     * @return The host name or textual representation of its IP address
     */
    public String getHost() {
        return host;
    }

    /**
     * Gets the port.
     *
     * @return The port
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the connect mode actually used.
     *
     * @return The connect mode
     */
    public ConnectMode getConnectMode() {
        return connectMode;
    }

    /**
     * Checks if this result's connect mode is equal to specified connect mode.
     *
     * @param other The other connect mode to check against
     * @return <code>true</code> if equal; otherwise <code>false</code>
     */
    public boolean equalsConnectMode(ConnectMode other) {
        return connectMode.equals(other);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        if (type != null) {
            builder.append("type=").append(type).append(", ");
        }
        if (user != null) {
            builder.append("user=").append(user).append(", ");
        }
        if (pwd != null) {
            builder.append("pwd=").append(pwd == null ? "null" : "XXXXXXXX").append(", ");
        }
        if (host != null) {
            builder.append("host=").append(host).append(", ");
        }
        builder.append("port=").append(port).append(", ");
        if (connectMode != null) {
            builder.append("connectMode=").append(connectMode);
        }
        builder.append(']');
        return builder.toString();
    }

}
