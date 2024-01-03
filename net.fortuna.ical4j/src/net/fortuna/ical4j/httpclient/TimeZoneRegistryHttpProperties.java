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

package net.fortuna.ical4j.httpclient;

import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.HttpBasicConfig;
import com.openexchange.rest.client.httpclient.util.HostCheckingRoutePlanner;
import com.openexchange.rest.client.httpclient.util.HostCheckingRoutePlanner.HostCheckerStrategy;
import java.net.InetAddress;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;

/**
 * {@link TimeZoneRegistryHttpProperties}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public class TimeZoneRegistryHttpProperties extends DefaultHttpClientConfigProvider {

    private static final String HTTP_CLIENT_ID = "icaltimezone";

    /**
     * Gets the identifier for the HTTP client.
     *
     * @return The HTTP client identifier
     */
    public static String getHttpClientId() {
        return HTTP_CLIENT_ID;
    }

    private static final HostCheckerStrategy CHECKER_STRATEGY = new HostCheckerStrategy() {

        @Override
        public void checkHost(HttpHost host, InetAddress hostAddress) throws HttpException {
            if (TimeZoneRegistryRedirectStrategy.DENIED_HOSTS.contains(hostAddress)) {
                throw new HttpException("Invalid target host: " + host.getHostName());
            }
        }
    };

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link TimeZoneRegistryHttpProperties}.
     */
    public TimeZoneRegistryHttpProperties() {
        super(HTTP_CLIENT_ID, "Open-Xchange ICal4j Time Zone Fetcher Client");
    }

    @Override
    public HttpBasicConfig configureHttpBasicConfig(HttpBasicConfig config) {
        config.setMaxTotalConnections(100);
        config.setMaxConnectionsPerRoute(100);
        config.setConnectTimeout(5000);
        config.setSocketReadTimeout(5000);
        return config;
    }

    @Override
    public void modify(HttpClientBuilder builder) {
        super.modify(builder);
        builder.setRedirectStrategy(TimeZoneRegistryRedirectStrategy.getInstance());
        HostCheckingRoutePlanner.injectHostCheckingRoutePlanner(CHECKER_STRATEGY, builder);
    }

}
