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

import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.lock.AccessControl;
import com.openexchange.lock.AccessControls;
import com.openexchange.lock.LockService;
import com.openexchange.lock.ReentrantLockAccessControl;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.session.oauth.OAuthTokens;

/**
 * {@link OAuthTokensGetterSetter}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.10.3
 */
public class OAuthTokensGetterSetter {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthTokensGetterSetter.class);

    private final ServiceLookup services;

    public OAuthTokensGetterSetter(ServiceLookup services) {
        super();
        this.services = services;
    }

    public Optional<OAuthTokens> getFromSessionAtomic(Session session) throws InterruptedException {
        return getFrom(doAtomic(session, () -> {
            LOG.debug("Getting OAuth tokens from session {}...", session.getSessionID());
            OAuthSessionParams sessionParams = new OAuthSessionParams(session);
            LOG.debug("Got OAuth tokens from session {}: {}", session.getSessionID(), sessionParams);
            return sessionParams;
        }), session);
    }

    public Optional<OAuthTokens> getFromSessionAtomic(Session session, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        return getFrom(doAtomic(session, timeout, unit, (AtomicResultOperation<OAuthSessionParams>) () -> {
            LOG.debug("Getting OAuth tokens from session {}...", session.getSessionID());
            OAuthSessionParams sessionParams = new OAuthSessionParams(session);
            LOG.debug("Got OAuth tokens from session {}: {}", session.getSessionID(), sessionParams);
            return sessionParams;
        }), session);
    }

    public void setInSessionAtomic(Session session, OAuthTokens tokens) throws InterruptedException {
        doAtomic(session, () -> setInSession(session, tokens));
    }

    public void setInSessionAtomic(Session session, OAuthTokens tokens, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        doAtomic(session, timeout, unit, () -> setInSession(session, tokens));
    }

    public void removeFromSessionAtomic(Session session) throws InterruptedException {
        doAtomic(session, () -> removeFromSession(session));
    }

    public void removeFromSessionAtomic(Session session, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        doAtomic(session, timeout, unit, () -> removeFromSession(session));
    }

    public Optional<OAuthTokens> getFromSession(Session session) {
        return getFrom(new OAuthSessionParams(session), session);
    }

    private static Optional<OAuthTokens> getFrom(OAuthSessionParams sessionParams, Session session) {
        String accessToken = sessionParams.getAccessToken();

        if (accessToken == null) {
            LOG.info("Missing OAuth access token in session {}", session.getSessionID());
            return Optional.empty();
        }

        Date expiryDate = null;
        String expiryString = sessionParams.getExpiryString();
        if (expiryString != null) {
            try {
                expiryDate = new Date(Long.parseLong(expiryString));
            } catch (@SuppressWarnings("unused") NumberFormatException e) {
                LOG.warn("Invalid expiry date string for OAuth access token in session {}: {}", session.getSessionID(), expiryString);
            }
        }

        String refreshToken = sessionParams.getRefreshToken();
        String idToken = sessionParams.getIDToken();
        OAuthTokens tokens = new OAuthTokens(accessToken, expiryDate, refreshToken, idToken);
        LOG.debug("Got OAuth tokens from session {}: {}", session.getSessionID(), tokens);
        return Optional.of(tokens);
    }

    public void setInSession(Session session, OAuthTokens tokens) {
        LOG.debug("Setting OAuth tokens in session {}...", session.getSessionID());
        session.setParameter(Session.PARAM_OAUTH_ACCESS_TOKEN, tokens.getAccessToken());
        if (tokens.hasExpiryDate()) {
            session.setParameter(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE, Long.toString(tokens.getExpiryDate().getTime()));
        }
        if (tokens.hasRefreshToken()) {
            session.setParameter(Session.PARAM_OAUTH_REFRESH_TOKEN, tokens.getRefreshToken());
        }
        if (tokens.hasIdToken()) {
            session.setParameter(Session.PARAM_OAUTH_ID_TOKEN, tokens.getIdToken());
        }
        LOG.debug("Set OAuth tokens in session {}: {}", session.getSessionID(), tokens);
    }

    public void removeFromSession(Session session) {
        LOG.debug("Removing OAuth tokens from session {}...", session.getSessionID());
        session.setParameter(Session.PARAM_OAUTH_ACCESS_TOKEN, null);
        session.setParameter(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE, null);
        session.setParameter(Session.PARAM_OAUTH_REFRESH_TOKEN, null);
        session.setParameter(Session.PARAM_OAUTH_ID_TOKEN, null);
        LOG.debug("Removed OAuth tokens from session {}", session.getSessionID());
    }

    private AccessControl getTokenLock(Session session) {
        LockService lockService = services.getService(LockService.class);
        if (null == lockService) {
            LOG.warn("Failed to acquire lock for session {}. Using global lock instead. LockService is absent.", session.getSessionID());
            return new ReentrantLockAccessControl((ReentrantLock) session.getParameter(Session.PARAM_LOCK));
        }

        try {
            StringBuilder identifier = new StringBuilder(48).append("oauth-tokens-").append(session.getSessionID());
            return lockService.getAccessControlFor(identifier.toString(), 1, session.getUserId(), session.getContextId());
        } catch (Exception e) {
            LOG.warn("Failed to acquire lock for session {}. Using global lock instead.", session.getSessionID(), e);
            return new ReentrantLockAccessControl((ReentrantLock) session.getParameter(Session.PARAM_LOCK));
        }
    }

    private static interface AtomicOperation {
        void perform() throws InterruptedException;
    }

    private static interface ThrowableAtomicOperation {
        void perform() throws InterruptedException, OXException;
    }

    private static interface AtomicResultOperation<R> {
        R perform() throws InterruptedException;
    }

    public static interface ThrowableAtomicResultOperation<R> {
        R perform() throws InterruptedException, OXException;
    }

    private void doAtomic(Session session, AtomicOperation op) throws InterruptedException {
        boolean locked = false;
        AccessControl accessControl = getTokenLock(session);
        try {
            accessControl.acquireGrant();
            locked = true;
            op.perform();
        } finally {
            AccessControls.release(accessControl, locked);
        }
    }

    private void doAtomic(Session session, long timeout, TimeUnit unit, AtomicOperation op) throws TimeoutException, InterruptedException {
        boolean acquired = false;
        AccessControl accessControl = getTokenLock(session);
        try {
            acquired = accessControl.tryAcquireGrant(timeout, unit);
            if (!acquired) {
                // Not acquired in time
                throw createTimeoutException();
            }
            op.perform();
        } finally {
            AccessControls.release(accessControl, acquired);
        }
    }

    public <R> R doThrowableAtomic(Session session, long timeout, TimeUnit unit, ThrowableAtomicResultOperation<R> op) throws TimeoutException, InterruptedException, OXException {
        boolean acquired = false;
        AccessControl accessControl = getTokenLock(session);
        try {
            acquired = accessControl.tryAcquireGrant(timeout, unit);
            if (!acquired) {
                // Not acquired in time
                throw createTimeoutException();
            }
            return op.perform();
        } finally {
            AccessControls.release(accessControl, acquired);
        }
    }

    private <R> R doAtomic(Session session, AtomicResultOperation<R> op) throws InterruptedException {
        boolean acquired = false;
        AccessControl accessControl = getTokenLock(session);
        try {
            accessControl.acquireGrant();
            acquired = true;
            return op.perform();
        } finally {
            AccessControls.release(accessControl, acquired);
        }
    }

    private <R> R doAtomic(Session session, long timeout, TimeUnit unit, AtomicResultOperation<R> op) throws TimeoutException, InterruptedException {
        boolean acquired = false;
        AccessControl accessControl = getTokenLock(session);
        try {
            acquired = accessControl.tryAcquireGrant(timeout, unit);
            if (!acquired) {
                // Not acquired in time
                throw createTimeoutException();
            }
            return op.perform();
        } finally {
            AccessControls.release(accessControl, acquired);
        }
    }

    private <R> R doAtomic(Session session, long timeout, TimeUnit unit, ThrowableAtomicResultOperation<R> op) throws TimeoutException, InterruptedException, OXException {
        boolean acquired = false;
        AccessControl accessControl = getTokenLock(session);
        try {
            acquired = accessControl.tryAcquireGrant(timeout, unit);
            if (!acquired) {
                // Not acquired in time
                throw createTimeoutException();
            }
            return op.perform();
        } finally {
            AccessControls.release(accessControl, acquired);
        }
    }

    private static TimeoutException createTimeoutException() {
        return new TimeoutException("Lock timeout exceeded");
    }

}
