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

package com.openexchange.chronos.scheduling.impl;

import static com.openexchange.chronos.scheduling.impl.ITipMailFlag.ANALYZED;
import static com.openexchange.chronos.scheduling.impl.ITipMailFlag.IGNORED;
import static com.openexchange.chronos.scheduling.impl.ITipMailFlag.PROCESSED;
import static com.openexchange.chronos.scheduling.impl.incoming.MailUtils.getMailAccess;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.tools.arrays.Arrays.isNullOrEmpty;
import static com.openexchange.tools.arrays.Collections.isNotEmpty;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import com.openexchange.annotation.NonNull;
import com.openexchange.chronos.scheduling.IncomingIMip;
import com.openexchange.chronos.scheduling.IncomingSchedulingMessage;
import com.openexchange.chronos.scheduling.IncomingSchedulingObject;
import com.openexchange.chronos.scheduling.MessageStatus;
import com.openexchange.chronos.scheduling.MessageStatusService;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailField;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.crypto.CryptographicAwareMailAccessFactory;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mailaccount.Account;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * 
 * {@link MessageStatusServiceImpl}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.6
 */
public class MessageStatusServiceImpl implements MessageStatusService {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link MessageStatusServiceImpl}.
     * 
     * @param services The service lookup
     */
    public MessageStatusServiceImpl(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public void setMessageStatus(Session session, IncomingSchedulingMessage message, MessageStatus status) throws OXException {
        /*
         * Check if applicable
         */
        IncomingSchedulingObject schedulingObject = message.getSchedulingObject();
        if (false == IncomingIMip.class.isAssignableFrom(schedulingObject.getClass())) {
            return;
        }
        IncomingIMip iMip = ((IncomingIMip) schedulingObject);
        if (null == status || status.equals(iMip.getState())) {
            return;
        }
        /*
         * Set flag on actual mail object on the mail server
         */
        ITipMailFlag flag = getFlag(status);
        if (null == flag) {
            return;
        }
        MailAccess<?, ?> mailAccess = null;
        try {
            mailAccess = getMailAccess(services.getOptionalService(CryptographicAwareMailAccessFactory.class), session, getAccountId(iMip));
            setFlag(mailAccess, iMip.getMailFolderId(), iMip.getMailId(), flag);
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }

    }

    /**
     * Sets a specific state for the given message
     * 
     * @param mailAccess The mail access to use to set the status as user flag on the mail
     * @param message The message to mark
     * @param messageStatus The status of the message
     * @throws OXException In case of error
     */
    public void setMessageStatus(MailAccess<?, ?> mailAccess, MailMessage message, MessageStatus messageStatus) throws OXException {
        ITipMailFlag flag = getFlag(messageStatus);
        if (null != flag) {
            setFlag(mailAccess, message, flag);
        }
    }

    private void setFlag(MailAccess<?, ?> mailAccess, String folderId, String mailId, ITipMailFlag flag) throws OXException {
        MailMessage[] messages = mailAccess.getMessageStorage().getMessages(folderId, new String[] { mailId }, new MailField[] { MailField.FLAGS });
        if (isNullOrEmpty(messages) || messages.length != 1) {
            return;
        }
        setFlag(mailAccess, messages[0], flag);
    }

    private void setFlag(MailAccess<?, ?> mailAccess, MailMessage message, ITipMailFlag flag) throws OXException {
        /*
         * Check if mail needs to be updated
         */
        List<String> userFlags = Arrays.asList(message.getUserFlags());
        if (isNotEmpty(userFlags)) {
            if (userFlags.contains(flag.getFlag())) {
                return;
            }
            if (userFlags.contains(ANALYZED.getFlag())) {
                /*
                 * Remove old flag
                 */
                mailAccess.getMessageStorage().updateMessageUserFlags(message.getFolder(), new String[] { message.getMailId() }, new String[] { ANALYZED.getFlag() }, false);
            } else if (userFlags.contains(PROCESSED.getFlag()) || userFlags.contains(IGNORED.getFlag())) {
                /*
                 * Message has been processed meanwhile, ignore current action
                 */
                return;
            }
        }
        /*
         * Add new flag and update the mail
         */
        mailAccess.getMessageStorage().updateMessageUserFlags(message.getFolder(), new String[] { message.getMailId() }, new String[] { flag.getFlag() }, true);
    }

    @Override
    public @NonNull MessageStatus getMessageStatus(IncomingSchedulingMessage message) {
        /*
         * Lookup if mail flag was set
         */
        Optional<IncomingIMip> iMip = getIMip(message);
        if (false == iMip.isPresent()) {
            return MessageStatus.NONE;
        }
        MessageStatus state = iMip.get().getState();
        return null == state ? MessageStatus.NONE : state;
    }

    /*
     * ============================== HELPERS ==============================
     */

    /**
     * Get the account ID from an {@link IncomingSchedulingMessage}
     *
     * @param message The message
     * @return The account ID or {@link Account#DEFAULT_ID}
     */
    private static int getAccountId(IncomingIMip iMip) {
        String mailAccountId = iMip.getMailAccountId();
        if (Strings.isNotEmpty(mailAccountId)) {
            Integer id = Integer.valueOf(mailAccountId);
            if (null != id) {
                return i(id);
            }
        }
        return Account.DEFAULT_ID;
    }

    /**
     * Tries to get the iMIP meta data from the incoming message
     *
     * @param message The message
     * @return An optional containing the iMIP meta data, if this is an iMIP message, or an empty optional
     */
    public static Optional<IncomingIMip> getIMip(IncomingSchedulingMessage message) {
        IncomingSchedulingObject schedulingObject = message.getSchedulingObject();
        if (IncomingIMip.class.isAssignableFrom(schedulingObject.getClass())) {
            return Optional.of(((IncomingIMip) schedulingObject));
        }
        return Optional.empty();
    }

    private ITipMailFlag getFlag(MessageStatus status) {
        switch (status) {
            case IGNORED:
                return ITipMailFlag.IGNORED;
            case NEEDS_USER_INTERACTION:
                return ITipMailFlag.ANALYZED;
            case APPLIED:
                return ITipMailFlag.PROCESSED;
            default:
                return null;
        }
    }

}
