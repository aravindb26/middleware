
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
    "missingServiceException"
})
@XmlRootElement(name = "MissingServiceException")
public class MissingServiceException {

    @XmlElement(name = "MissingServiceException", nillable = true)
    protected com.openexchange.sessiond.soap.exceptions.MissingServiceException missingServiceException;

    /**
     * Ruft den Wert der missingServiceException-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link com.openexchange.sessiond.soap.exceptions.MissingServiceException }
     *
     */
    public com.openexchange.sessiond.soap.exceptions.MissingServiceException getMissingServiceException() {
        return missingServiceException;
    }

    /**
     * Legt den Wert der missingServiceException-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link com.openexchange.sessiond.soap.exceptions.MissingServiceException }
     *
     */
    public void setMissingServiceException(com.openexchange.sessiond.soap.exceptions.MissingServiceException value) {
        this.missingServiceException = value;
    }

}
