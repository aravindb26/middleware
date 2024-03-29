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

package com.openexchange.java;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * {@link LineLimitedBufferedReader}
 *
 * @author Thorben Betten
 */
public class LineLimitedBufferedReader extends Reader {

    private Reader in;

    private char[] cb;
    private int nChars;
    private int nextChar;

    private static final int INVALIDATED = -2;
    private static final int UNMARKED = -1;
    private int markedChar = UNMARKED;
    private int readAheadLimit = 0; /* Valid only when markedChar > 0 */

    /** If the next character is a line feed, skip it */
    private boolean skipLF = false;

    /** The skipLF flag when the mark was set */
    private boolean markedSkipLF = false;

    private static int defaultCharBufferSize = 8192;
    private static int defaultExpectedLineLength = 80;

    /**
     * Creates a buffering character-input stream that uses an input buffer of
     * the specified size.
     *
     * @param  in   A Reader
     * @param  sz   Input-buffer size
     *
     * @throws IllegalArgumentException  If {@code sz <= 0}
     */
    public LineLimitedBufferedReader(Reader in, int sz) {
        super(in);
        if (sz <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.in = in;
        cb = new char[sz];
        nextChar = nChars = 0;
    }

    /**
     * Creates a buffering character-input stream that uses a default-sized
     * input buffer.
     *
     * @param  in   A Reader
     */
    public LineLimitedBufferedReader(Reader in) {
        this(in, defaultCharBufferSize);
    }

    /** Checks to make sure that the stream has not been closed */
    private void ensureOpen() throws IOException {
        if (in == null) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * Fills the input buffer, taking the mark into account if it is valid.
     */
    private void fill() throws IOException {
        int dst;
        if (markedChar <= UNMARKED) {
            /* No mark */
            dst = 0;
        } else {
            /* Marked */
            int delta = nextChar - markedChar;
            if (delta >= readAheadLimit) {
                /* Gone past read-ahead limit: Invalidate mark */
                markedChar = INVALIDATED;
                readAheadLimit = 0;
                dst = 0;
            } else {
                if (readAheadLimit <= cb.length) {
                    /* Shuffle in the current buffer */
                    System.arraycopy(cb, markedChar, cb, 0, delta);
                    markedChar = 0;
                    dst = delta;
                } else {
                    /* Reallocate buffer to accommodate read-ahead limit */
                    char[] ncb = new char[readAheadLimit];
                    System.arraycopy(cb, markedChar, ncb, 0, delta);
                    cb = ncb;
                    markedChar = 0;
                    dst = delta;
                }
                nextChar = nChars = delta;
            }
        }

        int n;
        do {
            n = in.read(cb, dst, cb.length - dst);
        } while (n == 0);
        if (n > 0) {
            nChars = dst + n;
            nextChar = dst;
        }
    }

    /**
     * Reads a single character.
     *
     * @return The character read, as an integer in the range
     *         0 to 65535 ({@code 0x00-0xffff}), or -1 if the
     *         end of the stream has been reached
     * @throws     IOException  If an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        synchronized (lock) {
            ensureOpen();
            for (;;) {
                if (nextChar >= nChars) {
                    fill();
                    if (nextChar >= nChars) {
                        return -1;
                    }
                }
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                        continue;
                    }
                }
                return cb[nextChar++];
            }
        }
    }

    /**
     * Reads characters into a portion of an array, reading from the underlying
     * stream if necessary.
     */
    private int read1(char[] cbuf, int off, int len) throws IOException {
        if (nextChar >= nChars) {
            /* If the requested length is at least as large as the buffer, and
               if there is no mark/reset activity, and if line feeds are not
               being skipped, do not bother to copy the characters into the
               local buffer.  In this way buffered streams will cascade
               harmlessly. */
            if (len >= cb.length && markedChar <= UNMARKED && !skipLF) {
                return in.read(cbuf, off, len);
            }
            fill();
        }
        if (nextChar >= nChars) {
            return -1;
        }
        if (skipLF) {
            skipLF = false;
            if (cb[nextChar] == '\n') {
                nextChar++;
                if (nextChar >= nChars) {
                    fill();
                }
                if (nextChar >= nChars) {
                    return -1;
                }
            }
        }
        int n = Math.min(len, nChars - nextChar);
        System.arraycopy(cb, nextChar, cbuf, off, n);
        nextChar += n;
        return n;
    }

    /**
     * Reads characters into a portion of an array.
     *
     * <p> This method implements the general contract of the corresponding
     * {@link Reader#read(char[], int, int) read} method of the
     * {@link Reader} class.  As an additional convenience, it
     * attempts to read as many characters as possible by repeatedly invoking
     * the {@code read} method of the underlying stream.  This iterated
     * {@code read} continues until one of the following conditions becomes
     * true:
     * <ul>
     *
     *   <li> The specified number of characters have been read,
     *
     *   <li> The {@code read} method of the underlying stream returns
     *   {@code -1}, indicating end-of-file, or
     *
     *   <li> The {@code ready} method of the underlying stream
     *   returns {@code false}, indicating that further input requests
     *   would block.
     *
     * </ul>
     * If the first {@code read} on the underlying stream returns
     * {@code -1} to indicate end-of-file then this method returns
     * {@code -1}.  Otherwise this method returns the number of characters
     * actually read.
     *
     * <p> Subclasses of this class are encouraged, but not required, to
     * attempt to read as many characters as possible in the same fashion.
     *
     * <p> Ordinarily this method takes characters from this stream's character
     * buffer, filling it from the underlying stream as necessary.  If,
     * however, the buffer is empty, the mark is not valid, and the requested
     * length is at least as large as the buffer, then this method will read
     * characters directly from the underlying stream into the given array.
     * Thus redundant {@code BufferedReader}s will not copy data
     * unnecessarily.
     *
     * @param      cbuf  {@inheritDoc}
     * @param      off   {@inheritDoc}
     * @param      len   {@inheritDoc}
     *
     * @return     {@inheritDoc}
     *
     * @throws     IndexOutOfBoundsException {@inheritDoc}
     * @throws     IOException  {@inheritDoc}
     */
    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        synchronized (lock) {
            ensureOpen();
            Objects.checkFromIndexSize(off, len, cbuf.length);
            if (len == 0) {
                return 0;
            }

            int n = read1(cbuf, off, len);
            if (n <= 0) {
                return n;
            }
            while ((n < len) && in.ready()) {
                int n1 = read1(cbuf, off + n, len - n);
                if (n1 <= 0) {
                    break;
                }
                n += n1;
            }
            return n;
        }
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     *
     * @param      ignoreLF  If true, the next '\n' will be skipped
     * @param      term      Output: Whether a line terminator was encountered
     *                       while reading the line; may be {@code null}.
     * @param  maxLineLength The max. allowed length for a line
     *
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached without reading any characters
     *
     * @see        java.io.LineNumberReader#readLine()
     *
     * @throws     IOException  If an I/O error occurs
     */
    String readLine(boolean ignoreLF, boolean[] term, int maxLineLength) throws IOException {
        Appender s = null;
        int startChar;

        try {
            synchronized (lock) {
                ensureOpen();
                boolean omitLF = ignoreLF || skipLF;
                if (term != null) {
                    term[0] = false;
                }

                bufferLoop: for (;;) {

                    if (nextChar >= nChars) {
                        fill();
                    }
                    if (nextChar >= nChars) { /* EOF */
                        return s != null && s.length() > 0 ? s.toString() : null;
                    }
                    boolean eol = false;
                    char c = 0;
                    int i;

                    /* Skip a leftover '\n', if necessary */
                    if (omitLF && (cb[nextChar] == '\n')) {
                        nextChar++;
                    }
                    skipLF = false;
                    omitLF = false;

                    charLoop: for (i = nextChar; i < nChars; i++) {
                        c = cb[i];
                        if ((c == '\n') || (c == '\r')) {
                            if (term != null) {
                                term[0] = true;
                            }
                            eol = true;
                            break charLoop;
                        }
                    }

                    startChar = nextChar;
                    nextChar = i;

                    if (eol) {
                        String str;
                        if (s == null) {
                            str = new String(cb, startChar, i - startChar);
                        } else {
                            s.append(cb, startChar, i - startChar);
                            str = s.toString();
                        }
                        nextChar++;
                        if (c == '\r') {
                            skipLF = true;
                        }
                        return str;
                    }

                    if (s == null) {
                        s = maxLineLength < 0 ? new StringBuilderAppender(new StringBuilder(defaultExpectedLineLength)) : new LimitedStringBuilderAppender(new LimitedStringBuilder(defaultExpectedLineLength, maxLineLength));
                    }
                    s.append(cb, startChar, i - startChar);
                }
            }
        } catch (LimitExceededException e) {
            throw new LimitExceededIOException("Response line exceeds max. allowed length of " + maxLineLength + " characters", e);
        }
    }

    /**
     * Reads a line of text.  A line is considered to be terminated by any one
     * of a line feed ('\n'), a carriage return ('\r'), a carriage return
     * followed immediately by a line feed, or by reaching the end-of-file
     * (EOF).
     *
     * @param      maxLineLength The optional max. allowed length for a line or <code>-1</code> for unlimited
     * @return     A String containing the contents of the line, not including
     *             any line-termination characters, or null if the end of the
     *             stream has been reached without reading any characters
     *
     * @throws     IOException  If an I/O error occurs
     * @throws     LimitExceededIOException  If specified max. line length is exceeded
     *
     * @see java.nio.file.Files#readAllLines
     */
    public String readLine(int maxLineLength) throws IOException, LimitExceededIOException {
        return readLine(false, null, maxLineLength);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(long n) throws IOException {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }
        synchronized (lock) {
            ensureOpen();
            long r = n;
            while (r > 0) {
                if (nextChar >= nChars) {
                    fill();
                }
                if (nextChar >= nChars) { /* EOF */
                	break;
                }
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                    }
                }
                long d = nChars - nextChar;
                if (r <= d) {
                    nextChar += r;
                    r = 0;
                    break;
                }
                else {
                    r -= d;
                    nextChar = nChars;
                }
            }
            return n - r;
        }
    }

    /**
     * Tells whether this stream is ready to be read.  A buffered character
     * stream is ready if the buffer is not empty, or if the underlying
     * character stream is ready.
     *
     * @throws     IOException  If an I/O error occurs
     */
    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            ensureOpen();

            /*
             * If newline needs to be skipped and the next char to be read
             * is a newline character, then just skip it right away.
             */
            if (skipLF) {
                /* Note that in.ready() will return true if and only if the next
                 * read on the stream will not block.
                 */
                if (nextChar >= nChars && in.ready()) {
                    fill();
                }
                if (nextChar < nChars) {
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                    }
                    skipLF = false;
                }
            }
            return (nextChar < nChars) || in.ready();
        }
    }

    /**
     * Tells whether this stream supports the mark() operation, which it does.
     */
    @Override
    public boolean markSupported() {
        return true;
    }

    /**
     * Marks the present position in the stream.  Subsequent calls to reset()
     * will attempt to reposition the stream to this point.
     *
     * @param readAheadLimit   Limit on the number of characters that may be
     *                         read while still preserving the mark. An attempt
     *                         to reset the stream after reading characters
     *                         up to this limit or beyond may fail.
     *                         A limit value larger than the size of the input
     *                         buffer will cause a new buffer to be allocated
     *                         whose size is no smaller than limit.
     *                         Therefore large values should be used with care.
     *
     * @throws     IllegalArgumentException  If {@code readAheadLimit < 0}
     * @throws     IOException  If an I/O error occurs
     */
    @Override
    public void mark(int readAheadLimit) throws IOException {
        if (readAheadLimit < 0) {
            throw new IllegalArgumentException("Read-ahead limit < 0");
        }
        synchronized (lock) {
            ensureOpen();
            this.readAheadLimit = readAheadLimit;
            markedChar = nextChar;
            markedSkipLF = skipLF;
        }
    }

    /**
     * Resets the stream to the most recent mark.
     *
     * @throws     IOException  If the stream has never been marked,
     *                          or if the mark has been invalidated
     */
    @Override
    public void reset() throws IOException {
        synchronized (lock) {
            ensureOpen();
            if (markedChar < 0) {
                throw new IOException((markedChar == INVALIDATED)
                                      ? "Mark invalid"
                                      : "Stream not marked");
            }
            nextChar = markedChar;
            skipLF = markedSkipLF;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            if (in == null) {
                return;
            }
            try {
                in.close();
            } finally {
                in = null;
                cb = null;
            }
        }
    }

    /**
     * Returns a {@code Stream}, the elements of which are lines read from
     * this {@code BufferedReader}.  The {@link Stream} is lazily populated,
     * i.e., read only occurs during the
     * <a href="../util/stream/package-summary.html#StreamOps">terminal
     * stream operation</a>.
     *
     * <p> The reader must not be operated on during the execution of the
     * terminal stream operation. Otherwise, the result of the terminal stream
     * operation is undefined.
     *
     * <p> After execution of the terminal stream operation there are no
     * guarantees that the reader will be at a specific position from which to
     * read the next character or line.
     *
     * <p> If an {@link IOException} is thrown when accessing the underlying
     * {@code BufferedReader}, it is wrapped in an {@link
     * UncheckedIOException} which will be thrown from the {@code Stream}
     * method that caused the read to take place. This method will return a
     * Stream if invoked on a BufferedReader that is closed. Any operation on
     * that stream that requires reading from the BufferedReader after it is
     * closed, will cause an UncheckedIOException to be thrown.
     *
     * @return a {@code Stream<String>} providing the lines of text
     *         described by this {@code BufferedReader}
     *
     * @since 1.8
     */
    public Stream<String> lines() {
        Iterator<String> iter = new Iterator<>() {
            String nextLine = null;

            @Override
            public boolean hasNext() {
                if (nextLine != null) {
                    return true;
                } else {
                    try {
                        nextLine = readLine(-1);
                        return (nextLine != null);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

            @Override
            public String next() {
                if (nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                iter, Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static interface Appender {

        /**
         * Appends the string representation of a subarray of the
         * {@code char} array argument to this sequence.
         *
         * @param   str      the characters to be appended.
         * @param   offset   the index of the first {@code char} to append.
         * @param   len      the number of {@code char}s to append.
         * @throws IndexOutOfBoundsException
         *         if {@code offset < 0} or {@code len < 0}
         *         or {@code offset+len > str.length}
         */
        void append(char[] str, int offset, int len);

        /**
         * Returns the length of this character sequence.  The length is the number
         * of 16-bit {@code char}s in the sequence.
         *
         * @return  the number of {@code char}s in this sequence
         */
        int length();

        /**
         * Returns a string containing the characters in this sequence in the same
         * order as this sequence. The length of the string will be the length of
         * this sequence.
         *
         * @return  a string consisting of exactly this sequence of characters
         */
        @Override
        String toString();
    }

    private static class StringBuilderAppender implements Appender {

        private final StringBuilder s;

        StringBuilderAppender(StringBuilder s) {
            super();
            this.s = s;
        }

        @Override
        public void append(char[] str, int offset, int len) {
            s.append(str, offset, len);
        }

        @Override
        public int length() {
            return s.length();
        }
        
        @Override
        public String toString() {
            return s.toString();
        }
    }

    private static class LimitedStringBuilderAppender implements Appender {

        private final LimitedStringBuilder s;

        LimitedStringBuilderAppender(LimitedStringBuilder s) {
            super();
            this.s = s;
        }

        @Override
        public void append(char[] str, int offset, int len) {
            s.append(str, offset, len);
        }

        @Override
        public int length() {
            return s.length();
        }
        
        @Override
        public String toString() {
            return s.toString();
        }
    }

}
