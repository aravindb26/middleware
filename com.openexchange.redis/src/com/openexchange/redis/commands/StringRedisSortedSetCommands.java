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
import io.lettuce.core.Limit;
import io.lettuce.core.Range;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.ScoredValue;
import io.lettuce.core.ScoredValueScanCursor;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.ZAddArgs;
import io.lettuce.core.ZAggregateArgs;
import io.lettuce.core.ZStoreArgs;
import io.lettuce.core.api.sync.RedisSortedSetCommands;
import io.lettuce.core.output.ScoredValueStreamingChannel;
import io.lettuce.core.output.ValueStreamingChannel;


/**
 * {@link StringRedisSortedSetCommands} - The Redis sorted-set commands for string values.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class StringRedisSortedSetCommands implements RedisSortedSetCommands<String, String> {

    private static final int DEFAULT_CAPACITY = 32;

    private final RedisSortedSetCommands<String, InputStream> commands;

    /**
     * Initializes a new {@link StringRedisSortedSetCommands}.
     */
    public StringRedisSortedSetCommands(RedisSortedSetCommands<String, InputStream> commands) {
        super();
        this.commands = commands;
    }

    @Override
    public KeyValue<String, ScoredValue<String>> bzpopmin(long timeout, String... keys) {
        KeyValue<String, ScoredValue<InputStream>> result = commands.bzpopmin(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), scoredValueForStream(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, ScoredValue<String>> bzpopmin(double timeout, String... keys) {
        KeyValue<String, ScoredValue<InputStream>> result = commands.bzpopmin(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), scoredValueForStream(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, ScoredValue<String>> bzpopmax(long timeout, String... keys) {
        KeyValue<String, ScoredValue<InputStream>> result = commands.bzpopmax(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), scoredValueForStream(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public KeyValue<String, ScoredValue<String>> bzpopmax(double timeout, String... keys) {
        KeyValue<String, ScoredValue<InputStream>> result = commands.bzpopmax(timeout, keys);
        if (result == null) {
            return null;
        }
        if (result.hasValue()) {
            return KeyValue.fromNullable(result.getKey(), scoredValueForStream(result.getValue()));
        }
        return KeyValue.empty(result.getKey());
    }

    @Override
    public Long zadd(String key, double score, String member) {
        return commands.zadd(key, score, streamFor(member));
    }

    @Override
    public Long zadd(String key, Object... scoresAndValues) {
        Object[] args = new Object[scoresAndValues.length];
        for (int i = 0; i < scoresAndValues.length; i++) {
            Object scoreOrValue = scoresAndValues[i];
            if (scoreOrValue instanceof String) {
                args[i] = streamFor(scoreOrValue.toString());
            } else {
                args[i] = scoreOrValue;
            }
        }
        return commands.zadd(key, args);
    }

    @Override
    public Long zadd(String key, ScoredValue<String>... scoredValues) {
        ScoredValue<InputStream>[] newValues = new ScoredValue[scoredValues.length];
        for (int i = 0; i < scoredValues.length; i++) {
            ScoredValue<String> v = scoredValues[i];
            newValues[i] = scoredValueForString(v);
        }
        return commands.zadd(key, newValues);
    }

    @Override
    public Long zadd(String key, ZAddArgs zAddArgs, double score, String member) {
        return commands.zadd(key, zAddArgs, score, streamFor(member));
    }

    @Override
    public Long zadd(String key, ZAddArgs zAddArgs, Object... scoresAndValues) {
        Object[] args = new Object[scoresAndValues.length];
        for (int i = 0; i < scoresAndValues.length; i++) {
            Object scoreOrValue = scoresAndValues[i];
            if (scoreOrValue instanceof String) {
                args[i] = streamFor(scoreOrValue.toString());
            } else {
                args[i] = scoreOrValue;
            }
        }
        return commands.zadd(key, zAddArgs, args);
    }

    @Override
    public Long zadd(String key, ZAddArgs zAddArgs, ScoredValue<String>... scoredValues) {
        ScoredValue<InputStream>[] newValues = new ScoredValue[scoredValues.length];
        for (int i = 0; i < scoredValues.length; i++) {
            ScoredValue<String> v = scoredValues[i];
            newValues[i] = scoredValueForString(v);
        }
        return commands.zadd(key, zAddArgs, newValues);
    }

    @Override
    public Double zaddincr(String key, double score, String member) {
        return commands.zaddincr(key, score, streamFor(member));
    }

    @Override
    public Double zaddincr(String key, ZAddArgs zAddArgs, double score, String member) {
        return commands.zaddincr(key, zAddArgs, score, streamFor(member));
    }

    @Override
    public Long zcard(String key) {
        return commands.zcard(key);
    }

    @Override
    public Long zcount(String key, double min, double max) {
        return commands.zcount(key, min, max);
    }

    @Override
    public Long zcount(String key, String min, String max) {
        return commands.zcount(key, min, max);
    }

    @Override
    public Long zcount(String key, Range<? extends Number> range) {
        return commands.zcount(key, range);
    }

    @Override
    public List<String> zdiff(String... keys) {
        List<InputStream> zdiff = commands.zdiff(keys);
        List<String> retval = new ArrayList<>(zdiff.size());
        for (InputStream stream : zdiff) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public Long zdiffstore(String destKey, String... srcKeys) {
        return commands.zdiffstore(destKey, srcKeys);
    }

    @Override
    public List<ScoredValue<String>> zdiffWithScores(String... keys) {
        List<ScoredValue<InputStream>> scores = commands.zdiffWithScores(keys);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public Double zincrby(String key, double amount, String member) {
        return commands.zincrby(key, amount, streamFor(member));
    }

    @Override
    public List<String> zinter(String... keys) {
        List<InputStream> zinter = commands.zinter(keys);
        List<String> retval = new ArrayList<>(zinter.size());
        for (InputStream stream : zinter) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zinter(ZAggregateArgs aggregateArgs, String... keys) {
        List<InputStream> zinter = commands.zinter(aggregateArgs, keys);
        List<String> retval = new ArrayList<>(zinter.size());
        for (InputStream stream : zinter) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public Long zintercard(String... keys) {
        return commands.zintercard(keys);
    }

    @Override
    public Long zintercard(long limit, String... keys) {
        return commands.zintercard(limit, keys);
    }

    @Override
    public List<ScoredValue<String>> zinterWithScores(ZAggregateArgs aggregateArgs, String... keys) {
        List<ScoredValue<InputStream>> scores = commands.zinterWithScores(aggregateArgs, keys);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zinterWithScores(String... keys) {
        List<ScoredValue<InputStream>> scores = commands.zinterWithScores(keys);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public Long zinterstore(String destination, String... keys) {
        return commands.zinterstore(destination, keys);
    }

    @Override
    public Long zinterstore(String destination, ZStoreArgs storeArgs, String... keys) {
        return commands.zinterstore(destination, storeArgs, keys);
    }

    @Override
    public Long zlexcount(String key, String min, String max) {
        return commands.zlexcount(key, min, max);
    }

    @Override
    public Long zlexcount(String key, Range<? extends String> range) {
        return commands.zlexcount(key, rangeFor(range));
    }

    @Override
    public List<Double> zmscore(String key, String... members) {
        InputStream[] newMembers = new InputStream[members.length];
        for (int i = 0; i < members.length; i++) {
            newMembers[i] = streamFor(members[i]);
        }
        return commands.zmscore(key, newMembers);
    }

    @Override
    public ScoredValue<String> zpopmin(String key) {
        ScoredValue<InputStream> scoredValue = commands.zpopmin(key);
        if (scoredValue == null) {
            return null;
        }
        return scoredValueForStream(scoredValue);
    }

    @Override
    public List<ScoredValue<String>> zpopmin(String key, long count) {
        List<ScoredValue<InputStream>> scores = commands.zpopmin(key, count);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public ScoredValue<String> zpopmax(String key) {
        ScoredValue<InputStream> scoredValue = commands.zpopmax(key);
        if (scoredValue == null) {
            return null;
        }
        return scoredValueForStream(scoredValue);
    }

    @Override
    public List<ScoredValue<String>> zpopmax(String key, long count) {
        List<ScoredValue<InputStream>> scores = commands.zpopmax(key, count);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public String zrandmember(String key) {
        InputStream stream = commands.zrandmember(key);
        return stream == null ? null : stringFrom(stream);
    }

    @Override
    public List<String> zrandmember(String key, long count) {
        List<InputStream> list = commands.zrandmember(key, count);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public ScoredValue<String> zrandmemberWithScores(String key) {
        ScoredValue<InputStream> scoredValue = commands.zrandmemberWithScores(key);
        if (scoredValue == null) {
            return null;
        }
        return scoredValueForStream(scoredValue);
    }

    @Override
    public List<ScoredValue<String>> zrandmemberWithScores(String key, long count) {
        List<ScoredValue<InputStream>> scores = commands.zrandmemberWithScores(key, count);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<String> zrange(String key, long start, long stop) {
        List<InputStream> list = commands.zrange(key, start, stop);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public Long zrange(ValueStreamingChannel<String> channel, String key, long start, long stop) {
        return commands.zrange(new WrappingValueStreamingChannel(channel), key, start, stop);
    }

    @Override
    public List<ScoredValue<String>> zrangeWithScores(String key, long start, long stop) {
        List<ScoredValue<InputStream>> scores = commands.zrangeWithScores(key, start, stop);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public Long zrangeWithScores(ScoredValueStreamingChannel<String> channel, String key, long start, long stop) {
        return commands.zrangeWithScores(new WrappingScoredValueStreamingChannel(channel), key, start, stop);
    }

    @Override
    public List<String> zrangebylex(String key, String min, String max) {
        List<InputStream> list = commands.zrangebylex(key, min, max);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebylex(String key, Range<? extends String> range) {
        List<InputStream> list = commands.zrangebylex(key, rangeFor(range));
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebylex(String key, String min, String max, long offset, long count) {
        List<InputStream> list = commands.zrangebylex(key, min, max, offset, count);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebylex(String key, Range<? extends String> range, Limit limit) {
        List<InputStream> list = commands.zrangebylex(key, rangeFor(range), limit);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebyscore(String key, double min, double max) {
        List<InputStream> list = commands.zrangebyscore(key, min, max);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebyscore(String key, String min, String max) {
        List<InputStream> list = commands.zrangebyscore(key, min, max);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebyscore(String key, Range<? extends Number> range) {
        List<InputStream> list = commands.zrangebyscore(key, range);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebyscore(String key, double min, double max, long offset, long count) {
        List<InputStream> list = commands.zrangebyscore(key, min, max, offset, count);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebyscore(String key, String min, String max, long offset, long count) {
        List<InputStream> list = commands.zrangebyscore(key, min, max, offset, count);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrangebyscore(String key, Range<? extends Number> range, Limit limit) {
        List<InputStream> list = commands.zrangebyscore(key, range, limit);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<String> channel, String key, double min, double max) {
        return commands.zrangebyscore(new WrappingValueStreamingChannel(channel), key, min, max);
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<String> channel, String key, String min, String max) {
        return commands.zrangebyscore(new WrappingValueStreamingChannel(channel), key, min, max);
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<String> channel, String key, Range<? extends Number> range) {
        return commands.zrangebyscore(new WrappingValueStreamingChannel(channel), key, range);
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<String> channel, String key, double min, double max, long offset, long count) {
        return commands.zrangebyscore(new WrappingValueStreamingChannel(channel), key, min, max, offset, count);
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<String> channel, String key, String min, String max, long offset, long count) {
        return commands.zrangebyscore(new WrappingValueStreamingChannel(channel), key, min, max, offset, count);
    }

    @Override
    public Long zrangebyscore(ValueStreamingChannel<String> channel, String key, Range<? extends Number> range, Limit limit) {
        return commands.zrangebyscore(new WrappingValueStreamingChannel(channel), key, range, limit);
    }

    @Override
    public List<ScoredValue<String>> zrangebyscoreWithScores(String key, double min, double max) {
        List<ScoredValue<InputStream>> scores = commands.zrangebyscoreWithScores(key, min, max);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrangebyscoreWithScores(String key, String min, String max) {
        List<ScoredValue<InputStream>> scores = commands.zrangebyscoreWithScores(key, min, max);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrangebyscoreWithScores(String key, Range<? extends Number> range) {
        List<ScoredValue<InputStream>> scores = commands.zrangebyscoreWithScores(key, range);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrangebyscoreWithScores(String key, double min, double max, long offset, long count) {
        List<ScoredValue<InputStream>> scores = commands.zrangebyscoreWithScores(key, min, max, offset, count);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrangebyscoreWithScores(String key, String min, String max, long offset, long count) {
        List<ScoredValue<InputStream>> scores = commands.zrangebyscoreWithScores(key, min, max, offset, count);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrangebyscoreWithScores(String key, Range<? extends Number> range, Limit limit) {
        List<ScoredValue<InputStream>> scores = commands.zrangebyscoreWithScores(key, range, limit);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, double min, double max) {
        return commands.zrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, min, max);
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, String min, String max) {
        return commands.zrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, min, max);
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, Range<? extends Number> range) {
        return commands.zrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, range);
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, double min, double max, long offset, long count) {
        return commands.zrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, min, max, offset, count);
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, String min, String max, long offset, long count) {
        return commands.zrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, min, max, offset, count);
    }

    @Override
    public Long zrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, Range<? extends Number> range, Limit limit) {
        return commands.zrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, range, limit);
    }

    @Override
    public Long zrangestore(String dstKey, String srcKey, Range<Long> range) {
        return commands.zrangestore(dstKey, srcKey, range);
    }

    @Override
    public Long zrangestorebylex(String dstKey, String srcKey, Range<? extends String> range, Limit limit) {
        return commands.zrangestorebylex(dstKey, srcKey, rangeFor(range), limit);
    }

    @Override
    public Long zrangestorebyscore(String dstKey, String srcKey, Range<? extends Number> range, Limit limit) {
        return commands.zrangestorebyscore(dstKey, srcKey, range, limit);
    }

    @Override
    public Long zrank(String key, String member) {
        return commands.zrank(key, streamFor(member));
    }

    @Override
    public Long zrem(String key, String... members) {
        InputStream[] streams = new InputStream[members.length];
        for (int i = 0; i < members.length; i++) {
            streams[i] = streamFor(members[i]);
        }
        return commands.zrem(key, streams);
    }

    @Override
    public Long zremrangebylex(String key, String min, String max) {
        return commands.zremrangebylex(key, min, max);
    }

    @Override
    public Long zremrangebylex(String key, Range<? extends String> range) {
        return commands.zremrangebylex(key, rangeFor(range));
    }

    @Override
    public Long zremrangebyrank(String key, long start, long stop) {
        return commands.zremrangebyrank(key, start, stop);
    }

    @Override
    public Long zremrangebyscore(String key, double min, double max) {
        return commands.zremrangebyscore(key, min, max);
    }

    @Override
    public Long zremrangebyscore(String key, String min, String max) {
        return commands.zremrangebyscore(key, min, max);
    }

    @Override
    public Long zremrangebyscore(String key, Range<? extends Number> range) {
        return commands.zremrangebyscore(key, range);
    }

    @Override
    public List<String> zrevrange(String key, long start, long stop) {
        List<InputStream> list = commands.zrevrange(key, start, stop);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public Long zrevrange(ValueStreamingChannel<String> channel, String key, long start, long stop) {
        return commands.zrevrange(new WrappingValueStreamingChannel(channel), key, start, stop);
    }

    @Override
    public List<ScoredValue<String>> zrevrangeWithScores(String key, long start, long stop) {
        List<ScoredValue<InputStream>> scores = commands.zrevrangeWithScores(key, start, stop);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public Long zrevrangeWithScores(ScoredValueStreamingChannel<String> channel, String key, long start, long stop) {
        return commands.zrevrangeWithScores(new WrappingScoredValueStreamingChannel(channel), key, start, stop);
    }

    @Override
    public List<String> zrevrangebylex(String key, Range<? extends String> range) {
        List<InputStream> list = commands.zrevrangebylex(key, rangeFor(range));
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrevrangebylex(String key, Range<? extends String> range, Limit limit) {
        List<InputStream> list = commands.zrevrangebylex(key, rangeFor(range), limit);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrevrangebyscore(String key, double max, double min) {
        List<InputStream> list = commands.zrevrangebyscore(key, max, min);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrevrangebyscore(String key, String max, String min) {
        List<InputStream> list = commands.zrevrangebyscore(key, max, min);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrevrangebyscore(String key, Range<? extends Number> range) {
        List<InputStream> list = commands.zrevrangebyscore(key, range);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrevrangebyscore(String key, double max, double min, long offset, long count) {
        List<InputStream> list = commands.zrevrangebyscore(key, max, min, offset, count);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrevrangebyscore(String key, String max, String min, long offset, long count) {
        List<InputStream> list = commands.zrevrangebyscore(key, max, min, offset, count);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zrevrangebyscore(String key, Range<? extends Number> range, Limit limit) {
        List<InputStream> list = commands.zrevrangebyscore(key, range, limit);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<String> channel, String key, double max, double min) {
        return commands.zrevrangebyscore(new WrappingValueStreamingChannel(channel), key, max, min);
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<String> channel, String key, String max, String min) {
        return commands.zrevrangebyscore(new WrappingValueStreamingChannel(channel), key, max, min);
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<String> channel, String key, Range<? extends Number> range) {
        return commands.zrevrangebyscore(new WrappingValueStreamingChannel(channel), key, range);
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<String> channel, String key, double max, double min, long offset, long count) {
        return commands.zrevrangebyscore(new WrappingValueStreamingChannel(channel), key, max, min, offset, count);
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<String> channel, String key, String max, String min, long offset, long count) {
        return commands.zrevrangebyscore(new WrappingValueStreamingChannel(channel), key, max, min, offset, count);
    }

    @Override
    public Long zrevrangebyscore(ValueStreamingChannel<String> channel, String key, Range<? extends Number> range, Limit limit) {
        return commands.zrevrangebyscore(new WrappingValueStreamingChannel(channel), key, range, limit);
    }

    @Override
    public List<ScoredValue<String>> zrevrangebyscoreWithScores(String key, double max, double min) {
        List<ScoredValue<InputStream>> scores = commands.zrevrangebyscoreWithScores(key, max, min);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrevrangebyscoreWithScores(String key, String max, String min) {
        List<ScoredValue<InputStream>> scores = commands.zrevrangebyscoreWithScores(key, max, min);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrevrangebyscoreWithScores(String key, Range<? extends Number> range) {
        List<ScoredValue<InputStream>> scores = commands.zrevrangebyscoreWithScores(key, range);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrevrangebyscoreWithScores(String key, double max, double min, long offset, long count) {
        List<ScoredValue<InputStream>> scores = commands.zrevrangebyscoreWithScores(key, max, min, offset, count);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrevrangebyscoreWithScores(String key, String max, String min, long offset, long count) {
        List<ScoredValue<InputStream>> scores = commands.zrevrangebyscoreWithScores(key, max, min, offset, count);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zrevrangebyscoreWithScores(String key, Range<? extends Number> range, Limit limit) {
        List<ScoredValue<InputStream>> scores = commands.zrevrangebyscoreWithScores(key, range, limit);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, double max, double min) {
        return commands.zrevrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, max, min);
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, String max, String min) {
        return commands.zrevrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, max, min);
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, Range<? extends Number> range) {
        return commands.zrevrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, range);
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, double max, double min, long offset, long count) {
        return commands.zrevrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, max, min, offset, count);
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, String max, String min, long offset, long count) {
        return commands.zrevrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, max, min, offset, count);
    }

    @Override
    public Long zrevrangebyscoreWithScores(ScoredValueStreamingChannel<String> channel, String key, Range<? extends Number> range, Limit limit) {
        return commands.zrevrangebyscoreWithScores(new WrappingScoredValueStreamingChannel(channel), key, range, limit);
    }

    @Override
    public Long zrevrangestore(String dstKey, String srcKey, Range<Long> range) {
        return commands.zrevrangestore(dstKey, srcKey, range);
    }

    @Override
    public Long zrevrangestorebylex(String dstKey, String srcKey, Range<? extends String> range, Limit limit) {
        return commands.zrevrangestorebylex(dstKey, srcKey, rangeFor(range), limit);
    }

    @Override
    public Long zrevrangestorebyscore(String dstKey, String srcKey, Range<? extends Number> range, Limit limit) {
        return commands.zrevrangestorebyscore(dstKey, srcKey, range, limit);
    }

    @Override
    public Long zrevrank(String key, String member) {
        return commands.zrevrank(key, streamFor(member));
    }

    @Override
    public ScoredValueScanCursor<String> zscan(String key) {
        ScoredValueScanCursor<InputStream> cursor = commands.zscan(key);
        List<ScoredValue<InputStream>> scores = cursor.getValues();
        ScoredValueScanCursor<String> newCursor = new ScoredValueScanCursor<>();
        for (ScoredValue<InputStream> scoredValue : scores) {
            newCursor.getValues().add(scoredValueForStream(scoredValue));
        }
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        return newCursor;
    }

    @Override
    public ScoredValueScanCursor<String> zscan(String key, ScanArgs scanArgs) {
        ScoredValueScanCursor<InputStream> cursor = commands.zscan(key, scanArgs);
        List<ScoredValue<InputStream>> scores = cursor.getValues();
        ScoredValueScanCursor<String> newCursor = new ScoredValueScanCursor<>();
        for (ScoredValue<InputStream> scoredValue : scores) {
            newCursor.getValues().add(scoredValueForStream(scoredValue));
        }
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        return newCursor;
    }

    @Override
    public ScoredValueScanCursor<String> zscan(String key, ScanCursor scanCursor, ScanArgs scanArgs) {
        ScoredValueScanCursor<InputStream> cursor = commands.zscan(key, scanCursor, scanArgs);
        List<ScoredValue<InputStream>> scores = cursor.getValues();
        ScoredValueScanCursor<String> newCursor = new ScoredValueScanCursor<>();
        for (ScoredValue<InputStream> scoredValue : scores) {
            newCursor.getValues().add(scoredValueForStream(scoredValue));
        }
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        return newCursor;
    }

    @Override
    public ScoredValueScanCursor<String> zscan(String key, ScanCursor scanCursor) {
        ScoredValueScanCursor<InputStream> cursor = commands.zscan(key, scanCursor);
        List<ScoredValue<InputStream>> scores = cursor.getValues();
        ScoredValueScanCursor<String> newCursor = new ScoredValueScanCursor<>();
        for (ScoredValue<InputStream> scoredValue : scores) {
            newCursor.getValues().add(scoredValueForStream(scoredValue));
        }
        newCursor.setCursor(cursor.getCursor());
        newCursor.setFinished(cursor.isFinished());
        return newCursor;
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<String> channel, String key) {
        return commands.zscan(new WrappingScoredValueStreamingChannel(channel), key);
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<String> channel, String key, ScanArgs scanArgs) {
        return commands.zscan(new WrappingScoredValueStreamingChannel(channel), key, scanArgs);
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<String> channel, String key, ScanCursor scanCursor, ScanArgs scanArgs) {
        return commands.zscan(new WrappingScoredValueStreamingChannel(channel), key, scanCursor, scanArgs);
    }

    @Override
    public StreamScanCursor zscan(ScoredValueStreamingChannel<String> channel, String key, ScanCursor scanCursor) {
        return commands.zscan(new WrappingScoredValueStreamingChannel(channel), key, scanCursor);
    }

    @Override
    public Double zscore(String key, String member) {
        return commands.zscore(key, streamFor(member));
    }

    @Override
    public List<String> zunion(String... keys) {
        List<InputStream> list = commands.zunion(keys);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<String> zunion(ZAggregateArgs aggregateArgs, String... keys) {
        List<InputStream> list = commands.zunion(aggregateArgs, keys);
        List<String> retval = new ArrayList<>(list.size());
        for (InputStream stream : list) {
            retval.add(stringFrom(stream));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zunionWithScores(ZAggregateArgs aggregateArgs, String... keys) {
        List<ScoredValue<InputStream>> scores = commands.zunionWithScores(aggregateArgs, keys);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public List<ScoredValue<String>> zunionWithScores(String... keys) {
        List<ScoredValue<InputStream>> scores = commands.zunionWithScores(keys);
        List<ScoredValue<String>> retval = new ArrayList<>(scores.size());
        for (ScoredValue<InputStream> scoredValue : scores) {
            retval.add(scoredValueForStream(scoredValue));
        }
        return retval;
    }

    @Override
    public Long zunionstore(String destination, String... keys) {
        return commands.zunionstore(destination, keys);
    }

    @Override
    public Long zunionstore(String destination, ZStoreArgs storeArgs, String... keys) {
        return commands.zunionstore(destination, storeArgs, keys);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static String stringFrom(InputStream stream) {
        return RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
    }

    private static InputStream streamFor(String member) {
        return RedisCommandUtils.string2Stream(member);
    }

    private static ScoredValue<String> scoredValueForStream(ScoredValue<InputStream> scoredValue) {
        if (scoredValue.hasValue()) {
            return (ScoredValue<String>) ScoredValue.fromNullable(scoredValue.getScore(), stringFrom(scoredValue.getValue()));
        }
        return ScoredValue.empty();
    }

    private static ScoredValue<InputStream> scoredValueForString(ScoredValue<String> scoredValue) {
        if (scoredValue.hasValue()) {
            return (ScoredValue<InputStream>) ScoredValue.fromNullable(scoredValue.getScore(), streamFor(scoredValue.getValue()));
        }
        return ScoredValue.empty();
    }

    private static Range<InputStream> rangeFor(Range<? extends String> range) {
        return Range.create(streamFor(range.getLower().getValue()), streamFor(range.getUpper().getValue()));
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

    private static final class WrappingScoredValueStreamingChannel implements ScoredValueStreamingChannel<InputStream> {

        private final ScoredValueStreamingChannel<String> channel;

        /**
         * Initializes a new {@link WrappingScoredValueStreamingChannel}.
         *
         * @param channel The channel to wrap
         */
        private WrappingScoredValueStreamingChannel(ScoredValueStreamingChannel<String> channel) {
            this.channel = channel;
        }

        @Override
        public void onValue(ScoredValue<InputStream> value) {
            channel.onValue(scoredValueForStream(value));
        }
    }

}
