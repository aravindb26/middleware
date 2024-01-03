
package com.openexchange.sessiond.soap.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse f\u00fcr anonymous complex type.
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "invalidCredentialsException"
})
@XmlRootElement(name = "InvalidCredentialsException")
public class InvalidCredentialsException {

    @XmlElement(name = "InvalidCredentialsException", nillable = true)
    protected com.openexchange.sessiond.soap.exceptions.InvalidCredentialsException invalidCredentialsException;

    /**
     * Ruft den Wert der invalidCredentialsException-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link com.openexchange.sessiond.soap.exceptions.InvalidCredentialsException }
     *
     */
    public com.openexchange.sessiond.soap.exceptions.InvalidCredentialsException getInvalidCredentialsException() {
        return invalidCredentialsException;
    }

    /**
     * Legt den Wert der invalidCredentialsException-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link com.openexchange.sessiond.soap.exceptions.InvalidCredentialsException }
     *
     */
    public void setInvalidCredentialsException(com.openexchange.sessiond.soap.exceptions.InvalidCredentialsException value) {
        this.invalidCredentialsException = value;
    }

}
