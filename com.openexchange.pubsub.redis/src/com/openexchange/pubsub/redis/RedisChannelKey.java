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

package com.openexchange.pubsub.redis;

import com.openexchange.pubsub.ChannelApplicationName;
import com.openexchange.pubsub.ChannelKey;
import com.openexchange.pubsub.ChannelName;

/**
 * {@link RedisChannelKey}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class RedisChannelKey implements ChannelKey {

    private static final long serialVersionUID = -4156658532265285785L;

    /** The delimiter character */
    public static final char DELIMITER = ':';

    /**
     * Creates builder to build {@link RedisChannelKey}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link RedisChannelKey}.
     */
    public static final class Builder {

        private ChannelApplicationName applicationName;
        private ChannelName channelName;

        /**
         * Initializes a new {@link Builder}.
         */
        private Builder() {
            super();
        }

        /**
         * Sets given application name.
         *
         * @param applicationName The application name to set
         * @return This builder
         */
        public Builder withApplicationName(ChannelApplicationName applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        /**
         * Sets given channel name.
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
        public RedisChannelKey build() {
            if (applicationName == null) {
                throw new IllegalArgumentException("Application name must not be null");
            }
            if (channelName == null) {
                throw new IllegalArgumentException("Channel name must not be null");
            }
            return new RedisChannelKey(applicationName, channelName);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final ChannelApplicationName applicationName;
    private final ChannelName channelName;
    private final String fqn;

    private final int hashCode;

    /**
     * Initializes a new {@link RedisChannelKey}.
     *
     * @param applicationName The application name
     * @param channelName The channel name
     */
    RedisChannelKey(ChannelApplicationName applicationName, ChannelName channelName) {
        super();
        this.applicationName = applicationName;
        this.channelName = channelName;

        StringBuilder sb = new StringBuilder(64);
        sb.append(applicationName.getName()).append(DELIMITER);
        sb.append(channelName.getName());
        fqn = sb.toString();

        int prime = 31;
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
        return applicationName;
    }

    @Override
    public ChannelName getChannel() {
        return channelName;
    }

    @Override
    public char getDelimiter() {
        return DELIMITER;
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
