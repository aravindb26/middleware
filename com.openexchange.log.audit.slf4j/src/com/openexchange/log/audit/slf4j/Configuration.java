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

package com.openexchange.log.audit.slf4j;

/**
 * {@link Configuration}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.2
 */
public class Configuration {

    /** The configuration builder */
    public static class Builder {

        private boolean enabled;
        private boolean includeAttributeNames;
        private Slf4jLogLevel level;
        private DateFormatter dateFormatter;
        private String delimiter;

        /**
         * Initializes a new {@link Builder}.
         */
        public Builder() {
            super();
            enabled = false;
            level = Slf4jLogLevel.INFO;
            dateFormatter = ISO8601DateFormatter.getInstance();
            includeAttributeNames = true;
        }

        /**
         * Sets the enabled flag
         *
         * @param enabled <code>true</code> to enable; otherwise <code>false</code>
         * @return This builder instance
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Sets the whether to include attribute names
         *
         * @param includeAttributeNames <code>true</code> to include; otherwise <code>false</code>
         * @return This builder instance
         */
        public Builder includeAttributeNames(boolean includeAttributeNames) {
            this.includeAttributeNames = includeAttributeNames;
            return this;
        }

        /**
         * Sets the level
         *
         * @param level The level to set
         * @return This builder instance
         */
        public Builder level(Slf4jLogLevel level) {
            this.level = level;
            return this;
        }

        /**
         * Sets the date formatter
         *
         * @param dateFormatter The date formatter to set
         * @return This builder instance
         */
        public Builder dateFormatter(DateFormatter dateFormatter) {
            this.dateFormatter = dateFormatter;
            return this;
        }


        /**
         * Sets the delimiter
         *
         * @param delimiter The delimiter to set
         * @return This builder instance
         */
        public Builder delimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * Builds the configuration carrying this builder's attributes.
         *
         * @return The configuration
         */
        public Configuration build() {
            return new Configuration(enabled, includeAttributeNames, level, dateFormatter, delimiter);
        }

    }

    // ------------------------------------------------------------------------------------------------------------------------------

    private final boolean enabled;
    private final boolean includeAttributeNames;
    private final Slf4jLogLevel level;
    private final DateFormatter dateFormatter;
    private final String delimiter;

    /**
     * Initializes a new {@link Configuration}.
     *
     * @param enabled <code>true</code> to enable audit logging, <code>false</code> otherwise
     * @param includeAttributeNames <code>true</code> to include attribute names in log message, <code>false</code> otherwise
     * @param level Specifies the log level to use for audit log messages
     * @param dateFormatter Specifies the optional date pattern to use
     * @param delimiter Specifies the delimiter between attributes to use
     */
    Configuration(boolean enabled, boolean includeAttributeNames, Slf4jLogLevel level, DateFormatter dateFormatter, String delimiter) {
        super();
        this.enabled = enabled;
        this.includeAttributeNames = includeAttributeNames;
        this.level = level;
        this.dateFormatter = dateFormatter;
        this.delimiter = delimiter;
    }

    /**
     * Gets the enabled flag
     *
     * @return <code>true</code> if enabled; otherwise <code>false</code>
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks whether to include attribute names
     *
     * @return <code>true</code> to include; otherwise <code>false</code>
     */
    public boolean isIncludeAttributeNames() {
        return includeAttributeNames;
    }

    /**
     * Gets the level
     *
     * @return The level
     */
    public Slf4jLogLevel getLevel() {
        return level;
    }

    /**
     * Gets the date formatter
     *
     * @return The date formatter
     */
    public DateFormatter getDateFormatter() {
        return dateFormatter;
    }

    /**
     * Gets the attribute delimiter.
     *
     * @return The attribute delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }

}
