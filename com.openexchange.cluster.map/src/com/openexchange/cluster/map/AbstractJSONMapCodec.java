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

package com.openexchange.cluster.map;

import java.io.InputStream;
import org.json.JSONObject;
import org.json.JSONServices;
import com.openexchange.annotation.NonNull;
import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;

/**
 * {@link AbstractJSONMapCodec} - The abstract JSON map codec.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public abstract class AbstractJSONMapCodec<V> implements MapCodec<V> {

    /**
     * Initializes a new {@link AbstractJSONMapCodec}.
     */
    protected AbstractJSONMapCodec() {
        super();
    }

    @Override
    public InputStream serializeValue(V value) throws Exception {
        try {
            JSONObject json = writeJson(value);
            return json.getStream(false);
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw OXException.general("Serialisation failed", e);
        }
    }

    @Override
    public V deserializeValue(InputStream data) throws Exception {
        try {
            return parseJson(JSONServices.parseObject(data));
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw OXException.general("Deserialisation failed", e);
        } finally {
            Streams.close(data);
        }
    }

    /**
     * Writes given value to JSON.
     *
     * @param value The value to generate the JSON value from
     * @return The JSON
     * @throws Exception If JSON value cannot be written
     */
    protected abstract @NonNull JSONObject writeJson(V value) throws Exception;

    /**
     * Parses given JSON to the appropriate type.
     *
     * @param jObject The JSON to parse
     * @return The parsed type
     * @throws Exception If parsing fails
     */
    protected abstract @NonNull V parseJson(JSONObject jObject) throws Exception;
}
