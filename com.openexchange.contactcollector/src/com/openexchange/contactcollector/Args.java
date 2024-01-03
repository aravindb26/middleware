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

package com.openexchange.contactcollector;

import com.openexchange.session.Session;

/**
 * {@link Args} - The arguments for contact collector invocation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class Args {

    /**
     * Creates a new builder for contact collector invocation with asynchronous execution enabled.
     *
     * @param session The session
     * @return The new builder
     */
    public static Builder builder(Session session) {
        return new Builder(session);
    }

    /** The builder for an instance of <code>Args</code> */
    public static class Builder {

        private final Session session;
        private boolean incrementUseCount;
        private boolean async;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder(Session session) {
            super();
            this.session = session;
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
         * Sets whether to increment use-count or not.
         *
         * @param incrementUseCount <code>true</code> to increment use-count; otherwise <code>false</code>
         * @return This builder
         */
        public Builder withIncrementUseCount(boolean incrementUseCount) {
            this.incrementUseCount = incrementUseCount;
            return this;
        }

        /**
         * Builds the instance of <code>Args</code> from this builder's arguments.
         *
         * @return The instance of <code>Args</code>
         */
        public Args build() {
            return new Args(incrementUseCount, async, session);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------

    private final boolean async;
    private final boolean incrementUseCount;
    private final Session session;

    /**
     * Initializes a new {@link Args}.
     */
    public Args(boolean incrementUseCount, boolean async, Session session) {
        super();
        this.incrementUseCount = incrementUseCount;
        this.async = async;
        this.session = session;
    }

    /**
     * Gets the session
     *
     * @return The session
     */
    public Session getSession() {
        return session;
    }

    /**
     * Checks whether to increment use-count or not.
     *
     * @return <code>true</code> to increment use-count; otherwise <code>false</code>
     */
    public boolean isIncrementUseCount() {
        return incrementUseCount;
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
