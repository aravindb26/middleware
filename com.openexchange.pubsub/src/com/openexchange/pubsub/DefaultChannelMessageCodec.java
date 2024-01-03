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

package com.openexchange.pubsub;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link DefaultChannelMessageCodec} - The default channel message codec using Jackson object-mapper that provides functionality for
 * reading and writing JSON, either to and from basic POJOs (Plain Old Java Objects).
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class DefaultChannelMessageCodec<V> implements ChannelMessageCodec<V> {

    private static final ConcurrentMap<Class<?>, DefaultChannelMessageCodec<?>> CACHE = new ConcurrentHashMap<Class<?>, DefaultChannelMessageCodec<?>>(16, 0.9F, 1);

    /**
     * Gets the codec instance for specified type.
     *
     * @param type The type this codec should serialize to/deserialize from
     * @return The codec instance
     */
    @SuppressWarnings("unchecked")
    public static <V> DefaultChannelMessageCodec<V> codecFor(Class<V> type) {
        DefaultChannelMessageCodec<?> codec = CACHE.get(type);
        if (codec == null) {
            DefaultChannelMessageCodec<V> newCodec = new DefaultChannelMessageCodec<>(type);
            codec = CACHE.putIfAbsent(type, newCodec);
            if (codec == null) {
                codec = newCodec;
            }
        }
        return (DefaultChannelMessageCodec<V>) codec;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Class<V> type;

    /**
     * Initializes a new {@link DefaultChannelMessageCodec}.
     *
     * @param type The type this codec should serialize to/deserialize from
     */
    private DefaultChannelMessageCodec(Class<V> type) {
        super();
        this.type = type;
    }

    @Override
    public String serialize(V message) throws Exception {
        return Codecs.value2jsonString(message);
    }

    @Override
    public V deserialize(String data) throws Exception {
        return Codecs.jsonString2value(data, type);
    }

}
