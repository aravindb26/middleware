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

package com.openexchange.drive.events;

import static com.openexchange.java.Autoboxing.Coll2i;
import static com.openexchange.java.Autoboxing.I;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.openexchange.drive.DefaultDirectoryVersion;
import com.openexchange.drive.DirectoryVersion;
import com.openexchange.drive.DriveAction;
import com.openexchange.drive.DriveVersion;
import com.openexchange.drive.events.internal.SyncDirectoriesAction;
import com.openexchange.drive.events.internal.SyncDirectoryAction;
import com.openexchange.java.util.UUIDs;

/**
 * {@link DriveEventImpl}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DriveEventImpl implements DriveEvent {

    private final int contextID;
    private final int[] userIDs;
    private final Map<Integer, Set<String>> affectedFoldersPerUser;
    private final List<DriveContentChange> folderContentChanges;
    private final boolean contentChangesOnly;
    private final boolean remote;
    private final String pushTokenReference;

    /**
     * Initializes a new {@link DriveEventImpl}.
     *
     * @param contextID the context ID
     * @param affectedFoldersPerUser The identifiers of the affected folders for each user
     * @param folderContentChanges The tracked folder content changes, or <code>null</code> if not applicable
     * @param contentChangesOnly <code>true</code> if there are only folder content changes, <code>false</code>, otherwise
     * @param remote <code>true</code> it this event is 'remote', <code>false</code>, otherwise
     * @param pushTokenReference The push token of the device causing the event, or <code>null</code> if not applicable
     */
    public DriveEventImpl(int contextID, Map<Integer, Set<String>> affectedFoldersPerUser, List<DriveContentChange> folderContentChanges, boolean contentChangesOnly, boolean remote, String pushTokenReference) {
        super();
        this.contextID = contextID;
        this.userIDs = Coll2i(affectedFoldersPerUser.keySet());
        this.affectedFoldersPerUser = affectedFoldersPerUser;
        this.folderContentChanges = folderContentChanges;
        this.contentChangesOnly = contentChangesOnly;
        this.remote = remote;
        this.pushTokenReference = pushTokenReference;
    }

    @Override
    public int getContextID() {
        return contextID;
    }

    @Override
    public int[] getUserIDs() {
        return userIDs;
    }

    @Override
    public Set<String> getFolderIDs(int userId) {
        Set<String> folderIDs = affectedFoldersPerUser.get(I(userId));
        return null == folderIDs ? Collections.emptySet() : folderIDs;
    }

    @Override
    public Map<Integer, Set<String>> getAffectedFoldersPerUser() {
        return affectedFoldersPerUser;
    }

    @Override
    public List<DriveContentChange> getContentChanges() {
        return null == folderContentChanges ? Collections.emptyList() : folderContentChanges;
    }

    @Override
    public boolean isContentChangesOnly() {
        return contentChangesOnly;
    }

    @Override
    public List<DriveAction<? extends DriveVersion>> getActions(int userId, List<String> rootFolderIDs, boolean useContentChanges) {
        Set<String> folderIDs = getFolderIDs(userId);
        List<DriveAction<? extends DriveVersion>> actions = new ArrayList<DriveAction<? extends DriveVersion>>(rootFolderIDs.size());
        for (String rootFolderID : rootFolderIDs) {
            if (useContentChanges && contentChangesOnly) {
                for (DriveContentChange contentChange : folderContentChanges) {
                    /*
                     * add SYNC action for directory with content changes if root folder affected
                     */
                    if (contentChange.isSubfolderOf(userId, rootFolderID)) {
                        String syntheticChecksum = UUIDs.getUnformattedStringFromRandom();
                        DirectoryVersion version = new DefaultDirectoryVersion(contentChange.getPath(userId, rootFolderID), syntheticChecksum);
                        actions.add(new SyncDirectoryAction(rootFolderID, version));
                    }
                }
            } else {
                /*
                 * add SYNC action for all directories if root folder affected, or separate content changes are not used
                 */
                if (folderIDs.contains(rootFolderID)) {
                    actions.add(new SyncDirectoriesAction(rootFolderID));
                }
            }
        }
        return actions;
    }

    @Override
    public boolean isRemote() {
        return remote;
    }

    @Override
    public String getPushTokenReference() {
        return pushTokenReference;
    }

    @Override
    public String toString() {
        return "DriveEvent [remote=" + remote + ", contextID=" + contextID + ", affectedFoldersPerUser=" + affectedFoldersPerUser + ", pushToken=" + pushTokenReference + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((affectedFoldersPerUser == null) ? 0 : affectedFoldersPerUser.hashCode());
        result = prime * result + (contentChangesOnly ? 1231 : 1237);
        result = prime * result + contextID;
        result = prime * result + ((folderContentChanges == null) ? 0 : folderContentChanges.hashCode());
        result = prime * result + ((pushTokenReference == null) ? 0 : pushTokenReference.hashCode());
        result = prime * result + (remote ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DriveEventImpl other = (DriveEventImpl) obj;
        if (affectedFoldersPerUser == null) {
            if (other.affectedFoldersPerUser != null)
                return false;
        } else if (!affectedFoldersPerUser.equals(other.affectedFoldersPerUser))
            return false;
        if (contentChangesOnly != other.contentChangesOnly)
            return false;
        if (contextID != other.contextID)
            return false;
        if (folderContentChanges == null) {
            if (other.folderContentChanges != null)
                return false;
        } else if (!folderContentChanges.equals(other.folderContentChanges))
            return false;
        if (pushTokenReference == null) {
            if (other.pushTokenReference != null)
                return false;
        } else if (!pushTokenReference.equals(other.pushTokenReference))
            return false;
        if (remote != other.remote)
            return false;
        return true;
    }

}
