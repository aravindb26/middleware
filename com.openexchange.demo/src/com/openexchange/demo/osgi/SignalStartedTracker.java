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

package com.openexchange.demo.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import com.openexchange.startup.SignalStartedService;

/**
 * Tracker for <code>SignalStartedService</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class SignalStartedTracker implements ServiceTrackerCustomizer<SignalStartedService, SignalStartedService> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(SignalStartedTracker.class);

    private final BundleContext context;
    private final TrackingState state;

    /**
     * Initializes a new {@link SignalStartedTracker}.
     *
     * @param state The {@link TrackingState}
     * @param context The {@link BundleContext}
     */
    public SignalStartedTracker(TrackingState state, BundleContext context) {
        super();
        this.state = state;
        this.context = context;
    }

    @Override
    public SignalStartedService addingService(ServiceReference<SignalStartedService> reference) {
        // Service is available
        SignalStartedService service = context.getService(reference);
        LOG.info("Obtained {}", SignalStartedService.class.getName());
        state.setSignalStartedServiceAvailable(true);
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<SignalStartedService> reference, SignalStartedService service) {
        // Ignore
    }

    @Override
    public void removedService(ServiceReference<SignalStartedService> reference, SignalStartedService service) {
        state.setSignalStartedServiceAvailable(false);
        context.ungetService(reference);
    }
}
