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

import com.google.common.collect.ImmutableList;
import java.util.List;


/**
 * {@link DefaultDeputyPermission} - The default implementation of a deputy permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DefaultDeputyPermission implements DeputyPermission {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>DefaultDeputyPermission</code> */
    public static class Builder {

        private int entityId;
        private boolean group;
        private List<ModulePermission> modulePermissions;
        private boolean sendOnBehalfOf;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
            entityId = -1;
        }

        /**
         * Sets the entity identifier.
         *
         * @param entityId The entity identifier to set
         * @return This builder
         */
        public Builder withEntityId(int entityId) {
            this.entityId = entityId;
            return this;
        }

        /**
         * Sets whether entity is group or a user.
         *
         * @param group <code>true</code> if entity is a group; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withGroup(boolean group) {
            this.group = group;
            return this;
        }

        /**
         * Sets whether sending on behalf of is allowed.
         *
         * @param sendOnBehalfOf <code>true</code> if sending on behalf of is allowed; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withSendOnBehalfOf(boolean sendOnBehalfOf) {
            this.sendOnBehalfOf = sendOnBehalfOf;
            return this;
        }

        /**
         * Sets the module permissions.
         *
         * @param modulePermissions The module permissions
         * @return This builder
         */
        public Builder withModulePermissions(List<ModulePermission> modulePermissions) {
            this.modulePermissions = modulePermissions;
            return this;
        }

        /**
         * Creates the instance of <code>DefaultDeputyPermission</code> from this builder's arguments.
         *
         * @return The instance of <code>DefaultDeputyPermission</code>
         * @throws IllegalArgumentException If this builder has missing or invalid arguments
         */
        public DefaultDeputyPermission build() {
            if (entityId <= 0) {
                throw new IllegalArgumentException("Entity identifier not set or invalid");
            }
            if (modulePermissions == null || modulePermissions.isEmpty()) {
                throw new IllegalArgumentException("Module permissions not set or invalid");
            }
            return new DefaultDeputyPermission(entityId, group, sendOnBehalfOf, modulePermissions);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final int entityId;
    private final boolean group;
    private final List<ModulePermission> modulePermissions;
    private final boolean sendOnBehalfOf;

    /**
     * Initializes a new {@link DefaultDeputyPermission}.
     *
     * @param entityId The entity identifier
     * @param group Whether entity is group or a user
     * @param sendOnBehalfOf Whether sending on behalf of is allowed
     * @param modulePermissions The module permissions
     */
    DefaultDeputyPermission(int entityId, boolean group, boolean sendOnBehalfOf, List<ModulePermission> modulePermissions) {
        super();
        this.entityId = entityId;
        this.group = group;
        this.sendOnBehalfOf = sendOnBehalfOf;
        this.modulePermissions = modulePermissions == null ? ImmutableList.of() : ImmutableList.copyOf(modulePermissions);
    }

    @Override
    public int getEntityId() {
        return entityId;
    }

    @Override
    public boolean isGroup() {
        return group;
    }

    @Override
    public List<ModulePermission> getModulePermissions() {
        return modulePermissions;
    }

    @Override
    public boolean isSendOnBehalfOf() {
        return sendOnBehalfOf;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("[entityId=").append(entityId).append(", group=").append(group).append(", ");
        if (modulePermissions != null) {
            b.append("modulePermissions=").append(modulePermissions).append(", ");
        }
        b.append("sendOnBehalfOf=").append(sendOnBehalfOf).append(']');
        return b.toString();
    }

}
