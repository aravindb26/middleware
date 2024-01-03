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

package com.openexchange.cluster.map.codec;

import java.io.InputStream;
import com.openexchange.java.Streams;

/**
 * {@link MapCodec} - The map codec that is responsible for serializing and deserializing of values managed in a cluster map.
 * <p>
 * It cares about converting a Java object to its byte stream for being put into a cluster map and vice versa.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 * @param <V> The type of serialized/deserialized Java object
 */
public interface MapCodec<V> {

    /**
     * Serializes the specified value to data stream.
     *
     * @param value The value to encode
     * @return The data
     * @throws Exception If serialization fails
     */
    InputStream serializeValue(V value) throws Exception;

    /**
     * Serializes the specified value to bytes.
     *
     * @param value The value to encode
     * @return The bytes
     * @throws Exception If serialization fails
     */
    default byte[] serializeValue2Bytes(V value) throws Exception {
        return Streams.stream2bytes(serializeValue(value));
    }

    /**
     * Deserializes the given data stream to the value.
     *
     * @param data The raw data of any Java object representation
     * @return The value or <code>null</code> if data cannot be deserialized into codec's type
     * @throws Exception If deserialization fails
     */
    V deserializeValue(InputStream data) throws Exception;

    /**
     * Deserializes the given bytes to the value.
     *
     * @param data The raw bytes of any Java object representation
     * @return The value or <code>null</code> if bytes cannot be deserialized into codec's type
     * @throws Exception If deserialization fails
     */
    default V deserializeValueFromBytes(byte[] data) throws Exception {
        return deserializeValue(Streams.newByteArrayInputStream(data));
    }

}
