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

package com.openexchange.session.oauth.impl;

import static com.openexchange.java.Autoboxing.L;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.session.oauth.OAuthTokens;
import com.openexchange.session.oauth.RefreshResult;
import com.openexchange.session.oauth.RefreshResult.FailReason;
import com.openexchange.session.oauth.RefreshResult.SuccessReason;
import com.openexchange.session.oauth.TokenRefreshConfig;
import com.openexchange.session.oauth.TokenRefreshResponse;
import com.openexchange.session.oauth.TokenRefreshResponse.Error;
import com.openexchange.session.oauth.TokenRefresher;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessionstorage.SessionStorageExceptionCodes;
import com.openexchange.sessionstorage.SessionStorageService;

/**
 * {@link OAuthTokenUpdaterImpl}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.3
 */
public class OAuthTokenUpdaterImpl {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthTokenUpdaterImpl.class);

    private static final long REMOTE_SESSION_LOOKUP_TIMEOUT_MILLIS = 5000L;

    private final Session session;
    private final TokenRefresher refresher;
    private final TokenRefreshConfig refreshConfig;
    private final ServiceLookup services;
    private final OAuthTokensGetterSetter tokenGetterSetter;

    /**
     * Initializes a new {@link OAuthTokenUpdaterImpl}.
     *
     * @param session The session for which to check OAuth tokens
     * @param refresher The refresher to use
     * @param refreshConfig The refresh configuration
     * @param tokenGetterSetter The getter/setter for tokens
     * @param services The tracked OSGi services
     */
    public OAuthTokenUpdaterImpl(Session session, TokenRefresher refresher, TokenRefreshConfig refreshConfig, OAuthTokensGetterSetter tokenGetterSetter, ServiceLookup services) {
        super();
        this.session = session;
        this.refresher = refresher;
        this.refreshConfig = refreshConfig;
        this.tokenGetterSetter = tokenGetterSetter;
        this.services = services;

    }

    /**
     * Checks (and respectively refreshes) tokens associated with this updater's session.
     *
     * @return The refresh result
     * @throws InterruptedException If operation gets interrupted
     * @throws OXException If an error occurs while trying to refresh access token
     */
    public RefreshResult checkAndRefreshTokens() throws InterruptedException, OXException {
        LOG.debug("Checking whether to refresh OAuth tokens from session '{}'", session.getSessionID());
        long lockTimeoutMillis = refreshConfig.getLockTimeoutMillis();
        long start = System.currentTimeMillis();
        OAuthTokens tokens;
        try {
            Optional<OAuthTokens> optTokens = tokenGetterSetter.getFromSessionAtomic(session, lockTimeoutMillis, TimeUnit.MILLISECONDS);
            if (!optTokens.isPresent()) {
                LOG.debug("Missing OAuth tokens in session '{}'", session.getSessionID());
                return RefreshResult.fail(FailReason.PERMANENT_ERROR, "Missing OAuth tokens in session " + session.getSessionID());
            }

            tokens = optTokens.get();
        } catch (TimeoutException e) {
            return RefreshResult.fail(FailReason.LOCK_TIMEOUT, "Lock timeout for expired session OAuth token exceeded while obtaining tokens from session " + session.getSessionID(), e);
        }

        if (refreshConfig.isForcedRefresh() || tokens.accessExpiresWithin(refreshConfig.getRefreshThresholdMillis(), TimeUnit.MILLISECONDS)) {
            LOG.debug("Need to refresh OAuth tokens from session '{}' with reason: {}", session.getSessionID(), refreshConfig.isForcedRefresh() ? "enforced refresh" : "tokens expire within");
            long lockMillisLeft = lockTimeoutMillis - (System.currentTimeMillis() - start);
            if (lockMillisLeft <= 0) {
                LOG.debug("Could not refresh OAuth tokens from session '{}' since lock timeout is exceeded", session.getSessionID());
                return RefreshResult.fail(FailReason.LOCK_TIMEOUT, "Lock timeout for expired session OAuth token exceeded");
            }

            LOG.debug("Trying to refresh OAuth tokens from session '{}'...", session.getSessionID());
            return refreshTokens(tokens, lockMillisLeft, TimeUnit.MILLISECONDS);
        }

        LOG.debug("No need to refresh OAuth tokens from session '{}' since not expired", session.getSessionID());
        return RefreshResult.success(SuccessReason.NON_EXPIRED);
    }

    private RefreshResult refreshTokens(OAuthTokens oldTokens, long lockTimeout, TimeUnit lockUnit) throws InterruptedException, OXException {
        try {
            return tokenGetterSetter.doThrowableAtomic(session, lockTimeout, lockUnit, () -> {
                LOG.debug("Refreshing OAuth tokens for session '{}'...", session.getSessionID());
                Optional<OAuthTokens> optTokens = tokenGetterSetter.getFromSession(session);
                if (!optTokens.isPresent()) {
                    LOG.debug("Refreshing OAuth tokens failed. Missing OAuth tokens in session '{}'", session.getSessionID());
                    return RefreshResult.fail(FailReason.PERMANENT_ERROR, "Missing OAuth tokens in session " + session.getSessionID());
                }

                OAuthTokens tokens = optTokens.get();
                if (!oldTokens.getAccessToken().equals(tokens.getAccessToken())) {
                    LOG.debug("Refreshing OAuth tokens failed. OAuth tokens from session '{}' were already refreshed by another thread", session.getSessionID());
                    return RefreshResult.success(SuccessReason.CONCURRENT_REFRESH);
                }

                LOG.debug("Now refreshing OAuth tokens for session '{}'...", session.getSessionID());
                TokenRefreshResponse response = refresher.execute(tokens);
                if (response.isSuccess()) {
                    LOG.debug("Succeeded refreshing OAuth tokens from session '{}': {}", session.getSessionID(), response.getTokens());
                    return handleSuccess(response.getTokens());
                }

                LOG.debug("Refreshing OAuth tokens failed. Failed refreshing OAuth tokens from session '{}': {}", session.getSessionID(), response.getError());
                return handleError(oldTokens, response.getError());
            });
        } catch (TimeoutException e) {
            return RefreshResult.fail(FailReason.LOCK_TIMEOUT, "Lock timeout for expired session OAuth token exceeded while trying to refresh tokes for session " + session.getSessionID(), e);
        }
    }

    private RefreshResult handleSuccess(OAuthTokens tokens) {
        if (tokens.accessExpiresWithin(refreshConfig.getRefreshThreshold(), refreshConfig.getRefreshThresholdUnit())) {
            // Some IDMs assign a max. lifetime to refresh tokens token, too. Often aligned with
            // SSO session duration. As a result, fresh access tokens might have a shorter validity
            // period than the previous ones (expiry == max. refresh token lifetime).
            // In case the expiration time becomes lower than the configured refresh threshold,
            // it doesn't make sense to use the new token pair at all. Any subsequent request
            // would immediately try to refresh it again.
            LOG.info("Discarding refreshed OAuth tokens for session '{}'. Expiration is lower than configured refresh threshold: {}sec / {}sec",
                     session.getSessionID(),
                     L(TimeUnit.MILLISECONDS.toSeconds(tokens.getExpiresInMillis())),
                     L(refreshConfig.getRefreshThresholdUnit().toSeconds(refreshConfig.getRefreshThreshold())));
            return RefreshResult.fail(FailReason.PERMANENT_ERROR, "Expiration date of new tokens is lower than refresh threshold");
        }

        SessiondService sessiondService = services.getOptionalService(SessiondService.class);
        if (null == sessiondService) {
            /*
             * can only set new tokens in this session reference
             */
            LOG.warn("Storing OAuth tokens in stored session '{}' failed. SessionD service unavailable.", session.getSessionID());
            tokenGetterSetter.setInSession(session, tokens);
            return RefreshResult.success(SuccessReason.REFRESHED);
        }

        if (false == sessiondService.isCentral()) {
            /*
             * set in session and try to store in distributed session storage
             */
            tokenGetterSetter.setInSession(session, tokens);
            try {
                if (sessiondService.storeSession(session.getSessionID(), false)) {
                    LOG.info("Successfully stored updated OAuth tokens in stored session '{}'", session.getSessionID());
                } else {
                    LOG.warn("Could not store updated OAuth tokens in stored session '{}'", session.getSessionID());
                }
            } catch (Exception e) {
                LOG.warn("Storing OAuth tokens in stored session '{}' failed", session.getSessionID(), e);
            }
            return RefreshResult.success(SuccessReason.REFRESHED);
        }

        /*
         * set in session, but revert back to original tokens if not taken over in central storage
         */
        Optional<OAuthTokens> originalTokens = tokenGetterSetter.getFromSession(session);
        tokenGetterSetter.setInSession(session, tokens);
        try {
            if (sessiondService.storeSession(session, false)) {
                LOG.info("Successfully stored updated OAuth tokens in stored session '{}'", session.getSessionID());
                originalTokens = Optional.empty(); // don't restore in finally clause
                return RefreshResult.success(SuccessReason.REFRESHED);
            }
            LOG.warn("Could not store updated OAuth tokens in stored session '{}'", session.getSessionID());
            return RefreshResult.fail(FailReason.TEMPORARY_ERROR, "Tokens not stored");
        } catch (Exception e) {
            LOG.warn("Storing OAuth tokens in stored session '{}' failed", session.getSessionID(), e);
            return RefreshResult.fail(FailReason.TEMPORARY_ERROR, "Tokens not stored", e);
        } finally {
            if (originalTokens.isPresent() && false == originalTokens.get().isAccessExpired()) {
                LOG.info("Restoring original tokens in session '{}' to avoid inconsistencies", session.getSessionID());
                tokenGetterSetter.setInSession(session, originalTokens.get());
            }
        }
    }

    private RefreshResult handleError(OAuthTokens oldTokens, Error error) {
        switch (error.getType()) {
            case INVALID_REFRESH_TOKEN:
                LOG.info("OAuth token refresh failed due to invalid refresh token for session '{}'", session.getSessionID());
                return handleInvalidRefreshToken(oldTokens);
            case INVALID_ID_TOKEN:
                LOG.info("OAuth token refresh failed due to invalid ID token for session '{}'", session.getSessionID());
                return RefreshResult.fail(FailReason.PERMANENT_ERROR, error.getDescription());
            case TEMPORARY:
                if (error.hasException()) {
                    OXException exception = error.getException();
                    LOG.warn("A temporary error occurred while trying to refresh tokens for session '{}'", session.getSessionID(), exception);
                    return RefreshResult.fail(FailReason.TEMPORARY_ERROR, error.getDescription(), exception);
                }
                LOG.warn("A temporary error occurred while trying to refresh tokens for session '{}'", session.getSessionID());
                return RefreshResult.fail(FailReason.TEMPORARY_ERROR, error.getDescription());
            case PERMANENT:
                if (error.hasException()) {
                    OXException exception = error.getException();
                    LOG.warn("A permanent error occurred while trying to refresh tokens for session '{}'", session.getSessionID(), exception);
                    return RefreshResult.fail(FailReason.PERMANENT_ERROR, error.getDescription(), exception);
                }
                LOG.warn("A permanent error occurred while trying to refresh tokens for session '{}'", session.getSessionID());
                return RefreshResult.fail(FailReason.PERMANENT_ERROR, error.getDescription());
            default:
                throw new IllegalStateException("Unknown error type: " + error.getType().name());
        }
    }

    private RefreshResult handleInvalidRefreshToken(OAuthTokens oldTokens) {
        if (!refreshConfig.isTryRecoverStoredTokens()) {
            tokenGetterSetter.removeFromSession(session);
            return invalidRefreshToken();
        }

        SessionStorageService sessionStorageService = services.getService(SessionStorageService.class);
        if (sessionStorageService != null) {
            try {
                LOG.debug("Trying to find newer tokens in stored session '{}'", session.getSessionID());
                Session remoteSession = sessionStorageService.lookupSession(session.getSessionID(), REMOTE_SESSION_LOOKUP_TIMEOUT_MILLIS);
                Optional<OAuthTokens> optRemoteTokens = tokenGetterSetter.getFromSession(remoteSession);
                if (!optRemoteTokens.isPresent()) {
                    LOG.warn("Stored session '{}' contains no tokens", session.getSessionID());
                    tokenGetterSetter.removeFromSession(session);
                    return invalidRefreshToken();
                }

                OAuthTokens remoteTokens = optRemoteTokens.get();
                boolean tokensDiffer = !oldTokens.getAccessToken().equals(remoteTokens.getAccessToken());
                if (tokensDiffer && !remoteTokens.isAccessExpired()) {
                    // success; seems like tokens have been refreshed on another node meanwhile
                    LOG.info("Taking over tokens from stored session '{}'", session.getSessionID());
                    tokenGetterSetter.setInSession(session, remoteTokens);
                    return RefreshResult.success(SuccessReason.CONCURRENT_REFRESH);
                }
                LOG.debug("Stored session '{}' contains no other valid tokens", session.getSessionID());
            } catch (OXException e) {
                if (SessionStorageExceptionCodes.NO_SESSION_FOUND.equals(e)) {
                    LOG.warn("No stored session found for ID '{}'", session.getSessionID());
                } else {
                    LOG.error("Error while looking up remote session '{}'", session.getSessionID(), e);
                }
            }
        }

        tokenGetterSetter.removeFromSession(session);
        return invalidRefreshToken();
    }

    private static RefreshResult invalidRefreshToken() {
        return RefreshResult.fail(FailReason.INVALID_REFRESH_TOKEN, "Invalid refresh token");
    }

}
