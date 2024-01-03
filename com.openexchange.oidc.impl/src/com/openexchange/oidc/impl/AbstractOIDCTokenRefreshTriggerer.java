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

import java.util.Optional;
import com.openexchange.exception.OXException;
import com.openexchange.oidc.OIDCBackend;
import com.openexchange.oidc.OIDCBackendConfig;
import com.openexchange.oidc.OIDCExceptionCode;
import com.openexchange.oidc.osgi.OIDCBackendRegistry;
import com.openexchange.oidc.osgi.Services;
import com.openexchange.oidc.state.StateManagement;
import com.openexchange.oidc.tools.OIDCTools;
import com.openexchange.session.Session;
import com.openexchange.session.oauth.OAuthTokens;
import com.openexchange.session.oauth.RefreshResult;
import com.openexchange.session.oauth.RefreshResult.FailReason;
import com.openexchange.session.oauth.RefreshResult.SuccessReason;
import com.openexchange.session.oauth.SessionOAuthTokenService;
import com.openexchange.session.oauth.TokenRefreshConfig;
import com.openexchange.session.oauth.TokenRefresher;
import com.openexchange.sessiond.SessiondService;

/**
 * {@link AbstractOIDCTokenRefreshTriggerer} - Basic class for triggering OIDC OAuth tokens.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 * @param <R> The return type when handling successful/failed token refresh attempt
 */
public abstract class AbstractOIDCTokenRefreshTriggerer<R> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AbstractOIDCTokenRefreshTriggerer.class);

    /** The OIDC back-end registry */
    protected final OIDCBackendRegistry oidcBackends;

    /** The OAuth token service */
    protected final SessionOAuthTokenService tokenService;

    /** The OIDC state management */
    protected final StateManagement stateManagement;

    /**
     * Initializes a new {@link AbstractOIDCTokenRefreshTriggerer}.
     *
     * @param oidcBackendRegistry The OIDC back-end registry
     * @param tokenService The token service
     * @param stateManagement The state management
     */
    protected AbstractOIDCTokenRefreshTriggerer(OIDCBackendRegistry oidcBackendRegistry, SessionOAuthTokenService tokenService, StateManagement stateManagement) {
        super();
        this.oidcBackends = oidcBackendRegistry;
        this.tokenService = tokenService;
        this.stateManagement = stateManagement;
    }

    /**
     * Triggers to check and refresh the OIDC OAuth tokens for given session.
     *
     * @param session The session
     * @param backend The OIDC back-end for which OAuth tokens shall be checked/refreshed
     * @throws InterruptedException If thread was interrupted while blocking
     * @throws OXException Any unexpected error re-thrown from {@link TokenRefresher#execute(OAuthTokens)}
     */
    protected R triggerCheckOrRefreshTokens(Session session, OIDCBackend backend) throws InterruptedException, OXException {
        RefreshResult result = performCheckOrRefreshTokens(session, backend);
        if (result.isSuccess()) {
            // Successful token refresh
            if (SuccessReason.REFRESHED.equals(result.getSuccessReason()) && (backend.getBackendConfig().isFrontchannelLogoutEnabled() || backend.getBackendConfig().isBackchannelLogoutEnabled())) {
                /*
                 * keep remembering oidc -> ox session id mapping in state
                 */
                OIDCTools.rememberSessionIds(stateManagement, session, (String) session.getParameter(OIDCTools.IDTOKEN));
            }
            return handleSuccessResult(session, backend, result);
        }

        // Failed token refresh
        boolean sessionRemoved = false;
        RefreshResult.FailReason failReason = result.getFailReason();
        if (failReason == FailReason.INVALID_REFRESH_TOKEN || failReason == FailReason.PERMANENT_ERROR) {
            if (result.hasException()) {
                LOG.info("Terminating session '{}' due to OAuth token refresh error: {} ({})", session.getSessionID(), failReason.name(), result.getErrorDesc(), result.getException());
            } else {
                LOG.info("Terminating session '{}' due to OAuth token refresh error: {} ({})", session.getSessionID(), failReason.name(), result.getErrorDesc());
            }
            SessiondService sessiondService = Services.getService(SessiondService.class);
            sessiondService.removeSession(session.getSessionID());
            sessionRemoved = true;
        }
        if (result.hasException()) {
            LOG.warn("Error while refreshing OAuth tokens for session '{}': {}", session.getSessionID(), result.getErrorDesc(), result.getException());
        } else {
            LOG.warn("Error while refreshing OAuth tokens for session '{}': {}", session.getSessionID(), result.getErrorDesc());
        }
        return handleErrorResult(session, backend, result, sessionRemoved);
    }

    /**
     * Performs the token refresh.
     *
     * @param session The session
     * @param backend The OIDC back-end for which OAuth tokens shall be checked/refreshed
     * @return The refresh result
     * @throws InterruptedException If thread was interrupted while blocking
     * @throws OXException Any unexpected error re-thrown from {@link TokenRefresher#execute(OAuthTokens)}
     */
    protected RefreshResult performCheckOrRefreshTokens(Session session, OIDCBackend backend) throws InterruptedException, OXException {
        TokenRefreshConfig refreshConfig = getTokenRefreshConfig(backend);
        OIDCTokenRefresher refresher = createTokenRefresher(session, backend);
        LOG.debug("Going to check (or optionally refresh) OIDC OAuth tokens for session '{}' with config: {}", session.getSessionID(), refreshConfig);
        RefreshResult result = tokenService.checkOrRefreshTokens(session, refresher, refreshConfig);
        LOG.debug("Checked/refreshed OIDC OAuth tokens for session '{}' with result: {}", session.getSessionID(), result);
        return result;
    }

    /**
     * Invoked on successful token refresh result.
     *
     * @param session The session
     * @param backend The OIDC back-end
     * @param result The refresh result
     * @return The result for handling successful token refresh
     * @throws OXException If handling fails or an error should be advertised
     */
    protected abstract R handleSuccessResult(Session session, OIDCBackend backend, RefreshResult result) throws OXException;

    /**
     * Invoked on failed token refresh result.
     *
     * @param session The session
     * @param backend The OIDC back-end
     * @param result The refresh result
     * @param sessionRemoved <code>true</code> if associated session has been removed; otherwise <code>false</code>
     * @return The result for handling The result for handling successful token refresh token refresh
     * @throws OXException If handling fails or an error should be advertised
     */
    protected abstract R handleErrorResult(Session session, OIDCBackend backend, RefreshResult result, boolean sessionRemoved) throws OXException;

    /**
     * Creates the refresher for OIDC OAuth tokens.
     * <p>
     * This method is invoked by {@link #performCheckOrRefreshTokens(Session, OIDCBackend)} and may be overridden to customize token
     * refresher.
     *
     * @param session The session
     * @param backend The OIDC back-end
     * @return The token refreshed
     */
    protected OIDCTokenRefresher createTokenRefresher(Session session, OIDCBackend backend) {
        return new OIDCTokenRefresher(backend, session);
    }

    /**
     * Gets the appropriate token refresh configuration.
     * <p>
     * This method is invoked by {@link #performCheckOrRefreshTokens(Session, OIDCBackend)} and may be overridden to customize token refresh
     * configuration.
     *
     * @param backend The OIDC back-end to yield the token refresh configuration for
     * @return The token refresh configuration
     */
    protected TokenRefreshConfig getTokenRefreshConfig(OIDCBackend backend) {
        OIDCBackendConfig config = backend.getBackendConfig();
        return OIDCTools.getTokenRefreshConfig(config);
    }

    /**
     * Loads the appropriate OIDC back-end for given session.
     *
     * @param session The session
     * @return The OIDC back-end
     * @throws OXException If OIDC back-end cannot be loaded
     */
    protected Optional<OIDCBackend> loadBackendForSession(Session session) throws OXException{
        String backendPath = (String) session.getParameter(OIDCTools.BACKEND_PATH);
        if (null == backendPath) {
            return Optional.empty();
        }

        for (OIDCBackend backend : this.oidcBackends.getAllRegisteredBackends()) {
            if (backend.getPath().equals(backendPath)) {
                return Optional.of(backend);
            }
        }
        throw OIDCExceptionCode.UNABLE_TO_FIND_BACKEND_FOR_SESSION.create(backendPath);
    }

}
