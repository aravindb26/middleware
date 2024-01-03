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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import com.openexchange.cluster.map.codec.MapCodec;
import com.openexchange.java.Streams;

/**
 * {@link StringMapCodec} - The String codec.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class StringMapCodec implements MapCodec<String> {

    private static final StringMapCodec INSTANCE= new StringMapCodec();

    /**
     * Gets the instance.
     *
     * @return The instance
     */
    public static StringMapCodec getInstance() {
        return INSTANCE;
    }

    // -----------------------------------------------------------------------------------------------------------------------------------

    private final Charset charset;

    private StringMapCodec() {
        super();
        this.charset = StandardCharsets.UTF_8;
    }

    @Override
    public InputStream serializeValue(String value) throws Exception {
        return Streams.newByteArrayInputStream(value.getBytes(charset));
    }

    @Override
    public String deserializeValue(InputStream data) throws Exception {
        return new String(Streams.stream2bytes(data), charset);
    }
}
