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

package com.openexchange.login;

import java.util.List;
import java.util.Map;
import com.openexchange.authentication.Cookie;

/**
 * {@link DelegatingLoginRequest}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.org">Tobias Friedrich</a>
 * @since 8.0.0
 */
public abstract class DelegatingLoginRequest implements LoginRequest {

    protected final LoginRequest delegate;

    /**
     * Initializes a new {@link DelegatingLoginRequest}.
     * 
     * @param delegate The login request to delegate to
     */
    protected DelegatingLoginRequest(LoginRequest delegate) {
        super();
        this.delegate = delegate;
    }

    @Override
    public String getLogin() {
        return delegate.getLogin();
    }

    @Override
    public String getPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getClientIP() {
        return delegate.getClientIP();
    }

    @Override
    public String getUserAgent() {
        return delegate.getUserAgent();
    }

    @Override
    public String getAuthId() {
        return delegate.getAuthId();
    }

    @Override
    public String getClient() {
        return delegate.getClient();
    }

    @Override
    public String getVersion() {
        return delegate.getVersion();
    }

    @Override
    public String getHash() {
        return delegate.getHash();
    }

    @Override
    public String getClientToken() {
        return delegate.getClientToken();
    }

    @Override
    public Interface getInterface() {
        return delegate.getInterface();
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return delegate.getHeaders();
    }

    @Override
    public Map<String, String[]> getRequestParameter() {
        return delegate.getRequestParameter();
    }

    @Override
    public Cookie[] getCookies() {
        return delegate.getCookies();
    }

    @Override
    public boolean isSecure() {
        return delegate.isSecure();
    }

    @Override
    public String getServerName() {
        return delegate.getServerName();
    }

    @Override
    public int getServerPort() {
        return delegate.getServerPort();
    }

    @Override
    public String getHttpSessionID() {
        return delegate.getHttpSessionID();
    }

    @Override
    public boolean markHttpSessionAuthenticated() {
        return delegate.markHttpSessionAuthenticated();
    }

    @Override
    public boolean isTransient() {
        return delegate.isTransient();
    }

    @Override
    public String getLanguage() {
        return delegate.getLanguage();
    }

    @Override
    public boolean isStoreLanguage() {
        return delegate.isStoreLanguage();
    }

    @Override
    public String getLocale() {
        return delegate.getLocale();
    }

    @Override
    public boolean isStoreLocale() {
        return delegate.isStoreLocale();
    }

    @Override
    public boolean isStaySignedIn() {
        return delegate.isStaySignedIn();
    }

}
