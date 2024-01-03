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

/**
 * {@link Permission}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public interface Permission {

    /**
     * The constant for no permission at all.
     */
    public static final int NO_PERMISSIONS = 0;

    /**
     * The constant for maximum permission.
     */
    public static final int MAX_PERMISSION = 128;

    /**
     * The permission constant granting folder visibility.
     */
    public static final int READ_FOLDER = 2;

    /**
     * The permission constant granting folder visibility and allowing to create objects in folder.
     */
    public static final int CREATE_OBJECTS_IN_FOLDER = 4;

    /**
     * The permission constant granting folder visibility, allowing to create objects in folder, and allowing to create subfolders below
     * folder.
     */
    public static final int CREATE_SUB_FOLDERS = 8;

    /**
     * The permission constant granting visibility for own objects.
     */
    public static final int READ_OWN_OBJECTS = 2;

    /**
     * The permission constant granting visibility for all objects.
     */
    public static final int READ_ALL_OBJECTS = 4;

    /**
     * The permission constant allowing to edit own objects.
     */
    public static final int WRITE_OWN_OBJECTS = 2;

    /**
     * The permission constant allowing to edit all objects.
     */
    public static final int WRITE_ALL_OBJECTS = 4;

    /**
     * The permission constant allowing to remove own objects.
     */
    public static final int DELETE_OWN_OBJECTS = 2;

    /**
     * The permission constant allowing to remove all objects.
     */
    public static final int DELETE_ALL_OBJECTS = 4;

    /*-
     * #################### METHODS ####################
     */

    /**
     * Checks if folder visibility is granted by this permission.
     *
     * @return <code>true</code> if folder visibility is granted (either admin or appropriate folder permission); otherwise <code>false</code>
     */
    boolean isVisible();

    /**
     * Gets this folder permission's entity identifier.
     *
     * @return This folder permission's entity identifier
     */
    int getEntity();

    /**
     * Checks if this permission's entity is a group.
     *
     * @return <code>true</code> if this permission's entity is a group; otherwise <code>false</code>
     */
    public boolean isGroup();

    /**
     * Checks if this permission denotes its entity as a folder administrator.
     *
     * @return <code>true</code> if this folder permission's entity is a folder administrator; otherwise <code>false</code>
     */
    boolean isAdmin();

    /**
     * Gets the folder permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #READ_FOLDER}</li>
     * <li>{@link #CREATE_OBJECTS_IN_FOLDER}</li>
     * <li>{@link #CREATE_SUB_FOLDERS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The folder permission
     */
    int getFolderPermission();

    /**
     * Gets the read permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #READ_OWN_OBJECTS}</li>
     * <li>{@link #READ_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The read permission
     */
    int getReadPermission();

    /**
     * Gets the write permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #WRITE_OWN_OBJECTS}</li>
     * <li>{@link #WRITE_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The write permission
     */
    int getWritePermission();

    /**
     * Gets the delete permission.
     * <p>
     * Returned value is one of:
     * <ul>
     * <li>{@link #NO_PERMISSIONS}</li>
     * <li>{@link #DELETE_OWN_OBJECTS}</li>
     * <li>{@link #DELETE_ALL_OBJECTS}</li>
     * <li>{@link #MAX_PERMISSION}</li>
     * </ul>
     *
     * @return The delete permission
     */
    int getDeletePermission();

}
