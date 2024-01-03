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

package com.openexchange.mail.autoconfig.sources;

import static com.openexchange.java.Autoboxing.I;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.openexchange.config.cascade.ComposedConfigProperty;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.mail.autoconfig.Autoconfig;
import com.openexchange.mail.autoconfig.DefaultAutoconfig;
import com.openexchange.mail.autoconfig.tools.AuthenticationResult;
import com.openexchange.mail.autoconfig.tools.ConnectMode;
import com.openexchange.mail.autoconfig.tools.FailedAuthenticationResult;
import com.openexchange.mail.autoconfig.tools.MailValidator;
import com.openexchange.mail.autoconfig.tools.SuccessAuthenticationResult;
import com.openexchange.mailaccount.utils.MailAccountUtils;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.net.URIDefaults;

/**
 * {@link Guess}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class Guess extends AbstractConfigSource {

    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Guess.class);

    /** The property name for context identifier. Value is <code>java.lang.Integer</code> */
    public static final String PROP_GENERAL_CONTEXT_ID = "general.context";

    /** The property name for user identifier. Value is <code>java.lang.Integer</code> */
    public static final String PROP_GENERAL_USER_ID = "general.user";

    /** The property name for flag whether SMTP supports authentication. Value is <code>java.lang.Boolean</code> */
    public static final String PROP_SMTP_AUTH_SUPPORTED = "smtp.auth-supported";

    private final ServiceLookup services;

    /**
     * Initializes a new {@link Guess}.
     */
    public Guess(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public Autoconfig getAutoconfig(String emailLocalPart, String emailDomain, String password, int userId, int contextId) throws OXException {
        return getAutoconfig(emailLocalPart, emailDomain, password, userId, contextId, true);
    }

    @Override
    public DefaultAutoconfig getAutoconfig(String emailLocalPart, String emailDomain, String password, int userId, int contextId, boolean forceSecure) throws OXException {
        ConfigViewFactory configViewFactory = services.getService(ConfigViewFactory.class);
        ConfigView view = configViewFactory.getView(userId, contextId);
        ComposedConfigProperty<Boolean> property = view.property("com.openexchange.mail.autoconfig.allowGuess", boolean.class);
        Boolean value = property.get();
        if (value != null && !value.booleanValue()) {
            // Guessing is disabled
            return null;
        }

        // Guess the configuration...
        DefaultAutoconfig config = new DefaultAutoconfig();
        Map<String, Object> properties = new HashMap<String, Object>(4);
        properties.put(PROP_GENERAL_USER_ID, I(userId));
        properties.put(PROP_GENERAL_CONTEXT_ID, I(contextId));

        // Check mail access (first IMAP, then POP3)
        boolean imapSuccess = fillProtocol(Protocol.IMAP, emailLocalPart, emailDomain, password, config, properties, forceSecure);
        boolean generalSuccess = imapSuccess;
        if (!imapSuccess) {
            generalSuccess = fillProtocol(Protocol.POP3, emailLocalPart, emailDomain, password, config, properties, forceSecure) || generalSuccess;
        }

        boolean mailSuccess = generalSuccess;

        // Check transport access (SMTP)
        generalSuccess = fillProtocol(Protocol.SMTP, emailLocalPart, emailDomain, password, config, properties, forceSecure) || generalSuccess;

        // Check for special SMTP that does not support authentication
        {
            Boolean smtpAuthSupported = (Boolean) properties.get(PROP_SMTP_AUTH_SUPPORTED);
            if (null != smtpAuthSupported && !smtpAuthSupported.booleanValue() && !mailSuccess) {
                // Neither IMAP nor POP3 reachable, but SMTP works as it does not support authentication
                // Therefore return null
                return null;
            }
        }


        return generalSuccess ? config : null;
    }

    private boolean fillProtocol(Protocol protocol, String emailLocalPart, String emailDomain, String password, DefaultAutoconfig config, Map<String, Object> properties, boolean forceSecure) throws OXException {
        Optional<ConnectSettings> optConnectSettings = guessHost(protocol, emailDomain);
        if (optConnectSettings.isPresent() == false) {
            return false;
        }

        AuthenticationResult authResult;
        {
            ConnectSettings connectSettings = optConnectSettings.get();
            authResult = guessLogin(protocol, connectSettings.host, connectSettings.port, connectSettings.secure, forceSecure, emailLocalPart, emailDomain, password, properties);
            if (authResult.isSuccess() == false) {
                OXException authFailedException = authResult.isFailedAuthentication() ? ((FailedAuthenticationResult) authResult).getAuthFailedException() : null;
                if (authFailedException != null) {
                    // Result signals failed authentication (due to wrong credentials/authentication data. Therefore, let auto-config fail...
                    throw authFailedException;
                }
                return false;
            }
        }

        SuccessAuthenticationResult successAuthResult = (SuccessAuthenticationResult) authResult;
        if (protocol.isTransport()) {
            config.setTransportPort(successAuthResult.getPort());
            config.setTransportProtocol(protocol.getProtocol());
            config.setTransportSecure(successAuthResult.equalsConnectMode(ConnectMode.SSL));
            config.setTransportServer(successAuthResult.getHost());
            config.setTransportStartTls(successAuthResult.equalsConnectMode(ConnectMode.STARTTLS));
            config.setUsername(successAuthResult.getUser());
        } else {
            config.setMailPort(successAuthResult.getPort());
            config.setMailProtocol(protocol.getProtocol());
            config.setMailSecure(successAuthResult.equalsConnectMode(ConnectMode.SSL));
            config.setMailServer(successAuthResult.getHost());
            config.setMailStartTls(successAuthResult.equalsConnectMode(ConnectMode.STARTTLS));
            config.setUsername(successAuthResult.getUser());
        }
        return true;
    }

    private AuthenticationResult guessLogin(Protocol protocol, String host, int port, boolean secure, boolean requireTls, String emailLocalPart, String emailDomain, String password, Map<String, Object> properties) {
        List<String> logins = Arrays.asList(emailLocalPart, emailLocalPart + '@' + emailDomain);
        ConnectMode connectMode = ConnectMode.connectModeFor(secure, requireTls);

        AuthenticationResult authenticationResult;
        for (String login : logins) {
            switch (protocol) {
                case IMAP:
                    authenticationResult = MailValidator.validateImapAuthentication(host, port, connectMode, login, password, properties);
                    if (authenticationResult.isSuccessOrFailedAuthentication()) {
                        // Either result signals success or a failed authentication. In both cases no further authentication attempts are reasonable
                        return authenticationResult;
                    }
                    break;
                case POP3:
                    authenticationResult = MailValidator.validatePop3Authentication(host, port, connectMode, login, password, properties);
                    if (authenticationResult.isSuccessOrFailedAuthentication()) {
                        // Either result signals success or a failed authentication. In both cases no further authentication attempts are reasonable
                        return authenticationResult;
                    }
                    break;
                case SMTP:
                    authenticationResult = MailValidator.validateSmtpAuthentication(host, port, connectMode, login, password, properties);
                    if (authenticationResult.isSuccessOrFailedAuthentication()) {
                        // Either result signals success or a failed authentication. In both cases no further authentication attempts are reasonable
                        return authenticationResult;
                    }
                    break;
            }
        }

        return AuthenticationResult.error();
    }

    /**
     * Guesses the connect settings for given protocol and domain part of the E-Mail address.
     *
     * @param protocol The protocol to guess for
     * @param emailDomain The domain part of the E-Mail address
     * @return The connect settings or empty
     */
    private Optional<ConnectSettings> guessHost(Protocol protocol, String emailDomain) {
        for (String prefix : protocol.getPrefixes()) {
            // Compile host name
            String host = prefix.length() > 0 ? prefix + emailDomain : emailDomain;

            // Try to connect against that host
            Optional<ConnectSettings> optConnectSettings = tryHost(protocol, host);
            if (optConnectSettings.isPresent()) {
                return optConnectSettings;
            }
        }

        return Optional.empty();
    }

    /**
     * Tries to connect against given host using specified protocol.
     *
     * @param protocol The protocol to use
     * @param host The host name
     * @return The connect settings or empty
     */
    private Optional<ConnectSettings> tryHost(Protocol protocol, String host) {
        // Check if black-listed
        if (MailAccountUtils.isBlacklisted(host)) {
            LOG.debug("Suppressed connect check against host \"{}\" for mail auto-config", host);
            return Optional.empty();
        }

        LOG.debug("Going to perform connect check against host \"{}\" for mail auto-config", host);
        URIDefaults uriDefaults = protocol.getUriDefaults();
        int altPort = protocol.getAltPort();

        // Try SSL connect using default SSL port
        if (tryConnect(protocol, host, uriDefaults.getSSLPort(), true)) {
            return Optional.of(new ConnectSettings(host, true, uriDefaults.getSSLPort()));
        }

        // Try SSL connect using default port
        if (tryConnect(protocol, host, uriDefaults.getPort(), true)) {
            return Optional.of(new ConnectSettings(host, true, uriDefaults.getPort()));
        }

        // Try plain connect using alternative port
        if (altPort > 0 && tryConnect(protocol, host, altPort, false)) {
            return Optional.of(new ConnectSettings(host, false, altPort));
        }

        // Try plain connect using default port
        if (tryConnect(protocol, host, uriDefaults.getPort(), false)) {
            return Optional.of(new ConnectSettings(host, false, uriDefaults.getPort()));
        }

        return Optional.empty();
    }

    /**
     * Tries to establish a connection for given protocol using specified arguments (host, port and secure flag).
     *
     * @param protocol The protocol to use
     * @param host The host name
     * @param port The port number
     * @param secure Whether to establish a secure or plain connection
     * @return <code>true</code> if connect attempt was successful; otherwise <code>false</code>
     */
    private boolean tryConnect(Protocol protocol, String host, int port, boolean secure) {
        switch (protocol) {
            case IMAP:
                return MailValidator.tryImapConnect(host, port, secure);
            case POP3:
                return MailValidator.tryPop3Connect(host, port, secure);
            case SMTP:
                return MailValidator.trySmtpConnect(host, port, secure);
        }

        return false;
    }

    // ----------------------------------------------------- Helper classes -------------------------------------------------

    private static enum Protocol {

        /** The IMAP protocol. */
        IMAP("imap", false, Arrays.asList("", "imap.", "mail."), URIDefaults.IMAP),

        /** The SMTP protocol. */
        SMTP("smtp", 587, true, Arrays.asList("", "smtp.", "mail."), URIDefaults.SMTP),

        /** The POP3 protocol. */
        POP3("pop3", false, Arrays.asList("", "pop3.", "mail."), URIDefaults.POP3);

        private final String protocol;
        private final boolean transport;
        private final List<String> prefixes;
        private final int altPort;
        private final URIDefaults uriDefaults;

        private Protocol(String protocol, boolean transport, List<String> prefixes, URIDefaults uriDefaults) {
            this(protocol, 0, transport, prefixes, uriDefaults);
        }

        private Protocol(String protocol, int altPort, boolean transport, List<String> prefixes, URIDefaults uriDefaults) {
            this.protocol = protocol;
            this.altPort = altPort;
            this.transport = transport;
            this.prefixes = prefixes;
            this.uriDefaults = uriDefaults;
        }

        URIDefaults getUriDefaults() {
            return uriDefaults;
        }

        /**
         * Gets the optional alternative port or <code>0</code> (zero) if there is none.
         *
         * @return The optional alternative port or <code>0</code> (zero) if there is none
         */
        int getAltPort() {
            return altPort;
        }

        List<String> getPrefixes() {
            return prefixes;
        }

        boolean isTransport() {
            return transport;
        }

        String getProtocol() {
            return protocol;
        }
    }

    private static class ConnectSettings {

        final String host;
        final int port;
        final boolean secure;

        ConnectSettings(String host, boolean secure, int port) {
            super();
            this.host = host;
            this.port = port;
            this.secure = secure;
        }
    }

}
