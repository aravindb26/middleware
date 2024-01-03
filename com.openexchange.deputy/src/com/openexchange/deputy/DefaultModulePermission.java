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
 * {@link DefaultModulePermission} - The default implementation of module permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DefaultModulePermission implements ModulePermission {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>DefaultModulePermission</code> */
    public static class Builder {

        private String moduleId;
        private List<String> folderIds;
        private Permission permission;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Sets the module identifier.
         *
         * @param moduleId The module identifier to set
         * @return This builder
         */
        public Builder withModuleId(String moduleId) {
            this.moduleId = moduleId;
            return this;
        }

        /**
         * Sets the folder identifiers.
         *
         * @param folderIds The folder identifiers to set
         * @return This builder
         */
        public Builder withFolderIds(List<String> folderIds) {
            this.folderIds = folderIds;
            return this;
        }

        /**
         * Sets the permission.
         *
         * @param permission The permission to set
         * @return This builder
         */
        public Builder withPermission(Permission permission) {
            this.permission = permission;
            return this;
        }

        /**
         * Creates the instance of <code>DefaultModulePermission</code> from this builder's arguments.
         *
         * @return The instance of <code>DefaultModulePermission</code>
         * @throws IllegalArgumentException If this builder has missing or invalid arguments
         */
        public DefaultModulePermission build() {
            if (moduleId == null) {
                throw new IllegalArgumentException("Module identifier not set or invalid");
            }
            if (permission == null) {
                throw new IllegalArgumentException("Permissions not set or invalid");
            }
            return new DefaultModulePermission(moduleId, permission, folderIds == null || folderIds.isEmpty() ? Optional.empty() : Optional.of(folderIds));
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String moduleId;
    private final Optional<List<String>> optionalFolderIds;
    private final Permission permission;

    /**
     * Initializes a new {@link DefaultModulePermission}.
     *
     * @param moduleId The module identifier
     * @param permission The permission
     * @param optionalFolderIds The optional list of folder identifiers
     */
    DefaultModulePermission(String moduleId, Permission permission, Optional<List<String>> optionalFolderIds) {
        super();
        this.moduleId = moduleId;
        this.permission = permission;
        this.optionalFolderIds = optionalFolderIds;
    }

    @Override
    public String getModuleId() {
        return moduleId;
    }

    @Override
    public Optional<List<String>> getOptionalFolderIds() {
        return optionalFolderIds;
    }

    @Override
    public Permission getPermission() {
        return permission;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append('[');
        if (moduleId != null) {
            b.append("moduleId=").append(moduleId).append(", ");
        }
        if (optionalFolderIds != null) {
            b.append("optionalFolderIds=").append(optionalFolderIds).append(", ");
        }
        if (permission != null) {
            b.append("permission=").append(permission);
        }
        b.append(']');
        return b.toString();
    }

}
