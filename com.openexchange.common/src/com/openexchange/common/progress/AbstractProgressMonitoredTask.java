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

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link AbstractProgressMonitoredTask} - The abstract progress tracked task.
 * Sometimes it might not be possible to set the {@link ProgressMonitor} at construction
 * time, hence the {@link #setMonitor(ProgressMonitor)} method.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public abstract class AbstractProgressMonitoredTask implements ProgressMonitoredTask {

    private final AtomicReference<ProgressMonitor> progressMonitor;

    /**
     * Initializes a new {@link AbstractProgressMonitoredTask}.
     */
    protected AbstractProgressMonitoredTask() {
        this(null);
    }

    /**
     * Initializes a new {@link AbstractProgressMonitoredTask}.
     *
     * @param progressMonitor The progress monitor to set
     */
    protected AbstractProgressMonitoredTask(ProgressMonitor progressMonitor) {
        super();
        this.progressMonitor = new AtomicReference<>(progressMonitor);
    }

    @Override
    public void setMonitor(ProgressMonitor monitor) {
        progressMonitor.set(monitor);
    }

    @Override
    public void progress() {
        progress(1);
    }

    @Override
    public void progress(int progressAmount) {
        ProgressMonitor monitor = progressMonitor.get();
        if (null != monitor) {
            monitor.reportProgress(progressAmount);
        }
    }

    @Override
    public void done() {
        ProgressMonitor monitor = progressMonitor.get();
        if (null != monitor) {
            monitor.done();
        }
    }
}
