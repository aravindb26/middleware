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

package com.openexchange.java;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * {@link CryptoUtil}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public final class CryptoUtil {

    private static final String RANDOM = "SECURE_RANDOM";
    private static final long RANDOM_TIMEOUT = 30;
    private static final Cache<String, SecureRandom> RANDOM_CACHE = CacheBuilder.newBuilder().expireAfterAccess(RANDOM_TIMEOUT, TimeUnit.MINUTES).build();

    /**
     * Get the instance of {@link SecureRandom}
     *
     * @return The random instance
     * @throws IllegalStateException If the secure random cannot be retrieved
     */
    public static SecureRandom getSecureRandom() {
        try {
            return RANDOM_CACHE.get(RANDOM, SecureRandom::new);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to fetch a secure random instance.", e);
        }
    }

    /**
     * Generates a random amount of bytes
     *
     * @param amountOfBytes The amount of random bytes to generate
     * @return The generated random bytes
     * @throws IllegalStateException if the random bytes cannot be generated
     */
    public static byte[] generateRandomBytes(int amountOfBytes) {
        try {
            SecureRandom rand = RANDOM_CACHE.get(RANDOM, SecureRandom::new);
            byte[] salt = new byte[amountOfBytes];
            rand.nextBytes(salt);
            return salt;
        } catch (ExecutionException e) {
            throw new IllegalStateException("Unable to fetch a secure random instance.", e);
        }
    }

    /**
     * Initialises the cipher
     *
     * @param mode The cipher mode ({@link Cipher#ENCRYPT_MODE} or {@link Cipher#DECRYPT_MODE})
     * @param cipherType The cipher type
     * @param key the key
     * @param spec the parameter specifications (such as the IV, etc.)
     * @return The initialised cipher
     * @throws NoSuchPaddingException if transformation contains a padding scheme that is not available
     * @throws NoSuchAlgorithmException if transformation is null, empty, in an invalid format, or if no Provider supports a CipherSpi implementation for the specified algorithm
     * @throws InvalidAlgorithmParameterException if the given algorithm parameters are inappropriate for this cipher,
     * or this cipher requires algorithm parameters and params is null, or the given algorithm parameters imply a cryptographic
     * strength that would exceed the legal limits (as determined from the configured jurisdiction policy files).
     * @throws InvalidKeyException if the given key is inappropriate for initializing this cipher, or its key-size exceeds
     * the maximum allowable key-size (as determined from the configured jurisdiction policy files).
     */
    public static Cipher initCipher(int mode, String cipherType, Key key, AlgorithmParameterSpec spec) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(cipherType);
        cipher.init(mode, key, spec);
        return cipher;
    }
}
