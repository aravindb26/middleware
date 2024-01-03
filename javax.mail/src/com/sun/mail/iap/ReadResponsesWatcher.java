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

import java.time.Duration;
import com.openexchange.java.AbstractOperationsWatcher;

/**
 * Watches reading response(s) from IMAP server.
 */
public class ReadResponsesWatcher extends AbstractOperationsWatcher<ReadResponsesInfo> {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ReadResponsesWatcher.class);

    private static final ReadResponsesWatcher INSTANCE = new ReadResponsesWatcher();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static ReadResponsesWatcher getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link ReadResponsesWatcher}.
     */
    private ReadResponsesWatcher() {
        super("IMAP-ReadResponsesWatcher", Duration.ofMinutes(5));
    }

    @Override
    protected ReadResponsesInfo getPoisonElement() {
        return ReadResponsesInfo.POISON;
    }

    @Override
    protected org.slf4j.Logger getLogger() {
        return LOG;
    }

    @Override
    protected void handleExpiredOperation(ReadResponsesInfo info) throws Exception {
        info.signalExpired();
        LOG.debug("Expired thread {} reading IMAP responses from {} for user {}", info.getProcessingThread().getName(), info.getProtocol().getHost(), info.getProtocol().getUser());
    }

    @Override
    protected void handleUnwatchedOperation(ReadResponsesInfo info, boolean removed) throws Exception {
        info.signalLoopExited();
    }
}
