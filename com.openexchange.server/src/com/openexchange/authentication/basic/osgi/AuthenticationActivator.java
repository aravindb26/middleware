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

package com.openexchange.authentication.basic.osgi;

import org.osgi.framework.ServiceReference;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.authentication.BasicAuthenticationService;
import com.openexchange.authentication.basic.DefaultBasicAuthentication;
import com.openexchange.context.ContextService;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.osgi.SimpleRegistryListener;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.user.UserService;

/**
 * Activator to register {@link BasicAuthenticationService}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public final class AuthenticationActivator extends HousekeepingActivator {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AuthenticationActivator.class);

    /**
     * Initializes a new {@link AuthenticationActivator}.
     */
    public AuthenticationActivator() {
        super();
    }

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class<?>[] { ContextService.class, UserService.class };
    }

    @Override
    protected void startBundle() throws Exception {
        track(AuthenticationServiceRegistry.class, new SimpleRegistryListener<AuthenticationServiceRegistry>() {

            @Override
            public void added(ServiceReference<AuthenticationServiceRegistry> ref, AuthenticationServiceRegistry service) {
                ServerServiceRegistry.getInstance().addService(AuthenticationServiceRegistry.class, service);
            }

            @Override
            public void removed(ServiceReference<AuthenticationServiceRegistry> ref, AuthenticationServiceRegistry service) {
                ServerServiceRegistry.getInstance().removeService(AuthenticationServiceRegistry.class);
            }

        });
        openTrackers();

        LOG.info("Registering default basic authentication service.");
        DefaultBasicAuthentication basicAuthentication = new DefaultBasicAuthentication(getService(ContextService.class), getService(UserService.class));
        registerService(BasicAuthenticationService.class, basicAuthentication);
    }

}
