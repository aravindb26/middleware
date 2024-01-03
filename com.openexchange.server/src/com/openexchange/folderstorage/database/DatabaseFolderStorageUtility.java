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

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.SortableId;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.folderstorage.UsedForSync;
import com.openexchange.folderstorage.osgi.FolderStorageServices;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.userconf.UserPermissionService;

/**
 * {@link DatabaseFolderStorageUtility} - Utility methods for database folder storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DatabaseFolderStorageUtility {

    /** The property indicating whether a 'shared' calendar folder is used for sync by default */
    private static final Property PROPERTY_CALENDAR_USED_FOR_SYNC_SHARED = DefaultProperty.valueOf("com.openexchange.calendar.usedForSync.shared.default", Boolean.TRUE);

    /** The property indicating whether the default used for sync value of a 'shared' calendar folder is protected or not */
    private static final Property PROPERTY_CALENDAR_USED_FOR_SYNC_SHARED_PROTECTED = DefaultProperty.valueOf("com.openexchange.calendar.usedForSync.shared.protected", Boolean.FALSE);

    /** The property indicating whether a 'public' calendar folder is used for sync by default */
    private static final Property PROPERTY_CALENDAR_USED_FOR_SYNC_PUBLIC = DefaultProperty.valueOf("com.openexchange.calendar.usedForSync.public.default", Boolean.TRUE);

    /** The property indicating whether the default used for sync value of a 'public' calendar folder is protected or not */
    private static final Property PROPERTY_CALENDAR_USED_FOR_SYNC_PUBLIC_PROTECTED = DefaultProperty.valueOf("com.openexchange.calendar.usedForSync.public.protected", Boolean.FALSE);

    /**
     * Initializes a new {@link DatabaseFolderStorageUtility}.
     */
    private DatabaseFolderStorageUtility() {
        super();
    }

    /**
     * Checks if passed <code>String</code> starts with shared prefix.
     *
     * @param str The string to check
     * @return <code>true</code> if passed <code>String</code> starts with shared prefix; otherwise <code>false</code>
     */
    public static boolean hasSharedPrefix(final String str) {
        return null != str && str.startsWith(FolderObject.SHARED_PREFIX, 0);
    }

    /**
     * Extracts the user's permission bits from the supplied storage paramters.
     *
     * @param connection A readable database connection, or <code>null</code> if not available
     * @param storageParameters The storage parameters
     * @return The permission bits
     */
    public static UserPermissionBits getUserPermissionBits(Connection connection, StorageParameters storageParameters) throws OXException {
        Session session = storageParameters.getSession();
        if (session != null && (session instanceof ServerSession)) {
            return ((ServerSession) session).getUserPermissionBits();
        }

        UserPermissionService userPermissionService = FolderStorageServices.requireService(UserPermissionService.class);
        if (null == connection) {
            return userPermissionService.getUserPermissionBits(storageParameters.getUserId(), storageParameters.getContextId());
        } else {
            Context context = storageParameters.getContext();
            if (context == null) {
                context = FolderStorageServices.requireService(ContextService.class).getContext(storageParameters.getContextId());
            }
            return userPermissionService.getUserPermissionBits(connection, storageParameters.getUserId(), context);
        }
    }

    /**
     * Creates an array containing sortable identifiers from a list of folders.
     *
     * @param folders The folder to extract the identifiers for
     * @return The extracted identifiers
     */
    public static SortableId[] extractIDs(List<FolderObject> folders) {
        int size = folders.size();
        SortableId[] ret = new SortableId[size];
        for (int k = size; k-- > 0;) {
            FolderObject folderObject = folders.get(k);
            String id = String.valueOf(folderObject.getObjectID());
            ret[k] = new DatabaseId(id, k, folderObject.getFolderName());
        }
        return ret;
    }

    /**
     * Localizes the display names of the supplied folders based on a specific locale.
     *
     * @param folders The folders
     * @param locale The target locale
     */
    public static void localizeFolderNames(List<FolderObject> folders, Locale locale) {
        StringHelper stringHelper = null;
        for (final FolderObject folderObject : folders) {
            /*
             * Check if folder is user's default folder and set locale-sensitive name
             */
            if (folderObject.isDefaultFolder()) {
                int module = folderObject.getModule();
                switch (module) {
                    case FolderObject.CALENDAR: {
                        if (null == stringHelper) {
                            stringHelper = StringHelper.valueOf(locale);
                        }
                        folderObject.setFolderName(stringHelper.getString(FolderStrings.DEFAULT_CALENDAR_FOLDER_NAME));
                    }
                        break;
                    case FolderObject.CONTACT: {
                        if (null == stringHelper) {
                            stringHelper = StringHelper.valueOf(locale);
                        }
                        folderObject.setFolderName(stringHelper.getString(FolderStrings.DEFAULT_CONTACT_FOLDER_NAME));
                    }
                        break;
                    case FolderObject.TASK: {
                        if (null == stringHelper) {
                            stringHelper = StringHelper.valueOf(locale);
                        }
                        folderObject.setFolderName(stringHelper.getString(FolderStrings.DEFAULT_TASK_FOLDER_NAME));
                    }
                        break;
                }
            }
        }
    } // End of 'localizeFolderNames' method

    /**
     * Gets the default value of the <i>used for sync</i> flag of a certain folder, including whether the flag is <i>protected</i>, i.e.
     * whether its value can be changed by the user or not.
     * 
     * @param folder The folder to get the default for
     * @param contextId The identifier of the context for whom the default is generated
     * @param userId The identifier of the user for whom the default is generated
     * @return The default value of the <i>used for sync</i> flag
     */
    public static UsedForSync getDefaultUsedForSync(FolderObject folder, int contextId, int userId) throws OXException {
        /*
         * only applicable for tasks, contacts, calendar, so deactivated and protected for all other modules
         */
        if (FolderObject.TASK != folder.getModule() && FolderObject.CONTACT != folder.getModule() && FolderObject.CALENDAR != folder.getModule()) {
            return UsedForSync.DEACTIVATED;
        }
        /*
         * user's default folder is enabled by default and protected
         */
        if (folder.isDefaultFolder() && FolderObject.PRIVATE == folder.getType(userId)) {
            return UsedForSync.FORCED_ACTIVE;
        }
        /*
         * shared/public task folders are not supported, so so deactivated and protected
         */
        if (FolderObject.TASK == folder.getModule() && FolderObject.PRIVATE != folder.getType(userId)) {
            return UsedForSync.DEACTIVATED;
        }
        /*
         * use configured defaults for shared/public calendar folders
         */        
        if (FolderObject.CALENDAR == folder.getModule() && FolderObject.PUBLIC == folder.getType(userId)) {
            LeanConfigurationService configService = FolderStorageServices.requireService(LeanConfigurationService.class);
            return new UsedForSync(configService.getBooleanProperty(userId, contextId, PROPERTY_CALENDAR_USED_FOR_SYNC_PUBLIC), 
                configService.getBooleanProperty(userId, contextId, PROPERTY_CALENDAR_USED_FOR_SYNC_PUBLIC_PROTECTED));
        } else if (FolderObject.CALENDAR == folder.getModule() && FolderObject.SHARED == folder.getType(userId)) {
            LeanConfigurationService configService = FolderStorageServices.requireService(LeanConfigurationService.class);
            return new UsedForSync(configService.getBooleanProperty(userId, contextId, PROPERTY_CALENDAR_USED_FOR_SYNC_SHARED), 
                configService.getBooleanProperty(userId, contextId, PROPERTY_CALENDAR_USED_FOR_SYNC_SHARED_PROTECTED));
        }            
        /*
         * plain default for all other folders
         */
        return UsedForSync.DEFAULT;
    }

}
