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

package com.openexchange.admin.tools.filestore;

import static com.openexchange.java.Autoboxing.I;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.daemons.ClientAdminThreadExtended;
import com.openexchange.admin.osgi.FilestoreLocationUpdaterRegistry;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.PoolException;
import com.openexchange.admin.rmi.exceptions.ProgrammErrorException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.utils.Filestore2UserUtil;
import com.openexchange.admin.tools.AdminCache;
import com.openexchange.database.Databases;
import com.openexchange.databaseold.Database;
import com.openexchange.exception.OXException;
import com.openexchange.filestore.FileStorage;
import com.openexchange.filestore.FileStorageCodes;
import com.openexchange.filestore.FilestoreDataMoveListener;
import com.openexchange.filestore.QuotaFileStorageExceptionCodes;
import com.openexchange.groupware.filestore.FileLocationHandler;
import com.openexchange.java.Streams;
import com.openexchange.osgi.ServiceListing;
import com.openexchange.osgi.ServiceListings;

/**
 * {@link FilestoreDataMover} - The base implementation to move files from one storage to another.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public abstract class FilestoreDataMover implements Callable<Void> {

    /** The logger constant */
    protected static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(FilestoreDataMover.class);

    // ------------------------------------------------------------------------------------------------------------------

    private static final AtomicReference<ServiceListing<FilestoreDataMoveListener>> LISTENERS_REF = new AtomicReference<ServiceListing<FilestoreDataMoveListener>>(null);

    /**
     * Applies given listener listing.
     *
     * @param listeners The listeners to apply
     */
    public static void setListeners(ServiceListing<FilestoreDataMoveListener> listeners) {
        LISTENERS_REF.set(listeners);
    }

    /**
     * Creates a new instance appropriate for moving the files for a single context.
     *
     * @param srcFilestore The source file storage
     * @param dstFilestore The destination file storage
     * @param ctx The associated context
     * @return The new instance
     */
    public static FilestoreDataMover newContextMover(Filestore srcFilestore, Filestore dstFilestore, Context ctx) {
        return new ContextFilestoreDataMover(srcFilestore, dstFilestore, ctx, LISTENERS_REF.get());
    }

    /**
     * Creates a new instance appropriate for moving the files to another storage location for a single user.
     *
     * @param srcFilestore The source file storage
     * @param dstFilestore The destination file storage
     * @param user The user
     * @param ctx The associated context
     * @return The new instance
     */
    public static FilestoreDataMover newUserMover(Filestore srcFilestore, Filestore dstFilestore, User user, Context ctx) {
        return new UserFilestoreDataMover(srcFilestore, dstFilestore, user, ctx, LISTENERS_REF.get());
    }

    /**
     * Creates a new instance appropriate for moving the files for a single user from an individual to a master storage.
     * <p>
     * <img src="./drive-business.png" alt="OX Drive Stand-Alone">
     *
     * @param srcFilestore The source file storage
     * @param dstFilestore The destination file storage
     * @param user The user
     * @param masterUser The master user
     * @param ctx The associated context
     * @return The new instance
     */
    public static FilestoreDataMover newUser2MasterMover(Filestore srcFilestore, Filestore dstFilestore, User user, User masterUser, Context ctx) {
        return new User2MasterUserFilestoreDataMover(srcFilestore, dstFilestore, user, masterUser, ctx, LISTENERS_REF.get());
    }

    /**
     * Creates a new instance appropriate for moving the files for a single user from a master to an individual storage.
     * <p>
     * <img src="./drive-standalone.png" alt="OX Drive Stand-Alone">
     *
     * @param srcFilestore The source file storage
     * @param dstFilestore The destination file storage
     * @param maxQuota The max. quota to apply
     * @param user The user
     * @param masterUser The master user
     * @param ctx The associated context
     * @return The new instance
     */
    public static FilestoreDataMover newUserFromMasterMover(Filestore srcFilestore, Filestore dstFilestore, long maxQuota, User user, User masterUser, Context ctx) {
        return new MasterUser2UserFilestoreDataMover(srcFilestore, dstFilestore, maxQuota, masterUser, user, ctx, LISTENERS_REF.get());
    }

    /**
     * Creates a new instance appropriate for moving files from a context to an individual user storage
     * <p>
     * <img src="./drive-userstorage.png" alt="OX Drive User Storage">
     *
     * @param srcFilestore The source file storage
     * @param dstFilestore The destination file storage
     * @param maxQuota The max. quota to apply
     * @param user The user
     * @param ctx The associated context
     * @return The new instance
     */
    public static FilestoreDataMover newContext2UserMover(Filestore srcFilestore, Filestore dstFilestore, long maxQuota, User user, Context ctx) {
        return new Context2UserFilestoreDataMover(srcFilestore, dstFilestore, maxQuota, user, ctx, LISTENERS_REF.get());
    }

    /**
     * Creates a new instance appropriate for moving files from an individual user to a context storage
     * <p>
     * <img src="./drive-userstorage.png" alt="OX Drive User Storage">
     *
     * @param srcFilestore The source file storage
     * @param dstFilestore The destination file storage
     * @param user The user
     * @param ctx The associated context
     * @return The new instance
     */
    public static FilestoreDataMover newUser2ContextMover(Filestore srcFilestore, Filestore dstFilestore, User user, Context ctx) {
        return new User2ContextFilestoreDataMover(srcFilestore, dstFilestore, user, ctx, LISTENERS_REF.get());
    }

    // ------------------------------------------------------------------------------------------------------------------

    /** The context */
    protected final Context ctx;

    /** The source file storage */
    protected final Filestore srcFilestore;

    /** The destination file storage */
    protected final Filestore dstFilestore;

    /** The queue holding post-process tasks */
    protected final Queue<PostProcessTask> postProcessTasks;

    /** The tracked listeners */
    private final ServiceListing<FilestoreDataMoveListener> listeners;

    /**
     * Initializes a new {@link FilestoreDataMover}.
     */
    protected FilestoreDataMover(Filestore srcFilestore, Filestore dstFilestore, Context ctx, ServiceListing<FilestoreDataMoveListener> listeners) {
        super();
        this.srcFilestore = srcFilestore;
        this.dstFilestore = dstFilestore;
        this.ctx = ctx;
        this.listeners = listeners;
        postProcessTasks = new ConcurrentLinkedQueue<PostProcessTask>();
    }

    /**
     * Gets the currently available listeners
     *
     * @return The listeners to notify
     */
    protected ServiceListing<FilestoreDataMoveListener> getListeners() {
        return null == listeners ? ServiceListings.<FilestoreDataMoveListener>emptyList() : listeners;
    }

    @Override
    public Void call() throws StorageException, IOException, InterruptedException, ProgrammErrorException {
        try {
            copy();
        } catch (StorageException e) {
            LOGGER.error("", e);
            // Because the client side only knows of the exceptions defined in the core we have
            // to throw the trace as string
            throw new StorageException(e.toString());
        } catch (IOException e) {
            LOGGER.error("", e);
            throw e;
        } catch (InterruptedException e) {
            LOGGER.error("", e);
            throw e;
        } catch (ProgrammErrorException e) {
            LOGGER.error("", e);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.error("", e);
            throw StorageException.storageExceptionFor(e);
        }
        return null;
    }

    /**
     * Adds given post-process task.
     *
     * @param task The task to execute when job is done successfully
     */
    public void addPostProcessTask(PostProcessTask task) {
        if (null != task) {
            postProcessTasks.add(task);
        }
    }

    /**
     * Gets the size in bytes for the denoted source directory
     *
     * @param sourcePathName The path name of the source directory
     * @return The size in bytes
     */
    public long getSize(String sourcePathName) {
        return FileUtils.sizeOfDirectory(new File(sourcePathName));
    }

    /**
     * Gets the list of files to copy from the source directory
     *
     * @param sourcePathName The path name of the source directory
     * @return The list of files to copy
     */
    public List<String> getFileList(String sourcePathName) {
        Collection<File> listFiles = FileUtils.listFiles(new File(sourcePathName), null, true);
        List<String> retval = new LinkedList<String>();
        for (final File file : listFiles) {
            retval.add(new StringBuilder(file.getPath()).append(File.pathSeparatorChar).append(file.getName()).toString());
        }
        return retval;
    }

    /**
     * Performs the copy (& delete).
     *
     * @throws StorageException If a storage error occurs
     * @throws InterruptedException If copy operation gets interrupted
     * @throws IOException If an I/O error occurs
     * @throws ProgrammErrorException If a program error occurs
     */
    private void copy() throws StorageException, IOException, InterruptedException, ProgrammErrorException {
        // Check "filestore2user" table
        if (Filestore2UserUtil.isNotTerminated(ClientAdminThreadExtended.cache)) {
            throw new StorageException("Table \"filestore2user\" not yet initialized");
        }

        // Pre-copy
        preDoCopy();

        Throwable thrown = null;
        try {
            // Do the copy
            doCopy(new URI(srcFilestore.getUrl()), new URI(dstFilestore.getUrl()));
        } catch (URISyntaxException e) {
            thrown = e; throw new StorageException(e.getMessage(), e);
        } catch (RuntimeException e) {
            LOGGER.error("", e);
            thrown = e; throw StorageException.storageExceptionFor(e);
        } catch (Error x) {
            thrown = x; throw x;
        } catch (Throwable t) {
            thrown = t; throw t;
        } finally {
            // Post-copy
            postDoCopy(thrown);

            executePostProcessTasks(null == thrown ? null : new ExecutionException(thrown));
        }
    }

    private static final int LOG_THRESHOLD = 100;

    /**
     * Copies specified files from source storage to destination storage.
     *
     * @param files The files to copy
     * @param srcStorage The source storage
     * @param dstStorage The destination storage
     * @param srcFullUri The fully qualifying URI from source storage
     * @param dstFullUri The fully qualifying URI from destination storage
     * @param optionalFileNotFoundHandler The optional file-not-found handler
     * @return The old file name to new file name mapping; [src-file] --&gt; [dst-file]
     * @throws StorageException If copy operation fails
     */
    protected CopyResult copyFiles(final Set<String> files, final FileStorage srcStorage, final FileStorage dstStorage, URI srcFullUri, URI dstFullUri, Optional<FileNotFoundHandler> optionalFileNotFoundHandler) throws StorageException {
        if (files.isEmpty()) {
            return new CopyResult(Collections.<String, String> emptyMap(), null);
        }

        final String threadName = Thread.currentThread().getName();
        final String moverName = getClass().getSimpleName();
        final Integer size = Integer.valueOf(files.size());

        LOGGER.info("{} is going to copy {} files from source \"{}\" to destination \"{}\" (with thread \"{}\").", moverName, size, srcFullUri, dstFullUri, threadName);

        if ("file".equalsIgnoreCase(srcFullUri.getScheme()) && "file".equalsIgnoreCase(dstFullUri.getScheme())) {
            // File-wise move possible
            try {
                final File srcParent = new File(srcFullUri);
                final File dstParent = new File(dstFullUri);
                long totalSize = 0L;

                int i = 1;
                for (String file : files) {
                    File toMove = new File(srcParent, file);
                    long length = toMove.length();
                    FileUtils.moveFile(toMove, new File(dstParent, file));

                    if ((i % LOG_THRESHOLD) == 0) {
                        LOGGER.info("{} moved {} files of {} in total (with thread \"{}\").", moverName, Integer.valueOf(i), size, threadName);
                    }
                    i++;

                    totalSize += length;
                }

                LOGGER.info("{} moved {} files (with thread \"{}\").", moverName, size, threadName);

                Reverter reverter = new Reverter() {

                    @Override
                    public void revertCopy() throws StorageException {
                        int numReverted = 0;
                        int i = 1;
                        for (String file : files) {
                            File toRevert = new File(dstParent, file);
                            try {
                                FileUtils.moveFile(toRevert, new File(srcParent, file));

                                if ((i % 10) == 0) {
                                    LOGGER.info("{} reverted {} files of {} in total (with thread \"{}\")", moverName, Integer.valueOf(i), size, threadName);
                                }
                                i++;
                                numReverted++;
                            } catch (IOException e) {
                                LOGGER.error("{} failed to revert file {} (with thread \"{}\")", moverName, toRevert.getPath(), threadName, e);
                            }
                        }

                        LOGGER.info("{} reverted {} files (with thread \"{}\").", moverName, Integer.valueOf(numReverted), threadName);
                    }
                };

                return new CopyResult(totalSize, reverter);
            } catch (IOException e) {
                throw new StorageException(e);
            }
        }

        // One-by-one...
        try {
            final Map<String, String> prevFileName2newFileName = new HashMap<String, String>(files.size());

            int i = 1;
            for (String file : files) {
                InputStream is = null;
                try {
                    is = srcStorage.getFile(file);
                    String newFile = dstStorage.saveNewFile(is);
                    if (null != newFile) {
                        prevFileName2newFileName.put(file, newFile);

                        if ((i % LOG_THRESHOLD) == 0) {
                            LOGGER.info("{} copied {} files of {} in total (with thread \"{}\")", moverName, Integer.valueOf(i), size, threadName);
                        }
                        i++;
                    }
                } catch (OXException e) {
                    if (optionalFileNotFoundHandler.isPresent() && FileStorageCodes.FILE_NOT_FOUND.equals(e)) {
                        optionalFileNotFoundHandler.get().handleFileNotFound(file, srcStorage);
                    } else {
                        // Re-throw
                        throw e;
                    }
                } finally {
                    Streams.close(is);
                }
            }

            LOGGER.info("{} copied {} files (with thread \"{}\").", moverName, size, threadName);

            Reverter reverter = new Reverter() {

                @Override
                public void revertCopy() throws StorageException {
                    String[] copiedFiles = prevFileName2newFileName.values().toArray(new String[size.intValue()]);
                    try {
                        dstStorage.deleteFiles(copiedFiles);
                    } catch (OXException x) {
                        // Failed bulk deletion - try one-by-one
                        for (String copiedFile : prevFileName2newFileName.values()) {
                            try {
                                dstStorage.deleteFile(copiedFile);
                            } catch (OXException e) {
                                LOGGER.error("{} failed to revert file {} (with thread \"{}\")", moverName, copiedFile, threadName, e.getCause() == null ? e : e.getCause());
                            }
                        }
                    }
                }
            };

            return new CopyResult(prevFileName2newFileName, reverter);
        } catch (OXException e) {
            throw StorageException.wrapForRMI(e);
        }
    }

    /**
     * Propagates new file locations throughout registered <code>FileLocationHandler</code> instances.
     *
     * @param prevFileName2newFileName The previous file name to new file name mapping
     * @throws OXException If an Open-Xchange error occurs
     * @throws SQLException If an SQL error occurs
     */
    protected void propagateNewLocations(Map<String, String> prevFileName2newFileName) throws OXException, SQLException {
        if (prevFileName2newFileName.isEmpty()) {
            return;
        }
        int contextId = ctx.getId().intValue();
        Connection con = Database.getNoTimeout(contextId, true);
        int rollback = 0;
        try {
            Databases.startTransaction(con);
            rollback = 1;

            for (FileLocationHandler updater : FilestoreLocationUpdaterRegistry.getInstance().getServices()) {
                updater.updateFileLocations(prevFileName2newFileName, contextId, con);
            }

            con.commit();
            rollback = 2;
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }
            Database.backNoTimeout(contextId, true, con);
        }
    }

    /**
     * Executes the post-process tasks.
     *
     * @param executionError The optional execution error that occurred or <code>null</code>
     * @throws StorageException If processing the tasks fails
     */
    protected void executePostProcessTasks(ExecutionException executionError) throws StorageException {
        for (PostProcessTask task; (task = postProcessTasks.poll()) != null;) {
            task.perform(executionError);
        }
    }

    /**
     * Determines the file locations for given user/context.
     *
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The associated file locations
     * @throws OXException If an Open-Xchange error occurs
     * @throws SQLException If an SQL error occurs
     */
    protected Set<String> determineFileLocationsFor(int userId, int contextId) throws OXException, SQLException {
        Set<String> srcFiles = null;
        {
            Connection con = Database.getNoTimeout(contextId, true);
            try {
                for (FileLocationHandler updater : FilestoreLocationUpdaterRegistry.getInstance().getServices()) {
                    if (srcFiles == null) {
                        srcFiles = new LinkedHashSet<String>(updater.determineFileLocationsFor(userId, contextId, con));
                    } else {
                        srcFiles.addAll(updater.determineFileLocationsFor(userId, contextId, con));
                    }
                }
            } finally {
                Database.backNoTimeout(contextId, false, con);
            }
        }
        return srcFiles == null ? Collections.emptySet() : srcFiles;
    }

    /**
     * <ul>
     * <li>Copies the files from source storage to destination storage
     * <li>Propagates new file locations throughout registered FilestoreLocationUpdater instances (if necessary)
     * <li>Deletes source files (if necessary)
     * <li>Applies changes & clears caches
     * </ul>
     *
     * @param srcBaseUri The base URI from source storage
     * @param dstBaseUri The base URI from destination storage
     * @throws StorageException If a storage error occurs
     * @throws InterruptedException If copy operation gets interrupted
     * @throws IOException If an I/O error occurs
     * @throws ProgrammErrorException If a program error occurs
     */
    protected abstract void doCopy(URI srcBaseUri, URI dstBaseUri) throws StorageException, IOException, InterruptedException, ProgrammErrorException;

    /**
     * Implementation hook to perform pre-copy operations (prior to {@link #doCopy(URI, URI)} is called).
     *
     * @throws StorageException If pre-copy operations fails
     */
    @SuppressWarnings("unused")
    protected void preDoCopy() throws StorageException {
        // Initially empty
    }

    /**
     * Implementation hook to perform post-copy operations (after {@link #doCopy(URI, URI)} was called).
     *
     * @param thrown The {@link Throwable} instance in case an error occurred; otherwise <code>null</code>
     */
    protected void postDoCopy(@SuppressWarnings("unused") Throwable thrown) {
        // Initially empty
    }

    // ---------------------------------------------------------------------------------------------------------------------------

    /**
     * Signals what operation was performed to pass files to new file storage location.
     */
    static enum Operation {
        /**
         * Signals that files were moved to the new location; keeping file identifiers
         */
        MOVED,
        /**
         * Signals that files were copied to new location; new file identifiers
         */
        COPIED;
    }

    /**
     * The result for {@link FilestoreDataMover#copyFiles(Set, FileStorage, FileStorage, URI, URI)} invocation.
     */
    static class CopyResult {

        /** Signals what operation was performed to pass files to new file storage location */
        final Operation operation;

        /** The total size of all files passed to new file storage location; <code>0</code> in case {@link #operation} is {@link Operation#COPIED} */
        final long totalSize;

        /** A mapping for previous to new file name; <code>null</code> in case {@link #operation} is {@link Operation#MOVED} */
        final Map<String, String> prevFileName2newFileName;

        /** Reverts the previously copied files */
        final Reverter reverter;

        CopyResult(long totalSize, Reverter reverter) {
            super();
            operation = Operation.MOVED;
            this.totalSize = totalSize;
            prevFileName2newFileName = null;
            this.reverter = reverter;
        }

        CopyResult(Map<String, String> prevFileName2newFileName, Reverter reverter) {
            super();
            operation = Operation.COPIED;
            this.prevFileName2newFileName = prevFileName2newFileName;
            totalSize = 0L;
            this.reverter = reverter;
        }
    }

    /**
     * Used to revert previously copied files.
     */
    static interface Reverter {

        /**
         * Reverts the previously copied files.
         */
        void revertCopy() throws StorageException;
    }

    /**
     * Runs the specified task safely.
     *
     * @param task The task
     */
    protected static void runSafely(Runnable task) {
        if (null != task) {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.error("Unexpected exception while running task", e);
            }
        }
    }

    // --------------------------------------------------------------------------------------------------------------------------

    protected static final class IncrementUsage {

        protected final long incrementUsage;
        protected final int ownerId;
        protected final int contextId;

        protected IncrementUsage(long usage, int ownerId, int contextId) {
            super();
            this.incrementUsage = usage;
            this.ownerId = ownerId;
            this.contextId = contextId;
        }
    }

    protected static IncrementUsage incUsage(long usage, int ownerId, int contextId) {
        return new IncrementUsage(usage, ownerId, contextId);
    }

    protected static final class DecrementUsage {

        protected final long decrementUsage;
        protected final int ownerId;
        protected final int contextId;

        protected DecrementUsage(long usage, int ownerId, int contextId) {
            super();
            this.decrementUsage = usage;
            this.ownerId = ownerId;
            this.contextId = contextId;
        }
    }

    protected static DecrementUsage decUsage(long usage, int ownerId, int contextId) {
        return new DecrementUsage(usage, ownerId, contextId);
    }

    /**
     * Increases/decreases the quota usages.
     *
     * @param incUsage The argument for increasing quota usage
     * @param decUsage The argument for decreasing quota usage
     * @throws StorageException If a database error occurs
     */
    protected static void changeUsage(DecrementUsage decUsage, IncrementUsage incUsage, int contextId) throws StorageException {
        AdminCache cache = ClientAdminThread.cache;
        Connection con = null;
        int rollback = 0;
        try {
            con = cache.getConnectionForContext(contextId);
            Databases.startTransaction(con);
            rollback = 1;

            doDecUsage(decUsage.decrementUsage, decUsage.ownerId, contextId, con);
            doIncUsage(incUsage.incrementUsage, incUsage.ownerId, contextId, con);

            con.commit();
            rollback = 2;
        } catch (PoolException e) {
            throw new StorageException(e);
        } catch (SQLException e) {
            throw new StorageException(e);
        } finally {
            if (rollback > 0) {
                if (rollback == 1) {
                    Databases.rollback(con);
                }
                Databases.autocommit(con);
            }

            if (null != con) {
                try {
                    cache.pushConnectionForContext(contextId, con);
                } catch (PoolException e) {
                    throw new StorageException(e);
                }
            }
        }
    }

    /**
     * Increases the quota usage.
     *
     * @param usage The value by which the quota is supposed to be increased
     * @throws StorageException If a database error occurs
     */
    private static void doIncUsage(long usage, int ownerId, int contextId, Connection con) throws StorageException {
        PreparedStatement sstmt = null;
        PreparedStatement ustmt = null;
        ResultSet rs = null;
        try {
            sstmt = con.prepareStatement("SELECT used FROM filestore_usage WHERE cid=? AND user=?");
            sstmt.setInt(1, contextId);
            sstmt.setInt(2, ownerId);
            rs = sstmt.executeQuery();
            final long oldUsage;
            if (rs.next()) {
                oldUsage = rs.getLong(1);
            } else {
                if (ownerId > 0) {
                    throw QuotaFileStorageExceptionCodes.NO_USAGE_USER.create(I(ownerId), I(contextId));
                }
                throw QuotaFileStorageExceptionCodes.NO_USAGE.create(I(contextId));
            }

            long newUsage = oldUsage + usage;
            ustmt = con.prepareStatement("UPDATE filestore_usage SET used=? WHERE cid=? AND user=?");
            ustmt.setLong(1, newUsage);
            ustmt.setInt(2, contextId);
            ustmt.setInt(3, ownerId);
            final int rows = ustmt.executeUpdate();
            if (rows == 0) {
                if (ownerId > 0) {
                    throw QuotaFileStorageExceptionCodes.UPDATE_FAILED_USER.create(I(ownerId), I(contextId));
                }
                throw QuotaFileStorageExceptionCodes.UPDATE_FAILED.create(I(contextId));
            }
        } catch (Exception s) {
            throw new StorageException(s);
        } finally {
            Databases.closeSQLStuff(rs);
            Databases.closeSQLStuff(sstmt);
            Databases.closeSQLStuff(ustmt);
        }
    }

    /**
     * Decreases the QuotaUsage.
     *
     * @param usage by that the Quota has to be decreased
     * @throws StorageException If a database error occurs
     */
    private static void doDecUsage(long usage, int ownerId, int contextId, Connection con) throws StorageException {
        PreparedStatement sstmt = null;
        PreparedStatement ustmt = null;
        ResultSet rs = null;
        try {
            sstmt = con.prepareStatement("SELECT used FROM filestore_usage WHERE cid=? AND user=?");
            sstmt.setInt(1, contextId);
            sstmt.setInt(2, ownerId);
            rs = sstmt.executeQuery();

            long oldUsage;
            if (rs.next()) {
                oldUsage = rs.getLong("used");
            } else {
                if (ownerId > 0) {
                    throw QuotaFileStorageExceptionCodes.NO_USAGE_USER.create(I(ownerId), I(contextId));
                }
                throw QuotaFileStorageExceptionCodes.NO_USAGE.create(I(contextId));
            }
            long newUsage = oldUsage - usage;

            if (newUsage < 0) {
                newUsage = 0;
                final OXException e = QuotaFileStorageExceptionCodes.QUOTA_UNDERRUN.create(I(ownerId), I(contextId));
                LOGGER.error("", e);
            }

            ustmt = con.prepareStatement("UPDATE filestore_usage SET used=? WHERE cid=? AND user=?");
            ustmt.setLong(1, newUsage);
            ustmt.setInt(2, contextId);
            ustmt.setInt(3, ownerId);

            int rows = ustmt.executeUpdate();
            if (1 != rows) {
                if (ownerId > 0) {
                    throw QuotaFileStorageExceptionCodes.UPDATE_FAILED_USER.create(I(ownerId), I(contextId));
                }
                throw QuotaFileStorageExceptionCodes.UPDATE_FAILED.create(I(contextId));
            }
        } catch (Exception s) {
            throw new StorageException(s);
        } finally {
            Databases.closeSQLStuff(rs);
            Databases.closeSQLStuff(sstmt);
            Databases.closeSQLStuff(ustmt);
        }
    }

}
