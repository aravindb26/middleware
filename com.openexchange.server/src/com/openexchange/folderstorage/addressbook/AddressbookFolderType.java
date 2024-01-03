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

package com.openexchange.folderstorage.addressbook;

import static com.openexchange.contact.common.ContactsAccount.ID_PREFIX;
import com.openexchange.folderstorage.FolderStorage;
import com.openexchange.folderstorage.FolderType;
import com.openexchange.tools.id.IDMangler;

/**
 * {@link AddressbookFolderType}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class AddressbookFolderType implements FolderType {

    /** The static prefix of addressbook folder identifiers (<code>con://</code>) */
    private static final String CONTACTS_PREFIX = IDMangler.mangle(ID_PREFIX, "");

    /**
     * Initializes a new {@link AddressbookFolderType}.
     */
    AddressbookFolderType() {
        super();
    }

    @Override
    public boolean servesTreeId(String treeId) {
        return FolderStorage.REAL_TREE_ID.equals(treeId);
    }

    @Override
    public boolean servesParentId(String folderId) {
        return FolderStorage.PRIVATE_ID.equals(folderId) || FolderStorage.SHARED_ID.equals(folderId) || FolderStorage.PUBLIC_ID.equals(folderId) || servesFolderId(folderId);
    }

    @Override
    public boolean servesFolderId(String folderId) {
        if (null == folderId) {
            return false;
        }
        // Check if a real provider is defined
        return folderId.startsWith(CONTACTS_PREFIX);
    }
}
