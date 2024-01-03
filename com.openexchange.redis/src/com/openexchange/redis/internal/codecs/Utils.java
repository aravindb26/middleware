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

import java.nio.ByteBuffer;

/**
 * {@link Utils} - Utilities for codecs.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
class Utils {

    /**
     * Initializes a new {@link Utils}.
     */
    private Utils() {
        super();
    }

    /**
     * Gets the content of the given buffer as a byte array.
     *
     * @param buf The buffer
     * @return The resulting byte array
     */
    static byte[] asByteArray(ByteBuffer buf) {
        if (buf.hasArray() && buf.arrayOffset() == 0 && buf.capacity() == buf.remaining()) {
            return buf.array();
        }

        byte[] result = new byte[buf.remaining()];
        if (buf.hasArray()) {
            System.arraycopy(buf.array(), buf.arrayOffset() + buf.position(), result, 0, result.length);
        } else {
            // Direct buffer
            ByteBuffer duplicate = buf.duplicate();
            duplicate.mark();
            duplicate.get(result);
            duplicate.reset();
        }
        return result;
    }

}
