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

package com.openexchange.authentication;

import java.util.Collections;
import java.util.Map;

/**
 * {@link DefaultLoginInfo} - The default login info.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DefaultLoginInfo implements LoginInfo {

    private final String userName;
    private final String password;
    private final Map<String, Object> properties;

    /**
     * Initializes a new {@link DefaultLoginInfo}.
     *
     * @param userName The user name
     * @param password The password
     */
    public DefaultLoginInfo(final String userName, final String password) {
        this(userName, password, null);
    }

    /**
     * Initializes a new {@link DefaultLoginInfo}.
     *
     * @param userName The user name
     * @param password The password
     * @param properties The optional properties; may be <code>null</code>
     */
    public DefaultLoginInfo(final String userName, final String password, final Map<String, Object> properties) {
        super();
        this.userName = userName;
        this.password = password;
        this.properties = null == properties ? Collections.<String, Object> emptyMap() : properties;
    }

    /**
     * Creates a new {@link DefaultLoginInfo} object from given {@link AuthenticationRequest}
     *
     * @param authenticationRequest The authentication request
     * @return The default-login-info object
     */
    public static DefaultLoginInfo of(AuthenticationRequest authenticationRequest) {
        return new DefaultLoginInfo(authenticationRequest.getLogin(), authenticationRequest.getPassword(), authenticationRequest.getProperties());
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Map<String, Object> getProperties() {
        return properties;
    }

}
