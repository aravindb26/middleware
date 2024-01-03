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

package com.openexchange.filestore.sproxyd.impl;

import java.io.IOException;
import java.util.UUID;
import com.openexchange.exception.ExceptionUtils;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;
import com.openexchange.rest.client.httpclient.HttpClients.HttpResponseStream;

/**
 * {@link ResumableInputStream} - Resumes reading an S3 object's content on premature EOF and ensures
 * underlying S3 object't content stream is aborted if this gets closed even though not all bytes have been read, yet.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public class ResumableInputStream extends AbstractResumableInputStream {

    private long numberOfReadBytes;

    /**
     * Initializes a new {@link ResumableInputStream}.
     *
     * @param objectContent The input stream containing the contents of an object
     * @param url The UUID of the desired object
     * @param sproxydClient The client to access Sproxyd resources
     */
    public ResumableInputStream(HttpResponseStream objectContent, UUID id, SproxydClient sproxydClient) {
        super(objectContent, id, sproxydClient);
        numberOfReadBytes = 0;
    }

    @Override
    protected void onBytesRead(long numberOfBytes) {
        numberOfReadBytes += numberOfBytes;
    }

    @Override
    protected long getCurrentMark() {
        return numberOfReadBytes;
    }

    @Override
    protected void resetMark(long mark) {
        numberOfReadBytes = mark;
    }

    @Override
    protected void initNewObjectStreamAfterPrematureEof() throws IOException {
        // Issue Get-Object request with appropriate range
        try {
            long rangeEnd = getContentLength() - 1;
            long rangeStart = numberOfReadBytes;

            // Close existent stream from which -1 was prematurely read
            Streams.close(in);
            in = null;

            in = sproxydClient.nonResumingGet(id, rangeStart, rangeEnd);
        } catch (OXException e) {
            IOException ioe = ExceptionUtils.extractFrom(e, IOException.class);
            if (ioe != null) {
                throw ioe;
            }
            throw new IOException(e);
        }
    }

}
