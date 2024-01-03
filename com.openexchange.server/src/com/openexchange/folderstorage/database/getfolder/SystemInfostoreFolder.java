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

package com.openexchange.folderstorage.database.getfolder;

import static com.openexchange.groupware.container.FolderObject.SYSTEM_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.container.FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.container.FolderObject.SYSTEM_USER_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.container.FolderObject.VIRTUAL_LIST_INFOSTORE_FOLDER_ID;
import static com.openexchange.groupware.i18n.FolderStrings.SYSTEM_PUBLIC_FILES_FOLDER_NAME;
import static com.openexchange.groupware.i18n.FolderStrings.SYSTEM_TRASH_FILES_FOLDER_NAME;
import static com.openexchange.groupware.i18n.FolderStrings.SYSTEM_TRASH_INFOSTORE_FOLDER_NAME;
import static com.openexchange.groupware.i18n.FolderStrings.SYSTEM_USER_FILES_FOLDER_NAME;
import static com.openexchange.groupware.i18n.FolderStrings.VIRTUAL_LIST_FILES_FOLDER_NAME;
import static com.openexchange.groupware.i18n.FolderStrings.VIRTUAL_LIST_INFOSTORE_FOLDER_NAME;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.config.cascade.ConfigView;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.FederatedSharingFolders;
import com.openexchange.folderstorage.FolderExceptionErrorMessage;
import com.openexchange.folderstorage.database.AltNameLocalizedDatabaseFolder;
import com.openexchange.folderstorage.database.DatabaseFolder;
import com.openexchange.folderstorage.database.contentType.InfostoreContentType;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.PutIfAbsent;
import com.openexchange.session.Session;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderExceptionCode;
import com.openexchange.tools.oxfolder.OXFolderIteratorSQL;
import com.openexchange.user.User;
import gnu.trove.list.TIntList;

/**
 * {@link SystemInfostoreFolder} - Gets the system infostore folder.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SystemInfostoreFolder {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SystemInfostoreFolder.class);

    /**
     * Initializes a new {@link SystemInfostoreFolder}.
     */
    private SystemInfostoreFolder() {
        super();
    }

    /**
     * Gets the database folder representing system infostore folder.
     *
     * @param fo The folder object fetched from database
     * @param altNames <code>true</code> to use alternative names for former InfoStore folders; otherwise <code>false</code>
     * @return The database folder representing system infostore folder
     */
    public static DatabaseFolder getSystemInfostoreFolder(final FolderObject fo, final boolean altNames) {
        /*
         * The system infostore folder
         */
        final DatabaseFolder retval = new AltNameLocalizedDatabaseFolder(fo, FolderStrings.SYSTEM_FILES_FOLDER_NAME);
        retval.setName(altNames ? FolderStrings.SYSTEM_FILES_FOLDER_NAME : FolderStrings.SYSTEM_INFOSTORE_FOLDER_NAME);
        retval.setContentType(InfostoreContentType.getInstance());
        // Enforce getSubfolders() on storage
        retval.setSubfolderIDs(null);
        retval.setSubscribedSubfolders(true);
        // Don't cache if altNames enabled -- "Shared files" is supposed NOT to be displayed if no shared files exist
        if (altNames) {
            retval.setCacheable(false);
        }
        return retval;
    }

    /**
     * Gets a list of folder identifier and -name tuples of database folders below the system infostore folder.
     *
     * @param user The user to get the subfolders for
     * @param permissionBits The user's permission bits
     * @param context The context
     * @param altNames <code>true</code> to prefer alternative names for infostore folders, <code>false</code>, otherwise
     * @param connection The database connection to use
     * @return A list of folder identifier and -name tuples of the subfolders below the system infostore folder
     */
    public static List<String[]> getSystemInfostoreFolderSubfolders(User user, UserPermissionBits permissionBits, Context context, boolean altNames, Session session, Connection connection) throws OXException {
        StringHelper stringHelper = StringHelper.valueOf(user.getLocale());
        List<String[]> subfolderIDs = new ArrayList<String[]>();
        /*
         * add subfolders from database
         */
        SearchIterator<FolderObject> searchIterator = null;
        try {
            searchIterator = OXFolderIteratorSQL.getVisibleSubfoldersIterator(SYSTEM_INFOSTORE_FOLDER_ID, user.getId(), user.getGroups(), context, permissionBits, null, connection);
            while (searchIterator.hasNext()) {
                FolderObject folder = searchIterator.next();
                if (SYSTEM_USER_INFOSTORE_FOLDER_ID == folder.getObjectID()) {
                    if (showPersonalBelowInfoStore(session, altNames)) {
                        /*
                         * personal folder is shown elsewhere - include "shared files" only if shared contents are actually available
                         */
                        TIntList visibleSubfolderIDs = OXFolderIteratorSQL.getVisibleSubfolders(
                            folder.getObjectID(), user.getId(), user.getGroups(), permissionBits.getAccessibleModules(), context, connection);
                        if (1 < visibleSubfolderIDs.size() ||
                            1 == visibleSubfolderIDs.size() && false == visibleSubfolderIDs.remove(getDefaultInfoStoreFolderId(session, context, connection)) ||
                            0 < new OXFolderAccess(connection, context).getItemCount(folder, session, context) ||
                            FederatedSharingFolders.hasFederalSharingAccount(session)) {
                            subfolderIDs.add(0, new String[] { String.valueOf(folder.getObjectID()), stringHelper.getString(SYSTEM_USER_FILES_FOLDER_NAME) });
                        }
                    } else {
                        String name = stringHelper.getString(altNames ? SYSTEM_USER_FILES_FOLDER_NAME : FolderStrings.SYSTEM_USER_INFOSTORE_FOLDER_NAME);
                        subfolderIDs.add(0, new String[] { String.valueOf(folder.getObjectID()), name });
                    }
                } else if (SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID == folder.getObjectID()) {
                    /*
                     * only include public infostore root if there are visible subfolders, or user has full public folder access and is able to create subfolders
                     */
                    if (permissionBits.hasFullPublicFolderAccess() && folder.getEffectiveUserPermission(user.getId(), permissionBits, connection).canCreateSubfolders() ||
                        0 < OXFolderIteratorSQL.getVisibleSubfolders(folder.getObjectID(), user.getId(), user.getGroups(), permissionBits.getAccessibleModules(), context, connection).size() ||
                        FederatedSharingFolders.hasFederalSharingAccount(session)) {
                        String name = stringHelper.getString(altNames ? SYSTEM_PUBLIC_FILES_FOLDER_NAME : FolderStrings.SYSTEM_PUBLIC_INFOSTORE_FOLDER_NAME);
                        subfolderIDs.add(new String[] { String.valueOf(folder.getObjectID()), name });
                    }
                } else if (FolderObject.TRASH == folder.getType() && folder.isDefaultFolder()) {
                    String name = stringHelper.getString(altNames ? SYSTEM_TRASH_FILES_FOLDER_NAME : SYSTEM_TRASH_INFOSTORE_FOLDER_NAME);
                    subfolderIDs.add(new String[] { String.valueOf(folder.getObjectID()), name });
                } else {
                    subfolderIDs.add(new String[] { String.valueOf(folder.getObjectID()), folder.getFolderName() });
                }
            }
        } catch (SQLException e) {
            throw FolderExceptionErrorMessage.SQL_ERROR.create(e, e.getMessage());
        } finally {
            SearchIterators.close(searchIterator);
        }
        /*
         * also include the virtual root if there are any non-tree-visible folders for the user
         */
        if (OXFolderIteratorSQL.hasVisibleFoldersNotSeenInTreeView(FolderObject.INFOSTORE, user.getId(), user.getGroups(), permissionBits, context, connection)) {
            String name = stringHelper.getString(altNames ? VIRTUAL_LIST_FILES_FOLDER_NAME : VIRTUAL_LIST_INFOSTORE_FOLDER_NAME);
            subfolderIDs.add(new String[] { String.valueOf(VIRTUAL_LIST_INFOSTORE_FOLDER_ID), name });
        }
        return subfolderIDs;
    }

    /**
     * Gets a value indicating whether the user's personal infostore folder should appear below the system infostore folder
     * ({@value FolderObject#SYSTEM_INFOSTORE_FOLDER_ID}), or below the userstore folder
     * ({@link FolderObject#SYSTEM_USER_INFOSTORE_FOLDER_ID}), next to the default folders of other users.
     * 
     * @param session The user's session
     * @param altNames <code>true</code> to prefer alternative names for infostore folders, <code>false</code>, otherwise
     * @return <code>true</code> if the folder is shown below the root infostore folder, <code>false</code>, otherwise
     */
    public static boolean showPersonalBelowInfoStore(final Session session, final boolean altNames) {
        if (!altNames) {
            return false;
        }
        final String paramName = "com.openexchange.folderstorage.outlook.showPersonalBelowInfoStore";
        final Boolean tmp = (Boolean) session.getParameter(paramName);
        if (null != tmp) {
            return tmp.booleanValue();
        }
        final ConfigViewFactory configViewFactory = ServerServiceRegistry.getInstance().getService(ConfigViewFactory.class);
        if (null == configViewFactory) {
            return false;
        }
        try {
            final ConfigView view = configViewFactory.getView(session.getUserId(), session.getContextId());
            final Boolean b = view.opt(paramName, boolean.class, Boolean.FALSE);
            if (session instanceof PutIfAbsent) {
                ((PutIfAbsent) session).setParameterIfAbsent(paramName, b);
            } else {
                session.setParameter(paramName, b);
            }
            return b.booleanValue();
        } catch (OXException e) {
            LOGGER.debug("", e);
            return false;
        }
    }

    /**
     * Gets the identifier of the user's default infostore folder.
     *
     * @param session The session
     * @param ctx The context
     * @param connection The database connection to use
     * @return The identifier of the user's default infostore folder, or <code>-1</code> if there is none
     */
    public static int getDefaultInfoStoreFolderId(Session session, Context ctx, Connection connection) {
        String parameterName = "com.openexchange.folderstorage.defaultInfoStoreFolderId";
        String parameterValue = (String) session.getParameter(parameterName);
        if (null != parameterValue) {
            try {
                return Integer.parseInt(parameterValue);
            } catch (NumberFormatException e) {
                LOGGER.warn("Error parsing folder ID from session parameter", e);
            }
        }
        int folderID = -1;
        try {
            folderID = new OXFolderAccess(connection, ctx).getDefaultFolderID(session.getUserId(), FolderObject.INFOSTORE);
        } catch (OXException e) {
            if (false == OXFolderExceptionCode.NO_DEFAULT_FOLDER_FOUND.equals(e)) {
                LOGGER.warn("Error gettting default infostore folder ID", e);
                return folderID;
            }
        }
        if ((session instanceof PutIfAbsent)) {
            ((PutIfAbsent) session).setParameterIfAbsent(parameterName, String.valueOf(folderID));
        } else {
            session.setParameter(parameterName, String.valueOf(folderID));
        }
        return folderID;
    }

}
