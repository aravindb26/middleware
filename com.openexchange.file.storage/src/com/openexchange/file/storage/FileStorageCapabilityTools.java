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

package com.openexchange.file.storage;

import org.slf4j.Logger;

/**
 * {@link FileStorageCapabilityTools} - Utility class for file storage capabilities.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class FileStorageCapabilityTools {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FileStorageCapabilityTools.class);
    }

    /**
     * Initializes a new {@link FileStorageCapabilityTools}.
     */
    private FileStorageCapabilityTools() {
        super();
    }

    /**
     * Gets a value indicating whether a specific account supports a specific capability.
     *
     * @param fileAccessClass The file access class to check the capability for
     * @param capability The capability to check
     * @return A {@code Boolean} instance indicating support for specified capability or <code>null</code> if support cannot be checked by class (but by instance; see {@link #supports(FileStorageFileAccess, FileStorageCapability)})
     * @see #supports(FileStorageFileAccess, FileStorageCapability)
     */
    public static Boolean supportsByClass(Class<? extends FileStorageFileAccess> fileAccessClass, FileStorageCapability capability) {
        if (capability.isFileAccessCapability() == false) {
            LoggerHolder.LOG.warn("Folder access capability given: {}", capability);
            return Boolean.FALSE;
        }
        switch (capability) {
            case FILE_VERSIONS:
                return Boolean.valueOf(FileStorageVersionedFileAccess.class.isAssignableFrom(fileAccessClass));
            case FOLDER_ETAGS:
                return Boolean.valueOf(FileStorageETagProvider.class.isAssignableFrom(fileAccessClass));
            case IGNORABLE_VERSION:
                return Boolean.valueOf(FileStorageIgnorableVersionFileAccess.class.isAssignableFrom(fileAccessClass));
            case PERSISTENT_IDS:
                return Boolean.valueOf(FileStoragePersistentIDs.class.isAssignableFrom(fileAccessClass));
            case RANDOM_FILE_ACCESS:
                return Boolean.valueOf(FileStorageRandomFileAccess.class.isAssignableFrom(fileAccessClass));
            case RECURSIVE_FOLDER_ETAGS:
                // Cannot be checked by class
                return null;
            case SEARCH_BY_TERM:
                return Boolean.valueOf(FileStorageAdvancedSearchFileAccess.class.isAssignableFrom(fileAccessClass));
            case SEQUENCE_NUMBERS:
                return Boolean.valueOf(FileStorageSequenceNumberProvider.class.isAssignableFrom(fileAccessClass));
            case THUMBNAIL_IMAGES:
                return Boolean.valueOf(ThumbnailAware.class.isAssignableFrom(fileAccessClass));
            case EFFICIENT_RETRIEVAL:
                return Boolean.valueOf(FileStorageEfficientRetrieval.class.isAssignableFrom(fileAccessClass));
            case LOCKS:
                return Boolean.valueOf(FileStorageLockedFileAccess.class.isAssignableFrom(fileAccessClass));
            case OBJECT_PERMISSIONS:
                return Boolean.valueOf(ObjectPermissionAware.class.isAssignableFrom(fileAccessClass));
            case RANGES:
                return Boolean.valueOf(FileStorageRangeFileAccess.class.isAssignableFrom(fileAccessClass));
            case EXTENDED_METADATA:
                return Boolean.valueOf(FileStorageExtendedMetadata.class.isAssignableFrom(fileAccessClass));
            case MULTI_MOVE:
                return Boolean.valueOf(FileStorageMultiMove.class.isAssignableFrom(fileAccessClass));
            case READ_ONLY:
                return Boolean.valueOf(FileStorageReadOnly.class.isAssignableFrom(fileAccessClass));
            case MAIL_ATTACHMENTS:
                return Boolean.valueOf(FileStorageMailAttachments.class.isAssignableFrom(fileAccessClass));
            case AUTO_NEW_VERSION:
                return Boolean.valueOf(FileStorageIgnorableVersionFileAccess.class.isAssignableFrom(fileAccessClass));
            case ZIPPABLE_FOLDER:
                return Boolean.valueOf(FileStorageZippableFolderFileAccess.class.isAssignableFrom(fileAccessClass));
            case COUNT_TOTAL:
                return Boolean.valueOf(FileStorageCountableFolderFileAccess.class.isAssignableFrom(fileAccessClass));
            case CASE_INSENSITIVE:
                return Boolean.valueOf(FileStorageCaseInsensitiveAccess.class.isAssignableFrom(fileAccessClass));
            case AUTO_RENAME_FOLDERS:
                return Boolean.valueOf(FileStorageAutoRenameFoldersAccess.class.isAssignableFrom(fileAccessClass));
            case RESTORE:
                return Boolean.valueOf(FileStorageRestoringFileAccess.class.isAssignableFrom(fileAccessClass));
            case BACKWARD_LINK:
                return Boolean.valueOf(FileStorageBackwardLinkAccess.class.isAssignableFrom(fileAccessClass));
            default:
                LoggerHolder.LOG.warn("Unknown file access capability: {}", capability);
                return Boolean.FALSE;
        }
    }

    /**
     * Gets a value indicating whether a specific account supports a specific capability.
     *
     * @param folderAccessClass The folder access class to check the capability for
     * @param capability The capability to check
     * @return A {@code Boolean} instance indicating support for specified capability or <code>null</code> if support cannot be checked by class (but by instance; see {@link #supports(FileStorageFolderAccess, FileStorageCapability)})
     * @see #supports(FileStorageFolderAccess, FileStorageCapability)
     */
    public static Boolean supportsFolderCapabilityByClass(Class<? extends FileStorageFolderAccess> folderAccessClass, FileStorageCapability capability) {
        if (capability.isFileAccessCapability()) {
            LoggerHolder.LOG.warn("File access capability given: {}", capability);
            return Boolean.FALSE;
        }
        switch (capability) {
            case SEARCH_IN_FOLDER_NAME:
                return Boolean.valueOf(SearchableFolderNameFolderAccess.class.isAssignableFrom(folderAccessClass));
            default:
                LoggerHolder.LOG.warn("Unknown folder access capability: {}", capability);
                return Boolean.FALSE;
        }
    }

    /**
     * Gets a value indicating whether a specific account supports a specific capability.
     *
     * @param fileAccess The file access reference to check the capability for
     * @param capability The capability to check
     * @return <code>true</code> if the capability is supported, <code>false</code>, otherwise
     */
    public static boolean supports(FileStorageFileAccess fileAccess, FileStorageCapability capability) {
        if (capability.isFileAccessCapability() == false) {
            LoggerHolder.LOG.warn("Folder access capability given: {}", capability);
            return false;
        }
        switch (capability) {
            case FILE_VERSIONS:
                return (fileAccess instanceof FileStorageVersionedFileAccess);
            case FOLDER_ETAGS:
                return (fileAccess instanceof FileStorageETagProvider);
            case IGNORABLE_VERSION:
                return (fileAccess instanceof FileStorageIgnorableVersionFileAccess);
            case PERSISTENT_IDS:
                return (fileAccess instanceof FileStoragePersistentIDs);
            case RANDOM_FILE_ACCESS:
                return (fileAccess instanceof FileStorageRandomFileAccess);
            case RECURSIVE_FOLDER_ETAGS:
                return (fileAccess instanceof FileStorageETagProvider) && ((FileStorageETagProvider) fileAccess).isRecursive();
            case SEARCH_BY_TERM:
                return (fileAccess instanceof FileStorageAdvancedSearchFileAccess);
            case SEQUENCE_NUMBERS:
                return (fileAccess instanceof FileStorageSequenceNumberProvider);
            case THUMBNAIL_IMAGES:
                return (fileAccess instanceof ThumbnailAware);
            case EFFICIENT_RETRIEVAL:
                return (fileAccess instanceof FileStorageEfficientRetrieval);
            case LOCKS:
                return (fileAccess instanceof FileStorageLockedFileAccess);
            case OBJECT_PERMISSIONS:
                return (fileAccess instanceof ObjectPermissionAware);
            case RANGES:
                return (fileAccess instanceof FileStorageRangeFileAccess);
            case EXTENDED_METADATA:
                return (fileAccess instanceof FileStorageExtendedMetadata);
            case MULTI_MOVE:
                return (fileAccess instanceof FileStorageMultiMove);
            case READ_ONLY:
                return (fileAccess instanceof FileStorageReadOnly);
            case MAIL_ATTACHMENTS:
                return (fileAccess instanceof FileStorageMailAttachments);
            case AUTO_NEW_VERSION:
                return (fileAccess instanceof FileStorageIgnorableVersionFileAccess);
            case ZIPPABLE_FOLDER:
                return (fileAccess instanceof FileStorageZippableFolderFileAccess);
            case COUNT_TOTAL:
                return (fileAccess instanceof FileStorageCountableFolderFileAccess);
            case CASE_INSENSITIVE:
                return (fileAccess instanceof FileStorageCaseInsensitiveAccess);
            case AUTO_RENAME_FOLDERS:
                return (fileAccess instanceof FileStorageAutoRenameFoldersAccess);
            case RESTORE:
                return (fileAccess instanceof FileStorageRestoringFileAccess);
            case BACKWARD_LINK:
                return (fileAccess instanceof FileStorageBackwardLinkAccess);
            default:
                LoggerHolder.LOG.warn("Unknown file access capability: {}", capability);
                return false;
        }
    }

    /**
     * Gets a value indicating whether a specific account supports a specific capability.
     *
     * @param folderAccess The folder access reference to check the capability for
     * @param capability The capability to check
     * @return <code>true</code> if the capability is supported, <code>false</code>, otherwise
     */
    public static boolean supports(FileStorageFolderAccess folderAccess, FileStorageCapability capability) {
        if (capability.isFileAccessCapability()) {
            LoggerHolder.LOG.warn("File access capability given: {}", capability);
            return false;
        }
        switch (capability) {
            case SEARCH_IN_FOLDER_NAME:
                return (folderAccess instanceof SearchableFolderNameFolderAccess);
            default:
                LoggerHolder.LOG.warn("Unknown folder access capability: {}", capability);
                return false;
        }
    }

}
