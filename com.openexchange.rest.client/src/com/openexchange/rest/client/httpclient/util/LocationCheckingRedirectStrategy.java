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
import java.net.URI;
import java.net.UnknownHostException;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;
import com.openexchange.java.InetAddresses;

/**
 * {@link LocationCheckingRedirectStrategy} - A redirect strategy for <code>HttpClient</code> that checks the URI provided by
 * <code>"Location"</code> header for validity.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public abstract class LocationCheckingRedirectStrategy extends DefaultRedirectStrategy {

    /**
     * Initializes a new {@link LocationCheckingRedirectStrategy}.
     */
    protected LocationCheckingRedirectStrategy() {
        super();
    }

    @Override
    public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException {
        Header locationHeader = response.getFirstHeader("location");
        String location = locationHeader.getValue();
        URI locationURI = super.createLocationURI(location);
        if (!isInternalHostAllowed(context)) {
            try {
                InetAddress inetAddress = InetAddresses.forString(locationURI.getHost());
                if (InetAddresses.isInternalAddress(inetAddress)) {
                    // Deny redirecting to a local address
                    throw new UriDeniedProtocolException("Invalid redirect URI: " + locationURI + ". The URI is local.", locationURI);
                }
            } catch (UnknownHostException e) {
                // IP address of that host could not be determined
                throw new UriDeniedProtocolException(locationURI.toString() + " contains an unknown host", locationURI, e);
            }
        }
        checkLocationUri(locationURI, context);
        return super.getRedirect(request, response, context);
    }

    /**
     * Signals whether internal hosts are allowed in redirect URIs.
     *
     * @param context The HTTP context providing attributes
     * @return <code>true</code> if allowed; otherwise <code>false</code>
     */
    protected abstract boolean isInternalHostAllowed(HttpContext context);

    /**
     * Checks given URI extracted from <code>"Location"</code> header for validity.
     *
     * @param locationURI The URI extracted from <code>"Location"</code> header
     * @param context The HTTP context providing attributes
     * @throws UriDeniedProtocolException If location URI is invalid
     */
    protected void checkLocationUri(URI locationURI, HttpContext context) throws UriDeniedProtocolException {
        // Nothing
    }
}
