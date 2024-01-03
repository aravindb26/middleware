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

package com.openexchange.cluster.map.codec.standard;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONServices;
import com.openexchange.cluster.map.codec.MapCodec;

/**
 * {@link StringListMapCodec} - The codec for a list of Strings.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class StringListMapCodec implements MapCodec<List<String>> {

    private static final StringListMapCodec INSTANCE = new StringListMapCodec();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static StringListMapCodec getInstance() {
        return INSTANCE;
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    private StringListMapCodec() {
        super();
    }

    @Override
    public InputStream serializeValue(List<String> values) throws Exception {
        JSONArray ja = new JSONArray(values.size());
        for (String value : values) {
            ja.put(value);
        }
        return ja.getStream(false);
    }

    @Override
    public List<String> deserializeValue(InputStream data) throws Exception {
        JSONArray ja = JSONServices.parseArray(data);
        List<String> values = new ArrayList<>(ja.length());
        for (Object o : ja) {
            values.add(o.toString());
        }
        return values;
    }

}
