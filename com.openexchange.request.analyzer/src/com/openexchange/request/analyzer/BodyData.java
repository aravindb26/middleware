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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.openexchange.java.Streams;

/**
 * {@link BodyData} - The body data for a request to examine.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface BodyData {

    /**
     * Tells if this data is capable of producing its data more than once.
     * <p>
     * A repeatable data's getData() can be called more than once whereas a non-repeatable entity's can not.
     *
     * @return <code>true</code>e if the entity is repeatable, <code>false</code> otherwise.
     */
    boolean isRepeatable();

    /**
     * Gets the length of the content, if known.
     *
     * @return The number of bytes of the content, or a negative number if unknown.
     */
    long getContentLength();

    /**
     * Obtains the Content-Type header, if known.
     *
     * @return The Content-Type header for this entity, or <code>null</code> if the content type is unknown
     */
    String getContentType();

    /**
     * Gets the binary request's body data.
     *
     * @return The body data
     * @throws An I/O error if data cannot be returned
     */
    InputStream getData() throws IOException;

    /**
     * Gets the data as UTF-8 string.
     *
     * @return The string
     * @throws IOException If an I/O error occurs
     */
    default String getDataAsString() throws IOException {
        try (InputStream stream = getData()) {
            return Streams.stream2string(stream, StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets the data as byte array.
     *
     * @return The byte array
     * @throws IOException If an I/O error occurs
     */
    default byte[] getDataAsByteArray() throws IOException {
        try (InputStream stream = getData()) {
            return Streams.stream2bytes(stream);
        }
    }

}
