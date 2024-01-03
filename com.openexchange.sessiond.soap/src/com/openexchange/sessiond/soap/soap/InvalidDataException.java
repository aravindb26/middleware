
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
    "invalidDataException"
})
@XmlRootElement(name = "InvalidDataException")
public class InvalidDataException {

    @XmlElement(name = "InvalidDataException", nillable = true)
    protected com.openexchange.sessiond.soap.exceptions.InvalidDataException invalidDataException;

    /**
     * Ruft den Wert der invalidDataException-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link com.openexchange.sessiond.soap.exceptions.InvalidDataException }
     *
     */
    public com.openexchange.sessiond.soap.exceptions.InvalidDataException getInvalidDataException() {
        return invalidDataException;
    }

    /**
     * Legt den Wert der invalidDataException-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link com.openexchange.sessiond.soap.exceptions.InvalidDataException }
     *
     */
    public void setInvalidDataException(com.openexchange.sessiond.soap.exceptions.InvalidDataException value) {
        this.invalidDataException = value;
    }

}
