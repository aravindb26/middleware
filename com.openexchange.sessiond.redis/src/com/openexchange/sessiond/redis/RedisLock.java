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

import static com.eaio.util.text.HumanTime.exactly;
import static com.openexchange.logging.LogUtility.toStringObjectFor;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import io.lettuce.core.RedisException;
import io.lettuce.core.SetArgs;

/**
 * {@link RedisLock} - An acquired Redis lock.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class RedisLock {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisLock.class);

    /**
     * Tries to acquire the lock for given lock key.
     *
     * @param lockKey The key of the lock
     * @param timeoutMillis The lock's time to live in milliseconds
     * @param updateFrequencyMillis The frequency in milliseconds to reset lock's time to live
     * @param commandsProvider Provides access to different command sets to communicate with Redis end-point
     * @param redisSessiondService The Redis sessiond service
     * @return The optional Redis lock
     * @throws RedisException If lock acquisition fails
     */
    public static Optional<RedisLock> lockFor(String lockKey, long timeoutMillis, long updateFrequencyMillis, RedisCommandsProvider commandsProvider, RedisSessiondService redisSessiondService) {
        return Optional.ofNullable(acquireLock(lockKey, timeoutMillis, updateFrequencyMillis, commandsProvider, redisSessiondService));
    }

    private static RedisLock acquireLock(String lockKey, long timeoutMillis, long updateFrequencyMillis, RedisCommandsProvider commandsProvider, RedisSessiondService redisSessiondService) {
        String uniqueLockValue = UUIDs.getUnformattedStringFromRandom();
        if ("OK".equals(commandsProvider.getStringCommands().set(lockKey, uniqueLockValue, new SetArgs().nx().px(timeoutMillis)))) {
            // Lock acquired
            LOG.debug("Acquired lock \"{}\" for {}", lockKey, toStringObjectFor(() -> exactly(timeoutMillis, true)));
            return new RedisLock(lockKey, uniqueLockValue, timeoutMillis, updateFrequencyMillis, redisSessiondService);
        }
        return null;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String lockKey;
    private final String lockValue;
    private final AtomicReference<ScheduledTimerTask> updaterTimerTaskRef;
    private final RedisSessiondService redisSessiondService;

    /**
     * Initializes a new {@link RedisLock}.
     *
     * @param lockKey The key of the lock
     * @param lockValue The value of the lock previously obtained
     * @param timeoutMillis The lock's time to live in milliseconds
     * @param updateFrequencyMillis The frequency in milliseconds to reset lock's time to live
     * @param redisSessiondService The Redis sessiond service
     */
    private RedisLock(String lockKey, String lockValue, long timeoutMillis, long updateFrequencyMillis, RedisSessiondService redisSessiondService) {
        super();
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.redisSessiondService = redisSessiondService;

        TimerService timerService = redisSessiondService.getServices().getOptionalService(TimerService.class);
        if (timerService != null) {
            Runnable task = new LockUpdateTask(lockKey, timeoutMillis, redisSessiondService);
            this.updaterTimerTaskRef = new AtomicReference<>(timerService.scheduleWithFixedDelay(task, updateFrequencyMillis, updateFrequencyMillis));
        } else {
            this.updaterTimerTaskRef = new AtomicReference<>();
        }
    }

    /**
     * Unlocks this Redis lock.
     *
     * @param commandsProvider Provides access to different command sets to communicate with Redis end-point
     * @return <code>true</code> if lock could be released; otherwise <code>false</code>
     */
    public boolean unlock(RedisCommandsProvider commandsProvider) {
        return unlock(true, commandsProvider);
    }

    /**
     * Unlocks this Redis lock.
     *
     * @param releaseLock Whether to release the lock immediately or to let it expire for next acquisition attempt
     * @param connection The connection to use
     * @return <code>true</code> if lock could be released; otherwise <code>false</code>
     */
    public boolean unlock(boolean releaseLock, RedisCommandsProvider commandsProvider) {
        cancelTimerTaskSafely();
        if (!releaseLock) {
            LOG.debug("Leaving lock \"{}\" to expire", lockKey);
            return false;
        }
        return releaseLockSafely(commandsProvider);
    }

    private boolean releaseLockSafely(RedisCommandsProvider commandsProvider) {
        try {
            boolean released = releaseLock(commandsProvider);
            LOG.debug(released ? "Released lock \"{}\"" : "Unable to release lock \"{}\"", lockKey);
            return released;
        } catch (Exception e) {
            LOG.error("Failed to release lock \"{}\"", lockKey, e);
            return false;
        }
    }

    private void cancelTimerTaskSafely() {
        ScheduledTimerTask updaterTimerTask = updaterTimerTaskRef.getAndSet(null);
        if (updaterTimerTask != null) {
            try {
                updaterTimerTask.cancel(false);
                TimerService timerService = redisSessiondService.getServices().getOptionalService(TimerService.class);
                if (timerService != null) {
                    timerService.purge();
                }
            } catch (Exception e) {
                LOG.error("Failed to cancel timer task for lock \"{}\"", lockKey, e);
            }
        }
    }

    /**
     * Releases the named lock.
     *
     * @return <code>true</code> if released; otherwise <code>false</code>
     * @throws OXException If lock release fails
     */
    private boolean releaseLock() throws OXException {
        return redisSessiondService.getConnector().<Boolean> executeOperation(commandsProvider -> Boolean.valueOf(releaseLock(commandsProvider))).booleanValue();
    }

    /**
     * Releases the named lock.
     *
     * @param commandsProvider Provides access to different command sets to communicate with Redis end-point
     * @return <code>true</code> if released; otherwise <code>false</code>
     * @throws OXException If lock release fails
     */
    private boolean releaseLock(RedisCommandsProvider commandsProvider) throws OXException {
        if (commandsProvider == null) {
            return releaseLock();
        }

        if (lockValue.equals(commandsProvider.getStringCommands().get(lockKey))) {
            return commandsProvider.getKeyCommands().del(lockKey).longValue() > 0;
        }
        return false;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final class LockUpdateTask implements Runnable {

        private final RedisSessiondService redisSessiondService;
        private final String lockKey;
        private final long timeoutMillis;

        /**
         * Initializes a new {@link RunnableImplementation}.
         *
         * @param lockKey The key of the lock
         * @param timeoutMillis The lock's time to live in milliseconds
         * @param redisSessiondService The Redis sessiond service
         */
        private LockUpdateTask(String lockKey, long timeoutMillis, RedisSessiondService redisSessiondService) {
            this.redisSessiondService = redisSessiondService;
            this.lockKey = lockKey;
            this.timeoutMillis = timeoutMillis;
        }

        @Override
        public void run() {
            try {
                updateLock(lockKey, timeoutMillis);
                LOG.info("Updated time to live of lock \"{}\" to {}", lockKey, toStringObjectFor(() -> exactly(timeoutMillis, true)));
            } catch (Exception e) {
                LOG.error("Failed to update time to live of lock \"{}\"", lockKey, e);
            }
        }

        /**
         * Updates the named lock; reset its time to live in milliseconds.
         *
         * @param lockKey The key of the lock
         * @param timeoutMillis The lock's time to live in milliseconds
         * @throws OXException If lock update fails
         */
        private void updateLock(String lockKey, long timeoutMillis) throws OXException {
            redisSessiondService.getConnector().executeVoidOperation(commandsProvider -> updateLock(lockKey, timeoutMillis, commandsProvider));
        }

        /**
         * Updates the named lock; reset its time to live in milliseconds.
         *
         * @param lockKey The key of the lock
         * @param timeoutMillis The lock's time to live in milliseconds
         * @param commandsProvider Provides access to different command sets to communicate with Redis end-point
         * @throws OXException If lock update fails
         */
        private static void updateLock(String lockKey, long timeoutMillis, RedisCommandsProvider commandsProvider) {
            commandsProvider.getKeyCommands().pexpire(lockKey, timeoutMillis);
        }
    }

}
