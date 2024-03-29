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

package com.openexchange.folderstorage.filestorage.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.CacheAware;
import com.openexchange.file.storage.FileStorageCapability;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageFolderType;
import com.openexchange.file.storage.FileStoragePermission;
import com.openexchange.file.storage.TypeAware;
import com.openexchange.file.storage.composition.IDBasedFolderAccess;
import com.openexchange.folderstorage.AbstractFolder;
import com.openexchange.folderstorage.ContentType;
import com.openexchange.folderstorage.FolderField;
import com.openexchange.folderstorage.FolderProperty;
import com.openexchange.folderstorage.ParameterizedFolder;
import com.openexchange.folderstorage.Permission;
import com.openexchange.folderstorage.SystemContentType;
import com.openexchange.folderstorage.Type;
import com.openexchange.folderstorage.database.getfolder.SystemInfostoreFolder;
import com.openexchange.folderstorage.filestorage.contentType.DocumentsContentType;
import com.openexchange.folderstorage.filestorage.contentType.FileStorageContentType;
import com.openexchange.folderstorage.filestorage.contentType.MusicContentType;
import com.openexchange.folderstorage.filestorage.contentType.PicturesContentType;
import com.openexchange.folderstorage.filestorage.contentType.PublicContentType;
import com.openexchange.folderstorage.filestorage.contentType.TemplatesContentType;
import com.openexchange.folderstorage.filestorage.contentType.TrashContentType;
import com.openexchange.folderstorage.filestorage.contentType.VideosContentType;
import com.openexchange.folderstorage.type.FileStorageType;
import com.openexchange.folderstorage.type.SystemType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.java.Strings;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.PutIfAbsent;
import com.openexchange.session.Session;

/**
 * {@link FileStorageFolderImpl} - A file storage folder.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FileStorageFolderImpl extends AbstractFolder implements ParameterizedFolder {

    private static final long serialVersionUID = 6445442372690458946L;

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FileStorageFolderImpl.class);

    private static final String CAPABILITY_ZIPPABLE_FOLDER = Strings.asciiLowerCase(FileStorageCapability.ZIPPABLE_FOLDER.name());
    private static final String CAPABILITY_FILE_VERSIONS = Strings.asciiLowerCase(FileStorageCapability.FILE_VERSIONS.name());
    private static final String CAPABILITY_EXTENDED_METADATA = Strings.asciiLowerCase(FileStorageCapability.EXTENDED_METADATA.name());
    private static final String CAPABILITY_LOCKS = Strings.asciiLowerCase(FileStorageCapability.LOCKS.name());
    private static final String CAPABILITY_COUNT_TOTAL = Strings.asciiLowerCase(FileStorageCapability.COUNT_TOTAL.name());
    private static final String CAPABILITY_CASE_INSENSITIVE = Strings.asciiLowerCase(FileStorageCapability.CASE_INSENSITIVE.name());
    private static final String CAPABILITY_AUTO_RENAME_FOLDERS = Strings.asciiLowerCase(FileStorageCapability.AUTO_RENAME_FOLDERS.name());
    private static final String CAPABILITY_RESTORE = Strings.asciiLowerCase(FileStorageCapability.RESTORE.name());

    private static final FolderField ACCOUNT_ERROR = AccountErrorField.getInstance();

    /**
     * <code>"9"</code>
     */
    private static final String INFOSTORE = Integer.toString(FolderObject.SYSTEM_INFOSTORE_FOLDER_ID);

    /**
     * <code>"10"</code>
     */
    private static final String INFOSTORE_USER = Integer.toString(FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID);

    /**
     * <code>"15"</code>
     */
    private static final String INFOSTORE_PUBLIC = Integer.toString(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID);

    /**
     * The mail folder content type.
     */
    public static enum FileStorageDefaultFolderType {
        NONE(FileStorageContentType.getInstance(), 0),
        ROOT(SystemContentType.getInstance(), 0),
        HOME_DIRECTORY(FileStorageContentType.getInstance(), 8), // FolderObject.FILE
        PUBLIC_FOLDER(PublicContentType.getInstance(), 15),
        TRASH(TrashContentType.getInstance(), 16),
        PICTURES(PicturesContentType.getInstance(), 20),
        DOCUMENTS(DocumentsContentType.getInstance(), 21),
        MUSIC(MusicContentType.getInstance(), 22),
        VIDEOS(VideosContentType.getInstance(), 23),
        TEMPLATES(TemplatesContentType.getInstance(), 24);

        private final ContentType contentType;
        private final int type;

        private FileStorageDefaultFolderType(final ContentType contentType, final int type) {
            this.contentType = contentType;
            this.type = type;
        }

        /**
         * Gets the content type associated with this mail folder type.
         *
         * @return The content type
         */
        public ContentType getContentType() {
            return contentType;
        }

        /**
         * Gets the type.
         *
         * @return The type
         */
        public int getType() {
            return type;
        }

    }

    private boolean cacheable;
    private final FileStorageDefaultFolderType defaultFolderType;
    private final Map<FolderField, FolderProperty> properties;

    /**
     * The private folder identifier.
     */
    private static final String PRIVATE_FOLDER_ID = String.valueOf(FolderObject.SYSTEM_PRIVATE_FOLDER_ID);

    /**
     * Initializes a new {@link FileStorageFolderImpl} from given messaging folder.
     * <p>
     * Subfolder identifiers and tree identifier are not set within this constructor.
     *
     * @param fsFolder The underlying file storage folder
     * @param accountId The full-qualified file storage account ID
     * @param session The requesting users session
     * @param altNames If the client requested alternative names
     * @param folderAccess The associated folder access
     */
    public FileStorageFolderImpl(final FileStorageFolder fsFolder, final String accountId, final Session session, final boolean altNames, IDBasedFolderAccess folderAccess) {
        this(fsFolder, accountId, showPersonalBelowInfoStore(session, altNames), folderAccess);
    }

    /**
     * Initializes a new {@link FileStorageFolderImpl} from given messaging folder.
     * <p>
     * Subfolder identifiers and tree identifier are not set within this constructor.
     *
     * @param fsFolder The underlying file storage folder
     * @param accountId The full-qualified file storage account ID
     * @param userId ID of the user requesting the folder
     * @param contextId The context ID
     * @param altNames If the client requested alternative names
     * @param folderAccess The associated folder access
     */
    public FileStorageFolderImpl(FileStorageFolder fsFolder, String accountId, int userId, int contextId, boolean altNames, IDBasedFolderAccess folderAccess) {
        this(fsFolder, accountId, showPersonalBelowInfoStore(userId, contextId, altNames), folderAccess);
    }

    /**
     * Initializes a new {@link FileStorageFolderImpl} from given messaging folder.
     * <p>
     * Subfolder identifiers and tree identifier are not set within this constructor.
     *
     * @param fsFolder The underlying file storage folder
     * @param accountId The full-qualified file storage account ID
     * @param showPersonalBelowInfoStore If the users personal FS folder shall be shown below folder 9 instead below folder 10
     * @param folderAccess The associated folder access
     */
    private FileStorageFolderImpl(FileStorageFolder fsFolder, String accountId, boolean showPersonalBelowInfoStore, IDBasedFolderAccess folderAccess) {
        super();
        id = fsFolder.getId();
        name = fsFolder.getName();
        this.accountId = accountId;
        if (fsFolder.isRootFolder()) {
            parent = PRIVATE_FOLDER_ID;
            defaultFolderType = FileStorageDefaultFolderType.NONE;
        } else {
            String parentId = null;
            if (fsFolder instanceof TypeAware) {
                final FileStorageFolderType folderType = ((TypeAware) fsFolder).getType();
                if (FileStorageFolderType.HOME_DIRECTORY.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.HOME_DIRECTORY;
                    if (showPersonalBelowInfoStore) {
                        parentId = INFOSTORE;
                    } else {
                        parentId = INFOSTORE_USER;
                    }
                } else if (FileStorageFolderType.PUBLIC_FOLDER.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.PUBLIC_FOLDER;
                    parentId = INFOSTORE_PUBLIC;
                } else if (FileStorageFolderType.TRASH_FOLDER.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.TRASH;
                } else if (FileStorageFolderType.PICTURES_FOLDER.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.PICTURES;
                } else if (FileStorageFolderType.DOCUMENTS_FOLDER.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.DOCUMENTS;
                } else if (FileStorageFolderType.MUSIC_FOLDER.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.MUSIC;
                } else if (FileStorageFolderType.VIDEOS_FOLDER.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.VIDEOS;
                } else if (FileStorageFolderType.TEMPLATES_FOLDER.equals(folderType)) {
                    defaultFolderType = FileStorageDefaultFolderType.TEMPLATES;
                }
                else {
                    defaultFolderType = FileStorageDefaultFolderType.NONE;
                }
            } else {
                defaultFolderType = FileStorageDefaultFolderType.NONE;
            }
            parent = null != parentId ? parentId : fsFolder.getParentId();
        }
        initPermissions(fsFolder);
        type = SystemType.getInstance();
        subscribed = fsFolder.isSubscribed();
        subscribedSubfolders = fsFolder.hasSubscribedSubfolders();
        {
            boolean hasSubfolders = fsFolder.hasSubfolders();
            setSubfolderIDs(hasSubfolders ? null : Strings.getEmptyStrings());
        }
        deefault = fsFolder.isDefaultFolder();
        total = fsFolder.getFileCount();
        defaultType = deefault ? FileStorageContentType.getInstance().getModule() : 0;
        if (fsFolder instanceof CacheAware) {
            cacheable = !fsFolder.isDefaultFolder() && ((CacheAware) fsFolder).cacheable();
        } else {
            cacheable = !fsFolder.isDefaultFolder();
        }
        meta = fsFolder.getMeta();
        this.supportedCapabilities = getSupportedCapabilities(fsFolder, folderAccess);
        lastModified = fsFolder.getLastModifiedDate();
        creationDate = fsFolder.getCreationDate();
        createdBy = fsFolder.getCreatedBy();
        modifiedBy = fsFolder.getModifiedBy();
        createdFrom = fsFolder.getCreatedFrom();
        modifiedFrom = fsFolder.getModifiedFrom();

        properties = new HashMap<FolderField, FolderProperty>(1);
        setProperty(ACCOUNT_ERROR, fsFolder.getAccountError());
    }

    private Set<String> getSupportedCapabilities(FileStorageFolder fsFolder, IDBasedFolderAccess folderAccess) {
        Set<String> supportedCapabilities = fsFolder.getCapabilities();
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.ZIPPABLE_FOLDER, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_ZIPPABLE_FOLDER);
        }
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.FILE_VERSIONS, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_FILE_VERSIONS);
        }
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.EXTENDED_METADATA, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_EXTENDED_METADATA);
        }
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.LOCKS, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_LOCKS);
        }
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.COUNT_TOTAL, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_COUNT_TOTAL);
        }
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.CASE_INSENSITIVE, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_CASE_INSENSITIVE);
        }
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.AUTO_RENAME_FOLDERS, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_AUTO_RENAME_FOLDERS);
        }
        if (optCheckCapability(fsFolder.getId(), FileStorageCapability.RESTORE, folderAccess)) {
            supportedCapabilities = initSupportedCapabilities(supportedCapabilities);
            supportedCapabilities.add(CAPABILITY_RESTORE);
        }
        return supportedCapabilities;
    }

    private void initPermissions(FileStorageFolder fsFolder) {
        final List<FileStoragePermission> fsPermissions = fsFolder.getPermissions();
        final int size = fsPermissions.size();
        permissions = new Permission[size];
        for (int i = 0; i < size; i++) {
            FileStoragePermissionImpl permissionImpl = new FileStoragePermissionImpl(fsPermissions.get(i));
            if (permissionImpl.isAdmin() && !permissionImpl.isGroup()) {
                createdBy = permissionImpl.getEntity();
            }
            permissions[i] = permissionImpl;
        }
    }

    private Set<String> initSupportedCapabilities(Set<String> supportedCapabilities) {
        if (null == supportedCapabilities) {
            supportedCapabilities = new LinkedHashSet<>(4);
        } else {
            supportedCapabilities = new LinkedHashSet<>(supportedCapabilities);
        }
        return supportedCapabilities;
    }

    private static boolean optCheckCapability(String folderId, FileStorageCapability capability, IDBasedFolderAccess folderAccess) {
        try {
            return folderAccess.hasCapability(capability, folderId);
        } catch (Exception e) {
            LOG.warn("Failed to check for capability {}. Assuming no support for it.", capability.name(), e);
            return false;
        }
    }

    private static boolean showPersonalBelowInfoStore(final Session session, final boolean altNames) {
        if (!altNames) {
            return false;
        }
        final String paramName = "com.openexchange.folderstorage.outlook.showPersonalBelowInfoStore";
        final Boolean tmp = (Boolean) session.getParameter(paramName);
        if (null != tmp) {
            return tmp.booleanValue();
        }

        final boolean b = showPersonalBelowInfoStore(session.getUserId(), session.getContextId(), altNames);
        if (session instanceof PutIfAbsent) {
            ((PutIfAbsent) session).setParameterIfAbsent(paramName, b ? Boolean.TRUE : Boolean.FALSE);
        } else {
            session.setParameter(paramName, b ? Boolean.TRUE : Boolean.FALSE);
        }
        return b;
    }

    private static boolean showPersonalBelowInfoStore(int userId, int contextId, boolean altNames) {
        if (!altNames) {
            return false;
        }
        final String paramName = "com.openexchange.folderstorage.outlook.showPersonalBelowInfoStore";
        final ConfigViewFactory configViewFactory = ServerServiceRegistry.getInstance().getService(ConfigViewFactory.class);
        if (null == configViewFactory) {
            return false;
        }
        try {
            final ConfigView view = configViewFactory.getView(userId, contextId);
            final Boolean b = view.opt(paramName, boolean.class, Boolean.FALSE);
            return b.booleanValue();
        } catch (OXException e) {
            org.slf4j.LoggerFactory.getLogger(SystemInfostoreFolder.class).warn("", e);
            return false;
        }
    }

    @Override
    public Object clone() {
        final FileStorageFolderImpl clone = (FileStorageFolderImpl) super.clone();
        clone.cacheable = cacheable;
        return clone;
    }

    @Override
    public boolean isCacheable() {
        return cacheable;
    }

    @Override
    public Type getType() {
        return FileStorageType.getInstance();
    }

    @Override
    public void setContentType(final ContentType contentType) {
        // Nothing to do
    }

    @Override
    public void setType(final Type type) {
        // Nothing to do
    }

    @Override
    public ContentType getContentType() {
        return defaultFolderType.getContentType();
    }

    @Override
    public int getDefaultType() {
        return defaultFolderType.getType();
    }

    @Override
    public void setDefaultType(final int defaultType) {
        // Nothing to do
    }

    @Override
    public boolean isGlobalID() {
        return false;
    }

    @Override
    public void setProperty(FolderField name, Object value) {
        if (null == value) {
            properties.remove(name);
        } else {
            properties.put(name, new FolderProperty(name.getName(), value));
        }
    }

    @Override
    public Map<FolderField, FolderProperty> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

}
