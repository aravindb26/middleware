
package com.openexchange.admin.soap.reseller.resource.reseller.soap;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-06T11:10:24.838+02:00
 * Generated source version: 2.6.0
 */

@WebFault(name = "NoSuchResourceException", targetNamespace = "http://soap.reseller.admin.openexchange.com")
public class NoSuchResourceException_Exception extends java.lang.Exception {

    private com.openexchange.admin.soap.reseller.resource.reseller.soap.NoSuchResourceException noSuchResourceException;

    public NoSuchResourceException_Exception() {
        super();
    }

    public NoSuchResourceException_Exception(String message) {
        super(message);
    }

    public NoSuchResourceException_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchResourceException_Exception(String message, com.openexchange.admin.soap.reseller.resource.reseller.soap.NoSuchResourceException noSuchResourceException) {
        super(message);
        this.noSuchResourceException = noSuchResourceException;
    }

    public NoSuchResourceException_Exception(String message, com.openexchange.admin.soap.reseller.resource.reseller.soap.NoSuchResourceException noSuchResourceException, Throwable cause) {
        super(message, cause);
        this.noSuchResourceException = noSuchResourceException;
    }

    public com.openexchange.admin.soap.reseller.resource.reseller.soap.NoSuchResourceException getFaultInfo() {
        return this.noSuchResourceException;
    }
}
