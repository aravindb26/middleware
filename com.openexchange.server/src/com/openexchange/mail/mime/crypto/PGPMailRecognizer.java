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

package com.openexchange.mail.mime.crypto;

import javax.mail.Message;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.MailMessage;

/**
 * {@link PGPMailRecognizer} - Service for detection of possibly PGP-encrypted messages.
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 * @since v7.8.4
 */
public interface PGPMailRecognizer {

    /**
     * Checks whether the given message is a PGP message or not.
     *
     * @param message The message
     * @return True, if the given message is a PGP message, false otherwise.
     * @throws OXException
     */
    boolean isPGPMessage(MailMessage message) throws OXException;

    /**
     * Checks whether the given message is a signed PGP message or not.
     *
     * @param message The message
     * @return True, if the given message is a PGP signed message, false otherwise
     * @throws OXException
     */
    boolean isPGPSignedMessage(MailMessage message) throws OXException;

    /**
     * Checks whether the given message is a PGP message or not.
     *
     * @param msg
     * @return
     * @throws OXException
     */
    boolean isPGPMessage(Message msg) throws OXException;
}