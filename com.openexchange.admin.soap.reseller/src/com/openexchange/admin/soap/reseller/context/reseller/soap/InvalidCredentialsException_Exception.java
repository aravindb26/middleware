
package com.openexchange.admin.soap.reseller.context.reseller.soap;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-06T11:28:45.978+02:00
 * Generated source version: 2.6.0
 */

@WebFault(name = "InvalidCredentialsException", targetNamespace = "http://soap.reseller.admin.openexchange.com")
public class InvalidCredentialsException_Exception extends java.lang.Exception {

    private com.openexchange.admin.soap.reseller.context.reseller.soap.InvalidCredentialsException invalidCredentialsException;

    public InvalidCredentialsException_Exception() {
        super();
    }

    public InvalidCredentialsException_Exception(String message) {
        super(message);
    }

    public InvalidCredentialsException_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCredentialsException_Exception(String message, com.openexchange.admin.soap.reseller.context.reseller.soap.InvalidCredentialsException invalidCredentialsException) {
        super(message);
        this.invalidCredentialsException = invalidCredentialsException;
    }

    public InvalidCredentialsException_Exception(String message, com.openexchange.admin.soap.reseller.context.reseller.soap.InvalidCredentialsException invalidCredentialsException, Throwable cause) {
        super(message, cause);
        this.invalidCredentialsException = invalidCredentialsException;
    }

    public com.openexchange.admin.soap.reseller.context.reseller.soap.InvalidCredentialsException getFaultInfo() {
        return this.invalidCredentialsException;
    }
}
