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

package com.openexchange.cluster.map.codec.standard;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;

/**
 * {@link DefaultMapCodec} - The default map codec using Jackson object-mapper that provides functionality for
 * reading and writing JSON, either to and from basic POJOs (Plain Old Java Objects).
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 * @param <V> The type of serialized/deserialized Java object 
 */
public class DefaultMapCodec<V> implements MapCodec<V> {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultMapCodec.class);
    }

    private static final ConcurrentMap<Class<?>, DefaultMapCodec<?>> CACHE = new ConcurrentHashMap<Class<?>, DefaultMapCodec<?>>(16, 0.9F, 1);

    /**
     * Gets the codec instance for specified type.
     *
     * @param type The type this codec should serialize to/deserialize from
     * @return The codec instance
     */
    @SuppressWarnings("unchecked")
    public static <V> DefaultMapCodec<V> codecFor(Class<V> type) {
        DefaultMapCodec<?> codec = CACHE.get(type);
        if (codec == null) {
            DefaultMapCodec<V> newCodec = new DefaultMapCodec<>(type);
            codec = CACHE.putIfAbsent(type, newCodec);
            if (codec == null) {
                codec = newCodec;
            }
        }
        return (DefaultMapCodec<V>) codec;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Class<V> type;

    /**
     * Initializes a new {@link DefaultMapCodec}.
     *
     * @param type The type this codec should serialize to/deserialize from
     */
    private DefaultMapCodec(Class<V> type) {
        super();
        this.type = type;
    }

    @Override
    public InputStream serializeValue(V message) throws Exception {
        return value2jsonStream(message);
    }

    @Override
    public V deserializeValue(InputStream data) throws Exception {
        return jsonStream2value(data, type);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        /*-
        {
            JsonSerializer<V> serializer = optionalSerializer.get();
            SimpleModule module = new SimpleModule(serializer.getClass().getSimpleName(), new Version(2, 1, 3, null, null, null));
            module.addSerializer(type, serializer);
            objectMapper.registerModule(module);
        }*/
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER = objectMapper;
    }

    /**
     * Serializes any Java value as JSON stream.
     *
     * @param <O> The type of the Java object
     * @param object The object to serialize
     * @return The JSON stream or <code>null</code> if given object was <code>null</code>, too
     * @throws OXException If serialization fails
     */
    public static <O> InputStream value2jsonStream(O object) throws OXException {
        if (object == null) {
            return null;
        }
        try {
            return Streams.newByteArrayInputStream(OBJECT_MAPPER.writeValueAsBytes(object));
        } catch (Exception e) {
            throw OXException.general("Serialisation failed", e);
        }
    }

    /**
     * De-serializes given JSON input to any Java object (using UTF-8 encoding).
     *
     * @param <O> The type of the Java object
     * @param data The JSON stream to read from
     * @param type The class of the Java object
     * @return The Java object or <code>null</code> if given data was <code>null</code>, too
     * @throws OXException If de-serialization fails
     * @throws IllegalArgumentException If given type is <code>null</code>
     */
    public static <O> O jsonStream2value(InputStream data, Class<O> type) throws OXException {
        if (data == null) {
            return null;
        }
        if (type == null) {
            throw new IllegalArgumentException("Type must not be null");
        }
        try {
            return OBJECT_MAPPER.readValue(data, type);
        } catch (JsonMappingException e) {
            LoggerHolder.LOG.warn("Failed to deserialze JSON string to type {}", type.getName(), e);
            return null;
        } catch (Exception e) {
            throw OXException.general("Deserialisation failed", e);
        }
    }

}
