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

package com.openexchange.imap.ping;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;
import org.jctools.maps.NonBlockingHashMap;
import com.openexchange.config.ConfigurationService;
import com.openexchange.imap.config.IIMAPProperties;
import com.openexchange.imap.services.Services;
import com.openexchange.imap.util.HostAndPort;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.imap.GreetingListener;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.util.MailLogger;

/**
 * {@link IMAPCapabilityAndGreetingCache} - A cache for CAPABILITY and greeting from IMAP servers.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class IMAPCapabilityAndGreetingCache {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(IMAPCapabilityAndGreetingCache.class);

    private static volatile Integer capabiltiesCacheIdleTime;
    private static int capabiltiesCacheIdleTime() {
        Integer tmp = capabiltiesCacheIdleTime;
        if (null == tmp) {
            synchronized (IMAPCapabilityAndGreetingCache.class) {
                tmp = capabiltiesCacheIdleTime;
                if (null == tmp) {
                    int defaultValue = 0; // Do not check again
                    ConfigurationService service = Services.getService(ConfigurationService.class);
                    if (null == service) {
                        return defaultValue;
                    }
                    tmp = Integer.valueOf(service.getIntProperty("com.openexchange.imap.capabiltiesCacheIdleTime", defaultValue));
                    capabiltiesCacheIdleTime = tmp;
                }
            }
        }
        return tmp.intValue();
    }

    private static final AtomicReference<ConcurrentMap<Key, Future<CapabilityAndGreeting>>> MAP_REF = new AtomicReference<>();

    /**
     * Initializes a new {@link IMAPCapabilityAndGreetingCache}.
     */
    private IMAPCapabilityAndGreetingCache() {
        super();
    }

    /**
     * Initializes this cache.
     */
    public static void init() {
        initElseGet();
    }

    private static ConcurrentMap<Key, Future<CapabilityAndGreeting>> initElseGet() {
        ConcurrentMap<Key, Future<CapabilityAndGreeting>> map = new NonBlockingHashMap<Key, Future<CapabilityAndGreeting>>();
        ConcurrentMap<Key, Future<CapabilityAndGreeting>> witness = MAP_REF.compareAndExchange(null, map);
        return witness == null ? map : witness;
    }

    /**
     * Tear-down for this cache.
     */
    public static void tearDown() {
        ConcurrentMap<Key, Future<CapabilityAndGreeting>> map = MAP_REF.getAndSet(null);
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Clears this cache.
     */
    public static void clear() {
        ConcurrentMap<Key, Future<CapabilityAndGreeting>> map = MAP_REF.get();
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Gets the cached greeting from IMAP server denoted by specified parameters.
     *
     * @param endpoint The IMAP server's end-point
     * @param isSecure Whether to establish a secure connection
     * @param imapProperties The IMAP properties
     * @param primary Whether considered IMAP end-point is the primary one or not
     * @return The greeting from IMAP server denoted by specified parameters
     * @throws IOException If an I/O error occurs
     */
    public static String getGreeting(HostAndPort endpoint, boolean isSecure, IIMAPProperties imapProperties, boolean primary) throws IOException {
        return getCapabilityAndGreeting(endpoint, isSecure, imapProperties, primary).getGreeting();
    }

    /**
     * Gets the cached capabilities from IMAP server denoted by specified parameters.
     *
     * @param endpoint The IMAP server's end-point
     * @param isSecure Whether to establish a secure connection
     * @param imapProperties The IMAP properties
     * @param primary Whether considered IMAP end-point is the primary one or not
     * @return The capabilities from IMAP server denoted by specified parameters
     * @throws IOException If an I/O error occurs
     */
    public static Map<String, String> getCapabilities(HostAndPort endpoint, boolean isSecure, IIMAPProperties imapProperties, boolean primary) throws IOException {
        return getCapabilityAndGreeting(endpoint, isSecure, imapProperties, primary).getCapability();
    }

    /**
     * Gets the cached capabilities & greeting from IMAP server denoted by specified parameters.
     *
     * @param endpoint The IMAP server's end-point
     * @param isSecure Whether to establish a secure connection
     * @param imapProperties The IMAP properties
     * @param primary Whether considered IMAP end-point is the primary one or not
     * @return The capabilities & greeting
     * @throws IOException If an I/O error occurs
     */
    public static CapabilityAndGreeting getCapabilityAndGreeting(HostAndPort endpoint, boolean isSecure, IIMAPProperties imapProperties, boolean primary) throws IOException {
        int idleTime = capabiltiesCacheIdleTime();
        if (idleTime < 0) {
            // Never cache
            FutureTask<CapabilityAndGreeting> ft = new FutureTask<CapabilityAndGreeting>(new CapabilityAndGreetingCallable(endpoint, isSecure, imapProperties, primary));
            ft.run();
            return getFrom(ft);
        }

        ConcurrentMap<Key, Future<CapabilityAndGreeting>> map = MAP_REF.get();
        if (null == map) {
            map = initElseGet();
        }

        boolean currentThreadInvokedRun = false;
        Key key = new Key(endpoint.getHost(), endpoint.getPort(), isSecure);
        Future<CapabilityAndGreeting> f = map.get(key);
        if (null == f) {
            FutureTask<CapabilityAndGreeting> ft = new FutureTask<CapabilityAndGreeting>(new CapabilityAndGreetingCallable(endpoint, isSecure, imapProperties, primary));
            f = map.putIfAbsent(key, ft);
            if (null == f) {
                f = ft;
                ft.run();
                currentThreadInvokedRun = true;
            }
        }

        try {
            CapabilityAndGreeting cag = getFrom(f);
            if (isElapsed(cag, idleTime)) {
                FutureTask<CapabilityAndGreeting> ft = new FutureTask<CapabilityAndGreeting>(new CapabilityAndGreetingCallable(endpoint, isSecure, imapProperties, primary));
                if (map.replace(key, f, ft)) {
                    f = ft;
                    ft.run();
                    currentThreadInvokedRun = true;
                } else {
                    f = map.get(key);
                }
                cag = getFrom(f);
            }

            if (currentThreadInvokedRun) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Retrieved greeting and capabilities from {}{}Greeting: {}{}Capabilities: {}", endpoint, Strings.getLineSeparator(), cag.getGreeting(), Strings.getLineSeparator(), cag.getCapability());
                } else {
                    LOG.info("Retrieved greeting and capabilities from {}", endpoint);
                }
            }
            return cag;
        } catch (IOException e) {
            if (currentThreadInvokedRun) {
                map.remove(key);
                LOG.warn("Failed to retrieve greeting and capabilities from {}", endpoint, e);
            }
            throw e;
        }
    }

    private static boolean isElapsed(CapabilityAndGreeting cag, int idleTime) {
        if (idleTime == 0) {
            return false; // never
        }
        // Check if elapsed
        return ((System.currentTimeMillis() - cag.getStamp()) > idleTime);
    }

    private static CapabilityAndGreeting getFrom(Future<CapabilityAndGreeting> f) throws IOException {
        try {
            return f.get();
        } catch (InterruptedException e) {
            // Keep interrupted status
            Thread.currentThread().interrupt();
            throw new IOException(e.getMessage());
        } catch (CancellationException e) {
            throw new IOException(e.getMessage());
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw ((IOException) cause);
            }
            if (cause instanceof ProtocolException) {
                throw new IOException("Encountered IMAP protocol exception", cause);
            }
            if (cause instanceof RuntimeException) {
                throw new IOException("Encountered unchecked exception", e);
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Not unchecked", cause);
        }
    }

    private static final class SimpleGreetingListener implements GreetingListener {

        private String greeting;

        SimpleGreetingListener() {
            super();
        }

        @Override
        public void onGreetingProcessed(String greeting, String host, int port) {
            this.greeting = greeting;
        }

        String getGreeting() {
            return greeting;
        }
    }

    private static final class CapabilityAndGreetingCallable implements Callable<CapabilityAndGreeting> {

        private final HostAndPort endpoint;
        private final boolean isSecure;
        private final IIMAPProperties imapProperties;
        private final boolean primary;

        CapabilityAndGreetingCallable(HostAndPort endpoint, boolean isSecure, IIMAPProperties imapProperties, boolean primary) {
            super();
            this.endpoint = endpoint;
            this.isSecure = isSecure;
            this.imapProperties = imapProperties;
            this.primary = primary;
        }

        @Override
        public CapabilityAndGreeting call() throws IOException, ProtocolException {
            SimpleGreetingListener greetingListener = new SimpleGreetingListener();
            IMAPProtocol imapProtocol = new IMAPProtocol("imap", endpoint.getHost(), endpoint.getPort(), "test", createImapProps(greetingListener), isSecure, createLogger());
            try {
                String greeting = greetingListener.getGreeting();
                imapProtocol.capability(true);
                Map<String, String> capabilities = imapProtocol.getCapabilities();
                return new CapabilityAndGreeting(capabilities, greeting);
            } finally {
                imapProtocol.disconnect();
            }
        }

        private MailLogger createLogger() {
            return new MailLogger(this.getClass(), "DEBUG IMAP", false, null);
        }

        private Properties createImapProps(GreetingListener greetingListener) {
            Properties imapProps = MimeDefaultSession.getDefaultMailProperties();
            {
                int connectionTimeout = imapProperties.getConnectTimeout();
                if (connectionTimeout > 0) {
                    imapProps.put("mail.imap.connectiontimeout", Integer.toString(connectionTimeout));
                }
            }
            {
                int timeout = imapProperties.getReadTimeout();
                if (timeout > 0) {
                    imapProps.put("mail.imap.timeout", Integer.toString(timeout));
                }
            }
            SSLSocketFactoryProvider factoryProvider = Services.getService(SSLSocketFactoryProvider.class);
            final String socketFactoryClass = factoryProvider.getDefault().getClass().getName();
            final String sPort = Integer.toString(endpoint.getPort());
            if (isSecure) {
                imapProps.put("mail.imap.socketFactory.class", socketFactoryClass);
                imapProps.put("mail.imap.socketFactory.port", sPort);
                imapProps.put("mail.imap.socketFactory.fallback", "false");
                applySslProtocols(imapProps);
                applySslCipherSuites(imapProps);
            } else {
                applyEnableTls(imapProps);
                applyRequireTls(imapProps);
                imapProps.put("mail.imap.socketFactory.port", sPort);
                imapProps.put("mail.imap.ssl.socketFactory.class", socketFactoryClass);
                imapProps.put("mail.imap.ssl.socketFactory.port", sPort);
                imapProps.put("mail.imap.socketFactory.fallback", "false");
                applySslProtocols(imapProps);
                applySslCipherSuites(imapProps);
            }
            if (primary) {
                imapProps.put("mail.imap.primary", "true");
            }
            {
                String authenc = imapProperties.getImapAuthEnc();
                if (Strings.isNotEmpty(authenc)) {
                    imapProps.put("mail.imap.login.encoding", authenc);
                }
            }
            imapProps.put("mail.imap.greeting.listeners", Collections.<GreetingListener> singletonList(greetingListener));
            return imapProps;
        }

        private void applyEnableTls(Properties imapprops) {
            boolean enableTls = imapProperties.isEnableTls();
            if (enableTls) {
                imapprops.put("mail.imap.starttls.enable", "true");
            }
        }

        private void applyRequireTls(Properties imapprops) {
            boolean requireTls = imapProperties.isRequireTls();
            if (requireTls) {
                imapprops.put("mail.imap.starttls.required", "true");
            }
        }

        private void applySslProtocols(Properties imapprops) {
            String sslProtocols = imapProperties.getSSLProtocols();
            if (Strings.isNotEmpty(sslProtocols)) {
                imapprops.put("mail.imap.ssl.protocols", sslProtocols);
            } else {
                SSLConfigurationService sslConfigService = Services.getService(SSLConfigurationService.class);
                if (sslConfigService != null) {
                    imapprops.put("mail.imap.ssl.protocols", Strings.toWhitespaceSeparatedList(sslConfigService.getSupportedProtocols()));
                }
            }
        }

        private void applySslCipherSuites(Properties imapprops) {
            String cipherSuites = imapProperties.getSSLCipherSuites();
            if (Strings.isNotEmpty(cipherSuites)) {
                imapprops.put("mail.imap.ssl.ciphersuites", cipherSuites);
            } else {
                SSLConfigurationService sslConfigService = Services.getService(SSLConfigurationService.class);
                if (sslConfigService != null) {
                    imapprops.put("mail.imap.ssl.ciphersuites", Strings.toWhitespaceSeparatedList(sslConfigService.getSupportedCipherSuites()));
                }
            }
        }
    }

    static InetSocketAddress toSocketAddress(HostAndPort endpoint) {
        if (null == endpoint) {
            return null;
        }
        int port = endpoint.getPort();
        if (port <= 0) {
            port = 143;
        }
        return new InetSocketAddress(endpoint.getHost(), port);
    }

    static ReadResult tryRead(PushbackInputStream in, Socket s) throws IOException {
        int newTimeout = 5000;
        int prevTimeout = s.getSoTimeout();
        if (prevTimeout <= newTimeout) {
            return tryRead(in);
        }

        s.setSoTimeout(newTimeout);
        try {
            return tryRead(in);
        } finally {
            // Restore timeout
            s.setSoTimeout(prevTimeout);
        }
    }

    private static ReadResult tryRead(PushbackInputStream in) throws IOException {
        try {
            int read = in.read();
            if (read == -1) {
                return ReadResult.END_OF_STREAM;
            }
            // Push byte back to stream & signal available byte
            in.unread(read);
            return ReadResult.BYTE_AVAILABLE;
        } catch (SocketTimeoutException e) {
            return ReadResult.NO_DATA;
        }
    }

    private static enum ReadResult {
        BYTE_AVAILABLE, END_OF_STREAM, NO_DATA;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * The capabilities & greeting information for an IMAP server (URL).
     */
    public static final class CapabilityAndGreeting {

        private final Map<String, String> capabilities;
        private final String greeting;
        private final long stamp;

        CapabilityAndGreeting(Map<String, String> capabilities, String greeting) {
            super();
            this.capabilities = capabilities;
            this.greeting = greeting;
            this.stamp = System.currentTimeMillis();
        }

        long getStamp() {
            return stamp;
        }

        /**
         * Gets the capabilities
         *
         * @return The capabilities
         */
        public Map<String, String> getCapability() {
            return capabilities;
        }

        /**
         * Gets the greeting
         *
         * @return The greeting
         */
        public String getGreeting() {
            return greeting;
        }

        @Override
        public int hashCode() {
            int prime = 31;
            int result = 1;
            result = prime * result + ((capabilities == null) ? 0 : capabilities.hashCode());
            result = prime * result + ((greeting == null) ? 0 : greeting.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof CapabilityAndGreeting)) {
                return false;
            }
            CapabilityAndGreeting other = (CapabilityAndGreeting) obj;
            if (capabilities == null) {
                if (other.capabilities != null) {
                    return false;
                }
            } else if (!capabilities.equals(other.capabilities)) {
                return false;
            }
            if (greeting == null) {
                if (other.greeting != null) {
                    return false;
                }
            } else if (!greeting.equals(other.greeting)) {
                return false;
            }
            return true;
        }
    }

    private static final class Key implements Comparable<Key> {

        final String host;
        final int port;
        final boolean secure;
        private final int hash;

        Key(String host, int port, boolean secure) {
            super();
            this.host = host;
            this.port = port;
            this.secure = secure;

            int prime = 31;
            int result = 1;
            result = prime * result + port;
            result = prime * result + (secure ? 1231 : 1237);
            hash = prime * result + ((host == null) ? 0 : host.hashCode());
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Key other = (Key) obj;
            if (port != other.port) {
                return false;
            }
            if (secure != other.secure) {
                return false;
            }
            if (host == null) {
                if (other.host != null) {
                    return false;
                }
            } else if (!host.equals(other.host)) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(Key other) {
            int c = Integer.compare(port, other.port);
            if (c == 0) {
                c = Boolean.compare(secure, other.secure);
            }
            if (c == 0) {
                c = host.compareTo(other.host);
            }
            return c;
        }

    }

}
