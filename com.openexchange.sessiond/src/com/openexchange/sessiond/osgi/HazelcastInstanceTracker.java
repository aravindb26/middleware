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
import com.hazelcast.core.HazelcastInstance;

/**
 * {@link HazelcastInstanceTracker} - The service tracker for <code>HazelcastInstance</code>.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class HazelcastInstanceTracker implements ServiceTrackerCustomizer<HazelcastInstance, HazelcastInstance> {

    private final BundleContext context;
    private final SessiondActivator activator;

    /**
     * Initializes a new {@link HazelcastInstanceTracker}.
     *
     * @param context The bundle context
     * @param activator The activator instance
     */
    public HazelcastInstanceTracker(BundleContext context, SessiondActivator activator) {
        super();
        this.context = context;
        this.activator = activator;
    }

    @Override
    public HazelcastInstance addingService(ServiceReference<HazelcastInstance> reference) {
        HazelcastInstance hzInstance = context.getService(reference);
        try {
            activator.addService(HazelcastInstance.class, hzInstance);
            return hzInstance;
        } catch (RuntimeException e) {
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
        context.ungetService(reference);
    }

}