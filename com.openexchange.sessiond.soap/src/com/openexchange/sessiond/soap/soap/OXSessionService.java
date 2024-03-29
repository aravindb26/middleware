package com.openexchange.sessiond.soap.soap;

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
@WebServiceClient(name = "OXSessionService",
                  // wsdlLocation = "null",
                  targetNamespace = "http://soap.sessiond.openexchange.com")
public class OXSessionService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://soap.sessiond.openexchange.com", "OXSessionService");
    public final static QName OXSessionServiceHttpsEndpoint = new QName("http://soap.sessiond.openexchange.com", "OXSessionServiceHttpsEndpoint");
    public final static QName OXSessionServiceHttpsSoap12Endpoint = new QName("http://soap.sessiond.openexchange.com", "OXSessionServiceHttpsSoap12Endpoint");
    public final static QName OXSessionServiceHttpSoap11Endpoint = new QName("http://soap.sessiond.openexchange.com", "OXSessionServiceHttpSoap11Endpoint");
    public final static QName OXSessionServiceHttpsSoap11Endpoint = new QName("http://soap.sessiond.openexchange.com", "OXSessionServiceHttpsSoap11Endpoint");
    public final static QName OXSessionServiceHttpEndpoint = new QName("http://soap.sessiond.openexchange.com", "OXSessionServiceHttpEndpoint");
    public final static QName OXSessionServiceHttpSoap12Endpoint = new QName("http://soap.sessiond.openexchange.com", "OXSessionServiceHttpSoap12Endpoint");
    static {
        WSDL_LOCATION = null;
    }

    public OXSessionService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public OXSessionService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public OXSessionService() {
        super(WSDL_LOCATION, SERVICE);
    }


    /**
     *
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpsEndpoint")
    public OXSessionServicePortType getOXSessionServiceHttpsEndpoint() {
        return super.getPort(OXSessionServiceHttpsEndpoint, OXSessionServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpsEndpoint")
    public OXSessionServicePortType getOXSessionServiceHttpsEndpoint(WebServiceFeature... features) {
        return super.getPort(OXSessionServiceHttpsEndpoint, OXSessionServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpsSoap12Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpsSoap12Endpoint() {
        return super.getPort(OXSessionServiceHttpsSoap12Endpoint, OXSessionServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpsSoap12Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpsSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXSessionServiceHttpsSoap12Endpoint, OXSessionServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpSoap11Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpSoap11Endpoint() {
        return super.getPort(OXSessionServiceHttpSoap11Endpoint, OXSessionServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpSoap11Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXSessionServiceHttpSoap11Endpoint, OXSessionServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpsSoap11Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpsSoap11Endpoint() {
        return super.getPort(OXSessionServiceHttpsSoap11Endpoint, OXSessionServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpsSoap11Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpsSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXSessionServiceHttpsSoap11Endpoint, OXSessionServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpEndpoint")
    public OXSessionServicePortType getOXSessionServiceHttpEndpoint() {
        return super.getPort(OXSessionServiceHttpEndpoint, OXSessionServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpEndpoint")
    public OXSessionServicePortType getOXSessionServiceHttpEndpoint(WebServiceFeature... features) {
        return super.getPort(OXSessionServiceHttpEndpoint, OXSessionServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpSoap12Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpSoap12Endpoint() {
        return super.getPort(OXSessionServiceHttpSoap12Endpoint, OXSessionServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXSessionServicePortType
     */
    @WebEndpoint(name = "OXSessionServiceHttpSoap12Endpoint")
    public OXSessionServicePortType getOXSessionServiceHttpSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXSessionServiceHttpSoap12Endpoint, OXSessionServicePortType.class, features);
    }

}
