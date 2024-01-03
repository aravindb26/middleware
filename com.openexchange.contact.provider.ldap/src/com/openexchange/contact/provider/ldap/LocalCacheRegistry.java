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

package com.openexchange.contact.provider.ldap;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.contact.provider.ldap.config.ProviderConfig;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.ldap.common.LDAPConnectionProvider;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.functions.ErrorAwareSupplier;
import com.openexchange.tools.strings.TimeSpanParser;

/**
 * {@link LocalCacheRegistry} is a registry for {@link LocalCache}s.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LocalCacheRegistry {

    private static class LoggerHolder {
        static final Logger LOG = LoggerFactory.getLogger(LocalCacheRegistry.class);
    }

    /** The property to configure the expire time of the ldap contact cache */
    public static final Property PROPERTY_EXPIRE = DefaultProperty.valueOf("com.openexchange.contacts.ldap.cache.expire", "2h");

    private final ServiceLookup services;
    private final Cache<String, LocalCache> caches;

    /**
     * Initializes a new {@link LocalCacheRegistry}.
     *
     * @param services The service look-up
     * @throws OXException
     */
    public LocalCacheRegistry(ServiceLookup services) throws OXException {
        super();
        this.services = services;
        try {
            LeanConfigurationService configurationService = services.getServiceSafe(LeanConfigurationService.class);
            Duration expire = Duration.ofMillis(TimeSpanParser.parseTimespanToPrimitive(configurationService.getProperty(PROPERTY_EXPIRE)));
            this.caches = CacheBuilder.newBuilder().expireAfterWrite(expire).build();
        } catch (IllegalArgumentException e) {
            throw LdapContactsExceptionCodes.WRONG_OR_MISSING_CONFIG_VALUE.create(e, PROPERTY_EXPIRE.getFQPropertyName());
        }
    }

    /**
     * Gets the optional cache.
     *
     * @param providerId The provider identifier
     * @param config The {@link ProviderConfig}
     * @param loader A supplier for the contact data
     * @return The optional cache
     * @throws OXException If an error occurred when refreshing the cache
     */
    public Optional<LocalCache> optCache(String providerId, ProviderConfig config, ErrorAwareSupplier<List<Contact>> loader) throws OXException {
        LocalCache cache = caches.getIfPresent(providerId);
        if (cache != null) {
            // cache already initialized
            return Optional.of(cache);
        }

        try {
            LDAPConnectionProvider connectionProvider = services.getServiceSafe(LDAPService.class).getConnection(config.getLdapClientId());
            if (connectionProvider.isIndividualBind()) {
                // Caching for connection with individual bind not possible
                return Optional.empty();
            }
            // check cache again
            return Optional.of(caches.get(providerId, () -> new LocalCache(loader)));
        } catch (ExecutionException | UncheckedExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException) {
                throw (OXException) cause;
            }
            LoggerHolder.LOG.error("Unexpected error during cache creation: {}", cause.getMessage(), cause);
            return Optional.empty();
        }
    }

    /**
     * Invalidates the cache
     */
    public void invalidateCache() {
        caches.invalidateAll();
    }

}
