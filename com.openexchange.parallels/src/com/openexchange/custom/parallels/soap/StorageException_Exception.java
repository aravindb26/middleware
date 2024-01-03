
package com.openexchange.custom.parallels.soap;

import javax.xml.ws.WebFault;


/**
 * This class was generated by Apache CXF 2.6.1
 * 2012-07-11T15:32:34.682+02:00
 * Generated source version: 2.6.1
 */

@WebFault(name = "StorageException", targetNamespace = "http://soap.parallels.custom.openexchange.com")
public class StorageException_Exception extends java.lang.Exception {
    
    private static final long serialVersionUID = -8162722577812839341L;
    
    private com.openexchange.custom.parallels.soap.StorageException storageException;

    public StorageException_Exception() {
        super();
    }
    
    public StorageException_Exception(String message) {
        super(message);
    }
    
    public StorageException_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public StorageException_Exception(String message, com.openexchange.custom.parallels.soap.StorageException storageException) {
        super(message);
        this.storageException = storageException;
    }

    public StorageException_Exception(String message, com.openexchange.custom.parallels.soap.StorageException storageException, Throwable cause) {
        super(message, cause);
        this.storageException = storageException;
    }

    public com.openexchange.custom.parallels.soap.StorageException getFaultInfo() {
        return this.storageException;
    }
}