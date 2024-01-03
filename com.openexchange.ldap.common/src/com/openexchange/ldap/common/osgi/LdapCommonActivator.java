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

package com.openexchange.ldap.common.osgi;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.ForcedReloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.keystore.KeyStoreService;
import com.openexchange.ldap.common.BindRequestFactory;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.ldap.common.config.ExternalLDAPConfigProviderRegistry;
import com.openexchange.ldap.common.config.LDAPConfigLoader;
import com.openexchange.ldap.common.impl.LDAPServiceImpl;
import com.openexchange.lock.LockService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.NearRegistryServiceTracker;

/**
 * {@link LdapCommonActivator}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.6
 */
public class LdapCommonActivator extends HousekeepingActivator {

    private static final Logger LOG = LoggerFactory.getLogger(LdapCommonActivator.class);

    /**
     * Initializes a new {@link LdapCommonActivator}.
     */
    public LdapCommonActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class, ConfigurationService.class, LockService.class, KeyStoreService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        try {
            LOG.info("Starting bundle {}", context.getBundle());
            ExternalLDAPConfigProviderRegistry providerRegistry = new ExternalLDAPConfigProviderRegistry(context);
            rememberTracker(providerRegistry);
            NearRegistryServiceTracker<BindRequestFactory> bindRequestFactories = new NearRegistryServiceTracker<>(context, BindRequestFactory.class);
            rememberTracker(bindRequestFactories);
            openTrackers();
            // TODO: Really? --> registerService(NearRegistryServiceTracker.class, providerRegistry);
            // @formatter:off
            LDAPServiceImpl ldapService = new LDAPServiceImpl(getServiceSafe(LockService.class),
                                                              new LDAPConfigLoader(getServiceSafe(ConfigurationService.class), providerRegistry),
                                                              bindRequestFactories,
                                                              getServiceSafe(KeyStoreService.class));
            // @formatter:on
            registerService(ForcedReloadable.class, ldapService);
            registerService(LDAPService.class, ldapService);
        } catch (Exception e) {
            LOG.error("Error starting {}", context.getBundle(), e);
            throw e;
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("Stopping bundle {}", context.getBundle());
        super.stop(context);
    }

}
