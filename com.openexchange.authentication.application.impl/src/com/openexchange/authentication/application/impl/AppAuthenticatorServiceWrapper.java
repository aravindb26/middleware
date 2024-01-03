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

package com.openexchange.authentication.application.impl;

import java.util.Map;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.application.AppAuthenticatorService;
import com.openexchange.authentication.application.AppLoginRequest;
import com.openexchange.authentication.application.AppPasswordGenerator;
import com.openexchange.exception.OXException;


/**
 * {@link AppAuthenticatorServiceWrapper} - Wraps an {@link AppAuthenticatorService} into {@link AuthenticationService} make it usable from {@link AuthenticationServiceRegistry}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class AppAuthenticatorServiceWrapper implements AuthenticationService {

    private final AppAuthenticatorService appAuthenticatorService;

    /**
     * Initializes a new {@link AppAuthenticatorServiceWrapper}.
     *
     * @param appAuthenticatorService The delegate {@link AppAuthenticatorService}
     */
    public AppAuthenticatorServiceWrapper(AppAuthenticatorService appAuthenticatorService) {
        super();
        this.appAuthenticatorService = appAuthenticatorService;
    }

    @Override
    public AuthenticationResult doLogin(AuthenticationRequest authenticationRequest, boolean autologin) throws OXException {
        if (isResponsibleFor(authenticationRequest)) {
            if (autologin) {
                return AuthenticationResult.failed(LoginExceptionCodes.NOT_SUPPORTED.create(AppAuthenticatorService.class.getName()));
            }
            try {
                return AuthenticationResult.success(appAuthenticatorService.doAuth(toAppLoginRequest(authenticationRequest)));
            } catch (OXException e) {
                return AuthenticationResult.failed(e);
            }
        }
        return AuthenticationResult.failed();
    }

    private boolean isResponsibleFor(AuthenticationRequest authenticationRequest) {
        if (AppPasswordGenerator.isInExpectedFormat(authenticationRequest.getPassword())) {
            return appAuthenticatorService.applies(toAppLoginRequest(authenticationRequest));
        }
        return false;
    }

    @Override
    public int getRanking() {
        return Integer.MAX_VALUE;
    }

    private AppLoginRequest toAppLoginRequest(AuthenticationRequest request) {
        return new AppLoginRequest() {

            @Override
            public String getUserAgent() {
                return request.getUserAgent();
            }

            @Override
            public String getPassword() {
                return request.getPassword();
            }

            @Override
            public Map<String, Object> getParameters() {
                return request.getParameters();
            }

            @Override
            public String getLogin() {
                return request.getLogin();
            }

            @Override
            public String getClientIP() {
                return request.getClientIP();
            }

            @Override
            public String getClient() {
                return request.getClient();
            }
        };
    }

}
