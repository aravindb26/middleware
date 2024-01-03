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

package com.openexchange.filestore.sproxyd.impl;

import java.io.FilterInputStream;
import java.io.IOException;
import java.util.UUID;
import com.openexchange.java.Streams;
import com.openexchange.rest.client.httpclient.HttpClients.HttpResponseStream;

/**
 * {@link AbstractResumableInputStream} - Resumes reading an Sproxyd object's content on premature EOF.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public abstract class AbstractResumableInputStream extends FilterInputStream {

    /** The client to access Sproxyd resources */
    protected final SproxydClient sproxydClient;

    /** The UUID of the desired object */
    protected final UUID id;

    private long mark;
    private boolean closed;

    /**
     * Initializes a new {@link AbstractResumableInputStream}.
     *
     * @param objectContent The input stream containing the contents of an object
     * @param url The UUID of the desired object
     * @param sproxydClient The client to access Sproxyd resources
     */
    protected AbstractResumableInputStream(HttpResponseStream objectContent, UUID id, SproxydClient sproxydClient) {
        super(objectContent);
        this.id = id;
        this.sproxydClient = sproxydClient;
        mark = -1;
    }

    /**
     * Gets the content length from the Sproxyd object.
     *
     * @return The length of the content
     * @throws IOException If content length cannot be returned
     */
    protected long getContentLength() {
        return ((HttpResponseStream) in).getContentLength();
    }

    /**
     * Notifies about specified number of currently consumed bytes.
     *
     * @param numberOfBytes The number of currently consumed bytes
     */
    protected abstract void onBytesRead(long numberOfBytes);

    /**
     * Initializes a new object stream after a premature EOF (<code>-1</code> was read) has been encountered.
     *
     * @throws IOException If initialization fails
     */
    protected abstract void initNewObjectStreamAfterPrematureEof() throws IOException;

    /**
     * Gets the position for the current mark.
     *
     * @return The current mark
     */
    protected abstract long getCurrentMark();

    /**
     * Resets to the given marked position.
     *
     * @param mark The position for the current mark
     */
    protected abstract void resetMark(long mark);

    /**
     * Handles given I/O exception that occurred while trying to read from S3 object's content stream.
     *
     * @param e The I/O exception to handle
     * @param errorOnPrematureEof Whether premature EOF should be handled through re-initializing S3 object's content stream
     * @throws IOException If an I/O exception should be advertised to caller
     */
    private void handleIOException(IOException e, boolean errorOnPrematureEof) throws IOException {
        if (errorOnPrematureEof || isNotPrematureEof(e)) {
            throw e;
        }

        // Initialize new object stream after premature EOF
        initNewObjectStreamAfterPrematureEof();
    }

    @Override
    public int read() throws IOException {
        return doRead(false);
    }

    private int doRead(boolean errorOnPrematureEof) throws IOException {
        try {
            int bite = in.read();
            if (bite >= 0) {
                onBytesRead(1);
            }
            return bite;
        } catch (IOException e) {
            handleIOException(e, errorOnPrematureEof);

            // Repeat with new S3ObjectInputStream instance
            return doRead(true);
        }
    }

    @Override
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        return doRead(b, off, len, false);
    }

    private int doRead(byte b[], int off, int len, boolean errorOnPrematureEof) throws IOException {
        try {
            int result = in.read(b, off, len);
            if (result >= 0) {
                onBytesRead(result);
            }
            return result;
        } catch (IOException e) {
            handleIOException(e, errorOnPrematureEof);

            // Repeat with new S3ObjectInputStream instance
            return doRead(b, off, len, true);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        long result = in.skip(n);
        onBytesRead(result);
        return result;
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
        mark = getCurrentMark();
    }

    @Override
    public void reset() throws IOException {
        if (!in.markSupported()) {
            throw new IOException("Mark not supported");
        }

        long mark = this.mark;
        if (mark == -1) {
            throw new IOException("Mark not set");
        }

        in.reset();
        resetMark(mark);
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            closed = true;
            Streams.close(in);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks if given I/O exception does <b>not</b> indicate premature EOF.
     *
     * @param e The I/O exception to examine
     * @return <code>true</code> if <b>no</b> premature EOF; otherwise <code>false</code>
     */
    protected static boolean isNotPrematureEof(IOException e) {
        return isPrematureEof(e) == false;
    }

    /**
     * Checks if given I/O exception indicates premature EOF.
     *
     * @param e The I/O exception to examine
     * @return <code>true</code> if premature EOF; otherwise <code>false</code>
     */
    protected static boolean isPrematureEof(IOException e) {
        if (e instanceof java.net.SocketException && "Connection reset".equals(e.getMessage())) {
            // The Sproxyd end-point has deliberately reset the connection
            return true;
        }

        if ("org.apache.http.ConnectionClosedException".equals(e.getClass().getName())) {
            // HTTP connection has been closed unexpectedly
            String message = e.getMessage();
            if (message != null && message.startsWith("Premature end of Content-Length delimited message body")) {
                /*-
                 * See org.apache.http.impl.io.ContentLengthInputStream.read(byte[], int, int)
                 *
                 * ...
                 * int readLen = this.in.read(b, off, chunk);
                 * if (readLen == -1 && pos < contentLength) {
                 *     throw new ConnectionClosedException(
                 *         "Premature end of Content-Length delimited message body (expected: %,d; received: %,d)",
                 *         contentLength, pos);
                 * }
                 * ...
                 *
                 * E.g. "Premature end of Content-Length delimited message body (expected: 52,428,800; received: 21,463,040)"
                 */
                return true;
            }
        }
        return false;
    }

}
