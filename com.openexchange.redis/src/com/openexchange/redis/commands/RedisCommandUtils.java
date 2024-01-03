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

import java.io.IOException;
import java.io.InputStream;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;

/**
 * {@link RedisCommandUtils}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisCommandUtils {

    /**
     * Initializes a new {@link RedisCommandUtils}.
     */
    private RedisCommandUtils() {
        super();
    }

    /**
     * Converts given stream to ASCII string.
     *
     * @param stream The stream
     * @param capacity The initial capacity of the buffer
     * @return The ASCII string or <code>null</code> if stream is <code>null</code> or empty
     */
    public static String stream2String(InputStream stream, int capacity) {
        if (stream == null) {
            return null;
        }

        try {
            String str = Charsets.toAsciiString(stream, capacity);
            return str.length() > 0 ? str : null;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to convert stream to string", e);
        } finally {
            Streams.close(stream);
        }
    }

    /**
     * Converts given ASCII string to a (byte array) stream
     *
     * @param str The ASCII string
     * @return The (byte array) stream
     */
    public static InputStream string2Stream(String str) {
        if (str == null) {
            return null;
        }

        return Charsets.toAsciiStream(str);
    }

}
