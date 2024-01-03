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

package com.openexchange.ajax.framework;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import com.openexchange.java.Strings;
import netscape.javascript.JSException;

/**
 * 
 * {@link AbstractAJAXEntityResponse}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 */
public abstract class AbstractAJAXEntityResponse extends AbstractAJAXResponse implements AutoCloseable {

    private final int statusCode;
    private final String contentType;
    private final long contentLength;
    private final ByteArrayOutputStream out;
    private final List<Header> headers;

    /**
     * Initializes a new {@link AbstractAJAXEntityResponse}.
     *
     * @param response The HTTP response to get data from
     */
    protected AbstractAJAXEntityResponse(HttpResponse response) {
        super(null);
        this.statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();
        if (null == entity) {
            this.contentType = null;
            this.contentLength = 0L;
            this.out = null;
        } else {
            this.contentType = entity.getContentType().getValue();
            this.contentLength = entity.getContentLength();
            this.out = new ByteArrayOutputStream(Long.valueOf(contentLength).intValue());
            try {
                entity.getContent().transferTo(out);
            } catch (UnsupportedOperationException | IOException e) {
                throw new JSException(e);
            }
        }
        this.headers = null != response.getAllHeaders() ? Arrays.asList(response.getAllHeaders()) : Collections.emptyList();
    }

    /**
     * The status code
     *
     * @return The code
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * The content type
     *
     * @return The type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get the raw content
     *
     * @return The raw content as {@link InputStream}
     * @throws IllegalStateException
     */
    public InputStream getContent() throws IllegalStateException {
        return null != out ? new ByteArrayInputStream(out.toByteArray()) : null;
    }

    /**
     * Get the raw content
     *
     * @return The raw content as byte array
     * @throws IllegalStateException
     */
    public byte[] getContentAsByteArray() {
        return null != out ? out.toByteArray() : null;
    }

    /**
     * Get the content length as set in the response
     *
     * @return The length
     */
    public long getContentLength() {
        return contentLength;
    }

    @Override
    public Object getData() {
        return getContent();
    }

    @Override
    public boolean hasError() {
        return HttpServletResponse.SC_OK != getStatusCode();
    }

    @Override
    public void close() throws Exception {
        if (null != out) {
            out.close();
        }
    }

    /**
     * Gets the headers
     *
     * @return The headers
     */
    public List<Header> getHeaders() {
        return headers;
    }

    /**
     * Get the first header with the given name
     *
     * @param name The header name
     * @return The header or <code>null</code>
     */
    public Header getFirstHeader(String name) {
        if (Strings.isEmpty(name) || headers.isEmpty()) {
            return null;
        }
        return headers.stream().filter(h -> name.equalsIgnoreCase(h.getName())).findFirst().orElse(null);
    }
}
