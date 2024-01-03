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

package com.openexchange.groupware.attach.json.actions;

import static com.openexchange.java.Autoboxing.I;

import java.io.*;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.Attachment;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.conversion.ConversionService;
import com.openexchange.conversion.Data;
import com.openexchange.conversion.DataArguments;
import com.openexchange.conversion.DataProperties;
import com.openexchange.conversion.DataSource;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.composition.IDBasedFileAccess;
import com.openexchange.file.storage.composition.IDBasedFileAccessFactory;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentBatch;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.attach.AttachmentExceptionCodes;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.attach.AttachmentUtility;
import com.openexchange.groupware.attach.DriveAttachment;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.upload.UploadFile;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.restricted.RestrictedAccessCheck;
import com.openexchange.session.restricted.Scope;
import com.openexchange.tools.RestrictedActionUtil;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tx.TransactionAwares;
import com.openexchange.user.User;

/**
 * {@link AttachAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
@RestrictedAction(hasCustomRestrictedAccessCheck = true)
public final class AttachAction extends AbstractAttachmentAction {

    private static final String DATASOURCE = "datasource";
    private static final String IDENTIFIER = "identifier";
    private static final String URI = "uri";

    private static final int CHUNK_SIZE = 65536;

    public static transient final AttachmentField[] REQUIRED = Attachment.REQUIRED;

    /**
     * Initializes a new {@link AttachAction}.
     *
     * @param serviceLookup The service lookup instance
     */
    public AttachAction(ServiceLookup serviceLookup) {
        super(serviceLookup);
    }

    @Override
    public AJAXRequestResult doPerform(AJAXRequestData requestData, ServerSession session) throws OXException {
        try {
            User user = session.getUser();
            UserConfiguration userConfiguration = session.getUserConfiguration();
            long maxUploadSize = AttachmentConfig.getMaxUploadSize();
            // Mixed mode
            if (requestData.hasUploads(-1, maxUploadSize > 0 ? maxUploadSize : -1L)) {
                AttachmentInfo attachmentInfo = parseAttachments(requestData);
                return attach(attachmentInfo.attachments, attachmentInfo.uploadFiles, attachmentInfo.driveFiles, session, session.getContext(), user, userConfiguration, requestData);
            }

            // Drive mode
            AttachmentInfo attachmentInfo = parseAttachments(requestData);
            if (attachmentInfo.driveFiles.size() > 0) {
                return attach(attachmentInfo.attachments, attachmentInfo.uploadFiles, attachmentInfo.driveFiles, session, session.getContext(), user, userConfiguration, requestData);
            }

            // PUT method (Note that the PUT method is NOT officially documented
            JSONObject object = getJSONObject(requestData);
            if (object == null) {
                return new AJAXRequestResult(I(0), new Date(System.currentTimeMillis()), "int");
            }

            AttachmentMetadata attachment = parseAttachmentMetadata(object);
            ATTACHMENT_BASE.startTransaction();
            long ts;
            InputStream inputStream = null;
            IDBasedFileAccess fileAccess = null;
            try {
                if (object.has(URI)) {
                    fileAccess = serviceLookup.getServiceSafe(IDBasedFileAccessFactory.class).createAccess(session);
                    File file = getFileMetadata(fileAccess, object.getString(URI));
                    inputStream = fileAccess.getDocument(file.getId(), file.getVersion());
                    if (Strings.isEmpty(attachment.getFilename())) {
                        attachment.setFilename(file.getFileName());
                    }
                } else {
                    inputStream = getInputStream(session, object, attachment);
                    if (attachment.getFilename() == null) {
                        attachment.setFilename("unknown" + System.currentTimeMillis());
                    }
                }
                attachment.setId(AttachmentBase.NEW);
                ts = ATTACHMENT_BASE.attachToObject(attachment, inputStream, session, session.getContext(), user, userConfiguration);
                ATTACHMENT_BASE.commit();
            } catch (OXException x) {
                ATTACHMENT_BASE.rollback();
                throw x;
            } finally {
                Streams.close(inputStream);
                TransactionAwares.finishSafe(fileAccess);
                ATTACHMENT_BASE.finish();
            }
            return new AJAXRequestResult(I(attachment.getId()), new Date(ts), "int");
        } catch (JSONException e) {
            throw AjaxExceptionCodes.JSON_ERROR.create(e, e.getMessage());
        } catch (RuntimeException e) {
            throw AjaxExceptionCodes.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    /**
     * Gets the file metadata referenced as referenced from an attachment URI with <code>drive://</code> scheme.
     * 
     * @param fileAccess The initialized file access to use
     * @param uri The attachment URI to get the referenced file metadata for
     * @return The file metadata
     */
    private static File getFileMetadata(IDBasedFileAccess fileAccess, String uri) throws OXException {
        try {
            DriveAttachment driveAttachment = AttachmentUtility.getDriveAttachmentFromUri(uri);
            return fileAccess.getFileMetadata(driveAttachment.getId(), driveAttachment.getVersion());
        } catch (IllegalArgumentException e) {
            throw AttachmentExceptionCodes.ATTACH_FAILED.create(e);
        }
    }

    /**
     * Takes over metadata properties found in the supplied {@link File} reference in the attachment metadata object, unless already set.
     * 
     * @param attachment The attachment metadata to enrich
     * @param file The file to take over the metadata properties from
     */
    private static void applyFileMetadata(AttachmentMetadata attachment, File file) {
        if (null == file || null == attachment) {
            return;
        }
        if (Strings.isEmpty(attachment.getFileMIMEType()) && Strings.isNotEmpty(file.getFileMIMEType())) {
            attachment.setFileMIMEType(file.getFileMIMEType());
        }
        if (Strings.isEmpty(attachment.getFilename()) && Strings.isNotEmpty(file.getFileName())) {
            attachment.setFilename(file.getFileName());
        }
        if (Strings.isEmpty(attachment.getFilename()) && Strings.isNotEmpty(file.getTitle())) {
            attachment.setFilename(file.getTitle());
        }
        if (0 >= attachment.getFilesize() && 0 < file.getFileSize()) {
            attachment.setFilesize(file.getFileSize());
        }
    }

    /**
     * Retrieves the {@link InputStream} for the file that is to be uploaded and attached to the object
     *
     * @param session The session
     * @param object The {@link JSONObject} containing the file information
     * @param attachment the attachment metadata
     * @return the {@link InputStream}
     * @throws OXException if an invalid data source is supplied, or vital services are missing
     * @throws JSONException if a JSON error is occurred
     */
    private InputStream getInputStream(ServerSession session, JSONObject object, AttachmentMetadata attachment) throws OXException, JSONException {
        ConversionService conversionService = ServerServiceRegistry.getInstance().getService(ConversionService.class);

        if (conversionService == null) {
            throw ServiceExceptionCode.SERVICE_UNAVAILABLE.create(ConversionService.class.getName());
        }

        JSONObject datasourceDef = object.getJSONObject(DATASOURCE);
        String datasourceIdentifier = datasourceDef.getString(IDENTIFIER);

        DataSource source = conversionService.getDataSource(datasourceIdentifier);
        if (source == null) {
            throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create("datasource", datasourceIdentifier);
        }

        List<Class<?>> types = Arrays.asList(source.getTypes());
        Map<String, String> arguments = new HashMap<>();
        for (String key : datasourceDef.keySet()) {
            arguments.put(key, datasourceDef.getString(key));
        }

        if (types.contains(InputStream.class)) {
            Data<InputStream> data = source.getData(InputStream.class, new DataArguments(arguments), session);
            String sizeS = data.getDataProperties().get(DataProperties.PROPERTY_SIZE);
            String contentTypeS = data.getDataProperties().get(DataProperties.PROPERTY_CONTENT_TYPE);

            if (sizeS != null) {
                attachment.setFilesize(Long.parseLong(sizeS));
            }

            if (contentTypeS != null) {
                attachment.setFileMIMEType(contentTypeS);
            }

            String name = data.getDataProperties().get(DataProperties.PROPERTY_NAME);
            if (name != null && null == attachment.getFilename()) {
                attachment.setFilename(name);
            }

            return data.getData();

        }
        if (types.contains(byte[].class)) {
            Data<byte[]> data = source.getData(byte[].class, new DataArguments(arguments), session);
            byte[] bytes = data.getData();
            InputStream is = new ByteArrayInputStream(bytes);
            attachment.setFilesize(bytes.length);

            String contentTypeS = data.getDataProperties().get(DataProperties.PROPERTY_CONTENT_TYPE);
            if (contentTypeS != null) {
                attachment.setFileMIMEType(contentTypeS);
            }

            String name = data.getDataProperties().get(DataProperties.PROPERTY_NAME);
            if (name != null && null == attachment.getFilename()) {
                attachment.setFilename(name);
            }
            return is;
        }
        throw AjaxExceptionCodes.INVALID_PARAMETER_VALUE.create("datasource", datasourceIdentifier);
    }

    /**
     * Returns the JSON object payload from the request data
     *
     * @param requestData The request data
     * @return The JSON object payload or <code>null</code> if no payload is present
     * @throws OXException if a required parameter is missing from the payload
     */
    private JSONObject getJSONObject(AJAXRequestData requestData) throws OXException {
        JSONObject object = (JSONObject) requestData.getData();
        if (object == null) {
            return null;
        }
        // Check if base attributes are set
        for (AttachmentField required : Attachment.REQUIRED) {
            if (!object.has(required.getName())) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create(required.getName());
            }
        }
        // Check for drive upload
        if (object.has(URI)) {
            return object;
        }
        // Check for data-source upload
        if (!object.has(DATASOURCE)) {
            throw AjaxExceptionCodes.MISSING_PARAMETER.create(DATASOURCE);
        }
        return object;
    }

    /**
     * Parses and returns the attachment information from the specified request data
     *
     * @param requestData the request data
     * @return The attachment information
     * @throws JSONException if a JSON error is occurred
     * @throws OXException If an error is occurred
     */
    private AttachmentInfo parseAttachments(AJAXRequestData requestData) throws JSONException, OXException {
        UploadEvent upload = requestData.getUploadEvent();
        List<AttachmentMetadata> attachments = new LinkedList<>();
        List<UploadFile> uploadFiles = new LinkedList<>();

        long sum = 0;
        JSONObject json = new JSONObject();
        List<UploadFile> l = upload.getUploadFiles();
        int size = l.size();
        Iterator<UploadFile> iter = l.iterator();
        Set<Integer> localFileIndexes = new HashSet<>();
        for (int a = 0; a < size; a++) {
            UploadFile uploadFile = iter.next();
            String fileField = uploadFile.getFieldName();
            int index = Integer.parseInt(fileField.substring(5));
            String obj = upload.getFormField("json_" + index);
            if (obj == null || obj.length() == 0) {
                continue;
            }
            json.reset();
            json.parseJSONString(obj);
            for (AttachmentField required : REQUIRED) {
                if (!json.has(required.getName())) {
                    throw AjaxExceptionCodes.MISSING_PARAMETER.create(required.getName());
                }
            }

            AttachmentMetadata attachment = parseAttachmentMetadata(json);
            AttachmentUtility.assureSize(index, attachments, uploadFiles);

            attachments.set(index, attachment);
            uploadFiles.set(index, uploadFile);
            sum += uploadFile.getSize();

            AttachmentUtility.checkSize(sum, requestData);
            localFileIndexes.add(I(index));
        }

        List<AttachmentMetadata> driveFiles = new LinkedList<>();
        Iterator<String> nameIter = upload.getFormFieldNames();
        while (nameIter.hasNext()) {
            String fieldName = nameIter.next();
            String obj = upload.getFormField(fieldName);
            if (obj == null || obj.length() == 0) {
                continue;
            }
            String[] split = fieldName.split("_");
            if (split.length != 2) {
                continue;
            }
            try {
                if (localFileIndexes.contains(Integer.valueOf(split[1]))) {
                    continue;
                }
            } catch (NumberFormatException e) {
                throw AjaxExceptionCodes.BAD_REQUEST.create(e);
            }
            json.reset();
            json.parseJSONString(obj);
            if (!json.has(URI)) {
                throw AjaxExceptionCodes.MISSING_PARAMETER.create(URI);
            }
            driveFiles.add(parseAttachmentMetadata(json));
        }

        return new AttachmentInfo(attachments, uploadFiles, driveFiles);
    }

    private AJAXRequestResult attach(List<AttachmentMetadata> attachments, List<UploadFile> uploadFiles, List<AttachmentMetadata> driveFiles, ServerSession session, Context ctx, User user, UserConfiguration userConfig, AJAXRequestData requestData) throws OXException {
        AttachmentUtility.initAttachments(attachments, uploadFiles);
        boolean rollback = false;
        try {
            ATTACHMENT_BASE.startTransaction();
            rollback = true;

            Iterator<UploadFile> ufIter = uploadFiles.iterator();
            JSONArray arr = new JSONArray();
            long timestamp = 0;
            UUID batchId = UUID.randomUUID();

            // Upload attachments
            timestamp = processUploadedAttachments(attachments, driveFiles, session, ctx, user, userConfig, requestData, ufIter, arr, timestamp, batchId);
            // Drive attachments
            timestamp = processDriveAttachments(driveFiles, session, ctx, user, userConfig, requestData, arr, timestamp, batchId);

            ATTACHMENT_BASE.commit();
            rollback = false;
            return new AJAXRequestResult(arr, new Date(timestamp), "json");
        } catch (IOException e) {
            throw AjaxExceptionCodes.IO_ERROR.create(e, e.getMessage());
        } finally {
            if (rollback) {
                AttachmentUtility.rollback();
            }
            AttachmentUtility.finish();
        }
    }

    /**
     * Processes the uploaded attachments
     */
    private static long processUploadedAttachments(List<AttachmentMetadata> attachments, List<AttachmentMetadata> driveFiles, ServerSession session, Context ctx, User user, UserConfiguration userConfig, AJAXRequestData requestData, Iterator<UploadFile> ufIter, JSONArray arr, long timestamp, UUID batchId) throws OXException, FileNotFoundException {
        long sum = 0;
        Iterator<AttachmentMetadata> iterator = attachments.iterator();
        while (iterator.hasNext()) {
            AttachmentMetadata attachment = iterator.next();
            UploadFile uploadFile = ufIter.next();

            attachment.setId(AttachmentBase.NEW);

            sum += uploadFile.getSize();
            AttachmentUtility.checkSize(sum, requestData);

            AttachmentBatch batch = new AttachmentBatch(batchId, !iterator.hasNext() && driveFiles.isEmpty());
            attachment.setAttachmentBatch(batch);

            BufferedInputStream data = new BufferedInputStream(new FileInputStream(uploadFile.getTmpFile()), CHUNK_SIZE);
            long modified = ATTACHMENT_BASE.attachToObject(attachment, data, session, ctx, user, userConfig);
            if (modified > timestamp) {
                timestamp = modified;
            }
            arr.put(attachment.getId());
        }
        return timestamp;
    }

    /**
     * Processes the drive attachments
     */
    private long processDriveAttachments(List<AttachmentMetadata> driveFiles, ServerSession session, Context ctx, User user, UserConfiguration userConfig, AJAXRequestData requestData, JSONArray arr, long timestamp, UUID batchId) throws OXException, IOException {
        Iterator<AttachmentMetadata> iterator;
        if (driveFiles.isEmpty()) {
            return timestamp;
        }
        IDBasedFileAccess fileAccess = null;
        try {
            fileAccess = serviceLookup.getServiceSafe(IDBasedFileAccessFactory.class).createAccess(session);
            iterator = driveFiles.iterator();
            while (iterator.hasNext()) {
                AttachmentMetadata attachment = iterator.next();
                File file = getFileMetadata(fileAccess, attachment.getUri());
                AttachmentUtility.checkSize(file.getFileSize(), requestData);
                applyFileMetadata(attachment, file);
                attachment.setId(AttachmentBase.NEW);
                attachment.setAttachmentBatch(new AttachmentBatch(batchId, !iterator.hasNext()));
                try (InputStream data = Streams.bufferedInputStreamFor(fileAccess.getDocument(file.getId(), file.getVersion()))) {
                    long modified = ATTACHMENT_BASE.attachToObject(attachment, data, session, ctx, user, userConfig);
                    if (modified > timestamp) {
                        timestamp = modified;
                    }
                }
                arr.put(attachment.getId());
            }
        } finally {
            TransactionAwares.finishSafe(fileAccess);
        }
        return timestamp;
    }

    @RestrictedAccessCheck
    public boolean accessAllowed(AJAXRequestData request, ServerSession session, Scope scope) throws OXException, JSONException {
        long maxUploadSize = AttachmentConfig.getMaxUploadSize();
        if (request.hasUploads(-1, maxUploadSize > 0 ? maxUploadSize : -1L)) {
            return RestrictedActionUtil.mayWriteWithScope(collectContentTypes(parseAttachments(request)), scope);
        }

        JSONObject object = (JSONObject) request.getData();
        if (object == null) {
            return false;
        }
        AttachmentMetadata metadata = parseAttachmentMetadata(object);
        return RestrictedActionUtil.mayWriteWithScope(getContentType(metadata.getModuleId()), scope);
    }

    /**
     * Collects all folder content types from the request
     *
     * @return A Set with all content types from the request
     */
    private Set<ContentType> collectContentTypes(AttachmentInfo attachmentInfos) {
        short items = 0;
        Set<ContentType> contentTypes = new HashSet<>(4);
        for (AttachmentMetadata attachment : attachmentInfos.attachments) {
            if (!contentTypes.add(getContentType(attachment.getModuleId()))) {
                continue;
            }
            items++;
            if (items == ACCEPTED_OAUTH_MODULES.size()) {
                break;
            }
        }
        return contentTypes;
    }

    private static class AttachmentInfo {

        final List<AttachmentMetadata> attachments;
        final List<UploadFile> uploadFiles;
        final List<AttachmentMetadata> driveFiles;

        /**
         * Initialises a new {@link AttachAction.AttachmentInfo}.
         */
        public AttachmentInfo(List<AttachmentMetadata> attachments, List<UploadFile> uploadFiles, List<AttachmentMetadata> driveFiles) {
            super();
            this.attachments = attachments;
            this.uploadFiles = uploadFiles;
            this.driveFiles = driveFiles;
        }
    }

}
