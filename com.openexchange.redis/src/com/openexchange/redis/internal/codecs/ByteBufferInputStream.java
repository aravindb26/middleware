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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * {@link ByteBufferInputStream} - An input stream reading from a given byte buffer.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 *
 */
public class ByteBufferInputStream extends InputStream {

    private final ByteBuffer bb;

    /**
     * Initializes a new {@link ByteBufferInputStream}.
     *
     * @param bb The {@link ByteBuffer} to use
     */
    public ByteBufferInputStream(ByteBuffer bb) {
        super();
        this.bb = bb;
    }

    @Override
    public int available() {
        return bb.remaining();
    }

    @Override
    public int read() {
        return bb.hasRemaining() ? bb.get() & 0xFF : -1;
    }

    @Override
    public int read(byte[] bytes, int off, int len) {
        Objects.checkFromIndexSize(off, len, bytes.length);
        if (len == 0) {
            return 0;
        }
        int remaining = bb.remaining();
        if (remaining <= 0) {
            return -1;
        }

        bb.mark(); // Mark to possibly read bytes individually
        try {
            int length = (len <= remaining) ? len : remaining;
            bb.get(bytes, off, length);
            return length;
        } catch (IllegalArgumentException e) {
            bb.reset();
            return readOneByOne(bytes, off, len);
        }
    }

    private int readOneByOne(byte[] b, int off, int len) {
        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte)c;

        int i = 1;
        for (; i < len ; i++) {
            c = read();
            if (c == -1) {
                break;
            }
            b[off + i] = (byte)c;
        }
        return i;
    }

}
