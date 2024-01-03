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

import com.google.common.cache.CacheLoader;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * {@link SecretKeyCacheLoader} - Generates a secret key from specified password string.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class SecretKeyCacheLoader extends CacheLoader<CryptoCacheKey, SecretKey> {

    private static final Logger LOG = LoggerFactory.getLogger(CryptoServiceImpl.class);

    /**
     * The algorithm.
     */
    private static final String ALGORITHM = "AES";

    /**
     * The key bytes for AES-256
     */
    private static final int KEY_BYTES = 256 / Bytes.SIZE;

    /**
     * Default constructor
     */
    public SecretKeyCacheLoader() {
        super();
    }

    @Override
    public SecretKey load(CryptoCacheKey cryptoCacheKey) throws Exception {
        byte[] hash = new byte[KEY_BYTES];
        long t0 = System.nanoTime();
        //@formatter:off
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_i).
                withSalt(cryptoCacheKey.getSalt()).
                withMemoryAsKB(cryptoCacheKey.getMemory()).
                withIterations(cryptoCacheKey.getIterations()).
                withParallelism(cryptoCacheKey.getLanes()).
                build();
        //@formatter:on
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        generator.generateBytes(cryptoCacheKey.getSecret().toCharArray(), hash);
        long t1 = System.nanoTime();
        long td = (t1 - t0) / 1000000;
        LOG.trace("generateSecretKey with memory: {}, iterations: {}, lanes: {} took {} ms.", cryptoCacheKey.getMemory(), cryptoCacheKey.getIterations(), cryptoCacheKey.getLanes(), Long.valueOf(td));
        return new SecretKeySpec(hash, ALGORITHM);
    }
}
