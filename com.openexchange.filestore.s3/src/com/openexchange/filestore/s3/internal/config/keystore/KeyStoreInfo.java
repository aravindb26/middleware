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

package com.openexchange.filestore.s3.internal.config.keystore;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * {@link KeyStoreInfo} is a simple wrapper around a {@link KeyStore} and a checksum
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class KeyStoreInfo {

    final KeyStore keystore;
    final byte[] md5;

    /**
     * Initializes a new {@link KeyStoreInfo}.
     *
     * @param store The {@link KeyStore}
     * @param md5 The md5 checksum
     */
    public KeyStoreInfo(KeyStore store, byte[] md5) {
        super();
        this.keystore = store;
        this.md5 = md5;
    }

    /**
     * Gets the keystore
     *
     * @return The keystore
     */
    public KeyStore getKeystore() {
        return keystore;
    }

    /**
     * Gets the md5 checksum of the keystore
     *
     * @return The checksum
     */
    public byte[] getMd5Sum() {
        return md5;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (md5 == null ? 0 : Arrays.hashCode(md5));
        result = prime * result + ((keystore == null) ? 0 : getHashSum(keystore));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof KeyStoreInfo)) {
            return false;
        }
        KeyStoreInfo other = (KeyStoreInfo) obj;
        if (md5 == null) {
            if (other.md5 != null) {
                return false;
            }
        } else if (false == Arrays.equals(md5, other.md5)) {
            return false;
        }
        if (keystore == null) {
            if (other.keystore != null) {
                return false;
            }
        } else if (0 != compare(keystore, other.keystore)) {
            return false;
        }
        return true;
    }

    /**
     * Calculates the hashCode for a KeyStore
     *
     * @param store The store to get the value for
     * @return The hashCode
     */
    private static int getHashSum(KeyStore store) {
        int hashCode = 0;
        if (null == store) {
            return hashCode;
        }
        hashCode += store.getType().hashCode();
        Enumeration<String> aliases;
        try {
            aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                String nextElement = aliases.nextElement();
                hashCode += nextElement.hashCode();
                Certificate certificate = store.getCertificate(nextElement);
                hashCode += certificate.hashCode();
            }
        } catch (KeyStoreException e) {
            throw new IllegalStateException("Not initialized", e);
        }

        return hashCode;
    }

    /**
     * Compares two key stores
     *
     * @param k1 The one {@link KeyStore}
     * @param k2 The other {@link KeyStore}
     * @return See {@link Comparable#compareTo(Object)}
     */
    private static int compare(KeyStore k1, KeyStore k2) {
        int hashSum = getHashSum(k1);
        int hashSum2 = getHashSum(k2);

        if (hashSum == hashSum2) {
            return 0;
        }

        return hashSum > hashSum2 ? 1 : -1;
    }
}
