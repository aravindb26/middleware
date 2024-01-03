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

package com.openexchange.mail.autoconfig.tools;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.mail.autoconfig.sources.Guess.PROP_GENERAL_CONTEXT_ID;
import static com.openexchange.mail.autoconfig.sources.Guess.PROP_GENERAL_USER_ID;
import static com.openexchange.mail.autoconfig.sources.Guess.PROP_SMTP_AUTH_SUPPORTED;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import javax.mail.AuthenticationFailedException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.config.cascade.ConfigViews;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.MimeMailExceptionCode;
import com.openexchange.mail.utils.NetUtils;
import com.openexchange.mailaccount.utils.MailAccountUtils;
import com.openexchange.net.ssl.SSLSocketFactoryProvider;
import com.openexchange.net.ssl.config.SSLConfigurationService;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.pop3.POP3Store;
import com.sun.mail.smtp.SMTPTransport;
import com.sun.mail.util.SocketFetcher;

/**
 * {@link MailValidator}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class MailValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailValidator.class);

    private static final int DEFAULT_CONNECT_TIMEOUT = 1000;
    private static final int DEFAULT_TIMEOUT = 10000;

    private static String lookUpProperty(String propName, String defaultValue, UserAndContext optUserAndCtx) {
        if (optUserAndCtx != null) {
            ConfigViewFactory viewFactory = Services.getService(ConfigViewFactory.class);
            if (viewFactory != null) {
                try {
                    ConfigView view = viewFactory.getView(optUserAndCtx.getUserId(), optUserAndCtx.getContextId());
                    String value = ConfigViews.getNonEmptyPropertyFrom(propName, view);
                    if (value != null) {
                        return value;
                    }
                } catch (OXException e) {
                    LOGGER.error("Failed to query property {} from config-cascade for user {} in context {}", propName, I(optUserAndCtx.getUserId()), I(optUserAndCtx.getContextId()), e);
                }
            }
        }

        ConfigurationService configuration = Services.getService(ConfigurationService.class);
        return configuration == null ? defaultValue : configuration.getNonEmptyProperty(propName, defaultValue);
    }

    private static String lookUpProperty(PropName propName, String defaultValue, UserAndContext optUserAndCtx) {
        String value = null;
        if (propName.optPrimaryName() != null) {
            value = lookUpProperty(propName.optPrimaryName(), null, optUserAndCtx);
        }
        if (value == null) {
            value = lookUpProperty(propName.getName(), defaultValue, optUserAndCtx);
        }
        return Strings.isNotEmpty(value) ? value.trim() : value;
    }

    private static boolean lookUpBooleanProperty(PropName propName, boolean defaultValue, UserAndContext optUserAndCtx) {
        String value = lookUpProperty(propName, null, optUserAndCtx);
        return Strings.isNotEmpty(value) ? Boolean.parseBoolean(value.trim()) : defaultValue;
    }

    private static void putSSLConfig(String name, Properties props, boolean primary, UserAndContext optUserAndCtx) {
        SSLConfigurationService sslConfigService = Services.getService(SSLConfigurationService.class);

        // Set SSL protocols
        {
            String defaultValue = sslConfigService == null ? NetUtils.getProtocolsListing() : Strings.toWhitespaceSeparatedList(sslConfigService.getSupportedProtocols());
            PropName propName = new PropName("com.openexchange." + name + ".ssl.protocols", primary ? "com.openexchange." + name + ".primary.ssl.protocols" : null);
            String sslProtocols = lookUpProperty(propName, defaultValue, optUserAndCtx);
            if (Strings.isNotEmpty(sslProtocols)) {
                props.put("mail." + name + ".ssl.protocols", sslProtocols.trim());
            }
        }

        // Set cipher suites
        {
            String defaultValue = sslConfigService == null ? "" : Strings.toWhitespaceSeparatedList(sslConfigService.getSupportedCipherSuites());
            PropName propName = new PropName("com.openexchange." + name + ".ssl.ciphersuites", primary ? "com.openexchange." + name + ".primary.ssl.ciphersuites" : null);
            String cipherSuites = lookUpProperty(propName, defaultValue, optUserAndCtx);
            if (Strings.isNotEmpty(cipherSuites)) {
                props.put("mail." + name + ".ssl.ciphersuites", cipherSuites.trim());
            }
        }
    }

    /**
     * Validates for successful authentication against specified IMAP server.
     *
     * @param host The IMAP host
     * @param port The IMAP port
     * @param connectMode The connect mode to use
     * @param user The login
     * @param pwd The password
     * @param optProperties The properties or <code>null</code>
     * @return The authentication result
     */
    public static AuthenticationResult validateImapAuthentication(String host, int port, ConnectMode connectMode, String user, String pwd, Map<String, Object> optProperties) {
        UserAndContext uac = null;
        boolean primary = false;
        if (optProperties != null) {
            Integer contextId = (Integer) optProperties.get(PROP_GENERAL_CONTEXT_ID);
            Integer userId = (Integer) optProperties.get(PROP_GENERAL_USER_ID);
            if (contextId != null && userId != null) {
                uac = new UserAndContext(userId.intValue(), contextId.intValue());
                if (Utils.isPrimaryImapAccount(host, port, userId.intValue(), contextId.intValue())) {
                    primary = true;
                }
            }
        }

        ConnectMode usedConnectMode = connectMode;
        Store store = null;
        try {
            SSLSocketFactoryProvider factoryProvider = Services.getService(SSLSocketFactoryProvider.class);
            String socketFactoryClass = factoryProvider.getDefault().getClass().getName();
            Properties props = MimeDefaultSession.getDefaultMailProperties();
            if (ConnectMode.SSL == connectMode) {
                props.put("mail.imap.socketFactory.class", socketFactoryClass);
            } else if (ConnectMode.STARTTLS == connectMode) {
                props.put("mail.imap.ssl.socketFactory.class", socketFactoryClass);
                props.put("mail.imap.ssl.socketFactory.port", I(port));
                props.put("mail.imap.starttls.required", Boolean.TRUE);
                props.put("mail.imap.ssl.trust", "*");
            } else {
                props.put("mail.imap.ssl.socketFactory.class", socketFactoryClass);
                props.put("mail.imap.ssl.socketFactory.port", I(port));
                {
                    PropName propName = new PropName("com.openexchange.imap.enableTls", primary ? "com.openexchange.imap.primary.enableTls" : null);
                    boolean enableTls = lookUpBooleanProperty(propName, true, uac);
                    if (enableTls) {
                        props.put("mail.imap.starttls.enable", Boolean.TRUE);
                        props.put("mail.imap.ssl.trust", "*");
                    }
                }
            }
            putSSLConfig("imap", props, primary, uac);
            props.put("mail.imap.socketFactory.fallback", "false");
            props.put("mail.imap.connectiontimeout", I(DEFAULT_CONNECT_TIMEOUT));
            props.put("mail.imap.timeout", I(DEFAULT_TIMEOUT));
            props.put("mail.imap.socketFactory.port", I(port));
            {
                String defaultValue = "UTF-8";
                PropName propName = new PropName("com.openexchange.imap.imapAuthEnc", primary ? "com.openexchange.imap.primary.imapAuthEnc" : null);
                String authenc = lookUpProperty(propName, defaultValue, uac);
                if (Strings.isNotEmpty(authenc)) {
                    props.put("mail.imap.login.encoding", authenc.trim());
                }
            }
            if (primary) {
                props.put("mail.imap.primary", "true");
            }

            Session session = Session.getInstance(props, null);
            store = session.getStore("imap");
            store.connect(host, port, user, pwd);
            usedConnectMode = (ConnectMode.SSL == connectMode) ? connectMode : ((IMAPStore) store).isSSL() ? ConnectMode.STARTTLS : connectMode;
            closeSafe(store);
            store = null;

            AuthenticationResult result = AuthenticationResult.success(user, pwd, host, port, usedConnectMode);
            LOGGER.debug("Successful IMAP authentication with: {}", result);
            return result;
        } catch (AuthenticationFailedException e) {
            LOGGER.debug("Failed IMAP authentication for login at {}://{}:{}", user, ConnectMode.SSL == connectMode ? "imaps" : "imap", host, Integer.valueOf(port), e);
            return AuthenticationResult.failedAuthentication(MimeMailExceptionCode.INVALID_CREDENTIALS.create(e, host, e.getMessage()));
        } catch (Exception e) {
            LOGGER.debug("Error during IMAP authentication for login at {}://{}:{}", user, ConnectMode.SSL == connectMode ? "imaps" : "imap", host, Integer.valueOf(port), e);
            return AuthenticationResult.error();
        } finally {
            closeSafe(store);
        }
    }

    /**
     * Validates for successful authentication against specified POP3 server.
     *
     * @param host The POP3 host
     * @param port The POP3 port
     * @param connectMode The connect mode to use
     * @param user The login
     * @param pwd The password
     * @param optProperties The properties or <code>null</code>
     * @return The authentication result
     */
    public static AuthenticationResult validatePop3Authentication(String host, int port, ConnectMode connectMode, String user, String pwd, Map<String, Object> optProperties) {
        UserAndContext uac = null;
        boolean primary = false;
        if (optProperties != null) {
            Integer contextId = (Integer) optProperties.get(PROP_GENERAL_CONTEXT_ID);
            Integer userId = (Integer) optProperties.get(PROP_GENERAL_USER_ID);
            if (contextId != null && userId != null) {
                uac = new UserAndContext(userId.intValue(), contextId.intValue());
                if (Utils.isPrimaryImapAccount(host, port, userId.intValue(), contextId.intValue())) {
                    primary = true;
                }
            }
        }

        ConnectMode usedConnectMode = connectMode;
        Store store = null;
        try {
            Properties props = MimeDefaultSession.getDefaultMailProperties();
            SSLSocketFactoryProvider factoryProvider = Services.getService(SSLSocketFactoryProvider.class);
            String socketFactoryClass = factoryProvider.getDefault().getClass().getName();
            if (ConnectMode.SSL == connectMode) {
                props.put("mail.pop3.socketFactory.class", socketFactoryClass);
            } else if (ConnectMode.STARTTLS == connectMode) {
                props.put("mail.pop3.ssl.socketFactory.class", socketFactoryClass);
                props.put("mail.pop3.ssl.socketFactory.port", I(port));
                props.put("mail.pop3.starttls.required", Boolean.TRUE);
                props.put("mail.pop3.ssl.trust", "*");
            } else {
                props.put("mail.pop3.ssl.socketFactory.class", socketFactoryClass);
                props.put("mail.pop3.ssl.socketFactory.port", I(port));
                {
                    PropName propName = new PropName("com.openexchange.pop3.enableTls", primary ? "com.openexchange.pop3.primary.enableTls" : null);
                    boolean enableTls = lookUpBooleanProperty(propName, true, uac);
                    if (enableTls) {
                        props.put("mail.pop3.starttls.enable", Boolean.TRUE);
                        props.put("mail.pop3.ssl.trust", "*");
                    }
                }
            }
            putSSLConfig("pop3", props, primary, uac);
            props.put("mail.pop3.socketFactory.fallback", "false");
            props.put("mail.pop3.socketFactory.port", I(port));
            props.put("mail.pop3.connectiontimeout", I(DEFAULT_CONNECT_TIMEOUT));
            props.put("mail.pop3.timeout", I(DEFAULT_TIMEOUT));
            Session session = Session.getInstance(props, null);
            store = session.getStore("pop3");
            store.connect(host, port, user, pwd);
            usedConnectMode = (ConnectMode.SSL == connectMode) ? connectMode : ((POP3Store) store).isSSL() ? ConnectMode.STARTTLS : connectMode;
            closeSafe(store);
            store = null;

            AuthenticationResult result = AuthenticationResult.success(user, pwd, host, port, usedConnectMode);
            LOGGER.debug("Successful POP3 authentication with: {}", result);
            return result;
        } catch (AuthenticationFailedException e) {
            LOGGER.debug("Failed POP3 authentication for login at {}://{}:{}", user, ConnectMode.SSL == connectMode ? "pop3s" : "pop3", host, Integer.valueOf(port), e);
            return AuthenticationResult.failedAuthentication(MimeMailExceptionCode.INVALID_CREDENTIALS.create(e, host, e.getMessage()));
        } catch (Exception e) {
            LOGGER.debug("Error during POP3 authentication for login at {}://{}:{}", user, ConnectMode.SSL == connectMode ? "pop3s" : "pop3", host, Integer.valueOf(port), e);
            return AuthenticationResult.error();
        } finally {
            closeSafe(store);
        }
    }

    /**
     * Validates for successful authentication against specified SMTP server.
     *
     * @param host The SMTP host
     * @param port The SMTP port
     * @param connectMode The connect mode to use
     * @param user The login
     * @param pwd The password
     * @return The authentication result
     */
    public static AuthenticationResult validateSmtpAuthentication(String host, int port, ConnectMode connectModes, String user, String pwd) {
        return validateSmtpAuthentication(host, port, connectModes, user, pwd, null);
    }

    /**
     * Validates for successful authentication against specified SMTP server.
     *
     * @param host The SMTP host
     * @param port The SMTP port
     * @param connectMode The connect mode to use
     * @param user The login
     * @param pwd The password
     * @param optProperties The optional container for arbitrary properties
     * @return The authentication result
     */
    public static AuthenticationResult validateSmtpAuthentication(String host, int port, ConnectMode connectMode, String user, String pwd, Map<String, Object> optProperties) {
        UserAndContext uac = null;
        boolean primary = false;
        if (optProperties != null) {
            Integer contextId = (Integer) optProperties.get(PROP_GENERAL_CONTEXT_ID);
            Integer userId = (Integer) optProperties.get(PROP_GENERAL_USER_ID);
            if (contextId != null && userId != null) {
                uac = new UserAndContext(userId.intValue(), contextId.intValue());
                if (Utils.isPrimaryImapAccount(host, port, userId.intValue(), contextId.intValue())) {
                    primary = true;
                }
            }
        }

        ConnectMode usedConnectMode = connectMode;
        Transport transport = null;
        try {
            SSLSocketFactoryProvider factoryProvider = Services.getService(SSLSocketFactoryProvider.class);
            String socketFactoryClass = factoryProvider.getDefault().getClass().getName();
            Properties props = MimeDefaultSession.getDefaultMailProperties();
            if (ConnectMode.SSL == connectMode) {
                props.put("mail.smtp.socketFactory.class", socketFactoryClass);
            } else if (ConnectMode.STARTTLS == connectMode) {
                props.put("mail.smtp.ssl.socketFactory.class", socketFactoryClass);
                props.put("mail.smtp.ssl.socketFactory.port", I(port));
                props.put("mail.smtp.starttls.required", Boolean.TRUE);
                props.put("mail.smtp.ssl.trust", "*");
            } else {
                props.put("mail.smtp.ssl.socketFactory.class", socketFactoryClass);
                props.put("mail.smtp.ssl.socketFactory.port", I(port));
                {
                    PropName propName = new PropName("com.openexchange.smtp.enableTls", primary ? "com.openexchange.smtp.primary.enableTls" : null);
                    boolean enableTls = lookUpBooleanProperty(propName, true, uac);
                    if (enableTls) {
                        props.put("mail.smtp.starttls.enable", Boolean.TRUE);
                        props.put("mail.smtp.ssl.trust", "*");
                    }
                }
            }
            putSSLConfig("smtp", props, primary, uac);
            props.put("mail.smtp.socketFactory.port", I(port));
            //props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", I(DEFAULT_CONNECT_TIMEOUT));
            props.put("mail.smtp.timeout", I(DEFAULT_TIMEOUT));
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.auth", "true");
            Session session = Session.getInstance(props, null);
            transport = session.getTransport("smtp");
            transport.connect(host, port, user, pwd);

            if (null != optProperties) {
                final SMTPTransport smtpTransport = (SMTPTransport) transport;
                if (!smtpTransport.supportsExtension("AUTH") && !smtpTransport.supportsExtension("AUTH=LOGIN")) {
                    // No authentication mechanism supported
                    optProperties.put(PROP_SMTP_AUTH_SUPPORTED, Boolean.FALSE);
                }
            }

            usedConnectMode = (ConnectMode.SSL == connectMode) ? connectMode : ((SMTPTransport) transport).isSSL() ? ConnectMode.STARTTLS : connectMode;
            closeSafe(transport);
            transport = null;

            AuthenticationResult result = AuthenticationResult.success(user, pwd, host, port, usedConnectMode);
            LOGGER.debug("Successful SMTP authentication with: {}", result);
            return result;
        } catch (AuthenticationFailedException e) {
            LOGGER.debug("Failed SMTP authentication for login at {}://{}:{}", user, ConnectMode.SSL == connectMode ? "smtps" : "smtp", host, Integer.valueOf(port), e);
            return AuthenticationResult.failedAuthentication(MimeMailExceptionCode.INVALID_CREDENTIALS.create(e, host, e.getMessage()));
        } catch (Exception e) {
            LOGGER.debug("Error during SMTP authentication for login at {}://{}:{}", user, ConnectMode.SSL == connectMode ? "smtps" : "smtp", host, Integer.valueOf(port), e);
            return AuthenticationResult.error();
        } finally {
            closeSafe(transport);
        }
    }

    // ------------------------------------------------------- Connect tests ---------------------------------------------------------

    /**
     * Checks if a (SSL) socket connection can be established to the specified IMAP end-point (host & port)
     *
     * @param host The IMAP host
     * @param port The IMAP port
     * @param secure Whether to create an SSL socket or a plain one.
     * @return <code>true</code> if such a socket could be successfully linked to the given IMAP end-point; otherwise <code>false</code>
     */
    public static boolean tryImapConnect(String host, int port, boolean secure) {
        return tryConnect(host, port, secure, "A11 LOGOUT\r\n", "imap");
    }

    /**
     * Checks if a (SSL) socket connection can be established to the specified SMTP end-point (host & port)
     *
     * @param host The SMTP host
     * @param port The SMTP port
     * @param secure Whether to create an SSL socket or a plain one.
     * @return <code>true</code> if such a socket could be successfully linked to the given SMTP end-point; otherwise <code>false</code>
     */
    public static boolean trySmtpConnect(String host, int port, boolean secure) {
        return tryConnect(host, port, secure, "QUIT\r\n", "smtp");
    }

    /**
     * Checks if a (SSL) socket connection can be established to the specified POP3 end-point (host & port)
     *
     * @param host The POP3 host
     * @param port The POP3 port
     * @param secure Whether to create an SSL socket or a plain one.
     * @return <code>true</code> if such a socket could be successfully linked to the given POP3 end-point; otherwise <code>false</code>
     */
    public static boolean tryPop3Connect(String host, int port, boolean secure) {
        return tryConnect(host, port, secure, "QUIT\r\n", "pop3");
    }

    private static boolean tryConnect(String host, int port, boolean secure, String closePhrase, String name) {
        Socket s = null;
        try {
            // Check if black-listed
            if (MailAccountUtils.isDenied(host, port)) {
                return false;
            }

            // Establish socket connection
            Properties props = createProps(name, port, secure);
            s = SocketFetcher.getSocket(host, port, props, "mail." + name, false);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            if (null == in || null == out) {
                return false;
            }

            // Read server greeting on connect
            boolean eol = false;
            boolean skipLF = false;
            int i = -1;
            while (!eol && ((i = in.read()) != -1)) {
                final char c = (char) i;
                if (c == '\r') {
                    eol = true;
                    skipLF = true;
                } else if (c == '\n') {
                    eol = true;
                    skipLF = false;
                }
                // else; Ignore
            }

            // Consume final LF
            if (skipLF && -1 == in.read()) {
                LOGGER.trace("Final LF should have been read but the end of the stream was already reached.");
            }

            // Close
            out.write(closePhrase.getBytes(StandardCharsets.ISO_8859_1));
            out.flush();
        } catch (Exception e) {
            LOGGER.debug("Unable to connect to {}{}://{}:{}", name, secure ? "s" : "", host, Integer.valueOf(port), e);
            return false;
        } finally {
            Streams.close(s);
        }
        return true;
    }

    private static void closeSafe(AutoCloseable s) {
        if (s != null) {
            try {
                s.close();
            } catch (Exception e) {
                // Ignore
                LOGGER.trace("Unable to close resource.", e);
            }
        }
    }

    private static Properties createProps(String name, int port, boolean secure) {
        Properties props = MimeDefaultSession.getDefaultMailProperties();
        {
            int connectionTimeout = DEFAULT_CONNECT_TIMEOUT;
            if (connectionTimeout > 0) {
                props.put("mail." + name + ".connectiontimeout", Integer.toString(connectionTimeout));
            }
        }
        {
            int timeout = DEFAULT_TIMEOUT;
            if (timeout > 0) {
                props.put("mail." + name + ".timeout", Integer.toString(timeout));
            }
        }
        SSLSocketFactoryProvider factoryProvider = Services.getService(SSLSocketFactoryProvider.class);
        final String socketFactoryClass = factoryProvider.getDefault().getClass().getName();
        final String sPort = Integer.toString(port);
        if (secure) {
            props.put("mail." + name + ".socketFactory.class", socketFactoryClass);
            props.put("mail." + name + ".socketFactory.port", sPort);
            props.put("mail." + name + ".socketFactory.fallback", "false");
        } else {
            props.put("mail." + name + ".socketFactory.port", sPort);
            props.put("mail." + name + ".ssl.socketFactory.class", socketFactoryClass);
            props.put("mail." + name + ".ssl.socketFactory.port", sPort);
            props.put("mail." + name + ".socketFactory.fallback", "false");
        }
        props.put("mail." + name + ".denyInternalAddress", "true");
        return props;
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class UserAndContext {

        private final int userId;
        private final int contextId;

        /**
         * Initializes a new {@link UserAndContext}.
         *
         * @param userId The user identifier
         * @param contextId The context identifier
         */
        UserAndContext(int userId, int contextId) {
            super();
            this.userId = userId;
            this.contextId = contextId;
        }

        /**
         * Gets the user identifier
         *
         * @return The user identifier
         */
        int getUserId() {
            return userId;
        }

        /**
         * Gets the context identifier
         *
         * @return The context identifier
         */
        int getContextId() {
            return contextId;
        }
    }

    private static class PropName {

        private final String name;
        private final String primaryName;

        PropName(String name, String primaryName) {
            super();
            this.name = name;
            this.primaryName = primaryName;
        }

        /**
         * Gets the property name
         *
         * @return The property name
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the optional name for the property associated with primary account
         *
         * @return The optional name the property associated with primary account or <code>null</code>
         */
        public String optPrimaryName() {
            return primaryName;
        }
    }

}
