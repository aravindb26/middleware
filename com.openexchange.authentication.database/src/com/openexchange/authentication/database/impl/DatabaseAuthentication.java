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

package com.openexchange.authentication.database.impl;

import com.openexchange.authentication.AuthenticationDriver;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.common.AbstractConfigurationAwareAuthenticationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;

/**
 * This implementation authenticates the user against the database.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DatabaseAuthentication extends AbstractConfigurationAwareAuthenticationService {

    private final static String AUTH_IDENTIFIER = "database";

    private final AuthenticationDriver databaseAuthenticationDriver;
    private final ServiceLookup services;

    /**
     * Default constructor.
     */
    public DatabaseAuthentication(AuthenticationDriver databaseAuthenticationDriver, ServiceLookup services) {
        super(AUTH_IDENTIFIER);
        this.databaseAuthenticationDriver = databaseAuthenticationDriver;
        this.services = services;
    }

    @Override
    public AuthenticationResult handleLoginRequest(AuthenticationRequest authenticationRequest) throws OXException {
        return databaseAuthenticationDriver.doLogin(authenticationRequest, false);
    }

    @Override
    public AuthenticationResult handleAutoLoginRequest(AuthenticationRequest authenticationRequest) throws OXException {
        return databaseAuthenticationDriver.doLogin(authenticationRequest, true);
    }

    @Override
    protected LeanConfigurationService getConfigurationService() throws OXException {
        return services.getServiceSafe(LeanConfigurationService.class);
    }

}
