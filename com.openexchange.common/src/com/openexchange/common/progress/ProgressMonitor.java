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

import java.util.Optional;

/**
 * {@link ProgressMonitor} - A progress monitor for tasks to report progress
 * It is initialised with a default total size of <code>1</code>, unless the {@link #setSteps(int)}
 * method is invoked after initialisation.
 * <p>
 * The {@link ProgressMonitor} can either be used on its own, or in conjunction with the {@link SubTaskProgressMonitor}
 * which keeps track of sub-tasks' progress. Below are some examples.
 * 
 * <p>
 * <h3>Usage example with sub-tasks and sub-monitors</h3>
 * <p>
 * First you need to initialise a main progress monitor for your tasks.
 * <pre>
 * int totalSubTasks = 12;
 * DefaultProgressMonitor progressMonitor = ProgressMonitorFactory.createProgressMonitor("Some long running main task", totalSubTasks);
 * </pre>
 * Then create the sub-tasks (which should be either implementing the {@link ProgressMonitoredTask} interface, or extending the
 * {@link AbstractProgressMonitoredTask}), and for each sub-task create and set a sub-monitor. That, will be a child of the main 
 * monitor. This way you can keep progress track of main and sub-tasks separately and have the sub-tasks contribute to the progress 
 * of their parent task.
 * <pre>
 * for (int i = 0; i < totalSubTasks; i++) {
 *      int subTaskSteps = 5; // The sub-task's total work that needs to be done
 *      Task task = new Task();
 *      task.setMonitor(ProgressMonitorFactory.createSubMonitor("Some Sub Task ", subTaskSteps, progressMonitor));
 * }
 * </pre>
 * Then execute the tasks and report the progress. For each task that gets work done, a +1 progress will be reported. 
 * <pre>
 * tasks.forEach(task) -> {
 *      task.doWork();
 *      progressMonitor.reportProgress(1);
 * });
 * </pre>
 * In each task you execute work and progress by the amount of work that was completed.
 * 
 * <pre>
 * class Task extends AbstractProgressMonitoredTask {
 *   void doWork() {
 *      while(work) {
 *         progress(amount);
 *      }
 *      done();
 *   }
 * }
 * </pre>
 * 
 * <h3>Usage example with main task</h3>
 * <p>
 * Initialise a main progress monitor for your task.
 * <pre>
 * int totalSteps = 8;
 * DefaultProgressMonitor progressMonitor = ProgressMonitorFactory.createProgressMonitor("Some long running task", totalSteps);
 * </pre>
 * 
 * Then execute the task and report the progress:
 * <pre>
 * while(working) {
 *    // do stuff
 *    progressMonitor.report(progress);
 * }
 * progressMonitor.done();
 * </pre>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public interface ProgressMonitor {

    /**
     * Returns the name of the task that this monitor is keeping track of
     *
     * @return The name of the task
     */
    String getTaskName();

    /**
     * Returns the total amount of steps to perform.
     *
     * @return the total amount of steps
     */
    int getSteps();

    /**
     * Sets the progress
     * 
     * @param totalProgress The total progress to set
     */
    void setProgress(int totalProgress);

    /**
     * Returns the actual progress of the monitored task
     *
     * @return the progress
     */
    int getProgress();

    /**
     * Reports the incremental progress, i.e. the progress done
     * since the last report.
     *
     * @param incrementalProgress the progress done since the last report.
     */
    void reportProgress(int incrementalProgress);

    /**
     * Returns an integer representing the percentage done
     * for this monitor
     *
     * @return The percentage done
     */
    int getPercentageDone();

    /**
     * Returns whether the monitored task is done
     *
     * @return <code>true</code> if the task is done; <code>false</code> otherwise
     */
    boolean isDone();

    /**
     * Sets this monitor as done and completes work
     */
    void done();

    /**
     * The optional parent monitor of this monitor. If empty then this is the root.
     *
     * @return the parent monitor or <code>empty()</code> if this is the root
     */
    Optional<ProgressMonitor> optParent();

    /**
     * Notifies all listeners to log the progress. If the progress monitor is a child
     * then it notifies the parent's listeners.
     * 
     * @param progressMonitor The {@link ProgressMonitor} that got updated
     */
    void notifyListeners(ProgressMonitor progressMonitor);
}
