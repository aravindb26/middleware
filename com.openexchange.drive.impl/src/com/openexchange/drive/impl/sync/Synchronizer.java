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

package com.openexchange.drive.impl.sync;

import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.l;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.openexchange.drive.DriveVersion;
import com.openexchange.drive.impl.comparison.Change;
import com.openexchange.drive.impl.comparison.ThreeWayComparison;
import com.openexchange.drive.impl.comparison.VersionMapper;
import com.openexchange.drive.impl.internal.SyncSession;
import com.openexchange.exception.OXException;


/**
 * {@link Synchronizer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class Synchronizer<T extends DriveVersion> {

    /** map to remember which WARN messages already got logged for the same user */
    private static final Cache<String, Long> WARNINGS_LOGGED_PER_USER = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

    protected static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Synchronizer.class);

    protected final SyncSession session;
    protected final VersionMapper<T> mapper;

    /**
     * Initializes a new {@link Synchronizer}.
     *
     * @param session The sync session
     * @param mapper The version mapper
     * @throws OXException
     */
    public Synchronizer(SyncSession session, VersionMapper<T> mapper) {
        super();
        this.session = session;
        this.mapper = mapper;
    }

    /**
     * Logs a <code>WARN</code> message to indicate a conflicting situation while synchronizing a certain file/directory version.
     * <p/>
     * The same warn message is only logged periodically on level <code>WARN</code> per user to avoid the generation of too many log
     * events.
     * 
     * @param format The message format to be logged, expecting to contain one argument
     * @param version The version for the message format
     * @param e The exception associated with the warning
     */
    protected void logWarning(String format, T version, OXException e) {
        String key = new StringBuilder().append(session.getServerSession().getUserId()).append('@').append(session.getServerSession().getContextId())
            .append(':').append(e.getPrefix()).append(e.getCode()).append(':').append(version.getChecksum()).toString();
        long now = System.currentTimeMillis();
        Long lastLogged = WARNINGS_LOGGED_PER_USER.getIfPresent(key);
        if (null == lastLogged || now - l(lastLogged) > TimeUnit.MINUTES.toMillis(5L)) {
            LOG.warn(format, version, e);
            WARNINGS_LOGGED_PER_USER.put(key, L(now));
        } else {
            LOG.debug(format, version, e);
        }
    }

    /**
     * Performs the synchronization.
     *
     * @return The sync result
     * @throws OXException
     */
    public IntermediateSyncResult<T> sync() throws OXException {
        IntermediateSyncResult<T> result = new IntermediateSyncResult<T>();
        int maxActions = getMaxActions();
        int nonTrivialActionCount = 0;
        for (Entry<String, ThreeWayComparison<T>> entry : mapper) {
            nonTrivialActionCount += process(result, entry.getValue());
            if (nonTrivialActionCount > maxActions) {
                session.trace("Interrupting processing since the maximum number of non-trivial actions (" + maxActions + ") is exceeded.");
                break;
            }
        }
        return result;
    }

    private int process(IntermediateSyncResult<T> result, ThreeWayComparison<T> comparison) throws OXException {
        Change clientChange = comparison.getClientChange();
        Change serverChange = comparison.getServerChange();
        if (Change.NONE == clientChange && Change.NONE == serverChange) {
            /*
             * nothing to do
             */
            return 0;
        } else if (Change.NONE == clientChange && Change.NONE != serverChange) {
            /*
             * process server-only change
             */
            return processServerChange(result, comparison);
        } else if (Change.NONE != clientChange && Change.NONE == serverChange) {
            /*
             * process client-only change
             */
            return processClientChange(result, comparison);
        } else {
            /*
             * process changes on both sides
             */
            return processConflictingChange(result, comparison);
        }
    }

    /**
     * Processes a server-only change caused by the supplied comparison.
     *
     * @param result The sync result to add resulting server- and client-actions
     * @param comparison The causing comparison
     * @return The number of non-trivial resulting actions, i.e. the number of actions of all added actions apart from
     *         <code>ACKNOWLEDGE</code>-actions.
     * @throws OXException
     */
    protected abstract int processServerChange(IntermediateSyncResult<T> result, ThreeWayComparison<T> comparison) throws OXException;

    /**
     * Processes a client-only change caused by the supplied comparison.
     *
     * @param result The sync result to add resulting server- and client-actions
     * @param comparison The causing comparison
     * @return The number of non-trivial resulting actions, i.e. the number of actions of all added actions apart from
     *         <code>ACKNOWLEDGE</code>-actions.
     * @throws OXException
     */
    protected abstract int processClientChange(IntermediateSyncResult<T> result, ThreeWayComparison<T> comparison) throws OXException;

    /**
     * Processes a conflicting, i.e. server- and client-change caused by the supplied comparison.
     *
     * @param result The sync result to add resulting server- and client-actions
     * @param comparison The causing comparison
     * @return The number of non-trivial resulting actions, i.e. the number of actions of all added actions apart from
     *         <code>ACKNOWLEDGE</code>-actions.
     * @throws OXException
     */
    protected abstract int processConflictingChange(IntermediateSyncResult<T> result, ThreeWayComparison<T> comparison) throws OXException;

    /**
     * Gets the maximum number of actions to be evaluated per synchronization request. Any further open actions will need to be handled in
     * consecutive synchronizations. A smaller value will lead to faster responses for the client and less resource utilization on the
     * backend, but increases the chance of rename- and move-optimizations not being detected.
     *
     * @return The maximum number of actions
     */
    protected abstract int getMaxActions();

}
