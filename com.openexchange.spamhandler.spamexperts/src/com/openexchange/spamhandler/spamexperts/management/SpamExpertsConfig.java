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

package com.openexchange.spamhandler.spamexperts.management;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.spamhandler.spamexperts.exceptions.SpamExpertsExceptionCode;
import com.openexchange.tools.net.URIDefaults;
import com.openexchange.tools.net.URIParser;

/**
 * {@link SpamExpertsConfig}
 */
public class SpamExpertsConfig {

    private final ServiceLookup services;
    private final Cache<String, URI> uriCache;

    /**
     * Initializes a new {@link SpamExpertsConfig}.
     */
    public SpamExpertsConfig(ServiceLookup services) {
        super();
        this.services = services;
        uriCache = CacheBuilder.newBuilder().expireAfterAccess(2, TimeUnit.HOURS).build();
    }

    /**
     * Clears the URI cache.
     */
    public void clearCache() {
        uriCache.invalidateAll();
    }

    /**
     * Gets the user-sensitive property.
     *
     * @param session The session providing user data
     * @param propertyName The name of the property to look-up
     * @param defaultValue The default value to return
     * @param clazz The type of the property's value
     * @return The property value of given default value if absent
     * @throws OXException If property value cannot be returned
     */
    public <V> V getPropertyFor(Session session, String propertyName, V defaultValue, Class<V> clazz) throws OXException {
        ConfigViewFactory factory = services.getService(ConfigViewFactory.class);
        ConfigView view = factory.getView(session.getUserId(), session.getContextId());

        ComposedConfigProperty<V> property = view.property(propertyName, clazz);
        if (null == property) {
            return defaultValue;
        }

        V value = property.get();
        return value == null ? defaultValue : value;
    }

    /**
     * Requires specified property
     *
     * @param session The session providing user data
     * @param propertyName The name of the required property
     * @return The value
     * @throws OXException If property value cannot be returned
     */
    public String requireProperty(Session session, String propertyName) throws OXException {
        String value = getPropertyFor(session, propertyName, "", String.class).trim();
        if (Strings.isEmpty(value)) {
            throw SpamExpertsExceptionCode.MISSING_CONFIG_OPTION.create(propertyName);
        }
        return value;
    }

    /**
     * Gets the denoted URI property
     *
     * @param session The session providing user data
     * @param propertyName The name of the required property
     * @param defaultValue The default value
     * @return The URI property
     * @throws OXException If URI property cannot be returned
     */
    public URI getUriProperty(Session session, String propertyName, String defaultValue) throws OXException {
        String sUri = getPropertyFor(session, propertyName, defaultValue, String.class).trim();
        return getUriFor(sUri);
    }

    /**
     * Requires the denoted URI property
     *
     * @param session The session providing user data
     * @param propertyName The name of the required property
     * @return The URI property
     * @throws OXException If URI property cannot be returned
     */
    public URI requireUriProperty(Session session, String propertyName) throws OXException {
        String sUri = requireProperty(session, propertyName);
        return getUriFor(sUri);
    }

    private URI getUriFor(String sUri) throws OXException {
        URI uri = uriCache.getIfPresent(sUri);
        if (null == uri) {
            try {
                uri = new URI(sUri);
                if (Strings.isEmpty(uri.getHost())) {
                    throw SpamExpertsExceptionCode.INVALID_URL.create(sUri);
                }
                uriCache.put(sUri, uri);
            } catch (URISyntaxException e) {
                throw SpamExpertsExceptionCode.INVALID_URL.create(e, sUri);
            }
        }
        return uri;
    }

    /**
     * Gets the configured IMAP URI
     *
     * @param session The session providing user data
     * @return The IMAP URI
     * @throws OXException If IMAP URI cannot be returned
     */
    public URI getImapURL(Session session) throws OXException {
        String iurl = requireProperty(session, "com.openexchange.custom.spamexperts.imapurl");

        URI uri = uriCache.getIfPresent(iurl);
        if (null == uri) {
            try {
                uri = URIParser.parse(iurl, URIDefaults.IMAP);
                uriCache.put(iurl, uri);
            } catch (URISyntaxException e) {
                throw SpamExpertsExceptionCode.INVALID_URL.create(e, iurl);
            }
        }
        return uri;
    }

}
