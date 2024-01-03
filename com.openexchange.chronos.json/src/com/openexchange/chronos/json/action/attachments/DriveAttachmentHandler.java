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

import static com.openexchange.java.Streams.bufferedInputStreamFor;
import java.util.Map;
import com.openexchange.ajax.container.FileHolder;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.json.exception.CalendarExceptionCodes;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.groupware.attach.AttachmentUtility;
import com.openexchange.groupware.attach.DriveAttachment;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tx.TransactionAwares;

/**
 * {@link DriveAttachmentHandler} - Handles drive attachments
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v8.0.0
 */
public class DriveAttachmentHandler extends AbstractAttachmentHandler {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link DriveAttachmentHandler}.
     *
     * @param services A service lookup reference
     */
    public DriveAttachmentHandler(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public void handle(Session session, Map<String, UploadFile> uploads, Attachment attachment) throws OXException {
        DriveAttachment driveAttachment = getDriveAttachment(attachment);
        IDBasedFileAccessFactory factory = services.getServiceSafe(IDBasedFileAccessFactory.class);
        IDBasedFileAccess fileAccess = factory.createAccess(session);
        try {
            File file = fileAccess.getFileMetadata(driveAttachment.getId(), driveAttachment.getVersion());
            attachment.setData(checkSize(new FileHolder(bufferedInputStreamFor(fileAccess.getDocument(driveAttachment.getId(), driveAttachment.getVersion())), file.getFileSize(), file.getFileMIMEType(), file.getFileName())));
            applyFileHolderMetadata(attachment, attachment.getData());
        } catch (OXException e) {
            if ("FLS-0017".equals(e.getErrorCode()) || "IFO-0438".equals(e.getErrorCode())) {
                throw CalendarExceptionCodes.UNABLE_TO_ADD_DRIVE_ATTACHMENT.create(e, driveAttachment.getId());
            }
            throw e;
        } finally {
            TransactionAwares.finishSafe(fileAccess);
        }
    }

    /**
     * Gets the {@link DriveAttachment} with the version and id from the specified {@link Attachment}
     *
     * @param attachment The attachment
     * @return The {@link DriveAttachment}
     * @throws OXException if the uri of the {@link Attachment} is malformed
     */
    private static DriveAttachment getDriveAttachment(Attachment attachment) throws OXException {
        try {
            return AttachmentUtility.getDriveAttachmentFromUri(attachment.getUri());
        } catch (Exception e) {
            throw CalendarExceptionCodes.UNABLE_TO_ADD_DRIVE_ATTACHMENT.create(e, attachment.getFilename());
        }
    }
}
