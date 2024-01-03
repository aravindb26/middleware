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


package com.openexchange.request.analyzer;

import com.openexchange.java.Streams;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


/**
 * {@link ByteArrayBodyData} - The request body data backed by a byte array.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ByteArrayBodyData implements BodyData {

    private final byte[] bytes;
    private final String contentType;

    /**
     * Initializes a new {@link ByteArrayBodyData}.
     *
     * @param bytes The request body data as byte array
     */
    public ByteArrayBodyData(byte[] bytes) {
        this(bytes, null);
    }

    /**
     * Initializes a new {@link ByteArrayBodyData}.
     *
     * @param bytes The request body data as byte array
     * @param contentType The Content-Type header, if known
     */
    public ByteArrayBodyData(byte[] bytes, String contentType) {
        super();
        Objects.requireNonNull(bytes);
        this.bytes = bytes;
        this.contentType = contentType;
    }

    @Override
    public InputStream getData() {
        return Streams.newByteArrayInputStream(bytes);
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        return bytes.length;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getDataAsString() throws IOException {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getDataAsByteArray() throws IOException {
        return bytes;
    }

}
