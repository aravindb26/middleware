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

package com.openexchange.sessiond.osgi;

import java.util.Map;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.openexchange.hazelcast.configuration.HazelcastConfigurationService;
import com.openexchange.sessiond.impl.container.TokenSessionContainer;

/**
 * {@link HazelcastConfTracker} - The service tracker for <code>HazelcastConfigurationService</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class HazelcastConfTracker implements ServiceTrackerCustomizer<HazelcastConfigurationService, HazelcastConfigurationService> {

    private final BundleContext context;
    private final SessiondActivator activator;
    private ServiceTracker<HazelcastInstance, HazelcastInstance> hzInstanceTracker;

    /**
     * Initializes a new {@link HazelcastConfTracker}.
     *
     * @param context The bundle context
     * @param activator The activator instance
     */
    public HazelcastConfTracker(BundleContext context, SessiondActivator activator) {
        super();
        this.context = context;
        this.activator = activator;
    }

    @Override
    public synchronized HazelcastConfigurationService addingService(ServiceReference<HazelcastConfigurationService> reference) {
        final HazelcastConfigurationService hzConfig = context.getService(reference);

        try {
            if (hzConfig.isEnabled()) {
                // Track HazelcastInstance service
                ServiceTrackerCustomizer<HazelcastInstance, HazelcastInstance> customizer = new ServiceTrackerCustomizer<HazelcastInstance, HazelcastInstance>() {

                    @Override
                    public HazelcastInstance addingService(ServiceReference<HazelcastInstance> reference) {
                        HazelcastInstance hzInstance = context.getService(reference);
                        try {
                            String hzMapName = discoverHzMapName(hzConfig.getConfig(), TokenSessionContainer.getInstance().getServerTokenMapName());
                            if (null == hzMapName) {
                                context.ungetService(reference);
                                return null;
                            }
                            activator.addService(HazelcastInstance.class, hzInstance);
                            TokenSessionContainer.getInstance().changeBackingMapToHz();
                            return hzInstance;
                        } catch (Exception e) {
                            SessiondActivator.LOG.warn("Couldn't initialize distributed token-session map.", e);
                        }
                        context.ungetService(reference);
                        return null;
                    }

                    @Override
                    public void modifiedService(ServiceReference<HazelcastInstance> reference, HazelcastInstance hzInstance) {
                        // Ignore
                    }

                    @Override
                    public void removedService(ServiceReference<HazelcastInstance> reference, HazelcastInstance hzInstance) {
                        activator.removeService(HazelcastInstance.class);
                        TokenSessionContainer.getInstance().changeBackingMapToLocalMap();
                        context.ungetService(reference);
                    }
                };
                ServiceTracker<HazelcastInstance, HazelcastInstance> hzInstanceTracker = new ServiceTracker<HazelcastInstance, HazelcastInstance>(context, HazelcastInstance.class, customizer);
                this.hzInstanceTracker = hzInstanceTracker;
                hzInstanceTracker.open();
            }

            return hzConfig;
        } catch (Exception e) {
            // Failed
            SessiondActivator.LOG.error("SessiondActivator: start: ", e);
        }

        context.ungetService(reference);
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<HazelcastConfigurationService> reference, HazelcastConfigurationService service) {
        // Ignore
    }

    @Override
    public synchronized void removedService(ServiceReference<HazelcastConfigurationService> reference, HazelcastConfigurationService service) {
        ServiceTracker<HazelcastInstance, HazelcastInstance> hzInstanceTracker = this.hzInstanceTracker;
        if (null != hzInstanceTracker) {
            hzInstanceTracker.close();
            this.hzInstanceTracker = null;
        }

        context.ungetService(reference);
    }

    private static String discoverHzMapName(final Config config, String mapPrefix) throws IllegalStateException {
        final Map<String, MapConfig> mapConfigs = config.getMapConfigs();
        if (null != mapConfigs && !mapConfigs.isEmpty()) {
            for (final String mapName : mapConfigs.keySet()) {
                if (mapName.startsWith(mapPrefix)) {
                    SessiondActivator.LOG.info("Using distributed token-session map '{}'.", mapName);
                    return mapName;
                }
            }
        }
        SessiondActivator.LOG.info("No distributed token-session map with mapPrefix {} in hazelcast configuration", mapPrefix);
        return null;
    }

}