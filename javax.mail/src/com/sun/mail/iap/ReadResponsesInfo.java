/*
 * Copyright (c) 1997, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.mail.iap;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import com.openexchange.java.AbstractOperationsWatcher;

/**
 * Collects information when reading response(s) from IMAP server.
 */
public class ReadResponsesInfo extends AbstractOperationsWatcher.Operation {

    /** The poison constant */
    public static final ReadResponsesInfo POISON = new ReadResponsesInfo() {

        @Override
        public int compareTo(Delayed o) {
            return -1;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }
    };

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Protocol protocol;
    private int expired;

    /**
     * Dummy constructor
     */
    private ReadResponsesInfo() {
        super();
        protocol = null;
        expired = 0;
    }

    /**
     * Initializes a new {@link ReadResponsesInfo}.
     *
     * @param reader The thread reading responses from IMAP server
     * @param protocol The protocol instance from which responses are read
     * @param timeoutMillis The timeout in milliseconds
     */
    public ReadResponsesInfo(Thread reader, Protocol protocol, int timeoutMillis) {
        super(reader, timeoutMillis);
        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException("Timeout must not be less than or equal to 0 (zero)");
        }
        this.protocol = protocol;
        expired = 0;
    }

    /**
     * Gets the protocol.
     *
     * @return The protocol
     */
    public Protocol getProtocol() {
        return protocol;
    }

    /**
     * Signals that this instance has expired since read responses timeout has been exceeded.
     */
    public synchronized void signalExpired() {
        if (expired == 2) {
            // Operation through; loop to read responses exited
            return;
        }
        expired = 1;
        protocol.getInputStream().interruptReadResponse();
        getProcessingThread().interrupt();
    }

    /**
     * Signals that this loop to read responses has been exited to avoid wrong state in ResponseInputStream and Thread.
     */
    public synchronized void signalLoopExited() {
        protocol.getInputStream().uninterruptReadResponse();
        if (expired == 1) {
            // Already interrupted
            return;
        }
        expired = 2;
    }

}
