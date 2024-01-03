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

package com.openexchange.sessiond.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.session.Session;
import com.openexchange.session.SessionAttributes;
import com.openexchange.sessiond.AddSessionParameter;
import com.openexchange.sessiond.SessionFilter;
import com.openexchange.sessiond.SessionMatcher;
import com.openexchange.sessiond.SessiondService;
import com.openexchange.sessiond.SessiondServiceExtended;
import com.openexchange.sessiond.impl.container.SessionControl;
import com.openexchange.sessiond.impl.container.ShortTermSessionControl;

/**
 * {@link SessiondServiceImpl} - Implementation of {@link SessiondService}
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SessiondServiceImpl implements SessiondServiceExtended {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SessiondServiceImpl.class);

    /**
     * Initializes a new {@link SessiondServiceImpl}.
     */
    public SessiondServiceImpl() {
        super();
    }

    @Override
    public boolean hasForContext(final int contextId) {
        return SessionHandler.hasForContext(contextId, false);
    }

    @Override
    public Session addSession(final AddSessionParameter param) throws OXException {
        return SessionHandler.addSession(
            param.getUserId(),
            param.getUserLoginInfo(),
            param.getPassword(),
            param.getContext().getContextId(),
            param.getClientIP(),
            param.getFullLogin(),
            param.getAuthId(),
            param.getHash(),
            param.getClient(),
            param.getClientToken(),
            param.isTransient(),
            param.isStaySignedIn(),
            param.getOrigin(),
            param.getEnhancements(),
            param.getUserAgent());
    }

    @Override
    public boolean storeSession(String sessionId) throws OXException {
        // Assume 'addIfAbsent' is false to force update of the session in session storage
        return SessionHandler.storeSession(sessionId, false);
    }

    @Override
    public boolean storeSession(String sessionId, boolean addIfAbsent) throws OXException {
        return SessionHandler.storeSession(sessionId, addIfAbsent);
    }

    @Override
    public void changeSessionPassword(final String sessionId, final String newPassword) throws OXException {
        SessionHandler.changeSessionPassword(sessionId, newPassword);
    }

    @Override
    public void setSessionAttributes(String sessionId, SessionAttributes attrs) throws OXException {
        SessionHandler.setSessionAttributes(getSession(sessionId), attrs);
    }

    @Override
    public boolean removeSession(final String sessionId) {
        return (null != SessionHandler.clearSession(sessionId, true));
    }

    @Override
    public int removeUserSessions(final int userId, final Context ctx) {
        return SessionHandler.removeUserSessions(userId, ctx.getContextId()).length;
    }

    @Override
    public void removeContextSessions(final int contextId) {
        SessionHandler.removeContextSessions(contextId);
    }

    @Override
    public void removeContextSessionsGlobal(Set<Integer> contextIds) throws OXException {
        SessionHandler.removeContextSessionsGlobal(contextIds);
    }

    @Override
    public void removeUserSessionsGlobally(int userId, int contextId) throws OXException {
        SessionHandler.removeUserSessionsGlobal(userId, contextId);
    }

    @Override
    public Collection<String> removeSessions(SessionFilter filter) throws OXException {
        return SessionHandler.removeLocalSessions(filter);
    }

    @Override
    public Collection<String> removeSessionsGlobally(SessionFilter filter) throws OXException {
        List<String> local = SessionHandler.removeLocalSessions(filter);
        List<String> remote = SessionHandler.removeRemoteSessions(filter);
        List<String> all = new ArrayList<String>(local.size() + remote.size());
        all.addAll(local);
        all.addAll(remote);
        return all;
    }

    @Override
    public int getUserSessions(int userId, int contextId) {
        return SessionHandler.SESSION_COUNTER.getNumberOfSessions(userId, contextId);
    }

    @Override
    public Collection<Session> getActiveSessions(int userId, int contextId) {
        List<ShortTermSessionControl> sessionControls = SessionHandler.getUserActiveSessions(userId, contextId);
        if (null == sessionControls) {
            return Collections.emptyList();
        }

        List<Session> list = new ArrayList<Session>(sessionControls.size());
        for (ShortTermSessionControl sc : sessionControls) {
            list.add(sc.getSession());
        }
        return list;
    }

    @Override
    public Collection<Session> getSessions(int userId, int contextId) {
        return getSessions(userId, contextId, false);
    }

    @Override
    public Collection<Session> getSessions(int userId, int contextId, boolean considerSessionStorage) {
        List<SessionControl> sessionControls = SessionHandler.getUserSessions(userId, contextId, considerSessionStorage);
        if (null == sessionControls) {
            return Collections.emptyList();
        }

        List<Session> list = new ArrayList<Session>(sessionControls.size());
        for (SessionControl sc : sessionControls) {
            list.add(sc.getSession());
        }
        return list;
    }

    @Override
    public SessionImpl getSession(String sessionId) {
        return getSession(sessionId, true);
    }

    @Override
    public Session peekSession(String sessionId) {
        return peekSession(sessionId, true);
    }

    @Override
    public Session peekSession(String sessionId, boolean considerSessionStorage) {
        if (null == sessionId) {
            return null;
        }

        SessionControl sessionControl = SessionHandler.getSession(sessionId, true, considerSessionStorage, true);
        return null == sessionControl ? null : sessionControl.getSession();
    }

    @Override
    public SessionImpl getSession(String sessionId, boolean considerSessionStorage) {
        return getSession(sessionId, considerSessionStorage, true);
    }

    @Override
    public SessionImpl getSession(String sessionId, boolean considerSessionStorage, boolean considerLocalStorage) {
        if (null == sessionId) {
            return null;
        }

        SessionControl sessionControl = SessionHandler.getSession(sessionId, considerLocalStorage, considerSessionStorage, false);
        if (null == sessionControl) {
            LOG.debug("Session not found. ID: {}", sessionId);
            return null;
        }
        return sessionControl.getSession();
    }

    @Override
    public boolean isActive(final String sessionId) {
        if (null == sessionId) {
            return false;
        }
        return SessionHandler.isActive(sessionId);
    }

    @Override
    public List<String> getActiveSessionIDs() {
        return SessionHandler.getActiveSessionIDs();
    }

    @Override
    public Session getSessionByAlternativeId(String altId) {
        return getSessionByAlternativeId(altId, false);
    }

    @Override
    public Session getSessionByAlternativeId(String altId, boolean lookupSessionStorage) {
        if (null == altId) {
            return null;
        }
        SessionControl sessionControl = SessionHandler.getSessionByAlternativeId(altId, lookupSessionStorage);
        if (null == sessionControl) {
            LOG.debug("Session not found by alternative identifier. Alternative ID: {}", altId);
            return null;
        }
        return sessionControl.getSession();
    }

    @Override
    public Session getSessionByRandomToken(final String randomToken, final String localIp) {
        return SessionHandler.getSessionByRandomToken(randomToken, localIp);
    }

    @Override
    public Session getSessionByRandomToken(final String randomToken) {
        return SessionHandler.getSessionByRandomToken(randomToken, null);
    }

    @Override
    public Session getSessionWithTokens(final String clientToken, final String serverToken) throws OXException {
        return SessionHandler.getSessionWithTokens(clientToken, serverToken);
    }

    @Override
    public int getNumberOfActiveSessions() {
        return SessionHandler.getNumberOfActiveSessions();
    }

    @Override
    public Session getAnyActiveSessionForUser(final int userId, final int contextId) {
        final SessionControl sessionControl = SessionHandler.getAnyActiveSessionForUser(userId, contextId, false, false);
        return null == sessionControl ? null: sessionControl.getSession();
    }

    @Override
    public Session findFirstMatchingSessionForUser(final int userId, final int contextId, final SessionMatcher matcher) {
        if (null == matcher) {
            return null;
        }
        final Set<SessionMatcher.Flag> flags = matcher.flags();
        return SessionHandler.findFirstSessionForUser(userId, contextId, matcher, flags.contains(SessionMatcher.Flag.IGNORE_SHORT_TERM), flags.contains(SessionMatcher.Flag.IGNORE_LONG_TERM), flags.contains(SessionMatcher.Flag.IGNORE_SESSION_STORAGE));
    }

    @Override
    public Collection<String> findSessions(SessionFilter filter) throws OXException {
        return SessionHandler.findLocalSessions(filter);
    }

    @Override
    public Collection<String> findSessionsGlobally(SessionFilter filter) throws OXException {
        List<String> local = SessionHandler.findLocalSessions(filter);
        List<String> remote = SessionHandler.findRemoteSessions(filter);
        List<String> all = new ArrayList<String>(local.size() + remote.size());
        all.addAll(local);
        all.addAll(remote);
        return all;
    }

    @Override
    public boolean isApplicableForSessionStorage(Session session) {
        return SessionHandler.useSessionStorage(session);
    }
}
