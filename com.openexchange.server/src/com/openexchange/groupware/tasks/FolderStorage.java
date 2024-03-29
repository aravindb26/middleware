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

package com.openexchange.groupware.tasks;

import java.sql.Connection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.impl.DBPool;

/**
 * Interface to different SQL implementations for storing task folder mapping.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public abstract class FolderStorage {

    /**
     * Singleton attribute.
     */
    private static final FolderStorage SINGLETON = new RdbFolderStorage();

    /**
     * Default constructor.
     */
    protected FolderStorage() {
        super();
    }

    /**
     * @return the singleton implementation.
     */
    public static FolderStorage getInstance() {
        return SINGLETON;
    }

    /**
     * Inserts task folder mappings.
     * @param ctx Context.
     * @param con writable database connection.
     * @param taskId unique identifier of the task.
     * @param folders folder mappings to insert.
     * @param type storage type of folder mappings.
     * @throws OXException if an exception occurs.
     */
    public abstract void insertFolder(Context ctx, Connection con, int taskId,
        Set<Folder> folders, StorageType type) throws OXException;

    /**
     * Inserts a task folder mapping.
     * @param ctx Context.
     * @param con writable database connection.
     * @param taskId unique identifier of the task.
     * @param folder folder mapping to insert.
     * @param type storage type of folder mappings.
     * @throws OXException if an exception occurs.
     */
    void insertFolder(final Context ctx, final Connection con, final int taskId,
        final Folder folder, final StorageType type) throws OXException {
        final Set<Folder> folders = new HashSet<>();
        folders.add(folder);
        insertFolder(ctx, con, taskId, folders, type);
    }

    /**
     * Reads the folder mappings of a task.
     * @param ctx Context.
     * @param con readable database connection.
     * @param taskId unique identifier of the task.
     * @param type storage type of the folder mappings.
     * @return the folder objects.
     * @throws OXException if an exception occurs.
     */
    public abstract Set<Folder> selectFolder(Context ctx, Connection con, int taskId,
        StorageType type) throws OXException;

    /**
     * Reads the folder mappings of a task.
     * @param ctx Context.
     * @param taskId unique identifier of the task.
     * @param type storage type of the folder mappings.
     * @return the folder objects.
     * @throws OXException if an exception occurs.
     */
    Set<Folder> selectFolder(final Context ctx, final int taskId,
        final StorageType type) throws OXException {
        final Connection con = DBPool.pickup(ctx);
        try {
            return selectFolder(ctx, con, taskId, type);
        } finally {
            DBPool.closeReaderSilent(ctx, con);
        }
    }

    /**
     * Selects a task folder mapping for a user.
     * @param ctx Context.
     * @param con readable database connection.
     * @param taskId unique identifier of the task.
     * @param userId unique identifier of the user.
     * @param type storage type of folder mapping that should be selected.
     * @return the folder or <code>null</code> if no folder could be found
     * especially for the user. This can occur for public folders.
     * @throws OXException if an exception occurs.
     */
    public abstract Folder selectFolderByUser(Context ctx, Connection con, int taskId,
        int userId, StorageType type) throws OXException;

    /**
     * Reads a task folder mapping.
     * @param ctx Context.
     * @param con readable database connection.
     * @param taskId unique identifier of the task.
     * @param folderId unique identifier of the folder.
     * @param type storage type of the folder mapping.
     * @return the folder object or <code>null</code> if no folder could be
     * found.
     * @throws OXException if an error occurs.
     */
    abstract Folder selectFolderById(Context ctx, Connection con, int taskId,
        int folderId, StorageType type) throws OXException;

    /**
     * Reads a task folder mapping.
     * @param ctx Context.
     * @param taskId unique identifier of the task.
     * @param folderId unique identifier of the folder.
     * @param type storage type of the folder mapping.
     * @return the folder object or <code>null</code> if no folder could be found.
     * @throws OXException if an error occurs.
     */
    Folder selectFolderById(final Context ctx, final int taskId, final int folderId, final StorageType type) throws OXException {
        final Connection con = DBPool.pickup(ctx);
        try {
            return selectFolderById(ctx, con, taskId, folderId, type);
        } finally {
            DBPool.closeReaderSilent(ctx, con);
        }
    }

    /**
     * Deletes task folder mappings.
     * @param ctx Context.
     * @param con writable database connection.
     * @param taskId unique identifier of the task.
     * @param folderIds array of folder identifier to delete for the task.
     * @param type storage type of folder mapping that should be deleted.
     * @param sanityCheck if <code>true</code> it will be checked if all given
     * folders have been deleted.
     * @throws OXException if an exception occurs or if not all folder can be
     * deleted and sanityCheck is <code>true</code>.
     */
    abstract void deleteFolder(Context ctx, Connection con, int taskId,
        int[] folderIds, StorageType type, boolean sanityCheck) throws OXException;

    /**
     * Deletes a task folder mapping.
     * @param ctx Context.
     * @param con writable database connection.
     * @param taskId unique identifier of the task.
     * @param folderId folder identifier of the to delete mapping.
     * @param type storage type of folder mapping that should be deleted.
     * @throws OXException if an exception occurs.
     */
    void deleteFolder(final Context ctx, final Connection con, final int taskId,
        final int folderId, final StorageType type) throws OXException {
        deleteFolder(ctx, con, taskId, new int[] { folderId }, type, true);
    }

    /**
     * Deletes a task folder mapping.
     * @param ctx Context.
     * @param con writable database connection.
     * @param taskId unique identifier of the task.
     * @param folderId folder identifier of the to delete mapping.
     * @param type storage type of folder mapping that should be deleted.
     * @param sanityCheck <code>true</code> to check if the folder is really
     * deleted.
     * @throws OXException if an exception occurs or if no folder is deleted
     * and sanityCheck is <code>true</code>.
     */
    void deleteFolder(final Context ctx, final Connection con, final int taskId,
        final int folderId, final StorageType type, final boolean sanityCheck)
        throws OXException {
        deleteFolder(ctx, con, taskId, new int[] { folderId }, type, sanityCheck);
    }

    /**
     * Deletes task folder mappings.
     * @param ctx Context.
     * @param con writable database connection.
     * @param taskId unique identifier of the task.
     * @param folders set of folder to delete for the task.
     * @param type storage type of folder mapping that should be deleted.
     * @throws OXException if an exception occurs.
     */
    void deleteFolder(final Context ctx, final Connection con, final int taskId,
        final Set<Folder> folders, final StorageType type)
        throws OXException {
        deleteFolder(ctx, con, taskId, folders, type, true);
    }

    /**
     * Deletes task folder mappings.
     * @param ctx Context.
     * @param con writable database connection.
     * @param taskId unique identifier of the task.
     * @param folders set of folder to delete for the task.
     * @param type storage type of folder mapping that should be deleted.
     * @param sanityCheck if <code>true</code> it will be checked if all given
     * folders have been deleted.
     * @throws OXException if an exception occurs or if not all folder can be
     * deleted and sanityCheck is <code>true</code>.
     */
    void deleteFolder(final Context ctx, final Connection con, final int taskId,
        final Set<Folder> folders, final StorageType type,
        final boolean sanityCheck) throws OXException {
        if (0 == folders.size()) {
            return;
        }
        final int[] folderIds = new int[folders.size()];
        final Iterator<Folder> iter = folders.iterator();
        for (int i = 0; i < folderIds.length; i++) {
            folderIds[i] = iter.next().getIdentifier();
        }
        deleteFolder(ctx, con, taskId, folderIds, type, sanityCheck);
    }

    /**
     * Gets the identifier of tasks in a folder.
     * @param ctx Context.
     * @param con readonly database connection.
     * @param folderId identifier of the folder.
     * @param type ACTIVE or DELETED.
     * @return an int array of tasks in that folder.
     * @throws OXException if a sql problem occurs.
     */
    abstract int[] getTasksInFolder(Context ctx, Connection con, int folderId,
        StorageType type) throws OXException;

    /**
     * Selects all task folder mappings for a user.
     * @param ctx Context.
     * @param readCon readable database connection.
     * @param userId unique identifier of the user.
     * @param type storage type of the folder mapping to select.
     * @return an 2-dimensional int array. the first dimension represents all
     * found entries while the second contains folder and task identifier.
     * @throws OXException if an exception occurs.
     */
    abstract int[][] searchFolderByUser(Context ctx, Connection readCon,
        int userId, StorageType type) throws OXException;

    /**
     * Lists all tasks in folders owned by the given user
     *
     * @param ctx The context
     * @param con The connection to use
     * @param userId The id of the user which owns the folders
     * @return A mapping of folder ids to lists of task ids
     * @throws OXException
     */
    abstract Map<Integer, List<Integer>> listTasksInFoldersOwnedBy(Context ctx, Connection con, int userId) throws OXException;

    /**
     * @param folders Set of task folder mappings.
     * @param userId unique identifier of a user.
     * @return the folder mapping for the user or <code>null</code> if it can't
     * be found.
     */
    public static Folder extractFolderOfUser(final Set<Folder> folders, final int userId) {
        Folder retval = null;
        for (final Folder folder : folders) {
            if (folder.getUser() == userId) {
                retval = folder;
                break;
            }
        }
        return retval;
    }

    public static Set<Folder> extractNonParticipantFolder(Set<Folder> folders, Set<InternalParticipant> participants) {
        Set<Folder> retval = new HashSet<>();
        retval.addAll(folders);
        for (InternalParticipant participant : participants) {
            Folder remove = extractFolderOfUser(retval, participant.getIdentifier());
            if (null != remove) {
                retval.remove(remove);
            }
        }
        return retval;
    }

    static Folder getFolder(final Set<Folder> folders, final int folderId) {
        Folder retval = null;
        for (final Folder folder : folders) {
            if (folder.getIdentifier() == folderId) {
                retval = folder;
                break;
            }
        }
        return retval;
    }
}
