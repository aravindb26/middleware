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

package com.openexchange.folderstorage.database;

import com.openexchange.folderstorage.BasicPermission;
import com.openexchange.server.impl.OCLPermission;

/**
 * {@link DatabasePermission} - A mail folder permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DatabasePermission extends BasicPermission {

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3807791429233228135L;

    /**
     * Converts an array of {@link OCLPermission}s to their folder storage compatible {@link DatabasePermission}s equivalents.
     * 
     * @param oclPermissions The OCL permissions to convert
     * @return The database folder permissions
     */
    public static DatabasePermission[] fromOCLPermissions(OCLPermission[] oclPermissions) {
        if (null == oclPermissions) {
            return null;
        }
        DatabasePermission[] permissions = new DatabasePermission[oclPermissions.length];
        for (int i = 0; i < oclPermissions.length; i++) {
            permissions[i] = DatabasePermission.fromOCLPermissions(oclPermissions[i]);
        }
        return permissions;
    }

    /**
     * Converts an {@link OCLPermission} to its folder storage compatible {@link DatabasePermission} equivalents.
     * 
     * @param oclPermission The OCL permission to convert
     * @return The database folder permission
     */
    public static DatabasePermission fromOCLPermissions(OCLPermission oclPermission) {
        return new DatabasePermission(oclPermission);
    }

    public DatabasePermission(final OCLPermission oclPermission) {
        super();
        admin = oclPermission.isFolderAdmin();
        deletePermission = oclPermission.getDeletePermission();
        entity = oclPermission.getEntity();
        folderPermission = oclPermission.getFolderPermission();
        group = oclPermission.isGroupPermission();
        readPermission = oclPermission.getReadPermission();
        system = oclPermission.getSystem();
        type = oclPermission.getType();
        legator = oclPermission.getPermissionLegator();
        writePermission = oclPermission.getWritePermission();
    }

    @Override
    public String toString() {
        return new StringBuilder(64)
            .append((admin ? "_FolderAdmin" : "")).append((group ? "Group" : "User")).append(entity).append('@')
            .append(folderPermission).append('.').append(readPermission).append('.').append(writePermission).append('.').append(deletePermission)
            .append(' ').append("system").append('=').append(system)
            .append(' ').append("type").append('=').append(type).toString();
    }
}
