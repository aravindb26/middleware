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


package org.json.internal;

import java.io.InputStream;
import java.io.Reader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONService;
import org.json.JSONValue;


/**
 * {@link JSONServiceImpl} - The JSON service implementation,
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class JSONServiceImpl implements JSONService {

    private static final JSONServiceImpl INSTANCE = new JSONServiceImpl();

    /**
     * Gets the instance
     *
     * @return The instance
     */
    public static JSONServiceImpl getInstance() {
        return INSTANCE;
    }

    // ------------------------------------------------------------------------------------

    /**
     * Initializes a new {@link JSONServiceImpl}.
     */
    private JSONServiceImpl() {
        super();
    }

    @Override
    public JSONValue parse(String source) throws JSONException {
        return JSONObject.parse(source);
    }

    @Override
    public JSONValue parse(Reader reader) throws JSONException {
        return JSONObject.parse(reader);
    }

    @Override
    public JSONValue parse(InputStream stream) throws JSONException {
        return JSONObject.parse(stream);
    }

    @Override
    public JSONObject parseObject(String source) throws JSONException {
        return new JSONObject(source);
    }

    @Override
    public JSONObject parseObject(Reader reader) throws JSONException {
        return new JSONObject(reader);
    }

    @Override
    public JSONObject parseObject(InputStream stream) throws JSONException {
        return new JSONObject(stream);
    }

    @Override
    public JSONObject parseObject(byte[] bytes) throws JSONException {
        return new JSONObject(bytes);
    }

    @Override
    public JSONArray parseArray(String source) throws JSONException {
        return new JSONArray(source);
    }

    @Override
    public JSONArray parseArray(Reader reader) throws JSONException {
        return new JSONArray(reader);
    }

    @Override
    public JSONArray parseArray(InputStream stream) throws JSONException {
        return new JSONArray(stream);
    }

    @Override
    public JSONArray parseArray(byte[] bytes) throws JSONException {
        return new JSONArray(bytes);
    }

}
