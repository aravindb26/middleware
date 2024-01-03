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

import static com.openexchange.drive.events.internal.DriveEventServiceImpl.MY_FILES_NAME;
import static com.openexchange.drive.events.internal.DriveEventServiceImpl.SHARED_FILES_ID;
import java.util.ArrayList;
import java.util.List;
import com.openexchange.drive.events.DriveContentChange;
import com.openexchange.file.storage.IdAndName;

/**
 * {@link DriveContentChangeImpl}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.3
 */
public class DriveContentChangeImpl implements DriveContentChange {

    private final int myFilesUserId;
    private final String folderId;
    private final List<IdAndName> pathToRoot;

    /**
     * Initializes a new {@link DriveContentChangeImpl}.
     * 
     * @param folderId The affected folder id
     * @param pathToRoot The path to the root folder
     * @param myFilesUserId The identifier of the user in whose "my files" subtree the change happened, or <code>-1</code> if not applicable
     */
    public DriveContentChangeImpl(String folderId, List<IdAndName> pathToRoot, int myFilesUserId) {
        super();
        this.folderId = folderId;
        this.pathToRoot = pathToRoot;
        this.myFilesUserId = myFilesUserId;
    }

    @Override
    public String getFolderId() {
        return folderId;
    }

    @Override
    public int getMyFilesUserId() {
        return myFilesUserId;
    }

    @Override
    public List<IdAndName> getPathToRoot() {
        return pathToRoot;
    }

    @Override
    public List<IdAndName> getPathToRoot(int userId) {
        return userId == myFilesUserId ? adjustForMyFilesUser(pathToRoot) : pathToRoot;
    }

    @Override
    public boolean isSubfolderOf(int userId, String rootFolderId) {
        for (IdAndName idAndName : getPathToRoot(userId)) {
            if (idAndName.getId().equals(rootFolderId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getPath(int userId, String rootFolderId) {
        String path = "";
        for (IdAndName idAndName : getPathToRoot(userId)) {
            if (idAndName.getId().equals(rootFolderId)) {
                path = '/' + path;
                break;
            }
            path = path.isEmpty() ? idAndName.getName() : idAndName.getName() + '/' + path;
        }
        return path;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((folderId == null) ? 0 : folderId.hashCode());
        result = prime * result + myFilesUserId;
        result = prime * result + ((pathToRoot == null) ? 0 : pathToRoot.hashCode());
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
        DriveContentChangeImpl other = (DriveContentChangeImpl) obj;
        if (folderId == null) {
            if (other.folderId != null)
                return false;
        } else if (!folderId.equals(other.folderId))
            return false;
        if (myFilesUserId != other.myFilesUserId)
            return false;
        if (pathToRoot == null) {
            if (other.pathToRoot != null)
                return false;
        } else if (!pathToRoot.equals(other.pathToRoot))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "DriveContentChangeImpl [myFilesUserId=" + myFilesUserId + ", folderId=" + folderId + ", pathToRoot=" + pathToRoot + "]";
    }

    private static List<IdAndName> adjustForMyFilesUser(List<IdAndName> pathToRoot) {
        List<IdAndName> filteredPathToRoot = new ArrayList<IdAndName>(pathToRoot.size());
        for (int i = 0; i < pathToRoot.size(); i++) {
            IdAndName idAndName = pathToRoot.get(i);
            if (SHARED_FILES_ID.equals(idAndName.getId())) {
                /*
                 * skip "shared files" folder, and also adjust name of previous folder to "my files"
                 */
                if (0 < filteredPathToRoot.size()) {
                    int myFilesIndex = filteredPathToRoot.size() - 1;
                    IdAndName myFilesIdAndName = filteredPathToRoot.get(myFilesIndex);
                    filteredPathToRoot.set(myFilesIndex, new IdAndName(myFilesIdAndName.getId(), MY_FILES_NAME));
                }
                continue;
            }
            filteredPathToRoot.add(idAndName);
        }
        return filteredPathToRoot;
    }

}
