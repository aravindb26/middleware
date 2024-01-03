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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.ScanCursor;
import io.lettuce.core.StreamScanCursor;
import io.lettuce.core.ValueScanCursor;
import io.lettuce.core.api.sync.RedisSetCommands;
import io.lettuce.core.output.ValueStreamingChannel;


/**
 * {@link StringRedisSetCommands} - The Redis set commands for string values.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class StringRedisSetCommands implements RedisSetCommands<String, String> {

    private static final int DEFAULT_CAPACITY = 32;

    private final RedisSetCommands<String, InputStream> commands;

    /**
     * Initializes a new {@link StringRedisSetCommands}.
     */
    public StringRedisSetCommands(RedisSetCommands<String, InputStream> commands) {
        super();
        this.commands = commands;
    }

    @Override
    public Long sadd(String key, String... members) {
        if (members == null || members.length <= 0) {
            return Long.valueOf(0);
        }

        List<InputStream> mems = new ArrayList<InputStream>(members.length);
        for (String member : members) {
            mems.add(RedisCommandUtils.string2Stream(member));
        }
        return commands.sadd(key, mems.toArray(new InputStream[mems.size()]));
    }

    @Override
    public Long scard(String key) {
        return commands.scard(key);
    }

    @Override
    public Set<String> sdiff(String... keys) {
        Set<InputStream> diff = commands.sdiff(keys);
        if (diff == null) {
            return null;
        }

        int size = diff.size();
        if (size <= 0) {
            return Collections.emptySet();
        }

        Set<String> retval = new LinkedHashSet<>(size);
        for (InputStream stream : diff) {
            String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
            if (str != null) {
                retval.add(str);
            }
        }
        return retval;
    }

    @Override
    public Long sdiff(ValueStreamingChannel<String> channel, String... keys) {
        throw new UnsupportedOperationException("StringRedisSetCommands.sdiff()");
    }

    @Override
    public Long sdiffstore(String destination, String... keys) {
        return commands.sdiffstore(destination, keys);
    }

    @Override
    public Set<String> sinter(String... keys) {
        Set<InputStream> streams = commands.sinter(keys);
        if (streams == null) {
            return null;
        }

        int size = streams.size();
        if (size <= 0) {
            return Collections.emptySet();
        }

        Set<String> retval = new LinkedHashSet<>(size);
        for (InputStream stream : streams) {
            String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
            if (str != null) {
                retval.add(str);
            }
        }
        return retval;
    }

    @Override
    public Long sinter(ValueStreamingChannel<String> channel, String... keys) {
        throw new UnsupportedOperationException("StringRedisSetCommands.sinter()");
    }

    @Override
    public Long sinterstore(String destination, String... keys) {
        return commands.sinterstore(destination, keys);
    }

    @Override
    public Long sintercard(long limit, String... keys) {
        return commands.sintercard(limit, keys);
    }

    @Override
    public Long sintercard(String... keys) {
        return commands.sintercard(keys);
    }

    @Override
    public Boolean sismember(String key, String member) {
        return commands.sismember(key, RedisCommandUtils.string2Stream(member));
    }

    @Override
    public Set<String> smembers(String key) {
        Set<InputStream> streams = commands.smembers(key);
        if (streams == null) {
            return null;
        }

        int size = streams.size();
        if (size <= 0) {
            return Collections.emptySet();
        }

        Set<String> retval = new LinkedHashSet<>(size);
        for (InputStream stream : streams) {
            String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
            if (str != null) {
                retval.add(str);
            }
        }
        return retval;
    }

    @Override
    public Long smembers(ValueStreamingChannel<String> channel, String key) {
        throw new UnsupportedOperationException("StringRedisSetCommands.smembers()");
    }

    @Override
    public List<Boolean> smismember(String key, String... members) {
        if (members == null || members.length <= 0) {
            return Collections.emptyList();
        }

        List<InputStream> mems = new ArrayList<InputStream>(members.length);
        for (String member : members) {
            mems.add(RedisCommandUtils.string2Stream(member));
        }
        return commands.smismember(key, mems.toArray(new InputStream[mems.size()]));
    }

    @Override
    public Boolean smove(String source, String destination, String member) {
        return commands.smove(source, destination, RedisCommandUtils.string2Stream(member));
    }

    @Override
    public String spop(String key) {
        return RedisCommandUtils.stream2String(commands.spop(key), DEFAULT_CAPACITY);
    }

    @Override
    public Set<String> spop(String key, long count) {
        Set<InputStream> streams = commands.spop(key, count);
        if (streams == null) {
            return null;
        }

        int size = streams.size();
        if (size <= 0) {
            return Collections.emptySet();
        }

        Set<String> retval = new LinkedHashSet<>(size);
        for (InputStream stream : streams) {
            String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
            if (str != null) {
                retval.add(str);
            }
        }
        return retval;
    }

    @Override
    public String srandmember(String key) {
        return RedisCommandUtils.stream2String(commands.srandmember(key), DEFAULT_CAPACITY);
    }

    @Override
    public List<String> srandmember(String key, long count) {
        List<InputStream> streams = commands.srandmember(key, count);
        if (streams == null) {
            return null;
        }

        int size = streams.size();
        if (size <= 0) {
            return Collections.emptyList();
        }

        List<String> retval = new ArrayList<>(size);
        for (InputStream stream : streams) {
            String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
            if (str != null) {
                retval.add(str);
            }
        }
        return retval;
    }

    @Override
    public Long srandmember(ValueStreamingChannel<String> channel, String key, long count) {
        throw new UnsupportedOperationException("StringRedisSetCommands.srandmember()");
    }

    @Override
    public Long srem(String key, String... members) {
        if (members == null || members.length <= 0) {
            return Long.valueOf(0);
        }

        List<InputStream> mems = new ArrayList<InputStream>(members.length);
        for (String member : members) {
            mems.add(RedisCommandUtils.string2Stream(member));
        }
        return commands.srem(key, mems.toArray(new InputStream[mems.size()]));
    }

    @Override
    public Set<String> sunion(String... keys) {
        Set<InputStream> streams = commands.sunion(keys);
        if (streams == null) {
            return null;
        }

        int size = streams.size();
        if (size <= 0) {
            return Collections.emptySet();
        }

        Set<String> retval = new LinkedHashSet<>(size);
        for (InputStream stream : streams) {
            String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
            if (str != null) {
                retval.add(str);
            }
        }
        return retval;
    }

    @Override
    public Long sunion(ValueStreamingChannel<String> channel, String... keys) {
        throw new UnsupportedOperationException("StringRedisSetCommands.sunion()");
    }

    @Override
    public Long sunionstore(String destination, String... keys) {
        return commands.sunionstore(destination, keys);
    }

    @Override
    public ValueScanCursor<String> sscan(String key) {
        ValueScanCursor<InputStream> cursor = commands.sscan(key);

        List<InputStream> streams = cursor.getValues();
        List<String> retval;
        if (streams == null) {
            retval = Collections.emptyList();
        } else {
            int size = streams.size();
            if (size <= 0) {
                retval = Collections.emptyList();
            } else {
                retval = new ArrayList<>(size);
                for (InputStream stream : streams) {
                    String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
                    if (str != null) {
                        retval.add(str);
                    }
                }
            }
        }

        return new ValueScanCursor<String>() {

            @Override
            public List<String> getValues() {
                return retval;
            }

            @Override
            public String getCursor() {
                return cursor.getCursor();
            }

            @Override
            public boolean isFinished() {
                return cursor.isFinished();
            }
        };
    }

    @Override
    public ValueScanCursor<String> sscan(String key, ScanArgs scanArgs) {
        ValueScanCursor<InputStream> cursor = commands.sscan(key, scanArgs);

        List<InputStream> streams = cursor.getValues();
        List<String> retval;
        if (streams == null) {
            retval = Collections.emptyList();
        } else {
            int size = streams.size();
            if (size <= 0) {
                retval = Collections.emptyList();
            } else {
                retval = new ArrayList<>(size);
                for (InputStream stream : streams) {
                    String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
                    if (str != null) {
                        retval.add(str);
                    }
                }
            }
        }

        return new ValueScanCursor<String>() {

            @Override
            public List<String> getValues() {
                return retval;
            }

            @Override
            public String getCursor() {
                return cursor.getCursor();
            }

            @Override
            public boolean isFinished() {
                return cursor.isFinished();
            }
        };
    }

    @Override
    public ValueScanCursor<String> sscan(String key, ScanCursor scanCursor, ScanArgs scanArgs) {
        ValueScanCursor<InputStream> cursor = commands.sscan(key, scanCursor, scanArgs);

        List<InputStream> streams = cursor.getValues();
        List<String> retval;
        if (streams == null) {
            retval = Collections.emptyList();
        } else {
            int size = streams.size();
            if (size <= 0) {
                retval = Collections.emptyList();
            } else {
                retval = new ArrayList<>(size);
                for (InputStream stream : streams) {
                    String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
                    if (str != null) {
                        retval.add(str);
                    }
                }
            }
        }

        return new ValueScanCursor<String>() {

            @Override
            public List<String> getValues() {
                return retval;
            }

            @Override
            public String getCursor() {
                return cursor.getCursor();
            }

            @Override
            public boolean isFinished() {
                return cursor.isFinished();
            }
        };
    }

    @Override
    public ValueScanCursor<String> sscan(String key, ScanCursor scanCursor) {
        ValueScanCursor<InputStream> cursor = commands.sscan(key, scanCursor);

        List<InputStream> streams = cursor.getValues();
        List<String> retval;
        if (streams == null) {
            retval = Collections.emptyList();
        } else {
            int size = streams.size();
            if (size <= 0) {
                retval = Collections.emptyList();
            } else {
                retval = new ArrayList<>(size);
                for (InputStream stream : streams) {
                    String str = RedisCommandUtils.stream2String(stream, DEFAULT_CAPACITY);
                    if (str != null) {
                        retval.add(str);
                    }
                }
            }
        }

        return new ValueScanCursor<String>() {

            @Override
            public List<String> getValues() {
                return retval;
            }

            @Override
            public String getCursor() {
                return cursor.getCursor();
            }

            @Override
            public boolean isFinished() {
                return cursor.isFinished();
            }
        };
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<String> channel, String key) {
        throw new UnsupportedOperationException("StringRedisSetCommands.sscan()");
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<String> channel, String key, ScanArgs scanArgs) {
        throw new UnsupportedOperationException("StringRedisSetCommands.sscan()");
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<String> channel, String key, ScanCursor scanCursor, ScanArgs scanArgs) {
        throw new UnsupportedOperationException("StringRedisSetCommands.sscan()");
    }

    @Override
    public StreamScanCursor sscan(ValueStreamingChannel<String> channel, String key, ScanCursor scanCursor) {
        throw new UnsupportedOperationException("StringRedisSetCommands.sscan()");
    }

}
