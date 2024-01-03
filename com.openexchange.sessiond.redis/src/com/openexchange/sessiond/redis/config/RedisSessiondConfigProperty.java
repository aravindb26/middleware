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

package com.openexchange.sessiond.redis.config;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.config.lean.Property;

/**
 * {@link RedisSessiondConfigProperty}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public enum RedisSessiondConfigProperty implements Property {

    /**
     * <code>"com.openexchange.sessiond.redis.enabled"</code>
     */
    REDIS_ENABLED("redis.enabled", Boolean.FALSE),
    /**
     * <code>"com.openexchange.sessiond.maxSession"</code>
     */
    MAX_SESSIONS("maxSession", I(0)),
    /**
     * <code>"com.openexchange.sessiond.maxSessionPerClient"</code>
     */
    MAX_SESSIONS_PER_CLIENT("maxSessionPerClient", I(0)),
    /**
     * <code>"com.openexchange.sessiond.sessionDefaultLifeTime"</code>
     * <p>
     * Specifies that each entry should be automatically removed from Redis once that duration
     * has elapsed after the entry's creation, the most recent replacement of its value, or its last
     * access.
     */
    SESSION_DEFAULT_LIFE_TIME("sessionDefaultLifeTime", I(3600000)),
    /**
     * <code>"com.openexchange.sessiond.sessionLongLifeTime"</code>
     * <p>
     * Specifies that each entry should be automatically removed from Redis once that duration
     * has elapsed after the entry's creation, the most recent replacement of its value, or its last
     * access.
     */
    SESSION_LONG_LIFE_TIME("sessionLongLifeTime", "1W"),
    /**
     * <code>"com.openexchange.sessiond.sessionLocalLifeTime"</code>
     * <p>
     * Specifies that each entry should be automatically removed from local cache once that duration
     * has elapsed after the entry's creation, or the most recent replacement of its value.
     */
    SESSION_LOCAL_LIFE_TIME("sessionLocalLifeTime", I(360000)),
    /**
     * <code>"com.openexchange.sessiond.ensureExistenceOnLocalFetch"</code>
     * <p>
     * Controls whether session existence is explicitly checked when a session is fetched from
     * node-local session cache. If <code>false</code>, sessions are only dropped from local
     * session cache when locally expired or a session invalidation event is remotely received.
     */
    SESSION_ENSURE_EXISTENCE_ON_LOCAL_FETCH("redis.ensureExistenceOnLocalFetch", Boolean.TRUE),
    /**
     * <code>"com.openexchange.sessiond.checkExistenceThreshold"</code>
     * <p>
     * Specifies the threshold (milliseconds) in which a local session needs to be checked if still existent in
     * Redis session storage. The higher chosen the value is, the less checks for existence are
     * performed with a higher risk of working with a non-existent session.<br>
     * Only applicable if "com.openexchange.sessiond.ensureExistenceOnLocalFetch" is set to "true".
     * <p>
     * A value of <code>0</code> (zero) or less disables the check; forcing every access to a
     * locally cached session's existence being verified.
     */
    SESSION_CHECK_EXISTENCE_THRESHOLD("redis.checkExistenceThreshold", I(10000)),
    /**
     * <code>"com.openexchange.sessiond.encryptionKey"</code>
     */
    ENCRYPTION_KEY("encryptionKey", ""),
    /**
     * <code>"com.openexchange.sessiond.useDistributedTokenSessions"</code>
     */
    USE_DISTRIBUTED_TOKEN_SESSIONS("useDistributedTokenSessions", Boolean.TRUE),
    /**
     * <code>"com.openexchange.sessiond.redis.tryLockBeforeRedisLookUp"</code>
     */
    REDIS_TRY_LOCK_BEFORE_LOOKUP("redis.tryLockBeforeRedisLookUp", Boolean.TRUE),
    /**
     * <code>"com.openexchange.sessiond.redis.consistencyCheckIntervalMinutes"</code>
     */
    REDIS_CONSISTENCY_CHECK_INTERVAL_MINUTES("redis.consistencyCheckIntervalMinutes", I(30)),
    /**
     * <code>"com.openexchange.sessiond.redis.expirerAndCountersUpdateIntervalMinutes"</code>
     */
    REDIS_EXPIRER_AND_COUNTERS_UPDATE_INTERVAL_MINUTES("redis.expirerAndCountersUpdateIntervalMinutes", I(5)),
    ;

    private final String fqn;
    private final Object defaultValue;

    private RedisSessiondConfigProperty(String appendix, Object defaultValue) {
        this.fqn = "com.openexchange.sessiond." + appendix;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
