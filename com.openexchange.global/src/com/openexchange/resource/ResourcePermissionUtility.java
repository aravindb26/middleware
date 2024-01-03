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

package com.openexchange.resource;

import com.openexchange.user.User;

/**
 * {@link ResourcePermissionUtility}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public class ResourcePermissionUtility {

    /** The identifier of the virtual group 'all users' - com.openexchange.group.GroupStorage.GROUP_ZERO_IDENTIFIER */
    private static final int GROUP_ALL_USERS = 0;

    /**
     * The implicit default permissions of <i>unmanaged</i> resources, which are assumed for resources where no other permissions are
     * defined in.
     */
    public static final ResourcePermission[] DEFAULT_PERMISSIONS = new ResourcePermission[] { 
        new ResourcePermission(GROUP_ALL_USERS, true, SchedulingPrivilege.BOOK_DIRECTLY)
    };

    /**
     * Initializes a new {@link ResourcePermissionUtility}
     */
    private ResourcePermissionUtility() {
        super();
    }

    /**
     * Gets the effective scheduling privilege for a resource of a certain user.
     * 
     * @param resource The resource to evaluate the user's effective privilege for
     * @param user The user to evaluate the effective resource scheduling privilege for
     * @return The effective scheduling privilege
     */
    public static SchedulingPrivilege getEffectivePrivilege(Resource resource, User user) {
        ResourcePermission[] permissions = resource.getPermissions();
        if (null == permissions) {
            permissions = DEFAULT_PERMISSIONS;
        }
        SchedulingPrivilege effectivePrivilege = SchedulingPrivilege.NONE;
        for (ResourcePermission permission : permissions) {
            /*
             * lookup highest scheduling privilege, considering user- and group permissions
             */
            if (effectivePrivilege.ordinal() < permission.getSchedulingPrivilege().ordinal()) {
                if (permission.isGroup()) {
                    /*
                     * take over privilege if user is member of referenced group
                     */
                    if (com.openexchange.tools.arrays.Arrays.contains(user.getGroups(), permission.getEntity())) {
                        effectivePrivilege = permission.getSchedulingPrivilege();
                    }
                } else if (permission.getEntity() == user.getId()) {
                    /*
                     * take over privilege of user entity
                     */
                    effectivePrivilege = permission.getSchedulingPrivilege();
                }
            }
        }
        return effectivePrivilege;
    }
    
}
