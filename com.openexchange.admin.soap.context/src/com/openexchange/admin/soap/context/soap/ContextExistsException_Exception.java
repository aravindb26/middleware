
package com.openexchange.admin.soap.context.soap;

import javax.xml.ws.WebFault;
import com.openexchange.admin.rmi.exceptions.ContextExistsException;


/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-01T18:39:07.985+02:00
 * Generated source version: 2.6.0
 */

@WebFault(name = "ContextExistsException", targetNamespace = "http://soap.admin.openexchange.com")
public class ContextExistsException_Exception extends java.lang.Exception {

    private com.openexchange.admin.soap.context.soap.ContextExistsException contextExistsException;

    public ContextExistsException_Exception() {
        super();
    }

    public ContextExistsException_Exception(String message) {
        super(message);
    }

    public ContextExistsException_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public ContextExistsException_Exception(String message, com.openexchange.admin.soap.context.soap.ContextExistsException contextExistsException) {
        super(message);
        this.contextExistsException = contextExistsException;
    }

    public ContextExistsException_Exception(String message, com.openexchange.admin.soap.context.soap.ContextExistsException contextExistsException, Throwable cause) {
        super(message, cause);
        this.contextExistsException = contextExistsException;
    }

    public com.openexchange.admin.soap.context.soap.ContextExistsException getFaultInfo() {
        return this.contextExistsException;
    }


    public static ContextExistsException_Exception faultFor(ContextExistsException e) {
        com.openexchange.admin.soap.context.soap.ContextExistsException faultDetail = new com.openexchange.admin.soap.context.soap.ContextExistsException();
        com.openexchange.admin.soap.context.exceptions.ContextExistsException value = new com.openexchange.admin.soap.context.exceptions.ContextExistsException();
        faultDetail.setContextExistsException(value);
        return new ContextExistsException_Exception(e.getMessage(), faultDetail, e);
    }
}