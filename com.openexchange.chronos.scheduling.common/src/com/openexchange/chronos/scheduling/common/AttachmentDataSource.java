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

package com.openexchange.chronos.scheduling.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.activation.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.Attachment;
import com.openexchange.exception.OXException;

/**
 * {@link AttachmentDataSource} - Loads attachment data
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class AttachmentDataSource implements DataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachmentDataSource.class);

    private DataSourceProvider provider;
    private Attachment attachment;

    /**
     * Initializes a new {@link AttachmentDataSource}.
     * 
     * @param provider The underlying provider for the attachment
     * @param attachment The {@link Attachment} to load
     */
    public AttachmentDataSource(DataSourceProvider provider, Attachment attachment) {
        super();
        this.provider = provider;
        this.attachment = attachment;
    }

    @Override
    public String getContentType() {
        return attachment.getFormatType();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (null == attachment || attachment.getManagedId() <= 0) {
            // Skip external managed attachments
            throw new IOException("Unable to load attachment.");
        }

        try {
            return provider.getAttachmentData(attachment.getManagedId());
        } catch (OXException e) {
            LOGGER.info("Unable to retrive attachment from storage", e);
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return attachment.getFilename();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException(this.getClass().getName() + ".getOutputStream() isn't implemented");
    }

}
