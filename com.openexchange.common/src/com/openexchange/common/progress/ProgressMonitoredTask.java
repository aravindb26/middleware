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

/**
 * {@link ProgressMonitoredTask} - Defines a task that is being monitored
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public interface ProgressMonitoredTask {

    /**
     * Sets the monitor for this monitored task
     *
     * @param monitor the monitor to set
     */
    void setMonitor(ProgressMonitor monitor);

    /**
     * Progresses the set {@link ProgressMonitor} of this monitored
     * task by <code>1</code>
     */
    void progress();

    /**
     * Progresses the set {@link ProgressMonitor} of this monitored
     * task by the specified <code>amount</code>
     *
     * @param amount The amount to progress this monitor
     */
    void progress(int amount);

    /**
     * Marks the task as done and sets the {@link ProgressMonitor}'s progress to 100%
     */
    void done();
}
