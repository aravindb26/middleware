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

import com.openexchange.config.ConfigurationService;
import com.openexchange.http.liveness.SocketLivenessServerStarter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * {@link ConfigServiceTracker} - Tracks configuration service & starts HTTP liveness end-point.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ConfigServiceTracker implements ServiceTrackerCustomizer<ConfigurationService, ConfigurationService> {

    private final BundleContext context;

    /**
     * Initializes a new {@link ConfigServiceTracker}.
     *
     * @param context The bundle context
     */
    public ConfigServiceTracker(BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public synchronized ConfigurationService addingService(ServiceReference<ConfigurationService> reference) {
        ConfigurationService configService = context.getService(reference);
        if (SocketLivenessServerStarter.getInstance().startSocketLivenessServer(configService)) {
            return configService;
        }

        context.ungetService(reference);
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<ConfigurationService> reference, ConfigurationService service) {
        // Ignore
    }

    @Override
    public synchronized void removedService(ServiceReference<ConfigurationService> reference, ConfigurationService service) {
        SocketLivenessServerStarter.getInstance().stopSocketLivenessServer();
        context.ungetService(reference);
    }

}
