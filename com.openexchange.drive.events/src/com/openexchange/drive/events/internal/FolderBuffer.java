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

package com.openexchange.drive.events.internal;

import static com.openexchange.drive.events.internal.DriveEventServiceImpl.DRIVE_ROOT_ID;
import static com.openexchange.drive.events.internal.DriveEventServiceImpl.SHARED_FILES_ID;
import static com.openexchange.drive.events.internal.DriveEventServiceImpl.SHARED_FILES_ID_AND_NAME;
import static com.openexchange.file.storage.FileStorageEventConstants.FOLDER_ID;
import static com.openexchange.file.storage.FileStorageEventConstants.OLD_FOLDER_PERMISSIONS;
import static com.openexchange.file.storage.FileStorageEventConstants.OLD_PARENT_FOLDER_ID;
import static com.openexchange.file.storage.FileStorageEventConstants.PARENT_FOLDER_ID;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i2I;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import org.osgi.service.event.Event;
import com.openexchange.context.ContextService;
import com.openexchange.drive.DriveSession;
import com.openexchange.drive.events.DriveContentChange;
import com.openexchange.drive.events.DriveEventImpl;
import com.openexchange.event.CommonEvent;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageEventConstants;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageFolderType;
import com.openexchange.file.storage.FileStoragePermission;
import com.openexchange.file.storage.IdAndName;
import com.openexchange.file.storage.TypeAware;
import com.openexchange.file.storage.composition.IDBasedFolderAccess;
import com.openexchange.file.storage.composition.IDBasedFolderAccessFactory;
import com.openexchange.group.Group;
import com.openexchange.group.GroupService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.java.Strings;
import com.openexchange.session.Session;

/**
 * {@link FolderBuffer}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class FolderBuffer {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(FolderBuffer.class);
    private static final String UNDEFINED_PUSH_TOKEN = "{undefined}";

    /** The session parameter used to temporary influence 'altNames' behavior */
    private static final String ALTNAMES_SESSION_PARAMETER_NAME = "com.openexchange.folderstorage.altNames";

    private final int consolidationTime;
    private final int maxDelayTime ;
    private final int defaultDelayTime;
    private final int contextID;
    private final Map<Integer, Set<String>> affectedFoldersPerUser;
    private final Set<DriveContentChange> folderContentChanges;

    private boolean contentsChangedOnly;
    private String pushToken;
    private long lastEventTime;
    private long firstEventTime;

    /**
     * Initializes a new {@link FolderBuffer}.
     *
     * @param contextID The context ID
     * @param consolidationTime The consolidation time after which the buffer is considered to be ready to publish if no further
     *            folders were added
     * @param maxDelayTime The maximum time after which the buffer is considered to be ready to publish, independently of the
     *            consolidation interval
     * @param defaultDelayTime The (minimum) default delay time to wait after the first folder was added before being ready to publish
     */
    public FolderBuffer(int contextID, int consolidationTime, int maxDelayTime, int defaultDelayTime) {
        super();
        this.contextID = contextID;
        this.consolidationTime = consolidationTime;
        this.maxDelayTime = maxDelayTime;
        this.defaultDelayTime = defaultDelayTime;
        this.contentsChangedOnly = true;
        this.affectedFoldersPerUser = new HashMap<Integer, Set<String>>();
        this.folderContentChanges = new HashSet<DriveContentChange>();
    }

    /**
     * Gets a value indicating whether this buffer is ready to publish or not, based on the configured delay- and consolidation times.
     *
     * @return <code>true</code> if the event is due, <code>false</code>, otherwise
     */
    public synchronized boolean isReady() {
        if (affectedFoldersPerUser.isEmpty()) {
            return false; // no event added yet
        }
        long now = System.currentTimeMillis();
        long timeSinceFirstEvent = now - firstEventTime;
        LOG.trace("isDue(): now={}, firstEventTime={}, lastEventTime={}, timeSinceFirstEvent={}, timeSinceLastEvent={}",
            L(now), L(firstEventTime), L(lastEventTime), L(timeSinceFirstEvent), L((now - lastEventTime)));
        if (timeSinceFirstEvent > maxDelayTime) {
            return true; // max delay time exceeded
        }
        if (timeSinceFirstEvent > defaultDelayTime && now - lastEventTime > consolidationTime) {
            return true; // consolidation time since last event passed, and default delay time exceeded
        }
        return false;
    }

    /**
     * Gets the context ID.
     *
     * @return The context ID
     */
    public int getContexctID() {
        return this.contextID;
    }

    public synchronized void add(Session session, Event event) {
        if (session.getContextId() != this.contextID) {
            throw new IllegalArgumentException("session not in this context");
        }
        /*
         * record event time, reset 'first' event time if applicable
         */
        lastEventTime = System.currentTimeMillis();
        if (affectedFoldersPerUser.isEmpty()) {
            firstEventTime = lastEventTime;
        }
        /*
         * gather affected users and resolve to root
         */
        List<DriveContentChange> contentChanges = collectAffectedFoldersPerUser(session, event, affectedFoldersPerUser);
        /*
         * track separately if event denotes a change of a folder's contents
         */
        if (isAboutChangedContents(event)) {
            folderContentChanges.addAll(contentChanges);
        } else {
            contentsChangedOnly = false;
        }
        /*
         * check for client push token
         */
        String pushToken = (String) session.getParameter(DriveSession.PARAMETER_PUSH_TOKEN);
        if (Strings.isNotEmpty(pushToken)) {
            if (null == this.pushToken) {
                this.pushToken = pushToken; // use push token from event
            } else if (false == this.pushToken.equals(pushToken)) {
                this.pushToken = UNDEFINED_PUSH_TOKEN; // different push token - reset to undefined
            }
        } else {
            this.pushToken = UNDEFINED_PUSH_TOKEN; // no push token - reset to undefined
        }
    }


    /**
     * Drains all collected changes in this buffer and generates a drive event out of them. The buffer is cleared implicitly.
     *
     * @return A drive event, or <code>null</code> if there were no changes
     */
    public synchronized DriveEventImpl drain() {
        /*
         * initialize drive event
         */
        HashMap<Integer, Set<String>> foldersPerUser = new HashMap<Integer, Set<String>>(this.affectedFoldersPerUser);
        ArrayList<DriveContentChange> contentChanges = new ArrayList<DriveContentChange>(this.folderContentChanges);
        String token = UNDEFINED_PUSH_TOKEN.equals(this.pushToken) ? null : this.pushToken;
        DriveEventImpl driveEvent = foldersPerUser.isEmpty() ? null : new DriveEventImpl(contextID, foldersPerUser, contentChanges, contentsChangedOnly, false, token);
        /*
         * reset buffer state & return event
         */
        affectedFoldersPerUser.clear();
        folderContentChanges.clear();
        pushToken = null;
        contentsChangedOnly = true;
        return driveEvent;
    }

    /**
     * Collects the identifiers of all folders that are directly or indirectly affected by a change described by the given event, and
     * associates them to the corresponding user identifiers.
     * <p/>
     * <i>Relevant</i> are those users that have access to the folder(s) where the change happened (based on their permissions). Since
     * each user might see the folder in a specific (individual) path, the considered folder paths are determined individually per user.
     * <p/>
     * Drive content changes are created implicitly containing the plain paths down to the root folder, as well as the identifier of the
     * user associated with the userstore, if the change happened within this user's userstore subtree.
     *
     * @param session The underlying session to use for retrieving the actual folders
     * @param folderId The identifier of the folder to get the affected users for
     * @param overallAffectedFoldersPerUser The overall map of folders per affected user to contribute to
     * @return A list of drive content changes representing the path down to the root folder, or an empty list for irrelevant events
     */
    private static List<DriveContentChange> collectAffectedFoldersPerUser(Session session, Event event, Map<Integer, Set<String>> overallAffectedFoldersPerUser) {
        /*
         * extract relevant event properties
         */
        String folderId = (String) event.getProperty(FOLDER_ID);
        String parentFolderId = (String) event.getProperty(PARENT_FOLDER_ID);
        String oldParentFolderId = (String) event.getProperty(OLD_PARENT_FOLDER_ID);
        CommonEvent commonEvent = (CommonEvent) event.getProperty(CommonEvent.EVENT_KEY);
        FileStoragePermission[] oldFolderPermissions = (FileStoragePermission[]) event.getProperty(OLD_FOLDER_PERMISSIONS);
        /*
         * derive affected folders per user based on event kind
         */
        List<DriveContentChange> contentChanges = new ArrayList<DriveContentChange>();
        switch (event.getTopic()) {
            case FileStorageEventConstants.DELETE_TOPIC:
            case FileStorageEventConstants.CREATE_TOPIC:
            case FileStorageEventConstants.CREATE_FOLDER_TOPIC:
                /*
                 * Newly created or deleted file in folder, or new folder: all users having access to this folder are affected, and for
                 * them, all folders on the path down to the root folder are relevant
                 */
                collectAffectedFoldersPerUser(session, folderId, null, overallAffectedFoldersPerUser, null).ifPresent(c -> contentChanges.add(c));
                break;
            case FileStorageEventConstants.UPDATE_TOPIC:
                /*
                 * Updated file in folder: all users having access to this folder are affected, and for them, all folders
                 * on the path down to the root folder are relevant.
                 * Additionally, if old parent folder id is set, assume move operation, so also include all users having access to the
                 * previous folder, and for them, consider all folders on the path down to the root folder as relevant
                 */
                collectAffectedFoldersPerUser(session, folderId, null, overallAffectedFoldersPerUser, null).ifPresent(c -> contentChanges.add(c));
                if (null != oldParentFolderId) {
                    collectAffectedFoldersPerUser(session, oldParentFolderId, null, overallAffectedFoldersPerUser, null).ifPresent(c -> contentChanges.add(c));
                }
                break;
            case FileStorageEventConstants.UPDATE_FOLDER_TOPIC:
                /*
                 * Updated folder: all users having access to this folder are affected, and for them, all folders
                 * on the path down to the root folder are relevant.
                 * Additionally, if old parent folder id is set, assume move operation, so also include all users having access to the
                 * previous folder, and for them, consider all folders on the path down to the root folder as relevant.
                 * If affected users are available from common event, restrict the latter to the actually indicated ones.
                 */
                collectAffectedFoldersPerUser(session, folderId, oldFolderPermissions, overallAffectedFoldersPerUser, null).ifPresent(c -> contentChanges.add(c));
                if (null != oldParentFolderId) {
                    collectAffectedFoldersPerUser(session, oldParentFolderId, oldFolderPermissions, overallAffectedFoldersPerUser, commonEvent).ifPresent(c -> contentChanges.add(c));
                }
                break;
            case FileStorageEventConstants.DELETE_FOLDER_TOPIC:
                /*
                 * Deleted folder below specific parent folder: all users having had access to the deleted folder are affected, and for
                 * them, all folders on the path down to the root folder are relevant.
                 * Since previous permissions are no longer accessible from deleted folder, restrict the users to the actually indicated
                 * from common event if available.
                 */
                collectAffectedFoldersPerUser(session, parentFolderId, oldFolderPermissions, overallAffectedFoldersPerUser, commonEvent).ifPresent(c -> contentChanges.add(c));
                break;
            default:
                LOG.warn("Unexpected event topic: {}" + event.getTopic());
                break;
        }
        return contentChanges;
    }

    /**
     * Collects the identifiers of all folders that are directly or indirectly affected by a change that occurred within a specific
     * folder, and associates them to the corresponding user identifiers.
     * <p/>
     * <i>Relevant</i> are those users that have access to this folder (based on their permissions). Since each user might see the folder
     * in a specific (individual) path, the considered folder paths are determined individually per user.
     * <p/>
     * A drive content change is created implicitly containing the plain path down to the root folder, as well as the identifier of the
     * user associated with the userstore, if the change happened within this user's userstore subtree.
     *
     * @param session The underlying session to use for retrieving the actual folders
     * @param folderId The identifier of the folder to get the affected users for
     * @param oldPermissions The pre-event permissions for the given folder
     * @param overallAffectedFoldersPerUser The overall map of folders per affected user to contribute to
     * @param commonEvent If provided, the affected users will be restricted to the users found in this common event
     * @return An optional drive content change representing the path down to the root folder
     */
    private static Optional<DriveContentChange> collectAffectedFoldersPerUser(Session session, String folderId, FileStoragePermission[] oldPermissions, Map<Integer, Set<String>> overallAffectedFoldersPerUser, CommonEvent commonEvent) {
        if (null == folderId) {
            LOG.warn("Unable to derive affected users due to missing folder identifier");
            return Optional.empty();
        }
        try {
            /*
             * get folder and its path, gather affected users from first folder's permissions (where the change happened)
             */
            FileStorageFolder[] path2DefaultFolder = getPath2DefaultFolder(session, folderId);
            if (null == path2DefaultFolder || 0 == path2DefaultFolder.length) {
                LOG.warn("Can't get get path to default folder for {}, unable to derive affected users.", folderId);
                return Optional.empty();
            }
            /*
             * skip further processing if change happened in trash subtree
             */
            for (FileStorageFolder folder : path2DefaultFolder) {
                if ((folder instanceof TypeAware) && FileStorageFolderType.TRASH_FOLDER.equals(((TypeAware) folder).getType())) {
                    LOG.trace("Skipping further processing due to trash folder {} on path to default folder.", folder.getId());
                    return Optional.empty();
                }
            }
            Collection<Integer> affectedUserIds = getUserIds(session.getContextId(), path2DefaultFolder[0].getPermissions(), oldPermissions);
            /*
             * traverse & track folders on path down to root folder
             */
            Map<Integer, Set<String>> affectedFoldersPerUser = new HashMap<Integer, Set<String>>(affectedUserIds.size());
            List<IdAndName> pathToRoot = new ArrayList<IdAndName>(path2DefaultFolder.length);
            int myFilesUserId = -1;
            for (FileStorageFolder folder : path2DefaultFolder) {
                /*
                 * add to path & track folder id for all affected users, then check for any special handling
                 */
                pathToRoot.add(SHARED_FILES_ID.equals(folder.getId()) ? SHARED_FILES_ID_AND_NAME : new IdAndName(folder.getId(), folder.getName()));
                for (Integer userId : affectedUserIds) {
                    com.openexchange.tools.arrays.Collections.put(affectedFoldersPerUser, userId, folder.getId(), HashSet::new);
                }
                if (folder.isDefaultFolder() && DRIVE_ROOT_ID.equals(folder.getParentId())) {
                    /*
                     * change happened in a personal default folder rendered as "my files" folder below infostore root. Therefore,
                     * track user associated with that default folder, and artificially insert the "shared files" folder in the path and
                     * consider it for all affected users as well.
                     */
                    myFilesUserId = folder.getCreatedBy();
                    pathToRoot.add(SHARED_FILES_ID_AND_NAME);
                    for (Integer userId : affectedUserIds) {
                        com.openexchange.tools.arrays.Collections.put(affectedFoldersPerUser, userId, SHARED_FILES_ID, HashSet::new);
                    }
                }
                if (folder.isDefaultFolder() && SHARED_FILES_ID.equals(folder.getParentId())) {
                    /*
                     * change happened in a personal default folder rendered as a folder below "shared files", so remember the user
                     * associated with that default folder.
                     */
                    myFilesUserId = folder.getCreatedBy();
                }
            }
            /*
             * for ox drive sync, "showPersonalBelowInfostore" is always true, so if the change happened within a userstore folder (below
             * "my files" or "shared files"), ensure to remove a tracked "shared files" folder id again for the user associated with this
             * subtree (as remembered in "myFilesUser").
             */
            if (0 < myFilesUserId) {
                Set<String> folderIds = affectedFoldersPerUser.get(I(myFilesUserId));
                if (null != folderIds) {
                    folderIds.remove(SHARED_FILES_ID);
                }
            }
            /*
             * retain only those users in the result that are also indicated by the common event, if applicable
             */
            if (null != commonEvent && null != commonEvent.getAffectedUsersWithFolder()) {
                affectedFoldersPerUser.keySet().retainAll(commonEvent.getAffectedUsersWithFolder().keySet());
            }
            /*
             * add gathered folder ids per user to overall map, then generate & return content change result
             */
            merge(overallAffectedFoldersPerUser, affectedFoldersPerUser);
            return Optional.of(new DriveContentChangeImpl(folderId, pathToRoot, myFilesUserId));
        } catch (OXException e) {
            LOG.warn("Error resolving affected users for drive event in folder {}", folderId, e);
            return Optional.empty();
        }
    }

    private static FileStorageFolder[] getPath2DefaultFolder(Session session, String folderId) throws OXException {
        /*
         * inject "altNames=false" into session - will get propagated to folder storage from within
         * com.openexchange.file.storage.infostore.folder.AbstractInfostoreFolderAccess#initDecorator
         */
        String parameterName = ALTNAMES_SESSION_PARAMETER_NAME + '@' + Thread.currentThread().getId();
        Object oldParametervalue = session.getParameter(parameterName);
        IDBasedFolderAccess folderAccess = null;
        try {
            session.setParameter(parameterName, Boolean.FALSE.toString());
            folderAccess = DriveEventServiceLookup.getService(IDBasedFolderAccessFactory.class, true).createAccess(session);
            return folderAccess.getPath2DefaultFolder(folderId);
        } finally {
            session.setParameter(parameterName, oldParametervalue);
            if (null != folderAccess) {
                try {
                    folderAccess.finish();
                } catch (OXException e) {
                    LOG.debug("Unexpected error while closing folder access", e);
                }
            }
        }
    }

    private static Collection<Integer> getUserIds(int contextId, List<FileStoragePermission> permissions, FileStoragePermission... additionalPermissions) {
        Set<Integer> userIds = new HashSet<Integer>();
        if (null != permissions) {
            for (FileStoragePermission permission : permissions) {
                userIds.addAll(getUserIds(contextId, permission));
            }
        }
        if (null != additionalPermissions) {
            for (FileStoragePermission permission : additionalPermissions) {
                userIds.addAll(getUserIds(contextId, permission));
            }
        }
        return userIds;
    }

    private static Set<Integer> getUserIds(int contextId, FileStoragePermission permission) {
        int entity = permission.getEntity();
        if (0 <= entity) {
            return permission.isGroup() ? getGroupMembers(contextId, entity) : Collections.singleton(I(entity));
        }
        return Collections.emptySet();
    }

    private static Set<Integer> getGroupMembers(int contextId, int groupId) {
        try {
            Context context = DriveEventServiceLookup.getService(ContextService.class, true).getContext(contextId);
            Group group = DriveEventServiceLookup.getService(GroupService.class, true).getGroup(context, groupId, true);
            return new HashSet<Integer>(Arrays.asList(i2I(group.getMember())));
        } catch (OXException e) {
            LOG.debug("Error resolving members of group {}", I(groupId), e);
        }
        return Collections.emptySet();
    }

    private static boolean isAboutChangedContents(Event event) {
        String topic = event.getTopic();
        return FileStorageEventConstants.CREATE_TOPIC.equals(topic) || FileStorageEventConstants.UPDATE_TOPIC.equals(topic) || FileStorageEventConstants.DELETE_TOPIC.equals(topic);
    }

    private static <K, V, C extends Collection<V>> Map<K, C> merge(Map<K, C> into, Map<K, C> from) {
        if (null == into) {
            return from;
        }
        if (null == from) {
            return into;
        }
        for (Entry<K, C> entry : from.entrySet()) {
            C value = into.get(entry.getKey());
            if (null == value) {
                into.put(entry.getKey(), entry.getValue());
            } else {
                value.addAll(entry.getValue());
            }
        }
        return into;
    }

}
