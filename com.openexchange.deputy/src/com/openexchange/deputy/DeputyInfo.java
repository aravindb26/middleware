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

package com.openexchange.deputy;

import java.util.List;

/**
 * {@link DeputyInfo} - Basic information for a deputy.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface DeputyInfo {

    /**
     * Gets the deputy identifier.
     *
     * @return The deputy identifier
     */
    String getDeputyId();

    /**
     * Gets the identifier of the granting user.
     *
     * @return The user identifier
     */
    int getUserId();

    /**
     * Gets the entity identifier.
     *
     * @return The entity identifier
     */
    int getEntityId();

    /**
     * Checks if this deputy permission's entity is a group.
     *
     * @return <code>true</code> if this deputy permission's entity is a group; otherwise <code>false</code>
     */
    boolean isGroup();

    /**
     * Whether sending on behalf of is allowed.
     *
     * @return <code>true</code> if sending on behalf of is allowed; otherwise <code>false</code>
     */
    boolean isSendOnBehalfOf();

    /**
     * Gets the module identifiers.
     *
     * @return The module identifiers
     */
    List<String> getModuleIds();

}
