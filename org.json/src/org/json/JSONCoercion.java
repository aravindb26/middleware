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

package org.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link JSONCoercion} - Turns JSON data to its Java representation and vice versa.
 * <p>
 * A {@link JSONObject} is coerced to a {@link Map}, a {@link JSONArray} is coerced to a {@link Collection}.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class JSONCoercion {

    /**
     * Initializes a new {@link JSONCoercion}.
     */
    private JSONCoercion() {
        super();
    }

    /**
     * Coerces given JSON data to its Java representation.
     *
     * @param object The JSON data to coerce
     * @return The resulting Java representation.
     * @throws JSONException If coercion fails
     */
    public static Object coerceToNative(final JSONValue object) throws JSONException {
        if (null == object) {
            return null;
        }
        if (object.isArray()) {
            final JSONArray jsonArray = object.toArray();
            final int length = jsonArray.length();
            final List<Object> list = new ArrayList<Object>(length);
            for (int i = 0; i < length; i++) {
                list.add(coerceToNative(jsonArray.get(i)));
            }
            return list;
        }
        if (object.isObject()) {
            final JSONObject jsonObject = object.toObject();
            final Map<String, Object> map = new LinkedHashMap<String, Object>(jsonObject.length());
            for (final String key : jsonObject.keySet()) {
                map.put(key, coerceToNative(jsonObject.get(key)));
            }
            return map;
        }
        return object;
    }

    /**
     * Parses given string to its JSON representation (JSON object, JSON array, Number, Boolean or String) and returns its native object.
     * <p>
     * <ul>
     * <li><code>'{"name": "Hans", ...}'</code> would return a {@link Map}
     * <li><code>'[12, 13, ...]'</code> would return a {@link List}
     * <li><code>'13'</code> would return an integer value
     * <li><code>'true'</code> would return a boolean value
     * <li><code>'null'</code> would return {@link JSONObject#NULL}
     * <li>Otherwise considered as {@link String}
     * </ul>
     *
     * @param s The string to parse
     * @return The native object
     */
    public static Object parseAndCoerceToNative(String s) {
        try {
            return new JSONObject(new StringBuilder(s.length() + 6).append("{\"v\":").append(s).append('}').toString()).asMap().get("v");
        } catch (@SuppressWarnings("unused") JSONException e) {
            return s;
        }
    }

    /**
     * Coerces given JSON data to its Java representation.
     *
     * @param object The JSON data to coerce
     * @return The resulting Java representation.
     * @throws JSONException If coercion fails
     */
    public static Object coerceToNative(final Object object) throws JSONException {
        if (object instanceof JSONArray) {
            JSONArray jsonArray = (JSONArray) object;
            int length = jsonArray.length();
            List<Object> list = new ArrayList<Object>(length);
            for (int i = 0; i < length; i++) {
                list.add(coerceToNative(jsonArray.get(i)));
            }
            return list;
        }

        if (object instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) object;
            Map<String, Object> map = new LinkedHashMap<String, Object>(jsonObject.length());
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                map.put(entry.getKey(), coerceToNative(entry.getValue()));
            }
            return map;
        }

        if (JSONObject.NULL == object) {
            return null;
        }

        return object;
    }

    /**
     * Checks if specified object needs to be coerced to JSON.
     *
     * @param value The object to check
     * @return <code>true</code> if specified object needs to be coerced to JSON; otherwise <code>false</code>
     */
    public static boolean needsJSONCoercion(final Object value) {
        return (value instanceof Map) || (value instanceof Collection) || isArray(value);
    }

    /**
     * Checks if specified object needs to be coerced to a native Java object.
     *
     * @param value The object to check
     * @return <code>true</code> if specified object needs to be coerced to a native Java object; otherwise <code>false</code>
     */
    public static boolean needsNativeCoercion(final Object value) {
        return (value instanceof JSONValue);
    }

    /**
     * Coerces given Java object to its JSON representation.
     *
     * @param value The Java object to coerce
     * @return The resulting JSON representation
     * @throws JSONException If coercion fails
     */
    public static Object coerceToJSON(final Object value) throws JSONException {
        if (null == value || JSONObject.NULL.equals(value)) {
            return JSONObject.NULL;
        }

        if (value instanceof JSONValue) {
            return value;
        }

        if (value instanceof Map) {
            try {
                @SuppressWarnings("unchecked") final Map<String, ?> map = (Map<String, ?>) value;
                final JSONObject jsonObject = new JSONObject(map.size());
                for (final Map.Entry<String, ?> entry : map.entrySet()) {
                    jsonObject.put(entry.getKey(), coerceToJSON(entry.getValue()));
                }
                return jsonObject;
            } catch (ClassCastException e) {
                throw new JSONException("Value cannot be coerced to JSON,", e);
            }
        }

        if (value instanceof Collection) {
            final Collection<?> collection = (Collection<?>) value;
            final JSONArray jsonArray = new JSONArray(collection.size());
            for (final Object object : collection) {
                jsonArray.put(coerceToJSON(object));
            }
            return jsonArray;
        }

        if (isArray(value)) {
            final int length = Array.getLength(value);
            final JSONArray jsonArray = new JSONArray(length);
            for (int i = 0; i < length; i++) {
                final Object object = Array.get(value, i);
                jsonArray.put(coerceToJSON(object));
            }
            return jsonArray;
        }

        if (value instanceof String) {
            return value.toString();
        }
        if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof AtomicInteger) {
                return Integer.valueOf(((AtomicInteger) n).get());
            } else if (n instanceof AtomicLong) {
                return Long.valueOf(((AtomicLong) n).get());
            } else {
                return n;
            }
        } else if (value instanceof byte[]) {
            return (value);
        } else if (value instanceof Boolean) {
            return (value);
        } else if (value instanceof AtomicBoolean) {
            return Boolean.valueOf(((AtomicBoolean) value).get());
        }

        // As string as last resort
        return value.toString();
    }

    /**
     * Checks if specified object is an array.
     *
     * @param object The object to check
     * @return <code>true</code> if specified object is an array; otherwise <code>false</code>
     */
    public static boolean isArray(final Object object) {
        /*-
         * getClass().isArray() is significantly slower on Sun Java 5 or 6 JRE than on IBM.
         * So much that using clazz.getName().charAt(0) == '[' is faster on Sun JVM.
         */
        // return (null != object && object.getClass().isArray());
        return (null != object && '[' == object.getClass().getName().charAt(0));
    }

}
