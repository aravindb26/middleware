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

package com.openexchange.deputy.provider.folderservice;

import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import com.openexchange.deputy.DefaultModulePermission;
import com.openexchange.deputy.DefaultPermission;
import com.openexchange.deputy.DefaultPermission.Builder;
import com.openexchange.deputy.provider.folderservice.utils.DatabaseFolderUtils;
import com.openexchange.deputy.DeputyExceptionCode;
import com.openexchange.deputy.DeputyInfo;
import com.openexchange.deputy.DeputyModuleProvider;
import com.openexchange.deputy.KnownModuleId;
import com.openexchange.deputy.ModulePermission;
import com.openexchange.deputy.Permission;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.BasicPermission;
import com.openexchange.folderstorage.CalculatePermission;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderPermissionType;
import com.openexchange.folderstorage.FolderService;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.UserizedFolder;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.groupware.userconfiguration.UserPermissionBitsStorage;
import com.openexchange.java.Reference;
import com.openexchange.java.Strings;
import com.openexchange.log.LogProperties;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.User;
import com.openexchange.user.UserService;

/**
 * {@link DeputyFolderServiceProvider} - The folder provider for deputy permission.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.6
 */
public class DeputyFolderServiceProvider implements DeputyModuleProvider {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DeputyFolderServiceProvider.class);
    }

    /** The implementation version of the mail provider for deputy permission */
    public static final int VERSION = 1;

    private final KnownModuleId moduleId;
    private final ContentType contentType;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link DeputyFolderServiceProvider}.
     *
     * @param moduleId The module identifier
     * @param services The service look-up
     * @throws IllegalArgumentException If given module identifier is not supported
     */
    public DeputyFolderServiceProvider(KnownModuleId moduleId, ServiceLookup services) {
        super();
        this.moduleId = moduleId;
        contentType = getContentType(moduleId);
        this.services = services;
    }

    private UserPermissionBits getPermissionBits(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getUserPermissionBits();
        }
        return UserPermissionBitsStorage.getInstance().getUserPermissionBits(session.getUserId(), session.getContextId());
    }

    private User getUser(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getUser();
        }
        return UserStorage.getInstance().getUser(session.getUserId(), session.getContextId());
    }

    private Context getContext(Session session) throws OXException {
        if (session instanceof ServerSession) {
            return ((ServerSession) session).getContext();
        }
        return ContextStorage.getInstance().getContext(session.getContextId());
    }

    private boolean existsEntity(DeputyInfo deputyInfo, Context context) throws OXException {
        int entityId = deputyInfo.getEntityId();
        boolean group = deputyInfo.isGroup();

        if (group) {
            return services.getServiceSafe(GroupService.class).exists(context, entityId);
        }
        return services.getServiceSafe(UserService.class).exists(entityId, context.getContextId());
    }

    @Override
    public String getModuleId() {
        return moduleId.getId();
    }

    @Override
    public boolean isApplicable(Optional<ModulePermission> optionalModulePermission, Session session) throws OXException {
        switch (moduleId) {
            case CALENDAR:
                return getPermissionBits(session).hasCalendar();
            case CONTACTS:
                return getPermissionBits(session).hasContact();
            case DRIVE:
                return getPermissionBits(session).hasInfostore();
            case TASKS:
                return getPermissionBits(session).hasTask();
            default:
                return false;

        }
    }

    @Override
    public void grantDeputyPermission(DeputyInfo deputyInfo, ModulePermission modulePermission, Session session) throws OXException {
        Permission permission = modulePermission.getPermission();
        if (permission.isVisible() == false) {
            // Invalid permissions shall be applied
            return;
        }

        FolderService folderService = services.getOptionalService(FolderService.class);
        if (null == folderService) {
            throw ServiceExceptionCode.absentService(FolderService.class);
        }

        List<String> folderIds = determineFolderIds(modulePermission, folderService, session);
        grantDeputyPermissionByFolders(folderIds, deputyInfo, permission, session, folderService);
    }

    private void grantDeputyPermissionByFolders(Collection<? extends String> folderIds, DeputyInfo deputyInfo, Permission permission, Session session, FolderService folderService) throws OXException {
        Context context = getContext(session);
        User user = getUser(session);

        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(folderIds.size());
        for (String folderId : folderIds) {
            UserizedFolder folder = folderService.getFolder(FolderStorage.REAL_TREE_ID, folderId, session, new FolderServiceDecorator());

            com.openexchange.folderstorage.Permission ownPermission = CalculatePermission.calculate(folder, ServerSessionAdapter.valueOf(session), Collections.singletonList(contentType));
            if (!ownPermission.isVisible()) {
                throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(folderId, Integer.toString(user.getId()), Integer.toString(context.getContextId()));
            }
            if (!ownPermission.isAdmin()) {
                throw FolderExceptionErrorMessage.INVALID_PERMISSIONS.create(I(com.openexchange.folderstorage.Permissions.createPermissionBits(ownPermission)), I(ownPermission.getEntity()), folder.getID() == null ? folder.getName() : folder.getID());
            }

            Map<String, Object> meta = folder.getMeta();
            if (meta != null) {
                for (Map.Entry<String, Object> metaEntry : meta.entrySet()) {
                    Object value = metaEntry.getValue();
                    if (value instanceof Map) {
                        @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) value;
                        if ("deputy".equals(m.get("type")) && deputyInfo.getEntityId() == getInt("entity", -1, m)) {
                            throw DeputyFolderServiceProviderExceptionCode.DUPLICATE_DEPUTY_PERMISSION.create(folderId);
                        }
                    }
                }
            }

            ContentType contentType = this.contentType;
            com.openexchange.folderstorage.Permission[] existentPermissions = folder.getPermissions();
            Callable<Void> callable = new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    UpdateableFolder folder = new UpdateableFolder();
                    folder.setID(folderId);
                    folder.setTreeID(FolderStorage.REAL_TREE_ID);
                    folder.setContentType(contentType);

                    com.openexchange.folderstorage.Permission overwritten = null;
                    List<com.openexchange.folderstorage.Permission> permissions = new ArrayList<com.openexchange.folderstorage.Permission>(existentPermissions.length);
                    for (com.openexchange.folderstorage.Permission p : existentPermissions) {
                        if (p.getEntity() == deputyInfo.getEntityId()) {
                            overwritten = p;
                        } else {
                            permissions.add(p);
                        }
                    }
                    {
                        com.openexchange.folderstorage.Permission newPermission = new BasicPermission();
                        newPermission.setEntity(deputyInfo.getEntityId());
                        newPermission.setGroup(deputyInfo.isGroup());
                        newPermission.setFolderPermission(permission.getFolderPermission());
                        newPermission.setReadPermission(permission.getReadPermission());
                        newPermission.setWritePermission(permission.getWritePermission());
                        newPermission.setDeletePermission(permission.getDeletePermission());
                        newPermission.setAdmin(permission.isAdmin());
                        permissions.add(newPermission);
                    }
                    folder.setPermissions(permissions.toArray(new com.openexchange.folderstorage.Permission[permissions.size()]));
                    folder.setMeta(buildMeta(deputyInfo, folderId, overwritten, meta));

                    LogProperties.putProperty(LogProperties.Name.DEPUTY, "true");
                    try {
                        folderService.updateFolder(folder, null, session, new FolderServiceDecorator());
                    } finally {
                        LogProperties.removeProperty(LogProperties.Name.DEPUTY);
                    }

                    return null;
                }
            };
            tasks.add(callable);
        }

        try {
            for (Callable<Void> task : tasks) {
                task.call();
            }
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void updateDeputyPermission(DeputyInfo deputyInfo, ModulePermission modulePermission, Session session) throws OXException {
        Permission permission = modulePermission.getPermission();
        if (permission.isVisible() == false) {
            // Invalid permissions shall be applied
            return;
        }

        FolderService folderService = services.getOptionalService(FolderService.class);
        if (null == folderService) {
            throw ServiceExceptionCode.absentService(FolderService.class);
        }

        Set<String> removeFolderIds;
        Set<String> newFolderIds;
        Set<String> updateFolderIds;
        {
            List<String> folderIds = determineFolderIds(modulePermission, folderService, session);
            Optional<ModulePermission> existentModulePermission = getDeputyPermission(deputyInfo, session);
            if (existentModulePermission.isPresent()) {
                List<String> existentFolderIds = existentModulePermission.get().getOptionalFolderIds().get();
                removeFolderIds = new HashSet<String>(existentFolderIds);
                removeFolderIds.removeAll(folderIds);

                newFolderIds = new HashSet<String>(folderIds);
                newFolderIds.removeAll(existentFolderIds);

                updateFolderIds = new HashSet<String>(folderIds);
                updateFolderIds.retainAll(existentFolderIds);
            } else {
                removeFolderIds = Collections.emptySet();
                newFolderIds = Collections.emptySet();
                updateFolderIds = new HashSet<String>(folderIds);
            }
        }

        if (updateFolderIds.isEmpty() == false) {
            updateDeputyPermissionByFolders(updateFolderIds, deputyInfo, permission, session, folderService);
        }

        if (newFolderIds.isEmpty() == false) {
            grantDeputyPermissionByFolders(newFolderIds, deputyInfo, permission, session, folderService);
        }

        if (removeFolderIds.isEmpty() == false) {
            revokeDeputyPermissionByFolders(removeFolderIds, deputyInfo, session, folderService);
        }
    }

    private void updateDeputyPermissionByFolders(Collection<? extends String> folderIds, DeputyInfo deputyInfo, Permission permission, Session session, FolderService folderService) throws OXException {
        Context context = getContext(session);
        User user = getUser(session);

        Boolean entityExists = null;
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(folderIds.size());
        for (String folderId : folderIds) {
            UserizedFolder folder = folderService.getFolder(FolderStorage.REAL_TREE_ID, folderId, session, new FolderServiceDecorator());

            com.openexchange.folderstorage.Permission ownPermission = CalculatePermission.calculate(folder, ServerSessionAdapter.valueOf(session), Collections.singletonList(contentType));
            if (!ownPermission.isVisible()) {
                throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(folderId, Integer.toString(user.getId()), Integer.toString(context.getContextId()));
            }
            if (!ownPermission.isAdmin()) {
                throw FolderExceptionErrorMessage.INVALID_PERMISSIONS.create(I(com.openexchange.folderstorage.Permissions.createPermissionBits(ownPermission)), I(ownPermission.getEntity()), folder.getID() == null ? folder.getName() : folder.getID());
            }

            Map<String, Object> meta = folder.getMeta();
            if (meta == null) {
                throw DeputyFolderServiceProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
            }

            @SuppressWarnings("unchecked") Map<String, Object> deputyMetadata = (Map<String, Object>) meta.get(deputyInfo.getDeputyId());
            if (deputyMetadata == null || !"deputy".equals(deputyMetadata.get("type")) || deputyInfo.getEntityId() != getInt("entity", -1, deputyMetadata)) {
                throw DeputyFolderServiceProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
            }

            // Check entity existence
            if (entityExists == null) {
                entityExists = Boolean.valueOf(existsEntity(deputyInfo, context));
            }
            if (entityExists.booleanValue() == false) {
                Map<String, Object> newMeta = new LinkedHashMap<String, Object>(meta);
                newMeta.remove(deputyInfo.getDeputyId());
                tasks.add(new UpdateMetadataCallable(folderId, this.contentType, newMeta, folderService, session));
            } else {
                // Check existent permissions
                com.openexchange.folderstorage.Permission[] permisisons = folder.getPermissions();
                List<com.openexchange.folderstorage.Permission> newPermissions = new ArrayList<com.openexchange.folderstorage.Permission>(permisisons.length);
                com.openexchange.folderstorage.Permission existentPermission = null;
                for (int i = permisisons.length; i-- > 0;) {
                    com.openexchange.folderstorage.Permission p = permisisons[i];
                    if (p.getEntity() == deputyInfo.getEntityId()) {
                        existentPermission = p;
                    } else {
                        newPermissions.add(p);
                    }
                }

                // Apply permission & store deputy meta-data
                if (existentPermission == null) {
                    throw DeputyFolderServiceProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
                }

                ContentType contentType = this.contentType;
                Callable<Void> task = new Callable<Void>() {

                    @Override
                    public Void call() throws Exception {
                        UpdateableFolder folder = new UpdateableFolder();
                        folder.setID(folderId);
                        folder.setTreeID(FolderStorage.REAL_TREE_ID);
                        folder.setContentType(contentType);

                        {
                            com.openexchange.folderstorage.Permission newPermission = new BasicPermission();
                            newPermission.setEntity(deputyInfo.getEntityId());
                            newPermission.setGroup(deputyInfo.isGroup());
                            newPermission.setFolderPermission(permission.getFolderPermission());
                            newPermission.setReadPermission(permission.getReadPermission());
                            newPermission.setWritePermission(permission.getWritePermission());
                            newPermission.setDeletePermission(permission.getDeletePermission());
                            newPermission.setAdmin(permission.isAdmin());
                            newPermissions.add(newPermission);
                        }
                        folder.setPermissions(newPermissions.toArray(new com.openexchange.folderstorage.Permission[newPermissions.size()]));

                        LogProperties.putProperty(LogProperties.Name.DEPUTY, "true");
                        try {
                            folderService.updateFolder(folder, null, session, new FolderServiceDecorator());
                        } finally {
                            LogProperties.removeProperty(LogProperties.Name.DEPUTY);
                        }

                        return null;
                    }
                };
                tasks.add(task);
            }
        }

        try {
            for (Callable<Void> task : tasks) {
                task.call();
            }
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    private void revokeDeputyPermissionByFolders(Collection<? extends String> folderIds, DeputyInfo deputyInfo, Session session, FolderService folderService) throws OXException {
        Context context = getContext(session);
        User user = getUser(session);

        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(folderIds.size());
        for (String folderId : folderIds) {
            UserizedFolder folder = folderService.getFolder(FolderStorage.REAL_TREE_ID, folderId, session, new FolderServiceDecorator());

            com.openexchange.folderstorage.Permission ownPermission = CalculatePermission.calculate(folder, ServerSessionAdapter.valueOf(session), Collections.singletonList(contentType));
            if (!ownPermission.isVisible()) {
                throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(folderId, Integer.toString(user.getId()), Integer.toString(context.getContextId()));
            }
            if (!ownPermission.isAdmin()) {
                throw FolderExceptionErrorMessage.INVALID_PERMISSIONS.create(I(com.openexchange.folderstorage.Permissions.createPermissionBits(ownPermission)), I(ownPermission.getEntity()), folder.getID() == null ? folder.getName() : folder.getID());
            }

            Map<String, Object> meta = folder.getMeta();
            if (meta == null) {
                throw DeputyFolderServiceProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
            }

            @SuppressWarnings("unchecked") Map<String, Object> deputyMetadata = (Map<String, Object>) meta.get(deputyInfo.getDeputyId());
            if (deputyMetadata == null || !"deputy".equals(deputyMetadata.get("type")) || deputyInfo.getEntityId() != getInt("entity", -1, deputyMetadata)) {
                throw DeputyFolderServiceProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
            }

            // Check existent permissions
            com.openexchange.folderstorage.Permission[] permisisons = folder.getPermissions();
            List<com.openexchange.folderstorage.Permission> newPermissions = new ArrayList<com.openexchange.folderstorage.Permission>(permisisons.length);
            com.openexchange.folderstorage.Permission existentPermission = null;
            for (int i = permisisons.length; existentPermission == null && i-- > 0;) {
                com.openexchange.folderstorage.Permission p = permisisons[i];
                if (p.getEntity() == deputyInfo.getEntityId()) {
                    existentPermission = p;
                } else {
                    newPermissions.add(p);
                }
            }

            // Apply permission & store deputy meta-data
            if (existentPermission == null) {
                throw DeputyFolderServiceProviderExceptionCode.MISSING_DEPUTY_PERMISSION.create(folderId);
            }

            ContentType contentType = this.contentType;
            Callable<Void> task = new Callable<Void>() {

                @Override
                public Void call() throws Exception {
                    UpdateableFolder folder = new UpdateableFolder();
                    folder.setID(folderId);
                    folder.setTreeID(FolderStorage.REAL_TREE_ID);
                    folder.setContentType(contentType);

                    folder.setPermissions(newPermissions.toArray(new com.openexchange.folderstorage.Permission[newPermissions.size()]));

                    LogProperties.putProperty(LogProperties.Name.DEPUTY, "true");
                    try {
                        folderService.updateFolder(folder, null, session, new FolderServiceDecorator());
                    } finally {
                        LogProperties.removeProperty(LogProperties.Name.DEPUTY);
                    }

                    return null;
                }
            };
            tasks.add(task);
        }

        try {
            for (Callable<Void> task : tasks) {
                task.call();
            }
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void revokeDeputyPermission(DeputyInfo deputyInfo, Session session) throws OXException {
        FolderService folderService = services.getOptionalService(FolderService.class);
        if (null == folderService) {
            throw ServiceExceptionCode.absentService(FolderService.class);
        }

        Context context = getContext(session);
        User user = getUser(session);

        Map<String, Map<String, Object>> deputyMetadatas = DatabaseFolderUtils.getAllFolderIdsHavingDeputy(deputyInfo.getDeputyId(), contentType, session.getContextId(), services);
        Boolean entityExists = null;
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(deputyMetadatas.size());
        for (Map.Entry<String, Map<String, Object>> deputyMetadataEntry : deputyMetadatas.entrySet()) {
            String folderId = deputyMetadataEntry.getKey();
            Map<String, Object> metadata = deputyMetadataEntry.getValue();

            // Look-up deputy metadata for given deputy identifier
            @SuppressWarnings("unchecked") Map<String, Object> deputyMetadata = (Map<String, Object>) metadata.get(deputyInfo.getDeputyId());
            if (deputyMetadata != null && "deputy".equals(deputyMetadata.get("type"))) {
                Integer entityId = (Integer) deputyMetadata.get("entity");
                if (entityId != null && entityId.intValue() == deputyInfo.getEntityId()) {
                    // Check entity existence
                    if (entityExists == null) {
                        entityExists = Boolean.valueOf(existsEntity(deputyInfo, context));
                    }
                    if (entityExists.booleanValue() == false) {
                        Map<String, Object> newMeta = new LinkedHashMap<String, Object>(metadata);
                        newMeta.remove(deputyInfo.getDeputyId());
                        tasks.add(new UpdateMetadataCallable(folderId, this.contentType, newMeta, folderService, session));
                    } else {
                        @SuppressWarnings("unchecked") Map<String, Object> previousPermission = (Map<String, Object>) deputyMetadata.get("previousPermission");

                        UserizedFolder folder = folderService.getFolder(FolderStorage.REAL_TREE_ID, folderId, session, new FolderServiceDecorator());

                        com.openexchange.folderstorage.Permission ownPermission = CalculatePermission.calculate(folder, ServerSessionAdapter.valueOf(session), Collections.singletonList(contentType));
                        if (!ownPermission.isVisible()) {
                            throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(folderId, Integer.toString(user.getId()), Integer.toString(context.getContextId()));
                        }
                        if (!ownPermission.isAdmin()) {
                            throw FolderExceptionErrorMessage.INVALID_PERMISSIONS.create(I(com.openexchange.folderstorage.Permissions.createPermissionBits(ownPermission)), I(ownPermission.getEntity()), folder.getID() == null ? folder.getName() : folder.getID());
                        }

                        com.openexchange.folderstorage.Permission[] permisisons = folder.getPermissions();
                        List<com.openexchange.folderstorage.Permission> newPermissions = new ArrayList<com.openexchange.folderstorage.Permission>(permisisons.length);
                        for (int i = permisisons.length; i-- > 0;) {
                            com.openexchange.folderstorage.Permission p = permisisons[i];
                            if (p.getEntity() != deputyInfo.getEntityId()) {
                                newPermissions.add(p);
                            }
                        }
                        if (previousPermission != null) {
                            com.openexchange.folderstorage.Permission restoredPerm = com.openexchange.folderstorage.Permissions.createPermission(deputyInfo.getEntityId(), deputyInfo.isGroup(), ((Integer) previousPermission.get("bits")).intValue());
                            restoredPerm.setType(FolderPermissionType.getType(((Integer) previousPermission.get("type")).intValue()));
                            String legator = (String) previousPermission.get("legator");
                            if (legator != null) {
                                restoredPerm.setPermissionLegator(legator);
                            }
                            newPermissions.add(restoredPerm);
                        }

                        metadata.remove(deputyInfo.getDeputyId());

                        ContentType contentType = this.contentType;
                        Callable<Void> task = new Callable<Void>() {

                            @Override
                            public Void call() throws Exception {
                                UpdateableFolder folder = new UpdateableFolder();
                                folder.setID(folderId);
                                folder.setTreeID(FolderStorage.REAL_TREE_ID);
                                folder.setContentType(contentType);

                                folder.setPermissions(newPermissions.toArray(new com.openexchange.folderstorage.Permission[newPermissions.size()]));
                                folder.setMeta(metadata);

                                LogProperties.putProperty(LogProperties.Name.DEPUTY, "true");
                                try {
                                    folderService.updateFolder(folder, null, session, new FolderServiceDecorator());
                                } finally {
                                    LogProperties.removeProperty(LogProperties.Name.DEPUTY);
                                }
                                return null;
                            }
                        };
                        tasks.add(task);
                    }
                }
            }
        }

        try {
            for (Callable<Void> task : tasks) {
                task.call();
            }
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public Optional<ModulePermission> getDeputyPermission(DeputyInfo deputyInfo, Session session) throws OXException {
        Map<String, Map<String, Object>> deputyMetadatas = DatabaseFolderUtils.getAllFolderIdsHavingDeputy(deputyInfo.getDeputyId(), contentType, session.getContextId(), services);
        int numberOfFolders = deputyMetadatas.size();
        if (numberOfFolders <= 0) {
            return Optional.empty();
        }

        FolderService folderService = services.getOptionalService(FolderService.class);
        if (null == folderService) {
            throw ServiceExceptionCode.absentService(FolderService.class);
        }

        Context context = getContext(session);
        User user = getUser(session);

        Reference<Permission> permissionReference = new Reference<Permission>();
        List<String> folderIds = new ArrayList<String>(numberOfFolders);
        List<Callable<Void>> tasks = new ArrayList<Callable<Void>>(numberOfFolders);
        Boolean entityExists = null;
        for (Map.Entry<String, Map<String, Object>> deputyMetadataEntry : deputyMetadatas.entrySet()) {
            String folderId = deputyMetadataEntry.getKey();
            Map<String, Object> metadata = deputyMetadataEntry.getValue();

            // Look-up deputy metadata for given deputy identifier
            @SuppressWarnings("unchecked") Map<String, Object> deputyMetadata = (Map<String, Object>) metadata.get(deputyInfo.getDeputyId());
            if (deputyMetadata != null && "deputy".equals(deputyMetadata.get("type"))) {
                // Check entity existence
                if (entityExists == null) {
                    entityExists = Boolean.valueOf(existsEntity(deputyInfo, context));
                }
                if (entityExists.booleanValue() == false) {
                    Map<String, Object> newMeta = new LinkedHashMap<String, Object>(metadata);
                    newMeta.remove(deputyInfo.getDeputyId());
                    tasks.add(new UpdateMetadataCallable(folderId, this.contentType, newMeta, folderService, session));
                } else {
                    try {
                        if (permissionReference.hasNoValue()) {
                            UserizedFolder folder = folderService.getFolder(FolderStorage.REAL_TREE_ID, folderId, session, new FolderServiceDecorator());

                            com.openexchange.folderstorage.Permission ownPermission = CalculatePermission.calculate(folder, ServerSessionAdapter.valueOf(session), Collections.singletonList(contentType));
                            if (!ownPermission.isVisible()) {
                                throw FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.create(folderId, Integer.toString(user.getId()), Integer.toString(context.getContextId()));
                            }

                            for (com.openexchange.folderstorage.Permission p : folder.getPermissions()) {
                                if (deputyInfo.getEntityId() == p.getEntity()) {
                                    Builder tmp = DefaultPermission.builder()
                                        .withFolderPermission(p.getFolderPermission())
                                        .withReadPermission(p.getReadPermission())
                                        .withWritePermission(p.getWritePermission())
                                        .withDeletePermission(p.getDeletePermission())
                                        .withAdmin(p.isAdmin())
                                        .withEntity(deputyInfo.getEntityId())
                                        .withGroup(deputyInfo.isGroup());
                                    permissionReference.setValue(tmp.build());
                                }
                            }
                        }
                        folderIds.add(folderId);
                    } catch (OXException e) {
                        if (FolderExceptionErrorMessage.FOLDER_NOT_VISIBLE.equals(e) == false) {
                            throw e;
                        }

                        // Otherwise swallow
                        LoggerHolder.LOG.info("Deputy {} in context {} has no access to folder {}. Therefore ignoring that permission.", I(deputyInfo.getEntityId()), I(session.getContextId()), folderId);
                    }
                }
            }
        }

        try {
            for (Callable<Void> task : tasks) {
                task.call();
            }
        } catch (OXException e) {
            throw e;
        } catch (Exception e) {
            throw DeputyExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }

        if (folderIds.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(DefaultModulePermission.builder()
            .withModuleId(moduleId.getId())
            .withFolderIds(folderIds)
            .withPermission(permissionReference.getValue())
            .build());
    }

    // ------------------------------------------------- Utility methods -------------------------------------------------------------------

    static Map<String, Object> buildMeta(DeputyInfo deputyInfo, String folderId, com.openexchange.folderstorage.Permission overwritten, Map<String, Object> meta) {
        Map<String, Object> deputyMetadata = new LinkedHashMap<String, Object>(4);
        deputyMetadata.put("type", "deputy");
        deputyMetadata.put("entity", I(deputyInfo.getEntityId()));
        deputyMetadata.put("folderId", folderId);
        if (overwritten != null) {
            Map<String, Object> permissionMetadata = new LinkedHashMap<>(4);
            permissionMetadata.put("bits", I(com.openexchange.folderstorage.Permissions.createPermissionBits(overwritten)));
            permissionMetadata.put("type", I(overwritten.getType().getTypeNumber()));
            String legator = overwritten.getPermissionLegator();
            if (legator != null) {
                permissionMetadata.put("legator", legator);
            }
            deputyMetadata.put("previousPermission", permissionMetadata);
        }

        Map<String, Object> newMeta = meta == null ? new LinkedHashMap<String, Object>(2) : new LinkedHashMap<String, Object>(meta);
        newMeta.put(deputyInfo.getDeputyId(), deputyMetadata);
        return newMeta;
    }

    private List<String> determineFolderIds(ModulePermission modulePermission, FolderService folderService, Session session) throws OXException {
        Optional<List<String>> optionalFolderIds = modulePermission.getOptionalFolderIds();
        if (optionalFolderIds.isPresent()) {
            return optionalFolderIds.get();
        }

        String folderId = folderService.getDefaultFolder(getUser(session), FolderStorage.REAL_TREE_ID, contentType, session, new FolderServiceDecorator()).getID();
        return Collections.singletonList(folderId);
    }

    private static ContentType getContentType(KnownModuleId moduleId) {
        switch (moduleId) {
            case CALENDAR:
                return com.openexchange.folderstorage.calendar.contentType.CalendarContentType.getInstance();
            case CONTACTS:
                return com.openexchange.folderstorage.addressbook.AddressDataContentType.getInstance();
            case DRIVE:
                return com.openexchange.folderstorage.database.contentType.InfostoreContentType.getInstance();
            case TASKS:
                return com.openexchange.folderstorage.database.contentType.TaskContentType.getInstance();
            default:
                throw new IllegalArgumentException("Invalid module: " + moduleId.getId());
        }
    }

    private static int getInt(String key, int defaultValue, Map<String, Object> deputyMetadata) {
        Object value = deputyMetadata.get(key);
        if (value == null) {
            return defaultValue;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        int i = Strings.parsePositiveInt(value.toString());
        return i >= 0 ? i : defaultValue;
    }

    private static class UpdateMetadataCallable implements Callable<Void> {

        private final String folderId;
        private final ContentType contentType;
        private final Map<String, Object> newMeta;
        private final FolderService folderService;
        private final Session session;

        UpdateMetadataCallable(String folderId, ContentType contentType, Map<String, Object> newMeta, FolderService folderService, Session session) {
            super();
            this.folderId = folderId;
            this.contentType = contentType;
            this.newMeta = newMeta;
            this.folderService = folderService;
            this.session = session;
        }

        @Override
        public Void call() throws Exception {
            UpdateableFolder folder = new UpdateableFolder();
            folder.setID(folderId);
            folder.setTreeID(FolderStorage.REAL_TREE_ID);
            folder.setContentType(contentType);

            folder.setMeta(newMeta);

            LogProperties.putProperty(LogProperties.Name.DEPUTY, "true");
            try {
                folderService.updateFolder(folder, null, session, new FolderServiceDecorator());
            } finally {
                LogProperties.removeProperty(LogProperties.Name.DEPUTY);
            }
            return null;
        }
    }

}
