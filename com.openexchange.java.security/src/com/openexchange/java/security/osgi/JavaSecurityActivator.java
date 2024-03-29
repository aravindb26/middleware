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

package com.openexchange.java.security.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@link JavaSecurityActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class JavaSecurityActivator implements BundleActivator {

    private ServiceTracker<Object, Object> serviceTracker;

    /**
     * Initializes a new {@link JavaSecurityActivator}.
     */
    public JavaSecurityActivator() {
        super();
    }

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        org.slf4j.LoggerFactory.getLogger(JavaSecurityActivator.class).info("Starting bundle {}", context.getBundle().getSymbolicName());

        JavaSecurityProviderReplaceTrigger signalStartedTracker = new JavaSecurityProviderReplaceTrigger(context);
        ServiceTracker<Object, Object> serviceTracker = signalStartedTracker.createTracker();
        serviceTracker.open();
        this.serviceTracker = serviceTracker;
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        org.slf4j.LoggerFactory.getLogger(JavaSecurityActivator.class).info("Stopping bundle {}", context.getBundle().getSymbolicName());

        ServiceTracker<Object, Object> serviceTracker = this.serviceTracker;
        if (serviceTracker != null) {
            this.serviceTracker = null;
            serviceTracker.close();
        }
    }

}
