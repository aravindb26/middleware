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

package com.openexchange.unifiedinbox;

import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.session.Session;
import com.openexchange.unifiedinbox.utility.LoggingCallable;

/**
 * {@link AbstractAccountOperation} - The abstract operation to access a certain account's mail storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @param <V> The result type
 */
public abstract class AbstractAccountOperation<V> extends LoggingCallable<V> {

    private final boolean debug;

    /**
     * Initializes a new {@link AbstractAccountOperation}.
     *
     * @param session The session
     * @param mailAccount The account
     * @param debug Whether to establish access to mail storage in debug mode
     */
    protected AbstractAccountOperation(Session session, MailAccount mailAccount, boolean debug) {
        this(session, mailAccount.getId(), debug);
    }

    /**
     * Initializes a new {@link AbstractAccountOperation}.
     *
     * @param session The session
     * @param accountId The account identifier
     * @param debug Whether to establish access to mail storage in debug mode
     */
    protected AbstractAccountOperation(Session session, int accountId, boolean debug) {
        super(session, accountId);
        this.debug = debug;
    }

    @Override
    public final V call() throws Exception {
        // Get account's mail access
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            mailAccess = MailAccess.getInstance(getSession(), getAccountId());
            mailAccess.connect(true, debug);
            return doOperation(mailAccess);
        } finally {
            closeSafe(mailAccess);
        }
    }

    /**
     * Performs the mail access operation.
     *
     * @param mailAccess The mail access to use
     * @return The result
     * @throws Exception If mail access operation fails
     */
    protected abstract V doOperation(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) throws Exception;

    // ------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Safely closes given mail access instance.
     *
     * @param mailAccess The instance to close (if not <code>null</code>)
     */
    private static void closeSafe(MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess) {
        if (null != mailAccess) {
            mailAccess.close(true);
        }
    }

}
