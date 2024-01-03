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
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.OXGroupInterface;
import com.openexchange.admin.rmi.OXResourceInterface;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.OXUtilInterface;
import com.openexchange.java.Strings;

/**
 * The tracker for administrative interfaces.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class RemoteTracker implements ServiceTrackerCustomizer<Remote, Remote> {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RemoteTracker.class);

    private final BundleContext context;
    private final TrackingState state;

    /**
     * Initializes a new {@link RemoteTracker}.
     *
     * @param state The {@link TrackingState}
     * @param context The {@link BundleContext}
     */
    public RemoteTracker(TrackingState state, BundleContext context) {
        super();
        this.state = state;
        this.context = context;
    }

    @Override
    public Remote addingService(ServiceReference<Remote> reference) {
        String rmiName = (String) reference.getProperty("RMIName");
        if (Strings.isEmpty(rmiName)) {
            return null;
        }

        if (OXUtilInterface.RMI_NAME.equals(rmiName)) {
            Remote remote = context.getService(reference);
            LOG.info("Obtained {}", OXUtilInterface.class.getName());
            state.setUtilInterface((OXUtilInterface) remote);
            return remote;
        }
        if (OXUserInterface.RMI_NAME.equals(rmiName)) {
            Remote remote = context.getService(reference);
            LOG.info("Obtained {}", OXUserInterface.class.getName());
            state.setUserInterface((OXUserInterface) remote);
            return remote;
        }
        if (OXContextInterface.RMI_NAME.equals(rmiName)) {
            Remote remote = context.getService(reference);
            LOG.info("Obtained {}", OXContextInterface.class.getName());
            state.setContextInterface((OXContextInterface) remote);
            return remote;
        }
        if (OXGroupInterface.RMI_NAME.equals(rmiName)) {
            Remote remote = context.getService(reference);
            LOG.info("Obtained {}", OXGroupInterface.class.getName());
            state.setGroupInterface((OXGroupInterface) remote);
            return remote;
        }
        if (OXResourceInterface.RMI_NAME.equals(rmiName)) {
            Remote remote = context.getService(reference);
            LOG.info("Obtained {}", OXResourceInterface.class.getName());
            state.setResourceInterface((OXResourceInterface) remote);
            return remote;
        }

        return null;
    }

    @Override
    public void modifiedService(ServiceReference<Remote> reference, Remote service) {
        // Ignore
    }

    @Override
    public void removedService(ServiceReference<Remote> reference, Remote service) {
        String rmiName = (String) reference.getProperty("RMIName");
        if (Strings.isNotEmpty(rmiName)) {
            if (OXUtilInterface.RMI_NAME.equals(rmiName)) {
                state.setUtilInterface(null);
            } else if (OXUserInterface.RMI_NAME.equals(rmiName)) {
                state.setUserInterface(null);
            } else if (OXContextInterface.RMI_NAME.equals(rmiName)) {
                state.setContextInterface(null);
            } else if (OXGroupInterface.RMI_NAME.equals(rmiName)) {
                state.setGroupInterface(null);
            } else if (OXResourceInterface.RMI_NAME.equals(rmiName)) {
                state.setResourceInterface(null);
            }
        }

        context.ungetService(reference);
    }
}
