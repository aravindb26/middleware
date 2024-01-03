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

package com.openexchange.push.dovecot.commands;

import static com.openexchange.imap.util.ImapUtility.prepareImapCommandForLogging;
import org.slf4j.Logger;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.log.LogProperties;
import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPFolder.ProtocolCommand;
import com.sun.mail.imap.protocol.IMAPProtocol;

/**
 * {@link UnregistrationCommand} - Issues the special SETMETADATA command to un-register a push listener at Dovecot.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class UnregistrationCommand implements ProtocolCommand {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(UnregistrationCommand.class);

    private final IMAPFolder imapFolder;

    /**
     * Initializes a new {@link UnregistrationCommand}.
     *
     * @param imapFolder The IMAP folder
     */
    public UnregistrationCommand(IMAPFolder imapFolder) {
        super();
        this.imapFolder = imapFolder;
    }

    @Override
    public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
        // Craft IMAP command
        String command = "SETMETADATA \"\" (/private/vendor/vendor.dovecot/http-notify NIL)";

        // Issue command
        Response[] r = IMAPCommandsCollection.performCommand(protocol, command);
        Response response = r[r.length - 1];
        if (response.isOK()) {
            LOGGER.info("Unregistered push notifications for {} using: {}", imapFolder.getStore(), command);
            return Boolean.TRUE;
        } else if (response.isBAD()) {
            LogProperties.putProperty(LogProperties.Name.MAIL_COMMAND, prepareImapCommandForLogging(command));
            if (null != imapFolder) {
                LogProperties.putProperty(LogProperties.Name.MAIL_FULL_NAME, imapFolder.getFullName());
            }
            throw new BadCommandException(response);
        } else if (response.isNO()) {
            LogProperties.putProperty(LogProperties.Name.MAIL_COMMAND, prepareImapCommandForLogging(command));
            if (null != imapFolder) {
                LogProperties.putProperty(LogProperties.Name.MAIL_FULL_NAME, imapFolder.getFullName());
            }
            throw new CommandFailedException(response);
        } else {
            LogProperties.putProperty(LogProperties.Name.MAIL_COMMAND, prepareImapCommandForLogging(command));
            protocol.handleResult(response);
        }
        LOGGER.warn("Failed to unregister push notifications for {} using: {}", imapFolder.getStore(), command);
        return Boolean.FALSE;
    }
}
