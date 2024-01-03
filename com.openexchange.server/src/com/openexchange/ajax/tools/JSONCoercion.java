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

import java.util.Collection;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;


/**
 * {@link JSONCoercion} - Turns JSON data to its Java representation and vice versa.
 * <p>
 * A {@link JSONObject} is coerced to a {@link Map}, a {@link JSONArray} is coerced to a {@link Collection}.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @deprecated Use {@link org.json.JSONCoercion}
 */
@Deprecated
public class JSONCoercion {

    /**
     * Coerces given JSON data to its Java representation.
     *
     * @param object The JSON data to coerce
     * @return The resulting Java representation.
     * @throws JSONException If coercion fails
     */
    public static Object coerceToNative(final JSONValue object) throws JSONException {
        return org.json.JSONCoercion.coerceToNative(object);
    }

    public static Object parseAndCoerceToNative(String s) {
        return org.json.JSONCoercion.parseAndCoerceToNative(s);
    }

    /**
     * Coerces given JSON data to its Java representation.
     *
     * @param object The JSON data to coerce
     * @return The resulting Java representation.
     * @throws JSONException If coercion fails
     */
    public static Object coerceToNative(final Object object) throws JSONException {
        return org.json.JSONCoercion.coerceToNative(object);
    }

    /**
     * Checks if specified object needs to be coerced to JSON.
     *
     * @param value The object to check
     * @return <code>true</code> if specified object needs to be coerced to JSON; otherwise <code>false</code>
     */
    public static boolean needsJSONCoercion(final Object value) {
        return org.json.JSONCoercion.needsJSONCoercion(value);
    }

    /**
     * Checks if specified object needs to be coerced to a native Java object.
     *
     * @param value The object to check
     * @return <code>true</code> if specified object needs to be coerced to a native Java object; otherwise <code>false</code>
     */
    public static boolean needsNativeCoercion(final Object value) {
        return org.json.JSONCoercion.needsNativeCoercion(value);
    }

    /**
     * Coerces given Java object to its JSON representation.
     *
     * @param value The Java object to coerce
     * @return The resulting JSON representation
     * @throws JSONException If coercion fails
     */
    public static Object coerceToJSON(final Object value) throws JSONException {
        return org.json.JSONCoercion.coerceToJSON(value);
    }

    /**
     * Checks if specified object is an array.
     *
     * @param object The object to check
     * @return <code>true</code> if specified object is an array; otherwise <code>false</code>
     */
    public static boolean isArray(final Object object) {
        return org.json.JSONCoercion.isArray(object);
    }

}
