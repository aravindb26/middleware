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

package com.openexchange.cluster.serialization.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.cluster.serialization.session.SessionCodec;
import com.openexchange.session.ObfuscatorService;
import com.openexchange.session.SessionVersionService;

/**
 * {@link ClusterSessionActivator}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ClusterSessionActivator implements BundleActivator {

    private ServiceTracker<ObfuscatorService, ObfuscatorService> obfuscatorServiceTracker;
    private ServiceTracker<SessionVersionService, SessionVersionService> sessionVersionServiceTracker;

    /**
     * Initializes a new {@link ClusterSessionActivator}.
     */
    public ClusterSessionActivator() {
        super();
    }

    @Override
    public synchronized void start(BundleContext context) throws Exception {
        ServiceTracker<ObfuscatorService, ObfuscatorService> obfuscatorServiceTracker = new ServiceTracker<>(context, ObfuscatorService.class, new ServiceTrackerCustomizer<ObfuscatorService, ObfuscatorService>() {

            @Override
            public ObfuscatorService addingService(ServiceReference<ObfuscatorService> reference) {
                ObfuscatorService obfuscatorService = context.getService(reference);
                SessionCodec.setObfuscatorService(obfuscatorService);
                return obfuscatorService;
            }

            @Override
            public void modifiedService(ServiceReference<ObfuscatorService> reference, ObfuscatorService service) {
                // Ignore
            }

            @Override
            public void removedService(ServiceReference<ObfuscatorService> reference, ObfuscatorService service) {
                SessionCodec.setObfuscatorService(null);
                context.ungetService(reference);
            }
        });
        obfuscatorServiceTracker.open();
        this.obfuscatorServiceTracker = obfuscatorServiceTracker;

        ServiceTracker<SessionVersionService, SessionVersionService> sessionVersionServiceTracker = new ServiceTracker<>(context, SessionVersionService.class, new ServiceTrackerCustomizer<SessionVersionService, SessionVersionService>() {

            @Override
            public SessionVersionService addingService(ServiceReference<SessionVersionService> reference) {
                SessionVersionService sessionVersionService = context.getService(reference);
                SessionCodec.setSessionVersionService(sessionVersionService);
                return sessionVersionService;
            }

            @Override
            public void modifiedService(ServiceReference<SessionVersionService> reference, SessionVersionService service) {
                // Ignore
            }

            @Override
            public void removedService(ServiceReference<SessionVersionService> reference, SessionVersionService service) {
                SessionCodec.setSessionVersionService(null);
                context.ungetService(reference);
            }
        });
        sessionVersionServiceTracker.open();
        this.sessionVersionServiceTracker = sessionVersionServiceTracker;
    }

    @Override
    public synchronized void stop(BundleContext context) throws Exception {
        ServiceTracker<SessionVersionService, SessionVersionService> sessionVersionServiceTracker = this.sessionVersionServiceTracker;
        if (sessionVersionServiceTracker != null) {
            this.sessionVersionServiceTracker = null;
            sessionVersionServiceTracker.close();
        }
        ServiceTracker<ObfuscatorService, ObfuscatorService> obfuscatorServiceTracker = this.obfuscatorServiceTracker;
        if (obfuscatorServiceTracker != null) {
            this.obfuscatorServiceTracker = null;
            obfuscatorServiceTracker.close();
        }
    }

}
