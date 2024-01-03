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

package com.openexchange.cluster.map.redis;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import com.openexchange.cluster.map.ClusterMap;
import com.openexchange.cluster.map.ApplicationName;
import com.openexchange.cluster.map.MapName;
import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.redis.RedisConnector;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.SetArgs;
import io.lettuce.core.TransactionResult;
import io.lettuce.core.api.sync.RedisTransactionalCommands;

/**
 * {@link RedisClusterMap} - The Redis-backed map.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 * @param <V> The type for map values
 */
public class RedisClusterMap<V> implements ClusterMap<V> {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RedisClusterMap.class);
    }

    private static final char DELIMITER = ':';

    private static final short SCAN_RESULTS_LIMIT = 1000;

    /** The application name portion of the map key */
    private final ApplicationName appName;

    /** The map name portion of the map key */
    private final MapName mapName;

    /** The codec to serialize/deserialize values */
    private final MapCodec<V> codec;

    /** The Redis connector */
    private final RedisConnector connector;

    /**
     * Initializes a new {@link RedisClusterMap}.
     *
     * @param appName The application name; e.g. <code>"ox-map"</code>
     * @param mapName The map name; e.g. <code>"distributedFiles"</code>
     * @param codec The codec
     * @param connector The Redis connector
     */
    public RedisClusterMap(ApplicationName appName, MapName mapName, MapCodec<V> codec, RedisConnector connector) {
        super();
        this.appName = appName;
        this.mapName = mapName;
        this.codec = codec;
        this.connector = connector;
    }

    /**
     * Builds the fully-qualifying key; e.g.
     *
     * <pre>
     * "ox-map:distributedFiles:1337"
     * </pre>
     *
     * @param key The element's key; e.g. <code>"1337"</code>
     * @return The fully-qualifying key
     */
    public String getFullKey(String key) {
        return new StringBuilder(appName.getName()).append(DELIMITER).append(mapName.getName()).append(DELIMITER).append(key).toString();
    }

    /**
     * Gets the codec to serialize/deserialize values.
     *
     * @return The codec
     */
    public MapCodec<V> getCodec() {
        return codec;
    }

    @Override
    public Set<String> keySet() throws OXException {
        String pattern = new StringBuilder(appName.getName()).append(DELIMITER).append(mapName.getName()).append(DELIMITER).toString();
        int patternLength = pattern.length();
        ScanArgs scanArgs = ScanArgs.Builder.matches(pattern).limit(SCAN_RESULTS_LIMIT);

        return connector.executeOperation(commandsProvider -> {
            try {
                Set<String> keySet = new LinkedHashSet<>();
                KeyScanCursor<String> cursor = commandsProvider.getKeyCommands().scan(scanArgs);
                while (cursor != null) {
                    // Obtain current keys...
                    List<String> keys = cursor.getKeys();

                    // ... and add them to set
                    int size = keys.size();
                    if (size > 0) {
                        for (String skey : keys) {
                            keySet.add(skey.substring(patternLength));
                        }
                    }

                    // Move cursor forward
                    cursor = cursor.isFinished() ? null : commandsProvider.getKeyCommands().scan(cursor, scanArgs);
                }
                return keySet;
            } catch (RuntimeException e) {
                throw OXException.general("Redis error", e);
            }
        });
    }

    @Override
    public boolean containsKey(String key) throws OXException {
        if (Strings.isEmpty(key)) {
            return false;
        }

        try {
            return connector.executeOperation(commandsProvider -> Boolean.valueOf(commandsProvider.getKeyCommands().exists(getFullKey(key)).longValue() > 0)).booleanValue();
        } catch (RuntimeException e) {
            throw OXException.general("Redis error", e);
        }
    }

    @Override
    public V get(String key) throws OXException {
        if (Strings.isEmpty(key)) {
            return null;
        }

        return connector.executeOperation(commandsProvider -> {
            InputStream data = commandsProvider.getRawStringCommands().get(getFullKey(key));
            if (data == null) {
                return null;
            }
            try {
                return getCodec().deserializeValue(data);
            } catch (RuntimeException e) {
                throw OXException.general("Redis error", e);
            } catch (OXException e) {
                throw e;
            } catch (Exception e) {
                throw OXException.general("Failed to deserialize value", e);
            } finally {
                Streams.close(data);
            }
        });
    }

    @Override
    public V put(String key, V value, long expireTimeMillis) throws OXException {
        if (Strings.isEmpty(key) || value == null) {
            throw new IllegalArgumentException("Neither key nor value may be null or empty");
        }

        return connector.executeOperation(commandsProvider -> {
            InputStream data = null;
            InputStream serializedValue = null;
            try {
                serializedValue = getCodec().serializeValue(value);
                if (expireTimeMillis > 0) {
                    data = commandsProvider.getRawStringCommands().setGet(getFullKey(key), serializedValue, new SetArgs().px(expireTimeMillis));
                } else {
                    data = commandsProvider.getRawStringCommands().getset(getFullKey(key), serializedValue);
                }
                return data == null ? null : getCodec().deserializeValue(data);
            } catch (RuntimeException e) {
                throw OXException.general("Redis error", e);
            } catch (OXException e) {
                throw e;
            } catch (Exception e) {
                throw OXException.general("Failed to de-/serialize value", e);
            } finally {
                Streams.close(serializedValue, data);
            }
        });
    }

    @Override
    public V remove(String key) throws OXException {
        if (Strings.isEmpty(key)) {
            return null;
        }

        return connector.executeOperation(commandsProvider -> {
            InputStream data = commandsProvider.getRawStringCommands().getdel(getFullKey(key));
            try {
                return data == null ? null : getCodec().deserializeValue(data);
            } catch (RuntimeException e) {
                throw OXException.general("Redis error", e);
            } catch (OXException e) {
                throw e;
            } catch (Exception e) {
                throw OXException.general("Failed to deserialize value", e);
            } finally {
                Streams.close(data);
            }
        });
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue, long expireTimeMillis) throws OXException {
        if (Strings.isEmpty(key) || oldValue == null || newValue == null) {
            throw new IllegalArgumentException("Neither key nor value may be null or empty");
        }

        return connector.executeOperation(commandsProvider -> {
            InputStream is = null;
            try {
                byte[] serializedValue = getCodec().serializeValue2Bytes(newValue);

                RedisTransactionalCommands<String, InputStream> transactionalCommands = commandsProvider.optTransactionalCommands().orElse(null);
                if (transactionalCommands == null) {
                    // No support for Redis transactions
                    byte[] replaced;
                    if (expireTimeMillis > 0) {
                        replaced = Streams.stream2bytes(commandsProvider.getRawStringCommands().setGet(getFullKey(key), Streams.newByteArrayInputStream(serializedValue), new SetArgs().xx().px(expireTimeMillis)));
                    } else {
                        replaced = Streams.stream2bytes(commandsProvider.getRawStringCommands().setGet(getFullKey(key), Streams.newByteArrayInputStream(serializedValue), new SetArgs().xx()));
                    }
                    if (replaced != null && Arrays.equals(replaced, getCodec().serializeValue2Bytes(oldValue))) {
                        return Boolean.TRUE;
                    }

                    // Restore old value
                    is = getCodec().serializeValue(oldValue);
                    commandsProvider.getRawStringCommands().set(getFullKey(key), is);
                    return Boolean.FALSE;
                }

                /*-
                 * Use transaction features
                 *
                 * Example:
                 *  WATCH mykey
                 *  val = GET mykey
                 *  val = val + 1
                 *  MULTI
                 *  SET mykey $val
                 *  EXEC
                 */
                transactionalCommands.watch(getFullKey(key));

                byte[] existent = Streams.stream2bytes(commandsProvider.getRawStringCommands().get(getFullKey(key)));
                if (existent != null && Arrays.equals(existent, getCodec().serializeValue2Bytes(oldValue))) {
                    // Expected value
                    boolean error = true;
                    transactionalCommands.multi();
                    try {
                        commandsProvider.getRawStringCommands().set(getFullKey(key), Streams.newByteArrayInputStream(serializedValue));
                        TransactionResult result = transactionalCommands.exec();
                        error = false;
                        if (result.isEmpty() == false && "OK".equals(result.get(0))) {
                            return Boolean.TRUE;
                        }
                    } finally {
                        if (error) {
                            try {
                                transactionalCommands.discard();
                            } catch (Exception x) {
                                LoggerHolder.LOG.warn("Failed to discard transaction", x);
                            }
                        }
                    }
                } else {
                    transactionalCommands.unwatch();
                }
                return Boolean.FALSE;
            } catch (RuntimeException e) {
                throw OXException.general("Redis error", e);
            } catch (OXException e) {
                throw e;
            } catch (Exception e) {
                throw OXException.general("Failed to de-/serialize value", e);
            } finally {
                Streams.close(is);
            }
        }).booleanValue();
    }

    @Override
    public V putIfAbsent(String key, V value, long expireTimeMillis) throws OXException {
        if (Strings.isEmpty(key) || value == null) {
            throw new IllegalArgumentException("Neither key nor value may be null or empty");
        }

        return connector.executeOperation(commandsProvider -> {
            InputStream data = null;
            InputStream serializedValue = null;
            try {
                serializedValue = getCodec().serializeValue(value);
                if (expireTimeMillis > 0) {
                    data = commandsProvider.getRawStringCommands().setGet(getFullKey(key), serializedValue, new SetArgs().nx().px(expireTimeMillis));
                } else {
                    data = commandsProvider.getRawStringCommands().setGet(getFullKey(key), serializedValue, new SetArgs().nx());
                }
                return data == null ? null : getCodec().deserializeValue(data);
            } catch (RuntimeException e) {
                throw OXException.general("Redis error", e);
            } catch (OXException e) {
                throw e;
            } catch (Exception e) {
                throw OXException.general("Failed to de-/serialize value", e);
            } finally {
                Streams.close(serializedValue, data);
            }
        });
    }

}
