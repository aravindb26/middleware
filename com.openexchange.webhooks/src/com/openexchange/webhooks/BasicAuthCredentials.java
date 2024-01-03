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

package com.openexchange.webhooks;

import com.openexchange.java.Strings;

/**
 * {@link BasicAuthCredentials} - A pair of login and password for HTTP Basic Authentication.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class BasicAuthCredentials {

    /**
     * Gets the HTTP Basic Authentication credentials for given login and password.
     * <p>
     * If both or not empty valid instance of <code>BasicAuthCredentials</code> is returned; otherwise <code>null</code>
     *
     * @param login The login string
     * @param password The password
     * @return A valid instance of <code>BasicAuthCredentials</code> is returned; otherwise <code>null</code>
     */
    public static BasicAuthCredentials getBasicAuthCredentialsFor(String login, String password) {
        return Strings.isEmpty(login) || Strings.isEmpty(password) ? null : new BasicAuthCredentials(login, password);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final String login;
    private final String password;

    /**
     * Initializes a new {@link BasicAuthCredentials}.
     *
     * @param login The login string
     * @param password The password
     */
    public BasicAuthCredentials(String login, String password) {
        super();
        this.login = login;
        this.password = password;
    }

    /**
     * Gets the login.
     *
     * @return The login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Gets the password.
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((login == null) ? 0 : login.hashCode());
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        BasicAuthCredentials other = (BasicAuthCredentials) obj;
        if (login == null) {
            if (other.login != null) {
                return false;
            }
        } else if (!login.equals(other.login)) {
            return false;
        }
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(32);
        builder.append("BasicAuthCredentials [");
        if (login != null) {
            builder.append("login=").append(login).append(", ");
        }
        if (password != null) {
            builder.append("password=").append(password);
        }
        builder.append(']');
        return builder.toString();
    }

}
