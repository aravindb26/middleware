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

package com.openexchange.mail.compose.json.action;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONServices;
import com.google.common.collect.ImmutableMap;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.Document;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.FileStorageCapability;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.composition.FileID;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.groupware.upload.StreamedUpload;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.java.util.UUIDs;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.compose.Attachment;
import com.openexchange.mail.compose.Attachment.ContentDisposition;
import com.openexchange.mail.compose.AttachmentDescription;
import com.openexchange.mail.compose.AttachmentDescriptionAndData;
import com.openexchange.mail.compose.AttachmentOrigin;
import com.openexchange.mail.compose.AttachmentResult;
import com.openexchange.mail.compose.CompositionSpaceId;
import com.openexchange.mail.compose.CompositionSpaceService;
import com.openexchange.mail.compose.ContentId;
import com.openexchange.mail.compose.DataProvider;
import com.openexchange.mail.compose.UploadLimits;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tx.TransactionAwares;


/**
 * {@link AddAttachmentMailComposeAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class AddAttachmentMailComposeAction extends AbstractMailComposeAction {

    private static interface OriginHandler {

        AttachmentResult addAttachment(JSONObject jAttachment, String disposition, CompositionSpaceId compositionSpaceId, CompositionSpaceService compositionSpaceService, AJAXRequestData requestData) throws OXException, JSONException;
    }

    private final Map<String, OriginHandler> originHandlers;

    /**
     * Initializes a new {@link AddAttachmentMailComposeAction}.
     *
     * @param services The service look-up
     */
    public AddAttachmentMailComposeAction(ServiceLookup services) {
        super(services);

        originHandlers = ImmutableMap.<String, OriginHandler> builderWithExpectedSize(4)
            .put("drive", new OriginHandler() {

                @Override
                public AttachmentResult addAttachment(JSONObject jAttachment, String disposition, CompositionSpaceId compositionSpaceId, CompositionSpaceService compositionSpaceService, AJAXRequestData requestData) throws OXException, JSONException {
                    IDBasedFileAccessFactory fileAccessFactory = services.getOptionalService(IDBasedFileAccessFactory.class);
                    if (null == fileAccessFactory) {
                        throw ServiceExceptionCode.absentService(IDBasedFileAccessFactory.class);
                    }

                    IDBasedFileAccess fileAccess = fileAccessFactory.createAccess(requestData.getSession());
                    try {
                        String version = jAttachment.optString("version", FileStorageFileAccess.CURRENT_VERSION);
                        JSONArray ids = jAttachment.optJSONArray("ids");
                        if (ids == null || ids.length() <= 0) {
                            // Expect single identifier
                            String id = jAttachment.getString("id");
                            AttachmentDescription attachment = new AttachmentDescription();
                            attachment.setCompositionSpaceId(compositionSpaceId.getId());
                            attachment.setContentDisposition(Attachment.ContentDisposition.dispositionFor(disposition));
                            InputStream attachmentData = parseDriveAttachment(attachment, id, version, fileAccess);
                            try {
                                return validateAttachmentResult(compositionSpaceService.addAttachmentToCompositionSpace(
                                    compositionSpaceId.getId(), attachment, attachmentData, getClientToken(requestData)));
                            } finally {
                                Streams.close(attachmentData);
                            }
                        }

                        // Expect multiple identifiers
                        List<AttachmentDescriptionAndData> attachmentsAndDatas = new ArrayList<AttachmentDescriptionAndData>(ids.length());
                        ContentDisposition contentDisposition = Attachment.ContentDisposition.dispositionFor(disposition);
                        for (Object id : ids) {
                            AttachmentDescription attachment = new AttachmentDescription();
                            attachment.setCompositionSpaceId(compositionSpaceId.getId());
                            attachment.setContentDisposition(contentDisposition);
                            attachmentsAndDatas.add(AttachmentDescriptionAndData.newInstanceFromDataProvider(new DriveAttachmentDataProvider(id.toString(), version, attachment, fileAccess), attachment));
                        }
                        return validateAttachmentResult(compositionSpaceService.addAttachmentsToCompositionSpace(
                            compositionSpaceId.getId(), attachmentsAndDatas, getClientToken(requestData)));
                    } finally {
                        TransactionAwares.finishSafe(fileAccess);
                    }
                }
            })
            .put("mail", new OriginHandler() {

                @Override
                public AttachmentResult addAttachment(JSONObject jAttachment, String disposition, CompositionSpaceId compositionSpaceId, CompositionSpaceService compositionSpaceService, AJAXRequestData requestData) throws OXException, JSONException {
                    String mailId = jAttachment.getString("id");
                    String folderId = jAttachment.getString("folderId");
                    String attachmentId = jAttachment.getString("attachmentId");
                    FullnameArgument fullnameArgument = MailFolderUtility.prepareMailFolderParam(folderId);

                    MailServletInterface mailInterface = null;
                    try {
                        mailInterface = MailServletInterface.getInstance(requestData.getSession());
                        mailInterface.openFor(folderId);

                        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = mailInterface.getMailAccess();
                        MailPart mailPart = mailAccess.getMessageStorage().getAttachment(fullnameArgument.getFullName(), mailId, attachmentId);
                        if (mailPart == null) {
                            throw MailExceptionCode.NO_ATTACHMENT_FOUND.create(attachmentId);
                        }

                        AttachmentDescription attachment = new AttachmentDescription();
                        attachment.setCompositionSpaceId(compositionSpaceId.getId());
                        attachment.setContentDisposition(Attachment.ContentDisposition.dispositionFor(disposition));
                        attachment.setMimeType(mailPart.getContentType().getBaseType());
                        attachment.setName(mailPart.getFileName());
                        attachment.setOrigin(AttachmentOrigin.MAIL);
                        AttachmentDescriptionAndData descriptionAndData = AttachmentDescriptionAndData.newInstanceFromDataProvider(new MailAttachmentDataProvider(mailPart), attachment);

                        return validateAttachmentResult(compositionSpaceService.addAttachmentsToCompositionSpace(
                            compositionSpaceId.getId(), Collections.singletonList(descriptionAndData), getClientToken(requestData)));
                    } finally {
                        if (mailInterface != null) {
                            mailInterface.close();
                        }
                    }
                }
            })
            .put("contacts", new OriginHandler() {

                @Override
                public AttachmentResult addAttachment(JSONObject jAttachment, String disposition, CompositionSpaceId compositionSpaceId, CompositionSpaceService compositionSpaceService, AJAXRequestData requestData) throws OXException, JSONException {
                    String contactId = jAttachment.getString("id");
                    String folderId = jAttachment.getString("folderId");
                    return validateAttachmentResult(compositionSpaceService.addContactVCardToCompositionSpace(
                        compositionSpaceId.getId(), contactId, folderId, getClientToken(requestData)));
                }
            })
            .build();
    }

    @Override
    protected AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException, JSONException {
        // Require composition space identifier
        String sId = requestData.requireParameter("id");
        CompositionSpaceId compositionSpaceId = parseCompositionSpaceId(sId);

        // Load composition space
        CompositionSpaceService compositionSpaceService = getCompositionSpaceService(compositionSpaceId.getServiceId(), session);

        // Determine upload quotas
        UploadLimits uploadLimits = compositionSpaceService.getAttachmentUploadLimits(compositionSpaceId.getId());
        boolean hasFileUploads = hasUploads(uploadLimits, requestData);

        StreamedUpload upload = requestData.getStreamedUpload();
        if (null == upload) {
            throw AjaxExceptionCodes.MISSING_REQUEST_BODY.create();
        }

        String disposition = upload.getFormField("contentDisposition");
        if (null == disposition) {
            disposition = ContentDisposition.ATTACHMENT.getId();
        }

        if (hasFileUploads) {
            // File upload available...
            AttachmentResult attachmentResult = validateAttachmentResult(compositionSpaceService.addAttachmentToCompositionSpace(
                compositionSpaceId.getId(), upload.getUploadFiles(), disposition, getClientToken(requestData)));
            return new AJAXRequestResult(attachmentResult, "compositionSpaceAttachment").addWarnings(compositionSpaceService.getWarnings());
        }

        // No file uploads available... Expect a "JSON" form field
        JSONObject jAttachment;
        {
            String expectedJsonContent = upload.getFormField("JSON");
            if (Strings.isEmpty(expectedJsonContent)) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create("JSON");
            }

            jAttachment = JSONServices.parseObject(expectedJsonContent);
        }

        // Determine origin
        String origin = Strings.asciiLowerCase(jAttachment.optString("origin", null));
        if (Strings.isEmpty(origin)) {
            // No origin given
            throw AjaxExceptionCodes.MISSING_PARAMETER.create("origin");
        }

        OriginHandler originHandler = originHandlers.get(origin);
        if (originHandler == null) {
            // Unknown origin given
            throw AjaxExceptionCodes.INVALID_PARAMETER.create("origin");
        }

        AttachmentResult attachmentResult = originHandler.addAttachment(jAttachment, disposition, compositionSpaceId, compositionSpaceService, requestData);
        return new AJAXRequestResult(attachmentResult, "compositionSpaceAttachment").addWarnings(compositionSpaceService.getWarnings());
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    static InputStream parseDriveAttachment(AttachmentDescription attachment, String id, String version, IDBasedFileAccess fileAccess) throws OXException {
        FileID fileID = new FileID(id);
        if (fileAccess.supports(fileID.getService(), fileID.getAccountId(), FileStorageCapability.EFFICIENT_RETRIEVAL)) {
            Document document = fileAccess.getDocumentAndMetadata(id, version);
            if (null != document) {
                try {
                    ContentType contentType = new ContentType(document.getMimeType());
                    if (contentType.startsWith("text/calendar")) {
                        attachment.setMimeType(contentType.toString(true));
                    } else {
                        attachment.setMimeType(contentType.getBaseType());
                    }
                    if (Attachment.ContentDisposition.INLINE == attachment.getContentDisposition() && contentType.startsWith("image/")) {
                        // Set a Content-Id for inline image, too
                        attachment.setContentId(ContentId.valueOf(UUIDs.getUnformattedStringFromRandom() + "@Open-Xchange"));
                    }
                    attachment.setName(document.getName());
                    attachment.setOrigin(AttachmentOrigin.DRIVE);
                    InputStream data = document.getData();
                    document = null;
                    return data;
                } finally {
                    if (null != document && !document.isRepetitive()) {
                        Streams.close(document.getData());
                    }
                }
            }
        }

        File metadata = fileAccess.getFileMetadata(id, version);
        ContentType contentType = new ContentType(metadata.getFileMIMEType());
        attachment.setMimeType(contentType.getBaseType());
        if (Attachment.ContentDisposition.INLINE == attachment.getContentDisposition() && contentType.startsWith("image/")) {
            // Set a Content-Id for inline image, too
            attachment.setContentId(ContentId.valueOf(UUIDs.getUnformattedStringFromRandom() + "@Open-Xchange"));
        }
        attachment.setName(metadata.getFileName());
        attachment.setOrigin(AttachmentOrigin.DRIVE);
        return fileAccess.getDocument(id, version);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class DriveAttachmentDataProvider implements DataProvider {

        private final String id;
        private final String version;
        private final AttachmentDescription attachment;
        private final IDBasedFileAccess fileAccess;

        DriveAttachmentDataProvider(String id, String version, AttachmentDescription attachment, IDBasedFileAccess fileAccess) {
            super();
            this.id = id;
            this.version = version;
            this.attachment = attachment;
            this.fileAccess = fileAccess;
        }

        @Override
        public InputStream getData() throws OXException {
            return parseDriveAttachment(attachment, id, version, fileAccess);
        }
    }

    private static class MailAttachmentDataProvider implements DataProvider {

        private final MailPart attachment;

        MailAttachmentDataProvider(MailPart attachment) {
            super();
            this.attachment = attachment;
        }

        @Override
        public InputStream getData() throws OXException {
            return attachment.getInputStream();
        }
    }

}
