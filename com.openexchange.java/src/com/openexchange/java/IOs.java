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

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link IOs} - A utility class for I/O associated processing.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public class IOs {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IOs.class);
    }

    /**
     * Initializes a new {@link IOs}.
     */
    private IOs() {
        super();
    }

    /**
     * Checks whether specified I/O exception can be considered as a connection reset.
     * <p>
     * A <code>"java.io.IOException: Connection reset by peer"</code> is thrown when the other side has abruptly aborted the connection in midst of a transaction.
     * <p>
     * That can have many causes which are not controllable from the Middleware side. E.g. the end-user decided to shutdown the client or change the
     * server abruptly while still interacting with your server, or the client program has crashed, or the enduser's Internet connection went down,
     * or the enduser's machine crashed, etc, etc.
     *
     * @param e The I/O exception to examine
     * @return <code>true</code> for a connection reset; otherwise <code>false</code>
     */
    public static boolean isConnectionReset(IOException e) {
        if (null == e) {
            return false;
        }

        String lcm = com.openexchange.java.Strings.asciiLowerCase(e.getMessage());
        if ("connection reset by peer".equals(lcm) || "broken pipe".equals(lcm)) {
            return true;
        }

        Throwable cause = e.getCause();
        return cause instanceof IOException ? isConnectionReset((IOException) cause) : false;
    }

    /**
     * Checks if cause of specified exception indicates an unexpected end of file or end of stream during reading input.
     *
     * @param e The exception to examine
     * @return <code>true</code> if an EOF problem is indicated; otherwise <code>false</code>
     */
    public static boolean isEOFException(Exception e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.io.EOFException.class);
    }

    /**
     * Checks if cause of specified exception indicates a connect problem.
     *
     * @param e The exception to examine
     * @return <code>true</code> if a connect problem is indicated; otherwise <code>false</code>
     */
    public static boolean isConnectException(Exception e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.ConnectException.class);
    }

    /**
     * Checks if cause of specified exception indicates a timeout or connect problem.
     *
     * @param e The exception to examine
     * @return <code>true</code> if a timeout or connect problem is indicated; otherwise <code>false</code>
     */
    public static boolean isTimeoutOrConnectException(Exception e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.SocketTimeoutException.class, java.net.ConnectException.class);
    }

    /**
     * Checks if cause of specified exception indicates a timeout problem.
     *
     * @param e The exception to examine
     * @return <code>true</code> if a timeout problem is indicated; otherwise <code>false</code>
     */
    public static boolean isTimeoutException(Exception e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.SocketTimeoutException.class);
    }

    /**
     * Checks if cause of specified exception indicates a no route to host problem.
     *
     * @param e The exception to examine
     * @return <code>true</code> if a no route to host problem is indicated; otherwise <code>false</code>
     */
    public static boolean isNoRouteToHostException(Exception e) {
        if (null == e) {
            return false;
        }

        return isEitherOf(e, java.net.NoRouteToHostException.class);
    }

    /**
     * Checks if cause of specified exception indicates an SSL hand-shake problem.
     *
     * @param e The exception to examine
     * @return <code>true</code> if an SSL hand-shake problem is indicated; otherwise <code>false</code>
     */
    public static boolean isSSLHandshakeException(Exception e) {
        return isEitherOf(e, javax.net.ssl.SSLHandshakeException.class);
    }

    @SafeVarargs
    private static boolean isEitherOf(Throwable e, Class<? extends Exception>... classes) {
        if (null == e || null == classes || 0 == classes.length) {
            return false;
        }

        for (Class<? extends Exception> clazz : classes) {
            if (clazz.isInstance(e)) {
                return true;
            }
        }

        Throwable next = e.getCause();
        return null == next ? false : isEitherOf(next, classes);
    }

    // ----------------------------------------------------- Connect socket ----------------------------------------------------------------

    /**
     * Encapsulates the connect invocation in order to retry connect attempt on a certain I/O error.
     */
    @FunctionalInterface
    public static interface Connector {

        /**
         * Performs the connect.
         *
         * @throws IOException If connect attempt fails
         */
        void connect() throws IOException;
    }

    /**
     * Invokes given connector's {@link Connector#connect() connect()} method.
     *
     * @param connector The connector to invoke
     * @throws IOException If an I/O error occurs
     */
    public static void connect(Connector connector) throws IOException {
        try {
            connector.connect();
        } catch (IOException e) {
            if (isEitherOf(e, java.net.NoRouteToHostException.class) == false) {
                throw e;
            }

            // Connect invocation failed due to 'java.net.NoRouteToHostException' that might be caused by stale cached DNS information.
            if (clearDnsCache() == false) {
                throw e;
            }

            // Try again after DNS cache has been cleared.
            connector.connect();
        }
    }

    // ----------------------------------------------------- Clear DNS cache ---------------------------------------------------------------

    /**
     * Clears the DNS cache.
     *
     * @return <code>true</code> if successfully cleared; otherwise <code>false</code>
     */
    public static boolean clearDnsCache() {
        String sVersion = System.getProperty("java.version");
        int version = getJavaVersion(sVersion);
        if (version == 8) {
            return clearDNSCacheForJava8();
        } else if (version == 17) {
            return clearDNSCacheForJava17();
        }
        LoggerHolder.LOG.warn("Failed to clear DNS cache. Unsupported Java version: {}", sVersion);
        return false;
    }

    private static boolean clearDNSCacheForJava8() {
        try {
            Field field = InetAddress.class.getDeclaredField("addressCache");
            field.setAccessible(true);
            Object obj = field.get(null);

            // Synchronized on addressCache
            synchronized (obj) {
                Field cacheField = obj.getClass().getDeclaredField("cache");
                cacheField.setAccessible(true);
                ((Map) cacheField.get(obj)).clear();
            }
            LoggerHolder.LOG.info("Cleared DNS cache for Java v8");
            return true;
        } catch (Exception e) {
            LoggerHolder.LOG.warn("Failed to clear DNS cache for Java v8", e);
        }
        return false;
    }

    private static boolean clearDNSCacheForJava17() {
        try {
            Field field = InetAddress.class.getDeclaredField("cache");
            field.setAccessible(true);
            Object obj = field.get(null);
            ((ConcurrentMap) obj).clear();
            LoggerHolder.LOG.info("Cleared DNS cache for Java v17");
            return true;
        } catch (Exception e) {
            LoggerHolder.LOG.warn("Failed to clear DNS cache for Java v17", e);
        }
        return false;
    }

    private static int getJavaVersion(String optVersion) {
        String version = (optVersion == null ? System.getProperty("java.version") : optVersion).trim();
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf('.');
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Strings.parsePositiveInt(version);
    }

}
