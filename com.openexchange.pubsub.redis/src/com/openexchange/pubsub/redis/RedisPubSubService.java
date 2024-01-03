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

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.ExecutionError;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.openexchange.pubsub.Channel;
import com.openexchange.pubsub.ChannelApplicationName;
import com.openexchange.pubsub.ChannelKey;
import com.openexchange.pubsub.ChannelMessageCodec;
import com.openexchange.pubsub.ChannelName;
import com.openexchange.pubsub.PubSubService;
import com.openexchange.server.ServiceLookup;

/**
 * {@link RedisPubSubService} - The pub/sb service nacked by Redis.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class RedisPubSubService implements PubSubService {

    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisPubSubService.class);
    }

    private final RedisPubSubConnectionProvider pubSubConnectionProvider;
    private final com.google.common.cache.Cache<ChannelKey, RedisChannel<?>> channels;
    private final UUID instanceId;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link RedisPubSubService}.
     *
     * @param pubSubConnectionProvider The pub/sub connection provider
     * @param services The service look-up
     */
    public RedisPubSubService(RedisPubSubConnectionProvider pubSubConnectionProvider, ServiceLookup services) {
        super();
        this.services = services;
        this.pubSubConnectionProvider = pubSubConnectionProvider;
        instanceId = UUID.randomUUID();
        channels = CacheBuilder.newBuilder().build();
    }

    /**
     * Shuts down the service
     */
    public void shutdown() {
        for (RedisChannel<?> channel : channels.asMap().values()) {
            channel.shutdown();
        }
        channels.invalidateAll();
    }

    @Override
    public char getDelimiter() {
        return RedisChannelKey.DELIMITER;
    }

    @Override
    public UUID getInstanceId() {
        return instanceId;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Gets the loader for given arguments.
     *
     * @param <M> The type of the message
     * @param key The channel key
     * @param codec The channel message codec
     * @param instanceId The unique instance identifier in cluster
     * @return The loader
     */
    private <M> Callable<RedisChannel<M>> loaderFor(ChannelKey key, ChannelMessageCodec<M> codec, UUID instanceId) {
        return new RedisChannelLoader<M>(key, pubSubConnectionProvider, codec, instanceId, services);
    }

    private static class RedisChannelLoader<M> implements Callable<RedisChannel<M>> {

        private final ChannelKey key;
        private final RedisPubSubConnectionProvider pubSubConnectionProvider;
        private final ChannelMessageCodec<M> codec;
        private final UUID instanceId;
        private final ServiceLookup services;

        RedisChannelLoader(ChannelKey key, RedisPubSubConnectionProvider pubSubConnectionProvider, ChannelMessageCodec<M> codec, UUID instanceId, ServiceLookup services) {
            super();
            this.key = key;
            this.pubSubConnectionProvider = pubSubConnectionProvider;
            this.codec = codec;
            this.instanceId = instanceId;
            this.services = services;
        }

        @Override
        public RedisChannel<M> call() throws Exception {
            return new RedisChannel<M>(codec, new NativeRedisChannel(key, pubSubConnectionProvider, instanceId, services));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M> Channel<M> getChannel(ChannelKey key, ChannelMessageCodec<M> codec) {
        RedisChannel<?> channel = channels.getIfPresent(key);
        if (channel != null) {
            return (Channel<M>) channel;
        }

        try {
            return (Channel<M>) channels.get(key, loaderFor(key, codec, instanceId));
        } catch (ExecutionException e) {
            logFailedChanellCreationFor(key, e.getCause());
            throw new IllegalStateException(e.getCause());
        } catch (UncheckedExecutionException e) {
            RuntimeException rte = (RuntimeException) e.getCause();
            logFailedChanellCreationFor(key, rte);
            throw rte;
        } catch (Exception e) {
            logFailedChanellCreationFor(key, e);
            throw new IllegalStateException(e);
        } catch (ExecutionError e) {
            throw (Error) e.getCause();
        }
    }

    private static void logFailedChanellCreationFor(ChannelKey key, Throwable error) {
        LoggerHolder.LOG.error("Failed to create channel for {}", key, error);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <M> Optional<Channel<M>> optChannel(ChannelKey key) {
        RedisChannel<?> channel = channels.getIfPresent(key);
        return channel == null ? Optional.empty() : Optional.of((Channel<M>) channel);
    }

    @Override
    public ChannelKey newKey(ChannelApplicationName application, ChannelName channel) {
        return RedisChannelKey.builder().withApplicationName(application).withChannelName(channel).build();
    }

}
