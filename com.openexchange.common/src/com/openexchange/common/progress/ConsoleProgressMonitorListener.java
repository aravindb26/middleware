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

/**
 * {@link ConsoleProgressMonitorListener} - A progress monitor listener that logs progress
 * to the console.
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class ConsoleProgressMonitorListener implements ProgressMonitorListener {

    /**
     * Initializes a new {@link ConsoleProgressMonitorListener}.
     */
    public ConsoleProgressMonitorListener() {
        super();
    }

    @Override
    public void logProgress(ProgressMonitor monitor) {
        StringBuilder sb = new StringBuilder();
        monitor.optParent().ifPresentOrElse((p) -> sb.append(" +-- "), () -> sb.append("+ "));
        sb.append(String.format("Completed %d/%d (%.2f%%) %s.", I(monitor.getProgress()), I(monitor.getSteps()), D(monitor.getPercentageDone()), monitor.getTaskName()));
        System.out.println(sb.toString());
    }
}
