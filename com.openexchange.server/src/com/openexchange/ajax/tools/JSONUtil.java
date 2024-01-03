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

package com.openexchange.ajax.tools;

import java.util.Iterator;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.openexchange.ajax.fields.ResponseFields;
import com.openexchange.exception.OXException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link JSONUtil} - Provides JSON utility methods.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class JSONUtil {

    /**
     * Initializes a new {@link JSONUtil}.
     */
    private JSONUtil() {
        super();
    }

    /**
     * Creates a {@link JSONObject} containing the merged view on given JSON objects performing a <i>"right merge"</i>,<br>
     * that is root element of latter arguments overwrite the ones from <tt>jObject1</tt>.
     *
     * @param jObject1 The first JSON object
     * @param jObjects The other JSON objects to merge with
     * @return The merged JSON object
     * @throws JSONException If composing merged JSON object fails for any reason
     */
    public static JSONObject rightMerge(final JSONObject jObject1, final JSONObject... jObjects) throws JSONException {
        if (null == jObject1 || (null == jObjects) || (0 == jObjects.length)) {
            return jObject1;
        }
        // Iterate others
        for (final JSONObject obj : jObjects) {
            if (null != obj) {
                for (final Entry<String,Object> entry : obj.entrySet()) {
                    jObject1.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return jObject1;
    }

    /**
     * Creates a {@link JSONObject} containing the merged view on given JSON objects.
     *
     * @param jObject1 The first JSON object
     * @param jObjects The other JSON objects to merge with
     * @return The merged JSON object
     * @throws JSONException If composing merged JSON object fails for any reason
     */
    public static JSONObject merge(final JSONObject jObject1, final JSONObject... jObjects) throws JSONException {
        if (null == jObject1) {
            return jObject1;
        }
        final JSONObject merged = new JSONObject(jObject1.length());
        for (final Entry<String,Object> entry : jObject1.entrySet()) {
            merged.put(entry.getKey(), entry.getValue());
        }
        if ((null == jObjects) || (0 == jObjects.length)) {
            return merged;
        }
        // Iterate others
        for (final JSONObject obj : jObjects) {
            if (null != obj) {
                mergeInto(merged, obj);
            }
        }
        return merged;
    }

    private static void mergeInto(final JSONObject j1, final JSONObject j2) throws JSONException {
        for (final Iterator<String> keys2 = j2.keys(); keys2.hasNext();) {
            final String key = keys2.next();
            final Object object2 = j2.get(key);
            if (isJSONObject(object2)) {
                if (j1.hasAndNotNull(key)) {
                    final Object object1 = j1.get(key);
                    if (!isJSONObject(object1)) {
                        throw new JSONException("JSON merge failed for key \"" + key + "\": Incompatible values " + object1.getClass().getSimpleName() + " != " + object2.getClass().getSimpleName());
                    }
                    mergeInto((JSONObject) object1, (JSONObject) object2);
                    j1.put(key, object1);
                } else {
                    j1.put(key, object2);
                }
            } else if (isJSONArray(object2)) {
                if (j1.hasAndNotNull(key)) {
                    final Object object1 = j1.get(key);
                    if (!isJSONArray(object1)) {
                        throw new JSONException("JSON merge failed for key \"" + key + "\": Incompatible values " + object1.getClass().getSimpleName() + " != " + object2.getClass().getSimpleName());
                    }
                    mergeInto((JSONArray) object1, (JSONArray) object2);
                    j1.put(key, object1);
                } else {
                    j1.put(key, object2);
                }
            } else {
                j1.put(key, object2);
            }
        }

    }

    private static boolean isJSONObject(Object o) {
        return (o instanceof JSONObject);
    }

    private static boolean isJSONArray(Object o) {
        return (o instanceof JSONArray);
    }

    private static void mergeInto(final JSONArray a1, final JSONArray a2) throws JSONException {
        final int len = a2.length();
        for (int i = 0; i < len; i++) {
            final Object object = a2.get(i);
            if (!contains(a1, object)) {
                a1.put(object);
            }
        }
    }

    private static boolean contains(final JSONArray jsonArray, final Object object) throws JSONException {
        final int len = jsonArray.length();
        for (int i = 0; i < len; i++) {
            if (jsonArray.get(i).equals(object)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the appropriate JSON value for specified string.
     * <p>
     * The value can be a Boolean, Double, Integer, JSONArray, JSONObject, Long, or String, or the JSONObject.NULL object.
     *
     * @param value The value
     * @return The resulting object
     * @throws JSONException If String cannot be transformed to any object according to JSON specification
     */
    public static Object toObject(final String value) throws JSONException {
        if (null == value) {
            return null;
        }
        return new JSONTokener(value).nextValue();
    }

    /**
     * Requires specified string value.
     *
     * @param name The field name
     * @param jsonObject The JSON object
     * @return The string value
     * @throws OXException If value is not available
     */
    public static String requireString(final String name, final JSONObject jsonObject) throws OXException {
        try {
            final String value = jsonObject.optString(name, null);
            if (null == value) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create(name);
            }
            return value;
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(e, name);
        }
    }

    /**
     * Requires specified integer value.
     *
     * @param name The field name
     * @param jsonObject The JSON object
     * @return The integer value
     * @throws OXException If value is not available
     */
    public static int requireInt(final String name, final JSONObject jsonObject) throws OXException {
        try {
            if (!jsonObject.has(name)) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create(name);
            }
            return jsonObject.optInt(name, -1);
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(e, name);
        }
    }

    /**
     * Requires specified boolean value.
     *
     * @param name The field name
     * @param jsonObject The JSON object
     * @return The boolean value
     * @throws OXException If value is not available
     */
    public static boolean requireBoolean(final String name, final JSONObject jsonObject) throws OXException {
        try {
            if (!jsonObject.has(name)) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create(name);
            }
            return jsonObject.optBoolean(name, false);
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(e, name);
        }
    }

    private static final String DATA = ResponseFields.DATA;

    /**
     * Requires specified data object.
     *
     * @param jsonObject The JSON object
     * @return The data JSON object
     * @throws OXException If data JSON object is not available
     */
    public static JSONObject requireDataObject(final JSONObject jsonObject) throws OXException {
        try {
            final JSONObject value = jsonObject.optJSONObject(DATA);
            if (null == value) {
                throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
            }
            return value;
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create(e);
        }
    }

}
