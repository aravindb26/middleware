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


package com.openexchange.gdpr.dataexport.provider.mail.internal;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.exception.OXException;
import com.openexchange.gdpr.dataexport.DataExportTask;
import com.openexchange.gdpr.dataexport.GeneratedSession;
import com.openexchange.gdpr.dataexport.Module;
import com.openexchange.gdpr.dataexport.provider.mail.generator.FailedAuthenticationResult;
import com.openexchange.gdpr.dataexport.provider.mail.generator.SessionGenerator;
import com.openexchange.java.util.UUIDs;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mail.service.MailService;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link MailOperationExecutor} - Executes mail operations.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class MailOperationExecutor {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailOperationExecutor.class);

    private final ServiceLookup services;
    private final DataExportTask task;
    private final Module mailModule;
    private final SessionGenerator sessionGenerator;
    private MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> currentMailAccess;

    /**
     * Initializes a new {@link MailOperationExecutor}.
     *
     * @param mailModule The mail module instance
     * @param sessionGenerator The session generator used to spawn a session
     * @param initialMailAccess The initial mail access instance
     * @param task The data export task for which messages are exported
     * @param services The service look-up
     */
    MailOperationExecutor(Module mailModule, SessionGenerator sessionGenerator, MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> initialMailAccess, DataExportTask task, ServiceLookup services) {
        super();
        this.mailModule = mailModule;
        this.sessionGenerator = sessionGenerator;
        this.currentMailAccess = initialMailAccess;
        this.task = task;
        this.services = services;
    }

    /**
     * Gets the currently active mail access.
     *
     * @return The current mail access
     */
    public MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> getCurrentMailAccess() {
        return currentMailAccess;
    }

    /**
     * Executes given mail operation with currently active mail access.
     * <p>
     * Operation is retried on unexpected connection loss.
     *
     * @param <V> The result type
     * @param operation
     * @return The operation's result
     * @throws OXException Of executing operation fails
     */
    public <V> V executeWithRetryOnConnectionLoss(MailOperation<V> operation) throws OXException {
        int runs = 2; // First and retry run
        while (runs-- > 0) {
            try {
                return operation.execute(currentMailAccess);
            } catch (OXException oxe) {
                if (runs <= 0 || !isConnectionLoss(oxe)) {
                    // Retry attempts exceeded or not a connection problem
                    throw oxe;
                }
                // Retry
                onRetry(oxe);
            } catch (IllegalArgumentException e) {
                String message = e.getMessage();
                if (message != null && message.startsWith("Invalid fully-qualifying mail folder identifier: ")) {
                    // Apparently, passed folder identifier is invalid
                    throw MailExceptionCode.INVALID_FOLDER_IDENTIFIER.create(e, message.substring(49));
                }
                // Unchecked exception occurred
                throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
            } catch (Exception e) {
                throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
            }
        }
        throw MailExceptionCode.UNEXPECTED_ERROR.create("Failed to execute mail operation");
    }

    private void onRetry(OXException connectionLossException) throws OXException {
        LOG.debug("Connection to mail server '{}' closed during data export {} of user {} in context {}. Retrying...", currentMailAccess.getMailConfig().getServer(), UUIDs.getUnformattedString(task.getId()), I(task.getUserId()), I(task.getContextId()), connectionLossException);

        // Grab session associated with current mail access in order to establish a new one
        Session session = currentMailAccess.getSession();

        // Check for failed authentication
        if (MailAccess.isAuthFailed(connectionLossException)) {
            // Retry with new generated session
            LOG.debug("Failed authentication against mail server '{}' during data export {} of user {} in context {}. Trying to establish a new session...", currentMailAccess.getMailConfig().getServer(), UUIDs.getUnformattedString(task.getId()), I(task.getUserId()), I(task.getContextId()));
            try {
                FailedAuthenticationResult failedAuthenticationResult = sessionGenerator.onFailedAuthentication(connectionLossException, (GeneratedSession) session, mailModule.getProperties().get());
                if (failedAuthenticationResult.retry()) {
                    session = failedAuthenticationResult.getOptionalSession().get();
                }
            } catch (OXException e) {
                LOG.warn("No remedy for failed authentication against mail server '{}' during data export {} of user {} in context {}", currentMailAccess.getMailConfig().getServer(), UUIDs.getUnformattedString(task.getId()), I(task.getUserId()), I(task.getContextId()), e);
                throw e;
            }
        }

        MailService mailService = services.getServiceSafe(MailService.class);
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            // Establish new mail access
            mailAccess = mailService.getMailAccess(session, 0);
            mailAccess.connect();

            // Drop old mail access
            boolean wasWaiting = currentMailAccess.isWaiting();
            if (wasWaiting) {
                currentMailAccess.setWaiting(false);
            }
            MailAccess.closeInstance(currentMailAccess, false);
            currentMailAccess = null;

            // Apply new mail access
            currentMailAccess = mailAccess;
            mailAccess = null; // Avoid premature closing
            if (wasWaiting) {
                // Restore waiting flag
                currentMailAccess.setWaiting(true);
            }
        } finally {
            MailAccess.closeInstance(mailAccess);
        }
    }

    /**
     * Checks if given exception indicates a loss of mail connection.
     *
     * @param oxe The exception to examine
     * @return <code>true</code> for connection loss; otherwise <code>false</code>
     */
    private static boolean isConnectionLoss(OXException oxe) {
        return MimeMailExceptionCode.CONNECTION_CLOSED.equals(oxe)
            || MimeMailExceptionCode.CONNECT_ERROR.equals(oxe)
            || MimeMailExceptionCode.READ_TIMEOUT.equals(oxe);
    }

}
