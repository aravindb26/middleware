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

package com.openexchange.streamcontrol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import com.openexchange.java.InterruptibleInputStream;
import com.openexchange.streamcontrol.internal.InputStreamControl;
import com.openexchange.streamcontrol.internal.InputStreamInfo;

/**
 * {@link ControlledInputStream} - An input stream that wraps a given stream to be able to control its usage.
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class ControlledInputStream extends InputStream {

    /**
     * Gets the controlled/watched input stream for specified input stream.
     * 
     * @param in The input stream to watch
     * @param timeoutDuration The timeout duration when reading from that stream shall be aborted
     * @return The controlled input stream
     */
    public static ControlledInputStream valueOf(InputStream in, Duration timeoutDuration) {
        long millis = timeoutDuration.toMillis();
        if (millis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid duration.");
        }
        return ControlledInputStream.valueOf(in, (int) millis);
    }

    /**
     * Gets the controlled/watched input stream for specified input stream.
     * 
     * @param in The input stream to watch
     * @param timeoutMillis The timeout in milliseconds when reading from that stream shall be aborted
     * @return The controlled input stream
     */
    public static ControlledInputStream valueOf(InputStream in, int timeoutMillis) {
        return in instanceof ControlledInputStream cis ? cis : new ControlledInputStream(in, timeoutMillis);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final InterruptibleInputStream interruptibleInputStream;
    private final int timeoutMillis;
    private InputStreamInfo control;

    /**
     * Initializes a new {@link ControlledInputStream}.
     *
     * @param in The input stream to control
     * @param timeoutMillis The timeout in milliseconds
     */
    private ControlledInputStream(InputStream in, int timeoutMillis) {
        this(InterruptibleInputStream.valueOf(in), timeoutMillis);
    }

    /**
     * Initializes a new {@link ControlledInputStream}.
     *
     * @param interruptibleInputStream The input stream to control
     * @param timeoutMillis The timeout in milliseconds
     */
    private ControlledInputStream(InterruptibleInputStream interruptibleInputStream, int timeoutMillis) {
        super();
        if (interruptibleInputStream == null) {
            throw new IllegalArgumentException("Input stream must not be null");
        }
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout must not be less than or equal to 0 (zero)");
        }
        this.interruptibleInputStream = interruptibleInputStream;
        this.timeoutMillis = timeoutMillis;
        this.control = null;
    }

    private void control() throws IOException {
        InputStreamInfo streamInfo = control;
        if (streamInfo == InputStreamInfo.POISON) {
            throw new IOException("Stream already closed");
        }
        if (streamInfo == null) {
            InputStreamInfo info = new InputStreamInfo(Thread.currentThread(), interruptibleInputStream, timeoutMillis);
            if (InputStreamControl.getInstance().add(info)) {
                control = info;
            }
        }
    }

    @Override
    public synchronized int read() throws IOException {
        control();
        return interruptibleInputStream.read();
    }

    @Override
    public synchronized int read(byte[] b) throws IOException {
        control();
        return interruptibleInputStream.read(b);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException {
        control();
        return interruptibleInputStream.read(b, off, len);
    }

    @Override
    public synchronized byte[] readAllBytes() throws IOException {
        control();
        return interruptibleInputStream.readAllBytes();
    }

    @Override
    public synchronized byte[] readNBytes(int len) throws IOException {
        control();
        return interruptibleInputStream.readNBytes(len);
    }

    @Override
    public synchronized int readNBytes(byte[] b, int off, int len) throws IOException {
        control();
        return interruptibleInputStream.readNBytes(b, off, len);
    }

    @Override
    public synchronized long skip(long n) throws IOException {
        control();
        return interruptibleInputStream.skip(n);
    }

    @Override
    public synchronized void skipNBytes(long n) throws IOException {
        control();
        interruptibleInputStream.skipNBytes(n);
    }

    @Override
    public synchronized long transferTo(OutputStream out) throws IOException {
        control();
        return interruptibleInputStream.transferTo(out);
    }

    @Override
    public synchronized void close() throws IOException {
        InputStreamInfo streamInfo = control;
        if (streamInfo != null) {
            control = InputStreamInfo.POISON;
            InputStreamControl.getInstance().remove(streamInfo);
        }
        interruptibleInputStream.close();
    }
}
