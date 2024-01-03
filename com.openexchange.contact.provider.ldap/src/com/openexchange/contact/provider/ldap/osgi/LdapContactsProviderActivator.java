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

package com.openexchange.contact.provider.ldap.osgi;

import static com.openexchange.contact.provider.ldap.config.ConfigUtils.CONFIG_FILENAME;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.MAPPING_FILENAME;
import static com.openexchange.contact.provider.ldap.config.ConfigUtils.PROPERTY_ACCOUNTS;
import java.util.Map;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.contact.provider.ContactsAccountService;
import com.openexchange.contact.provider.ContactsProvider;
import com.openexchange.contact.provider.ldap.LdapContactsProvider;
import com.openexchange.contact.provider.ldap.LocalCacheRegistry;
import com.openexchange.contact.provider.ldap.config.ProviderConfig;
import com.openexchange.contact.provider.ldap.config.ProviderConfigFactory;
import com.openexchange.exception.OXException;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.osgi.HousekeepingActivator;

/**
 * {@link LdapContactsProviderActivator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class LdapContactsProviderActivator extends HousekeepingActivator implements Reloadable {

    private static final Logger LOG = LoggerFactory.getLogger(LdapContactsProviderActivator.class);

    private LocalCacheRegistry registry;

    /**
     * Initializes a new {@link LdapContactsProviderActivator}.
     */
    public LdapContactsProviderActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class, ConfigurationService.class, ContactsAccountService.class, LDAPService.class };
    }

    @Override
    protected synchronized void startBundle() throws Exception {
        try {
            LOG.info("Starting bundle {}", context.getBundle());
            registerService(Reloadable.class, this);
            registry = registerProviderServices();
        } catch (Exception e) {
            LOG.error("Error starting {}", context.getBundle(), e);
            throw e;
        }
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        LOG.info("Stopping bundle {}", context.getBundle());
        unregisterProviderServices();
        super.stop(context);
    }

    @Override
    public Interests getInterests() {
        return DefaultInterests.builder().configFileNames(CONFIG_FILENAME, MAPPING_FILENAME).propertiesOfInterest(PROPERTY_ACCOUNTS.getFQPropertyName(), LocalCacheRegistry.PROPERTY_EXPIRE.getFQPropertyName()).build();
    }

    @Override
    public synchronized void reloadConfiguration(ConfigurationService configService) {
        LOG.info("Reloading configuration for {}", context.getBundle());
        unregisterProviderServices();
        try {
            this.registry = registerProviderServices();
        } catch (Exception e) {
            LOG.error("Unexpected error registering LDAP contact providers", e);
        }
    }

    private void unregisterProviderServices() {
        LOG.info("Unregistering LDAP contacts providers");
        try {
            // recreate LocalCacheRegistry to consider changed cache configuration
            LocalCacheRegistry registry = this.registry;
            if (registry != null) {
                this.registry = null;
                registry.invalidateCache();
            }
            unregisterService(ContactsProvider.class);
        } catch (Exception e) {
            LOG.error("Unexpected error unregistering LDAP contact providers", e);
        }
    }

    private LocalCacheRegistry registerProviderServices() throws OXException {
        LocalCacheRegistry registry = new LocalCacheRegistry(this);
        for (Map.Entry<String, ProviderConfig> entry : new ProviderConfigFactory(this).getConfigs().entrySet()) {
            registerService(ContactsProvider.class, new LdapContactsProvider(this, entry.getKey(), entry.getValue(), registry));
            LOG.info("Successfully registered LDAP contacts provider \"{}\"", entry.getKey());
        }
        return registry;
    }

}
