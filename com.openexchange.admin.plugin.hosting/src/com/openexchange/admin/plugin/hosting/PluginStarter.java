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

package com.openexchange.admin.plugin.hosting;

import static com.openexchange.java.Autoboxing.I;
import java.rmi.Remote;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.admin.plugin.hosting.rmi.impl.OXContextGroup;
import com.openexchange.admin.plugin.hosting.service.impl.OXContextServiceImpl;
import com.openexchange.admin.rmi.OXContextGroupInterface;
import com.openexchange.admin.rmi.service.OXContextService;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;

public class PluginStarter {

    private BundleContext context;
    private final List<ServiceRegistration<?>> services = new LinkedList<ServiceRegistration<?>>();

    /**
     * Initializes a new {@link PluginStarter}.
     */
    public PluginStarter() {
        super();
    }

    public void start(BundleContext context) {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PluginStarter.class);
        try {
            this.context = context;

            // Create all OLD Objects and bind export them
            com.openexchange.admin.plugin.hosting.rmi.impl.OXContext oxctx_v2 = new com.openexchange.admin.plugin.hosting.rmi.impl.OXContext();

            // bind all NEW Objects to registry
            Dictionary<String, Object> properties = new Hashtable<String, Object>(2);
            properties.put("RMIName", com.openexchange.admin.rmi.OXContextInterface.RMI_NAME);
            LeanConfigurationService configurationService = context.getService(context.getServiceReference(LeanConfigurationService.class));
            int port = configurationService.getIntProperty(DefaultProperty.valueOf("com.openexchange.admin.rmi.oxcontext.port", I(-1)));
            if (port > 0) {
                properties.put("RMIPort", I(port));
            }
            services.add(context.registerService(Remote.class, oxctx_v2, properties));

            // Register RMI interface for ContextGroup
            properties = new Hashtable<String, Object>(2);
            properties.put("RMIName", OXContextGroupInterface.RMI_NAME);
            services.add(context.registerService(Remote.class, new OXContextGroup(), properties));

            services.add(context.registerService(OXContextService.class, new OXContextServiceImpl(oxctx_v2), null));
        } catch (Exception e) {
            logger.error("Error while creating one instance for RMI interface", e);
            throw e;
        }
    }

    public void stop() {
        for (ServiceRegistration<?> registration : services) {
            context.ungetService(registration.getReference());
        }
    }

}
