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

package com.openexchange.crypto.internal;

import com.openexchange.crypto.CryptoErrorMessage;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.CryptoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;

/**
 * {@link Crypto8x}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
class Crypto8x {
    private static final Logger LOG = LoggerFactory.getLogger(CryptoServiceImpl.class);

    private static final Charset UTF_8 = Charsets.UTF_8;

    /**
     * The algorithm.
     */
    private static final String ALGORITHM = "AES";

    /**
     * The mode.
     */
    private static final String MODE = "GCM";

    /**
     * The padding.
     */
    private static final String PADDING = "NoPadding";

    /**
     * The transformation following pattern <i>"algorithm/mode/padding"</i>.
     */
    private static final String CIPHER_TYPE = ALGORITHM + "/" + MODE + "/" + PADDING;

    /**
     * The amount of tag bits for the GCM mode
     */
    private static final int GCM_TAG_BITS = 128;


    /**
     * Performs the decryption. If decryption fails with the current algorithm a {@link GeneralSecurityException}
     * will be raised, to signalise that. Consumers should try decrypting with the legacy method in {@link LegacyCrypto}.
     *
     * @param encryptedData the encrypted data
     * @param key the key
     * @return The decrypted data
     * @throws OXException If the cipher fails initialisation or the encrypted data is not in BASE-64
     */
    static String doDecrypt(String encryptedData, Key key, byte[] iv) throws OXException {
        byte[] encrypted;
        try {
            /*-
             * It's safe to use "US-ASCII" to turn Base64 encoded encrypted password string into bytes.
             * Taken from RFC 2045 Section 6.8. "Base64 Content-Transfer-Encoding":
             *
             * A 65-character subset of US-ASCII is used, enabling 6 bits to be
             * represented per printable character. (The extra 65th character, "=",
             * is used to signify a special processing function.)
             *
             * NOTE: This subset has the important property that it is represented
             * identically in all versions of ISO 646, including US-ASCII, and all
             * characters in the subset are also represented identically in all
             * versions of EBCDIC. Other popular encodings, such as the encoding
             * used by the uuencode utility, Macintosh binhex 4.0 [RFC-1741], and
             * the base85 encoding specified as part of Level 2 PostScript, do not
             * share these properties, and thus do not fulfill the portability
             * requirements a binary transport encoding for mail must meet.
             *
             */
            if (encryptedData.getBytes(StandardCharsets.US_ASCII).length < 2) {
                LOG.debug("Data is too short to be decrypted");
                throw CryptoErrorMessage.SecurityException.create();
            }
            encrypted = Base64.getDecoder().decode(encryptedData);
        } catch (IllegalArgumentException e) {
            // No valid base64 scheme
            throw CryptoErrorMessage.SecurityException.create(e);
        }

        try {
            Cipher cipher = CryptoUtil.initCipher(Cipher.DECRYPT_MODE, CIPHER_TYPE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] outputBytes = cipher.doFinal(encrypted);
            return new String(outputBytes, UTF_8);
        } catch (GeneralSecurityException e) {
            throw CryptoErrorMessage.BadPassword.create(e);
        }
    }
}
