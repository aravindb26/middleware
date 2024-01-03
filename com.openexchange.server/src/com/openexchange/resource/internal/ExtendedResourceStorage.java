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
package com.openexchange.resource.internal;

import java.sql.Connection;
import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.resource.Resource;
import com.openexchange.resource.ResourcePermission;
import com.openexchange.resource.storage.UsecountAwareResourceStorage;

/**
 * {@link ExtendedResourceStorage} extends the {@link UsecountAwareResourceStorage}
 * with some methods which help to handle delete operations for certain entities.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v8
 */
public interface ExtendedResourceStorage extends UsecountAwareResourceStorage {

    /**
     * Looks up the identifiers of those resources that have a reference to a specific user or group within their resource permissions.
     *
     * @param ctx The context
     * @param connection The connection to use
     * @param entity The entity identifier to lookup in the resource permissions table
     * @param group <code>true</code> if the entity refers to a group, <code>false</code>, otherwise
     * @return The identifiers of the referenced resources, or an empty array if there are none
     * @throws OXException
     */
    public List<Resource> getResourceIdsWithPermissionsForEntity(final Context ctx, Connection con, int entity, boolean group) throws OXException;

    /**
     * Deletes the permission of the given resource
     *
     * @param ctx The context
     * @param connection The connection to use
     * @param resourceId The resource id
     * @return The number of deleted permissions
     * @throws OXException
     */
    public int deletePermissions(Context ctx, Connection connection, int resourceId) throws OXException;

    /**
     * Inserts the given permissions
     *
     * @param ctx The context
     * @param connection The connection to use
     * @param resourceId The resource id
     * @param permissions The permissions to insert
     * @return The number of inserted permissions
     * @throws OXException
     */
    public int[] insertPermissions(Context ctx, Connection connection, int resourceId, ResourcePermission[] permissions) throws OXException;

}
