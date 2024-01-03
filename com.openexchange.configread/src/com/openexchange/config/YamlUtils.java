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

package com.openexchange.config;

import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.Autoboxing.l;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import com.openexchange.java.Enums;
import com.openexchange.tools.strings.TimeSpanParser;

/**
 * {@link YamlUtils}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
*/
public class YamlUtils {
    
    /**
     * Casts the supplied object into a generic map.
     * 
     * @param yaml The parsed YAML structure
     * @return The casted map, or <code>null</code> if the supplied argument was <code>null</code>
     */
    public static Map<String, Object> asMap(Object yaml) {
        if (null == yaml) {
            return null;
        }
        @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) yaml;
        return map;
    }

    /**
     * Parses a YAML file using the configuration service.
     * 
     * @param configurationService A reference to the configuration service
     * @param fileName The name of the YAML file to read
     * @return The parsed data from the YAML file, or an empty map if no such file exists
     * @throws IllegalStateException if parsing fails
     * @throws ClassCastException if the parsed data is not null and is not assignable to the type <code>Map<String, Object></code>
     */
    public static Map<String, Object> readYaml(ConfigurationService configurationService, String fileName) {
        Object yaml = configurationService.getYaml(fileName);
        return null == yaml ? Collections.emptyMap() : asMap(yaml);
    }

    /**
     * Gets the string value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as string
     * @throws IllegalArgumentException if there is no such mapped value found under the given key
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>String</code>
     */
    public static String get(Map<String, Object> map, String key) {
        String value = opt(map, key, String.class, null);
        if (null == value) {
            throw new IllegalArgumentException(key);
        }
        return value;
    }

    /**
     * Parses a textual timespan value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The parsed timespan in milliseconds
     * @throws IllegalArgumentException if there is no such mapped value found under the given key, or if the value cannot be parsed
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>String</code>
     */
    public static long getTimeSpan(Map<String, Object> map, String key) {
        String value = opt(map, key, String.class, null);
        if (null == value) {
            throw new IllegalArgumentException(key);
        }
        return TimeSpanParser.parseTimespanToPrimitive(value);
    }

    /**
     * Gets an generic array list for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as arraylist
     * @throws IllegalArgumentException if there is no such mapped value found under the given key
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>ArrayList</code>
     */
    public static ArrayList<?> getArray(Map<String, Object> map, String key) {
        ArrayList<?> value = opt(map, key, ArrayList.class, null);
        if (null == value) {
            throw new IllegalArgumentException(key);
        }
        return value;
    }

    /**
     * Gets a nested map value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the nested map value is mapped to
     * @return The nested map
     * @throws IllegalArgumentException if there is no such mapped value found under the given key
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>Map<String, Object></code>
     */
    public static Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (null == value) {
            throw new IllegalArgumentException(key);
        }
        @SuppressWarnings("unchecked") Map<String, Object> casted = (Map<String, Object>) value;
        return casted;
    }

    /**
     * Optionally gets a nested map value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the nested map value is mapped to
     * @return The nested map, or <code>null</code> if not set
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>Map<String, Object></code>
     */
    public static Map<String, Object> optMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (null == value) {
            return null;
        }
        @SuppressWarnings("unchecked") Map<String, Object> casted = (Map<String, Object>) value;
        return casted;
    }

    /**
     * Gets an integer value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as integer
     * @throws IllegalArgumentException if there is no such mapped value found under the given key
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>Integer</code>
     */
    public static int getInt(Map<String, Object> map, String key) {
        Integer value = opt(map, key, Integer.class, null);
        if (null == value) {
            throw new IllegalArgumentException(key);
        }
        return i(value);
    }

    /**
     * Optionally parses a textual timespan value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The parsed timespan in milliseconds, or the given default value if not set
     * @throws IllegalArgumentException if there is no such mapped value found under the given key, or if the value cannot be parsed
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>String</code>
     */
    public static long optTimeSpan(Map<String, Object> map, String key, long defaultValue) {
        String value = opt(map, key, String.class, null);
        return value == null ? defaultValue : TimeSpanParser.parseTimespanToPrimitive(value);
    }

    /**
     * Optionally gets the integer value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as int, or the given default value if not set
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>Integer</code>
     */
    public static int optInt(Map<String, Object> map, String key, int defaultValue) {
        Integer value = opt(map, key, Integer.class, null);
        return null == value ? defaultValue : i(value);
    }

    /**
     * Optionally gets the long value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as long, or the given default value if not set
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>Long</code>
     */
    public static long optLong(Map<String, Object> map, String key, long defaultValue) {
        Long value = opt(map, key, Long.class, null);
        return null == value ? defaultValue : l(value);
    }

    /**
     * Gets a boolean value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as boolean
     * @throws IllegalArgumentException if there is no such mapped value found under the given key
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>Boolean</code>
     */
    public static boolean optBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Boolean value = opt(map, key, Boolean.class, null);
        return null == value ? defaultValue : b(value);
    }

    /**
     * Optionally gets the boolean value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as boolean, or the given default value if not set
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>Boolean</code>
     */
    public static boolean getBoolean(Map<String, Object> map, String key) {
        Boolean value = opt(map, key, Boolean.class, null);
        if (null == value) {
            throw new IllegalArgumentException(key);
        }
        return b(value);
    }

    /**
     * Optionally gets the parsed enum value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @param enumeration The enumeration class from which to return a constant
     * @param defaultValue The default value to return as fallback
     * @return The value as enum, or the given default value if not set
     * @throws IllegalArgumentException if there's no suitable enum constant for the parsed name
     */
    public static <E extends Enum<E>> E optEnum(Map<String, Object> map, String key, Class<E> enumeration, E defaultValue) {
        String value = opt(map, key);
        if (null == value) {
            return defaultValue;
        }
        return Enums.parse(enumeration, value);
    }

    /**
     * Gets the parsed enum value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @param enumeration The enumeration class from which to return a constant
     * @return The value as enum
     * @throws IllegalArgumentException if there's no suitable enum constant for the parsed name
     */
    public static <E extends Enum<E>> E getEnum(Map<String, Object> map, String key, Class<E> enumeration) {
        return Enums.parse(enumeration, get(map, key));
    }

    /**
     * Optionally gets the string value for a certain key from the supplied map.
     * 
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @return The value as string, or <code>null</code> if not set
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>String</code>
     */
    public static String opt(Map<String, Object> map, String key) {
        return opt(map, key, String.class, null);
    }

    /**
     * Optionally gets the casted value for a certain key from the supplied map.
     * 
     * @param <T> The value's type
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @param clazz The class to cast the value into
     * @return The value, or <code>null</code> if not set
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>T</code>
     */
    public static <T> T opt(Map<String, Object> map, String key, Class<T> clazz) {
        return opt(map, key, clazz, null);
    }

    /**
     * Optionally gets the casted value for a certain key from the supplied map.
     * 
     * @param <T> The value's type
     * @param map The map to get the value from
     * @param key The key the value is mapped to
     * @param clazz The class to cast the value into
     * @param defaultValue The default value to return as fallback
     * @return The value, or the given default value if not set
     * @throws ClassCastException if the mapped object is not null and is not assignable to the type <code>T</code>
     */
    public static <T> T opt(Map<String, Object> map, String key, Class<T> clazz, T defaultValue) {
        Object value = map.get(key);
        return null == value ? defaultValue : clazz.cast(value);
    }

}
