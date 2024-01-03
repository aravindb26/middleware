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

package com.openexchange.file.storage.infostore.internal;

import static com.openexchange.file.storage.FileStorageUtility.checkUrl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.AdministrativeFileStorageFileAccess;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageFileAccess.IDTuple;
import com.openexchange.file.storage.FileStorageFileAccess.SortDirection;
import com.openexchange.file.storage.Range;
import com.openexchange.file.storage.infostore.FileMetadata;
import com.openexchange.file.storage.infostore.InfostoreAccess;
import com.openexchange.file.storage.infostore.InfostoreFile;
import com.openexchange.file.storage.infostore.osgi.Services;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.InfostoreFacades;
import com.openexchange.groupware.results.TimedResult;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.user.User;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserPermissionService;


/**
 * An {@link AdministrativeFileStorageFileAccess} implementation of the infostore.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.8.0
 */
public class AdministrativeInfostoreFileAccess extends InfostoreAccess implements AdministrativeFileStorageFileAccess {

    private final Context context;

    public AdministrativeInfostoreFileAccess(InfostoreFacade infostore, Context context) {
        super(infostore);
        this.context = context;
    }

    @Override
    public File getFileMetadata(String folderId, String id, String version) throws OXException {
        try {
            DocumentMetadata documentMetadata = getInfostore(folderId).getDocumentMetadata(ID(id), VERSION(version), context);
            if (null != folderId && documentMetadata.getFolderId() > 0 && !folderId.equals(Long.toString(documentMetadata.getFolderId()))) {
                throw FileStorageExceptionCodes.FILE_NOT_FOUND.create(id, folderId);
            }

            return new InfostoreFile(documentMetadata);
        } catch (NumberFormatException e) {
            throw FileStorageExceptionCodes.FILE_NOT_FOUND.create(e, id, folderId);
        }
    }

    @Override
    public void saveFileMetadata(File file, long sequenceNumber, List<Field> modifiedFields) throws OXException {
        if (modifiedFields.contains(Field.URL)) {
            checkUrl(file);
        }

        InfostoreFacade infostoreFacade = getInfostore(file.getFolderId());
        infostoreFacade.startTransaction();
        boolean committed = false;
        try {
            infostoreFacade.saveDocumentMetadata(new FileMetadata(file), sequenceNumber, FieldMapping.getMatching(modifiedFields), context);
            infostoreFacade.commit();
            committed = true;
        } finally {
            if (!committed) {
                infostoreFacade.rollback();
            }
            infostoreFacade.finish();
        }
    }

    @Override
    public void removeDocument(String folderId, String id) throws OXException {
        InfostoreFacade infostoreFacade = getInfostore(folderId);
        infostoreFacade.startTransaction();
        boolean committed = false;
        try {
            infostoreFacade.removeDocuments(Collections.singletonList(new IDTuple(folderId, id)), context);
            infostoreFacade.commit();
            committed = true;
        } finally {
            if (!committed) {
                infostoreFacade.rollback();
            }
            infostoreFacade.finish();
        }
    }

    @Override
    public void removeDocuments(List<IDTuple> ids) throws OXException {
        Map<String, List<IDTuple>> idsByFolder = new HashMap<String, List<IDTuple>>(ids.size());
        for (IDTuple tuple : ids) {
            String folderId = tuple.getFolder();
            List<IDTuple> fileIds = idsByFolder.get(folderId);
            if (fileIds == null) {
                fileIds = new LinkedList<IDTuple>();
                idsByFolder.put(folderId, fileIds);
            }

            fileIds.add(tuple);
        }

        List<InfostoreFacade> openedFacades = new ArrayList<InfostoreFacade>(idsByFolder.size());
        boolean allCommitted = false;
        try {
            for (Entry<String, List<IDTuple>> entry : idsByFolder.entrySet()) {
                InfostoreFacade infostoreFacade = getInfostore(entry.getKey());
                infostoreFacade.startTransaction();
                openedFacades.add(infostoreFacade);
                infostoreFacade.removeDocuments(entry.getValue(), context);
                infostoreFacade.commit();
            }
            allCommitted = true;
        } finally {
            if (!allCommitted) {
                for (InfostoreFacade infostoreFacade : openedFacades) {
                    InfostoreFacades.rollback(infostoreFacade);
                }
            }
            for (InfostoreFacade infostoreFacade : openedFacades) {
                InfostoreFacades.finish(infostoreFacade);
            }
        }
    }

    @Override
    public boolean exists(String folderId, String id, String version) throws OXException {
        return getInfostore(folderId).exists(ID(id), VERSION(version), context);
    }

    @Override
    public boolean canRead(String folderId, String id, int userId) throws OXException {
        User user = Services.getService(UserService.class).getUser(userId, context);
        return getInfostore(folderId).hasDocumentAccess(ID(id), InfostoreFacade.AccessPermission.READ, user, context);
    }

    @Override
    public boolean canWrite(String folderId, String id, int userId) throws OXException {
        User user = Services.getService(UserService.class).getUser(userId, context);
        return getInfostore(folderId).hasDocumentAccess(ID(id), InfostoreFacade.AccessPermission.WRITE, user, context);
    }

    @Override
    public boolean canDelete(String folderId, String id, int userId) throws OXException {
        User user = Services.getService(UserService.class).getUser(userId, context);
        return getInfostore(folderId).hasDocumentAccess(ID(id), InfostoreFacade.AccessPermission.DELETE, user, context);
    }

    @Override
    public TimedResult<File> getDocuments(String folderId, int userId, List<Field> fields, Field sort, SortDirection order, Range range) throws OXException {
        User user = Services.getService(UserService.class).getUser(userId, context);
        UserPermissionBits permissionBits = Services.getService(UserPermissionService.class).getUserPermissionBits(userId, context);
        TimedResult<DocumentMetadata> timedResult = getInfostore(folderId).getDocuments(ID(folderId), FieldMapping.getMatching(fields),
            FieldMapping.getMatching(sort), FieldMapping.getSortDirection(order), null != range ? range.from : -1, null != range ? range.to : -1, context, user, permissionBits);
        return new InfostoreTimedResult(timedResult);
    }

    @Override
    public void touch(String folderId, String id) throws OXException {
        getInfostore(folderId).touch(ID(id), context);
    }

}
