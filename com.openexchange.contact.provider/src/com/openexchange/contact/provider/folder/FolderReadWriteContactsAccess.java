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

package com.openexchange.contact.provider.folder;

import java.util.Collections;
import java.util.List;
import com.openexchange.contact.ContactID;
import com.openexchange.contact.common.ContactsFolder;
import com.openexchange.contact.provider.extensions.ReadWriteAware;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contact.helpers.ContactField;
import com.openexchange.groupware.container.Contact;

/**
 * {@link FolderReadWriteContactsAccess}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public interface FolderReadWriteContactsAccess extends UpdateableFolderContactsAccess, ReadWriteAware {

    /**
     * Creates a new contact in the specified folder.
     *
     * @param folderId The identifier of the folder to create the event in
     * @param contact The contact data
     */
    void createContact(String folderId, Contact contact) throws OXException;

    /**
     * Updates an existing contact
     *
     * @param contactId The contact identifier
     * @param contact The contact data to update
     * @param clientTimestamp The last know timestamp by the client
     */
    void updateContact(ContactID contactId, Contact contact, long clientTimestamp) throws OXException;

    /**
     * Deletes an existing contact
     *
     * @param contactId The contact identifier
     * @param clientTimestamp The last know timestamp by the client
     */
    default void deleteContact(ContactID contactId, long clientTimestamp) throws OXException {
        deleteContacts(Collections.singletonList(contactId), clientTimestamp);
    }

    /**
     * Deletes all contacts with the specified contact identifiers
     *
     * @param contactIds The contact identifiers
     * @param clientTimestamp The last know timestamp by the client
     */
    void deleteContacts(List<ContactID> contactsIds, long clientTimestamp) throws OXException;

    /**
     * Creates a new folder.
     *
     * @param folder The folder data to create
     * @return The identifier of the newly created folder
     */
    String createFolder(ContactsFolder folder) throws OXException;

    /**
     * Deletes an existing folder.
     *
     * @param folderId The identifier of the folder to delete
     * @param clientTimestamp The last timestamp / sequence number known by the client to catch concurrent updates
     */
    void deleteFolder(String folderId, long clientTimestamp) throws OXException;

    /**
     * Returns if the provided {@link ContactField}s are supported by the storage. To 'support' the given field the storage should
     * be able to set new values for it. If at least one of the provided fields is not supported <code>false</code> will be
     * returned.
     *
     * @param folderId The ID of the folder to check
     * @param fields the contact fields that should be checked for support
     * @return <code>true</code> if all fields are supported; <code>false</code> if at least one is not supported
     */
    boolean supports(String folderId, ContactField... fields) throws OXException;

}
