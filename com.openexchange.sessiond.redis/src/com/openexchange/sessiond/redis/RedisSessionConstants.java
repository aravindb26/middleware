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


/**
 * {@link RedisSessionConstants} - Constants for Redis Session Storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public final class RedisSessionConstants {

    /**
     * Initializes a new {@link RedisSessionConstants}.
     */
    private RedisSessionConstants() {
        super();
    }

    /** The version of the Redis sessiond service. */
    public static final int VERSION = 2;

    /** The default limit when querying data from Redis storage */
    public static final int LIMIT_1000 = 1000;

    /** The constant providing the amount of milliseconds for one day */
    public static final long MILLIS_DAY = 86_400_000L;

    // -------------------------------------------------------------------------------------------------------------------------------------

    /** The delimiter character for composite keys */
    public static final char DELIMITER = ':';

    // --------------------------------------------------------- Sets ----------------------------------------------------------------------

    /** The prefix for keys referencing session sets */
    public static final String REDIS_SET_SESSIONIDS = "ox-sessionids";

    // --------------------------------------------------------- Keys ----------------------------------------------------------------------

    /** The prefix for keys referencing sessions */
    public static final String REDIS_SESSION = "ox-session";

    /** The prefix for keys referencing an alternative ID to session ID mapping */
    public static final String REDIS_SESSION_ALTERNATIVE_ID = "ox-session-altid";

    /** The prefix for keys referencing an auth-ID to session ID mapping */
    public static final String REDIS_SESSION_AUTH_ID = "ox-session-authid";

    /** The key for the consistency lock */
    public static final String REDIS_SESSION_CONSISTENCY_LOCK = "ox-session-conslock";

    /** The key for the expire/counter update lock */
    public static final String REDIS_SESSION_EXPIRE_AND_COUNTERS_LOCK = "ox-session-expirecounterslock";

    /** The key for the version of session data held in Redis storage */
    public static final String REDIS_SESSION_VERSION = "ox-session-version";

    /** The key for the version lock */
    public static final String REDIS_SESSION_VERSION_LOCK = "ox-session-versionlock";

    // --------------------------------------------------------- Hash & Counters -----------------------------------------------------------

    /** The keys for session counters managed in a Redis hash */
    public static final String REDIS_HASH_SESSION_COUNTER = "ox-sessioncounters";

    // ---------------------------------------

    /** The appendix for the names for such counters referencing total number of sessions */
    public static final String COUNTER_SESSION_TOTAL_APPENDIX = ".total";

    /** The appendix for the names for such counters referencing active sessions */
    public static final String COUNTER_SESSION_ACTIVE_APPENDIX = ".active";

    /** The name for the counter for total number of sessions */
    public static final String COUNTER_SESSION_TOTAL = "session" + COUNTER_SESSION_TOTAL_APPENDIX;

    /** The name for the counter for number of active sessions */
    public static final String COUNTER_SESSION_ACTIVE = "session" + COUNTER_SESSION_ACTIVE_APPENDIX;

    /** The name for the counter for number of short-term sessions */
    public static final String COUNTER_SESSION_SHORT = "session.short";

    /** The name for the counter for number of long-term sessions */
    public static final String COUNTER_SESSION_LONG = "session.long";

    // --------------------------------------------------------- Sorted sets ---------------------------------------------------------------

    /** The prefix for keys referencing brand-associated session identifiers */
    public static final String REDIS_SORTEDSET_SESSIONIDS_BRAND = "ox-sessionids-brand";

    /** The prefix for keys referencing identifiers of sessions with a long time-to-live */
    public static final String REDIS_SORTEDSET_SESSIONIDS_LONG_LIFETIME = "ox-sessionids-longlife";

    /** The prefix for keys referencing identifiers of sessions with a short time-to-live */
    public static final String REDIS_SORTEDSET_SESSIONIDS_SHORT_LIFETIME = "ox-sessionids-shortlife";

}
