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

package com.openexchange.rest.client.httpclient.util;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.UnknownHostException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.impl.conn.DefaultSchemePortResolver;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import com.openexchange.java.InetAddresses;
import com.openexchange.java.Reference;

/**
 * {@link HostCheckingRoutePlanner} - The route planner that checks target host's address for validity.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class HostCheckingRoutePlanner extends DefaultRoutePlanner {

    private static final Field routePlannerField;
    private static final Field schemePortResolverField;
    private static final Field proxyField;
    private static final Field systemPropertiesField;

    static {
        Field f;
        try {
            f = HttpClientBuilder.class.getDeclaredField("routePlanner");
            f.setAccessible(true);
        } catch (Exception e) {
            f = null;
        }
        routePlannerField = f;

        try {
            f = HttpClientBuilder.class.getDeclaredField("schemePortResolver");
            f.setAccessible(true);
        } catch (Exception e) {
            f = null;
        }
        schemePortResolverField = f;

        try {
            f = HttpClientBuilder.class.getDeclaredField("proxy");
            f.setAccessible(true);
        } catch (Exception e) {
            f = null;
        }
        proxyField = f;

        try {
            f = HttpClientBuilder.class.getDeclaredField("systemProperties");
            f.setAccessible(true);
        } catch (Exception e) {
            f = null;
        }
        systemPropertiesField = f;
    }

    private static <T> Reference<T> optFieldValueFromBuilder(Field field, HttpClientBuilder builder) {
        if (null == field) {
            return null;
        }
        try {
            return new Reference<T>((T) field.get(builder));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Injects specified host-checking HTTP route planner into given builder instance.
     *
     * @param checkerStrategy The strategy to check by
     * @param builder The builder instance to inject to
     * @return The given builder
     */
    public static HttpClientBuilder injectHostCheckingRoutePlanner(HostCheckerStrategy checkerStrategy, HttpClientBuilder builder) {
        if (checkerStrategy == null || builder == null) {
            return builder;
        }

        if (routePlannerField != null && schemePortResolverField != null && proxyField != null && systemPropertiesField != null) {
            Reference<HttpRoutePlanner> routePlannerRef = optFieldValueFromBuilder(routePlannerField, builder);
            Reference<SchemePortResolver> schemePortResolverRef = optFieldValueFromBuilder(schemePortResolverField, builder);
            Reference<HttpHost> proxyRef = optFieldValueFromBuilder(proxyField, builder);
            Reference<Boolean> systemPropertiesRef = optFieldValueFromBuilder(systemPropertiesField, builder);
            if (routePlannerRef != null && schemePortResolverRef != null && proxyRef != null && systemPropertiesRef != null) {
                HttpRoutePlanner routePlannerCopy = routePlannerRef.getValue();
                if (routePlannerCopy == null) {
                    SchemePortResolver schemePortResolverCopy = schemePortResolverRef.getValue();
                    if (schemePortResolverCopy == null) {
                        schemePortResolverCopy = DefaultSchemePortResolver.INSTANCE;
                    }
                    if (proxyRef.getValue() != null) {
                        routePlannerCopy = new DefaultProxyRoutePlanner(proxyRef.getValue(), schemePortResolverCopy);
                    } else if (systemPropertiesRef.getValue() != null && systemPropertiesRef.getValue().booleanValue()) {
                        routePlannerCopy = new SystemDefaultRoutePlanner(
                                schemePortResolverCopy, ProxySelector.getDefault());
                    } else {
                        routePlannerCopy = new DefaultRoutePlanner(schemePortResolverCopy);
                    }
                }
                return builder.setRoutePlanner(new HostCheckingRoutePlanner(checkerStrategy, routePlannerCopy));
            }
        }

        return builder.setRoutePlanner(new HostCheckingRoutePlanner(checkerStrategy, null));
    }

    /** The strategy for checking a host */
    public static interface HostCheckerStrategy {

        /**
         * Checks specified host's address for validity.
         *
         * @param host The host
         * @param hostAddress The host's address
         * @throws HttpException If host's address is invalid
         */
        void checkHost(HttpHost host, InetAddress hostAddress) throws HttpException;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final HttpRoutePlanner routePlanner;
    private final HostCheckerStrategy checkerStrategy;

    /**
     * Initializes a new {@link HostCheckingRoutePlanner}.
     *
     * @param checkerStrategy The strategy to check by
     * @param routePlanner The route planner to delegate to or <code>null</code>
     */
    protected HostCheckingRoutePlanner(HostCheckerStrategy checkerStrategy, HttpRoutePlanner routePlanner) {
        super(null);
        this.checkerStrategy = checkerStrategy;
        this.routePlanner = routePlanner;
    }

    @Override
    public HttpRoute determineRoute(HttpHost host, HttpRequest request, HttpContext context) throws HttpException {
        HttpRoute route = routePlanner == null ? super.determineRoute(host, request, context) : routePlanner.determineRoute(host, request, context);

        // Check for proxy
        if (route.getProxyHost() != null) {
            // Routed through a proxy
            return route;
        }

        // No proxy in between. Deny access to internal address then...
        try {
            InetAddress inetAddress = host.getAddress();
            if (inetAddress == null) {
                inetAddress = InetAddresses.forString(host.getHostName());
            }
            checkHost(host, inetAddress);
        } catch (UnknownHostException e) {
            throw new HttpException("Invalid target host: " + host.getHostName(), e);
        }
        return route;
    }

    /**
     * Checks specified host's address for validity.
     *
     * @param host The host
     * @param hostAddress The host's address
     * @throws HttpException If host's address is invalid
     */
    protected void checkHost(HttpHost host, InetAddress hostAddress) throws HttpException {
        checkerStrategy.checkHost(host, hostAddress);
    }

}
