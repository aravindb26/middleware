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

package com.openexchange.pubsub.redis;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.java.BufferingQueue;
import com.openexchange.java.util.Pair;
import com.openexchange.java.util.UUIDs;
import com.openexchange.marker.KnownThreadLocalValue;
import com.openexchange.marker.OXThreadMarkers;
import com.openexchange.pubsub.Channel;
import com.openexchange.pubsub.ChannelKey;
import com.openexchange.pubsub.ChannelListener;
import com.openexchange.pubsub.Message;
import com.openexchange.pubsub.MessageCollectionId;
import com.openexchange.pubsub.PubSubExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;
import io.lettuce.core.pubsub.RedisPubSubListener;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands;
import io.lettuce.core.pubsub.api.sync.RedisPubSubCommands;

/**
 * {@link NativeRedisChannel} - The native Redis Cluster channel publishing and listening to String messages.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class NativeRedisChannel implements Channel<String> {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(NativeRedisChannel.class);

    /**
     * The delay for pooled messages.
     */
    private static final long DELAY_MSEC = 5000L;

    /**
     * The frequency to check for delayed pooled messages.
     */
    private static final int DELAY_FREQUENCY = 3000;

    private static final Object PRESENT = new Object();

    private final RedisPubSubConnectionProvider mConnectionProvider;
    private final ChannelKey channelKey;
    private final ConcurrentMap<ChannelListener<String>, RedisPubSubListenerImpl> listeners;
    private final ConcurrentMap<MessageCollectionId, ConcurrentMap<String, Object>> messageCollections;
    private final AtomicReference<Future<StatefulRedisPubSubConnection<String, JSONObject>>> subscribeConnectionReference;
    private final BufferingQueue<String> publishQueue;
    private final AtomicReference<ScheduledTimerTask> timerTaskReference;
    private final UUID instanceId;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link NativeRedisChannel}.
     *
     * @param channelKey The channel key
     * @param connectionProvider The provider for the pub/sub connection
     * @param instanceId The unique identifier in cluster
     * @param services The service look-up
     */
    public NativeRedisChannel(ChannelKey channelKey, RedisPubSubConnectionProvider connectionProvider, UUID instanceId, ServiceLookup services) {
        super();
        this.channelKey = channelKey;
        this.mConnectionProvider = connectionProvider;
        this.instanceId = instanceId;
        this.services = services;
        listeners = new ConcurrentHashMap<>();
        messageCollections = new ConcurrentHashMap<>(16, 0.9F, 1);
        subscribeConnectionReference = new AtomicReference<>(null);
        publishQueue = new BufferingQueue<>(DELAY_MSEC);
        timerTaskReference = new AtomicReference<>();
    }

    @Override
    public UUID getInstanceId() {
        return instanceId;
    }

    /**
     * Gets the key of this channel.
     *
     * @return The channel key
     */
    public ChannelKey getChannelKey() {
        return channelKey;
    }

    /**
     * Shuts down this channel.
     */
    @SuppressWarnings("unused")
    public void shutdown() {
        List<String> messages = publishQueue.toListAndClear();

        try {
            publishNow(messages.isEmpty() ? NO_EXPIRED_MESSAGES : messages);
        } catch (Exception x) {
            // Ignore
        }

        Future<StatefulRedisPubSubConnection<String, JSONObject>> f = subscribeConnectionReference.getAndSet(null);
        if (f == null) {
            return;
        }
        try {
            StatefulRedisPubSubConnection<String, JSONObject> connection = f.get();

            for (Iterator<RedisPubSubListenerImpl> it = listeners.values().iterator(); it.hasNext();) {
                RedisPubSubListenerImpl redisListener = it.next();
                connection.removeListener(redisListener);
                it.remove();
            }

            RedisPubSubCommands<String, JSONObject> sync = connection.sync();
            sync.unsubscribe(channelKey.getFQN());

            connection.close();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            /* ignore */
        }
    }

    /**
     * Optionally gets the singleton subscribe connection for this channel.
     *
     * @return The subscribe connection or <code>null</code>
     * @throws OXException If subscribe connection cannot be returned
     */
    protected StatefulRedisPubSubConnection<String, JSONObject> optSubscribeConnection() throws OXException {
        Future<StatefulRedisPubSubConnection<String, JSONObject>> f = subscribeConnectionReference.get();
        return f == null ? null : getFromFuture(f);
    }

    /**
     * Gets the singleton subscribe connection for this channel.
     *
     * @return The subscribe connection
     * @throws OXException If subscribe connection cannot be returned
     */
    protected StatefulRedisPubSubConnection<String, JSONObject> getSubscribeConnection() throws OXException {
        Future<StatefulRedisPubSubConnection<String, JSONObject>> f = subscribeConnectionReference.get();
        if (f != null) {
            return getFromFuture(f);
        }

        RedisPubSubConnectionProvider connectionProvider = this.mConnectionProvider;
        Callable<StatefulRedisPubSubConnection<String, JSONObject>> callable = () -> {
            StatefulRedisPubSubConnection<String, JSONObject> connection = connectionProvider.newPubSubConnection();
            RedisPubSubCommands<String, JSONObject> sync = connection.sync();
            sync.subscribe(channelKey.getFQN());
            return connection;
        };

        FutureTask<StatefulRedisPubSubConnection<String, JSONObject>> ft = new FutureTask<>(callable);
        if (subscribeConnectionReference.compareAndSet(null, ft)) {
            ft.run();
            return getFromFuture(ft);
        }

        // Apparently no more null
        return getFromFuture(subscribeConnectionReference.get());
    }

    @Override
    public MessageCollectionId startMessageCollection(Optional<String> optionalInitialMessage) throws OXException {
        Optional<Pair<MessageCollectionId, ConcurrentMap<String, Object>>> optMessageCollectionForThread = optMessageCollectionForThread();
        if (optMessageCollectionForThread.isPresent()) {
            Pair<MessageCollectionId, ConcurrentMap<String, Object>> pair = optMessageCollectionForThread.get();
            if (optionalInitialMessage.isPresent()) {
                pair.getSecond().put(optionalInitialMessage.get(), PRESENT);
            }
            return pair.getFirst();
        }

        MessageCollectionId collectionId = new MessageCollectionId(UUID.randomUUID(), channelKey);
        ConcurrentMap<String, Object> messageQueue = messageCollections.get(collectionId);
        if (messageQueue == null) {
            messageQueue = getMessageQueue(collectionId);
        }
        if (optionalInitialMessage.isPresent()) {
            messageQueue.put(optionalInitialMessage.get(), PRESENT);
        }
        return collectionId;
    }

    /**
     * Retrieves the message queue from the specified collection id
     *
     * @param collectionId The message collection id
     * @return The message queue for the specified id
     */
    private ConcurrentMap<String, Object> getMessageQueue(MessageCollectionId collectionId) {
        ConcurrentMap<String, Object> newQueue = new ConcurrentHashMap<>();
        ConcurrentMap<String, Object> messageQueue = messageCollections.putIfAbsent(collectionId, newQueue);
        if (messageQueue != null) {
            return messageQueue;
        }
        Map<ChannelKey, Queue<MessageCollectionId>> threadLocalCollections = OXThreadMarkers.getThreadLocalValue(KnownThreadLocalValue.CACHE_MESSAGE_COLLECTIONS.getIdentifier());
        if (threadLocalCollections == null) {
            threadLocalCollections = new HashMap<>();
            OXThreadMarkers.putThreadLocalValue(KnownThreadLocalValue.CACHE_MESSAGE_COLLECTIONS.getIdentifier(), threadLocalCollections);
        }
        Queue<MessageCollectionId> collectionIds = threadLocalCollections.get(channelKey);
        if (collectionIds == null) {
            collectionIds = new LinkedList<>();
            threadLocalCollections.put(channelKey, collectionIds);
        }
        collectionIds.offer(collectionId);
        return newQueue;
    }

    @Override
    public void addToMessageCollection(String message, MessageCollectionId collectionId) throws OXException {
        ConcurrentMap<String, Object> messageQueue = messageCollections.get(collectionId);
        if (messageQueue == null) {
            throw PubSubExceptionCode.NO_SUCH_MESSAGE_COLLECTION.create(collectionId);
        }
        messageQueue.putIfAbsent(message, PRESENT);
    }

    @Override
    public void releaseMessageCollection(MessageCollectionId collectionId) throws OXException {
        if (collectionId == null) {
            return;
        }

        if (!collectionId.getChannelKey().equals(channelKey)) {
            // Wrong channel...
            throw PubSubExceptionCode.NO_SUCH_MESSAGE_COLLECTION.create(collectionId);
        }

        ConcurrentMap<String, Object> messageQueue = messageCollections.remove(collectionId);
        if (messageQueue == null) {
            throw PubSubExceptionCode.NO_SUCH_MESSAGE_COLLECTION.create(collectionId);
        }

        removeLocalThreadValue(collectionId);
        doPublish(messageQueue.keySet(), false);
    }

    @Override
    public void dropMessageCollection(MessageCollectionId collectionId) throws OXException {
        if (collectionId == null) {
            return;
        }

        if (!collectionId.getChannelKey().equals(channelKey)) {
            // Wrong channel...
            throw PubSubExceptionCode.NO_SUCH_MESSAGE_COLLECTION.create(collectionId);
        }

        messageCollections.remove(collectionId);
        removeLocalThreadValue(collectionId);
    }

    @Override
    public void publish(Collection<String> messages) throws OXException {
        doPublish(messages, true);
    }

    /**
     * Removes the thread-local value associated with given collection id
     *
     * @param collectionId The collection id
     */
    private void removeLocalThreadValue(MessageCollectionId collectionId) {
        Map<ChannelKey, Queue<MessageCollectionId>> threadLocalCollections = OXThreadMarkers.getThreadLocalValue(KnownThreadLocalValue.CACHE_MESSAGE_COLLECTIONS.getIdentifier());
        if (threadLocalCollections == null) {
            return;
        }
        Queue<MessageCollectionId> collectionIds = threadLocalCollections.get(collectionId.getChannelKey());
        if (collectionIds == null || !collectionIds.remove(collectionId) || !collectionIds.isEmpty()) {
            return;
        }
        threadLocalCollections.remove(collectionId.getChannelKey());
        if (threadLocalCollections.isEmpty()) {
            OXThreadMarkers.removeThreadLocalValue(KnownThreadLocalValue.CACHE_MESSAGE_COLLECTIONS.getIdentifier());
        }
    }

    /**
     * Publishes the specified messages
     *
     * @param messages The messages to publish
     * @param considerThreadLocal Whether to publish to a local queue for the local thread
     * @throws OXException if an error is occurred and publishing fails
     */
    private void doPublish(Collection<String> messages, boolean considerThreadLocal) throws OXException {
        if (messages == null || messages.isEmpty()) {
            return;
        }

        if (considerThreadLocal && !messageCollections.isEmpty()) {
            Optional<Pair<MessageCollectionId, ConcurrentMap<String, Object>>> optMessageCollectionForThread = optMessageCollectionForThread();
            if (optMessageCollectionForThread.isPresent()) {
                ConcurrentMap<String, Object> messageQueue = optMessageCollectionForThread.get().getSecond();
                for (String message : messages) {
                    if (message != null) {
                        messageQueue.put(message, PRESENT);
                    }
                }
                return;
            }
        }

        List<String> expiredMessages;
        Lock lock = publishQueue.getLock();
        lock.lock();
        try {
            boolean initTimerTask = true;
            for (String message : messages) {
                if (publishQueue.offerIfAbsent(message) && initTimerTask) {
                    // ENsure timer task is running
                    initTimerTask();
                    initTimerTask = false;
                }
            }
            expiredMessages = drainExpiredMessages();
        } finally {
            lock.unlock();
        }

        publishNow(expiredMessages);
    }

    private void publishNow(List<String> messages) throws OXException {
        if (messages == NO_EXPIRED_MESSAGES || messages.isEmpty()) {
            return;
        }

        StatefulRedisPubSubConnection<String, JSONObject> connection = mConnectionProvider.newPubSubConnection();
        try {
            RedisPubSubAsyncCommands<String, JSONObject> async = connection.async();
            for (String message : messages) {
                if (message != null) {
                    try {
                        JSONObject jMessage = new JSONObject(2);
                        jMessage.putOpt("data", message);
                        jMessage.put("instanceId", UUIDs.getUnformattedString(instanceId));
                        async.publish(channelKey.getFQN(), jMessage);
                    } catch (Exception e) {
                        LOG.warn("Failed to publish text message", e);
                    }
                }
            }
        } finally {
            connection.close();
        }
    }

    private void initTimerTask() throws OXException { // Only called when holding lock
        if (timerTaskReference.get() != null) {
            return;
        }
        TimerService timerService = services.getServiceSafe(TimerService.class);
        org.slf4j.Logger log = LOG;
        Runnable r = () -> {
            try {
                triggerPublish();
            } catch (Exception e) {
                log.warn("Failed to trigger publishing messages.", e);
            }
        };
        int delay = DELAY_FREQUENCY;
        timerTaskReference.set(timerService.scheduleWithFixedDelay(r, delay, delay));
    }

    /**
     * Cancels the timer.
     */
    private void cancelTimerTask() {
        ScheduledTimerTask timerTask = timerTaskReference.getAndSet(null);
        if (timerTask == null) {
            return;
        }
        timerTask.cancel();
        TimerService timerService = services.getOptionalService(TimerService.class);
        if (timerService != null) {
            timerService.purge();
        }
    }

    /**
     * Triggers all due messages.
     *
     * @throws OXException If publishing due messages fails
     */
    void triggerPublish() throws OXException {
        List<String> dueMessages;
        Lock lock = publishQueue.getLock();
        lock.lock();
        try {
            dueMessages = drainExpiredMessages();
            if (dueMessages == NO_EXPIRED_MESSAGES && publishQueue.isEmpty()) {
                // No messages with an expired delay and apparently queue is currently empty. Cancel timer task.
                cancelTimerTask();
            }
        } finally {
            lock.unlock();
        }

        publishNow(dueMessages);
    }

    /** Constant to signal no expired messages from queue */
    private static final List<String> NO_EXPIRED_MESSAGES = Collections.emptyList();

    /**
     * Drains expired messages from queue.
     *
     * @return Expired messages or {@link #NO_EXPIRED_MESSAGES} if there are no messages with an expired delay
     */
    private List<String> drainExpiredMessages() {
        String message = publishQueue.poll();
        if (message == null) {
            // No message with expired delay
            return NO_EXPIRED_MESSAGES;

        }

        // Check for further expired messages
        List<String> messages = new LinkedList<>();
        messages.add(message);
        publishQueue.drainTo(messages);
        return messages;
    }

    private Optional<Pair<MessageCollectionId, ConcurrentMap<String, Object>>> optMessageCollectionForThread() {
        Map<ChannelKey, Queue<MessageCollectionId>> threadLocalCollections = OXThreadMarkers.getThreadLocalValue(KnownThreadLocalValue.CACHE_MESSAGE_COLLECTIONS.getIdentifier());
        if (threadLocalCollections == null) {
            return Optional.empty();
        }
        Queue<MessageCollectionId> collectionIds = threadLocalCollections.get(channelKey);
        if (collectionIds == null) {
            return Optional.empty();
        }
        MessageCollectionId messageCollectionId = collectionIds.peek();
        if (messageCollectionId == null) {
            return Optional.empty();
        }
        ConcurrentMap<String, Object> messageQueue = messageCollections.get(messageCollectionId);
        if (messageQueue != null) {
            return Optional.of(new Pair<>(messageCollectionId, messageQueue));
        }

        // Hm... No such message collection
        collectionIds.remove(messageCollectionId);
        return Optional.empty();
    }

    @Override
    public void subscribe(ChannelListener<String> listener) throws OXException {
        nullCheck(listener);

        RedisPubSubListenerImpl redisListener = new RedisPubSubListenerImpl(listener, instanceId);
        if (listeners.putIfAbsent(listener, redisListener) != null) {
            // Already added
            return;
        }

        StatefulRedisPubSubConnection<String, JSONObject> connection = getSubscribeConnection();
        connection.addListener(redisListener);
    }

    @Override
    public void unsubscribe(ChannelListener<String> listener) throws OXException {
        if (listener == null) {
            return;
        }

        RedisPubSubListenerImpl redisListener = listeners.remove(listener);
        if (redisListener == null) {
            // No such listener
            return;
        }

        StatefulRedisPubSubConnection<String, JSONObject> connection = optSubscribeConnection();
        if (connection != null) {
            connection.removeListener(redisListener);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Performs a null check against the specified message
     *
     * @param message The message
     * @throws IllegalArgumentException If the message is <code>null</code>
     */
    protected static <M> void nullCheck(M message) {
        if (message == null) {
            throw new IllegalArgumentException("Message must not be null");
        }
    }

    /**
     * Performs a null check against the specified listener
     *
     * @param listener The listener
     * @throws IllegalArgumentException If the listener is <code>null</code>
     */
    protected static <M> void nullCheck(ChannelListener<M> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener must not be null");
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Wrapper for <code>io.lettuce.core.pubsub.RedisPubSubListener</code>.
     */
    protected static class RedisPubSubListenerImpl implements RedisPubSubListener<String, JSONObject> {

        private final ChannelListener<String> listener;
        private final UUID localInstanceId;

        /**
         * Initializes a new {@link RedisPubSubListenerImpl}.
         *
         * @param listener The listener to delegate to
         * @param localInstanceId The local instance identifier
         */
        public RedisPubSubListenerImpl(ChannelListener<String> listener, UUID localInstanceId) {
            super();
            this.listener = listener;
            this.localInstanceId = localInstanceId;
        }

        @Override
        public void unsubscribed(String channel, long count) {
            // Nothing
        }

        @Override
        public void subscribed(String channel, long count) {
            // Nothing
        }

        @Override
        public void punsubscribed(String pattern, long count) {
            // Nothing
        }

        @Override
        public void psubscribed(String pattern, long count) {
            // Nothing
        }

        @Override
        public void message(String pattern, String channel, JSONObject jMessage) {
            // Nothing
        }

        @Override
        public void message(String channel, JSONObject jMessage) {
            try {
                UUID instanceId = UUIDs.fromUnformattedString(jMessage.getString("instanceId"));
                listener.onMessage(new StringMessage(jMessage.getString("data"), instanceId, !this.localInstanceId.equals(instanceId)));
            } catch (Exception e) {
                LOG.warn("Failed to parse text message.", e);
            }
        }
    }

    private static class StringMessage implements Message<String> {

        private final String data;
        private final UUID instanceId;
        private final boolean remote;

        StringMessage(String data, UUID instanceId, boolean remote) {
            super();
            this.data = data;
            this.instanceId = instanceId;
            this.remote = remote;
        }

        @Override
        public boolean isRemote() {
            return remote;
        }

        @Override
        public UUID getSenderId() {
            return instanceId;
        }

        @Override
        public String getData() {
            return data;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append('[');
            if (instanceId != null) {
                builder.append("instanceId=").append(UUIDs.getUnformattedString(instanceId)).append(", ");
            }
            builder.append("remote=").append(remote);
            if (data != null) {
                builder.append(", ").append("data=").append(data);
            }
            builder.append(']');
            return builder.toString();
        }
    }

    private static <V> V getFromFuture(Future<V> f) throws OXException {
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw PubSubExceptionCode.UNEXPECTED_ERROR.create(e, "Interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OXException oxe) {
                throw oxe;
            }
            if (cause instanceof Error err) {
                throw err;
            }
            throw PubSubExceptionCode.UNEXPECTED_ERROR.create(cause == null ? e : cause, "Interrupted");
        }
    }

}
