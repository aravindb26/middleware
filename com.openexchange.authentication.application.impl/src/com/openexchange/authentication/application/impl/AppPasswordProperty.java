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

package com.openexchange.authentication.application.impl;

import com.openexchange.config.lean.Property;

/**
 * {@link AppPasswordProperty} Configuration properties for Application Specific Passwords
 *
 * @author <a href="mailto:greg.hill@open-xchange.com">Greg Hill</a>
 * @since v7.10.4
 */
public enum AppPasswordProperty implements Property {

    /**
     * Configures whether application-specific passwords are globally enabled or not.
     */
    ENABLED("enabled", Boolean.FALSE),
    
    /**
     * Defines a comma-separated list of client strings that identify appsuite UI or clients that should not be allowed to use application
     * passwords.
     */
    BLACKLISTED_CLIENTS("blacklistedClients", "open-xchange-appsuite,com.openexchange.ajax.framework.AJAXClient"),
    
    /**
     * Comma separated list of defined application types that should be available for a user, e.g. "mailapp,caldav,carddav,driveapp,webdav".
     */
    APP_TYPES("appTypes", ""),
    
    ;

    private final Object defaultValue;
    private final String fqn;

    /**
     * Initializes a new {@link AppPasswordProperty}.
     * 
     * @param suffix The property name suffix
     * @param defaultValue The property's default value
     */
    private AppPasswordProperty(String suffix, Object defaultValue) {
        this.defaultValue = defaultValue;
        this.fqn = "com.openexchange.authentication.application." + suffix;
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
