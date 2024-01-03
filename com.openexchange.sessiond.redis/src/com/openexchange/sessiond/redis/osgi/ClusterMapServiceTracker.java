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

package com.openexchange.sessiond.redis.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import com.openexchange.cluster.map.ClusterMapService;
import com.openexchange.sessiond.redis.token.TokenSessionContainer;

/**
 * {@link ClusterMapServiceTracker} - Tracks cluster map service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class ClusterMapServiceTracker implements ServiceTrackerCustomizer<ClusterMapService, ClusterMapService> {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(ClusterMapServiceTracker.class);
    }

    private final BundleContext context;
    private final RedisSessiondActivator activator;

    /**
     * Initializes a new {@link ClusterMapServiceTracker}.
     *
     * @param activator The activator
     * @param context The bundle context
     */
    public ClusterMapServiceTracker(RedisSessiondActivator activator, BundleContext context) {
        super();
        this.activator = activator;
        this.context = context;
    }

    @Override
    public ClusterMapService addingService(ServiceReference<ClusterMapService> reference) {
        ClusterMapService clusterMapService = context.getService(reference);
        activator.addService(ClusterMapService.class, clusterMapService);
        try {
            TokenSessionContainer.getInstance().changeBackingMapToClusterMap();
        } catch (Exception e) {
            LoggerHolder.LOG.error("Failed to enable cluster map for token session container", e);
        }
        return clusterMapService;
    }

    @Override
    public void modifiedService(ServiceReference<ClusterMapService> reference, ClusterMapService service) {
        // Ignore
    }

    @Override
    public void removedService(ServiceReference<ClusterMapService> reference, ClusterMapService service) {
        TokenSessionContainer.getInstance().changeBackingMapToLocalMap();
        activator.removeService(ClusterMapService.class);
        context.ungetService(reference);
    }

}
