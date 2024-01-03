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

import java.net.InetAddress;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.HttpClientBuilder;
import com.openexchange.java.InetAddresses;

/**
 * {@link InternalAddressDenyingRoutePlanner} - The route planner that denies access to an internal address (if the route determined for
 * a request has no proxy in between).
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public final class InternalAddressDenyingRoutePlanner extends HostCheckingRoutePlanner {

    /**
     * Injects internal address denying HTTP route planner into given builder instance.
     *
     * @param builder The builder instance to inject to
     * @return The given builder
     */
    public static HttpClientBuilder injectInternalAddressDenyingRoutePlanner(HttpClientBuilder builder) {
        return HostCheckingRoutePlanner.injectHostCheckingRoutePlanner(INTERNAL_ADDRESS_DENIED_STRATEGY, builder);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static final HostCheckerStrategy INTERNAL_ADDRESS_DENIED_STRATEGY = new HostCheckerStrategy() {

        @Override
        public void checkHost(HttpHost host, InetAddress hostAddress) throws HttpException {
            if (InetAddresses.isInternalAddress(hostAddress)) {
                throw new HttpException("Invalid target host: " + host.getHostName() + ". No access to local address allowed.");
            }
        }
    };

    /**
     * Initializes a new {@link InternalAddressDenyingRoutePlanner}.
     *
     * @param routePlanner The route planner to delegate to
     */
    private InternalAddressDenyingRoutePlanner(HttpRoutePlanner routePlanner) {
        super(INTERNAL_ADDRESS_DENIED_STRATEGY, routePlanner);
    }

}
