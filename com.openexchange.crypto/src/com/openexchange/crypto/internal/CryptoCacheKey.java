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

import static com.openexchange.java.Autoboxing.I;
import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * {@link CryptoCacheKey} - Defines a crypto cache key with secret and salt
 * <p/>
 * Primarily used for caching {@link javax.crypto.SecretKey}s
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class CryptoCacheKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 205251418966841810L;

    private final String secret;
    private final byte[] salt;
    private final int memory;
    private final int iterations;
    private final int lanes;
    private final int hashCode;

    /**
     * Default constructor
     *
     * @param secret The secret
     * @param salt The salt
     * @param memory the memory
     * @param iterations the iterations
     * @param lanes the lanes
     */
    private CryptoCacheKey(String secret, byte[] salt, int memory, int iterations, int lanes) {
        this.secret = secret;
        this.salt = salt;
        this.memory = memory;
        this.iterations = iterations;
        this.lanes = lanes;
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(salt);
        hashCode = prime * result + Objects.hash(I(iterations), I(lanes), I(memory), secret);
    }

    /**
     * Returns the secret
     *
     * @return The secret
     */
    public String getSecret() {
        return secret;
    }

    /**
     * Returns the salt
     *
     * @return the salt
     */
    public byte[] getSalt() {
        return salt;
    }

    /**
     * Returns the memory setting
     *
     * @return the memory setting
     */
    public int getMemory() {
        return memory;
    }

    /**
     * Returns the iterations setting
     *
     * @return the iterations setting
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * Returns the lanes setting
     *
     * @return the lanes setting
     */
    public int getLanes() {
        return lanes;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        CryptoCacheKey other = (CryptoCacheKey) obj;
        return iterations == other.iterations && lanes == other.lanes && memory == other.memory && Arrays.equals(salt, other.salt) && Objects.equals(secret, other.secret);
    }

    /**
     * Builder
     */
    public static final class CryptoCacheKeyBuilder {

        private String secret;
        private byte[] salt;

        private int memory;
        private int iterations;
        private int lanes;

        public CryptoCacheKeyBuilder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public CryptoCacheKeyBuilder salt(byte[] salt) {
            this.salt = salt;
            return this;
        }

        public CryptoCacheKeyBuilder memory(int memory) {
            this.memory = memory;
            return this;
        }

        public CryptoCacheKeyBuilder iterations(int iterations) {
            this.iterations = iterations;
            return this;
        }

        public CryptoCacheKeyBuilder lanes(int lanes) {
            this.lanes = lanes;
            return this;
        }

        public CryptoCacheKey build() {
            return new CryptoCacheKey(secret, salt, memory, iterations, lanes);
        }
    }
}
