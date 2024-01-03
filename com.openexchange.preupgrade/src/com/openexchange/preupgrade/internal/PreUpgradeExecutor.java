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

package com.openexchange.preupgrade.internal;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.common.progress.DefaultProgressMonitor;
import com.openexchange.common.progress.ProgressMonitorFactory;
import com.openexchange.common.progress.ProgressMonitoredTask;
import com.openexchange.exception.OXException;
import com.openexchange.preupgrade.PreUpgradeTask;

/**
 * {@link PreUpgradeExecutor} - The pre-upgrade executor. Executes pre-upgrade tasks
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class PreUpgradeExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreUpgradeExecutor.class);

    private final BundleContext context;
    private final Collection<ServiceReference<PreUpgradeTask>> serviceReferences;

    private List<PreUpgradeTask> tasks;
    private DefaultProgressMonitor progressMonitor;
    private boolean shutDown;

    /**
     * Initializes a new {@link PreUpgradeExecutor}.
     *
     * @param context The bundle context
     */
    public PreUpgradeExecutor(BundleContext context) throws InvalidSyntaxException {
        super();
        this.context = context;
        this.serviceReferences = context.getServiceReferences(PreUpgradeTask.class, null);
        this.shutDown = false;
    }

    /**
     * Prepares an upgrade by running all {@link PreUpgradeTask#prepareUpgrade()} methods of
     * the registered pre-upgrade tasks.
     *
     * @throws OXException if any error is occurred during upgrade
     */
    public synchronized void prepareUpgrade() throws OXException {
        if (shutDown) {
            throw OXException.general("Pre-upgrade executor already shut-down.");
        }

        // Get tasks
        this.tasks = serviceReferences.stream().map(context::getService).collect(Collectors.toList());
        this.progressMonitor = ProgressMonitorFactory.createProgressMonitor("Pre-Upgrade", tasks.size());

        // Prepare upgrade
        LOGGER.info("Pre-upgrade preparation phase began. A total of {} pre-upgrade tasks will be processed.", I(tasks.size()));
        for (PreUpgradeTask task : tasks) {
            String taskName = task.getName();
            LOGGER.info("Preparing pre-upgrade task '{}'...", taskName);
            int steps = task.prepareUpgrade();
            if (false == task.isRequired()) {
                LOGGER.info("Pre-upgrade task '{}' is not required.", taskName);
                continue;
            }
            if (task instanceof ProgressMonitoredTask monitoredTask) {
                // For each pre-upgrade task create a sub-task monitor
                monitoredTask.setMonitor(ProgressMonitorFactory.createSubMonitor(taskName, steps, progressMonitor));
            }
            LOGGER.info("Pre-upgrade task '{}' will execute {} sub-task(s).", taskName, I(steps));
        }
        LOGGER.info("Pre-upgrade preparation phase ended.");
    }

    /**
     * Executes the upgrade by running all registered {@link PreUpgradeTask}s
     *
     * @throws OXException if any error is occurred during upgrade
     */
    public synchronized void executeUpgrade() throws OXException {
        if (shutDown) {
            throw OXException.general("Pre-upgrade executor already shut-down.");
        }

        if (this.tasks == null) {
            throw OXException.general("Pre-upgrade execution denied. Run prepareUpgrade() before.");
        }

        LOGGER.info("Pre-upgrade execution phase began. A total of {} pre-upgrade task(s) will be processed.", I(tasks.size()));
        for (PreUpgradeTask task : tasks) {
            String taskName = task.getName();
            if (!task.isRequired()) {
                LOGGER.info("Pre-upgrade task '{}' is not required, thus not executing", taskName);
                continue;
            }
            LOGGER.info("Executing pre-upgrade task '{}'...", taskName);
            try {
                task.executeUpgrade();
                LOGGER.info("Pre-upgrade task '{}' finished.", taskName);
            } catch (Exception e) {
                LOGGER.warn("Pre-upgrade task '{}' failed.", taskName, e);
                throw e;
            }
        }
        progressMonitor.done();
        LOGGER.info("Pre-upgrade execution phase ended.");
    }

    /**
     * Shuts-down this pre-upgrade executor.
     */
    public synchronized void shutDown() {
        if (shutDown) {
            // Already shut-down before
            return;
        }

        if (tasks == null) {
            // Not initialized
            return;
        }

        // Drop tasks & un-get services
        tasks = null;
        for (ServiceReference<PreUpgradeTask> serviceReference : serviceReferences) {
            context.ungetService(serviceReference);
        }
        shutDown = true;
    }
}
