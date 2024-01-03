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

package com.openexchange.folderstorage;

import com.openexchange.exception.OXException;

/**
 * {@link SequenceFixingFolderStorage} - Extends a folder storage to search for file storage folders by name.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v8.0.0
 */
public interface SequenceFixingFolderStorage extends FolderStorage {

    /**
     * Checks if provided exception hints to a failure while applying next suitable folder identifier.
     *
     * @param e The exception to check
     * @return <code>true</code> if folder identifier sequence failure; otherwise <code>false</code>
     * @throws OXException
     */
    boolean isFolderSequenceFailure(OXException e) throws OXException;

    /**
     * Fixes the sequence used to determine next folder identifier.
     *
     * @param storageParameters The storage parameters
     * @return <code>true</code> if folder identifier sequence could be fixed; otherwise <code>false</code>
     * @throws OXException If fixing folder idenifier sequence fails
     */
    boolean fixFolderSequence(StorageParameters storageParameters) throws OXException;

}
