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

import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.L;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * {@link Meta} - Meta information for a registered token.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Meta {

    /**
     * Creates a new builder.
     *
     * @return The newly created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder for an instance of <code>Meta</code> */
    public static final class Builder {

        private final Map<String, Object> properties;

        /**
         * Initializes a new {@link Builder}.
         */
        private Builder() {
            super();
            properties = new LinkedHashMap<>();
        }

        /**
         * Adds the given property to this builder.
         *
         * @param name The property name
         * @param value The property value
         * @return This builder
         */
        public Builder withProperty(String name, Object value) {
            if (name == null) {
                throw new IllegalArgumentException("Name must not be null");
            }
            if (value == null) {
                return this;
            }
            if (value instanceof Boolean) {
                properties.put(name, value);
                return this;
            }
            if (value instanceof Long) {
                properties.put(name, value);
                return this;
            }
            if (value instanceof Integer) {
                properties.put(name, value);
                return this;
            }
            if (value instanceof String) {
                properties.put(name, value);
                return this;
            }
            if (value instanceof Meta) {
                properties.put(name, value);
                return this;
            }
            throw new IllegalArgumentException("Illegal value type: " + value.getClass().getName());
        }

        /**
         * Adds the given text property to this builder.
         *
         * @param name The property name
         * @param value The property value
         * @return This builder
         */
        public Builder withProperty(String name, String value) {
            if (name == null) {
                throw new IllegalArgumentException("Name must not be null");
            }
            if (value != null) {
                properties.put(name, value);
            }
            return this;
        }

        /**
         * Adds the given numeric property to this builder.
         *
         * @param name The property name
         * @param value The property value
         * @return This builder
         */
        public Builder withProperty(String name, long value) {
            if (name == null) {
                throw new IllegalArgumentException("Name must not be null");
            }
            properties.put(name, L(value));
            return this;
        }

        /**
         * Adds the given numeric property to this builder.
         *
         * @param name The property name
         * @param value The property value
         * @return This builder
         */
        public Builder withProperty(String name, int value) {
            if (name == null) {
                throw new IllegalArgumentException("Name must not be null");
            }
            properties.put(name, Integer.valueOf(value));
            return this;
        }

        /**
         * Adds the given boolean property to this builder.
         *
         * @param name The property name
         * @param value The property value
         * @return This builder
         */
        public Builder withProperty(String name, boolean value) {
            if (name == null) {
                throw new IllegalArgumentException("Name must not be null");
            }
            properties.put(name, B(value));
            return this;
        }

        /**
         * Adds the given meta property to this builder.
         *
         * @param name The property name
         * @param value The property value
         * @return This builder
         */
        public Builder withProperty(String name, Meta value) {
            if (name == null) {
                throw new IllegalArgumentException("Name must not be null");
            }
            if (value == null) {
                properties.remove(name);
            } else {
                properties.put(name, value);
            }
            return this;
        }

        /**
         * Builds the instance of <code>Meta</code> from this builder's arguments.
         *
         * @return The instance of <code>Meta</code>
         */
        public Meta build() {
            return new Meta(properties);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final Map<String, Object> properties;

    /**
     * Initializes a new {@link Meta}.
     *
     * @param properties The underlying properties
     */
    private Meta(Map<String, Object> properties) {
        super();
        this.properties = Map.copyOf(properties);
    }

    /**
     * Gets the number of properties held by this meta instance.
     *
     * @return The number of properties
     */
    public int size() {
        return properties.size();
    }

    /**
     * Checks if this meta instance is empty.
     *
     * @return <code>true</code> if empty; otherwise <code>false</code>
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    /**
     * Checks if this meta instance contains a property with given name.
     *
     * @param name The name of the property to check
     * @return <code>true</code> if contained; otherwise <code>false</code>
     */
    public boolean contains(String name) {
        checkName(name);
        return properties.containsKey(name);
    }

    /**
     * Gets the value for given property name.
     *
     * @param name The name of the property
     * @return The value (of either type <code>String</code>, <code>Long</code>, <code>Boolean</code> or <code>Meta</code>) or <code>null</code>
     */
    public Object get(String name) {
        checkName(name);
        return properties.get(name);
    }

    /**
     * Gets the <code>String</code> value for given property name.
     *
     * @param name The name of the property
     * @param def The default value to return if there is no such property or value is not of type <code>String</code>
     * @return The <code>String</code> value
     */
    public String getString(String name, String def) {
        checkName(name);
        Object object = properties.get(name);
        return object instanceof String ? (String) object : def;
    }

    /**
     * Gets the <code>long</code> value for given property name.
     *
     * @param name The name of the property
     * @param def The default value to return if there is no such property or value is not of type <code>long</code>
     * @return The <code>long</code> value
     */
    public long getLong(String name, long def) {
        checkName(name);
        Object object = properties.get(name);
        return object instanceof Long ? ((Long) object).longValue() : def;
    }

    /**
     * Gets the <code>int</code> value for given property name.
     *
     * @param name The name of the property
     * @param def The default value to return if there is no such property or value is not of type <code>int</code>
     * @return The <code>int</code> value
     */
    public int getInt(String name, int def) {
        checkName(name);
        Object object = properties.get(name);
        return object instanceof Number ? ((Number) object).intValue() : def;
    }

    /**
     * Gets the <code>boolean</code> value for given property name.
     *
     * @param name The name of the property
     * @param def The default value to return if there is no such property or value is not of type <code>boolean</code>
     * @return The <code>boolean</code> value
     */
    public boolean getBoolean(String name, boolean def) {
        checkName(name);
        Object object = properties.get(name);
        return object instanceof Boolean ? ((Boolean) object).booleanValue() : def;
    }

    /**
     * Gets a set view of the properties contained in this meta instance.
     * <p>
     * The values are of either type <code>String</code>, <code>Long</code>, <code>Boolean</code> or <code>Meta</code>.
     *
     * @return A set view of the properties
     */
    public Set<Entry<String, Object>> entrySet() {
        return properties.entrySet();
    }

    @Override
    public String toString() {
        return properties.toString();
    }

    /**
     * Checks given name to be not <code>null</code>.
     *
     * @param name The name to check
     * @throws IllegalArgumentException If name is <code>null</code>
     */
    private static void checkName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null");
        }
    }

}
