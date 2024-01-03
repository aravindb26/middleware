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

package com.openexchange.chronos.provider.ical.httpclient.properties;

import java.net.URI;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

/**
 * 
 * {@link BlacklistAwareRedirectStrategy}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class BlacklistAwareRedirectStrategy extends DefaultRedirectStrategy {

    private static final BlacklistAwareRedirectStrategy TARGET_STRATEGY_INSTANCE = new BlacklistAwareRedirectStrategy();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static BlacklistAwareRedirectStrategy getInstance() {
        return TARGET_STRATEGY_INSTANCE;
    }

    private BlacklistAwareRedirectStrategy() {
        super();
    }

    @Override
    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        /*
         * Get location and check if it is internal, if so check that internal requests are allowed
         */
        final Header locationHeader = response.getFirstHeader("location");
        final String location = locationHeader.getValue();
        URI locationURI = super.createLocationURI(location);
        if (ICalCalendarProviderProperties.isBlacklisted(locationURI.getHost())) {
            throw new ProtocolException("Invalid redirect URI: " + locationURI.getHost() + ". The URI is not allowed due to configuration.");
        }
        return super.getRedirect(request, response, context);
    }
}
