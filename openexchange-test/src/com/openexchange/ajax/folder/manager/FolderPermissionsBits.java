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

package com.openexchange.ajax.folder.manager;

import static com.openexchange.java.Autoboxing.I;
import com.openexchange.testing.httpclient.models.FolderPermission;

/**
 * 
 * {@link FolderPermissionsBits}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v8.0.0
 */
public enum FolderPermissionsBits {

    /** Permission bits of the <b>administrator</b> */
    ADMIN(272662788),
    /** Permission bits of the <b>owner</b> */
    OWNER(403710016),
    /** Permission bits of an <b>author</b> */
    AUTHOR(4227332),
    /** Permission bits of an <b>reviewer</b> */
    REVIEWER(33025),
    /** Permission bits of an <b>viewer</b> */
    VIEWER(257),
    /** Permission bits of the <b>folder administrator</b> */
    FOLDERADMIN(268435456),
    ;

    private final int bits;

    private FolderPermissionsBits(int bits) {
        this.bits = bits;
    }

    /**
     * Get the permission bits
     *
     * @return The permission bits
     */
    public int getBits() {
        return bits;
    }

    /**
     * Get the permission bits
     *
     * @return The permission bits
     */
    public Integer getBitsI() {
        return I(bits);
    }

    /**
     * Get the permission as folder permission for the given user
     *
     * @param userId The user to create the folder permission for
     * @return The folder permission
     */
    public FolderPermission getPermission(int userId) {
        return getPermission(I(userId));
    }

    /**
     * Get the permission as folder permission for the given user
     *
     * @param userId The user to create the folder permission for
     * @return The folder permission
     */
    public FolderPermission getPermission(Integer userId) {
        FolderPermission p = new FolderPermission();
        p.setEntity(userId);
        p.setBits(I(this.bits));
        p.setIdentifier(userId.toString());
        p.setGroup(Boolean.FALSE);
        return p;
    }

}
