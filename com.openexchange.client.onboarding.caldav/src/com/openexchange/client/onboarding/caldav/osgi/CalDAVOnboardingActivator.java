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

package com.openexchange.client.onboarding.caldav.osgi;

import com.openexchange.client.onboarding.OnboardingProvider;
import com.openexchange.client.onboarding.caldav.CalDAVOnboardingProvider;
import com.openexchange.client.onboarding.caldav.custom.CustomLoginSource;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.serverconfig.ServerConfigService;


/**
 * {@link CalDAVOnboardingActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class CalDAVOnboardingActivator extends HousekeepingActivator {

    /**
     * Initializes a new {@link CalDAVOnboardingActivator}.
     */
    public CalDAVOnboardingActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ConfigViewFactory.class, ConfigurationService.class, ServerConfigService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        RankingAwareNearRegistryServiceTracker<CustomLoginSource> loginSources = new RankingAwareNearRegistryServiceTracker<>(context, CustomLoginSource.class);
        rememberTracker(loginSources);
        openTrackers();

        registerService(OnboardingProvider.class, new CalDAVOnboardingProvider(loginSources, this));
    }

}
