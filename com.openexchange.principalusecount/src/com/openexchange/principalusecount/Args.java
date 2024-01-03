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

package com.openexchange.principalusecount;

/**
 * {@link Args} - The arguments for principal use-count operation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class Args {

    /**
     * Creates a new builder for increment operation.
     *
     * @param principal The id of the principal
     * @return The new builder
     */
    public static Builder builderForIncrement(int principal) {
        return new Builder(principal, -1);
    }

    /**
     * Creates a new builder for reset operation.
     *
     * @param principal The id of the principal
     * @return The new builder
     */
    public static Builder builderForReset(int principal) {
        return new Builder(principal, -1);
    }

    /**
     * Creates a new builder for set operation.
     *
     * @param principal The id of the principal
     * @param value The use-count value
     * @return The new builder
     */
    public static Builder builderForSet(int principal, int value) {
        return new Builder(principal, value);
    }

    /** The builder for an instance of <code>Args</code> */
    public static class Builder {

        private final int principal;
        private final int value;
        private boolean async;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder(int principal, int value) {
            super();
            this.principal = principal;
            this.value = value;
            async = true;
        }

        /**
         * Sets whether asynchronous execution is allowed.
         *
         * @param async <code>true</code> if asynchronous execution is allowed; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withAsync(boolean async) {
            this.async = async;
            return this;
        }

        /**
         * Builds the instance of <code>Args</code> from this builder's arguments.
         *
         * @return The instance of <code>Args</code>
         */
        public Args build() {
            return new Args(principal, value, async);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    private final int principal;
    private final int value;
    private final boolean async;

    /**
     * Initializes a new {@link Args}.
     *
     * @param principal The id of the principal
     * @param value The use-count value
     * @param principals Multiple principal identifiers
     * @param async Whether asynchronous execution is allowed
     */
    public Args(int principal, int value, boolean async) {
        super();
        this.principal = principal;
        this.value = value;
        this.async = async;
    }

    /**
     * Gets the principal identifier
     *
     * @return The principal identifier
     */
    public int getPrincipal() {
        return principal;
    }

    /**
     * Gets the value to set use-count to
     *
     * @return The value
     */
    public int getValue() {
        return value;
    }

    /**
     * Checks if asynchronous execution is allowed
     *
     * @return <code>true</code> if asynchronous execution is allowed; otherwise <code>false</code>
     */
    public boolean isAsync() {
        return async;
    }

}
