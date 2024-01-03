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

package com.openexchange.common.progress;

import static com.openexchange.java.Autoboxing.D;
import static com.openexchange.java.Autoboxing.I;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SLF4JProgressMonitorListener} - A progress listener that logs progress to a {@link Logger}.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public class SLF4JProgressMonitorListener implements ProgressMonitorListener {

    private static final Logger LOG = LoggerFactory.getLogger(SLF4JProgressMonitorListener.class);

    /**
     * Initializes a new {@link SLF4JProgressMonitorListener}.
     */
    public SLF4JProgressMonitorListener() {
        super();
    }

    @Override
    public void logProgress(ProgressMonitor monitor) {
        StringBuilder sb = new StringBuilder();
        // Print the start
        if (monitor.getProgress() == 0) {
            logStart(sb, monitor);
            return;
        }

        // Log progress only if there is any progress
        if (monitor.getProgress() < 0) {
            return;
        }

        logProgress(sb, monitor);
        sb.setLength(0);

        // Log finished once the task is done
        if (monitor.isDone()) {
            logFinished(sb, monitor);
            sb.setLength(0);
        }
    }

    /**
     * Logs the start of a task
     *
     * @param sb The {@link StringBuilder}
     * @param monitor The monitor
     */
    private void logStart(StringBuilder sb, ProgressMonitor monitor) {
        sb.append(String.format("Started task '%s' with %d step", monitor.getTaskName(), I(monitor.getSteps())));
        sb.append(monitor.getSteps() == 1 ? "." : "s.");
        LOG.info(sb.toString());
    }

    /**
     * Logs normal progress
     *
     * @param sb The {@link StringBuilder}
     * @param monitor The monitor
     */
    private void logProgress(StringBuilder sb, ProgressMonitor monitor) {
        sb.append(String.format("Completed %d/%d (%.2f%%) of '%s'.", I(monitor.getProgress()), I(monitor.getSteps()), D(monitor.getPercentageDone()), monitor.getTaskName()));
        LOG.info(sb.toString());
    }

    /**
     * Logs the finish of a task
     *
     * @param sb The {@link StringBuilder}
     * @param monitor The monitor
     */
    private void logFinished(StringBuilder sb, ProgressMonitor monitor) {
        sb.append(String.format("Finished task '%s'.", monitor.getTaskName()));
        LOG.info(sb.toString());
    }
}
