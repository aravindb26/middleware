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

package com.openexchange.groupware.update;

import java.io.Serializable;

/**
 * {@link TaskStatus}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public class TaskStatus implements Serializable {

    private static final long serialVersionUID = 1117834466553992385L;

    private final String jobId;
    private final String statusText;

    /**
     * Initialises a new {@link TaskStatus}.
     */
    public TaskStatus(String jobId, String statusText) {
        super();
        this.jobId = jobId;
        this.statusText = statusText;
    }

    /**
     * Gets the jobId
     *
     * @return The jobId
     */
    public String getJobId() {
        return jobId;
    }

    /**
     * Gets the statusText
     *
     * @return The statusText
     */
    public String getStatusText() {
        return statusText;
    }
}
