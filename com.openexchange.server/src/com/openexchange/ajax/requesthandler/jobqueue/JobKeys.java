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

package com.openexchange.ajax.requesthandler.jobqueue;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.openexchange.java.Charsets;

/**
 * {@link JobKeys} - Utility class for job keys.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class JobKeys {

    /**
     * Initializes a new {@link JobKeys}.
     */
    private JobKeys() {
        super();
    }

    private static final String ALGORITHM_MD5 = "MD5";
    private static final String ALGORITHM_SHA1 = "SHA1";

    /**
     * Gets the MD5 sum of passed arguments.
     *
     * @param args The arguments
     * @return The MD5 sum's hex representation
     */
    public static String getMD5Sum(final String... args) {
        if (null == args || 0 == args.length) {
            return "";
        }
        return getCheckSum(ALGORITHM_MD5, args);
    }

    /**
     * Gets the SHA1 sum of passed arguments.
     *
     * @param args The arguments
     * @return The SHA1 sum's hex representation
     */
    public static String getSHA1Sum(final String... args) {
        if (null == args || 0 == args.length) {
            return "";
        }
        return getCheckSum(ALGORITHM_SHA1, args);
    }

    private static String getCheckSum(final String algorithm, final String[] args) {
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            for (final String arg : args) {
                if (null != arg) {
                    md.update(arg.getBytes(Charsets.UTF_8));
                }
            }
            return asHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Turns array of bytes into string representing each byte as unsigned hex number.
     *
     * @param hash Array of bytes to convert to hex-string
     * @return Generated hex string
     */
    public static String asHex(final byte[] hash) {
        final int length = hash.length;
        final char[] buf = new char[length << 1];
        for (int i = 0, x = 0; i < length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }

}
