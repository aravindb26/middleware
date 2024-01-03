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

package com.openexchange.jsieve.export;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import com.openexchange.exception.OXException;
import com.openexchange.mail.utils.NetUtils;
import com.openexchange.mailfilter.services.Services;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;

/**
 * {@link SocketFetcher} - Utility class to get Sockets.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SocketFetcher {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SocketFetcher.class);

    private static final class PrivilegedActionImpl implements PrivilegedAction<Object> {

        private final org.slf4j.Logger logger;

        PrivilegedActionImpl(final org.slf4j.Logger logger) {
            super();
            this.logger = logger;
        }

        @Override
        public Object run() {
            ClassLoader cl = null;
            try {
                cl = Thread.currentThread().getContextClassLoader();
            } catch (SecurityException ex) {
                logger.error("", ex);
            }
            return cl;
        }
    }

    /**
     * Initializes a new {@link SocketFetcher}.
     */
    private SocketFetcher() {
        super();
    }

    /**
     * Start TLS on an existing socket. Supports the "STARTTLS" command in many protocols.
     */
    public static Socket startTLS(Socket socket, String host, String[] protocols) throws IOException {
        final int port = socket.getPort();
        try {
            // Get SSL socket factory
            SSLSocketFactoryProvider factoryProvider = Services.optService(SSLSocketFactoryProvider.class);
            if (null == factoryProvider) {
                throw new IllegalStateException("No " + SSLSocketFactoryProvider.class.getSimpleName() + " available. Bundle \"com.openexchange.net.ssl\" seems not to be started.");
            }

            // Create new socket layered over an existing socket connected to the named host, at the given port.
            SSLSocketFactory ssf = factoryProvider.getDefault();
            Socket newSocket = ssf.createSocket(socket, host, port, true);
            configureSSLSocket(newSocket, protocols);
            return newSocket;
        } catch (Exception ex) {
            if (ex instanceof InvocationTargetException ite) {
                Throwable t = ite.getTargetException();
                if (t instanceof Exception e) {
                    ex = e;
                }
            }
            if (ex instanceof IOException ioe) {
                throw ioe;
            }
            final StringBuilder err = new StringBuilder(256);
            err.append("Exception in startTLS using unknown socket factory: host, port: ");
            err.append(host).append(", ").append(port).append("; Exception: ").append(ex);
            // wrap anything else before sending it on
            throw new IOException(err.toString(), ex);
        }
    }

    /**
     * Gets a socket factory of the specified class.
     *
     * @param sfClass The socket factory class name
     * @return A socket factory of the specified class
     * @throws ClassNotFoundException If class cannot be found
     * @throws NoSuchMethodException If "getDefault()" does not exist in socket factory
     * @throws IllegalAccessException If "getDefault()" is not accessible
     * @throws InvocationTargetException If an error occurs on "getDefault()" invocation
     */
    public static SocketFactory getSocketFactory(final String sfClass) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (sfClass == null || sfClass.length() == 0) {
            return null;
        }

        // dynamically load the class

        ClassLoader cl = getContextClassLoader();
        Class<?> clsSockFact = null;
        if (cl != null) {
            try {
                clsSockFact = cl.loadClass(sfClass);
            } catch (ClassNotFoundException cex) {
                LOG.error("", cex);
            }
        }
        if (clsSockFact == null) {
            clsSockFact = Class.forName(sfClass);
        }
        // get & invoke the getDefault() method
        Method mthGetDefault = clsSockFact.getMethod("getDefault", new Class[] {});
        return (SocketFactory) mthGetDefault.invoke(new Object(), new Object[] {});
    }

    /**
     * Convenience method to get our context class loader. Assert any privileges we might have and then call the
     * Thread.getContextClassLoader method.
     */
    private static ClassLoader getContextClassLoader() {
        return (ClassLoader) AccessController.doPrivileged(new PrivilegedActionImpl(LOG));
    }

    /**
     * Configure the SSL options for the socket (if it's an SSL socket).
     * 
     * @throws OXException
     */
    private static void configureSSLSocket(Socket socket, String[] protocols) {
        if (socket instanceof SSLSocket sslSocket) {
            sslSocket.setEnabledProtocols(protocols == null || protocols.length <= 0 ? NetUtils.getProtocolsArray() : protocols);
        }
    }

}
