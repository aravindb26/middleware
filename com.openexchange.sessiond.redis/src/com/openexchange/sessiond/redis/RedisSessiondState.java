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

import com.openexchange.config.ConfigTools;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.java.Strings;
import com.openexchange.sessiond.redis.cache.LocalSessionCache;
import com.openexchange.sessiond.redis.config.RedisSessiondConfigProperty;
import com.openexchange.sessiond.redis.usertype.UserTypeSessiondConfigRegistry;

/**
 * {@link RedisSessiondState} - The state for Redis session storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisSessiondState {

    /**
     * Creates a new builder.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** A builder for an instance of <code>RedisSessiondConfig</code> */
    public static class Builder {

        private int maxSessions;
        private int maxSessionPerClient;
        private int sessionDefaultLifeTime;
        private long sessionLongLifeTime;
        private int sessionLocalLifeTime;
        private boolean ensureExistenceOnLocalFetch;
        private int checkExistenceThreshold;
        private Obfuscator obfuscator;
        private UserTypeSessiondConfigRegistry userConfigRegistry;
        private LocalSessionCache localSessionCache;
        private boolean tryLockBeforeRedisLookUp;
        private int consistencyCheckIntervalMinutes;
        private int expirerAndCountersUpdateIntervalMinutes;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Initializes this builder from given configuration service
         *
         * @param configService The configuration service
         * @return THis builder
         */
        public Builder initFrom(LeanConfigurationService configService) {
            this.maxSessions = configService.getIntProperty(RedisSessiondConfigProperty.MAX_SESSIONS);
            int sessionShortLifeTime = configService.getIntProperty(RedisSessiondConfigProperty.SESSION_DEFAULT_LIFE_TIME);
            long longLifeTime = ConfigTools.parseTimespan(configService.getProperty(RedisSessiondConfigProperty.SESSION_LONG_LIFE_TIME).trim());
            if (longLifeTime < sessionShortLifeTime) {
                longLifeTime = sessionShortLifeTime;
            }
            int sessionLocalLifeTime = configService.getIntProperty(RedisSessiondConfigProperty.SESSION_LOCAL_LIFE_TIME);
            if (sessionShortLifeTime < sessionLocalLifeTime) {
                sessionShortLifeTime = sessionLocalLifeTime;
            }
            int checkExistenceThreshold = configService.getIntProperty(RedisSessiondConfigProperty.SESSION_CHECK_EXISTENCE_THRESHOLD);
            if (checkExistenceThreshold < 0) {
                checkExistenceThreshold = 0;
            }
            if (sessionLocalLifeTime < checkExistenceThreshold) {
                sessionLocalLifeTime = checkExistenceThreshold;
            }

            {
                String encryptionKey = configService.getProperty(RedisSessiondConfigProperty.ENCRYPTION_KEY);
                if (Strings.isEmpty(encryptionKey)) {
                    throw new IllegalStateException("Missing \"com.openexchange.sessiond.encryptionKey\" property");
                }
                this.obfuscator = new Obfuscator(encryptionKey.toCharArray());
            }

            this.sessionDefaultLifeTime = sessionShortLifeTime;
            this.sessionLongLifeTime = longLifeTime;
            this.checkExistenceThreshold = checkExistenceThreshold;
            this.sessionLocalLifeTime = sessionLocalLifeTime;
            this.ensureExistenceOnLocalFetch = configService.getBooleanProperty(RedisSessiondConfigProperty.SESSION_ENSURE_EXISTENCE_ON_LOCAL_FETCH);
            this.tryLockBeforeRedisLookUp = configService.getBooleanProperty(RedisSessiondConfigProperty.REDIS_TRY_LOCK_BEFORE_LOOKUP);
            this.consistencyCheckIntervalMinutes = configService.getIntProperty(RedisSessiondConfigProperty.REDIS_CONSISTENCY_CHECK_INTERVAL_MINUTES);
            this.expirerAndCountersUpdateIntervalMinutes = configService.getIntProperty(RedisSessiondConfigProperty.REDIS_EXPIRER_AND_COUNTERS_UPDATE_INTERVAL_MINUTES);

            this.userConfigRegistry = new UserTypeSessiondConfigRegistry(configService);

            this.localSessionCache = new LocalSessionCache(sessionLocalLifeTime);

            return this;
        }

        /**
         * Sets the max. number of sessions.
         *
         * @param maxSessions The max. number of sessions to set
         * @return This builder
         */
        public Builder withMaxSessions(int maxSessions) {
            this.maxSessions = maxSessions;
            return this;
        }

        /**
         * Sets the interval in minutes when to perform session expiration & counters update for Redis session storage.
         *
         * @param consistencyCheckIntervalMinutes The interval in minutes
         * @return This builder
         */
        public Builder withExpirerAndCountersUpdateIntervalMinutes(int expirerAndCountersUpdateIntervalMinutes) {
            this.expirerAndCountersUpdateIntervalMinutes = expirerAndCountersUpdateIntervalMinutes;
            return this;
        }

        /**
         * Sets the interval in minutes when to perform consistency check for Redis session storage.
         *
         * @param consistencyCheckIntervalMinutes The interval in minutes
         * @return This builder
         */
        public Builder withConsistencyCheckIntervalMinutes(int consistencyCheckIntervalMinutes) {
            this.consistencyCheckIntervalMinutes = consistencyCheckIntervalMinutes;
            return this;
        }

        /**
         * Sets the whether to synchronize access when performing session look-up in Redis.
         *
         * @param tryLockBeforeRedisLookUp <code>true</code> to synchronize access when performing session look-up in Redis; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withTryLockBeforeRedisLookUp(boolean tryLockBeforeRedisLookUp) {
            this.tryLockBeforeRedisLookUp = tryLockBeforeRedisLookUp;
            return this;
        }

        /**
         * Sets the local session cache.
         *
         * @param localSessionCache The local session cache
         * @return This builder
         */
        public Builder withLocalSessionCache(LocalSessionCache localSessionCache) {
            this.localSessionCache = localSessionCache;
            return this;
        }

        /**
         * Sets the user config registry.
         *
         * @param userConfigRegistry The user config registry to set
         * @return This builder
         */
        public Builder withUserConfigRegistry(UserTypeSessiondConfigRegistry userConfigRegistry) {
            this.userConfigRegistry = userConfigRegistry;
            return this;
        }

        /**
         * Sets the encryptionKey
         *
         * @param encryptionKey The encryptionKey to set
         * @return This builder
         */
        public Builder withEncryptionKey(String encryptionKey) {
            obfuscator = encryptionKey == null ? null : new Obfuscator(encryptionKey.toCharArray());
            return this;
        }

        /**
         * Sets the maxSessionPerClient
         *
         * @param maxSessionPerClient The maxSessionPerClient to set
         * @return This builder
         */
        public Builder withMaxSessionPerClient(int maxSessionPerClient) {
            this.maxSessionPerClient = maxSessionPerClient;
            return this;
        }

        /**
         * Sets the sessionDefaultLifeTime
         *
         * @param sessionDefaultLifeTime The sessionDefaultLifeTime to set
         * @return This builder
         */
        public Builder withSessionDefaultLifeTime(int sessionDefaultLifeTime) {
            this.sessionDefaultLifeTime = sessionDefaultLifeTime;
            return this;
        }

        /**
         * Sets the sessionLongLifeTime
         *
         * @param sessionLongLifeTime The sessionLongLifeTime to set
         * @return This builder
         */
        public Builder withSessionLongLifeTime(long sessionLongLifeTime) {
            this.sessionLongLifeTime = sessionLongLifeTime;
            return this;
        }

        /**
         * Sets the sessionLocalLifeTime
         *
         * @param sessionLocalLifeTime The sessionLocalLifeTime to set
         * @return This builder
         */
        public Builder withSessionLocalLifeTime(int sessionLocalLifeTime) {
            this.sessionLocalLifeTime = sessionLocalLifeTime;
            return this;
        }

        /**
         * Sets the ensureExistenceOnLocalFetch
         *
         * @param ensureExistenceOnLocalFetch The ensureExistenceOnLocalFetch to set
         * @return This builder
         */
        public Builder withEnsureExistenceOnLocalFetch(boolean ensureExistenceOnLocalFetch) {
            this.ensureExistenceOnLocalFetch = ensureExistenceOnLocalFetch;
            return this;
        }

        /**
         * Sets the checkExistenceThreshold
         *
         * @param checkExistenceThreshold The checkExistenceThreshold to set
         * @return This builder
         */
        public Builder withCheckExistenceThreshold(int checkExistenceThreshold) {
            this.checkExistenceThreshold = checkExistenceThreshold;
            return this;
        }

        /**
         * Builds the instance of <code>RedisSessiondConfig</code> from this builder's arguments.
         *
         * @return The instance of <code>RedisSessiondConfig</code>
         */
        public RedisSessiondState build() {
            return new RedisSessiondState(maxSessions, maxSessionPerClient, sessionDefaultLifeTime, sessionLongLifeTime, sessionLocalLifeTime, ensureExistenceOnLocalFetch, checkExistenceThreshold, obfuscator, userConfigRegistry, localSessionCache, tryLockBeforeRedisLookUp, consistencyCheckIntervalMinutes, expirerAndCountersUpdateIntervalMinutes);
        }

    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final int maxSessions;
    private final int maxSessionPerClient;
    private final int sessionDefaultLifeTime;
    private final long sessionLongLifeTime;
    private final int sessionLocalLifeTime;
    private final boolean ensureExistenceOnLocalFetch;
    private final int checkExistenceThreshold;
    private final Obfuscator obfuscator;
    private final UserTypeSessiondConfigRegistry userConfigRegistry;
    private final LocalSessionCache localSessionCache;
    private final boolean tryLockBeforeRedisLookUp;
    private final int consistencyCheckIntervalMinutes;
    private final int expirerAndCountersUpdateIntervalMinutes;

    /**
     * Initializes a new {@link RedisSessiondState}.
     */
    RedisSessiondState(int maxSessions, int maxSessionPerClient, int sessionDefaultLifeTime, long sessionLongLifeTime, int sessionLocalLifeTime, boolean ensureExistenceOnLocalFetch, int checkExistenceThreshold, Obfuscator obfuscator, UserTypeSessiondConfigRegistry userConfigRegistry, LocalSessionCache localSessionCache, boolean tryLockBeforeRedisLookUp, int consistencyCheckIntervalMinutes, int expirerAndCountersUpdateIntervalMinutes) {
        super();
        this.maxSessions = maxSessions;
        this.maxSessionPerClient = maxSessionPerClient;
        this.sessionDefaultLifeTime = sessionDefaultLifeTime;
        this.sessionLongLifeTime = sessionLongLifeTime;
        this.sessionLocalLifeTime = sessionLocalLifeTime;
        this.ensureExistenceOnLocalFetch = ensureExistenceOnLocalFetch;
        this.checkExistenceThreshold = checkExistenceThreshold;
        this.obfuscator = obfuscator;
        this.userConfigRegistry = userConfigRegistry;
        this.localSessionCache = localSessionCache;
        this.tryLockBeforeRedisLookUp = tryLockBeforeRedisLookUp;
        this.consistencyCheckIntervalMinutes = consistencyCheckIntervalMinutes;
        this.expirerAndCountersUpdateIntervalMinutes = expirerAndCountersUpdateIntervalMinutes;
    }

    /**
     * Destroys this state.
     */
    public void destroy() {
        localSessionCache.invalidateAll();
        obfuscator.destroy();
    }

    /**
     * Gets the interval in minutes when to perform session expiration & counters update against Redis session storage.
     *
     * @return The interval in minutes
     */
    public int getExpirerAndCountersUpdateIntervalMinutes() {
        return expirerAndCountersUpdateIntervalMinutes;
    }

    /**
     * Gets the interval in minutes when to perform consistency check against Redis session storage.
     *
     * @return The interval in minutes
     */
    public int getConsistencyCheckIntervalMinutes() {
        return consistencyCheckIntervalMinutes;
    }

    /**
     * Checks whether to synchronize access when performing session look-up in Redis.
     *
     * @return <code>true</code> to synchronize access when performing session look-up in Redis; otherwise <code>false</code>
     */
    public boolean isTryLockBeforeRedisLookUp() {
        return tryLockBeforeRedisLookUp;
    }

    /**
     * Gets the local session cache.
     *
     * @return The local session cache
     */
    public LocalSessionCache getLocalSessionCache() {
        return localSessionCache;
    }

    /**
     * Gets the user config registry.
     *
     * @return The user config registr
     */
    public UserTypeSessiondConfigRegistry getUserConfigRegistry() {
        return userConfigRegistry;
    }

    /**
     * Gets the obfuscator.
     *
     * @return The obfuscator
     */
    public Obfuscator getObfuscator() {
        return obfuscator;
    }

    /**
     * Gets the max. number of sessions.
     *
     * @return The max. number of sessions
     */
    public int getMaxSessions() {
        return maxSessions;
    }

    /**
     * Gets the maxSessionPerClient
     *
     * @return The maxSessionPerClient
     */
    public int getMaxSessionPerClient() {
        return maxSessionPerClient;
    }

    /**
     * Gets the sessionDefaultLifeTime
     *
     * @return The sessionDefaultLifeTime
     */
    public int getSessionDefaultLifeTime() {
        return sessionDefaultLifeTime;
    }

    /**
     * Gets the sessionLongLifeTime
     *
     * @return The sessionLongLifeTime
     */
    public long getSessionLongLifeTime() {
        return sessionLongLifeTime;
    }

    /**
     * Gets the sessionLocalLifeTime
     *
     * @return The sessionLocalLifeTime
     */
    public int getSessionLocalLifeTime() {
        return sessionLocalLifeTime;
    }

    /**
     * Gets the ensureExistenceOnLocalFetch
     *
     * @return The ensureExistenceOnLocalFetch
     */
    public boolean isEnsureExistenceOnLocalFetch() {
        return ensureExistenceOnLocalFetch;
    }

    /**
     * Gets the checkExistenceThreshold
     *
     * @return The checkExistenceThreshold
     */
    public int getCheckExistenceThreshold() {
        return checkExistenceThreshold;
    }

}
