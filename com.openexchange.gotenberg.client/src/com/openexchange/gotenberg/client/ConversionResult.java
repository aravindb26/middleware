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

package com.openexchange.gotenberg.client;

import java.io.InputStream;
import java.util.Optional;

/**
 * {@link ConversionResult}
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class ConversionResult implements AutoCloseable {

    private final String contentType;
    private final InputStream data;
    private Optional<String> name;
    private final long size;

    /**
     * Initializes a new {@link ConversionResult}.
     *
     * @param contentType The Content-Type of the converted data
     * @param contentLength The Content-Length of the converted data
     * @param data The converted data as {@link InputStream}
     */
    public ConversionResult(String contentType, long contentLength, InputStream data) {
        this.contentType = contentType;
        this.data = data;
        this.name = Optional.empty();
        this.size = contentLength;
    }

    /**
     * Gets the Content-Type of the converted data
     *
     * @return The Content-Type of the converted data
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the converted data
     *
     * @return The converted data
     */
    public InputStream getData() {
        return data;
    }

    /**
     * Gets the size of the data
     *
     * @return The size of the data
     */
    public long getSize() {
        return size;
    }

    /**
     * The optional name of the converted result
     *
     * @return The optional name
     */
    public Optional<String> getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name The name to set, can be <code>null</code>
     * @return this
     */
    public ConversionResult setName(String name) {
        this.name = Optional.ofNullable(name);
        return this;
    }

    /**
     * Acts as a convenience method for calling the close method on the underlaying {@link InputStream} data,
     * obtained with {@link #getData()}
     */
    @Override
    public void close() throws Exception {
        if (data != null) {
            data.close();
        }
    }
}
