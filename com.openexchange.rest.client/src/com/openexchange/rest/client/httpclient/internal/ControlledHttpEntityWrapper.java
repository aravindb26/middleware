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
package com.openexchange.rest.client.httpclient.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import com.openexchange.java.InterruptibleInputStream;
import com.openexchange.streamcontrol.ControlledInputStream;


/**
 * A wrapper that returns a controlled input stream on {@link #getContent()} invocation.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ControlledHttpEntityWrapper implements HttpEntity {

    private final HttpEntity entity;
    private final int timeout;

    /**
     * Initializes a new {@link ControlledHttpEntityWrapper}.
     *
     * @param entity The HTTP entity to wrap
     * @param timeout The timeout when reading data from entity's input stream
     */
    public ControlledHttpEntityWrapper(HttpEntity entity, int timeout) {
        super();
        this.entity = entity;
        this.timeout = timeout;
    }

    @Override
    public boolean isRepeatable() {
        return entity.isRepeatable();
    }

    @Override
    public boolean isChunked() {
        return entity.isChunked();
    }

    @Override
    public long getContentLength() {
        return entity.getContentLength();
    }

    @Override
    public Header getContentType() {
        return entity.getContentType();
    }

    @Override
    public Header getContentEncoding() {
        return entity.getContentEncoding();
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        return ControlledInputStream.valueOf(InterruptibleInputStream.valueOf((entity.getContent())), timeout);
    }

    @Override
    public void writeTo(OutputStream outStream) throws IOException {
        InputStream inStream = getContent();
        try {
            final byte[] tmp = new byte[8192];
            for (int l; (l = inStream.read(tmp)) != -1;) {
                outStream.write(tmp, 0, l);
            }
        } finally {
            inStream.close();
        }
    }

    @Override
    public boolean isStreaming() {
        return entity.isStreaming();
    }

    @Override
    public void consumeContent() throws IOException {
        entity.consumeContent();
    }

}
