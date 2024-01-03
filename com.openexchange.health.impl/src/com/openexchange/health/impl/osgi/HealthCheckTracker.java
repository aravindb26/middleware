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

package com.openexchange.health.impl.osgi;

import org.eclipse.microprofile.health.HealthCheck;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.health.Constants;
import com.openexchange.health.impl.MWHealthCheckServiceImpl;
import com.openexchange.health.impl.MicroprofileHealthCheckWrapper;

/**
 * {@link HealthCheckTracker}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.10.1
 */
public class HealthCheckTracker implements ServiceTrackerCustomizer<HealthCheck, HealthCheck> {

    private final BundleContext context;
    private final MWHealthCheckServiceImpl healthCheckService;

    /**
     * Initializes a new {@link HealthCheckTracker}.
     *
     * @param context The bundle context
     * @param healthCheckService The health check service to add tracked Microprofile checks to
     */
    public HealthCheckTracker(BundleContext context, MWHealthCheckServiceImpl healthCheckService) {
        super();
        this.context = context;
        this.healthCheckService = healthCheckService;
    }

    @Override
    public HealthCheck addingService(ServiceReference<HealthCheck> reference) {
        HealthCheck check = context.getService(reference);

        long cacheTimeToLive = parseTimeToLive(reference);
        if (healthCheckService.addCheck(new MicroprofileHealthCheckWrapper(check, cacheTimeToLive))) {
            return check;
        }

        context.ungetService(reference);
        return null;
    }

    private long parseTimeToLive(ServiceReference<HealthCheck> reference) {
        final Object oTimeToLive = reference.getProperty(Constants.SERVICE_CACHE_TIME_TO_LIVE);
        if (oTimeToLive != null) {
            if (oTimeToLive instanceof Number n) {
                return n.longValue();
            }

            // Try to parse String representation as long value
            try {
                return Long.parseLong(oTimeToLive.toString().trim());
            } catch (NumberFormatException e) {
                // Ignore...
            }
        }
        return 0L;
    }

    @Override
    public void modifiedService(ServiceReference<HealthCheck> reference, HealthCheck service) {
        // nothing to do
    }

    @Override
    public void removedService(ServiceReference<HealthCheck> reference, HealthCheck service) {
        healthCheckService.removeCheck(service.getClass().getName());
        context.ungetService(reference);
    }

}
