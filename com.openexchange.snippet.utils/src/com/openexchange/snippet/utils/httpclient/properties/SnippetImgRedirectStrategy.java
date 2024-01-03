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

package com.openexchange.snippet.utils.httpclient.properties;

import java.net.URI;
import org.apache.http.protocol.HttpContext;
import com.openexchange.rest.client.httpclient.util.LocationCheckingRedirectStrategy;
import com.openexchange.rest.client.httpclient.util.UriDeniedProtocolException;
import com.openexchange.snippet.utils.SnippetProcessor;

/**
 *
 * {@link SnippetImgRedirectStrategy}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SnippetImgRedirectStrategy extends LocationCheckingRedirectStrategy {

    private static final SnippetImgRedirectStrategy MYINSTANCE = new SnippetImgRedirectStrategy();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static SnippetImgRedirectStrategy getInstance() {
        return MYINSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private SnippetImgRedirectStrategy() {
        super();
    }

    @Override
    protected boolean isInternalHostAllowed(HttpContext context) {
        return false;
    }

    @Override
    protected void checkLocationUri(URI locationURI, HttpContext context) throws UriDeniedProtocolException {
        if (SnippetProcessor.DENIED_HOSTS.contains(locationURI.getHost())) {
            // Deny redirecting to a local address
            throw new UriDeniedProtocolException("Invalid redirect URI: " + locationURI + ". The URI is local.", locationURI);
        }
    }

}
