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

package com.openexchange.chronos.json.action.attachments;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import com.openexchange.ajax.container.FileHolder;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.json.exception.CalendarExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.session.Session;

/**
 * {@link ContentIDAttachmentHandler} - Associate uploads to attachments by matching 'cid' URI
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class ContentIDAttachmentHandler extends AbstractAttachmentHandler {

    /**
     * Initializes a new {@link ContentIDAttachmentHandler}.
     */
    public ContentIDAttachmentHandler() {
        super();
    }

    @Override
    public void handle(Session session, Map<String, UploadFile> uploads, Attachment attachment) throws OXException {
        String contentId = getContentId(attachment);
        UploadFile uploadFile = uploads.get(contentId);
        if (uploadFile == null) {
            throw CalendarExceptionCodes.MISSING_BODY_PART_ATTACHMENT_REFERENCE.create(contentId);
        }
        File tmpFile = uploadFile.getTmpFile();
        attachment.setData(checkSize(new FileHolder(FileHolder.newClosureFor(tmpFile), tmpFile.length(), attachment.getFormatType(), attachment.getFilename())));
    }

    /**
     * Returns the content-id from the attachment's URI
     *
     * @param attachment the attachment
     * @return The content-id
     * @throws OXException if no content-id is pressent
     */
    private static String getContentId(Attachment attachment) throws OXException {
        try {
            URI uri = new URI(attachment.getUri());
            return uri.getSchemeSpecificPart();
        } catch (URISyntaxException e) {
            throw CalendarExceptionCodes.MISSING_METADATA_ATTACHMENT_REFERENCE.create(e, attachment.getFilename());
        }
    }
}
