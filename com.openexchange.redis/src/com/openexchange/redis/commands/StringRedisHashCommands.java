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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.lettuce.core.KeyValue;
import io.lettuce.core.MapScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.api.sync.RedisHashCommands;
import io.lettuce.core.output.KeyStreamingChannel;
import io.lettuce.core.output.KeyValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;


/**
 * {@link StringRedisHashCommands} - The Redis hash commands for string values.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class StringRedisHashCommands implements RedisHashCommands<String, String> {

    private static final int DEFAULT_CAPACITY = 32;

    private final RedisHashCommands<String, InputStream> commands;

    /**
     * Initializes a new {@link StringRedisHashCommands}.
     */
    public StringRedisHashCommands(RedisHashCommands<String, InputStream> commands) {
        super();
        this.commands = commands;
    }

    @Override
    public Long hdel(String key, String... fields) {
        return commands.hdel(key, fields);
    }

    @Override
    public Boolean hexists(String key, String field) {
        return commands.hexists(key, field);
    }

    @Override
    public String hget(String key, String field) {
        InputStream stream = commands.hget(key, field);
        if (stream == null) {
            return null;
        }
        return RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
    }

    @Override
    public Long hincrby(String key, String field, long amount) {
        return commands.hincrby(key, field, amount);
    }

    @Override
    public Double hincrbyfloat(String key, String field, double amount) {
        return commands.hincrbyfloat(key, field, amount);
    }

    @Override
    public Map<String, String> hgetall(String key) {
        Map<String, InputStream> map = commands.hgetall(key);
        if (map == null) {
            return null;
        }

        Map<String, String> retval = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, InputStream> e : map.entrySet()) {
            retval.put(e.getKey(), RedisCommandUtils.stream2String(e.getValue(), DEFAULT_CAPACITY));
        }
        return retval;
    }

    @Override
    public Long hgetall(KeyValueStreamingChannel<String, String> channel, String key) {
        return commands.hgetall(new WrappingKeyValueStreamingChannel(channel), key);
    }

    @Override
    public List<String> hkeys(String key) {
        return commands.hkeys(key);
    }

    @Override
    public Long hkeys(KeyStreamingChannel<String> channel, String key) {
        return commands.hkeys(channel, key);
    }

    @Override
    public Long hlen(String key) {
        return commands.hlen(key);
    }

    @Override
    public List<KeyValue<String, String>> hmget(String key, String... fields) {
        List<KeyValue<String, InputStream>> list = commands.hmget(key, fields);
        if (list == null) {
            return null;
        }

        List<KeyValue<String, String>> retval = new ArrayList<>(list.size());
        for (KeyValue<String,InputStream> keyValue : list) {
            if (keyValue.hasValue()) {
                retval.add(KeyValue.fromNullable(keyValue.getKey(), RedisCommandUtils.stream2String(keyValue.getValue(), DEFAULT_CAPACITY)));
            } else {
                retval.add(KeyValue.empty(keyValue.getKey()));
            }
        }
        return retval;
    }

    @Override
    public Long hmget(KeyValueStreamingChannel<String, String> channel, String key, String... fields) {
        return commands.hmget(new WrappingKeyValueStreamingChannel(channel), key, fields);
    }

    @Override
    public String hmset(String key, Map<String, String> map) {
        Map<String, InputStream> val = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            val.put(e.getKey(), RedisCommandUtils.string2Stream(e.getValue()));
        }
        return commands.hmset(key, null);
    }

    @Override
    public String hrandfield(String key) {
        return commands.hrandfield(key);
    }

    @Override
    public List<String> hrandfield(String key, long count) {
        return commands.hrandfield(key, count);
    }

    @Override
    public KeyValue<String, String> hrandfieldWithvalues(String key) {
        KeyValue<String, InputStream> keyValue = commands.hrandfieldWithvalues(key);
        if (keyValue == null) {
            return null;
        }
        if (keyValue.hasValue()) {
            return KeyValue.fromNullable(keyValue.getKey(), RedisCommandUtils.stream2String(keyValue.getValue(), DEFAULT_CAPACITY));
        }
        return KeyValue.empty(keyValue.getKey());
    }

    @Override
    public List<KeyValue<String, String>> hrandfieldWithvalues(String key, long count) {
        List<KeyValue<String, InputStream>> keyValues = commands.hrandfieldWithvalues(key, count);
        if (keyValues == null) {
            return null;
        }

        List<KeyValue<String, String>> retval = new ArrayList<>(keyValues.size());
        for (KeyValue<String,InputStream> keyValue : keyValues) {
            if (keyValue.hasValue()) {
                retval.add(KeyValue.fromNullable(keyValue.getKey(), RedisCommandUtils.stream2String(keyValue.getValue(), DEFAULT_CAPACITY)));
            } else {
                retval.add(KeyValue.empty(keyValue.getKey()));
            }
        }
        return retval;
    }

    @Override
    public MapScanCursor<String, String> hscan(String key) {
        MapScanCursor<String, InputStream> cursor = commands.hscan(key);

        Map<String, InputStream> map = cursor.getMap();
        MapScanCursor<String, String> newCursor = new MapScanCursor<>();
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        for (Map.Entry<String, InputStream> e : map.entrySet()) {
            newCursor.getMap().put(e.getKey(), RedisCommandUtils.stream2String(e.getValue(), DEFAULT_CAPACITY));
        }
        return newCursor;
    }

    @Override
    public MapScanCursor<String, String> hscan(String key, ScanArgs scanArgs) {
        MapScanCursor<String, InputStream> cursor = commands.hscan(key, scanArgs);

        Map<String, InputStream> map = cursor.getMap();
        MapScanCursor<String, String> newCursor = new MapScanCursor<>();
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        for (Map.Entry<String, InputStream> e : map.entrySet()) {
            newCursor.getMap().put(e.getKey(), RedisCommandUtils.stream2String(e.getValue(), DEFAULT_CAPACITY));
        }
        return newCursor;
    }

    @Override
    public MapScanCursor<String, String> hscan(String key, ScanCursor scanCursor, ScanArgs scanArgs) {
        MapScanCursor<String, InputStream> cursor = commands.hscan(key, scanCursor, scanArgs);

        Map<String, InputStream> map = cursor.getMap();
        MapScanCursor<String, String> newCursor = new MapScanCursor<>();
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        for (Map.Entry<String, InputStream> e : map.entrySet()) {
            newCursor.getMap().put(e.getKey(), RedisCommandUtils.stream2String(e.getValue(), DEFAULT_CAPACITY));
        }
        return newCursor;
    }

    @Override
    public MapScanCursor<String, String> hscan(String key, ScanCursor scanCursor) {
        MapScanCursor<String, InputStream> cursor = commands.hscan(key, scanCursor);

        Map<String, InputStream> map = cursor.getMap();
        MapScanCursor<String, String> newCursor = new MapScanCursor<>();
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        for (Map.Entry<String, InputStream> e : map.entrySet()) {
            newCursor.getMap().put(e.getKey(), RedisCommandUtils.stream2String(e.getValue(), DEFAULT_CAPACITY));
        }
        return newCursor;
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<String, String> channel, String key) {
        return commands.hscan(new WrappingKeyValueStreamingChannel(channel), key);
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<String, String> channel, String key, ScanArgs scanArgs) {
        return commands.hscan(new WrappingKeyValueStreamingChannel(channel), key, scanArgs);
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<String, String> channel, String key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return commands.hscan(new WrappingKeyValueStreamingChannel(channel), key, scanCursor, scanArgs);
    }

    @Override
    public StreamScanCursor hscan(KeyValueStreamingChannel<String, String> channel, String key, ScanCursor scanCursor) {
        return commands.hscan(new WrappingKeyValueStreamingChannel(channel), key, scanCursor);
    }

    @Override
    public Boolean hset(String key, String field, String value) {
        return commands.hset(key, field, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public Long hset(String key, Map<String, String> map) {
        Map<String, InputStream> val = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            val.put(e.getKey(), RedisCommandUtils.string2Stream(e.getValue()));
        }
        return commands.hset(key, val);
    }

    @Override
    public Boolean hsetnx(String key, String field, String value) {
        return commands.hsetnx(key, field, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public Long hstrlen(String key, String field) {
        return commands.hstrlen(key, field);
    }

    @Override
    public List<String> hvals(String key) {
        List<InputStream> list = commands.hvals(key);
        if (list == null) {
            return null;
        }

        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY));
        }
        return retval;
    }

    @Override
    public Long hvals(ValueStreamingChannel<String> channel, String key) {
        return commands.hvals(new WrappingValueStreamingChannel(channel), key);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final class WrappingKeyValueStreamingChannel implements KeyValueStreamingChannel<String, InputStream> {

        private final KeyValueStreamingChannel<String, String> channel;

        /**
         * Initializes a new {@link WrappingKeyValueStreamingChannel}.
         *
         * @param channel The channel to wrap
         */
        private WrappingKeyValueStreamingChannel(KeyValueStreamingChannel<String, String> channel) {
            this.channel = channel;
        }

        @Override
        public void onKeyValue(String key, InputStream value) {
            channel.onKeyValue(key, RedisCommandUtils.stream2String(value, DEFAULT_CAPACITY));
        }
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
            channel.onValue(RedisCommandUtils.stream2String(value, DEFAULT_CAPACITY));
        }
    }

}
