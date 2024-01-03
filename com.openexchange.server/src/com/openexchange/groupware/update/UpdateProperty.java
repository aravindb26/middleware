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


/**
 * {@link UpdateProperty} - The properties for update task framework.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public enum UpdateProperty implements com.openexchange.config.lean.Property {

    /**
     * Specifies a comma-separated list of name-spaces of update tasks that are supposed to be excluded from automatic update procedure.
     */
    EXCLUDED_UPDATE_TASKS("excludedUpdateTasks", ""),
    /**
     * Whether pending update tasks are triggered for context-associated database schema when a context is loaded from database.
     */
    DENY_IMPLICIT_UPDATE_ON_CONTEXT_LOAD("denyImplicitUpdateOnContextLoad", Boolean.FALSE),
    /**
     * Specifies the interval in milliseconds when to refresh/update lock's last-touched time stamp.
     */
    REFRESH_INTERVAL_MILLIS("refreshIntervalMillis", Long.valueOf(20000L)),
    /**
     * Accepts the number of milliseconds specifying the allowed idle time for acquired lock for non-background update tasks.
     */
    BLOCKED_IDLE_MILLIS("locked.idleMillis", Long.valueOf(60000L)),
    /**
     * Accepts the number of milliseconds specifying the allowed idle time for acquired lock for background update tasks.
     */
    BACKGROUND_IDLE_MILLIS("background.idleMillis", Long.valueOf(0L)),

    ;

    private final String fqn;
    private final Object defaultValue;

    private UpdateProperty(String appendix, Object defaultValue) {
        fqn = "com.openexchange.groupware.update." + appendix;
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
