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

package com.openexchange.mail.mime.crypto.impl;

import java.util.List;
import javax.mail.Message;
import com.openexchange.crypto.CryptoType;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.SecurityInfo;
import com.openexchange.mail.mime.crypto.CryptoMailRecognizer;
import com.openexchange.mail.mime.crypto.CryptoMailRecognizerService;

/**
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.6
 */
public class CryptoMailRecognizerServiceImpl implements CryptoMailRecognizerService {

    @Override
    public boolean isCryptoMessage(MailMessage message) throws OXException {
        return !getTypeCrypto(message).equals(CryptoType.PROTOCOL.OTHER);
    }

    @Override
    public boolean isSignedMessage(MailMessage message) throws OXException {
        return !getTypeSigned(message).equals(CryptoType.PROTOCOL.OTHER);
    }

    @Override
    public CryptoType.PROTOCOL getTypeCrypto(MailMessage message) throws OXException {
        List<CryptoMailRecognizer> recognizers = CryptoMailRecognizerRegistry.getInstance().getRecognizers();
        for (CryptoMailRecognizer recognizer : recognizers) {
            if (recognizer.isCryptoMessage(message)) {
                return recognizer.getType();
            }
        }
        return CryptoType.PROTOCOL.OTHER;
    }

    /**
     *
     * Returns type of crypto used for signatures
     *
     * @param message
     * @return type or null if not found
     * @throws OXException
     */
    private CryptoType.PROTOCOL getTypeSigned(MailMessage message) throws OXException {
        List<CryptoMailRecognizer> recognizers = CryptoMailRecognizerRegistry.getInstance().getRecognizers();
        for (CryptoMailRecognizer recognizer : recognizers) {
            if (recognizer.isSignedMessage(message)) {
                return recognizer.getType();
            }
        }
        return CryptoType.PROTOCOL.OTHER;
    }

    private boolean isRecognizedType(CryptoType.PROTOCOL type) {
        return !type.equals(CryptoType.PROTOCOL.OTHER);
    }

    @Override
    public SecurityInfo getSecurityInfo(MailMessage message) throws OXException {
        CryptoType.PROTOCOL typeSigned = getTypeSigned(message);
        CryptoType.PROTOCOL crypto = getTypeCrypto(message);
        return new SecurityInfo(isRecognizedType(crypto), isRecognizedType(typeSigned), isRecognizedType(crypto) ? crypto : typeSigned);
    }

    @Override
    public boolean isEncryptedMessage(Message msg) throws OXException {
        List<CryptoMailRecognizer> recognizers = CryptoMailRecognizerRegistry.getInstance().getRecognizers();
        for (CryptoMailRecognizer recognizer : recognizers) {
            if (recognizer.isEncryptedMessage(msg)) {
                return true;
            }
        }
        return false;
    }

}
