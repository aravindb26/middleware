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

package com.openexchange.mail.authenticity.impl.trusted.internal.fetcher.httpclient;

import org.apache.http.impl.client.HttpClientBuilder;
import com.openexchange.rest.client.httpclient.DefaultHttpClientConfigProvider;
import com.openexchange.rest.client.httpclient.HttpBasicConfig;

/**
 * {@link TrustedMailIconHttpProperties}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.4
 */
public class TrustedMailIconHttpProperties extends DefaultHttpClientConfigProvider {

    private static final String HTTP_CLIENT_ID = "trustedmailicon";

    /**
     * Gets the identifier for the HTTP client: <code>"trustedmailicon"</code>.
     *
     * @return The HTTP client identifier
     */
    public static String getHttpClientId() {
        return HTTP_CLIENT_ID;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link TrustedMailIconHttpProperties}.
     */
    public TrustedMailIconHttpProperties() {
        super(HTTP_CLIENT_ID, "Open-Xchange Trusted Mail Icon Client");
    }

    @Override
    public HttpBasicConfig configureHttpBasicConfig(HttpBasicConfig config) {
        config.setMaxTotalConnections(100);
        config.setMaxConnectionsPerRoute(100);
        config.setConnectTimeout(2000);
        config.setSocketReadTimeout(2000);
        return config;
    }

    @Override
    public void modify(HttpClientBuilder builder) {
        super.modify(builder);
        builder.setRedirectStrategy(TrustedMailconRedirectStrategy.getInstance());
    }

}
