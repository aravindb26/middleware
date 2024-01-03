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

package com.openexchange.demo;

import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.OXGroupInterface;
import com.openexchange.admin.rmi.OXResourceInterface;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.OXUtilInterface;

/**
 * {@link ProvisioningInterfaces} - Provides access to provisioning interfaces.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class ProvisioningInterfaces {

    /**
     * Creates a new builder.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>ProvisioningInterfaces</code> */
    public static class Builder {

        private OXUtilInterface utilInterface;
        private OXUserInterface userInterface;
        private OXContextInterface contextInterface;
        private OXGroupInterface groupInterface;
        private OXResourceInterface resourceInterface;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Sets the util interface
         *
         * @param utilInterface The util interface to set
         * @return This builder
         */
        public Builder withUtilInterface(OXUtilInterface utilInterface) {
            this.utilInterface = utilInterface;
            return this;
        }

        /**
         * Sets the user interface
         *
         * @param userInterface The user interface to set
         * @return This builder
         */
        public Builder withUserInterface(OXUserInterface userInterface) {
            this.userInterface = userInterface;
            return this;
        }

        /**
         * Sets the context interface
         *
         * @param contextInterface The context interface to set
         * @return This builder
         */
        public Builder withContextInterface(OXContextInterface contextInterface) {
            this.contextInterface = contextInterface;
            return this;
        }

        /**
         * Sets the group interface.
         *
         * @param groupInterface The group interface to set
         * @return This builder
         */
        public Builder withGroupInterface(OXGroupInterface groupInterface) {
            this.groupInterface = groupInterface;
            return this;
        }

        /**
         * Sets the resource interface
         *
         * @param resourceInterface The resource interface to set
         * @return This builder
         */
        public Builder withResourceInterface(OXResourceInterface resourceInterface) {
            this.resourceInterface = resourceInterface;
            return this;
        }

        /**
         * Builds the instance of <b>ProvisioningInterfaces</b> from this builder's arguments.
         *
         * @return The instance of <b>ProvisioningInterfaces</b>
         */
        public ProvisioningInterfaces build() {
            return new ProvisioningInterfaces(utilInterface, userInterface, contextInterface, groupInterface, resourceInterface);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final OXUtilInterface utilInterface;
    private final OXUserInterface userInterface;
    private final OXContextInterface contextInterface;
    private final OXGroupInterface groupInterface;
    private final OXResourceInterface resourceInterface;

    /**
     * Initializes a new {@link ProvisioningInterfaces}.
     *
     * @param utilInterface The {@link OXUtilInterface}
     * @param userInterface The {@link OXUserInterface}
     * @param contextInterface The {@link OXContextInterface}
     * @param groupInterface The {@link OXGroupInterface}
     * @param resourceInterface The {@link OXResourceInterface}
     */
    ProvisioningInterfaces(OXUtilInterface utilInterface, OXUserInterface userInterface, OXContextInterface contextInterface, OXGroupInterface groupInterface, OXResourceInterface resourceInterface) {
        super();
        this.utilInterface = utilInterface;
        this.userInterface = userInterface;
        this.contextInterface = contextInterface;
        this.groupInterface = groupInterface;
        this.resourceInterface = resourceInterface;
    }

    /**
     * Gets the util interface.
     *
     * @return The util interface
     */
    public OXUtilInterface getUtilInterface() {
        return utilInterface;
    }

    /**
     * Gets the user interface.
     *
     * @return The user interface
     */
    public OXUserInterface getUserInterface() {
        return userInterface;
    }

    /**
     * Gets the context interface.
     *
     * @return The context interface
     */
    public OXContextInterface getContextInterface() {
        return contextInterface;
    }

    /**
     * Gets the group interface.
     *
     * @return The group interface
     */
    public OXGroupInterface getGroupInterface() {
        return groupInterface;
    }

    /**
     * Gets the resource interface.
     *
     * @return The resource interface
     */
    public OXResourceInterface getResourceInterface() {
        return resourceInterface;
    }

}
