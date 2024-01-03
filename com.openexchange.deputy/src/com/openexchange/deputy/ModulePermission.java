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
import java.util.Optional;

/**
 * {@link ModulePermission} - Represent a permission to apply to a certain module's folders.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface ModulePermission {

    /**
     * Gets the module identifier.
     *
     * @return The module identifier
     */
    String getModuleId();

    /**
     * Gets the optional list of folder identifiers.
     * <p>
     * If absent, the appropriate default folder of the associated module is supposed to be used.
     *
     * @return The optional list of folder identifiers or empty
     */
    Optional<List<String>> getOptionalFolderIds();

    /**
     * Gets the permission to grant.
     *
     * @return The permission
     */
    Permission getPermission();

}
