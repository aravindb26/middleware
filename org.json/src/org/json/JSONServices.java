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

import java.io.InputStream;
import java.io.Reader;
import org.json.internal.JSONServiceImpl;

/**
 * {@link JSONServices} - Utility class for JSON service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class JSONServices {

    private JSONService jsonService;

    /**
     * Initializes a new {@link JSONServices}.
     */
    private JSONServices() {
        super();
    }

    /**
     * Gets the singleton JSON service instance.
     *
     * @return The JSON service instance to set or <code>null</code>
     */
    public static JSONService getJSONService() {
        return JSONServiceImpl.getInstance();
    }

    // ----------------------------------- Delegate methods ----------------------------------

    /**
     * Parses source to a JSON value.
     *
     * @param source The source
     * @return The parsed JSON value
     * @throws JSONException If parsing fails
     */
    public static JSONValue parse(final String source) throws JSONException {
        return getJSONService().parse(source);
    }

    /**
     * Parses readers' content to a JSON value.
     *
     * @param reader The reader
     * @return The parsed JSON value
     * @throws JSONException If parsing fails
     */
    public static JSONValue parse(final Reader reader) throws JSONException {
        return getJSONService().parse(reader);
    }

    /**
     * Parses stream' content to a JSON value.
     *
     * @param stream The stream
     * @return The parsed JSON value
     * @throws JSONException If parsing fails
     */
    public static JSONValue parse(final InputStream stream) throws JSONException {
        return getJSONService().parse(stream);
    }

    // ------------------------------------------------------------------------------------------

    /**
     * Parses source to a JSON object.
     *
     * @param source The source
     * @return The parsed JSON object
     * @throws JSONException If parsing fails
     */
    public static JSONObject parseObject(final String source) throws JSONException {
        return getJSONService().parseObject(source);
    }

    /**
     * Parses readers' content to a JSON object.
     *
     * @param reader The reader
     * @return The parsed JSON object
     * @throws JSONException If parsing fails
     */
    public static JSONObject parseObject(final Reader reader) throws JSONException {
        return getJSONService().parseObject(reader);
    }

    /**
     * Parses stream' content to a JSON object.
     *
     * @param stream The stream
     * @return The parsed JSON object
     * @throws JSONException If parsing fails
     */
    public static JSONObject parseObject(final InputStream stream) throws JSONException {
        return getJSONService().parseObject(stream);
    }

    /**
     * Parses byte array to a JSON object.
     *
     * @param bytes The byte array
     * @return The parsed JSON object
     * @throws JSONException If parsing fails
     */
    public static JSONObject parseObject(final byte[] bytes) throws JSONException {
        return getJSONService().parseObject(bytes);
    }

    // ------------------------------------------------------------------------------------------

    /**
     * Parses source to a JSON array.
     *
     * @param source The source
     * @return The parsed JSON array
     * @throws JSONException If parsing fails
     */
    public static JSONArray parseArray(final String source) throws JSONException {
        return getJSONService().parseArray(source);
    }

    /**
     * Parses readers' content to a JSON array.
     *
     * @param reader The reader
     * @return The parsed JSON array
     * @throws JSONException If parsing fails
     */
    public static JSONArray parseArray(final Reader reader) throws JSONException {
        return getJSONService().parseArray(reader);
    }

    /**
     * Parses stream' content to a JSON array.
     *
     * @param stream The stream
     * @return The parsed JSON array
     * @throws JSONException If parsing fails
     */
    public static JSONArray parseArray(final InputStream stream) throws JSONException {
        return getJSONService().parseArray(stream);
    }

    /**
     * Parses byte array to a JSON array.
     *
     * @param bytes The byte array
     * @return The parsed JSON array
     * @throws JSONException If parsing fails
     */
    public static JSONArray parseArray(final byte[] bytes) throws JSONException {
        return getJSONService().parseArray(bytes);
    }

}
