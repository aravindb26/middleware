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

package com.openexchange.mail.compose;

/**
 * {@link DeleteAfterTransportOptions} - The options telling how to handle the draft associated with composition space after resulting mail
 * has been sent.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class DeleteAfterTransportOptions {

    /**
     * Creates a new builder instance.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>DeleteAfterTransportOptions</code> */
    public static class Builder {

        private boolean deleteAfterTransport;
        private boolean deleteDraftAfterTransport;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Sets the delete-after-transport flag.
         *
         * @param deleteAfterTransport The delete-after-transport flag to set
         * @return This builder
         */
        public Builder withDeleteAfterTransport(boolean deleteAfterTransport) {
            this.deleteAfterTransport = deleteAfterTransport;
            return this;
        }

        /**
         * Sets the delete-draft-after-transport flag.
         *
         * @param deleteDraftAfterTransport The delete-draft-after-transport flag to set
         * @return This builder
         */
        public Builder withDeleteDraftAfterTransport(boolean deleteDraftAfterTransport) {
            this.deleteDraftAfterTransport = deleteDraftAfterTransport;
            return this;
        }

        /**
         * Builds the resulting instance of <code>DeleteAfterTransportOptions</code> from this builder's properties.
         *
         * @return The resulting instance of <code>DeleteAfterTransportOptions</code>
         */
        public DeleteAfterTransportOptions build() {
            return new DeleteAfterTransportOptions(deleteAfterTransport, deleteDraftAfterTransport);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final boolean deleteAfterTransport;
    private final boolean deleteDraftAfterTransport;

    /**
     * Initializes a new {@link DeleteAfterTransportOptions}.
     *
     * @param deleteAfterTransport The delete-after-transport flag
     * @param deleteDraftAfterTransport The delete-draft-after-transport flag
     */
    DeleteAfterTransportOptions(boolean deleteAfterTransport, boolean deleteDraftAfterTransport) {
        super();
        this.deleteAfterTransport = deleteAfterTransport;
        this.deleteDraftAfterTransport = deleteDraftAfterTransport;
    }

    /**
     * Gets the he delete-after-transport flag.
     *
     * @return The he delete-after-transport flag
     */
    public boolean isDeleteAfterTransport() {
        return deleteAfterTransport;
    }

    /**
     * Gets the he delete-draft-after-transport flag.
     *
     * @return The he delete-draft-after-transport flag
     */
    public boolean isDeleteDraftAfterTransport() {
        return deleteDraftAfterTransport;
    }

}
