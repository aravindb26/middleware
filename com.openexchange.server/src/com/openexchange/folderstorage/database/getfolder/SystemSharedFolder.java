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

import java.sql.Connection;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.database.DatabaseFolder;
import com.openexchange.folderstorage.database.LocalizedDatabaseFolder;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.i18n.FolderStrings;
import com.openexchange.groupware.i18n.Groups;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.tools.iterator.FolderObjectIterator;
import com.openexchange.groupware.userconfiguration.UserPermissionBits;
import com.openexchange.i18n.tools.StringHelper;
import com.openexchange.java.Collators;
import com.openexchange.java.Strings;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIterators;
import com.openexchange.tools.oxfolder.OXFolderIteratorSQL;
import com.openexchange.tools.oxfolder.memory.ConditionTreeMap;
import com.openexchange.tools.oxfolder.memory.ConditionTreeMapManagement;
import com.openexchange.user.User;

/**
 * {@link SystemSharedFolder} - Gets the system shared folder.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SystemSharedFolder {

    /**
     * Initializes a new {@link SystemSharedFolder}.
     */
    private SystemSharedFolder() {
        super();
    }

    /**
     * Gets the database folder representing system shared folder for given user.
     *
     * @param fo The folder object fetched from database
     * @param user The user
     * @param userPerm The user permission bits
     * @param ctx The context
     * @param con The connection
     * @return The database folder representing system shared folder for given user
     * @throws OXException
     */
    public static DatabaseFolder getSystemSharedFolder(final FolderObject fo, final User user, final UserPermissionBits userPerm, final Context ctx, final Connection con) throws OXException {
        /*
         * The system shared folder
         */
        final DatabaseFolder retval = new LocalizedDatabaseFolder(fo, false);
        retval.setName(FolderStrings.SYSTEM_SHARED_FOLDER_NAME);
        // Enforce getSubfolders() from storage if at least one shared folder is accessible for user
        ConditionTreeMap treeMap = ConditionTreeMapManagement.getInstance().optMapFor(ctx.getContextId());
        if (null != treeMap) {
            /*
             * Set to null if a shared folder exists otherwise to empty array to indicate no subfolders
             */
            final boolean hasNext = treeMap.hasSharedFolder(user.getId(), user.getGroups(), userPerm.getAccessibleModules());
            retval.setSubfolderIDs(hasNext ? null : Strings.getEmptyStrings());
            retval.setSubscribedSubfolders(hasNext);
            return retval;
        }
        /*
         * Query database
         */
        final SearchIterator<FolderObject> searchIterator;
        searchIterator =
            OXFolderIteratorSQL.getVisibleSubfoldersIterator(
                FolderObject.SYSTEM_SHARED_FOLDER_ID,
                user.getId(),
                user.getGroups(),
                ctx,
                userPerm,
                null,
                con);
        try {
            /*
             * Set to null if a shared folder exists otherwise to empty array to indicate no subfolders
             */
            final boolean hasNext = searchIterator.hasNext();
            retval.setSubfolderIDs(hasNext ? null : Strings.getEmptyStrings());
            retval.setSubscribedSubfolders(hasNext);
        } finally {
            SearchIterators.close(searchIterator);
        }
        return retval;
    }

    /**
     * Gets the subfolder identifiers of database folder representing system shared folder for given user.
     *
     * @param user The user
     * @param userPerm The user permission bits
     * @param ctx The context
     * @param con The connection
     * @return The subfolder identifiers of database folder representing system shared folder for given user
     * @throws OXException If the database folder cannot be returned
     */
    public static List<String[]> getSystemSharedFolderSubfolder(final User user, final UserPermissionBits userPerm, final Context ctx, final Connection con) throws OXException {
        /*
         * The system shared folder
         */
        final Map<String, Integer> displayNames;
        {
            final Queue<FolderObject> q;
            q = ((FolderObjectIterator) OXFolderIteratorSQL.getVisibleSubfoldersIterator(
                    FolderObject.SYSTEM_SHARED_FOLDER_ID,
                    user.getId(),
                    user.getGroups(),
                    ctx,
                    userPerm,
                    null,
                    con)).asQueue();
            int size = q.size();
            if (size <= 0) {
                return Collections.emptyList();
            }
            /*
             * Gather all display names
             */
            StringHelper strHelper = null;
            displayNames = new HashMap<String, Integer>(size);
            UserStorage us = UserStorage.getInstance();
            Iterator<FolderObject> iter = q.iterator();
            for (int i = size; i-- > 0;) {
                final FolderObject sharedFolder = iter.next();
                String creatorDisplayName = null;
                try {
                    creatorDisplayName = us.getUser(sharedFolder.getCreatedBy(), ctx).getDisplayName();
                } catch (OXException e) {
                    if (sharedFolder.getCreatedBy() != OCLPermission.ALL_GROUPS_AND_USERS) {
                        throw e;
                    }
                }
                if (creatorDisplayName == null) {
                    if (strHelper == null) {
                        strHelper = StringHelper.valueOf(user.getLocale());
                    }
                    creatorDisplayName = strHelper.getString(Groups.ALL_USERS);
                }
                displayNames.putIfAbsent(creatorDisplayName, Integer.valueOf(sharedFolder.getCreatedBy()));
            }
        }
        /*
         * Sort display names and write corresponding virtual owner folder
         */
        final List<String> sortedDisplayNames = new ArrayList<String>(displayNames.keySet());
        Collections.sort(sortedDisplayNames, new DisplayNameComparator(user.getLocale()));
        final StringBuilder sb = new StringBuilder(8);
        final List<String[]> subfolderIds = new ArrayList<String[]>(displayNames.size());
        for (final String displayName : sortedDisplayNames) {
            sb.setLength(0);
            final int createdBy = displayNames.get(displayName).intValue();
            subfolderIds.add(new String[] {sb.append(FolderObject.SHARED_PREFIX).append(createdBy).toString(), displayName});
        }
        return subfolderIds;
    }

    /**
     * {@link DisplayNameComparator} - Sorts display names with respect to a certain locale.
     *
     * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
     */
    private static final class DisplayNameComparator implements Comparator<String> {

        private final Collator collator;

        public DisplayNameComparator(final Locale locale) {
            super();
            collator = Collators.getSecondaryInstance(locale);
        }

        @Override
        public int compare(final String displayName1, final String displayName2) {
            if (displayName1 == null) {
                return displayName2 == null ? 0 : -1;
            }
            return displayName2 == null ? 1 : collator.compare(displayName1, displayName2);
        }

    }

}
