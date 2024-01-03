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

import java.util.Collections;
import java.util.List;

/**
 * {@link SubProgressMonitor}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class SubProgressMonitor extends AbstractProgressMonitor {

    /**
     * Initializes a new {@link SubProgressMonitor}.
     *
     * @param builder The builder to use for building the sub-monitor
     */
    private SubProgressMonitor(Builder builder) {
        super(builder.taskName, builder.steps, builder.parent, builder.listeners);
    }

    /**
     * Initializes a new {@link SubProgressMonitor}.
     *
     * @param taskName The task's name
     * @param steps The steps
     * @param parent The optional parent
     * @param listeners The listeners for this sub-monitor
     */
    private SubProgressMonitor(String taskName, int steps, DefaultProgressMonitor parent, List<ProgressMonitorListener> listeners) {
        super(taskName, steps, parent, listeners);
    }

    /**
     * Creates builder to build {@link SubProgressMonitor}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void notifyListeners(ProgressMonitor progressMonitor) {
        // First notify the parent's listeners
        optParent().ifPresent(parent -> parent.notifyListeners(progressMonitor));
        // Then notify our own
        super.notifyListeners(progressMonitor);
    }

    /**
     * Builder to build {@link SubProgressMonitor}.
     */
    public static final class Builder {

        private String taskName;
        private int steps;
        private ProgressMonitor parent;
        private List<ProgressMonitorListener> listeners = Collections.emptyList();

        /**
         * Initializes a new {@link Builder}.
         */
        private Builder() {
            super();
        }

        /**
         * Adds the task name
         *
         * @param taskName The task name
         * @return this instance for chained calls
         */
        public Builder withTaskName(String taskName) {
            this.taskName = taskName;
            return this;
        }

        /**
         * Adds the total steps
         *
         * @param steps the steps of the monitor
         * @return this instance for chained calls
         */
        public Builder withSteps(int steps) {
            this.steps = steps;
            return this;
        }

        /**
         * Adds the parent of this sub-monitor
         *
         * @param parent The parent to set
         * @return this instance for chained calls
         * @throws IllegalArgumentException if the parent monitor is <code>null</code>
         */
        public Builder withParent(DefaultProgressMonitor parent) {
            if (parent == null) {
                throw new IllegalArgumentException("The parent of this sub-monitor cannot be null");
            }
            this.parent = parent;
            return this;
        }

        /**
         * Adds a set of listeners for the main monitor
         *
         * @param listeners The listeners to add
         * @return this instance for chained calls
         */
        public Builder withListeners(List<ProgressMonitorListener> listeners) {
            this.listeners = listeners;
            return this;
        }

        /**
         * Builds the progress monitor
         *
         * @return The progress monitor
         */
        public SubProgressMonitor build() {
            return new SubProgressMonitor(this);
        }
    }
}
