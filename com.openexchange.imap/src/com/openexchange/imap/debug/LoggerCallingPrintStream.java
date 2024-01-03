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

package com.openexchange.imap.debug;

import static com.openexchange.java.Autoboxing.I;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * {@link LoggerCallingPrintStream} - A print stream writing passed bytes to an instance of <code>org.slf4j.Logger</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.4
 */
public class LoggerCallingPrintStream extends AbstractLoggerCallingPrintStream {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(LoggerCallingPrintStream.class);
    }

    private final javax.mail.Session imapSession;
    private final String server;
    private final int userId;
    private final int contextId;

    /**
     * Initializes a new {@link LoggerCallingPrintStream}.
     *
     * @param imapSession The IMAP session to enabled debug logging for
     * @param server The IMAP server
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public LoggerCallingPrintStream(javax.mail.Session imapSession, String server, int userId, int contextId) {
        super();
        this.imapSession = imapSession;
        this.server = server;
        this.userId = userId;
        this.contextId = contextId;
    }

    @Override
    protected Optional<Logger> createLogger() {
        try {
            return Optional.of(IMAPDebugLoggerGenerator.generateLoggerFor(imapSession, server, userId, contextId));
        } catch (Exception e) {
            // Failed to create logger
            LoggerHolder.LOG.warn("Failed to create DEBUG logger for IMAP server {} for user {} in context {} with new IMAP session {}", server, I(userId), I(contextId), IMAPDebugLoggerGenerator.toPositiveString(imapSession.hashCode()), e);
        }
        return Optional.empty();
    }

}
