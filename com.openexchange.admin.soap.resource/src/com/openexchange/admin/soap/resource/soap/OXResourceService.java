package com.openexchange.admin.soap.resource.soap;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-01T18:56:00.643+02:00
 * Generated source version: 2.6.0
 *
 */
@WebServiceClient(name = "OXResourceService",
                  // wsdlLocation = "null",
                  targetNamespace = "http://soap.admin.openexchange.com")
public class OXResourceService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://soap.admin.openexchange.com", "OXResourceService");
    public final static QName OXResourceServiceHttpSoap12Endpoint = new QName("http://soap.admin.openexchange.com", "OXResourceServiceHttpSoap12Endpoint");
    public final static QName OXResourceServiceHttpsSoap11Endpoint = new QName("http://soap.admin.openexchange.com", "OXResourceServiceHttpsSoap11Endpoint");
    public final static QName OXResourceServiceHttpsSoap12Endpoint = new QName("http://soap.admin.openexchange.com", "OXResourceServiceHttpsSoap12Endpoint");
    public final static QName OXResourceServiceHttpSoap11Endpoint = new QName("http://soap.admin.openexchange.com", "OXResourceServiceHttpSoap11Endpoint");
    public final static QName OXResourceServiceHttpsEndpoint = new QName("http://soap.admin.openexchange.com", "OXResourceServiceHttpsEndpoint");
    public final static QName OXResourceServiceHttpEndpoint = new QName("http://soap.admin.openexchange.com", "OXResourceServiceHttpEndpoint");
    static {
        WSDL_LOCATION = null;
    }

    public OXResourceService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public OXResourceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public OXResourceService() {
        super(WSDL_LOCATION, SERVICE);
    }


    /**
     *
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpSoap12Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpSoap12Endpoint() {
        return super.getPort(OXResourceServiceHttpSoap12Endpoint, OXResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpSoap12Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResourceServiceHttpSoap12Endpoint, OXResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpsSoap11Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpsSoap11Endpoint() {
        return super.getPort(OXResourceServiceHttpsSoap11Endpoint, OXResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpsSoap11Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpsSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResourceServiceHttpsSoap11Endpoint, OXResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpsSoap12Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpsSoap12Endpoint() {
        return super.getPort(OXResourceServiceHttpsSoap12Endpoint, OXResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpsSoap12Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpsSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResourceServiceHttpsSoap12Endpoint, OXResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpSoap11Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpSoap11Endpoint() {
        return super.getPort(OXResourceServiceHttpSoap11Endpoint, OXResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpSoap11Endpoint")
    public OXResourceServicePortType getOXResourceServiceHttpSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResourceServiceHttpSoap11Endpoint, OXResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpsEndpoint")
    public OXResourceServicePortType getOXResourceServiceHttpsEndpoint() {
        return super.getPort(OXResourceServiceHttpsEndpoint, OXResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpsEndpoint")
    public OXResourceServicePortType getOXResourceServiceHttpsEndpoint(WebServiceFeature... features) {
        return super.getPort(OXResourceServiceHttpsEndpoint, OXResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpEndpoint")
    public OXResourceServicePortType getOXResourceServiceHttpEndpoint() {
        return super.getPort(OXResourceServiceHttpEndpoint, OXResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResourceServicePortType
     */
    @WebEndpoint(name = "OXResourceServiceHttpEndpoint")
    public OXResourceServicePortType getOXResourceServiceHttpEndpoint(WebServiceFeature... features) {
        return super.getPort(OXResourceServiceHttpEndpoint, OXResourceServicePortType.class, features);
    }

}
