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

import java.util.List;
import com.openexchange.file.storage.IdAndName;

/**
 * {@link DriveContentChange}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.3
 */
public interface DriveContentChange {

    /**
     * Gets the identifier of the folder where the change happened.
     *
     * @return The folder identifier
     */
    String getFolderId();

    /**
     * Gets the identifier of the user in whose "my files" subtree the change happened.
     *
     * @return The identifier of the user in whose "my files" subtree the change happened, or <code>-1</code> if not applicable
     */
    int getMyFilesUserId();

    /**
     * Gets the full folder path down to the (system) root folder, w/o considering an individual view on the path.
     * 
     * @return The full path to root, as list of id/name pairs
     */
    List<IdAndName> getPathToRoot();

    /**
     * Gets the full folder path down to the (system) root folder.
     * 
     * @param userId The identifier of the user to get the (individual) path for
     * @return The full path to root, as list of id/name pairs
     */
    List<IdAndName> getPathToRoot(int userId);

    /**
     * Gets a value indicating whether the content change happened within the subtree of a specific root folder or not.
     *
     * @param userId The identifier of the user to perform the check for
     * @param rootFolderId The root folder identifier to check
     * @return <code>true</code> if the folder change happened in or in a subfolder of the passed root folder, <code>false</code>, otherwise
     */
    boolean isSubfolderOf(int userId, String rootFolderId);

    /**
     * Gets the name of the path to the folder where the change happened, relative to a specific root folder.
     *
     * @param userId The identifier of the user to get the (individual) path for
     * @param rootFolderId The root folder identifier for which the path should be created
     * @return The relative path to the root folder
     */
    String getPath(int userId, String rootFolderId);

}
