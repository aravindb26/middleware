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

package com.openexchange.mail.exportpdf.collabora.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import com.openexchange.exception.OXException;
import com.openexchange.mail.exportpdf.MailExportAttachmentInformation;
import com.openexchange.mail.exportpdf.converters.images.InlineImageUtils;

/**
 * {@link InlineImage} - Represents an inline image which can be converted along with a HTML body
 *
 * @author <a href="mailto:benjamin.gruedelbach@open-xchange.com">Benjamin Gruedelbach</a>
 */
public class InlineImage implements AutoCloseable {

    private final String cid;
    private final String contentType;
    private final InputStream data;

    /**
     * Initializes a new {@link InlineImage}.
     *
     * @param inlineImageAttachment The {@link MailExportAttachmentInformation} to create an {@link InlineImage} from
     * @throws OXException if an error is occurred
     */
    public InlineImage(MailExportAttachmentInformation inlineImageAttachment) throws OXException {
        Objects.requireNonNull(inlineImageAttachment, "inlineImageAttachment must not be null");
        this.cid = InlineImageUtils.normalizeCID(inlineImageAttachment.getImageCID());
        this.contentType = inlineImageAttachment.getBaseContentType();
        this.data = inlineImageAttachment.getMailPart().getInputStream();
    }

    /**
     * Gets the CID
     *
     * @return The CID
     */
    public String getCid() {
        return cid;
    }

    /**
     * Gets the content type of the image
     *
     * @return The content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the actual image data
     *
     * @return The image data as {@link InputStream}
     */
    public InputStream getData() {
        return data;
    }

    @Override
    public void close() throws OXException {
        try {
            this.data.close();
        } catch (IOException e) {
            throw CollaboraMailExportExceptionCodes.IO_ERROR.create(e, e.getMessage());
        }
    }
}
