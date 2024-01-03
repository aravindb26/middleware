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

package com.openexchange.oidc.impl;

import static com.openexchange.java.Autoboxing.I;
import java.net.URI;
import java.text.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ErrorResponse;
import com.nimbusds.oauth2.sdk.OAuth2Error;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;
import com.openexchange.authentication.Authenticated;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.nimbusds.oauth2.sdk.http.send.HTTPSender;
import com.openexchange.oidc.AuthenticationInfo;
import com.openexchange.oidc.OIDCBackend;
import com.openexchange.oidc.OIDCExceptionCode;
import com.openexchange.oidc.http.outbound.OIDCHttpClientConfig;
import com.openexchange.oidc.osgi.Services;
import com.openexchange.oidc.tools.OIDCTools;
import com.openexchange.oidc.tools.verifier.IDTokenVerifier;
import com.openexchange.oidc.tools.verifier.TokenVerifier;
import com.openexchange.rest.client.httpclient.HttpClientService;
import com.openexchange.session.Session;
import com.openexchange.session.oauth.OAuthTokens;
import com.openexchange.session.oauth.TokenRefreshResponse;
import com.openexchange.session.oauth.TokenRefreshResponse.ErrorType;
import com.openexchange.session.oauth.TokenRefresher;

public class OIDCTokenRefresher implements TokenRefresher {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCTokenRefresher.class);

    private final OIDCBackend backend;
    private final Session session;

    /**
     * Initializes a new {@link OIDCTokenRefresher}.
     *
     * @param backend The ODIC back-end to use
     * @param session The session for which tokens are supposed to be refreshed
     */
    public OIDCTokenRefresher(OIDCBackend backend, Session session) {
        super();
        this.backend = backend;
        this.session = session;
    }

    @Override
    public TokenRefreshResponse execute(OAuthTokens currentTokens) throws OXException {
        if (!currentTokens.hasRefreshToken()) {
            LOG.debug("Cannot refresh OAuth tokens from session '{}' since no refresh token available", session.getSessionID());
            return TokenRefreshResponse.MISSING_REFRESH_TOKEN;
        }

        Object debugInfoForRefreshToken = OAuthTokens.getDebugInfoForRefreshToken(currentTokens);
        LOG.debug("Trying to refresh OAuth tokens from session '{}' using refresh token '{}'", session.getSessionID(), debugInfoForRefreshToken);

        RefreshToken refreshToken = new RefreshToken(currentTokens.getRefreshToken());
        AuthorizationGrant authorizationGrant = new RefreshTokenGrant(refreshToken);

        URI tokenEndpoint = OIDCTools.getURIFromPath(backend.getBackendConfig().getOpTokenEndpoint());
        TokenRequest request = new TokenRequest(tokenEndpoint,
                                                backend.getClientAuthentication(),
                                                authorizationGrant);

        LOG.debug("Sending refresh token request for session '{}' using refresh token '{}'", session.getSessionID(), debugInfoForRefreshToken);
        try {
            HTTPRequest httpRequest = backend.getHttpRequest(request.toHTTPRequest());
            TokenResponse response = parseTokenResponse(HTTPSender.send(httpRequest, () -> {
                HttpClientService httpClientService = Services.getOptionalService(HttpClientService.class);
                if (httpClientService == null) {
                    throw new IllegalStateException("Missing service " + HttpClientService.class.getName());
                }
                return httpClientService.getHttpClient(OIDCHttpClientConfig.getClientIdOidc());
            }));
            return validateResponse(currentTokens, response, debugInfoForRefreshToken);
        } catch (Exception e) {
            // Log with exception in DEBUG
            if (LOG.isDebugEnabled()) {
                LOG.info("Unable to refresh access token for session '{}' of user {} in context {}",
                         session.getSessionID(), I(session.getUserId()), I(session.getContextId()), e);
            } else {
                LOG.info("Unable to refresh access token for session '{}' of user {} in context {}: {} ({})",
                         session.getSessionID(), I(session.getUserId()), I(session.getContextId()), e.getMessage(), e.getClass().getName());
            }
            TokenRefreshResponse.Error error = new TokenRefreshResponse.Error(ErrorType.TEMPORARY, "refresh_failed", e.getMessage());
            return new TokenRefreshResponse(error);
        }
    }

    private TokenRefreshResponse validateResponse(OAuthTokens currentTokens, TokenResponse response, Object debugInfoForRefreshToken) {
        if (response.indicatesSuccess()) {
            if (response instanceof OIDCTokenResponse oidcTokenResponse) {
                // Re-validate ID token if present and different from current one
                String idTokenString = oidcTokenResponse.getOIDCTokens().getIDTokenString();
                if (Strings.isNotEmpty(idTokenString) && false == currentTokens.getIdToken().equals(idTokenString)) {
                    try {
                        JWT updatedIDToken = JWTParser.parse(idTokenString);
                        JWT currentIDToken = JWTParser.parse(currentTokens.getIdToken());
                        verifyIDToken(currentIDToken, updatedIDToken, debugInfoForRefreshToken);
                        verifyUserAndContextClaims(currentIDToken, updatedIDToken);
                    } catch (OXException | ParseException e) {
                        return new TokenRefreshResponse(new TokenRefreshResponse.Error(ErrorType.INVALID_ID_TOKEN, "refresh_failed", e.getMessage()));
                    }
                }
            }
            AccessTokenResponse tokenResponse = (AccessTokenResponse) response;
            OAuthTokens refreshedTokens = OIDCTools.convertNimbusTokens(tokenResponse.getTokens());
            if (LOG.isDebugEnabled()) {
                Object debugInfoForRefreshedToken = OAuthTokens.getDebugInfoForRefreshToken(refreshedTokens);
                LOG.debug("Got successful token response for refresh request for session '{}' using refresh token '{}': '{}'", 
                    session.getSessionID(), debugInfoForRefreshToken, debugInfoForRefreshedToken);
            }
            return new TokenRefreshResponse(refreshedTokens);
        }

        // Error response from OAuth end-point
        ErrorObject error = ((ErrorResponse) response).getErrorObject();
        TokenRefreshResponse.Error rError;
        if (OAuth2Error.INVALID_GRANT.equals(error)) {
            LOG.debug("Got 'invalid_grant' response for refresh request for session '{}' using refresh token '{}': HTTP {}, {}", 
                session.getSessionID(), debugInfoForRefreshToken, I(error.getHTTPStatusCode()), error.toJSONObject());
            rError = new TokenRefreshResponse.Error(ErrorType.INVALID_REFRESH_TOKEN, error.getCode(), error.getDescription());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got token error response for refresh request for session '{}' using refresh token '{}': HTTP {}, {}", 
                    session.getSessionID(), debugInfoForRefreshToken, I(error.getHTTPStatusCode()), error.toJSONObject());
            }
            rError = new TokenRefreshResponse.Error(ErrorType.TEMPORARY, null != error.getCode() ? error.getCode() : "<unknown>", error.getDescription());
        }

        return new TokenRefreshResponse(rError);
    }

    /**
     * Checks if an updated ID token from the received refresh response is valid according to OpenID Connect Core 1.0, section 12.2
     * 
     * @param currentIDToken The original ID token
     * @param updatedIDToken The updated ID token to be verified
     * @param debugInfoForRefreshToken Debug information for refresh token.
     * @throws OXException Signals that the token did not pass the checks
     */
    private void verifyIDToken(JWT currentIDToken, JWT updatedIDToken, Object debugInfoForRefreshToken) throws OXException {
        LOG.debug("Validating ID token received when validating TokenResponse for session '{}' using refresh token '{}'", session.getSessionID(), debugInfoForRefreshToken);
        IDTokenClaimsSet currentClaimsSet;
        try {
            currentClaimsSet = new IDTokenClaimsSet(currentIDToken.getJWTClaimsSet());
        } catch (com.nimbusds.oauth2.sdk.ParseException | ParseException e) {
            throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create(e, "Unable to parse ID token for session {} using refresh token '{}'", session.getSessionID(), debugInfoForRefreshToken);
        }
        IDTokenClaimsSet updatedClaimsSet = backend.validateIdToken(updatedIDToken, null);

        TokenVerifier<IDTokenClaimsSet> verifier = new IDTokenVerifier(currentClaimsSet.getIssuer(),
            currentClaimsSet.getSubject(),
            currentClaimsSet.getIssueTime(),
            currentClaimsSet.getAudience(),
            currentClaimsSet.getAuthenticationTime(),
            currentClaimsSet.getAuthorizedParty());

        verifier.verify(updatedClaimsSet);
    }

    private void verifyUserAndContextClaims(JWT currentIDToken, JWT updatedIDToken) throws OXException {
        //Resolve user and context from stored id token
        Authenticated currentAuthenticated = backend.extractLoginInfo(currentIDToken);

        //Resolve user and context from updated id token
        Authenticated updatedAuthenticated = backend.extractLoginInfo(updatedIDToken);

        //Compare resolved values
        if (!currentAuthenticated.getContextInfo().equals(updatedAuthenticated.getContextInfo()) || !currentAuthenticated.getUserInfo().equals(updatedAuthenticated.getUserInfo())) {
            AuthenticationInfo authenticationInfo = backend.resolveLoginInfo(backend.extractLoginInfo(updatedIDToken));
            if (authenticationInfo.getContextId() != session.getContextId()) {
                throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid context ID");
            }
            if (authenticationInfo.getUserId() != session.getUserId()) {
                throw OIDCExceptionCode.IDTOKEN_VALIDATON_FAILED_CONTENT.create("Invalid user ID");
            }
        }
    }

    /**
     * Parses a token response from the specified HTTP response, yielding either an {@link OIDCTokenResponse} (upon HTTP status code 200),
     * or an {@link TokenErrorResponse}, otherwise.
     *
     * @param httpResponse The HTTP response. Must not be {@code null}.
     * @return The OIDC access token or token error response.
     * @throws com.nimbusds.oauth2.sdk.ParseException If the HTTP response couldn't be parsed to a token response.
     */
    private static TokenResponse parseTokenResponse(HTTPResponse httpResponse) throws com.nimbusds.oauth2.sdk.ParseException {
        if (HTTPResponse.SC_OK == httpResponse.getStatusCode()) {
            return OIDCTokenResponse.parse(httpResponse);
        }
        return TokenErrorResponse.parse(httpResponse);
    }

}
