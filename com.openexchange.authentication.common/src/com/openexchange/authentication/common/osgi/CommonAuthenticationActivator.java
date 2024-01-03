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

package com.openexchange.authentication.common.osgi;

import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.authentication.BasicAuthenticationService;
import com.openexchange.authentication.common.impl.AuthenticationServiceRegistryImpl;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.context.ContextService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.user.UserService;

/**
 * {@link CommonAuthenticationActivator}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class CommonAuthenticationActivator extends HousekeepingActivator {

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ContextService.class, UserService.class, LeanConfigurationService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        AuthenticationServiceRegistryImpl registry = new AuthenticationServiceRegistryImpl();

        track(AuthenticationService.class, new AuthenticationServiceTracker(registry));
        track(BasicAuthenticationService.class, new BasicAuthenticationServiceTracker(registry));
        openTrackers();

        registerService(AuthenticationServiceRegistry.class, registry);
        registerService(Reloadable.class, registry);
    }

}
