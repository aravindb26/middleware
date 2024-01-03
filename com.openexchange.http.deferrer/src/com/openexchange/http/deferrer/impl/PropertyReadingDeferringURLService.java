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

package com.openexchange.http.deferrer.impl;

import java.net.URI;
import java.net.URISyntaxException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;

/**
 * {@link PropertyReadingDeferringURLService} - The deferring URL service reading value from property <code>"com.openexchange.http.deferrer.url"</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class PropertyReadingDeferringURLService extends AbstractDeferringURLService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PropertyReadingDeferringURLService.class);

    private final ServiceLookup services;

    /**
     * Initializes a new {@link PropertyReadingDeferringURLService}.
     *
     * @param services The service look-up
     */
    public PropertyReadingDeferringURLService(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    protected String getDeferrerURL(final int userId, final int contextId) {
        if (userId <= 0 || contextId <= 0) {
            return services.getService(ConfigurationService.class).getProperty("com.openexchange.http.deferrer.url");
        }
        // Valid user/context identifiers
        try {
            final ConfigView view = services.getService(ConfigViewFactory.class).getView(userId, contextId);
            return view.get("com.openexchange.http.deferrer.url", String.class);
        } catch (Exception e) {
            final String url = services.getService(ConfigurationService.class).getProperty("com.openexchange.http.deferrer.url");
            LOG.error("Failed to retrieve deferrer URL via config-cascade look-up. Using global one instead: {}", null == url ? "null" : url, e);
            return url;
        }
    }

    @Override
    protected String determinePath(String deferrerURL, int userId, int contextId) {
        try {
            URI uri = new URI(deferrerURL);
            String path = uri.getPath();
            if (Strings.isEmpty(path)) {
                return new StringBuilder(PREFIX.get().getPrefix()).append("defer").toString();
            }

            String expectedPath = new StringBuilder(PREFIX.get().getPrefix()).append("defer").toString();
            if (Strings.asciiLowerCase(path).equals(Strings.asciiLowerCase(expectedPath))) {
                // Already ends with expected path
                return "";
            }

            URI alt1 = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "", uri.getQuery(), uri.getFragment());
            URI alt2 = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), expectedPath, uri.getQuery(), uri.getFragment());
            throw new IllegalStateException("Configured deferrer URL (\"" + deferrerURL + "\") does not end with expected path (\"" + expectedPath + "\") for user " + userId + " in context " + contextId + ". Please adjust property \"com.openexchange.http.deferrer.url\" accordingly to either \"" + alt1 + "\" or \"" + alt2 + "\"");
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Configured deferrer URL (\"" + deferrerURL + "\") is not a valid URI for user " + userId + " in context " + contextId, e);
        }
    }

}
