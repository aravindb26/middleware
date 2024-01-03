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

package com.openexchange.redis;

import com.openexchange.exception.OXException;

/**
 * {@link RedisConnectorProvider} - The Redis connector provider.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public interface RedisConnectorProvider {

    /**
     * Gets the configured operation mode.
     *
     * @return The configured operation mode
     */
    OperationMode getOperationMode();

    /**
     * Gets the effective connector dependent on operation mode.
     *
     * @return The effective connector
     * @throws OXException If effective connector cannot be returned
     */
    RedisConnector getConnector() throws OXException;

    /**
     * Checks if operation mode is set to <code>CLUSTER</code>.
     *
     * @return <code>true</code> if operation mode is set to <code>CLUSTER</code>; otherwise <code>false</code>
     */
    default boolean isCluster() {
        return getOperationMode() == OperationMode.CLUSTER;
    }

    /**
     * Checks if operation mode is set to <code>STAND_ALONE</code>.
     *
     * @return <code>true</code> if operation mode is set to <code>STAND_ALONE</code>; otherwise <code>false</code>
     */
    default boolean isStandAlone() {
        return getOperationMode() == OperationMode.STAND_ALONE;
    }

    /**
     * Checks if operation mode is set to <code>SENTINEL</code>.
     *
     * @return <code>true</code> if operation mode is set to <code>SENTINEL</code>; otherwise <code>false</code>
     */
    default boolean isSentinel() {
        return getOperationMode() == OperationMode.SENTINEL;
    }

}
