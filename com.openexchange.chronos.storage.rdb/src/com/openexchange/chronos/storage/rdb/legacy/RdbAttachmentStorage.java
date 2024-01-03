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

package com.openexchange.chronos.storage.rdb.legacy;

import static com.openexchange.groupware.tools.mappings.database.DefaultDbMapper.getParameters;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.I2i;
import static com.openexchange.tools.arrays.Collections.put;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.openexchange.chronos.Attachment;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.exception.ProblemSeverity;
import com.openexchange.chronos.storage.AttachmentStorage;
import com.openexchange.chronos.storage.rdb.RdbStorage;
import com.openexchange.chronos.storage.rdb.osgi.Services;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.database.Databases;
import com.openexchange.database.provider.DBProvider;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorages;
import com.openexchange.filestore.Info;
import com.openexchange.filestore.QuotaFileStorage;
import com.openexchange.filestore.QuotaFileStorageService;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentExceptionCodes;
import com.openexchange.groupware.attach.AttachmentField;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.attach.AttachmentMetadataFactory;
import com.openexchange.groupware.attach.Attachments;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Streams;
import com.openexchange.java.Strings;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;

/**
 * {@link RdbAttachmentStorage}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class RdbAttachmentStorage extends RdbStorage implements AttachmentStorage {

    /**
     * Configures a comma-separated list of URI schemes that can be stored for externally linked attachments of appointments.
     * Attachments with other URI schemes will be rejected/ignored during import.
     */
    private static final Property PROPERTY_ALLOWED_ATTACHMENT_SCHEMES = DefaultProperty.valueOf("com.openexchange.calendar.allowedAttachmentSchemes", "http,https,ftp,ftps");

    private static final int MODULE_ID = com.openexchange.groupware.Types.APPOINTMENT;
    private static final AttachmentMetadataFactory METADATA_FACTORY = new AttachmentMetadataFactory();

    /**
     * Initializes a new {@link RdbAttachmentStorage}.
     *
     * @param context The context
     * @param dbProvider The database provider to use
     * @param txPolicy The transaction policy
     */
    public RdbAttachmentStorage(Context context, DBProvider dbProvider, DBTransactionPolicy txPolicy) {
        super(context, dbProvider, txPolicy);
    }

    @Override
    public List<Attachment> loadAttachments(String objectID) throws OXException {
        return loadAttachments(new String[] { objectID }).get(objectID);
    }

    @Override
    public Map<String, List<Attachment>> loadAttachments(String[] objectIDs) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectAttachments(connection, context.getContextId(), objectIDs);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public Set<String> hasAttachments(String[] eventIds) throws OXException {
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            return selectHasAttachments(connection, context.getContextId(), eventIds);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
    }

    @Override
    public void deleteAttachments(Session session, String folderID, List<String> eventIDs) throws OXException {
        ServerSession serverSession = checkSession(session);
        AttachmentBase attachmentBase = initAttachmentBase();
        serverSession.setParameter(AttachmentStorage.class.getName(), Boolean.TRUE);
        try {
            attachmentBase.startTransaction();
            for (String eventID : eventIDs) {
                deleteAttachments(serverSession, attachmentBase, folderID, eventID);
            }
            attachmentBase.commit();
        } finally {
            serverSession.setParameter(AttachmentStorage.class.getName(), null);
            attachmentBase.finish();
        }
    }
    
    @Override
    public void deleteAttachments(Session session, String folderID, String eventID, List<Attachment> attachments) throws OXException {
        if (null == attachments || 0 == attachments.size()) {
            return;
        }
        ServerSession serverSession = checkSession(session);
        AttachmentBase attachmentBase = initAttachmentBase();
        serverSession.setParameter(AttachmentStorage.class.getName(), Boolean.TRUE);
        try {
            attachmentBase.startTransaction();
            List<Integer> attachmentIds = getAttachmentIds(serverSession, attachmentBase, folderID, eventID, attachments);
            attachmentBase.detachFromObject(asInt(folderID), asInt(eventID), MODULE_ID, I2i(attachmentIds),
                session, context, serverSession.getUser(), serverSession.getUserConfiguration());
            attachmentBase.commit();
        } finally {
            serverSession.setParameter(AttachmentStorage.class.getName(), null);
            attachmentBase.finish();
        }
    }

    @Override
    public void insertAttachments(Session session, String folderID, String eventID, List<Attachment> attachments) throws OXException {
        if (null == attachments || 0 == attachments.size()) {
            return;
        }
        ServerSession serverSession = checkSession(session);
        AttachmentBase attachmentBase = initAttachmentBase();
        serverSession.setParameter(AttachmentStorage.class.getName(), Boolean.TRUE);
        try {
            attachmentBase.startTransaction();
            for (Attachment attachment : attachments) {
                if (null != attachment.getData()) {
                    /*
                     * store new binary attachment
                     */
                    AttachmentMetadata metadata = getMetadata(attachment, asInt(folderID), asInt(eventID));
                    metadata.setId(AttachmentBase.NEW);
                    metadata.setUri(null);
                    InputStream inputStream = null;
                    try {
                        inputStream = attachment.getData().getStream();
                        attachmentBase.attachToObject(serverSession, metadata, inputStream);
                    } finally {
                        Streams.close(inputStream);
                    }
                } else if (Strings.isNotEmpty(attachment.getUri())) {
                    /*
                     * insert external attachments (if supported)
                     */
                    if (isAllowedScheme(attachment.getUri())) {
                        AttachmentMetadata metadata = getMetadata(attachment, asInt(folderID), asInt(eventID));
                        metadata.setId(AttachmentBase.NEW);
                        attachmentBase.attachToObject(serverSession, metadata, null);
                    } else {
                        String message = "Unsupported attachment URI '" + attachment.getUri() + '\'';
                        addUnsupportedDataError(eventID, EventField.ATTACHMENTS, ProblemSeverity.MAJOR, message);
                    }
                } else if (0 < attachment.getManagedId()) {
                    /*
                     * copy over referenced managed attachments
                     */
                    AttachmentMetadata metadata = getMetadata(attachment, asInt(folderID), asInt(eventID));
                    metadata.setId(AttachmentBase.NEW);
                    metadata.setUri(null);
                    InputStream inputStream = null;
                    try {
                        inputStream = loadAttachmentData(attachment.getManagedId());
                        attachmentBase.attachToObject(serverSession, metadata, inputStream);
                    } finally {
                        Streams.close(inputStream);
                    }
                } else {
                    addUnsupportedDataError(eventID, EventField.ATTACHMENTS, ProblemSeverity.MAJOR, "Unsupported attachment " + attachment);
                }
            }
            attachmentBase.commit();
        } finally {
            serverSession.setParameter(AttachmentStorage.class.getName(), null);
            attachmentBase.finish();
        }
    }

    @Override
    public InputStream loadAttachmentData(int attachmentID) throws OXException {
        String fileID;
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            fileID = selectFileID(connection, context.getContextId(), attachmentID);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
        if (Strings.isEmpty(fileID)) {
            throw AttachmentExceptionCodes.ATTACHMENT_NOT_FOUND.create();
        }
        return getFileStorage().getFile(fileID);
    }

    @Override
    public String resolveAttachmentId(int managedId) throws OXException {
        String eventId;
        Connection connection = null;
        try {
            connection = dbProvider.getReadConnection(context);
            eventId = selectEventID(connection, context.getContextId(), managedId);
        } catch (SQLException e) {
            throw asOXException(e);
        } finally {
            dbProvider.releaseReadConnection(context, connection);
        }
        if (null == eventId) {
            throw AttachmentExceptionCodes.ATTACHMENT_NOT_FOUND.create();
        }
        return eventId;
    }

    private QuotaFileStorage getFileStorage() throws OXException {
        QuotaFileStorageService storageService = FileStorages.getQuotaFileStorageService();
        if (null == storageService) {
            throw AttachmentExceptionCodes.FILESTORE_DOWN.create();
        }
        return storageService.getQuotaFileStorage(context.getContextId(), Info.general());
    }

    private ServerSession checkSession(Session session) throws OXException {
        if (null == session || session.getContextId() != context.getContextId()) {
            throw new UnsupportedOperationException();
        }
        return ServerSessionAdapter.valueOf(session);
    }

    private AttachmentBase initAttachmentBase() {
        return Attachments.getInstance(dbProvider, true);
    }

    private void deleteAttachments(ServerSession serverSession, AttachmentBase attachmentBase, String folderID, String eventID) throws OXException {
        List<Integer> attachmentIDs = new ArrayList<Integer>();
        SearchIterator<AttachmentMetadata> iterator = null;
        try {
            iterator = getAttachmentMetadata(serverSession, attachmentBase, folderID, eventID, AttachmentField.ID_LITERAL);
            while (iterator.hasNext()) {
                AttachmentMetadata metadata = iterator.next();
                if (metadata != null) {
                    attachmentIDs.add(I(metadata.getId()));
                }
            }
        } finally {
            SearchIterators.close(iterator);
        }
        if (0 < attachmentIDs.size()) {
            attachmentBase.detachFromObject(asInt(folderID), asInt(eventID), MODULE_ID, I2i(attachmentIDs),
                serverSession, context, serverSession.getUser(), serverSession.getUserConfiguration());
        }
    }
    
    private SearchIterator<AttachmentMetadata> getAttachmentMetadata(ServerSession serverSession, AttachmentBase attachmentBase, String folderID, String eventID, AttachmentField... fields) throws OXException {
        AttachmentField[] columns = null == fields || 0 == fields.length ? AttachmentField.VALUES_ARRAY : fields;
        return attachmentBase.getAttachments(serverSession, asInt(folderID), asInt(eventID), MODULE_ID, columns, null, 0,
            context, serverSession.getUser(), serverSession.getUserConfiguration()).results();
    }
    
    private List<Integer> getAttachmentIds(ServerSession serverSession, AttachmentBase attachmentBase, String folderID, String eventID, List<Attachment> attachments) throws OXException {
        /*
         * take over identifiers from managed attachments directly if set
         */
        List<Integer> attachmentIDs = new ArrayList<Integer>(attachments.size());
        List<Attachment> unknownAttachments = new ArrayList<Attachment>();        
        for (Attachment attachment : attachments) {
            if (0 < attachment.getManagedId()) {
                attachmentIDs.add(I(attachment.getManagedId()));
            } else {
                unknownAttachments.add(attachment);
            }
        }
        if (unknownAttachments.isEmpty()) {
            return attachmentIDs;
        }
        /*
         * re-get metadata for unknown attachments & associate the attachment identifiers
         */
        List<AttachmentMetadata> attachmentMetadatas;
        SearchIterator<AttachmentMetadata> iterator = null;
        try {
            iterator = getAttachmentMetadata(serverSession, attachmentBase, folderID, eventID, AttachmentField.ID_LITERAL, AttachmentField.FILE_ID_LITERAL, 
                AttachmentField.URI_LITERAL, AttachmentField.FILE_MIMETYPE_LITERAL, AttachmentField.FILE_SIZE_LITERAL, AttachmentField.CHECKSUM_LITERAL);
            attachmentMetadatas = SearchIterators.asList(iterator);
        } finally {
            SearchIterators.close(iterator);
        }
        for (Attachment unknownAttachment : unknownAttachments) {
            AttachmentMetadata matchingMetadata = find(attachmentMetadatas, unknownAttachment);
            if (null == matchingMetadata) {
                throw CalendarExceptionCodes.ATTACHMENT_NOT_FOUND.create(
                    new Exception("Unknown attachment" + unknownAttachment), I(unknownAttachment.getManagedId()), eventID, folderID);
            }
            attachmentIDs.add(I(matchingMetadata.getId()));
        }
        return attachmentIDs;
    }

    private static Map<String, List<Attachment>> selectAttachments(Connection connection, int contextID, String[] objectIDs) throws SQLException {
        Map<String, List<Attachment>> attachmentsById = new HashMap<String, List<Attachment>>();
        String sql = new StringBuilder()
            .append("SELECT attached,id,file_mimetype,file_size,filename,file_id,creation_date,checksum,uri FROM prg_attachment ")
            .append("WHERE cid=? AND attached IN (").append(getParameters(objectIDs.length)).append(") AND module=?;")
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            for (String objectID : objectIDs) {
                stmt.setInt(parameterIndex++, asInt(objectID));
            }
            stmt.setInt(parameterIndex++, MODULE_ID);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    put(attachmentsById, asString(resultSet.getInt("attached")), readAttachment(resultSet));
                }
            }
        }
        return attachmentsById;
    }

    private static Attachment readAttachment(ResultSet resultSet) throws SQLException {
        Attachment attachment = new Attachment();
        /*
         * differentiate between external attachments (linked via uri) and managed attachments (stored in filestore)  
         */
        String uri = resultSet.getString("uri");
        if (Strings.isNotEmpty(uri) && AttachmentBase.NO_FILE_ID.equals(resultSet.getString("file_id"))) {
            /*
             * external attachment linked via uri 
             */
            attachment.setUri(uri);            
        } else {
            /*
             * managed attachment stored in filestore
             */
            attachment.setManagedId(resultSet.getInt("id"));
        }
        attachment.setFormatType(resultSet.getString("file_mimetype"));
        attachment.setSize(resultSet.getLong("file_size"));
        attachment.setFilename(resultSet.getString("filename"));
        attachment.setCreated(new Date(resultSet.getLong("creation_date")));
        attachment.setChecksum(resultSet.getString("checksum"));
        return attachment;
    }

    private static Set<String> selectHasAttachments(Connection connection, int contextID, String[] eventIds) throws SQLException {
        if (null == eventIds || 0 == eventIds.length) {
            return Collections.emptySet();
        }
        Set<String> eventIdsWithAttachment = new HashSet<String>();
        String sql = new StringBuilder()
            .append("SELECT DISTINCT(attached) FROM prg_attachment ")
            .append("WHERE cid=? AND module=? AND attached").append(Databases.getPlaceholders(eventIds.length)).append(';')
        .toString();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int parameterIndex = 1;
            stmt.setInt(parameterIndex++, contextID);
            stmt.setInt(parameterIndex++, MODULE_ID);
            for (String id : eventIds) {
                stmt.setInt(parameterIndex++, asInt(id));
            }
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                while (resultSet.next()) {
                    eventIdsWithAttachment.add(String.valueOf(resultSet.getInt("attached")));
                }
            }
        }
        return eventIdsWithAttachment;
    }

    private static String selectFileID(Connection connection, int contextID, int attachmentID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT file_id FROM prg_attachment WHERE cid=? AND id=? AND module=?;")) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, attachmentID);
            stmt.setInt(3, MODULE_ID);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                if (resultSet.next()) {
                    String value = resultSet.getString(1);
                    return AttachmentBase.NO_FILE_ID.equals(value) ? null : value;
                }
                return null;
            }
        }
    }

    private static String selectEventID(Connection connection, int contextID, int attachmentID) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT attached FROM prg_attachment WHERE cid=? AND id=? AND module=?;")) {
            stmt.setInt(1, contextID);
            stmt.setInt(2, attachmentID);
            stmt.setInt(3, MODULE_ID);
            try (ResultSet resultSet = logExecuteQuery(stmt)) {
                return resultSet.next() ? String.valueOf(resultSet.getInt(1)) : null;
            }
        }
    }

    private static AttachmentMetadata getMetadata(Attachment attachment, int folderID, int eventID) {
        AttachmentMetadata metadata = METADATA_FACTORY.newAttachmentMetadata();
        metadata.setModuleId(MODULE_ID);
        metadata.setId(attachment.getManagedId());
        metadata.setFolderId(folderID);
        metadata.setAttachedId(eventID);
        if (null != attachment.getFormatType()) {
            metadata.setFileMIMEType(attachment.getFormatType());
        } else if (null != attachment.getData()) {
            metadata.setFileMIMEType(attachment.getData().getContentType());
        }
        if (null != attachment.getFilename()) {
            metadata.setFilename(attachment.getFilename());
        } else if (null != attachment.getData()) {
            metadata.setFilename(attachment.getData().getName());
        }
        if (0 < attachment.getSize()) {
            metadata.setFilesize(attachment.getSize());
        } else if (null != attachment.getData()) {
            metadata.setFilesize(attachment.getData().getLength());
        }
        metadata.setChecksum(attachment.getChecksum());
        metadata.setUri(attachment.getUri());
        return metadata;
    }

    private static boolean matches(AttachmentMetadata attachmentMetadata, Attachment attachment) {
        if (0 < attachment.getManagedId() && attachment.getManagedId() == attachmentMetadata.getId()) {
            return true;
        }
        if (null != attachment.getUri() && AttachmentBase.NO_FILE_ID.equals(attachmentMetadata.getFileId()) && 
            Objects.equals(attachment.getUri(), attachmentMetadata.getUri())) {
            return true;
        }
        return false;
    }
    
    private static AttachmentMetadata find(List<AttachmentMetadata> attachmentMetadatas, Attachment attachment) {
        if (null != attachmentMetadatas) {
            for (AttachmentMetadata attachmentMetadata : attachmentMetadatas) {
                if (matches(attachmentMetadata, attachment)) {
                    return attachmentMetadata;
                }
            }        
        }
        return null;
    }
    
    /**
     * Gets a value indicating whether an attachment URI is using one of the schemes defined in
     * {@link #PROPERTY_ALLOWED_ATTACHMENT_SCHEMES} or not, i.e. whether the externally linked attachment can be saved.
     * 
     * @param uri The attachment URI to check
     * @return <code>true</code> if the URI uses one of the supported schemes, <code>false</code>, otherwise
     */
    private boolean isAllowedScheme(String uri) {
        String allowedSchemes;
        try {
            LeanConfigurationService configurationService = Services.get().getServiceSafe(LeanConfigurationService.class);
            allowedSchemes = configurationService.getProperty(-1, context.getContextId(), PROPERTY_ALLOWED_ATTACHMENT_SCHEMES);
        } catch (OXException e) {
            LOG.warn("Error getting configuration for \"{}\", falling back to defaults.", PROPERTY_ALLOWED_ATTACHMENT_SCHEMES.getFQPropertyName(), e);
            allowedSchemes = PROPERTY_ALLOWED_ATTACHMENT_SCHEMES.getDefaultValue(String.class);
        }
        String scheme;
        try {
            scheme = new URI(uri).getScheme();
        } catch (URISyntaxException e) {
            LOG.debug("Unable to get scheme from URI \"{}\", considering as not allowed scheme.", uri, e);
            return false;
        }
        return Strings.splitByComma(allowedSchemes, new HashSet<String>()).contains(scheme);
    }

}
