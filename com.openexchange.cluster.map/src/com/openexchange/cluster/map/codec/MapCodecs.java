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

package com.openexchange.cluster.map.codec;

import java.util.List;
import com.openexchange.cluster.map.codec.standard.DefaultMapCodec;
import com.openexchange.cluster.map.codec.standard.LongMapCodec;
import com.openexchange.cluster.map.codec.standard.StringListMapCodec;
import com.openexchange.cluster.map.codec.standard.StringMapCodec;

/**
 * {@link MapCodecs} - Provides access to basic map codecs.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class MapCodecs {

    /**
     * Initializes a new {@link MapCodecs}.
     */
    private MapCodecs() {
        super();
    }

    /**
     * Gets the basic <code>String</code> codec.
     *
     * @return The <code>String</code> codec
     */
    public static MapCodec<String> getStringCodec() {
        return StringMapCodec.getInstance();
    }

    /**
     * Gets the basic codec for a list of <code>String</code>s.
     *
     * @return The codec for a list of <code>String</code>s
     */
    public static MapCodec<List<String>> getStringListCodec() {
        return StringListMapCodec.getInstance();
    }

    /**
     * Gets the basic <code>long</code> codec.
     *
     * @return The <code>long</code> codec
     */
    public static MapCodec<Long> getLongCodec() {
        return LongMapCodec.getInstance();
    }

    /**
     * Gets the default codec for given type (using Jackson object-mapper).
     *
     * @param <T> The type
     * @param type The type's class
     * @return The codec
     */
    public static <T> MapCodec<T> getDefaultCodec(Class<T> type) {
        return DefaultMapCodec.codecFor(type);
    }

}
