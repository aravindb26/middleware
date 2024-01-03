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

package com.openexchange.pubsub;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;

/**
 * {@link Codecs} - Utilities for cache/channel codecs.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class Codecs {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(Codecs.class);
    }

    /**
     * Initializes a new {@link Codecs}.
     */
    private Codecs() {
        super();
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        ObjectMapper objectMapper = new ObjectMapper();
        /*-
        {
            JsonSerializer<V> serializer = optionalSerializer.get();
            SimpleModule module = new SimpleModule(serializer.getClass().getSimpleName(), new Version(2, 1, 3, null, null, null));
            module.addSerializer(type, serializer);
            objectMapper.registerModule(module);
        }*/
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER = objectMapper;
    }

    /**
     * Serializes any Java value as JSON output (using UTF-8 encoding).
     *
     * @param <O> The type of the Java object
     * @param object The object to serialize
     * @return The JSON output
     * @throws OXException If serialization fails
     */
    public static <O> InputStream value2jsonData(O object) throws OXException {
        if (object == null) {
            return null;
        }
        try {
            ByteArrayOutputStream outputStream = Streams.newByteArrayOutputStream();
            OBJECT_MAPPER.writeValue(outputStream, object);
            return Streams.asInputStream(outputStream);
        } catch (Exception e) {
            throw OXException.general("Serialisation failed", e);
        }
    }

    /**
     * De-serializes given JSON input to any Java object (using UTF-8 encoding).
     *
     * @param <O> The type of the Java object
     * @param data The JSON input to read from
     * @param type The class of the Java object
     * @return The Java object
     * @throws OXException If de-serialization fails
     */
    public static <O> O jsonData2value(InputStream data, Class<O> type) throws OXException {
        try {
            return OBJECT_MAPPER.readValue(data, type);
        } catch (JsonMappingException e) {
            LoggerHolder.LOG.warn("Failed to deserialze data to type {}", type.getName(), e);
            return null;
        } catch (Exception e) {
            throw OXException.general("Deserialisation failed", e);
        }
    }

    /**
     * Serializes any Java value as JSON string.
     *
     * @param <O> The type of the Java object
     * @param object The object to serialize
     * @return The JSON string
     * @throws OXException If serialization fails
     */
    public static <O> String value2jsonString(O object) throws OXException {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            throw OXException.general("Serialisation failed", e);
        }
    }

    /**
     * De-serializes given JSON input to any Java object (using UTF-8 encoding).
     *
     * @param <O> The type of the Java object
     * @param data The JSON string to read from
     * @param type The class of the Java object
     * @return The Java object
     * @throws OXException If de-serialization fails
     */
    public static <O> O jsonString2value(String data, Class<O> type) throws OXException {
        try {
            return OBJECT_MAPPER.readValue(data, type);
        } catch (JsonMappingException e) {
            LoggerHolder.LOG.warn("Failed to deserialze JSON string to type {}", type.getName(), e);
            return null;
        } catch (Exception e) {
            throw OXException.general("Deserialisation failed", e);
        }
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    /**
     * Converts given integer array to a JSON array.
     *
     * @param arr The integer array
     * @return The JSON array
     */
    public static JSONArray intArrayToJsonArray(int[] arr) {
        if (arr == null) {
            return null;
        }

        int length = arr.length;
        JSONArray jArr = new JSONArray(length);
        for (int i = 0; i < length; i++) {
            jArr.put(arr[i]);
        }
        return jArr;
    }

    /**
     * Converts given JSON array to an integer array.
     *
     * @param jArr The JSON array
     * @return The integer array
     * @throws JSONException If conversion fails
     */
    public static int[] jsonArrayToIntArray(JSONArray jArr) throws JSONException {
        if (jArr == null) {
            return null;
        }

        int length = jArr.length();
        int[] arr = new int[length];
        for (int i = length; i-- > 0;) {
            arr[i] = jArr.getInt(i);
        }
        return arr;
    }

    /**
     * Converts given String array to a JSON array.
     *
     * @param arr The String array
     * @return The JSON array
     */
    public static JSONArray stringArrayToJsonArray(String[] arr) {
        if (arr == null) {
            return null;
        }

        int length = arr.length;
        JSONArray jArr = new JSONArray(length);
        for (int i = 0; i < length; i++) {
            jArr.put(arr[i]);
        }
        return jArr;
    }

    /**
     * Converts given JSON array to a String array.
     *
     * @param jArr The JSON array
     * @return The String array
     * @throws JSONException If conversion fails
     */
    public static String[] jsonArrayToStringArray(JSONArray jArr) throws JSONException {
        if (jArr == null) {
            return null;
        }

        int length = jArr.length();
        String[] arr = new String[length];
        for (int i = length; i-- > 0;) {
            arr[i] = jArr.getString(i);
        }
        return arr;
    }

    /**
     * Converts given JSON object to a String map.
     *
     * @param jObj The JSON object
     * @return The String map
     */
    public static Map<String, String> jsonObjectToStringMap(JSONObject jObj) {
        if (jObj == null) {
            return null;
        }

        int length = jObj.length();
        Map<String, String> map = new LinkedHashMap<>(length);
        for (Map.Entry<String, Object> entry : jObj.entrySet()) {
            map.put(entry.getKey(), entry.getValue().toString());
        }
        return map;
    }

    /**
     * Converts given String map to a JSON object.
     *
     * @param map The map
     * @return The JSON object
     * @throws JSONException If conversion fails
     */
    public static JSONObject stringMapToJsonObject(Map<String, String> map) throws JSONException {
        if (map == null) {
            return null;
        }

        int size = map.size();
        JSONObject jObj = new JSONObject(size);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            jObj.put(entry.getKey(), entry.getValue());
        }
        return jObj;
    }

}
