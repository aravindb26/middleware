/*
 * @copyright Copyright (c) OX Software GmbH, Germany <info@open-xchange.com>
 * @license AGPL-3.0
 *
 * This code is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OX App Suite.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>.
 *
 * Any use of the work other than as authorized under this license or copyright law is prohibited.
 *
 */

package com.openexchange.soap.cxf.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import javax.jws.WebService;
import org.apache.cxf.binding.soap.interceptor.SoapActionInInterceptor;
import org.apache.cxf.binding.xml.interceptor.XMLFaultOutInterceptor;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.wsdl.interceptors.DocLiteralInInterceptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import com.openexchange.soap.cxf.ExceptionUtils;
import com.openexchange.soap.cxf.WebserviceName;
import com.openexchange.soap.cxf.interceptor.ChangingStatusCodeInterceptor;
import com.openexchange.soap.cxf.interceptor.LoggingInInterceptor;
import com.openexchange.soap.cxf.interceptor.LoggingOutInterceptor;
import com.openexchange.soap.cxf.interceptor.MetricsInterceptor;

/**
 * {@link WebserviceCollector}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class WebserviceCollector implements ServiceListener {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(WebserviceCollector.class);

    private static final String WEBSERVICE_NAME = "WebserviceName";

    private final ConcurrentMap<String, Server> endpoints;
    private final BundleContext context;
    private volatile boolean open;
    private final String baseUri;
    private final boolean considerClientFaults;

    /**
     * Initializes a new {@link WebserviceCollector}.
     *
     * @param baseUri The base uri of the soap endpoints
     * @param context The bundle context
     */
    public WebserviceCollector(String baseUri, BundleContext context) {
        super();
        this.baseUri = baseUri;
        endpoints = new ConcurrentHashMap<String, Server>();
        this.context = context;
        considerClientFaults = false;
    }

    @Override
    public void serviceChanged(final ServiceEvent event) {
        if (!open) {
            return;
        }
        final int type = event.getType();
        if (ServiceEvent.REGISTERED == type) {
            add(event.getServiceReference());
        } else if (ServiceEvent.UNREGISTERING == type) {
            remove(event.getServiceReference());
        }
    }

    /**
     * Opens this collector.
     */
    public void open() {
        try {
            for (final ServiceReference<?> serviceReference : context.getAllServiceReferences(null, null)) {
                add(serviceReference);
            }
        } catch (@SuppressWarnings("unused") InvalidSyntaxException e) {
            // Impossible, no filter specified.
        }

        open = true;
    }

    /**
     * Closes this collector.
     */
    public void close() {
        open = false;
        for (final Entry<String, Server> entry : new ArrayList<Entry<String, Server>>(endpoints.entrySet())) {
            remove(entry.getKey());
        }
    }

    private void remove(final ServiceReference<?> ref) {
        final Object service = context.getService(ref);
        if (isWebservice(service)) {
            final String name = getName(ref, service);
            remove(name);
        } else {
            context.ungetService(ref);
        }
    }

    private void add(final ServiceReference<?> ref) {
        final Object service = context.getService(ref);
        if (isWebservice(service)) {
            final String name = getName(ref, service);
            replace(name, service);
        } else {
            context.ungetService(ref);
        }
    }

    private String getName(final ServiceReference<?> ref, final Object service) {
        // If an annotation is present with the name, use that
        {
            final WebserviceName webserviceName = service.getClass().getAnnotation(WebserviceName.class);
            if (webserviceName != null) {
                return webserviceName.value();
            }
        }
        // If a service property for WebserviceName is present, use that
        {
            final Object name = ref.getProperty(WEBSERVICE_NAME);
            final String sName = null == name ? null : name.toString();
            if (!com.openexchange.java.Strings.isEmpty(sName)) {
                return sName;
            }
        }
        // Next try the WebService annotation
        {
            final WebService webService = service.getClass().getAnnotation(WebService.class);
            String serviceName = webService.serviceName();
            if (!com.openexchange.java.Strings.isEmpty(serviceName)) {
                return serviceName;
            }
            serviceName = webService.name();
            if (!com.openexchange.java.Strings.isEmpty(serviceName)) {
                return serviceName;
            }
        }
        // Else use the class name
        return service.getClass().getSimpleName();
    }

    private void remove(final String name) {
        final Server endpoint = endpoints.remove(name);
        if (endpoint != null) {
            endpoint.stop();
        }
    }

    private void replace(final String name, final Object service) {
        String address = '/' + name;
        Server oldEndpoint = null;
        try {
            // Publish new server endpoint
            JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
            factory.setServiceBean(service);
            factory.setAddress(address);
            Server server = factory.create();
            {
                if (null != baseUri) {
                    // @formatter:off
                    server.getEndpoint()
                          .getEndpointInfo()
                          .setProperty(WSDLGetUtils.PUBLISHED_ENDPOINT_URL, baseUri + address);
                    // @formatter:on
                }

                // Alter server's in-interceptors
                List<Interceptor<? extends Message>> inInterceptors = server.getEndpoint().getBinding().getInInterceptors();

                // Replace default DocLiteralInInterceptor with one which is less strict
                replaceInterceptor(inInterceptors, DocLiteralInInterceptor.class, () -> new com.openexchange.soap.cxf.interceptor.DocLiteralInInterceptor());

                // Replace default SoapActionInInterceptor with one which removes soap header
                replaceInterceptor(inInterceptors, SoapActionInInterceptor.class, () -> new com.openexchange.soap.cxf.interceptor.SoapActionInInterceptor());

                // Replace default WSDLGetInterceptor with one which is https aware
                replaceInterceptor(server.getEndpoint().getInInterceptors(), WSDLGetInterceptor.class, () -> new com.openexchange.soap.cxf.interceptor.HttpsAwareWSDLGetInterceptor());

                // Alter server's out-fault-interceptors
                // Replace default XMLFaultOutInterceptor with custom one
                replaceInterceptor(server.getEndpoint().getBinding().getOutFaultInterceptors(), XMLFaultOutInterceptor.class, () -> new com.openexchange.soap.cxf.interceptor.XMLFaultOutInterceptor());

                // Add logging interceptors
                server.getEndpoint().getInInterceptors().add(new LoggingInInterceptor());
                server.getEndpoint().getOutInterceptors().add(new LoggingOutInterceptor());
                // Add metric interceptors
                server.getEndpoint().getInInterceptors().add(new MetricsInterceptor(Phase.RECEIVE));
                server.getEndpoint().getOutInterceptors().add(new MetricsInterceptor(Phase.SEND));
                server.getEndpoint().getOutFaultInterceptors().add(new MetricsInterceptor(Phase.SEND));
                if (considerClientFaults) {
                    // Add interceptor to send proper HTTP status codes on exception path
                    server.getEndpoint().getOutFaultInterceptors().add(new ChangingStatusCodeInterceptor());
                }
            }
            oldEndpoint = endpoints.replace(name, server);
            LOG.info("Publishing endpoint succeeded. Published \"{}\" under address \"{}\".", name, address);
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            LOG.error("Publishing endpoint failed. Couldn't publish \"{}\" under address \"{}\".", name, address, t);
        }
        if (oldEndpoint != null) {
            oldEndpoint.stop();
        }
    }

    /**
     * Replaces an existing interceptor with the given one
     *
     * @param listOfInterceptors The list of interceptors (e.g. in-interceptors)
     * @param clazz2find The class to find
     * @param interceptorSupplier A supplier for the replacement interceptor
     */
    private void replaceInterceptor(List<Interceptor<? extends Message>> listOfInterceptors, Class<? extends Interceptor<?>> clazz2find, Supplier<? extends Interceptor<?>> interceptorSupplier) {
        boolean found = false;
        int index = 0;
        for (final Interceptor<? extends Message> interceptor : listOfInterceptors) {
            if (clazz2find.isInstance(interceptor)) {
                found = true;
                break;
            }
            index++;
        }
        if (found) {
            listOfInterceptors.remove(index);
            listOfInterceptors.add(index, interceptorSupplier.get());
        }
    }

    /**
     * Checks if the given service is a {@link WebService}
     *
     * @param service The service to check
     * @return <code>true</code> if it is a {@link WebService}, <code>false</code> otherwise
     */
    private boolean isWebservice(final Object service) {
        try {
            final Class<? extends Object> clazz = service.getClass();
            return null != clazz.getAnnotation(WebService.class);
        } catch (@SuppressWarnings("unused") Exception e) {
            return false;
        }
    }
}
