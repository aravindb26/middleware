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

package com.openexchange.mail.utils;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.openexchange.crypto.CryptoService;
import com.openexchange.database.Databases;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.CryptoUtil;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountExceptionCodes;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.internal.GenericProperty;
import com.openexchange.secret.SecretEncryptionFactoryService;
import com.openexchange.secret.SecretEncryptionService;
import com.openexchange.secret.SecretEncryptionStrategy;
import com.openexchange.secret.SecretExceptionCodes;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;

/**
 * {@link MailPasswordUtil} - Utility class to encrypt/decrypt passwords with a key aka <b>p</b>assword <b>b</b>ased <b>e</b>ncryption
 * (PBE).
 * <p>
 * PBE is a form of symmetric encryption where the same key or password is used to encrypt and decrypt a string.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailPasswordUtil {

    /** The logger constant */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MailPasswordUtil.class);

    /** The key length. */
    private static final int KEY_LENGTH = 8;

    /** The DES algorithm. */
    private static final String ALGORITHM_DES = "DES";

    /** The transformation following pattern <i>"algorithm/mode/padding"</i>. */
    private static final String CIPHER_TYPE = ALGORITHM_DES + "/ECB/PKCS5Padding";

    /**
     * Mail account secret encryption strategy.
     */
    public static final SecretEncryptionStrategy<GenericProperty> STRATEGY = new SecretEncryptionStrategy<GenericProperty>() {

        @Override
        public void update(String recrypted, GenericProperty customizationNote) throws OXException {
            final int contextId = customizationNote.session.getContextId();
            final Connection con = Database.get(contextId, true);
            int rollback = 0;
            try {
                con.setAutoCommit(false);
                rollback = 1;

                update0(recrypted, customizationNote, con);

                con.commit();
                rollback = 2;
            } catch (SQLException e) {
                throw MailAccountExceptionCodes.SQL_ERROR.create(e, e.getMessage());
            } catch (RuntimeException e) {
                throw MailAccountExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
            } finally {
                if (rollback > 0) {
                    if (rollback == 1) {
                        Databases.rollback(con);
                    }
                    Databases.autocommit(con);
                }
                Database.back(contextId, true, con);
            }
        }

        private void update0(String recrypted, GenericProperty customizationNote, Connection con) throws SQLException {
            PreparedStatement stmt = null;
            final Session session = customizationNote.session;
            final MailAccountStorageService service = ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class);
            if (null != service) {
                MailAccount mailAccount = null;
                try {
                    mailAccount = service.getMailAccount(customizationNote.accountId, session.getUserId(), session.getContextId());
                } catch (OXException e) {
                    LOG.warn("Could not update encrypted mail account password.", e);
                    return;
                }

                if (customizationNote.server != null) {
                    try {
                        if (customizationNote.server.equals(mailAccount.getMailServer())) {
                            stmt = con.prepareStatement("UPDATE user_mail_account SET password = ? WHERE cid = ? AND user = ? AND id = ?");
                            stmt.setString(1, recrypted);
                            stmt.setInt(2, session.getContextId());
                            stmt.setInt(3, session.getUserId());
                            stmt.setInt(4, customizationNote.accountId);
                            stmt.executeUpdate();
                            Databases.closeSQLStuff(stmt);
                        }

                        if (customizationNote.server.equals(mailAccount.getTransportServer())) {
                            stmt = con.prepareStatement("UPDATE user_transport_account SET password = ? WHERE cid = ? AND user = ? AND id = ? AND (password IS NOT NULL AND password <> '')");
                            stmt.setString(1, recrypted);
                            stmt.setInt(2, session.getContextId());
                            stmt.setInt(3, session.getUserId());
                            stmt.setInt(4, customizationNote.accountId);
                            stmt.executeUpdate();
                        }
                    } finally {
                        Databases.closeSQLStuff(stmt);
                    }

                    try {
                        service.invalidateMailAccount(customizationNote.accountId, session.getUserId(), session.getContextId());
                    } catch (Exception e) {
                        LOG.warn("Could not invalidate mail account after password update.", e);
                    }
                }
            }
        }
    };

    /**
     * Encrypts specified password with given key.
     *
     * @param password The password
     * @param key The key
     * @return The encrypted password as Base64 encoded string
     * @throws GeneralSecurityException If password encryption fails
     */
    public static String encrypt(String password, String key) throws GeneralSecurityException {
        return encrypt(password, generateSecretKey(key));
    }

    /**
     * Decrypts specified encrypted password with given key.
     *
     * @param encryptedPassword The Base64 encoded encrypted password
     * @param session The session
     * @return The decrypted password
     * @throws GeneralSecurityException If password decryption fails
     * @throws OXException
     */
    public static String decrypt(String encryptedPassword, Session session, int accountId, String login, String server) throws OXException {
        try {
            SecretEncryptionService<GenericProperty> encryptionService = ServerServiceRegistry.getInstance().getService(SecretEncryptionFactoryService.class).createService(STRATEGY);
            return encryptionService.decrypt(session, encryptedPassword, new GenericProperty(accountId, session, login, server));
        } catch (OXException e) {
            if (!SecretExceptionCodes.EMPTY_SECRET.equals(e)) {
                throw e;
            }
            // Apparently empty password
            OXException oxe = MailExceptionCode.CONFIG_ERROR.create(e, "The mail configuration is invalid. Please check \"com.openexchange.mail.passwordSource\" property or set a valid secret source in file 'secret.properties'.");
            oxe.setCategory(Category.CATEGORY_CONFIGURATION);
            throw oxe;
        }
    }

    /**
     * Decrypts specified encrypted password with given key.
     *
     * @param encryptedPassword The Base64 encoded encrypted password
     * @param key The key
     * @return The decrypted password
     * @throws GeneralSecurityException If password decryption fails
     */
    public static String decrypt(String encryptedPassword, String key) throws GeneralSecurityException {
        try {
            return decrypt(encryptedPassword, generateSecretKey(key));
        } catch (GeneralSecurityException e) {
            // Decrypting failed; retry with CryptoService
            final CryptoService crypto = ServerServiceRegistry.getInstance().getService(CryptoService.class);
            if (null == crypto) {
                LOG.warn("MailPasswordUtil.decrypt(): Missing {}", CryptoService.class.getSimpleName());
                throw e;
            }
            try {
                return crypto.decrypt(encryptedPassword, key);
            } catch (OXException ce) {
                // CryptoServce failed, too
                LOG.debug("MailPasswordUtil.decrypt(): Failed to decrypt \"{}\" with {}", encryptedPassword, CryptoService.class.getSimpleName(), ce);
            }
            throw e;
        }
    }

    /**
     * Encrypts specified password with given key.
     *
     * @param password The password to encrypt
     * @param key The key
     * @return The encrypted password as Base64 encoded string
     * @throws GeneralSecurityException If password encryption fails
     */
    public static String encrypt(String password, Key key) throws GeneralSecurityException {
        if (null == password || null == key) {
            return null;
        }
        final Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
        cipher.init(Cipher.ENCRYPT_MODE, key);

        final byte[] outputBytes = cipher.doFinal(password.getBytes(com.openexchange.java.Charsets.UTF_8));
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
        return Charsets.toAsciiString(org.apache.commons.codec.binary.Base64.encodeBase64(outputBytes));
    }

    /**
     * Decrypts specified encrypted password with given key.
     *
     * @param encryptedPassword The Base64 encoded encrypted password
     * @param key The key
     * @return The decrypted password
     * @throws GeneralSecurityException If password decryption fails
     */
    public static String decrypt(String encryptedPassword, Key key) throws GeneralSecurityException {
        if (null == encryptedPassword || null == key) {
            return null;
        }
        final byte encrypted[];
        {
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
            encrypted = org.apache.commons.codec.binary.Base64.decodeBase64(Charsets.toAsciiBytes(encryptedPassword));
        }

        final Cipher cipher = Cipher.getInstance(CIPHER_TYPE);
        cipher.init(Cipher.DECRYPT_MODE, key);

        final byte[] outputBytes = cipher.doFinal(encrypted);

        return new String(outputBytes, com.openexchange.java.Charsets.UTF_8);
    }

    /**
     * Create a key for use in the cipher code
     */
    public static Key generateRandomKey() throws NoSuchAlgorithmException {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM_DES);
        keyGenerator.init(CryptoUtil.getSecureRandom());
        final SecretKey secretKey = keyGenerator.generateKey();
        return secretKey;
    }

    /**
     * Generates a secret key from specified key string.
     *
     * @param key The key string
     * @return A secret key generated from specified key string
     */
    public static Key generateSecretKey(String key) {
        if (null == key) {
            return null;
        }
        return new SecretKeySpec(ensureLength(key.getBytes(com.openexchange.java.Charsets.UTF_8)), ALGORITHM_DES);
    }

    /**
     * Generates a secret key from specified bytes.
     *
     * @param bytes The bytes
     * @return A secret key generated from specified bytes
     */
    public static Key generateSecretKey(byte[] bytes) {
        if (null == bytes) {
            return null;
        }
        return new SecretKeySpec(ensureLength(bytes), ALGORITHM_DES);
    }

    private static byte[] ensureLength(byte[] bytes) {
        final byte[] keyBytes;
        final int len = bytes.length;
        if (len < KEY_LENGTH) {
            keyBytes = new byte[KEY_LENGTH];
            System.arraycopy(bytes, 0, keyBytes, 0, len);
            for (int i = len; i < keyBytes.length; i++) {
                keyBytes[i] = 48;
            }
        } else if (len > KEY_LENGTH) {
            keyBytes = new byte[KEY_LENGTH];
            System.arraycopy(bytes, 0, keyBytes, 0, keyBytes.length);
        } else {
            keyBytes = bytes;
        }
        return keyBytes;
    }

    /*-
     * Encode a secret key as a string that can be stored for later use.
     *
     * @param key
     * @return
    public static String encodeKey(Key key) {
        final BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(key.getEncoded());
    }
     */

    /*-
     * Reconstruct a secret key from a string representation.
     *
     * @param encodedKey
     * @return
     * @throws IOException
    public static Key decodeKey(String encodedKey) throws IOException {
        final BASE64Decoder decoder = new BASE64Decoder();
        final byte raw[] = decoder.decodeBuffer(encodedKey);
        final SecretKey key = new SecretKeySpec(raw, "DES");
        return key;
    }
     */

}
