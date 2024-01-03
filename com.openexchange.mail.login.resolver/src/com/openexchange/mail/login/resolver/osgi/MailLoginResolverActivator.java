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
package com.openexchange.mail.login.resolver.osgi;

import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.mail.login.resolver.MailLoginResolver;
import com.openexchange.mail.login.resolver.MailLoginResolverService;
import com.openexchange.mail.login.resolver.impl.MailLoginResolverServiceImpl;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;

/**
 * {@link MailLoginResolverActivator}
 *
 * @author <a href="mailto:philipp.schumacher@open-xchange.com">Philipp Schumacher</a>
 * @since v7.10.6
 */
public class MailLoginResolverActivator extends HousekeepingActivator {

    /**
     * Initialises a new {@link MailLoginResolverActivator}.
     */
    public MailLoginResolverActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { LeanConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        RankingAwareNearRegistryServiceTracker<MailLoginResolver> resolverTracker = new RankingAwareNearRegistryServiceTracker<MailLoginResolver>(context, MailLoginResolver.class);
        rememberTracker(resolverTracker);
        openTrackers();
        LeanConfigurationService configService = getServiceSafe(LeanConfigurationService.class);
        MailLoginResolverServiceImpl resolverService = new MailLoginResolverServiceImpl(configService, resolverTracker);
        registerService(MailLoginResolverService.class, resolverService);
    }

}