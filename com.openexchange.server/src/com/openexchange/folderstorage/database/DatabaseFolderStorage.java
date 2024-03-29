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

package com.openexchange.folderstorage.database;

import static com.openexchange.folderstorage.database.DatabaseFolderStorageUtility.extractIDs;
import static com.openexchange.folderstorage.database.DatabaseFolderStorageUtility.getDefaultUsedForSync;
import static com.openexchange.folderstorage.database.DatabaseFolderStorageUtility.getUserPermissionBits;
import static com.openexchange.folderstorage.database.DatabaseFolderStorageUtility.localizeFolderNames;
import static com.openexchange.java.Autoboxing.B;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.b;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.java.util.Tools.getUnsignedInteger;
import static com.openexchange.server.impl.OCLPermission.ADMIN_PERMISSION;
import static com.openexchange.server.impl.OCLPermission.DELETE_ALL_OBJECTS;
import static com.openexchange.server.impl.OCLPermission.READ_ALL_OBJECTS;
import static com.openexchange.server.impl.OCLPermission.READ_FOLDER;
import static com.openexchange.server.impl.OCLPermission.WRITE_ALL_OBJECTS;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import com.openexchange.cache.impl.FolderCacheManager;
import com.openexchange.caching.Cache;
import com.openexchange.caching.CacheKey;
import com.openexchange.caching.CacheService;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.database.DatabaseService;
import com.openexchange.database.Databases;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.AccountAware;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.SharingFileStorageService;
import com.openexchange.file.storage.composition.FolderID;
import com.openexchange.file.storage.registry.FileStorageServiceRegistry;
import com.openexchange.folderstorage.AfterReadAwareFolderStorage;
import com.openexchange.folderstorage.CalculatePermission;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FederatedSharingFolders;
import com.openexchange.folderstorage.Folder;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.FolderMoveWarningCollector;
import com.openexchange.folderstorage.FolderPath;
import com.openexchange.folderstorage.FolderPermissionType;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderType;
import com.openexchange.folderstorage.FolderUpdate;
import com.openexchange.folderstorage.GlobalAddressBookUtils;
import com.openexchange.folderstorage.LockCleaningFolderStorage;
import com.openexchange.folderstorage.MoveFolderPermissionMode;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.RestoringFolderStorage;
import com.openexchange.folderstorage.SearchableFileFolderNameFolderStorage;
import com.openexchange.folderstorage.SetterAwareFolder;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.StorageParametersUtility;
import com.openexchange.folderstorage.StoragePriority;
import com.openexchange.folderstorage.StorageType;
import com.openexchange.folderstorage.SystemContentType;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.UsedForSync;
import com.openexchange.folderstorage.cache.memory.FolderMap;
import com.openexchange.folderstorage.cache.memory.FolderMapManagement;
import com.openexchange.folderstorage.database.contentType.CalendarContentType;
import com.openexchange.folderstorage.database.contentType.ContactsContentType;
import com.openexchange.folderstorage.database.contentType.InfostoreContentType;
import com.openexchange.folderstorage.database.contentType.TaskContentType;
import com.openexchange.folderstorage.database.contentType.UnboundContentType;
import com.openexchange.folderstorage.database.getfolder.SharedPrefixFolder;
import com.openexchange.folderstorage.database.getfolder.SystemInfostoreFolder;
import com.openexchange.folderstorage.database.getfolder.SystemPrivateFolder;
import com.openexchange.folderstorage.database.getfolder.SystemPublicFolder;
import com.openexchange.folderstorage.database.getfolder.SystemRootFolder;
import com.openexchange.folderstorage.database.getfolder.SystemSharedFolder;
import com.openexchange.folderstorage.database.getfolder.VirtualListFolder;
import com.openexchange.folderstorage.filestorage.FileStorageId;
import com.openexchange.folderstorage.internal.ConfiguredDefaultPermissions;
import com.openexchange.folderstorage.internal.performers.ComparedFolderPermissions;
import com.openexchange.folderstorage.outlook.OutlookFolderStorage;
import com.openexchange.folderstorage.outlook.osgi.Services;
import com.openexchange.folderstorage.outlook.sql.Delete;
import com.openexchange.folderstorage.tx.TransactionManager;
import com.openexchange.folderstorage.type.DocumentsType;
import com.openexchange.folderstorage.type.MusicType;
import com.openexchange.folderstorage.type.PicturesType;
import com.openexchange.folderstorage.type.PrivateType;
import com.openexchange.folderstorage.type.PublicType;
import com.openexchange.folderstorage.type.SharedType;
import com.openexchange.folderstorage.type.SystemType;
import com.openexchange.folderstorage.type.TemplatesType;
import com.openexchange.folderstorage.type.TrashType;
import com.openexchange.folderstorage.type.VideosType;
import com.openexchange.group.GroupService;
import com.openexchange.group.GroupStorage;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.FolderPathObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.InfostoreFacades;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tools.iterator.FolderObjectIterator;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.java.Autoboxing;
import com.openexchange.java.CallerRunsCompletionService;
import com.openexchange.java.Collators;
import com.openexchange.java.Strings;
import com.openexchange.java.util.Tools;
import com.openexchange.server.ServiceLookup;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.share.GuestInfo;
import com.openexchange.share.ShareService;
import com.openexchange.share.recipient.RecipientType;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderBatchLoader;
import com.openexchange.tools.oxfolder.OXFolderExceptionCode;
import com.openexchange.tools.oxfolder.OXFolderIteratorSQL;
import com.openexchange.tools.oxfolder.OXFolderLoader;
import com.openexchange.tools.oxfolder.OXFolderLoader.IdAndName;
import com.openexchange.tools.oxfolder.OXFolderManager;
import com.openexchange.tools.oxfolder.OXFolderSQL;
import com.openexchange.tools.oxfolder.UpdatedFolderHandler;
import com.openexchange.tools.oxfolder.property.FolderSubscriptionHelper;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.User;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.ConcurrentTIntObjectHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

/**
 * {@link DatabaseFolderStorage} - The database folder storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DatabaseFolderStorage implements AfterReadAwareFolderStorage, LockCleaningFolderStorage, RestoringFolderStorage, SearchableFileFolderNameFolderStorage {

    private static final String DEL_OXFOLDER_PERMISSIONS = "del_oxfolder_permissions";

    private static final String DEL_OXFOLDER_TREE = "del_oxfolder_tree";

    /** The logger */
    static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DatabaseFolderStorage.class);

    private static final String PARAM_CONNECTION = DatabaseParameterConstants.PARAM_CONNECTION;

    /**
     * Simple interface for providing and closing a connection.
     */
    private static interface ConnectionProvider {

        /**
         * Gets the (active) connection.
         *
         * @return The connection
         */
        Connection getConnection();

        /**
         * Closes underlying connection.
         */
        void close();
    }

    private static final class NonClosingConnectionProvider implements ConnectionProvider {

        private final ConnectionMode connection;

        protected NonClosingConnectionProvider(final ConnectionMode connection) {
            super();
            this.connection = connection;
        }

        @Override
        public Connection getConnection() {
            return connection.connection;
        }

        @Override
        public void close() {
            // Nothing to do
        }
    }

    private static final class ClosingConnectionProvider implements ConnectionProvider {

        private final DatabaseService databaseService;

        private final ConnectionMode connection;

        private final int contextId;

        protected ClosingConnectionProvider(final ConnectionMode connection, final DatabaseService databaseService, final int contextId) {
            super();
            this.connection = connection;
            this.databaseService = databaseService;
            this.contextId = contextId;
        }

        @Override
        public Connection getConnection() {
            return connection.connection;
        }

        @Override
        public void close() {
            connection.close(databaseService, contextId);
        }
    }

    static final EnumSet<Mode> WRITEES = EnumSet.of(Mode.WRITE, Mode.WRITE_AFTER_READ);

    // -------------------------------------------------------------------------------------------------------------------------- //

    private final ServiceLookup services;

    /**
     * Initializes a new {@link DatabaseFolderStorage}.
     */
    public DatabaseFolderStorage(ServiceLookup services) {
        super();
        this.services = services;
    }

    @Override
    public void clearCache(final int userId, final int contextId) {
        /*
         * Nothing to do...
         */
    }

    /**
     * Clears possible remembered stamps for given context identifier.
     *
     * @param contextId The context identifier
     */
    public static void clearStampsFor(int contextId) {
        STAMPS.remove(contextId);
    }

    private static final ConcurrentTIntObjectHashMap<Long> STAMPS = new ConcurrentTIntObjectHashMap<Long>(128);
    private static final long DELAY = 60 * 60 * 1000;
    private static final int MAX = 3;

    @Override
    public void checkConsistency(final String treeId, final StorageParameters storageParameters) throws OXException {
        final int contextId = storageParameters.getContextId();
        final long now = System.currentTimeMillis();
        final Long stamp = STAMPS.get(contextId);
        if ((null != stamp) && ((stamp.longValue() + DELAY) > now)) {
            return;
        }
        // Delay exceeded
        STAMPS.remove(contextId);
        final DatabaseService databaseService = services.getService(DatabaseService.class);
        Connection con = null;
        boolean close = true;
        Mode mode = Mode.READ;
        boolean modified = false;
        try {
            {
                final ConnectionMode conMode = optParameter(ConnectionMode.class, DatabaseParameterConstants.PARAM_CONNECTION, storageParameters);
                if (null != conMode) {
                    con = conMode.connection;
                    mode = conMode.readWrite;
                    close = false;
                } else {
                    con = databaseService.getReadOnly(contextId);
                }
            }
            final ServerSession session = ServerSessionAdapter.valueOf(storageParameters.getSession());
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create();
            }
            /*
             * Determine folder with non-existing parents
             */
            final Context context = session.getContext();
            int[] nonExistingParents = OXFolderSQL.getNonExistingParents(context, con);
            if (null == nonExistingParents || 0 == nonExistingParents.length) {
                return;
            }
            /*
             * Upgrade to read-write connection & repeat if check was performed with read-only connection
             */
            if (Mode.READ == mode) {
                if (close) {
                    databaseService.backReadOnly(contextId, con);
                }
                con = databaseService.getWritable(contextId);
                mode = Mode.WRITE_AFTER_READ;
                close = true;
                // Query again...
                nonExistingParents = OXFolderSQL.getNonExistingParents(context, con);
                if (null == nonExistingParents || 0 == nonExistingParents.length) {
                    return;
                }
            }
            /*
             * Some variables
             */
            final TIntSet shared = new TIntHashSet();
            final OXFolderManager manager = OXFolderManager.getInstance(session, con, con);
            final OXFolderAccess folderAccess = getFolderAccess(context, con);
            final int userId = session.getUserId();
            /*
             * Iterate folders
             */
            int runCount = 0;
            final TIntSet tmp = new TIntHashSet();
            do {
                for (final int folderId : nonExistingParents) {
                    if (folderId >= FolderObject.MIN_FOLDER_ID) {
                        if (FolderObject.SHARED == folderAccess.getFolderType(folderId, userId)) {
                            shared.add(folderId);
                        } else {
                            manager.deleteValidatedFolder(folderId, now, -1, true);
                            modified = true;
                        }
                    }
                }
                tmp.clear();
                tmp.addAll(OXFolderSQL.getNonExistingParents(context, con));
                if (tmp.isEmpty()) {
                    nonExistingParents = null;
                } else {
                    tmp.removeAll(shared.toArray());
                    for (int i = 0; i < FolderObject.MIN_FOLDER_ID; i++) {
                        tmp.remove(i);
                    }
                    nonExistingParents = tmp.toArray();
                }
            } while (++runCount <= MAX && null != nonExistingParents && nonExistingParents.length > 0);
        } finally {
            if (null != con && close) {
                if (Mode.READ == mode) {
                    databaseService.backReadOnly(contextId, con);
                } else {
                    if (modified) {
                        databaseService.backWritable(contextId, con);
                    } else {
                        databaseService.backWritableAfterReading(contextId, con);
                    }
                }
            }
            STAMPS.put(contextId, Long.valueOf(now));
        }
    }

    @Override
    public ContentType[] getSupportedContentTypes() {
        return new ContentType[] { TaskContentType.getInstance(), CalendarContentType.getInstance(), ContactsContentType.getInstance(), InfostoreContentType.getInstance(), UnboundContentType.getInstance(), SystemContentType.getInstance() };
    }

    @Override
    public ContentType getDefaultContentType() {
        return ContactsContentType.getInstance();
    }

    @Override
    public void commitTransaction(final StorageParameters params) throws OXException {
        final ConnectionMode con;
        try {
            con = optParameter(ConnectionMode.class, PARAM_CONNECTION, params);
        } catch (OXException e) {
            LOG.warn("Storage already committed:\n{}", params.getCommittedTrace(), e);
            return;
        }
        if (null == con) {
            return;
        }

        if (!con.controlledExternally) {
            if (WRITEES.contains(con.readWrite)) {
                try {
                    con.connection.commit();
                } catch (SQLException e) {
                    throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
                } finally {
                    Databases.autocommit(con.connection);
                    final DatabaseService databaseService = services.getService(DatabaseService.class);
                    if (null != databaseService) {
                        con.close(databaseService, params.getContext().getContextId());
                    }
                }
            } else {
                final DatabaseService databaseService = services.getService(DatabaseService.class);
                if (null != databaseService) {
                    databaseService.backReadOnly(params.getContext(), con.connection);
                }
            }
        }

        final FolderType folderType = getFolderType();
        params.putParameter(folderType, PARAM_CONNECTION, null);
        params.markCommitted();
    }

    @Override
    public void restore(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.WRITE, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create();
            }
            final int folderId = Integer.parseInt(folderIdentifier);
            final Context context = storageParameters.getContext();
            FolderObject.loadFolderObjectFromDB(folderId, context, con, false, false, DEL_OXFOLDER_TREE, DEL_OXFOLDER_PERMISSIONS);
            /*
             * From backup to working table
             */
            OXFolderSQL.restore(folderId, context, null);
        } catch (NumberFormatException e) {
            throw FolderExceptionErrorMessage.INVALID_FOLDER_ID.create(folderIdentifier);
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    /**
     * Gets the optionally configured default permissions for a new folder that is supposed to be created below specified parent.
     *
     * @param parentId The identifier of the parent folder
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The configured default permissions or <code>null</code>
     * @throws OXException If look-up for configured default permissions fails
     */
    private Permission[] getConfiguredDefaultPermissionsFor(String parentId, int userId, int contextId) throws OXException {
        return ConfiguredDefaultPermissions.getInstance().getConfiguredDefaultPermissionsFor(parentId, userId, contextId);
    }

    @Override
    public void createFolder(final Folder folder, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.WRITE, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final Session session = storageParameters.getSession();
            Context context = storageParameters.getContext();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create();
            }
            final long millis = System.currentTimeMillis();

            final FolderObject createMe = new FolderObject();
            createMe.setCreatedBy(session.getUserId());
            createMe.setCreationDate(new Date(millis));
            createMe.setCreator(session.getUserId());
            createMe.setDefaultFolder(false);
            {
                final String name = folder.getName();
                if (null != name) {
                    createMe.setFolderName(name);
                }
            }
            createMe.setLastModified(new Date(millis));
            createMe.setModifiedBy(session.getUserId());
            {
                final ContentType ct = folder.getContentType();
                if (null != ct) {
                    createMe.setModule(getModuleByContentType(ct));
                }
            }
            {
                final String parentId = folder.getParentID();
                if (null != parentId) {
                    createMe.setParentFolderID(Integer.parseInt(parentId));
                }
            }
            {
                final Type t = folder.getType();
                if (null == t) {
                    /*
                     * Determine folder type by examining parent folder
                     */
                    createMe.setType(getFolderType(createMe.getModule(), createMe.getParentFolderID(), context, con));
                } else {
                    createMe.setType(getTypeByFolderType(t));
                }
            }
            // Meta
            {
                final Map<String, Object> meta = folder.getMeta();
                if (null != meta) {
                    createMe.setMeta(meta);
                }
            }
            // Permissions
            final Permission[] perms = folder.getPermissions();
            if (null != perms) {
                final OCLPermission[] oclPermissions = new OCLPermission[perms.length];
                for (int i = 0; i < perms.length; i++) {
                    oclPermissions[i] = newOCLPermissionFor(perms[i]);
                }
                createMe.setPermissionsAsArray(oclPermissions);
            } else {
                final int parentFolderID = createMe.getParentFolderID();
                /*
                 * Prepare
                 */
                final FolderObject parent = getFolderObject(parentFolderID, context, con, storageParameters);
                final int userId = storageParameters.getUserId();
                final boolean isShared = parent.isShared(userId);
                final boolean isSystem = FolderObject.SYSTEM_TYPE == parent.getType();
                final List<OCLPermission> parentPermissions = parent.getPermissions();
                /*
                 * Create permission list
                 */
                final List<OCLPermission> permissions;
                {
                    Permission[] configuredPermissions = getConfiguredDefaultPermissionsFor(Integer.toString(parentFolderID), session.getUserId(), session.getContextId());
                    if (null == configuredPermissions) {
                        permissions = new ArrayList<OCLPermission>((isSystem ? 1 : parentPermissions.size()) + 1);
                    } else {
                        permissions = new ArrayList<OCLPermission>(configuredPermissions.length + 2);
                        for (Permission permission : configuredPermissions) {
                            permissions.add(newOCLPermissionFor(permission));
                        }
                    }
                }
                if (isShared) {
                    permissions.add(newMaxPermissionFor(parent.getCreatedBy()));
                    permissions.add(newStandardPermissionFor(userId));
                } else {
                    /*
                     * client did not pass permissions, check whether to apply default ones
                     */
                    permissions.add(newMaxPermissionFor(userId));
                }
                TIntSet pim = new TIntHashSet(3);
                pim.add(FolderObject.CALENDAR);
                pim.add(FolderObject.CONTACT);
                pim.add(FolderObject.TASK);
                if (!isSystem && !pim.contains(createMe.getModule())) {
                    final TIntSet ignore = new TIntHashSet(4);
                    ignore.add(userId);
                    ignore.add(OCLPermission.ALL_GUESTS);
                    if (isShared) {
                        ignore.add(parent.getCreatedBy());
                    }
                    for (final OCLPermission permission : parentPermissions) {
                        if ((permission.getSystem() <= 0) && !ignore.contains(permission.getEntity())) {
                            if (false == permission.isGroupPermission()) {
                                GuestInfo guestInfo = ServerServiceRegistry.getServize(ShareService.class).getGuestInfo(session, permission.getEntity());
                                if (null != guestInfo && RecipientType.ANONYMOUS.equals(guestInfo.getRecipientType()) && permission.getType() == FolderPermissionType.NORMAL) {
                                    continue; // don't inherit anonymous share links
                                }
                            }
                            if (permission.getType() == FolderPermissionType.LEGATOR){
                                OCLPermission tmp = permission.deepClone();
                                tmp.setType(FolderPermissionType.INHERITED);
                                tmp.setPermissionLegator(String.valueOf(parent.getObjectID()));
                                permissions.add(tmp);
                            } else {
                                permissions.add(permission);
                            }
                        }
                    }
                }
                createMe.setPermissions(permissions);
            }
            // Create
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            FolderObject createdFolder = folderManager.createFolder(createMe, true, millis);
            final int fuid = createMe.getObjectID();
            if (fuid <= 0) {
                throw OXFolderExceptionCode.CREATE_FAILED.create();
            }
            folder.setID(String.valueOf(fuid));

            // store user properties as needed
            storeUserProperties(Optional.of(con), context, session.getUserId(), createdFolder, folder);

            // Handle warnings
            final List<OXException> warnings = folderManager.getWarnings();
            if (null != warnings) {
                for (final OXException warning : warnings) {
                    storageParameters.addWarning(warning);
                }
            }
        } finally {
            provider.close();
        }
    }

    private static OCLPermission newOCLPermissionFor(Permission p) {
        if (null == p) {
            return null;
        }
        OCLPermission oclPerm = new OCLPermission();
        oclPerm.setEntity(p.getEntity());
        oclPerm.setGroupPermission(p.isGroup());
        oclPerm.setFolderAdmin(p.isAdmin());
        oclPerm.setAllPermission(p.getFolderPermission(), p.getReadPermission(), p.getWritePermission(), p.getDeletePermission());
        oclPerm.setSystem(p.getSystem());
        oclPerm.setType(p.getType() == null ? FolderPermissionType.NORMAL: p.getType());
        oclPerm.setPermissionLegator(p.getPermissionLegator());
        return oclPerm;
    }

    private static OCLPermission newMaxPermissionFor(final int entity) {
        final OCLPermission oclPerm = new OCLPermission();
        oclPerm.setEntity(entity);
        oclPerm.setGroupPermission(false);
        oclPerm.setFolderAdmin(true);
        oclPerm.setAllPermission(ADMIN_PERMISSION, ADMIN_PERMISSION, ADMIN_PERMISSION, ADMIN_PERMISSION);
        oclPerm.setSystem(0);
        return oclPerm;
    }

    private static OCLPermission newStandardPermissionFor(final int entity) {
        final OCLPermission oclPerm = new OCLPermission();
        oclPerm.setEntity(entity);
        oclPerm.setGroupPermission(false);
        oclPerm.setFolderAdmin(false);
        oclPerm.setAllPermission(READ_FOLDER, READ_ALL_OBJECTS, WRITE_ALL_OBJECTS, DELETE_ALL_OBJECTS);
        oclPerm.setSystem(0);
        return oclPerm;
    }

    private static final int[] PUBLIC_FOLDER_IDS = { FolderObject.SYSTEM_PUBLIC_FOLDER_ID, FolderObject.SYSTEM_INFOSTORE_FOLDER_ID, FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID, FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID };

    /**
     * Determines the target folder type for new folders below a parent folder.
     *
     * @param module The module identifier of the new folder
     * @param parentId The ID of the parent folder
     * @param ctx The context
     * @param con A readable database connection
     * @return The folder type
     * @throws OXException
     * @throws OXException
     */
    private static int getFolderType(int module, final int parentId, final Context ctx, final Connection con) throws OXException {
        int type = -1;
        int pid = parentId;
        /*
         * Special treatment for system folders
         */
        if (pid == FolderObject.SYSTEM_SHARED_FOLDER_ID) {
            pid = FolderObject.SYSTEM_PRIVATE_FOLDER_ID;
            type = FolderObject.SHARED;
        } else if (pid == FolderObject.SYSTEM_PRIVATE_FOLDER_ID) {
            type = FolderObject.PRIVATE;
        } else if (Arrays.binarySearch(PUBLIC_FOLDER_IDS, pid) >= 0) {
            type = FolderObject.PUBLIC;
        } else {
            type = getFolderAccess(ctx, con).getFolderType(pid);
        }
        return type;
    }

    @Override
    public void clearFolder(final String treeId, final String folderId, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.WRITE, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final FolderObject fo = getFolderObject(Integer.parseInt(folderId), storageParameters.getContext(), con, storageParameters);
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create();
            }
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            folderManager.clearFolder(fo, true, System.currentTimeMillis());

            final List<OXException> warnings = folderManager.getWarnings();
            if (null != warnings) {
                for (final OXException warning : warnings) {
                    storageParameters.addWarning(warning);
                }
            }
        } finally {
            provider.close();
        }
    }

    @Override
    public void deleteFolder(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws OXException {
        int folderId = Integer.parseInt(folderIdentifier);

        ConnectionProvider provider = getConnection(Mode.WRITE, storageParameters);
        try {
            Connection con = provider.getConnection();
            FolderObject fo = new FolderObject();
            fo.setObjectID(folderId);
            Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create();
            }
            FolderServiceDecorator decorator = storageParameters.getDecorator();
            boolean hardDelete = null != decorator && (Boolean.TRUE.equals(decorator.getProperty("hardDelete")) || decorator.getBoolProperty("hardDelete"));
            OXFolderManager folderManager = OXFolderManager.getInstance(session, con, con);
            folderManager.deleteFolder(fo, true, System.currentTimeMillis(), hardDelete);

            // Cleanse from other folder storage, too
            if (hardDelete) {
                Delete.hardDeleteFolder(session.getContextId(), getUnsignedInteger(OutlookFolderStorage.OUTLOOK_TREE_ID), session.getUserId(), folderIdentifier, true, true, con);
            } else {
                Delete.deleteFolder(session.getContextId(), getUnsignedInteger(OutlookFolderStorage.OUTLOOK_TREE_ID), session.getUserId(), folderIdentifier, true, true, con);
            }

            List<OXException> warnings = folderManager.getWarnings();
            if (null != warnings) {
                for (final OXException warning : warnings) {
                    storageParameters.addWarning(warning);
                }
            }
        } finally {
            provider.close();
        }
    }

    @Override
    public String getDefaultFolderID(final User user, final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create();
            }
            final Context context = storageParameters.getContext();
            int folderId = -1;
            if (TaskContentType.getInstance().equals(contentType)) {
                folderId = OXFolderSQL.getUserDefaultFolder(session.getUserId(), FolderObject.TASK, con, context);
            } else if (CalendarContentType.getInstance().equals(contentType)) {
                folderId = OXFolderSQL.getUserDefaultFolder(session.getUserId(), FolderObject.CALENDAR, con, context);
            } else if (ContactsContentType.getInstance().equals(contentType)) {
                folderId = OXFolderSQL.getUserDefaultFolder(session.getUserId(), FolderObject.CONTACT, con, context);
            } else if (InfostoreContentType.getInstance().equals(contentType)) {
                if (TrashType.getInstance().equals(type)) {
                    folderId = OXFolderSQL.getUserDefaultFolder(user.getId(), FolderObject.INFOSTORE, FolderObject.TRASH, con, context);
                } else if (DocumentsType.getInstance().equals(type)) {
                    folderId = OXFolderSQL.getUserDefaultFolder(user.getId(), FolderObject.INFOSTORE, FolderObject.DOCUMENTS, con, context);
                } else if (TemplatesType.getInstance().equals(type)) {
                    folderId = OXFolderSQL.getUserDefaultFolder(user.getId(), FolderObject.INFOSTORE, FolderObject.TEMPLATES, con, context);
                } else if (VideosType.getInstance().equals(type)) {
                    folderId = OXFolderSQL.getUserDefaultFolder(user.getId(), FolderObject.INFOSTORE, FolderObject.VIDEOS, con, context);
                } else if (MusicType.getInstance().equals(type)) {
                    folderId = OXFolderSQL.getUserDefaultFolder(user.getId(), FolderObject.INFOSTORE, FolderObject.MUSIC, con, context);
                } else if (PicturesType.getInstance().equals(type)) {
                    folderId = OXFolderSQL.getUserDefaultFolder(user.getId(), FolderObject.INFOSTORE, FolderObject.PICTURES, con, context);
                } else {
                    folderId = OXFolderSQL.getUserDefaultFolder(user.getId(), FolderObject.INFOSTORE, con, context);
                }
            }
            if (-1 == folderId) {
                throw FolderExceptionErrorMessage.NO_DEFAULT_FOLDER.create(contentType, treeId);
            }
            return String.valueOf(folderId);
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public Type getTypeByParent(final User user, final String treeId, final String parentId, final StorageParameters storageParameters) throws OXException {
        /*
         * Special treatment for system folders
         */
        final int pid = Integer.parseInt(parentId);
        if (pid == FolderObject.SYSTEM_SHARED_FOLDER_ID) {
            return SharedType.getInstance();
        } else if (pid == FolderObject.SYSTEM_PRIVATE_FOLDER_ID) {
            return PrivateType.getInstance();
        } else if (Arrays.binarySearch(PUBLIC_FOLDER_IDS, pid) >= 0) {
            return PublicType.getInstance();
        } else {
            final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
            try {
                final FolderObject p = getFolderAccess(storageParameters.getContext(), provider.getConnection()).getFolderObject(pid);
                final int parentType = p.getType();
                if (FolderObject.PRIVATE == parentType) {
                    return p.getCreatedBy() == user.getId() ? PrivateType.getInstance() : SharedType.getInstance();
                } else if (FolderObject.PUBLIC == parentType) {
                    return PublicType.getInstance();
                } else if (FolderObject.TRASH == parentType) {
                    return TrashType.getInstance();
                }
            } finally {
                provider.close();
            }
        }
        return SystemType.getInstance();
    }

    @Override
    public boolean containsForeignObjects(final User user, final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final Context ctx = storageParameters.getContext();
            /*
             * A numeric folder identifier
             */
            final int folderId = getUnsignedInteger(folderIdentifier);
            if (folderId < 0) {
                throw OXFolderExceptionCode.NOT_EXISTS.create(folderIdentifier, Integer.valueOf(ctx.getContextId()));
            }
            if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                return false;
            } else if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                /*
                 * The system shared folder
                 */
                return false;
            } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
                /*
                 * The system public folder
                 */
                return false;
            } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
                /*
                 * The system infostore folder
                 */
                return false;
            } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == folderId) {
                /*
                 * The system private folder
                 */
                return false;
            } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                /*
                 * A virtual database folder
                 */
                return true;
            } else {
                /*
                 * A non-virtual database folder
                 */
                final OXFolderAccess folderAccess = getFolderAccess(ctx, con);
                return folderAccess.containsForeignObjects(getFolderObject(folderId, ctx, con, storageParameters), storageParameters.getSession(), ctx);
            }
        } finally {
            provider.close();
        }
    }

    @Override
    public boolean isEmpty(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final Context ctx = storageParameters.getContext();
            /*
             * A numeric folder identifier
             */
            final int folderId = getUnsignedInteger(folderIdentifier);
            if (folderId < 0) {
                throw OXFolderExceptionCode.NOT_EXISTS.create(folderIdentifier, Integer.valueOf(ctx.getContextId()));
            }
            if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                return true;
            } else if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                /*
                 * The system shared folder
                 */
                return true;
            } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
                /*
                 * The system public folder
                 */
                return true;
            } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
                /*
                 * The system infostore folder
                 */
                return true;
            } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == folderId) {
                /*
                 * The system private folder
                 */
                return true;
            } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                /*
                 * A virtual database folder
                 */
                return false;
            } else {
                /*
                 * A non-virtual database folder
                 */
                final OXFolderAccess folderAccess = getFolderAccess(ctx, con);
                return folderAccess.isEmpty(getFolderObject(folderId, ctx, con, storageParameters), storageParameters.getSession(), ctx);
            }
        } finally {
            provider.close();
        }
    }

    @Override
    public void updateLastModified(final long lastModified, final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws OXException {
        ConnectionProvider provider = getConnection(Mode.WRITE, storageParameters);
        try {
            Connection con = provider.getConnection();

            int folderId = getUnsignedInteger(folderIdentifier);
            if (getFolderAccess(storageParameters.getContext(), con).getFolderLastModified(folderId).after(new Date(lastModified))) {
                throw FolderExceptionErrorMessage.CONCURRENT_MODIFICATION.create();
            }

            FolderObject updateMe = new FolderObject();
            updateMe.setObjectID(folderId);
            updateMe.setDefaultFolder(false);
            updateMe.setLastModified(new Date(lastModified));
            updateMe.setModifiedBy(storageParameters.getUserId());

            UpdatedFolderHandler handler = updatedFolderHandlerFor(storageParameters);
            OXFolderManager folderManager = OXFolderManager.getInstance(storageParameters.getSession(), Collections.singleton(handler), con, con);

            folderManager.updateFolder(updateMe, false, false, lastModified);
        } finally {
            provider.close();
        }
    }

    @Override
    public Folder getFolder(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws OXException {
        return getFolder(treeId, folderIdentifier, StorageType.WORKING, storageParameters);
    }

    private static final int[] VIRTUAL_IDS = { FolderObject.VIRTUAL_LIST_TASK_FOLDER_ID, FolderObject.VIRTUAL_LIST_CALENDAR_FOLDER_ID, FolderObject.VIRTUAL_LIST_CONTACT_FOLDER_ID, FolderObject.VIRTUAL_LIST_INFOSTORE_FOLDER_ID };

    @Override
    public Folder getFolder(final String treeId, final String folderIdentifier, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final DatabaseFolder retval = loadFolder(folderIdentifier, storageType, storageParameters, con, treeId);

            if (storageParameters.getUser().isAnonymousGuest()) {
                handleAnonymousUser(retval, treeId, storageType, storageParameters, con);
            }
            return retval;
        } finally {
            provider.close();
        }
    }

    private DatabaseFolder loadFolder(final String folderIdentifier, final StorageType storageType, final StorageParameters storageParameters, final Connection con, final String treeId) throws OXException {
        final DatabaseFolder retval;
        final User user = storageParameters.getUser();
        final Context ctx = storageParameters.getContext();
        final UserPermissionBits userPermissionBits = getUserPermissionBits(con, storageParameters);
        if (StorageType.WORKING.equals(storageType)) {
            if (DatabaseFolderStorageUtility.hasSharedPrefix(folderIdentifier)) {
                retval = SharedPrefixFolder.getSharedPrefixFolder(folderIdentifier, user, ctx);
            } else {
                /*
                 * A numeric folder identifier
                 */
                final int folderId = getUnsignedInteger(folderIdentifier);

                if (folderId < 0) {
                    throw OXFolderExceptionCode.NOT_EXISTS.create(folderIdentifier, Integer.valueOf(ctx.getContextId()));
                }

                if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                    retval = SystemRootFolder.getSystemRootFolder();
                } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                    /*
                     * A virtual database folder
                     */
                    final boolean altNames = StorageParametersUtility.getBoolParameter("altNames", storageParameters);
                    retval = VirtualListFolder.getVirtualListFolder(folderId, altNames);
                } else {
                    /*
                     * A non-virtual database folder
                     */
                    final FolderObject fo = getFolderObject(folderId, ctx, con, storageParameters);
                    final boolean altNames = StorageParametersUtility.getBoolParameter("altNames", storageParameters);
                    boolean separateFederatedShares = StorageParametersUtility.getBoolParameter("separateFederatedShares", storageParameters);
                    retval = DatabaseFolderConverter.convert(fo, user, userPermissionBits, ctx, storageParameters.getSession(), altNames, separateFederatedShares, con);
                }
            }
        } else {
            /*
             * Get from backup tables
             */
            final int folderId = getUnsignedInteger(folderIdentifier);

            if (folderId < 0) {
                throw OXFolderExceptionCode.NOT_EXISTS.create(folderIdentifier, Integer.valueOf(ctx.getContextId()));
            }

            final FolderObject fo = FolderObject.loadFolderObjectFromDB(folderId, ctx, con, true, false, DEL_OXFOLDER_TREE, DEL_OXFOLDER_PERMISSIONS);
            retval = new DatabaseFolder(fo);
        }
        retval.setTreeID(treeId);
        return retval;
    }

    /**
     * Adjusts the parent folder and subfolders of the given folder. Only folder ids of folders for which the user has
     * non system permissions will be present.
     *
     * @param folder - The folder to adjust
     * @param treeId - The tree id, needed to load folders
     * @param storageType - The storage type to use
     * @param storageParameters - The storage parameters with needed user information
     * @param con - The connection to use for storage operations
     * @throws OXException - If loading a folder fails
     */
    private void handleAnonymousUser(final DatabaseFolder folder, final String treeId, final StorageType storageType, final StorageParameters storageParameters, final Connection con) throws OXException {
        final Type type = folder.getType();
        if (SystemType.getInstance().equals(type)) {
            return;
        }
        if (PublicType.getInstance().equals(type) && folder.isDefault() == false) {
            folder.setParentID(getFirstNonSystemVisibleParent(folder, treeId, storageType, storageParameters, con).getID());
        }
        folder.setSubfolderIDs(getNonHiddenSubfolders(folder, treeId, storageType, storageParameters).stream().map(f -> f.getID()).toArray(String[]::new));
        folder.setCacheable(false);
    }

    /**
     * Get the parent Folder for the given folder that references a system folder, a default folder or the next higher
     * folder for which the user has a non system permission.
     *
     * @param folder - The folder to adjust
     * @param treeId - The tree id, needed to load folders
     * @param storageType - The storage type to use
     * @param storageParameters - The storage parameters with needed user information
     * @param con - The connection to use for storage operations
     * @return The first visible folder without system permissions
     * @throws OXException - If loading a folder fails
     */
    private DatabaseFolder getFirstNonSystemVisibleParent(final DatabaseFolder folder, final String treeId, final StorageType storageType, final StorageParameters storageParameters, final Connection con) throws OXException {
        DatabaseFolder parentFolder = loadFolder(folder.getParentID(), storageType, storageParameters, con, treeId);
        final String parentID = parentFolder.getParentID();
        for (int max = 256; max-- > 0 && !FolderStorage.ROOT_ID.equals(parentID) && null != parentID && parentFolder.isHidden();) {
            parentFolder = loadFolder(parentFolder.getParentID(), storageType, storageParameters, con, treeId);
        }
        return parentFolder;
    }

    /**
     * Get the subfolders for the given folder, for which
     * the current user has not only visible but other non system permissions.
     *
     * @param folder - The folder to start with
     * @param treeId - The tree id, needed to load folders
     * @param storageType - The storage type to use
     * @param storageParameters - The storage parameters with needed user information
     * @return Return a list of all folders that are visible to the user with non system permissions or an empty list
     * @throws OXException - If loading a folder fails
     */
    private List<Folder> getNonHiddenSubfolders(Folder folder, String treeId, StorageType storageType, StorageParameters storageParameters) throws OXException {
        /*
         * gather all subfolder identifiers
         */
        String[] subfolderIDs = folder.getSubfolderIDs();
        if (null == subfolderIDs) {
            SortableId[] sortableIds = getSubfolders(treeId, folder.getID(), storageParameters);
            subfolderIDs = new String[sortableIds.length];
            for (int i = 0; i < sortableIds.length; i++) {
                subfolderIDs[i] = sortableIds[i].getId();
            }
        }
        if (0 == subfolderIDs.length) {
            return Collections.emptyList();
        }
        /*
         * collect all non-hidden subfolders recursively
         */
        List<Folder> resultingSubfolders = new ArrayList<>();
        for (Folder subfolder : getFolders(treeId, Arrays.asList(folder.getSubfolderIDs()), storageType, storageParameters)) {
            if ((subfolder instanceof DatabaseFolder) && ((DatabaseFolder) subfolder).isHidden()) {
                /*
                 * this subfolder is hidden, recursively collect remaining non-hidden folders from subtree
                 */
                if (((DatabaseFolder) subfolder).isVisibleThroughSystemPermissions()) {
                    resultingSubfolders.addAll(getNonHiddenSubfolders(subfolder, treeId, storageType, storageParameters));
                }
            } else {
                /*
                 * add non-hidden subfolder
                 */
                resultingSubfolders.add(subfolder);
            }
        }
        return resultingSubfolders;
    }

    @Override
    public Folder prepareFolder(final String treeId, final Folder folder, final StorageParameters storageParameters) throws OXException {
        /*
         * Owner
         */
        final int owner = folder.getCreatedBy();
        if (owner < 0) {
            return folder;
        }
        /*
         * Check shared...
         */
        if (owner != storageParameters.getUserId() && PrivateType.getInstance().equals(folder.getType())) {
            try {
                return getFolder(treeId, folder.getID(), StorageType.WORKING, storageParameters);
            } catch (OXException e) {
                if (OXFolderExceptionCode.NOT_EXISTS.equals(e) || FolderExceptionErrorMessage.NOT_FOUND.equals(e)) {
                    return getFolder(treeId, folder.getID(), StorageType.BACKUP, storageParameters);
                }
                throw e;
            }
        }
        return folder;
    }

    @Override
    public List<Folder> getFolders(final String treeId, final List<String> folderIdentifiers, final StorageParameters storageParameters) throws OXException {
        return getFolders(treeId, folderIdentifiers, StorageType.WORKING, storageParameters);
    }

    @Override
    public List<Folder> getFolders(final String treeId, final List<String> folderIdentifiers, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        ConnectionProvider provider = null;
        try {
            final User user = storageParameters.getUser();
            final Context ctx = storageParameters.getContext();
            final boolean altNames = StorageParametersUtility.getBoolParameter("altNames", storageParameters);
            /*
             * Either from working or from backup storage type
             */
            if (StorageType.WORKING.equals(storageType)) {
                final int size = folderIdentifiers.size();
                final Folder[] ret = new Folder[size];
                final TIntIntMap map = new TIntIntHashMap(size);
                /*
                 * Check for special folder identifier
                 */
                for (int index = 0; index < size; index++) {
                    final String folderIdentifier = folderIdentifiers.get(index);
                    if (DatabaseFolderStorageUtility.hasSharedPrefix(folderIdentifier)) {
                        ret[index] = SharedPrefixFolder.getSharedPrefixFolder(folderIdentifier, user, ctx);
                    } else {
                        /*
                         * A numeric folder identifier
                         */
                        final int folderId = getUnsignedInteger(folderIdentifier);
                        if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                            ret[index] = SystemRootFolder.getSystemRootFolder();
                        } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                            ret[index] = VirtualListFolder.getVirtualListFolder(folderId, altNames);
                        } else {
                            map.put(folderId, index);
                        }
                    }
                }
                /*
                 * Batch load
                 */
                if (0 < map.size()) {
                    provider = getConnection(Mode.READ, storageParameters);
                    try {
                        Connection con = provider.getConnection();
                        Session session = storageParameters.getSession();
                        UserPermissionBits userPermissionBits = getUserPermissionBits(con, storageParameters);
                        boolean separateFederatedShares = StorageParametersUtility.getBoolParameter("separateFederatedShares", storageParameters);
                        for (FolderObject folder : getFolderObjects(map.keys(), ctx, con, storageParameters)) {
                            if (null != folder) {
                                int index = map.get(folder.getObjectID());
                                ret[index] = DatabaseFolderConverter.convert(folder, user, userPermissionBits, ctx, session, altNames, separateFederatedShares, con);
                            }
                        }
                    } finally {
                        if (null != provider) {
                            provider.close();
                            provider = null;
                        }
                    }
                }
                /*
                 * Set proper tree identifier
                 */
                for (final Folder folder : ret) {
                    if (null != folder) {
                        folder.setTreeID(treeId);
                    }
                }
                /*
                 * Return
                 */
                final int length = ret.length;
                final List<Folder> l = new ArrayList<Folder>(length);
                for (int i = 0; i < length; i++) {
                    final Folder folder = ret[i];
                    if (null != folder) {
                        l.add(folder);
                    } else {
                        storageParameters.addWarning(FolderExceptionErrorMessage.NOT_FOUND.create(Integer.valueOf(folderIdentifiers.get(i)), treeId));
                    }
                }
                return l;
            }
            /*
             * Get from backup tables
             */
            final TIntList list = new TIntArrayList(folderIdentifiers.size());
            for (final String folderIdentifier : folderIdentifiers) {
                list.add(getUnsignedInteger(folderIdentifier));
            }
            provider = getConnection(Mode.READ, storageParameters);
            final Connection con = provider.getConnection();
            final List<FolderObject> folders = OXFolderBatchLoader.loadFolderObjectsFromDB(list.toArray(), ctx, con, true, false, DEL_OXFOLDER_TREE, DEL_OXFOLDER_PERMISSIONS);
            provider.close();
            provider = null;
            final int size = folders.size();
            final List<Folder> ret = new ArrayList<Folder>(size);
            for (int i = 0; i < size; i++) {
                final FolderObject fo = folders.get(i);
                if (null == fo) {
                    storageParameters.addWarning(FolderExceptionErrorMessage.NOT_FOUND.create(Integer.valueOf(list.get(i)), treeId));
                } else {
                    final DatabaseFolder df = new DatabaseFolder(fo);
                    df.setTreeID(treeId);
                    ret.add(df);
                }
            }
            return ret;
        } finally {
            if (null != provider) {
                provider.close();
            }
        }
    }

    @Override
    public FolderType getFolderType() {
        return DatabaseFolderType.getInstance();
    }

    @Override
    public SortableId[] getVisibleFolders(String rootFolderId, String treeId, ContentType contentType, Type type, StorageParameters storageParameters) throws OXException {
        final User user = storageParameters.getUser();
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final int userId = user.getId();
            final Context ctx = storageParameters.getContext();
            final UserPermissionBits userPermissionBits = getUserPermissionBits(con, storageParameters);
            final int iType = getTypeByFolderTypeWithShared(type);
            final int iModule = getModuleByContentType(contentType);
            final List<FolderObject> list;
            if (Strings.isEmpty(rootFolderId)) {
                list = ((FolderObjectIterator) OXFolderIteratorSQL.getAllVisibleFoldersIteratorOfType(userId, user.getGroups(), userPermissionBits.getAccessibleModules(), iType, new int[] { iModule }, ctx)).asList();
            } else {
                final int iParent = Integer.parseInt(rootFolderId);
                list = ((FolderObjectIterator) OXFolderIteratorSQL.getAllVisibleFoldersIteratorOfType(userId, user.getGroups(), userPermissionBits.getAccessibleModules(), iType, new int[] { iModule }, iParent, ctx)).asList();
            }
            if (FolderObject.PRIVATE == iType) {
                /*
                 * Remove shared ones manually
                 */
                for (final Iterator<FolderObject> iterator = list.iterator(); iterator.hasNext();) {
                    if (iterator.next().getCreatedBy() != userId) {
                        iterator.remove();
                    }
                }
            } else if (FolderObject.PUBLIC == iType && FolderObject.CONTACT == iModule) {
                try {
                    /*
                     * Add global address book manually
                     */
                    final CapabilityService capsService = ServerServiceRegistry.getInstance().getService(CapabilityService.class);
                    if (null == capsService || capsService.getCapabilities(user.getId(), ctx.getContextId()).contains("gab")) {
                        final FolderObject gab = getFolderObject(FolderObject.SYSTEM_LDAP_FOLDER_ID, ctx, con, storageParameters);
                        if (gab.isVisible(userId, userPermissionBits)) {
                            gab.setFolderName(GlobalAddressBookUtils.getFolderName(user.getLocale(), ctx.getContextId()));
                            list.add(gab);
                        }
                    }
                } catch (RuntimeException e) {
                    throw OXFolderExceptionCode.RUNTIME_ERROR.create(e, Integer.valueOf(ctx.getContextId()));
                }
            }
            /*
             * localize & sort folders
             */
            localizeFolderNames(list, user.getLocale());
            if (FolderObject.PRIVATE == iType) {
                /*
                 * Sort them by default-flag and name: <user's default folder>, <aaa>, <bbb>, ... <zzz>
                 */
                Collections.sort(list, new FolderObjectComparator(user.getLocale(), ctx));
            } else {
                /*
                 * Sort them by name only
                 */
                if (FolderObject.SHARED == iType) {
                    /*
                     * Pre-load users with current connection
                     */
                    Map<String, Integer> tracker = new HashMap<String, Integer>(list.size());
                    for (FolderObject folder : list) {
                        Integer pre = tracker.get(folder.getFolderName());
                        if (null == pre) {
                            int owner = folder.getCreatedBy();
                            if (owner > 0) {
                                tracker.put(folder.getFolderName(), Integer.valueOf(owner));
                            }
                        } else {
                            int owner = folder.getCreatedBy();
                            if (owner > 0) {
                                int otherOwner = pre.intValue();
                                if (otherOwner != owner) {
                                    UserStorage.getInstance().loadIfAbsent(otherOwner, ctx, con);
                                    UserStorage.getInstance().loadIfAbsent(owner, ctx, con);
                                }
                            }
                        }
                    }
                }
                Collections.sort(list, new FolderNameComparator(user.getLocale(), storageParameters.getContext()));
            }
            /*
             * return sortable identifiers
             */
            return extractIDs(list);
        } finally {
            provider.close();
        }
    }

    @Override
    public SortableId[] getVisibleFolders(final String treeId, final ContentType contentType, final Type type, final StorageParameters storageParameters) throws OXException {
        return getVisibleFolders(null, treeId, contentType, type, storageParameters);
    }

    @Override
    public SortableId[] getUserSharedFolders(String treeId, ContentType contentType, StorageParameters storageParameters) throws OXException {
        ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        SearchIterator<FolderObject> searchIterator = null;
        try {
            /*
             * load & filter user shared folders of content type
             */
            Connection connection = provider.getConnection();
            User user = storageParameters.getUser();
            UserPermissionBits userPermissionBits = getUserPermissionBits(connection, storageParameters);
            searchIterator = OXFolderIteratorSQL.getAllVisibleFoldersIteratorOfModule(user.getId(), user.getGroups(), userPermissionBits.getAccessibleModules(), getModuleByContentType(contentType), storageParameters.getContext(), connection);
            List<FolderObject> folders = getUserSharedFolders(searchIterator, connection, storageParameters);
            /*
             * localize / sort folders & return their sortable identifiers
             */
            if (0 == folders.size()) {
                return new SortableId[0];
            }
            localizeFolderNames(folders, user.getLocale());
            if (1 < folders.size()) {
                Collections.sort(folders, new FolderObjectComparator(user.getLocale(), storageParameters.getContext()));
            }
            return extractIDs(folders);
        } finally {
            SearchIterators.close(searchIterator);
            provider.close();
        }
    }

    @Override
    public SortableId[] getSubfolders(final String treeId, final String parentIdentifier, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final User user = storageParameters.getUser();
            final Context ctx = storageParameters.getContext();
            final Connection con = provider.getConnection();
            final UserPermissionBits userPermissionBits = getUserPermissionBits(con, storageParameters);
            if (DatabaseFolderStorageUtility.hasSharedPrefix(parentIdentifier)) {
                final List<FolderIdNamePair> subfolderIds = SharedPrefixFolder.getSharedPrefixFolderSubfolders(parentIdentifier, user, userPermissionBits, ctx, con);
                final List<SortableId> list = new ArrayList<>(subfolderIds.size());
                int i = 0;
                for (final FolderIdNamePair props : subfolderIds) {
                    list.add(new DatabaseId(props.fuid, i++, props.name));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            final int parentId = Integer.parseInt(parentIdentifier);

            if (FolderObject.SYSTEM_ROOT_FOLDER_ID == parentId) {
                final List<String[]> subfolderIds = SystemRootFolder.getSystemRootFolderSubfolder(user.getLocale());
                final List<SortableId> list = new ArrayList<>(subfolderIds.size());
                int i = 0;
                for (final String[] sa : subfolderIds) {
                    list.add(new DatabaseId(sa[0], i++, sa[1]));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (Arrays.binarySearch(VIRTUAL_IDS, parentId) >= 0) {
                /*
                 * A virtual database folder
                 */
                final List<String[]> subfolderIds = VirtualListFolder.getVirtualListFolderSubfolders(parentId, user, userPermissionBits, ctx, con);
                final int size = subfolderIds.size();
                final List<SortableId> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    final String[] sa = subfolderIds.get(i);
                    list.add(new DatabaseId(sa[0], i, sa[1]));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == parentId) {
                /*
                 * The system private folder
                 */
                final List<String[]> subfolderIds = SystemPrivateFolder.getSystemPrivateFolderSubfolders(user, userPermissionBits, ctx, con);
                final int size = subfolderIds.size();
                final List<SortableId> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    final String[] sa = subfolderIds.get(i);
                    list.add(new DatabaseId(sa[0], i, sa[1]));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_SHARED_FOLDER_ID == parentId) {
                /*
                 * The system shared folder
                 */
                final List<String[]> subfolderIds = SystemSharedFolder.getSystemSharedFolderSubfolder(user, userPermissionBits, ctx, con);
                final int size = subfolderIds.size();
                final List<SortableId> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    final String[] sa = subfolderIds.get(i);
                    list.add(new DatabaseId(sa[0], i, sa[1]));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == parentId) {
                /*
                 * The system public folder
                 */
                final List<String[]> subfolderIds = SystemPublicFolder.getSystemPublicFolderSubfolders(user, userPermissionBits, ctx, con);
                final int size = subfolderIds.size();
                final List<SortableId> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    final String[] sa = subfolderIds.get(i);
                    list.add(new DatabaseId(sa[0], i, sa[1]));
                }
                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == parentId) {
                /*
                 * The system infostore folder
                 */
                final Session s = storageParameters.getSession();

                List<String[]> l = null;
                if (REAL_TREE_ID.equals(treeId)) {
                    /*
                     * File storage accounts
                     */
                    final Queue<FileStorageAccount> fsAccounts = new ConcurrentLinkedQueue<>();
                    final FileStorageServiceRegistry fsr = Services.getService(FileStorageServiceRegistry.class);
                    if (null == fsr) {
                        // Do nothing
                    } else {
                        CompletionService<Void> completionService = new CallerRunsCompletionService<>();
                        int taskCount = 0;
                        try {
                            final List<FileStorageService> allServices = fsr.getAllServices();
                            boolean separateFederatedShares = StorageParametersUtility.getBoolParameter("separateFederatedShares", storageParameters);
                            for (final FileStorageService fsService : allServices) {
                                if (false == separateFederatedShares && (fsService instanceof SharingFileStorageService)) {
                                    continue; // federated shares will be mounted below folders 10 and 15
                                }
                                Callable<Void> task = new Callable<Void>() {

                                    @Override
                                    public Void call() throws Exception {
                                        /*
                                         * Check if file storage service provides a root folder
                                         */
                                        List<FileStorageAccount> userAccounts = null;
                                        if (fsService instanceof AccountAware aa) {
                                            userAccounts = aa.getAccounts(s);
                                        }
                                        if (null == userAccounts) {
                                            userAccounts = fsService.getAccountManager().getAccounts(s);
                                        }
                                        for (final FileStorageAccount userAccount : userAccounts) {
                                            if ("infostore".equals(userAccount.getId()) || FileStorageAccount.DEFAULT_ID.equals(userAccount.getId())) {
                                                // Ignore infostore file storage and default account
                                                continue;
                                            }
                                            fsAccounts.add(userAccount);
                                        }
                                        return null;
                                    }
                                };
                                completionService.submit(task);
                                taskCount++;
                            }
                        } catch (OXException e) {
                            LOG.error("", e);
                        }
                        for (int i = taskCount; i-- > 0;) {
                            completionService.take();
                        }
                        if (fsAccounts.isEmpty()) {
                            // Do nothing
                        } else {
                            l = new LinkedList<>();
                            List<FileStorageAccount> accountList = new ArrayList<>(fsAccounts);
                            Collections.sort(accountList, new FileStorageAccountComparator(user.getLocale()));
                            final int sz = accountList.size();
                            final String fid = FileStorageFolder.ROOT_FULLNAME;
                            for (int i = 0; i < sz; i++) {
                                final FileStorageAccount fsa = accountList.get(i);
                                final String serviceId;
                                if (fsa instanceof com.openexchange.file.storage.ServiceAware srvAware) {
                                    serviceId = srvAware.getServiceId();
                                } else {
                                    final FileStorageService tmp = fsa.getFileStorageService();
                                    serviceId = null == tmp ? null : tmp.getId();
                                }
                                FolderID folderID = new FolderID(serviceId, fsa.getId(), fid);
                                l.add(new String[] { folderID.toUniqueID(), fsa.getDisplayName() });
                            }
                        }
                    }
                }

                boolean altNames = StorageParametersUtility.getBoolParameter("altNames", storageParameters);
                List<String[]> subfolderIds = SystemInfostoreFolder.getSystemInfostoreFolderSubfolders(user, userPermissionBits, ctx, altNames, storageParameters.getSession(), con);

                List<SortableId> list = new ArrayList<>(subfolderIds.size() + (null == l ? 0 : l.size()));
                int in = 0;
                for (String[] sa : subfolderIds) {
                    list.add(new DatabaseId(sa[0], in++, sa[1]));
                }

                if (null != l) {
                    for (String[] sa : l) {
                        list.add(new DatabaseId(sa[0], in++, sa[1]));
                    }
                }

                return list.toArray(new SortableId[list.size()]);
            }

            if (FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID == parentId) {
                if (!InfostoreFacades.isInfoStoreAvailable()) {
                    final Session session = storageParameters.getSession();
                    final FileStorageAccount defaultAccount = DatabaseFolderConverter.getDefaultFileStorageAccess(session);
                    if (null != defaultAccount) {
                        final FileStorageService fileStorageService = defaultAccount.getFileStorageService();
                        final String defaultId = FileStorageAccount.DEFAULT_ID;
                        final FileStorageAccountAccess defaultFileStorageAccess = fileStorageService.getAccountAccess(defaultId, session);
                        defaultFileStorageAccess.connect();
                        try {
                            final FileStorageFolder personalFolder = defaultFileStorageAccess.getFolderAccess().getPersonalFolder();
                            FolderID folderID = new FolderID(fileStorageService.getId(), defaultAccount.getId(), personalFolder.getId());
                            return new SortableId[] { new DatabaseId(folderID.toUniqueID(), 0, personalFolder.getName()) };
                            // TODO: Shared?
                        } finally {
                            defaultFileStorageAccess.close();
                        }
                    }
                }
                else {
                    List<SortableId> ret = new ArrayList<>();
                    int ordinal = 0;

                    /*
                     * User-sensitive loading of user infostore folder
                     */
                    final TIntList subfolders = OXFolderIteratorSQL.getVisibleSubfolders(parentId, storageParameters.getUserId(), user.getGroups(), getUserPermissionBits(con, storageParameters).getAccessibleModules(), ctx, con);
                    for (int i = 0; i < subfolders.size(); i++) {
                        ret.add(new DatabaseId(subfolders.get(i), ordinal++, null));
                    }

                    /*
                     * Add federal sharing folders unless already added below folder 9
                     */
                    if (false == StorageParametersUtility.getBoolParameter("separateFederatedShares", storageParameters)) {
                        final boolean forceRetry = StorageParametersUtility.getBoolParameter("forceRetry", storageParameters);
                        SortableId[] sharedFolders = FederatedSharingFolders.getFolders(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID, storageParameters.getSession(), forceRetry);
                        for (SortableId sharedFolder : sharedFolders) {
                            ret.add(new FileStorageId(sharedFolder.getId(), ordinal++, sharedFolder.getName()));
                        }
                    }
                    return ret.toArray(new SortableId[ret.size()]);
                }
            }

            if (FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID == parentId) {
                if (!InfostoreFacades.isInfoStoreAvailable()) {
                    final Session session = storageParameters.getSession();
                    final FileStorageAccount defaultAccount = DatabaseFolderConverter.getDefaultFileStorageAccess(session);
                    if (null != defaultAccount) {
                        final FileStorageService fileStorageService = defaultAccount.getFileStorageService();
                        final String defaultId = FileStorageAccount.DEFAULT_ID;
                        final FileStorageAccountAccess defaultFileStorageAccess = fileStorageService.getAccountAccess(defaultId, session);
                        defaultFileStorageAccess.connect();
                        try {
                            final FileStorageFolder[] publicFolders = defaultFileStorageAccess.getFolderAccess().getPublicFolders();
                            final SortableId[] ret = new SortableId[publicFolders.length];
                            final String serviceId = fileStorageService.getId();
                            final String accountId = defaultAccount.getId();
                            for (int i = 0; i < publicFolders.length; i++) {
                                final FileStorageFolder folder = publicFolders[i];
                                FolderID folderID = new FolderID(serviceId, accountId, folder.getId());
                                ret[i] = new DatabaseId(folderID.toUniqueID(), i, folder.getName());
                            }
                            return ret;
                        } finally {
                            defaultFileStorageAccess.close();
                        }
                    }
                }

                List<SortableId> ret = new ArrayList<SortableId>();
                int ordinal = 0;

                /*
                 * User-sensitive loading of public infostore folder
                 */
                final TIntList subfolders = OXFolderIteratorSQL.getVisibleSubfolders(parentId, storageParameters.getUserId(), user.getGroups(), getUserPermissionBits(con, storageParameters).getAccessibleModules(), ctx, con);
                for (int i = 0; i < subfolders.size(); i++) {
                    ret.add(new DatabaseId(subfolders.get(i), ordinal++, null));
                }

                /*
                 * Add federal sharing folders unless already added below folder 9
                 */
                if (false == StorageParametersUtility.getBoolParameter("separateFederatedShares", storageParameters)) {
                    final boolean forceRetry = StorageParametersUtility.getBoolParameter("forceRetry", storageParameters);
                    SortableId[] sharedFolders = FederatedSharingFolders.getFolders(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID, storageParameters.getSession(), forceRetry);
                    for (SortableId sharedFolder : sharedFolders) {
                        ret.add(new FileStorageId(sharedFolder.getId(), ordinal++, sharedFolder.getName()));
                    }
                }
                return ret.toArray(new SortableId[ret.size()]);
            }

            /*-
             * IDs already sorted by default_flag DESC, fname
             *
             * TODO: Ensure locale-specific ordering is maintained
             */
            final boolean doDBSorting = true;
            if (doDBSorting) {
                final List<IdAndName> idAndNames = OXFolderLoader.getSubfolderIdAndNames(parentId, storageParameters.getContext(), con);
                final int size = idAndNames.size();
                final List<SortableId> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    final IdAndName idAndName = idAndNames.get(i);
                    list.add(new DatabaseId(idAndName.getFolderId(), i, idAndName.getName()));
                }
                return list.toArray(new SortableId[size]);
            }
            /*
             * Ensure locale-specific ordering is maintained
             */
            final List<FolderObject> subfolders;
            {
                final List<Integer> subfolderIds = FolderObject.getSubfolderIds(parentId, storageParameters.getContext(), con);
                final int[] arr = new int[subfolderIds.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = subfolderIds.get(i).intValue();
                }
                subfolders = getFolderObjects(arr, storageParameters.getContext(), con, storageParameters);
            }
            final ServerSession session;
            {
                final Session s = storageParameters.getSession();
                if (null == s) {
                    throw FolderExceptionErrorMessage.MISSING_SESSION.create();
                }
                session = ServerSessionAdapter.valueOf(s);
            }
            Collections.sort(subfolders, new FolderObjectComparator(session.getUser().getLocale(), storageParameters.getContext()));
            final int size = subfolders.size();
            final List<SortableId> list = new ArrayList<SortableId>(size);
            for (int i = 0; i < size; i++) {
                final FolderObject folderObject = subfolders.get(i);
                list.add(new DatabaseId(folderObject.getObjectID(), i, folderObject.getFolderName()));
            }
            return list.toArray(new SortableId[size]);
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } catch (InterruptedException e) {
            // Keep interrupted state
            Thread.currentThread().interrupt();
            throw FolderExceptionErrorMessage.UNEXPECTED_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    @Override
    public void rollback(final StorageParameters params) {
        final ConnectionMode con;
        try {
            con = optParameter(ConnectionMode.class, PARAM_CONNECTION, params);
        } catch (OXException e) {
            LOG.error("", e);
            return;
        }
        if (null == con) {
            return;
        }

        if (!con.controlledExternally) {
            params.putParameter(getFolderType(), PARAM_CONNECTION, null);
            if (con.isWritable()) {
                try {
                    Databases.rollback(con.connection);
                } finally {
                    Databases.autocommit(con.connection);
                    final DatabaseService databaseService = services.getService(DatabaseService.class);
                    if (null != databaseService) {
                        con.close(databaseService, params.getContext().getContextId());
                    }
                }
            } else {
                final DatabaseService databaseService = services.getService(DatabaseService.class);
                if (null != databaseService) {
                    databaseService.backReadOnly(params.getContext(), con.connection);
                }
            }
        }

        params.putParameter(getFolderType(), PARAM_CONNECTION, null);
    }

    @Override
    public boolean startTransaction(final StorageParameters parameters, final boolean modify) throws OXException {
        return startTransaction(parameters, modify ? Mode.WRITE : Mode.READ);
    }

    @Override
    public boolean startTransaction(final StorageParameters parameters, final Mode mode) throws OXException {
        final FolderType folderType = getFolderType();
        try {
            final DatabaseService databaseService = services.getService(DatabaseService.class);
            final Context context = parameters.getContext();
            ConnectionMode connectionMode = parameters.getParameter(folderType, PARAM_CONNECTION);
            if (null != connectionMode) {
                if (connectionMode.supports(mode)) {
                    // Connection already present in proper access mode
                    return false;
                }
                /*-
                 * Connection in wrong access mode:
                 *
                 * commit, restore auto-commit & push to pool
                 */
                if (!connectionMode.controlledExternally) {
                    parameters.putParameter(folderType, PARAM_CONNECTION, null);
                    if (connectionMode.isWritable()) {
                        try {
                            connectionMode.connection.commit();
                        } catch (Exception e) {
                            // Ignore
                            Databases.rollback(connectionMode.connection);
                        }
                        Databases.autocommit(connectionMode.connection);
                    }
                    connectionMode.close(databaseService, context.getContextId());
                }
            }

            Connection connection = null;
            FolderServiceDecorator decorator = parameters.getDecorator();
            if (decorator != null) {
                Object obj = decorator.getProperty(Connection.class.getName());
                if (obj instanceof Connection con) {
                    connection = con;
                }
            }

            if (WRITEES.contains(mode)) {
                if (connection == null || connection.isReadOnly()) {
                    connectionMode = new ConnectionMode(databaseService.getWritable(context), mode);
                    connectionMode.connection.setAutoCommit(false);
                } else {
                    connectionMode = new ConnectionMode(connection, mode, true);
                }
            } else {
                if (connection == null) {
                    connectionMode = new ConnectionMode(databaseService.getReadOnly(context), mode);
                } else {
                    connectionMode = new ConnectionMode(connection, mode, true);
                }
            }
            // Put to parameters
            if (parameters.putParameterIfAbsent(folderType, PARAM_CONNECTION, connectionMode)) {
                // Success
            } else {
                // Fail
                if (!connectionMode.controlledExternally) {
                    if (connectionMode.isWritable()) {
                        connectionMode.connection.setAutoCommit(true);
                    }
                    connectionMode.close(databaseService, context.getContextId());
                }
            }

            if (TransactionManager.isManagedTransaction(parameters)) {
                TransactionManager.getTransactionManager(parameters).transactionStarted(this);
                return false;
            }

            return true;
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public void updateFolder(final Folder folder, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.WRITE, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final Session session = storageParameters.getSession();
            if (null == session) {
                throw FolderExceptionErrorMessage.MISSING_SESSION.create();
            }

            final String id = folder.getID();
            final Context context = storageParameters.getContext();

            if (DatabaseFolderStorageUtility.hasSharedPrefix(id)) {
                final int owner = Integer.parseInt(id.substring(FolderObject.SHARED_PREFIX.length()));
                throw OXFolderExceptionCode.NO_ADMIN_ACCESS.create(Integer.valueOf(session.getUserId()), UserStorage.getInstance().getUser(owner, context).getDisplayName(), Integer.valueOf(context.getContextId()));
            }

            final int folderId = Integer.parseInt(id);
            FolderObject originalFolder = getFolderObject(folderId, context, con, storageParameters);
            boolean changedFolder = false;

            /*
             * Check for concurrent modification
             */
            {
                final Date clientLastModified = storageParameters.getTimeStamp();
                if (null != clientLastModified && getFolderAccess(context, con).getFolderLastModified(folderId).after(clientLastModified)) {
                    throw FolderExceptionErrorMessage.CONCURRENT_MODIFICATION.create();
                }
            }

            final Date millis = new Date();

            final FolderObject updateMe = new FolderObject();
            updateMe.setObjectID(folderId);
            updateMe.setDefaultFolder(false);
            {
                final String name = folder.getName();
                if (null != name) {
                    updateMe.setFolderName(name);
                    changedFolder = changedFolder || false == updateMe.getFolderName().equals(originalFolder.getFolderName());
                }
            }
            updateMe.setLastModified(millis);
            folder.setLastModified(millis);
            updateMe.setModifiedBy(session.getUserId());
            {
                final ContentType ct = folder.getContentType();
                if (null != ct) {
                    updateMe.setModule(getModuleByContentType(ct));
                    changedFolder = changedFolder || updateMe.getModule() != originalFolder.getModule();
                }
            }
            {
                final String parentId = folder.getParentID();
                if (null == parentId) {
                    updateMe.setParentFolderID(originalFolder.getParentFolderID());
                } else {
                    if (DatabaseFolderStorageUtility.hasSharedPrefix(parentId)) {
                        updateMe.setParentFolderID(originalFolder.getParentFolderID());
                    } else {
                        updateMe.setParentFolderID(Integer.parseInt(parentId));
                    }
                    changedFolder = changedFolder || updateMe.getParentFolderID() != originalFolder.getParentFolderID();
                }
            }
            {
                FolderPath originPath = folder.getOriginPath();
                if (null != originPath) {
                    updateMe.setOriginPath(originPath.isEmpty() ? FolderPathObject.EMPTY_PATH : FolderPathObject.copyOf(originPath));
                    changedFolder = changedFolder || false == updateMe.getOriginPath().equals(originalFolder.getOriginPath());
                }
            }
            {
                final Type t = folder.getType();
                if (null != t) {
                    updateMe.setType(getTypeByFolderType(t));
                    changedFolder = changedFolder || updateMe.getType() != originalFolder.getType();
                }
            }
            // Meta
            {
                final Map<String, Object> meta = folder.getMeta();
                if (null != meta) {
                    updateMe.setMeta(meta);
                    changedFolder = changedFolder || false == updateMe.getMeta().equals(originalFolder.getMeta());
                }
            }
            // Permissions
            Permission[] perms = folder.getPermissions();
            if (null != perms && storageParameters.getUser().isGuest() && new ComparedFolderPermissions(
                session, perms, DatabasePermission.fromOCLPermissions(originalFolder.getPermissionsAsArray())).hasChanges()) {
                throw FolderExceptionErrorMessage.UPDATE_NOT_PERMITTED.create(folder.getID(), I(session.getUserId()), I(session.getContextId()));
            }
            UpdatedFolderHandler handler = updatedFolderHandlerFor(storageParameters);
            final OXFolderManager folderManager = OXFolderManager.getInstance(session, Collections.singleton(handler), con, con);
            FolderObject parent = getFolderObject(updateMe.getParentFolderID(), context, con, storageParameters);
            if (null != perms) {
                    final List<OCLPermission> oclPermissions = new ArrayList<>(perms.length);
                    for (Permission p: perms) {
                        oclPermissions.add(newOCLPermissionFor(p));
                    }
                    Optional<List<OCLPermission>> optNewPermissions = processInheritedPermissions(parent, Optional.of(oclPermissions), Optional.empty());
                    updateMe.setPermissions(optNewPermissions.orElse(oclPermissions));
                    changedFolder = changedFolder || false == updateMe.getPermissions().equals(originalFolder.getPermissions());
            } else {
                ServerSession serverSession;
                if (context != null) {
                    serverSession = ServerSessionAdapter.valueOf(session, context, storageParameters.getUser());
                } else {
                    serverSession = ServerSessionAdapter.valueOf(session);
                }

                // Check folder update is a move operation and change permission according to MoveFolderPermissionMode
                if (updateMe.getParentFolderID() != originalFolder.getParentFolderID()) {
                    MoveFolderPermissionMode mode = new FolderMoveWarningCollector(serverSession, storageParameters).getMoveFolderPermissionMode(folder.getParentID());

                    switch (mode) {
                        case INHERIT:
                            inheritFolderPermissions(updateMe, parent, context, con, storageParameters, folderManager, millis, false);
                            changedFolder = changedFolder || false == updateMe.getPermissions().equals(originalFolder.getPermissions());
                            break;
                        case MERGE:
                            inheritFolderPermissions(updateMe, parent, context, con, storageParameters, folderManager, millis, true);
                            changedFolder = changedFolder || false == updateMe.getPermissions().equals(originalFolder.getPermissions());
                            break;
                        case KEEP:
                        default:
                            cascadeInheritedPermissions(context, con, folderManager, storageParameters, updateMe, parent, millis);
                            break;
                    }
                }
            }

            // Perform update of folder in storage if actually changed, otherwise update last modification time if permissions are sufficient
            if (changedFolder) {
                folderManager.updateFolder(updateMe, true, StorageParametersUtility.isHandDownPermissions(storageParameters), millis.getTime());
            } else if (ADMIN_PERMISSION <= originalFolder.getEffectiveUserPermission(session.getUserId(), ServerSessionAdapter.valueOf(session).getUserPermissionBits()).getFolderPermission()) {
                folderManager.updateFolder(updateMe, true, false, millis.getTime());
            }

            // Store updated user properties for folder as needed
            storeUserProperties(Optional.ofNullable(con), context, session.getUserId(), originalFolder, folder);

            // Handle warnings
            final List<OXException> warnings = folderManager.getWarnings();
            if (null != warnings) {
                for (final OXException warning : warnings) {
                    storageParameters.addWarning(warning);
                }
            }
        } finally {
            provider.close();
        }
    }

    /**
     * Stores any updates of user-specific properties for a specific database folder.
     * <p/>
     * This includes explicitly set properties in the updated folder, as well as implicit property changes due to other folder updates.
     *
     * @param optCon An optional database connection to use
     * @param context The context
     * @param userId The identifier of the user to store the properties for
     * @param originalFolder The original folder object
     * @param folderUpdate The folder update as passed from the client
     */
    private static void storeUserProperties(Optional<Connection> optCon, Context context, int userId, FolderObject originalFolder, Folder folderUpdate) throws OXException {
        FolderSubscriptionHelper subscriptionHelper = ServerServiceRegistry.getInstance().getService(FolderSubscriptionHelper.class, true);
        if (false == subscriptionHelper.isSubscribableModule(originalFolder.getModule())) {
            return; // not applicable
        }
        /*
         * check and set 'subscribed' flag
         */
        if (false == (folderUpdate instanceof SetterAwareFolder) || ((SetterAwareFolder) folderUpdate).containsSubscribed()) {
            Boolean originalSubscribed = subscriptionHelper.isSubscribed(optCon, context.getContextId(), userId, originalFolder.getObjectID(), originalFolder.getModule()).orElse(Boolean.TRUE);
            if (b(originalSubscribed) != folderUpdate.isSubscribed()) {
                if (false == isSubscribable(subscriptionHelper, originalFolder, userId)) {
                    throw FolderExceptionErrorMessage.SUBSCRIBE_NOT_ALLOWED.create();
                }
                subscriptionHelper.setSubscribed(optCon, context.getContextId(), userId, originalFolder.getObjectID(), originalFolder.getModule(), folderUpdate.isSubscribed());
            }
        }
        /*
         * check and set 'usedForSync' flag
         */
        if (false == (folderUpdate instanceof SetterAwareFolder) || ((SetterAwareFolder) folderUpdate).containsUsedForSync()) {
            UsedForSync defaultUsedForSync = getDefaultUsedForSync(originalFolder, context.getContextId(), userId);
            Boolean originalUsedForSync = defaultUsedForSync.isProtected() ? B(defaultUsedForSync.isUsedForSync()) : subscriptionHelper.isUsedForSync(
                optCon, context.getContextId(), userId, originalFolder.getObjectID(), originalFolder.getModule()).orElse(B(defaultUsedForSync.isUsedForSync()));
            if (b(originalUsedForSync) != folderUpdate.getUsedForSync().isUsedForSync()) {
                if (defaultUsedForSync.isProtected()) {
                    throw FolderExceptionErrorMessage.SUBSCRIBE_NOT_ALLOWED.create();
                }
                subscriptionHelper.setUsedForSync(optCon, context.getContextId(), userId, originalFolder.getObjectID(), originalFolder.getModule(), folderUpdate.getUsedForSync());
            }
        }
        /*
         * ensure to remove any 'subscribed' and 'usedForSync' flags of affected users when moving to no longer supported location
         */
        if (null != folderUpdate.getParentID()) {
            int newParentId = Strings.parseUnsignedInt(folderUpdate.getParentID());
            if (newParentId != originalFolder.getParentFolderID()) {
                FolderObject updatedFolder = originalFolder.clone();
                updatedFolder.setParentFolderID(newParentId);
                if (false == isSubscribable(subscriptionHelper, updatedFolder, userId)) {
                    int[] affectedUserIds = getAffectedUserIds(context, originalFolder);
                    if (affectedUserIds.length > 0) {
                        subscriptionHelper.clearSubscribed(optCon, context.getContextId(), affectedUserIds, originalFolder.getObjectID(), originalFolder.getModule());
                        subscriptionHelper.clearUsedForSync(optCon, context.getContextId(), affectedUserIds, originalFolder.getObjectID(), originalFolder.getModule());
                    }
                }
            }
        }
    }

    /**
     * Gets the identifiers of all users that have access to a specific folder, based on its permissions.
     *
     * @param context The context in which the folder is located
     * @param folder The folder to get the affected users for
     * @return The identifiers of all users that have access to the folder
     */
    private static int[] getAffectedUserIds(Context context, FolderObject folder) throws OXException {
        Set<Integer> userIds = new HashSet<Integer>();
        if (null != folder.getPermissions()) {
            for (OCLPermission permission : folder.getPermissions()) {
                if (permission.isSystem() || false == permission.isFolderVisible()) {
                    continue;
                }
                if (permission.isGroupPermission()) {
                    GroupService groupService = ServerServiceRegistry.getInstance().getService(GroupService.class, true);
                    for (int member : groupService.getGroup(context, permission.getEntity(), true).getMember()) {
                        userIds.add(I(member));
                    }
                } else {
                    userIds.add(I(permission.getEntity()));
                }
            }
        }
        return Autoboxing.I2i(userIds);
    }

    private static boolean isSubscribable(FolderSubscriptionHelper subscriptionHelper, FolderObject folder, int userId) throws OXException {
        if (null == folder || false == subscriptionHelper.isSubscribableModule(folder.getModule())) {
            return false;
        }
        if (FolderObject.INFOSTORE == folder.getModule()) {
            if (FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID == folder.getParentFolderID()) {
                return true;
            }
            if (FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID == folder.getParentFolderID()) {
                return userId != folder.getCreatedBy();
            }
            return false;
        }
        if (folder.isDefaultFolder() && FolderObject.PRIVATE == folder.getType(userId)) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether the given folder needs any adjustments concerning inherited permissions and cascade those permissions to subfolders
     *
     * For example it removes previous inherited permissions in case of a move out or add new one in case of a move in.
     *
     * @param context The context
     * @param con The {@link Connection} to use
     * @param folderManager The {@link OXFolderManager}
     * @param storageParameters The storage parameters
     * @param folder The folder to check
     * @param parent The parent folder
     * @param millis The last modified date of the client
     * @throws OXException in case of errors
     */
    private void cascadeInheritedPermissions(Context context, Connection con, OXFolderManager folderManager, StorageParameters storageParameters, FolderObject folder, FolderObject parent, Date millis) throws OXException {
        FolderObject storageFolder = folder;
        if (folder.containsPermissions() == false) {
            storageFolder = getFolderObject(folder.getObjectID(), context, con, storageParameters);
        }
        Optional<List<OCLPermission>> optPermissions = processInheritedPermissions(parent, Optional.empty(), Optional.of(storageFolder));
        if (optPermissions.isPresent()) {
            folder.setPermissions(optPermissions.get());
            folderManager.updateFolder(folder, false, false, millis.getTime());
            if (folder.hasSubfolders()) {
                SortableId[] subFolderIds = getSubfolders(FolderStorage.REAL_TREE_ID, String.valueOf(folder.getObjectID()), storageParameters);
                for (SortableId subfolderId : subFolderIds) {
                    FolderObject subfolder = getFolderObject(Integer.parseInt(subfolderId.getId()), context, con, storageParameters);
                    cascadeInheritedPermissions(context, con, folderManager, storageParameters, subfolder, folder, millis);
                }
            }
        }
    }

    /**
     * Processes inherited permissions and returns a new list of permissions in case the they need to change
     *
     * @param parent The parent folder
     * @param optPerms The optional permissions to check
     * @param storageFolder The optional storage folder in case the permissions are empty
     * @return An optional list of {@link OCLPermission}s in case they need to be changed
     */
    private Optional<List<OCLPermission>> processInheritedPermissions(FolderObject parent, Optional<List<OCLPermission>> optPerms, Optional<FolderObject> storageFolder) {
        if (parent.getModule() != FolderObject.INFOSTORE || (optPerms.isPresent() == false && storageFolder.isPresent() == false)) {
            return Optional.empty();
        }
        // Get parent permissions
        List<OCLPermission> permissionsToInherit = parent.getPermissions().stream().filter((p) -> FolderPermissionType.NORMAL.equals(p.getType()) == false).collect(Collectors.toList());
        List<OCLPermission> perms = optPerms.isPresent() ? optPerms.get() : storageFolder.get().getPermissions();
        if (permissionsToInherit.isEmpty()) {
            if (perms.parallelStream().filter((p) -> FolderPermissionType.INHERITED.equals(p.getType())).findAny().isPresent()) {
                // Remove existing permissions
                return Optional.of(perms.parallelStream().filter((p) -> p.getType().equals(FolderPermissionType.INHERITED) == false).collect(Collectors.toList()));
            }
            return Optional.empty();
        }
        // Add inherited permissions and transform legator permissions
        List<OCLPermission> result = new ArrayList<>(perms.size() + permissionsToInherit.size());
        for (OCLPermission p : permissionsToInherit) {
            OCLPermission cloned;
            try {
                cloned = (OCLPermission) p.clone();
            } catch (@SuppressWarnings("unused") CloneNotSupportedException e) {
                // use original instead
                cloned = p;
            }
            int entity = cloned.getEntity();
            Optional<OCLPermission> existingPermission = perms.stream().filter(e -> entity == e.getEntity()).findAny();
            if (existingPermission.isPresent()) {
                // Permission needs to be removed as it will be replaced by inherited permission
                perms.remove(existingPermission.get());
            }
            cloned.setType(FolderPermissionType.INHERITED);
            if (FolderPermissionType.LEGATOR.equals(p.getType())) {
                cloned.setPermissionLegator(String.valueOf(parent.getObjectID()));
            }
            result.add(cloned);
        }
        // Add previous not inherited permissions
        result.addAll(perms.parallelStream().filter((p) -> p.getType().equals(FolderPermissionType.INHERITED) == false).collect(Collectors.toList()));
        // Return new permissions
        return Optional.of(result);
    }

    private void inheritFolderPermissions(FolderObject folder, FolderObject parent, Context context, Connection con, StorageParameters storageParameters, OXFolderManager folderManager, Date millis, boolean mergePermissions) throws OXException {
        // Only merge/inherit permission for infostore folders
        if (parent.getModule() != FolderObject.INFOSTORE) {
            return;
        }
        if (mergePermissions) {
            // Load permissions
            FolderObject folder2 = getFolderObject(folder.getObjectID(), context, con, storageParameters);
            List<OCLPermission> compiledPerms = getCombinedFolderPermissions(folder2, parent);
            folder.setPermissions(cleanupGuestGroupPermissions(compiledPerms));
        } else {
            folder.setPermissions(cleanupGuestGroupPermissions(parent.getPermissions()));
        }
        folder.setPermissions(cleanupAnonymousShareLinkPermissionsWithTypeNormal(folder.getPermissions(), storageParameters));
        Optional<List<OCLPermission>> optPermissions = processInheritedPermissions(parent, Optional.of(folder.getPermissions()), Optional.empty());
        optPermissions.ifPresent(p -> folder.setPermissions(p));

        folderManager.updateFolder(folder, false, false, millis.getTime());
        if (folder.hasSubfolders()) {
            SortableId[] subFolderIds = getSubfolders(FolderStorage.REAL_TREE_ID, String.valueOf(folder.getObjectID()), storageParameters);
            for (SortableId subfolderId : subFolderIds) {
                FolderObject subfolder = getFolderObject(Integer.parseInt(subfolderId.getId()), context, con, storageParameters);
                inheritFolderPermissions(subfolder, parent, context, con, storageParameters, folderManager, millis, mergePermissions);
            }
        }
    }

    /**
     * Removes permissions for anonymous share links with FolderPermissionType.NORMAL from the permissions.
     * See MWB-1119.
     * Needs ShareService for valid result.
     *
     * @param perms The permissions.
     * @param storageParameters The storage parameters.
     * @return The cleaned list of permissions.
     */
    private List<OCLPermission> cleanupAnonymousShareLinkPermissionsWithTypeNormal(List<OCLPermission> perms, StorageParameters storageParameters) {
        ShareService shareService = ServerServiceRegistry.getServize(ShareService.class);
        Optional<OCLPermission> existingNormalSharingLinkPermission = perms.stream().filter(e -> {
            try {
                GuestInfo guestInfo = null;
                //@formatter:off
                return (FolderPermissionType.NORMAL == e.getType()
                    && shareService != null
                    && (guestInfo = shareService.getGuestInfo(storageParameters.getSession(), e.getEntity())) != null
                    && RecipientType.ANONYMOUS.equals(guestInfo.getRecipientType()));
                //@formatter:on
            } catch (OXException exception) {
                LOG.debug("Determination if the permission is a anonymous share link permission failed.", exception);
                return false;
            }
        }).findAny();
        if (existingNormalSharingLinkPermission.isPresent()) {
            perms.remove(existingNormalSharingLinkPermission.get());
        }
        return perms;
    }

    private List<OCLPermission> cleanupGuestGroupPermissions(List<OCLPermission> perms) {
        Optional<OCLPermission> existingGuestPermission = perms.stream().filter(e -> GroupStorage.GUEST_GROUP_IDENTIFIER == e.getEntity()).findAny();
        if (existingGuestPermission.isPresent()) {
            perms.remove(existingGuestPermission.get());
        }
        return perms;
    }

    private UpdatedFolderHandler updatedFolderHandlerFor(final StorageParameters storageParameters) {
        return (fo, con) -> {
            CacheService cacheService = ServerServiceRegistry.getInstance().getService(CacheService.class);
            if (null == cacheService) {
                return;
            }
            try {
                User user = storageParameters.getUser();
                Context ctx = storageParameters.getContext();
                UserPermissionBits userPermissionBits = getUserPermissionBits(con, storageParameters);

                boolean altNames = StorageParametersUtility.getBoolParameter("altNames", storageParameters);
                boolean separateFederatedShares = StorageParametersUtility.getBoolParameter("separateFederatedShares", storageParameters);
                DatabaseFolder f = DatabaseFolderConverter.convert(fo, user, userPermissionBits, ctx, storageParameters.getSession(), altNames, separateFederatedShares, con);
                f.setTreeID(REAL_TREE_ID);

                if (false == f.isCacheable()) {
                    return;
                }
                if (f.isGlobalID()) {
                    Cache globalCache = cacheService.getCache("GlobalFolderCache");
                    CacheKey cacheKey = cacheService.newCacheKey(1, FolderStorage.REAL_TREE_ID, String.valueOf(fo.getObjectID()));
                    globalCache.putInGroup(cacheKey, Integer.toString(storageParameters.getContextId()), f, true);
                    return;
                }
                FolderMap folderMap = FolderMapManagement.getInstance().optFor(storageParameters.getSession());
                if (folderMap != null) {
                    folderMap.remove(f.getID(), FolderStorage.REAL_TREE_ID, storageParameters.getSession());
                    folderMap.put(FolderStorage.REAL_TREE_ID, f, storageParameters.getSession());
                }
            } catch (Exception e) {
                LOG.warn("", e);
            }
        };
    }

    private List<OCLPermission> getCombinedFolderPermissions(FolderObject folder, FolderObject parent) {
        Map<Integer, OCLPermission> permsMappingPerEntity = new HashMap<>();
        Map<Integer, OCLPermission> systemPermsMappingPerEntity = new HashMap<>();
        Map<Integer, OCLPermission> parentMappingPerEntity = new HashMap<>();
        Map<Integer, OCLPermission> parentSystemMappingPerEntity = new HashMap<>();

        mapPermissions(folder.getPermissionsAsArray(), permsMappingPerEntity, systemPermsMappingPerEntity);
        mapPermissions(parent.getPermissionsAsArray(), parentMappingPerEntity, parentSystemMappingPerEntity);

        addParentPermissionsToFolder(folder, permsMappingPerEntity, parentMappingPerEntity);
        addParentPermissionsToFolder(folder, systemPermsMappingPerEntity, parentSystemMappingPerEntity);

        return mergeAllPermissions(permsMappingPerEntity, systemPermsMappingPerEntity);
    }

    private List<OCLPermission> mergeAllPermissions(Map<Integer, OCLPermission> permsMappingPerEntity, Map<Integer, OCLPermission> systemPermsMappingPerEntity) {
        List<OCLPermission> compiledPerms = new ArrayList<>();
        compiledPerms.addAll(permsMappingPerEntity.values());
        compiledPerms.addAll(systemPermsMappingPerEntity.values());
        return compiledPerms;
    }

    private void mapPermissions(OCLPermission[] perms, Map<Integer, OCLPermission> permsMappingPerEntity, Map<Integer, OCLPermission> systemPermsMappingPerEntity) {
        for (OCLPermission p : perms) {
            if (p.isSystem()) {
                systemPermsMappingPerEntity.put(Integer.valueOf(p.getEntity()), p);
            } else {
                permsMappingPerEntity.put(Integer.valueOf(p.getEntity()), p);
            }
        }
    }

    private void addParentPermissionsToFolder(FolderObject folder, Map<Integer, OCLPermission> permsMappingPerEntity, Map<Integer, OCLPermission> parentMappingPerEntity) {
        for (Map.Entry<Integer, OCLPermission> entityEntry : parentMappingPerEntity.entrySet()) {
            Integer entity = entityEntry.getKey();
            if (OCLPermission.ALL_GUESTS == entity.intValue()) {
                continue;
            }
            OCLPermission parentPerm = entityEntry.getValue();
            if (permsMappingPerEntity.containsKey(entity)) {
                OCLPermission folderPerm = permsMappingPerEntity.remove(entity);
                folderPerm.setAllPermission(Math.max(folderPerm.getFolderPermission(), parentPerm.getFolderPermission()), Math.max(folderPerm.getReadPermission(), parentPerm.getReadPermission()), Math.max(folderPerm.getWritePermission(), parentPerm.getWritePermission()), Math.max(folderPerm.getDeletePermission(), parentPerm.getDeletePermission()));
                folderPerm.setFolderAdmin(folderPerm.isFolderAdmin() || parentPerm.isFolderAdmin());
                folderPerm.setEntity(entity.intValue());
                folderPerm.setFuid(folder.getObjectID());
                permsMappingPerEntity.put(entity, folderPerm);
            } else {
                permsMappingPerEntity.put(entity, parentPerm);
            }
        }
    }

    @Override
    public StoragePriority getStoragePriority() {
        return StoragePriority.NORMAL;
    }

    @Override
    public boolean containsFolder(final String treeId, final String folderIdentifier, final StorageParameters storageParameters) throws OXException {
        return containsFolder(treeId, folderIdentifier, StorageType.WORKING, storageParameters);
    }

    @Override
    public boolean containsFolder(final String treeId, final String folderIdentifier, final StorageType storageType, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final User user = storageParameters.getUser();
            final Context ctx = storageParameters.getContext();
            final UserPermissionBits userPermissionBits = getUserPermissionBits(con, storageParameters);
            final boolean retval;

            if (StorageType.WORKING.equals(storageType)) {
                if (DatabaseFolderStorageUtility.hasSharedPrefix(folderIdentifier)) {

                    retval = SharedPrefixFolder.existsSharedPrefixFolder(folderIdentifier, user, userPermissionBits, ctx, con);
                } else {
                    /*
                     * A numeric folder identifier
                     */
                    final int folderId = getUnsignedInteger(folderIdentifier);

                    if (folderId < 0) {
                        retval = false;
                    } else {
                        if (FolderObject.SYSTEM_ROOT_FOLDER_ID == folderId) {
                            retval = true;
                        } else if (Arrays.binarySearch(VIRTUAL_IDS, folderId) >= 0) {
                            /*
                             * A virtual database folder
                             */
                            retval = VirtualListFolder.existsVirtualListFolder(folderId, user, userPermissionBits, ctx, con);
                        } else {
                            /*
                             * A non-virtual database folder
                             */

                            if (FolderObject.SYSTEM_SHARED_FOLDER_ID == folderId) {
                                /*
                                 * The system shared folder
                                 */
                                retval = true;
                            } else if (FolderObject.SYSTEM_PUBLIC_FOLDER_ID == folderId) {
                                /*
                                 * The system public folder
                                 */
                                retval = true;
                            } else if (FolderObject.SYSTEM_INFOSTORE_FOLDER_ID == folderId) {
                                /*
                                 * The system infostore folder
                                 */
                                retval = true;
                            } else if (FolderObject.SYSTEM_PRIVATE_FOLDER_ID == folderId) {
                                /*
                                 * The system private folder
                                 */
                                retval = true;
                            } else {
                                /*
                                 * Check for shared folder, that is folder is of type private and requesting user is different from folder's
                                 * owner
                                 */
                                retval = OXFolderSQL.exists(folderId, con, ctx);
                            }
                        }
                    }
                }
            } else {
                final int folderId = getUnsignedInteger(folderIdentifier);

                if (folderId < 0) {
                    retval = false;
                } else {
                    retval = OXFolderSQL.exists(folderId, con, ctx, DEL_OXFOLDER_TREE);
                }
            }
            return retval;
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }// End of containsFolder()

    @Override
    public String[] getModifiedFolderIDs(String treeId, Date timeStamp, ContentType[] includeContentTypes, StorageParameters storageParameters) throws OXException {
        long start = System.nanoTime();
        String[] modifiedFolderIds;
        if (null == timeStamp || 0L == timeStamp.getTime()) {
            /*
             * get all visible folders in optimized way if no timestamp is specified
             */
            int[] modules = null != includeContentTypes ? getModulesByContentType(includeContentTypes) : null;
            VisibleFolders visibleFolders = getVisibleFolders(FolderObject.SYSTEM_ROOT_FOLDER_ID, modules, true, storageParameters);
            Set<String> folderIds = new HashSet<String>();
            for (int id : visibleFolders.getSpecialFolders()) {
                folderIds.add(Integer.toString(id));
            }
            for (int id : visibleFolders.getRegularFolders()) {
                folderIds.add(Integer.toString(id));
            }
            modifiedFolderIds = folderIds.toArray(new String[folderIds.size()]);
        } else {
            ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
            SearchIterator<FolderObject> searchIterator = null;
            try {
                if (null == includeContentTypes) {
                    searchIterator = OXFolderIteratorSQL.getAllModifiedFoldersSince(timeStamp, storageParameters.getContext(), provider.getConnection());
                } else {
                    searchIterator = OXFolderIteratorSQL.getModifiedFoldersSince(timeStamp, getModulesByContentType(includeContentTypes), storageParameters.getContext(), provider.getConnection());
                }
                modifiedFolderIds = filterByContentType(searchIterator, includeContentTypes);
            } finally {
                SearchIterators.close(searchIterator);
                provider.close();
            }
        }
        LOG.trace("Successfully collected {} modified folder identifiers since {} in {}ms.",
            I(modifiedFolderIds.length), L(null == timeStamp ? 0L : timeStamp.getTime()), L(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
        return modifiedFolderIds;
    }// End of getModifiedFolderIDs()

    @Override
    public String[] getDeletedFolderIDs(final String treeId, final Date timeStamp, final StorageParameters storageParameters) throws OXException {
        return getDeletedFolderIDs(treeId, timeStamp, null, storageParameters);
    }

    public String[] getDeletedFolderIDs(@SuppressWarnings("unused") final String treeId, final Date timeStamp, ContentType[] includeContentTypes, final StorageParameters storageParameters) throws OXException {
        Date since = null != timeStamp ? timeStamp : new Date(0L);
        ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        SearchIterator<FolderObject> searchIterator = null;
        try {
            Connection connection = provider.getConnection();
            User user = storageParameters.getUser();
            UserPermissionBits userPermissionBits = getUserPermissionBits(connection, storageParameters);
            searchIterator = OXFolderIteratorSQL.getDeletedFoldersSince(since, user.getId(), user.getGroups(), userPermissionBits.getAccessibleModules(), storageParameters.getContext(), connection);
            return filterByContentType(searchIterator, includeContentTypes);
        } finally {
            SearchIterators.close(searchIterator);
            provider.close();
        }
    }// End of getDeletedFolderIDs()

    /*-
     * ############################# HELPER METHODS #############################
     */

    private static FolderObject getFolderObject(final int folderId, final Context ctx, final Connection con, StorageParameters storageParameters) throws OXException {
        Boolean ignoreCache = storageParameters.getIgnoreCache();
        if (!FolderCacheManager.isEnabled()) {
            return FolderObject.loadFolderObjectFromDB(folderId, ctx, con, true, true);
        }

        FolderCacheManager cacheManager = FolderCacheManager.getInstance();
        if (Boolean.TRUE.equals(ignoreCache)) {
            FolderObject fo = FolderObject.loadFolderObjectFromDB(folderId, ctx, con, true, true);
            Boolean do_not_cache = storageParameters.getParameter(FolderType.GLOBAL, "DO_NOT_CACHE");
            if (null == do_not_cache || !do_not_cache.booleanValue()) {
                cacheManager.putFolderObject(fo, ctx, true, null);
            }
            return fo;
        }

        FolderObject fo = cacheManager.getFolderObject(folderId, ctx);
        if (null == fo) {
            fo = FolderObject.loadFolderObjectFromDB(folderId, ctx, con, true, true);
            cacheManager.putFolderObject(fo, ctx, false, null);
        }
        return fo;
    }

    private static List<FolderObject> getFolderObjects(final int[] folderIds, final Context ctx, final Connection con, StorageParameters storageParameters) throws OXException {
        Boolean ignoreCache = storageParameters.getIgnoreCache();
        if (!FolderCacheManager.isEnabled()) {
            /*
             * OX folder cache not enabled
             */
            return OXFolderBatchLoader.loadFolderObjectsFromDB(folderIds, ctx, con, true, true);
        }

        FolderCacheManager cacheManager = FolderCacheManager.getInstance();
        if (Boolean.TRUE.equals(ignoreCache)) {
            List<FolderObject> folders = OXFolderBatchLoader.loadFolderObjectsFromDB(folderIds, ctx, con, true, true);
            for (FolderObject fo : folders) {
                cacheManager.putFolderObject(fo, ctx, true, null);
            }
            return folders;
        }

        // Load them either from cache or from database
        final int length = folderIds.length;
        final FolderObject[] ret = new FolderObject[length];
        final TIntIntMap toLoad = new TIntIntHashMap(length);
        for (int index = 0; index < length; index++) {
            final int folderId = folderIds[index];
            final FolderObject fo = cacheManager.getFolderObject(folderId, ctx);
            if (null == fo) {// Cache miss
                toLoad.put(folderId, index);
            } else {// Cache hit
                ret[index] = fo;
            }
        }
        if (!toLoad.isEmpty()) {
            final List<FolderObject> list = OXFolderBatchLoader.loadFolderObjectsFromDB(toLoad.keys(), ctx, con, true, true);
            for (final FolderObject folderObject : list) {
                if (null != folderObject) {
                    final int index = toLoad.get(folderObject.getObjectID());
                    ret[index] = folderObject;
                    cacheManager.putFolderObject(folderObject, ctx, false, null);
                }
            }
        }
        return Arrays.asList(ret);
    }

    private static OXFolderAccess getFolderAccess(final Context ctx, final Connection con) {
        return new OXFolderAccess(con, ctx);
    }

    private ConnectionProvider getConnection(final Mode mode, final StorageParameters storageParameters) throws OXException {
        ConnectionMode connection = optParameter(ConnectionMode.class, PARAM_CONNECTION, storageParameters);
        if (null != connection) {
            return new NonClosingConnectionProvider(connection/* , databaseService, context.getContextId() */);
        }
        final Context context = storageParameters.getContext();
        final DatabaseService databaseService = services.getService(DatabaseService.class);
        connection = WRITEES.contains(mode) ? new ConnectionMode(databaseService.getWritable(context), mode) : new ConnectionMode(databaseService.getReadOnly(context), mode);
        return new ClosingConnectionProvider(connection, databaseService, context.getContextId());
    }

    private static <T> T optParameter(final Class<T> clazz, final String name, final StorageParameters parameters) throws OXException {
        final Object obj = parameters.getParameter(DatabaseFolderType.getInstance(), name);
        if (null == obj) {
            return null;
        }
        try {
            return clazz.cast(obj);
        } catch (ClassCastException e) {
            throw OXFolderExceptionCode.MISSING_PARAMETER.create(e, name);
        }
    }

    private static String[] filterByContentType(SearchIterator<FolderObject> searchIterator, ContentType[] allowedContentTypes) throws OXException {
        List<String> folderIDs = new ArrayList<String>();
        while (searchIterator.hasNext()) {
            FolderObject folder = searchIterator.next();
            if (matches(folder, allowedContentTypes)) {
                folderIDs.add(String.valueOf(folder.getObjectID()));
            }
        }
        return folderIDs.toArray(new String[folderIDs.size()]);
    }

    private static boolean matches(FolderObject folder, ContentType[] allowedContentTypes) {
        if (null == allowedContentTypes) {
            return true;
        }
        for (ContentType allowedContentType : allowedContentTypes) {
            if (folder.getModule() == getModuleByContentType(allowedContentType)) {
                return true;
            }
        }
        return false;
    }

    private static int getModuleByContentType(final ContentType contentType) {
        final String cts = contentType.toString();
        if (TaskContentType.getInstance().toString().equals(cts)) {
            return FolderObject.TASK;
        }
        if (CalendarContentType.getInstance().toString().equals(cts)) {
            return FolderObject.CALENDAR;
        }
        if (ContactsContentType.getInstance().toString().equals(cts)) {
            return FolderObject.CONTACT;
        }
        if (InfostoreContentType.getInstance().toString().equals(cts)) {
            return FolderObject.INFOSTORE;
        }
        return FolderObject.UNBOUND;
    }

    private static int[] getModulesByContentType(ContentType[] contentTypes) {
        if (null == contentTypes) {
            return null;
        }
        int[] modules = new int[contentTypes.length];
        for (int i = 0; i < contentTypes.length; i++) {
            modules[i] = getModuleByContentType(contentTypes[i]);
        }
        return modules;
    }

    private static int getTypeByFolderType(final Type type) {
        if (PrivateType.getInstance().equals(type)) {
            return FolderObject.PRIVATE;
        }
        if (PublicType.getInstance().equals(type)) {
            return FolderObject.PUBLIC;
        }
        if (TrashType.getInstance().equals(type)) {
            return FolderObject.TRASH;
        }
        if (DocumentsType.getInstance().equals(type)) {
            return FolderObject.DOCUMENTS;
        }
        if (TemplatesType.getInstance().equals(type)) {
            return FolderObject.TEMPLATES;
        }
        if (MusicType.getInstance().equals(type)) {
            return FolderObject.MUSIC;
        }
        if (PicturesType.getInstance().equals(type)) {
            return FolderObject.PICTURES;
        }
        if (VideosType.getInstance().equals(type)) {
            return FolderObject.VIDEOS;
        }
        return FolderObject.SYSTEM_TYPE;
    }

    private static int getTypeByFolderTypeWithShared(final Type type) {
        if (SharedType.getInstance().equals(type)) {
            return FolderObject.SHARED;
        }
        return getTypeByFolderType(type);
    }

    /**
     * Reads the supplied search iterator and filters those folders that are considered as "shared" by the user, i.e. non-public folders
     * of the user that have been shared to at least one other entity.
     *
     * @param searchIterator The search iterator to process
     * @param connection An opened database connection to use
     * @param storageParameters The storage parameters as passed from the client
     * @return The shared folders of the user
     */
    private static List<FolderObject> getUserSharedFolders(SearchIterator<FolderObject> searchIterator, Connection connection, StorageParameters storageParameters) throws OXException {
        List<FolderObject> folders = new ArrayList<FolderObject>();
        Set<Integer> knownPrivateFolders = new HashSet<Integer>();
        while (searchIterator.hasNext()) {
            /*
             * only include shared, non-public folders created by the user
             */
            FolderObject folder = searchIterator.next();
            if (folder.getCreatedBy() == storageParameters.getUserId()) {
                OCLPermission[] permissions = folder.getNonSystemPermissionsAsArray();
                if (null != permissions && 1 < permissions.length && considerPrivate(folder, knownPrivateFolders, connection, storageParameters)) {
                    folders.add(folder);
                }
            }
        }
        return folders;
    }

    /**
     * Gets a value indicating whether the supplied folder can be considered as a "private" one, i.e. it is marked directly as such, or is
     * an infostore folder somewhere in the "private" subtree below the user's personal default infostore folder.
     *
     * @param folder The folder to check
     * @param knownPrivateFolders A set of already known private folders (from previous checks)
     * @param connection An opened database connection to use
     * @param storageParameters The storage parameters as passed from the client
     * @return <code>true</code> if the folder can be considered as "public", <code>false</code>, otherwise
     */
    private static boolean considerPrivate(FolderObject folder, Set<Integer> knownPrivateFolders, Connection connection, StorageParameters storageParameters) throws OXException {
        if (FolderObject.INFOSTORE != folder.getModule()) {
            /*
             * always consider non-infostore "private" folders as private
             */
            return FolderObject.PRIVATE == folder.getType(storageParameters.getUserId());
        }
        /*
         * infostore folders are always of "public" type, so check if they're somewhere below the personal subtree
         */
        List<Integer> seenFolders = new ArrayList<Integer>();
        FolderObject currentFolder = folder;
        do {
            Integer id = I(currentFolder.getObjectID());
            seenFolders.add(id);
            if (knownPrivateFolders.contains(id) || knownPrivateFolders.contains(I(currentFolder.getParentFolderID())) || currentFolder.isDefaultFolder() && FolderObject.PUBLIC == currentFolder.getType()) {
                /*
                 * folder or its parent is a previously recognized "private" folder, or the user's personal infostore folder is reached
                 */
                knownPrivateFolders.addAll(seenFolders);
                return true;
            }
            /*
             * check parent folder if there are more parent folders available
             */
            if (FolderObject.MIN_FOLDER_ID < currentFolder.getParentFolderID()) {
                currentFolder = getFolderObject(currentFolder.getParentFolderID(), storageParameters.getContext(), connection, storageParameters);
            } else {
                currentFolder = null;
                return false;
            }
        } while (null != currentFolder);
        return false;
    }

    private static final class FolderObjectComparator implements Comparator<FolderObject> {

        private final Collator collator;
        private final Context context;

        FolderObjectComparator(Locale locale, Context context) {
            super();
            collator = Collators.getSecondaryInstance(locale);
            this.context = context;
        }

        @Override
        public int compare(final FolderObject o1, final FolderObject o2) {
            if (o1.isDefaultFolder()) {
                if (o2.isDefaultFolder()) {
                    if (o1.getFolderName().equals(o2.getFolderName())) {
                        /*
                         * Sort by owner's display name
                         */
                        final int owner1 = o1.getCreatedBy();
                        final int owner2 = o2.getCreatedBy();
                        if (owner1 > 0 && owner2 > 0 && owner1 != owner2) {
                            String d1;
                            try {
                                d1 = UserStorage.getInstance().getUser(owner1, context).getDisplayName();
                            } catch (OXException e) {
                                d1 = null;
                            }
                            String d2;
                            try {
                                d2 = UserStorage.getInstance().getUser(owner2, context).getDisplayName();
                            } catch (OXException e) {
                                d2 = null;
                            }
                            return collator.compare(d1, d2);
                        }
                    }
                    return compareById(o1.getObjectID(), o2.getObjectID());
                }
                return -1;
            } else if (o2.isDefaultFolder()) {
                return 1;
            }
            // Compare by name
            return collator.compare(o1.getFolderName(), o2.getFolderName());
        }

        private static int compareById(final int id1, final int id2) {
            return (id1 < id2 ? -1 : (id1 == id2 ? 0 : 1));
        }

    }// End of FolderObjectComparator

    private static final class FolderNameComparator implements Comparator<FolderObject> {

        private final Collator collator;
        private final Context context;

        FolderNameComparator(Locale locale, Context context) {
            super();
            collator = Collators.getSecondaryInstance(locale);
            this.context = context;
        }

        @Override
        public int compare(final FolderObject o1, final FolderObject o2) {
            /*
             * Compare by name
             */
            final String folderName1 = o1.getFolderName();
            final String folderName2 = o2.getFolderName();
            if (folderName1.equals(folderName2)) {
                /*
                 * Sort by owner's display name
                 */
                final int owner1 = o1.getCreatedBy();
                final int owner2 = o2.getCreatedBy();
                if (owner1 > 0 && owner2 > 0 && owner1 != owner2) {
                    String d1;
                    try {
                        d1 = UserStorage.getInstance().getUser(owner1, context).getDisplayName();
                    } catch (OXException e) {
                        d1 = null;
                    }
                    String d2;
                    try {
                        d2 = UserStorage.getInstance().getUser(owner2, context).getDisplayName();
                    } catch (OXException e) {
                        d2 = null;
                    }
                    return collator.compare(d1, d2);
                }
            }
            return collator.compare(folderName1, folderName2);
        }

    }// End of FolderNameComparator

    private static final class FileStorageAccountComparator implements Comparator<FileStorageAccount> {

        private final Collator collator;

        public FileStorageAccountComparator(final Locale locale) {
            super();
            collator = Collators.getSecondaryInstance(locale);
        }

        @Override
        public int compare(final FileStorageAccount o1, final FileStorageAccount o2) {
            return collator.compare(o1.getDisplayName(), o2.getDisplayName());
        }

    }// End of FileStorageAccountComparator

    public static final class ConnectionMode {

        /**
         * The connection.
         */
        public final Connection connection;

        /**
         * Whether connection is read-write or read-only.
         */
        public Mode readWrite;

        /**
         * Whether this connection is in a transactional state controlled by an outer scope.
         */
        public boolean controlledExternally;

        /**
         * Initializes a new {@link ConnectionMode}.
         *
         * @param connection
         * @param readWrite
         */
        public ConnectionMode(final Connection connection, final Mode readWrite) {
            this(connection, readWrite, false);
        }

        /**
         * Initializes a new {@link ConnectionMode}.
         *
         * @param connection
         * @param readWrite
         * @param controlledExternally
         */
        public ConnectionMode(final Connection connection, final Mode readWrite, final boolean controlledExternally) {
            super();
            this.connection = connection;
            this.readWrite = readWrite;
            this.controlledExternally = controlledExternally;
        }

        public boolean isWritable() {
            return WRITEES.contains(readWrite);
        }

        public boolean supports(Mode mode) {
            if (isWritable()) {
                if (WRITEES.contains(mode)) {
                    readWrite = mode;
                }
                return true;
            }
            return readWrite == mode;
        }

        /**
         * Closes the connection
         *
         * @param databaseService The database service
         * @param contextId The context identifier
         */
        public void close(final DatabaseService databaseService, final int contextId) {
            if (!controlledExternally) {
                if (Mode.WRITE == readWrite) {
                    databaseService.backWritable(contextId, connection);
                } else if (Mode.WRITE_AFTER_READ == readWrite) {
                    databaseService.backWritableAfterReading(contextId, connection);
                } else {
                    databaseService.backReadOnly(contextId, connection);
                }
            }
        }
    }

    @Override
    public void cleanLocksFor(Folder folder, int[] userIds, final StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.WRITE, storageParameters);
        try {
            final Connection con = provider.getConnection();
            final OXFolderManager folderManager = OXFolderManager.getInstance(storageParameters.getSession(), con, con);

            FolderObject fo = new FolderObject(i(Integer.valueOf(folder.getID())));
            folderManager.cleanLocksForFolder(fo, userIds);
        } finally {
            provider.close();
        }

    }

    @Override
    public Map<String, String> restoreFromTrash(String treeId, List<String> folderIds, String defaultDestFolderId, StorageParameters storageParameters) throws OXException {
        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            Connection con = provider.getConnection();

            // Check trash folder
            int trashFolderId = OXFolderSQL.getUserDefaultFolder(storageParameters.getUserId(), FolderObject.INFOSTORE, FolderObject.TRASH, con, storageParameters.getContext());
            if (trashFolderId < 0) {
                throw FolderExceptionErrorMessage.NO_DEFAULT_FOLDER.create(TrashType.getInstance().toString(), treeId);
            }

            // Get proper folder identifiers
            List<Integer> fuids = new ArrayList<>(folderIds.size());
            for (String folderIdentifier : folderIds) {
                if (false == DatabaseFolderStorageUtility.hasSharedPrefix(folderIdentifier)) {
                    fuids.add(Integer.valueOf(folderIdentifier));
                }
            }

            // Get their paths..
            Map<Integer, FolderPath> originPaths = OXFolderSQL.getOriginPaths(fuids, storageParameters);

            // ... and iterate them
            Map<String, String> result = new LinkedHashMap<>(folderIds.size());
            int defaultDestId = Tools.getUnsignedInteger(defaultDestFolderId);
            int personalFolderId = -1;
            boolean[] pathRecreated = new boolean[] { false };
            for (Map.Entry<Integer, FolderPath> entry : originPaths.entrySet()) {
                FolderPath originPath = entry.getValue();

                // Determine the folder identifier to start from
                int folderId;
                switch (originPath.getType()) {
                    case PRIVATE:
                        if (personalFolderId < 0) {
                            personalFolderId = OXFolderSQL.getUserDefaultFolder(storageParameters.getUserId(), FolderObject.INFOSTORE, FolderObject.PUBLIC, con, storageParameters.getContext());
                        }
                        folderId = personalFolderId;
                        break;
                    case SHARED:
                        folderId = FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID;
                        break;
                    case PUBLIC:
                        folderId = FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID;
                        break;
                    case UNDEFINED: /* fall-through */
                    default:
                        // Restore at given destination folder
                        folderId = defaultDestId;
                        originPath = FolderPath.EMPTY_PATH;
                        break;
                }

                FolderObject toRestore = getFolderObject(entry.getKey().intValue(), storageParameters.getContext(), con, storageParameters);
                try {
                    if (false == isBelowTrashFolder(entry.getKey().intValue(), trashFolderId, defaultDestId, storageParameters, con)) {
                        throw FolderExceptionErrorMessage.INVALID_FOLDER_ID.create("Folder does not reside in trash folder");
                    }

                    Permission[] lastParentPermissions = null;
                    if (false == originPath.isEmpty()) {
                        pathRecreated[0] = false;
                        for (String name : originPath.getPathForRestore()) {
                            Folder folder = ensureFolderExistsForName(treeId, name, folderId, pathRecreated, storageParameters, con);
                            lastParentPermissions = folder.getPermissions();
                            folderId = Integer.parseInt(folder.getID());
                        }
                    }

                    toRestore.setParentFolderID(folderId);
                    toRestore.setOriginPath(FolderPathObject.EMPTY_PATH);
                    if (null != lastParentPermissions) {
                        for (Permission p : lastParentPermissions) {
                            if (p.getEntity() != storageParameters.getUserId()) {
                                OCLPermission newOCLPermission = newOCLPermissionFor(p);
                                if (newOCLPermission.getType().equals(FolderPermissionType.LEGATOR)) {
                                    // Change LEGATOR permissions to inherited permissions
                                    newOCLPermission.setType(FolderPermissionType.INHERITED);
                                    newOCLPermission.setPermissionLegator(String.valueOf(folderId));
                                }
                                toRestore.addPermission(newOCLPermission);
                            }
                        }
                    }
                    String restoredFolderName = OXFolderSQL.getUnusedFolderName(toRestore.getFolderName(), folderId, storageParameters);
                    toRestore.setFolderName(restoredFolderName);
                    storageParameters.getDecorator().put("permissions", "inherit");
                    updateFolder(new DatabaseFolder(toRestore), storageParameters);
                } catch (OXException e) {
                    if ("FLD".equals(e.getPrefix()) && 6 == e.getCode()) {
                        toRestore.setParentFolderID(defaultDestId);
                        updateFolder(new DatabaseFolder(toRestore), storageParameters);
                    } else {
                        throw e;
                    }
                }
                result.put(Integer.toString(entry.getKey().intValue()), Integer.toString(folderId));
            }

            return result;
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    private Folder ensureFolderExistsForName(String treeId, String name, int parentFolderId, boolean[] pathRecreated, StorageParameters storageParameters, Connection con) throws OXException {
        if (false == pathRecreated[0]) {
            SortableId[] subfolders = getSubfolders(treeId, Integer.toString(parentFolderId), storageParameters);
            for (SortableId sortableId : subfolders) {
                String subfolderName = null != sortableId.getName() ? sortableId.getName() : loadFolder(sortableId.getId(), StorageType.WORKING, storageParameters, con, treeId).getName();
                if (name.equals(subfolderName)) {
                    return getFolder(treeId, sortableId.getId(), storageParameters);
                }
            }
        }

        // Does no more exist; re-create the folder
        FolderUpdate toCreate = new FolderUpdate();
        toCreate.setName(name);
        toCreate.setParentID(String.valueOf(parentFolderId));
        toCreate.setContentType(InfostoreContentType.getInstance());
        createFolder(toCreate, storageParameters);
        pathRecreated[0] = true;
        return loadFolder(toCreate.getID(), StorageType.WORKING, storageParameters, con, treeId);
    }

    private boolean isBelowTrashFolder(int folderId, int trashFolderId, int rootFolderId, StorageParameters storageParameters, Connection con) throws OXException {
        int fid = folderId;
        while (fid > 0) {
            if (trashFolderId == fid) {
                return true;
            }
            if (rootFolderId == fid) {
                return false;
            }
            fid = getFolderObject(fid, storageParameters.getContext(), con, storageParameters).getParentFolderID();
        }
        return false;
    }

    @Override
    public List<Folder> searchFileStorageFolders(String treeId, String rootFolderId, String query, long date, boolean includeSubfolders, int start, int end, StorageParameters storageParameters) throws OXException {
        if (Strings.isEmpty(query)) {
            return Collections.emptyList();
        }

        final ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            final Connection con = provider.getConnection();

            // Query non-standard folders by database query
            int parentId = getUnsignedInteger(rootFolderId);
            if (parentId < 0) {
                throw OXFolderExceptionCode.NOT_EXISTS.create(rootFolderId, Integer.valueOf(storageParameters.getContextId()));
            }
            Context context = storageParameters.getContext();
            User user = storageParameters.getUser();

            // Collect identifiers of visible folders
            VisibleFolders visibleFolders = getVisibleFolders(parentId, new int[] { FolderObject.INFOSTORE }, includeSubfolders, storageParameters);

            // Filter regular ones by database query
            int[] filteredFolders = OXFolderSQL.searchInfostoreFoldersByName(query, visibleFolders.getRegularFolders(), date, start, end, context, con);

            // Remember reference to special ones
            int[] specialFolders = visibleFolders.getSpecialFolders();
            visibleFolders = null;

            // Load folders by identifiers
            List<Folder> result = new ArrayList<Folder>(filteredFolders.length);
            TIntSet ids = new TIntHashSet(filteredFolders.length);
            for (int id : filteredFolders) {
                if (ids.add(id)) {
                    DatabaseFolder folder = loadFolder(Integer.toString(id), StorageType.WORKING, storageParameters, con, treeId);
                    if (user.isAnonymousGuest()) {
                        handleAnonymousUser(folder, treeId, StorageType.WORKING, storageParameters, con);
                    }
                    Permission ownPermission = CalculatePermission.calculate(folder, user, context, storageParameters.getDecorator().getAllowedContentTypes());
                    if (ownPermission.isVisible()) {
                        result.add(folder);
                    }
                }
            }
            filteredFolders = null;

            // Check for special folders (personal, trash, documents, music, pictures, videos, ...)
            Locale locale = storageParameters.getUser().getLocale();
            if (specialFolders.length > 0) {
                String lowerCaseQuery = query.toLowerCase(locale);
                for (int specialFolderId : specialFolders) {
                    if (ids.add(specialFolderId)) {
                        DatabaseFolder folder = loadFolder(Integer.toString(specialFolderId), StorageType.WORKING, storageParameters, con, treeId);
                        if (localizedNameMatchesQuery(folder, lowerCaseQuery, locale)) {
                            if (storageParameters.getUser().isAnonymousGuest()) {
                                handleAnonymousUser(folder, treeId, StorageType.WORKING, storageParameters, con);
                            }
                            Permission ownPermission = CalculatePermission.calculate(folder, storageParameters.getUser(), storageParameters.getContext(), storageParameters.getDecorator().getAllowedContentTypes());
                            if (ownPermission.isVisible()) {
                                result.add(folder);
                            }
                        }
                    }
                }
            }
            specialFolders = null;

            if (1 < result.size()) {
                Collections.sort(result, new FolderComparator(locale));
            }

            return result;
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } finally {
            provider.close();
        }
    }

    private static final class FolderComparator implements Comparator<Folder> {

        private final Locale locale;
        private final Collator collator;

        /**
         * Initializes a new {@link FolderComparator}.
         *
         * @param locale The locale to use, or <code>null</code> to fall back to the default locale
         */
        public FolderComparator(Locale locale) {
            super();
            this.locale = locale;
            collator = Collators.getSecondaryInstance(null == locale ? Locale.US : locale);
        }

        @Override
        public int compare(Folder folder1, Folder folder2) {
            return collator.compare(folder1.getName(locale), folder2.getName(locale));
        }

    }

    /**
     * Get all user-visible folder in module below given parent folder.
     *
     * @param parentId The parent folder identifier
     * @param modules The modules, or <code>null</code> to include folders from all visible folders
     * @param includeSubfolders Whether to include sub-folders or not
     * @param storageParameters The storage parameters
     * @return A listing containing identifiers of all visible folders
     * @throws OXException On server error
     */
    private VisibleFolders getVisibleFolders(int parentId, int[] modules, boolean includeSubfolders, StorageParameters storageParameters) throws OXException {
        ConnectionProvider provider = getConnection(Mode.READ, storageParameters);
        try {
            Connection con = provider.getConnection();
            return getVisibleFolders(parentId, modules, includeSubfolders, storageParameters, con);
        } finally {
            provider.close();
        }
    }

    /**
     * Get all user-visible folder in module below given parent folder.
     *
     * @param parentId The parent folder identifier
     * @param modules The modules, or <code>null</code> to include folders from all visible folders
     * @param includeSubfolders Whether to include sub-folders or not
     * @param storageParameters The storage parameters
     * @param con THe connection to use
     * @return A listing containing identifiers of all visible folders
     * @throws OXException On server error
     */
    private VisibleFolders getVisibleFolders(int parentId, int[] modules, boolean includeSubfolders, StorageParameters storageParameters, Connection con) throws OXException {
        UserPermissionBits userPermissionBits = getUserPermissionBits(con, storageParameters);
        User user = storageParameters.getUser();

        VisibleFolders visibleFolders = new VisibleFolders();
        if (false == includeSubfolders) {
            /*
             * only consider direct ancestor folders
             */
            SearchIterator<FolderObject> searchIterator = null;
            try {
                searchIterator = OXFolderIteratorSQL.getVisibleSubfoldersIterator(
                    parentId, user.getId(), user.getGroups(), storageParameters.getContext(), userPermissionBits, null, con);
                while (searchIterator.hasNext()) {
                    FolderObject fo = searchIterator.next();
                    if (null == modules || com.openexchange.tools.arrays.Arrays.contains(modules, fo.getModule())) {
                        visibleFolders.add(fo);
                    }
                }
            } finally {
                SearchIterators.close(searchIterator);
            }
        } else {
            /*
             * gather all visible infostore folders
             */
            Map<Integer, FolderObject> foldersById = new HashMap<Integer, FolderObject>();
            for (int module : null == modules ? userPermissionBits.getAccessibleModules() : modules) {
                SearchIterator<FolderObject> searchIterator = null;
                try {
                    searchIterator = OXFolderIteratorSQL.getAllVisibleFoldersIteratorOfModule(
                        user.getId(), user.getGroups(), userPermissionBits.getAccessibleModules(), module, storageParameters.getContext(), con);
                    while (searchIterator.hasNext()) {
                        FolderObject folder = searchIterator.next();
                        foldersById.put(I(folder.getObjectID()), folder);
                    }
                } finally {
                    SearchIterators.close(searchIterator);
                }
            }
            /*
             * if parent folder is specified, filter accordingly
             */
            for (FolderObject folder : foldersById.values()) {
                if (0 > parentId || isBelow(parentId, folder.getObjectID(), foldersById)) {
                    visibleFolders.add(folder);
                }
            }
        }
        return visibleFolders;
    }

    /**
     * Check if localized folder name matches query, needed for default folders like 'Documents', 'Templates', etc
     *
     * @param folder The folder
     * @param lowerCaseQuery The query in locale-specific lower-case
     * @param locale The user's locale
     * @return <code>true</code> if localized folder name matches query, <code>false</code> if not
     */
    private static boolean localizedNameMatchesQuery(DatabaseFolder folder, String lowerCaseQuery, Locale locale) {
        if (null == folder) {
            return false;
        }
        String localizedName = folder.getName(locale);
        return Strings.isEmpty(localizedName) ? false : localizedName.toLowerCase(locale).indexOf(lowerCaseQuery) >= 0;
    }

    /** The result of <code>getVisibleFolders()</code> call */
    private static class VisibleFolders {

        private final TIntList regularFolders;
        private final TIntList specialFolders;

        VisibleFolders() {
            super();
            this.specialFolders = new TIntArrayList();
            this.regularFolders = new TIntArrayList();
        }

        /**
         * Adds the identifier of a folder to one of the internal lists.
         *
         * @param folder The folder whose id should be added
         */
        void add(FolderObject folder) {
            int folderId = folder.getObjectID();
            if (folder.isDefaultFolder() || folderId < FolderObject.MIN_FOLDER_ID) {
                specialFolders.add(folderId);
            } else {
                regularFolders.add(folderId);
            }
        }

        /**
         * Gets the identifiers of regular folders.
         *
         * @return The identifiers of regular folders
         */
        public int[] getRegularFolders() {
            return regularFolders.toArray();
        }

        /**
         * Gets the identifiers of special folders.
         *
         * @return The identifiers of special folders
         */
        public int[] getSpecialFolders() {
            return specialFolders.toArray();
        }
    }

    private static boolean isBelow(int parentFolderId, int folderId, Map<Integer, FolderObject> foldersById) {
        FolderObject folder = foldersById.get(I(folderId));
        if (null == folder || FolderObject.SYSTEM_ROOT_FOLDER_ID == folder.getObjectID()) {
            return false;
        }
        return folder.getParentFolderID() == parentFolderId || isBelow(parentFolderId, folder.getParentFolderID(), foldersById);
    }

}
