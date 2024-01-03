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
import com.openexchange.crypto.CryptoType;
import com.openexchange.crypto.CryptoType.PROTOCOL;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.SecurityInfo;

/**
 * {@link CryptoMailRecognizerService}
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.6
 */
public interface CryptoMailRecognizerService {

    /**
     * Checks whether the given message is a crypto message or not.
     *
     * @param message The message
     * @return <code>true</code>, if the given message is a PGP message, <code>false</code> otherwise.
     * @throws OXException
     */
    boolean isCryptoMessage(MailMessage message) throws OXException;

    /**
     * Checks whether the given message is a signed PGP message or not.
     *
     * @param message The message
     * @return <code>true</code>, if the given message is a signed message, <code>false</code> otherwise
     * @throws OXException In case of error
     */
    boolean isSignedMessage(MailMessage message) throws OXException;

    /**
     * Returns a value indicating whether the given message contains encrypted content or not
     *
     * @param msg The message to look at
     * @return <code>true</code> if the message contains encrypted content, <code>false</code> otherwise
     * @throws OXException In case of error
     */
    boolean isEncryptedMessage(Message msg) throws OXException;

    /**
     * Creates SecurityInfo from Mail Message
     *
     * @param message The message
     * @return A {@link SecurityInfo} for the given message
     * @throws OXException In case info can't be generated
     */
    SecurityInfo getSecurityInfo(MailMessage message) throws OXException;

    /**
     * Get the protocol which is used for crypto on the given message
     *
     * @param message The message
     * @return The protocol of the message, otherwise if not found {@link PROTOCOL#OTHER} is returned
     * @throws OXException In case of error
     */
    CryptoType.PROTOCOL getTypeCrypto(MailMessage message) throws OXException;
}