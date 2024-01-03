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

package com.openexchange.authentication.oauth.impl;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.openexchange.authentication.AuthenticationRequest;
import com.openexchange.authentication.AuthenticationResult;
import com.openexchange.authentication.DefaultLoginInfo;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.LoginInfo;
import com.openexchange.authentication.common.AbstractConfigurationAwareAuthenticationService;
import com.openexchange.authentication.oauth.http.OAuthAuthenticationHttpClientConfig;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.nimbusds.oauth2.sdk.http.send.HTTPSender;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.server.ServiceLookup;


/**
 * {@link OAuthAuthenticationService}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v8.0.0
 */
public class OAuthAuthenticationService extends AbstractConfigurationAwareAuthenticationService {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthAuthenticationService.class);
    private static final String AUTH_IDENTIFIER = "oauth";

    private final ServiceLookup services;
    private final PasswordGrantAuthentication passwordGrantAuthentication;

    /**
     * Initializes a new {@link OAuthAuthenticationService}.
     *
     * @param passwordGrantAuthentication The {@link PasswordGrantAuthentication}
     * @param services The {@link ServiceLookup}
     * @throws OXException
     */
    public OAuthAuthenticationService(PasswordGrantAuthentication passwordGrantAuthentication, ServiceLookup services) throws OXException {
        super(AUTH_IDENTIFIER);
        this.passwordGrantAuthentication = passwordGrantAuthentication;
        this.services = services;
    }

    @Override
    protected LeanConfigurationService getConfigurationService() throws OXException {
        return services.getServiceSafe(LeanConfigurationService.class);
    }

    @Override
    public AuthenticationResult handleLoginRequest(AuthenticationRequest authenticationRequest) throws OXException {
        if (Strings.isEmpty(authenticationRequest.getLogin()) || Strings.isEmpty(authenticationRequest.getPassword())) {
            return AuthenticationResult.failed(LoginExceptionCodes.INVALID_CREDENTIALS.create());
        }
        LoginInfo loginInfo = DefaultLoginInfo.of(authenticationRequest);
        OAuthAuthenticationConfig config = passwordGrantAuthentication.getConfig();

        //@formatter:off
        ResourceOwnerPasswordCredentialsGrant authorizationGrant = passwordGrantAuthentication.getAuthorizationGrant(loginInfo);
        TokenRequest request = new TokenRequest(config.getTokenEndpoint(),
                                                passwordGrantAuthentication.getClientAuthentication(),
                                                authorizationGrant,
                                                Scope.parse(config.getScope()));
        //@formatter:on
        TokenResponse response;
        try {
            LOG.debug("Sending password grant token request for user '{}' as '{}'", loginInfo.getUsername(), authorizationGrant.getUsername());
            response = TokenResponse.parse(HTTPSender.send(request.toHTTPRequest(), () -> {
                HttpClientService httpClientService = services.getOptionalService(HttpClientService.class);
                if (httpClientService == null) {
                    throw new IllegalStateException("Missing service " + HttpClientService.class.getName());
                }
                return httpClientService.getHttpClient(OAuthAuthenticationHttpClientConfig.getClientIdOAuthAuthentication());
            }));
        } catch (com.nimbusds.oauth2.sdk.ParseException | IOException e) {
            throw LoginExceptionCodes.UNKNOWN.create(e, e.getMessage());
        }

        AccessTokenResponse accessTokenResponse = passwordGrantAuthentication.validateResponse(request, response, loginInfo.getUsername());
        return AuthenticationResult.success(passwordGrantAuthentication.authenticate(loginInfo, accessTokenResponse));
    }

    @Override
    public AuthenticationResult handleAutoLoginRequest(AuthenticationRequest authenticationRequest) throws OXException {
        return AuthenticationResult.failed(LoginExceptionCodes.NOT_SUPPORTED.create(PasswordGrantAuthentication.class.getName()));
    }
}
