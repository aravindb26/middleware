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
 * {@link PreUpgradeProperty} - The generic pre-upgrade properties. Different pre-upgrade modules
 * should have their own properties under their own domain (e.g. {@link DatabasePreUpgradeProperty})
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public enum PreUpgradeProperty implements Property {

    /**
     * The property to decide on whether the server should prepare an upgrade or not.
     * Defaults to <code>false</code>
     */
    ENABLED("enabled", Boolean.FALSE),
    /**
     * The property to decide on whether the server should shut down after all preparation are completed or not.
     * Defaults to <code>true</code>
     */
    SHUTDOWN("shutdown", Boolean.TRUE);

    private final String fqn;
    private final Object defaultValue;

    /**
     * Initializes a new {@link PreUpgradeProperty}.
     *
     * @param appendix The appendix for the fully-qualifying name
     * @param defaultValue The default value
     */
    private PreUpgradeProperty(String appendix, Object defaultValue) {
        this.fqn = "com.openexchange.preupgrade." + appendix;
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
