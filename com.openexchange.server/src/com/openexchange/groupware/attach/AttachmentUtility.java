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

package com.openexchange.groupware.attach;

import static com.openexchange.java.Autoboxing.I;
import static java.net.URLDecoder.decode;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import com.openexchange.ajax.Attachment;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.groupware.upload.impl.UploadSizeExceededException;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.java.Charsets;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.User;

/**
 * {@link AttachmentUtility} - Utility class for attachments.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since 7.6.0
 */
public final class AttachmentUtility {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(AttachmentUtility.class);
    
    private static final String DRIVE_SCHEME = "drive://";

    /**
     * Initializes a new {@link AttachmentUtility}.
     */
    private AttachmentUtility() {
        super();
    }

    /**
     * Attaches available upload files to specified entity.
     *
     * @param objectId The object/entity identifier
     * @param module The module identifier; see {@link Types}
     * @param folderId The folder identifier
     * @param requestData The AJAX request data
     * @param session The associated session
     * @return The identifiers of the attachments bound to specified entity
     * @throws OXException If attaching upload files fails
     */
    public static List<Integer> attachTo(final int objectId, final int module, final int folderId, final AJAXRequestData requestData, final ServerSession session) throws OXException {
        long maxUploadSize = AttachmentConfig.getMaxUploadSize();
        if (!requestData.hasUploads(-1, maxUploadSize > 0 ? maxUploadSize : -1L)) {
            return Collections.emptyList();
        }
        final UploadEvent upload = requestData.getUploadEvent(-1, maxUploadSize > 0 ? maxUploadSize : -1L);
        if (null == upload) {
            return Collections.emptyList();
        }

        final List<AttachmentMetadata> attachments = new ArrayList<>(4);
        final List<UploadFile> uploadFiles = new ArrayList<>(4);

        long sum = 0;
        int index = 0;
        for (final UploadFile uploadFile : upload.getUploadFiles()) {
            final AttachmentMetadata attachment = new AttachmentMetadataImpl(objectId, module, folderId);

            assureSize(index, attachments, uploadFiles);

            attachments.set(index, attachment);
            uploadFiles.set(index, uploadFile);
            sum += uploadFile.getSize();

            checkSize(sum, requestData);

            index++;
        }

        return attach(attachments, uploadFiles, session, session.getContext(), session.getUser(), session.getUserConfiguration());
    }

    private static List<Integer> attach(final List<AttachmentMetadata> attachments, final List<UploadFile> uploadFiles, final ServerSession session, final Context ctx, final User user, final UserConfiguration userConfig) throws OXException {
        initAttachments(attachments, uploadFiles);
        final List<Closeable> closeables = new LinkedList<>();
        boolean rollback = false;
        try {
            Attachment.ATTACHMENT_BASE.startTransaction();
            rollback = true;

            final Iterator<UploadFile> ufIter = uploadFiles.iterator();
            final List<Integer> ids = new LinkedList<>();
            long timestamp = 0;
            for (final AttachmentMetadata attachment : attachments) {
                final UploadFile uploadFile = ufIter.next();
                attachment.setId(AttachmentBase.NEW);

                final BufferedInputStream data = new BufferedInputStream(new FileInputStream(uploadFile.getTmpFile()), 65536);
                closeables.add(data);

                final long modified = Attachment.ATTACHMENT_BASE.attachToObject(attachment, data, session, ctx, user, userConfig);
                if (modified > timestamp) {
                    timestamp = modified;
                }
                ids.add(I(attachment.getId()));
            }
            Attachment.ATTACHMENT_BASE.commit();
            rollback = false;
            return ids;
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            if (rollback) {
                rollback();
            }
            finish();

            for (final Closeable closeable : closeables) {
                Streams.close(closeable);
            }
        }
    }

    public static void initAttachments(List<AttachmentMetadata> attachments, List<UploadFile> uploads) {
        List<AttachmentMetadata> attList = new ArrayList<>(attachments);
        Iterator<UploadFile> ufIter = new ArrayList<>(uploads).iterator();

        int index = 0;
        for (AttachmentMetadata attachment : attList) {
            if (attachment == null) {
                attachments.remove(index);
                ufIter.next();
                uploads.remove(index);
                continue;
            }
            UploadFile upload = ufIter.next();
            if (upload == null) {
                attachments.remove(index);
                uploads.remove(index);
                continue;
            }
            if (attachment.getFilename() == null || "".equals(attachment.getFilename())) {
                attachment.setFilename(upload.getPreparedFileName());
            }
            if (attachment.getFilesize() <= 0) {
                attachment.setFilesize(upload.getSize());
            }
            if (attachment.getFileMIMEType() == null || "".equals(attachment.getFileMIMEType())) {
                attachment.setFileMIMEType(upload.getContentType());
            }
            index++;
        }
    }

    public static void assureSize(final int index, final List<AttachmentMetadata> attachments, final List<UploadFile> uploadFiles) {
        int enlarge = index - (attachments.size() - 1);
        for (int i = 0; i < enlarge; i++) {
            attachments.add(null);
        }

        enlarge = index - (uploadFiles.size() - 1);
        for (int i = 0; i < enlarge; i++) {
            uploadFiles.add(null);
        }
    }
    
    /**
     * Decodes the specified URI and extracts the {@link DriveAttachment} metadata
     * 
     * @param uri The uri
     * @return The {@link DriveAttachment} metadata
     * @throws IllegalArgumentException if the uri is <code>null</code> or empty, or if it does not start
     *             with the correct scheme (@see {@link #DRIVE_SCHEME}, or if it has an invalid structure, or if it cannot be
     *             URL decoded.
     */
    public static DriveAttachment getDriveAttachmentFromUri(String uri) {
        if (Strings.isEmpty(uri) || false == uri.startsWith(DRIVE_SCHEME)) {
            throw new IllegalArgumentException("Invalid URI was specified. URI cannot not be empty and must begin with " + DRIVE_SCHEME);
        }
        String[] components = Strings.splitBy(uri.substring(DRIVE_SCHEME.length()), '/', false);
        if (components.length < 2) {
            throw new IllegalArgumentException("Invalid URI structure was specified. Missing file/folder and/or version.");
        }
        String id = decode(components[1], Charsets.UTF_8);
        String version = components.length >= 3 ? decode(components[2], Charsets.UTF_8) : FileStorageFileAccess.CURRENT_VERSION;

        return new DriveAttachment(id, version);
    }

    private static final String CALLBACK = "callback";

    /**
     * Checks current size of uploaded data against possible quota restrictions.
     *
     * @param size The size
     * @param requestData The associated request data
     * @throws OXException If any quota restrictions are exceeded
     */
    public static void checkSize(final long size, final AJAXRequestData requestData) throws OXException {
        final long maxUploadSize = AttachmentConfig.getMaxUploadSize();
        if (maxUploadSize == 0) {
            return;
        }
        if (size > maxUploadSize) {
            if (!requestData.containsParameter(CALLBACK)) {
                requestData.putParameter(CALLBACK, "error");
            }
            throw UploadSizeExceededException.create(size, maxUploadSize, true);
        }
    }

    /**
     * Performs a roll-back on {@link Attachment#ATTACHMENT_BASE} instance.
     */
    public static void rollback() {
        try {
            Attachment.ATTACHMENT_BASE.rollback();
        } catch (Exception e) {
            LOG.debug("Rollback failed.", e);
        }
    }

    /**
     * Performs finishing stuff on {@link Attachment#ATTACHMENT_BASE} instance.
     */
    public static void finish() {
        try {
            Attachment.ATTACHMENT_BASE.finish();
        } catch (Exception e) {
            LOG.debug("Finishing failed.", e);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------- //

    private static final class AttachmentMetadataImpl implements AttachmentMetadata {

        private int createdBy;
        private Date creationDate;
        private String fileMIMEType;
        private String filename;
        private long filesize;
        private boolean rtfFlag;
        private int objectId;
        private int moduleId;
        private int id;
        private int folderId;
        private String comment;
        private String fileId;
        private AttachmentBatch batch;
        private String checksum;
        private String uri;

        /**
         * Initializes a new {@link AttachmentMetadataImpl}.
         *
         * @param attachedId The object identifier
         * @param moduleId The module identifier; see {@link Types}
         * @param folderId The folder identifier
         */
        AttachmentMetadataImpl(int attachedId, int moduleId, int folderId) {
            super();
            this.objectId = attachedId;
            this.moduleId = moduleId;
            this.folderId = folderId;
        }

        @Override
        public int getCreatedBy() {
            return createdBy;
        }

        @Override
        public void setCreatedBy(final int createdBy) {
            this.createdBy = createdBy;
        }

        @Override
        public Date getCreationDate() {
            return creationDate;
        }

        @Override
        public void setCreationDate(final Date creationDate) {
            this.creationDate = creationDate;
        }

        @Override
        public String getFileMIMEType() {
            return fileMIMEType;
        }

        @Override
        public void setFileMIMEType(final String fileMIMEType) {
            this.fileMIMEType = fileMIMEType;
        }

        @Override
        public String getFilename() {
            return filename;
        }

        @Override
        public void setFilename(final String filename) {
            this.filename = filename;
        }

        @Override
        public long getFilesize() {
            return filesize;
        }

        @Override
        public void setFilesize(final long filesize) {
            this.filesize = filesize;
        }

        @Override
        public int getAttachedId() {
            return objectId;
        }

        @Override
        public void setAttachedId(final int objectId) {
            this.objectId = objectId;
        }

        @Override
        public boolean getRtfFlag() {
            return rtfFlag;
        }

        @Override
        public void setRtfFlag(final boolean rtfFlag) {
            this.rtfFlag = rtfFlag;
        }

        @Override
        public int getModuleId() {
            return moduleId;
        }

        @Override
        public void setModuleId(final int moduleId) {
            this.moduleId = moduleId;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public void setId(final int id) {
            this.id = id;
        }

        @Override
        public void setFolderId(final int folderId) {
            this.folderId = folderId;
        }

        @Override
        public int getFolderId() {
            return folderId;
        }

        @Override
        public void setComment(final String comment) {
            this.comment = comment;
        }

        @Override
        public String getComment() {
            return comment;
        }

        @Override
        public void setFileId(final String fileId) {
            this.fileId = fileId;
        }

        @Override
        public String getFileId() {
            return fileId;
        }

        @Override
        public void setAttachmentBatch(AttachmentBatch batch) {
            this.batch = batch;
        }

        @Override
        public AttachmentBatch getAttachmentBatch() {
            return batch;
        }

        @Override
        public String getChecksum() {
            return checksum;
        }

        @Override
        public void setChecksum(String checksum) {
            this.checksum = checksum;
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public void setUri(String uri) {
            this.uri = uri;
        }
    }

}
