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

package com.openexchange.contact.provider.ldap;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.tools.functions.ErrorAwareSupplier;

/**
 * {@link LocalCache} is a local cache for LDAP contacts.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.6
 */
public class LocalCache {

    private final Map<FolderAndContactId, Contact> map;
    private final Multimap<String, Contact> folderMap;

    /**
     * Initializes a new {@link LocalCache} with given loader.
     *
     * @param loader The loader for the contacts to cache
     * @throws OXException If initialization fails
     */
    public LocalCache(ErrorAwareSupplier<List<Contact>> loader) throws OXException {
        super();
        List<Contact> contacts = loader.get();
        ImmutableMap.Builder<FolderAndContactId, Contact> map = ImmutableMap.builderWithExpectedSize(contacts.size());
        ImmutableMultimap.Builder<String, Contact> folderMap = ImmutableMultimap.builder();
        for (Contact contact : contacts) {
            map.put(toKey(contact.getFolderId(), contact.getId()), contact);
            folderMap.put(contact.getFolderId(), contact);
        }
        this.map = map.build();
        this.folderMap = folderMap.build();
    }

    /**
     * Gets all contacts
     *
     * @return All contacts within this cache
     */
    Collection<Contact> getAll() {
        return map.values();
    }

    /**
     * Gets the contact with the given identifier
     *
     * @param folderId The folder identifier
     * @param contactId The contact identifier
     * @return The Contact or null
     */
    Contact get(String folderId, String contactId) {
        return map.get(toKey(folderId, contactId));
    }

    /**
     * Returns all contacts for a given folder identifier
     *
     * @param folder The folder identifier
     * @return A list of contacts within this folder
     */
    Collection<Contact> getByFolder(String folder) {
        return folderMap.get(folder);
    }

    /**
     * Creates a key for the given folder and contact identifier
     *
     * @param folderId The folder identifier
     * @param contactId The contact identifier
     * @return The key
     */
    private FolderAndContactId toKey(String folderId, String contactId) {
        return new FolderAndContactId(folderId, contactId);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------

    private static class FolderAndContactId {

        private final String folderId;
        private final String contactId;
        private final int hash;

        FolderAndContactId(String folderId, String contactId) {
            super();
            this.folderId = folderId;
            this.contactId = contactId;
            hash = Objects.hash(folderId, contactId);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (!(obj instanceof FolderAndContactId)) {
                return false;
            }
            FolderAndContactId other = (FolderAndContactId) obj;
            if (folderId == null) {
                if (other.folderId != null) {
                    return false;
                }
            } else if (!folderId.equals(other.folderId)) {
                return false;
            }
            if (contactId == null) {
                if (other.contactId != null) {
                    return false;
                }
            } else if (!contactId.equals(other.contactId)) {
                return false;
            }
            return true;
        }
    }

}
