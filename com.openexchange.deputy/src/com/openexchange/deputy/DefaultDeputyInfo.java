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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.openexchange.java.Strings;

/**
 * {@link DefaultDeputyInfo} - Default implementation of deputy information.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DefaultDeputyInfo implements DeputyInfo {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>DefaultDeputyInfo</code> */
    public static class Builder {

        private int userId;
        private int entityId;
        private boolean group;
        private String deputyId;
        private List<String> moduleIds;
        private boolean sendOnBehalfOf;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
            entityId = -1;
            userId = -1;
        }

        /**
         * Sets this builder's arguments to the ones from specified deputy information
         *
         * @param deputyInfo The deputy information to copy from
         * @return This builder
         */
        public Builder copyFrom(DeputyInfo deputyInfo) {
            if (deputyInfo != null) {
                entityId = deputyInfo.getEntityId();
                group = deputyInfo.isGroup();
                deputyId = deputyInfo.getDeputyId();
                sendOnBehalfOf = deputyInfo.isSendOnBehalfOf();
                moduleIds = deputyInfo.getModuleIds();
            }
            return this;
        }

        /**
         * Sets the identifier of the granting user.
         *
         * @param userId The user identifier to set
         * @return This builder
         */
        public Builder withUserId(int userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the module identifiers.
         *
         * @param moduleIds The module identifiers to set
         * @return This builder
         */
        public Builder withModuleIds(List<String> moduleIds) {
            this.moduleIds = moduleIds;
            return this;
        }

        /**
         * Sets the module identifiers.
         *
         * @param moduleIds The module identifiers to set
         * @return This builder
         */
        public Builder withModuleIds(Collection<String> moduleIds) {
            if (moduleIds instanceof List) {
                return withModuleIds((List<String>) moduleIds);
            }

            this.moduleIds = new ArrayList<String>(moduleIds);
            return this;
        }

        /**
         * Sets the deputy identifier.
         *
         * @param deputyId The deputy identifier to set
         * @return This builder
         */
        public Builder withDeputyId(String deputyId) {
            this.deputyId = deputyId;
            return this;
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
         * Creates the instance of <code>DefaultDeputyInfo</code> from this builder's arguments.
         *
         * @return The instance of <code>DefaultDeputyInfo</code>
         * @throws IllegalArgumentException If this builder has missing or invalid arguments
         */
        public DefaultDeputyInfo build() {
            if (entityId <= 0) {
                throw new IllegalArgumentException("Entity identifier not set or invalid");
            }
            if (userId <= 0) {
                throw new IllegalArgumentException("Grantee identifier not set or invalid");
            }
            if (Strings.isEmpty(deputyId)) {
                throw new IllegalArgumentException("Deputy identifier not set or invalid");
            }
            if (moduleIds == null || moduleIds.isEmpty()) {
                throw new IllegalArgumentException("Module identifiers not set or invalid");
            }
            return new DefaultDeputyInfo(userId, deputyId, entityId, group, sendOnBehalfOf, moduleIds);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final int userId;
    private final int entityId;
    private final boolean group;
    private final String deputyId;
    private final List<String> moduleIds;
    private final boolean sendOnBehalfOf;

    /**
     * Initializes a new {@link DefaultDeputyInfo}.
     *
     * @param userId The identifier of the granting user
     * @param deputyId The deputy identifier
     * @param entityId The entity identifier
     * @param group Whether entity is group or a user
     * @param sendOnBehalfOf Whether sending on behalf of is allowed
     * @param moduleIds The module identifiers
     */
    DefaultDeputyInfo(int userId, String deputyId, int entityId, boolean group, boolean sendOnBehalfOf, List<String> moduleIds) {
        super();
        this.userId = userId;
        this.deputyId = deputyId;
        this.entityId = entityId;
        this.group = group;
        this.sendOnBehalfOf = sendOnBehalfOf;
        this.moduleIds = ImmutableList.copyOf(moduleIds);
    }

    @Override
    public int getUserId() {
        return userId;
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
    public String getDeputyId() {
        return deputyId;
    }

    @Override
    public java.util.List<String> getModuleIds() {
        return moduleIds;
    }

    @Override
    public boolean isSendOnBehalfOf() {
        return sendOnBehalfOf;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(64);
        b.append("[entityId=").append(entityId).append(", group=").append(group).append(", ");
        if (deputyId != null) {
            b.append("deputyId=").append(deputyId).append(", ");
        }
        if (moduleIds != null) {
            b.append("moduleIds=").append(moduleIds).append(", ");
        }
        b.append("sendOnBehalfOf=").append(sendOnBehalfOf).append(']');
        return b.toString();
    }

}
