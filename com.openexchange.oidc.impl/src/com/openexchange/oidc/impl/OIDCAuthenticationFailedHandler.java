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

import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.exception.OXException;
import com.openexchange.mail.api.AuthType;
import com.openexchange.mail.api.AuthenticationFailedHandler;
import com.openexchange.mail.api.AuthenticationFailureHandlerResult;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.oidc.OIDCBackend;
import com.openexchange.oidc.osgi.OIDCBackendRegistry;
import com.openexchange.oidc.state.StateManagement;
import com.openexchange.oidc.tools.OIDCTools;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.session.oauth.RefreshResult;
import com.openexchange.session.oauth.SessionOAuthTokenService;
import com.openexchange.session.oauth.TokenRefreshConfig;
import com.openexchange.session.oauth.RefreshResult.SuccessReason;


/**
 * {@link OIDCAuthenticationFailedHandler}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.3
 */
public class OIDCAuthenticationFailedHandler extends AbstractOIDCTokenRefreshTriggerer<Optional<AuthenticationFailureHandlerResult>> implements AuthenticationFailedHandler {

    private static final Logger LOG = LoggerFactory.getLogger(OIDCAuthenticationFailedHandler.class);

    /**
     * Boolean property <code>"com.openexchange.oidc.mail.immediateTokenRefreshOnFailedAuth"</code>. Default is <code>false</code>.
     * <p>
     * Controls whether immediate refresh of OIDC OAuth tokens on failed authentication against mail/transport service is enabled or not.
     */
    private static final Property IMMEDIATE_TOKEN_REFRESH_ON_FAILED_AUTH = new Property() {

        @Override
        public String getFQPropertyName() {
            return "com.openexchange.oidc.mail.immediateTokenRefreshOnFailedAuth";
        }

        @Override
        public Object getDefaultValue() {
            return Boolean.FALSE;
        }
    };

    /** The service look-up */
    private final ServiceLookup services;

    /**
     * Initializes a new {@link OIDCAuthenticationFailedHandler}.
     *
     * @param oidcBackendRegistry The OIDC back-end registry
     * @param tokenService The token service
     * @param stateManagement The state management
     * @param services The service look-up
     */
    public OIDCAuthenticationFailedHandler(OIDCBackendRegistry oidcBackendRegistry, SessionOAuthTokenService tokenService, StateManagement stateManagement, ServiceLookup services) {
        super(oidcBackendRegistry, tokenService, stateManagement);
        this.services = services;
    }

    @Override
    protected TokenRefreshConfig getTokenRefreshConfig(OIDCBackend backend) {
        TokenRefreshConfig tokenRefreshConfig = super.getTokenRefreshConfig(backend);
        // Enforce refresh
        return TokenRefreshConfig.newBuilder(tokenRefreshConfig).setForcedRefresh(true).build();
    }

    @Override
    public AuthenticationFailureHandlerResult handleAuthenticationFailed(OXException failedAuthentication, Service service, MailConfig mailConfig, Session session, Map<String, Object> state) throws OXException {
        if (!AuthType.isOAuthType(mailConfig.getAuthType())) {
            LOG.debug("Skipping non-oauth session: {}", session.getSessionID());
            return AuthenticationFailureHandlerResult.createContinueResult();
        }

        if (!session.containsParameter(OIDCTools.IDTOKEN)) {
            LOG.debug("Skipping unmanaged session: {}", session.getSessionID());
            return AuthenticationFailureHandlerResult.createContinueResult();
        }

        {
            LeanConfigurationService configService = services.getOptionalService(LeanConfigurationService.class);
            boolean immediateTokenRefreshOnFailedAuth = configService == null ? IMMEDIATE_TOKEN_REFRESH_ON_FAILED_AUTH.getDefaultValue(Boolean.class).booleanValue() : configService.getBooleanProperty(session.getUserId(), session.getContextId(), IMMEDIATE_TOKEN_REFRESH_ON_FAILED_AUTH);
            if (immediateTokenRefreshOnFailedAuth == false) {
                LOG.debug("Skipping refresh of OIDC OAuth token(s) for session: {}. Immediate token refresh is not enabled via property \"{}\"", session.getSessionID(), IMMEDIATE_TOKEN_REFRESH_ON_FAILED_AUTH.getFQPropertyName());
                // re-throw; let SessionInspector handle eager refresh alone
                return AuthenticationFailureHandlerResult.createContinueResult();
            }
        }

        if (isInvalidTokenError(failedAuthentication) == false) {
            LOG.debug("Failed authentication for session '{}' seems NOT to be caused by invalid/expired OAuth tokens: {}", session.getSessionID(), failedAuthentication.getMessage(), failedAuthentication);
            return AuthenticationFailureHandlerResult.createContinueResult();
        }

        Optional<OIDCBackend> optBackend = this.loadBackendForSession(session);
        if (optBackend.isEmpty()) {
            LOG.warn("Unable to load OIDC backend for session '{}' due to missing path parameter", session.getSessionID());
            return AuthenticationFailureHandlerResult.createErrorResult(failedAuthentication);
        }

        LOG.debug("Appears to be a failed authentication for session '{}' due to invalid OAuth tokens: {}", session.getSessionID(), failedAuthentication.getMessage(), failedAuthentication);
        try {
            Optional<AuthenticationFailureHandlerResult> optFailureResult = triggerCheckOrRefreshTokens(session, optBackend.get());
            return optFailureResult.isPresent() ? optFailureResult.get() : AuthenticationFailureHandlerResult.createErrorResult(failedAuthentication);
        } catch (OXException e) {
            LOG.error("Error while checking OAuth tokens for session '{}'", session.getSessionID(), e);
            return AuthenticationFailureHandlerResult.createErrorResult(failedAuthentication);
        } catch (InterruptedException e) {
            LOG.warn("Thread was interrupted while checking session OAuth tokens");
            // keep interrupted state
            Thread.currentThread().interrupt();
            return AuthenticationFailureHandlerResult.createErrorResult(failedAuthentication);
        }
    }

    private boolean isInvalidTokenError(OXException failedAuthentication) {
        Throwable cause = failedAuthentication.getCause();
        if (cause instanceof com.sun.mail.iap.ProtocolException) {
            com.sun.mail.iap.ProtocolException pe = (com.sun.mail.iap.ProtocolException) cause;
            com.sun.mail.iap.ResponseCode rc = pe.getKnownResponseCode();
            if (com.sun.mail.iap.ResponseCode.UNAVAILABLE.equals(rc)) {
                LOG.debug("Considering failed authentication as being caused by invalid/expired OAuth tokens: {}", pe.getMessage(), pe);
                return true;
            }
        }
        return false;
    }

    @Override
    protected Optional<AuthenticationFailureHandlerResult> handleSuccessResult(Session session, OIDCBackend backend, RefreshResult result) {
        if (SuccessReason.REFRESHED.equals(result.getSuccessReason()) || SuccessReason.CONCURRENT_REFRESH.equals(result.getSuccessReason())) {
            LOG.debug("Returning \"retry\" failure result for session '{}' due to successful token refresh result: {}", session.getSessionID(), result.getSuccessReason().name());
            return Optional.of(AuthenticationFailureHandlerResult.createRetryResult());
        }
        LOG.debug("Returning \"error\" failure result for session '{}': {}", session.getSessionID(), result.getSuccessReason().name());
        return Optional.empty();
    }

    @Override
    protected Optional<AuthenticationFailureHandlerResult> handleErrorResult(Session session, OIDCBackend backend, RefreshResult result, boolean sessionRemoved) {
        // Maybe different handling here?
        return Optional.empty();
    }

}
