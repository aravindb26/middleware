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

package com.openexchange.admin.soap.secondaryaccount.osgi;

import java.rmi.Remote;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.admin.rmi.OXSecondaryAccountInterface;
import com.openexchange.admin.soap.secondaryaccount.soap.OXSecondaryAccountServicePortType;
import com.openexchange.admin.soap.secondaryaccount.soap.OXSecondaryAccountServicePortTypeImpl;


/**
 * {@link OXSecondaryAccountInterfaceTracker}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class OXSecondaryAccountInterfaceTracker implements ServiceTrackerCustomizer<Remote, Remote> {

    private final BundleContext context;
    private ServiceRegistration<OXSecondaryAccountServicePortType> serviceRegistration;

    /**
     * Initializes a new {@link OXSecondaryAccountInterfaceTracker}.
     *
     * @param context The bundle context
     */
    public OXSecondaryAccountInterfaceTracker(BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public synchronized Remote addingService(final ServiceReference<Remote> reference) {
        final Remote service = context.getService(reference);
        if (!(service instanceof OXSecondaryAccountInterface)) {
            context.ungetService(reference);
            return null;
        }
        OXSecondaryAccountServicePortTypeImpl secondaryAccountEndpoint = new OXSecondaryAccountServicePortTypeImpl((OXSecondaryAccountInterface) service);
        serviceRegistration = context.registerService(OXSecondaryAccountServicePortType.class, secondaryAccountEndpoint, null);
        return service;
    }

    @Override
    public void modifiedService(final ServiceReference<Remote> reference, final Remote service) {
        // Ignore
    }

    @Override
    public synchronized void removedService(final ServiceReference<Remote> reference, final Remote service) {
        if (null != service) {
            ServiceRegistration<OXSecondaryAccountServicePortType> serviceRegistration = this.serviceRegistration;
            if (serviceRegistration != null) {
                this.serviceRegistration = null;
                serviceRegistration.unregister();
            }
            context.ungetService(reference);
        }
    }

}