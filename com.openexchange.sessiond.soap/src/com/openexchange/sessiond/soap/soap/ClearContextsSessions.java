
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
    "contextIds",
    "auth"
})
@XmlRootElement(name = "clearContextsSessions")
public class ClearContextsSessions {

    @XmlElement(nillable = true)
    protected String contextIds;
    @XmlElement(nillable = true)
    protected Credentials auth;

    /**
     * Gets the contextIds
     *
     * @return The contextIds
     */
    public String getContextIds() {
        return contextIds;
    }

    /**
     * Sets the contextIds
     *
     * @param contextIds The contextIds to set
     */
    public void setContextIds(String contextIds) {
        this.contextIds = contextIds;
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
