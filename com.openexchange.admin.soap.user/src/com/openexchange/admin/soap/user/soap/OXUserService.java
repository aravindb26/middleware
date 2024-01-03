package com.openexchange.admin.soap.user.soap;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-01T18:24:59.105+02:00
 * Generated source version: 2.6.0
 *
 */
@WebServiceClient(name = "OXUserService",
                  // wsdlLocation = "null",
                  targetNamespace = "http://soap.admin.openexchange.com")
public class OXUserService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://soap.admin.openexchange.com", "OXUserService");
    public final static QName OXUserServiceHttpsEndpoint = new QName("http://soap.admin.openexchange.com", "OXUserServiceHttpsEndpoint");
    public final static QName OXUserServiceHttpsSoap12Endpoint = new QName("http://soap.admin.openexchange.com", "OXUserServiceHttpsSoap12Endpoint");
    public final static QName OXUserServiceHttpSoap11Endpoint = new QName("http://soap.admin.openexchange.com", "OXUserServiceHttpSoap11Endpoint");
    public final static QName OXUserServiceHttpsSoap11Endpoint = new QName("http://soap.admin.openexchange.com", "OXUserServiceHttpsSoap11Endpoint");
    public final static QName OXUserServiceHttpEndpoint = new QName("http://soap.admin.openexchange.com", "OXUserServiceHttpEndpoint");
    public final static QName OXUserServiceHttpSoap12Endpoint = new QName("http://soap.admin.openexchange.com", "OXUserServiceHttpSoap12Endpoint");
    static {
        WSDL_LOCATION = null;
    }

    public OXUserService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public OXUserService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public OXUserService() {
        super(WSDL_LOCATION, SERVICE);
    }


    /**
     *
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpsEndpoint")
    public OXUserServicePortType getOXUserServiceHttpsEndpoint() {
        return super.getPort(OXUserServiceHttpsEndpoint, OXUserServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpsEndpoint")
    public OXUserServicePortType getOXUserServiceHttpsEndpoint(WebServiceFeature... features) {
        return super.getPort(OXUserServiceHttpsEndpoint, OXUserServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpsSoap12Endpoint")
    public OXUserServicePortType getOXUserServiceHttpsSoap12Endpoint() {
        return super.getPort(OXUserServiceHttpsSoap12Endpoint, OXUserServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpsSoap12Endpoint")
    public OXUserServicePortType getOXUserServiceHttpsSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXUserServiceHttpsSoap12Endpoint, OXUserServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpSoap11Endpoint")
    public OXUserServicePortType getOXUserServiceHttpSoap11Endpoint() {
        return super.getPort(OXUserServiceHttpSoap11Endpoint, OXUserServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpSoap11Endpoint")
    public OXUserServicePortType getOXUserServiceHttpSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXUserServiceHttpSoap11Endpoint, OXUserServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpsSoap11Endpoint")
    public OXUserServicePortType getOXUserServiceHttpsSoap11Endpoint() {
        return super.getPort(OXUserServiceHttpsSoap11Endpoint, OXUserServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpsSoap11Endpoint")
    public OXUserServicePortType getOXUserServiceHttpsSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXUserServiceHttpsSoap11Endpoint, OXUserServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpEndpoint")
    public OXUserServicePortType getOXUserServiceHttpEndpoint() {
        return super.getPort(OXUserServiceHttpEndpoint, OXUserServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpEndpoint")
    public OXUserServicePortType getOXUserServiceHttpEndpoint(WebServiceFeature... features) {
        return super.getPort(OXUserServiceHttpEndpoint, OXUserServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpSoap12Endpoint")
    public OXUserServicePortType getOXUserServiceHttpSoap12Endpoint() {
        return super.getPort(OXUserServiceHttpSoap12Endpoint, OXUserServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXUserServicePortType
     */
    @WebEndpoint(name = "OXUserServiceHttpSoap12Endpoint")
    public OXUserServicePortType getOXUserServiceHttpSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXUserServiceHttpSoap12Endpoint, OXUserServicePortType.class, features);
    }

}
