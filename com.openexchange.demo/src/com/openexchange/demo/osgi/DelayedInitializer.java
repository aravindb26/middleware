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

import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.demo.InitializationPerformer;
import com.openexchange.startup.SignalStartedService;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * {@link DelayedInitializer} - Tracks for availability of {@link SignalStartedService} and required provisioning interfaces. Triggers
 * initialization if all is available.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class DelayedInitializer {

    private final BundleContext context;
    private final TrackingState state;

    private ServiceTracker<SignalStartedService, SignalStartedService> signalStartedTracker;
    private ServiceTracker<Remote, Remote> remoteTracker;

    /**
     * Initializes a new {@link DelayedInitializer}.
     *
     * @param initializer The {@link InitializationPerformer}
     * @param threadPool The {@link ThreadPoolService} to use
     * @param context The {@link BundleContext}
     */
    public DelayedInitializer(InitializationPerformer initializer, ThreadPoolService threadPool, BundleContext context) {
        super();
        this.context = context;
        this.state = new TrackingState(initializer, threadPool);
    }

    /**
     * Called in Activator's start() method
     */
    public synchronized void start() {
        List<ServiceTracker<?, ?>> openedTackers = new ArrayList<>(2);
        try {
            ServiceTracker<SignalStartedService, SignalStartedService> signalStartedTracker = new ServiceTracker<SignalStartedService, SignalStartedService>(context, SignalStartedService.class, new SignalStartedTracker(state, context));
            signalStartedTracker.open();
            openedTackers.add(signalStartedTracker);
            this.signalStartedTracker = signalStartedTracker;

            ServiceTracker<Remote, Remote> remoteTracker = new ServiceTracker<Remote, Remote>(context, Remote.class, new RemoteTracker(state, context));
            remoteTracker.open();
            openedTackers.add(remoteTracker);
            this.remoteTracker = remoteTracker;

            openedTackers = null; // All went fine
        } finally {
            if (openedTackers != null) {
                for (ServiceTracker<?, ?> tracker : openedTackers) {
                    tracker.close();
                }
            }
        }
    }

    /**
     * Called in Activator's stop() method
     */
    public synchronized void stop() {
        ServiceTracker<SignalStartedService, SignalStartedService> signalStartedTracker = this.signalStartedTracker;
        if (signalStartedTracker != null) {
            this.signalStartedTracker = null;
            signalStartedTracker.close();
        }

        ServiceTracker<Remote, Remote> remoteTracker = this.remoteTracker;
        if (remoteTracker != null) {
            this.remoteTracker = null;
            remoteTracker.close();
        }
    }

}
