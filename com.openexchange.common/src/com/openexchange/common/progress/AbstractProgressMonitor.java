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

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AbstractProgressMonitor} - The abstract progress monitor
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
abstract class AbstractProgressMonitor implements ProgressMonitor {

    private final List<ProgressMonitorListener> listeners;
    private static final int DEFAULT_SIZE = 1;

    private final AtomicInteger progress = new AtomicInteger();
    private final String taskName;
    private final Optional<ProgressMonitor> parent;
    private int steps = DEFAULT_SIZE;

    /**
     * Initializes a new {@link AbstractProgressMonitor}.
     *
     * @param taskName the task name
     * @param steps The steps
     * @param parent The optional parent
     * @param listeners The optional listeners for this monitor
     */
    AbstractProgressMonitor(String taskName, int steps, ProgressMonitor parent, List<ProgressMonitorListener> listeners) {
        super();
        this.taskName = taskName;
        this.steps = steps;
        this.parent = Optional.ofNullable(parent);
        this.listeners = listeners == null ? ImmutableList.of() : listeners;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public void setProgress(int progressProgress) {
        this.progress.set(Math.max(0, progressProgress));
        progress();
    }

    @Override
    public void reportProgress(int incrementalProgress) {
        int actualProgress = Math.max(0, incrementalProgress);
        progress.addAndGet(actualProgress);
        progress();
    }

    @Override
    public int getSteps() {
        return steps;
    }

    @Override
    public int getProgress() {
        return progress.get();
    }

    @Override
    public int getPercentageDone() {
        return Math.min((int) (100 * ((float) progress.get() / (float) steps)), 100);
    }

    @Override
    public boolean isDone() {
        return progress.get() >= steps;
    }

    @Override
    public void done() {
        if (!isDone()) {
            setProgress(steps);
        }
    }

    @Override
    public Optional<ProgressMonitor> optParent() {
        return parent;
    }

    @Override
    public void notifyListeners(ProgressMonitor progressMonitor) {
        listeners.forEach((listener) -> listener.logProgress(progressMonitor));
    }

    /**
     * Progresses the main monitor (if present) and
     * if the current child of that monitor is done.
     */
    private void progress() {
        notifyListeners(this);
        optParent().ifPresent((parent) -> {
            if (isDone()) {
                parent.reportProgress(1);
            }
        });
    }
}
