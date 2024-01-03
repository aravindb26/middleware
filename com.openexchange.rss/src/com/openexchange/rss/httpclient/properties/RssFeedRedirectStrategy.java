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

package com.openexchange.rss.httpclient.properties;

import java.net.URI;
import org.apache.http.protocol.HttpContext;
import com.openexchange.rest.client.httpclient.util.LocationCheckingRedirectStrategy;
import com.openexchange.rest.client.httpclient.util.UriDeniedProtocolException;
import com.openexchange.rss.osgi.Services;
import com.openexchange.rss.utils.RssProperties;

/**
 *
 * {@link RssFeedRedirectStrategy} - The redirect strategy for RSS feeds.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RssFeedRedirectStrategy extends LocationCheckingRedirectStrategy {

    /** The attribute name to refer to a boolean value whether original address was a remote one */
    public static final String ATTRIBUTE_ORIGINAL_ADDR_IS_REMOTE = "com.openexchange.rss.originalAddressIsRemote";

    private static final RssFeedRedirectStrategy MYINSTANCE = new RssFeedRedirectStrategy();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static RssFeedRedirectStrategy getInstance() {
        return MYINSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private RssFeedRedirectStrategy() {
        super();
    }

    @Override
    protected boolean isInternalHostAllowed(HttpContext context) {
        Object attribute = context.getAttribute(ATTRIBUTE_ORIGINAL_ADDR_IS_REMOTE);
        return (attribute == null) || !Boolean.parseBoolean(attribute.toString());
    }

    @Override
    protected void checkLocationUri(URI locationURI, HttpContext context) throws UriDeniedProtocolException {
        RssProperties rssProperties = Services.getService(RssProperties.class);
        if (rssProperties == null) {
            throw new UriDeniedProtocolException("Absent service: " + RssProperties.class.getName(), locationURI);
        }
        if (rssProperties.isDenied(locationURI) ) {
            throw new UriDeniedProtocolException("Invalid redirect URI: " + locationURI + ". The URI is not allowed due to configuration.", locationURI);
        }
    }

}
