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

package com.openexchange.pns.subscription.storage.rdb;

import static com.openexchange.database.Databases.closeSQLStuff;
import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import com.openexchange.context.ContextService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.java.BoolReference;
import com.openexchange.java.util.UUIDs;
import com.openexchange.pns.DefaultPushSubscription;
import com.openexchange.pns.DefaultPushSubscription.Builder;
import com.openexchange.pns.DefaultToken;
import com.openexchange.pns.KnownTopic;
import com.openexchange.pns.Meta;
import com.openexchange.pns.PushExceptionCodes;
import com.openexchange.pns.PushMatch;
import com.openexchange.pns.PushNotifications;
import com.openexchange.pns.PushSubscription;
import com.openexchange.pns.PushSubscriptionDescription;
import com.openexchange.pns.PushSubscriptionRegistry;
import com.openexchange.pns.PushSubscriptionRestrictions;
import com.openexchange.pns.PushSubscriptionResult;
import com.openexchange.pns.Token;
import com.openexchange.pns.subscription.storage.ClientAndTransport;
import com.openexchange.pns.subscription.storage.MapBackedHits;
import com.openexchange.pns.subscription.storage.rdb.cache.RdbPushSubscriptionRegistryCache;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link RdbPushSubscriptionRegistry}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class RdbPushSubscriptionRegistry implements PushSubscriptionRegistry {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RdbPushSubscriptionRegistry.class);

    /** Controls whether to use cache instance instead of querying database */
    private static final boolean USE_CACHE = false;

    private final DatabaseService databaseService;
    private final ContextService contextService;
    private final TimerService timerService;
    private final AtomicReference<RdbPushSubscriptionRegistryCache> cacheReference;

    /**
     * Initializes a new {@link RdbPushSubscriptionRegistry}.
     *
     * @param databaseService The database service to use
     * @param contextService The context service
     * @param timerService The timer service
     */
    public RdbPushSubscriptionRegistry(DatabaseService databaseService, ContextService contextService, TimerService timerService) {
        super();
        this.databaseService = databaseService;
        this.contextService = contextService;
        this.timerService = timerService;
        this.cacheReference = new AtomicReference<>();
    }

    /**
     * Sets the cache instance (if enabled).
     *
     * @param cache The cache
     */
    public void setCache(RdbPushSubscriptionRegistryCache cache) {
        if (USE_CACHE) {
            this.cacheReference.set(cache);
        }
    }

    /**
     * Loads the subscriptions belonging to given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The user-associated subscriptions
     * @throws OXException If subscriptions cannot be loaded
     */
    public List<PushSubscription> loadSubscriptionsFor(int userId, int contextId) throws OXException {
        Connection con = databaseService.getReadOnly(contextId);
        try {
            return loadSubscriptionsFor(userId, contextId, con);
        } finally {
            databaseService.backReadOnly(contextId, con);
        }
    }

    /**
     * Loads the subscriptions belonging to given user.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param con The connection to use
     * @return The user-associated subscriptions
     * @throws OXException If subscriptions cannot be loaded
     */
    public List<PushSubscription> loadSubscriptionsFor(int userId, int contextId, Connection con) throws OXException {
        if (null == con) {
            return loadSubscriptionsFor(userId, contextId);
        }

        Map<UUID, BuilderAndTopics> builders;
        {
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                stmt = con.prepareStatement(
                    "SELECT p.id, p.token, p.client, p.transport, p.all_flag, p.expires, w.topic AS prefix, e.topic, p.meta FROM pns_subscription p"
                        + " LEFT JOIN pns_subscription_topic_wildcard w ON p.id=w.id"
                        + " LEFT JOIN pns_subscription_topic_exact e ON p.id=e.id"
                        + " WHERE p.cid=? AND p.user=?");
                stmt.setInt(1, contextId);
                stmt.setInt(2, userId);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    return Collections.emptyList();
                }

                builders = new LinkedHashMap<>();
                do {
                    UUID uuid = UUIDs.toUUID(rs.getBytes(1));
                    List<String> topics;
                    {
                        BuilderAndTopics builderAndTopics = builders.get(uuid);
                        if (null == builderAndTopics) {
                            // Create new BuilderAndTopics instance and start with a new topics list
                            topics = new LinkedList<>();
                            DefaultPushSubscription.Builder builder = DefaultPushSubscription.builder()
                                .contextId(contextId)
                                .userId(userId)
                                .token(DefaultToken.builder().withValue(rs.getString(2)).withMeta(MetaConverter.parseStringToMeta(rs.getString(9))).build())
                                .client(rs.getString(3))
                                .transportId(rs.getString(4));
                            if (rs.getInt(5) > 0) {
                                topics.add(KnownTopic.ALL.getName());
                            }
                            long expires = rs.getLong(6);
                            if (!rs.wasNull()) {
                                builder.expires(new Date(expires));
                            }
                            builderAndTopics = new BuilderAndTopics(builder, topics);
                            builders.put(uuid, builderAndTopics);
                        } else {
                            // Grab topics list
                            topics = builderAndTopics.topics;
                        }
                    }
                    String prefix = rs.getString(7);
                    if (null != prefix) {
                        // E.g. "ox:mail:*"
                        topics.add(prefix + (prefix.endsWith(":") ? "*" : ":*"));
                    }
                    String topic = rs.getString(8);
                    if (null != topic) {
                        // E.g. "ox:mail:new"
                        topics.add(topic);
                    }
                } while (rs.next());

            } catch (SQLException e) {
                throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            } finally {
                Databases.closeSQLStuff(rs, stmt);
            }
        }

        Map<UUID, PushSubscription> subscriptions = new LinkedHashMap<UUID, PushSubscription>(builders.size());
        for (Entry<UUID, BuilderAndTopics> entry : builders.entrySet()) {
            DefaultPushSubscription.Builder builder = entry.getValue().builder;
            builder.topics(entry.getValue().topics);
            subscriptions.put(entry.getKey(), builder.build());
        }
        return filterExpiredSubscriptions(contextId, subscriptions);
    }

    private List<PushSubscription> filterExpiredSubscriptions(int contextId, Map<UUID, PushSubscription> subscriptionsById) {
        if (null == subscriptionsById || subscriptionsById.isEmpty()) {
            return Collections.emptyList();
        }
        List<PushSubscription> subscriptions = new ArrayList<PushSubscription>(subscriptionsById.size());
        Date now = new Date();
        List<byte[]> expiredSubscriptions = null;
        for (Entry<UUID, PushSubscription> entry : subscriptionsById.entrySet()) {
            PushSubscription subscription = entry.getValue();
            Date expires = subscription.getExpires();
            if (null != expires && expires.before(now)) {
                if (expiredSubscriptions == null) {
                    expiredSubscriptions = new ArrayList<byte[]>();
                }
                expiredSubscriptions.add(UUIDs.toByteArray(entry.getKey()));
            } else {
                subscriptions.add(subscription);
            }
        }

        try {
            unregisterSubscriptions(contextId, expiredSubscriptions);
        } catch (Exception e) {
            LOG.error("Error unregistering expired subscriptions", e);
        }
        return subscriptions;
    }

    @Override
    public boolean hasInterestedSubscriptions(int userId, int contextId, String topic) throws OXException {
        return hasInterestedSubscriptions(null, userId, contextId, topic);
    }

    @Override
    public boolean hasInterestedSubscriptions(String clientId, int userId, int contextId, String topic) throws OXException {
        // Check cache
        RdbPushSubscriptionRegistryCache cache = this.cacheReference.get();
        if (null != cache) {
            return cache.getCollectionFor(userId, contextId).hasInterestedSubscriptions(clientId, topic);
        }

        Connection con = databaseService.getReadOnly(contextId);
        try {
            return hasSubscriptions(clientId, userId, contextId, topic, con);
        } finally {
            databaseService.backReadOnly(contextId, con);
        }
    }

    /**
     * Gets all subscriptions interested in specified topic belonging to given user.
     *
     * @param optClient The optional client to filter by
     * @param userId The user identifier
     * @param contextId The context identifier
     * @param topic The topic
     * @param con The connection to use
     * @return All subscriptions for specified affiliation
     * @throws OXException If subscriptions cannot be returned
     */
    public boolean hasSubscriptions(String optClient, int userId, int contextId, String topic, Connection con) throws OXException {
        if (null == con) {
            return hasInterestedSubscriptions(optClient, userId, contextId, topic);
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement(
                "SELECT 1 FROM pns_subscription s" +
                " LEFT JOIN pns_subscription_topic_wildcard twc ON s.id=twc.id" +
                " LEFT JOIN pns_subscription_topic_exact te ON s.id=te.id" +
                " WHERE s.cid=? AND s.user=?" + (null == optClient ? "" : " AND s.client=?") + " AND ((s.all_flag=1) OR (te.topic=?) OR (? LIKE CONCAT(twc.topic, '%')));");
            int pos = 1;
            stmt.setInt(pos++, contextId);
            stmt.setInt(pos++, userId);
            if (null != optClient) {
                stmt.setString(pos++, optClient);
            }
            stmt.setString(pos++, topic);
            stmt.setString(pos, topic);
            rs = stmt.executeQuery();

            return rs.next();
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public MapBackedHits getInterestedSubscriptions(int[] userIds, int contextId, String topic) throws OXException {
        return getInterestedSubscriptions(null, userIds, contextId, topic);
    }

    @Override
    public MapBackedHits getInterestedSubscriptions(String clientId, int[] userIds, int contextId, String topic) throws OXException {
        // Check cache
        RdbPushSubscriptionRegistryCache cache = this.cacheReference.get();
        if (null != cache) {
            HashMap<ClientAndTransport, List<PushMatch>> map = new HashMap<>();
            for (int userId : userIds) {
                MapBackedHits hits = cache.getCollectionFor(userId, contextId).getInterestedSubscriptions(clientId, topic);
                if (null != hits && !hits.isEmpty()) {
                    map.putAll(hits.getMap());
                }
            }
            return new MapBackedHits(map);
        }

        Map<ClientAndTransport, List<PushMatch>> subscriptions;
        Connection con = databaseService.getReadOnly(contextId);
        try {
            subscriptions = getSubscriptions(clientId, userIds, contextId, topic, con);
        } finally {
            databaseService.backReadOnly(contextId, con);
        }
        filterExpired(contextId, subscriptions);
        return null == subscriptions || subscriptions.isEmpty() ? MapBackedHits.EMPTY : new MapBackedHits(subscriptions);
    }

    private void filterExpired(int contextId, Map<ClientAndTransport, List<PushMatch>> subscriptions) {
        if (null != subscriptions && !subscriptions.isEmpty()) {
            Date now = new Date();
            List<byte[]> expiredSubscriptions = null;
            for (Iterator<Entry<ClientAndTransport, List<PushMatch>>> entryIterator = subscriptions.entrySet().iterator(); entryIterator.hasNext();) {
                Entry<ClientAndTransport, List<PushMatch>> entry = entryIterator.next();
                for (Iterator<PushMatch> iterator = entry.getValue().iterator(); iterator.hasNext();) {
                    PushMatch match = iterator.next();
                    if (match instanceof RdbPushMatch rdbMatch) {
                        Date expires = rdbMatch.getExpires();
                        if (null != expires && expires.before(now)) {
                            if (expiredSubscriptions == null) {
                                expiredSubscriptions = new ArrayList<byte[]>();
                            }
                            expiredSubscriptions.add(UUIDs.toByteArray(rdbMatch.getId()));
                            iterator.remove();
                        }
                    }
                }
                if (entry.getValue().isEmpty()) {
                    entryIterator.remove();
                }
            }

            if (null != expiredSubscriptions && !expiredSubscriptions.isEmpty()) {
                try {
                    unregisterSubscriptions(contextId, expiredSubscriptions);
                } catch (Exception e) {
                    LOG.error("Error unregistering expired subscriptions", e);
                }
            }
        }
    }

    private void unregisterSubscriptions(int contextId, List<byte[]> subscriptionIds) throws OXException {
        int rollback = 0;
        boolean modified = false;
        Connection con = databaseService.getWritable(contextId);
        try {
            Databases.startTransaction(con);
            rollback = 1;
            modified = 0 < deleteSubscription(subscriptionIds, con);
            con.commit();
            rollback = 2;
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            if (modified) {
                databaseService.backWritable(contextId, con);
            } else {
                databaseService.backWritableAfterReading(contextId, con);
            }
        }
    }

    /**
     * Gets all subscriptions interested in specified topic belonging to certain users.
     *
     * @param optClient The optional client to filter by
     * @param userIds The user identifiers
     * @param contextId The context identifier
     * @param topic The topic
     * @param con The connection to use
     * @return All subscriptions for specified affiliation
     * @throws OXException If subscriptions cannot be returned
     */
    private Map<ClientAndTransport, List<PushMatch>> getSubscriptions(String optClient, int[] userIds, int contextId, String topic, Connection con) throws OXException {
        if (null == con) {
            return getInterestedSubscriptions(optClient, userIds, contextId, topic).getMap();
        }
        if (null == userIds || 0 == userIds.length) {
            return null;
        }
        StringBuilder sqlBuilder = new StringBuilder()
            .append("SELECT s.id, s.user, s.token, s.client, s.transport, s.last_modified, s.expires, s.all_flag, twc.topic wildcard, te.topic, s.meta FROM pns_subscription s")
            .append(" LEFT JOIN pns_subscription_topic_wildcard twc ON s.id=twc.id")
            .append(" LEFT JOIN pns_subscription_topic_exact te ON s.id=te.id")
            .append(" WHERE s.cid=?")
        ;
        if (1 == userIds.length) {
            sqlBuilder.append(" AND s.user=?");
        } else {
            sqlBuilder.append(" AND s.user IN (?");
            for (int i = 1; i < userIds.length; i++) {
                sqlBuilder.append(",?");
            }
            sqlBuilder.append(')');
        }
        if (null != optClient) {
            sqlBuilder.append(" AND s.client=?");
        }
        sqlBuilder.append(" AND ((s.all_flag=1) OR (te.topic=?) OR (? LIKE CONCAT(twc.topic, '%')));");

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement(sqlBuilder.toString());
            sqlBuilder = null;
            int pos = 1;
            stmt.setInt(pos++, contextId);
            for (int userId : userIds) {
                stmt.setInt(pos++, userId);
            }
            if (null != optClient) {
                stmt.setString(pos++, optClient);
            }
            stmt.setString(pos++, topic);
            stmt.setString(pos, topic);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                return null;
            }

            Map<ClientAndTransport, List<PushMatch>> map = new LinkedHashMap<>(6);
            Set<UUID> processed = new HashSet<>();
            do {
                UUID id = UUIDs.toUUID(rs.getBytes(1));

                // Check if not yet processed
                if (processed.add(id)) {
                    int userId = rs.getInt(2);
                    String token = rs.getString(3);
                    Meta meta = MetaConverter.parseStringToMeta(rs.getString(11));
                    String client = rs.getString(4);
                    String transportId = rs.getString(5);
                    Date lastModified = new Date(rs.getLong(6));
                    long expiresValue = rs.getLong(7);
                    Date expires = rs.wasNull() ? null : new Date(expiresValue);

                    // Determine matching topic
                    String matchingTopic;
                    {
                        boolean all = rs.getInt(8) > 0;
                        if (all) {
                            matchingTopic = KnownTopic.ALL.getName();
                        } else {
                            matchingTopic = rs.getString(9);
                            if (rs.wasNull()) {
                                matchingTopic = null;
                            } else {
                                // E.g. "ox:mail:*"
                                if (topic.startsWith(matchingTopic)) {
                                    matchingTopic = new StringBuilder(matchingTopic).append('*').toString();
                                } else {
                                    // Unsatisfiable match
                                    matchingTopic = null;
                                }
                            }

                            if (null == matchingTopic) { // Last reason why that row is present in result set
                                // E.g. "ox:mail:new"
                                matchingTopic = rs.getString(10);
                            }
                        }
                    }

                    // Add to appropriate list
                    RdbPushMatch pushMatch = new RdbPushMatch(id, userId, contextId, client, transportId, DefaultToken.builder().withValue(token).withMeta(meta).build(), matchingTopic, lastModified, expires);
                    ClientAndTransport cat = new ClientAndTransport(client, transportId);
                    com.openexchange.tools.arrays.Collections.put(map, cat, pushMatch);
                }
            } while (rs.next());

            return map;
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private static byte[] getSubscriptionId(int userId, int contextId, String token, String client, Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `id` FROM `pns_subscription` WHERE `cid`=? AND `user`=? AND `token`=? AND `client`=?");
            stmt.setInt(1, contextId);
            stmt.setInt(2, userId);
            stmt.setString(3, token);
            stmt.setString(4, client);
            rs = stmt.executeQuery();
            return !rs.next() ? null : rs.getBytes(1);
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    @Override
    public PushSubscriptionResult registerSubscription(PushSubscriptionDescription desc, PushSubscriptionRestrictions restrictions) throws OXException {
        if (null == desc) {
            return PushSubscriptionResult.builder().withError(OXException.general("subscription must not be null")).build();
        }

        int contextId = desc.getContextId();
        BoolReference modified = new BoolReference(false);
        Connection con = databaseService.getWritable(contextId);
        try {
            return registerSubscription(desc, restrictions, con, modified);
        } finally {
            if (modified.getValue()) {
                databaseService.backWritable(contextId, con);
            } else {
                databaseService.backWritableAfterReading(contextId, con);
            }
        }
    }

    /** The lock name for push subscription registration */
    private static final String LOCK_NAME_REGISTRATION = "lock.registration";

    /**
     * Registers or updates specified subscription.
     *
     * @param subscription The subscription to register
     * @param restrictions Possible restrictions for the subscription
     * @param con The connection to use
     * @param modified The boolean reference to signal whether connection was actually used for database modification
     * @throws OXException If registration fails
     */
    public PushSubscriptionResult registerSubscription(PushSubscriptionDescription desc, PushSubscriptionRestrictions restrictions, Connection con, BoolReference modified) throws OXException {
        if (null == con) {
            return registerSubscription(desc, restrictions);
        }

        PushSubscription subscription = DefaultPushSubscription.instanceFor(desc); // Yield immutable representation
        PreparedStatement stmt = null;
        ResultSet rs = null;
        DatabaseLock databaseLock = null;
        try {
            StringBuilder sqlBuilder = new StringBuilder("SELECT `cid`, `user` FROM `pns_subscription` WHERE `token`=? AND `transport`=? AND `client`=?");
            if (restrictions.isAllowSharedToken()) {
                // Only match subscription of this user if shared tokens are allowed
                sqlBuilder.append(" AND `cid`=? AND `user`=?");
            }
            stmt = con.prepareStatement(sqlBuilder.toString());
            sqlBuilder = null;
            stmt.setString(1, subscription.getToken().getValue());
            stmt.setString(2, subscription.getTransportId());
            stmt.setString(3, subscription.getClient());
            if (restrictions.isAllowSharedToken()) {
                // Only match subscription of this user if shared tokens are allowed
                stmt.setInt(4, subscription.getContextId());
                stmt.setInt(5, subscription.getUserId());
            }
            rs = stmt.executeQuery();
            if (rs.next()) {
                // Such a token has already been registered for given transport and client. Check to what user that token belongs.
                int tokenUsingContextId = rs.getInt(1);
                int tokenUsingUserId = rs.getInt(2);
                if (tokenUsingContextId == subscription.getContextId() && tokenUsingUserId == subscription.getUserId()) {
                    // Actually an update, but may force to invalidate some resources as new registration might ship with other topics. Therefore:
                    return PushSubscriptionResult.builder().withConflictingUserId(tokenUsingUserId, tokenUsingContextId).build();
                }
                return PushSubscriptionResult.builder().withConflictingUserId(tokenUsingUserId, tokenUsingContextId).build();
            }
            Databases.closeSQLStuff(rs, stmt);
            stmt = null;
            rs = null;

            // Acquire lock to avoid concurrency issues
            int loopCount = 1;
            do {
                databaseLock = acquireLock(LOCK_NAME_REGISTRATION, subscription, con);
                if (databaseLock == null) {
                    exponentialBackoffWait(loopCount++, 1000L);
                }
            } while (databaseLock == null);
            modified.setValue(true); // Through acquiring lock

            if (restrictions.getMaxNumberOfSuchSubscription() > 0) {
                stmt = con.prepareStatement("SELECT COUNT(`id`) AS number FROM `pns_subscription` WHERE `cid`=? AND `user`=? AND `transport`=? AND `client`=? AND `token`<>?");
                stmt.setInt(1, subscription.getContextId());
                stmt.setInt(2, subscription.getUserId());
                stmt.setString(3, subscription.getTransportId());
                stmt.setString(4, subscription.getClient());
                stmt.setString(5, LOCK_NAME_REGISTRATION);
                rs = stmt.executeQuery();
                int count = rs.next() ? rs.getInt(1) : 0;
                Databases.closeSQLStuff(rs, stmt);
                stmt = null;
                rs = null;

                if (count >= restrictions.getMaxNumberOfSuchSubscription()) {
                    LOG.warn("Denied subscription: Too many subscriptions for transport {} and client {} of user {} and context {}",
                        subscription.getTransportId(), subscription.getClient(), I(subscription.getUserId()), I(subscription.getContextId()));
                    OXException error = PushExceptionCodes.PUSH_SUBSCRIPTION_MAX_NUMBER_EXCEEDED.create(subscription.getTransportId(), subscription.getClient());
                    return PushSubscriptionResult.builder().withError(error).build();
                }
            }

            List<String> prefixes = null;
            List<String> topics = null;
            boolean isAll = false;
            for (Iterator<String> iter = subscription.getTopics().iterator(); !isAll && iter.hasNext();) {
                String topic = iter.next();
                if (KnownTopic.ALL.getName().equals(topic)) {
                    isAll = true;
                } else {
                    validateTopicName(topic);
                    if (topic.endsWith(":*")) {
                        // Wild-card topic: we remove the *
                        if (null == prefixes) {
                            prefixes = new LinkedList<>();
                        }
                        prefixes.add(topic.substring(0, topic.length() - 1));
                    } else {
                        // Exact match
                        if (null == topics) {
                            topics = new LinkedList<>();
                        }
                        topics.add(topic);
                    }
                }
            }

            // Obtain existent identifier (if any)
            byte[] id = getSubscriptionId(subscription.getUserId(), subscription.getContextId(), subscription.getToken().getValue(), subscription.getClient(), con);

            // Insert new or update existing subscription (within a database transaction)
            int rollback = 0;
            try {
                Databases.startTransaction(con);
                rollback = 1;

                long lastModified = System.currentTimeMillis();
                if (id == null) {
                    id = UUIDs.toByteArray(UUID.randomUUID());
                    stmt = con.prepareStatement(
                        "INSERT INTO `pns_subscription` (`id`,`cid`,`user`,`token`,`client`,`transport`,`all_flag`,`last_modified`,`expires`,`meta`) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?)"
                    );
                    stmt.setBytes(1, id);
                    stmt.setInt(2, subscription.getContextId());
                    stmt.setInt(3, subscription.getUserId());
                    stmt.setString(4, subscription.getToken().getValue());
                    stmt.setString(5, subscription.getClient());
                    stmt.setString(6, subscription.getTransportId());
                    stmt.setInt(7, isAll ? 1 : 0);
                    stmt.setLong(8, lastModified);
                    if (null == subscription.getExpires()) {
                        stmt.setNull(9, Types.BIGINT);
                    } else {
                        long expiration = subscription.getExpires().getTime();
                        stmt.setLong(9, expiration);
                    }
                    Optional<String> optional = MetaConverter.convertMetaToString(subscription.getToken().getOptionalMeta().orElse(null));
                    if (optional.isPresent()) {
                        stmt.setString(10, optional.get());
                    } else {
                        stmt.setNull(10, Types.VARCHAR);
                    }
                    int rowCount = stmt.executeUpdate();
                    if (rowCount <= 0) {
                        // Dropped in the meantime
                        return PushSubscriptionResult.builder().withError(OXException.general("Token registration failed")).build();
                    }
                    Databases.closeSQLStuff(stmt);
                    stmt = null;
                } else {
                    stmt = con.prepareStatement("UPDATE `pns_subscription` SET `transport`=?, `all_flag`=?, `last_modified`=?, `expires`=?, `meta`=? WHERE `cid`=? AND `user`=? AND `token`=? AND `client`=?");
                    stmt.setString(1, subscription.getTransportId());
                    stmt.setInt(2, isAll ? 1 : 0);
                    stmt.setLong(3, lastModified);
                    if (null == subscription.getExpires()) {
                        stmt.setNull(4, Types.BIGINT);
                    } else {
                        long expiration = subscription.getExpires().getTime();
                        stmt.setLong(4, expiration);
                    }
                    Optional<String> optional = MetaConverter.convertMetaToString(subscription.getToken().getOptionalMeta().orElse(null));
                    if (optional.isPresent()) {
                        stmt.setString(5, optional.get());
                    } else {
                        stmt.setNull(5, Types.VARCHAR);
                    }
                    stmt.setInt(6, subscription.getContextId());
                    stmt.setInt(7, subscription.getUserId());
                    stmt.setString(8, subscription.getToken().getValue());
                    stmt.setString(9, subscription.getClient());
                    int rowCount = stmt.executeUpdate();
                    if (rowCount <= 0) {
                        // Dropped in the meantime
                        return PushSubscriptionResult.builder().withError(OXException.general("Token registration failed")).build();
                    }
                    Databases.closeSQLStuff(stmt);
                    stmt = null;

                    // Drop possibly existing entries
                    stmt = con.prepareStatement("DELETE FROM `pns_subscription_topic_exact` WHERE `id`=?");
                    stmt.setBytes(1, id);
                    stmt.executeUpdate();
                    Databases.closeSQLStuff(stmt);
                    stmt = null;

                    stmt = con.prepareStatement("DELETE FROM `pns_subscription_topic_wildcard` WHERE `id`=?");
                    stmt.setBytes(1, id);
                    stmt.executeUpdate();
                    Databases.closeSQLStuff(stmt);
                    stmt = null;
                }

                // Insert individual topics / topic wild-cards (if subscription is not interested in all)
                if (!isAll) {
                    if (null != prefixes) {
                        stmt = con.prepareStatement("INSERT IGNORE INTO `pns_subscription_topic_wildcard` (`id`, `cid`, `topic`) VALUES (?, ?, ?)");
                        stmt.setBytes(1, id);
                        stmt.setInt(2, subscription.getContextId());
                        for (String prefix : prefixes) {
                            stmt.setString(3, prefix);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                        Databases.closeSQLStuff(stmt);
                        stmt = null;
                    }

                    if (null != topics) {
                        stmt = con.prepareStatement("INSERT IGNORE INTO `pns_subscription_topic_exact` (`id`, `cid`, `topic`) VALUES (?, ?, ?)");
                        stmt.setBytes(1, id);
                        stmt.setInt(2, subscription.getContextId());
                        for (String topic : topics) {
                            stmt.setString(3, topic);
                            stmt.addBatch();
                        }
                        stmt.executeBatch();
                        Databases.closeSQLStuff(stmt);
                        stmt = null;
                    }
                }

                // Insert into in-memory collection as well
                RdbPushSubscriptionRegistryCache cache = this.cacheReference.get();
                if (null != cache) {
                    cache.addAndInvalidateIfPresent(subscription);
                }

                con.commit();
                rollback = 2;
                modified.setValue(true);
            } finally {
                if (rollback > 0) {
                    if (rollback == 1) {
                        Databases.rollback(con);
                    }
                    Databases.autocommit(con);
                }
            }
            return PushSubscriptionResult.OK_RESULT;
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
            if (databaseLock != null) {
                databaseLock.unlock();
            }
        }
    }

    /**
     * Performs a wait according to exponential back-off strategy.
     * <pre>
     * (retry-count * base-millis) + random-millis
     * </pre>
     *
     * @param retryCount The current number of retries
     * @param baseMillis The base milliseconds
     */
    private static void exponentialBackoffWait(int retryCount, long baseMillis) {
        long nanosToWait = TimeUnit.NANOSECONDS.convert((retryCount * baseMillis) + ((long) (Math.random() * baseMillis)), TimeUnit.MILLISECONDS);
        LockSupport.parkNanos(nanosToWait);
    }

    private DatabaseLock acquireLock(String lockName, PushSubscription subscription, Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `last_modified`, `id` FROM `pns_subscription` WHERE `cid`=? AND `user`=? AND `token`=? AND `transport`=? AND `client`=?");
            stmt.setInt(1, subscription.getContextId());
            stmt.setInt(2, subscription.getUserId());
            stmt.setString(3, lockName);
            stmt.setString(4, subscription.getTransportId());
            stmt.setString(5, subscription.getClient());
            rs = stmt.executeQuery();
            long stamp;
            UUID id;
            if (rs.next()) {
                stamp = Long.parseLong(rs.getString(1));
                id = UUIDs.toUUID(rs.getBytes(2));
            } else {
                stamp = 0L;
                id = null;
            }
            closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;

            long now = System.currentTimeMillis();
            int rows;
            if (id == null) {
                // No lock entry
                id = UUID.randomUUID();
                stmt = con.prepareStatement("INSERT INTO `pns_subscription` (`id`,`cid`,`user`,`token`,`transport`,`client`,`last_modified`) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `cid`=`cid`");
                stmt.setBytes(1, UUIDs.toByteArray(id));
                stmt.setInt(2, subscription.getContextId());
                stmt.setInt(3, subscription.getUserId());
                stmt.setString(4, lockName);
                stmt.setString(5, subscription.getTransportId());
                stmt.setString(6, subscription.getClient());
                stmt.setLong(7, now);
                rows = stmt.executeUpdate();
                closeSQLStuff(stmt);
                stmt = null;
            } else if (now - stamp > 20000L) { // Expired if last-modified is older than 20 seconds
                // Lock entry expired
                stmt = con.prepareStatement("UPDATE `pns_subscription` SET `last_modified`=? WHERE `id`=? AND `last_modified`=?");
                stmt.setString(1, Long.toString(now));
                stmt.setBytes(2, UUIDs.toByteArray(id));
                stmt.setLong(3, stamp);
                rows = stmt.executeUpdate();
                closeSQLStuff(stmt);
                stmt = null;
            } else {
                // Still locked...
                rows = 0;
            }

            if (rows <= 0) {
                return null;
            }

            ScheduledTimerTask timerTask = null;
            boolean error = true;
            try {
                UUID idToRefresh = id;
                Runnable task = () -> refreshLock(idToRefresh, lockName, subscription, databaseService);
                long refreshIntervalMillis = 5000L; // Refresh every 5 seconds
                timerTask = timerService.scheduleWithFixedDelay(task, refreshIntervalMillis, refreshIntervalMillis, TimeUnit.MILLISECONDS);

                DatabaseLock lock = new DatabaseLock(id, lockName, subscription, databaseService, timerTask);
                LOG.debug("Successfully acquired push subscription lock '{}' ({}) for transport {} and client {} of user {} in context {}", lockName, UUIDs.getUnformattedStringObjectFor(id), subscription.getTransportId(), subscription.getClient(), I(subscription.getUserId()), I(subscription.getContextId()));
                error = false;
                return lock;
            } finally {
                if (error) {
                    deleteLock(id, lockName, subscription, databaseService);
                    if (timerTask != null) { // NOSONARLINT
                        timerTask.cancel();
                    }
                }
            }
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            closeSQLStuff(rs, stmt);
        }
    }

    private static void deleteLock(UUID id, String lockName, PushSubscription subscription, DatabaseService databaseService) {
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = databaseService.getWritable(subscription.getContextId());
            stmt = con.prepareStatement("DELETE FROM `pns_subscription` WHERE `id`=?");
            stmt.setBytes(1, UUIDs.toByteArray(id));
            stmt.executeUpdate();
        } catch (Exception e) {
            LOG.warn("Failed to delete push subscription lock '{}' ({}) for transport {} and client {} of user {} in context {}", lockName, UUIDs.getUnformattedStringObjectFor(id), subscription.getTransportId(), subscription.getClient(), I(subscription.getUserId()), I(subscription.getContextId()), e);
        } finally {
            closeSQLStuff(stmt);
            if (con != null) {
                databaseService.backWritable(subscription.getContextId(), con);
            }
        }
    }

    private static void refreshLock(UUID id, String lockName, PushSubscription subscription, DatabaseService databaseService) {
        PreparedStatement stmt = null;
        Connection con = null;
        try {
            con = databaseService.getWritable(subscription.getContextId());
            stmt = con.prepareStatement("UPDATE `pns_subscription` SET `last_modified`=? WHERE `id`=?");
            stmt.setString(1, Long.toString(System.currentTimeMillis()));
            stmt.setBytes(2, UUIDs.toByteArray(id));
            stmt.executeUpdate();
        } catch (Exception e) {
            LOG.warn("Failed to update push subscription lock '{}' ({}) for transport {} and client {} of user {} in context {}", lockName, UUIDs.getUnformattedStringObjectFor(id), subscription.getTransportId(), subscription.getClient(), I(subscription.getUserId()), I(subscription.getContextId()), e);
        } finally {
            closeSQLStuff(stmt);
            if (con != null) {
                databaseService.backWritable(subscription.getContextId(), con);
            }
        }
    }

    private static void validateTopicName(String topic) throws OXException {
        try {
            PushNotifications.validateTopicName(topic);
        } catch (IllegalArgumentException e) {
            throw PushExceptionCodes.INVALID_TOPIC.create(e, topic);
        }
    }

    @Override
    public boolean unregisterSubscription(PushSubscriptionDescription subscription, PushSubscriptionRestrictions restrictions) throws OXException {
        if (null == subscription) {
            return false;
        }

        return null != removeSubscription(DefaultPushSubscription.instanceFor(subscription), restrictions);
    }

    /**
     * Unregisters specified subscription.
     *
     * @param subscription The subscription to unregister
     * @param restrictions Possible restrictions for the subscription
     * @throws OXException If unregistration fails
     */
    public PushSubscription removeSubscription(PushSubscription subscription, PushSubscriptionRestrictions restrictions) throws OXException {
        if (null == subscription) {
            return null;
        }

        int contextId = subscription.getContextId();
        Connection con = databaseService.getWritable(contextId);
        int rollback = 0;
        PushSubscription deleted = null;
        try {
            Databases.startTransaction(con);
            rollback = 1;

            deleted = removeSubscription(subscription, restrictions, con);

            con.commit();
            rollback = 2;

            return deleted;
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            if (null != deleted) {
                databaseService.backWritable(contextId, con);
            } else {
                databaseService.backWritableAfterReading(contextId, con);
            }
        }
    }

    /**
     * Unregisters specified subscription.
     *
     * @param subscription The subscription to unregister
     * @param restrictions Possible restrictions for the subscription
     * @param con The connection to use
     * @throws OXException If unregistration fails
     */
    public PushSubscription removeSubscription(PushSubscription subscription, PushSubscriptionRestrictions restrictions, Connection con) throws OXException {
        if (null == con) {
            return removeSubscription(subscription, restrictions);
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            String client = subscription.getClient();
            if (null == client) {
                stmt = con.prepareStatement("SELECT `id` FROM `pns_subscription` WHERE `cid`=? AND `user`=? AND `token`=?");
                stmt.setInt(1, subscription.getContextId());
                stmt.setInt(2, subscription.getUserId());
                stmt.setString(3, subscription.getToken().getValue());
            } else {
                stmt = con.prepareStatement("SELECT `id` FROM `pns_subscription` WHERE `cid`=? AND `user`=? AND `token`=? AND `client`=?");
                stmt.setInt(1, subscription.getContextId());
                stmt.setInt(2, subscription.getUserId());
                stmt.setString(3, subscription.getToken().getValue());
                stmt.setString(4, client);
            }
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return null;
            }

            byte[] id = rs.getBytes(1);
            Databases.closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;

            stmt = con.prepareStatement("SELECT s.cid, s.user, s.token, s.client, s.transport, s.last_modified, s.expires, s.all_flag, twc.topic wildcard, te.topic, s.meta FROM pns_subscription s LEFT JOIN pns_subscription_topic_wildcard twc ON s.id=twc.id LEFT JOIN pns_subscription_topic_exact te ON s.id=te.id WHERE s.id=?");
            stmt.setBytes(1, id);
            rs = stmt.executeQuery();

            // Select first row and build subscription instance
            rs.next();
            Token token = DefaultToken.builder().withValue(rs.getString(3)).withMeta(MetaConverter.parseStringToMeta(rs.getString(11))).build();
            DefaultPushSubscription.Builder removedSubscription = DefaultPushSubscription.builder().contextId(rs.getInt(1)).userId(rs.getInt(2)).token(token).client(rs.getString(4)).transportId(rs.getString(5));
            long expires = rs.getLong(7); // expires
            if (!rs.wasNull()) {
                removedSubscription.expires(new Date(expires));
            }

            // Collect topics
            {
                List<String> topics = new LinkedList<>();
                if (rs.getInt(8) > 0) {
                    topics.add(KnownTopic.ALL.getName());
                }

                Set<String> setTopics = new LinkedHashSet<>();
                Set<String> setWildcards = new LinkedHashSet<>();
                do {
                    String wildcard = rs.getString(9); // wildcard
                    if (!rs.wasNull()) {
                        setWildcards.add(wildcard);
                    }

                    String topic = rs.getString(10); // topic
                    if (!rs.wasNull()) {
                        setTopics.add(topic);
                    }
                } while (rs.next());
                for (String wildcard : setWildcards) {
                    topics.add(wildcard + "*");
                }
                for (String topic : setTopics) {
                    topics.add(topic);
                }
                removedSubscription.topics(topics);
            }
            Databases.closeSQLStuff(rs, stmt);
            rs = null;
            stmt = null;

            boolean deleted = deleteById(id, null, con);
            if (deleted) {
                // Remove from in-memory collection as well
                RdbPushSubscriptionRegistryCache cache = this.cacheReference.get();
                if (null != cache) {
                    cache.removeAndInvalidateIfPresent(subscription);
                }
            }

            return removedSubscription.build();
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    /**
     * Deletes the subscription identified by given ID.
     *
     * @param id The ID
     * @param cleanUpTask The optional clean-up task
     * @param con The connection to use
     * @return <code>true</code> if such a subscription was deleted; otherwise <code>false</code>
     * @throws OXException If delete attempt fails
     */
    public boolean deleteById(byte[] id, Runnable cleanUpTask, Connection con) throws OXException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("DELETE FROM `pns_subscription_topic_exact` WHERE `id`=?");
            stmt.setBytes(1, id);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);

            stmt = con.prepareStatement("DELETE FROM `pns_subscription_topic_wildcard` WHERE `id`=?");
            stmt.setBytes(1, id);
            stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);

            stmt = con.prepareStatement("DELETE FROM `pns_subscription` WHERE `id`=?");
            stmt.setBytes(1, id);
            int rows = stmt.executeUpdate();
            Databases.closeSQLStuff(stmt);
            stmt = null;

            if (null != cleanUpTask) {
                cleanUpTask.run();
            }

            return rows > 0;
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw PushExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    @Override
    public int unregisterSubscription(String token, String transportId) throws OXException {
        if (null == token || null == transportId) {
            return 0;
        }

        int removed = 0;
        for (Integer iContextId : contextService.getDistinctContextsPerSchema()) {
            // Delete for whole schema using connection for representative context
            int contextId = iContextId.intValue();
            Connection con = databaseService.getWritable(contextId);
            int rollback = 0;
            boolean modified = false;
            try {
                List<byte[]> ids = getSubscriptionIds(contextId, token, transportId, con);

                if (!ids.isEmpty()) {
                    Databases.startTransaction(con);
                    rollback = 1;

                    int numDeleted = deleteSubscription(ids, con);
                    modified = numDeleted > 0;
                    removed += numDeleted;

                    con.commit();
                    rollback = 2;
                }
            } catch (SQLException e) {
                throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            } finally {
                if (rollback > 0) {
                    if (rollback == 1) {
                        Databases.rollback(con);
                    }
                    Databases.autocommit(con);
                }
                if (modified) {
                    databaseService.backWritable(contextId, con);
                } else {
                    databaseService.backWritableAfterReading(contextId, con);
                }
            }
        }
        return removed;
    }

    private static List<byte[]> getSubscriptionIds(int contextId, String token, String transportId, Connection con) throws OXException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            stmt = con.prepareStatement("SELECT `id` FROM `pns_subscription` WHERE `cid`=? AND `transport`=? AND `token`=?");
            stmt.setInt(1, contextId);
            stmt.setString(2, transportId);
            stmt.setString(3, token);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                return Collections.emptyList();
            }

            List<byte[]> ids = new LinkedList<>();
            do {
                ids.add(rs.getBytes(1));
            } while (rs.next());
            return ids;
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(rs, stmt);
        }
    }

    private int deleteSubscription(List<byte[]> ids, Connection con) throws OXException {
        int deleted = 0;
        for (byte[] id : ids) {
            if (deleteById(id, null, con)) {
                deleted++;
            }
        }

        // Clear cache
        RdbPushSubscriptionRegistryCache cache = this.cacheReference.get();
        if (null != cache) {
            cache.clear(true);
        }

        return deleted;
    }

    @Override
    public boolean updateToken(PushSubscriptionDescription subscription, String newToken) throws OXException {
        if (null == subscription || null == newToken) {
            return false;
        }

        int contextId = subscription.getContextId();
        Connection con = databaseService.getWritable(contextId);
        try {
            return updateToken(subscription, newToken, con);
        } finally {
            databaseService.backWritable(contextId, con);
        }
    }

    /**
     * Updates specified subscription's token (and last-modified time stamp).
     *
     * @param subscription The subscription to update
     * @param newToken The new token to set
     * @param con The connection to use
     * @return <code>true</code> if such a subscription has been updated; otherwise <code>false</code> if no such subscription existed
     * @throws OXException If update fails
     */
    public boolean updateToken(PushSubscriptionDescription subscription, String newToken, Connection con) throws OXException {
        if (null == con) {
            return updateToken(subscription, newToken);
        }

        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement("UPDATE `pns_subscription` SET `token`=?, `last_modified`=? WHERE `cid`=? AND `user`=? AND `token`=? AND `client`=?");
            stmt.setString(1, newToken);
            stmt.setLong(2, System.currentTimeMillis());
            stmt.setInt(3, subscription.getContextId());
            stmt.setInt(4, subscription.getUserId());
            stmt.setString(5, subscription.getToken().getValue());
            stmt.setString(6, subscription.getClient());
            int rows = stmt.executeUpdate();
            boolean updated = rows > 0;

            if (updated) {
                RdbPushSubscriptionRegistryCache cache = this.cacheReference.get();
                if (null != cache) {
                    cache.dropFor(subscription.getUserId(), subscription.getContextId());
                }
            }

            return updated;
        } catch (SQLException e) {
            throw PushExceptionCodes.SQL_ERROR.create(e, e.getMessage());
        } finally {
            Databases.closeSQLStuff(stmt);
        }
    }

    // -----------------------------------------------------------------------------------------------------

    private static final class BuilderAndTopics {

        final DefaultPushSubscription.Builder builder;
        final List<String> topics;

        BuilderAndTopics(Builder builder, List<String> topics) {
            super();
            this.builder = builder;
            this.topics = topics;
        }
    }

    private static class DatabaseLock {

        private final UUID id;
        private final String lockName;
        private final PushSubscription subscription;
        private final ScheduledTimerTask timerTask;
        private final DatabaseService databaseService;

        DatabaseLock(UUID id, String lockName, PushSubscription subscription, DatabaseService databaseService, ScheduledTimerTask timerTask) {
            super();
            this.id = id;
            this.lockName = lockName;
            this.subscription = subscription;
            this.databaseService = databaseService;
            this.timerTask = timerTask;
        }

        void unlock() {
            if (timerTask != null) {
                timerTask.cancel();
            }
            deleteLock(id, lockName, subscription, databaseService);
        }
    }

}
