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
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link InterruptibleInputStream} - Wraps an <code>InputStream</code> instance and makes it interruptable.
 * <p>
 * The reading process can be interrupted by calling {@link #interrupt} or {@link #interrupt(java.io.IOException)} which will throw an
 * exception on the next read attempt and close the decorated input stream.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class InterruptibleInputStream extends InputStream {

    /**
     * Gets the interruptible input stream for given <code>InputStream</code> instance.
     *
     * @param in The <code>InputStream</code> instance to wrap
     * @return The interruptible input stream
     */
    public static InterruptibleInputStream valueOf(InputStream in) {
        if (in == null) {
            return null;
        }

        return in instanceof InterruptibleInputStream iis ? iis : new InterruptibleInputStream(in);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /** The exception to be thrown. If <code>null</code> then the stream is not yet interrupted. */
    private final AtomicReference<IOException> interruptedRef;

    /** The decorated input stream. */
    private final InputStream in;

    /**
     * Initializes a new {@link InterruptibleInputStream}.
     *
     * @param in The delegate input stream
     */
    public InterruptibleInputStream(InputStream in) {
        this.in = in;
        interruptedRef = new AtomicReference<>();
    }

    /**
     * Gets the wrapped input stream.
     *
     * @return The wrapped input stream
     */
    public InputStream getInputStream() {
        return in;
    }

    @Override
    public int read() throws IOException {
        checkInterrupted();
        return in.read();
    }

    @Override
    public int available() throws IOException {
        checkInterrupted();
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void reset() throws IOException {
        checkInterrupted();
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public synchronized void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public long skip(long n) throws IOException {
        checkInterrupted();
        return in.skip(n);
    }

    @Override
    public int read(byte[] b) throws IOException {
        checkInterrupted();
        return in.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        checkInterrupted();
        return in.read(b, off, len);
    }

    /**
     * Checks if this instance has been marked as interrupted.
     *
     * @throws IOException The thrown I/O error in case marked as interrupted
     */
    private void checkInterrupted() throws IOException {
        IOException interrupted = this.interruptedRef.get();
        if (interrupted != null) {
            throw interrupted;
        }
    }

    /**
     * Signals if this input stream has already been interrupted.
     *
     * @return <code>true</code> if interrupted; otherwise <code>false</code>
     */
    public boolean isInterrupted() {
        return interruptedRef.get() != null;
    }

    /**
     * Interrupts this input stream through a newly created <code>java.io.InterruptedIOException</code> marker.
     */
    public void interrupt() {
        interrupt(new InterruptedIOException("Input stream marked as interrupted"));
    }

    /**
     * Interrupts this input stream using given <code>java.io.IOException</code> instance.
     *
     * @param exc The <code>java.io.IOException</code> instance that marks this input stream as interrupted
     * @throws IllegalStateException If this instance has already been interrupted
     */
    public void interrupt(IOException exc) {
        // check if not already interrupted
        if (null != this.interruptedRef.get()) {
            throw new IllegalStateException("Input stream already interrupted.");
        }

        synchronized (this) {
            // check if not already interrupted
            if (this.interruptedRef.get() != null) {
                throw new IllegalStateException("Input stream already interrupted.");
            }
            this.interruptedRef.set(exc);
            // close the decorated stream
            Streams.close(in);
        }
    }

}
