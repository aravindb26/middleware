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

package com.openexchange.mail.compose.mailstorage.util;

import gnu.trove.list.TByteList;
import gnu.trove.list.array.TByteArrayList;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * {@link TrackingInputStream} - An input stream that tracks read bytes.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public class TrackingInputStream extends FilterInputStream {

    private final TByteList bytes;

    /**
     * Initializes a new {@link TrackingInputStream}.
     *
     * @param in The wrapped stream
     */
    public TrackingInputStream(InputStream in) {
        super(in);
        bytes = new TByteArrayList(1024);
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        if (read >= 0) {
            bytes.add((byte) read);
        }
        return read;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        int read = super.read(b, off, len);
        if (read > 0) {
            this.bytes.add(b, off, len);
        }
        return read;
    }

    /**
     * Gets the bytes that were consumed so far from underlying input stream.
     *
     * @return The consumed bytes
     */
    public byte[] getReadBytes() {
        return bytes.toArray();
    }

}
