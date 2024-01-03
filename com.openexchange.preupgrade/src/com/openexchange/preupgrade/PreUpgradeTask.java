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

import com.openexchange.exception.OXException;

/**
 * {@link PreUpgradeTask} - Defines a pre-upgrade task
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public interface PreUpgradeTask {

    /**
     * Prepares the upgrade.
     * <p>
     * Gathers required information for the upgrade. Does a pre-flight check before the actual upgrade.
     *
     * @return The number of steps to perform by this task
     * @throws OXException If an error occurred during the preparation of the upgrade
     */
    int prepareUpgrade() throws OXException;

    /**
     * Execute this pre-upgrade task.
     *
     * @throws OXException If an error occurred during the upgrade
     */
    void executeUpgrade() throws OXException;

    /**
     * Whether this pre-upgrade task needs to be executed or not
     *
     * @return <code>true</code> if it needs to be executed, <code>false</code> otherwise
     */
    boolean isRequired();

    /**
     * Gets the name of the pre-upgrade task.
     * <p>
     * Default behavior is to return {@link Class#getSimpleName()} as task name.
     *
     * @return The name
     */
    default String getName() {
        return getClass().getSimpleName();
    }
}
