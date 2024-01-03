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

package com.openexchange.preupgrade.properties;

import com.openexchange.config.lean.Property;

/**
 * {@link DatabasePreUpgradeProperty} - The database pre-upgrade properties
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public enum DatabasePreUpgradeProperty implements Property {

    /**
     * Defines the schemata to pre-upgrade. If empty, then all schemata will be upgraded.
     * Defaults to empty
     */
    SCHEMATA("schemata", "");

    private final String fqn;
    private final Object defaultValue;

    /**
     * Initializes a new {@link DatabasePreUpgradeProperty}.
     *
     * @param appendix The appendix for the fully-qualifying name
     * @param defaultValue The default value
     */
    private DatabasePreUpgradeProperty(String appendix, Object defaultValue) {
        this.fqn = "com.openexchange.preupgrade.database." + appendix;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the fully-qualifying name for the property
     *
     * @return the fully-qualifying name for the property
     */
    @Override
    public String getFQPropertyName() {
        return fqn;
    }

    /**
     * Returns the default value of this property
     *
     * @return the default value of this property
     */
    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }
}
