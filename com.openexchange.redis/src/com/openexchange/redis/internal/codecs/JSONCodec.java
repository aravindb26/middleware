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
import java.nio.ByteBuffer;
import org.json.JSONObject;
import org.json.JSONServices;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;

/**
 * {@link JSONCodec} - The JSON codec for Redis channels.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class JSONCodec implements RedisCodec<String, JSONObject> {

    private static final JSONCodec INSTANCE = new JSONCodec();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static JSONCodec getInstance() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final StringCodec stringCodec;

    /**
     * Initializes a new {@link JSONCodec}.
     */
    private JSONCodec() {
        super();
        stringCodec = StringCodec.UTF8;
    }

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return stringCodec.decodeKey(bytes);
    }

    @Override
    public JSONObject decodeValue(ByteBuffer bytes) {
        try {
            return JSONServices.parseObject(asByteArray(bytes));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception x) {
            throw new IllegalStateException(x);
        }
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return stringCodec.encodeKey(key);
    }

    @Override
    public ByteBuffer encodeValue(JSONObject value) {
        try {
            return ByteBuffer.wrap(value.toByteArray(), 1024, 256);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception x) {
            throw new IllegalStateException(x);
        }
    }

}
