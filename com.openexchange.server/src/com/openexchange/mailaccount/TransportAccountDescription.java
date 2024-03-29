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

package com.openexchange.mailaccount;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.mail.internet.idn.IDNA;
import com.openexchange.exception.OXException;
import com.openexchange.tools.net.URIDefaults;
import com.openexchange.tools.net.URIParser;
import com.openexchange.tools.net.URITools;

/**
 * {@link TransportAccountDescription}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public class TransportAccountDescription {

    private int id;
    private String transportLogin;
    private String name;
    private String transportPassword;
    private String personal;
    private String primaryAddress;
    private String replyTo;
    private TransportAuth transportAuth;
    private int transportPort;
    private String transportProtocol;
    private boolean transportSecure;
    private String transportServer;
    private boolean transportStartTls;
    private String transportUrl;
    private Map<String, String> transportProperties;
    private int transportOAuthId;
    private boolean transportDisabled;
    private boolean secondaryAccount;
    private boolean deactivated;

    /**
     * Initializes a new {@link TransportAccountDescription}.
     */
    public TransportAccountDescription() {
        super();
        transportProperties = new HashMap<>(4);
        transportAuth = TransportAuth.MAIL;
        transportPort = 25;
        transportProtocol = "smtp";
        id = -1;
        transportOAuthId = -1;
        transportDisabled = false;
        secondaryAccount = false;
        deactivated = false;
    }

    /**
     * Parses specified transport server URL. If the given URL is <code>null</code>, then the transport server URL will be set to <code>null</code> too.
     *
     * @param transportServerURL The transport server URL to parse
     * @throws OXException If URL cannot be parsed
     */
    public void parseTransportServerURL(final String transportServerURL) throws OXException {
        if (null == transportServerURL) {
            setTransportServer((String) null);
            return;
        }
        try {
            setTransportServer(URIParser.parse(IDNA.toASCII(transportServerURL), URIDefaults.SMTP));
        } catch (URISyntaxException e) {
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(e, transportServerURL);
        }
    }

    /**
     * Initializes a new {@link TransportAccountDescription}.
     *
     * @param id
     * @param login
     * @param name
     * @param password
     * @param personal
     * @param primaryAddress
     * @param replyTo
     * @param transportOAuthId
     * @param transportAuth
     * @param transportPort
     * @param transportProtocol
     * @param transportSecure
     * @param transportServer
     * @param transportStartTls
     * @param transportUrl
     */
    public TransportAccountDescription(int id, String login, String name, String password, String personal, String primaryAddress, String replyTo, int transportOAuthId, TransportAuth transportAuth, int transportPort, String transportProtocol, boolean transportSecure, String transportServer, boolean transportStartTls, String transportUrl) {
        super();
        this.id = id;
        this.transportLogin = login;
        this.name = name;
        this.transportPassword = password;
        this.personal = personal;
        this.primaryAddress = primaryAddress;
        this.replyTo = replyTo;
        this.transportOAuthId = transportOAuthId < 0 ? -1 : transportOAuthId;
        this.transportAuth = transportAuth;
        this.transportPort = transportPort;
        this.transportProtocol = transportProtocol;
        this.transportSecure = transportSecure;
        this.transportServer = transportServer;
        this.transportStartTls = transportStartTls;
        this.transportUrl = transportUrl;
    }

    /**
     * Generates the transport server URL. If the transportServer string is empty or null, then this method will return null.
     *
     * @return The transport server URL
     * @throws OXException If URL cannot be parsed
     */
    public String generateTransportServerURL() throws OXException {
        if (null != transportUrl) {
            return transportUrl;
        }
        if (com.openexchange.java.Strings.isEmpty(transportServer)) {
            return null;
        }
        final String protocol = transportSecure ? transportProtocol + 's' : transportProtocol;
        try {
            return transportUrl = URITools.generateURI(protocol, IDNA.toASCII(transportServer), transportPort).toString();
        } catch (URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(transportProtocol);
            if (transportSecure) {
                sb.append('s');
            }
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(e, sb.append("://").append(transportServer).append(':').append(transportPort).toString());
        }
    }

    /**
     * Gets the ID.
     *
     * @return The ID or <code>-1</code> if not set
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the ID
     *
     * @return The ID
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     * Gets the account name.
     *
     * @return The account name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the personal.
     *
     * @return The personal
     */
    public String getPersonal() {
        return personal;
    }

    /**
     * Gets the primary email address.
     *
     * @return The primary email address
     */
    public String getPrimaryAddress() {
        return primaryAddress;
    }

    /**
     * Gets the reply-to address
     *
     * @return The reply-to address
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets the account name.
     *
     * @param name The account name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the primary email address.
     *
     * @param primaryAddress The primary email address
     */
    public void setPrimaryAddress(final String primaryAddress) {
        this.primaryAddress = primaryAddress;
    }

    /**
     * Sets the personal.
     *
     * @param personal The personal
     */
    public void setPersonal(final String personal) {
        this.personal = personal;
    }

    /**
     * Sets the reply-to address
     *
     * @param replyTo The reply-to address
     */
    public void setReplyTo(final String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Checks whether mail transport is disabled
     *
     * @return <code>true</code> if disabled; otherwise <code>false</code>
     */
    public boolean isTransportDisabled() {
        return transportDisabled;
    }

    /**
     * Sets whether mail transport is disabled
     *
     * @param transportDisabled <code>true</code> if disabled; otherwise <code>false</code>
     */
    public void setTransportDisabled(boolean transportDisabled) {
        this.transportDisabled = transportDisabled;
    }

    /**
     * Checks if this account is a secondary one; e.g. considered mail/transport end-point is equal to the primary one.
     *
     * @return <code>true</code> if secondary; otherwise <code>false</code>
     */
    public boolean isSecondaryAccount() {
        return secondaryAccount;
    }

    /**
     * Sets if this account is a secondary one; e.g. considered mail/transport end-point is equal to the primary one.
     *
     * @param secondaryAccount <code>true</code> if secondary; otherwise <code>false</code>
     */
    public void setSecondaryAccount(boolean secondaryAccount) {
        this.secondaryAccount = secondaryAccount;
    }

   /**
    * Checks if this account is deactivated.
    *
    * @return <code>true</code> if deactivated; otherwise <code>false</code>
    */
   public boolean isDeactivated() {
       return deactivated;
   }

   /**
    * Sets if this account is deactivated.
    *
    * @param deactivated <code>true</code> if deactivated; otherwise <code>false</code>
    */
   public void setDeactivated(boolean deactivated) {
       this.deactivated = deactivated;
   }

    /**
     * Gets the transport authentication information
     *
     * @return The transport authentication information
     */
    public TransportAuth getTransportAuth() {
        return transportAuth;
    }

    /**
     * Gets the transport login.
     *
     * @return The transport login
     */
    public String getTransportLogin() {
        return transportLogin;
    }

    /**
     * Sets the transport login
     *
     * @param login The transport login to set
     */
    public void setTransportLogin(String login) {
        this.transportLogin = login;
    }

    /**
     * Gets the transport password.
     *
     * @return The transport password
     */
    public String getTransportPassword() {
        return transportPassword;
    }

    /**
     * Sets the password
     *
     * @param password The password to set
     */
    public void setTransportPassword(String password) {
        this.transportPassword = password;
    }

    /**
     * Gets the transport server port.
     *
     * @return The transport server port
     */
    public int getTransportPort() {
        return transportPort;
    }

    /**
     * Gets the transport server protocol.
     *
     * @return The transport server protocol
     */
    public String getTransportProtocol() {
        return transportProtocol;
    }

    /**
     * Gets the transport server name.
     * <p>
     * The transport server name can either be a machine name, such as "<code>java.sun.com</code>", or a textual representation of its IP
     * address.
     *
     * @return The transport server name
     */
    public String getTransportServer() {
        return transportServer;
    }

    /**
     * Sets the transport server name
     *
     * @param transportServer The transport server name to set
     */
    public void setTransportServer(String transportServer) {
        this.transportServer = transportServer;
    }

    /**
     * Sets transport server URI
     *
     * @param transportServer The transport server URI
     */
    public void setTransportServer(final URI transportServer) {
        if (null == transportServer) {
            // Parse like old parser to prevent problems.
            setTransportServer("");
        } else {
            final String protocol = transportServer.getScheme();
            if (protocol.endsWith("s")) {
                setTransportSecure(true);
                setTransportProtocol(protocol.substring(0, protocol.length() - 1));
            } else {
                setTransportSecure(false);
                setTransportProtocol(protocol);
            }
            setTransportServer(URITools.getHost(transportServer));
            setTransportPort(transportServer.getPort());
        }
    }

    /**
     * Sets the transport authentication information
     *
     * @param transportAuth The transport authentication information to set
     */
    public void setTransportAuth(TransportAuth transportAuth) {
        this.transportAuth = transportAuth;
    }

    /**
     * Sets the transport server port
     *
     * @param transportPort The transport server port to set
     */
    public void setTransportPort(final int transportPort) {
        transportUrl = null;
        this.transportPort = checkTransportPort(transportPort);
    }

    private static int checkTransportPort(final int port) {
        if (URIDefaults.IMAP.getPort() == port) {
            return URIDefaults.SMTP.getPort();
        }
        if (URIDefaults.IMAP.getSSLPort() == port) {
            return URIDefaults.SMTP.getSSLPort();
        }
        return port;
    }

    /**
     * Sets the transport server protocol
     *
     * @param transportProtocol The transport server protocol to set
     */
    public void setTransportProtocol(final String transportProtocol) {
        transportUrl = null;
        this.transportProtocol = transportProtocol;
    }

    /**
     * Sets if a secure connection to transport server shall be established.
     *
     * @param transportSecure <code>true</code> if a secure connection to transport server shall be established; otherwise <code>false</code>
     */
    public void setTransportSecure(final boolean transportSecure) {
        transportUrl = null;
        this.transportSecure = transportSecure;
    }

    /**
     * Gets the transport secure flag
     *
     * @return The transport secure flag
     */
    public boolean isTransportSecure() {
        return transportSecure;
    }

    /**
     * Sets if STARTTLS should be used to connect to transport server
     *
     * @return true/false
     */
    public void setTransportStartTls(boolean transportStartTls) {
        this.transportStartTls = transportStartTls;
    }

    /**
     * Checks if STARTTLS should be used to connect to transport server
     *
     * @return true/false
     */
    public boolean isTransportStartTls() {
        return transportStartTls;
    }

    /**
     * Checks if transport server expects to authenticate via OAuth or not.
     *
     * @return <code>true</code> for OAuth authentication, otherwise <code>false</code>.
     */
    public boolean isTransportOAuthAble() {
        return transportOAuthId >= 0;
    }

    /**
     * Gets the identifier of the associated OAuth account (if any) to authenticate against transport server.
     *
     * @return The OAuth account identifier or <code>-1</code> if there is no associated OAuth account
     */
    public int getTransportOAuthId() {
        return transportOAuthId < 0 ? -1 : transportOAuthId;
    }

    /**
     * Sets the identifier of the associated OAuth account for transport server
     *
     * @param transportOAuthId The OAuth account identifier or <code>-1</code> to signal none
     */
    public void setTransportOAuthId(int transportOAuthId) {
        this.transportOAuthId = transportOAuthId < 0 ? -1 : transportOAuthId;
    }

    /**
     * Gets the transport properties
     *
     * @return The transport properties
     */
    public Map<String, String> getTransportProperties() {
        if (transportProperties.isEmpty()) {
            return Collections.emptyMap();
        }
        return new HashMap<>(transportProperties);
    }

    /**
     * Sets the transport properties
     *
     * @param transportProperties The transport properties to set
     */
    public void setTransportProperties(final Map<String, String> transportProperties) {
        if (null == transportProperties) {
            this.transportProperties = new HashMap<>(4);
        } else {
            this.transportProperties = transportProperties;
        }
    }

    /**
     * Adds specified name-value-pair to transport properties.
     *
     * @param name The transport property name
     * @param value The transport property value
     */
    public void addTransportProperty(final String name, final String value) {
        if (transportProperties.isEmpty()) {
            transportProperties = new HashMap<>(4);
        }
        transportProperties.put(name, value);
    }

}
