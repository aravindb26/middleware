
package com.openexchange.sessiond.soap.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.sessiond.soap.dataobjects.Credentials;

/**
 * <p>Java-Klasse f\u00fcr anonymous complex type.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "contextId",
    "userId",
    "auth"
})
@XmlRootElement(name = "clearUserSessions")
public class ClearUserSessions {

    @XmlElement(nillable = true)
    protected Integer contextId;
    @XmlElement(nillable = true)
    protected Integer userId;
    @XmlElement(nillable = true)
    protected Credentials auth;

    /**
     * Gets the contextId
     *
     * @return The contextId
     */
    public Integer getContextId() {
        return contextId;
    }

    /**
     * Sets the contextId
     *
     * @param contextId The contextId to set
     */
    public void setContextId(Integer contextId) {
        this.contextId = contextId;
    }

    /**
     * Gets the userId
     *
     * @return The userId
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * Sets the userId
     *
     * @param userId The userId to set
     */
    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    /**
     * Ruft den Wert der auth-Eigenschaft ab.
     *
     * @return
     *         possible object is
     *         {@link Credentials }
     *
     */
    public Credentials getAuth() {
        return auth;
    }

    /**
     * Legt den Wert der auth-Eigenschaft fest.
     *
     * @param value
     *            allowed object is
     *            {@link Credentials }
     *
     */
    public void setAuth(Credentials value) {
        this.auth = value;
    }

}
