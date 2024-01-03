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
package com.openexchange.segment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.ImmutableJSONObject;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import com.openexchange.java.Strings;

/**
 * A {@link SegmentMarker} - A segment marker contains information from which the associated segment (data center) can be derived
 * through the segmenter service.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
public class SegmentMarker {

    /**
     * Creates a new {@link SegmentMarker} from the given database schema
     *
     * @param databaseSchema The database schema
     * @return The {@link SegmentMarker}
     */
    public static SegmentMarker of(String databaseSchema) {
        return new SegmentMarker(databaseSchema);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private final String schema;
    private final JSONObject json;

    /**
     * Initializes a new {@link SegmentMarker}.
     *
     * @param schema The database schema
     * @throws IllegalArgumentException If the schema name is empty
     */
    private SegmentMarker(String schema) {
        this(schema, new JSONObject(1).putSafe("schema", schema));
    }

    /**
     * Initializes a new {@link SegmentMarker}.
     *
     * @param schema The database schema
     * @param json The JSON representation
     * @throws IllegalArgumentException If the schema name is empty
     */
    private SegmentMarker(String schema, JSONObject json) {
        super();
        if (Strings.isEmpty(schema)) {
            throw new IllegalArgumentException("Schema must not be null or empty");
        }
        if (json == null) {
            throw new IllegalArgumentException("JSON must not be null");
        }
        this.schema = schema;
        this.json = ImmutableJSONObject.immutableFor(json);
    }

    /**
     * Gets the name of the database schema.
     *
     * @return The schema name
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Gets the JSON representation for this instance.
     *
     * @return The JSON representation
     */
    public JSONObject toJSON() {
        return json;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    // ------------------------------------------------------- En-/decoding ---------------------------------------------------------------

    /**
     * Creates the base64-encoded marker representation from this segment marker.
     *
     * @return The base64-encoded marker representation
     */
    public String encode() {
        return Base64.getEncoder().encodeToString(toJSON().toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes the marker and creates a segment marker from it.
     *
     * @param encodedMarker The base64-encoded marker
     * @return The decoded segment marker
     * @throws JSONException If decoding fails
     */
    public static SegmentMarker decode(String encodedMarker) throws JSONException {
        JSONObject json = JSONServices.parseObject(Base64.getDecoder().decode(encodedMarker.getBytes(StandardCharsets.ISO_8859_1)));
        return new SegmentMarker(json.getString("schema"), json);
    }

}
