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
 * {@link DefaultPermission} - The default permission implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DefaultPermission implements Permission {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>DefaultPermission</code> */
    public static class Builder {

        private int entity;
        private boolean group;
        private boolean admin;
        private int folderPermission;
        private int readPermission;
        private int writePermission;
        private int deletePermission;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
            entity = -1;
            folderPermission = Permission.NO_PERMISSIONS;
            readPermission = Permission.NO_PERMISSIONS;
            deletePermission = Permission.NO_PERMISSIONS;
            writePermission = Permission.NO_PERMISSIONS;
            admin = false;
        }

        /**
         * Sets if this permission denotes its entity as a folder administrator.
         *
         * @param admin <code>true</code> if this permission's entity is a folder administrator; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withAdmin(final boolean admin) {
            this.admin = admin;
            return this;
        }

        /**
         * Convenience method to set all permissions at once.
         *
         * @param folderPermission The folder permission
         * @param readPermission The read permission
         * @param writePermission The write permission
         * @param deletePermission The delete permission
         * @see #setFolderPermission(int)
         * @see #setReadPermission(int)
         * @see #setWritePermission(int)
         * @see #setDeletePermission(int)
         * @return This builder
         */
        public Builder withAllPermissions(final int folderPermission, final int readPermission, final int writePermission, final int deletePermission) {
            this.folderPermission = folderPermission;
            this.readPermission = readPermission;
            this.deletePermission = deletePermission;
            this.writePermission = writePermission;
            return this;
        }

        /**
         * Sets the delete permission.
         * <p>
         * Passed value is one of:
         * <ul>
         * <li>{@link #NO_PERMISSIONS}</li>
         * <li>{@link #DELETE_OWN_OBJECTS}</li>
         * <li>{@link #DELETE_ALL_OBJECTS}</li>
         * <li>{@link #MAX_PERMISSION}</li>
         * </ul>
         *
         * @param permission The delete permission
         * @return This builder
         */
        public Builder withDeletePermission(final int permission) {
            deletePermission = permission;
            return this;
        }

        /**
         * Sets this permission's entity identifier.
         *
         * @param entity The entity identifier
         * @return This builder
         */
        public Builder withEntity(int entity) {
            this.entity = entity;
            return this;
        }

        /**
         * Sets the folder permission.
         * <p>
         * Passed value is one of:
         * <ul>
         * <li>{@link #NO_PERMISSIONS}</li>
         * <li>{@link #READ_FOLDER}</li>
         * <li>{@link #CREATE_OBJECTS_IN_FOLDER}</li>
         * <li>{@link #CREATE_SUB_FOLDERS}</li>
         * <li>{@link #MAX_PERMISSION}</li>
         * </ul>
         *
         * @param permission The folder permission
         * @return This builder
         */
        public Builder withFolderPermission(final int permission) {
            folderPermission = permission;
            return this;
        }

        public Builder withGroup(final boolean group) {
            this.group = group;
            return this;
        }

        /**
         * Convenience method which passes {@link #MAX_PERMISSION} to all permissions and sets folder administrator flag to <code>true</code>.
         *
         * @return This builder
         */
        public Builder withMaxPermissions() {
            folderPermission = Permission.MAX_PERMISSION;
            readPermission = Permission.MAX_PERMISSION;
            deletePermission = Permission.MAX_PERMISSION;
            writePermission = Permission.MAX_PERMISSION;
            admin = true;
            return this;
        }

        /**
         * Convenience method which passes {@link #NO_PERMISSIONS} to all permissions and sets folder administrator flag to <code>false</code>.
         *
         * @return This builder
         */
        public Builder withNoPermissions() {
            folderPermission = Permission.NO_PERMISSIONS;
            readPermission = Permission.NO_PERMISSIONS;
            deletePermission = Permission.NO_PERMISSIONS;
            writePermission = Permission.NO_PERMISSIONS;
            admin = false;
            return this;
        }

        /**
         * Sets the read permission.
         * <p>
         * Passed value is one of:
         * <ul>
         * <li>{@link #NO_PERMISSIONS}</li>
         * <li>{@link #READ_OWN_OBJECTS}</li>
         * <li>{@link #READ_ALL_OBJECTS}</li>
         * <li>{@link #MAX_PERMISSION}</li>
         * </ul>
         *
         * @param permission The read permission
         * @return This builder
         */
        public Builder withReadPermission(final int permission) {
            readPermission = permission;
            return this;
        }

        /**
         * Sets the write permission.
         * <p>
         * Passed value is one of:
         * <ul>
         * <li>{@link #NO_PERMISSIONS}</li>
         * <li>{@link #WRITE_OWN_OBJECTS}</li>
         * <li>{@link #WRITE_ALL_OBJECTS}</li>
         * <li>{@link #MAX_PERMISSION}</li>
         * </ul>
         *
         * @param permission The write permission
         * @return This builder
         */
        public Builder withWritePermission(final int permission) {
            writePermission = permission;
            return this;
        }

        /**
         * Creates the instance of <code>DefaultPermission</code> from this builder's arguments.
         *
         * @return The instance of <code>DefaultPermission</code>
         */
        public DefaultPermission build() {
            return new DefaultPermission(entity, group, admin, folderPermission, readPermission, writePermission, deletePermission);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final int entity;
    private final boolean group;
    private final boolean admin;
    private final int folderPermission;
    private final int readPermission;
    private final int writePermission;
    private final int deletePermission;

    /**
     * Initializes a new {@link DefaultPermission}.
     */
    DefaultPermission(int entity, boolean group, boolean admin, int folderPermission, int readPermission, int writePermission, int deletePermission) {
        super();
        this.entity = entity;
        this.group = group;
        this.admin = admin;
        this.folderPermission = folderPermission;
        this.readPermission = readPermission;
        this.writePermission = writePermission;
        this.deletePermission = deletePermission;
    }

    @Override
    public boolean isVisible() {
        return isAdmin() || getFolderPermission() > NO_PERMISSIONS;
    }

    @Override
    public int getEntity() {
        return entity;
    }

    @Override
    public boolean isGroup() {
        return group;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }

    @Override
    public int getFolderPermission() {
        return folderPermission;
    }

    @Override
    public int getReadPermission() {
        return readPermission;
    }

    @Override
    public int getWritePermission() {
        return writePermission;
    }

    @Override
    public int getDeletePermission() {
        return deletePermission;
    }

    @Override
    public String toString() {
        return new StringBuilder(64)
            .append("[entity=").append(entity)
            .append(", group=").append(group)
            .append(", admin=").append(admin)
            .append(", folderPermission=").append(folderPermission)
            .append(", readPermission=").append(readPermission)
            .append(", writePermission=").append(writePermission)
            .append(", deletePermission=").append(deletePermission)
            .append(']')
            .toString();
    }

}
