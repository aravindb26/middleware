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

package com.openexchange.admin.rmi.dataobjects;

import java.io.Serializable;

/**
 * This class represents account data required for creating/modifying an account.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class AccountData implements Serializable, Cloneable {

    private static final long serialVersionUID = -4068828009821317094L;

    private String login;
    private boolean loginset;

    private String password;
    private boolean passwordset;

    private String name;
    private boolean nameset;

    private String primaryAddress;
    private boolean primaryAddressset;

    private String personal;
    private boolean personalset;

    private String replyTo;
    private boolean replyToset;

    private String mailServer;
    private boolean mailServerset;

    private int mailPort;
    private boolean mailPortset;

    private String mailProtocol;
    private boolean mailProtocolset;

    private boolean mailSecure;
    private boolean mailSecureset;

    private boolean mailStartTls;
    private boolean mailStartTlsset;

    private String transportLogin;
    private boolean transportLoginset;

    private String transportPassword;
    private boolean transportPasswordset;

    private String transportServer;
    private boolean transportServerset;

    private int transportPort;
    private boolean transportPortset;

    private String transportProtocol;
    private boolean transportProtocolset;

    private boolean transportSecure;
    private boolean transportSecureset;

    private boolean transportStartTls;
    private boolean transportStartTlsset;

    private String trashFullname;
    private boolean trashFullnameset;

    private String archiveFullname;
    private boolean archiveFullnameset;

    private String sentFullname;
    private boolean sentFullnameset;

    private String draftsFullname;
    private boolean draftsFullnameset;

    private String spamFullname;
    private boolean spamFullnameset;

    private String confirmedSpamFullname;
    private boolean confirmedSpamFullnameset;

    private String confirmedHamFullname;
    private boolean confirmedHamFullnameset;

    /**
     * Initializes a new {@link AccountData}.
     */
    public AccountData() {
        super();
    }

    /**
     * Gets the name
     *
     * @return The name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name
     *
     * @param name The name
     */
    public void setName(String name) {
        this.name = name;
        this.nameset = true;
    }

    /**
     * Gets the login
     *
     * @return The login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Sets the login
     *
     * @param login The login to set
     */
    public void setLogin(String login) {
        this.login = login;
        this.loginset = true;
    }

    /**
     * Gets the password
     *
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password
     *
     * @param password The password to set
     */
    public void setPassword(String password) {
        this.password = password;
        this.passwordset = true;
    }

    /**
     * Gets the primary address
     *
     * @return The primary address
     */
    public String getPrimaryAddress() {
        return primaryAddress;
    }

    /**
     * Sets the primary address
     *
     * @param primaryAddress The primary address to set
     */
    public void setPrimaryAddress(String primaryAddress) {
        this.primaryAddress = primaryAddress;
        this.primaryAddressset = true;
    }

    /**
     * Gets the personal
     *
     * @return The personal
     */
    public String getPersonal() {
        return personal;
    }

    /**
     * Sets the personal
     *
     * @param personal The personal to set
     */
    public void setPersonal(String personal) {
        this.personal = personal;
        this.personalset = true;
    }

    /**
     * Gets the reply-to
     *
     * @return The reply-to
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets the reply-to
     *
     * @param replyTo The reply-to to set
     */
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
        this.replyToset = true;
    }

    /**
     * Gets the mail server
     *
     * @return The mail server
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * Sets the mail server
     *
     * @param mailServer The mail server to set
     */
    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
        this.mailServerset = true;
    }

    /**
     * Gets the mail port
     *
     * @return The mail port
     */
    public int getMailPort() {
        return mailPort;
    }

    /**
     * Sets the mail port
     *
     * @param mailPort The mail port to set
     */
    public void setMailPort(int mailPort) {
        this.mailPort = mailPort;
        this.mailPortset = true;
    }

    /**
     * Gets the mail protocol
     *
     * @return The mail protocol
     */
    public String getMailProtocol() {
        return mailProtocol;
    }

    /**
     * Sets the mail protocol
     *
     * @param mailProtocol The mail protocol to set
     */
    public void setMailProtocol(String mailProtocol) {
        this.mailProtocol = mailProtocol;
        this.mailProtocolset = true;
    }

    /**
     * Gets the mail secure
     *
     * @return The mail secure
     */
    public boolean isMailSecure() {
        return mailSecure;
    }

    /**
     * Sets the mail secure
     *
     * @param mailSecure The mail secure to set
     */
    public void setMailSecure(boolean mailSecure) {
        this.mailSecure = mailSecure;
        this.mailSecureset = true;
    }

    /**
     * Gets the mail STARTTLS flag.
     *
     * @return The mail STARTTLS flag
     */
    public boolean isMailStartTls() {
        return mailStartTls;
    }

    /**
     * Sets the mail STARTTLS flag
     *
     * @param mailStartTls The mail STARTTLS flag to set
     */
    public void setMailStartTls(boolean mailStartTls) {
        this.mailStartTls = mailStartTls;
        this.mailStartTlsset = true;
    }

    /**
     * Gets the transport login
     *
     * @return The transport login
     */
    public String getTransportLogin() {
        return transportLogin;
    }


    /**
     * Sets the transport login
     *
     * @param transportLogin The transport login
     */
    public void setTransportLogin(String transportLogin) {
        this.transportLogin = transportLogin;
        this.transportLoginset = true;
    }

    /**
     * Gets the transport password
     *
     * @return The transport password
     */
    public String getTransportPassword() {
        return transportPassword;
    }


    /**
     * Sets the transport password
     *
     * @param transportPassword The transport password
     */
    public void setTransportPassword(String transportPassword) {
        this.transportPassword = transportPassword;
        this.transportPasswordset = true;
    }

    /**
     * Gets the transport server
     *
     * @return The transport server
     */
    public String getTransportServer() {
        return transportServer;
    }

    /**
     * Sets the transport server
     *
     * @param transportServer The transport server to set
     */
    public void setTransportServer(String transportServer) {
        this.transportServer = transportServer;
        this.transportServerset = true;
    }

    /**
     * Gets the transport port
     *
     * @return The transport port
     */
    public int getTransportPort() {
        return transportPort;
    }

    /**
     * Sets the transport port
     *
     * @param transportPort The transport port to set
     */
    public void setTransportPort(int transportPort) {
        this.transportPort = transportPort;
        this.transportPortset = true;
    }

    /**
     * Gets the transport protocol
     *
     * @return The transport protocol
     */
    public String getTransportProtocol() {
        return transportProtocol;
    }

    /**
     * Sets the transport protocol
     *
     * @param transportProtocol The transport protocol to set
     */
    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
        this.transportProtocolset = true;
    }

    /**
     * Gets the transport secure
     *
     * @return The transport secure
     */
    public boolean isTransportSecure() {
        return transportSecure;
    }

    /**
     * Sets the transport secure
     *
     * @param transportSecure The transport secure to set
     */
    public void setTransportSecure(boolean transportSecure) {
        this.transportSecure = transportSecure;
        this.transportSecureset = true;
    }

    /**
     * Gets the transport STARTTLS flag
     *
     * @return The transport STARTTLS flag
     */
    public boolean isTransportStartTls() {
        return transportStartTls;
    }

    /**
     * Sets the transport STARTTLS flag
     *
     * @param transportStartTls The transport STARTTLS flag to set
     */
    public void setTransportStartTls(boolean transportStartTls) {
        this.transportStartTls = transportStartTls;
        this.transportStartTlsset = true;
    }

    /**
     * Gets the trash full name
     *
     * @return The trash full name
     */
    public String getTrashFullname() {
        return trashFullname;
    }

    /**
     * Sets the trash full name
     *
     * @param trashFullname The trash full name to set
     */
    public void setTrashFullname(String trashFullname) {
        this.trashFullname = trashFullname;
        this.trashFullnameset = true;
    }

    /**
     * Gets the archive full name
     *
     * @return The archive full name
     */
    public String getArchiveFullname() {
        return archiveFullname;
    }

    /**
     * Sets the archive full name
     *
     * @param archiveFullname The archive full name to set
     */
    public void setArchiveFullname(String archiveFullname) {
        this.archiveFullname = archiveFullname;
        this.archiveFullnameset = true;
    }

    /**
     * Gets the sent full name
     *
     * @return The sent full name
     */
    public String getSentFullname() {
        return sentFullname;
    }

    /**
     * Sets the sent full name
     *
     * @param sentFullname The sent full name to set
     */
    public void setSentFullname(String sentFullname) {
        this.sentFullname = sentFullname;
        this.sentFullnameset = true;
    }

    /**
     * Gets the drafts full name
     *
     * @return The drafts full name
     */
    public String getDraftsFullname() {
        return draftsFullname;
    }

    /**
     * Sets the drafts full name
     *
     * @param draftsFullname The drafts full name to set
     */
    public void setDraftsFullname(String draftsFullname) {
        this.draftsFullname = draftsFullname;
        this.draftsFullnameset = true;
    }

    /**
     * Gets the spam full name
     *
     * @return The spam full name
     */
    public String getSpamFullname() {
        return spamFullname;
    }

    /**
     * Sets the spam full name
     *
     * @param spamFullname The spam full name to set
     */
    public void setSpamFullname(String spamFullname) {
        this.spamFullname = spamFullname;
        this.spamFullnameset = true;
    }

    /**
     * Gets the confirmed-spam full name
     *
     * @return The confirmed-spam full name
     */
    public String getConfirmedSpamFullname() {
        return confirmedSpamFullname;
    }

    /**
     * Sets the confirmed-spam full name
     *
     * @param confirmedSpamFullname The confirmed-spam full name to set
     */
    public void setConfirmedSpamFullname(String confirmedSpamFullname) {
        this.confirmedSpamFullname = confirmedSpamFullname;
        this.confirmedSpamFullnameset = true;
    }

    /**
     * Gets the confirmed-ham full name
     *
     * @return The confirmed-ham full name
     */
    public String getConfirmedHamFullname() {
        return confirmedHamFullname;
    }

    /**
     * Sets the confirmed-ham full name
     *
     * @param confirmedHamFullname The confirmed-ham full name to set
     */
    public void setConfirmedHamFullname(String confirmedHamFullname) {
        this.confirmedHamFullname = confirmedHamFullname;
        this.confirmedHamFullnameset = true;
    }

    /**
     * Checks whether login is set
     *
     * @return <code>true</code> if login is set; otherwise <code>false</code>
     */
    public boolean isLoginset() {
        return loginset;
    }

    /**
     * Checks whether password is set
     *
     * @return <code>true</code> if password is set; otherwise <code>false</code>
     */
    public boolean isPasswordset() {
        return passwordset;
    }

    /**
     * Checks whether name is set
     *
     * @return <code>true</code> if name is set; otherwise <code>false</code>
     */
    public boolean isNameset() {
        return nameset;
    }

    /**
     * Checks whether primary address is set
     *
     * @return <code>true</code> if primary address is set; otherwise <code>false</code>
     */
    public boolean isPrimaryAddressset() {
        return primaryAddressset;
    }

    /**
     * Checks whether personal is set
     *
     * @return <code>true</code> if personal is set; otherwise <code>false</code>
     */
    public boolean isPersonalset() {
        return personalset;
    }

    /**
     * Checks whether reply-to is set
     *
     * @return <code>true</code> if reply-to is set; otherwise <code>false</code>
     */
    public boolean isReplyToset() {
        return replyToset;
    }

    /**
     * Checks whether mail server is set
     *
     * @return <code>true</code> if mail server is set; otherwise <code>false</code>
     */
    public boolean isMailServerset() {
        return mailServerset;
    }

    /**
     * Checks whether mail port is set
     *
     * @return <code>true</code> if mail port is set; otherwise <code>false</code>
     */
    public boolean isMailPortset() {
        return mailPortset;
    }

    /**
     * Checks whether mail protocol is set
     *
     * @return <code>true</code> if mail protocol is set; otherwise <code>false</code>
     */
    public boolean isMailProtocolset() {
        return mailProtocolset;
    }

    /**
     * Checks whether mail secure is set
     *
     * @return <code>true</code> if mail secure is set; otherwise <code>false</code>
     */
    public boolean isMailSecureset() {
        return mailSecureset;
    }

    /**
     * Checks whether mail STARTTLS flag is set
     *
     * @return <code>true</code> if mail STARTTLS flag is set; otherwise <code>false</code>
     */
    public boolean isMailStartTlsset() {
        return mailStartTlsset;
    }

    /**
     * Checks whether transport login is set
     *
     * @return <code>true</code> if transport login is set; otherwise <code>false</code>
     */
    public boolean isTransportLoginset() {
        return transportLoginset;
    }

    /**
     * Checks whether transport password is set
     *
     * @return <code>true</code> if transport password is set; otherwise <code>false</code>
     */
    public boolean isTransportPasswordset() {
        return transportPasswordset;
    }

    /**
     * Checks whether transport server is set
     *
     * @return <code>true</code> if transport server is set; otherwise <code>false</code>
     */
    public boolean isTransportServerset() {
        return transportServerset;
    }

    /**
     * Checks whether transport port is set
     *
     * @return <code>true</code> if transport port is set; otherwise <code>false</code>
     */
    public boolean isTransportPortset() {
        return transportPortset;
    }

    /**
     * Checks whether transport protocol is set
     *
     * @return <code>true</code> if transport protocol is set; otherwise <code>false</code>
     */
    public boolean isTransportProtocolset() {
        return transportProtocolset;
    }

    /**
     * Checks whether transport secure is set
     *
     * @return <code>true</code> if transport secure is set; otherwise <code>false</code>
     */
    public boolean isTransportSecureset() {
        return transportSecureset;
    }

    /**
     * Checks whether transport STARTTLS flag is set
     *
     * @return <code>true</code> if transport STARTTLS flag is set; otherwise <code>false</code>
     */
    public boolean isTransportStartTlsset() {
        return transportStartTlsset;
    }

    /**
     * Checks whether trash full name is set
     *
     * @return <code>true</code> if trash full name is set; otherwise <code>false</code>
     */
    public boolean isTrashFullnameset() {
        return trashFullnameset;
    }

    /**
     * Checks whether archive full name is set
     *
     * @return <code>true</code> if archive full name is set; otherwise <code>false</code>
     */
    public boolean isArchiveFullnameset() {
        return archiveFullnameset;
    }

    /**
     * Checks whether sent full name is set
     *
     * @return <code>true</code> if sent full name is set; otherwise <code>false</code>
     */
    public boolean isSentFullnameset() {
        return sentFullnameset;
    }

    /**
     * Checks whether drafts full name is set
     *
     * @return <code>true</code> if drafts full name is set; otherwise <code>false</code>
     */
    public boolean isDraftsFullnameset() {
        return draftsFullnameset;
    }

    /**
     * Checks whether spam full name is set
     *
     * @return <code>true</code> if spam full name is set; otherwise <code>false</code>
     */
    public boolean isSpamFullnameset() {
        return spamFullnameset;
    }

    /**
     * Checks whether confirmed-spam full name is set
     *
     * @return <code>true</code> if confirmed-spam full name is set; otherwise <code>false</code>
     */
    public boolean isConfirmedSpamFullnameset() {
        return confirmedSpamFullnameset;
    }

    /**
     * Checks whether confirmed-ham full name is set
     *
     * @return <code>true</code> if confirmed-ham full name is set; otherwise <code>false</code>
     */
    public boolean isConfirmedHamFullnameset() {
        return confirmedHamFullnameset;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
