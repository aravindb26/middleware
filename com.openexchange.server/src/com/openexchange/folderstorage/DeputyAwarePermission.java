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

package com.openexchange.folderstorage;


/**
 * {@link DeputyAwarePermission} - A permission knowing if backed by a deputy permission or not.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface DeputyAwarePermission extends Permission {

    /**
     * Checks if this interface is supported at all.
     *
     * @return <code>true</code> if supported; otherwise <code>false</code>
     */
    boolean isDeputyPermissionSupported();

    /**
     * Checks if this permission is a deputy permission
     *
     * @return <code>true</code> for deputy permission; otherwise <code>false</code>
     */
    boolean isDeputyPermission();

}
