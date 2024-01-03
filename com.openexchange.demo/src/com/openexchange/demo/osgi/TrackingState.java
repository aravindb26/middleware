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

import org.slf4j.Logger;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.OXGroupInterface;
import com.openexchange.admin.rmi.OXResourceInterface;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.OXUtilInterface;
import com.openexchange.demo.InitializationPerformer;
import com.openexchange.demo.InitializationTask;
import com.openexchange.demo.ProvisioningInterfaces;
import com.openexchange.threadpool.ThreadPoolService;

/**
 * Simple container that remembers collected services and triggers initialization of demo system once all needed services are available.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
class TrackingState {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(TrackingState.class);
    }

    private final InitializationPerformer initializer;
    private final ThreadPoolService threadPool;
    private boolean submitted;

    private boolean signalStartedServiceAvailable;
    private OXUtilInterface utilInterface;
    private OXUserInterface userInterface;
    private OXContextInterface contextInterface;
    private OXGroupInterface groupInterface;
    private OXResourceInterface resourceInterface;

    /**
     * Initializes a new {@link TrackingState}.
     *
     * @param initializer The {@link InitializationPerformer}
     * @param threadPool The {@link ThreadPoolService} to use
     */
    TrackingState(InitializationPerformer initializer, ThreadPoolService threadPool) {
        super();
        this.initializer = initializer;
        this.threadPool = threadPool;
        submitted = false;
    }

    synchronized void setSignalStartedServiceAvailable(boolean signalStartedServiceAvailable) {
        this.signalStartedServiceAvailable = signalStartedServiceAvailable;
        if (signalStartedServiceAvailable) {
            tryScheduleInitTask();
        }
    }

    synchronized void setUtilInterface(OXUtilInterface utilInterface) {
        this.utilInterface = utilInterface;
        if (utilInterface != null) {
            tryScheduleInitTask();
        }
    }

    synchronized void setUserInterface(OXUserInterface userInterface) {
        this.userInterface = userInterface;
        if (userInterface != null) {
            tryScheduleInitTask();
        }
    }

    synchronized void setContextInterface(OXContextInterface contextInterface) {
        this.contextInterface = contextInterface;
        if (contextInterface != null) {
            tryScheduleInitTask();
        }
    }

    synchronized void setGroupInterface(OXGroupInterface groupInterface) {
        this.groupInterface = groupInterface;
        if (groupInterface != null) {
            tryScheduleInitTask();
        }
    }

    synchronized void setResourceInterface(OXResourceInterface resourceInterface) {
        this.resourceInterface = resourceInterface;
        if (resourceInterface != null) {
            tryScheduleInitTask();
        }
    }

    private boolean tryScheduleInitTask() {
        if (submitted) {
            // Already submitted
            return false;
        }

        if (signalStartedServiceAvailable == false) {
            return false;
        }
        if (utilInterface == null) {
            return false;
        }
        if (userInterface == null) {
            return false;
        }
        if (contextInterface == null) {
            return false;
        }
        if (groupInterface == null) {
            return false;
        }
        if (resourceInterface == null) {
            return false;
        }

        if (submitted == false) {
            // All available
            //@formatter:off
            ProvisioningInterfaces provisioningInterfaces = ProvisioningInterfaces.builder()
                .withContextInterface(contextInterface)
                .withGroupInterface(groupInterface)
                .withResourceInterface(resourceInterface)
                .withUserInterface(userInterface)
                .withUtilInterface(utilInterface)
                .build();
            //@formatter:on
            LoggerHolder.LOG.info("All needed services/interfaces available. Submitting execution of demo system initialization.");
            threadPool.submit(new InitializationTask(provisioningInterfaces, initializer));
            submitted = true;

            // Null'ify to free-up memory
            utilInterface = null;
            userInterface = null;
            contextInterface = null;
            groupInterface = null;
            resourceInterface = null;
        }
        return true;
    }
}
