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

import static com.openexchange.crypto.CryptoErrorMessage.BadPassword;
import static com.openexchange.crypto.CryptoErrorMessage.NoSalt;
import static com.openexchange.crypto.CryptoErrorMessage.SecurityException;
import static com.openexchange.java.Charsets.UTF_8;
import static java.util.Base64.getEncoder;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.crypto.CryptoErrorMessage;
import com.openexchange.crypto.EncryptedData;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.CryptoUtil;
import com.openexchange.java.Strings;
import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2Parameters;

/**
 * {@link LegacyCrypto} - This class is not secure anymore. It contains the legacy crypto
 * mechanisms used to until 8.3.
 * <p/>
 * The only reason that exists is to provide a fallback performer for cryptos created
 * with that cipher and be able to decipher them correctly.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public final class LegacyCrypto {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyCrypto.class);

    /**
     * Key Charset
     */
    private static final String CHARSET = "UTF-8";

    /**
     * Hash Algorithm for generating PBE-Keys.
     */
    @Deprecated
    private static final String KEY_ALGORITHM = "HMacSHA1";

    /**
     * Key length
     */
    private static final int KEY_LENGTH = 16;
    /**
     * Salt length 128 bits.
     */
    private static final short SALT_LENGTH = 128 / Bytes.SIZE;

    /**
     * The algorithm.
     */
    private static final String ALGORITHM = "AES";

    /**
     * The mode.
     */
    @Deprecated
    private static final String MODE = "CBC";

    /**
     * The padding.
     */
    @Deprecated
    private static final String PADDING = "PKCS5Padding";

    /**
     * Initialization Vector
     */
    @Deprecated
    private static final IvParameterSpec IV = new IvParameterSpec(new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });

    /**
     * The transformation following pattern <i>"algorithm/mode/padding"</i>.
     */
    private static final String CIPHER_TYPE = ALGORITHM + "/" + MODE + "/" + PADDING;

    /**
     * Default Salt, if no salt is given or "used". But password encryption needs always encryption.
     */
    @Deprecated
    private static final byte[] SALT = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f };

    /**
     * Decrypts specified encrypted data with the given password.
     *
     * @param data EncryptedData object with the encrypted data (Base64 String) and salt
     * @param password The password
     * @param useSalt use Salt from the given EncryptedData object if true
     * @return The decrypted data as String
     * @throws OXException If decryption fails
     * @deprecated Use {@link CryptoService#encrypt(String, String)} instead.
     */
    @Deprecated
    static EncryptedData encrypt(String data, String password, boolean useSalt) throws OXException {
        if (data == null) {
            return null;
        }
        if (useSalt) {
            byte[] salt = CryptoUtil.generateRandomBytes(SALT_LENGTH);
            return new EncryptedData(encrypt(data, generateSecretKey(password, salt)), salt);
        }
        return new EncryptedData(encrypt(data, generateSecretKey(password, SALT)), null);
    }

    /**
     * Decrypts specified encrypted data with the given password.
     *
     * @param data EncryptedData object with the encrypted data (Base64 String) and salt
     * @param password The password
     * @param useSalt use Salt from the given EncryptedData object if true
     * @return The decrypted data as String
     * @throws OXException If decryption fails
     * @deprecated Use {@link CryptoService#decrypt(String, String)} instead.
     */
    @Deprecated
    static String decrypt(EncryptedData data, String password, boolean useSalt) throws OXException {
        if (useSalt && data.salt() == null) {
            throw NoSalt.create();
        }
        if (Strings.isEmpty(data.data())) {
            return data.data();
        }
        return decrypt(data.data(), generateSecretKey(password, useSalt ? data.salt() : SALT));
    }

    /**
     * Decrypts specified encrypted data with the given password.
     *
     * @param encryptedData The Base64 encoded encrypted data
     * @param key The key
     * @return The decrypted data as String
     * @throws OXException If decryption fails
     * @deprecated Use {@link CryptoService#decrypt(String, Key)} instead.
     */
    @Deprecated
    static String decrypt(String encryptedData, Key key) throws OXException {
        Cipher cipher;
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
                throw SecurityException.create();
            }
            encrypted = java.util.Base64.getDecoder().decode(encryptedData);

            cipher = Cipher.getInstance(CIPHER_TYPE);
            cipher.init(Cipher.DECRYPT_MODE, key, IV);
        } catch (GeneralSecurityException e) {
            throw SecurityException.create(e);
        } catch (IllegalArgumentException e) {
            // No valid base64 scheme
            throw SecurityException.create(e);
        }

        try {
            byte[] outputBytes = cipher.doFinal(encrypted);
            return new String(outputBytes, Charsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw BadPassword.create(e);
        }
    }

    /**
     * Encrypts specified data with given key.
     *
     * @param data The data to encrypt
     * @param password The password
     * @return The encrypted data as Base64 encoded string
     * @throws OXException if the data cannot be encrypted
     * @deprecated Use {@link CryptoService#encrypt(String, Key)} instead.
     */
    @Deprecated
    static String encrypt(String data, Key password) throws OXException {
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
            cipher.init(Cipher.ENCRYPT_MODE, password, IV);
            byte[] outputBytes = cipher.doFinal(data.getBytes(UTF_8));
            /*-
             * It's safe to use "US-ASCII" to turn bytes into a Base64 encoded encrypted password string.
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
            return getEncoder().encodeToString(outputBytes);
        } catch (GeneralSecurityException e) {
            throw SecurityException.create(e);
        }
    }

    @Deprecated
    static CipherInputStream encryptingStreamFor(InputStream in, Key key) throws OXException {
        if (null == in) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
            cipher.init(Cipher.ENCRYPT_MODE, key, IV);
            return new CipherInputStream(in, cipher);
        } catch (GeneralSecurityException e) {
            throw SecurityException.create(e);
        }
    }

    @Deprecated
    static CipherInputStream decryptingStreamFor(InputStream in, Key key) throws OXException {
        if (null == in) {
            return null;
        }

        try {
            Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
            cipher.init(Cipher.DECRYPT_MODE, key, IV);
            return new CipherInputStream(in, cipher);
        } catch (GeneralSecurityException e) {
            throw SecurityException.create(e);
        }
    }

    /**
     * Generates a secret key from specified password string.
     *
     * @param password The password string
     * @return A secret key generated from specified password string
     * @throws OXException if the supplied password is empty or any other error is occurred
     */
    private static SecretKey generateSecretKey(String password, byte[] salt) throws OXException {
        if (Strings.isEmpty(password)) {
            throw CryptoErrorMessage.EmptyPassword.create();
        }
        try {
            PBKDF2Parameters params = new PBKDF2Parameters(KEY_ALGORITHM, CHARSET, salt, 1000);
            PBKDF2Engine engine = new PBKDF2Engine(params);
            return new SecretKeySpec(engine.deriveKey(password, KEY_LENGTH), ALGORITHM);
        } catch (RuntimeException e) {
            throw CryptoErrorMessage.SecurityException.create(e, e.getMessage());
        }
    }
}
