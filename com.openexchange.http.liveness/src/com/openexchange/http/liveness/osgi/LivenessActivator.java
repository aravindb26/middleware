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


package com.openexchange.http.liveness.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.config.Interests;
import com.openexchange.config.Reloadable;
import com.openexchange.config.Reloadables;
import com.openexchange.http.liveness.SocketLivenessServerStarter;

/**
 * {@link LivenessActivator} - The activator for HTTP liveness end-point.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class LivenessActivator implements BundleActivator, Reloadable {

    private ServiceTracker<ConfigurationService, ConfigurationService> tracker;
    private ServiceRegistration<Reloadable> reloadableRegistration;

    /**
     * Initializes a new {@link LivenessActivator}.
     */
    public LivenessActivator() {
        super();
    }

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        LoggerFactory.getLogger(LivenessActivator.class).info("Starting bundle {}", context.getBundle().getSymbolicName());
        tracker = new ServiceTracker<>(context, ConfigurationService.class, new ConfigServiceTracker(context));
        tracker.open();

        reloadableRegistration = context.registerService(Reloadable.class, this, null);
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        LoggerFactory.getLogger(LivenessActivator.class).info("Stopping bundle {}", context.getBundle().getSymbolicName());
        ServiceTracker<ConfigurationService, ConfigurationService> tracker = this.tracker;
        if (tracker != null) {
            this.tracker = null;
            tracker.close();
        }
        ServiceRegistration<Reloadable> reloadableRegistration = this.reloadableRegistration;
        if (reloadableRegistration != null) {
            this.reloadableRegistration = null;
            reloadableRegistration.unregister();
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public Interests getInterests() {
        return Reloadables.interestsForProperties(
            "com.openexchange.http.grizzly.livenessEnabled",
            "com.openexchange.connector.networkListenerHost",
            "com.openexchange.connector.livenessPort"
        );
    }

    @Override
    public void reloadConfiguration(ConfigurationService configService) {
        SocketLivenessServerStarter.getInstance().startSocketLivenessServer(configService);
    }

}
