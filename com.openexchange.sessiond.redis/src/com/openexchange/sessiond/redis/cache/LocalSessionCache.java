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

package com.openexchange.sessiond.redis.cache;

import static com.openexchange.java.Autoboxing.I;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.openexchange.exception.OXException;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessionExceptionCodes;
import com.openexchange.sessiond.SessionMatcher;
import com.openexchange.sessiond.SessiondEventConstants;
import com.openexchange.sessiond.redis.SessionId;
import com.openexchange.sessiond.redis.SessionImpl;
import com.openexchange.sessiond.redis.osgi.Services;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPools;

/**
 * {@link LocalSessionCache} - The local volatile session cache for fast look-up of already obtained sessions.
 * <p>
 * Basically manages a local Google cache with a fixed expire-after-write less than TTL in Redis session storage.
 * <p>
 * This mitigates the need to adjust EXPIRY on each session access in Redis session storage: Only check EXISTS as long as session is
 * alive in local cache. Grab it newly from Redis session storage (and adjust EXPIRY) once local session expires.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class LocalSessionCache {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(LocalSessionCache.class);

    private static final Object PRESENT = new Object();

    private final Cache<String, SessionImpl> localSessions;
    private final ConcurrentMap<String, String> alternativeId2sessionId;
    private final ConcurrentMap<Integer, ConcurrentMap<Integer, Map<String, Object>>> userSessions;

    /**
     * Initializes a new {@link LocalSessionCache}.
     *
     * @param durationMillis The length of time (in milliseconds) after an entry is created that it should be automatically removed
     */
    public LocalSessionCache(int durationMillis) {
        super();
        ConcurrentMap<String, String> alternativeId2sessionId = new ConcurrentHashMap<>(32, 0.9F, 1);
        this.alternativeId2sessionId = alternativeId2sessionId;

        ConcurrentMap<Integer, ConcurrentMap<Integer, Map<String, Object>>> userSessions = new ConcurrentHashMap<>(32, 0.9F, 1);
        this.userSessions = userSessions;

        // Specify removal listener that cares about adjusting other maps and firing event
        RemovalListener<String, SessionImpl> removalListener = notification -> {
            SessionImpl session = notification.getValue();
            if (session != null) {
                String sessionId = session.getSessionID();
                LOG.debug("Local session removed: {}", sessionId);

                // Remove alternative identifier to session identifier association
                String altId = (String) session.getParameter(Session.PARAM_ALTERNATIVE_ID);
                if (altId != null) {
                    alternativeId2sessionId.remove(altId);
                }

                // Drop from user-sessions-map
                int contextId = session.getContextId();
                int userId = session.getUserId();
                ConcurrentMap<Integer, Map<String, Object>> map = userSessions.get(I(contextId));
                if (null != map) {
                    Map<String, Object> sessionIds = map.get(I(userId));
                    if (sessionIds != null) {
                        sessionIds.remove(sessionId);
                    }
                }

                ThreadPools.submitElseExecute(new CheckForLastSessionTask(userId, contextId, this));
            }
        };

        // The a local Google cache with a fixed expire-after-write less than TTL in Redis session storage.
        this.localSessions = CacheBuilder.newBuilder().expireAfterWrite(Duration.ofMillis(durationMillis)).removalListener(removalListener).build();

    }

    /**
     * Checks if this container contains an entry for specified session identifier
     *
     * @param sessionId The session identifier
     * @return <code>true</code> if this container contains an entry for specified session identifier; otherwise <code>false</code>
     */
    public boolean containsSessionId(final String sessionId) {
        return localSessions.getIfPresent(sessionId) != null;
    }

    /**
     * Checks if this container contains an entry for specified alternative identifier.
     *
     * @param altId The alternative identifier
     * @return <code>true</code> if this container contains an entry for specified alternative identifier; otherwise <code>false</code>
     */
    public boolean containsAlternativeId(final String altId) {
        return alternativeId2sessionId.containsKey(altId);
    }

    /**
     * Gets the listing of all session identifiers that are currently held in local cache.
     *
     * @return The listing of session identifiers
     */
    public List<String> getSessionIds() {
        return new ArrayList<>(localSessions.asMap().keySet());
    }

    /**
     * Gets the session bound to specified session identifier.
     *
     * @param sessionId The session identifier
     * @return The session bound to specified session identifier, or <code>null</code> if there's no session for specified session identifier
     */
    public SessionImpl getSessionByIdIfPresent(final String sessionId) {
        return localSessions.getIfPresent(sessionId);
    }

    /**
     * Gets the session bound to specified session identifier.
     *
     * @param sessionId The session identifier
     * @return The session bound to specified session identifier, or <code>null</code> if there's no session for specified session identifier
     */
    public SessionImpl getSessionByIdIfPresent(SessionId sessionId) {
        return sessionId.isAlternativeId() ? getSessionByAlternativeIdIfPresent(sessionId.getIdentifier()) : getSessionByIdIfPresent(sessionId.getIdentifier());
    }

    /**
     * Gets the session bound to specified session identifier, obtaining that value from <code>loader</code> if necessary.
     *
     * @param sessionId The session identifier
     * @param loader The loader providing the value (if necessary)
     * @return The session bound to specified session identifier
     */
    public SessionImpl getSessionById(final String sessionId, Loader loader) {
        try {
            SessionImpl session = localSessions.get(sessionId, loader);
            if (loader.isLoaded()) {
                // Loaded...
                putSessionToOtherMaps(session);
            }
            return session;
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to load session", e.getCause() == null ? e : e.getCause());
        }
    }

    /**
     * Gets the session bound to specified alternative identifier.
     *
     * @param altId The alternative identifier
     * @return The session bound to specified alternative identifier, or <code>null</code> if there's no session for specified alternative identifier.
     */
    public SessionImpl getSessionByAlternativeIdIfPresent(final String altId) {
        String sessionId = alternativeId2sessionId.get(altId);
        if (sessionId == null) {
            return null;
        }

        SessionImpl session = localSessions.getIfPresent(sessionId);
        if (session == null) {
            // No such session associated with alternative identifier
            alternativeId2sessionId.remove(altId);
        }
        return session;
    }

    /**
     * Gets the sessions bound to specified user identifier and context identifier.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param matcher The matcher to use
     * @return The first matching session or <code>null</code>
     */
    public SessionImpl getFirstMatchingSessionForUser(final int userId, final int contextId, final SessionMatcher matcher) {
        Integer iContextId = Integer.valueOf(contextId);
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.get(iContextId);
        if (null == map) {
            return null;
        }

        Integer iUserId = Integer.valueOf(userId);
        Map<String, Object> sessionIds = map.remove(iUserId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return null;
        }

        for (String sessionId : new HashSet<>(sessionIds.keySet())) {
            SessionImpl s = localSessions.getIfPresent(sessionId);
            if (s != null && matcher.accepts(s)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Puts given session instance into this local cache.
     *
     * @param session The session to put
     * @param ignoreCollision Whether to ignore possible session identifier collision and to just put into local cache
     * @throws OXException If <code>ignoreCollision</code> is <code>true</code> and a session identifier collision occurs
     */
    public void put(SessionImpl session, boolean ignoreCollision) throws OXException {
        // Put to local sessions
        if (ignoreCollision) {
            localSessions.put(session.getSessionID(), session);
        } else {
            try {
                SessionImpl check = localSessions.get(session.getSessionID(), () -> session);
                if (check != session) {
                    throw SessionExceptionCodes.SESSIONID_COLLISION.create(check.getLogin(), session.getLogin());
                }
            } catch (ExecutionException e) {
                throw OXException.general("Failed to load session", e.getCause() == null ? e : e.getCause());
            }
        }

        putSessionToOtherMaps(session);
    }

    private void putSessionToOtherMaps(SessionImpl session) {
        // Remember alternative identifier to session identifier association
        String altId = (String) session.getParameter(Session.PARAM_ALTERNATIVE_ID);
        if (altId != null) {
            alternativeId2sessionId.put(altId, session.getSessionID());
        }

        // Add session identifier to user-sessions-map
        Integer iContextId = Integer.valueOf(session.getContextId());
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.get(iContextId);
        if (null == map) {
            ConcurrentMap<Integer, Map<String, Object>> newMap = new ConcurrentHashMap<Integer, Map<String,Object>>(32, 0.9F, 1);
            map = userSessions.putIfAbsent(iContextId, newMap);
            if (null == map) {
                map = newMap;
            }
        }

        Integer iUserId = Integer.valueOf(session.getUserId());
        Map<String, Object> sessionIds = map.get(iUserId);
        if (sessionIds == null) {
            Map<String, Object> newSet = new ConcurrentHashMap<String, Object>(16, 0.9F, 1);
            sessionIds = map.putIfAbsent(iUserId, newSet);
            if (null == sessionIds) {
                sessionIds = newSet;
            }
        }
        sessionIds.put(session.getSessionID(), PRESENT);
    }

    /**
     * Removes the session bound to specified session identifier.
     *
     * @param sessionId The session Id
     * @return The session previously associated with specified session identifier, or <code>null</code>.
     */
    public SessionImpl removeAndGetSessionById(final String sessionId) {
        if (null == sessionId) {
            return null;
        }

        // Adjusting other maps is performed in RemovalListener...
        return localSessions.asMap().remove(sessionId);
    }

    /**
     * Removes the session bound to specified session identifier.
     *
     * @param sessionId The session identifier
     */
    public void removeSessionById(final String sessionId) {
        if (null == sessionId) {
            return;
        }

        // Adjusting other maps is performed in RemovalListener...
        localSessions.invalidate(sessionId);
    }

    /**
     * Removes the sessions bound to specified session identifiers.
     *
     * @param sessionIds The session identifiers
     * @return The list of removed sessions
     */
    public void removeSessionsByIds(Collection<String> sessionIds) {
        if (null == sessionIds) {
            return;
        }

        // Adjusting other maps is performed in RemovalListener...
        for (String sessionId : sessionIds) {
            localSessions.invalidate(sessionId);
        }
    }

    /**
     * Removes the sessions bound to specified session identifiers.
     *
     * @param sessionIds The session identifiers
     * @return The list of removed sessions
     */
    public List<SessionImpl> removeAndGetSessionsByIds(Collection<String> sessionIds) {
        if (null == sessionIds) {
            return Collections.emptyList();
        }

        // Adjusting other maps is performed in RemovalListener...
        List<SessionImpl> l = new ArrayList<SessionImpl>(sessionIds.size());
        ConcurrentMap<String, SessionImpl> mapView = localSessions.asMap();
        for (String sessionId : sessionIds) {
            SessionImpl removed = mapView.remove(sessionId);
            if (removed != null) {
                l.add(removed);
            }
        }
        return l;
    }

    /**
     * Removes the sessions bound to specified user identifier and context identifier.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The sessions previously associated with specified user identifier and context identifier
     */
    public List<SessionImpl> removeAndGetSessionsByUser(final int userId, final int contextId) {
        Integer iContextId = Integer.valueOf(contextId);
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.get(iContextId);
        if (null == map) {
            return Collections.emptyList();
        }

        Integer iUserId = Integer.valueOf(userId);
        Map<String, Object> sessionIds = map.remove(iUserId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SessionImpl> l = new ArrayList<SessionImpl>(sessionIds.size());
        ConcurrentMap<String, SessionImpl> mapView = localSessions.asMap();
        for (String sessionId : new HashSet<>(sessionIds.keySet())) {
            // Adjusting other maps is performed in RemovalListener...
            SessionImpl removed = mapView.remove(sessionId);
            if (removed != null) {
                l.add(removed);
            }
        }
        return l;
    }

    /**
     * Removes the sessions bound to specified context identifier.
     *
     * @param contextId The context identifier
     * @return The sessions previously associated with specified context identifier.
     */
    public List<SessionImpl> removeAndGetSessionsByContext(final int contextId) {
        Integer iContextId = Integer.valueOf(contextId);
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.remove(iContextId);
        if (null == map || map.isEmpty()) {
            return Collections.emptyList();
        }

        List<SessionImpl> l = new ArrayList<SessionImpl>(128);
        ConcurrentMap<String, SessionImpl> mapView = localSessions.asMap();
        for (Map<String, Object> sessionIds : map.values()) {
            for (String sessionId : new HashSet<>(sessionIds.keySet())) {
                // Adjusting other maps is performed in RemovalListener...
                SessionImpl s = mapView.remove(sessionId);
                if (s != null) {
                    l.add(s);
                }
            }
        }
        return l;
    }

    /**
     * Removes the sessions bound to specified context identifier.
     *
     * @param contextId The context identifier
     */
    public void removeSessionsByContext(final int contextId) {
        Integer iContextId = Integer.valueOf(contextId);
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.remove(iContextId);
        if (null == map || map.isEmpty()) {
            return;
        }


        for (Map<String, Object> sessionIds : map.values()) {
            for (String sessionId : new HashSet<>(sessionIds.keySet())) {
                // Adjusting other maps is performed in RemovalListener...
                localSessions.invalidate(sessionId);
            }
        }
    }

    /**
     * Removes the sessions bound to the given contextIds.
     *
     * @param contextIds The context identifiers to remove
     * @return The sessions previously associated with context identifiers.
     */
    public List<SessionImpl> removeAndGetSessionsByContexts(final Set<Integer> contextIds) {
        if (contextIds == null) {
            return Collections.emptyList();
        }

        List<SessionImpl> removedSessionsByContexts = new ArrayList<SessionImpl>();
        for (int contextId : contextIds) {
            removedSessionsByContexts.addAll(this.removeAndGetSessionsByContext(contextId));
        }
        return removedSessionsByContexts;
    }

    /**
     * Removes the sessions bound to the given contextIds.
     *
     * @param contextIds The context identifiers to remove
     */
    public void removeSessionsByContexts(final Set<Integer> contextIds) {
        if (contextIds == null) {
            return;
        }

        for (int contextId : contextIds) {
            removeSessionsByContext(contextId);
        }
    }

    /**
     * Checks if there is any session for given context.
     *
     * @param contextId The context identifier
     * @return <code>true</code> if there is such a session; otherwise <code>false</code>
     */
    public boolean hasForContext(final int contextId) {
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.get(Integer.valueOf(contextId));
        if (null == map || map.isEmpty()) {
            return false;
        }

        for (Map<String, Object> sessionIds : map.values()) {
            if (false == sessionIds.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Discards all entries in the cache.
     */
    public void invalidateAll() {
        localSessions.invalidateAll();
        alternativeId2sessionId.clear();
        userSessions.clear();
    }

    /**
     * Discards all entries in the cache.
     *
     * @return All locally held sessions that were invalidated
     */
    public List<SessionImpl> invalidateAndGetAll() {
        List<SessionImpl> removedSessions = new ArrayList<>(localSessions.asMap().values());
        localSessions.invalidateAll();
        alternativeId2sessionId.clear();
        userSessions.clear();
        return removedSessions;
    }

    // ------------------------------------------------ Last session checks ----------------------------------------------------------------

    private boolean isUserActiveOnThisNode(int userId, int contextId) {
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.get(Integer.valueOf(contextId));
        if (null == map) {
            return false;
        }

        Map<String, Object> sessionIds = map.get(Integer.valueOf(userId));
        return sessionIds != null && !sessionIds.isEmpty();
    }

    private boolean isContextActiveOnThisNode(int contextId) {
        ConcurrentMap<Integer, Map<String, Object>> map = userSessions.get(Integer.valueOf(contextId));
        if (null == map) {
            return false;
        }

        for (Map<String, Object> sessionIds : map.values()) {
            if (sessionIds.isEmpty() == false) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------- Event stuff -----------------------------------------------------------------

    private void postLastSessionGone(int userId, int contextId, EventAdmin eventAdmin2) {
        EventAdmin eventAdmin = eventAdmin2 == null ? Services.optService(EventAdmin.class) : eventAdmin2;
        if (eventAdmin == null) {
            // Missing EventAdmin service. Nothing to do.
            return;
        }

        Dictionary<String, Object> dic = new Hashtable<String, Object>(2);
        dic.put(SessiondEventConstants.PROP_USER_ID, I(userId));
        dic.put(SessiondEventConstants.PROP_CONTEXT_ID, I(contextId));
        eventAdmin.postEvent(new Event(SessiondEventConstants.TOPIC_LAST_SESSION, dic));
        LOG.debug("Posted event for last removed session for user {} in context {}", I(userId), I(contextId));

        if (isContextActiveOnThisNode(contextId) == false) {
            postContextLastSessionGone(contextId, eventAdmin);
        }
    }

    private void postContextLastSessionGone(int contextId, EventAdmin eventAdmin2) {
        EventAdmin eventAdmin = eventAdmin2 == null ? Services.optService(EventAdmin.class) : eventAdmin2;
        if (eventAdmin == null) {
            // Missing EventAdmin service. Nothing to do.
            return;
        }

        Dictionary<String, Object> dic = new Hashtable<String, Object>(2);
        dic.put(SessiondEventConstants.PROP_CONTEXT_ID, I(contextId));
        eventAdmin.postEvent(new Event(SessiondEventConstants.TOPIC_LAST_SESSION_CONTEXT, dic));
        LOG.debug("Posted event for last removed session for context {}", I(contextId));
    }

    private static class CheckForLastSessionTask extends AbstractTask<Void> {

        private final int userId;
        private final int contextId;
        private final LocalSessionCache cache;

        CheckForLastSessionTask(int userId, int contextId, LocalSessionCache cache) {
            super();
            this.userId = userId;
            this.contextId = contextId;
            this.cache = cache;
        }

        @Override
        public Void call() {
            try {
                if (cache.isUserActiveOnThisNode(userId, contextId) == false) {
                    cache.postLastSessionGone(userId, contextId, null);
                }
            } catch (Exception e) {
                LOG.warn("Failed to check & fire event for last session gone for user {} in context {}", I(contextId), I(userId), e);
            }
            return null;
        }
    }

}
