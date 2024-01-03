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


package com.openexchange.http.liveness;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SocketLivenessServer} - A simple socket-based HTTP liveness end-point.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class SocketLivenessServer {

    private static final Logger LOG = LoggerFactory.getLogger(SocketLivenessServer.class);

    private final String hostName;
    private final int port;
    private final AtomicBoolean stopped;

    private ServerSocket serverSocket;
    private LivenessThreadFactory threadFactory;
    private ExecutorService executors;

    /**
     * Initializes a new {@link SocketLivenessServer}.
     *
     * @param hostName The name of the host on which to bind the HTTP end-point
     * @param port The port on which to accept client liveness requests
     */
    public SocketLivenessServer(String hostName, int port) {
        super();
        this.hostName = hostName;
        this.port = port;
        this.stopped = new AtomicBoolean(false);
    }

    /**
     * Starts the HTTP Liveness end-point,
     *
     * @throws IOException If the start fails
     */
    public synchronized void start() throws IOException {
        LivenessThreadFactory threadFactory = null;
        ExecutorService executors = null;
        ServerSocket serverSocket = null;
        boolean error = true;
        try {
            serverSocket = new ServerSocket(port, 50, InetAddress.getByName(hostName));
            threadFactory = new LivenessThreadFactory();
            executors = Executors.newFixedThreadPool(1, threadFactory);
            executors.execute(this::accept);
            this.threadFactory = threadFactory;
            this.serverSocket = serverSocket;
            this.executors = executors;
            error = false;
        } finally {
            if (error) {
                stopped.set(true);
                close(serverSocket);
                if (executors != null) {
                    executors.shutdown();
                }
                if (threadFactory != null) {
                    threadFactory.shutDown();
                }
            }
        }
    }

    /**
     * Accepts a new connection
     */
    private void accept() {
        // Re-usable byte buffer
        byte[] lineBuffer = new byte[MAX_LINE_LENGTH];

        // Accept client sockets until stopped
        while (!stopped.get()) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
                handleClientSocket(clientSocket, lineBuffer);
            } catch (Exception e) {
                LOG.error("Failed to accept client socket", e);
            } finally {
                close(clientSocket);
            }
        }
    }

    private static final byte[] RESPONSE_OK = ResponseConstants.RESPONSE_OK;

    private static final byte[] RESPONSE_NOK = ResponseConstants.RESPONSE_NOK;

    private static final int MAX_LINE_LENGTH = 32;

    /**
     * Handles the client socket
     *
     * @param clientSocket The socket to handle
     * @param lineBuffer The line buffer to use
     */
    private static void handleClientSocket(Socket clientSocket, byte[] lineBuffer) {
        InputStream in = null;
        OutputStream out = null;
        try {
            // Initialize streams
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();

            // Expect: GET /live HTTP/1.1
            int numBytesRead = in.read(lineBuffer, 0, MAX_LINE_LENGTH);
            if (startsWithExpectedPrefix(lineBuffer, numBytesRead)) {
                // All fine...
                out.write(RESPONSE_OK, 0, RESPONSE_OK.length);
            } else {
                // Nope...
                out.write(RESPONSE_NOK, 0, RESPONSE_NOK.length);
            }
            out.flush();
        } catch (Exception e) {
            LOG.error("Failed to write response to client", e);
        } finally {
            close(in, out);
        }
    }

    private static final byte[] EXPECTED_REQUEST_LINE_PREFIX = ResponseConstants.EXPECTED_REQUEST_LINE_PREFIX;

    /**
     * Whether the linebuffer starts with the expected prefix
     *
     * @param lineBuffer The linebuffer to check
     * @param numBytesRead The number of read bytes
     * @return <code>true</code> if the linebuffer starts with the expected prefix, <code>false</code> otherwise
     */
    private static boolean startsWithExpectedPrefix(byte[] lineBuffer, int numBytesRead) {
        if (numBytesRead < EXPECTED_REQUEST_LINE_PREFIX.length) {
            return false;
        }

        for (int i = EXPECTED_REQUEST_LINE_PREFIX.length; i-- > 0;) {
            if (EXPECTED_REQUEST_LINE_PREFIX[i] != lineBuffer[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Stops the HTTP Liveness end-point.
     */
    public synchronized void stop() {
        stopped.set(true);
        ServerSocket serverSocket = this.serverSocket;
        if (serverSocket != null) {
            this.serverSocket = null;
            close(serverSocket);
        }
        ExecutorService executors = this.executors;
        if (executors != null) {
            this.executors = null;
            executors.shutdown();
        }
        LivenessThreadFactory threadFactory = this.threadFactory;
        if (threadFactory != null) {
            this.threadFactory = null;
            threadFactory.shutDown();
        }
    }

    /**
     * Safely closes specified {@link AutoCloseable} instances.
     *
     * @param toCloses The {@link AutoCloseable} instances
     */
    private static void close(AutoCloseable... toCloses) {
        if (null != toCloses && toCloses.length > 0) {
            for (AutoCloseable toClose : toCloses) {
                try {
                    toClose.close();
                } catch (Exception e) {
                    LOG.error("Failed to close {}", toClose.getClass().getSimpleName(), e);
                }
            }
        }
    }

}
