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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import com.openexchange.exception.OXException;
import com.openexchange.java.util.UUIDs;
import com.openexchange.pubsub.Channel;
import com.openexchange.pubsub.ChannelListener;
import com.openexchange.pubsub.ChannelMessageCodec;
import com.openexchange.pubsub.Message;
import com.openexchange.pubsub.MessageCollectionId;
import com.openexchange.pubsub.PubSubExceptionCode;


/**
 * {@link RedisChannel}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisChannel<M> implements Channel<M> {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisChannel.class);
    }

    private final ChannelMessageCodec<M> codec;
    private final NativeRedisChannel nativeChannel;
    private final ConcurrentMap<ChannelListener<M>, ChannelListener<String>> listeners;

    /**
     * Initializes a new {@link RedisChannel}.
     *
     * @param codec The codec
     * @param nativeChannel The native channel
     */
    public RedisChannel(ChannelMessageCodec<M> codec, NativeRedisChannel nativeChannel) {
        super();
        this.codec = codec;
        this.nativeChannel = nativeChannel;
        listeners = new ConcurrentHashMap<>();
    }

    @Override
    public UUID getInstanceId() {
        return nativeChannel.getInstanceId();
    }

    /**
     * Shuts down the channel.
     */
    public void shutdown() {
        nativeChannel.shutdown();
    }

    @Override
    public MessageCollectionId startMessageCollection(Optional<M> optionalInitialMessage) throws OXException {
        try {
            Optional<String> optInitialMessage = optionalInitialMessage.isPresent() ? Optional.of(codec.serialize(optionalInitialMessage.get())) : Optional.empty();
            return nativeChannel.startMessageCollection(optInitialMessage);
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw PubSubExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void addToMessageCollection(M message, MessageCollectionId collectionId) throws OXException {
        if (message == null) {
            return;
        }

        try {
            nativeChannel.addToMessageCollection(codec.serialize(message), collectionId);
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw PubSubExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void releaseMessageCollection(MessageCollectionId collectionId) throws OXException {
        nativeChannel.releaseMessageCollection(collectionId);
    }

    @Override
    public void dropMessageCollection(MessageCollectionId collectionId) throws OXException {
        nativeChannel.dropMessageCollection(collectionId);
    }

    @Override
    public void publish(M message) throws OXException {
        if (message == null) {
            return;
        }

        try {
            String serializedMessage = codec.serialize(message);
            nativeChannel.publish(serializedMessage);

            //System.out.println("\tPublished message to channel \"" + nativeChannel.getChannelKey() + "\": " + serializedMessage);
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw PubSubExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void publish(Collection<M> messages) throws OXException {
        if (messages == null) {
            return;
        }

        int size = messages.size();
        if (size <= 0) {
            return;
        }

        try {
            if (size == 1) {
                String serializedMessage = codec.serialize(messages.iterator().next());
                nativeChannel.publish(Collections.singletonList(serializedMessage));
                return;
            }

            List<String> serializedMessages = new ArrayList<String>(size);
            for (M message : messages) {
                serializedMessages.add(codec.serialize(message));
            }
            nativeChannel.publish(serializedMessages);

            //System.out.println("\tPublished messages to channel \"" + nativeChannel.getChannelKey() + "\": " + serializedMessages);
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw PubSubExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void subscribe(ChannelListener<M> listener) throws OXException {
        try {
            ChannelListener<String> listenerImpl = new ChannelListenerImpl<M>(listener, codec);
            if (listeners.putIfAbsent(listener, listenerImpl) != null) {
                // Already added
                return;
            }
            nativeChannel.subscribe(listenerImpl);
        } catch (Exception e) {
            throw PubSubExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void unsubscribe(ChannelListener<M> listener) throws OXException {
        if (listener == null) {
            return;
        }

        ChannelListener<String> listenerImpl = listeners.remove(listener);
        if (listenerImpl == null) {
            // No such listener
            return;
        }

        nativeChannel.unsubscribe(listenerImpl);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class ChannelListenerImpl<D> implements ChannelListener<String> {

        private static final AtomicReference<DeserializedMessage> LAST_DESERIALIZED_MESSAGE = new AtomicReference<>();

        private final ChannelListener<D> listener;
        private final ChannelMessageCodec<D> codec;

        ChannelListenerImpl(ChannelListener<D> listener, ChannelMessageCodec<D> codec) {
            super();
            this.listener = listener;
            this.codec = codec;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void onMessage(Message<String> message) {
            try {
                int messageInstanceId = System.identityHashCode(message);

                DataMessage<D> dataMessage;
                {
                    DeserializedMessage deserialized = LAST_DESERIALIZED_MESSAGE.get();
                    if (deserialized != null && deserialized.instanceId == messageInstanceId) {
                        // Use "cached" data
                        dataMessage = (DataMessage<D>) deserialized.dataMessage;
                    } else {
                        // Deserialize and "cache"
                        dataMessage = new DataMessage<>(codec.deserialize(message.getData()), message.getSenderId(), message.isRemote());
                        LAST_DESERIALIZED_MESSAGE.set(new DeserializedMessage(messageInstanceId, dataMessage));
                    }
                }

                listener.onMessage(dataMessage);
            } catch (Exception e) {
                LoggerHolder.LOG.error("Failed to deserialize & deliver message: {}", message, e);
            }
        }
    }

    private static class DeserializedMessage {

        final int instanceId;
        final DataMessage<?> dataMessage;

        DeserializedMessage(int instanceId, DataMessage<?> dataMessage) {
            super();
            this.instanceId = instanceId;
            this.dataMessage = dataMessage;
        }
    }

    private static class DataMessage<D> implements Message<D> {

        private final D data;
        private final UUID instanceId;
        private final boolean remote;

        DataMessage(D data, UUID instanceId, boolean remote) {
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
        public D getData() {
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

}
