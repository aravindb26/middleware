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

package com.openexchange.redis;

/**
 * {@link DefaultRedisOperationKey}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class DefaultRedisOperationKey implements RedisOperationKey<DefaultRedisOperationKey> {

    /**
     * Creates a new builder.
     *
     * @return The new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder for an instance of <code>DefaultRedisOperationKey</code>.
     */
    public static class Builder {

        private RedisCommand command;
        private String hash;
        private int userId;
        private int contextId;

        /**
         * Initializes a new {@link Builder}.
         */
        Builder() {
            super();
        }

        /**
         * Sets the Redis command; e.g. <code>"EXISTS"</code>.
         *
         * @param command The command to set
         * @return This builder
         */
        public Builder withCommand(RedisCommand command) {
            this.command = command;
            return this;
        }

        /**
         * Sets the hash as arbitrary salt for this key.
         *
         * @param hash The hash to set
         * @return This builder
         */
        public Builder withHash(String hash) {
            this.hash = hash;
            return this;
        }

        /**
         * Sets the user identifier.
         *
         * @param userId The user identifier to set
         * @return This builder
         */
        public Builder withUserId(int userId) {
            this.userId = userId;
            return this;
        }

        /**
         * Sets the context identifier.
         *
         * @param contextId The context identifier to set
         * @return This builder
         */
        public Builder withContextId(int contextId) {
            this.contextId = contextId;
            return this;
        }

        /**
         * Creates the resulting instance of <code>DefaultRedisOperationKey</code> from this builder's arguments.
         *
         * @return The resulting instance of <code>DefaultRedisOperationKey</code>
         */
        public DefaultRedisOperationKey build() {
            return new DefaultRedisOperationKey(command, hash, userId, contextId);
        }
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final RedisCommand command;
    private final String hash;
    private final int userId;
    private final int contextId;
    private final int hashc;

    /**
     * Initializes a new {@link DefaultRedisOperationKey}.
     *
     * @param command The Redis command
     * @param hash The hash
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    DefaultRedisOperationKey(RedisCommand command, String hash, int userId, int contextId) {
        super();
        this.command = command;
        this.hash = hash;
        this.userId = userId;
        this.contextId = contextId;

        int prime = 31;
        int result = prime * 1 + contextId;
        result = prime * result + userId;
        result = prime * result + (null == command ? 0 : command.hashCode());
        result = prime * result + (null == hash ? 0 : hash.hashCode());
        hashc = result;
    }

    /**
     * Gets the command.
     *
     * @return The command
     */
    public RedisCommand getCommand() {
        return command;
    }

    /**
     * Gets the hash.
     *
     * @return The hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * Gets the user identifier.
     *
     * @return The user identifier
     */
    public int getUserId() {
        return userId;
    }

    /**
     * Gets the context identifier.
     *
     * @return The context identifier
     */
    public int getContextId() {
        return contextId;
    }

    @Override
    public int hashCode() {
        return hashc;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        DefaultRedisOperationKey other = (DefaultRedisOperationKey) obj;
        if (contextId != other.contextId) {
            return false;
        }
        if (userId != other.userId) {
            return false;
        }
        if (command != other.command) {
            return false;
        }
        if (hash == null) {
            if (other.hash != null) {
                return false;
            }
        } else if (!hash.equals(other.hash)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(DefaultRedisOperationKey o) {
        int c = Integer.compare(contextId, o.contextId);
        if (c == 0) {
            c = Integer.compare(userId, o.userId);
        }
        if (c == 0) {
            c = command.compareTo(o.command);
        }
        if (c == 0) {
            c = hash.compareTo(o.hash);
        }
        return c;
    }

}
