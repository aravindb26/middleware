
/**
 * Please modify this class to meet your needs
 * This class is not complete
 */

package com.openexchange.custom.parallels.soap;

import java.util.logging.Logger;
import javax.jws.WebService;

/**
 * This class was generated by Apache CXF 2.6.1
 * 2012-07-11T15:32:34.709+02:00
 * Generated source version: 2.6.1
 *
 */

@WebService(
                      serviceName = "OXServerService",
                      portName = "OXServerServiceHttpsSoap12Endpoint",
                      targetNamespace = "http://soap.parallels.custom.openexchange.com",
                      // wsdlLocation = "null",
                      endpointInterface = "com.openexchange.custom.parallels.soap.OXServerServicePortType")

public class OXServerServicePortTypeImpl1 implements OXServerServicePortType {

    private static final Logger LOG = Logger.getLogger(OXServerServicePortTypeImpl.class.getName());

    @Override
    public java.util.List<com.openexchange.custom.parallels.soap.Bundle> getServerBundleList(com.openexchange.custom.parallels.soap.rmi.Credentials auth) throws InvalidCredentialsException_Exception , InvalidDataException_Exception , RemoteException_Exception , StorageException_Exception    {
        LOG.info("Executing operation getServerBundleList");
        System.out.println(auth);
        try {
            java.util.List<com.openexchange.custom.parallels.soap.Bundle> _return = null;
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new InvalidCredentialsException_Exception("InvalidCredentialsException...");
        //throw new InvalidDataException_Exception("InvalidDataException...");
        //throw new RemoteException_Exception("RemoteException...");
        //throw new StorageException_Exception("StorageException...");
    }

    @Override
    public java.lang.String getServerVersion(com.openexchange.custom.parallels.soap.rmi.Credentials auth) throws InvalidCredentialsException_Exception , InvalidDataException_Exception , RemoteException_Exception , StorageException_Exception    {
        LOG.info("Executing operation getServerVersion");
        System.out.println(auth);
        try {
            java.lang.String _return = "";
            return _return;
        } catch (java.lang.Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
        //throw new InvalidCredentialsException_Exception("InvalidCredentialsException...");
        //throw new InvalidDataException_Exception("InvalidDataException...");
        //throw new RemoteException_Exception("RemoteException...");
        //throw new StorageException_Exception("StorageException...");
    }

}
