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
import static com.openexchange.crypto.CryptoErrorMessage.SecurityException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Base64;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.bouncycastle.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.hash.Hashing;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.DefaultInterests;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.crypto.CryptoErrorMessage;
import com.openexchange.crypto.CryptoProperty;
import com.openexchange.crypto.CryptoService;
import com.openexchange.crypto.EncryptedData;
import com.openexchange.crypto.internal.CryptoCacheKey.CryptoCacheKeyBuilder;
import com.openexchange.exception.OXException;
import com.openexchange.java.Charsets;
import com.openexchange.java.CryptoUtil;

/**
 * This Service provides methods for encrypting and decrypting of Strings.
 * <p/>
 * This services encrypts data and adds to each encrypted set a random IV ({@value IV_LENGTH} bytes)
 * and salt ({@value SALT_LENGTH} bytes), as well as the hashing settings, which are returned by the encrypt methods.
 * <p/>
 * The produced encrypted strings have the following form:
 * <pre>
 * IV$SALT$MEMORY$ITERATIONS$LANES$DATA
 * </pre>
 * The IV and salt are randomly generated and prefix the encrypted data. The hashing settings memory, iterations and lanes
 * correspond to the Argon2 settings. All parts except from the argon2 settings, i.e. the IV, salt, and data
 * are BASE64 encoded and returned as such by the encrypt methods. The separator {@value SEPARATOR} is used to
 * distinguish each individual part and is NOT part of the BASE64 encoding process.
 * <p/>
 * Warning: Do not change the parameters (IV, algorithms, ...) in productive environment,
 * because decryption of former encrypted data will be impossible.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class CryptoServiceImpl implements CryptoService, Reloadable {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoServiceImpl.class);
    private static final Charset UTF_8 = Charsets.UTF_8;
    private static final char SEPARATOR = '$';

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
     * IV Length 96 bits.
     */
    private static final short IV_LENGTH = 96 / Bytes.SIZE;

    /**
     * Salt length 128 bits.
     */
    private static final short SALT_LENGTH = 128 / Bytes.SIZE;

    private final LoadingCache<CryptoCacheKey, SecretKey> keyCache = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build(new SecretKeyCacheLoader());
    private final LeanConfigurationService leanConfigService;

    /**
     * Initializes a new {@link CryptoServiceImpl}.
     */
    public CryptoServiceImpl(LeanConfigurationService leanConfigService) {
        super();
        this.leanConfigService = leanConfigService;
    }

    @Override
    public String encrypt(String data, String password) throws OXException {
        byte[] randomIV = CryptoUtil.generateRandomBytes(IV_LENGTH);
        byte[] randomSalt = CryptoUtil.generateRandomBytes(SALT_LENGTH);
        try {
            CryptoCacheKey cacheKey = createCryptoCacheKey(password, randomSalt);
            String encryptedData = doEncrypt(data, keyCache.get(cacheKey), randomIV);
            String base64IV = Base64.getEncoder().encodeToString(randomIV);
            String base64Salt = Base64.getEncoder().encodeToString(randomSalt);
            return base64IV + SEPARATOR + base64Salt + SEPARATOR + cacheKey.getMemory() + SEPARATOR + cacheKey.getIterations() + SEPARATOR + cacheKey.getLanes() + SEPARATOR + encryptedData;
        } catch (ExecutionException e) {
            throw CryptoErrorMessage.IOError.create(e);
        }
    }

    @Override
    public String decrypt(String encryptedData, String password) throws OXException {
        String[] split = encryptedData.split("\\" + SEPARATOR);
        if (split.length == 1) {
            // Check for malformed encrypted data
            if (encryptedData.getBytes(StandardCharsets.US_ASCII).length < 2) {
                LOG.debug("Data is too short to be decrypted");
                throw CryptoErrorMessage.SecurityException.create();
            }
            try {
                java.util.Base64.getDecoder().decode(encryptedData);
            } catch (IllegalArgumentException e) {
                throw CryptoErrorMessage.MalformedEncryptedData.create(e);
            }

            // Just legacy mechanism
            throw CryptoErrorMessage.LegacyEncryption.create();
        }
        if (split.length != 6) {
            throw CryptoErrorMessage.MalformedEncryptedData.create();
        }
        byte[] iv = Base64.getDecoder().decode(split[0]);
        byte[] salt = Base64.getDecoder().decode(split[1]);
        int memory = parseInt(split[2]);
        int iterations = parseInt(split[3]);
        int lanes = parseInt(split[4]);
        try {
            return doDecrypt(split[5], keyCache.get(createCryptoCacheKey(password, salt, memory, iterations, lanes)), iv);
        } catch (ExecutionException e) {
            throw CryptoErrorMessage.IOError.create(e);
        }
    }

    @Override
    @Deprecated
    public EncryptedData encrypt(String data, String password, boolean useSalt) throws OXException {
        return LegacyCrypto.encrypt(data, password, useSalt);
    }

    @Override
    @Deprecated
    public String decrypt(EncryptedData data, String password, boolean useSalt) throws OXException {
        return LegacyCrypto.decrypt(data, password, useSalt);
    }

    @Override
    public String encrypt(String data, Key password) throws OXException {
        return LegacyCrypto.encrypt(data, password);
    }

    @Override
    public String decrypt(String encryptedData, Key key) throws OXException {
        return LegacyCrypto.decrypt(encryptedData, key);
    }

    @Override
    public CipherInputStream encryptingStreamFor(InputStream in, Key key) throws OXException {
        return LegacyCrypto.encryptingStreamFor(in, key);
    }

    @Override
    public CipherInputStream decryptingStreamFor(InputStream in, Key key) throws OXException {
        return LegacyCrypto.decryptingStreamFor(in, key);
    }

    @Override
    public Interests getInterests() {
        String[] propertyNames = new String[CryptoProperty.values().length];
        int index = 0;
        for (CryptoProperty property : CryptoProperty.values()) {
            propertyNames[index++] = property.getFQPropertyName();
        }
        return DefaultInterests.builder().propertiesOfInterest(propertyNames).build();
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        keyCache.invalidateAll();
    }

    /**
     * Encrypts the specified data with the password and the IV
     *
     * @param data The data to encrypt
     * @param password the password
     * @param iv The initialisation vector
     * @return The encrypted data
     * @throws OXException if a security error is occurred
     */
    private static String doEncrypt(String data, Key password, byte[] iv) throws OXException {
        try {
            Cipher cipher = CryptoUtil.initCipher(Cipher.ENCRYPT_MODE, CIPHER_TYPE, password, new GCMParameterSpec(GCM_TAG_BITS, iv));
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
            return Base64.getEncoder().encodeToString(outputBytes);
        } catch (GeneralSecurityException e) {
            throw SecurityException.create(e);
        }
    }

    /**
     * Performs the decryption. If decryption fails with the current algorithm a {@link GeneralSecurityException}
     * will be raised, to signalise that. Consumers should try decrypting with the legacy method in {@link LegacyCrypto}.
     *
     * @param encryptedData the encrypted data
     * @param key the key
     * @return The decrypted data
     * @throws OXException If the cipher fails initialisation or the encrypted data is not in BASE-64
     */
    private static String doDecrypt(String encryptedData, Key key, byte[] iv) throws OXException {
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
            encrypted = Base64.getDecoder().decode(encryptedData);
        } catch (IllegalArgumentException e) {
            // No valid base64 scheme
            throw SecurityException.create(e);
        }

        try {
            Cipher cipher = CryptoUtil.initCipher(Cipher.DECRYPT_MODE, CIPHER_TYPE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] outputBytes = cipher.doFinal(encrypted);
            return new String(outputBytes, UTF_8);
        } catch (GeneralSecurityException e) {
            throw BadPassword.create(e);
        }
    }

    private static int parseInt(String value) throws OXException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw CryptoErrorMessage.MalformedEncryptedData.create(e);
        }
    }

    /**
     * Creates a crypto cache key with the specified password and salt, and from
     * the lean config memory/iterations/lanes settings
     *
     * @param password the password
     * @param salt The salt
     * @return The new crypto cache key
     */
    private CryptoCacheKey createCryptoCacheKey(String password, byte[] salt) {
        int memory = leanConfigService.getIntProperty(CryptoProperty.memory);
        int iterations = leanConfigService.getIntProperty(CryptoProperty.iterations);
        int lanes = leanConfigService.getIntProperty(CryptoProperty.lanes);
        return createCryptoCacheKey(password, salt, memory, iterations, lanes);
    }

    /**
     * Creates a new crypto cache key with the specified settings
     *
     * @param password The password
     * @param salt The salt
     * @param memory the memory
     * @param iterations the iterations
     * @param lanes the lanes
     * @return The new crypto cache key
     */
    private CryptoCacheKey createCryptoCacheKey(String password, byte[] salt, int memory, int iterations, int lanes) {
        return new CryptoCacheKeyBuilder().secret(Hashing.murmur3_128().hashBytes(password.getBytes()).toString()).salt(salt).memory(memory).iterations(iterations).lanes(lanes).build();
    }
}
