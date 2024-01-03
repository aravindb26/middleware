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


package com.openexchange.pns;

import java.util.Objects;
import java.util.Optional;
import com.openexchange.java.Strings;

/**
 * {@link DefaultToken} - The default token implementation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class DefaultToken implements Token, Comparable<Token> {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>DefaultToken</code> */
    public static class Builder {

        private String value;
        private Meta meta;

        /**
         * Initializes a new builder.
         */
        Builder() {
            super();
        }

        /**
         * Sets the token value.
         *
         * @param token The token value to set
         * @return This builder
         */
        public Builder withValue(String token) {
            this.value = token;
            return this;
        }

        /**
         * Sets the meta.
         *
         * @param meta The meta to set
         * @return This builder
         */
        public Builder withMeta(Meta meta) {
            this.meta = meta;
            return this;
        }

        /**
         * Creates the instance of <code>DefaultToken</code> from this builder's arguments.
         *
         * @return The instance of <code>DefaultToken</code>
         */
        public DefaultToken build() {
            return new DefaultToken(value, meta);
        }
    }

    /**
     * Creates a new token for specified value.
     *
     * @param value The token value to set
     * @return The newly created token only carrying given value and no meta information
     */
    public static DefaultToken tokenFor(String value) {
        return new DefaultToken(value, null);
    }

    // ------------------------------------------------------------------------------------------------------------------------------------

    private final String value;
    private final Optional<Meta> optionalMeta;
    private int hash;

    /**
     * Initializes a new {@link DefaultToken}.
     *
     * @param token The token
     * @param meta The meta information or <code>null</code>
     */
    DefaultToken(String token, Meta meta) {
        super();
        this.value = token;
        this.optionalMeta = Optional.ofNullable(meta);
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Optional<Meta> getOptionalMeta() {
        return optionalMeta;
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            // Not yet computed
            result = 31 * 1 + (null == value ? 0 : value.hashCode());
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Token)) {
            return false;
        }
        return Objects.equals(value, ((Token) obj).getValue());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append("value=").append(value);
        if (optionalMeta.isPresent()) {
            sb.append(", meta=").append(optionalMeta.get());
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public int compareTo(Token o) {
        return Strings.compare(value, o.getValue());
    }

}
