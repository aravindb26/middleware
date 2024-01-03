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
import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.KeyValue;
import io.lettuce.core.SetArgs;
import io.lettuce.core.StrAlgoArgs;
import io.lettuce.core.StringMatchResult;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.output.KeyValueStreamingChannel;


/**
 * {@link StringRedisStringCommands} - The Redis string commands for string values.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class StringRedisStringCommands implements RedisStringCommands<String, String> {

    private static final int DEFAULT_CAPACITY = 32;

    private final RedisStringCommands<String, InputStream> commands;

    /**
     * Initializes a new {@link StringRedisStringCommands}.
     *
     * @param commands The commands to delegate to
     */
    public StringRedisStringCommands(RedisStringCommands<String, InputStream> commands) {
        super();
        this.commands = commands;
    }

    @Override
    public Long append(String key, String value) {
        return commands.append(key, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public Long bitcount(String key) {
        return commands.bitcount(key);
    }

    @Override
    public Long bitcount(String key, long start, long end) {
        return commands.bitcount(key, start, end);
    }

    @Override
    public List<Long> bitfield(String key, BitFieldArgs bitFieldArgs) {
        return commands.bitfield(key, bitFieldArgs);
    }

    @Override
    public Long bitpos(String key, boolean state) {
        return commands.bitpos(key, state);
    }

    @Override
    public Long bitpos(String key, boolean state, long start) {
        return commands.bitpos(key, state, start);
    }

    @Override
    public Long bitpos(String key, boolean state, long start, long end) {
        return commands.bitpos(key, state, start, end);
    }

    @Override
    public Long bitopAnd(String destination, String... keys) {
        return commands.bitopAnd(destination, keys);
    }

    @Override
    public Long bitopNot(String destination, String source) {
        return commands.bitopNot(destination, source);
    }

    @Override
    public Long bitopOr(String destination, String... keys) {
        return commands.bitopOr(destination, keys);
    }

    @Override
    public Long bitopXor(String destination, String... keys) {
        return commands.bitopXor(destination, keys);
    }

    @Override
    public Long decr(String key) {
        return commands.decr(key);
    }

    @Override
    public Long decrby(String key, long amount) {
        return commands.decrby(key, amount);
    }

    @Override
    public String get(String key) {
        return RedisCommandUtils.stream2String(commands.get(key), DEFAULT_CAPACITY);
    }

    @Override
    public Long getbit(String key, long offset) {
        return commands.getbit(key, offset);
    }

    @Override
    public String getdel(String key) {
        return RedisCommandUtils.stream2String(commands.getdel(key), DEFAULT_CAPACITY);
    }

    @Override
    public String getex(String key, GetExArgs args) {
        return RedisCommandUtils.stream2String(commands.getex(key, args), DEFAULT_CAPACITY);
    }

    @Override
    public String getrange(String key, long start, long end) {
        return RedisCommandUtils.stream2String(commands.getrange(key, start, end), DEFAULT_CAPACITY);
    }

    @Override
    public String getset(String key, String value) {
        return RedisCommandUtils.stream2String(commands.getset(key, RedisCommandUtils.string2Stream(value)), DEFAULT_CAPACITY);
    }

    @Override
    public Long incr(String key) {
        return commands.incr(key);
    }

    @Override
    public Long incrby(String key, long amount) {
        return commands.incrby(key, amount);
    }

    @Override
    public Double incrbyfloat(String key, double amount) {
        return commands.incrbyfloat(key, amount);
    }

    @Override
    public List<KeyValue<String, String>> mget(String... keys) {
        List<KeyValue<String, InputStream>> mget = commands.mget(keys);
        List<KeyValue<String, String>> retval = new ArrayList<>(mget.size());
        for (KeyValue<String,InputStream> keyValue : mget) {
            if (keyValue.hasValue()) {
                retval.add(KeyValue.just(keyValue.getKey(), RedisCommandUtils.stream2String(keyValue.getValue(), DEFAULT_CAPACITY)));
            } else {
                retval.add(KeyValue.empty(keyValue.getKey()));
            }
        }
        return retval;
    }

    @Override
    public Long mget(KeyValueStreamingChannel<String, String> channel, String... keys) {
        throw new UnsupportedOperationException("StringRedisStringCommands.mget()");
    }

    @Override
    public String mset(Map<String, String> map) {
        Map<String, InputStream> m = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            m.put(e.getKey(), RedisCommandUtils.string2Stream(e.getValue()));
        }
        return commands.mset(m);
    }

    @Override
    public Boolean msetnx(Map<String, String> map) {
        Map<String, InputStream> m = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            m.put(e.getKey(), RedisCommandUtils.string2Stream(e.getValue()));
        }
        return commands.msetnx(m);
    }

    @Override
    public String set(String key, String value) {
        return commands.set(key, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public String set(String key, String value, SetArgs setArgs) {
        return commands.set(key, RedisCommandUtils.string2Stream(value), setArgs);
    }

    @Override
    public String setGet(String key, String value) {
        return RedisCommandUtils.stream2String(commands.setGet(key, RedisCommandUtils.string2Stream(value)), DEFAULT_CAPACITY);
    }

    @Override
    public String setGet(String key, String value, SetArgs setArgs) {
        return RedisCommandUtils.stream2String(commands.setGet(key, RedisCommandUtils.string2Stream(value), setArgs), DEFAULT_CAPACITY);
    }

    @Override
    public Long setbit(String key, long offset, int value) {
        return commands.setbit(key, offset, value);
    }

    @Override
    public String setex(String key, long seconds, String value) {
        return commands.setex(key, seconds, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public String psetex(String key, long milliseconds, String value) {
        return commands.psetex(key, milliseconds, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public Boolean setnx(String key, String value) {
        return commands.setnx(key, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public Long setrange(String key, long offset, String value) {
        return commands.setrange(key, offset, RedisCommandUtils.string2Stream(value));
    }

    @Override
    public StringMatchResult stralgoLcs(StrAlgoArgs strAlgoArgs) {
        return commands.stralgoLcs(strAlgoArgs);
    }

    @Override
    public Long strlen(String key) {
        return commands.strlen(key);
    }

}
