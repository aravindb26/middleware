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

package com.openexchange.health.rest;

import java.lang.reflect.Method;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.health.MWHealthCheckProperty;
import com.openexchange.health.MWHealthCheckService;
import com.openexchange.java.Strings;
import com.openexchange.rest.services.EndpointAuthenticator;
import com.openexchange.server.ServiceLookup;

/**
 * {@link AbstractMWHealthEndpoint} - The abstract class for health check REST end-points.
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.6
 */
public abstract class AbstractMWHealthEndpoint implements EndpointAuthenticator {

    /** The service look-up */
    protected final ServiceLookup services;

    /**
     * Initializes a new {@link AbstractMWHealthEndpoint}.
     *
     * @param services The {@link ServiceLookup}
     */
    protected AbstractMWHealthEndpoint(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public String getRealmName() {
        return "OX HEALTH";
    }

    @Override
    public boolean permitAll(Method invokedMethod) {
        try {
            Credentials credentials = getCredentials();
            return (Strings.isEmpty(credentials.getLogin()) && Strings.isEmpty(credentials.getPassword()));
        } catch (Exception e) {
            MWHealthCheckService.LOG.error("Failed to determine credentials for health check REST end-point. Signaling no 'permit all' access.", e);
            return false;
        }
    }

    @Override
    public boolean authenticate(String login, String password, Method invokedMethod) {
        try {
            Credentials credentials = getCredentials();
            return credentials.getLogin().equals(login) && credentials.getPassword().equals(password);
        } catch (Exception e) {
            MWHealthCheckService.LOG.error("Failed to determine credentials for health check REST end-point. Denying access.", e);
            return false;
        }
    }

    private Credentials getCredentials() throws OXException {
        LeanConfigurationService configurationService = services.getServiceSafe(LeanConfigurationService.class);
        return new Credentials(configurationService.getProperty(MWHealthCheckProperty.username), configurationService.getProperty(MWHealthCheckProperty.password));
    }

}
