
package com.openexchange.admin.soap.secondaryaccount.dataobjects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse f\u00fcr AccountData complex type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "AccountDataUpdate", propOrder = {
    "login",
    "password",
    "name",
    "personal",
    "replyTo",
    "mailServer",
    "mailPort",
    "mailProtocol",
    "mailSecure",
    "mailStartTls",
    "transportLogin",
    "transportPassword",
    "transportServer",
    "transportPort",
    "transportProtocol",
    "transportSecure",
    "transportStartTls",
    "archiveFullname",
    "draftsFullname",
    "sentFullname",
    "spamFullname",
    "trashFullname",
    "confirmedSpamFullname",
    "confirmedHamFullname",
})
public class AccountDataUpdate {

    @XmlElement(nillable = true)
    protected String login;
    @XmlElement(nillable = true)
    protected String password;
    @XmlElement(nillable = true)
    protected String name;
    @XmlElement(nillable = true)
    protected String personal;
    @XmlElement(nillable = true)
    protected String replyTo;
    @XmlElement(nillable = true)
    protected String mailServer;
    @XmlElement(nillable = true)
    protected Integer mailPort;
    @XmlElement(nillable = true)
    protected String mailProtocol;
    @XmlElement(nillable = true)
    protected Boolean mailSecure;
    @XmlElement(nillable = true)
    protected Boolean mailStartTls;
    @XmlElement(nillable = true)
    protected String transportLogin;
    @XmlElement(nillable = true)
    protected String transportPassword;
    @XmlElement(nillable = true)
    protected String transportServer;
    @XmlElement(nillable = true)
    protected Integer transportPort;
    @XmlElement(nillable = true)
    protected String transportProtocol;
    @XmlElement(nillable = true)
    protected Boolean transportSecure;
    @XmlElement(nillable = true)
    protected Boolean transportStartTls;
    @XmlElement(nillable = true)
    protected String archiveFullname;
    @XmlElement(nillable = true)
    protected String draftsFullname;
    @XmlElement(nillable = true)
    protected String sentFullname;
    @XmlElement(nillable = true)
    protected String spamFullname;
    @XmlElement(nillable = true)
    protected String trashFullname;
    @XmlElement(nillable = true)
    protected String confirmedSpamFullname;
    @XmlElement(nillable = true)
    protected String confirmedHamFullname;

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
    }

    /**
     * Gets the name
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
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
    }

    /**
     * Gets the replyTo
     *
     * @return The replyTo
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Sets the replyTo
     *
     * @param replyTo The replyTo to set
     */
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Gets the mailServer
     *
     * @return The mailServer
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * Sets the mailServer
     *
     * @param mailServer The mailServer to set
     */
    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * Gets the mailPort
     *
     * @return The mailPort
     */
    public Integer getMailPort() {
        return mailPort;
    }

    /**
     * Sets the mailPort
     *
     * @param mailPort The mailPort to set
     */
    public void setMailPort(Integer mailPort) {
        this.mailPort = mailPort;
    }

    /**
     * Gets the mailProtocol
     *
     * @return The mailProtocol
     */
    public String getMailProtocol() {
        return mailProtocol;
    }

    /**
     * Sets the mailProtocol
     *
     * @param mailProtocol The mailProtocol to set
     */
    public void setMailProtocol(String mailProtocol) {
        this.mailProtocol = mailProtocol;
    }

    /**
     * Gets the mailSecure
     *
     * @return The mailSecure
     */
    public Boolean getMailSecure() {
        return mailSecure;
    }

    /**
     * Sets the mailSecure
     *
     * @param mailSecure The mailSecure to set
     */
    public void setMailSecure(Boolean mailSecure) {
        this.mailSecure = mailSecure;
    }

    /**
     * Gets the mailStartTls
     *
     * @return The mailStartTls
     */
    public Boolean getMailStartTls() {
        return mailStartTls;
    }

    /**
     * Sets the mailStartTls
     *
     * @param mailStartTls The mailStartTls to set
     */
    public void setMailStartTls(Boolean mailStartTls) {
        this.mailStartTls = mailStartTls;
    }

    /**
     * Gets the transportLogin
     *
     * @return The transportLogin
     */
    public String getTransportLogin() {
        return transportLogin;
    }

    /**
     * Sets the transportLogin
     *
     * @param transportLogin The transportLogin to set
     */
    public void setTransportLogin(String transportLogin) {
        this.transportLogin = transportLogin;
    }

    /**
     * Gets the transportPassword
     *
     * @return The transportPassword
     */
    public String getTransportPassword() {
        return transportPassword;
    }

    /**
     * Sets the transportPassword
     *
     * @param transportPassword The transportPassword to set
     */
    public void setTransportPassword(String transportPassword) {
        this.transportPassword = transportPassword;
    }

    /**
     * Gets the transportServer
     *
     * @return The transportServer
     */
    public String getTransportServer() {
        return transportServer;
    }

    /**
     * Sets the transportServer
     *
     * @param transportServer The transportServer to set
     */
    public void setTransportServer(String transportServer) {
        this.transportServer = transportServer;
    }

    /**
     * Gets the transportPort
     *
     * @return The transportPort
     */
    public Integer getTransportPort() {
        return transportPort;
    }

    /**
     * Sets the transportPort
     *
     * @param transportPort The transportPort to set
     */
    public void setTransportPort(Integer transportPort) {
        this.transportPort = transportPort;
    }

    /**
     * Gets the transportProtocol
     *
     * @return The transportProtocol
     */
    public String getTransportProtocol() {
        return transportProtocol;
    }

    /**
     * Sets the transportProtocol
     *
     * @param transportProtocol The transportProtocol to set
     */
    public void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    /**
     * Gets the transportSecure
     *
     * @return The transportSecure
     */
    public Boolean getTransportSecure() {
        return transportSecure;
    }

    /**
     * Sets the transportSecure
     *
     * @param transportSecure The transportSecure to set
     */
    public void setTransportSecure(Boolean transportSecure) {
        this.transportSecure = transportSecure;
    }

    /**
     * Gets the transportStartTls
     *
     * @return The transportStartTls
     */
    public Boolean getTransportStartTls() {
        return transportStartTls;
    }

    /**
     * Sets the transportStartTls
     *
     * @param transportStartTls The transportStartTls to set
     */
    public void setTransportStartTls(Boolean transportStartTls) {
        this.transportStartTls = transportStartTls;
    }

    /**
     * Gets the archiveFullname
     *
     * @return The archiveFullname
     */
    public String getArchiveFullname() {
        return archiveFullname;
    }

    /**
     * Sets the archiveFullname
     *
     * @param archiveFullname The archiveFullname to set
     */
    public void setArchiveFullname(String archiveFullname) {
        this.archiveFullname = archiveFullname;
    }

    /**
     * Gets the draftsFullname
     *
     * @return The draftsFullname
     */
    public String getDraftsFullname() {
        return draftsFullname;
    }

    /**
     * Sets the draftsFullname
     *
     * @param draftsFullname The draftsFullname to set
     */
    public void setDraftsFullname(String draftsFullname) {
        this.draftsFullname = draftsFullname;
    }

    /**
     * Gets the sentFullname
     *
     * @return The sentFullname
     */
    public String getSentFullname() {
        return sentFullname;
    }

    /**
     * Sets the sentFullname
     *
     * @param sentFullname The sentFullname to set
     */
    public void setSentFullname(String sentFullname) {
        this.sentFullname = sentFullname;
    }

    /**
     * Gets the spamFullname
     *
     * @return The spamFullname
     */
    public String getSpamFullname() {
        return spamFullname;
    }

    /**
     * Sets the spamFullname
     *
     * @param spamFullname The spamFullname to set
     */
    public void setSpamFullname(String spamFullname) {
        this.spamFullname = spamFullname;
    }

    /**
     * Gets the trashFullname
     *
     * @return The trashFullname
     */
    public String getTrashFullname() {
        return trashFullname;
    }

    /**
     * Sets the trashFullname
     *
     * @param trashFullname The trashFullname to set
     */
    public void setTrashFullname(String trashFullname) {
        this.trashFullname = trashFullname;
    }

    /**
     * Gets the confirmedSpamFullname
     *
     * @return The confirmedSpamFullname
     */
    public String getConfirmedSpamFullname() {
        return confirmedSpamFullname;
    }

    /**
     * Sets the confirmedSpamFullname
     *
     * @param confirmedSpamFullname The confirmedSpamFullname to set
     */
    public void setConfirmedSpamFullname(String confirmedSpamFullname) {
        this.confirmedSpamFullname = confirmedSpamFullname;
    }

    /**
     * Gets the confirmedHamFullname
     *
     * @return The confirmedHamFullname
     */
    public String getConfirmedHamFullname() {
        return confirmedHamFullname;
    }

    /**
     * Sets the confirmedHamFullname
     *
     * @param confirmedHamFullname The confirmedHamFullname to set
     */
    public void setConfirmedHamFullname(String confirmedHamFullname) {
        this.confirmedHamFullname = confirmedHamFullname;
    }

}
