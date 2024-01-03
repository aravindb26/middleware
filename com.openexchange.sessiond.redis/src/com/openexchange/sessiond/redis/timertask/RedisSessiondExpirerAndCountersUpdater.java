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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.sessiond.redis.RedisSessiondService;
import com.openexchange.sessiond.redis.RedisLock;
import com.openexchange.sessiond.redis.RedisSessionConstants;
import com.openexchange.sessiond.redis.RedisSessiondState;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;

/**
 * {@link RedisSessiondExpirerAndCountersUpdater} - Performs session expiration & the counters update for data structures held in Redis for session data.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisSessiondExpirerAndCountersUpdater extends AbstractRedisTimerTask {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisSessiondExpirerAndCountersUpdater.class);

    /**
     * Initializes a new {@link RedisSessiondExpirerAndCountersUpdater}.
     *
     * @param sessiondService The Redis SessiondD service
     */
    public RedisSessiondExpirerAndCountersUpdater(RedisSessiondService sessiondService) {
        super(sessiondService);
    }

    /**
     * Expires sessions & updates the counters for session-related Redis keys and collections.
     *
     * @param lockExpirationMillis The time in milliseconds after which the expiration/counters lock is considered as expired
     * @param lockUpdateFrequencyMillis The frequency in milliseconds for updating the named lock
     */
    public void expireSessionsAndUpdateCounters(long lockExpirationMillis, long lockUpdateFrequencyMillis) {
        if (lockExpirationMillis <= 0) {
            throw new IllegalArgumentException("Lock expiration milliseconds must not be less than or equal to 0 (zero)");
        }
        try {
            redisSessiondService.getConnector().executeOperation(commandsProvider -> doExpireSessionsAndUpdateCounters(lockExpirationMillis, lockUpdateFrequencyMillis, commandsProvider));
        } catch (Exception e) {
            LOG.warn("Failed session expiration & counters update for Redis session storage", e);
        }
    }

    private Void doExpireSessionsAndUpdateCounters(long lockExpirationMillis, long lockUpdateFrequencyMillis, RedisCommandsProvider commandsProvider) {
        // Try to acquire lock
        Optional<RedisLock> optLock = RedisLock.lockFor(RedisSessionConstants.REDIS_SESSION_EXPIRE_AND_COUNTERS_LOCK, lockExpirationMillis, lockUpdateFrequencyMillis, commandsProvider, redisSessiondService);
        if (optLock.isEmpty()) {
            LOG.info("Aborted session expiration & counters update for Redis session storage. Another node is currently processing or operation already performed recently");
            return null;
        }

        // Lock acquired. Let it expire for next run
        RedisLock lock = optLock.get();
        try {
            LOG.info("Starting session expiration & counters update for Redis session storage...");
            long start = System.nanoTime();

            RedisSessiondState state = redisSessiondService.getState();
            long now = System.currentTimeMillis();
            expireSessions(commandsProvider, RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_LONG_LIFETIME, now - state.getSessionLongLifeTime());
            expireSessions(commandsProvider, RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_SHORT_LIFETIME, now - state.getSessionDefaultLifeTime());
            updateCounts(commandsProvider);

            long dur = Duration.ofNanos(System.nanoTime() - start).toMillis();
            LOG.info("Finished session expiration & counters update for Redis session storage after {}", toStringObjectFor(() -> exactly(dur, true)));
        } finally {
            lock.unlock(false, commandsProvider);
        }

        return null;
    }

    private static final int LIMIT = 1000;

    private void expireSessions(RedisCommandsProvider commandsProvider, String setKey, long threshold) {
        // Determine sessions with expiration time stamp before now
        RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();

        final Range<Number> range = Range.create(Long.valueOf(0), Long.valueOf(threshold));
        long offset = 0;
        Limit limit = Limit.create(offset, LIMIT);

        List<String> allExpired = null;
        List<String> expired = sortedSetCommands.zrangebyscore(setKey, range, limit);
        while (expired != null && !expired.isEmpty()) {
            // Remove expired sessions
            redisSessiondService.removeSessions(expired, false, false);
            LOG.debug("Expired session(s) {} from Redis storage", expired);

            // Remember removed session identifiers
            if (allExpired == null) {
                allExpired = expired;
            } else {
                allExpired.addAll(expired);
            }

            // Get next chunk (if any)
            if (expired.size() >= LIMIT) {
                offset += LIMIT;
                limit = Limit.create(offset, LIMIT);
                expired = sortedSetCommands.zrangebyscore(setKey, range, limit);
            } else {
                expired = null;
            }
        }
        if (allExpired != null) {
            sortedSetCommands.zrem(setKey, allExpired.toArray(new String[allExpired.size()]));
        }
    }

    private static final Set<String> HASH_FIELDS_TO_IGNORE = Set.of(RedisSessionConstants.COUNTER_SESSION_TOTAL, RedisSessionConstants.COUNTER_SESSION_ACTIVE, RedisSessionConstants.COUNTER_SESSION_LONG, RedisSessionConstants.COUNTER_SESSION_SHORT);

    private void updateCounts(RedisCommandsProvider commandsProvider) {
        RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
        RedisHashCommands<String, String> hashCommands = commandsProvider.getHashCommands();

        /*-
         * According to previous implementation considering first two short-term session containers;
         * see com.openexchange.sessiond.impl.SessiondConfigImpl.SHORT_CONTAINER_LIFE_TIME (6 minutes)
         */
        long ageMillis = Duration.ofMinutes(12).toMillis();
        long now = System.currentTimeMillis();
        long threshold = now - ageMillis;
        Range<Number> activeRange = Range.create(Long.valueOf(threshold), Long.valueOf(now + RedisSessionConstants.MILLIS_DAY));

        long countLong;
        {
            String setKey = RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_LONG_LIFETIME;
            countLong = sortedSetCommands.zcard(setKey).longValue();
            hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_LONG, Long.toString(countLong));
        }

        long countShort;
        {
            String setKey = RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_SHORT_LIFETIME;
            countShort = sortedSetCommands.zcard(setKey).longValue();
            hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_SHORT, Long.toString(countShort));
        }

        {
            hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_TOTAL, Long.toString(countLong + countShort));

            long activeLong = countLong == 0 ? 0 : determineCounterForNumberOfActiveSessions(RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_LONG_LIFETIME, activeRange, sortedSetCommands);
            long activeShort = countShort == 0 ? 0 : determineCounterForNumberOfActiveSessions(RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_SHORT_LIFETIME, activeRange, sortedSetCommands);
            hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_ACTIVE, Long.toString(activeLong + activeShort));
        }

        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        Set<String> handledBrandIds = new HashSet<>();
        ScanArgs scanArgs = ScanArgs.Builder.matches(redisSessiondService.getAllBrandSetKeyPattern()).limit(1000);
        KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
        while (cursor != null) {
            // Iterate current keys...
            for (String setKey : cursor.getKeys()) {
                String brandId = setKey.substring(setKey.lastIndexOf(':') + 1);
                long count = sortedSetCommands.zcard(setKey).longValue();
                hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_TOTAL_APPENDIX, Long.toString(count));

                long active = count == 0 ? 0 : determineCounterForNumberOfActiveSessions(setKey, activeRange, sortedSetCommands);
                hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_ACTIVE_APPENDIX, Long.toString(active));
                handledBrandIds.add(brandId);
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
        }

        for (String field : hashCommands.hkeys(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER)) {
            if (!HASH_FIELDS_TO_IGNORE.contains(field)) {
                String brandId = field.substring(0, field.lastIndexOf('.'));
                if (handledBrandIds.add(brandId)) {
                    // Not yet handled
                    String setKey = redisSessiondService.getSetKeyForBrand(brandId);
                    long count = sortedSetCommands.zcard(setKey).longValue();
                    hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_TOTAL_APPENDIX, Long.toString(count));

                    long active = count == 0 ? 0 : determineCounterForNumberOfActiveSessions(setKey, activeRange, sortedSetCommands);
                    hashCommands.hset(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_ACTIVE_APPENDIX, Long.toString(active));
                    handledBrandIds.add(brandId);
                }
            }
        }
    }

    protected long determineCounterForNumberOfActiveSessions(String setKey, Range<Number> range, RedisSortedSetCommands<String, String> sortedSetCommands) {
        long offset = 0;
        Limit limit = Limit.create(offset, RedisSessionConstants.LIMIT_1000);

        long count = 0;
        List<String> active = sortedSetCommands.zrangebyscore(setKey, range, limit);
        for (int size; active != null && (size = active.size()) > 0;) {
            count += size;

            // Get next chunk (if any)
            if (size >= RedisSessionConstants.LIMIT_1000) {
                offset += RedisSessionConstants.LIMIT_1000;
                limit = Limit.create(offset, RedisSessionConstants.LIMIT_1000);
                active = sortedSetCommands.zrangebyscore(setKey, range, limit);
            } else {
                active = null;
            }
        }
        return count;
    }

}
