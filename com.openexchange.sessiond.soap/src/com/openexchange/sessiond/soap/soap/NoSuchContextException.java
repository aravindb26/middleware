
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
    "noSuchContextException"
})
@XmlRootElement(name = "NoSuchContextException")
public class NoSuchContextException {

    @XmlElement(name = "NoSuchContextException", nillable = true)
    protected com.openexchange.sessiond.soap.exceptions.NoSuchContextException noSuchContextException;

    /**
     * Ruft den Wert der noSuchContextException-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link com.openexchange.sessiond.soap.exceptions.NoSuchContextException }
     *
     */
    public com.openexchange.sessiond.soap.exceptions.NoSuchContextException getNoSuchContextException() {
        return noSuchContextException;
    }

    /**
     * Legt den Wert der noSuchContextException-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link com.openexchange.sessiond.soap.exceptions.NoSuchContextException }
     *
     */
    public void setNoSuchContextException(com.openexchange.sessiond.soap.exceptions.NoSuchContextException value) {
        this.noSuchContextException = value;
    }

}
