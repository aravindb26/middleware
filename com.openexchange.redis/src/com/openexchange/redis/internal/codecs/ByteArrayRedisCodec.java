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

package com.openexchange.redis.internal.codecs;

import static com.openexchange.redis.internal.codecs.Utils.asByteArray;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import io.lettuce.core.codec.RedisCodec;

/**
 * {@link ByteArrayRedisCodec} - The Redis codec accepting raw bytes of any Java object's string representation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class ByteArrayRedisCodec implements RedisCodec<String, InputStream> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ByteArrayRedisCodec.class);

    private static final ByteArrayRedisCodec INSTANCE = new ByteArrayRedisCodec();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static ByteArrayRedisCodec getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final Charset CHARSET = Charsets.UTF_8;
    private final RedisSerializationMetricsCollector metricsCollector;

    /**
     * Initializes a new {@link ByteArrayRedisCodec}.
     */
    private ByteArrayRedisCodec() {
        super();
        metricsCollector = new RedisSerializationMetricsCollector();
    }

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return CHARSET.decode(bytes).toString();
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return CHARSET.encode(key);
    }

    @Override
    public InputStream decodeValue(ByteBuffer bytes) {
        int remaining = bytes.remaining();
        if (remaining <= 0) {
            return Streams.EMPTY_INPUT_STREAM;
        }

        boolean error = true; // pessimistic
        long now = System.nanoTime();
        try {
            byte[] ba = asByteArray(bytes);
            error = false;
            return Streams.newByteArrayInputStream(ba);
        } catch (Exception e) {
            LOGGER.error("An I/O error occurred while decoding value", e);
        } finally {
            if (!error) {
                metricsCollector.timeDeserialization(InputStream.class, System.nanoTime() - now);
            }
        }
        return Streams.EMPTY_INPUT_STREAM;
    }

    private static final byte[] EMPTY = new byte[0];

    @Override
    public ByteBuffer encodeValue(InputStream value) {
        boolean error = true; // pessimistic
        long now = System.nanoTime();
        try {
            ByteBuffer bytes = value == null ? ByteBuffer.wrap(EMPTY) : ByteBuffer.wrap(Streams.stream2bytes(value, 1024, 256));
            error = false;
            return bytes;
        } catch (Exception e) {
            LOGGER.error("An I/O error occurred while encoding value", e);
        } finally {
            if (!error) {
                metricsCollector.timeSerialization(InputStream.class, System.nanoTime() - now);
            }
        }
        return ByteBuffer.wrap(EMPTY);
    }

}
