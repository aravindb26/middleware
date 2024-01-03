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

package com.openexchange.sessiond.redis.commands;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.openexchange.cluster.serialization.session.SessionCodec;
import com.openexchange.java.Streams;
import com.openexchange.redis.RedisCommand;
import com.openexchange.redis.RedisConversionException;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.Session;
import com.openexchange.session.VersionMismatchException;
import com.openexchange.sessiond.redis.RedisSessionVersionService;
import com.openexchange.sessiond.redis.VersionMismatchRuntimeException;
import io.lettuce.core.BitFieldArgs;
import io.lettuce.core.GetExArgs;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisException;
import io.lettuce.core.SetArgs;
import io.lettuce.core.StrAlgoArgs;
import io.lettuce.core.StringMatchResult;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.output.KeyValueStreamingChannel;


/**
 * {@link SessionRedisStringCommands} - The Redis string commands converting to/from a session's JSON representation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SessionRedisStringCommands implements RedisStringCommands<String, Session> {

    /**
     * A simple handler that is called when a version mismatch exception occurs.
     */
    public static interface VersionMismatchHandler {

        /**
         * Invoked if a version mismatch exception occurred.
         *
         * @param key The key for which the version mismatch happened
         * @param e The version mismatch exception
         * @param redisCommand The Redis command during which the mismatch happened
         * @return <code>true</code> if successfully handled; otherwise <code>false</code> to signal version mismatch could not be handled
         */
        boolean onMismatch(String key, VersionMismatchException e, RedisCommand redisCommand);
    }

    /** The constant version mismtach handler that does nothing */
    public static final VersionMismatchHandler DO_NOTHING_VERSION_MISMATCH_HANDLER = (key, e, redisCommand) -> true;

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final RedisStringCommands<String, InputStream> commands;
    private final ObfuscatorService obfuscatorService;
    private final VersionMismatchHandler versionMismatchHandler;

    /**
     * Initializes a new {@link SessionRedisStringCommands}.
     *
     * @param commands The wrapped string commands
     * @param obfuscatorService The obfuscator service
     * @param optionalVersionMismatchHandler An optional handler that is called when version of stored session data does not match the application's expected version
     */
    public SessionRedisStringCommands(RedisStringCommands<String, InputStream> commands, ObfuscatorService obfuscatorService, VersionMismatchHandler optionalVersionMismatchHandler) {
        super();
        this.commands = commands;
        this.obfuscatorService = obfuscatorService;
        this.versionMismatchHandler = optionalVersionMismatchHandler;
    }

    @Override
    public Long append(String key, Session value) {
        InputStream stream = null;
        try {
            stream = SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false);
            return commands.append(key, stream);
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert session", e);
        } finally {
            Streams.close(stream);
        }
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
    public Session get(String key) {
        try {
            InputStream jsonData = commands.get(key);
            return jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance());
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(key, e, RedisCommand.GET)) {
                return null;
            }
            throw new VersionMismatchRuntimeException(e);
        } catch (Exception e) {
            throw new RedisConversionException("Failed to get", e);
        }
    }

    @Override
    public Long getbit(String key, long offset) {
        return commands.getbit(key, offset);
    }

    @Override
    public Session getdel(String key) {
        try {
            InputStream jsonData = commands.getdel(key);
            return jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance());
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            // Already deleted by GETDEL command
            return null;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to getdel", e);
        }
    }

    @Override
    public Session getex(String key, GetExArgs args) {
        try {
            InputStream jsonData = commands.getex(key, args);
            return jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance());
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(key, e, RedisCommand.GETEX)) {
                return null;
            }
            throw new VersionMismatchRuntimeException(e);
        } catch (Exception e) {
            throw new RedisConversionException("Failed to getex", e);
        }
    }

    @Override
    public Session getrange(String key, long start, long end) {
        try {
            InputStream jsonData = commands.getrange(key, start, end);
            return jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance());
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(key, e, RedisCommand.GETRANGE)) {
                return null;
            }
            throw new VersionMismatchRuntimeException(e);
        } catch (Exception e) {
            throw new RedisConversionException("Failed to getex", e);
        }
    }

    @Override
    public Session getset(String key, Session value) {
        try {
            InputStream jsonData = commands.getset(key, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
            return jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance());
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(key, e, RedisCommand.GETSET)) {
                return null;
            }
            throw new VersionMismatchRuntimeException(e);
        } catch (Exception e) {
            throw new RedisConversionException("Failed to getset", e);
        }
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
    public List<KeyValue<String, Session>> mget(String... keys) {
        try {
            List<KeyValue<String, InputStream>> entries = commands.mget(keys);
            List<KeyValue<String, Session>> retval = new ArrayList<>(entries.size());
            for (KeyValue<String, InputStream> keyValue : entries) {
                if (keyValue.hasValue()) {
                    InputStream jsonData = keyValue.getValue();
                    try {
                        retval.add(jsonData == null ? KeyValue.empty(keyValue.getKey()) : KeyValue.just(keyValue.getKey(), SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance())));
                    } catch (VersionMismatchException e) {
                        if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(keyValue.getKey(), e, RedisCommand.MGET)) {
                            retval.add(KeyValue.empty(keyValue.getKey()));
                        } else {
                            throw e;
                        }
                    }
                } else {
                    retval.add(KeyValue.empty(keyValue.getKey()));
                }
            }
            return retval;
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            throw new VersionMismatchRuntimeException(e);
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public Long mget(KeyValueStreamingChannel<String, Session> channel, String... keys) {
        return commands.mget(new WrappingKeyValueStreamingChannel(channel, obfuscatorService, versionMismatchHandler, RedisCommand.MGET), keys);
    }

    @Override
    public String mset(Map<String, Session> map) {
        try {
            Map<String, InputStream> m = new LinkedHashMap<>(map.size());
            for (Map.Entry<String, Session> e : map.entrySet()) {
                m.put(e.getKey(), SessionCodec.session2Json(e.getValue(), obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
            }
            return commands.mset(m);
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public Boolean msetnx(Map<String, Session> map) {
        try {
            Map<String, InputStream> m = new LinkedHashMap<>(map.size());
            for (Map.Entry<String, Session> e : map.entrySet()) {
                m.put(e.getKey(), SessionCodec.session2Json(e.getValue(), obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
            }
            return commands.msetnx(m);
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public String set(String key, Session value) {
        try {
            return commands.set(key, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public String set(String key, Session value, SetArgs setArgs) {
        try {
            return commands.set(key, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false), setArgs);
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public Session setGet(String key, Session value) {
        try {
            InputStream jsonData = commands.setGet(key, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
            return jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance());
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(key, e, RedisCommand.GETSET)) {
                return null;
            }
            throw new VersionMismatchRuntimeException(e);
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public Session setGet(String key, Session value, SetArgs setArgs) {
        try {
            InputStream jsonData = commands.setGet(key, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false), setArgs);
            return jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance());
        } catch (RedisException e) {
            throw e;
        } catch (VersionMismatchException e) {
            if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(key, e, RedisCommand.GETSET)) {
                return null;
            }
            throw new VersionMismatchRuntimeException(e);
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public Long setbit(String key, long offset, int value) {
        return commands.setbit(key, offset, value);
    }

    @Override
    public String setex(String key, long seconds, Session value) {
        try {
            return commands.setex(key, seconds, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public String psetex(String key, long milliseconds, Session value) {
        try {
            return commands.psetex(key, milliseconds, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public Boolean setnx(String key, Session value) {
        try {
            return commands.setnx(key, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public Long setrange(String key, long offset, Session value) {
        try {
            return commands.setrange(key, offset, SessionCodec.session2Json(value, obfuscatorService, RedisSessionVersionService.getInstance()).getStream(false));
        } catch (RedisException e) {
            throw e;
        } catch (Exception e) {
            throw new RedisConversionException("Failed to convert", e);
        }
    }

    @Override
    public StringMatchResult stralgoLcs(StrAlgoArgs strAlgoArgs) {
        return commands.stralgoLcs(strAlgoArgs);
    }

    @Override
    public Long strlen(String key) {
        return commands.strlen(key);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final class WrappingKeyValueStreamingChannel implements KeyValueStreamingChannel<String, InputStream> {

        private final KeyValueStreamingChannel<String, Session> channel;
        private final ObfuscatorService obfuscatorService;
        private final VersionMismatchHandler versionMismatchHandler;
        private final RedisCommand redisCommand;

        /**
         * Initializes a new {@link WrappingKeyValueStreamingChannel}.
         *
         * @param channel The channel to wrap
         * @param obfuscatorService The obfuscator service
         * @param commands
         */
        private WrappingKeyValueStreamingChannel(KeyValueStreamingChannel<String, Session> channel, ObfuscatorService obfuscatorService, VersionMismatchHandler versionMismatchHandler, RedisCommand redisCommand) {
            this.channel = channel;
            this.obfuscatorService = obfuscatorService;
            this.versionMismatchHandler = versionMismatchHandler;
            this.redisCommand = redisCommand;
        }

        @Override
        public void onKeyValue(String key, InputStream jsonData) {
            try {
                channel.onKeyValue(key, jsonData == null ? null : SessionCodec.stream2Session(jsonData, obfuscatorService, RedisSessionVersionService.getInstance()));
            } catch (RedisException e) {
                throw e;
            } catch (VersionMismatchException e) {
                if (versionMismatchHandler != null && versionMismatchHandler.onMismatch(key, e, redisCommand)) {
                    channel.onKeyValue(key, null);
                } else {
                    throw new VersionMismatchRuntimeException(e);
                }
            } catch (Exception e) {
                throw new RedisConversionException("Failed to convert JSON session data", e);
            }
        }
    }
}
