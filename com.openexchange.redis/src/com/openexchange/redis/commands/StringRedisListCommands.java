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

package com.openexchange.redis.commands;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import io.lettuce.core.KeyValue;
import io.lettuce.core.LMPopArgs;
import io.lettuce.core.LMoveArgs;
import io.lettuce.core.LPosArgs;
import io.lettuce.core.api.sync.RedisListCommands;
import io.lettuce.core.output.ValueStreamingChannel;

/**
 * {@link StringRedisListCommands} - The Redis list commands for string values.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class StringRedisListCommands implements RedisListCommands<String, String> {

    private static final int DEFAULT_CAPACITY = 32;

    private final RedisListCommands<String, InputStream> commands;

    /**
     * Initializes a new {@link StringRedisListCommands}.
     */
    public StringRedisListCommands(RedisListCommands<String, InputStream> commands) {
        super();
        this.commands = commands;
    }

    @Override
    public String blmove(String source, String destination, LMoveArgs args, long timeout) {
        return stringFrom(commands.blmove(source, destination, args, timeout));
    }

    @Override
    public String blmove(String source, String destination, LMoveArgs args, double timeout) {
        return stringFrom(commands.blmove(source, destination, args, timeout));
    }

    @Override
    public KeyValue<String, List<String>> blmpop(long timeout, LMPopArgs args, String... keys) {
        KeyValue<String, List<InputStream>> result = commands.blmpop(timeout, args, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), streams2strings(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, List<String>> blmpop(double timeout, LMPopArgs args, String... keys) {
        KeyValue<String, List<InputStream>> result = commands.blmpop(timeout, args, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), streams2strings(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, String> blpop(long timeout, String... keys) {
        KeyValue<String, InputStream> result = commands.blpop(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), stringFrom(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, String> blpop(double timeout, String... keys) {
        KeyValue<String, InputStream> result = commands.blpop(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), stringFrom(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, String> brpop(long timeout, String... keys) {
        KeyValue<String, InputStream> result = commands.brpop(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), stringFrom(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, String> brpop(double timeout, String... keys) {
        KeyValue<String, InputStream> result = commands.brpop(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), stringFrom(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public String brpoplpush(long timeout, String source, String destination) {
        return stringFrom(commands.brpoplpush(timeout, source, destination));
    }

    @Override
    public String brpoplpush(double timeout, String source, String destination) {
        return stringFrom(commands.brpoplpush(timeout, source, destination));
    }

    @Override
    public String lindex(String key, long index) {
        return stringFrom(commands.lindex(key, index));
    }

    @Override
    public Long linsert(String key, boolean before, String pivot, String value) {
        return commands.linsert(key, before, streamFor(pivot), streamFor(value));
    }

    @Override
    public Long llen(String key) {
        return commands.llen(key);
    }

    @Override
    public String lmove(String source, String destination, LMoveArgs args) {
        return stringFrom(commands.lmove(source, destination, args));
    }

    @Override
    public KeyValue<String, List<String>> lmpop(LMPopArgs args, String... keys) {
        KeyValue<String, List<InputStream>> result = commands.lmpop(args, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), streams2strings(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public String lpop(String key) {
        return stringFrom(commands.lpop(key));
    }

    @Override
    public List<String> lpop(String key, long count) {
        return streams2strings(commands.lpop(key, count));
    }

    @Override
    public Long lpos(String key, String value) {
        return commands.lpos(key, streamFor(value));
    }

    @Override
    public Long lpos(String key, String value, LPosArgs args) {
        return commands.lpos(key, streamFor(value), args);
    }

    @Override
    public List<Long> lpos(String key, String value, int count) {
        return commands.lpos(key, streamFor(value), count);
    }

    @Override
    public List<Long> lpos(String key, String value, int count, LPosArgs args) {
        return commands.lpos(key, streamFor(value), count, args);
    }

    @Override
    public Long lpush(String key, String... values) {
        return commands.lpush(key, stringArray2streamArray(values));
    }

    @Override
    public Long lpushx(String key, String... values) {
        return commands.lpushx(key, stringArray2streamArray(values));
    }

    @Override
    public List<String> lrange(String key, long start, long stop) {
        return streams2strings(commands.lrange(key, start, stop));
    }

    @Override
    public Long lrange(ValueStreamingChannel<String> channel, String key, long start, long stop) {
        return commands.lrange(new WrappingValueStreamingChannel(channel), key, start, stop);
    }

    @Override
    public Long lrem(String key, long count, String value) {
        return commands.lrem(key, count, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public String lset(String key, long index, String value) {
        return commands.lset(key, index, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public String ltrim(String key, long start, long stop) {
        return commands.ltrim(key, start, stop);
    }

    @Override
    public String rpop(String key) {
        return stringFrom(commands.rpop(key));
    }

    @Override
    public List<String> rpop(String key, long count) {
        return streams2strings(commands.rpop(key, count));
    }

    @Override
    public String rpoplpush(String source, String destination) {
        return stringFrom(commands.rpoplpush(source, destination));
    }

    @Override
    public Long rpush(String key, String... values) {
        return commands.rpush(key, stringArray2streamArray(values));
    }

    @Override
    public Long rpushx(String key, String... values) {
        return commands.rpushx(key, stringArray2streamArray(values));
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private static String stringFrom(InputStream stream) {
        return RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
    }

    private static InputStream streamFor(String element) {
        return RedisCommandUtils.string2Stream(element);
    }

    private static List<String> streams2strings(List<InputStream> streams) {
        if (streams == null) {
            return null;
        }
        List<String> strings = new ArrayList<>(streams.size());
        for (InputStream stream : streams) {
            strings.add(stringFrom(stream));
        }
        return strings;
    }

    private static InputStream[] stringArray2streamArray(String[] strings) {
        if (strings == null) {
            return null;
        }
        InputStream[] streams = new InputStream[strings.length];
        for (int i = strings.length; i-- > 0;) {
            streams[i] = streamFor(strings[i]);
        }
        return streams;
    }

    private static final class WrappingValueStreamingChannel implements ValueStreamingChannel<InputStream> {

        private final ValueStreamingChannel<String> channel;

        /**
         * Initializes a new {@link WrappingValueStreamingChannel}.
         *
         * @param channel The channel to wrap
         */
        private WrappingValueStreamingChannel(ValueStreamingChannel<String> channel) {
            this.channel = channel;
        }

        @Override
        public void onValue(InputStream value) {
            channel.onValue(stringFrom(value));
        }
    }
}
