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

package com.openexchange.database.cleanup.impl.osgi;

import com.openexchange.database.cleanup.impl.DatabaseCleanUpServiceImpl;
import com.openexchange.startup.SignalStartedService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * {@link DatabaseCleanUpSchedulingStarter} - Starts scheduling of clean-up jobs once signal-started service is available.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.14.0
 */
public class DatabaseCleanUpSchedulingStarter implements ServiceTrackerCustomizer<SignalStartedService, SignalStartedService> {

    private final BundleContext bundleContext;
    private final DatabaseCleanUpServiceImpl cleanUpService;

    /**
     * Initializes a new {@link DatabaseCleanUpSchedulingStarter}.
     *
     * @param cleanUpService The clean-up service to start
     * @param bundleContext The bundle context
     */
    public DatabaseCleanUpSchedulingStarter(DatabaseCleanUpServiceImpl cleanUpService, BundleContext bundleContext) {
        super();
        this.cleanUpService = cleanUpService;
        this.bundleContext = bundleContext;
    }

    @Override
    public SignalStartedService addingService(ServiceReference<SignalStartedService> reference) {
        SignalStartedService service = bundleContext.getService(reference);
        cleanUpService.startScheduling();
        return service;
    }

    @Override
    public void modifiedService(ServiceReference<SignalStartedService> reference, SignalStartedService service) {
        // Don't care
    }

    @Override
    public void removedService(ServiceReference<SignalStartedService> reference, SignalStartedService service) {
        bundleContext.ungetService(reference);
    }

}
