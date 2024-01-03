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
package com.openexchange.mail.login.resolver.ldap.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.ldap.common.LDAPService;
import com.openexchange.mail.login.resolver.MailLoginResolver;
import com.openexchange.mail.login.resolver.ldap.LDAPResolverProperties;
import com.openexchange.mail.login.resolver.ldap.impl.LDAPResolver;
import com.openexchange.mail.login.resolver.ldap.impl.LDAPResolverConfig;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;

/**
 * {@link LDAPResolverActivator}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class LDAPResolverActivator extends HousekeepingActivator implements Reloadable {

    private static final Logger LOG = LoggerFactory.getLogger(LDAPResolverActivator.class);

    /**
     * Initializes a new {@link LDAPResolverActivator}.
     */
    public LDAPResolverActivator() {
        super();
    }

    @Override
    protected void startBundle() throws Exception {
        registerService(Reloadable.class, this);
        registerLDAPResolver();
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        unregisterService(LDAPResolver.class);
        try {
            registerLDAPResolver();
        } catch (Exception e) {
            LOG.error("Unexpected error registering LDAP-based mail login resolver", e);
        }
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { UserService.class, ContextService.class, LeanConfigurationService.class, LDAPService.class };
    }

    @Override
    public Interests getInterests() {
        return DefaultInterests.builder().propertiesOfInterest(
            LDAPResolverProperties.CACHE_EXPIRE.getFQPropertyName()
        ).build();
    }

    private void registerLDAPResolver() throws Exception {
        LeanConfigurationService leanConfigService = getServiceSafe(LeanConfigurationService.class);
        LDAPService ldapService = getServiceSafe(LDAPService.class);
        UserService userService = getServiceSafe(UserService.class);
        ContextService contextService = getServiceSafe(ContextService.class);

        LDAPResolverConfig resolverConfig = new LDAPResolverConfig(leanConfigService);
        LDAPResolver ldapResolver = new LDAPResolver(userService, contextService, ldapService, resolverConfig);

        registerService(MailLoginResolver.class, ldapResolver); 
    }

}