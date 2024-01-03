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

package com.openexchange.file.storage.webdav;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScheme;
import org.apache.http.client.AuthCache;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.util.Args;

/**
 * {@link BearerAuthCache} is a {@link AuthCache} for oauth bearer authentication
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class BearerAuthCache implements AuthCache {

    private final Map<HttpHost, AuthScheme> map;
    private final SchemePortResolver schemePortResolver;

    /**
     * Initializes a new {@link BearerAuthCache}.
     */
    public BearerAuthCache() {
        super();
        this.map = new ConcurrentHashMap<HttpHost, AuthScheme>();
        this.schemePortResolver = DefaultSchemePortResolver.INSTANCE;
    }

    private HttpHost getKey(final HttpHost host) {
        if (host.getPort() <= 0) {
            final int port;
            try {
                port = schemePortResolver.resolve(host);
            } catch (@SuppressWarnings("unused") final UnsupportedSchemeException ignore) {
                return host;
            }
            return new HttpHost(host.getHostName(), port, host.getSchemeName());
        }
        return host;
    }

    @Override
    public void put(final HttpHost host, final AuthScheme authScheme) {
        Args.notNull(host, "HTTP host");
        if (authScheme == null) {
            return;
        }
        map.put(getKey(host), authScheme);
    }

    @Override
    public AuthScheme get(final HttpHost host) {
        Args.notNull(host, "HTTP host");
        return map.get(getKey(host));
    }

    @Override
    public void remove(final HttpHost host) {
        Args.notNull(host, "HTTP host");
        map.remove(getKey(host));
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        return map.toString();
    }

}
