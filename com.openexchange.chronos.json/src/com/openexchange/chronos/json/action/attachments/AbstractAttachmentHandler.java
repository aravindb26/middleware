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

import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.chronos.Attachment;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.upload.impl.UploadSizeExceededException;
import com.openexchange.java.Strings;

/**
 * {@link AbstractAttachmentHandler}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v8.0.0
 */
public abstract class AbstractAttachmentHandler implements AttachmentHandler {

    /**
     * Initializes a new {@link AbstractAttachmentHandler}.
     */
    protected AbstractAttachmentHandler() {
        super();
    }

    /**
     * Takes over metadata properties found in the supplied {@link IFileHolder} reference in the attachment metadata object, unless
     * already set.
     * 
     * @param attachment The attachment metadata to enrich
     * @param fileHolder The file holder to take over the metadata properties from
     */
    protected static void applyFileHolderMetadata(Attachment attachment, IFileHolder fileHolder) {
        if (null == fileHolder || null == attachment) {
            return;
        }
        if (Strings.isEmpty(attachment.getFormatType()) && Strings.isNotEmpty(fileHolder.getContentType())) {
            attachment.setFormatType(fileHolder.getContentType());
        }
        if (Strings.isEmpty(attachment.getFilename()) && Strings.isNotEmpty(fileHolder.getName())) {
            attachment.setFilename(fileHolder.getName());
        }
        if (0 >= attachment.getSize() && 0 < fileHolder.getLength()) {
            attachment.setSize(fileHolder.getLength());
        }
    }

    /**
     * Checks current size of uploaded data against possible quota restrictions.
     *
     * @param size The size
     * @throws OXException If any quota restrictions are exceeded
     */
    protected static void checkSize(long size) throws OXException {
        long maxUploadSize = AttachmentConfig.getMaxUploadSize();
        if (maxUploadSize == 0) {
            return;
        }
        if (size > maxUploadSize) {
            throw UploadSizeExceededException.create(size, maxUploadSize, true);
        }
    }

    /**
     * Checks current size of uploaded data against possible quota restrictions.
     *
     * @param fileHolder The file holder to check
     * @throws OXException If any quota restrictions are exceeded
     * @return The passed file holder reference, after its size was checked
     */
    protected static IFileHolder checkSize(IFileHolder fileHolder) throws OXException {
        if (null != fileHolder && 0 < fileHolder.getLength()) {
            checkSize(fileHolder.getLength());
        }
        return fileHolder;
    }

}
