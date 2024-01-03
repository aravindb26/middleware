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

/**
 * {@link InitializationState}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public class InitializationState {

    /**
     * Creates a new builder.
     *
     * @param totalNumberOfContexts The total number of contexts that should be created
     * @return The new builder
     */
    public static Builder builder(int totalNumberOfContexts) {
        return new Builder(totalNumberOfContexts);
    }

    /**
     * Creates a new builder.
     *
     * @param source The source
     * @return The new builder
     */
    public static Builder builder(InitializationState source) {
        return new Builder(source);
    }

    /** The builder for an instance of <code>InitializationState</code> */
    public static class Builder {

        private final int totalNumberOfContexts;
        private boolean serverRegistered;
        private boolean filestoreRegistered;
        private boolean databaseRegistered;
        private int numberOfCreatedContexts;
        private boolean finished;
        private String stateMessage;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder(int totalNumberOfContexts) {
            super();
            this.totalNumberOfContexts = totalNumberOfContexts;
            serverRegistered = false;
            filestoreRegistered = false;
            databaseRegistered = false;
            numberOfCreatedContexts = 0;
            finished = false;
            stateMessage = "Not yet initialized. Still ongoing...";
        }

        Builder(InitializationState source) {
            super();
            this.totalNumberOfContexts = source.getTotalNumberOfContexts();
            serverRegistered = source.isServerRegistered();
            filestoreRegistered = source.isFilestoreRegistered();
            databaseRegistered = source.isDatabaseRegistered();
            numberOfCreatedContexts = source.getNumberOfCreatedContexts();
            finished = source.isFinished();
            stateMessage = source.getStateMessage();
        }

        /**
         * Sets the state message.
         *
         * @param stateMessage The message to set
         * @return This builder
         */
        public Builder withStateMessage(String stateMessage) {
            this.stateMessage = stateMessage;
            return this;
        }

        /**
         * Sets the serverRegistered
         *
         * @param serverRegistered The serverRegistered to set
         * @return This builder
         */
        public Builder withServerRegistered(boolean serverRegistered) {
            this.serverRegistered = serverRegistered;
            return this;
        }

        /**
         * Sets the filestoreRegistered
         *
         * @param filestoreRegistered The filestoreRegistered to set
         * @return This builder
         */
        public Builder withFilestoreRegistered(boolean filestoreRegistered) {
            this.filestoreRegistered = filestoreRegistered;
            return this;
        }

        /**
         * Sets the databaseRegistered
         *
         * @param databaseRegistered The databaseRegistered to set
         * @return This builder
         */
        public Builder withDatabaseRegistered(boolean databaseRegistered) {
            this.databaseRegistered = databaseRegistered;
            return this;
        }

        /**
         * Sets the numberOfCreatedContexts
         *
         * @param numberOfCreatedContexts The numberOfCreatedContexts to set
         * @return This builder
         */
        public Builder withNumberOfCreatedContexts(int numberOfCreatedContexts) {
            this.numberOfCreatedContexts = numberOfCreatedContexts;
            return this;
        }

        /**
         * Sets if demo system initialization is completed or not.
         *
         * @param finished The flag to set
         * @return This builder
         */
        public Builder withFinished(boolean finished) {
            this.finished = finished;
            return this;
        }

        /**
         * Builds the instance of <b>InitializationState</b> from this builder's arguments.
         *
         * @return The instance of <b>InitializationState</b>
         */
        public InitializationState build() {
            return new InitializationState(serverRegistered, filestoreRegistered, databaseRegistered, numberOfCreatedContexts, totalNumberOfContexts, finished, stateMessage);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final int totalNumberOfContexts;
    private final boolean serverRegistered;
    private final boolean filestoreRegistered;
    private final boolean databaseRegistered;
    private final int numberOfCreatedContexts;
    private final boolean finished;
    private final String stateMessage;

    InitializationState(boolean serverRegistered, boolean filestoreRegistered, boolean databaseRegistered, int numberOfCreatedContexts, int totalNumberOfContexts, boolean finished, String stateMessage) {
        super();
        this.serverRegistered = serverRegistered;
        this.filestoreRegistered = filestoreRegistered;
        this.databaseRegistered = databaseRegistered;
        this.numberOfCreatedContexts = numberOfCreatedContexts;
        this.totalNumberOfContexts = totalNumberOfContexts;
        this.finished = finished;
        this.stateMessage = stateMessage;
    }

    /**
     * Gets the state message.
     *
     * @return The state message
     */
    public String getStateMessage() {
        return stateMessage;
    }

    /**
     * Checks if demo system initialization is completed.
     *
     * @return <code>true</code> if completed/finished; otherwise <code>false</code>
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * Gets the serverRegistered
     *
     * @return The serverRegistered
     */
    public boolean isServerRegistered() {
        return serverRegistered;
    }

    /**
     * Gets the filestoreRegistered
     *
     * @return The filestoreRegistered
     */
    public boolean isFilestoreRegistered() {
        return filestoreRegistered;
    }

    /**
     * Gets the databaseRegistered
     *
     * @return The databaseRegistered
     */
    public boolean isDatabaseRegistered() {
        return databaseRegistered;
    }

    /**
     * Gets the numberOfCreatedContexts
     *
     * @return The numberOfCreatedContexts
     */
    public int getNumberOfCreatedContexts() {
        return numberOfCreatedContexts;
    }

    /**
     * Gets the totalNumberOfContexts
     *
     * @return The totalNumberOfContexts
     */
    public int getTotalNumberOfContexts() {
        return totalNumberOfContexts;
    }

}
