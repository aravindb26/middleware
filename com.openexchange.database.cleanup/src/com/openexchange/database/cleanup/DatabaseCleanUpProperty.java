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


package com.openexchange.database.cleanup;

import com.openexchange.config.lean.Property;

/**
 * {@link DatabaseCleanUpProperty} - Property enumeration for database clean-up framework.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.13.0
 */
public enum DatabaseCleanUpProperty implements Property {

    /**
     * Whether clean-up jobs are enabled or not.
     */
    ENABLED("enabled", Boolean.TRUE),
    /**
     * The schedule when to execute database clean-up jobs.
     */
    SCHEDULE("schedule", "Mon-Sun"),
    /**
     * The frequency in milliseconds when to check for new job executions with configured schedule.
     */
    FREQUENCY("frequency", Long.valueOf(600000L)),
    /**
     * The concurrency level for job executions specifying how many jobs may be executed concurrently.
     */
    CONCURRENCY_LEVEL("concurrencylevel", Integer.valueOf(4)),
    /**
     * <code>true</code> to abort running clean-up jobs of general type when specified schedule is elapsed; otherwise <code>false</code> to allow continuing execution.
     */
    ABORT_SCHEDULED_JOBS("abortRunningGeneralJobs", Boolean.FALSE)
    ;

    private final String fqn;
    private final Object defaultValue;

    private DatabaseCleanUpProperty(String appendix, Object defaultValue) {
        this.fqn = "com.openexchange.database.cleanup." + appendix;
        this.defaultValue = defaultValue;
    }

    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
