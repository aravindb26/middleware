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

package com.openexchange.sessiond.redis.timertask;

import static com.eaio.util.text.HumanTime.exactly;
import static com.openexchange.logging.LogUtility.toStringObjectFor;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.openexchange.java.Functions;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.session.Session;
import com.openexchange.sessiond.redis.RedisSessiondService;
import com.openexchange.sessiond.redis.RedisLock;
import com.openexchange.sessiond.redis.RedisSessionConstants;
import com.openexchange.sessiond.redis.RedisSessiondState;
import com.openexchange.sessiond.redis.util.BrandNames;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.api.sync.RedisStringCommands;

/**
 * {@link RedisSessiondConsistencyCheck} - Performs the consistency check for data structures held in Redis for session data.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisSessiondConsistencyCheck extends AbstractRedisTimerTask {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisSessiondConsistencyCheck.class);

    /**
     * Initializes a new {@link RedisSessiondConsistencyCheck}.
     *
     * @param sessiondService The Redis SessiondD service
     */
    public RedisSessiondConsistencyCheck(RedisSessiondService sessiondService) {
        super(sessiondService);
    }

    /**
     * Checks consistency of session-related Redis keys and collections.
     * <ul>
     * <li><code>"ox-session"</code>; e.g. <code>"ox-session:abcde123de"</code></li>
     * <li><code>"ox-session-altid"</code>; e.g. <code>"ox-session-altid:abcde123de"</code></li>
     * <li><code>"ox-session-authid"</code>; e.g. <code>"ox-session-authid:abcde123de"</code></li>
     * <li><code>"ox-sessionids"</code>; e.g. <code>"ox-sessionids:1337:3"</code></li>
     * </ul>
     *
     * @param withSessionConsistency <code>true</code> to check that each session has its corresponding entry in Redis set and Redis mapping keys; otherwise <code>false</code>
     * @param lockExpirationMillis The time in milliseconds after which the consistency lock is considered as expired
     * @param lockUpdateFrequencyMillis The frequency in milliseconds for updating the named lock
     */
    public void consistencyCheck(boolean withSessionConsistency, long lockExpirationMillis, long lockUpdateFrequencyMillis) {
        if (lockExpirationMillis <= 0) {
            throw new IllegalArgumentException("Lock expiration milliseconds must not be less than or equal to 0 (zero)");
        }
        try {
            redisSessiondService.getConnector().executeOperation(commandsProvider -> doConsistencyCheck(withSessionConsistency, lockExpirationMillis, lockUpdateFrequencyMillis, commandsProvider));
        } catch (Exception e) {
            LOG.warn("Failed consistency check for Redis session storage", e);
        }
    }

    private Void doConsistencyCheck(boolean withSessionConsistency, long lockExpirationMillis, long lockUpdateFrequencyMillis, RedisCommandsProvider commandsProvider) {
        // Try to acquire lock
        LOG.info("Starting consistency check for Redis session storage...");

        Optional<RedisLock> optLock = RedisLock.lockFor(RedisSessionConstants.REDIS_SESSION_CONSISTENCY_LOCK, lockExpirationMillis, lockUpdateFrequencyMillis, commandsProvider, redisSessiondService);
        if (optLock.isEmpty()) {
            LOG.info("Aborted consistency check for Redis session storage. Another node is currently processing or consistency check already performed recently");
            return null;
        }

        // Lock acquired. Let it expire for next run
        RedisLock lock = optLock.get();
        try {
            long start = System.nanoTime();

            checkSortedSetSessionIdsConsistency(commandsProvider, RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_LONG_LIFETIME);
            checkSortedSetSessionIdsConsistency(commandsProvider, RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_SHORT_LIFETIME);
            checkSessionIdsBrandSetConsistency(commandsProvider);
            checkSessionIdsSetConsistency(commandsProvider);
            checkAlternativeIdsConsistency(commandsProvider);
            checkAuthIdsConsistency(commandsProvider);
            if (withSessionConsistency) {
                checkSessionConsistency(commandsProvider);
            }

            long dur = Duration.ofNanos(System.nanoTime() - start).toMillis();
            LOG.info("Finished consistency check for Redis session storage storage after {}", toStringObjectFor(() -> exactly(dur, true)));
        } finally {
            lock.unlock(false, commandsProvider);
        }

        return null;
    }

    private void checkSessionConsistency(RedisCommandsProvider commandsProvider) {
        // Ensure each session has its entry in session-ids-set and alternativeId-to-sessionId association
        RedisStringCommands<String, Session> sessionCommands = redisSessiondService.getSessionCommands(commandsProvider);
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
        RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();

        RedisSessiondState state = null;

        String pattern = redisSessiondService.getAllSessionsPattern();
        ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(1000);

        KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
        while (cursor != null) {
            // Obtain current keys...
            List<String> keys = cursor.getKeys();
            if (keys.isEmpty() == false) {
                for (String sessionKey : keys) {
                    // Get session for Redis key
                    Session session = sessionCommands.get(sessionKey);
                    if (session != null) {
                        long now = System.currentTimeMillis();
                        String brandId = BrandNames.getBrandIdentifierFrom(session);
                        if (brandId != null) {
                            String setKey = redisSessiondService.getSetKeyForBrand(brandId);
                            if (sortedSetCommands.zrank(setKey, session.getSessionID()) == null) {
                                sortedSetCommands.zadd(setKey, now, session.getSessionID());
                            }
                        }

                        String setKey = redisSessiondService.getSetKeyForUser(session.getUserId(), session.getContextId());
                        if (setCommands.sismember(setKey, session.getSessionID()).booleanValue() == false) {
                            setCommands.sadd(setKey, session.getSessionID());
                        }

                        Object alternativeId = session.getParameter(Session.PARAM_ALTERNATIVE_ID);
                        if (alternativeId != null) {
                            String sessionAlternativeKey = redisSessiondService.getSessionAlternativeKey(alternativeId.toString());
                            if (keyCommands.exists(sessionAlternativeKey).longValue() <= 0) {
                                commandsProvider.getStringCommands().set(sessionAlternativeKey, session.getSessionID());
                            }
                        }

                        String authId = session.getAuthId();
                        if (authId != null) {
                            String authIdKey = redisSessiondService.getSessionAuthIdKey(authId);
                            if (keyCommands.exists(authIdKey).longValue() <= 0) {
                                commandsProvider.getStringCommands().set(authIdKey, session.getSessionID());
                            }
                        }

                        String sortedSetKey = redisSessiondService.getSortSetKeyForSession(session);
                        if (sortedSetCommands.zrank(sortedSetKey, session.getSessionID()) == null) {
                            if (state == null) {
                                state = redisSessiondService.getState();
                            }
                            sortedSetCommands.zadd(sortedSetKey, now, session.getSessionID());
                        }
                    }
                }
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
        }
    }

    private void checkAuthIdsConsistency(RedisCommandsProvider commandsProvider) {
        // Query all authentication identifiers
        String pattern = new StringBuilder(RedisSessionConstants.REDIS_SESSION_AUTH_ID).append(RedisSessionConstants.DELIMITER).append('*').toString();
        ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(1000);
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        RedisStringCommands<String, String> stringCommands = commandsProvider.getStringCommands();

        List<String> authIdsToDelete = null;
        KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
        while (cursor != null) {
            // Obtain current keys...
            List<String> keys = cursor.getKeys();

            // ... and checks them
            if (keys.isEmpty() == false) {
                // Check validity for each key
                for (String authIdKey : keys) {
                    String sessionId = stringCommands.get(authIdKey);
                    // Check session existence
                    if (keyCommands.exists(redisSessiondService.getSessionKey(sessionId)).longValue() <= 0) {
                        if (authIdsToDelete == null) {
                            authIdsToDelete = new LinkedList<>();
                        }
                        authIdsToDelete.add(authIdKey);
                    }
                }
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
        }

        if (authIdsToDelete != null) {
            keyCommands.del(authIdsToDelete.toArray(new String[authIdsToDelete.size()]));
        }
    }

    private void checkAlternativeIdsConsistency(RedisCommandsProvider commandsProvider) {
        // Query all alternative identifiers
        String pattern = new StringBuilder(RedisSessionConstants.REDIS_SESSION_ALTERNATIVE_ID).append(RedisSessionConstants.DELIMITER).append('*').toString();
        ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(1000);
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        RedisStringCommands<String, String> stringCommands = commandsProvider.getStringCommands();

        List<String> altIdsToDelete = null;
        KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
        while (cursor != null) {
            // Obtain current keys...
            List<String> keys = cursor.getKeys();

            // ... and check them
            if (keys.isEmpty() == false) {
                // Check validity for each key
                for (String altIdKey : keys) {
                    String sessionId = stringCommands.get(altIdKey);
                    // Check session existence
                    if (keyCommands.exists(redisSessiondService.getSessionKey(sessionId)).longValue() <= 0) {
                        if (altIdsToDelete == null) {
                            altIdsToDelete = new LinkedList<>();
                        }
                        altIdsToDelete.add(altIdKey);
                    }
                }
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
        }

        if (altIdsToDelete != null) {
            keyCommands.del(altIdsToDelete.toArray(new String[altIdsToDelete.size()]));
        }
    }

    private void checkSessionIdsSetConsistency(RedisCommandsProvider commandsProvider) {
        // Query all sets.
        ScanArgs scanArgs = ScanArgs.Builder.matches(redisSessiondService.getAllSetKeyPattern()).limit(1000);
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();

        Map<String, List<String>> sessionIdsToDelete = null;
        KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
        while (cursor != null) {
            // Obtain current keys...
            List<String> keys = cursor.getKeys();

            // ... and add them
            if (!keys.isEmpty()) {
                // Check validity for each key
                for (String setId : keys) {
                    Set<String> sessionIds = setCommands.smembers(setId);
                    if (sessionIds != null && !sessionIds.isEmpty()) {
                        for (String sessionId : sessionIds) {
                            // Check session existence
                            if (keyCommands.exists(redisSessiondService.getSessionKey(sessionId)).longValue() <= 0) {
                                // Remember to remove from set since non-existent
                                if (sessionIdsToDelete == null) {
                                    sessionIdsToDelete = new HashMap<>();
                                }
                                sessionIdsToDelete.computeIfAbsent(setId, Functions.getNewLinkedListFuntion()).add(sessionId);
                            }
                        }
                    }
                }
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
        }

        if (sessionIdsToDelete != null) {
            for (Map.Entry<String, List<String>> entry : sessionIdsToDelete.entrySet()) {
                List<String> sremList = entry.getValue();
                setCommands.srem(entry.getKey(), sremList.toArray(new String[sremList.size()]));
            }
        }
    }

    private void checkSessionIdsBrandSetConsistency(RedisCommandsProvider commandsProvider) {
        // Query all sets.
        ScanArgs scanArgs = ScanArgs.Builder.matches(redisSessiondService.getAllBrandSetKeyPattern()).limit(1000);
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();

        List<String> setIds = null;

        KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
        while (cursor != null) {
            // Obtain current keys...
            List<String> keys = cursor.getKeys();
            if (!keys.isEmpty()) {
                if (setIds == null) {
                    setIds = new ArrayList<>(keys);
                }else {
                    setIds.addAll(keys);
                }
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
        }

        // Check validity for each brand's sorted set
        if (setIds != null) {
            for (String setId : setIds) {
                checkSortedSetSessionIdsConsistency(commandsProvider, setId);
            }
        }
    }

    private void checkSortedSetSessionIdsConsistency(RedisCommandsProvider commandsProvider, String setKey) {
        ScanArgs scanArgs = ScanArgs.Builder.limit(1000);
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();

        List<String> sessionIdsToDelete = null;
        ScoredValueScanCursor<String> cursor = sortedSetCommands.zscan(setKey, scanArgs);
        while (cursor != null) {
            List<ScoredValue<String>> sessionIds = cursor.getValues();
            for (ScoredValue<String> sessionId : sessionIds) {
                // Check session existence
                if (keyCommands.exists(redisSessiondService.getSessionKey(sessionId.getValue())).longValue() <= 0) {
                    // Remember to remove
                    if (sessionIdsToDelete == null) {
                        sessionIdsToDelete = new ArrayList<>();
                    }
                    sessionIdsToDelete.add(sessionId.getValue());
                }
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : sortedSetCommands.zscan(setKey, cursor, scanArgs);
        }

        if (sessionIdsToDelete != null) {
            sortedSetCommands.zrem(setKey, sessionIdsToDelete.toArray(new String[sessionIdsToDelete.size()]));
        }
    }

}
