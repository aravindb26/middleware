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

package com.openexchange.sessiond.redis;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.openexchange.session.SessionAttributes;
import com.openexchange.sessionstorage.SessionStorageService;

/**
 * {@link RedisSessionStorageService}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisSessionStorageService implements SessionStorageService {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisSessionStorageService.class);

    private final RedisSessiondService redisSessiondService;

    /**
     * Initializes a new {@link RedisSessionStorageService}.
     *
     * @param redisSessiondService The Redis sessiond service instance
     */
    public RedisSessionStorageService(RedisSessiondService redisSessiondService) {
        super();
        this.redisSessiondService = redisSessiondService;
    }

    @Override
    public Session lookupSession(String sessionId) throws OXException {
        LOG.trace("Getting stored session by {}", sessionId);
        return redisSessiondService.getSessionFromRedis(SessionId.newSessionId(sessionId), true);
    }

    @Override
    public Session lookupSession(String sessionId, long timeoutMillis) throws OXException {
        LOG.trace("Getting stored session by {}", sessionId);
        return redisSessiondService.getSessionFromRedis(SessionId.newSessionId(sessionId), true);
    }

    @Override
    public void addSession(Session session) throws OXException {
        doAddSession(session, false);
    }

    @Override
    public void addSessionsIfAbsent(Collection<Session> sessions) throws OXException {
        for (Session session : sessions) {
            doAddSession(session, true);
        }
    }

    @Override
    public boolean addSessionIfAbsent(Session session) throws OXException {
        return doAddSession(session, true);
    }

    private boolean doAddSession(Session session, boolean addIfAbsent) throws OXException {
        if (session == null) {
            return false;
        }

        LOG.trace("Storing session {} for user {} in context {}", session.getSessionID(), I(session.getUserId()), I(session.getContextId()));
        boolean added = redisSessiondService.putSessionIntoRedisAndLocal(redisSessiondService.newSessionImplFor(session), addIfAbsent);
        if (added) {
            LOG.trace("Stored session {} of user {} in context {}", session.getSessionID(), I(session.getUserId()), I(session.getContextId()));
        } else {
            LOG.trace("Not stored session {} of user {} in context {} since already contained", session.getSessionID(), I(session.getUserId()), I(session.getContextId()));
        }
        return added;
    }

    @Override
    public void removeSession(String sessionId) throws OXException {
        LOG.trace("Removing stored session {}", sessionId);
        redisSessiondService.removeSession(sessionId);
    }

    @Override
    public List<Session> removeSessions(List<String> sessionIds) throws OXException {
        List<Session> removed = null;
        for (String sessionId : sessionIds) {
            Session sessionFromRedis = sessionId == null ? null : redisSessiondService.getSessionFromRedis(SessionId.newSessionId(sessionId), true);
            if (sessionFromRedis != null) {
                LOG.trace("Removing stored session {}", sessionId);
                if (redisSessiondService.removeSession(sessionId)) {
                    if (removed == null) {
                        removed = new ArrayList<>(sessionIds.size());
                    }
                    removed.add(sessionFromRedis);
                }
            }
        }
        return removed == null ? Collections.emptyList() : removed;
    }

    @Override
    public Session[] removeLocalUserSessions(int userId, int contextId) throws OXException {
        return removeUserSessions(userId, contextId);
    }

    @Override
    public Session[] removeUserSessions(int userId, int contextId) throws OXException {
        LOG.trace("Removing stored sessions for user {} in context {}", I(userId), I(contextId));
        List<Session> removedUserSessions = redisSessiondService.removeAndReturnUserSessions(userId, contextId, true);
        return removedUserSessions.toArray(new Session[removedUserSessions.size()]);
    }

    @Override
    public void removeLocalContextSessions(int contextId) throws OXException {
        removeContextSessions(contextId);
    }

    @Override
    public void removeContextSessions(int contextId) throws OXException {
        LOG.trace("Removing stored sessions for context {}", I(contextId));
        redisSessiondService.removeContextSessions(contextId);
    }

    @Override
    public boolean hasForContext(int contextId) throws OXException {
        return redisSessiondService.hasForContext(contextId);
    }

    @Override
    public Session[] getUserSessions(int userId, int contextId) throws OXException {
        Collection<Session> sessions = redisSessiondService.getActiveSessions(userId, contextId);
        return sessions.toArray(new Session[sessions.size()]);
    }

    @Override
    public Session getAnyActiveSessionForUser(int userId, int contextId) throws OXException {
        return redisSessiondService.getAnyActiveSessionForUser(userId, contextId);
    }

    @Override
    public Session findFirstSessionForUser(int userId, int contextId) throws OXException {
        return redisSessiondService.findFirstMatchingSessionForUser(userId, contextId, session -> true);
    }

    @Override
    public List<Session> getSessions() {
        try {
            Collection<String> sessions = redisSessiondService.findSessions(com.openexchange.sessiond.SessionFilter.ALL);
            List<Session> list = new ArrayList<>(sessions.size());
            for (String sessionId : sessions) {
                list.add(lookupSession(sessionId));
            }
            return list;
        } catch (OXException e) {
            LOG.error("failed to get sessions", e);
            return Collections.emptyList();
        }
    }

    @Override
    public int getNumberOfActiveSessions() {
        return redisSessiondService.getNumberOfActiveSessions();
    }

    @Override
    public Session getSessionByRandomToken(String randomToken, String newIP) throws OXException {
        return redisSessiondService.getSessionByRandomToken(randomToken, newIP);
    }

    @Override
    public Session getSessionByAlternativeId(String altId) throws OXException {
        return redisSessiondService.getSessionByAlternativeId(altId);
    }

    @Override
    public Session getCachedSession(String sessionId) throws OXException {
        return lookupSession(sessionId);
    }

    @Override
    public void changePassword(String sessionId, String newPassword) throws OXException {
        redisSessiondService.changeSessionPassword(sessionId, newPassword);
    }

    @Override
    public void setSessionAttributes(String sessionId, SessionAttributes attrs) throws OXException {
        redisSessiondService.setSessionAttributes(sessionId, attrs);
    }

    @Override
    public void checkAuthId(String login, String authId) throws OXException {
        // Nothing
    }

    @Override
    public void cleanUp() throws OXException {
        redisSessiondService.removeAllSessions();
    }

    @Override
    public int getUserSessionCount(int userId, int contextId) throws OXException {
        return redisSessiondService.getUserSessions(userId, contextId);
    }

}
