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

package com.openexchange.login.internal;

import static com.openexchange.authentication.AuthenticationResult.Status.SUCCESS;
import java.util.Collections;
import java.util.Map;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.AuthenticationServiceRegistry;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.login.LoginRequest;
import com.openexchange.server.services.ServerServiceRegistry;

/**
 * Uses the auto login authentication method to perform the authentication.
 *
 * @see AuthenticationService#handleAutoLoginRequest(com.openexchange.authentication.LoginInfo)
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
final class AutoLoginMethod implements LoginMethodClosure {

    private final LoginRequest request;
    private final Map<String, Object> properties;

    /**
     * Initializes a new {@link AutoLoginMethod}.
     *
     * @param request The login request
     * @param properties The arbitrary properties; e.g. <code>"headers"</code> or <code>{@link com.openexchange.authentication.Cookie "cookies"}</code>
     */
    AutoLoginMethod(LoginRequest request, Map<String, Object> properties) {
        super();
        this.request = request;
        this.properties = properties;
    }

    @Override
    public Authenticated doAuthentication(LoginResultImpl retval) throws OXException {
        // @formatter:off
        AuthenticationRequest authRequest = AuthenticationRequest.builder()
            .withLogin(request.getLogin())
            .withPassword(request.getPassword())
            .withClient(request.getClient())
            .withClientIP(request.getClientIP())
            .withUserAgent(request.getUserAgent())
            .withParameters(Collections.singletonMap(com.openexchange.login.LoginRequest.class.getName(), (Object)request))
            .withProperties(properties)
            .build();
        // @formatter:on
        AuthenticationServiceRegistry authenticationServiceRegistry = ServerServiceRegistry.getInstance().getService(AuthenticationServiceRegistry.class, true);
        AuthenticationResult result = authenticationServiceRegistry.doAutologin(authRequest);
        if (SUCCESS.equals(result.getStatus())) {
            return result.getAuthenticated().get();
        }
        if (result.getException().isPresent()) {
            OXException e = result.getException().get();
            if (LoginExceptionCodes.INVALID_CREDENTIALS_MISSING_USER_MAPPING.equals(e)) {
                // @formatter:off
                AuthenticationRequest newAuthenticationRequest = AuthenticationRequest.builder()
                    .withLogin(checkAceNotation(authRequest, e))
                    .withPassword(authRequest.getPassword())
                    .withClient(authRequest.getClient())
                    .withClientIP(authRequest.getClientIP())
                    .withUserAgent(authRequest.getUserAgent())
                    .withParameters(authRequest.getParameters())
                    .withProperties(authRequest.getProperties())
                    .build();
                // @formatter:on
                result = authenticationServiceRegistry.doAutologin(newAuthenticationRequest);
                if (SUCCESS.equals(result.getStatus())) {
                    return result.getAuthenticated().get();
                }
            }
            throw result.getException().orElseThrow(() -> e);
        }
        LOG.error("No authentication service found to handle request for {}", request.getLogin());
        throw LoginExceptionCodes.LOGIN_DENIED.create();
    }
}
