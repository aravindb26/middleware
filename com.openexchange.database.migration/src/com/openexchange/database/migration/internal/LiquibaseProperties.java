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

package com.openexchange.database.migration.internal;

import com.openexchange.config.lean.Property;

/**
 * {@link LiquibaseProperties}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public enum LiquibaseProperties implements Property {

    /**
     * Specifies the interval in milliseconds between updates to the liquibase lock's timestamp.
     */
    REFRESH_INTERVAL_MILLIS("refreshIntervalMillis", Long.valueOf(20000)),
    /**
     * Specifies the max time in milliseconds that a liquibase migration is still considered to be running.
     */
    MAX_IDLE_MILLIS("locked.maxIdleMillis", Long.valueOf(60000L))
    ;

    private String name;
    private Object defaultValue;
    private static final String PREFIX = "com.openexchange.liquibase.";

    /**
     * Initializes a new {@link LiquibaseProperties}.
     */
    private LiquibaseProperties(String name, Object defValue) {
        this.name = name;
        this.defaultValue = defValue;
    }

    @Override
    public String getFQPropertyName() {
        return PREFIX + name;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

}
