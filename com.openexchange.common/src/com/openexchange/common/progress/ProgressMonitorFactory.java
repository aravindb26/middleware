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

import java.util.List;
import com.google.common.collect.ImmutableList;

/**
 * {@link ProgressMonitorFactory} - Creates {@link ProgressMonitor}s
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public final class ProgressMonitorFactory {

    /**
     * Creates a {@link ProgressMonitor} with a {@link ConsoleProgressMonitorListener}
     *
     * @param taskName The monitored task's name
     * @param steps The total steps of the monitored task
     * @return The {@link ProgressMonitor}
     */
    public static DefaultProgressMonitor createProgressMonitor(String taskName, int steps) {
        return createProgressMonitor(taskName, steps, null, ImmutableList.of(new SLF4JProgressMonitorListener()));
    }

    /**
     * Creates a new sub-monitor with the specified parent. The sub-monitor will keep
     * track of the progress of a sub-task with in the main task monitored by
     * the parent monitor. It defaults to size of 1.
     *
     * @param taskName The monitored sub-task's name
     * @param steps The steps
     * @param parent The parent of the sub-monitor
     * @return the new sub-monitor
     */
    public static SubProgressMonitor createSubMonitor(String taskName, int steps, DefaultProgressMonitor parent) {
        return SubProgressMonitor.builder().withTaskName(taskName).withSteps(steps).withParent(parent).withListeners(ImmutableList.of()).build();
    }

    /**
     * Creates a new sub-monitor with the specified parent and listeners. The sub-monitor will keep
     * track of the progress of a sub-task with in the main task monitored by
     * the parent monitor. It defaults to size of 1.
     *
     * @param taskName The monitored sub-task's name
     * @param steps The steps
     * @param parent The parent of the sub-monitor
     * @param listeners a list with listeners
     * @return the new sub-monitor
     */
    public static SubProgressMonitor createSubMonitor(String taskName, int steps, DefaultProgressMonitor parent, List<ProgressMonitorListener> listeners) {
        return SubProgressMonitor.builder().withTaskName(taskName).withSteps(steps).withParent(parent).withListeners(listeners).build();
    }

    /**
     * Creates a new sub-monitor with the specified parent. The sub-monitor will keep
     * track of the progress of a sub-task with in the main task monitored by
     * the parent monitor. It defaults to size of 1.
     *
     * @param taskName The monitored sub-task's name
     * @param steps The steps
     * @param parent The parent of the sub-monitor
     * @param listeners a list with listeners
     * @return the new sub-monitor
     */
    public static DefaultProgressMonitor createProgressMonitor(String taskName, int steps, AbstractProgressMonitor parent, List<ProgressMonitorListener> listeners) {
        return DefaultProgressMonitor.builder().withTaskName(taskName).withSteps(steps).withListeners(listeners).build();
    }

}
