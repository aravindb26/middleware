
package com.openexchange.sessiond.soap.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse f\u00fcr anonymous complex type.
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "noSuchUserException"
})
@XmlRootElement(name = "NoSuchUserException")
public class NoSuchUserException {

    @XmlElement(name = "NoSuchUserException", nillable = true)
    protected com.openexchange.sessiond.soap.exceptions.NoSuchUserException noSuchUserException;

    /**
     * Ruft den Wert der noSuchUserException-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link com.openexchange.sessiond.soap.exceptions.NoSuchUserException }
     *
     */
    public com.openexchange.sessiond.soap.exceptions.NoSuchUserException getNoSuchUserException() {
        return noSuchUserException;
    }

    /**
     * Legt den Wert der noSuchUserException-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link com.openexchange.sessiond.soap.exceptions.NoSuchUserException }
     *
     */
    public void setNoSuchUserException(com.openexchange.sessiond.soap.exceptions.NoSuchUserException value) {
        this.noSuchUserException = value;
    }

}
