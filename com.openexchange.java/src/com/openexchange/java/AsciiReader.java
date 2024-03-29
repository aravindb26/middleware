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

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.openexchange.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * A simple ASCII byte reader.
 * <p>
 * This is an optimized reader for reading byte streams that only contain 7-bit ASCII characters.
 *
 * @author Andy Clark, IBM
 */
public class AsciiReader extends Reader {

    //
    // Constants
    //

    /** Default byte buffer size (2048). */
    public static final int DEFAULT_BUFFER_SIZE = 2048;

    //
    // Data
    //

    /** Input stream. */
    private final InputStream fInputStream;

    /** Byte buffer. */
    private final byte[] fBuffer;

    /** Strict ASCII check */
    private boolean errorOnAsciiFault;

    //
    // Constructors
    //

    /**
     * Constructs an ASCII reader from the specified input stream using the default buffer size.
     *
     * @param inputStream The input stream.
     * @param messageFormatter the MessageFormatter to use to message reporting.
     * @param locale the Locale for which messages are to be reported
     */
    public AsciiReader(final InputStream inputStream) {
        this(inputStream, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Constructs an ASCII reader from the specified input stream and buffer size.
     *
     * @param inputStream The input stream.
     * @param size The initial buffer size.
     * @param messageFormatter the MessageFormatter to use to message reporting.
     * @param locale the Locale for which messages are to be reported
     */
    public AsciiReader(final InputStream inputStream, final int size) {
        this(inputStream, new byte[size]);
    }

    /**
     * Constructs an ASCII reader from the specified input stream and buffer.
     *
     * @param inputStream The input stream.
     * @param buffer The byte buffer.
     * @param messageFormatter the MessageFormatter to use to message reporting.
     * @param locale the Locale for which messages are to be reported
     */
    public AsciiReader(final InputStream inputStream, final byte[] buffer) {
        fInputStream = inputStream;
        fBuffer = buffer;
        errorOnAsciiFault = false;
    }

    /**
     * Sets the error-on-ASCII-fault flag.
     *
     * @param errorOnAsciiFault The error-on-ASCII-fault flag
     * @return This reader with new behavior applied.
     */
    public AsciiReader setErrorOnAsciiFault(final boolean errorOnAsciiFault) {
        this.errorOnAsciiFault = errorOnAsciiFault;
        return this;
    }

    //
    // Reader methods
    //

    /**
     * Read a single character. This method will block until a character is available, an I/O error occurs, or the end of the stream is
     * reached.
     * <p>
     * Subclasses that intend to support efficient single-character input should override this method.
     *
     * @return The character read, as an integer in the range 0 to 127 (<tt>0x00-0x7f</tt>), or -1 if the end of the stream has been reached
     * @exception IOException If an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        final int b0 = fInputStream.read();
        if (errorOnAsciiFault && b0 >= 0x80) {
            throw new IOException("Invalid ASCII: " + Integer.toString(b0));
        }
        return b0;
    } // read():int

    /**
     * Read characters into a portion of an array. This method will block until some input is available, an I/O error occurs, or the end of
     * the stream is reached.
     *
     * @param ch Destination buffer
     * @param offset Offset at which to start storing characters
     * @param length Maximum number of characters to read
     * @return The number of characters read, or -1 if the end of the stream has been reached
     * @exception IOException If an I/O error occurs
     */
    @Override
    public int read(final char[] ch, final int offset, int length) throws IOException {
        int len = length;
        if (len > fBuffer.length) {
            len = fBuffer.length;
        }
        final int count = fInputStream.read(fBuffer, 0, len);
        for (int i = count; i-- > 0;) {
            final int b0 = fBuffer[i];
            if (errorOnAsciiFault && b0 < 0) {
                throw new IOException("Invalid ASCII: " + Integer.toString(b0 & 0x0FF));
            }
            ch[offset + i] = (char) b0;
        }
        return count;
    } // read(char[],int,int)

    /**
     * Skip characters. This method will block until some characters are available, an I/O error occurs, or the end of the stream is
     * reached.
     *
     * @param n The number of characters to skip
     * @return The number of characters actually skipped
     * @exception IOException If an I/O error occurs
     */
    @Override
    public long skip(final long n) throws IOException {
        return fInputStream.skip(n);
    } // skip(long):long

    /**
     * Tell whether this stream is ready to be read.
     *
     * @return True if the next read() is guaranteed not to block for input, false otherwise. Note that returning false does not guarantee
     *         that the next read will block.
     * @exception IOException If an I/O error occurs
     */
    @Override
    public boolean ready() throws IOException {
        return false;
    } // ready()

    /**
     * Tell whether this stream supports the mark() operation.
     */
    @Override
    public boolean markSupported() {
        return fInputStream.markSupported();
    } // markSupported()

    /**
     * Mark the present position in the stream. Subsequent calls to reset() will attempt to reposition the stream to this point. Not all
     * character-input streams support the mark() operation.
     *
     * @param readAheadLimit Limit on the number of characters that may be read while still preserving the mark. After reading this many
     *            characters, attempting to reset the stream may fail.
     * @exception IOException If the stream does not support mark(), or if some other I/O error occurs
     */
    @Override
    public void mark(final int readAheadLimit) throws IOException {
        fInputStream.mark(readAheadLimit);
    } // mark(int)

    /**
     * Reset the stream. If the stream has been marked, then attempt to reposition it at the mark. If the stream has not been marked, then
     * attempt to reset it in some way appropriate to the particular stream, for example by repositioning it to its starting point. Not all
     * character-input streams support the reset() operation, and some support reset() without supporting mark().
     *
     * @exception IOException If the stream has not been marked, or if the mark has been invalidated, or if the stream does not support
     *                reset(), or if some other I/O error occurs
     */
    @Override
    public void reset() throws IOException {
        fInputStream.reset();
    } // reset()

    /**
     * Close the stream. Once a stream has been closed, further read(), ready(), mark(), or reset() invocations will throw an IOException.
     * Closing a previously-closed stream, however, has no effect.
     *
     * @exception IOException If an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        fInputStream.close();
    } // close()

}
