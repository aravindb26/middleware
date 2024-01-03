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
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.logging.LogUtility.toStringObjectFor;
import static com.openexchange.sessiond.redis.SessionId.newAlternativeSessionId;
import static com.openexchange.sessiond.redis.SessionId.newSessionId;
import static com.openexchange.sessiond.redis.cache.InstanceLoader.loaderFor;
import static com.openexchange.sessiond.redis.commands.SessionRedisStringCommands.DO_NOTHING_VERSION_MISMATCH_HANDLER;
import static com.openexchange.sessiond.redis.util.BrandNames.getBrandIdentifierFor;
import static com.openexchange.sessiond.redis.util.BrandNames.getBrandIdentifierFrom;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.json.JSONException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import com.google.common.collect.Maps;
import com.openexchange.authentication.SessionEnhancement;
import com.openexchange.cluster.serialization.session.ClusterSession;
import com.openexchange.cluster.serialization.session.SessionCodec;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXRuntimeException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.ExceptionCatchingRunnable;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.lock.AccessControl;
import com.openexchange.lock.AccessControls;
import com.openexchange.lock.LockService;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.pubsub.Channel;
import com.openexchange.pubsub.ChannelKey;
import com.openexchange.pubsub.ChannelListener;
import com.openexchange.pubsub.DefaultChannelKey;
import com.openexchange.pubsub.Message;
import com.openexchange.pubsub.PubSubService;
import com.openexchange.pubsub.core.CoreChannelApplicationName;
import com.openexchange.pubsub.core.CoreChannelName;
import com.openexchange.redis.DefaultRedisOperationKey;
import com.openexchange.redis.RedisCommand;
import com.openexchange.redis.RedisCommandsProvider;
import com.openexchange.redis.RedisConnector;
import com.openexchange.redis.RedisConnectorProvider;
import com.openexchange.redis.RedisConnectorService;
import com.openexchange.redis.RedisExceptionCode;
import com.openexchange.redis.RedisOperation;
import com.openexchange.redis.RedisOperationKey;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Origin;
import com.openexchange.session.Session;
import com.openexchange.session.SessionAttributes;
import com.openexchange.session.SessionDescription;
import com.openexchange.session.SessionSerializationInterceptor;
import com.openexchange.session.UserAndContext;
import com.openexchange.sessiond.AddSessionParameter;
import com.openexchange.sessiond.SessionExceptionCodes;
import com.openexchange.sessiond.SessionFilter;
import com.openexchange.sessiond.SessionMatcher;
import com.openexchange.sessiond.SessiondEventConstants;
import com.openexchange.sessiond.SessiondServiceExtended;
import com.openexchange.sessiond.redis.cache.Loader;
import com.openexchange.sessiond.redis.cache.LocalSessionCache;
import com.openexchange.sessiond.redis.commands.SessionRedisStringCommands;
import com.openexchange.sessiond.redis.commands.SessionRedisStringCommands.VersionMismatchHandler;
import com.openexchange.sessiond.redis.config.RedisSessiondConfigProperty;
import com.openexchange.sessiond.redis.metrics.SessionMetricHandler;
import com.openexchange.sessiond.redis.osgi.Services;
import com.openexchange.sessiond.redis.timertask.RedisSessiondConsistencyCheck;
import com.openexchange.sessiond.redis.timertask.RedisSessiondExpirerAndCountersUpdater;
import com.openexchange.sessiond.redis.token.TokenSessionContainer;
import com.openexchange.sessiond.redis.token.TokenSessionControl;
import com.openexchange.sessiond.redis.usertype.UserTypeSessiondConfigInterface;
import com.openexchange.sessiond.redis.util.Counter;
import com.openexchange.sessiond.redis.util.RemovalCollection;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.RedisException;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.api.sync.RedisKeyCommands;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.api.sync.RedisStringCommands;

/**
 * {@link RedisSessiondService} - The abstract Redis sessiond service service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisSessiondService implements SessiondServiceExtended, ChannelListener<SessionEvent> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisSessiondService.class);

    private static final List<String> SORTED_SETS = List.of(RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_LONG_LIFETIME, RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_SHORT_LIFETIME);

    /** The channel key for session events */
    private static final ChannelKey SESSION_EVENT_CHANNEL_KEY = DefaultChannelKey.builder(RedisSessionConstants.DELIMITER).withChannelApplicationName(CoreChannelApplicationName.CORE).withChannelName(CoreChannelName.SESSION_EVENTS).build();

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final long LOCK_UPDATE_FREQUENCY_MILLIS = 30_000;

    protected final ServiceLookup services;
    protected final RedisConnector connector;

    private final VersionMismatchHandler versionMismatchHandler;
    private final AtomicReference<RedisSessiondState> stateReference;
    private final ServiceListing<SessionSerializationInterceptor> serializationInterceptors;
    private final AtomicReference<ScheduledTimerTask> consistencyCheckTimerTaskReference;
    private final AtomicReference<ScheduledTimerTask> expirerAndCounterUpdateTimerTaskReference;
    private final Channel<SessionEvent> channel;

    /**
     * Initializes a new {@link RedisSessiondService}.
     *
     * @param connector The connector
     * @param initialState The initial state to take over
     * @param serializationInterceptors The serialization interceptors
     * @param services The service look-up
     * @throws OXException If initialization fails
     */
    public RedisSessiondService(RedisConnector connector, RedisSessiondState initialState, ServiceListing<SessionSerializationInterceptor> serializationInterceptors, ServiceLookup services) throws OXException {
        super();
        this.connector = connector;
        this.serializationInterceptors = serializationInterceptors;
        this.services = services;
        this.stateReference = new AtomicReference<>(initialState);

        TimerService timerService = services.getServiceSafe(TimerService.class);
        {
            boolean lockExists = connector.executeOperation(commandsProvider -> B(commandsProvider.getKeyCommands().exists(RedisSessionConstants.REDIS_SESSION_CONSISTENCY_LOCK).longValue() > 0)).booleanValue();
            long intervalMillis = Duration.ofMinutes(initialState.getConsistencyCheckIntervalMinutes()).toMillis();
            long initialDelayMillis = ((long) (Math.random() * (lockExists ? intervalMillis : 10000)));
            consistencyCheckTimerTaskReference = new AtomicReference<>(timerService.scheduleWithFixedDelay(() -> new RedisSessiondConsistencyCheck(this).consistencyCheck(true, intervalMillis, LOCK_UPDATE_FREQUENCY_MILLIS), initialDelayMillis, intervalMillis, TimeUnit.MILLISECONDS));
        }
        {
            boolean lockExists = connector.executeOperation(commandsProvider -> B(commandsProvider.getKeyCommands().exists(RedisSessionConstants.REDIS_SESSION_EXPIRE_AND_COUNTERS_LOCK).longValue() > 0)).booleanValue();
            long intervalMillis = Duration.ofMinutes(initialState.getExpirerAndCountersUpdateIntervalMinutes()).toMillis();
            long initialDelayMillis =  ((long) (Math.random() * (lockExists ? intervalMillis : 10000)));
            expirerAndCounterUpdateTimerTaskReference = new AtomicReference<>(timerService.scheduleWithFixedDelay(() -> new RedisSessiondExpirerAndCountersUpdater(this).expireSessionsAndUpdateCounters(intervalMillis, LOCK_UPDATE_FREQUENCY_MILLIS), initialDelayMillis, intervalMillis, TimeUnit.MILLISECONDS));
        }

        PubSubService pubSubService = services.getServiceSafe(PubSubService.class);
        channel = pubSubService.getChannel(SESSION_EVENT_CHANNEL_KEY, SessionEventChannelMessageCodec.getInstance());
        channel.subscribe(this);

        if (checkVersion()) {
            for (String brandId : getBrandIdsForCounters()) {
                SessionMetricHandler.registerBrandMetricIfAbsent(brandId);
            }
        }

        versionMismatchHandler = (key, e, command) -> {
            String sessionId = key.substring(key.lastIndexOf(':') + 1);
            if (LOG.isDebugEnabled()) {
                LOG.info("Encountered version mismatch for session {}", sessionId, e);
            } else {
                LOG.info("Encountered version mismatch for session {}", sessionId);
            }

            if (RedisCommand.GETSET == command) {
                // Session data has been replaced anyway
                return true;
            }

            try {
                removeSession(sessionId);
            } catch (Exception fail) {
                LOG.error("Failed to handle version mismatch: {}", e.getMessage(), fail);
            }
            return true;
        };
    }

    /**
     * Replaces the state used by this service.
     *
     * @param state The state to set
     */
    public void setState(RedisSessiondState state) {
        if (state != null) {
            // Drop existent timer task (if any)
            ScheduledTimerTask tmrTask = consistencyCheckTimerTaskReference.getAndSet(null);
            if (tmrTask != null) {
                tmrTask.cancel();
            }
            tmrTask = expirerAndCounterUpdateTimerTaskReference.getAndSet(null);
            if (tmrTask != null) {
                tmrTask.cancel();
            }

            // Re-schedule timer task
            TimerService timerService = services.getOptionalService(TimerService.class);
            if (timerService != null) {
                {
                    int intervalMinutes = state.getConsistencyCheckIntervalMinutes();
                    long initialDelayMinutes = 1 + ((long) (Math.random() * intervalMinutes));
                    consistencyCheckTimerTaskReference.set(timerService.scheduleWithFixedDelay(() -> new RedisSessiondConsistencyCheck(this).consistencyCheck(true, Duration.ofMinutes(intervalMinutes).toMillis(), LOCK_UPDATE_FREQUENCY_MILLIS), initialDelayMinutes, intervalMinutes, TimeUnit.MINUTES));
                }
                {
                    int intervalMinutes = state.getExpirerAndCountersUpdateIntervalMinutes();
                    long initialDelayMinutes = 1 + ((long) (Math.random() * intervalMinutes));
                    expirerAndCounterUpdateTimerTaskReference.set(timerService.scheduleWithFixedDelay(() -> new RedisSessiondExpirerAndCountersUpdater(this).expireSessionsAndUpdateCounters(Duration.ofMinutes(intervalMinutes).toMillis(), LOCK_UPDATE_FREQUENCY_MILLIS), initialDelayMinutes, intervalMinutes, TimeUnit.MINUTES));
                }
            }

            // Replace state
            replaceState(state);
        }
    }

    private void replaceState(RedisSessiondState state) {
        RedisSessiondState oldState = this.stateReference.getAndSet(state);
        if (oldState != null) {
            oldState.destroy();
        }
    }

    /**
     * Shuts-down this sessiond service.
     */
    public void shutDown() {
        // Drop existent timer task (if any)
        ScheduledTimerTask timerTask = this.consistencyCheckTimerTaskReference.getAndSet(null);
        if (timerTask != null) {
            timerTask.cancel();
        }

        // Replace state
        replaceState(null);

        // Unsubscribe from Pub/Sub channel
        try {
            channel.unsubscribe(this);
        } catch (Exception e) {
            LOG.warn("Failed to unsibscribe channel listener", e);
        }
    }

    /**
     * Gets the current state.
     *
     * @return The state
     */
    public RedisSessiondState getState() {
        RedisSessiondState state = this.stateReference.get();
        if (state == null) {
            throw new IllegalStateException("Redis session storage shutting down...");
        }
        return state;
    }

    /**
     * Gets the Redis connector.
     *
     * @return The Redis connector
     */
    public RedisConnector getConnector() {
        return connector;
    }

    /**
     * Gets the service look-up.
     *
     * @return The service look-up
     */
    public ServiceLookup getServices() {
        return services;
    }

    // ------------------------------------------------------------ Utils ------------------------------------------------------------------

    private static DefaultRedisOperationKey newExistsOperationKey(String sessionId) {
        return newExistsOperationKey(sessionId, false);
    }

    private static DefaultRedisOperationKey newExistsOperationKey(String sessionId, boolean custom) {
        return DefaultRedisOperationKey.builder().withCommand(RedisCommand.EXISTS).withContextId(custom ? 1 : 0).withUserId(1).withHash(sessionId).build();
    }

    /**
     * Gets the key for a session; e.g. <code>"ox-session:abcde123de"</code>
     *
     * @param sessionId The session identifier
     * @return The key
     */
    public String getSessionKey(String sessionId) {
        return new StringBuilder(RedisSessionConstants.REDIS_SESSION).append(RedisSessionConstants.DELIMITER).append(sessionId).toString();
    }

    /**
     * Gets the key pattern to query all sessions; e.g. <code>"ox-session:*"</code>.
     *
     * @return The key pattern for all sessions
     */
    public String getAllSessionsPattern() {
        return new StringBuilder(RedisSessionConstants.REDIS_SESSION).append(RedisSessionConstants.DELIMITER).append('*').toString();
    }

    /**
     * Gets the key for an alternative session identifier; e.g. <code>"ox-session-altid:abcde123de"</code>
     *
     * @param alternativeId The alternative session identifier
     * @return The key for an alternative session identifier
     */
    public String getSessionAlternativeKey(String alternativeId) {
        return new StringBuilder(RedisSessionConstants.REDIS_SESSION_ALTERNATIVE_ID).append(RedisSessionConstants.DELIMITER).append(alternativeId).toString();
    }

    /**
     * Gets the key for an authentication identifier; e.g. <code>"ox-session-authid:3e5de123de"</code>
     *
     * @param authId The authentication identifier
     * @return The key for an authentication identifier
     */
    public String getSessionAuthIdKey(String authId) {
        return new StringBuilder(RedisSessionConstants.REDIS_SESSION_AUTH_ID).append(RedisSessionConstants.DELIMITER).append(authId).toString();
    }

    /**
     * Gets the set key for given user; e.g. <code>"ox-sessionids:1337:3"</code>
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The set key
     */
    public String getSetKeyForUser(int userId, int contextId) {
        return new StringBuilder(RedisSessionConstants.REDIS_SET_SESSIONIDS).append(RedisSessionConstants.DELIMITER).append(contextId).append(RedisSessionConstants.DELIMITER).append(userId).toString();
    }

    /**
     * Gets the pattern to look-up context-associated set keys; e.g. <code>"ox-sessionids:1337:*"</code>
     *
     * @return The pattern
     */
    public String getSetKeyPattern(int contextId) {
        return new StringBuilder(RedisSessionConstants.REDIS_SET_SESSIONIDS).append(RedisSessionConstants.DELIMITER).append(contextId).append(RedisSessionConstants.DELIMITER).append('*').toString();
    }

    /**
     * Gets the pattern to look-up all set keys: <code>"ox-sessionids:*"</code>
     *
     * @return The pattern
     */
    public String getAllSetKeyPattern() {
        return new StringBuilder(RedisSessionConstants.REDIS_SET_SESSIONIDS).append(RedisSessionConstants.DELIMITER).append('*').toString();
    }

    /**
     * Gets the set key for given brand; e.g. <code>"ox-sessionids-brand:aol"</code>
     *
     * @param brandId The identifier of the brand; e.g. <code>"aol"</code>
     * @return The set key
     */
    public String getSetKeyForBrand(String brandId) {
        return new StringBuilder(RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_BRAND).append(RedisSessionConstants.DELIMITER).append(brandId).toString();
    }

    /**
     * Gets the pattern to look-up all brand set keys: <code>"ox-sessionids-brand:*"</code>
     *
     * @return The pattern
     */
    public String getAllBrandSetKeyPattern() {
        return new StringBuilder(RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_BRAND).append(RedisSessionConstants.DELIMITER).append('*').toString();
    }

    /**
     * Gets the key for the sorted set applicable to given session.
     *
     * @param session The session
     * @return The sort set key
     */
    public String getSortSetKeyForSession(Session session) {
        return session.isStaySignedIn() ? RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_LONG_LIFETIME : RedisSessionConstants.REDIS_SORTEDSET_SESSIONIDS_SHORT_LIFETIME;
    }

    // --------------------------------------------------------- Commands ------------------------------------------------------------------

    /**
     * Gets the session commands using given Redis commands provider.
     *
     * @param commandsProvider The commands provider
     * @return The session commands
     */
    public RedisStringCommands<String, Session> getSessionCommands(RedisCommandsProvider commandsProvider) {
        return getSessionCommands(commandsProvider, getState().getObfuscator(), versionMismatchHandler);
    }

    /**
     * Gets the session commands using given Redis commands provider.
     *
     * @param commandsProvider The commands provider
     * @return The session commands
     */
    public RedisStringCommands<String, Session> getSessionCommands(RedisCommandsProvider commandsProvider, Obfuscator obfuscator, VersionMismatchHandler versionMismatchHandler) {
        return new SessionRedisStringCommands(commandsProvider.getRawStringCommands(), obfuscator, versionMismatchHandler);
    }

    // ------------------------------------------------------- Locking/version stuff -------------------------------------------------------

    /**
     * Acquires the named lock.
     *
     * @param lockKey The key of the lock
     * @param timeoutMillis The lock's time to live in milliseconds
     * @param updateFrequencyMillis The frequency in milliseconds to reset lock's time to live
     * @param commandsProvider The commands provider to use
     * @return The lock instance if acquired; otherwise empty
     * @throws RedisException If lock acquisition fails
     */
    public Optional<RedisLock> acquireLock(String lockKey, long timeoutMillis, long updateFrequencyMillis, RedisCommandsProvider commandsProvider) {
        return RedisLock.lockFor(lockKey, timeoutMillis, updateFrequencyMillis, commandsProvider, this);
    }

    private static final long VERSION_LOCK_TIMEOUT_MILLIS = 30_000L;
    private static final long VERSION_LOCK_UPDATE_MILLIS = 10_000L;

    /**
     * Check if version of session-related stuff held in Redis is equal to expected version.
     * <p>
     * Otherwise version is incremented and all sesssion-related stuff is dropped.
     *
     * @return <code>true</code> if version is all fine; otherwise <code>false</code> to signal version has been updated
     * @throws OXException If version check fails
     */
    private boolean checkVersion() throws OXException {
        Boolean result = connector.<Boolean> executeOperation(commandsProvider -> {
            String sVersion = commandsProvider.getStringCommands().get(RedisSessionConstants.REDIS_SESSION_VERSION);
            if (sVersion != null && RedisSessionConstants.VERSION <= Integer.parseInt(sVersion)) {
                return Boolean.TRUE;
            }

            Optional<RedisLock> optionalLock = acquireLock(RedisSessionConstants.REDIS_SESSION_VERSION_LOCK, VERSION_LOCK_TIMEOUT_MILLIS, VERSION_LOCK_UPDATE_MILLIS, commandsProvider);
            if (optionalLock.isEmpty()) {
                // Lock NOT acquired
                return null;
            }

            // Lock acquired
            return checkVersionElseDrop(commandsProvider, optionalLock.get());
        });

        if (result != null) {
            // There is a result already
            return result.booleanValue();
        }

        // Lock could not be acquired. Await lock availability & proceed
        int retryCount = 0;
        while (result == null) {
            long millisToWait = (++retryCount * VERSION_LOCK_UPDATE_MILLIS) + ((long) (Math.random() * VERSION_LOCK_UPDATE_MILLIS));
            LOG.info("Failed to acquire lock \"{}\". Waiting for {} and then retrying...", RedisSessionConstants.REDIS_SESSION_VERSION_LOCK, toStringObjectFor(() -> exactly(millisToWait, true)));
            long nanosToWait = TimeUnit.NANOSECONDS.convert(millisToWait, TimeUnit.MILLISECONDS);
            LockSupport.parkNanos(nanosToWait);
            result = connector.<Boolean> executeOperation(commandsProvider -> {
                Optional<RedisLock> optionalLock = acquireLock(RedisSessionConstants.REDIS_SESSION_VERSION_LOCK, VERSION_LOCK_TIMEOUT_MILLIS, VERSION_LOCK_UPDATE_MILLIS, commandsProvider);
                if (optionalLock.isEmpty()) {
                    // Lock NOT acquired
                    return null;
                }

                // Lock acquired
                return checkVersionElseDrop(commandsProvider, optionalLock.get());
            });
        }
        return result.booleanValue();
    }

    private Boolean checkVersionElseDrop(RedisCommandsProvider commandsProvider, RedisLock lock) {
        try {
            // Check version again
            String sVersion = commandsProvider.getStringCommands().get(RedisSessionConstants.REDIS_SESSION_VERSION);
            if (sVersion != null && RedisSessionConstants.VERSION <= Integer.parseInt(sVersion)) {
                // Version equal to expected version in the meantime
                return Boolean.TRUE;
            }

            // Collect keys to drop
            List<String> keysToDrop = collectAllSessionReleatedKeys(commandsProvider);

            // Delete all keys related to session
            int numOfKeys = keysToDrop.size();
            if (numOfKeys > 0) {
                commandsProvider.getKeyCommands().del(keysToDrop.toArray(new String[numOfKeys]));
            }

            // Finally, update version
            commandsProvider.getStringCommands().set(RedisSessionConstants.REDIS_SESSION_VERSION, Integer.toString(RedisSessionConstants.VERSION));
        } finally {
            lock.unlock(commandsProvider);
        }
        return Boolean.FALSE;
    }

    private static final Set<String> VERSION_KEYS = Set.of(RedisSessionConstants.REDIS_SESSION_VERSION, RedisSessionConstants.REDIS_SESSION_VERSION_LOCK);

    private static List<String> collectAllSessionReleatedKeys(RedisCommandsProvider commandsProvider) {
        List<String> keysToDrop = null;
        ScanArgs allSessionKeysArgs = ScanArgs.Builder.matches(RedisSessionConstants.REDIS_SESSION + "*").limit(RedisSessionConstants.LIMIT_1000);
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        KeyScanCursor<String> cursor = keyCommands.scan(allSessionKeysArgs);
        while (cursor != null) {
            // Obtain current keys...
            List<String> keys = cursor.getKeys();
            if (!keys.isEmpty()) {
                keys = keys.stream().filter(k -> !VERSION_KEYS.contains(k)).collect(Collectors.toList());
                if (keysToDrop == null) {
                    keysToDrop = new ArrayList<>(keys);
                } else {
                    keysToDrop.addAll(keys);
                }
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, allSessionKeysArgs);
        }
        return keysToDrop == null ? Collections.emptyList() : keysToDrop;
    }

    // ------------------------------------------------------- Counter stuff ---------------------------------------------------------------

    private void incrementCountersFor(Session session) {
        try {
            String brandName = (String) session.getParameter(Session.PARAM_BRAND);
            String brandId = getBrandIdentifierFor(brandName);

            // Register metric
            if (brandId != null) {
                SessionMetricHandler.registerBrandMetricIfAbsent(brandId);
            }

            // Increment counter in Redis
            connector.executeVoidOperation(commandsProvider -> {
                RedisHashCommands<String, String> hashCommands = commandsProvider.getHashCommands();

                hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_TOTAL, 1);
                hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_ACTIVE, 1);
                hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, session.isStaySignedIn() ? RedisSessionConstants.COUNTER_SESSION_LONG : RedisSessionConstants.COUNTER_SESSION_SHORT, 1);

                if (brandId != null) {
                    hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_TOTAL_APPENDIX, 1);
                    hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_ACTIVE_APPENDIX, 1);
                }
            });
        } catch (Exception e) {
            LOG.warn("Failed to increment counters for session {}", session.getSessionID(), e);
        }
    }

    private void decrementCountersFor(Session session) {
        try {
            String brandId = getBrandIdentifierFrom(session);
            connector.executeVoidOperation(commandsProvider -> {
                RedisHashCommands<String, String> hashCommands = commandsProvider.getHashCommands();
                hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_TOTAL, -1);
                hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, session.isStaySignedIn() ? RedisSessionConstants.COUNTER_SESSION_LONG : RedisSessionConstants.COUNTER_SESSION_SHORT, -1);
                if (brandId != null) {
                    hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_TOTAL_APPENDIX, -1);
                }
            });
        } catch (Exception e) {
            LOG.warn("Failed to decrement counters for session {}", session.getSessionID(), e);
        }
    }

    private static final Function<? super String, ? extends Counter> F_NEW_COUNTER = k -> new Counter(0);

    private <S extends Session> void decrementCountersFor(Collection<S> sessions) {
        if (sessions == null) {
            return;
        }

        if (sessions.size() == 1) {
            decrementCountersFor(sessions.iterator().next());
            return;
        }

        try {
            Map<String, Counter> countByField = new HashMap<>();
            for (Session session : sessions) {
                String brandId = getBrandIdentifierFrom(session);
                if (brandId != null) {
                    countByField.computeIfAbsent(brandId + RedisSessionConstants.COUNTER_SESSION_TOTAL_APPENDIX, F_NEW_COUNTER).increment();
                }
                if (session.isStaySignedIn()) {
                    countByField.computeIfAbsent(RedisSessionConstants.COUNTER_SESSION_LONG, F_NEW_COUNTER).increment();
                } else {
                    countByField.computeIfAbsent(RedisSessionConstants.COUNTER_SESSION_SHORT, F_NEW_COUNTER).increment();
                }
            }

            connector.executeVoidOperation(commandsProvider -> {
                RedisHashCommands<String, String> hashCommands = commandsProvider.getHashCommands();
                hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_TOTAL, Math.negateExact(sessions.size()));
                for (Map.Entry<String, Counter> countEntry : countByField.entrySet()) {
                    hashCommands.hincrby(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, countEntry.getKey(), Math.negateExact(countEntry.getValue().getCount()));
                }
            });
        } catch (Exception e) {
            LOG.warn("Failed to decrement counters for sessions {}", sessions, e);
        }
    }

    /**
     * Gets the configured value for max. number of sessions.
     *
     * @return The max. number of sessions
     */
    public int getMaxNumberOfSessions() {
        return getState().getMaxSessions();
    }

    /**
     * Queries the counter for the number of active sessions.
     *
     * @param brandId The brand identifier
     * @return The counter's value for number of active sessions
     * @throws OXException If number of active sessions be returned
     */
    public long queryCounterForNumberOfActiveSessionsForBrand(String brandId) throws OXException {
        return connector.<Long> executeOperation(commandsProvider -> {
            long total = doQueryNumberOfSessionsForBrand(brandId, commandsProvider);
            if (total <= 0) {
                return Long.valueOf(0);
            }

            String sCount = commandsProvider.getHashCommands().hget(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_ACTIVE_APPENDIX);
            long active = sCount == null ? 0L : Long.parseLong(sCount);
            return Long.valueOf(active > total ? total : active);
        }).longValue();
    }

    /**
     * Queries the number of sessions for given brand name.
     *
     * @param brandId The identifier of the brand
     * @return The number of sessions for given brand name
     * @throws OXException If number of sessions for given brand name cannot be returned
     */
    public long queryNumberOfSessionsForBrand(String brandId) throws OXException {
        return connector.<Long> executeOperation(commandsProvider -> Long.valueOf(doQueryNumberOfSessionsForBrand(brandId, commandsProvider))).longValue();
    }

    private long doQueryNumberOfSessionsForBrand(String brandId, RedisCommandsProvider commandsProvider) {
        String sCount = commandsProvider.getHashCommands().hget(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, brandId + RedisSessionConstants.COUNTER_SESSION_TOTAL_APPENDIX);
        return (sCount == null ? 0L : Long.parseLong(sCount));
    }

    /**
     * Queries the counter for the number of active sessions.
     *
     * @return The counter's value for number of active sessions
     * @throws OXException If number of active sessions be returned
     */
    public long queryCounterForNumberOfActiveSessions() throws OXException {
        return connector.<Long> executeOperation(commandsProvider -> {
            long total = doQueryCounterForNumberOfSessions(commandsProvider);
            if (total <= 0) {
                return Long.valueOf(0);
            }

            String sCount = commandsProvider.getHashCommands().hget(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_ACTIVE);
            long active = sCount == null ? 0L : Long.parseLong(sCount);
            return Long.valueOf(active > total ? total : active);
        }).longValue();
    }

    /**
     * Queries the counter for the total number of sessions.
     *
     * @return The counter's value for total number of sessions
     * @throws OXException If total number of sessions cannot be returned
     */
    public long queryCounterForNumberOfSessions() throws OXException {
        return connector.<Long> executeOperation(commandsProvider -> Long.valueOf(doQueryCounterForNumberOfSessions(commandsProvider))).longValue();
    }

    /**
     * Queries the counter for the total number of sessions.
     *
     * @return The counter's value for total number of sessions
     */
    protected long doQueryCounterForNumberOfSessions(RedisCommandsProvider commandsProvider) {
        String sCount = commandsProvider.getHashCommands().hget(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_TOTAL);
        return (sCount == null ? 0 : Long.parseLong(sCount));
    }

    /**
     * Queries the counter for the number of sessions having a short life time.
     *
     * @return The counter's value for number of sessions having a short life time
     * @throws OXException If number of sessions having a short life time cannot be returned
     */
    public long queryCounterForNumberOfShortSessions() throws OXException {
        return connector.<Long> executeOperation(commandsProvider -> {
            String sCount = commandsProvider.getHashCommands().hget(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_SHORT);
            return Long.valueOf(sCount == null ? 0L : Long.parseLong(sCount));
        }).longValue();
    }

    /**
     * Queries the counter for the number of sessions having a long life time.
     *
     * @return The counter's value for number of sessions having a long life time
     * @throws OXException If number of sessions having a long life time cannot be returned
     */
    public long queryCounterForNumberOfLongSessions() throws OXException {
        return connector.<Long> executeOperation(commandsProvider -> {
            String sCount = commandsProvider.getHashCommands().hget(RedisSessionConstants.REDIS_HASH_SESSION_COUNTER, RedisSessionConstants.COUNTER_SESSION_LONG);
            return Long.valueOf(sCount == null ? 0L : Long.parseLong(sCount));
        }).longValue();
    }

    private List<String> getBrandIdsForCounters() {
        ScanArgs scanArgs = ScanArgs.Builder.matches(getAllBrandSetKeyPattern()).limit(RedisSessionConstants.LIMIT_1000);

        try {
            return connector.executeOperation(commandsProvider -> {
                RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
                List<String> brandIds = null;

                KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
                while (cursor != null) {
                    // Obtain current keys...
                    List<String> brandSetKeys = cursor.getKeys();
                    for (String brandSetKey : brandSetKeys) {
                        if (brandIds == null) {
                            brandIds = new ArrayList<>(brandSetKeys.size());
                        }
                        brandIds.add(brandSetKey.substring(brandSetKey.lastIndexOf(':') + 1));
                    }

                    // Move cursor forward
                    cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
                }

                return brandIds == null ? Collections.emptyList() : brandIds;
            });
        } catch (Exception e) {
            LOG.error("Failed to determine brand identifiers from Redis storage", e);
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------- Session stuff ---------------------------------------------------------------

    @Override
    public boolean isCentral() {
        return true;
    }

    @Override
    public Session addSession(AddSessionParameter parameter) throws OXException {
        int userId = parameter.getUserId();
        int contextId = parameter.getContext().getContextId();
        LOG.debug("Adding session with client {} for user {} in context {} ({})", parameter.getClient(), I(userId), I(contextId), parameter.getFullLogin());

        // Obtain current state
        RedisSessiondState state = getState();

        // Various checks
        {
            int maxSessions = state.getMaxSessions();
            UserTypeSessiondConfigInterface userTypeConfig = state.getUserConfigRegistry().getConfigFor(userId, contextId);
            int maxSessPerUser = userTypeConfig.getMaxSessionsPerUserType();
            int maxSessPerClient = services.getServiceSafe(LeanConfigurationService.class).getIntProperty(userId, contextId, RedisSessiondConfigProperty.MAX_SESSIONS_PER_CLIENT);
            LOG.debug("Performing checks for max. number of sessions with client {} for user {} in context {} ({})", parameter.getClient(), I(userId), I(contextId), parameter.getFullLogin());
            connector.executeVoidOperation(commandsProvider -> {
                checkMaxSessions(maxSessions, commandsProvider);
                checkMaxSessPerUser(maxSessPerUser, userId, contextId, commandsProvider);
                checkMaxSessPerClient(maxSessPerClient, parameter.getClient(), userId, contextId, commandsProvider);
                checkAuthId(parameter.getFullLogin(), parameter.getAuthId(), commandsProvider);
            });
        }

        // Create and optionally enhance new session instance
        SessionImpl newSession;
        {
            // Create session instance
            List<SessionEnhancement> enhancements = parameter.getEnhancements();
            if (null == enhancements || enhancements.isEmpty()) {
                newSession = createNewSession(userId, parameter.getUserLoginInfo(), parameter.getPassword(), contextId, parameter.getClientIP(), parameter.getFullLogin(), parameter.getAuthId(), parameter.getHash(), parameter.getClient(), parameter.isStaySignedIn(), parameter.getOrigin());
            } else {
                // Create intermediate SessionDescription instance to offer more flexibility to possible SessionEnhancement implementations
                SessionDescription sessionDescription = createSessionDescription(userId, parameter.getUserLoginInfo(), parameter.getPassword(), contextId, parameter.getClientIP(), parameter.getFullLogin(), parameter.getAuthId(), parameter.getHash(), parameter.getClient(), parameter.isStaySignedIn(), parameter.getOrigin());
                for (SessionEnhancement enhancement: enhancements) {
                    enhancement.enhanceSession(sessionDescription);
                }
                newSession = new SessionImpl(sessionDescription);
            }

            if (Strings.isNotEmpty(parameter.getUserAgent())) {
                newSession.setParameter(Session.PARAM_USER_AGENT, parameter.getUserAgent());
            }

            // Set time stamp
            newSession.setParameter(Session.PARAM_LOGIN_TIME, Long.valueOf(System.currentTimeMillis()));
            LOG.debug("Created new session instance {} with client {} for user {} in context {} ({})", newSession.getSessionID(), parameter.getClient(), I(userId), I(contextId), parameter.getFullLogin());
        }

        // Either add session or yield short-time token for it
        if (null != parameter.getClientToken()) {
            String serverToken = UUIDs.getUnformattedString(UUID.randomUUID());
            newSession.setParameter("serverToken", serverToken);
            TokenSessionContainer.getInstance().addSession(newSession, parameter.getClientToken(), serverToken);
            LOG.debug("Created server token for session {} of user {} in context {}", newSession.getSessionID(), I(userId), I(contextId));
            return newSession;
        }

        // Add session & increment counters
        putSessionIntoRedisAndLocal(newSession, state);
        incrementCountersFor(newSession);

        // Post event for created session
        postSessionCreation(newSession);
        postSessionStored(newSession, false);
        LOG.debug("Completed adding session {} of user {} in context {}", newSession.getSessionID(), I(userId), I(contextId));
        return newSession;
    }

    /**
     * Puts specified session instance into Redis storage and local cache
     *
     * @param sessionToAdd The session to add
     * @param addIfAbsent <code>true</code> to only add session if not already contained; otherwise <code>false</code>
     * @return <code>true</code> if session has been put; otherwise <code>false</code>
     * @throws OXException If operation fails
     */
    public boolean putSessionIntoRedisAndLocal(SessionImpl sessionToAdd, boolean addIfAbsent) throws OXException {
        if (addIfAbsent && connector.executeOperation(commandsProvider -> commandsProvider.getKeyCommands().exists(getSessionKey(sessionToAdd.getSessionID()))).longValue() > 0) {
            // Already contained
            return false;
        }

        // Do add session
        putSessionIntoRedisAndLocal(sessionToAdd, getState());
        return true;
    }

    /**
     * Puts specified session instance into Redis storage and local cache
     *
     * @param sessionToAdd The session to add
     * @param state The current state
     * @throws OXException If adding session fails
     */
    private void putSessionIntoRedisAndLocal(SessionImpl sessionToAdd, RedisSessiondState state) throws OXException {
        // Add to Redis
        putSessionIntoRedis(sessionToAdd, state);

        // Add to local cache
        try {
            // Pre-set last-checked time stamp if threshold is enabled to avoid superfluous EXISTS command for newly added session
            if (state.getCheckExistenceThreshold() > 0) {
                sessionToAdd.setLastChecked(System.currentTimeMillis());
            }
            state.getLocalSessionCache().put(sessionToAdd, true);
            LOG.debug("Put session {} into local cache for user {} in context {}", sessionToAdd.getSessionID(), I(sessionToAdd.getUserId()), I(sessionToAdd.getContextId()));
        } catch (Exception e) {
            LOG.warn("Failed to put session {} into local cache for user {} in context {}", sessionToAdd.getSessionID(), I(sessionToAdd.getUserId()), I(sessionToAdd.getContextId()), e);
        }
    }

    private void putSessionIntoRedis(SessionImpl sessionToAdd, RedisSessiondState state) throws OXException {
        // Invoke interceptors...
        SessionImpl modifiedSession = null;
        for (SessionSerializationInterceptor interceptor : serializationInterceptors) {
            if (modifiedSession == null) {
                // Only copy if needed
                modifiedSession = new SessionImpl(sessionToAdd);
            }
            interceptor.serialize(modifiedSession);
        }

        SessionImpl newSession = modifiedSession != null ? modifiedSession : sessionToAdd;
        byte[] sessionBytes = generateBytesFromSession(newSession, state);

        connector.executeVoidOperation(commandsProvider -> {
            RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
            RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
            RedisStringCommands<String, String> stringCommands = commandsProvider.getStringCommands();
            RedisStringCommands<String, InputStream> rawStringCommands = commandsProvider.getRawStringCommands();

            addToRedisCollections(newSession, sessionBytes, false, state, rawStringCommands, sortedSetCommands, setCommands, stringCommands);
        });

        RedisConnectorService connectorService = services.getOptionalService(RedisConnectorService.class);
        if (connectorService != null) {
            List<RedisConnectorProvider> remoteConnectorProviders = connectorService.getRemoteConnectorProviders();
            if (!remoteConnectorProviders.isEmpty()) {
                for (RedisConnectorProvider connectorProvider : remoteConnectorProviders) {
                    connectorProvider.getConnector().executeVoidOperation(commandsProvider -> {
                        RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
                        RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                        RedisStringCommands<String, String> stringCommands = commandsProvider.getStringCommands();
                        RedisStringCommands<String, InputStream> rawStringCommands = commandsProvider.getRawStringCommands();

                        addToRedisCollections(newSession, sessionBytes, true, state, rawStringCommands, sortedSetCommands, setCommands, stringCommands);
                    });
                }
            }
        }
    }

    private static byte[] generateBytesFromSession(SessionImpl session, RedisSessiondState state) throws OXException {
        try {
            return SessionCodec.session2Json(session, state.getObfuscator(), RedisSessionVersionService.getInstance()).toByteArray();
        } catch (JSONException e) {
            throw RedisExceptionCode.JSON_ERROR.create(e, e.getMessage());
        }
    }

    private void addToRedisCollections(SessionImpl newSession, byte[] sessionBytes, boolean remote, RedisSessiondState state, RedisStringCommands<String, InputStream> rawStringCommands, RedisSortedSetCommands<String, String> sortedSetCommands, RedisSetCommands<String, String> setCommands, RedisStringCommands<String, String> stringCommands) {
        // Add to main Redis collection
        rawStringCommands.set(getSessionKey(newSession.getSessionID()), Streams.newByteArrayInputStream(sessionBytes));

        // Add to Redis set and mapping tables
        long now = System.currentTimeMillis();
        sortedSetCommands.zadd(getSortSetKeyForSession(newSession), now, newSession.getSessionID());
        setCommands.sadd(getSetKeyForUser(newSession.getUserId(), newSession.getContextId()), newSession.getSessionID());
        if (newSession.getAlternativeId() != null) {
            stringCommands.set(getSessionAlternativeKey(newSession.getAlternativeId()), newSession.getSessionID());
        }
        if (newSession.getAuthId() != null) {
            stringCommands.set(getSessionAuthIdKey(newSession.getAuthId()), newSession.getSessionID());
        }
        String brandId = getBrandIdentifierFrom(newSession);
        if (brandId != null) {
            sortedSetCommands.zadd(getSetKeyForBrand(brandId), now, newSession.getSessionID());
        }
        LOG.debug("Put session {} into {}Redis storage for user {} in context {}:{}{}", newSession.getSessionID(), remote ? "remote " : "", I(newSession.getUserId()), I(newSession.getContextId()), Strings.getLineSeparator(), prettyPrinterFor(newSession, state));
    }

    /**
     * Creates a new instance of {@code SessionImpl} from specified arguments
     *
     * @param userId The user identifier
     * @param loginName The login name
     * @param password The password
     * @param contextId The context identifier
     * @param clientHost The client host name or IP address
     * @param login The login; e.g. <code>"someone@invalid.com"</code>
     * @param authId The authentication identifier
     * @param hash The hash string
     * @param client The client identifier
     * @param staySignedIn Whether session is supposed to be annotated with "stay signed in"; otherwise <code>false</code>
     * @return The newly created {@code SessionImpl} instance
     */
    private static SessionImpl createNewSession(int userId, String loginName, String password, int contextId, String clientHost, String login, String authId, String hash, String client, boolean staySignedIn, Origin origin) {
        // Generate identifier, secret, and random
        String sessionId = UUIDs.getUnformattedString(UUID.randomUUID());
        String secret = UUIDs.getUnformattedString(UUID.randomUUID());
        String randomToken = UUIDs.getUnformattedString(UUID.randomUUID());

        // Create & return the instance
        return new SessionImpl(userId, loginName, password, contextId, sessionId, secret, randomToken, clientHost, login, authId, hash, client, staySignedIn, origin);
    }

    /**
     * Creates a new instance of {@code SessionDescription} from specified arguments
     *
     * @param userId The user identifier
     * @param loginName The login name
     * @param password The password
     * @param contextId The context identifier
     * @param clientHost The client host name or IP address
     * @param login The login; e.g. <code>"someone@invalid.com"</code>
     * @param authId The authentication identifier
     * @param hash The hash string
     * @param client The client identifier
     * @param staySignedIn Whether session is supposed to be annotated with "stay signed in"; otherwise <code>false</code>
     * @return The newly created {@code SessionDescription} instance
     */
    private static SessionDescription createSessionDescription(int userId, String loginName, String password, int contextId, String clientHost, String login, String authId, String hash, String client, boolean staySignedIn, Origin origin) {
        // Generate identifier, secret, and random
        String sessionId = UUIDs.getUnformattedString(UUID.randomUUID());
        String secret = UUIDs.getUnformattedString(UUID.randomUUID());
        String randomToken = UUIDs.getUnformattedString(UUID.randomUUID());

        // Create instance
        SessionDescription newSession = new SessionDescription(userId, contextId, login, password, sessionId, secret, UUIDs.getUnformattedString(UUID.randomUUID()), origin);
        newSession.setLoginName(loginName);
        newSession.setLocalIp(clientHost);
        newSession.setAuthId(authId);
        newSession.setStaySignedIn(staySignedIn);
        newSession.setClient(client);
        newSession.setRandomToken(randomToken);
        newSession.setHash(hash);
        return newSession;
    }

    private void checkMaxSessions(int maxSessions, RedisCommandsProvider commandsProvider) throws OXException {
        if (maxSessions <= 0) {
            return;
        }

        long numSessions = doQueryCounterForNumberOfSessions(commandsProvider);
        if (numSessions + 1 > maxSessions) {
            LOG.debug("Max. number of sessions ({}) exceeded. Denying adding session...", I(maxSessions));
            throw SessionExceptionCodes.MAX_SESSION_EXCEPTION.create();
        }
    }

    private void checkMaxSessPerUser(int maxSessPerUser, int userId, int contextId, RedisCommandsProvider commandsProvider) throws OXException {
        if (maxSessPerUser <= 0) {
            return;
        }

        Long numberOfSessions = commandsProvider.getSetCommands().scard(getSetKeyForUser(userId, contextId));
        if (numberOfSessions != null && numberOfSessions.longValue() >= maxSessPerUser) {
            LOG.debug("Max. number of sessions ({}) exceeded user {} in context {}. Denying adding session...", I(maxSessPerUser), I(userId), I(contextId));
            throw SessionExceptionCodes.MAX_SESSION_PER_USER_EXCEPTION.create(I(userId), I(contextId));
        }
    }

    private void checkMaxSessPerClient(int maxSessPerClient, String client, int userId, int contextId, RedisCommandsProvider commandsProvider) throws OXException {
        if (null == client || maxSessPerClient <= 0) {
            return;
        }

        RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);
        RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
        Set<String> sessionIds = setCommands.smembers(getSetKeyForUser(userId, contextId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }

        int count = 0;
        List<String> sessionKeys = sessionIds.stream().map(this::getSessionKey).collect(Collectors.toList());
        for (KeyValue<String, Session> keyValue : sessionCommands.mget(sessionKeys.toArray(new String[sessionKeys.size()]))) {
            String sessionId = keyValue.getKey().substring(keyValue.getKey().lastIndexOf(':') + 1);
            Session session = keyValue.hasValue() ? keyValue.getValue() : null;
            if (session == null) {
                setCommands.srem(getSetKeyForUser(userId, contextId), sessionId);
            } else if (client.equals(session.getClient()) && ++count > maxSessPerClient) {
                LOG.debug("Max. number of sessions ({}) exceeded for client {} of user {} in context {}. Denying adding session...", I(maxSessPerClient), client, I(userId), I(contextId));
                throw SessionExceptionCodes.MAX_SESSION_PER_CLIENT_EXCEPTION.create(client, I(userId), I(contextId));
            }
        }
    }

    private void checkAuthId(String login, String authId, RedisCommandsProvider commandsProvider) throws OXException {
        String sessionId = commandsProvider.getStringCommands().get(getSessionAuthIdKey(authId));
        if (sessionId != null) {
            // There is a session identifier associated with given authentication identifier
            Session session = getSessionCommands(commandsProvider).get(getSessionKey(sessionId));
            if (session != null) {
                throw SessionExceptionCodes.DUPLICATE_AUTHID.create(session.getLogin(), login);
            }
            // No such session
            commandsProvider.getKeyCommands().del(getSessionAuthIdKey(authId));
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean storeSession(String sessionId) throws OXException {
        return storeSession(sessionId, false);
    }

    @Override
    public boolean storeSession(String sessionId, boolean addIfAbsent) throws OXException {
        LOG.debug("Storing session by {}", sessionId);
        RedisSessiondState state = getState();

        // Only "store" a session when there is a local session instance, which might hold changes that need to be added to Redis storage
        // Check local cache
        LOG.debug("Look-up in local cache for {}", sessionId);
        SessionImpl session = state.getLocalSessionCache().getSessionByIdIfPresent(sessionId);
        if (session == null) {
            // As already available in Redis storage the session can be considered as "stored" as long as session exists
            return connector.executeOperation(newExistsOperationKey(sessionId), commandsProvider -> commandsProvider.getKeyCommands().exists(getSessionKey(sessionId))).longValue() > 0;
        }

        // Ensure existence in Redis storage
        LOG.debug("Obtained session {} from local cache", sessionId);
        session = ensureExistenceElseNull(session, state);
        if (session == null) {
            return false;
        }

        // Replace session in Redis storage
        replaceSession(session, state);
        return true;
    }

    @Override
    public boolean storeSession(Session session, boolean addIfAbsent) throws OXException {
        if (session == null) {
            return false;
        }

        LOG.debug("Storing session instance for {}", session.getSessionID());
        if (connector.<Long> executeOperation(newExistsOperationKey(session.getSessionID()), commandsProvider -> commandsProvider.getKeyCommands().exists(getSessionKey(session.getSessionID()))).longValue() <= 0) {
            LOG.debug("Denied storing session for {}: No such session in Redis storage.", session.getSessionID());
        }

        // Replace session in Redis storage
        replaceSession(newSessionImplFor(session), getState());
        return true;
    }

    private void replaceSession(SessionImpl sessionToStore, RedisSessiondState state) throws OXException {
        // Do replace session
        putSessionIntoRedisAndLocal(sessionToStore, state);

        // Post event for "stored" session
        postSessionStored(sessionToStore, true);
        LOG.debug("Completed storing session {} of user {} in context {}", sessionToStore.getSessionID(), I(sessionToStore.getUserId()), I(sessionToStore.getContextId()));
    }

    @Override
    public void changeSessionPassword(String sessionId, String newPassword) throws OXException {
        RedisSessiondState state = getState();
        SessionImpl session = state.getLocalSessionCache().getSessionByIdIfPresent(sessionId);
        if (session != null) {
            session.setPassword(newPassword);
        }

        List<String> sessionIdsToRemove = connector.executeOperation(commandsProvider -> {
            Session redisSession = getSessionCommands(commandsProvider).get(getSessionKey(sessionId));
            if (redisSession instanceof ClusterSession newSession) {
                newSession.setPassword(newPassword);
                RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
                long now = System.currentTimeMillis();
                sortedSetCommands.zadd(getSortSetKeyForSession(newSession), now, newSession.getSessionID());
                String brandId = getBrandIdentifierFrom(newSession);
                if (brandId != null) {
                    sortedSetCommands.zadd(getSetKeyForBrand(brandId), now, newSession.getSessionID());
                }

                // Invalidate all other user sessions
                Set<String> sessionIds = commandsProvider.getSetCommands().smembers(getSetKeyForUser(redisSession.getUserId(), redisSession.getContextId()));
                List<String> toRemove = new ArrayList<>(sessionIds.size());
                for (String sessId : sessionIds) {
                    if (isNotEqual(sessionId, sessId)) {
                        toRemove.add(sessId);
                    }
                }
                return toRemove;
            }
            return Collections.emptyList();
        });

        // Publish INVALIDATE event for changed session manually
        publishInvalidateEventQuietly(sessionId);

        removeSessions(sessionIdsToRemove);
    }

    private static boolean isNotEqual(String sessionId, String otherSessionId) {
        return !isEqual(sessionId, otherSessionId);
    }

    private static boolean isEqual(String sessionId, String otherSessionId) {
        if (sessionId == null) {
            if (otherSessionId != null) {
                return false;
            }
        } else if (!sessionId.equals(otherSessionId)) {
            return false;
        }
        return true;
    }

    @Override
    public void setSessionAttributes(String sessionId, SessionAttributes attrs) throws OXException {
        RedisSessiondState state = getState();
        SessionImpl session = state.getLocalSessionCache().getSessionByIdIfPresent(sessionId);
        if (session != null) {
            if (attrs.getLocalIp().isSet()) {
                session.setLocalIp(attrs.getLocalIp().get(), false);
            }
            if (attrs.getClient().isSet()) {
                session.setClient(attrs.getClient().get(), false);
            }
            if (attrs.getHash().isSet()) {
                session.setHash(attrs.getHash().get(), false);
            }
            if (attrs.getUserAgent().isSet()) {
                session.setParameter(Session.PARAM_USER_AGENT, attrs.getUserAgent().get());
            }
        }

        connector.executeVoidOperation(commandsProvider -> {
            RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);
            Session session1 = sessionCommands.get(getSessionKey(sessionId));
            if (session1 instanceof ClusterSession newSession) {
                if (attrs.getLocalIp().isSet()) {
                    newSession.setLocalIp(attrs.getLocalIp().get());
                }
                if (attrs.getClient().isSet()) {
                    newSession.setClient(attrs.getClient().get());
                }
                if (attrs.getHash().isSet()) {
                    newSession.setHash(attrs.getHash().get());
                }
                if (attrs.getUserAgent().isSet()) {
                    session1.setParameter(Session.PARAM_USER_AGENT, attrs.getUserAgent().get());
                }
                RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
                long now = System.currentTimeMillis();
                sortedSetCommands.zadd(getSortSetKeyForSession(newSession), now, newSession.getSessionID());
                String brandId = getBrandIdentifierFrom(newSession);
                if (brandId != null) {
                    sortedSetCommands.zadd(getSetKeyForBrand(brandId), now, newSession.getSessionID());
                }
            }
        });

        // Publish INVALIDATE event for changed session manually
        publishInvalidateEventQuietly(sessionId);
    }

    @Override
    public boolean removeSession(final String sessionId) {
        return removeSessions(Collections.singletonList(sessionId)) > 0;
    }

    /**
     * Removes the sessions associated with given identifiers.
     *
     * @param sessionIds The session identifiers
     * @param removeFromSortedSet Whether to remove from sorted set as well
     * @return The number of actually removed sessions
     */
    public int removeSessions(Collection<String> sessionIds) {
        return removeSessions(sessionIds, true, true);
    }

    /**
     * Removes the sessions associated with given identifiers.
     *
     * @param sessionIds The session identifiers
     * @param removeFromSortedSet Whether to remove from sorted set as well
     * @param replayToRemote Whether to replay Redis operation ro remote ones
     * @return The number of actually removed sessions
     */
    public int removeSessions(Collection<String> sessionIds, boolean removeFromSortedSet, boolean replayToRemote) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return 0;
        }

        LOG.debug("Removing session(s) {}", sessionIds);

        ThreadPools.submitElseExecute(ThreadPools.task(() -> {
            List<SessionImpl> removedSessions = getState().getLocalSessionCache().removeAndGetSessionsByIds(sessionIds);
            if (removedSessions.size() == 1) {
                postSessionRemoval(removedSessions.get(0));
            } else {
                postContainerRemoval(removedSessions);
            }
            LOG.debug("Removed session(s) {} from local cache", sessionIds);
        }));

        try {
            List<Session> removedOnes = connector.<List<Session>> executeOperation(commandsProvider -> {
                RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);

                List<Session> removedSessions = null;
                for (String sessionId : sessionIds) {
                    Session removedSession = sessionCommands.getdel(getSessionKey(sessionId));
                    if (removedSession == null) {
                        LOG.debug("No such session {} in Redis storage. Hence, nothing to remove.", sessionId);
                    } else {
                        if (removedSessions == null) {
                            removedSessions = new ArrayList<>(sessionIds.size());
                        }
                        removedSessions.add(removedSession);
                    }
                }

                if (removedSessions == null) {
                    return Collections.emptyList();
                }

                RemovalCollection removalCollection = new RemovalCollection();
                for (Session removedSession : removedSessions) {
                    removalCollection.addSession(removedSession, true, removeFromSortedSet, this);
                }
                removalCollection.removeCollected(commandsProvider);

                LOG.debug("Removed session(s) {} from Redis storage", sessionIds);

                return removedSessions;
            });

            int numRemoved = removedOnes.size();
            if (numRemoved > 0) {
                if (numRemoved == 1) {
                    decrementCountersFor(removedOnes.get(0));
                } else {
                    decrementCountersFor(removedOnes);
                }
            }

            if (replayToRemote) {
                RedisConnectorService connectorService = services.getOptionalService(RedisConnectorService.class);
                if (connectorService != null) {
                    List<RedisConnectorProvider> remoteConnectorProviders = connectorService.getRemoteConnectorProviders();
                    if (!remoteConnectorProviders.isEmpty()) {
                        Obfuscator obfuscator = getState().getObfuscator();
                        ExceptionCatchingRunnable task = () -> {
                            VersionMismatchHandler versionMismatchHandler = DO_NOTHING_VERSION_MISMATCH_HANDLER;
                            for (RedisConnectorProvider connectorProvider : remoteConnectorProviders) {
                                connectorProvider.getConnector().executeVoidOperation(commandsProvider -> {
                                    RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider, obfuscator, versionMismatchHandler);

                                    List<Session> removedSessions = null;
                                    for (String sessionId : sessionIds) {
                                        Session removedSession = sessionCommands.getdel(getSessionKey(sessionId));
                                        if (removedSession == null) {
                                            LOG.debug("No such session {} in remote Redis storage. Hence, nothing to remove.", sessionId);
                                        } else {
                                            if (removedSessions == null) {
                                                removedSessions = new ArrayList<>(sessionIds.size());
                                            }
                                            removedSessions.add(removedSession);
                                        }
                                    }

                                    if (removedSessions == null) {
                                        return;
                                    }

                                    RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
                                    RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                                    RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
                                    RemovalCollection removalCollection = new RemovalCollection();
                                    for (Session removedSession : removedSessions) {
                                        removalCollection.addSession(removedSession, true, removeFromSortedSet, this);
                                    }
                                    removalCollection.removeCollected(keyCommands, setCommands, sortedSetCommands);

                                    LOG.debug("Removed session(s) {} from remote Redis storage", sessionIds);
                                });
                            }
                        };
                        ThreadPools.submitElseExecute(ThreadPools.task(task));
                    }
                }
            }

            return numRemoved;
        } catch (Exception e) {
            LOG.warn("Failed to remove session from Redis session storage", e);
            return 0;
        }
    }

    @Override
    public int removeUserSessions(final int userId, final Context ctx) {
        if (ctx == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        try {
            return removeAndReturnUserSessions(userId, ctx.getContextId(), true).size();
        } catch (OXException e) {
            LOG.warn("Failed to remove sessions for user {} in context {} from Redis session storage", I(userId), I(ctx.getContextId()), e);
            return 0;
        }
    }

    /**
     * Removes the sessions associated with given user from local cache storage.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The number of removed sessions
     */
    public void removeLocalUserSessions(int userId, int contextId) {
        List<SessionImpl> removedSessions = getState().getLocalSessionCache().removeAndGetSessionsByUser(userId, contextId);
        if (removedSessions.size() == 1) {
            postSessionRemoval(removedSessions.get(0));
        } else {
            postContainerRemoval(removedSessions);
        }
        LOG.debug("Removed sessions from local cache for user {} in context {}", I(userId), I(contextId));
    }

    /**
     * Removes the sessions associated with given user from Redis session storage.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param replayToRemote Whether to replay Redis operations to remote ones
     * @return The removed sessions
     * @throws OXException If a Redis error occurs
     */
    public List<Session> removeAndReturnUserSessions(int userId, int contextId, boolean replayToRemote) throws OXException {
        LOG.debug("Removing sessions for user {} in context {}", I(userId), I(contextId));

        ThreadPools.submitElseExecute(ThreadPools.task(() -> {
            List<SessionImpl> removedSessions = getState().getLocalSessionCache().removeAndGetSessionsByUser(userId, contextId);
            if (removedSessions.size() == 1) {
                postSessionRemoval(removedSessions.get(0));
            } else {
                postContainerRemoval(removedSessions);
            }
            LOG.debug("Removed sessions from local cache for user {} in context {}", I(userId), I(contextId));
        }));

        List<Session> removedFromRedis = connector.executeOperation(commandsProvider -> {
            Set<String> sessionIds = commandsProvider.getSetCommands().smembers(getSetKeyForUser(userId, contextId));
            if (sessionIds == null || sessionIds.isEmpty()) {
                LOG.debug("No such sessions in Redis storage for user {} in context {}", I(userId), I(contextId));
                return Collections.emptyList();
            }

            RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);
            List<Session> removedSessions = new ArrayList<>();
            RemovalCollection removalCollection = new RemovalCollection();
            for (String sessionId : sessionIds) {
                Session removedSession = sessionCommands.getdel(getSessionKey(sessionId));
                if (removedSession != null) {
                    LOG.debug("Removed session {} from Redis storage for user {} in context {}", sessionId, I(removedSession.getUserId()), I(removedSession.getContextId()));
                    removalCollection.addSession(removedSession, false, true, this);
                    removedSessions.add(removedSession);
                }
            }
            removalCollection.addKey(getSetKeyForUser(userId, contextId));
            removalCollection.removeCollected(commandsProvider);
            LOG.debug("Removed sessions from Redis storage for user {} in context {}", I(userId), I(contextId));

            decrementCountersFor(removedSessions);
            return removedSessions;
        });

        if (replayToRemote) {
            RedisConnectorService connectorService = services.getOptionalService(RedisConnectorService.class);
            if (connectorService != null) {
                List<RedisConnectorProvider> remoteConnectorProviders = connectorService.getRemoteConnectorProviders();
                if (!remoteConnectorProviders.isEmpty()) {
                    Obfuscator obfuscator = getState().getObfuscator();
                    ExceptionCatchingRunnable task = () -> {
                        VersionMismatchHandler versionMismatchHandler = DO_NOTHING_VERSION_MISMATCH_HANDLER;
                        for (RedisConnectorProvider connectorProvider : remoteConnectorProviders) {
                            connectorProvider.getConnector().executeVoidOperation(commandsProvider -> {
                                RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                                RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider, obfuscator, versionMismatchHandler);

                                Set<String> sessionIds = setCommands.smembers(getSetKeyForUser(userId, contextId));
                                if (sessionIds == null || sessionIds.isEmpty()) {
                                    LOG.debug("No such sessions in remote Redis storage for user {} in context {}", I(userId), I(contextId));
                                    return;
                                }

                                RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
                                RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
                                RemovalCollection removalCollection = new RemovalCollection();
                                for (String sessionId : sessionIds) {
                                    Session removedSession = sessionCommands.getdel(getSessionKey(sessionId));
                                    if (removedSession != null) {
                                        LOG.debug("Removed session {} from remote Redis storage for user {} in context {}", sessionId, I(removedSession.getUserId()), I(removedSession.getContextId()));
                                        removalCollection.addSession(removedSession, false, true, this);
                                    }
                                }
                                removalCollection.addKey(getSetKeyForUser(userId, contextId));
                                removalCollection.removeCollected(keyCommands, setCommands, sortedSetCommands);
                                LOG.debug("Removed sessions from remote Redis storage for user {} in context {}", I(userId), I(contextId));
                            });
                        }
                    };

                    ThreadPools.submitElseExecute(ThreadPools.task(task));
                }
            }
        }

        return removedFromRedis;
    }

    @Override
    public void removeUserSessionsGlobally(int userId, int contextId) throws OXException {
        removeAndReturnUserSessions(userId, contextId, true);
    }

    @Override
    public boolean hasForContext(int contextId) {
        if (getState().getLocalSessionCache().hasForContext(contextId)) {
            return true;
        }

        ScanArgs scanArgs = ScanArgs.Builder.matches(getSetKeyPattern(contextId)).limit(RedisSessionConstants.LIMIT_1000);
        try {
            return connector.executeOperation(commandsProvider -> {
                KeyScanCursor<String> cursor = commandsProvider.getKeyCommands().scan(scanArgs);
                while (cursor != null) {
                    // Check current keys
                    if (!cursor.getKeys().isEmpty()) {
                        return Boolean.TRUE;
                    }

                    // Move cursor forward
                    cursor = cursor.isFinished() ? null : commandsProvider.getKeyCommands().scan(cursor, scanArgs);
                }
                return Boolean.FALSE;
            }).booleanValue();
        } catch (Exception e) {
            LOG.warn("Failed to check sessions for context {} from Redis session storage", I(contextId), e);
            return false;
        }
    }

    @Override
    public void removeContextSessionsGlobal(Set<Integer> contextIds) throws OXException {
        // Drop local sessions for given context identifiers
        ThreadPools.submitElseExecute(ThreadPools.task(() -> {
            List<SessionImpl> removedSessions = getState().getLocalSessionCache().removeAndGetSessionsByContexts(contextIds);
            if (removedSessions.size() == 1) {
                postSessionRemoval(removedSessions.get(0));
            } else {
                postContainerRemoval(removedSessions);
            }
            LOG.debug("Removed sessions from local cache for contexts {}", contextIds);
        }));

        // Drop sessions from Redis
        for (Integer contextId : contextIds) {
            try {
                removeSessionsBy(ScanArgs.Builder.matches(getSetKeyPattern(contextId.intValue())).limit(RedisSessionConstants.LIMIT_1000), Optional.empty(), true);
            } catch (Exception e) {
                LOG.warn("Failed to remove sessions for context {} from Redis session storage", contextId, e);
            }
        }
    }

    @Override
    public void removeContextSessions(final int contextId) {
        LOG.debug("Removing sessions for context {}", I(contextId));

        // Drop local sessions for given context identifier
        ThreadPools.submitElseExecute(ThreadPools.task(() -> {
            List<SessionImpl> removedSessions = getState().getLocalSessionCache().removeAndGetSessionsByContext(contextId);
            if (removedSessions.size() == 1) {
                postSessionRemoval(removedSessions.get(0));
            } else {
                postContainerRemoval(removedSessions);
            }
            LOG.debug("Removed sessions from local cache for context {}", I(contextId));
        }));

        // Drop sessions from Redis
        try {
            removeSessionsBy(ScanArgs.Builder.matches(getSetKeyPattern(contextId)).limit(RedisSessionConstants.LIMIT_1000), Optional.empty(), true);
            LOG.debug("Removed sessions from Redis storage for context {}", I(contextId));
        } catch (Exception e) {
            LOG.warn("Failed to remove sessions for context {} from Redis session storage", I(contextId), e);
        }
    }

    /**
     * Removes all sessions from Redis session storage.
     */
    public void removeAllSessions() {
        LOG.debug("Removing all sessions");

        // Drop all local sessions
        ThreadPools.submitElseExecute(ThreadPools.task(() -> {
            List<SessionImpl> removedSessions = getState().getLocalSessionCache().invalidateAndGetAll();
            if (removedSessions.size() == 1) {
                postSessionRemoval(removedSessions.get(0));
            } else {
                postContainerRemoval(removedSessions);
            }
            LOG.debug("Removed all sessions from local cache");
        }));

        // Drop sessions from Redis
        try {
            removeSessionsBy(ScanArgs.Builder.matches(new StringBuilder(RedisSessionConstants.REDIS_SET_SESSIONIDS).append(RedisSessionConstants.DELIMITER).append('*').toString()).limit(RedisSessionConstants.LIMIT_1000), Optional.of(ScanArgs.Builder.matches(getAllSessionsPattern()).limit(RedisSessionConstants.LIMIT_1000)), true);
            LOG.debug("Removed all sessions from Redis storage");
        } catch (Exception e) {
            LOG.warn("Failed to remove all sessions from Redis session storage", e);
        }
    }

    private void removeSessionsBy(ScanArgs sessionSetArgs, Optional<ScanArgs> optSessionArgs, boolean replayToRemote) throws OXException {
        connector.executeVoidOperation(commandsProvider -> {
            RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
            RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);
            RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
            RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();

            List<Session> removedSessions = removeSessionsFromRedis(sessionSetArgs, optSessionArgs, setCommands, sessionCommands, keyCommands, sortedSetCommands);
            if (removedSessions != null) {
                decrementCountersFor(removedSessions);
            }
        });

        if (replayToRemote) {
            RedisConnectorService connectorService = services.getOptionalService(RedisConnectorService.class);
            if (connectorService != null) {
                List<RedisConnectorProvider> remoteConnectorProviders = connectorService.getRemoteConnectorProviders();
                if (!remoteConnectorProviders.isEmpty()) {
                    Obfuscator obfuscator = getState().getObfuscator();
                    ExceptionCatchingRunnable task = () -> {
                        VersionMismatchHandler versionMismatchHandler = DO_NOTHING_VERSION_MISMATCH_HANDLER;
                        for (RedisConnectorProvider connectorProvider : remoteConnectorProviders) {
                            connectorProvider.getConnector().executeVoidOperation(commandsProvider -> {
                                RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                                RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider, obfuscator, versionMismatchHandler);
                                RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
                                RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();

                                removeSessionsFromRedis(sessionSetArgs, optSessionArgs, setCommands, sessionCommands, keyCommands, sortedSetCommands);
                            });
                        }
                    };

                    ThreadPools.submitElseExecute(ThreadPools.task(task));
                }
            }
        }
    }

    private List<Session> removeSessionsFromRedis(ScanArgs sessionSetArgs, Optional<ScanArgs> optSessionArgs, RedisSetCommands<String, String> setCommands, RedisStringCommands<String, Session> sessionCommands, RedisKeyCommands<String, InputStream> keyCommands, RedisSortedSetCommands<String, String> sortedSetCommands) {
        List<Session> removedSessions = null;
        RemovalCollection removalCollection = new RemovalCollection();
        KeyScanCursor<String> cursor = keyCommands.scan(sessionSetArgs);
        while (cursor != null) {
            // Obtain current keys...
            List<String> keys = cursor.getKeys();
            int numberOfKeys = keys.size();
            if (numberOfKeys > 0) {
                removalCollection.addKeys(keys);
                // Iterate set identifier
                for (String setKey : keys) {
                    // Get set's members (list of session identifiers)
                    Set<String> sessionIds = setCommands.smembers(setKey);
                    if (sessionIds != null && !sessionIds.isEmpty()) {
                        for (String sessionId : sessionIds) {
                            Session removedSession = sessionCommands.getdel(getSessionKey(sessionId));
                            if (removedSession != null) {
                                LOG.debug("Removed session {} from Redis storage for user {} in context {}", sessionId, I(removedSession.getUserId()), I(removedSession.getContextId()));
                                removalCollection.addSession(removedSession, false, true, this);
                                if (removedSessions == null) {
                                    removedSessions = new ArrayList<>();
                                }
                                removedSessions.add(removedSession);
                            }
                        }
                    }
                }
                keyCommands.del(keys.toArray(new String[numberOfKeys]));
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, sessionSetArgs);
        }
        removalCollection.removeCollected(keyCommands, setCommands, sortedSetCommands);

        // Check for fall-back SCAN argument for session keys
        if (optSessionArgs.isPresent()) {
            ScanArgs sessionArgs = optSessionArgs.get();
            List<String> sessionKeys = null;
            cursor = keyCommands.scan(sessionArgs);
            while (cursor != null) {
                // Obtain current keys...
                List<String> keys = cursor.getKeys();
                if (!keys.isEmpty()) {
                    // Add session identifiers
                    if (sessionKeys == null) {
                        sessionKeys = new ArrayList<>(keys);
                    } else {
                        sessionKeys.addAll(keys);
                    }
                }

                // Move cursor forward
                cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, sessionArgs);
            }

            if (sessionKeys != null) {
                removalCollection.reset();
                for (String sessionKey : sessionKeys) {
                    Session removedSession = sessionCommands.getdel(sessionKey);
                    if (removedSession != null) {
                        LOG.debug("Removed session {} from Redis storage for user {} in context {}", removedSession.getSessionID(), I(removedSession.getUserId()), I(removedSession.getContextId()));
                        removalCollection.addSession(removedSession, true, true, this);
                        if (removedSessions == null) {
                            removedSessions = new ArrayList<>();
                        }
                        removedSessions.add(removedSession);
                    }
                }
                removalCollection.removeCollected(keyCommands, setCommands, sortedSetCommands);
            }
        }
        return removedSessions;
    }


    @Override
    public Session getSession(String sessionId) {
        return getSession(sessionId, true);
    }

    @Override
    public Session getSession(String sessionId, boolean considerSessionStorage) {
        return getSession(sessionId, considerSessionStorage, true);
    }

    @Override
    public Session getSession(String sessionId, boolean considerSessionStorage, boolean considerLocalStorage) {
        return doGetSession(newSessionId(sessionId), false, considerLocalStorage);
    }

    @Override
    public Session peekSession(String sessionId, boolean considerSessionStorage) {
        return peekSession(sessionId);
    }

    @Override
    public Session peekSession(String sessionId) {
        return doGetSession(newSessionId(sessionId), true, true);
    }

    private Session doGetSession(SessionId sessionId, boolean peek, boolean considerLocalStorage) {
        LOG.debug("Getting session by {}", sessionId);
        RedisSessiondState state = getState();
        LocalSessionCache localSessionCache = state.getLocalSessionCache();

        // Check local cache first
        LOG.debug("Look-up in local cache for {}", sessionId);
        SessionImpl session = considerLocalStorage ? localSessionCache.getSessionByIdIfPresent(sessionId) : null;
        if (session != null) {
            // Ensure existence if locally fetched
            LOG.debug("Obtained session {} from local cache for {}", session.getSessionID(), sessionId);
            return ensureExistenceElseNull(session, state);
        }

        // Need to look-up in Redis session storage while optionally holding appropriate lock to avoid concurrent session look-up
        AccessControl accessControl = null;
        boolean acquired = false;
        try {
            boolean lookUpCacheAgain = false;
            if (state.isTryLockBeforeRedisLookUp()) {
                LOG.debug("Try lock before Redis look-up is enabled");
                LockService optLockService = Services.optService(LockService.class);
                if (optLockService != null) {
                    accessControl = optLockService.getAccessControlFor(sessionId.getIdentifier(), 1, 1, 1);
                    if (accessControl.tryAcquireGrant()) {
                        LOG.debug("Immediately acquired lock for {}", sessionId);
                        acquired = true;
                    } else {
                        accessControl.acquireGrant();
                        acquired = true;
                        lookUpCacheAgain = true;
                        LOG.debug("Waited for acquiring lock for {}", sessionId);
                    }
                } else {
                    LOG.debug("No lock service available. Hence, no try lock before Redis look-up for {}", sessionId);
                }
            } else {
                LOG.debug("Try lock before Redis look-up is disabled. Hence, no try lock before Redis look-up for {}", sessionId);
            }

            // Check local cache again
            if (lookUpCacheAgain) {
                LOG.debug("Second look-up in local cache for {} since waited for lock acquisition", sessionId);
                session = considerLocalStorage ? localSessionCache.getSessionByIdIfPresent(sessionId) : null;
                if (session != null) {
                    // Ensure existence if locally fetched
                    LOG.debug("Obtained session {} from local cache for {}", session.getSessionID(), sessionId);
                    return ensureExistenceElseNull(session, state);
                }
            }

            // Fetch from Redis storage
            return getSessionFromRedisAndStoreLocally(sessionId, peek, considerLocalStorage, state);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while getting session by {} from Redis session storage", sessionId, e);
            return null;
        } catch (OXRuntimeException e) {
            throw e;
        } catch (OXException e) { // NOSONARLINT
            LOG.warn("Failed to get session by {} from Redis session storage", sessionId, e);
            throw new OXRuntimeException(e);
        } catch (Exception e) { // NOSONARLINT
            LOG.warn("Failed to get session by {} from Redis session storage", sessionId, e);
            throw new OXRuntimeException(OXException.general(new StringBuilder("Failed to get session by ").append(sessionId).append(" from Redis session storage").toString(), e));
        } finally {
            AccessControls.release(accessControl, acquired);
        }
    }

    private SessionImpl getSessionFromRedisAndStoreLocally(SessionId sessionId, boolean peek, boolean absentInLocalStorage, RedisSessiondState state) throws OXException {
        // Fetch from Redis storage
        SessionImpl restoredSession = newSessionImplFor(getSessionFromRedis(sessionId, peek, state));
        if (restoredSession == null) {
            // No such session available in Redis session storage
            return null;
        }

        if (peek) {
            // Only peek session. Invoke interceptors
            for (SessionSerializationInterceptor interceptor : serializationInterceptors) {
                interceptor.deserialize(restoredSession);
            }
        } else {
            if (absentInLocalStorage) {
                // Local cache has been checked before, but no such session contained
                Loader loader = loaderFor(restoredSession);
                state.getLocalSessionCache().getSessionById(restoredSession.getSessionID(), loader);

                if (loader.isLoaded()) {
                    LOG.debug("Put session {} into local cache", restoredSession.getSessionID());

                    // Invoke interceptors
                    for (SessionSerializationInterceptor interceptor : serializationInterceptors) {
                        interceptor.deserialize(restoredSession);
                    }

                    // Post event for restored session
                    postSessionRestauration(restoredSession);
                }
            } else {
                // Simply replace in local cache
                state.getLocalSessionCache().put(restoredSession, true);

                // Invoke interceptors
                for (SessionSerializationInterceptor interceptor : serializationInterceptors) {
                    interceptor.deserialize(restoredSession);
                }
            }
        }

        LOG.debug("Returning session {} from Redis storage", restoredSession.getSessionID());
        return restoredSession;
    }

    /**
     * Gets referenced session from Redis storage.
     *
     * @param sessionId The session identifier
     * @param peek Whether session is peeked or not; if simply peeked its expiration time stamp is not reseted
     * @return The session fetched from Redis or <code>null</code>
     * @throws OXException
     */
    public Session getSessionFromRedis(SessionId sessionId, boolean peek) throws OXException {
        return getSessionFromRedis(sessionId, peek, getState());
    }

    private Session getSessionFromRedis(SessionId sessionId, boolean peek, RedisSessiondState state) throws OXException {
        LOG.debug("Performing look-up for {} in Redis storage", sessionId);
        AtomicLong now = new AtomicLong();
        AtomicReference<String> brandIdRef = new AtomicReference<>();
        Session sessionFromRedis = connector.executeOperation(commandsProvider -> {
            // Determine session identifier
            String sesId;
            if (sessionId.isAlternativeId()) {
                // Look-up of alternative identifier to session identifier association
                sesId = commandsProvider.getStringCommands().get(getSessionAlternativeKey(sessionId.getAlternativeIdentifier()));
                if (sesId == null) {
                    // No such alternative identifier to session identifier association
                    return null;
                }
            } else {
                sesId = sessionId.getIdentifier();
            }

            // Look-up of session in Redis storage by session identifier
            Session fetchedFromRedis = getSessionCommands(commandsProvider).get(getSessionKey(sesId));
            if (fetchedFromRedis == null) {
                // No such session in Redis storage
                if (sessionId.isAlternativeId()) {
                    // Delete alternative identifier to session identifier association
                    commandsProvider.getKeyCommands().del(getSessionAlternativeKey(sessionId.getAlternativeIdentifier()));
                }
                return null;
            }

            // Reset expiry time on regular access
            if (!peek) {
                // Reset session's expire time
                RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
                long currentTime = System.currentTimeMillis();
                now.set(currentTime);
                String brandId = getBrandIdentifierFrom(fetchedFromRedis);
                brandIdRef.set(brandId);
                resetSessionsExpireTime(fetchedFromRedis, brandId, currentTime, sortedSetCommands);
            }
            return fetchedFromRedis;
        });

        // Session obtained from Redis?
        if (sessionFromRedis == null) {
            LOG.debug("No such session for {} in Redis storage", sessionId);
            return null;
        }

        // Also consider remote ones to reset session's expire time
        LOG.debug("Obtained session {} from Redis storage for {}{}{}", sessionFromRedis.getSessionID(), sessionId, Strings.getLineSeparator(), prettyPrinterFor(sessionFromRedis, state));
        if (!peek) {
            RedisConnectorService connectorService = services.getOptionalService(RedisConnectorService.class);
            if (connectorService != null) {
                List<RedisConnectorProvider> remoteConnectorProviders = connectorService.getRemoteConnectorProviders();
                if (!remoteConnectorProviders.isEmpty()) {
                    long currentTime = now.get();
                    String brandId = brandIdRef.get();
                    ExceptionCatchingRunnable task = () -> {
                        for (RedisConnectorProvider connectorProvider : remoteConnectorProviders) {
                            connectorProvider.getConnector().executeVoidOperation(commandsProvider -> {
                                RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();
                                resetSessionsExpireTime(sessionFromRedis, brandId, currentTime, sortedSetCommands);
                            });
                        }
                    };

                    ThreadPools.submitElseExecute(ThreadPools.task(task));
                }
            }
        }
        return sessionFromRedis;
    }

    private void resetSessionsExpireTime(Session session, String brandId, long currentTime, RedisSortedSetCommands<String, String> sortedSetCommands) {
        sortedSetCommands.zadd(getSortSetKeyForSession(session), currentTime, session.getSessionID());
        if (brandId != null) {
            sortedSetCommands.zadd(getSetKeyForBrand(brandId), currentTime, session.getSessionID());
        }
    }

    /**
     * Checks if given session does exist in Redis session storage; returns the session if it does otherwise <code>null</code>
     *
     * @param session The session to check existence for; must not be <code>null</code>
     * @param state The current state
     * @param existenceCheckedFlag The boolean array to signal whether existence in Redis session storage has already been checked
     * @return The session if existent; otherwise <code>null</code>
     * @throws OXRuntimeException If a connectivity issue occurs while trying to communicate with Redis end-point
     */
    private SessionImpl ensureExistenceElseNull(SessionImpl session, RedisSessiondState state) throws OXRuntimeException {
        if (!state.isEnsureExistenceOnLocalFetch()) {
            // No existence check enabled... Rely on session events only
            LOG.debug("No existence check enabled. Returning locally fetched session {} unchecked", session.getSessionID());
            return session;
        }

        try {
            // Check session existence in Redis session storage
            int checkExistenceThreshold = state.getCheckExistenceThreshold();
            if (checkExistenceThreshold <= 0) {
                // No last-checked test. Directly query Redis for existence.
                return checkExistenceFor(session, 0L, state);
            }

            // Do last-checked test
            LOG.debug("Check existence threshold enabled: {}", I(checkExistenceThreshold));
            long now = System.currentTimeMillis();
            long lastChecked = session.getLastChecked();
            if (lastChecked > 0) {
                long lastCheckedDuration = now - lastChecked;
                if (lastCheckedDuration < checkExistenceThreshold) {
                    // Assume still existent
                    LOG.debug("Last existence check {}ms ago, re-check threshold not exceeded. Returning locally fetched session {} unchecked", L(lastCheckedDuration), session.getSessionID());
                    return session;
                }
            }

            // Query Redis for existence
            return checkExistenceFor(session, now, state);
        } catch (OXException e) {
            throw new OXRuntimeException(e);
        }
    }

    private SessionImpl checkExistenceFor(SessionImpl session, long lastCheckedToSet, RedisSessiondState state) throws OXException {
        try {
            LOG.debug("Checking existence for locally fetched session {} in Redis storage", session.getSessionID());

            RedisOperationKey<DefaultRedisOperationKey> key;
            RedisOperation<Boolean> existsOperation;
            if (lastCheckedToSet > 0) {
                // Set last-checked time stamp (if any) to session as soon as possible. Therefore include it in passed RedisOperation instance.
                key = newExistsOperationKey(session.getSessionID(), true);
                existsOperation = commandsProvider -> {
                    boolean exists = commandsProvider.getKeyCommands().exists(getSessionKey(session.getSessionID())).longValue() > 0;
                    if (exists) {
                        // Session does exist in Redis storage. Thus, set last-checked time stamp
                        session.setLastChecked(lastCheckedToSet);
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                };
            } else {
                key = newExistsOperationKey(session.getSessionID());
                existsOperation = commandsProvider -> B(commandsProvider.getKeyCommands().exists(getSessionKey(session.getSessionID())).longValue() > 0);
            }
            if (connector.executeOperation(key, existsOperation).booleanValue()) {
                // Exists...
                LOG.debug("Locally fetched session {} does exist in Redis storage. Hence, returning that session", session.getSessionID());
                return session;
            }

            // Non-existent.
            LOG.debug("Locally fetched session {} does NOT exist in Redis storage. Dropping from local cache as well as Redis collections and returning negative result (null)", session.getSessionID());
            state.getLocalSessionCache().removeSessionById(session.getSessionID());

            // Drop from other Redis collections
            new RemovalCollection().addSession(session, true, true, this).removeCollected(this);
            return null;
        } catch (OXException e) { // NOSONARLINT
            LOG.warn("Failed to check existence for locally fetched session {} in Redis session storage", session.getSessionID(), e);
            throw e;
        } catch (Exception e) { // NOSONARLINT
            LOG.warn("Failed to check existence for locally fetched session {} in Redis session storage", session.getSessionID(), e);
            throw OXException.general(new StringBuilder("Failed to check existence for locally fetched session ").append(session.getSessionID()).append(" in Redis session storage").toString(), e);
        }
    }

    /**
     * Gets a new <code>SessionImpl</code> instance for given session.
     *
     * @param ses The session to yield for
     * @return The new <code>SessionImpl</code> instance or <code>null</code>
     */
    protected SessionImpl newSessionImplFor(Session ses) {
        if (ses instanceof SessionImpl sessionImpl) {
            return sessionImpl;
        }

        if (ses == null) {
            return null;
        }

        SessionImpl sessionImpl = new SessionImpl(ses.getUserId(), ses.getLoginName(), ses.getPassword(), ses.getContextId(), ses.getSessionID(), ses.getSecret(), ses.getRandomToken(), ses.getLocalIp(), ses.getLogin(), ses.getAuthId(), ses.getHash(), ses.getClient(), ses.isStaySignedIn(), ses.getOrigin());
        for (String name : ses.getParameterNames()) {
            Object value = ses.getParameter(name);
            sessionImpl.setParameter(name, value);
        }
        return sessionImpl;
    }

    @Override
    public Session getSessionByAlternativeId(String altId) {
        return doGetSession(newAlternativeSessionId(altId), false, true);
    }

    @Override
    public Session getSessionByAlternativeId(String altId, boolean lookupSessionStorage) {
        return getSessionByAlternativeId(altId);
    }

    @Override
    public int getUserSessions(int userId, int contextId) {
        try {
            return connector.executeOperation(commandsProvider -> commandsProvider.getSetCommands().scard(getSetKeyForUser(userId, contextId))).intValue();
        } catch (Exception e) {
            LOG.warn("Failed to look-up sessions for user {} in context {} from Redis session storage", I(userId), I(contextId), e);
            return 0;
        }
    }

    @Override
    public Collection<Session> getActiveSessions(int userId, int contextId) {
        try {
            Set<String> sessionIds = connector.executeOperation(commandsProvider -> commandsProvider.getSetCommands().smembers(getSetKeyForUser(userId, contextId)));
            if (sessionIds == null || sessionIds.isEmpty()) {
                return Collections.emptyList();
            }

            List<Session> sessions = new ArrayList<>(sessionIds.size());
            for (String sessionId : sessionIds) {
                Session session = doGetSession(newSessionId(sessionId), true, true);
                if (session != null) {
                    sessions.add(session);
                }
            }
            return sessions;
        } catch (Exception e) {
            LOG.warn("Failed to look-up sessions for user {} in context {} from Redis session storage", I(userId), I(contextId), e);
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<Session> getSessions(int userId, int contextId) {
        return getActiveSessions(userId, contextId);
    }

    @Override
    public Collection<Session> getSessions(int userId, int contextId, boolean considerSessionStorage) {
        return getActiveSessions(userId, contextId);
    }

    @Override
    public boolean isActive(final String sessionId) {
        if (null == sessionId) {
            return false;
        }

        if (getState().getLocalSessionCache().getSessionByIdIfPresent(sessionId) != null) {
            return true;
        }

        try {
            return connector.executeOperation(newExistsOperationKey(sessionId), commandsProvider -> commandsProvider.getKeyCommands().exists(getSessionKey(sessionId))).longValue() > 0;
        } catch (Exception e) {
            LOG.warn("Failed to check existence of session {} in Redis session storage", sessionId, e);
            return false;
        }
    }

    @Override
    public List<String> getActiveSessionIDs() {
        long now = System.currentTimeMillis();
        long threshold = now - getState().getSessionDefaultLifeTime();
        final Range<Number> range = Range.create(Long.valueOf(threshold), Long.valueOf(now + RedisSessionConstants.MILLIS_DAY));

        try {
            DefaultRedisOperationKey key = DefaultRedisOperationKey.builder().withCommand(RedisCommand.ZRANGEBYSCORE).withContextId(1).withUserId(1).withHash("sessions.activeids").build();
            return connector.<List<String>> executeOperation(key, commandsProvider -> {
                RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();

                List<String> sessionIds = null;
                for (String sortedSetKey : SORTED_SETS) {
                    long offset = 0;
                    Limit limit = Limit.create(offset, RedisSessionConstants.LIMIT_1000);

                    List<String> active = sortedSetCommands.zrangebyscore(sortedSetKey, range, limit);
                    for (int size; active != null && (size = active.size()) > 0;) {
                        if (sessionIds == null) {
                            sessionIds = new ArrayList<>(active);
                        } else {
                            sessionIds.addAll(active);
                        }

                        // Get next chunk (if any)
                        if (size >= RedisSessionConstants.LIMIT_1000) {
                            offset += RedisSessionConstants.LIMIT_1000;
                            limit = Limit.create(offset, RedisSessionConstants.LIMIT_1000);
                            active = sortedSetCommands.zrangebyscore(sortedSetKey, range, limit);
                        } else {
                            active = null;
                        }
                    }
                }

                return sessionIds;
            });
        } catch (Exception e) {
            LOG.warn("Failed to look-up active sessions in Redis session storage", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Session getSessionByRandomToken(final String randomToken, final String localIp) {
        throw new UnsupportedOperationException("AbstractRedisSessiondService.getSessionByRandomToken()");
    }

    @Override
    public Session getSessionByRandomToken(final String randomToken) {
        throw new UnsupportedOperationException("AbstractRedisSessiondService.getSessionByRandomToken()");
    }

    @Override
    public Session getSessionWithTokens(final String clientToken, final String serverToken) throws OXException {
        // find session matching to token
        LOG.debug("Redeeming server token {}", serverToken);
        TokenSessionControl tokenControl = TokenSessionContainer.getInstance().getSession(clientToken, serverToken);
        SessionImpl activatedSession = tokenControl.getSession();
        LOG.debug("Redeemed server token {} for session {} of user {} in context {}", serverToken, activatedSession.getSessionID(), I(activatedSession.getUserId()), I(activatedSession.getContextId()));
        putSessionIntoRedisAndLocal(activatedSession, getState());
        LOG.debug("Completed adding session {} of user {} in context {}", activatedSession.getSessionID(), I(activatedSession.getUserId()), I(activatedSession.getContextId()));
        return activatedSession;
    }

    @Override
    public int getNumberOfActiveSessions() {
        try {
            return connector.executeOperation(commandsProvider -> L(doGetNumberSessionsInRedis(commandsProvider))).intValue();
        } catch (Exception e) {
            LOG.warn("Failed to count sessions in Redis session storage", e);
            return 0;
        }
    }

    /**
     * Gets the number of sessions in Redis session storage through issuing a <code>SCAN</code> command.
     *
     * @param connection The connection to use
     * @return The number of sessions
     */
    public long doGetNumberSessionsInRedis(RedisCommandsProvider commandsProvider) {
        RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
        RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();

        ScanArgs scanArgs = ScanArgs.Builder.matches(getAllSetKeyPattern()).limit(RedisSessionConstants.LIMIT_1000);

        long count = 0;
        KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
        while (cursor != null) {
            // Iterate current keys...
            for (String setKey : cursor.getKeys()) {
                count += setCommands.scard(setKey).longValue();
            }

            // Move cursor forward
            cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
        }
        return count;
    }

    @Override
    public Session getAnyActiveSessionForUser(final int userId, final int contextId) {
        try {
            Session sessionFromRedis = connector.executeOperation(commandsProvider -> {
                RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();

                Session session;
                do {
                    String sessionId = setCommands.srandmember(getSetKeyForUser(userId, contextId));
                    if (sessionId == null) {
                        return null;
                    }
                    session = getSessionCommands(commandsProvider).get(getSessionKey(sessionId));
                    if (session == null) {
                        setCommands.srem(getSetKeyForUser(userId, contextId), sessionId);
                    }
                } while (session == null);
                return session;
            });

            SessionImpl newSessionImpl = newSessionImplFor(sessionFromRedis);
            if (newSessionImpl != null) {
                getState().getLocalSessionCache().getSessionById(newSessionImpl.getSessionID(), loaderFor(newSessionImpl));

                // Post event for restored session
                postSessionRestauration(newSessionImpl);
            }
            return newSessionImpl;
        } catch (Exception e) {
            LOG.warn("Failed to get session from Redis session storage", e);
            return null;
        }
    }

    @Override
    public Session findFirstMatchingSessionForUser(final int userId, final int contextId, final SessionMatcher matcher) {
        if (null == matcher) {
            return null;
        }

        {
            RedisSessiondState state = getState();
            SessionImpl session = state.getLocalSessionCache().getFirstMatchingSessionForUser(userId, contextId, matcher);
            if (session != null && ensureExistenceElseNull(session, state) != null) { // Ensure existence if locally fetched
                return session;
            }
        }

        try {
            RedisSessiondState state = getState();
            Session sessionFromRedis = connector.executeOperation(commandsProvider -> {
                RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);

                Set<String> sessionIds = setCommands.smembers(getSetKeyForUser(userId, contextId));
                if (sessionIds == null || sessionIds.isEmpty()) {
                    return null;
                }

                List<String> sessionKeys = sessionIds.stream().map(this::getSessionKey).collect(Collectors.toList());
                for (KeyValue<String, Session> keyValue : sessionCommands.mget(sessionKeys.toArray(new String[sessionKeys.size()]))) {
                    String sessionId = keyValue.getKey().substring(keyValue.getKey().lastIndexOf(':') + 1);
                    Session session = keyValue.hasValue() ? keyValue.getValue() : null;
                    if (session == null) {
                        setCommands.srem(getSetKeyForUser(userId, contextId), sessionId);
                    } else {
                        if (matcher.accepts(session)) {
                            return session;
                        }
                    }
                }
                return null;
            });

            SessionImpl newSessionImpl = newSessionImplFor(sessionFromRedis);
            if (newSessionImpl != null) {
                state.getLocalSessionCache().getSessionById(newSessionImpl.getSessionID(), loaderFor(newSessionImpl));
            }
            return newSessionImpl;
        } catch (Exception e) {
            LOG.warn("Failed to get session from Redis session storage", e);
            return null;
        }
    }

    @Override
    public Collection<String> removeSessions(SessionFilter filter) throws OXException {
        LOG.debug("Removing sessions by session filter");

        ScanArgs scanArgs = ScanArgs.Builder.matches(getAllSetKeyPattern()).limit(RedisSessionConstants.LIMIT_1000);

        List<String> sessionIdentifiers = connector.executeOperation(commandsProvider -> {
            RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);
            RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
            RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();

            List<String> retval = null;
            RemovalCollection removalCollection = new RemovalCollection();

            KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
            while (cursor != null) {
                // Iterate current keys...
                for (String setKey : cursor.getKeys()) {
                    // Get set's members (list of session identifiers)
                    Set<String> sessionIds = setCommands.smembers(setKey);
                    if (sessionIds != null && !sessionIds.isEmpty()) {
                        List<String> sessionKeys = sessionIds.stream().map(this::getSessionKey).collect(Collectors.toList());
                        for (KeyValue<String, Session> keyValue : sessionCommands.mget(sessionKeys.toArray(new String[sessionKeys.size()]))) {
                            String sessionId = keyValue.getKey().substring(keyValue.getKey().lastIndexOf(':') + 1);
                            Session session = keyValue.hasValue() ? keyValue.getValue() : null;
                            if (session == null) {
                                // Remove from set since non-existent
                                setCommands.srem(setKey, sessionId);
                            } else {
                                // Check if filter applies. Remove session if it does.
                                if (filter.apply(session) && (session = sessionCommands.getdel(getSessionKey(sessionId))) != null) {
                                    removalCollection.addSession(session, true, true, this);
                                    LOG.debug("Removed session {} by session filter from Redis storage for user {} in context {}", sessionId, I(session.getUserId()), I(session.getContextId()));

                                    if (retval == null) {
                                        retval = new ArrayList<>();
                                    }
                                    retval.add(sessionId);
                                }
                            }
                        }
                    }
                }

                // Move cursor forward
                cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
            }

            removalCollection.removeCollected(commandsProvider);

            return retval == null ? Collections.emptyList() : retval;
        });

        RedisConnectorService connectorService = services.getOptionalService(RedisConnectorService.class);
        if (connectorService != null) {
            List<RedisConnectorProvider> remoteConnectorProviders = connectorService.getRemoteConnectorProviders();
            if (!remoteConnectorProviders.isEmpty()) {
                Obfuscator obfuscator = getState().getObfuscator();
                ExceptionCatchingRunnable task = () -> {
                    VersionMismatchHandler versionMismatchHandler = DO_NOTHING_VERSION_MISMATCH_HANDLER;
                    for (RedisConnectorProvider connectorProvider : remoteConnectorProviders) {
                        connectorProvider.getConnector().executeVoidOperation(commandsProvider -> {
                            RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider, obfuscator, versionMismatchHandler);
                            RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                            RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
                            RedisSortedSetCommands<String, String> sortedSetCommands = commandsProvider.getSortedSetCommands();

                            RemovalCollection removalCollection = new RemovalCollection();

                            KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
                            while (cursor != null) {
                                // Iterate current keys...
                                for (String setKey : cursor.getKeys()) {
                                    // Get set's members (list of session identifiers)
                                    Set<String> sessionIds = setCommands.smembers(setKey);
                                    if (sessionIds != null && !sessionIds.isEmpty()) {
                                        List<String> sessionKeys = sessionIds.stream().map(this::getSessionKey).collect(Collectors.toList());
                                        for (KeyValue<String, Session> keyValue : sessionCommands.mget(sessionKeys.toArray(new String[sessionKeys.size()]))) {
                                            String sessionId = keyValue.getKey().substring(keyValue.getKey().lastIndexOf(':') + 1);
                                            Session session = keyValue.hasValue() ? keyValue.getValue() : null;
                                            if (session == null) {
                                                // Remove from set since non-existent
                                                setCommands.srem(setKey, sessionId);
                                            } else {
                                                // Check if filter applies. Remove session if it does.
                                                if (filter.apply(session) && (session = sessionCommands.getdel(getSessionKey(sessionId))) != null) {
                                                    removalCollection.addSession(session, true, true, this);
                                                    LOG.debug("Removed session {} by session filter from remote Redis storage for user {} in context {}", sessionId, I(session.getUserId()), I(session.getContextId()));
                                                }
                                            }
                                        }
                                    }
                                }

                                // Move cursor forward
                                cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
                            }

                            removalCollection.removeCollected(keyCommands, setCommands, sortedSetCommands);
                        });
                    }
                };

                ThreadPools.submitElseExecute(ThreadPools.task(task));
            }
        }

        int numberOfRemovedSessions = sessionIdentifiers.size();
        if (numberOfRemovedSessions > 0) {
            getState().getLocalSessionCache().removeSessionsByIds(sessionIdentifiers);
            LOG.debug("Removed {} filtered sessions from local cache", I(numberOfRemovedSessions));
        }

        LOG.debug("Removed {} filtered sessions from Redis storage", I(numberOfRemovedSessions));
        return sessionIdentifiers;
    }

    @Override
    public Collection<String> removeSessionsGlobally(SessionFilter filter) throws OXException {
        return removeSessions(filter);
    }

    @Override
    public Collection<String> findSessions(SessionFilter filter) throws OXException {
        UserAndContext userAndContext = isFilterByUser(filter);
        if (userAndContext != null) {
            int contextId = userAndContext.getContextId();
            int userId = userAndContext.getUserId();
            return connector.executeOperation(commandsProvider -> {
                RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);

                Set<String> sessionIds = setCommands.smembers(getSetKeyForUser(userId, contextId));
                if (sessionIds == null || sessionIds.isEmpty()) {
                    return null;
                }

                List<String> sessionKeys = sessionIds.stream().map(this::getSessionKey).collect(Collectors.toList());
                List<String> matchingSessionIds = null;
                for (KeyValue<String, Session> keyValue : sessionCommands.mget(sessionKeys.toArray(new String[sessionKeys.size()]))) {
                    String sessionId = keyValue.getKey().substring(keyValue.getKey().lastIndexOf(':') + 1);
                    Session session = keyValue.hasValue() ? keyValue.getValue() : null;
                    if (session == null) {
                        setCommands.srem(getSetKeyForUser(userId, contextId), sessionId);
                    } else {
                        if (filter.apply(session)) {
                            if (matchingSessionIds == null) {
                                matchingSessionIds = new ArrayList<>(sessionKeys.size());
                            }
                            matchingSessionIds.add(sessionId);
                        }
                    }
                }
                return matchingSessionIds == null ? Collections.emptyList() : matchingSessionIds;
            });
        }

        int contextId = isFilterByContext(filter);
        if (contextId > 0) {
            ScanArgs scanArgs = ScanArgs.Builder.matches(getSetKeyPattern(contextId)).limit(RedisSessionConstants.LIMIT_1000);
            return connector.executeOperation(commandsProvider -> {
                RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
                RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);
                RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();

                List<String> matchingSessionIds = null;
                KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
                while (cursor != null) {
                    // Obtain current keys...
                    List<String> keys = cursor.getKeys();
                    int numberOfKeys = keys.size();
                    if (numberOfKeys > 0) {
                        // Iterate set identifier
                        for (String setKey : keys) {
                            // Get set's members (list of session identifiers)
                            Set<String> sessionIds = setCommands.smembers(setKey);
                            if (sessionIds != null && !sessionIds.isEmpty()) {
                                List<String> sessionKeys = sessionIds.stream().map(this::getSessionKey).collect(Collectors.toList());
                                for (KeyValue<String, Session> keyValue : sessionCommands.mget(sessionKeys.toArray(new String[sessionKeys.size()]))) {
                                    String sessionId = keyValue.getKey().substring(keyValue.getKey().lastIndexOf(':') + 1);
                                    Session session = keyValue.hasValue() ? keyValue.getValue() : null;
                                    if (session == null) {
                                        // Remove from set since non-existent
                                        setCommands.srem(setKey, sessionId);
                                    } else {
                                        if (filter.apply(session)) {
                                            if (matchingSessionIds == null) {
                                                matchingSessionIds = new ArrayList<>(sessionKeys.size());
                                            }
                                            matchingSessionIds.add(sessionId);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Move cursor forward
                    cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
                }
                return matchingSessionIds == null ? Collections.emptyList() : matchingSessionIds;
            });
        }

        // The hard way...
        ScanArgs scanArgs = ScanArgs.Builder.matches(getAllSetKeyPattern()).limit(RedisSessionConstants.LIMIT_1000);

        return connector.executeOperation(commandsProvider -> {
            RedisStringCommands<String, Session> sessionCommands = getSessionCommands(commandsProvider);
            RedisSetCommands<String, String> setCommands = commandsProvider.getSetCommands();
            RedisKeyCommands<String, InputStream> keyCommands = commandsProvider.getKeyCommands();
            List<String> retval = new ArrayList<>();

            KeyScanCursor<String> cursor = keyCommands.scan(scanArgs);
            while (cursor != null) {
                // Iterate current keys...
                for (String setKey : cursor.getKeys()) {
                    // Get set's members (list of session identifiers)
                    Set<String> sessionIds = setCommands.smembers(setKey);
                    if (sessionIds != null && !sessionIds.isEmpty()) {
                        List<String> sessionKeys = sessionIds.stream().map(this::getSessionKey).collect(Collectors.toList());
                        for (KeyValue<String, Session> keyValue : sessionCommands.mget(sessionKeys.toArray(new String[sessionKeys.size()]))) {
                            String sessionId = keyValue.getKey().substring(keyValue.getKey().lastIndexOf(':') + 1);
                            Session session = keyValue.hasValue() ? keyValue.getValue() : null;
                            if (session == null) {
                                // Remove from set since non-existent
                                setCommands.srem(setKey, sessionId);
                            } else {
                                if (filter.apply(session)) {
                                    retval.add(sessionId);
                                }
                            }
                        }
                    }
                }

                // Move cursor forward
                cursor = cursor.isFinished() ? null : keyCommands.scan(cursor, scanArgs);
            }
            return retval;
        });
    }

    private static final Pattern FILTER_USER = Pattern.compile("\\("+SessionFilter.CONTEXT_ID+"=([0-9]+)\\)\\("+SessionFilter.USER_ID+"=([0-9]+)\\)");

    private static UserAndContext isFilterByUser(SessionFilter filter) {
        Optional<UserAndContext> optUserFilter = filter.getUserAssociation();
        if (optUserFilter.isPresent()) {
            return optUserFilter.get();
        }

        if (filter.toString().indexOf(SessionFilter.CONTEXT_ID) < 0) {
            return null;
        }
        if (filter.toString().indexOf(SessionFilter.USER_ID) < 0) {
            return null;
        }

        Matcher m = FILTER_USER.matcher(filter.toString());
        if (!m.find()) {
            return null;
        }

        try {
            return UserAndContext.newInstance(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final Pattern FILTER_CONTEXT = Pattern.compile("\\("+SessionFilter.CONTEXT_ID+"=([0-9]+)\\)");

    private static int isFilterByContext(SessionFilter filter) {
        Optional<Integer> optContextFilter = filter.getContextAssociation();
        if (optContextFilter.isPresent()) {
            return optContextFilter.get().intValue();
        }

        if (filter.toString().indexOf(SessionFilter.CONTEXT_ID) < 0) {
            return 0;
        }

        Matcher m = FILTER_CONTEXT.matcher(filter.toString());
        if (!m.find()) {
            return 0;
        }

        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public Collection<String> findSessionsGlobally(SessionFilter filter) throws OXException {
        return findSessions(filter);
    }

    @Override
    public boolean isApplicableForSessionStorage(Session session) {
        return session != null;
    }

    // -------------------------------------------------------- Channel listener------------------------------------------------------------

    @Override
    public void onMessage(Message<SessionEvent> message) {
        if (!message.isRemote()) {
            // Ignore local event
            return;
        }

        SessionEvent sessionEvent = message.getData();
        if (SessionOperation.INVALIDATE == sessionEvent.getOperation()) {
            getState().getLocalSessionCache().removeSessionsByIds(sessionEvent.getSessionIds());
        }
    }

    // -------------------------------------------------------- Events ---------------------------------------------------------------------

    /**
     * Post the event that a single session has been put into Redis storage.
     *
     * @param session The stored session
     * @param publishInvalidateEvent Whether to publish invalidate event in cluster
     */
    private void postSessionStored(Session session, boolean publishInvalidateEvent) {
        EventAdmin eventAdmin = services.getOptionalService(EventAdmin.class);
        if (eventAdmin != null) {
            Map<String, Object> dic = new HashMap<>(2);
            dic.put(SessiondEventConstants.PROP_SESSION, session);
            eventAdmin.postEvent(new Event(SessiondEventConstants.TOPIC_STORED_SESSION, dic));
            LOG.debug("Posted event for stored session");
        }

        if (publishInvalidateEvent) {
            publishInvalidateEventQuietly(session.getSessionID());
        }
    }

    /**
     * Posts the event that a single session has been locally created.
     *
     * @param incrementCounters Whether to increment counters for that session
     */
    private void postSessionCreation(Session session) {
        EventAdmin eventAdmin = services.getOptionalService(EventAdmin.class);
        if (eventAdmin != null) {
            Map<String, Object> dic = new HashMap<>(2);
            dic.put(SessiondEventConstants.PROP_SESSION, session);
            eventAdmin.postEvent(new Event(SessiondEventConstants.TOPIC_ADD_SESSION, dic));
            LOG.debug("Posted event for added session");
        }
    }

    /**
     * Posts the event for a locally restored session.
     * <p>
     * That is session representation has been fetched from Redis storage and has been added to local cache.
     *
     * @param session The restored session
     */
    private void postSessionRestauration(Session session) {
        EventAdmin eventAdmin = services.getOptionalService(EventAdmin.class);
        if (eventAdmin != null) {
            Map<String, Object> dic = new HashMap<>(2);
            dic.put(SessiondEventConstants.PROP_SESSION, session);
            eventAdmin.postEvent(new Event(SessiondEventConstants.TOPIC_RESTORED_SESSION, dic));
            LOG.debug("Posted event for restored session");
        }
    }

    /**
     * Posts the event for removal of locally held sessions.
     *
     * @param <S> The session type
     * @param sessions The removed sessions
     */
    private <S extends Session> void postContainerRemoval(List<S> sessions) {
        EventAdmin eventAdmin = services.getOptionalService(EventAdmin.class);
        if (eventAdmin != null) {
            Map<String, Session> eventMap = Maps.newHashMapWithExpectedSize(sessions.size());
            for (Session session : sessions) {
                eventMap.put(session.getSessionID(), session);
            }
            Map<String, Object> dic = new HashMap<>(2);
            dic.put(SessiondEventConstants.PROP_CONTAINER, eventMap);
            eventAdmin.postEvent(new Event(SessiondEventConstants.TOPIC_REMOVE_CONTAINER, dic));
        }

        publishInvalidateEventQuietly(sessions.stream().map(Session::getSessionID).collect(Collectors.toList()));
    }

    /**
     * Posts the event for removal of a locally held session.
     *
     * @param session The removed session
     */
    private void postSessionRemoval(Session session) {
        EventAdmin eventAdmin = services.getOptionalService(EventAdmin.class);
        if (eventAdmin != null) {
            Map<String, Object> dic = new HashMap<>(2);
            dic.put(SessiondEventConstants.PROP_SESSION, session);
            eventAdmin.postEvent(new Event(SessiondEventConstants.TOPIC_REMOVE_SESSION, dic));
            LOG.debug("Posted event for removed session {}", session.getSessionID());
        }

        publishInvalidateEventQuietly(session.getSessionID());
    }

    private void publishInvalidateEventQuietly(String sessionId) {
        publishInvalidateEventQuietly(Collections.singletonList(sessionId));
    }

    private void publishInvalidateEventQuietly(List<String> sessionIds) {
        try {
            channel.publish(new SessionEvent(SessionOperation.INVALIDATE, sessionIds));
        } catch (Exception e) {
            LOG.warn("Failed to publish session event for session identifiers: {}", sessionIds, e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static Object prettyPrinterFor(Session session, RedisSessiondState state) {
        return new SessionPrettyPrinter(session, state);
    }

    private static final class SessionPrettyPrinter {

        private final Session session;
        private final RedisSessiondState state;

        /**
         * Initializes a new {@link ObjectExtension}.
         *
         * @param session The session to pretty-print
         * @param state The current state
         */
        SessionPrettyPrinter(Session session, RedisSessiondState state) {
            super();
            this.session = session;
            this.state = state;
        }

        @Override
        public String toString() {
            try {
                return SessionCodec.session2Json(session, state.getObfuscator(), RedisSessionVersionService.getInstance()).toString(0);
            } catch (Exception e) {
                LOG.warn("Failed to pretty-print session {}", session.getSessionID(), e);
                return session.toString();
            }
        }
    }

}
