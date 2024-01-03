
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
    "sessionId",
    "auth"
})
@XmlRootElement(name = "clearUserSession")
public class ClearUserSession {

    @XmlElement(nillable = true)
    protected String sessionId;
    @XmlElement(nillable = true)
    protected Credentials auth;

    /**
     * Gets the sessionId
     *
     * @return The sessionId
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Sets the sessionId
     *
     * @param sessionId The sessionId to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
