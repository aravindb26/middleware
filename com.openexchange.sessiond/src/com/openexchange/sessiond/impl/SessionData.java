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

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import com.openexchange.exception.OXException;
import com.openexchange.exception.UncheckedInterruptedException;
import com.openexchange.session.Session;
import com.openexchange.sessiond.SessionExceptionCodes;
import com.openexchange.sessiond.SessionFilter;
import com.openexchange.sessiond.SessionMatcher;
import com.openexchange.sessiond.impl.container.LongTermSessionControl;
import com.openexchange.sessiond.impl.container.SessionControl;
import com.openexchange.sessiond.impl.container.ShortTermSessionControl;
import com.openexchange.sessiond.impl.container.UserRefCounter;
import com.openexchange.sessiond.impl.util.RotatableCopyOnWriteArrayListV2;
import com.openexchange.sessiond.impl.util.RotatableSessionContainerList;
import com.openexchange.sessiond.impl.util.RotateShortResult;
import com.openexchange.sessiond.impl.util.SessionContainer;
import com.openexchange.sessiond.impl.util.SessionMap;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * Object handling the multi threaded access to session container. Excessive locking is used to secure container data structures.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
final class SessionData {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SessionData.class);

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final int maxSessions;
    private final long randomTokenTimeout;
    private final Map<String, String> randoms;

    /** Plain array+direct indexing is the fastest technique of iterating. So, use CopyOnWriteArrayList since 'sessionList' is seldom modified (see rotateShort()) */
    private final RotatableSessionContainerList sessionContainers;

    /**
     * The LongTermUserGuardian contains an entry for a given UserKey if the longTermList contains a session for the user
     * <p>
     * This is used to guard against potentially slow serial searches of the long term sessions
     */
    private final UserRefCounter longTermUserGuardian = new UserRefCounter();

    /** Plain array+direct indexing is the fastest technique of iterating. So, use CopyOnWriteArrayList since 'longTermList' is seldom modified (see rotateLongTerm()) */
    private final RotatableCopyOnWriteArrayListV2<SessionMap<LongTermSessionControl>> longTermList;

    private final AtomicReference<ThreadPoolService> threadPoolService;
    private final AtomicReference<TimerService> timerService;

    protected final Map<String, ScheduledTimerTask> removers = new ConcurrentHashMap<String, ScheduledTimerTask>();

    /** The lock protecting all list mutators. */
    private final Lock lock = new ReentrantLock();

    /** The concurrent map to synchronize session "touches" */
    private final ConcurrentMap<String, Future<SessionControl>> touchSync;

    /** The modification counter that gets incremented whenever a session is touched */
    private final AtomicLong touchedCount;

    /**
     * Initializes a new {@link SessionData}.
     *
     * @param containerCount The container count for short-term sessions
     * @param maxSessions The max. number of total sessions
     * @param randomTokenTimeout The timeout for random tokens
     * @param longTermContainerCount The container count for long-term sessions
     */
    SessionData(int containerCount, int maxSessions, long randomTokenTimeout, int longTermContainerCount) {
        super();
        threadPoolService = new AtomicReference<ThreadPoolService>();
        timerService = new AtomicReference<TimerService>();
        touchSync = new ConcurrentHashMap<>(1024);
        touchedCount = new AtomicLong(0);
        this.maxSessions = maxSessions;
        this.randomTokenTimeout = randomTokenTimeout;

        randoms = new ConcurrentHashMap<String, String>(1024, 0.75F, 1);

        SessionContainer[] shortTermInit = new SessionContainer[containerCount];
        for (int i = containerCount; i-- > 0;) {
            shortTermInit[i] = new SessionContainer();
        }
        sessionContainers = new RotatableSessionContainerList(shortTermInit);
        LOG.info("Initialized SessionD short-term list with {} containers", I(containerCount));

        List<SessionMap<LongTermSessionControl>> longTermInit = new ArrayList<SessionMap<LongTermSessionControl>>(longTermContainerCount);
        for (int i = longTermContainerCount; i-- > 0;) {
            longTermInit.add(new SessionMap<LongTermSessionControl>(256));
        }
        longTermList = new RotatableCopyOnWriteArrayListV2<SessionMap<LongTermSessionControl>>(longTermInit);
        LOG.info("Initialized SessionD long-term list with {} containers", I(longTermContainerCount));
    }

    private long getCurrentTouchedCount() {
        return touchedCount.get();
    }

    private boolean isUnexpectedTouchedCount(long expectedTouchedCount) {
        return (expectedTouchedCount != touchedCount.get());
    }

    private boolean isExpectedTouchedCount(long expectedTouchedCount) {
        return (expectedTouchedCount == touchedCount.get());
    }

    void clear() {
        boolean locked = false;
        try {
            lock.lockInterruptibly();
            locked = true;
            sessionContainers.clear();
            randoms.clear();
            longTermUserGuardian.clear();
            longTermList.clear();
        } catch (InterruptedException e) { // NOSONARLINT
            throw UncheckedInterruptedException.restoreInterruptedStatusAndRethrow(e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * Rotates the session containers. A new slot is added to head of each queue, while the last one is removed.
     *
     * @return The removed sessions
     */
    RotateShortResult rotateShort() {
        boolean locked = false;
        try {
            lock.lockInterruptibly();
            locked = true;
            // This is the only location which alters 'sessionList' during runtime
            Collection<ShortTermSessionControl> droppedSessions = sessionContainers.rotate(new SessionContainer()).getSessionControls();
            if (droppedSessions.isEmpty()) {
                return new RotateShortResult(null, null);
            }

            List<SessionControl> movedToLongTerm = null;
            List<SessionControl> removed = null;
            List<SessionControl> transientSessions = null;
            try {
                for (ShortTermSessionControl control : droppedSessions) {
                    SessionImpl session = control.getSession();
                    if (session.isTransient()) {
                        // A transient session -- do not move to long-term container
                        if (null == transientSessions) {
                            transientSessions = new ArrayList<SessionControl>();
                        }
                        transientSessions.add(control);
                        LOG.debug("Removed transient session {} from short-term container due to SessionData.rotateShort()", session.getSessionID());
                    } else {
                        // A (non-transient) regular session
                        if (session.isStaySignedIn()) {
                            // Has "stay signed in" flag
                            longTermList.get(0).putBySessionId(session.getSessionID(), new LongTermSessionControl(control));
                            longTermUserGuardian.incrementCounter(session.getUserId(), session.getContextId());
                            if (movedToLongTerm == null) {
                                movedToLongTerm = new ArrayList<SessionControl>();
                            }
                            movedToLongTerm.add(control);
                            LOG.debug("Put session {} from short-term into long-term container due to SessionData.rotateShort()", session.getSessionID());
                        } else {
                            // No "stay signed in" flag; let session time out
                            if (removed == null) {
                                removed = new ArrayList<SessionControl>();
                            }
                            removed.add(control);
                            LOG.debug("Removed session {} from short-term container due to SessionData.rotateShort()", session.getSessionID());
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                // About to shut-down
                LOG.error("First long-term session container does not exist. Likely SessionD is shutting down...", e);
            }

            if (null != transientSessions) {
                SessionHandler.postContainerRemoval(transientSessions, true);
            }

            return new RotateShortResult(movedToLongTerm, removed);
        } catch (InterruptedException e) { // NOSONARLINT
            throw UncheckedInterruptedException.restoreInterruptedStatusAndRethrow(e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    List<LongTermSessionControl> rotateLongTerm() {
        boolean locked = false;
        try {
            lock.lockInterruptibly();
            locked = true;
            // This is the only location which alters 'longTermList' during runtime
            List<LongTermSessionControl> removedSessions = new ArrayList<LongTermSessionControl>(longTermList.rotate(new SessionMap<LongTermSessionControl>(256)).values());
            for (LongTermSessionControl sessionControl : removedSessions) {
                longTermUserGuardian.decrementCounter(sessionControl.getUserId(), sessionControl.getContextId());
                LOG.debug("Removed session {} from long-term container due to SessionData.rotateLongTerm()", sessionControl.getSessionID());
            }
            return removedSessions;
        } catch (InterruptedException e) { // NOSONARLINT
            throw UncheckedInterruptedException.restoreInterruptedStatusAndRethrow(e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    /**
     * Checks if given user in specified context has an active session kept in session container(s)
     *
     * @param userId The user identifier
     * @param contextId The user's context identifier
     * @param includeLongTerm <code>true</code> to also lookup the long term sessions, <code>false</code>, otherwise
     * @return <code>true</code> if given user in specified context has an active session; otherwise <code>false</code>
     */
    boolean isUserActive(int userId, int contextId, boolean includeLongTerm) {
        // A read-only access to session list
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();
            for (final SessionContainer container : sessionContainers) {
                if (container.containsUser(userId, contextId)) {
                    return true;
                }
            }

            // Check long-term container
            if (includeLongTerm && hasLongTermSession(userId, contextId)) {
                return true;
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
        return false;
    }

    private final boolean hasLongTermSession(final int userId, final int contextId) {
        return this.longTermUserGuardian.contains(userId, contextId);
    }

    private final boolean hasLongTermSession(final int contextId) {
        return this.longTermUserGuardian.contains(contextId);
    }

    SessionControl[] removeUserSessions(final int userId, final int contextId) {
        // Removing sessions is a write operation.
        final List<SessionControl> retval = new LinkedList<SessionControl>();

        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();
            for (final SessionContainer container : sessionContainers) {
                retval.addAll(container.removeSessionsByUser(userId, contextId));
            }

            if (hasLongTermSession(userId, contextId)) {
                for (SessionMap<LongTermSessionControl> longTerm : longTermList) {
                    for (LongTermSessionControl control : longTerm.values()) {
                        if (control.equalsUserAndContext(userId, contextId)) {
                            if (longTerm.removeBySessionId(control.getSessionID()) != null) {
                                longTermUserGuardian.decrementCounter(userId, contextId);
                            }
                            retval.add(control);
                        }
                    }
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
        return retval.toArray(new SessionControl[retval.size()]);
    }

    List<SessionControl> removeContextSessions(final int contextId) {
        // Removing sessions is a write operation.
        final List<SessionControl> list = new LinkedList<SessionControl>();

        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();
            for (final SessionContainer container : sessionContainers) {
                list.addAll(container.removeSessionsByContext(contextId));
            }

            if (hasLongTermSession(contextId)) {
                for (SessionMap<LongTermSessionControl> longTerm : longTermList) {
                    for (LongTermSessionControl control : longTerm.values()) {
                        if (control.equalsContext(contextId)) {
                            if (longTerm.removeBySessionId(control.getSessionID()) != null) {
                                longTermUserGuardian.decrementCounter(control.getUserId(), contextId);
                            }
                            list.add(control);
                        }
                    }
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
        return list;
    }

    /**
     * Removes all sessions belonging to given contexts from long-term and short-term container.
     *
     * @param contextIds - Set with the context identifiers to remove sessions for
     * @return List of {@link SessionControl} objects for each handled session
     */
    List<SessionControl> removeContextSessions(final Set<Integer> contextIds) {
        // Removing sessions is a write operation.
        final List<SessionControl> list = new ArrayList<SessionControl>();

        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();
            for (final SessionContainer container : sessionContainers) {
                list.addAll(container.removeSessionsByContexts(contextIds));
            }
            TIntSet contextIdsToCheck = new TIntHashSet(contextIds.size());
            for (int contextId : contextIds) {
                if (hasLongTermSession(contextId)) {
                    contextIdsToCheck.add(contextId);
                }
            }
            for (final SessionMap<LongTermSessionControl> longTerm : longTermList) {
                for (LongTermSessionControl control : longTerm.values()) {
                    Session session = control.getSession();
                    int contextId = session.getContextId();
                    if (contextIdsToCheck.contains(contextId)) {
                        if (longTerm.removeBySessionId(session.getSessionID()) != null) {
                            longTermUserGuardian.decrementCounter(session.getUserId(), contextId);
                        }
                        list.add(control);
                    }
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating

        return list;
    }

    boolean hasForContext(final int contextId) {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            for (SessionContainer container : sessionContainers) {
                if (container.hasForContext(contextId)) {
                    return true;
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
        return false;
    }

    /**
     * Gets the first session for given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param includeLongTerm Whether long-term container should be considered or not
     * @return The first matching session or <code>null</code>
     */
    public SessionControl getAnyActiveSessionForUser(final int userId, final int contextId, final boolean includeLongTerm) {
        ShortTermSessionControl control;
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            for (SessionContainer container : sessionContainers) {
                control = container.getAnySessionByUser(userId, contextId);
                if (control != null) {
                    return control;
                }
            }

            if (includeLongTerm && hasLongTermSession(userId, contextId)) {
                for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                    for (LongTermSessionControl ltCcontrol : longTermMap.values()) {
                        if (ltCcontrol.equalsUserAndContext(userId, contextId)) {
                            return ltCcontrol;
                        }
                    }
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating

        return null;
    }

    /**
     * Finds the first session for given user that satisfies given matcher.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param matcher The matcher to satisfy
     * @param ignoreShortTerm Whether short-term container should be considered or not
     * @param ignoreLongTerm Whether long-term container should be considered or not
     * @return The first matching session or <code>null</code>
     */
    public Session findFirstSessionForUser(int userId, int contextId, SessionMatcher matcher, boolean ignoreShortTerm, boolean ignoreLongTerm) {
        boolean includeShortTerm = !ignoreShortTerm;
        boolean includeLongTerm = !ignoreLongTerm;

        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();
            if (includeShortTerm) {
                ShortTermSessionControl control;
                for (SessionContainer container : sessionContainers) {
                    control = container.getAnySessionByUser(userId, contextId);
                    if ((control != null) && matcher.accepts(control.getSession())) {
                        return control.getSession();
                    }
                }
            }

            if (includeLongTerm) {
                if (!hasLongTermSession(userId, contextId)) {
                    return null;
                }
                for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                    for (LongTermSessionControl control : longTermMap.values()) {
                        if (control.equalsUserAndContext(userId, contextId) && matcher.accepts(control.getSession())) {
                            return control.getSession();
                        }
                    }
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating

        return null;
    }

    public List<Session> filterSessions(SessionFilter filter) {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            List<Session> sessions = new LinkedList<Session>();
            for (SessionContainer container : sessionContainers) {
                collectSessions(filter, container.getSessionControls(), sessions);
            }

            for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                collectSessions(filter, longTermMap.values(), sessions);
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return sessions;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    private static void collectSessions(SessionFilter filter, Collection<? extends SessionControl> sessionControls, List<Session> sessions) {
        sessions.addAll(sessionControls.stream().map(SessionControl::getSession).filter(filter::apply).toList());
    }

    /**
     * Gets the <b>local-only</b> active (short-term-only) sessions associated with specified user in given context.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The <b>local-only</b> active sessions or an empty list
     */
    List<ShortTermSessionControl> getUserActiveSessions(int userId, int contextId) {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            // A read-only access to session list
            List<ShortTermSessionControl> retval = null;

            // Short term ones
            List<ShortTermSessionControl> sessionsByUser;
            for (SessionContainer container : sessionContainers) {
                sessionsByUser = container.getSessionsByUser(userId, contextId);
                if (!sessionsByUser.isEmpty()) {
                    if (retval == null) {
                        retval = new ArrayList<ShortTermSessionControl>();
                    }
                    retval.addAll(sessionsByUser);
                }
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return retval == null ? Collections.emptyList() : retval;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    /**
     * Gets the <b>local-only</b> sessions associated with specified user in given context.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The <b>local-only</b> sessions or an empty list
     */
    List<SessionControl> getUserSessions(int userId, int contextId) {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            // A read-only access to session list
            List<SessionControl> retval = new LinkedList<SessionControl>();

            // Short term ones
            for (SessionContainer container : sessionContainers) {
                retval.addAll(container.getSessionsByUser(userId, contextId));
            }

            // Long term ones
            if (!hasLongTermSession(userId, contextId)) {
                return retval;
            }

            for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                for (LongTermSessionControl control : longTermMap.values()) {
                    if (control.equalsUserAndContext(userId, contextId)) {
                        retval.add(control);
                    }
                }
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return retval;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    /**
     * Gets the number of <b>local-only</b> sessions associated with specified user in given context.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param considerLongTerm <code>true</code> to also consider long-term sessions; otherwise <code>false</code>
     * @return The number of sessions
     */
    int getNumOfUserSessions(int userId, int contextId, boolean considerLongTerm) {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            // A read-only access to session list
            int count = 0;
            for (SessionContainer container : sessionContainers) {
                count += container.numOfUserSessions(userId, contextId);
            }

            if (considerLongTerm) {
                if (!hasLongTermSession(userId, contextId)) {
                    return count;
                }
                for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                    for (LongTermSessionControl control : longTermMap.values()) {
                        if (control.equalsUserAndContext(userId, contextId)) {
                            count++;
                        }
                    }
                }
            }
            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return count;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    /**
     * Checks validity/uniqueness of specified authentication identifier for given login
     *
     * @param login The login
     * @param authId The authentication identifier
     * @throws OXException If authentication identifier is invalid/non-unique
     */
    void checkAuthId(String login, String authId) throws OXException {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            if (null != authId) {
                for (SessionContainer container : sessionContainers) {
                    for (ShortTermSessionControl sc : container.getSessionControls()) {
                        if (authId.equals(sc.getSession().getAuthId())) {
                            throw SessionExceptionCodes.DUPLICATE_AUTHID.create(sc.getSession().getLogin(), login);
                        }
                    }
                }

                for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                    for (LongTermSessionControl control : longTermMap.values()) {
                        if (authId.equals(control.getSession().getAuthId())) {
                            throw SessionExceptionCodes.DUPLICATE_AUTHID.create(control.getSession().getLogin(), login);
                        }
                    }
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
    }

    /**
     * Adds specified session.
     *
     * @param session The session to add
     * @param noLimit <code>true</code> to add without respect to limitation; otherwise <code>false</code> to honor limitation
     * @return The associated {@link SessionControl} instance
     * @throws OXException If add operation fails
     */
    SessionControl addSession(final SessionImpl session, final boolean noLimit) throws OXException {
        return addSession(session, noLimit, false);
    }

    /**
     * Adds specified session.
     *
     * @param session The session to add
     * @param noLimit <code>true</code> to add without respect to limitation; otherwise <code>false</code> to honor limitation
     * @param addIfAbsent <code>true</code> to perform an add-if-absent operation; otherwise <code>false</code> to fail on duplicate session
     * @return The associated {@link SessionControl} instance
     * @throws OXException If add operation fails
     */
    SessionControl addSession(final SessionImpl session, final boolean noLimit, final boolean addIfAbsent) throws OXException {
        if (!noLimit && countSessions() > maxSessions) {
            throw SessionExceptionCodes.MAX_SESSION_EXCEPTION.create();
        }

        // Add session
        try {
            ShortTermSessionControl control = sessionContainers.get(0).put(session, addIfAbsent);
            randoms.put(session.getRandomToken(), session.getSessionID());
            scheduleRandomTokenRemover(session.getRandomToken());
            return control;
        } catch (IndexOutOfBoundsException e) {
            // About to shut-down
            throw SessionExceptionCodes.NOT_INITIALIZED.create();
        }
    }

    /**
     * Gets the max. number of total sessions
     *
     * @return the max. number of total sessions
     */
    int getMaxSessions() {
        return this.maxSessions;
    }

    int countSessions() {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            // A read-only access to session list
            int count = 0;
            for (SessionContainer container : sessionContainers) {
                count += container.size();
            }

            for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                count += longTermMap.size();
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return count;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    int[] getShortTermSessionsPerContainer() {
        // read-only access to short term sessions.
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            TIntList counts = new TIntArrayList(10);
            for (SessionContainer container : sessionContainers) {
                counts.add(container.size());
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return counts.toArray();
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    int[] getLongTermSessionsPerContainer() {
        // read-only access to long term sessions.
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            TIntList counts = new TIntArrayList(10);
            for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                counts.add(longTermMap.size());
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return counts.toArray();
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    SessionControl getSessionByAlternativeId(final String altId) {
        ShortTermSessionControl control = sessionContainers.get(0).getSessionByAlternativeId(altId);
        if (control != null) {
            // If found in first container there is no need for moving session
            return control;
        }

        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            for (SessionContainer container : sessionContainers) {
                control = container.getSessionByAlternativeId(altId);
                if (control != null) {
                    return getSession(control.getSessionID(), false);
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
        return null;
    }

    SessionControl getSession(final String sessionId, boolean peek) {
        if (peek) {
            // No need to synchronize since session shall only be "peeked" (not moved for first container if found)
            long expectedTouchedCount;
            do {
                expectedTouchedCount = getCurrentTouchedCount();
                for (SessionContainer container : sessionContainers) {
                    ShortTermSessionControl control = container.getSessionById(sessionId);
                    if (null != control) {
                        return control;
                    }
                }
            } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
        } else {
            // Check first container
            SessionControl fromFirst = sessionContainers.get(0).getSessionById(sessionId);
            if (null != fromFirst) {
                return fromFirst;
            }

            /*-
             * Synchronize concurrent look-up of a certain session to avoid "overlooking" that session by a another thread.
             *
             * Thread A moves session to first container while Thread B iterates containers looking for the same session and already passed
             * first container. Thread B would not find that session, then.
             */
            FutureTask<SessionControl> myLookUp = new FutureTask<>(new SessionTouchCallable(sessionId, sessionContainers, touchedCount));
            Future<SessionControl> future = touchSync.putIfAbsent(sessionId, myLookUp);
            if (future == null) {
                future = myLookUp;
                myLookUp.run();
                touchSync.remove(sessionId);
            } else {
                // Help GC
                myLookUp = null; // NOSONARLINT
            }
            SessionControl control = getFrom(future);
            if (control != null) {
                return control;
            }
        }

        Long expectedTouchedCount = L(getCurrentTouchedCount());
        LongTermSessionControl control = null;
        for (Iterator<SessionMap<LongTermSessionControl>> iterator = longTermList.iterator(); null == control && iterator.hasNext();) {
            SessionMap<LongTermSessionControl> longTermMap = iterator.next();
            control = longTermMap.getBySessionId(sessionId); // GET not REMOVE (to avoid inaccessible session)
            if (null != control) {
                // Put session into first container and then remove from current one (to avoid inaccessible session).
                sessionContainers.get(0).putSessionControlIgnoreDuplicate(new ShortTermSessionControl(control));
                if (longTermMap.removeBySessionId(control.getSessionID()) != null) {
                    longTermUserGuardian.decrementCounter(control.getUserId(), control.getContextId());
                    touchedCount.incrementAndGet();
                    expectedTouchedCount = null;
                    SessionHandler.postSessionReactivation(control.getSession());
                    LOG.debug("Reactivated session: Moved session {} from long-term container to most up-to-date short-term container", control.getSessionID());
                }
            }
        }
        return expectedTouchedCount != null && expectedTouchedCount.longValue() != getCurrentTouchedCount() ? getSession(sessionId, peek) : control;
    }

    private static SessionControl getFrom(Future<SessionControl> f) {
        try {
            return f.get();
        } catch (InterruptedException e) { // NOSONARLINT
            throw UncheckedInterruptedException.restoreInterruptedStatusAndRethrow(e);
        } catch (ExecutionException e) {
            // Cannot occur
            Throwable cause = e.getCause();
            throw new IllegalStateException(cause == null ? e : cause);
        }
    }

    ShortTermSessionControl optShortTermSession(final String sessionId) {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            ShortTermSessionControl control;
            for (SessionContainer container : sessionContainers) {
                control = container.getSessionById(sessionId);
                if (control != null) {
                    return control;
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating
        return null;
    }

    SessionControl getSessionByRandomToken(final String randomToken) {
        // A read-only access to session and a write access to random list
        final String sessionId = randoms.remove(randomToken);
        if (null == sessionId) {
            return null;
        }

        final SessionControl sessionControl = getSession(sessionId, false);
        if (null == sessionControl) {
            LOG.error("Unable to get session for sessionId: {}.", sessionId);
            SessionHandler.clearSession(sessionId, true);
            return null;
        }
        final SessionImpl session = sessionControl.getSession();
        if (!randomToken.equals(session.getRandomToken())) {
            final OXException e = SessionExceptionCodes.WRONG_BY_RANDOM.create(session.getSessionID(), session.getRandomToken(), randomToken, sessionId);
            LOG.error("", e);
            SessionHandler.clearSession(sessionId, true);
            return null;
        }
        session.removeRandomToken();
        if (sessionControl.getCreationTime() + randomTokenTimeout < System.currentTimeMillis()) {
            SessionHandler.clearSession(sessionId, true);
            return null;
        }
        return sessionControl;
    }

    SessionControl clearSession(final String sessionId) {
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            // Look-up in short-term list
            {
                ShortTermSessionControl sessionControl = null;
                for (Iterator<SessionContainer> it = sessionContainers.iterator(); sessionControl == null && it.hasNext();) {
                    sessionControl = it.next().removeSessionById(sessionId);
                }
                if (null != sessionControl) {
                    Session session = sessionControl.getSession();

                    String random = session.getRandomToken();
                    if (null != random) {
                        // If session is accessed through random token, random token is removed in the session.
                        randoms.remove(random);
                    }

                    LOG.debug("Removed session {} from short-term container due to SessionData.clearSession()", sessionId);
                    return sessionControl;
                }
            }

            // Look-up in long-term list
            for (SessionMap<LongTermSessionControl> longTermMap : longTermList) {
                LongTermSessionControl sessionControl = longTermMap.removeBySessionId(sessionId);
                if (null != sessionControl) {
                    Session session = sessionControl.getSession();

                    String random = session.getRandomToken();
                    if (null != random) {
                        // If session is accessed through random token, random token is removed in the session.
                        randoms.remove(random);
                    }

                    LOG.debug("Removed session {} from long-term container due to SessionData.clearSession()", sessionId);
                    return sessionControl;
                }
            }
        } while (isUnexpectedTouchedCount(expectedTouchedCount)); // Repeat since session might have been moved to first container while iterating

        // No such session...
        return null;
    }

    List<ShortTermSessionControl> getShortTermSessions() {
        // A read-only access
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            List<ShortTermSessionControl> retval = new ArrayList<ShortTermSessionControl>();
            for (SessionContainer container : sessionContainers) {
                retval.addAll(container.getSessionControls());
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return retval;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    List<String> getShortTermSessionIDs() {
        // A read-only access
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            List<String> retval = new ArrayList<String>();
            for (SessionContainer container : sessionContainers) {
                retval.addAll(container.getSessionIDs());
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return retval;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    List<LongTermSessionControl> getLongTermSessions() {
        // A read-only access
        long expectedTouchedCount;
        do {
            expectedTouchedCount = getCurrentTouchedCount();

            List<LongTermSessionControl> retval = null;
            Iterator<SessionMap<LongTermSessionControl>> it = longTermList.iterator();
            if (it.hasNext()) {
                retval = new ArrayList<LongTermSessionControl>(it.next().values());
                while (it.hasNext()) {
                    retval.addAll(it.next().values());
                }
            }

            if (isExpectedTouchedCount(expectedTouchedCount)) {
                return retval == null ? Collections.emptyList() : retval;
            }
        } while (true); // Repeat since session might have been moved to first container while iterating
    }

    void removeRandomToken(final String randomToken) {
        randoms.remove(randomToken);
    }

    public void addThreadPoolService(final ThreadPoolService service) {
        threadPoolService.set(service);
    }

    public void removeThreadPoolService() {
        threadPoolService.set(null);
    }

    /**
     * Adds the specified timer service.
     *
     * @param service The timer service
     */
    public void addTimerService(final TimerService service) {
        timerService.set(service);
    }

    /**
     * Removes the timer service
     */
    public void removeTimerService() {
        for (final ScheduledTimerTask timerTask : removers.values()) {
            timerTask.cancel();
        }
        timerService.set(null);
    }

    private void scheduleRandomTokenRemover(final String randomToken) {
        final RandomTokenRemover remover = new RandomTokenRemover(randomToken);
        final TimerService timerService = this.timerService.get();
        if (null == timerService) {
            remover.run();
        } else {
            final ScheduledTimerTask timerTask = timerService.schedule(remover, randomTokenTimeout, TimeUnit.MILLISECONDS);
            removers.put(randomToken, timerTask);
        }
    }

    /** Callable that looks-up a certain session in short-term containers and moves session to first container if found */
    private static class SessionTouchCallable implements Callable<SessionControl> {

        private final String sessionId;
        private final RotatableSessionContainerList sessionContainers;
        private final AtomicLong touchedCount;

        SessionTouchCallable(String sessionId, RotatableSessionContainerList sessionContainers, AtomicLong touchedCount) {
            super();
            this.sessionId = sessionId;
            this.sessionContainers = sessionContainers;
            this.touchedCount = touchedCount;
        }

        @Override
        public SessionControl call() {
            SessionContainer first = null;
            for (SessionContainer container : sessionContainers) {
                if (first == null) {
                    ShortTermSessionControl control = container.getSessionById(sessionId);
                    if (null != control) {
                        return control;
                    }
                    first = container;
                } else {
                    ShortTermSessionControl control = container.getSessionById(sessionId); // GET not REMOVE (to avoid inaccessible session)
                    if (null != control) {
                        first.putSessionControlIgnoreDuplicate(control);
                        if (container.removeSessionById(control.getSessionID()) != null) {
                            touchedCount.incrementAndGet();
                            SessionHandler.postSessionTouched(control.getSession());
                            LOG.debug("Touched session: Moved session {} from short-term container to first short-term container", control.getSessionID());
                        }
                        return control;
                    }
                }
            }
            return null;
        }
    }

    private class RandomTokenRemover implements Runnable {

        private final String randomToken;

        RandomTokenRemover(final String randomToken) {
            super();
            this.randomToken = randomToken;
        }

        @Override
        public void run() {
            try {
                removers.remove(randomToken);
                removeRandomToken(randomToken);
            } catch (Exception e) {
                LOG.error("Random token removal failed", e);
            }
        }
    }

    /**
     * Gets the number of sessions in the short term container
     *
     * @return The number of sessions in the short term container
     */
    public int getNumShortTerm() {
        int result = 0;
        for (SessionContainer container : sessionContainers) {
            result += container.size();
        }
        return result;
    }

    /**
     * Gets the number of sessions in the long term container
     *
     * @return the number of sessions in the long term container
     */
    public int getNumLongTerm() {
        int result = 0;
        for (final SessionMap<LongTermSessionControl> container : longTermList) {
            result += container.size();
        }
        return result;
    }

}
