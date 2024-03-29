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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.mozilla.universalchardet.UniversalDetector;

/**
 * {@link CharsetDetector} - A charset detector based on <a href="https://code.google.com/p/juniversalchardet/">juniversalchardet</a>
 * library, as included in Apache Tika bundle.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CharsetDetector {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CharsetDetector.class);

    private static final String FALLBACK = "ISO-8859-1";

    /**
     * Initializes a new {@link CharsetDetector}
     */
    private CharsetDetector() {
        super();
    }

    /**
     * Gets the fall-back charset name.
     *
     * @return The fall-back charset name
     */
    public static String getFallback() {
        return FALLBACK;
    }

    private static final ConcurrentMap<String, Boolean> VALIDITY_CACHE = new ConcurrentHashMap<>();

    /**
     * Convenience method to check if given name is valid; meaning not <code>null</code>, a legal charset name and supported as indicated by
     * {@link Charset#isSupported(String)}.
     *
     * @param charset The charset name whose validity shall be checked
     * @return <code>true</code> if given name is valid; otherwise <code>false</code>
     */
    public static boolean isValid(final String charset) {
        if (charset == null) {
            return false;
        }

        Boolean validityResult = VALIDITY_CACHE.get(charset);
        if (validityResult != null) {
            return validityResult.booleanValue();
        }

        // Not yet cached
        try {
            boolean valid = checkName(charset) && Charset.isSupported(charset);
            // No thread synchronization needed. Just perform put(); possibly overwriting concurrently computed result
            VALIDITY_CACHE.put(charset, Boolean.valueOf(valid));
            return valid;
        } catch (RuntimeException rte) {
            // Don't cache on error
            LOG.warn("RuntimeException while checking charset: {}", charset, rte);
            return false;
        } catch (Error e) {
            // Don't cache on error
            handleThrowable(e);
            LOG.warn("Error while checking charset: {}", charset, e);
            return false;
        } catch (Throwable t) {
            // Don't cache on error
            handleThrowable(t);
            LOG.warn("Unexpected error while checking charset: {}", charset, t);
            return false;
        }
    }

    /**
     * Checks whether the supplied <tt>Throwable</tt> is one that needs to be re-thrown and swallows all others.
     *
     * @param t The <tt>Throwable</tt> to check
     */
    private static void handleThrowable(final Throwable t) {
        if (t instanceof ThreadDeath) {
            LOG.error(" ---=== /!\\ ===--- Thread death ---=== /!\\ ===--- ", t);
            throw (ThreadDeath) t;
        }
        if (t instanceof VirtualMachineError) {
            LOG.error(
                " ---=== /!\\ ===--- The Java Virtual Machine is broken or has run out of resources necessary for it to continue operating. ---=== /!\\ ===--- ",
                t);
            throw (VirtualMachineError) t;
        }
        // All other instances of Throwable will be silently swallowed
    }

    /**
     * Checks that the given string is a legal charset name.
     *
     * @param s The charset name
     * @throws NullPointerException If given name is <code>null</code>
     * @return <code>true</code> if the given name is a legal charset name; otherwise <code>false</code>
     */
    public static boolean checkName(final String s) {
        if (s == null) {
            throw new NullPointerException("name is null");
        }
        final int n = s.length();
        if (n == 0) {
            return false;
        }
        boolean legal = true;
        for (int i = 0; legal && i < n; i++) {
            final char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                continue;
            }
            if (c >= 'a' && c <= 'z') {
                continue;
            }
            if (c >= '0' && c <= '9') {
                continue;
            }
            if (c == '-') {
                continue;
            }
            if (c == ':') {
                continue;
            }
            if (c == '_') {
                continue;
            }
            if (c == '.') {
                continue;
            }
            legal = false;
        }
        return legal;
    }

    /**
     * Detects the charset of specified byte array.
     *
     * @param in The byte array to examine
     * @throws NullPointerException If byte array is <code>null</code>
     * @return The detected charset or <i>US-ASCII</i> if no matching/supported charset could be found
     */
    public static String detectCharset(final byte[] in) {
        return detectCharset(in, in.length);
    }

    /**
     * Detects the charset of specified byte array.
     *
     * @param in The byte array to examine
     * @param len The bytes length
     * @throws NullPointerException If byte array is <code>null</code>
     * @return The detected charset or <i>US-ASCII</i> if no matching/supported charset could be found
     */
    public static String detectCharset(final byte[] in, final int len) {
        return detectCharset(in, 0, len);
    }

    /**
     * Detects the charset of specified byte array.
     *
     * @param in The byte array to examine
     * @param off The offset to start from
     * @param len The bytes length
     * @throws NullPointerException If byte array is <code>null</code>
     * @return The detected charset or <i>US-ASCII</i> if no matching/supported charset could be found
     */
    public static String detectCharset(final byte[] in, final int off, final int len) {
        if (null == in) {
            throw new NullPointerException("byte array input stream is null");
        }
        UniversalDetector detector = new UniversalDetector(null);
        detector.handleData(in, off, len);
        detector.dataEnd();
        return getResultingCharset(detector);
    }

    /**
     * Detects the charset of specified byte array input stream's data.
     * <p>
     * <b>Note</b>: Specified input stream is going to be closed in this method.
     *
     * @param in The byte array input stream to examine
     * @throws NullPointerException If input stream is <code>null</code>
     * @return The detected charset or <i>US-ASCII</i> if no matching/supported charset could be found
     */
    public static String detectCharset(final ByteArrayInputStream in) {
        return detectCharset((InputStream)in);
    }

    /**
     * Detects the charset of specified input stream's data.
     * <p>
     * <b>Note</b>: Specified input stream is going to be closed in this method.
     *
     * @param in The input stream to examine
     * @throws NullPointerException If input stream is <code>null</code>
     * @return The detected charset or <i>US-ASCII</i> if no matching/supported charset could be found
     */
    public static String detectCharset(final InputStream in) {
        return detectCharset(in, getFallback(), true);
    }

    /**
     * Detects the charset of specified input stream's data.
     *
     * @param in The input stream to examine
     * @param fallback The fallback charset to return if detection was not successful
     * @param close <code>true</code> to close the input stream after detection, <code>false</code>, otherwise
     * @throws NullPointerException If input stream is <code>null</code>
     * @return The detected charset or <i>US-ASCII</i> if no matching/supported charset could be found
     */
    public static String detectCharset(final InputStream in, String fallback, boolean close) {
        return detectCharsetFailOnError(in, fallback, close);
    }

    /**
     * Detects the charset of specified input stream's data.
     * <p>
     * <b>Note</b>: Specified input stream is going to be closed in this method.
     *
     * @param in The input stream to examine
     * @throws NullPointerException If input stream is <code>null</code>
     * @return The detected charset or <i>US-ASCII</i> if no matching/supported charset could be found
     */
    public static String detectCharsetFailOnError(final InputStream in) {
        return detectCharsetFailOnError(in, getFallback(), true);
    }

    /**
     * Detects the charset of specified input stream's data.
     *
     * @param in The input stream to examine
     * @param fallback The fallback charset to return if detection was not successful
     * @param close <code>true</code> to close the input stream after detection, <code>false</code>, otherwise
     * @throws NullPointerException If input stream is <code>null</code>
     * @return The detected charset or the supplied fallback if no matching/supported charset could be found
     */
    public static String detectCharsetFailOnError(final InputStream in, String fallback, boolean close) {
        if (null == in) {
            throw new NullPointerException("input stream is null");
        }
        UniversalDetector detector = new UniversalDetector(null);
        try {
            byte[] buffer = new byte[4096];
            int read;
            while (0 < (read = in.read(buffer)) && false == detector.isDone()) {
                detector.handleData(buffer, 0, read);
            }
        } catch (IOException e) {
            LOG.warn("", e);
        } finally {
            if (close) {
                Streams.close(in);
            }
        }
        detector.dataEnd();
        return getResultingCharset(detector, fallback);
    }

    /**
     * Gets the resulting charset from given detector instance; falls-back to <code>"ISO-8859-1"</code> if detector yields no result.
     *
     * @param detector The detector to get from
     * @return The name of the detected charset or <code>"ISO-8859-1"</code> as fall-back
     */
    public static String getResultingCharset(UniversalDetector detector) {
        return getResultingCharset(detector, FALLBACK);
    }

    /**
     * Gets the resulting charset from given detector instance; falls-back to <code>fallback</code> parameter if detector yields no result.
     *
     * @param detector The detector to get from
     * @param fallback The name of the fall-back charset to return if detector yields no result
     * @return The name of the detected charset or given fall-back name
     */
    private static String getResultingCharset(UniversalDetector detector, String fallback) {
        String detectedCharset = detector.getDetectedCharset();
        if (null == detectedCharset || false == isValid(detectedCharset)) {
            return Strings.isEmpty(fallback) ? FALLBACK : fallback;
        }
        return detectedCharset;
    }

}
