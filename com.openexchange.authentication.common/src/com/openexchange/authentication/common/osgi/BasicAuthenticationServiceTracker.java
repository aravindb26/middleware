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

import org.osgi.framework.ServiceReference;
import com.openexchange.authentication.BasicAuthenticationService;
import com.openexchange.authentication.common.impl.AuthenticationServiceRegistryImpl;
import com.openexchange.osgi.SimpleRegistryListener;


/**
 * {@link BasicAuthenticationServiceTracker}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class BasicAuthenticationServiceTracker implements SimpleRegistryListener<BasicAuthenticationService> {

    private final AuthenticationServiceRegistryImpl registry;

    /**
     * Initializes a new {@link BasicAuthenticationServiceTracker}.
     *
     * @param registry The registry instance
     */
    public BasicAuthenticationServiceTracker(AuthenticationServiceRegistryImpl registry) {
        super();
        this.registry = registry;
    }

    @Override
    public void added(ServiceReference<BasicAuthenticationService> ref, BasicAuthenticationService service) {
        registry.setBasicAuthenticationService(service);
    }

    @Override
    public void removed(ServiceReference<BasicAuthenticationService> ref, BasicAuthenticationService service) {
        registry.setBasicAuthenticationService(null);
    }

}
