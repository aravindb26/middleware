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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.sessiond.impl.SessionHandler;
import com.openexchange.sessiond.impl.container.TokenSessionContainer;
import com.openexchange.timer.TimerService;

/**
 * {@link TimerServiceTracker}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class TimerServiceTracker implements ServiceTrackerCustomizer<TimerService,TimerService> {

    private final BundleContext context;

    /**
     * Initializes a new {@link TimerServiceTracker}.
     *
     * @param context The bundle context
     */
    public TimerServiceTracker(final BundleContext context) {
        super();
        this.context = context;
    }

    @Override
    public TimerService addingService(final ServiceReference<TimerService> reference) {
        final TimerService service = context.getService(reference);
        SessionHandler.addTimerService(service);
        TokenSessionContainer.getInstance().addTimerService(service);
        return service;
    }

    @Override
    public void modifiedService(final ServiceReference<TimerService> reference, final TimerService service) {
        // Nothing to do.
    }

    @Override
    public void removedService(final ServiceReference<TimerService> reference, final TimerService service) {
        TokenSessionContainer.getInstance().removeTimerService();
        SessionHandler.removeTimerService();
        context.ungetService(reference);
    }
}
