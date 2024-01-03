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

package com.openexchange.preupgrade;

import java.util.concurrent.atomic.AtomicBoolean;
import com.openexchange.common.progress.AbstractProgressMonitoredTask;

/**
 * {@link AbstractPreUpgradeTask} - Abstract pre-upgrade task which tracks the required status of the task.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8.0.0
 */
public abstract class AbstractPreUpgradeTask extends AbstractProgressMonitoredTask implements PreUpgradeTask {

    private final AtomicBoolean isRequired;

    /**
     * Initializes a new {@link AbstractPreUpgradeTask}.
     */
    protected AbstractPreUpgradeTask() {
        super();
        isRequired = new AtomicBoolean(true);
    }

    /**
     * Sets the required flag.
     *
     * @param isRequired sets whether this task is required
     */
    protected void setRequired(boolean isRequired) {
        this.isRequired.set(isRequired);
    }

    @Override
    public boolean isRequired() {
        return isRequired.get();
    }

    /**
     * Default implementation that uses {@link Class#getSimpleName()} as task name.
     * <p/>
     * Override if applicable.
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
