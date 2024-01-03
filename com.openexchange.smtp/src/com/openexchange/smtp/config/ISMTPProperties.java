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

package com.openexchange.smtp.config;

import com.openexchange.mail.transport.config.ITransportProperties;

public interface ISMTPProperties extends ITransportProperties {

    /**
     * Gets the smtpLocalhost
     *
     * @return the smtpLocalhost
     */
    String getSmtpLocalhost();

    /**
     * Gets the smtpAuth
     *
     * @return the smtpAuth
     */
    boolean isSmtpAuth();

    /**
     * Gets the smtpEnvelopeFrom
     *
     * @return the smtpEnvelopeFrom
     */
    boolean isSmtpEnvelopeFrom();

    /**
     * Gets the logTransport flag
     *
     * @return the logTransport flag
     */
    boolean isLogTransport();

    /**
     * Gets the smtpAuthEnc
     *
     * @return the smtpAuthEnc
     */
    String getSmtpAuthEnc();

    /**
     * Gets the smtpTimeout
     *
     * @return the smtpTimeout
     */
    int getSmtpTimeout();

    /**
     * Gets the smtpConnectionTimeout
     *
     * @return the smtpConnectionTimeout
     */
    int getSmtpConnectionTimeout();

    /**
     * Gets supported SSL protocols
     *
     * @return Supported SSL protocols
     */
    String getSSLProtocols();

    /**
     * Gets the SSL cipher suites that will be enabled for SSL connections. The property value is a whitespace separated list of tokens
     * acceptable to the <code>javax.net.ssl.SSLSocket.setEnabledProtocols</code> method.
     *
     * @return The SSL cipher suites
     */
    String getSSLCipherSuites();

    /**
     * Whether partial send is allowed or message transport is supposed to be aborted.
     *
     * @return <code>true</code> if partial send is allowed; otherwise <code>false</code>
     */
    boolean isSendPartial();

    /**
     * Whether XCLIENT extension should be used if supported by SMTP server.
     * <p>
     * When an SMTP server announces support for the XCLIENT command, an SMTP client may send information that overrides one or more
     * client-related session attributes.
     *
     * @return <code>true</code> if XCLIENT is allowed; otherwise <code>false</code>
     */
    boolean isAllowXClient();

    /**
     * Get the primary address header to append.
     *
     * @return The primary address header name or <code>null</code> if not set
     */
    String getPrimaryAddressHeader();

    /**
     * Whether using the STARTTLS command is enforced
     *
     * @return <code>true</code> to enforce STARTTLS command; otherwise <code>false</code>
     */
    public boolean isRequireTls();

}
