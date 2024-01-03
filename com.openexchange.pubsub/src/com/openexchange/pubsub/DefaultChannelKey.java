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

package com.openexchange.pubsub;

/**
 * {@link DefaultChannelKey} - The default cache key implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class DefaultChannelKey implements ChannelKey {

    private static final long serialVersionUID = -6756658532265285785L;

    /**
     * Creates builder to build {@link DefaultChannelKey}.
     *
     * @param delimiter The delimiter character
     * @return created builder
     */
    public static Builder builder(char delimiter) {
        return new Builder(delimiter);
    }

    /**
     * Builder to build {@link DefaultChannelKey}.
     */
    public static final class Builder {

        private final char delimiter;
        private ChannelApplicationName applicationName;
        private ChannelName channelName;

        /**
         * Initializes a new {@link Builder}.
         *
         * @param delimiter The delimiter character
         */
        Builder(char delimiter) {
            super();
            this.delimiter = delimiter;
        }

        /**
         * Sets given application name.
         *
         * @param applicationName The application name to set
         * @return This builder
         */
        public Builder withChannelApplicationName(ChannelApplicationName applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        /**
         * Sets given module name.
         *
         * @param channelName The channel name to set
         * @return This builder
         */
        public Builder withChannelName(ChannelName channelName) {
            this.channelName = channelName;
            return this;
        }

        /**
         * Builds the resulting instance of <code>RedisCacheKey</code> from this builder.
         *
         * @return The resulting instance of <code>RedisCacheKey</code>
         */
        public DefaultChannelKey build() {
            if (applicationName == null) {
                throw new IllegalArgumentException("Application name must not be null");
            }
            return new DefaultChannelKey(delimiter, applicationName, channelName);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final char delimiter;
    private final ChannelApplicationName applicationKey;
    private final ChannelName channelName;
    private final String fqn;

    private final int hashCode;

    /**
     * Initializes a new {@link DefaultChannelKey}.
     *
     * @param delimiter The delimiter character
     * @param applicationKey The application key
     * @param channelName The module key
     * @param suffix The suffix
     */
    DefaultChannelKey(char delimiter, ChannelApplicationName applicationKey, ChannelName channelName) {
        super();
        this.delimiter = delimiter;
        this.applicationKey = applicationKey;
        this.channelName = channelName;

        StringBuilder sb = new StringBuilder(64);
        sb.append(applicationKey.getName());
        if (channelName != null) {
            sb.append(delimiter).append(channelName.getName());
        }
        this.fqn = sb.toString();

        final int prime = 31;
        int result = 1;
        result = prime * result + ((fqn == null) ? 0 : fqn.hashCode());
        hashCode = result;
    }

    @Override
    public String getFQN() {
        return fqn;
    }

    @Override
    public ChannelApplicationName getApp() {
        return applicationKey;
    }

    @Override
    public ChannelName getChannel() {
        return channelName;
    }

    @Override
    public char getDelimiter() {
        return delimiter;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof ChannelKey) {
            return false;
        }
        ChannelKey other = (ChannelKey) obj;
        if (fqn == null) {
            if (other.getFQN() != null) {
                return false;
            }
        } else if (!fqn.equals(other.getFQN())) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getFQN();
    }

}
